package io.github.bineq.medalog.meta;

import io.github.bineq.medalog.AnnotationProcessorException;
import io.github.bineq.medalog.ProcessorTestBase;
import io.github.bineq.medalog.SouffleFixture;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MetadataAnnotationProcessor}.
 */
class MetadataAnnotationProcessorTest extends ProcessorTestBase {

    private final MetadataAnnotationProcessor proc = new MetadataAnnotationProcessor();

    // ── Error tests ────────────────────────────────────────────────────────

    @Test
    void idAsMetadataKeyThrows() throws Exception {
        String input = loadResource("meta/error-id-as-metadata-input.dl");
        SouffleFixture.assumeValidSouffle(input);
        AnnotationProcessorException ex = assertThrows(
                AnnotationProcessorException.class,
                () -> proc.process(input, Set.of("id")));
        assertTrue(ex.getMessage().toLowerCase().contains("id"), ex.getMessage());
    }

    @Test
    void conflictingAnnotationsOnCompThrow() throws Exception {
        String input = loadResource("meta/error-conflicting-comp-annotations-input.dl");
        SouffleFixture.assumeValidSouffle(input);
        AnnotationProcessorException ex = assertThrows(
                AnnotationProcessorException.class,
                () -> proc.process(input, Set.of("author")));
        assertTrue(ex.getMessage().contains("author"), ex.getMessage());
    }

    // ── Annotation fact tests (run Souffle, check inferred annotation relation) ──

    @Test
    void annotationFacts_noAnnotations() throws Exception {
        // No annotations in source → no metadata component → empty annotation relation.
        SouffleFixture.assumeSouffleAvailable();
        String input = loadResource("meta/no-annotations-input.dl");
        SouffleFixture.assumeValidSouffle(input);
        String output = proc.process(input, Set.of("author"));
        SouffleFixture.assertValidSouffle(output);
        assertEquals(Set.of(), SouffleFixture.runAndCollectAnnotations(output));
    }

    @Test
    void annotationFacts_sameAnnotationValueIsNotConflict() throws Exception {
        // Two @[author = "alice"] on the same bare rule (no comp, no id) → no conflict,
        // no metadata generated (no comp or rule id to attach to) → empty annotation relation.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input = loadResource("meta/same-annotation-value-input.dl");
        SouffleFixture.assumeValidSouffle(input);
        String output = proc.process(input, Set.of("author"));
        SouffleFixture.assertValidSouffle(output);
        assertEquals(Set.of(), SouffleFixture.runAndCollectAnnotations(output));
    }

    @Test
    void annotationFacts_compSimple() throws Exception {
        // Family has author + description; gp-rule overrides description and inherits author.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input = loadResource("meta/comp-simple-input.dl");
        SouffleFixture.assumeValidSouffle(input);
        String output = proc.process(input, Set.of("author", "description"));
        SouffleFixture.assertValidSouffle(output);
        assertEquals(Set.of(
                List.of("Family",  "author",      "jens"),
                List.of("Family",  "description", "family rules"),
                List.of("gp-rule", "description", "grandparent derivation"),
                List.of("gp-rule", "author",      "jens")    // inherited from Family
        ), SouffleFixture.runAndCollectAnnotations(output));
    }

    @Test
    void annotationFacts_compNoId() throws Exception {
        // No rule IDs → only the component-level annotations are output.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input = loadResource("meta/comp-no-id-input.dl");
        SouffleFixture.assumeValidSouffle(input);
        String output = proc.process(input, Set.of("author", "description"));
        SouffleFixture.assertValidSouffle(output);
        assertEquals(Set.of(
                List.of("Family", "author",      "jens"),
                List.of("Family", "description", "family rules")
        ), SouffleFixture.runAndCollectAnnotations(output));
    }

