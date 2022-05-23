package org.javabeeb.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import org.javabeeb.ui.icon.ButtonIcon;

// A simple icon-only, square button
public class IconButton extends JButton {

    private final ButtonIcon icon;
    private final int size;
    private Color rolloverBackground = null;

    public IconButton(final ButtonIcon icon, final int size) {
        this.icon = Objects.requireNonNull(icon);
        this.size = size;
        setOpaque(false);
        setBorder(null);
        setFocusable(false);
    }

    public void setRolloverBackground(final Color rolloverBackground) {
        this.rolloverBackground = rolloverBackground;
        repaint();
    }

    @Override
    public void paintComponent(final Graphics g1) {
        final Graphics2D g = (Graphics2D) g1;

        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        final Rectangle inner = SwingUtilities.calculateInnerArea(this, null);
        final ButtonModel model = getModel();

        if (rolloverBackground != null && model.isRollover() || model.isPressed() || model.isArmed()) {
            g.setColor(rolloverBackground);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        icon.paint(g, inner, model.isEnabled(), model.isRollover() || model.isPressed(), model.isArmed());
    }

    @Override
    public Dimension getPreferredSize() {
        final Insets insets = getInsets();
        return new Dimension(
                insets.left + size + insets.right,
                insets.top + size + insets.bottom
        );
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
