package org.javabeeb.device;

import org.javabeeb.util.ScheduledTask;
import org.javabeeb.util.Scheduler;
import org.javabeeb.util.SystemStatus;

import java.util.Objects;

//
// Mostly based on  https://github.com/mattgodbolt/jsbeeb/adc.js by Matt Godbolt
//
public class ADC extends AbstractMemoryMappedDevice {

    private final SystemVIA systemVIA;

    private int status = 0x40;
    private int low = 0x00;
    private int high = 0x00;

    private final ScheduledTask task;

    public ADC(final SystemStatus systemStatus, final String name, final int startAddress, final Scheduler scheduler, final SystemVIA systemVIA) {
        super(systemStatus, name, startAddress, 32);
        this.systemVIA = Objects.requireNonNull(systemVIA);
        this.task = scheduler.newTask(this::onComplete);
    }

    private void onComplete() {
        // Intended to do nothing
        int val = 0x8000;
        this.status = (this.status & 0x0f) | 0x40 | ((val >>> 10) & 0x03);
        this.low = val & 0xff;
        this.high = (val >>> 8) & 0xff;
        this.systemVIA.setCB1(false);
    }

    @Override
    public int readRegister(int index) {
        switch (index & 3) {
            case 0:
                return this.status;
            case 1:
                return this.high;
            case 2:
                return this.low;
            default:
                break;
        }
        return 0x40;
    }

    @Override
    public void writeRegister(int index, int value) {
        if ((index & 3) != 0) {
            return;
        }
        this.task.cancel();
        this.task.schedule((value & 0x08) != 0 ? 20000 : 8000);
        this.status = (value & 0x0F) | 0x80;
        this.systemVIA.setCB1(true);
    }
}
