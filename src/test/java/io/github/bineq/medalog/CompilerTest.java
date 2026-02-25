package io.github.bineq.medalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for basic compiler functionality: declaration rewriting, fact/rule
 * transformation, and module membership generation.
 */
class CompilerTest {

    // ==============================
    // Declaration tests
    // ==============================

    @Test
    void declarationGetsIdSlot() {
        String source = ".decl parent(x: symbol, y: symbol)\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("parent(id: symbol, x: symbol, y: symbol)"),
                "Expected id slot in compiled declaration, got:\n" + out);
    }

    @Test
    void declarationOldSyntaxGetsIdSlot() {
        String source = ".decl parent(x symbol, y symbol)\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("parent(id: symbol, x: symbol, y: symbol)"),
                "Expected id slot in old-syntax declaration, got:\n" + out);
    }

    @Test
    void declarationAlreadyHasIdSlotIsUnchanged() {
        // Predicate already declares id symbol as first param; must not add another
        String source = ".decl parent(id symbol, x: symbol, y: symbol)\n";
        String out = Compiler.compile(source);
        // Should have exactly one 'id: symbol'
        long count = countOccurrences(out, "id: symbol");
        assertTrue(count == 1, "Expected exactly one id slot, got:\n" + out);
    }

    @Test
    void emptyDeclarationGetsIdSlot() {
        String source = ".decl marker()\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("marker(id: symbol)"),
                "Expected id slot in empty declaration, got:\n" + out);
    }

    // ==============================
    // Fact tests
    // ==============================

    @Test
    void factWithAtIdAnnotation() {
        String source = ".decl parent(x: symbol, y: symbol)\n"
                + "@id: \"fact-1\"\n"
                + "parent(\"Tim\", \"Tom\").\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("parent(\"fact-1\", \"Tim\", \"Tom\")."),
                "Expected annotated id in fact, got:\n" + out);
    }

    @Test
    void factWithoutIdGetsGeneratedId() {
        String source = ".decl parent(x: symbol, y: symbol)\n"
                + "parent(\"Tim\", \"Tom\").\n";
        String out = Compiler.compile(source);
        // Should contain parent( with a quoted id, the original args
        assertTrue(out.matches("(?s).*parent\\(\"[^\"]+\",\\s*\"Tim\",\\s*\"Tom\"\\)\\..*"),
                "Expected generated id in fact, got:\n" + out);
    }

    @Test
    void unknownPredicateFactIsPassedThrough() {
        String source = "undeclared(\"x\").\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("undeclared(\"x\")."),
                "Expected pass-through for undeclared predicate, got:\n" + out);
    }

    // ==============================
    // Rule tests
    // ==============================

    @Test
    void ruleBodyPredicatesGetIdVariables() {
        String source = ".decl grandparent(gp: symbol, gc: symbol)\n"
                + ".decl parent(p: symbol, c: symbol)\n"
                + "grandparent(x, y) :- parent(x, z), parent(z, y).\n";
        String out = Compiler.compile(source);
        // Body should have parent(_id1, x, z) and parent(_id2, z, y)
        assertTrue(out.contains("parent(_id1,"), "Expected _id1 in body, got:\n" + out);
        assertTrue(out.contains("parent(_id2,"), "Expected _id2 in body, got:\n" + out);
    }

    @Test
    void ruleHeadHasAggregatedId() {
        String source = ".decl grandparent(gp: symbol, gc: symbol)\n"
                + ".decl parent(p: symbol, c: symbol)\n"
                + "@id: \"gprule\"\n"
                + "grandparent(x, y) :- parent(x, z), parent(z, y).\n";
        String out = Compiler.compile(source);
        // Head should start with cat aggregation
        assertTrue(out.contains("cat(\"gprule\""),
                "Expected cat aggregation in head, got:\n" + out);
    }

    @Test
    void ruleWithoutIdGetsGeneratedRuleId() {
        String source = ".decl gp(a: symbol, b: symbol)\n"
                + ".decl parent(p: symbol, c: symbol)\n"
                + "gp(x, y) :- parent(x, z), parent(z, y).\n";
        String out = Compiler.compile(source);
        // There should be some cat(...) expression in the head
        assertTrue(out.contains("cat("), "Expected cat aggregation in head, got:\n" + out);
    }

    @Test
    void negatedAtomInBodyGetsWildcard() {
        String source = ".decl active(x: symbol)\n"
                + ".decl removed(x: symbol)\n"
                + ".decl result(x: symbol)\n"
                + "result(x) :- active(x), !removed(x).\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("!removed(_,"), "Expected wildcard in negated atom, got:\n" + out);
    }

    @Test
    void comparisonInBodyIsPassedThrough() {
        String source = ".decl age(name: symbol, years: number)\n"
                + ".decl adult(name: symbol)\n"
                + "adult(n) :- age(n, a), a >= 18.\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("a >= 18"), "Expected comparison pass-through, got:\n" + out);
    }

    // ==============================
    // Module tests
    // ==============================

    @Test
    void moduleGeneratesModuleMembershipFact() {
        String source = ".decl parent(x: symbol, y: symbol)\n"
                + "module family {\n"
                + "  @id: \"r1\"\n"
                + "  parent(\"a\", \"b\").\n"
                + "}\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_module(\"r1\", \"family\")"),
                "Expected _module fact for rule in module, got:\n" + out);
    }

    @Test
    void nestedModuleGeneratesModuleMembership() {
        String source = "module outer {\n"
                + "  module inner {\n"
                + "  }\n"
                + "}\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_module(\"inner\", \"outer\")"),
                "Expected _module fact for nested module, got:\n" + out);
    }

    // ==============================
    // Annotation tests
    // ==============================

    @Test
    void annotationGeneratesAssertedAnnotationFact() {
        String source = ".decl parent(x: symbol, y: symbol)\n"
                + "@id: \"f1\"\n"
                + "@author: \"Alice\"\n"
                + "parent(\"a\", \"b\").\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_assertedAnnotation(\"f1\", \"author\", \"Alice\")"),
                "Expected _assertedAnnotation fact, got:\n" + out);
    }

    @Test
    void builtinDeclarationsPresent() {
        String out = Compiler.compile(".decl foo(x: symbol)\n");
        assertTrue(out.contains(".decl _module("), "Expected _module declaration, got:\n" + out);
        assertTrue(out.contains(".decl _annotation("), "Expected _annotation declaration, got:\n" + out);
        assertTrue(out.contains(".decl _assertedAnnotation("),
                "Expected _assertedAnnotation declaration, got:\n" + out);
    }

    @Test
    void annotationInheritanceRulePresent() {
        String out = Compiler.compile(".decl foo(x: symbol)\n");
        assertTrue(out.contains("_annotation(id, key, val) :-"),
                "Expected inheritance rule, got:\n" + out);
        assertTrue(out.contains("_module(id, modId)"),
                "Expected _module in inheritance rule, got:\n" + out);
    }

    // ==============================
    // Helper
    // ==============================

    private long countOccurrences(String text, String sub) {
        long count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
