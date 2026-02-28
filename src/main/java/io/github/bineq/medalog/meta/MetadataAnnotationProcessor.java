package io.github.bineq.medalog.meta;

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
 * Annotation processor that extracts metadata annotations from Souffle programs and
 * generates a {@code .comp metadata { ... }} component containing:
 * <ul>
 *   <li>an {@code annotation} predicate declaration</li>
 *   <li>facts for asserted annotations on components and rules/facts</li>
 *   <li>facts encoding the component hierarchy</li>
 *   <li>facts encoding rule/fact membership in components</li>
 *   <li>Datalog rules for annotation inheritance (child inherits from parent
 *       component unless the child has the same key asserted)</li>
 * </ul>
 *
 * <p>Metadata annotations that use the reserved key {@code id} will cause an error.
 * Metadata annotations are specified as a set of key names; only annotations whose
 * key matches one of the specified metadata keys are processed.  All other {@code @[...]}
 * annotations are left intact.
 */
public class MetadataAnnotationProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataAnnotationProcessor.class);

    private static final String ROOT_ID = "_root";

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public void process(File input, File output, Set<String> metadataKeys) throws IOException {
        process(input.toPath(), output.toPath(), metadataKeys);
    }

    public void process(Path input, Path output, Set<String> metadataKeys) throws IOException {
        String result = process(
                Files.readString(input, StandardCharsets.UTF_8), metadataKeys);
        Files.writeString(output, result, StandardCharsets.UTF_8);
    }

    public void process(Reader input, Writer output, Set<String> metadataKeys) throws IOException {
        char[] buf = new char[8192];
        StringBuilder sb = new StringBuilder();
        int n;
        while ((n = input.read(buf)) != -1) sb.append(buf, 0, n);
        output.write(process(sb.toString(), metadataKeys));
    }

    public void process(InputStream input, OutputStream output,
                        Set<String> metadataKeys) throws IOException {
        process(new InputStreamReader(input, StandardCharsets.UTF_8),
                new OutputStreamWriter(output, StandardCharsets.UTF_8), metadataKeys);
    }

    /** Convenience overload accepting a comma-separated string of metadata keys. */
    public void process(Path input, Path output, String metadataKeysCSV) throws IOException {
        process(input, output, parseKeys(metadataKeysCSV));
    }

    /**
     * Processes a Souffle program and returns the transformed program.
     *
     * @param source       the Souffle program as a string
     * @param metadataKeys the set of annotation keys to treat as metadata
     * @return the transformed Souffle program
     */
    public String process(String source, Set<String> metadataKeys) {
        if (metadataKeys == null) metadataKeys = Collections.emptySet();
        // 'id' is reserved and must never appear as a metadata key
        for (String k : metadataKeys) {
            if ("id".equalsIgnoreCase(k)) {
                throw new AnnotationProcessorException(
                        "'id' must not be used as a metadata annotation key", 0);
            }
        }

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

        Set<String> finalMetadataKeys = metadataKeys;

        // Collected data for metadata component generation
        // component id → parent id
        Map<String, String> componentParent = new LinkedHashMap<>();
        // (id, key) → value — asserted annotations (duplicate key+different value = error)
        Map<String, Map<String, String>> assertedAnnotations = new LinkedHashMap<>();
        // id → component id (for rules and facts inside components)
        Map<String, String> membership = new LinkedHashMap<>();

        // ── Collect phase ─────────────────────────────────────────────────
        // Stack to track the enclosing component chain
        Deque<String> compStack = new ArrayDeque<>();
        compStack.push(ROOT_ID);

        ParseTreeWalker.DEFAULT.walk(new SouffleBaseListener() {

            private List<AnnotationContext> currentAnnotations = Collections.emptyList();

            @Override
            public void enterItem(ItemContext ctx) {
                currentAnnotations = ctx.annotation();
            }

            @Override
            public void enterCompDirective(CompDirectiveContext ctx) {
                String compId = ctx.IDENTIFIER(0).getText();
                String parentId = compStack.peek();
                componentParent.put(compId, parentId);
                compStack.push(compId);
                // Process metadata annotations on the component itself
                processAnnotations(compId, currentAnnotations, finalMetadataKeys,
                        assertedAnnotations, ctx.getStart().getLine());
            }

            @Override
            public void exitCompDirective(CompDirectiveContext ctx) {
                compStack.pop();
            }

            @Override
            public void enterRule_(Rule_Context ctx) {
                String ruleId = getRuleId(currentAnnotations, ctx.getStart().getLine());
                if (ruleId == null) return; // unnamed rule — skip metadata
                String enclosing = compStack.peek();
                if (!ROOT_ID.equals(enclosing)) membership.put(ruleId, enclosing);
                processAnnotations(ruleId, currentAnnotations, finalMetadataKeys,
                        assertedAnnotations, ctx.getStart().getLine());
            }

            @Override
            public void enterFact(FactContext ctx) {
                String factId = getRuleId(currentAnnotations, ctx.getStart().getLine());
                if (factId == null) return;
                String enclosing = compStack.peek();
                if (!ROOT_ID.equals(enclosing)) membership.put(factId, enclosing);
                processAnnotations(factId, currentAnnotations, finalMetadataKeys,
                        assertedAnnotations, ctx.getStart().getLine());
            }
        }, tree);

        // ── Rewrite: strip metadata annotations ─────────────────────────────
        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
        ParseTreeWalker.DEFAULT.walk(new SouffleBaseListener() {
            private List<AnnotationContext> currentAnnotations = Collections.emptyList();

            @Override
            public void enterItem(ItemContext ctx) {
                currentAnnotations = ctx.annotation();
            }

            @Override
            public void enterDeclDirective(DeclDirectiveContext ctx) {
                deleteMetadataAnnotations(currentAnnotations, finalMetadataKeys, rewriter);
            }

            @Override
            public void enterCompDirective(CompDirectiveContext ctx) {
                deleteMetadataAnnotations(currentAnnotations, finalMetadataKeys, rewriter);
            }

            @Override
            public void enterRule_(Rule_Context ctx) {
                deleteMetadataAnnotations(currentAnnotations, finalMetadataKeys, rewriter);
            }

            @Override
            public void enterFact(FactContext ctx) {
                deleteMetadataAnnotations(currentAnnotations, finalMetadataKeys, rewriter);
            }
        }, tree);

        String rewritten = rewriter.getText();

        // ── Append metadata component ────────────────────────────────────────
        if (!assertedAnnotations.isEmpty() || !componentParent.isEmpty()) {
            rewritten = rewritten + "\n" + generateMetadataComponent(
                    componentParent, assertedAnnotations, membership);
        }

        return rewritten;
    }

    // -----------------------------------------------------------------------
    // Metadata component generation
    // -----------------------------------------------------------------------

    private String generateMetadataComponent(
            Map<String, String> componentParent,
            Map<String, Map<String, String>> assertedAnnotations,
            Map<String, String> membership) {

        StringBuilder sb = new StringBuilder();
        sb.append(".comp _metadata {\n");

        // Predicate declarations
        sb.append("    .decl annotation(id: symbol, annotation_name: symbol, annotation_value: symbol)\n");
        sb.append("    .decl _assertedAnnotation(id: symbol, annotation_name: symbol, annotation_value: symbol)\n");
        sb.append("    .decl _annotationKeyAsserted(id: symbol, annotation_name: symbol)\n");
        sb.append("    .decl _compHierarchy(childId: symbol, parentId: symbol)\n");
        sb.append("    .decl _member(ruleId: symbol, compId: symbol)\n\n");

        // Component hierarchy facts
        for (Map.Entry<String, String> e : componentParent.entrySet()) {
            if (!ROOT_ID.equals(e.getValue())) {
                sb.append("    _compHierarchy(\"").append(escape(e.getKey()))
                  .append("\", \"").append(escape(e.getValue())).append("\").\n");
            }
        }

        // Membership facts
        for (Map.Entry<String, String> e : membership.entrySet()) {
            sb.append("    _member(\"").append(escape(e.getKey()))
              .append("\", \"").append(escape(e.getValue())).append("\").\n");
        }

        // Asserted annotation facts
        for (Map.Entry<String, Map<String, String>> byId : assertedAnnotations.entrySet()) {
            for (Map.Entry<String, String> kv : byId.getValue().entrySet()) {
                sb.append("    _assertedAnnotation(\"")
                  .append(escape(byId.getKey())).append("\", \"")
                  .append(escape(kv.getKey())).append("\", \"")
                  .append(escape(kv.getValue())).append("\").\n");
            }
        }

        sb.append("\n");
        // Helper: annotationKeyAsserted
        sb.append("    _annotationKeyAsserted(id, name) :- _assertedAnnotation(id, name, _).\n\n");

        // Annotation = asserted annotations
        sb.append("    annotation(id, name, value) :- _assertedAnnotation(id, name, value).\n\n");

        // Annotation inheritance: rule inherits from its component unless overridden
        sb.append("    // Annotation inheritance from component to member\n");
        sb.append("    annotation(memberId, name, value) :-\n");
        sb.append("        _member(memberId, compId),\n");
        sb.append("        _assertedAnnotation(compId, name, value),\n");
        sb.append("        !_annotationKeyAsserted(memberId, name).\n\n");

        // Annotation inheritance along component hierarchy
        sb.append("    // Annotation inheritance along component hierarchy\n");
        sb.append("    annotation(childId, name, value) :-\n");
        sb.append("        _compHierarchy(childId, parentId),\n");
        sb.append("        _assertedAnnotation(parentId, name, value),\n");
        sb.append("        !_annotationKeyAsserted(childId, name).\n\n");

        sb.append("    .output annotation\n");
        sb.append("}\n");
        sb.append(".init metadata = _metadata\n");

        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void processAnnotations(String id, List<AnnotationContext> annotations,
                                    Set<String> metadataKeys,
                                    Map<String, Map<String, String>> assertedAnnotations,
                                    int line) {
        for (AnnotationContext ann : annotations) {
            for (AnnotationPairContext pair : ann.annotationPair()) {
                String key = pair.annotationKey().getText();
                if (key.equalsIgnoreCase("id")) {
                    // @[id] is reserved for the identity processor; skip silently
                    continue;
                }
                if (!isMetadataKey(key, metadataKeys)) continue;

                // Warn if keys are equivalent but not equal
                for (String existingKey : metadataKeys) {
                    if (existingKey.equalsIgnoreCase(key) && !existingKey.equals(key)) {
                        LOG.warn("Line {}: annotation key '{}' is equivalent to '{}' but not equal",
                                line, key, existingKey);
                    }
                }

                String rawValue = pair.annotationValue().getText();
                String value = stripQuotes(rawValue);

                Map<String, String> byKey = assertedAnnotations
                        .computeIfAbsent(id, k -> new LinkedHashMap<>());
                if (byKey.containsKey(key) && !byKey.get(key).equals(value)) {
                    throw new AnnotationProcessorException(
                            "Conflicting annotation: key '" + key + "' has values '"
                            + byKey.get(key) + "' and '" + value + "' for '" + id + "'", line);
                }
                byKey.put(key, value);
            }
        }
    }

    private boolean isMetadataKey(String key, Set<String> metadataKeys) {
        if (metadataKeys.isEmpty()) return true; // no filter — all non-id keys are metadata
        for (String mk : metadataKeys) {
            if (mk.equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    private String getRuleId(List<AnnotationContext> annotations, int line) {
        for (AnnotationContext ann : annotations) {
            for (AnnotationPairContext pair : ann.annotationPair()) {
                if ("id".equals(pair.annotationKey().getText())) {
                    return stripQuotes(pair.annotationValue().getText());
                }
            }
        }
        return null;
    }

    private void deleteMetadataAnnotations(List<AnnotationContext> annotations,
                                            Set<String> metadataKeys,
                                            TokenStreamRewriter rewriter) {
        for (AnnotationContext ann : annotations) {
            boolean hasMetadata = ann.annotationPair().stream()
                    .anyMatch(p -> isMetadataKey(p.annotationKey().getText(), metadataKeys));
            if (hasMetadata) {
                rewriter.delete(ann.getStart(), ann.getStop());
            }
        }
    }

    private static Set<String> parseKeys(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptySet();
        Set<String> keys = new LinkedHashSet<>();
        for (String k : csv.split(",")) {
            String trimmed = k.trim();
            if (!trimmed.isEmpty()) keys.add(trimmed);
        }
        return keys;
    }

    private static String stripQuotes(String s) {
        if (s != null && s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
