package org.javabeeb.util;

public interface InterruptSource {
    String getName();
    boolean isIRQ();
    boolean isNMI();
}
