package org.javabeeb.disk;

import org.javabeeb.cpu.Cpu;
import org.javabeeb.device.AbstractMemoryMappedDevice;
import org.javabeeb.util.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FloppyDiskController extends AbstractMemoryMappedDevice implements InterruptSource {

    public static final int DISC_TIME_SLICE = 16 * 16;

    private static final int UNDEFINED_INT = -1;
    private static final int[] UNDEFINED_ARRAY = new int[]{};

    private int status = 0;
    private int curData = 0;
    private int result = 0;
    private int curCommand = 0xFF;
    private int curDrive = 0;
    private int drvout = 0;
    private int paramNum = 0;
    private int paramReq = 0;
    private boolean verify = false;
    private boolean written = false;
    private final int[] realTrack = new int[2];
    private final Disk[] drives = new Disk[2];
    private int phase;
    private final boolean[] motorOn = new boolean[2];
    private final int[] params = new int[8];
    private final int[] curTrack = new int[2];
    private int curSector = 0;
    private int sectorsLeft = 0;

    private final Scheduler scheduler;
    private Cpu cpu;
    private final ScheduledTask callbackTask;
    private final ScheduledTask[] motorSpinDownTask;

    public FloppyDiskController(SystemStatus systemStatus, final Scheduler scheduler, String name, int startAddress) {
        super(systemStatus, name, startAddress, 32);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.callbackTask = scheduler.newTask(this::callback);
        this.motorSpinDownTask = new ScheduledTask[2];
        this.motorSpinDownTask[0] = scheduler.newTask(() -> {
            this.motorOn[0] = false;
            this.drvout &= ~0x40;
        });
        this.motorSpinDownTask[1] = scheduler.newTask(() -> {
            this.motorOn[1] = false;
            this.drvout &= ~0x80;
        });
        this.drives[0] = new EmptyDisk(this);
        this.drives[1] = new EmptyDisk(this);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setCpu(final Cpu cpu) {
        this.cpu = cpu;
    }

    private void nmi() {
        cpu.requestNMI((status & 8) != 0);
    }

    public void load(final int driveIndex, final File file) throws IOException {
        final String name = file.getName();
        final int[] data = Util.readFileAsInts(file);
        final Disk disk = new BaseDisk(this, scheduler, name, data, null);
        drives[driveIndex] = disk;
    }

    @Override
    public boolean isIRQ() {
        return false;
    }

    @Override
    public int readRegister(int index) {
        int ret = 0;
        switch (index & 0x7) {
            case 0:
                ret = status;
                break;
            case 1:
                this.status &= ~0x18;
                nmi();
                ret = result;
                break;
            case 4:
            case 5:
            case 6:
            case 7:
                this.status &= ~0x0C;
                nmi();
                ret = this.curData;
                break;
        }
        Util.log("FDC: readRegister " + index + " value = " + Util.formatHexByte(ret), 0);
        return ret;
    }

    private void error(int result) {
        this.result = result;
        this.status = 0x18;
        nmi();
        callbackTask.cancel();
        this.setspindown();
    }

    void notFound() {
        this.error(0x18);
    }

    void writeProtect() {
        this.error(0x12);
    }

    private void headerCrcError() {
        this.error(0x0c);
    }

    private void dataCrcError() {
        this.error(0x0e);
    }

    void discData(int data) {
        if (this.verify) return;
        this.curData = data & 0xFF;
        this.status = 0x8c;
        nmi();
        this.result = 0;
    }

    int readDiscData(boolean last) {
        if (!this.written) return 0x00;
        if (!last) {
            this.status = 0x8c;
            this.result = 0;
            nmi();
        }
        this.written = false;
        return this.curData;
    }

    void discFinishRead() {
        callbackTask.reschedule(DISC_TIME_SLICE);
    }

    private static final Map<Integer, Integer> PARAMS_MAP = new HashMap<>();
    static {
        PARAMS_MAP.put(0x35, 4);
        PARAMS_MAP.put(0x29, 1);
        PARAMS_MAP.put(0x2C, 0);
        PARAMS_MAP.put(0x3D, 1);
        PARAMS_MAP.put(0x3A, 2);
        PARAMS_MAP.put(0x13, 3);
        PARAMS_MAP.put(0x0B, 3);
        PARAMS_MAP.put(0x1B, 3);
        PARAMS_MAP.put(0x1F, 3);
        PARAMS_MAP.put(0x23, 5);
    }

    private Integer numParams(int command) {
        return PARAMS_MAP.get(command);
    }

    private void command(int val) {
        if ((this.status & 0x80) != 0) {
            return;
        }

        setCurCommand(val & 0x3f);
        if (curCommand == 0x17) {
            setCurCommand(0x13);
        }
        this.curDrive = ((val & 0x80) != 0) ? 1 : 0;
        if (curCommand < 0x2c) {
            this.drvout &= ~(0x80 | 0x40);
            this.drvout |= (val & (0x80 | 0x40));
        }
        this.paramNum = 0;
        final Integer num = this.numParams(curCommand);
        this.paramReq = (num == null) ? 0 : num;
        this.status = 0x80;
        if (num != null) {
            if (curCommand == 0x2c) {
                // read drive status
                this.status = 0x10;
                this.result = 0x80;
                this.result |= ((this.realTrack[this.curDrive] != 0) ? 0 : 2);
                this.result |= (this.drives[this.curDrive].writeProt() ? 0x08 : 0);
                if ((this.drvout & 0x40) != 0) {
                    this.result |= 0x04;
                }
                if ((this.drvout & 0x80) != 0) {
                    this.result |= 0x40;
                }
            } else {
                this.result = 0x18;
                this.status = 0x18;
                nmi();
            }
        }
    }

    private void writeSpecial(int reg, int val) {
        this.status = 0;
        switch (reg) {
            case 0x17:
                break; // apparently "mode register"
            case 0x12:
                this.curTrack[0] = val;
                break;
            case 0x1a:
                this.curTrack[1] = val;
                break;
            case 0x23:
                this.drvout = val;
                break;
            default:
                this.result = this.status = 0x18;
                nmi();
                break;
        }
    }

    private void readSpecial(int reg) {
        this.status = 0x10;
        this.result = 0;
        switch (reg) {
            case 0x06:
                break;
            case 0x12:
                this.result = this.curTrack[0];
                break;
            case 0x1a:
                this.result = this.curTrack[1];
                break;
            case 0x23:
                this.result = this.drvout;
                break;
            default:
                this.result = this.status = 0x18;
                nmi();
                break;
        }
    }

    private void spinup() {

        int time = DISC_TIME_SLICE;

        if (!this.motorOn[this.curDrive]) {
            // Half a second.
            time = 1_000_000;
            this.motorOn[this.curDrive] = true;
            //this.noise.spinUp();
        }

        this.callbackTask.reschedule(time);
        this.motorSpinDownTask[this.curDrive].cancel();
        this.phase = 0;
    }

    private void setspindown() {
        if (this.motorOn[this.curDrive]) {
            this.motorSpinDownTask[this.curDrive].reschedule(4_000_000);
        }
    }

    private void seek(int track) {
        int realTrack = this.realTrack[this.curDrive];
        realTrack += (track - this.curTrack[this.curDrive]);
        if (realTrack < 0)
            realTrack = 0;
        if (realTrack > 79) {
            realTrack = 79;
        }
        this.realTrack[this.curDrive] = realTrack;
        final int diff = this.drives[this.curDrive].seek(realTrack);
        // Let disc noises overlap by ~10%
        //final int seekLen = (this.noise.seek(diff) * 0.9 * this.cpu.peripheralCyclesPerSecond) | 0;
        this.callbackTask.reschedule(250_000);//2_000_000);//DiscTimeSlice);//Math.max(DiscTimeSlice, 10_000));
        this.phase = 1;
    }

    private void prepareSectorIO(int track, int sector, int numSectors) {
        if (numSectors != UNDEFINED_INT) {
            this.sectorsLeft = numSectors & 31;
        }
        if (sector != UNDEFINED_INT) {
            this.curSector = sector;
        }
        this.spinup(); // State: spinup -> seek.
    }

    private void parameter(int val) {
        if (this.paramNum < 5) {
            this.params[this.paramNum++] = val;
        }
        if (this.paramNum != this.paramReq) {
            return;
        }
        switch (curCommand) {
            case 0x35: // Specify.
                this.status = 0;
                break;
            case 0x29: // Seek
                this.spinup(); // State: spinup -> seek.
                break;
            case 0x1f: // Verify
            case 0x13: // Read
            case 0x0b: // Write
                this.prepareSectorIO(this.params[0], this.params[1], this.params[2]);
                break;
            case 0x1b: // Read ID
                this.prepareSectorIO(this.params[0], UNDEFINED_INT, this.params[2]);
                break;
            case 0x23: // Format
                this.prepareSectorIO(this.params[0], UNDEFINED_INT, UNDEFINED_INT);
                break;
            case 0x3a: // Special register write
                this.writeSpecial(this.params[0], this.params[1]);
                break;
            case 0x3d: // Special register read
                this.readSpecial(this.params[0]);
                break;
            default:
                this.result = 0x18;
                this.status = 0x18;
                nmi();
                break;
        }
    }

    private void reset(final int cmd) {

    }

    private void data(int val) {
        this.curData = val;
        this.written = true;
        this.status &= ~0x0c;
        // TODO: Should nmi() clear as well as set?
        nmi();
    }

    @Override
    public void writeRegister(int index, int value) {
        Util.log("FDC: writeRegister " + index + " value = " + Util.formatHexByte(value), 0);
        switch (index & 7) {
            case 0:
                this.command(value);
                break;
            case 1:
                this.parameter(value);
                break;
            case 2:
                this.reset(value);
                break;
            case 4:
            case 5:
            case 6:
            case 7:
                this.data(value);
                break;
        }
    }

    private boolean density() {
        return (this.drvout & 0x20) != 0;
    }

    private void update(int status) {
        this.status = status;
        this.result = 0;
        nmi();
    }

    private void done() {
        this.update(0x18);
        this.setspindown();
        this.verify = false;
    }

    private void setCurCommand(final int n) {
        this.curCommand = n;
        Util.log("FDC : curCommand = " + Util.formatHexByte(n), 0);
    }

    private void callback() {
        Util.log("FDC : callback: phase = " + phase + " cmd = " + Util.formatHexByte(curCommand), 0);
        if (this.phase == 0) {
            // Spinup complete.
            this.seek(this.params[0]);
            return;
        }

        switch (curCommand) {
            case 0x29: // Seek
                this.curTrack[this.curDrive] = this.params[0];
                this.done();
                break;

            case 0x0b: // Write
                if (this.phase == 1) {
                    this.curTrack[this.curDrive] = this.params[0];
                    this.phase = 2;
                    this.drives[this.curDrive].write(this.curSector, this.params[0], 0, this.density());
                    this.update(0x8c);
                    return;
                }
                if (--this.sectorsLeft == 0) {
                    this.done();
                    return;
                }
                this.curSector++;
                this.drives[this.curDrive].write(this.curSector, this.params[0], 0, this.density());
                this.update(0x8c);
                break;

            case 0x13: // Read
            case 0x1f: // Verify
                if (this.phase == 1) {
                    this.curTrack[this.curDrive] = this.params[0];
                    this.phase = 2;
                    this.drives[this.curDrive].read(this.curSector, this.params[0], 0, this.density());
                    return;
                }
                if (--this.sectorsLeft == 0) {
                    this.done();
                    return;
                }
                this.curSector++;
                this.drives[this.curDrive].read(this.curSector, this.params[0], 0, this.density());
                break;

            case 0x1b: // Read ID
                if (this.phase == 1) {
                    this.curTrack[this.curDrive] = this.params[0];
                    this.phase = 2;
                    this.drives[this.curDrive].address(this.params[0], 0, this.density());
                    return;
                }
                if (--this.sectorsLeft == 0) {
                    this.done();
                    return;
                }
                this.drives[this.curDrive].address(this.params[0], 0, this.density());
                break;

            case 0x23: // Format
                switch (this.phase) {
                    case 1:
                        this.curTrack[this.curDrive] = this.params[0];
                        this.drives[this.curDrive].write(this.curSector, this.params[0], 0, this.density());
                        this.update(0x8c);
                        this.phase = 2;
                        break;
                    case 2:
                        this.drives[this.curDrive].format(this.params[0], 0, this.density());
                        this.phase = 3;
                        break;
                    case 3:
                        this.done();
                        break;
                }
                break;

            case 0xff:
                break;
            default:
                Util.log("ERK bad command: " + Util.formatHexByte(this.curCommand), 0);
                break;
        }
    }
}
