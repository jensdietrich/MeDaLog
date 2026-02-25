package io.github.bineq.medalog;

/**
 * A parsed annotation, consisting of a key and a value.
 */
public record Annotation(String key, AnnotationValue value, int lineNumber) {

    /** Returns true if this is an {@code @id} annotation (case-insensitive). */
    public boolean isId() {
        return "id".equalsIgnoreCase(key);
    }

    /**
     * Returns the normalised key: trimmed and lowercased.
     * Used for equivalence checks.
     */
    public String normalisedKey() {
        return key.trim().toLowerCase();
    }
}
