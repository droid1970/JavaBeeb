package org.javabeeb.ui;

import java.awt.*;

public class RateIcon extends RoundIcon {

    private static final Color NORMAL_COLOUR = Color.GREEN.darker();
    private static final Color FAST_COLOUR = Color.ORANGE;
    private static final Color SLOW_COLOUR = Color.ORANGE;

    private final double minRate;
    private final double maxRate;

    public RateIcon(int width, int height, double normalRate) {
        super(width, height);
        this.minRate = normalRate * 0.975;
        this.maxRate = normalRate * 1.05;
    }

    public void setRate(final double rate) {
        if (rate >= minRate && rate <= maxRate) {
            setColour(NORMAL_COLOUR);
        } else {
            if (rate < minRate) {
                setColour(SLOW_COLOUR);
            } else {
                setColour(FAST_COLOUR);
            }
        }
    }
}
