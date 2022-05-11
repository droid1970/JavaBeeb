package org.javabeeb.ui;

import java.awt.*;

public final class LedIcon extends RoundIcon {

    private static final Color LED_ON_COLOUR = Color.RED;
    private static final Color LED_OFF_COLOUR = Color.GRAY;

    public LedIcon(final int width, final int height, final int yoffset) {
        super(width, height, yoffset);
    }

    public void setOn(final boolean on) {
        setColour(on ? LED_ON_COLOUR : LED_OFF_COLOUR);
    }
};