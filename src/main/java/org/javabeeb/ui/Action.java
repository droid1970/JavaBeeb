package org.javabeeb.ui;

import org.javabeeb.BBCMicro;

public interface Action {
    String getText();
    boolean isEnabled();
    boolean isSelected();
    void performAction(BBCMicro bbc);
}
