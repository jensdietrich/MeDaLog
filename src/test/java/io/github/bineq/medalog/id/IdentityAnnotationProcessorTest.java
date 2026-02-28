package io.github.bineq.medalog.id;

import io.github.bineq.medalog.AnnotationProcessorException;
import io.github.bineq.medalog.ProcessorTestBase;
import io.github.bineq.medalog.SouffleFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IdentityAnnotationProcessor}.
 */
class IdentityAnnotationProcessorTest extends ProcessorTestBase {

    private final IdentityAnnotationProcessor proc = new IdentityAnnotationProcessor();

    // ── Declaration tests ──────────────────────────────────────────────────

    @Test
    void addsIdSlotToDecl() throws IOException {
        String input  = loadResource("id/decl-simple-input.dl");
        String oracle = loadResource("id/decl-simple-oracle.dl");
        assertEquivalent(oracle, proc.process(input));
    }

    @Test
    void doesNotDuplicateIdSlotWhenAlreadyPresent() throws IOException {
        String input  = loadResource("id/decl-already-has-id-input.dl");
        String oracle = loadResource("id/decl-already-has-id-oracle.dl");
        assertEquivalent(oracle, proc.process(input));
    }

    @Test
    void errorWhenIdSlotIsNotFirst() throws IOException {
        String input = loadResource("id/error-id-not-first-input.dl");
        AnnotationProcessorException ex = assertThrows(
                AnnotationProcessorException.class, () -> proc.process(input));
        assertTrue(ex.getMessage().contains("first"), ex.getMessage());
        assertTrue(ex.getLineNumber() > 0);
    }

    // ── Fact tests ─────────────────────────────────────────────────────────

    @Test
    void prependsAnnotatedIdToFact() throws IOException {
        String input  = loadResource("id/fact-with-id-input.dl");
        String oracle = loadResource("id/fact-with-id-oracle.dl");
        assertEquivalent(oracle, proc.process(input));
    }

    @Test
    void prependsGeneratedIdToFactWithoutAnnotation() throws IOException {
        String input  = loadResource("id/fact-without-id-input.dl");
        String result = proc.process(input);
        assertTrue(result.contains("parent(\"F"), "Expected generated fact id starting with F");
        assertFalse(result.contains("@["), "Annotation should be consumed");
    }

    @Test
    void duplicateIdAnnotationThrows() throws IOException {
        String input = loadResource("id/error-duplicate-id-input.dl");
        AnnotationProcessorException ex = assertThrows(
                AnnotationProcessorException.class, () -> proc.process(input));
        assertTrue(ex.getMessage().contains("dup"), ex.getMessage());
    }

    // ── Rule tests ─────────────────────────────────────────────────────────

    @Test
    void injectsIdVariablesAndAggregationIntoRule() throws IOException {
        String input  = loadResource("id/rule-with-id-input.dl");
        String oracle = loadResource("id/rule-with-id-oracle.dl");
        assertEquivalent(oracle, proc.process(input));
    }

    @Test
    void handlesNegatedAtomInRule() throws IOException {
        String input  = loadResource("id/rule-negated-atom-input.dl");
        String oracle = loadResource("id/rule-negated-atom-oracle.dl");
        assertEquivalent(oracle, proc.process(input));
    }

    @Test
    void passesUndeclaredPredicateThrough() throws IOException {
        String input  = loadResource("id/undeclared-passthrough-input.dl");
        String oracle = loadResource("id/undeclared-passthrough-oracle.dl");
        assertEquivalent(oracle, proc.process(input));
    }

    @Test
    void generatesUniqueRuleIdsForUnlabelledRules() throws IOException {
        String input  = loadResource("id/auto-rule-id-input.dl");
        String oracle = loadResource("id/auto-rule-id-oracle.dl");
        assertEquivalent(oracle, proc.process(input));
    }

    // ── Souffle validation ─────────────────────────────────────────────────

    @Test
    void outputIsValidSouffle_simpleFact() throws Exception {
        SouffleFixture.assumeSouffleAvailable();
        String input  = loadResource("id/fact-with-id-input.dl");
        String output = proc.process(input);
        SouffleFixture.assertValidSouffle(output);
    }

    @Test
    void outputIsValidSouffle_ruleWithId() throws Exception {
        SouffleFixture.assumeSouffleAvailable();
        String input  = loadResource("id/rule-with-id-input.dl");
        String output = proc.process(input);
        SouffleFixture.assertValidSouffle(output);
    }
}
