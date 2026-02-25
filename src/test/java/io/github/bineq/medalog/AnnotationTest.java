package io.github.bineq.medalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for annotation parsing and compilation.
 *
 * <p>Each test loads a MeDaLog input from
 * {@code src/test/resources/<testname>-input.mdl} and compares the compiled
 * Souffle output against {@code src/test/resources/<testname>-oracle.dl}.
 *
 * <p>One additional test verifies the {@link AnnotationValue} serialisation API
 * directly, without compiler invocation.
 */
class AnnotationTest extends CompilerTestBase {

    @Test
    void annotationString() {
        assertCompileMatchesOracle("annotation-string");
    }

    @Test
    void annotationTimestamp() {
        assertCompileMatchesOracle("annotation-timestamp");
    }

    @Test
    void annotationJsonObject() {
        assertCompileMatchesOracle("annotation-json-object");
    }

    @Test
    void annotationBoolean() {
        assertCompileMatchesOracle("annotation-boolean");
    }

    @Test
    void annotationNumerical() {
        assertCompileMatchesOracle("annotation-numerical");
    }

    @Test
    void annotationHyphenatedKey() {
        // @last-modified: key with hyphen must be preserved verbatim
        assertCompileMatchesOracle("annotation-hyphenated-key");
    }

    @Test
    void annotationEqualsSign() {
        // @id="value" uses = instead of : as separator
        assertCompileMatchesOracle("annotation-equals-sign");
    }

    @Test
    void annotationMultiple() {
        // Several annotations on the same fact all generate separate _assertedAnnotation facts
        assertCompileMatchesOracle("annotation-multiple");
    }

    @Test
    void moduleAnnotation() {
        // Annotation placed before a module generates a fact for the module id
        assertCompileMatchesOracle("module-annotation");
    }

    @Test
    void duplicateSameValueIsAllowed() {
        // Duplicate annotations with the same key AND value are silently de-duplicated
        assertCompileMatchesOracle("annotation-duplicate-same-value");
    }

    // ==============================
    // AnnotationValue serialisation unit tests (no compiler invocation)
    // ==============================

    @Test
    void annotationValueSerialisationToSouffleString() {
        assertEquals("\"hello\"",           new AnnotationValue.StringVal("hello").toSouffleString());
        assertEquals("\"42\"",              new AnnotationValue.NumberVal("42").toSouffleString());
        assertEquals("\"true\"",            new AnnotationValue.BooleanVal(true).toSouffleString());
        assertEquals("\"false\"",           new AnnotationValue.BooleanVal(false).toSouffleString());
        assertEquals("\"2025-02-16T14:30:00Z\"",
                new AnnotationValue.TimestampVal("2025-02-16T14:30:00Z").toSouffleString());
        assertEquals("\"{\\\"k\\\":1}\"",   new AnnotationValue.JsonVal("{\"k\":1}").toSouffleString());
    }

    @Test
    void annotationValueRawString() {
        assertEquals("world",                   new AnnotationValue.StringVal("world").toRawString());
        assertEquals("-7",                      new AnnotationValue.NumberVal("-7").toRawString());
        assertEquals("true",                    new AnnotationValue.BooleanVal(true).toRawString());
        assertEquals("2025-01-01T00:00:00Z",    new AnnotationValue.TimestampVal("2025-01-01T00:00:00Z").toRawString());
    }
}
