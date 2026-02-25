package io.github.bineq.medalog;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property-based (QuickCheck-style) tests using jqwik.
 *
 * <p>Properties verified:
 * <ul>
 *   <li>Predicate names from declarations appear in the compiled output.</li>
 *   <li>String constants in facts appear (perhaps modified) in the output.</li>
 *   <li>The compiler does not crash on well-formed inputs.</li>
 *   <li>Fact IDs provided via @id appear verbatim in the compiled output.</li>
 * </ul>
 */
class CompilerPropertiesTest {

    /**
     * For any valid predicate name and string argument, compiling a simple fact
     * should produce output containing the predicate name and the argument value.
     */
    @Property
    void factArgAppearsInOutput(
            @ForAll("predicateNames") String predName,
            @ForAll("safeStrings") String arg,
            @ForAll("safeStrings") String factId) {

        String source = ".decl " + predName + "(x: symbol)\n"
                + "@id: \"" + factId + "\"\n"
                + predName + "(\"" + arg + "\").\n";

        String out = assertDoesNotThrow(() -> Compiler.compile(source));
        assertTrue(out.contains(predName),
                "Predicate name '" + predName + "' should appear in output");
        assertTrue(out.contains("\"" + arg + "\""),
                "Argument '\"" + arg + "\"' should appear in output");
    }

    /**
     * The @id value provided for a fact must appear verbatim in the compiled fact.
     */
    @Property
    void idAnnotationPreserved(
            @ForAll("predicateNames") String predName,
            @ForAll("safeStrings") String factId) {

        String source = ".decl " + predName + "(x: symbol)\n"
                + "@id: \"" + factId + "\"\n"
                + predName + "(\"v\").\n";

        String out = assertDoesNotThrow(() -> Compiler.compile(source));
        assertTrue(out.contains("\"" + factId + "\""),
                "Fact id '" + factId + "' should appear in output");
    }

    /**
     * A rule with known predicates: the predicate names should appear in the output.
     */
    @Property
    void rulePredicateNamesAppearInOutput(
            @ForAll("predicateNames") String headName,
            @ForAll("predicateNames") String bodyName) {

        // Ensure distinct names to avoid collisions
        if (headName.equals(bodyName)) return;

        String source = ".decl " + headName + "(x: symbol)\n"
                + ".decl " + bodyName + "(x: symbol)\n"
                + headName + "(x) :- " + bodyName + "(x).\n";

        String out = assertDoesNotThrow(() -> Compiler.compile(source));
        assertTrue(out.contains(headName),
                "Head predicate '" + headName + "' should appear in output");
        assertTrue(out.contains(bodyName),
                "Body predicate '" + bodyName + "' should appear in output");
    }

    /**
     * Compiling any program with a single simple declaration should not throw.
     */
    @Property
    void singleDeclarationCompilesSafely(
            @ForAll("predicateNames") String predName,
            @ForAll("predicateNames") String paramName) {

        if (predName.equals(paramName)) return;

        String source = ".decl " + predName + "(" + paramName + ": symbol)\n";
        assertDoesNotThrow(() -> Compiler.compile(source));
    }

    /**
     * Module names appear in the _module predicate output.
     */
    @Property
    void moduleNameAppearsInModuleFacts(
            @ForAll("moduleNames") String moduleName) {

        String source = ".decl foo(x: symbol)\n"
                + "module " + moduleName + " {\n"
                + "  @id: \"factin" + moduleName + "\"\n"
                + "  foo(\"a\").\n"
                + "}\n";

        String out = assertDoesNotThrow(() -> Compiler.compile(source));
        assertTrue(out.contains("\"" + moduleName + "\""),
                "Module name '" + moduleName + "' should appear in output");
    }

    // ==============================
    // Arbitraries
    // ==============================

    @Provide
    Arbitrary<String> predicateNames() {
        return Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyz")
                .ofMinLength(2)
                .ofMaxLength(10)
                .filter(s -> isValidIdentifier(s) && !isSouffleKeyword(s));
    }

    @Provide
    Arbitrary<String> moduleNames() {
        return Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyz")
                .ofMinLength(2)
                .ofMaxLength(8)
                .filter(s -> isValidIdentifier(s) && !isSouffleKeyword(s));
    }

    @Provide
    Arbitrary<String> safeStrings() {
        // Only safe characters: no quotes, backslashes, or control chars
        return Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_")
                .ofMinLength(1)
                .ofMaxLength(20);
    }

    private boolean isValidIdentifier(String s) {
        if (s == null || s.isEmpty()) return false;
        if (!Character.isLetter(s.charAt(0))) return false;
        return s.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_');
    }

    private boolean isSouffleKeyword(String s) {
        return switch (s) {
            case "true", "false", "null", "module", "as", "inline",
                 "stateful", "land", "lor", "lxor", "band", "bor",
                 "bxor", "bshr", "bshl", "bshru", "bnot" -> true;
            default -> false;
        };
    }
}
