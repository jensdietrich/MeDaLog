package io.github.bineq.medalog;

/**
 * Represents a parsed annotation value.
 * Provides a {@link #toSouffleString()} method that serializes the value
 * as a Souffle {@code symbol} literal (a double-quoted string).
 */
public sealed interface AnnotationValue
        permits AnnotationValue.StringVal,
                AnnotationValue.NumberVal,
                AnnotationValue.BooleanVal,
                AnnotationValue.TimestampVal,
                AnnotationValue.JsonVal {

    /** Returns the Souffle string representation (always a quoted symbol). */
    String toSouffleString();

    /** Returns the raw string content (without Souffle quoting). */
    String toRawString();

    // ------------------------------------------------------------------
    record StringVal(String value) implements AnnotationValue {
        @Override
        public String toSouffleString() {
            return "\"" + escape(value) + "\"";
        }
        @Override
        public String toRawString() { return value; }
    }

    // ------------------------------------------------------------------
    record NumberVal(String rawText) implements AnnotationValue {
        @Override
        public String toSouffleString() { return "\"" + rawText + "\""; }
        @Override
        public String toRawString() { return rawText; }
    }

    // ------------------------------------------------------------------
    record BooleanVal(boolean value) implements AnnotationValue {
        @Override
        public String toSouffleString() { return "\"" + value + "\""; }
        @Override
        public String toRawString() { return Boolean.toString(value); }
    }

    // ------------------------------------------------------------------
    record TimestampVal(String isoText) implements AnnotationValue {
        @Override
        public String toSouffleString() { return "\"" + isoText + "\""; }
        @Override
        public String toRawString() { return isoText; }
    }

    // ------------------------------------------------------------------
    /** JSON object or array, serialized as a Souffle string symbol. */
    record JsonVal(String jsonText) implements AnnotationValue {
        @Override
        public String toSouffleString() { return "\"" + escape(jsonText) + "\""; }
        @Override
        public String toRawString() { return jsonText; }
    }

    // ------------------------------------------------------------------
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
