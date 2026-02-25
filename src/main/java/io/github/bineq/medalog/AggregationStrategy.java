package io.github.bineq.medalog;

import java.util.List;

/**
 * Strategy interface for generating unique IDs for derived facts in compiled Souffle rules.
 *
 * <p>When a MeDaLog rule is compiled, a derived fact's ID is constructed from:
 * <ul>
 *   <li>the rule's own ID</li>
 *   <li>the IDs of the body atoms that produced it</li>
 * </ul>
 *
 * <p>The strategy is responsible for generating the Souffle expression (as a string)
 * that computes such a composite ID at query time.
 */
public interface AggregationStrategy {

    /**
     * Generates the Souffle expression that computes the ID for a derived fact.
     *
     * @param ruleId    the Souffle expression for the rule ID (e.g., a quoted string constant)
     * @param bodyIds   the Souffle variable names holding the IDs of matching body atoms
     * @return a Souffle expression string, e.g. {@code cat(ruleId, cat("[", cat(id1, "]")))}
     */
    String generateId(String ruleId, List<String> bodyIds);
}
