package io.github.bineq.medalog.id;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatAggregationStrategyTest {

    private final CatAggregationStrategy strat = new CatAggregationStrategy();

    @Test
    void noBodyIds_noHeadArgs_returnsRuleIdOnly() {
        assertEquals("\"r1\"", strat.generateId("\"r1\"", List.of(), List.of()));
    }

    @Test
    void noBodyIds_oneHeadArg_usesFlatCat() {
        assertEquals("cat(\"r1\", x)", strat.generateId("\"r1\"", List.of(), List.of("x")));
    }

    @Test
    void noBodyIds_twoHeadArgs_usesNestedFlatCat() {
        assertEquals("cat(cat(\"r1\", x), y)", strat.generateId("\"r1\"", List.of(), List.of("x", "y")));
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
