package io.github.bineq.medalog.meta;

import io.github.bineq.medalog.AnnotationProcessorException;
import io.github.bineq.medalog.ProcessorTestBase;
import io.github.bineq.medalog.SouffleFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
    void outputIsValidSouffle_compWithoutIdAnnotations() throws Exception {
        SouffleFixture.assumeSouffleAvailable();
        // Use input without @[id] annotations so the metadata-processor-only output is valid Souffle
        String input = loadResource("meta/comp-no-id-input.dl");
        String output = proc.process(input, Set.of("author", "description"));
        SouffleFixture.assertValidSouffle(output);
    }
}
