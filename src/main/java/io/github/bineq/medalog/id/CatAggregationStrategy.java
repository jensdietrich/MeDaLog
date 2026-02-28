package io.github.bineq.medalog.id;

import java.util.List;

/**
 * Default {@link AggregationStrategy} using Souffle's built-in {@code cat} function.
 *
 * <p>Produces IDs of the form {@code ruleId[id1,id2,...,idN]} using nested {@code cat}
 * calls, so that IDs conform to the proof grammar (see
 * <a href="https://github.com/binaryeq/daleq/blob/main/src/main/antlr4/io/github/bineq/daleq/souffle/provenance/Proof.g4">Proof.g4</a>).
 *
 * <p>Examples:
 * <ul>
 *   <li>No body IDs: returns {@code ruleId} unchanged.</li>
 *   <li>One body ID: {@code cat(ruleId, cat("[", cat(_id1, "]")))}</li>
 *   <li>Two body IDs: {@code cat(ruleId, cat("[", cat(_id1, cat(",", cat(_id2, "]")))))}</li>
 * </ul>
 */
public class CatAggregationStrategy implements AggregationStrategy {

    @Override
    public String generateId(String ruleId, List<String> bodyIds) {
        if (bodyIds.isEmpty()) {
            return ruleId;
        }
        return "cat(" + ruleId + ", cat(\"[\", " + buildInner(bodyIds, 0) + "))";
    }

    private String buildInner(List<String> bodyIds, int index) {
        String current = bodyIds.get(index);
        if (index == bodyIds.size() - 1) {
            return "cat(" + current + ", \"]\")";
        }
        return "cat(" + current + ", cat(\",\", " + buildInner(bodyIds, index + 1) + "))";
    }
}
