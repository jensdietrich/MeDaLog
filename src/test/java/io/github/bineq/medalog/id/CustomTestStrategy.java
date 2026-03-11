package io.github.bineq.medalog.id;

import java.util.List;

/**
 * Minimal {@link AggregationStrategy} used in tests for the pluggable-strategy CLI feature.
 * Produces {@code concat(ruleId, "|", id1, ",", id2, ...)} so tests can distinguish
 * it clearly from the default {@link CatAggregationStrategy} output.
 */
public class CustomTestStrategy implements AggregationStrategy {

    @Override
    public String generateId(String ruleId, List<String> bodyIds, List<String> headArgs) {
        if (!bodyIds.isEmpty()) {
            return ruleId + "|" + String.join(",", bodyIds);
        }
        if (!headArgs.isEmpty()) {
            return ruleId + "|" + String.join(",", headArgs);
        }
        return ruleId;
    }
}
