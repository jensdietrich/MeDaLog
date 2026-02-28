package io.github.bineq.medalog.id;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique, short fact and rule IDs for a single annotation-processing run.
 *
 * <ul>
 *   <li>Fact IDs: {@code "F1"}, {@code "F2"}, …</li>
 *   <li>Rule IDs: {@code "R1"}, {@code "R2"}, …</li>
 * </ul>
 *
 * Each {@code IdGenerator} instance has its own counters, so one instance
 * per processing run guarantees uniqueness within that run.
 */
public class IdGenerator {

    private final AtomicInteger factCounter = new AtomicInteger(0);
    private final AtomicInteger ruleCounter = new AtomicInteger(0);

    /** Returns the next unique fact ID, e.g. {@code "F1"}. */
    public String nextFactId() {
        return "F" + factCounter.incrementAndGet();
    }

    /** Returns the next unique rule ID, e.g. {@code "R1"}. */
    public String nextRuleId() {
        return "R" + ruleCounter.incrementAndGet();
    }
}
