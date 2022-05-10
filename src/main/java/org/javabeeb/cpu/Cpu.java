package org.javabeeb.cpu;

import org.javabeeb.assembler.Disassembler;
import org.javabeeb.clock.ClockListener;
import org.javabeeb.clock.ClockDefinition;
import org.javabeeb.device.Device;
import org.javabeeb.memory.Memory;
import org.javabeeb.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

@StateKey(key = "cpu6502")
public final class Cpu implements Device, ClockListener, Runnable, Scheduler {

    private static final boolean USE_QUEUE = true;

    public static final int NMI_JUMP_VECTOR = 0xFFFA;
    public static final int CODE_START_VECTOR = 0xFFFC;
    public static final int IRQ_JUMP_VECTOR = 0xFFFE;

    private static final int STACK_START = 0x100;

    private final Memory memory;

    private final SystemStatus systemStatus;
    private final Scheduler scheduler;
    private final InstructionSet instructionSet = new InstructionSet();
    private final AtomicLong cycleCount = new AtomicLong();
    private final OpQueue queue = new OpQueue();
    private final Disassembler disassembler;

    private InterruptSource interruptSource;
    private Runnable saveStateCallback;

    //
    // State
    //
    @StateKey(key = "A")
    private int a;

    @StateKey(key = "X")
    private int x;

    @StateKey(key = "Y")
    private int y;

    @StateKey(key = "pc")
    private int pc;

    @StateKey(key = "sp")
    private int sp = 0xFF;

    @StateKey(key = "flags")
    private int flags;

    @StateKey(key = "servicingInterrupt")
    private boolean servicingInterrupt;

    @StateKey(key = "inISR")
    private boolean inIRQ;

    private boolean inNMI;

    //
    // Temporary registers
    //
    private Instruction instruction;
    private String instructionDis;
    private int pcDis;
    private AddressMode addressMode;
    private int operand;
    private int lo;
    private int hi;
    private int elo;
    private int ehi;
    private int jumpVector;

    private boolean halted;
    private int haltCode;
    private BooleanSupplier verboseCondition;

    private Runnable haltHook;

    private long maxCycleCount = -1L;

    private int fetchDelayMillis = 0;
    private Predicate<Cpu> fetchDelayCondition;

    private boolean haltIfPCLoop = false;

    public Cpu(final SystemStatus systemStatus, final Scheduler scheduler, final Memory memory) {
        this.systemStatus = Objects.requireNonNull(systemStatus);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.memory = Objects.requireNonNull(memory);
        this.disassembler = new Disassembler(instructionSet, memory);
        reset();
    }

    public void setHaltIfPCLoop(final boolean haltIfPCLoop) {
        // For testing
        this.haltIfPCLoop = haltIfPCLoop;
    }

    @Override
    public ScheduledTask newTask(Runnable runnable) {
        return scheduler.newTask(runnable);
    }

    @Override
    public void schedule(ScheduledTask task, long delay) {
        scheduler.schedule(task, delay);
    }

    @Override
    public void unschedule(ScheduledTask task) {
        scheduler.unschedule(task);
    }

    public SystemStatus getSystemStatus() {
        return systemStatus;
    }

    public void reset() {
        this.pc = memory.readWord(CODE_START_VECTOR);
        this.sp = 0xFF;
        this.flags = Flag.INTERRUPT.set(0);
        this.halted = false;
        this.haltCode = 0;
    }

    public void setInterruptSource(final InterruptSource interruptSource) {
        this.interruptSource = interruptSource;
    }

    @Override
    public String getName() {
        return "6502";
    }

    public int getHaltCode() {
        return haltCode;
    }

    public void setFetchDelayMillis(final int fetchDelayMillis) {
        this.fetchDelayMillis = fetchDelayMillis;
    }

    public void setFetchDelayCondition(final Predicate<Cpu> condition) {
        this.fetchDelayCondition = condition;
    }

    private boolean isIRQ() {
        return interruptSource != null && interruptSource.isIRQ();
    }

    private boolean isNMI() {
        return nmiRequested;
    }

    public Memory getMemory() {
        return memory;
    }

    public Cpu setVerboseCondition(final BooleanSupplier verboseCondition) {
        this.verboseCondition = (verboseCondition == null) ? () -> false : verboseCondition;
        return this;
    }

    public Cpu setHaltHook(final Runnable haltHook) {
        this.haltHook = haltHook;
        return this;
    }

