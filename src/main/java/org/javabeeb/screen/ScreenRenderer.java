package org.javabeeb.screen;

import org.javabeeb.clock.ClockSpeed;

import java.awt.image.BufferedImage;

public interface ScreenRenderer {

    boolean isClockBased();
    void tick(BufferedImage image, ClockSpeed clockSpeed, long elapsedNanos);
    void newFrame();

    void refreshWholeImage(BufferedImage image);
    boolean isImageReady();
}
