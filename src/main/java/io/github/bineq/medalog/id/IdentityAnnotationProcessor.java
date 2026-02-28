package io.github.bineq.medalog.id;

import io.github.bineq.medalog.AnnotationProcessorException;
import io.github.bineq.medalog.SouffleLexer;
import io.github.bineq.medalog.SouffleParser;
import io.github.bineq.medalog.SouffleParser.*;
import io.github.bineq.medalog.SouffleBaseListener;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Annotation processor that adds identity (provenance) tracking to Souffle programs.
 *
 * <h3>Transformations applied:</h3>
 * <ul>
 *   <li>{@code .decl}: prepends {@code id: symbol} as the first slot (warns if already
 *       present; throws if an {@code id} slot exists but is not first or not
 *       {@code symbol} type).</li>
 *   <li>Facts: prepends the value from {@code @[id = "..."]} (or a generated ID) as the
 *       first argument.</li>
 *   <li>Rules: injects {@code _id1}, {@code _id2}, … into declared positive body atoms;
 *       injects {@code _} for declared negated body atoms; adds the aggregated provenance
 *       ID as the first argument of the head.</li>
 *   <li>{@code @[id = "..."]} annotations are consumed and removed from the output.</li>
 *   <li>Predicates that are not declared via {@code .decl} are passed through unchanged.</li>
 * </ul>
 */
