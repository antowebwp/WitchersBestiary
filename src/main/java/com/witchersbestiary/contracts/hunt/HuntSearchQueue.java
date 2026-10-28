package com.witchersbestiary.contracts.hunt;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Cooperative server-thread scheduler. Steps must never load chunks or block on IO. */
public final class HuntSearchQueue {
    public static final long DEFAULT_BUDGET_NANOS = 1_000_000L;
    public enum Outcome { CONTINUE, FOUND, WAITING, CANCELLED }
    public interface Search {
        UUID owner();
        /** One bounded candidate probe; WAITING removes the job until explicitly re-enqueued. */
        Outcome step();
    }
    public record Sample(long nanos, int probes, int pending) {}
    private final ArrayDeque<Search> queue = new ArrayDeque<>();
    private final Map<UUID, Search> owners = new HashMap<>();
    private final LongSupplier clock;
    private final long budget;
    private final int maximumSteps;
    public HuntSearchQueue() { this(System::nanoTime, DEFAULT_BUDGET_NANOS, 64); }
    public HuntSearchQueue(LongSupplier clock, long budget, int maximumSteps) {
        if (budget <= 0 || maximumSteps <= 0) throw new IllegalArgumentException("Positive budget and step limit required");
        this.clock = java.util.Objects.requireNonNull(clock); this.budget = budget; this.maximumSteps = maximumSteps;
    }
    public boolean enqueue(Search job) {
        java.util.Objects.requireNonNull(job); java.util.Objects.requireNonNull(job.owner());
        if (owners.putIfAbsent(job.owner(), job) != null) return false;
        queue.addLast(job); return true;
    }
    public boolean cancel(UUID owner) {
        var job = owners.remove(owner);
        return job != null && queue.remove(job);
    }
    public int pending() { return owners.size(); }
    public void clear() { queue.clear(); owners.clear(); }
    public Sample tick() {
        long begin = clock.getAsLong(); int steps = 0;
        // Cooperative limit: one in-flight step can overrun; it cannot safely be preempted.
        while (!queue.isEmpty() && steps < maximumSteps && clock.getAsLong() - begin < budget) {
            var job = queue.removeFirst(); Outcome outcome;
            try { outcome = java.util.Objects.requireNonNull(job.step()); }
            catch (RuntimeException e) { owners.remove(job.owner(), job); throw e; }
            steps++;
            if (owners.get(job.owner()) != job) continue;
            if (outcome == Outcome.CONTINUE) queue.addLast(job);
            else owners.remove(job.owner(), job);
        }
        return new Sample(Math.max(0, clock.getAsLong() - begin), steps, owners.size());
    }
}
