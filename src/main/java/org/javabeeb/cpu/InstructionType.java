package org.javabeeb.cpu;

public enum InstructionType {
    IMPLIED,
    STACK,
    BRANCH,
    JUMP,
    READ,
    READ_MODIFY_WRITE,
    WRITE
}