public class IdentityAnnotationProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(IdentityAnnotationProcessor.class);

    private final AggregationStrategy strategy;

    public IdentityAnnotationProcessor() {
        this(new CatAggregationStrategy());
    }

    public IdentityAnnotationProcessor(AggregationStrategy strategy) {
        this.strategy = strategy;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public void process(File input, File output) throws IOException {
        process(input.toPath(), output.toPath());
    }

    public void process(Path input, Path output) throws IOException {
        String result = process(Files.readString(input, StandardCharsets.UTF_8));
        Files.writeString(output, result, StandardCharsets.UTF_8);
    }

    public void process(Reader input, Writer output) throws IOException {
        char[] buf = new char[8192];
        StringBuilder sb = new StringBuilder();
        int n;
        while ((n = input.read(buf)) != -1) sb.append(buf, 0, n);
        output.write(process(sb.toString()));
    }

    public void process(InputStream input, OutputStream output) throws IOException {
        process(new InputStreamReader(input, StandardCharsets.UTF_8),
                new OutputStreamWriter(output, StandardCharsets.UTF_8));
    }

    /**
     * Processes a Souffle program source string and returns the transformed program.
     */
    public String process(String source) {
        SouffleLexer lexer = new SouffleLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SouffleParser parser = new SouffleParser(tokens);

        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        BaseErrorListener errListener = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> rec, Object sym, int line,
                                    int col, String msg, RecognitionException e) {
                throw new AnnotationProcessorException("Syntax error: " + msg, line);
            }
        };
        lexer.addErrorListener(errListener);
        parser.addErrorListener(errListener);

        ProgramContext tree = parser.program();
        tokens.fill();

        // ── Pass 1: collect declared predicates ──────────────────────────────
        Set<String> declared = new LinkedHashSet<>();
        Set<String> alreadyHasId = new LinkedHashSet<>();
        ParseTreeWalker.DEFAULT.walk(new SouffleBaseListener() {
            @Override
            public void enterDeclDirective(DeclDirectiveContext ctx) {
                String name = ctx.qualifiedName().getText();
                SlotListContext slots = ctx.slotList();
                if (slots != null) {
                    List<SlotContext> list = slots.slot();
                    for (int i = 0; i < list.size(); i++) {
                        SlotContext s = list.get(i);
                        if ("id".equals(s.IDENTIFIER().getText())) {
                            if (i != 0) {
                                throw new AnnotationProcessorException(
                                        "Predicate '" + name + "': 'id' slot must be first",
                                        ctx.getStart().getLine());
                            }
                            if (!"symbol".equals(s.typeExpr().getText())) {
                                throw new AnnotationProcessorException(
                                        "Predicate '" + name + "': 'id' slot must be of type symbol",
                                        ctx.getStart().getLine());
                            }
                            LOG.warn("Line {}: predicate '{}' already has 'id: symbol' — skipping injection",
                                    ctx.getStart().getLine(), name);
                            alreadyHasId.add(name);
                            declared.add(name);
                            return;
                        }
                    }
                }
                declared.add(name);
            }
        }, tree);

        LOG.info("IdentityAnnotationProcessor: {} predicate(s) declared", declared.size());

        // ── Pass 2: rewrite using TokenStreamRewriter ────────────────────────
        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
        IdGenerator idGen = new IdGenerator();
        Set<String> seenIds = new LinkedHashSet<>();

        ParseTreeWalker.DEFAULT.walk(new SouffleBaseListener() {

            private List<AnnotationContext> currentAnnotations = Collections.emptyList();

            @Override
            public void enterItem(ItemContext ctx) {
                currentAnnotations = ctx.annotation();
            }

            // .decl ──────────────────────────────────────────────────────────
            @Override
            public void enterDeclDirective(DeclDirectiveContext ctx) {
                String name = ctx.qualifiedName().getText();
                deleteIdAnnotations(currentAnnotations, rewriter);
                if (alreadyHasId.contains(name)) return;

                SlotListContext slots = ctx.slotList();
                if (slots != null) {
                    rewriter.insertBefore(slots.slot(0).getStart(), "id: symbol, ");
                } else {
                    rewriter.insertAfter(ctx.LPAREN().getSymbol(), "id: symbol");
                }
            }

            // Facts ──────────────────────────────────────────────────────────
            @Override
            public void enterFact(FactContext ctx) {
                AtomContext atom = ctx.atom();
                String pred = atom.qualifiedName().getText();
                if (!declared.contains(pred)) {
                    deleteIdAnnotations(currentAnnotations, rewriter);
                    return;
                }

                String idValue = extractId(currentAnnotations, seenIds, ctx.getStart().getLine());
                if (idValue == null) idValue = idGen.nextFactId();
                deleteIdAnnotations(currentAnnotations, rewriter);

                insertFirstArg(atom, "\"" + idValue + "\"", rewriter);
            }

            // Rules ──────────────────────────────────────────────────────────
            @Override
            public void enterRule_(Rule_Context ctx) {
                String ruleId = extractId(currentAnnotations, seenIds, ctx.getStart().getLine());
                if (ruleId == null) ruleId = idGen.nextRuleId();
                deleteIdAnnotations(currentAnnotations, rewriter);

                List<String> idParts = new ArrayList<>();
                int[] idCounter = {0};

                for (ConjunctionContext conj : ctx.body().conjunction()) {
                    for (BodyLiteralContext lit : conj.bodyLiteral()) {
                        if (lit instanceof NegAtomContext neg) {
                            String pred = neg.atom().qualifiedName().getText();
                            if (declared.contains(pred)) {
                                idParts.add("\"!" + pred + "\"");
                                insertFirstArg(neg.atom(), "_", rewriter);
                            }
                        } else if (lit instanceof PosAtomContext pos) {
                            String pred = pos.atom().qualifiedName().getText();
                            if (declared.contains(pred)) {
                                idCounter[0]++;
                                String idVar = "_id" + idCounter[0];
                                idParts.add(idVar);
                                insertFirstArg(pos.atom(), idVar, rewriter);
                            }
                        }
                        // Constraints and grouped bodies: pass through unchanged
                    }
                }

                // Rewrite head atom(s)
                for (AtomContext headAtom : ctx.head().atom()) {
                    String pred = headAtom.qualifiedName().getText();
                    if (!declared.contains(pred)) continue;
                    String aggExpr = strategy.generateId("\"" + ruleId + "\"", idParts);
                    insertFirstArg(headAtom, aggExpr, rewriter);
                }
            }

        }, tree);

        return rewriter.getText();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Extracts the @[id = "..."] value, checking for duplicates. Returns null if absent. */
    private String extractId(List<AnnotationContext> annotations,
                              Set<String> seenIds, int line) {
        for (AnnotationContext ann : annotations) {
            for (AnnotationPairContext pair : ann.annotationPair()) {
                String key = pair.annotationKey().getText();
                if (key.equalsIgnoreCase("id")) {
                    if (!"id".equals(key)) {
                        LOG.warn("Line {}: annotation key '{}' is equivalent to 'id' but not equal", line, key);
                    }
                    String raw = pair.annotationValue().getText();
                    String value = stripQuotes(raw);
                    if (seenIds.contains(value)) {
                        throw new AnnotationProcessorException(
                                "Duplicate @id value: \"" + value + "\"", line);
                    }
                    seenIds.add(value);
                    return value;
                }
            }
        }
        return null;
    }

    /** Removes @[...] annotation tokens that contain an 'id' key. */
    private void deleteIdAnnotations(List<AnnotationContext> annotations,
                                     TokenStreamRewriter rewriter) {
        for (AnnotationContext ann : annotations) {
            boolean hasId = ann.annotationPair().stream()
                    .anyMatch(p -> p.annotationKey().getText().equalsIgnoreCase("id"));
            if (hasId) {
                rewriter.delete(ann.getStart(), ann.getStop());
            }
        }
    }

    /** Inserts an expression as the first argument of an atom. */
    private void insertFirstArg(AtomContext atom, String expr, TokenStreamRewriter rewriter) {
        ArgListContext args = atom.argList();
        Token lparen = atom.LPAREN().getSymbol();
        if (args != null) {
            rewriter.insertAfter(lparen, expr + ", ");
        } else {
            rewriter.insertAfter(lparen, expr);
        }
    }

    private static String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
