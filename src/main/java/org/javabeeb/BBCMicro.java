package org.javabeeb;

import org.javabeeb.clock.Clock;
import org.javabeeb.clock.ClockDefinition;
import org.javabeeb.cpu.Cpu;
import org.javabeeb.device.*;
import org.javabeeb.disk.FloppyDiskController;
import org.javabeeb.localfs.FilingSystem;
import org.javabeeb.localfs.LocalFilingSystem;
import org.javabeeb.memory.*;
import org.javabeeb.screen.Screen;
import org.javabeeb.screen.SystemPalette;
import org.javabeeb.sound.SoundChip;
import org.javabeeb.util.*;

import java.io.File;
import java.util.*;
import java.util.function.BooleanSupplier;

public final class BBCMicro implements InterruptSource {

    public static final File HOME = new File(System.getProperty("user.home"), "jbeeb");
    public static final File ROMS = new File(HOME, "roms");
    public static final File STATE = new File(HOME, "state");
    public static final File FILES = new File(HOME, "files");

    static {
        HOME.mkdirs();
        ROMS.mkdirs();
        STATE.mkdirs();
        FILES.mkdirs();
    }

    private static final boolean INSTALL_DFS = false;

    private static final File STATE_FILE = new File(System.getProperty("user.home"), "state.bbc");
    private static final String BASIC_ROM_RESOURCE_NAME = "/roms/BASIC2.rom";
    private static final String DFS_ROM_RESOURCE_NAME = "/roms/DFS-1.2.rom";
    private static final String OS_ROM_RESOURCE_NAME = "/roms/OS-1.2.rom";

    private static final int SHEILA = 0xFE00;

    private InterruptSource[] interruptSources = new InterruptSource[0];
    private final SystemStatus systemStatus;

    private final Screen screen;
    private final VideoULA videoULA;
    private final SystemVIA systemVIA;
    private final SoundChip soundChip;
    private final UserVIA userVIA;
    private final Crtc6845 crtc6845;
    private final ADC adc;
    private final FloppyDiskController fdc;
    private final PagedRomSelect pagedRomSelect;
    private final RandomAccessMemory ram;
    private final Memory memory;
    private final SystemPalette palette = SystemPalette.DEFAULT;

    private final Cpu cpu;

    private final Clock clock;

