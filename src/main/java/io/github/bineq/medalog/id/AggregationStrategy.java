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
 * </ul>
 *
 * <p>The strategy is responsible for producing the Souffle expression (as a string)
 * that computes such a composite ID at query time.
 */
public interface AggregationStrategy {

    /**
     * Generates the Souffle expression that computes the derived-fact ID.
     *
     * @param ruleId  the Souffle expression for the rule ID (e.g. {@code "\"r1\""})
     * @param bodyIds the list of ID expressions for body atoms; positive atoms contribute
     *                their {@code _id1}, {@code _id2}, … variables; negated atoms
     *                contribute a quoted string literal such as {@code "\"!adopted\""}
     * @return a Souffle expression, e.g.
     *         {@code cat("r1", cat("[", cat(_id1, cat(",", cat(_id2, "]")))))}
     */
    String generateId(String ruleId, List<String> bodyIds);
}
