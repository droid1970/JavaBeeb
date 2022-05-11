package org.javabeeb.screen;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface ScreenImageConsumer {
    void setImage(BufferedImage image, Point imageOrigin);
    void setPaused(final boolean paused);
}
