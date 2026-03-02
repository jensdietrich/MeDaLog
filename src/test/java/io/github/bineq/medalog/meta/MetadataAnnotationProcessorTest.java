package io.github.bineq.medalog.meta;

import io.github.bineq.medalog.AnnotationProcessorException;
import io.github.bineq.medalog.ProcessorTestBase;
import io.github.bineq.medalog.SouffleFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MetadataAnnotationProcessor}.
 */
class MetadataAnnotationProcessorTest extends ProcessorTestBase {

    private final MetadataAnnotationProcessor proc = new MetadataAnnotationProcessor();

    @Test
    void generatesMetadataComponentForAnnotatedComp() throws IOException {
        String input  = loadResource("meta/comp-simple-input.dl");
        String oracle = loadResource("meta/comp-simple-oracle.dl");
        assertEquivalent(oracle, proc.process(input, Set.of("author", "description")));
    }

    @Test
    void idAsMetadataKeyThrows() throws IOException {
        // Passing 'id' explicitly in the metadata key set should be an error
        String input = loadResource("meta/error-id-as-metadata-input.dl");
        AnnotationProcessorException ex = assertThrows(
                AnnotationProcessorException.class,
                () -> proc.process(input, Set.of("id")));
        assertTrue(ex.getMessage().toLowerCase().contains("id"), ex.getMessage());
    }

    @Test
    void conflictingAnnotationsOnCompThrow() throws IOException {
        // Two annotations with same key but different values on a component → error
        String input = loadResource("meta/error-conflicting-comp-annotations-input.dl");
        AnnotationProcessorException ex = assertThrows(
                AnnotationProcessorException.class,
                () -> proc.process(input, Set.of("author")));
        assertTrue(ex.getMessage().contains("author"), ex.getMessage());
    }

    @Test
    void sameAnnotationValueIsNotConflict() throws IOException {
        String input  = loadResource("meta/same-annotation-value-input.dl");
        String oracle = loadResource("meta/same-annotation-value-oracle.dl");
        assertEquivalent(oracle, proc.process(input, Set.of("author")));
    }

    @Test
    void emptyInputProducesNoMetadataComponent() throws IOException {
        String input  = loadResource("meta/no-annotations-input.dl");
        String oracle = loadResource("meta/no-annotations-oracle.dl");
        assertEquivalent(oracle, proc.process(input, Set.of("author")));
    }

    @Test
    void compWithAnnotationsButNoRuleIdGeneratesMetadataWithoutMember() throws IOException {
        String input  = loadResource("meta/comp-no-id-input.dl");
        String oracle = loadResource("meta/comp-no-id-oracle.dl");
        assertEquivalent(oracle, proc.process(input, Set.of("author", "description")));
    }

    // ── Annotation inheritance and overriding ──────────────────────────────

    @Test
    void nestedCompInheritsAnnotationFromParent() throws IOException {
        String input  = loadResource("meta/comp-nested-inherit-input.dl");
        String oracle = loadResource("meta/comp-nested-inherit-oracle.dl");
        assertEquivalent(oracle, proc.process(input, Set.of("author")));
    }

    @Test
    void nestedCompOverridesParentAnnotation() throws IOException {
        String input  = loadResource("meta/comp-nested-override-input.dl");
        String oracle = loadResource("meta/comp-nested-override-oracle.dl");
        assertEquivalent(oracle, proc.process(input, Set.of("author")));
    }

    @Test
    void nestedCompInheritsOneAnnotationAndOverridesAnother() throws IOException {
        // Outer has author + project; Inner overrides author only — Inner should
        // inherit project at runtime via the _compHierarchy rule.
        String input  = loadResource("meta/comp-partial-override-input.dl");
        String oracle = loadResource("meta/comp-partial-override-oracle.dl");
        assertEquivalent(oracle, proc.process(input, Set.of("author", "project")));
    }

