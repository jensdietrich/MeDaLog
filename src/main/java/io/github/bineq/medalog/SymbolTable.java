package io.github.bineq.medalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Symbol table built during the first pass of compilation.
 * Tracks declared predicates and allocated unique IDs.
 */
public class SymbolTable {

    private final Map<String, PredicateInfo> predicates = new LinkedHashMap<>();
    private int factCounter = 0;
    private int ruleCounter = 0;

    /** Registers a declared predicate. Duplicate registrations are silently ignored. */
    public void registerPredicate(PredicateInfo info) {
        predicates.put(info.getName(), info);
    }

    /** Returns the {@link PredicateInfo} for a given predicate name, or {@code null} if unknown. */
    public PredicateInfo getPredicate(String name) {
        return predicates.get(name);
    }

    /** Returns true if the predicate name is declared in this program. */
    public boolean isDeclared(String name) {
        return predicates.containsKey(name);
    }

    /** Returns an unmodifiable view of all declared predicate names. */
    public Set<String> getDeclaredPredicateNames() {
        return Collections.unmodifiableSet(predicates.keySet());
    }

    /** Allocates and returns a unique compiler-generated fact ID, e.g. {@code "F1"}. */
    public String nextFactId() {
        return "F" + (++factCounter);
    }

    /** Allocates and returns a unique compiler-generated rule ID, e.g. {@code "R1"}. */
    public String nextRuleId() {
        return "R" + (++ruleCounter);
    }
}
