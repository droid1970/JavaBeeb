package org.javabeeb.device;

import org.javabeeb.util.SystemStatus;

public final class SheilaMemoryMappedDevice extends AbstractMemoryMappedDevice {

    public SheilaMemoryMappedDevice(final SystemStatus systemStatus) {
        super(systemStatus, "sheila", 0xFE00, 0x100);
    }

    @Override
    public int readRegister(int index) {
        return 0;
    }

    @Override
    public void writeRegister(int index, int value) {
        // Nothing to do here
    }
}
