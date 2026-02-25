package io.github.bineq.medalog;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * MeDaLog compiler.
 *
 * <p>Compiles MeDaLog source (a Souffle Datalog extension with modules and annotations)
 * into plain Souffle Datalog.
 *
 * <p>Static {@code compile} overloads accept various input/output combinations
 * (String, Stream, File, Reader/Writer). All throw {@link CompilerException}
 * for semantic errors and {@link CompilerException} wrapping parse errors.
 *
 * <h2>Usage</h2>
 * <pre>
 *   java -cp &lt;classpath&gt; io.github.bineq.medalog.Compiler -i input.dl -o output.dl
 * </pre>
 */
public class Compiler {

    private static final Logger LOG = LoggerFactory.getLogger(Compiler.class);

    // Default aggregation strategy (pluggable)
    private static AggregationStrategy defaultStrategy = new CatAggregationStrategy();

    private Compiler() {}

    // ==============================
    // Public static compile API
    // ==============================

    /**
     * Compiles a MeDaLog source string and returns the Souffle output as a string.
     *
     * @param source MeDaLog source text
     * @return compiled Souffle Datalog
     * @throws CompilerException on semantic or parse errors
     */
    public static String compile(String source) {
        try {
            StringWriter sw = new StringWriter();
            compile(new StringReader(source), sw);
            return sw.toString();
        } catch (IOException e) {
            // StringReader/StringWriter never throw IOException
            throw new IllegalStateException("Unexpected I/O error on in-memory streams", e);
        }
    }

    /**
     * Compiles MeDaLog from an {@link InputStream} to an {@link OutputStream}.
     *
     * @param in  source stream (UTF-8 encoded)
     * @param out destination stream (UTF-8 encoded)
     * @throws CompilerException on semantic or parse errors
     * @throws IOException       on I/O errors
     */
    public static void compile(InputStream in, OutputStream out) throws IOException {
        compile(new InputStreamReader(in, StandardCharsets.UTF_8),
                new OutputStreamWriter(out, StandardCharsets.UTF_8));
    }

    /**
     * Compiles MeDaLog from a {@link Reader} to a {@link Writer}.
     *
     * @param reader source reader
     * @param writer output writer
     * @throws CompilerException on semantic or parse errors
     * @throws IOException       on I/O errors
     */
    public static void compile(Reader reader, Writer writer) throws IOException {
        String source = readAll(reader);
        String result = compileSource(source, defaultStrategy);
        writer.write(result);
        writer.flush();
    }

    /**
     * Compiles a MeDaLog input {@link File} to an output {@link File}.
     *
     * @param inputFile  source file
     * @param outputFile destination file
     * @throws CompilerException on semantic or parse errors
     * @throws IOException       on I/O errors
     */
    public static void compile(File inputFile, File outputFile) throws IOException {
        LOG.info("Compiling '{}' -> '{}'.", inputFile.getPath(), outputFile.getPath());
        try (Reader r = new FileReader(inputFile, StandardCharsets.UTF_8);
             Writer w = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
            compile(r, w);
        }
    }

    /**
     * Compiles a MeDaLog source string using a custom {@link AggregationStrategy}.
     *
     * @param source   MeDaLog source text
     * @param strategy aggregation strategy for ID generation
     * @return compiled Souffle Datalog
     * @throws CompilerException on semantic or parse errors
     */
    public static String compile(String source, AggregationStrategy strategy) {
        return compileSource(source, strategy);
    }

    // ==============================
    // Core compilation logic
    // ==============================

    static String compileSource(String source, AggregationStrategy strategy) {
        LOG.info("Starting compilation ({} characters).", source.length());

        // 1. Lex and parse
        CharStream charStream = CharStreams.fromString(source);
        MeDaLogLexer lexer = new MeDaLogLexer(charStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingErrorListener());

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MeDaLogParser parser = new MeDaLogParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowingErrorListener());

        ParseTree tree = parser.program();
        LOG.info("Parsing complete.");

        // 2. First pass: build symbol table
        SymbolTable symbolTable = new SymbolTable();
        SymbolTableBuilder symbolTableBuilder = new SymbolTableBuilder(symbolTable);
        ParseTreeWalker.DEFAULT.walk(symbolTableBuilder, tree);
        LOG.info("Symbol table built: {} predicate(s) declared.",
                symbolTable.getDeclaredPredicateNames().size());

        // 3. Second pass: generate output
        CodeGenerator generator = new CodeGenerator(symbolTable, strategy, source);
        generator.visit(tree);
        LOG.info("Code generation complete.");

        return generator.getOutput();
    }

    // ==============================
    // Error listener
    // ==============================

    /** ANTLR4 error listener that converts parse errors to {@link CompilerException}. */
    private static final class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine,
                                String msg, RecognitionException e) {
            throw new CompilerException(
                    "Syntax error at column " + charPositionInLine + ": " + msg, line, e);
        }
    }

    // ==============================
    // Utility
    // ==============================

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    // ==============================
    // main – CLI entry point
    // ==============================

    /**
     * Command-line entry point.
     *
     * <pre>
     * Usage: Compiler -i &lt;input&gt; -o &lt;output&gt;
     *   -i,--input  &lt;file&gt;   MeDaLog input file
     *   -o,--output &lt;file&gt;   Souffle Datalog output file
     * </pre>
     */
    public static void main(String[] args) {
        Options options = buildOptions();

        CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            printHelp(options);
            System.exit(1);
            return;
        }

        if (!cmd.hasOption("i") || !cmd.hasOption("o")) {
            printHelp(options);
            System.exit(1);
            return;
        }

        File inputFile = new File(cmd.getOptionValue("i"));
        File outputFile = new File(cmd.getOptionValue("o"));

        if (!inputFile.exists()) {
            System.err.println("Input file not found: " + inputFile.getPath());
            System.exit(2);
            return;
        }

        try {
            compile(inputFile, outputFile);
            System.out.println("Compilation successful: " + outputFile.getPath());
        } catch (CompilerException e) {
            System.err.println("Compiler error: " + e.getMessage());
            LOG.error("Compiler error", e);
            System.exit(3);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            LOG.error("I/O error", e);
            System.exit(4);
        }
    }

    private static Options buildOptions() {
        return new Options()
                .addOption(Option.builder("i")
                        .longOpt("input")
                        .hasArg()
                        .argName("file")
                        .desc("MeDaLog input file to compile")
                        .required()
                        .build())
                .addOption(Option.builder("o")
                        .longOpt("output")
                        .hasArg()
                        .argName("file")
                        .desc("Souffle Datalog output file")
                        .required()
                        .build());
    }

    private static void printHelp(Options options) {
        new HelpFormatter().printHelp("Compiler", options, true);
    }
}
