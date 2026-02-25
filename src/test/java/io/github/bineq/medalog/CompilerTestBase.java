package io.github.bineq.medalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Base class for MeDaLog compiler tests.
 *
 * <p>Provides helpers to load test resources, compile MeDaLog inputs, and compare
 * results with oracle files using a whitespace- and comment-insensitive comparison.
 *
 * <h2>Resource file naming convention</h2>
 * <ul>
 *   <li>{@code <testname>-input.mdl} – MeDaLog source to compile</li>
 *   <li>{@code <testname>-oracle.dl} – expected Souffle Datalog output</li>
 * </ul>
 *
 * <p>All resource files are resolved from the classpath root (i.e. from
 * {@code src/test/resources/}).
 */
public abstract class CompilerTestBase {

    // ==============================
    // Core assertion
    // ==============================

    /**
     * Compiles the content of {@code <testname>-input.mdl} and asserts that the
     * result is equivalent to the content of {@code <testname>-oracle.dl}.
     *
     * <p>Equivalence is determined after stripping all comments and collapsing
     * whitespace in both strings (see {@link #normalise(String)}).
     *
     * @param testName the base name shared by the input and oracle files
     */
    protected void assertCompileMatchesOracle(String testName) {
        String actual   = compileResource(testName + "-input.mdl");
        String oracle   = loadResource(testName + "-oracle.dl");
        assertEquivalent(actual, oracle,
                "Compiler output for '" + testName + "' does not match oracle");
    }

    /**
     * Asserts that {@code actual} and {@code oracle} are equivalent after
     * stripping {@code // …} and {@code /* … *}{@code /} comments and collapsing
     * all runs of whitespace (spaces, tabs, newlines) to a single space.
     *
     * @param actual  the compiler output
     * @param oracle  the expected output (may include human-readable comments)
     * @param message assertion failure message
     */
    protected static void assertEquivalent(String actual, String oracle, String message) {
        assertEquals(normalise(oracle), normalise(actual), message);
    }

    /** Overload without a custom failure message. */
    protected static void assertEquivalent(String actual, String oracle) {
        assertEquivalent(actual, oracle, "Compiler output does not match oracle");
    }

    // ==============================
    // Resource helpers
    // ==============================

    /**
     * Compiles the MeDaLog source from the given resource file and returns
     * the Souffle Datalog output as a string.
     *
     * @param resourceName classpath-relative resource name, e.g. {@code "fact-with-id-input.mdl"}
     * @return compiled Souffle source
     * @throws CompilerException on compiler errors
     */
    protected static String compileResource(String resourceName) {
        return Compiler.compile(loadResource(resourceName));
    }

    /**
     * Loads the content of a classpath resource as a UTF-8 string.
     *
     * @param resourceName classpath-relative resource name
     * @return file content
     * @throws IllegalArgumentException if the resource cannot be found
     */
    protected static String loadResource(String resourceName) {
        try (InputStream is = CompilerTestBase.class.getClassLoader()
                .getResourceAsStream(resourceName)) {
            assertNotNull(is, "Test resource not found: " + resourceName);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read resource: " + resourceName, e);
        }
    }

    // ==============================
    // Normalisation
    // ==============================

    /**
     * Normalises a Souffle source string for comparison:
     * <ol>
     *   <li>Removes block comments ({@code /* … *}{@code /})</li>
     *   <li>Removes line comments ({@code // …})</li>
     *   <li>Collapses all whitespace runs to a single space</li>
     *   <li>Trims leading/trailing whitespace</li>
     * </ol>
     */
    static String normalise(String s) {
        // Remove block comments (non-greedy, dot-all)
        s = s.replaceAll("(?s)/\\*.*?\\*/", " ");
        // Remove line comments
        s = s.replaceAll("//[^\n]*", " ");
        // Collapse whitespace
        s = s.replaceAll("[ \\t\\r\\n]+", " ").trim();
        return s;
    }
}
