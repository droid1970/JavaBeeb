package org.javabeeb.ui.icon;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

public class SettingsIcon extends ButtonIcon {

    private static final int NOTCH_COUNT = 6;
    private static final double NOTCH_ANGLE = ((2.0 * Math.PI) / NOTCH_COUNT);

    @Override
    public void paintImpl(Graphics2D g, Rectangle rect, boolean enabled, boolean rollover, boolean armed) {
        final double d = 2.0;
        final Ellipse2D.Double ellipse = new Ellipse2D.Double(rect.x + d, rect.y + d, rect.width - d * 2.0, rect.height - d * 2.0);
        final double radius = (rect.width - d * 2.0) / 2.0;
        final Point2D.Double centre = new Point2D.Double(rect.x + d + radius, rect.y + d + radius);
        final Area area = new Area(ellipse);
        final double notchRadius = 2.0;
        double angle = NOTCH_ANGLE / 2.0;
        for (int i = 0; i < NOTCH_COUNT; i++) {
            final double x = centre.x + radius * Math.cos(angle);
            final double y = centre.y + radius * Math.sin(angle);
            area.subtract(new Area(new Ellipse2D.Double(x - notchRadius, y - notchRadius, notchRadius * 2, notchRadius * 2)));
            angle += NOTCH_ANGLE;
        }
        area.subtract(new Area(new Ellipse2D.Double(centre.x - notchRadius, centre.y - notchRadius, notchRadius * 2, notchRadius * 2)));

        g.fill(area);
    }
}

