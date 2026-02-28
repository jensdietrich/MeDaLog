package io.github.bineq.medalog;

import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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
            Files.writeString(tmp.toPath(), "@[test = \"1\"]\n.decl a(x: symbol)\n");
            Process p = new ProcessBuilder("souffle", "--show=parse-errors", tmp.getAbsolutePath())
                    .redirectErrorStream(true)
                    .start();
            p.getInputStream().readAllBytes(); // drain output
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
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