    public Cpu setMaxCycleCount(final long maxCycleCount) {
        this.maxCycleCount = maxCycleCount;
        return this;
    }

    public void halt(final int code) {
        this.haltCode = code & 0xFF;
        this.halted = true;
        if (haltHook != null) {
            haltHook.run();
        }
    }

    public void test(final int code) {
        final TestCode testCode = TestCode.get(code);
        if (!testCode.test(this)) {
            halt(1);
        }
    }

    public void trap(final int code) {
        final TestCode testCode = TestCode.get(code);
        if (testCode.test(this)) {
            halt(2);
        }
    }

    public boolean isInIRQ() {
        return inIRQ;
    }

    public boolean isInNMI() {
        return inNMI;
    }

    private void serviceBRK() {
        if (!servicingInterrupt) {
            serviceInterrupt(true, false);
        }
    }

    private void serviceInterrupt(final boolean isBRK, final boolean isReset) {
        servicingInterrupt = true;
        if (isBRK) {
            queue(this::readFromAndIncrementPC); // skip next byte
            queue(() -> pushByte(getPCH()));
        } else {
            queue(() -> pushByte(getPCH())); // not queued
        }
        queue(() -> pushByte(getPCL()));
        queue(() -> {
            pushByte(Flag.RESERVED.set(Flag.BREAK.set(flags, isBRK)));
        });
        queue(() -> {
            if (isReset) {
                jumpVector = CODE_START_VECTOR;
                nmiRequested = false;
                inNMI = false;
                inIRQ = false;
            } else if (nmiRequested) {
                jumpVector = NMI_JUMP_VECTOR;
                nmiRequested = false;
                inNMI = true;
            } else {
                jumpVector = IRQ_JUMP_VECTOR;
                inIRQ = (!isBRK);
            }
            setPCL(readMemory(jumpVector));
        });
        queue(() -> {
            setPCH(readMemory(jumpVector + 1));
            servicingInterrupt = false;
            flags = Flag.INTERRUPT.set(flags);
            if (isReset) {
                reset();
            }
        });
    }

    public void setQuiescentCallback(Runnable callback) {
        this.saveStateCallback = callback;
    }

    private volatile boolean nmiRequested;
    public void requestNMI(final boolean b) {
        nmiRequested = b;
    }

    private volatile boolean resetRequested;
    public void requestReset(final boolean b) {
        this.resetRequested = b;
    }

    @Override
    public void tick(final ClockDefinition clockDefinition, final long elapsedNanos) {
        scheduler.tick(clockDefinition, elapsedNanos);
        pcDis = pc;
        if (queue.isEmpty()) {
            if (!servicingInterrupt) {
                // We are quiescent here
                if (saveStateCallback != null) {
                    saveStateCallback.run();
                    saveStateCallback = null;
                }

                // Check reset request
                if (resetRequested) {
                    serviceInterrupt(false, true);
                    resetRequested = false;
                    return;
                }
                // Check interrupt status
                if (isNMI() || (Flag.INTERRUPT.isClear(flags) && isIRQ())) {
                    serviceInterrupt(false, false);
                    return;
                }
            }

            if (fetchDelayMillis > 0 && (fetchDelayCondition == null || fetchDelayCondition.test(this))) {
                Util.sleep(fetchDelayMillis);
            }

            fetch();

            cycleCount.incrementAndGet();
        } else {
            instructionDis = "";
            queue.remove().run();
            cycleCount.incrementAndGet();
        }

        if (maxCycleCount > 0L && cycleCount.get() >= maxCycleCount) {
            halt(0);
        }
    }

    @Override
    public void run() {
        while (!halted) {
            tick(ClockDefinition.CR200, 0L);
        }
    }

    public long getCycleCount() {
        return cycleCount.get();
    }

    private int lastFetchPC;
    private void fetch() {

        if (lastFetchPC == this.pc && haltIfPCLoop) {
            this.halted = true;
            this.haltCode = 99;
            System.out.println("LOOP AT " + Util.formatHexWord(pc));
            System.out.println(this);
            return;
        }

        lastFetchPC = this.pc;

        final boolean verbose = (verboseCondition != null && verboseCondition.getAsBoolean());

        while (memory.processIntercepts(pc)) {
            // Do nothing
        }

        if (verbose) {
            instructionDis = Util.formatHexWord(this.pc) + ": " + disassembler.disassemble(this.pc);
        }

        final int opcode = readFromAndIncrementPC();
        final InstructionKey key = instructionSet.decode(opcode);
        this.instruction = key.getInstruction();
        this.addressMode = key.getAddressMode();

        if (verbose) {
            Util.log(toString(), 0);
        }

        execute();
    }

