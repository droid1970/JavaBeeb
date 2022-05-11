package org.javabeeb.screen;

import org.javabeeb.clock.ClockDefinition;

import java.awt.image.BufferedImage;

public interface ScreenRenderer {

    boolean isClockBased();
    void tick(BufferedImage image, ClockDefinition clockDefinition, long elapsedNanos);
    void newFrame();

    void refreshWholeImage(BufferedImage image);
}
