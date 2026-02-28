package io.github.bineq.medalog.id;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatAggregationStrategyTest {

    private final CatAggregationStrategy strat = new CatAggregationStrategy();

    @Test
    void noBodyIds_returnsRuleIdOnly() {
        assertEquals("\"r1\"", strat.generateId("\"r1\"", List.of()));
    }

    @Test
    void oneBodyId() {
        assertEquals("cat(\"r1\", cat(\"[\", cat(_id1, \"]\")))",
                strat.generateId("\"r1\"", List.of("_id1")));
    }

    @Test
    void twoBodyIds() {
        assertEquals("cat(\"r1\", cat(\"[\", cat(_id1, cat(\",\", cat(_id2, \"]\")))))",
                strat.generateId("\"r1\"", List.of("_id1", "_id2")));
    }

    @Test
    void negatedAtomStringInBodyIds() {
        assertEquals("cat(\"r1\", cat(\"[\", cat(_id1, cat(\",\", cat(\"!adopted\", \"]\")))))",
                strat.generateId("\"r1\"", List.of("_id1", "\"!adopted\"")));
    }
}
