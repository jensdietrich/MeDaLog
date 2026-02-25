package io.github.bineq.medalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for annotation parsing, value types, and serialisation.
 */
class AnnotationTest {

    @Test
    void stringAnnotation() {
        String source = ".decl foo(x: symbol)\n"
                + "@id: \"f1\"\n"
                + "@author: \"John Doe\"\n"
                + "foo(\"a\").\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_assertedAnnotation(\"f1\", \"author\", \"John Doe\")"),
                "Expected string annotation fact, got:\n" + out);
    }

    @Test
    void timestampAnnotation() {
        String source = ".decl foo(x: symbol)\n"
                + "@id: \"t1\"\n"
                + "@created: 2025-02-16T14:30:00Z\n"
                + "foo(\"a\").\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_assertedAnnotation(\"t1\", \"created\", \"2025-02-16T14:30:00Z\")"),
                "Expected timestamp annotation fact, got:\n" + out);
    }

    @Test
    void jsonObjectAnnotation() {
        String source = ".decl foo(x: symbol)\n"
                + "@id: \"j1\"\n"
                + "@verified: {\"name\":\"Alice\",\"email\":\"a@b.com\"}\n"
                + "foo(\"a\").\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_assertedAnnotation(\"j1\", \"verified\","),
                "Expected JSON object annotation fact, got:\n" + out);
    }

    @Test
    void booleanAnnotation() {
        String source = ".decl foo(x: symbol)\n"
                + "@id: \"b1\"\n"
                + "@active: true\n"
                + "foo(\"a\").\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_assertedAnnotation(\"b1\", \"active\", \"true\")"),
                "Expected boolean annotation fact, got:\n" + out);
    }

    @Test
    void numericalAnnotation() {
        String source = ".decl foo(x: symbol)\n"
                + "@id: \"n1\"\n"
                + "@version: 42\n"
                + "foo(\"a\").\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_assertedAnnotation(\"n1\", \"version\", \"42\")"),
                "Expected numerical annotation fact, got:\n" + out);
    }

    @Test
    void hyphenatedAnnotationKey() {
        String source = ".decl foo(x: symbol)\n"
                + "@id: \"hk1\"\n"
                + "@last-modified: 2025-02-16T14:30:00Z\n"
                + "foo(\"a\").\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("\"last-modified\""),
                "Expected hyphenated key in annotation, got:\n" + out);
    }

    @Test
    void idAnnotationWithEqualsSign() {
        // @id=value (using = instead of :)
        String source = ".decl foo(x: symbol)\n"
                + "@id=\"myfact\"\n"
                + "foo(\"a\").\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("foo(\"myfact\","),
                "Expected @id via = sign, got:\n" + out);
    }

    @Test
    void multipleAnnotationsAllApplied() {
        String source = ".decl foo(x: symbol)\n"
                + "@id: \"multi1\"\n"
                + "@author: \"Alice\"\n"
                + "@version: 1\n"
                + "foo(\"x\").\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("\"author\""), "Expected author annotation, got:\n" + out);
        assertTrue(out.contains("\"version\""), "Expected version annotation, got:\n" + out);
    }

    @Test
    void moduleAnnotationAppliedToModule() {
        String source = "@author: \"Bob\"\n"
                + "module m {\n"
                + "}\n";
        String out = Compiler.compile(source);
        assertTrue(out.contains("_assertedAnnotation(\"m\", \"author\", \"Bob\")"),
                "Expected module annotation fact, got:\n" + out);
    }

    @Test
    void annotationValueTypes() {
        AnnotationValue sv = new AnnotationValue.StringVal("hello");
        assertEquals("\"hello\"", sv.toSouffleString());
        assertEquals("hello", sv.toRawString());

        AnnotationValue nv = new AnnotationValue.NumberVal("42");
        assertEquals("\"42\"", nv.toSouffleString());

        AnnotationValue bv = new AnnotationValue.BooleanVal(true);
        assertEquals("\"true\"", bv.toSouffleString());

        AnnotationValue tv = new AnnotationValue.TimestampVal("2025-02-16T14:30:00Z");
        assertEquals("\"2025-02-16T14:30:00Z\"", tv.toSouffleString());
    }
}
