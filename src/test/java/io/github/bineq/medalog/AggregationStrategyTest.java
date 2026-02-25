package io.github.bineq.medalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the {@link CatAggregationStrategy}.
 */
class AggregationStrategyTest {

    private final CatAggregationStrategy strategy = new CatAggregationStrategy();

    @Test
    void noBodyIds_returnsRuleIdUnchanged() {
        assertEquals("\"myRule\"", strategy.generateId("\"myRule\"", List.of()));
    }

    @Test
    void oneBodyId_wrapsInBrackets() {
        String result = strategy.generateId("\"r1\"", List.of("_id1"));
        assertEquals("cat(\"r1\", cat(\"[\", cat(_id1, \"]\")))", result);
    }

    @Test
    void twoBodyIds_commaDelimited() {
        String result = strategy.generateId("\"r1\"", List.of("_id1", "_id2"));
        assertEquals(
                "cat(\"r1\", cat(\"[\", cat(_id1, cat(\",\", cat(_id2, \"]\")))))",
                result);
    }

    @Test
    void threeBodyIds() {
        String result = strategy.generateId("\"r\"", List.of("a", "b", "c"));
        // buildInner(0) = cat(a, cat(",", buildInner(1)))
        // buildInner(1) = cat(b, cat(",", buildInner(2)))
        // buildInner(2) = cat(c, "]")    <- last element
        // So inner = cat(a, cat(",", cat(b, cat(",", cat(c, "]")))))
        // generateId wraps: cat("r", cat("[", inner))
        // = cat("r", cat("[", cat(a, cat(",", cat(b, cat(",", cat(c, "]")))))))
        String expected = "cat(\"r\", cat(\"[\", cat(a, cat(\",\", cat(b, cat(\",\", cat(c, \"]\")))))))";
        assertEquals(expected, result);
    }

    @Test
    void proofFormatHoldsForOneId() {
        // Verify output conforms to proof grammar: ruleId[id1]
        String expr = strategy.generateId("\"rule42\"", List.of("\"F1\""));
        // Evaluating cat("rule42", cat("[", cat("F1", "]"))) = "rule42[F1]"
        assertEquals("cat(\"rule42\", cat(\"[\", cat(\"F1\", \"]\")))", expr);
    }
}
