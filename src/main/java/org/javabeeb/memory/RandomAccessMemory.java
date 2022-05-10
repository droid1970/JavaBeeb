package org.javabeeb.memory;

import org.javabeeb.util.StateKey;

@StateKey(key = "randomAccessMemory")
public final class RandomAccessMemory extends AbstractMemory {
    public RandomAccessMemory(int start, int size) {
        super(start, size, false);
    }
}
