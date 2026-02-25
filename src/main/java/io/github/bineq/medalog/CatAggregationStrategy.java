package io.github.bineq.medalog;

import java.util.List;

/**
 * Default aggregation strategy using Souffle's built-in {@code cat} string concatenation.
 *
 * <p>Produces IDs of the form {@code ruleId[bodyId1,bodyId2,...]} using nested {@code cat}
 * calls, so that IDs conform to the proof grammar:
 * <pre>
 *   proof  : node EOF ;
 *   node   : ID children? ;
 *   children : '[' node (',' node)* ']' ;
 * </pre>
 *
 * <p>Examples:
 * <ul>
 *   <li>No body atoms (fact): returns the rule ID expression unchanged.</li>
 *   <li>One body atom: {@code cat(ruleId, cat("[", cat(id0, "]")))}</li>
 *   <li>Two body atoms: {@code cat(ruleId, cat("[", cat(id0, cat(",", cat(id1, "]")))))} </li>
 * </ul>
 */
public class CatAggregationStrategy implements AggregationStrategy {

    @Override
    public String generateId(String ruleId, List<String> bodyIds) {
        if (bodyIds.isEmpty()) {
            return ruleId;
        }
        // Build the inner part: id0,id1,...,idN]
        String inner = buildInner(bodyIds, 0);
        return "cat(" + ruleId + ", cat(\"[\", " + inner + "))";
    }

    /**
     * Recursively builds the inner concatenation for body IDs.
     * For index i:
     *   - if last: cat(bodyIds[i], "]")
     *   - else:    cat(bodyIds[i], cat(",", buildInner(i+1)))
     */
    private String buildInner(List<String> bodyIds, int index) {
        String currentId = bodyIds.get(index);
        if (index == bodyIds.size() - 1) {
            return "cat(" + currentId + ", \"]\")";
        }
        return "cat(" + currentId + ", cat(\",\", " + buildInner(bodyIds, index + 1) + "))";
    }
}
