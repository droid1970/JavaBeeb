package org.javabeeb.util;

import org.javabeeb.clock.ClockListener;

public interface Scheduler extends ClockListener {
    ScheduledTask newTask(final Runnable runnable);
    void schedule(ScheduledTask task, final long delay);
    void unschedule(ScheduledTask task);
}
