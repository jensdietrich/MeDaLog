package io.github.bineq.medalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for compiler error conditions:
 * <ul>
 *   <li>Duplicate @id values</li>
 *   <li>Conflicting annotations (same key, different value)</li>
 *   <li>Syntax errors</li>
 * </ul>
 */
class CompilerErrorTest {

    @Test
    void duplicateIdThrowsCompilerException() {
        String source = ".decl parent(x: symbol, y: symbol)\n"
                + "@id: \"dup\"\n"
                + "parent(\"a\", \"b\").\n"
                + "@id: \"dup\"\n"
                + "parent(\"c\", \"d\").\n";
        CompilerException ex = assertThrows(CompilerException.class,
                () -> Compiler.compile(source));
        assertTrue(ex.getMessage().contains("dup"),
                "Exception should mention the duplicate id, got: " + ex.getMessage());
    }

    @Test
    void duplicateModuleIdThrowsCompilerException() {
        String source = "module m1 {}\nmodule m1 {}\n";
        assertThrows(CompilerException.class, () -> Compiler.compile(source),
                "Expected CompilerException for duplicate module id 'm1'");
    }

    @Test
    void conflictingAnnotationsSameKeyDifferentValue() {
        String source = ".decl foo(x: symbol)\n"
                + "@author: \"Alice\"\n"
                + "@author: \"Bob\"\n"
                + "@id: \"x1\"\n"
                + "foo(\"a\").\n";
        CompilerException ex = assertThrows(CompilerException.class,
                () -> Compiler.compile(source));
        assertTrue(ex.getMessage().contains("author"),
                "Exception should mention conflicting key, got: " + ex.getMessage());
    }

    @Test
    void syntaxErrorThrowsCompilerException() {
        String source = ".decl foo(x: symbol)\n"
                + "foo(\"a\")  // missing period\n";
        assertThrows(CompilerException.class, () -> Compiler.compile(source),
                "Expected CompilerException for syntax error");
    }

    @Test
    void compilerExceptionIncludesLineNumber() {
        String source = ".decl foo(x: symbol)\n"
                + "@id: \"a\"\n"
                + "foo(\"x\").\n"
                + "@id: \"a\"\n"           // duplicate on line 4
                + "foo(\"y\").\n";
        CompilerException ex = assertThrows(CompilerException.class,
                () -> Compiler.compile(source));
        assertTrue(ex.getLineNumber() > 0,
                "Expected positive line number, got: " + ex.getLineNumber());
    }

    @Test
    void duplicateSameAnnotationValueIsAllowed() {
        // Same key AND same value: duplicate but no conflict – should compile OK
        String source = ".decl foo(x: symbol)\n"
                + "@author: \"Alice\"\n"
                + "@author: \"Alice\"\n"
                + "@id: \"x2\"\n"
                + "foo(\"a\").\n";
        // Should not throw
        String out = Compiler.compile(source);
        assertTrue(out.contains("_assertedAnnotation"),
                "Expected output with annotation fact, got:\n" + out);
    }
}
