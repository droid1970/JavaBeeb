package org.javabeeb.ui;

import org.javabeeb.BBCMicro;
import org.javabeeb.device.SystemVIA;
import org.javabeeb.screen.ScreenImageConsumer;
import org.javabeeb.screen.SystemPalette;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public final class ScreenComponent extends JComponent implements ScreenImageConsumer {

    private static final String PAUSED_TEXT = "PAUSED";
    private static final Color PAUSED_OVERLAY = new Color(255, 255, 255, 92);
    private static final int IMAGE_BORDER_SIZE = 32;

    private final BBCMicro bbc;
    private final SystemVIA systemVIA;
    private final SystemPalette palette;

    private final JLabel pausedLabel;
    private final List<IntConsumer> keyUpListeners = new ArrayList<>();
    private final List<BiConsumer<Integer, Boolean>> keyDownListeners = new ArrayList<>();

    private BufferedImage image;
    private boolean paused;
    private Point imageOrigin;

    private Timer disableCursorTimer;

    public ScreenComponent(final BBCMicro bbc, final int imageWidth, final int imageHeight) {
        this.bbc = bbc;
        this.systemVIA = bbc.getSystemVIA();
        this.palette = Objects.requireNonNull(bbc.getPalette());
        setOpaque(false);
        setBackground(bbc.getPalette().getColour(0));
        setPreferredSize(new Dimension(imageWidth + IMAGE_BORDER_SIZE * 2, imageHeight + IMAGE_BORDER_SIZE * 2));
        this.pausedLabel = new JLabel(PAUSED_TEXT);
        this.pausedLabel.setOpaque(false);
        this.pausedLabel.setForeground(new Color(255, 255, 255, 128));
        this.pausedLabel.setHorizontalAlignment(JLabel.CENTER);
        this.pausedLabel.setFont(pausedLabel.getFont().deriveFont(48.0f));
        disableCursor();
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                enableCursor(2000);
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                requestFocus();
            }
        });
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                bbc.getClock().setPaused(false);
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                bbc.getClock().setPaused(true);
                repaint();
            }
        });
        setFocusable(true);
        SwingUtilities.invokeLater(this::requestFocus);
        addKeyListener(new KeyHandler());
        addKeyUpListener(systemVIA::keyUp);
        addKeyDownListener(systemVIA::keyDown);
    }

    @Override
    public void setImage(final BufferedImage image, final Point imageOrigin) {
        this.image = image;
        this.imageOrigin = imageOrigin;
        repaint();
    }

    @Override
    public void setPaused(final boolean paused) {
        if (this.paused != paused) {
            this.paused = paused;
            repaint();
        }
    }

    public void addKeyUpListener(IntConsumer l) {
        keyUpListeners.add(l);
    }

    public void addKeyDownListener(BiConsumer<Integer, Boolean> l) {
        keyDownListeners.add(l);
    }

    @Override
    public void paintComponent(Graphics g) {
        final long startTime = System.nanoTime();
        final Rectangle r = SwingUtilities.calculateInnerArea(this, null);
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        if (image != null) {
            final int iw = image.getWidth() + IMAGE_BORDER_SIZE * 2;
            final int ih = image.getHeight() + IMAGE_BORDER_SIZE * 2;

            final int rw = r.width;
            final int rh = r.height;
            final double raspect = (double) rw / rh;
            final double iaspect = (double) iw / ih;
            final int px;
            final int py;
            final int pw;
            final int ph;
            if (raspect < iaspect) {
                pw = rw;
                ph = (int) (pw / iaspect);
                px = r.x;
                py = r.y + (rh - ph) / 2;
            } else {
                ph = rh;
                pw = (int) (iaspect * ph);
                px = r.x + (rw - pw) / 2;
                py = r.y;
            }
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            final Rectangle imageRect = new Rectangle(px + IMAGE_BORDER_SIZE, py + IMAGE_BORDER_SIZE, pw - IMAGE_BORDER_SIZE * 2, ph - IMAGE_BORDER_SIZE * 2);
            g.setColor(palette.getColour(0));
            g.fillRect(imageRect.x - IMAGE_BORDER_SIZE / 2, imageRect.y - IMAGE_BORDER_SIZE / 2, imageRect.width + IMAGE_BORDER_SIZE, imageRect.height + IMAGE_BORDER_SIZE);
            final int offsetX = (imageOrigin == null) ? 0 : imageOrigin.x;
            final int offsetY = (imageOrigin == null) ? 0 : imageOrigin.y;
            g.drawImage(image, offsetX + imageRect.x, offsetY + imageRect.y, imageRect.width, imageRect.height, null);
            if (paused) {
                g.setColor(PAUSED_OVERLAY);
                g.fillRect(0, 0, getWidth(), getHeight());
                SwingUtilities.paintComponent(g, pausedLabel, this, 0, 0, getWidth(), getHeight());
            }
        }
    }

    private void stopDisableCursorTimer() {
        if (disableCursorTimer != null) {
            disableCursorTimer.stop();
            disableCursorTimer = null;
        }
    }

    private void startDisableCursorTimer(final int delay) {
        stopDisableCursorTimer();
        disableCursorTimer = new Timer(delay, e -> {
            disableCursor();
        });
        disableCursorTimer.setRepeats(false);
        disableCursorTimer.start();
    }

    private void enableCursor(final int enableTime) {
        setCursor(Cursor.getDefaultCursor());
        startDisableCursorTimer(enableTime);
    }

    private void disableCursor() {
        setCursor( getToolkit().createCustomCursor(
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
                new Point(),
                null)
        );
    }

    private final class KeyHandler extends KeyAdapter {

        private final HashSet<Integer> pressedKeys = new HashSet<>();

        @Override
        public void keyPressed(KeyEvent e) {
            final int code = e.getKeyCode();
            if (e.getKeyCode() == KeyEvent.VK_F11) {
                bbc.getCpu().requestReset(true);
                return;
            }
            if (!pressedKeys.contains(code)) {
                pressedKeys.add(code);
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {
                    final String text = getClipboardText();
                    if (text != null && !text.isEmpty()) {
                        typeText(text);
                    }
                } else {
                    keyDownListeners.forEach(l -> l.accept(code, e.isShiftDown()));
                }
            }
        }

        private void typeText(final String text) {
            systemVIA.keyUp(KeyEvent.VK_CONTROL);
            final Queue<Runnable> runnables = new LinkedList<>();
            for (char c : text.toCharArray()) {
                runnables.add(() -> systemVIA.characterDown(c));
                runnables.add(() -> systemVIA.characterUp(c));
            }
            final AtomicReference<Timer> timerRef = new AtomicReference<>();
            final Timer timer = new Timer(50, e -> {
                final Runnable r = runnables.poll();
                if (r != null) {
                    r.run();
                } else {
                    timerRef.get().stop();
                }
            });
            timerRef.set(timer);
            timer.start();
        }

        @Override
        public void keyReleased(KeyEvent e) {
            final int code = e.getKeyCode();
            pressedKeys.remove(code);
            keyUpListeners.forEach(l -> l.accept(code));
        }
    }

    private static String getClipboardText() {
        String ret = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasStringText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasStringText) {
            try {
                ret = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException ex) {
                ret = "";
            }
        }
        return ret;
    }
}
