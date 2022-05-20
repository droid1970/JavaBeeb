package org.javabeeb;

import org.javabeeb.ui.MainFrame;

import javax.swing.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public final class JavaBeebMain {

    public static void main(final String[] args) throws Exception {
        final BBCMicro bbc = new BBCMicro();
        SwingUtilities.invokeLater(() -> createAndShowUI(bbc));
        final long startTime = System.nanoTime();
        bbc.run(() -> false);
        reportCyclesPerSecond(bbc.getCpu().getCycleCount(), System.nanoTime() - startTime);
    }

    private static void createAndShowUI(final BBCMicro bbc) {
        new MainFrame(bbc);
    }

    private static final NumberFormat FMT = new DecimalFormat("0.00");

    private static void reportCyclesPerSecond(final long cycleCount, final long duration) {
        final double seconds = (double) duration / 1_000_000_000L;
        final double cyclesPerSecond = cycleCount / seconds / 1000000.0;
        System.err.println("cycles = " + cycleCount + " secs = " + FMT.format(seconds) + " mega-cps = " + FMT.format(cyclesPerSecond));
    }
}
