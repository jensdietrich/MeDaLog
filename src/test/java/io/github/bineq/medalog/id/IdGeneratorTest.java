package io.github.bineq.medalog.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdGeneratorTest {

    @Test
    void factIdsStartWithF() {
        IdGenerator gen = new IdGenerator();
        assertTrue(gen.nextFactId().startsWith("F"));
    }

    @Test
    void ruleIdsStartWithR() {
        IdGenerator gen = new IdGenerator();
        assertTrue(gen.nextRuleId().startsWith("R"));
    }

    @Test
    void factIdsAreMonotonicallyIncreasing() {
        IdGenerator gen = new IdGenerator();
        assertEquals("F1", gen.nextFactId());
        assertEquals("F2", gen.nextFactId());
        assertEquals("F3", gen.nextFactId());
    }

    @Test
    void ruleIdsAreMonotonicallyIncreasing() {
        IdGenerator gen = new IdGenerator();
        assertEquals("R1", gen.nextRuleId());
        assertEquals("R2", gen.nextRuleId());
    }

    @Test
    void factAndRuleCountersAreIndependent() {
        IdGenerator gen = new IdGenerator();
        assertEquals("F1", gen.nextFactId());
        assertEquals("R1", gen.nextRuleId());
        assertEquals("F2", gen.nextFactId());
        assertEquals("R2", gen.nextRuleId());
    }

    @Test
    void differentInstancesHaveIndependentCounters() {
        IdGenerator g1 = new IdGenerator();
        IdGenerator g2 = new IdGenerator();
        assertEquals("F1", g1.nextFactId());
        assertEquals("F1", g2.nextFactId());
    }
}
