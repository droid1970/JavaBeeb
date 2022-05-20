package org.javabeeb.ui;

import org.javabeeb.BBCMicro;
import org.javabeeb.device.SystemVIA;
import org.javabeeb.screen.Screen;
import org.javabeeb.screen.SystemPalette;
import org.javabeeb.util.SystemStatus;
import org.javabeeb.util.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;

public class MainFrame extends JFrame {

    private final SystemStatus systemStatus;
    private final BBCMicro bbc;
    private final SystemVIA systemVIA;

    public MainFrame(final BBCMicro bbc) {
        super("JavaBeeb");
        this.systemStatus = bbc.getSystemStatus();
        this.bbc = Objects.requireNonNull(bbc);
        this.systemVIA = bbc.getSystemVIA();
        createAndShowUI(bbc);
    }

    private void createAndShowUI(final BBCMicro bbc) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final Screen screen = bbc.getScreen();
        final ScreenComponent screenComponent = new ScreenComponent(bbc, screen.getImageWidth(), screen.getImageHeight());
        screen.setScreenImageConsumer(screenComponent);
        getContentPane().setBackground(bbc.getPalette().getColour(0));
        getContentPane().add(BorderLayout.CENTER, screenComponent);

        final StatusBar statusBar = new StatusBar();
        getContentPane().add(BorderLayout.SOUTH, statusBar);

        final Timer refreshTimer = new Timer(1000, e -> statusBar.refresh());
        refreshTimer.start();

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private final class StatusBar extends JComponent {

        final RateIcon clockIcon;
        final RateIcon fpsIcon;
        final JLabel clockLabel;

        final JLabel fpsLabel;
        final JLabel capsLockLabel;
        final LedIcon capsLockIcon;
        boolean verbose = false;

        StatusBar() {
            setOpaque(true);
            setBackground(Color.DARK_GRAY);
            setBorder(new EmptyBorder(4, 8, 8, 4));
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

            final var saveStateButton = createButton("SAVE");
            saveStateButton.addActionListener(e -> {
                bbc.saveState();
            });
            add(saveStateButton);

            final var restoreStateButton = createButton("RESTORE");
            restoreStateButton.addActionListener(e -> {
                bbc.restoreState();
            });
            add(Box.createRigidArea(new Dimension(4,0)));
            add(restoreStateButton);

            final var verboseCheckbox = createCheckbox("verbose");
            verboseCheckbox.addActionListener(e -> {
                verbose = !verbose;
                bbc.getCpu().setVerboseCondition(verbose ? () ->true : () -> false);
                bbc.getCpu().setFetchDelayMillis(verbose ? 1 : 0);
                bbc.getCpu().setFetchDelayCondition(verbose ? cpu -> cpu.getPC() < 0x8000 : null);
            });
            add(Box.createRigidArea(new Dimension(4,0)));
            add(verboseCheckbox);

            final JCheckBox keyMapCheckbox = createCheckbox("logical map");
            keyMapCheckbox.setSelected(false);
            keyMapCheckbox.addActionListener(e -> {
                systemVIA.swapKeyMap();
            });
            add(keyMapCheckbox);
            add(Box.createGlue());

            //
            // Labels
            //
            add(Box.createRigidArea(new Dimension(8,0)));
            clockIcon = new RateIcon(12, 16, 2.0);
            clockIcon.setColour(Color.BLACK);
            clockLabel = createLabel();
            clockLabel.setHorizontalAlignment(JLabel.RIGHT);
            clockLabel.setIcon(clockIcon);
            clockLabel.setText("00.00 mhz");
            clockLabel.setPreferredSize(clockLabel.getPreferredSize());
            clockLabel.setText("");
            add(clockLabel);

            fpsIcon = new RateIcon(12, 16, 50.0);
            fpsIcon.setColour(Color.BLACK);
            fpsLabel = createLabel();
            fpsLabel.setHorizontalAlignment(JLabel.RIGHT);
            fpsLabel.setIcon(fpsIcon);
            fpsLabel.setText("99.99 fps");
            fpsLabel.setPreferredSize(fpsLabel.getPreferredSize());
            add(Box.createRigidArea(new Dimension(8,0)));
            add(fpsLabel);

            capsLockLabel = createLabel();
            capsLockLabel.setText("caps");
            capsLockIcon = new LedIcon(12, 16);
            capsLockIcon.setOn(bbc.getSystemVIA().isCapslockLightOn());
            capsLockLabel.setIcon(capsLockIcon);
            bbc.getSystemVIA().setCapsLockChangedCallback(() -> {
                capsLockIcon.setOn(bbc.getSystemVIA().isCapslockLightOn());
                capsLockLabel.repaint();
            });
            add(Box.createRigidArea(new Dimension(8,0)));
            add(capsLockLabel);

            final Dimension currentPreferredSize = getPreferredSize();
            setPreferredSize(new Dimension(currentPreferredSize.width, currentPreferredSize.height - 6));
        }

        JButton createButton(final String text) {
            final JButton button = new JButton(text);
            button.setFocusable(false);
            button.setForeground(Color.BLACK);
            button.setContentAreaFilled(true);
            return button;
        }

        JCheckBox createCheckbox(final String text) {
            final JCheckBox button = new JCheckBox(text);
            button.setFocusable(false);
            button.setForeground(Color.WHITE);
            button.setContentAreaFilled(false);
            return button;
        }

        JLabel createLabel() {
            final JLabel label = new JLabel();
            label.setOpaque(false);
            label.setForeground(Color.LIGHT_GRAY);
            return label;
        }

        void refresh() {
            final double mhz = systemStatus.getDouble(SystemStatus.KEY_MHZ, 0.0);
            final double fps = systemStatus.getDouble(SystemStatus.KEY_FPS, 0.0);
            clockIcon.setRate(mhz);
            clockLabel.setText(Util.formatDouble(mhz) + " mhz");
            fpsIcon.setRate(fps);
            fpsLabel.setText(Util.formatDouble(fps) + " fps");
        }

        @Override
        public void paintComponent(final Graphics g) {
            if (isOpaque()) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }
}
