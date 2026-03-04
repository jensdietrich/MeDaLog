package io.github.bineq.medalog;

import io.github.bineq.medalog.id.AggregationStrategy;
import io.github.bineq.medalog.id.CatAggregationStrategy;
import io.github.bineq.medalog.id.CustomTestStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Main#loadStrategy(String)}.
 */
class MainAggregationStrategyTest {

    @Test
    void nullReturnsDefaultCatStrategy() {
        AggregationStrategy strategy = Main.loadStrategy(null);
        assertInstanceOf(CatAggregationStrategy.class, strategy);
    }

    @Test
    void blankReturnsDefaultCatStrategy() {
        AggregationStrategy strategy = Main.loadStrategy("   ");
        assertInstanceOf(CatAggregationStrategy.class, strategy);
    }

    @Test
    void emptyStringReturnsDefaultCatStrategy() {
        AggregationStrategy strategy = Main.loadStrategy("");
        assertInstanceOf(CatAggregationStrategy.class, strategy);
    }

    @Test
    void loadsCustomStrategyByFqcn() {
        AggregationStrategy strategy = Main.loadStrategy(CustomTestStrategy.class.getName());
        assertInstanceOf(CustomTestStrategy.class, strategy);
    }

    @Test
    void loadsCatStrategyByFqcn() {
        AggregationStrategy strategy = Main.loadStrategy(CatAggregationStrategy.class.getName());
        assertInstanceOf(CatAggregationStrategy.class, strategy);
    }

    @Test
    void classNotFoundThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Main.loadStrategy("com.example.NonExistentStrategy"));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void classNotImplementingInterfaceThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Main.loadStrategy(String.class.getName()));
        assertTrue(ex.getMessage().contains("does not implement"));
    }

    @Test
    void loadedCustomStrategyProducesExpectedOutput() {
        AggregationStrategy strategy = Main.loadStrategy(CustomTestStrategy.class.getName());
        String result = strategy.generateId("\"R1\"", java.util.List.of("_id1", "_id2"));
        assertEquals("\"R1\"|_id1,_id2", result);
    }
}
