package org.javabeeb.assembler;

import org.javabeeb.cpu.AddressMode;
import org.javabeeb.cpu.Instruction;
import org.javabeeb.cpu.InstructionKey;
import org.javabeeb.cpu.InstructionSet;
import org.javabeeb.memory.Memory;

import java.util.Objects;

public final class Disassembler {

    private final InstructionSet instructionSet;
    private final Memory memory;

    private int pc;

    public Disassembler(final InstructionSet instructionSet, final Memory memory) {
        this.instructionSet = Objects.requireNonNull(instructionSet);
        this.memory = Objects.requireNonNull(memory);
    }

    public void setPC(int pc) {
        this.pc = pc;
    }

    public String disassemble() {
        final StringBuilder s = new StringBuilder();
        final int opcode = memory.readByte(pc++);
        final InstructionKey key = instructionSet.decode(opcode);
        final Instruction instruction = key.getInstruction();
        final AddressMode addressMode = key.getAddressMode();
        int bytesToFollow = (instruction == Instruction.BRK) ? 1 : addressMode.getParameterByteCount();
        s.append(instruction);
        if (bytesToFollow == 1) {
            s.append(addressMode.formatOperand(pc, memory.readByte(pc)));
        } else if (bytesToFollow == 2) {
            s.append(addressMode.formatOperand(pc, memory.readWord(pc)));
        } else {
            s.append(addressMode.formatOperand(pc, 0));
        }
        pc += bytesToFollow;
        return s.toString();
    }

    public String disassemble(int pc) {
        final StringBuilder s = new StringBuilder();
        final int opcode = memory.readByte(pc++);
        final InstructionKey key = instructionSet.decode(opcode);
        final Instruction instruction = key.getInstruction();
        final AddressMode addressMode = key.getAddressMode();
        int bytesToFollow = (instruction == Instruction.BRK) ? 1 : addressMode.getParameterByteCount();
        s.append(instruction);
        if (bytesToFollow == 1) {
            s.append(addressMode.formatOperand(pc, memory.readByte(pc)));
        } else if (bytesToFollow == 2) {
            s.append(addressMode.formatOperand(pc, memory.readWord(pc)));
        } else {
            s.append(addressMode.formatOperand(pc, 0));
        }
        return s.toString();
    }
}