    @Test
    void deeplyNestedCompsGenerateChainedHierarchyFacts() throws IOException {
        // Three levels: Top > Middle > Bottom.
        // Inheritance is one hop from _assertedAnnotation, so Bottom inherits
        // directly from Middle; transitive inheritance from Top is a runtime property.
        String input  = loadResource("meta/comp-deeply-nested-input.dl");
        String oracle = loadResource("meta/comp-deeply-nested-oracle.dl");
        assertEquivalent(oracle, proc.process(input, Set.of("author", "project")));
    }

    @Test
    void ruleInheritsAnnotationFromContainingComp() throws IOException {
        String input  = loadResource("meta/comp-rule-inherits-input.dl");
        String oracle = loadResource("meta/comp-rule-inherits-oracle.dl");
        assertEquivalent(oracle, proc.process(input, Set.of("author")));
    }

    @Test
    void ruleOverridesContainingCompAnnotation() throws IOException {
        String input  = loadResource("meta/comp-rule-overrides-input.dl");
        String oracle = loadResource("meta/comp-rule-overrides-oracle.dl");
        assertEquivalent(oracle, proc.process(input, Set.of("author")));
    }

    // ── Souffle validation (requires Souffle ≥ 2.5 for @[...] annotation support) ──

    @Test
    void outputIsValidSouffle_compWithNoRuleId() throws Exception {
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input  = loadResource("meta/comp-no-id-input.dl");
        String output = proc.process(input, Set.of("author", "description"));
        SouffleFixture.assertValidSouffle(output);
    }

    @Test
    void outputIsValidSouffle_compWithRuleAnnotations() throws Exception {
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input  = loadResource("meta/comp-simple-input.dl");
        String output = proc.process(input, Set.of("author", "description"));
        SouffleFixture.assertValidSouffle(output);
    }

    @Test
    void outputIsValidSouffle_nestedCompInheritance() throws Exception {
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input  = loadResource("meta/comp-nested-inherit-input.dl");
        String output = proc.process(input, Set.of("author"));
        SouffleFixture.assertValidSouffle(output);
    }

    @Test
    void outputIsValidSouffle_nestedCompOverride() throws Exception {
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input  = loadResource("meta/comp-nested-override-input.dl");
        String output = proc.process(input, Set.of("author"));
        SouffleFixture.assertValidSouffle(output);
    }

    @Test
    void outputIsValidSouffle_partialOverride() throws Exception {
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input  = loadResource("meta/comp-partial-override-input.dl");
        String output = proc.process(input, Set.of("author", "project"));
        SouffleFixture.assertValidSouffle(output);
    }

    @Test
    void outputIsValidSouffle_deeplyNested() throws Exception {
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input  = loadResource("meta/comp-deeply-nested-input.dl");
        String output = proc.process(input, Set.of("author", "project"));
        SouffleFixture.assertValidSouffle(output);
    }

    @Test
    void outputIsValidSouffle_ruleInheritsFromComp() throws Exception {
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input  = loadResource("meta/comp-rule-inherits-input.dl");
        String output = proc.process(input, Set.of("author"));
        SouffleFixture.assertValidSouffle(output);
    }

    @Test
    void outputIsValidSouffle_ruleOverridesComp() throws Exception {
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input  = loadResource("meta/comp-rule-overrides-input.dl");
        String output = proc.process(input, Set.of("author"));
        SouffleFixture.assertValidSouffle(output);
    }

    // ── Souffle execution: verify inferred annotation facts ────────────────

    @Test
    void annotationFacts_compSimple() throws Exception {
        // Family has author + description; gp-rule overrides description and inherits author.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String output = proc.process(loadResource("meta/comp-simple-input.dl"), Set.of("author", "description"));
        Set<List<String>> facts = SouffleFixture.runAndCollectAnnotations(output);
        assertEquals(Set.of(
                List.of("Family",  "author",      "jens"),
                List.of("Family",  "description", "family rules"),
                List.of("gp-rule", "description", "grandparent derivation"),
                List.of("gp-rule", "author",      "jens")    // inherited from Family
        ), facts);
    }

