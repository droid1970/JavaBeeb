package org.javabeeb.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import org.javabeeb.ui.icon.BlankButtonIcon;
import org.javabeeb.ui.icon.ButtonIcon;
import org.javabeeb.ui.icon.CloseWindowIcon;
import org.javabeeb.ui.icon.MaximiseWindowIcon;
import org.javabeeb.ui.icon.MinimiseWindowIcon;
import org.javabeeb.ui.icon.SettingsIcon;

public class BFrame extends JFrame {

    public static final Color DECORATION_BACKGROUND = new Color(80, 90, 116);
    public static final Color OUTLINE_COLOR = Color.GRAY;

    private static final Border BUTTON_BORDER = new EmptyBorder(0, 4, 0, 4);
    private static final Color BUTTON_ROLLOVER_BACKGROUND = new Color(255, 255, 255, 32);
    private static final Color CLOSE_BUTTON_ROLLOVER_BACKGROUND = Color.RED;
    private static final Color MENU_BUTTON_ROLLOVER_BACKGROUND = new Color(0, 128, 255, 192);

    private static final int EDGE_SIZE = 6;
    private static final int CORNER_SIZE = 12;
    private static final int TITLE_HEIGHT = 18;


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

    private enum FrameArea {
        TITLE,
        TOP,
        LEFT,
        BOTTOM,
        RIGHT,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private final class BRootFrame extends JRootPane {

        boolean dragging;
        FrameArea pressArea;
        Point pressPoint;
        Dimension pressSize;
        Point pressLocation;

        BRootFrame() {
            setBorder(new EmptyBorder(EDGE_SIZE, EDGE_SIZE, EDGE_SIZE, EDGE_SIZE));
            setOpaque(true);
            setBackground(DECORATION_BACKGROUND);

            final MouseAdapter mouseHandler = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                        swapMaximised();
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        dragging = true;
                        pressPoint = e.getLocationOnScreen();
                        pressArea = computeFrameArea(e.getPoint());
                        pressSize = BFrame.this.getSize();
                        pressLocation = BFrame.this.getLocationOnScreen();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        dragging = false;
                        pressPoint = null;
                        pressArea = null;
                        pressSize = null;
                        pressLocation = null;
                        setCursor(Cursor.getDefaultCursor());
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragging) {
                        int dx = e.getLocationOnScreen().x - pressPoint.x;
                        int dy = e.getLocationOnScreen().y - pressPoint.y;
                        switch (pressArea) {
                            case TITLE:
                                BFrame.this.setBounds(pressLocation.x + dx, pressLocation.y + dy, pressSize.width, pressSize.height);
                                break;

                            case TOP:
                                BFrame.this.setBounds(pressLocation.x, pressLocation.y + dy, pressSize.width, pressSize.height - dy);
                                break;

                            case LEFT:
                                BFrame.this.setBounds(pressLocation.x + dx, pressLocation.y, pressSize.width - dx, pressSize.height);
                                break;

                            case BOTTOM:
                                BFrame.this.setBounds(pressLocation.x, pressLocation.y, pressSize.width, pressSize.height + dy);
                                break;

                            case RIGHT:
                                BFrame.this.setBounds(pressLocation.x, pressLocation.y, pressSize.width + dx, pressSize.height);
                                break;

                            case TOP_LEFT:
                                BFrame.this.setBounds(pressLocation.x + dx, pressLocation.y + dy, pressSize.width - dx, pressSize.height - dy);
                                break;

                            case TOP_RIGHT:
                                BFrame.this.setBounds(pressLocation.x, pressLocation.y + dy, pressSize.width + dx, pressSize.height - dy);
                                break;

                            case BOTTOM_LEFT:
                                BFrame.this.setBounds(pressLocation.x + dx, pressLocation.y, pressSize.width - dx, pressSize.height + dy);
                                break;

                            case BOTTOM_RIGHT:
                                BFrame.this.setBounds(pressLocation.x, pressLocation.y, pressSize.width + dx, pressSize.height + dy);
                                break;
                        }
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    final FrameArea area = computeFrameArea(e.getPoint());
                    setCursorForArea(area);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setCursor(Cursor.getDefaultCursor());
                }
            };
            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        private void setCursorForArea(final FrameArea area) {
            switch (area) {
                case TOP:
                    setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                    break;

                case LEFT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                    break;

                case BOTTOM:
                    setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                    break;

                case RIGHT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    break;

                case TOP_LEFT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                    break;

                case TOP_RIGHT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                    break;

                case BOTTOM_LEFT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                    break;

                case BOTTOM_RIGHT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    break;

                default:
                    setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        public void paintComponent(final Graphics g1) {
            super.paintComponent(g1);
            final Graphics2D g = (Graphics2D) g1;
            g.setColor(OUTLINE_COLOR);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(getBackground());
            g.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
        }

        private FrameArea computeFrameArea(final Point p) {
            if (p.x < EDGE_SIZE) {
                if (p.y < CORNER_SIZE) {
                    return FrameArea.TOP_LEFT;
                }
                if (p.y > getHeight() - CORNER_SIZE) {
                    return FrameArea.BOTTOM_LEFT;
                }
                return FrameArea.LEFT;
            }
            if (p.x > getWidth() - EDGE_SIZE) {
                if (p.y < CORNER_SIZE) {
                    return FrameArea.TOP_RIGHT;
                }
                if (p.y > getHeight() - CORNER_SIZE) {
                    return FrameArea.BOTTOM_RIGHT;
                }
                return FrameArea.RIGHT;
            }
            if (p.y < EDGE_SIZE) {
                if (p.x < CORNER_SIZE) {
                    return FrameArea.TOP_LEFT;
                }
                if (p.x > getWidth() - CORNER_SIZE) {
                    return FrameArea.TOP_RIGHT;
                }
                return FrameArea.TOP;
            }
            if (p.y > getHeight() - EDGE_SIZE) {
                if (p.x < CORNER_SIZE) {
                    return FrameArea.BOTTOM_LEFT;
                }
                if (p.x > getWidth() - CORNER_SIZE) {
                    return FrameArea.BOTTOM_RIGHT;
                }
                return FrameArea.BOTTOM;
            }
            return FrameArea.TITLE;
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
            titleLabel = new JLabel(getTitle());
            titleLabel.setForeground(Color.LIGHT_GRAY);
            titleLabel.setOpaque(false);
            titleLabel.setHorizontalAlignment(JLabel.CENTER);
            add(leftButtonComponent, BorderLayout.WEST);
            add(titleLabel, BorderLayout.CENTER);
            add(rightButtonComponent, BorderLayout.EAST);

            final IconButton settingsButton = createButton(new SettingsIcon(), MENU_BUTTON_ROLLOVER_BACKGROUND);

            final IconButton minimiseButton = createButton(new MinimiseWindowIcon(), BUTTON_ROLLOVER_BACKGROUND);
            minimiseButton.addActionListener(e -> setExtendedState(JFrame.ICONIFIED));

            final IconButton maximiseButton = createButton(new MaximiseWindowIcon(), BUTTON_ROLLOVER_BACKGROUND);
            maximiseButton.addActionListener(e -> swapMaximised());

            final IconButton closeButton = createButton(new CloseWindowIcon(), CLOSE_BUTTON_ROLLOVER_BACKGROUND);
            closeButton.addActionListener(e -> close());

            leftButtonComponent.addComponent(settingsButton);
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
}
