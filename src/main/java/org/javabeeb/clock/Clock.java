package org.javabeeb.clock;

import org.javabeeb.util.SystemStatus;
import org.javabeeb.util.Util;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class Clock {

    private static final int MAX_RESET_CYCLES = 8_000_000;
    private static final long ADJUST_MASK = 0xFF;

    private final SystemStatus systemStatus;
    private final ClockListener[] listeners;
    private final long maxCycleCount;
    private volatile boolean paused;
    private long cycleCount;
    private long cycleCountSinceReset;

    private ClockDefinition definition = ClockDefinition.CR200;
    private long initialDelayNanos;
    private long delayNanos;

    private long nextTickTime;

    public Clock(
            final SystemStatus systemStatus,
            final ClockDefinition definition,
            final long maxCycleCount,
            final List<ClockListener> listeners
    ) {
        this.systemStatus = Objects.requireNonNull(systemStatus);
        setDefinition(definition);
        this.maxCycleCount = maxCycleCount;
        this.listeners = new ClockListener[listeners.size()];
        for (int i = 0; i < listeners.size(); i++) {
            this.listeners[i] = listeners.get(i);
        }
    }

    public ClockDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(final ClockDefinition definition) {
        this.definition = Objects.requireNonNull(definition);
        this.delayNanos = 1_000_000_000L / definition.getClockRate();
        this.initialDelayNanos = this.delayNanos;
    }

    public long getCycleCount() {
        return cycleCount;
    }

    public void setPaused(final boolean paused) {
        for (ClockListener l : listeners) {
            l.setPaused(paused);
        }
        this.paused = paused;
    }

    public void run(final BooleanSupplier stopCondition) {
        long firstStartTime = System.nanoTime();
        long resetTime = firstStartTime;
        this.nextTickTime = resetTime + delayNanos;
        while (!stopCondition.getAsBoolean()) {
            if (paused) {
                while (paused) {
                    final long t = System.nanoTime();
                    Util.sleep(100);
                    firstStartTime += System.nanoTime() - t;
                }
                resetTime = resetDelay(resetTime, false);
            }

            final long nanoTime = awaitNextCycle();

            // Send tick to all the listeners
            for (ClockListener l : listeners) {
                l.tick(definition, nanoTime - firstStartTime);
            }

            cycleCount++;
            cycleCountSinceReset++;
            if (cycleCount >= maxCycleCount) {
                return;
            }
            if ((cycleCountSinceReset & ADJUST_MASK) == 0) {
                adjustDelay(System.nanoTime() - resetTime);
            }

            // Reset every second or so
            if (cycleCountSinceReset >= Math.min(definition.getClockRate(), MAX_RESET_CYCLES)) {
                resetTime = resetDelay(resetTime, true);
            }
        }
    }

    private void updateSystemStatus(final long duration) {
        final double seconds = (double) duration / 1_000_000_000L;
        final double cyclesPerSecond = cycleCountSinceReset / seconds / 1000000.0;
        systemStatus.putDouble(SystemStatus.KEY_MHZ, cyclesPerSecond);
    }

    private void adjustDelay(final long durationNanos) {
        // Adjust delay between cycles in order to maintain average clock rate
        final double secs = (double) durationNanos / 1_000_000_000L;
        final double cps = cycleCountSinceReset / secs;
        final double delta = cps / definition.getClockRate();
        delayNanos = Math.min(initialDelayNanos, Math.max(10L, (long) (delayNanos * delta)));
    }

    private long resetDelay(final long resetTime, final boolean updateStatus) {
        delayNanos = initialDelayNanos;
        final long now = System.nanoTime();
        if (updateStatus) {
            updateSystemStatus(now - resetTime);
        }
        cycleCountSinceReset = 0L;
        nextTickTime = now + delayNanos;
        return now;
    }

    private long awaitNextCycle() {
        // This is heavy on CPU but, for clockrates < 4Mhz, results in an, on average, very precise clock
        long nanoTime;
        do {
            nanoTime = System.nanoTime();
        } while (definition.isThrottled() && nextTickTime > 0L && ((nextTickTime - nanoTime) > 0));
        nextTickTime += delayNanos;
        return nanoTime;
    }
}
