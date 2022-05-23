package org.javabeeb.ui.icon;

import java.awt.*;

public class RoundButtonIcon extends ButtonIcon {

    @Override
    public void paintImpl(Graphics2D g, Rectangle rect, boolean enabled, boolean rollover, boolean armed) {
        g.fillOval(rect.x, rect.y, rect.width, rect.height);
    }
}
