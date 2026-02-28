package io.github.bineq.medalog;

import io.github.bineq.medalog.id.IdentityAnnotationProcessor;
import io.github.bineq.medalog.meta.MetadataAnnotationProcessor;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Command-line entry point for the MeDaLog annotation processors.
 *
 * <pre>
 * Usage: medalogc -processor &lt;id|metadata|all&gt; -input &lt;file&gt; -output &lt;file&gt; [-metadata &lt;keys&gt;]
 *
 *   -processor   Which processor to run: id, metadata, or all
 *   -input       Input Souffle program file
 *   -output      Output Souffle program file
 *   -metadata    Comma-separated list of metadata annotation keys (used by metadata and all)
 * </pre>
 */
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

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

        if (!cmd.hasOption("processor") || !cmd.hasOption("input") || !cmd.hasOption("output")) {
            printHelp(options);
            System.exit(1);
            return;
        }

        String processor = cmd.getOptionValue("processor").toLowerCase();
        File input = new File(cmd.getOptionValue("input"));
        File output = new File(cmd.getOptionValue("output"));
        Set<String> metadataKeys = parseMetadataKeys(cmd.getOptionValue("metadata"));

        if (!input.exists()) {
            System.err.println("Error: input file not found: " + input);
            System.exit(1);
            return;
        }

        try {
            switch (processor) {
                case "id" -> {
                    LOG.info("Running identity annotation processor: {} -> {}", input, output);
                    new IdentityAnnotationProcessor().process(input, output);
                }
                case "metadata" -> {
                    LOG.info("Running metadata annotation processor: {} -> {}", input, output);
                    new MetadataAnnotationProcessor().process(input, output, metadataKeys);
                }
                case "all" -> {
                    // Run identity first, then metadata on the result
                    LOG.info("Running all annotation processors: {} -> {}", input, output);
                    File intermediate = File.createTempFile("medalog-id-", ".dl");
                    intermediate.deleteOnExit();
                    new IdentityAnnotationProcessor().process(input, intermediate);
                    new MetadataAnnotationProcessor().process(intermediate, output, metadataKeys);
                }
                default -> {
                    System.err.println("Error: unknown processor '" + processor +
                            "'. Use one of: id, metadata, all");
                    System.exit(1);
                }
            }
            LOG.info("Done. Output written to {}", output);
        } catch (AnnotationProcessorException e) {
            System.err.println("Annotation processing error: " + e.getMessage());
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            LOG.error("Unexpected error", e);
            System.exit(3);
        }
    }

    private static Options buildOptions() {
        Options opts = new Options();
        opts.addRequiredOption("p", "processor", true,
                "Annotation processor to run: id | metadata | all");
        opts.addRequiredOption("i", "input", true,
                "Input Souffle program file");
        opts.addRequiredOption("o", "output", true,
                "Output Souffle program file");
        opts.addOption("m", "metadata", true,
                "Comma-separated metadata annotation keys (e.g. author,description,created)");
        return opts;
    }

    private static void printHelp(Options options) {
        new HelpFormatter().printHelp("medalogc", options, true);
    }

    private static Set<String> parseMetadataKeys(String csv) {
        Set<String> keys = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) return keys;
        for (String k : csv.split(",")) {
            String trimmed = k.trim();
            if (!trimmed.isEmpty()) keys.add(trimmed);
        }
        return keys;
    }
}
