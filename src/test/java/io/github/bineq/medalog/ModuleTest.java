package io.github.bineq.medalog;

import org.junit.jupiter.api.Test;

/**
 * Tests for module declaration, nesting, and annotation inheritance.
 *
 * <p>Each test loads a MeDaLog input from
 * {@code src/test/resources/<testname>-input.mdl} and compares the compiled
 * Souffle output against {@code src/test/resources/<testname>-oracle.dl}.
 */
class ModuleTest extends CompilerTestBase {

    @Test
    void topLevelModule() {
        // Fact inside a top-level module gets membership; the module itself has no parent
        assertCompileMatchesOracle("module-top-level");
    }

    @Test
    void nestedModule() {
        // Inner module gets a _module membership fact pointing to its parent
        assertCompileMatchesOracle("module-nested");
    }

    @Test
    void moduleWithRule() {
        // Rule inside a module gets a _module membership fact
        assertCompileMatchesOracle("module-with-rule");
    }

    @Test
    void deeplyNestedModules() {
        // Three-level nesting generates two separate _module membership facts
        assertCompileMatchesOracle("module-deeply-nested");
    }

    @Test
    void moduleWithAnnotations() {
        // Annotations before a module declaration generate _assertedAnnotation facts
        assertCompileMatchesOracle("module-with-annotations");
    }

    @Test
    void moduleMixedContent() {
        // Module containing both a sub-module and a fact: both get separate _module facts
        assertCompileMatchesOracle("module-mixed-content");
    }
}
