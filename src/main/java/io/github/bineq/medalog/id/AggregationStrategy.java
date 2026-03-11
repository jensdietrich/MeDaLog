package io.github.bineq.medalog.id;

import java.util.List;

/**
 * Strategy interface for generating provenance IDs for derived facts.
 *
 * <p>When the identity annotation processor rewrites a rule, the derived fact's
 * ID is constructed from:
 * <ul>
 *   <li>the rule's own ID</li>
 *   <li>the IDs of the body atoms (positive atoms contribute their {@code _idN}
 *       variable; negated atoms contribute a literal string {@code "!predname"})</li>
 *   <li>if no body IDs are available, the existing arguments of the head atom are
 *       used to ensure the generated ID is unique per derived fact</li>
 * </ul>
 *
 * <p>The strategy is responsible for producing the Souffle expression (as a string)
 * that computes such a composite ID at query time.
 */
public interface AggregationStrategy {

    /**
     * Generates the Souffle expression that computes the derived-fact ID.
     *
     * @param ruleId   the Souffle expression for the rule ID (e.g. {@code "\"r1\""})
     * @param bodyIds  the list of ID expressions for body atoms; positive atoms contribute
     *                 their {@code _id1}, {@code _id2}, … variables; negated atoms
     *                 contribute a quoted string literal such as {@code "\"!adopted\""}
     * @param headArgs the existing arguments of the head atom (excluding the id slot),
     *                 used as fallback when {@code bodyIds} is empty
     * @return a Souffle expression, e.g.
     *         {@code cat("r1", cat("[", cat(_id1, cat(",", cat(_id2, "]")))))}
     *         or {@code cat("r1", headArg)} when body IDs are absent
     */
    String generateId(String ruleId, List<String> bodyIds, List<String> headArgs);

    /**
     * Convenience overload with no head-argument fallback; delegates to
     * {@link #generateId(String, List, List)} with an empty head-args list.
     */
    default String generateId(String ruleId, List<String> bodyIds) {
        return generateId(ruleId, bodyIds, List.of());
    }
}