    public BBCMicro() throws Exception {

        this.systemStatus = new SystemStatus();

        this.videoULA = new VideoULA(
                systemStatus,
                palette,
                "Video ULA",
                SHEILA + 0x20
        );

        this.soundChip = SystemVIA.createSoundChip();

        this.systemVIA = new SystemVIA(
                systemStatus,
                soundChip,
                "System VIA",
                SHEILA + 0x40, 32
        );

        this.userVIA = new UserVIA(
                systemStatus,
                "User VIA",
                SHEILA + 0x60,
                32
        );

        this.crtc6845 = new Crtc6845(
                systemStatus,
                "CRTC 6845",
                SHEILA,
                systemVIA
        );

        final Scheduler scheduler = new DefaultScheduler();
        this.adc = new ADC(
                systemStatus,
                "ADC",
                SHEILA + 0xC0,
                scheduler,
                systemVIA
        );

        this.fdc = (INSTALL_DFS) ? new FloppyDiskController(systemStatus, scheduler, "FDC8271", SHEILA + 0x80) : null;

        this.pagedRomSelect = new PagedRomSelect(systemStatus, "Paged ROM", SHEILA + 0x30, 1);

        final List<MemoryMappedDevice> devices = new ArrayList<>();
        devices.add(videoULA);
        devices.add(systemVIA);
        devices.add(crtc6845);
        devices.add(userVIA);
        devices.add(adc);
        if (fdc != null) {
            devices.add(fdc);
        }
        devices.add(pagedRomSelect);

        // Always add this one at the end
        devices.add(new SheilaMemoryMappedDevice(systemStatus));

        final ReadOnlyMemory basicRom = ReadOnlyMemory.fromResource(0x8000, BASIC_ROM_RESOURCE_NAME);
        final ReadOnlyMemory dfsRom = ReadOnlyMemory.fromResource(0x8000, DFS_ROM_RESOURCE_NAME);
        final FilingSystem filingSystemROM = new LocalFilingSystem("Local DFS", "(C) Ian T 2022");
        final Map<Integer, ReadOnlyMemory> roms = new HashMap<>();

        // Install BASIC ROM
        roms.put(15, basicRom);

        // Install Filing System ROM
        if (INSTALL_DFS) {
            roms.put(12, dfsRom);
        } else {
            roms.put(12, filingSystemROM);
        }

        final PagedROM pagedROM = new PagedROM(0x8000, 16_384, pagedRomSelect, roms);
        final Memory osRom = ReadOnlyMemory.fromResource(0xC000, OS_ROM_RESOURCE_NAME);
        this.ram = new RandomAccessMemory(0, 32768);

        this.memory = Memory.bbcMicroB(devices, ram, pagedROM, osRom);

        this.screen = new Screen(
                systemStatus,
                this,
                memory,
                videoULA,
                crtc6845,
                systemVIA
        );

        crtc6845.addNewFrameListener(screen::newFrame);

        this.cpu = new Cpu(systemStatus, scheduler, memory);
        filingSystemROM.initialise(cpu, memory);

        cpu.setVerboseCondition(() -> false);
        if (fdc != null) {
            this.fdc.setCpu(cpu);
        }
        this.clock = new Clock(
                systemStatus,
                ClockDefinition.CR200,
                Long.MAX_VALUE,
                Arrays.asList(cpu, systemVIA, userVIA, crtc6845, screen)
        );
        addInterruptSource(crtc6845);
        addInterruptSource(systemVIA);
        addInterruptSource(userVIA);
        addInterruptSource(videoULA);
        if (fdc != null) {
            addInterruptSource(fdc);
        }
        cpu.setInterruptSource(this);
    }

    public SystemStatus getSystemStatus() {
        return systemStatus;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public Memory getMemory() {
        return memory;
    }

    public Crtc6845 getCrtc6845() {
        return crtc6845;
    }

    public SystemVIA getSystemVIA() {
        return systemVIA;
    }

    public VideoULA getVideoULA() {
        return videoULA;
    }

    public Screen getScreen() {
        return screen;
    }

    public SystemPalette getPalette() {
        return palette;
    }

    private State savedState;

    public void saveState() {
        cpu.setQuiescentCallback(() -> {
            try {
                savedState = createState();
                savedState.write(STATE_FILE);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public void restoreState() {
        if (STATE_FILE.exists()) {
            cpu.setQuiescentCallback(() -> {
                try {
                    savedState = State.read(STATE_FILE);
                    restoreState(savedState);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    private State createState() throws Exception {
        final State state = new State();
        Util.populateState(state, videoULA);
        Util.populateState(state, systemVIA);
        Util.populateState(state, soundChip);
        Util.populateState(state, userVIA);
        Util.populateState(state, crtc6845);
        Util.populateState(state, cpu);
        Util.populateState(state, ram);
        return state;
    }

    private void restoreState(final State state) throws Exception {
        Util.applyState(state, videoULA);
        Util.applyState(state, systemVIA);
        Util.applyState(state, soundChip);
        Util.applyState(state, userVIA);
        Util.applyState(state, crtc6845);
        Util.applyState(state, cpu);
        Util.applyState(state, ram);
    }

    public void run(final BooleanSupplier haltCondition) {
        this.clock.run(haltCondition);
    }

    public void addInterruptSource(final InterruptSource source) {
        interruptSources = Arrays.copyOf(interruptSources, interruptSources.length + 1);
        interruptSources[interruptSources.length - 1] = source;
    }

    public Clock getClock() {
        return clock;
    }

    @Override
    public boolean isIRQ() {
        for (InterruptSource s : interruptSources) {
            if (s.isIRQ()) {
                return true;
            }
        }
        return false;
    }
}
