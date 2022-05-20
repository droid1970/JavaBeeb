package org.javabeeb.ui;

import javax.swing.*;
import java.awt.*;

public class RoundIcon implements Icon {

    private static final int OVAL_WIDTH = 8;
    private static final int OVAL_HEIGHT = 8;

    private final int width;
    private final int height;

    private Color colour;

    public RoundIcon(final int width, final int height) {
        this.width = width;
        this.height = height;
    }

    public void setColour(final Color colour) {
        this.colour = colour;
    }

    @Override
    public void paintIcon(Component c, Graphics g1, int x, int y) {
        final Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(colour);
        g.fillOval(x + (getIconWidth() - OVAL_WIDTH) / 2, y + (c.getHeight() - OVAL_HEIGHT) / 2, OVAL_WIDTH, OVAL_HEIGHT);
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}