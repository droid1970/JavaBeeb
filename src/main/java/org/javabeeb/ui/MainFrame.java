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

    public MainFrame(final BBCMicro bbc, final SystemPalette systemPalette) {
        super("JavaBeeb");
        this.systemStatus = bbc.getSystemStatus();
        this.bbc = Objects.requireNonNull(bbc);
        this.systemVIA = bbc.getSystemVIA();
        createAndShowUI(bbc, systemPalette);
    }

    private void createAndShowUI(final BBCMicro bbc, final SystemPalette systemPalette) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final Screen screen = bbc.getScreen();
        final ScreenComponent screenComponent = new ScreenComponent(bbc, screen.getImageWidth(), screen.getImageHeight());
        screen.setScreenImageConsumer(screenComponent);
        getContentPane().setBackground(systemPalette.getColour(0));
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

        final ClockIcon clockIcon;
        final JLabel clockLabel;

        final JLabel screenLabel;
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

            //
            // Clock speed
            //
//            final JComboBox<ClockSpeed> speedCombo = new JComboBox<>(ClockSpeed.getStandardValues());
//            speedCombo.setPreferredSize(new Dimension(128, 18));
//            speedCombo.setMinimumSize(new Dimension(128, 18));
//            speedCombo.setMaximumSize(new Dimension(128, 18));
//            speedCombo.setSelectedItem(bbc.getClock().getClockSpeed());
//            speedCombo.addActionListener(e -> {
//                bbc.getClock().setClockSpeed(speedCombo.getItemAt(speedCombo.getSelectedIndex()));
//            });
//            add(Box.createRigidArea(new Dimension(4,0)));
//            add(speedCombo);

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
            clockIcon = new ClockIcon(12, 16, 1);
            clockIcon.setColour(Color.BLACK);
            clockLabel = createLabel();
            clockLabel.setHorizontalAlignment(JLabel.RIGHT);
            clockLabel.setIcon(clockIcon);
            clockLabel.setText("00.00 Mhz");
            clockLabel.setPreferredSize(clockLabel.getPreferredSize());
            clockLabel.setText("");

            add(clockLabel);

            capsLockLabel = createLabel();
            capsLockLabel.setText("caps");
            capsLockIcon = new LedIcon(12, 16, 1);
            capsLockIcon.setOn(bbc.getSystemVIA().isCapslockLightOn());
            capsLockLabel.setIcon(capsLockIcon);
            bbc.getSystemVIA().setCapsLockChangedCallback(() -> {
                capsLockIcon.setOn(bbc.getSystemVIA().isCapslockLightOn());
                capsLockLabel.repaint();
            });
            add(Box.createRigidArea(new Dimension(8,0)));
            add(capsLockLabel);

            screenLabel = createLabel();
            add(Box.createRigidArea(new Dimension(8,0)));
            add(screenLabel);

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
            final String mhzString = systemStatus.getString(SystemStatus.KEY_MILLION_CYCLES_PER_SECOND, "?");
            try {
                clockIcon.setRate(Double.parseDouble(mhzString));
            } catch (Exception ex) {
                // Ignored
            }
            clockLabel.setText(mhzString + " Mhz");
            screenLabel.setText("fps = " + Util.formatDouble(systemStatus.getDouble(SystemStatus.FRAMES_PER_SECOND, 0.0)));
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