    public void JSR(final int address) {
        final int pcNow = pc;
        final int pcDec = pcNow - 1;
        pushByte((pcDec >>> 8) & 0xFF);
        pushByte(pcDec & 0xFF);
        setPC(address);
        while (pc != pcNow) {
            tick(ClockDefinition.CR200, 0L);
        }
    }

    private final Map<Integer, Runnable> returnTo = new HashMap<>();

    public void onReturnTo(final int address, Runnable runnable) {
        returnTo.put(address, runnable);
    }

    private void execute() {

        switch (instruction) {
            case BRK: {
                serviceBRK();
                return;
            }
            case RTI: {
                queue(this::readFromPC);
                queue(this::incSP);
                queue(() -> {
                    flags = Flag.BREAK.clear(popByteNoIncrement());
                    incSP();
                });
                queue(() -> {
                    setPCL(popByteNoIncrement());
                    incSP();
                });
                queue(() -> {
                    setPCH(popByteNoIncrement());
                    inIRQ = false;
                    inNMI = false;
                });
                return;
            }
            case RTS: {
                queue(this::readFromPC);
                queue(this::incSP);
                queue(() -> {
                    setPCL(popByteNoIncrement());
                    incSP();
                });
                queue(() -> setPCH(popByteNoIncrement()));
                queue(() -> {
                    if (returnTo.containsKey(pc)) {
                        returnTo.get(pc).run();
                        returnTo.remove(pc);
                    }
                    incPC();
                });
                return;
            }
            case PHA: {
                queue(this::readFromPC);
                queue(() -> {
                    pushByte(a);
                });
                return;
            }
            case PHP: {
                queue(this::readFromPC);
                queue(() -> {
                    pushByte(Flag.RESERVED.set(Flag.BREAK.set(flags)));
                });
                return;
            }
            case PLA: {
                queue(this::readFromPC);
                queue(() -> incSP());
                queue(() -> setA(popByteNoIncrement(), true));
                return;
            }
            case PLP: {
                queue(this::readFromPC);
                queue(() -> incSP());
                queue(() -> flags = Flag.BREAK.clear(popByteNoIncrement()));
                return;
            }
            case JSR: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(this::nop);
                queue(() -> pushByte(getPCH()));
                queue(() -> pushByte(getPCL()));
                queue(() -> {
                    hi = readFromPC();
                    setPCL(lo);
                    setPCH(hi);
                });
                return;
            }
        }