    @Test
    void annotationFacts_nestedCompInherit() throws Exception {
        // Inner has no asserted author → it inherits from Outer.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input = loadResource("meta/comp-nested-inherit-input.dl");
        SouffleFixture.assumeValidSouffle(input);
        String output = proc.process(input, Set.of("author"));
        SouffleFixture.assertValidSouffle(output);
        assertEquals(Set.of(
                List.of("Outer", "author", "jens"),
                List.of("Inner", "author", "jens")  // inherited from Outer
        ), SouffleFixture.runAndCollectAnnotations(output));
    }

    @Test
    void annotationFacts_nestedCompOverride() throws Exception {
        // Inner asserts a different author → inheritance is suppressed for that key.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input = loadResource("meta/comp-nested-override-input.dl");
        SouffleFixture.assumeValidSouffle(input);
        String output = proc.process(input, Set.of("author"));
        SouffleFixture.assertValidSouffle(output);
        assertEquals(Set.of(
                List.of("Outer", "author", "jens"),
                List.of("Inner", "author", "alice")  // own assertion wins
        ), SouffleFixture.runAndCollectAnnotations(output));
    }

    @Test
    void annotationFacts_partialOverride() throws Exception {
        // Inner overrides author but not project → project is inherited from Outer.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input = loadResource("meta/comp-partial-override-input.dl");
        SouffleFixture.assumeValidSouffle(input);
        String output = proc.process(input, Set.of("author", "project"));
        SouffleFixture.assertValidSouffle(output);
        assertEquals(Set.of(
                List.of("Outer", "author",  "jens"),
                List.of("Outer", "project", "medalog"),
                List.of("Inner", "author",  "alice"),    // overrides Outer
                List.of("Inner", "project", "medalog")   // inherited from Outer
        ), SouffleFixture.runAndCollectAnnotations(output));
    }

    @Test
    void annotationFacts_deeplyNested() throws Exception {
        // Hierarchy: Top > Middle > Bottom.
        // Inheritance uses _assertedAnnotation on the direct parent only (one hop).
        // Middle inherits project from Top's asserted fact.
        // Bottom inherits author from Middle's asserted fact.
        // Bottom does NOT inherit project because Middle's project is inferred, not asserted.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input = loadResource("meta/comp-deeply-nested-input.dl");
        SouffleFixture.assumeValidSouffle(input);
        String output = proc.process(input, Set.of("author", "project"));
        SouffleFixture.assertValidSouffle(output);
        assertEquals(Set.of(
                List.of("Top",    "project", "medalog"),
                List.of("Middle", "author",  "jens"),
                List.of("Middle", "project", "medalog"),  // inherits from Top (asserted)
                List.of("Bottom", "author",  "jens")      // inherits from Middle (asserted)
                // Bottom does NOT get project: Middle's project is inherited, not asserted
        ), SouffleFixture.runAndCollectAnnotations(output));
    }

    @Test
    void annotationFacts_ruleInheritsFromComp() throws Exception {
        // gp-rule has no asserted author → it inherits from Family.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input = loadResource("meta/comp-rule-inherits-input.dl");
        SouffleFixture.assumeValidSouffle(input);
        String output = proc.process(input, Set.of("author"));
        SouffleFixture.assertValidSouffle(output);
        assertEquals(Set.of(
                List.of("Family",  "author", "jens"),
                List.of("gp-rule", "author", "jens")  // inherited from Family
        ), SouffleFixture.runAndCollectAnnotations(output));
    }

    @Test
    void annotationFacts_ruleOverridesComp() throws Exception {
        // gp-rule asserts a different author → component-level value is not inherited.
        SouffleFixture.assumeSouffleSupportsAnnotations();
        String input = loadResource("meta/comp-rule-overrides-input.dl");
        SouffleFixture.assumeValidSouffle(input);
        String output = proc.process(input, Set.of("author"));
        SouffleFixture.assertValidSouffle(output);
        assertEquals(Set.of(
                List.of("Family",  "author", "jens"),
                List.of("gp-rule", "author", "alice")  // own assertion wins
        ), SouffleFixture.runAndCollectAnnotations(output));
    }
}
