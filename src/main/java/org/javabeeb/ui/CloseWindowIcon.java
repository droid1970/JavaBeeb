package org.javabeeb.ui;

import java.awt.*;
import java.awt.geom.Line2D;

public class CloseWindowIcon extends ButtonIcon {

    @Override
    public void paintImpl(Graphics2D g, Rectangle rect, boolean enabled, boolean rollover, boolean armed) {
        g.setStroke(new BasicStroke(rect.width / 8.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        final float d = rect.width / 4.0f;
        g.draw(new Line2D.Float(rect.x + d, rect.y + d, rect.x + rect.width - d, rect.y + rect.height - d));
        g.draw(new Line2D.Float(rect.x + rect.width - d, rect.y + d, rect.x + d, rect.y + rect.height - d));
    }
}
