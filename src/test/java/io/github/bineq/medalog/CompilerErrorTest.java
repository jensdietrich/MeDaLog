package io.github.bineq.medalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for compiler error conditions:
 * <ul>
 *   <li>Duplicate {@code @id} values</li>
 *   <li>Duplicate module identifiers</li>
 *   <li>Conflicting annotations (same key, different value)</li>
 *   <li>Syntax errors</li>
 * </ul>
 *
 * <p>Each test loads a MeDaLog input from
 * {@code src/test/resources/<testname>-input.mdl}. Error tests have no oracle
 * file since the expected outcome is a thrown {@link CompilerException}.
 */
class CompilerErrorTest extends CompilerTestBase {

    @Test
    void duplicateIdThrowsCompilerException() {
        CompilerException ex = assertThrows(CompilerException.class,
                () -> compileResource("error-duplicate-id-input.mdl"));
        assertTrue(ex.getMessage().contains("dup"),
                "Exception should mention the duplicate id, got: " + ex.getMessage());
    }

    @Test
    void duplicateModuleIdThrowsCompilerException() {
        assertThrows(CompilerException.class,
                () -> compileResource("error-duplicate-module-id-input.mdl"),
                "Expected CompilerException for duplicate module id 'm1'");
    }

    @Test
    void conflictingAnnotationsSameKeyDifferentValueThrowsCompilerException() {
        CompilerException ex = assertThrows(CompilerException.class,
                () -> compileResource("error-conflicting-annotations-input.mdl"));
        assertTrue(ex.getMessage().contains("author"),
                "Exception should mention conflicting key, got: " + ex.getMessage());
    }

    @Test
    void syntaxErrorThrowsCompilerException() {
        assertThrows(CompilerException.class,
                () -> compileResource("error-syntax-input.mdl"),
                "Expected CompilerException for syntax error");
    }

    @Test
    void compilerExceptionIncludesLineNumber() {
        // error-duplicate-id-input.mdl has a duplicate on line 4
        CompilerException ex = assertThrows(CompilerException.class,
                () -> compileResource("error-duplicate-id-input.mdl"));
        assertTrue(ex.getLineNumber() > 0,
                "Expected positive line number, got: " + ex.getLineNumber());
    }
}
