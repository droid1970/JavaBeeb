package org.javabeeb.clock;

public interface ClockListener {

    void tick(ClockDefinition clockDefinition, long elapsedNanos);

    default void setPaused(final boolean paused) {
        // Do nothing by default
    }
}
