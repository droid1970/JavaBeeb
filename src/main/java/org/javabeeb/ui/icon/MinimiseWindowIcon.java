package org.javabeeb.ui.icon;

import java.awt.*;
import java.awt.geom.Line2D;

public class MinimiseWindowIcon extends ButtonIcon {

    @Override
    public void paintImpl(Graphics2D g, Rectangle rect, boolean enabled, boolean rollover, boolean armed) {
        g.setStroke(new BasicStroke(rect.width / 8.0f));
        final float d = rect.width / 4.0f;
        g.draw(new Line2D.Float(rect.x + d, rect.y + rect.height / 2.0f, rect.x + rect.width - d, rect.y + rect.height / 2.0f));
    }
}
