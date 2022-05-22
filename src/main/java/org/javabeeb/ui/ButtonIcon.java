package org.javabeeb.ui;

import java.awt.*;

public abstract class ButtonIcon {

    private static final Color DISABLED_COLOR = Color.DARK_GRAY;
    private static final Color ENABLED_COLOR = Color.LIGHT_GRAY;
    private static final Color ROLLOVER_COLOR = Color.WHITE;
    private static final Color ARMED_COLOR = Color.WHITE;

    public void paint(final Graphics2D g, final Rectangle rect, final boolean enabled, final boolean rollover, final boolean armed) {
        Color color = ENABLED_COLOR;
        if (!enabled) {
            color = DISABLED_COLOR;
        } else if (armed) {
            color = ARMED_COLOR;
        } else if (rollover) {
            color = ROLLOVER_COLOR;
        }
        g.setColor(color);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintImpl(g, rect, enabled, rollover, armed);
    }

    public abstract void paintImpl(Graphics2D g, Rectangle rect, boolean enabled, boolean rollover, boolean armed);
}
