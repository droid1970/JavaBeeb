package org.javabeeb.ui;

import java.awt.*;

public final class ClockIcon extends RoundIcon  {

    private static final double NORMAL_CLOCK_RATE = 2.0;
    private static final double MIN_NORMAL_CLOCK_RATE = NORMAL_CLOCK_RATE * 0.975;
    private static final double MAX_NORMAL_CLOCK_RATE = NORMAL_CLOCK_RATE * 1.05;
    private static final Color NORMAL_CLOCK_RATE_COLOUR = Color.GREEN.darker();
    private static final Color FAST_CLOCK_RATE_COLOUR = Color.ORANGE;
    private static final Color SLOW_CLOCK_RATE_COLOUR = Color.ORANGE;

    public ClockIcon(final int width, final int height, final int yoffset) {
        super(width, height, yoffset);
    }

    public void setRate(final double rate) {
        if (rate >= MIN_NORMAL_CLOCK_RATE && rate <= MAX_NORMAL_CLOCK_RATE) {
            setColour(NORMAL_CLOCK_RATE_COLOUR);
        } else {
            if (rate < NORMAL_CLOCK_RATE) {
                setColour(SLOW_CLOCK_RATE_COLOUR);
            } else {
                setColour(FAST_CLOCK_RATE_COLOUR);
            }
        }
    }
}