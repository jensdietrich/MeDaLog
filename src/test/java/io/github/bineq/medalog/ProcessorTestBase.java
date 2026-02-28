package io.github.bineq.medalog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base class for annotation processor tests.
 * Provides helpers for loading resource files and comparing output to oracle.
 */
public abstract class ProcessorTestBase {

    /** Loads a classpath resource as a UTF-8 string. */
    protected String loadResource(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Normalises a Souffle program for comparison: collapses whitespace runs,
     * removes blank lines, and trims each line.
     */
    protected String normalise(String text) {
        return text.lines()
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    protected void assertEquivalent(String expected, String actual) {
        assertEquals(normalise(expected), normalise(actual));
    }
}
