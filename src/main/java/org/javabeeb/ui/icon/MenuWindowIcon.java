package org.javabeeb.ui.icon;

import java.awt.*;
import java.awt.geom.Line2D;

public class MenuWindowIcon extends ButtonIcon {

    @Override
    public void paintImpl(Graphics2D g, Rectangle rect, boolean enabled, boolean rollover, boolean armed) {
        g.setStroke(new BasicStroke(rect.width / 8.0f));
        final float d = rect.width / 4.0f;
        for (int i = 0; i < 3; i++) {
            float y = rect.y + d * (i + 1);
            g.draw(new Line2D.Float(rect.x + d, y, rect.x + rect.width - d, y));
        }
    }
}
