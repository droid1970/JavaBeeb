package org.javabeeb.device;

import org.javabeeb.memory.FetchIntercept;
import org.javabeeb.util.SystemStatus;
import org.javabeeb.util.Util;

import java.util.Objects;

public abstract class AbstractMemoryMappedDevice implements MemoryMappedDevice {

    private final SystemStatus systemStatus;

    private final String name;

    private final int startAddress;
    private final int endAddress;

    protected boolean verbose;

    public AbstractMemoryMappedDevice(final SystemStatus systemStatus, final String name, final int startAddress, final int size) {
        this.systemStatus = Objects.requireNonNull(systemStatus);
        this.name = Objects.requireNonNull(name);
        this.startAddress = startAddress;
        this.endAddress = startAddress + size - 1;
    }

    public SystemStatus getSystemStatus() {
        return systemStatus;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public final boolean hasAddress(int address) {
        return (address >= startAddress && address <= endAddress);
    }

    @Override
    public int getMinAddress() {
        return startAddress;
    }

    @Override
    public int getMaxAddress() {
        return endAddress;
    }

    @Override
    public final int readByte(int address) {
        final int ret = readRegister(address - startAddress) & 0xFF;
        if (verbose) {
            Util.log(getName() + ": read register " + Util.formatHexByte(address) + " = " + ret, 0);
        }
        return ret;
    }

    @Override
    public final void writeByte(int address, int value) {
        if (verbose) {
            Util.log(getName() + ": write register " + Util.formatHexByte(address) + " = " + value, 0);
        }
        writeRegister(address - startAddress, value & 0xFF);
    }

    @Override
    public void installIntercept(int address, FetchIntercept intercept, boolean addRTS) {
        // Do nothing
    }

    @Override
    public void removeIntercept(int address) {
        // Do nothing
    }

    @Override
    public boolean processIntercepts(int address) {
        return false;
    }

    public abstract int readRegister(int index);
    public abstract void writeRegister(int index, int value);
}
