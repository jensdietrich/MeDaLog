package io.github.bineq.medalog;

import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper that runs the {@code souffle} binary to validate Souffle programs.
 *
 * <p>Tests that need Souffle validation should call {@link #assumeSouffleAvailable()}
 * at the start; this skips the test if {@code souffle} is not on the PATH.
 */
public final class SouffleFixture {

    private SouffleFixture() {}

    /** Skip the current test if {@code souffle} is not on the PATH. */
    public static void assumeSouffleAvailable() {
        Assumptions.assumeTrue(souffleOnPath(),
                "souffle binary not found on PATH — skipping Souffle validation test");
    }

    /**
     * Skip the current test if the installed Souffle does not support {@code @[...]}
     * annotation syntax (requires Souffle ≥ 2.5).
     */
    public static void assumeSouffleSupportsAnnotations() {
        assumeSouffleAvailable();
        Assumptions.assumeTrue(souffleSupportsAnnotations(),
                "souffle does not support @[...] annotation syntax (requires ≥ 2.5) — skipping");
    }

    /**
     * Returns true if {@code souffle} is available on the PATH.
     */
    public static boolean souffleOnPath() {
        try {
            Process p = new ProcessBuilder("souffle", "--version")
                    .redirectErrorStream(true)
                    .start();
            p.waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if the available Souffle binary accepts {@code @[...]} annotation syntax.
     */
    public static boolean souffleSupportsAnnotations() {
        try {
            File tmp = File.createTempFile("medalog-annot-probe-", ".dl");
            tmp.deleteOnExit();
            Files.writeString(tmp.toPath(), ".decl a(x: symbol)\n" + "@[test = \"1\"]\n" + "a(\"foo\").");


            Process p = new ProcessBuilder("souffle", "--show=parse-errors", tmp.getAbsolutePath())
                    .redirectErrorStream(true)
                    .start();
            String out = new String(p.getInputStream().readAllBytes()); // drain output
            System.out.println(out);
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Runs Souffle on {@code source}, executes the program, and returns the facts in the
     * {@code metadata.annotation} output relation as a set of
     * {@code [id, annotation_name, annotation_value]} triples.
     *
     * <p>The caller must call {@link #assumeSouffleAvailable()} or
     * {@link #assumeSouffleSupportsAnnotations()} before invoking this method if needed.
     *
     * @throws AssertionError if Souffle exits with a non-zero status
     */
    public static Set<List<String>> runAndCollectAnnotations(String source)
            throws IOException, InterruptedException {
        Path outDir  = Files.createTempDirectory("medalog-out-");
        Path srcFile = Files.createTempFile("medalog-prog-", ".dl");
        Files.writeString(srcFile, source);
        Process p = new ProcessBuilder(
                "souffle", "-D", outDir.toAbsolutePath().toString(),
                srcFile.toAbsolutePath().toString())
                .redirectErrorStream(true)
                .start();
        String souffleOutput = new String(p.getInputStream().readAllBytes());
        int exit = p.waitFor();
        if (exit != 0) {
            throw new AssertionError("Souffle exited with status " + exit + ":\n" + souffleOutput);
        }
        Path csvFile = outDir.resolve("metadata.annotation.csv");
        if (!Files.exists(csvFile)) return Set.of();
        Set<List<String>> result = new HashSet<>();
        for (String line : Files.readAllLines(csvFile)) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\t", -1);
            result.add(List.of(parts[0], parts[1], parts[2]));
        }
        return result;
    }

    /**
     * Validates that the given Souffle source is a syntactically valid Souffle program
     * by writing it to a temp file and running {@code souffle --parse-errors-only}.
     *
     * @throws AssertionError if souffle reports errors
     */
    public static void assertValidSouffle(String source) throws IOException, InterruptedException {
        assumeSouffleAvailable();
        File tmp = File.createTempFile("medalog-validate-", ".dl");
        tmp.deleteOnExit();
        Files.writeString(tmp.toPath(), source);
        Process p = new ProcessBuilder("souffle", "--show=parse-errors", tmp.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes());
        int exit = p.waitFor();
        if (exit != 0) {
            throw new AssertionError("Output is not valid Souffle:\n" + output);
        }
    }
}
