package org.javabeeb.ui.icon;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class MaximiseWindowIcon extends ButtonIcon {

    @Override
    public void paintImpl(Graphics2D g, Rectangle rect, boolean enabled, boolean rollover, boolean armed) {
        g.setStroke(new BasicStroke(rect.width / 8.0f));
        final float d = rect.width / 4.0f;
        g.draw(new Rectangle2D.Float(rect.x + d, rect.y + d, rect.width - d * 2, rect.height - d * 2));
    }
}