        switch (addressMode) {

            case ACCUMULATOR:
                accumulator();
                break;

            case IMPLIED:
                implied();
                break;

            case IMMEDIATE:
                immediate();
                break;

            case INDIRECT:
                indirect();
                break;

            case ABSOLUTE:
                absolute();
                break;

            case ABSOLUTE_X:
                absoluteIndexed(true);
                break;

            case ABSOLUTE_Y:
                absoluteIndexed(false);
                break;

            case ZPG:
                zeroPage();
                break;

            case ZPG_X:
                zeroPageIndexed(true);
                break;

            case ZPG_Y:
                zeroPageIndexed(false);
                break;

            case RELATIVE:
                relative();
                break;

            case X_INDIRECT:
                xIndirect();
                break;

            case INDIRECT_Y:
                indirectY();
                break;

            default:
                throw new IllegalStateException(addressMode + ": unsupported address mode");
        }
    }

    private void queue(final Runnable runnable) {
        if (USE_QUEUE) {
            queue.add(runnable);
        } else {
            runnable.run();
            cycleCount.incrementAndGet();
        }
    }

    private void nop() {
        // Do nothing
    }

    private void readEffectiveAddressIndirectX() {
        queue(() -> lo = readFromAndIncrementPC());
        queue(() -> {
            readMemory(lo);
            lo = (lo + getX()) & 0xFF;
        });
        queue(() -> elo = readMemory(lo));
        queue(() -> ehi = readMemory((lo + 1) & 0xFF));
    }

    private void xIndirect() {
        switch (instruction.getType()) {
            case READ: {

                readEffectiveAddressIndirectX();

                queue(() -> instruction.acceptValue(this, readMemory(elo, ehi)));

                break;
            }
            case READ_MODIFY_WRITE: {

                readEffectiveAddressIndirectX();

                queue(() -> operand = readMemory(elo, ehi));
                queue(() -> {
                    writeMemory(elo, ehi, operand);
                    operand = instruction.transformValue(this, operand);
                });
                queue(() -> writeMemory(elo, ehi, operand));

                break;
            }
            case WRITE: {

                readEffectiveAddressIndirectX();

                queue(() -> writeMemory(elo, ehi, instruction.readValue(this)));

                break;
            }

            default:
                throw new IllegalStateException(instruction + ": not implemented for X indirect address mode");
        }
    }

    private void readEffectiveAddressIndirectY() {
        queue(() -> lo = readFromAndIncrementPC());
        queue(() -> elo = readMemory(lo));
        queue(() -> {
            ehi = readMemory((lo + 1) & 0xFF);
            elo += getY();
        });
    }

    private void indirectY() {

        switch (instruction.getType()) {
            case READ: {

                readEffectiveAddressIndirectY();

                queue(() -> {
                    operand = readMemory(elo, ehi);
                    if (elo > 255) {
                        ehi++;
                        queue(() -> {
                            operand = readMemory(elo, ehi);
                            instruction.acceptValue(this, operand);
                        });
                    } else {
                        instruction.acceptValue(this, operand);
                    }
                });
                break;
            }
            case READ_MODIFY_WRITE: {

                readEffectiveAddressIndirectY();

                queue(() -> {
                    operand = readMemory(elo, ehi);
                    if (elo > 255) {
                        ehi++;
                    }
                });
                queue(() -> operand = readMemory(elo, ehi));
                queue(() -> {
                    writeMemory(elo, ehi, operand);
                    operand = instruction.transformValue(this, operand);
                });
                queue(() -> writeMemory(elo, ehi, operand));
                break;
            }

            case WRITE: {

                readEffectiveAddressIndirectY();

                queue(() -> {
                    readMemory(elo, ehi);
                    if (elo > 255) {
                        ehi++;
                    }
                });
                queue(() -> writeMemory(elo, ehi, instruction.readValue(this)));
                break;
            }

            default:
                throw new IllegalStateException(instruction + ": not implemented for indirect Y address mode");
        }
    }

    private void indirect() {
        switch (instruction.getType()) {
            case JUMP: {

                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> hi = readFromAndIncrementPC());
                queue(() -> elo = readMemory(lo, hi));
                queue(() -> {
                    ehi = readMemory((lo + 1) & 0xFF, hi);
                    // JMP indirect bug - don't correct hi
                    setPCL(elo);
                    setPCH(ehi);
                });

                break;
            }

            default:
                throw new IllegalStateException(instruction + ": not implemented for indirect address mode");
        }
    }

    private void immediate() {
        queue.add(() -> instruction.acceptValue(this, readFromAndIncrementPC()));
    }

    private void accumulator() {
        queue.add(() -> {
            readFromPC();
            setA(instruction.transformValue(this, getA()), true);
        });
    }

    private void implied() {
        queue.add(() -> {
            readFromPC();
            instruction.performImpliedAction(this);
        });
    }

    public void maintainNZ(final int value) {
        flags = Flag.ZERO.set(flags, (value == 0));
        flags = Flag.NEGATIVE.set(flags, (value & 0x80) != 0);
    }

    private void absolute() {
        switch (instruction.getType()) {
            case JUMP: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> {
                    hi = readFromAndIncrementPC();
                    setPCL(lo);
                    setPCH(hi);
                });
                break;
            }
            case READ: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> hi = readFromAndIncrementPC());
                queue(() -> instruction.acceptValue(this, readMemory(lo, hi)));
                break;
            }

            case READ_MODIFY_WRITE: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> hi = readFromAndIncrementPC());
                queue(() -> operand = readMemory(lo, hi));
                queue(() -> writeMemory(lo, hi, operand));
                queue(() -> writeMemory(lo, hi, instruction.transformValue(this, operand)));
                break;
            }

            case WRITE: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> hi = readFromAndIncrementPC());
                queue(() -> writeMemory(lo, hi, instruction.readValue(this)));
                break;
            }

            default:
                throw new IllegalStateException(instruction + ": not implemented for absolute address mode");
        }
    }

    private void relative() {
        switch (instruction.getType()) {
            case BRANCH: {
                queue(() -> {
                    operand = readFromAndIncrementPC();
                    if (instruction.branchCondition(this)) {
                        queue(() -> {
                            lo = getPCL() + Util.signed(operand);
                            setPCL(lo);
                            final int oldPch = getPCH();
                            final int newPch;
                            if (lo < 0) {
                                newPch = oldPch - 1;
                            } else if (lo > 255) {
                                newPch = oldPch + 1;
                            } else {
                                newPch = oldPch;
                            }
                            setPCH(newPch);

                            if (newPch != oldPch) {
                                queue(this::readFromPC);
                            }
                        });
                    }
                });

                break;
            }

            default:
                throw new IllegalStateException(instruction + ": not implemented for relative address mode");
        }
    }

    private void absoluteIndexed(final boolean x) {

        switch (instruction.getType()) {

            case READ: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> {
                    hi = readFromAndIncrementPC();
                    elo = lo + ((x) ? getX() : getY());
                });
                queue(() -> operand = readMemory(elo, hi));
                queue(() -> {
                   if (elo > 255) {
                       queue(() -> {
                           operand = readMemory(elo, hi + 1);
                           instruction.acceptValue(this, operand);
                       });
                   } else {
                       instruction.acceptValue(this, operand);
                   }
                });
                break;
            }

            case READ_MODIFY_WRITE: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> {
                    hi = readFromAndIncrementPC();
                    elo = lo + ((x) ? getX() : getY());
                });
                queue(() -> {
                    readMemory(elo, hi);
                    ehi = (elo > 255) ? hi + 1 : hi;
                });
                queue(() -> operand = readMemory(elo, ehi));
                queue(() -> writeMemory(elo, ehi, operand));
                queue(() -> writeMemory(elo, ehi, instruction.transformValue(this, operand)));
                break;
            }

            case WRITE: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> {
                    hi = readFromAndIncrementPC();
                    elo = lo + ((x) ? getX() : getY());
                });
                queue(() -> {
                    readMemory(elo, hi);
                    ehi = (elo > 255) ? hi + 1 : hi;
                });
                queue(() -> writeMemory(elo, ehi, instruction.readValue(this)));
                break;
            }

            default:
                throw new IllegalStateException(instruction + ": not implemented for absolute indexed address mode");
        }
    }

    private void zeroPageIndexed(final boolean x) {

        switch (instruction.getType()) {

            case READ: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> {
                    readMemory(lo);
                    final int inc = (x) ? getX() : getY();
                    elo = (lo + inc) & 0xFF;
                });
                queue(() -> instruction.acceptValue(this, readMemory(elo)));
                break;
            }

            case READ_MODIFY_WRITE: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> {
                    readMemory(lo);
                    final int inc = (x) ? getX() : getY();
                    elo = (lo + inc) & 0xFF;
                });
                queue(() -> operand = readMemory(elo));
                queue(() -> {
                    writeMemory(elo, operand);
                    operand = instruction.transformValue(this, operand);
                });
                queue(() -> writeMemory(elo, operand));
                break;
            }

            case WRITE: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> {
                    readMemory(lo);
                    final int inc = (x) ? getX() : getY();
                    elo = (lo + inc) & 0xFF;
                });
                queue(() -> writeMemory(elo, instruction.readValue(this)));
                break;
            }

            default:
                throw new IllegalStateException(instruction + ": not implemented for zero page indexed address mode");
        }
    }

    private void zeroPage() {
        switch (instruction.getType()) {

            case READ: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> instruction.acceptValue(this, readMemory(lo)));
                break;
            }

            case READ_MODIFY_WRITE: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> operand = readMemory(lo));
                queue(() -> writeMemory(lo, operand));
                queue(() -> writeMemory(lo, instruction.transformValue(this, operand)));
                break;
            }

            case WRITE: {
                queue(() -> lo = readFromAndIncrementPC());
                queue(() -> writeMemory(lo, instruction.readValue(this)));
                break;
            }

            default:
                throw new IllegalStateException(instruction + ": not implemented for zero page address mode");
        }
    }

    public void setA(final int value, final boolean maintainNZ) {
        this.a = value & 0xFF;
        if (maintainNZ) {
            maintainNZ(value);
        }
    }

    public void setX(final int value, final boolean maintainNZ) {
        this.x = value & 0xFF;
        if (maintainNZ) {
            maintainNZ(value);
        }
    }

    public void setY(final int value, final boolean maintainNZ) {
        this.y = value & 0xFF;
        if (maintainNZ) {
            maintainNZ(value);
        }
    }

    public int getSP() {
        return sp;
    }

    public void setSP(final int value) {
        this.sp = value & 0xFF;
    }

    public int getA() {
        return a;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getPC() {
        return pc;
    }

    private void incPC() {
        pc++;
    }

    public void setPC(final int pc) {
        this.pc = pc;
    }

    private int getPCL() {
        return (pc & 0xFF);
    }

    private void setPCL(final int n) {
        pc = (pc & 0xFF00) | (n & 0xFF);
    }

    private int getPCH() {
        return (pc >>> 8) & 0xFF;
    }

    private void setPCH(final int n) {
        pc = (pc & 0x00FF) | ((n & 0xFF) << 8);
    }

    private int readFromPC() {
        return readMemory(getPC());
    }

    private int readFromAndIncrementPC() {
        final int n = readFromPC();
        incPC();
        return n;
    }

    private int readMemory(final int address) {
        return memory.readByte(address);
    }

    private int readMemory(final int lo, final int hi) {
        return readMemory((lo & 0xFF) | ((hi & 0xFF) << 8));
    }

    private void writeMemory(final int address, final int value) {
        memory.writeByte(address, value);
    }

    private void writeMemory(final int lo, final int hi, final int value) {
        writeMemory(lohiToAddress(lo, hi), value);
    }

    private static int lohiToAddress(final int lo, final int hi) {
        return (lo & 0xFF) | ((hi & 0xFF) << 8);
    }

    public boolean isFlagSet(final Flag flag) {
        return flag.isSet(flags);
    }

    public boolean isFlagClear(final Flag flag) {
        return flag.isClear(flags);
    }

    public int getFlagValue(final Flag flag) {
        return flag.isSet(flags) ? 1 : 0;
    }

    public void setFlag(final Flag f) {
        flags = f.set(flags);
    }

    public void setFlag(final Flag f, final boolean set) {
        flags = (set) ? f.set(flags) : f.clear(flags);
    }

    public void clearFlag(final Flag f) {
        flags = f.clear(flags);
    }

    private static void checkSpInBounds(final int sp) {
        if (sp < 0) {
            throw new IllegalStateException("Stack overflow");
        }
        if (sp > 255) {
            throw new IllegalStateException("Stack underflow");
        }
    }

    public void pushByte(final int value) {
        pushByteNoIncrement(value);
        decSP();
    }

    public void pushByteNoIncrement(final int value) {
        checkSpInBounds(sp);
        Util.checkUnsignedByte(value);
        memory.writeByte(STACK_START + sp, value);
    }

    public void pushWord(final int value) {
        Util.checkUnsignedWord(value);
        pushByte((value >>> 8));
        pushByte((value & 0xFF));
    }

    private void decSP() {
        sp = (sp - 1) & 0xFF;
    }

    private void incSP() {
        sp = (sp + 1) & 0xFF;
    }

    public int popByte() {
        incSP();
        return popByteNoIncrement();
    }

    public int popByteNoIncrement() {
        checkSpInBounds(sp);
        return memory.readByte(STACK_START + sp);
    }

    public int peekByte(int offset) {
        return memory.readByte(STACK_START + sp + offset);
    }

    public String toString() {
        return toString(instructionDis);
    }

    public String toString(final String lhs) {
        final StringBuilder s = new StringBuilder();
        s.append(Util.padRight(lhs, 16));
        s.append(" PC = ").append(Util.formatHexWord(pc));
        s.append(" A = ").append(Util.formatHexByte(getA()));
        s.append("  X = ").append(Util.formatHexByte(getX()));
        s.append("  Y = ").append(Util.formatHexByte(getY()));
        s.append("  SP = ").append(Util.formatHexByte(getSP()));
        s.append("  SR = ").append(Flag.toString(flags));
        s.append("  irq = " + isIRQ());
        s.append("  nmi = " + isNMI());
        return s.toString();
    }

    private static final class OpQueue {

        private final Runnable[] queue = new Runnable[16];

        private int size = 0;
        private int head = 0;
        private int tail = 0;

        boolean isEmpty() {
            return size == 0;
        }

        void add(Runnable op) {
            if (size == 16) {
                throw new IllegalStateException("queue size exceeded");
            }
            queue[tail] = op;
            tail = (tail + 1) & 0xF;
            size++;
        }

        Runnable remove() {
            if (size == 0) {
                throw new IllegalStateException("queue is empty");
            }
            final Runnable ret = queue[head];
            head = (head + 1) & 0xF;
            size--;
            return ret;
        }
    }
}
