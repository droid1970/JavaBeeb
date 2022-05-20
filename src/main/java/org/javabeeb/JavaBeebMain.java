package org.javabeeb;

import org.javabeeb.ui.MainFrame;

import javax.swing.*;

public final class JavaBeebMain {

    public static void main(final String[] args) throws Exception {
        final BBCMicro bbc = new BBCMicro();
        SwingUtilities.invokeLater(() -> createAndShowUI(bbc));
        bbc.run(() -> false);
    }

    private static void createAndShowUI(final BBCMicro bbc) {
        new MainFrame(bbc);
    }
}
