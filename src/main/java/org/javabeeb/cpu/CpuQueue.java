package org.javabeeb.cpu;

// Fixed capacity Runnable queue
public final class CpuQueue {

    private static final int MAX_SIZE = 64; // Must be a power of 2 and >= 16
    private static final int MASK;
    static {
        if (MAX_SIZE < 16) {
            throw new IllegalStateException(MAX_SIZE + ": MAX_SIZE must be >= 16");
        }
        // Enforce that size is a power of 2
        final int shift = 32 - Integer.numberOfLeadingZeros(MAX_SIZE) - 1;
        if (((MAX_SIZE >>> shift) << shift) != MAX_SIZE) {
            throw new IllegalStateException(MAX_SIZE + ": MAX_SIZE must be a power of 2");
        }
        MASK = MAX_SIZE - 1;
    }

    private final Runnable[] queue = new Runnable[MAX_SIZE];

    private int size = 0;
    private int head = 0;
    private int tail = 0;

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(Runnable op) {
        if (size == MAX_SIZE) {
            throw new IllegalStateException("queue size exceeded");
        }
        queue[tail] = op;
        tail = (tail + 1) & MASK;
        size++;
    }

    public Runnable remove() {
        if (size == 0) {
            throw new IllegalStateException("queue is empty");
        }
        final Runnable ret = queue[head];
        head = (head + 1) & MASK;
        size--;
        return ret;
    }
}
