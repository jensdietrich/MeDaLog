package io.github.bineq.medalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for core compiler transformations: predicate declarations, facts, and rules.
 *
 * <p>Each test loads a MeDaLog input from
 * {@code src/test/resources/<testname>-input.mdl} and compares the compiled
 * Souffle output against {@code src/test/resources/<testname>-oracle.dl} using
 * the whitespace- and comment-insensitive {@link CompilerTestBase#assertEquivalent}.
 */
class CompilerTest extends CompilerTestBase {

    // ==============================
    // Predicate declarations
    // ==============================

    @Test
    void declarationWithParams() {
        assertCompileMatchesOracle("declaration-with-params");
    }

    @Test
    void declarationOldSyntax() {
        // Old Souffle "name type" syntax is normalised to "name: type" with id slot prepended
        assertCompileMatchesOracle("declaration-old-syntax");
    }

    @Test
    void declarationExistingIdSlot() {
        // Predicate that already has "id symbol" as first param must not get an extra id slot
        assertCompileMatchesOracle("declaration-existing-id-slot");
    }

    @Test
    void declarationEmpty() {
        // Zero-arity predicate should become (id: symbol)
        assertCompileMatchesOracle("declaration-empty");
    }

    // ==============================
    // Facts
    // ==============================

    @Test
    void factWithIdAnnotation() {
        // @id annotation provides the fact id verbatim
        assertCompileMatchesOracle("fact-with-id");
    }

    @Test
    void factWithoutId() {
        // Missing @id: compiler assigns the auto-generated id "F1"
        assertCompileMatchesOracle("fact-without-id");
    }

    @Test
    void factUnknownPredicate() {
        // Undeclared predicates are passed through to Souffle unchanged
        assertCompileMatchesOracle("fact-unknown-predicate");
    }

    // ==============================
    // Rules
    // ==============================

    @Test
    void ruleWithId() {
        // @id on a rule: body atoms get fresh id vars; head uses cat-aggregated id
        assertCompileMatchesOracle("rule-with-id");
    }

    @Test
    void ruleNegatedAtom() {
        // Negated body atom gets a wildcard _ for the id slot
        assertCompileMatchesOracle("rule-negated-atom");
    }

    @Test
    void ruleComparison() {
        // Comparison constraints are passed through verbatim; only predicate atoms get id vars
        assertCompileMatchesOracle("rule-comparison");
    }
}
