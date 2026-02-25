package io.github.bineq.medalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for module declaration, nesting, and annotation inheritance.
 */
class ModuleTest {

    @Test
    void topLevelModuleHasNoModuleMembership() {
        String source = "module standalone {}\n";
        String out = Compiler.compile(source);
        // standalone is not inside any module, so no _module fact should mention it as member
        assertFalse(out.contains("_module(\"standalone\","),
                "Top-level module should have no _module membership fact, got:\n" + out);
    }

    @Test
    void innerModuleHasModuleMembership() {
        String source = "module outer {\n  module inner {}\n}\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_module(\"inner\", \"outer\")"),
                "Expected inner module membership, got:\n" + out);
    }

    @Test
    void ruleInsideModuleHasModuleMembership() {
        String source = ".decl parent(x: symbol, y: symbol)\n"
                + "module fam {\n"
                + "  @id: \"r1\"\n"
                + "  parent(\"a\", \"b\").\n"
                + "}\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_module(\"r1\", \"fam\")"),
                "Expected rule membership in module, got:\n" + out);
    }

    @Test
    void moduleAnnotationsGenerateFacts() {
        String source = "@author: \"Team\"\n"
                + "@version: 2\n"
                + "module mymod {}\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_assertedAnnotation(\"mymod\", \"author\", \"Team\")"),
                "Expected author annotation on module, got:\n" + out);
        assertTrue(out.contains("_assertedAnnotation(\"mymod\", \"version\", \"2\")"),
                "Expected version annotation on module, got:\n" + out);
    }

    @Test
    void annotationInheritanceRuleGenerated() {
        String out = Compiler.compile("module m {}\n");
        assertTrue(out.contains("!_annotationKeyAsserted(id, key)"),
                "Expected negation in inheritance rule, got:\n" + out);
    }

    @Test
    void deeplyNestedModuleChain() {
        String source = "module a {\n  module b {\n    module c {}\n  }\n}\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_module(\"b\", \"a\")"), "Expected b in a, got:\n" + out);
        assertTrue(out.contains("_module(\"c\", \"b\")"), "Expected c in b, got:\n" + out);
    }

    @Test
    void ruleAndModuleInSameParentModule() {
        String source = ".decl foo(x: symbol)\n"
                + "module parent {\n"
                + "  module child {}\n"
                + "  @id: \"f1\"\n"
                + "  foo(\"x\").\n"
                + "}\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_module(\"child\", \"parent\")"),
                "Expected child in parent, got:\n" + out);
        assertTrue(out.contains("_module(\"f1\", \"parent\")"),
                "Expected fact in parent, got:\n" + out);
    }
}
