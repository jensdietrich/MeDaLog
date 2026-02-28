package io.github.bineq.medalog.id;

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for {@link IdentityAnnotationProcessor}.
 */
class IdentityProcessorProperties {

    private final IdentityAnnotationProcessor proc = new IdentityAnnotationProcessor();

    /**
     * For any declared predicate name and argument list, the output should contain
     * the predicate declaration with id: symbol as the first slot.
     */
    @Property(tries = 200)
    void outputAlwaysContainsIdSlotInDecl(
            @ForAll @AlphaChars @StringLength(min = 1, max = 10) String predName) {
        // Ensure predName starts with a letter
        if (!Character.isLetter(predName.charAt(0))) predName = "p" + predName;
        String input = ".decl " + predName + "(x: symbol)\n";
        String result = proc.process(input);
        assertTrue(result.contains("id: symbol"),
                "Expected 'id: symbol' in output for input: " + input + "\nGot: " + result);
    }

    /**
     * Facts with explicit @[id = "..."] should have that id in the output.
     */
    @Property(tries = 200)
    void explicitFactIdAppearsInOutput(
            @ForAll @AlphaChars @StringLength(min = 1, max = 10) String factId) {
        String input = ".decl p(x: symbol)\n@[id = \"" + factId + "\"]\np(\"a\").\n";
        String result = proc.process(input);
        assertTrue(result.contains("\"" + factId + "\""),
                "Expected fact id '" + factId + "' in output");
    }

    /**
     * Facts without explicit @[id] should get a generated id starting with 'F'.
     */
    @Property(tries = 100)
    void generatedFactIdStartsWithF(
            @ForAll @AlphaChars @StringLength(min = 1, max = 8) String val) {
        String input = ".decl p(x: symbol)\np(\"" + val + "\").\n";
        String result = proc.process(input);
        assertTrue(result.contains("\"F"), "Expected generated fact id starting with F");
    }
}
