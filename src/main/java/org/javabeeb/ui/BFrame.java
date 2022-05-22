package org.javabeeb.ui;

import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

public class BFrame extends JFrame {

    private static final Color DECORATION_BACKGROUND = Color.DARK_GRAY;

    private static final Border BUTTON_BORDER = new EmptyBorder(0, 4, 0, 4);
    private static final Color BUTTON_ROLLOVER_BACKGROUND = new Color(255, 255, 255, 32);
    private static final Color CLOSE_BUTTON_ROLLOVER_BACKGROUND = Color.RED;
    private static final Color MENU_BUTTON_ROLLOVER_BACKGROUND = new Color(0, 128, 255, 192);

    private static final int EDGE_SIZE = 6;
    private static final int TITLE_HEIGHT = 18;
    private static final Color OUTLINE_COLOR = new Color(255, 255, 255, 64);

    public BFrame(String title) throws HeadlessException {
        super(title);
        setRootPane(new BRootFrame());
        setUndecorated(true);
        final TitleComponent titleComponent = new TitleComponent();
        getContentPane().add(titleComponent, BorderLayout.NORTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void close() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void swapMaximised() {
        if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
            setExtendedState(JFrame.NORMAL);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    private final class TitleComponent extends JComponent {

        final ButtonComponent leftButtonComponent;
        final ButtonComponent rightButtonComponent;
        final JLabel titleLabel;

        TitleComponent() {
            setOpaque(true);
            setBackground(DECORATION_BACKGROUND);
            setPreferredSize(new Dimension(0, TITLE_HEIGHT + EDGE_SIZE));
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(0, 0, EDGE_SIZE, 0));
            leftButtonComponent = new ButtonComponent();
            rightButtonComponent = new ButtonComponent();
            titleLabel = new JLabel("Title");
            titleLabel.setForeground(Color.LIGHT_GRAY);
            titleLabel.setOpaque(false);
            titleLabel.setHorizontalAlignment(JLabel.CENTER);
            add(leftButtonComponent, BorderLayout.WEST);
            add(titleLabel, BorderLayout.CENTER);
            add(rightButtonComponent, BorderLayout.EAST);

            final IconButton menuButton = createButton(new MenuWindowIcon(), MENU_BUTTON_ROLLOVER_BACKGROUND);

            final IconButton minimiseButton = createButton(new MinimiseWindowIcon(), BUTTON_ROLLOVER_BACKGROUND);
            minimiseButton.addActionListener(e -> setExtendedState(JFrame.ICONIFIED));

            final IconButton maximiseButton = createButton(new MaximiseWindowIcon(), BUTTON_ROLLOVER_BACKGROUND);
            maximiseButton.addActionListener(e -> swapMaximised());

            final IconButton closeButton = createButton(new CloseWindowIcon(), CLOSE_BUTTON_ROLLOVER_BACKGROUND);
            closeButton.addActionListener(e -> close());

            leftButtonComponent.addComponent(menuButton);
            leftButtonComponent.add(createBlankButton());
            leftButtonComponent.add(createBlankButton());

            rightButtonComponent.addComponent(minimiseButton);
            rightButtonComponent.addComponent(maximiseButton);
            rightButtonComponent.addComponent(closeButton);
        }

        IconButton createButton(final ButtonIcon icon, final Color rolloverBackground) {
            final IconButton button = new IconButton(icon, TITLE_HEIGHT);
            button.setRolloverBackground(rolloverBackground);
            button.setBorder(BUTTON_BORDER);
            return button;
        }

        IconButton createBlankButton() {
            return new IconButton(new BlankButtonIcon(), TITLE_HEIGHT);
        }

        @Override
        public void paintComponent(final Graphics g) {
            if (isOpaque()) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    private static final class ButtonComponent extends JComponent {

        final List<JComponent> buttonComponents = new ArrayList<>();

        ButtonComponent() {
            setOpaque(false);
        }

        void addComponent(final JComponent button) {
            buttonComponents.add(button);
            setLayout(new GridLayout(1, buttonComponents.size()));
            removeAll();
            buttonComponents.forEach(this::add);
            revalidate();
            repaint();
        }
    }

    private final class BRootFrame extends JRootPane {
        BRootFrame() {
            setBorder(new EmptyBorder(EDGE_SIZE, EDGE_SIZE, EDGE_SIZE, EDGE_SIZE));
            setOpaque(true);
            setBackground(DECORATION_BACKGROUND);
        }

        @Override
        public void paintComponent(final Graphics g1) {
            super.paintComponent(g1);
            final Graphics2D g = (Graphics2D) g1;
            g.setColor(OUTLINE_COLOR);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }
}