    @Test
    void annotationFacts_compNoId() throws Exception {
        // No rule IDs → only the component-level annotations are output.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String output = proc.process(loadResource("meta/comp-no-id-input.dl"), Set.of("author", "description"));
        Set<List<String>> facts = SouffleFixture.runAndCollectAnnotations(output);
        assertEquals(Set.of(
                List.of("Family", "author",      "jens"),
                List.of("Family", "description", "family rules")
        ), facts);
    }

    @Test
    void annotationFacts_nestedCompInherit() throws Exception {
        // Inner has no asserted author → it inherits from Outer.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String output = proc.process(loadResource("meta/comp-nested-inherit-input.dl"), Set.of("author"));
        Set<List<String>> facts = SouffleFixture.runAndCollectAnnotations(output);
        assertEquals(Set.of(
                List.of("Outer", "author", "jens"),
                List.of("Inner", "author", "jens")  // inherited from Outer
        ), facts);
    }

    @Test
    void annotationFacts_nestedCompOverride() throws Exception {
        // Inner asserts a different author → inheritance is suppressed for that key.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String output = proc.process(loadResource("meta/comp-nested-override-input.dl"), Set.of("author"));
        Set<List<String>> facts = SouffleFixture.runAndCollectAnnotations(output);
        assertEquals(Set.of(
                List.of("Outer", "author", "jens"),
                List.of("Inner", "author", "alice")  // own assertion wins
        ), facts);
    }

    @Test
    void annotationFacts_partialOverride() throws Exception {
        // Inner overrides author but not project → project is inherited from Outer.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String output = proc.process(loadResource("meta/comp-partial-override-input.dl"), Set.of("author", "project"));
        Set<List<String>> facts = SouffleFixture.runAndCollectAnnotations(output);
        assertEquals(Set.of(
                List.of("Outer", "author",  "jens"),
                List.of("Outer", "project", "medalog"),
                List.of("Inner", "author",  "alice"),    // overrides Outer
                List.of("Inner", "project", "medalog")   // inherited from Outer
        ), facts);
    }

    @Test
    void annotationFacts_deeplyNested() throws Exception {
        // Hierarchy: Top > Middle > Bottom.
        // Inheritance uses _assertedAnnotation on the direct parent only (one hop).
        // Middle inherits project from Top's asserted fact.
        // Bottom inherits author from Middle's asserted fact.
        // Bottom does NOT inherit project because Middle's project is inferred, not asserted.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String output = proc.process(loadResource("meta/comp-deeply-nested-input.dl"), Set.of("author", "project"));
        Set<List<String>> facts = SouffleFixture.runAndCollectAnnotations(output);
        assertEquals(Set.of(
                List.of("Top",    "project", "medalog"),
                List.of("Middle", "author",  "jens"),
                List.of("Middle", "project", "medalog"),  // inherits from Top (asserted)
                List.of("Bottom", "author",  "jens")      // inherits from Middle (asserted)
                // Bottom does NOT get project: Middle's project is inherited, not asserted
        ), facts);
    }

    @Test
    void annotationFacts_ruleInheritsFromComp() throws Exception {
        // gp-rule has no asserted author → it inherits from Family.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String output = proc.process(loadResource("meta/comp-rule-inherits-input.dl"), Set.of("author"));
        Set<List<String>> facts = SouffleFixture.runAndCollectAnnotations(output);
        assertEquals(Set.of(
                List.of("Family",  "author", "jens"),
                List.of("gp-rule", "author", "jens")  // inherited from Family
        ), facts);
    }

    @Test
    void annotationFacts_ruleOverridesComp() throws Exception {
        // gp-rule asserts a different author → component-level value is not inherited.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String output = proc.process(loadResource("meta/comp-rule-overrides-input.dl"), Set.of("author"));
        Set<List<String>> facts = SouffleFixture.runAndCollectAnnotations(output);
        assertEquals(Set.of(
                List.of("Family",  "author", "jens"),
                List.of("gp-rule", "author", "alice")  // own assertion wins
        ), facts);
    }
}
