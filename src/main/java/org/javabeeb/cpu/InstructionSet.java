package org.javabeeb.cpu;

import org.javabeeb.util.Util;

import java.util.*;

public final class InstructionSet {

    public static final int RTS_OPCODE = 0x60;

    private final Map<InstructionKey, Integer> instructionToCode = new HashMap<>();
    private final InstructionKey[] codeToInstruction = new InstructionKey[256];
    private final Map<Instruction, Set<AddressMode>> instructionSupportedAddressModes = new EnumMap<>(Instruction.class);
    private final Map<AddressMode, Set<Instruction>> addressModeToInstructions = new EnumMap<>(AddressMode.class);

    public InstructionSet() {

        // Special HALT instruction (for debugging and testing)
        register(Instruction.HLT, AddressMode.IMPLIED, 0x02);

        // Special instructions (that would usually be NOP) for tests/traps
        register(Instruction.ERR, AddressMode.IMMEDIATE, 0x80);
        register(Instruction.TST, AddressMode.IMMEDIATE, 0x82);
        register(Instruction.TRP, AddressMode.IMMEDIATE, 0x89);

        // Official instructions
        register(Instruction.ADC, AddressMode.IMMEDIATE, 0x69);
        register(Instruction.ADC, AddressMode.ZPG, 0x65);
        register(Instruction.ADC, AddressMode.ZPG_X, 0x75);
        register(Instruction.ADC, AddressMode.ABSOLUTE, 0x6d);
        register(Instruction.ADC, AddressMode.ABSOLUTE_X, 0x7d);
        register(Instruction.ADC, AddressMode.ABSOLUTE_Y, 0x79);
        register(Instruction.ADC, AddressMode.X_INDIRECT, 0x61);
        register(Instruction.ADC, AddressMode.INDIRECT_Y, 0x71);
        register(Instruction.AND, AddressMode.IMMEDIATE, 0x29);
        register(Instruction.AND, AddressMode.ZPG, 0x25);
        register(Instruction.AND, AddressMode.ZPG_X, 0x35);
        register(Instruction.AND, AddressMode.ABSOLUTE, 0x2d);
        register(Instruction.AND, AddressMode.ABSOLUTE_X, 0x3d);
        register(Instruction.AND, AddressMode.ABSOLUTE_Y, 0x39);
        register(Instruction.AND, AddressMode.X_INDIRECT, 0x21);
        register(Instruction.AND, AddressMode.INDIRECT_Y, 0x31);
        register(Instruction.ASL, AddressMode.ACCUMULATOR, 0xa);
        register(Instruction.ASL, AddressMode.ZPG, 0x6);
        register(Instruction.ASL, AddressMode.ZPG_X, 0x16);
        register(Instruction.ASL, AddressMode.ABSOLUTE, 0xe);
        register(Instruction.ASL, AddressMode.ABSOLUTE_X, 0x1e);
        register(Instruction.BCC, AddressMode.RELATIVE, 0x90);
        register(Instruction.BCS, AddressMode.RELATIVE, 0xb0);
        register(Instruction.BEQ, AddressMode.RELATIVE, 0xf0);
        register(Instruction.BIT, AddressMode.ZPG, 0x24);
        register(Instruction.BIT, AddressMode.ABSOLUTE, 0x2c);
        register(Instruction.BMI, AddressMode.RELATIVE, 0x30);
        register(Instruction.BNE, AddressMode.RELATIVE, 0xd0);
        register(Instruction.BPL, AddressMode.RELATIVE, 0x10);
        register(Instruction.BRK, AddressMode.IMPLIED, 0x0);
        register(Instruction.BVC, AddressMode.RELATIVE, 0x50);
        register(Instruction.BVS, AddressMode.RELATIVE, 0x70);
        register(Instruction.CLC, AddressMode.IMPLIED, 0x18);
        register(Instruction.CLD, AddressMode.IMPLIED, 0xd8);
        register(Instruction.CLI, AddressMode.IMPLIED, 0x58);
        register(Instruction.CLV, AddressMode.IMPLIED, 0xb8);
        register(Instruction.CMP, AddressMode.IMMEDIATE, 0xc9);
        register(Instruction.CMP, AddressMode.ZPG, 0xc5);
        register(Instruction.CMP, AddressMode.ZPG_X, 0xd5);
        register(Instruction.CMP, AddressMode.ABSOLUTE, 0xcd);
        register(Instruction.CMP, AddressMode.ABSOLUTE_X, 0xdd);
        register(Instruction.CMP, AddressMode.ABSOLUTE_Y, 0xd9);
        register(Instruction.CMP, AddressMode.X_INDIRECT, 0xc1);
        register(Instruction.CMP, AddressMode.INDIRECT_Y, 0xd1);
        register(Instruction.CPX, AddressMode.IMMEDIATE, 0xe0);
        register(Instruction.CPX, AddressMode.ZPG, 0xe4);
        register(Instruction.CPX, AddressMode.ABSOLUTE, 0xec);
        register(Instruction.CPY, AddressMode.IMMEDIATE, 0xc0);
        register(Instruction.CPY, AddressMode.ZPG, 0xc4);
        register(Instruction.CPY, AddressMode.ABSOLUTE, 0xcc);
        register(Instruction.DEC, AddressMode.ZPG, 0xc6);
        register(Instruction.DEC, AddressMode.ZPG_X, 0xd6);
        register(Instruction.DEC, AddressMode.ABSOLUTE, 0xce);
        register(Instruction.DEC, AddressMode.ABSOLUTE_X, 0xde);
        register(Instruction.DEX, AddressMode.IMPLIED, 0xca);
        register(Instruction.DEY, AddressMode.IMPLIED, 0x88);
        register(Instruction.EOR, AddressMode.IMMEDIATE, 0x49);
        register(Instruction.EOR, AddressMode.ZPG, 0x45);
        register(Instruction.EOR, AddressMode.ZPG_X, 0x55);
        register(Instruction.EOR, AddressMode.ABSOLUTE, 0x4d);
        register(Instruction.EOR, AddressMode.ABSOLUTE_X, 0x5d);
        register(Instruction.EOR, AddressMode.ABSOLUTE_Y, 0x59);
        register(Instruction.EOR, AddressMode.X_INDIRECT, 0x41);
        register(Instruction.EOR, AddressMode.INDIRECT_Y, 0x51);
        register(Instruction.INC, AddressMode.ZPG, 0xe6);
        register(Instruction.INC, AddressMode.ZPG_X, 0xf6);
        register(Instruction.INC, AddressMode.ABSOLUTE, 0xee);
        register(Instruction.INC, AddressMode.ABSOLUTE_X, 0xfe);
        register(Instruction.INX, AddressMode.IMPLIED, 0xe8);
        register(Instruction.INY, AddressMode.IMPLIED, 0xc8);
        register(Instruction.JMP, AddressMode.ABSOLUTE, 0x4c);
        register(Instruction.JMP, AddressMode.INDIRECT, 0x6c);
        register(Instruction.JSR, AddressMode.ABSOLUTE, 0x20);
        register(Instruction.LDA, AddressMode.IMMEDIATE, 0xa9);
        register(Instruction.LDA, AddressMode.ZPG, 0xa5);
        register(Instruction.LDA, AddressMode.ZPG_X, 0xb5);
        register(Instruction.LDA, AddressMode.ABSOLUTE, 0xad);
        register(Instruction.LDA, AddressMode.ABSOLUTE_X, 0xbd);
        register(Instruction.LDA, AddressMode.ABSOLUTE_Y, 0xb9);
        register(Instruction.LDA, AddressMode.X_INDIRECT, 0xa1);
        register(Instruction.LDA, AddressMode.INDIRECT_Y, 0xb1);
        register(Instruction.LDX, AddressMode.IMMEDIATE, 0xa2);
        register(Instruction.LDX, AddressMode.ZPG, 0xa6);
        register(Instruction.LDX, AddressMode.ZPG_Y, 0xb6);
        register(Instruction.LDX, AddressMode.ABSOLUTE, 0xae);
        register(Instruction.LDX, AddressMode.ABSOLUTE_Y, 0xbe);
        register(Instruction.LDY, AddressMode.IMMEDIATE, 0xa0);
        register(Instruction.LDY, AddressMode.ZPG, 0xa4);
        register(Instruction.LDY, AddressMode.ZPG_X, 0xb4);
        register(Instruction.LDY, AddressMode.ABSOLUTE, 0xac);
        register(Instruction.LDY, AddressMode.ABSOLUTE_X, 0xbc);
        register(Instruction.LSR, AddressMode.ACCUMULATOR, 0x4a);
        register(Instruction.LSR, AddressMode.ZPG, 0x46);
        register(Instruction.LSR, AddressMode.ZPG_X, 0x56);
        register(Instruction.LSR, AddressMode.ABSOLUTE, 0x4e);
        register(Instruction.LSR, AddressMode.ABSOLUTE_X, 0x5e);
        register(Instruction.NOP, AddressMode.IMPLIED, 0xea);
        register(Instruction.ORA, AddressMode.IMMEDIATE, 0x9);
        register(Instruction.ORA, AddressMode.ZPG, 0x5);
        register(Instruction.ORA, AddressMode.ZPG_X, 0x15);
        register(Instruction.ORA, AddressMode.ABSOLUTE, 0xd);
        register(Instruction.ORA, AddressMode.ABSOLUTE_X, 0x1d);
        register(Instruction.ORA, AddressMode.ABSOLUTE_Y, 0x19);
        register(Instruction.ORA, AddressMode.X_INDIRECT, 0x1);
        register(Instruction.ORA, AddressMode.INDIRECT_Y, 0x11);
        register(Instruction.PHA, AddressMode.IMPLIED, 0x48);
        register(Instruction.PHP, AddressMode.IMPLIED, 0x8);
        register(Instruction.PLA, AddressMode.IMPLIED, 0x68);
        register(Instruction.PLP, AddressMode.IMPLIED, 0x28);
        register(Instruction.ROL, AddressMode.ACCUMULATOR, 0x2a);
        register(Instruction.ROL, AddressMode.ZPG, 0x26);
        register(Instruction.ROL, AddressMode.ZPG_X, 0x36);
        register(Instruction.ROL, AddressMode.ABSOLUTE, 0x2e);
        register(Instruction.ROL, AddressMode.ABSOLUTE_X, 0x3e);
        register(Instruction.ROR, AddressMode.ACCUMULATOR, 0x6a);
        register(Instruction.ROR, AddressMode.ZPG, 0x66);
        register(Instruction.ROR, AddressMode.ZPG_X, 0x76);
        register(Instruction.ROR, AddressMode.ABSOLUTE, 0x6e);
        register(Instruction.ROR, AddressMode.ABSOLUTE_X, 0x7e);
        register(Instruction.RTI, AddressMode.IMPLIED, 0x40);
        register(Instruction.RTS, AddressMode.IMPLIED, RTS_OPCODE);
        register(Instruction.SBC, AddressMode.IMMEDIATE, 0xe9);
        register(Instruction.SBC, AddressMode.ZPG, 0xe5);
        register(Instruction.SBC, AddressMode.ZPG_X, 0xf5);
        register(Instruction.SBC, AddressMode.ABSOLUTE, 0xed);
        register(Instruction.SBC, AddressMode.ABSOLUTE_X, 0xfd);
        register(Instruction.SBC, AddressMode.ABSOLUTE_Y, 0xf9);
        register(Instruction.SBC, AddressMode.X_INDIRECT, 0xe1);
        register(Instruction.SBC, AddressMode.INDIRECT_Y, 0xf1);
        register(Instruction.SEC, AddressMode.IMPLIED, 0x38);
        register(Instruction.SED, AddressMode.IMPLIED, 0xf8);
        register(Instruction.SEI, AddressMode.IMPLIED, 0x78);
        register(Instruction.STA, AddressMode.ZPG, 0x85);
        register(Instruction.STA, AddressMode.ZPG_X, 0x95);
        register(Instruction.STA, AddressMode.ABSOLUTE, 0x8d);
        register(Instruction.STA, AddressMode.ABSOLUTE_X, 0x9d);
        register(Instruction.STA, AddressMode.ABSOLUTE_Y, 0x99);
        register(Instruction.STA, AddressMode.X_INDIRECT, 0x81);
        register(Instruction.STA, AddressMode.INDIRECT_Y, 0x91);
        register(Instruction.STX, AddressMode.ZPG, 0x86);
        register(Instruction.STX, AddressMode.ZPG_Y, 0x96);
        register(Instruction.STX, AddressMode.ABSOLUTE, 0x8e);
        register(Instruction.STY, AddressMode.ZPG, 0x84);
        register(Instruction.STY, AddressMode.ZPG_X, 0x94);
        register(Instruction.STY, AddressMode.ABSOLUTE, 0x8c);
        register(Instruction.TAX, AddressMode.IMPLIED, 0xaa);
        register(Instruction.TAY, AddressMode.IMPLIED, 0xa8);
        register(Instruction.TSX, AddressMode.IMPLIED, 0xba);
        register(Instruction.TXA, AddressMode.IMPLIED, 0x8a);
        register(Instruction.TXS, AddressMode.IMPLIED, 0x9a);
        register(Instruction.TYA, AddressMode.IMPLIED, 0x98);
    }

    private void register(final Instruction instruction, final AddressMode addressMode, final int opCode) {
        final InstructionKey key = new InstructionKey(instruction, addressMode);
        if (instructionToCode.containsKey(key)) {
            throw new IllegalStateException(key + ": duplicate instruction key");
        }
        instructionToCode.put(key, opCode);
        if (codeToInstruction[opCode] != null) {
            throw new IllegalStateException("0x" + Integer.toHexString(opCode) + ": duplicate opcode");
        }
        instructionSupportedAddressModes.computeIfAbsent(instruction, k -> EnumSet.noneOf(AddressMode.class)).add(addressMode);
        addressModeToInstructions.computeIfAbsent(addressMode, k -> EnumSet.noneOf(Instruction.class)).add(instruction);
        codeToInstruction[opCode] = key;
    }

    public InstructionKey decode(final int opcode) {
        final InstructionKey ret = codeToInstruction[opcode];
        if (ret == null) {
            throw new IllegalStateException("0x" + Integer.toHexString(opcode) + ": unrecognised opcode");
        }
        return ret;
    }

    public List<Integer> encode(final Instruction instruction, final AddressMode addressMode, final int parm) {
        final InstructionKey key = new InstructionKey(instruction, addressMode);
        if (!instructionToCode.containsKey(key)) {
            throw new IllegalStateException("Could not decode " + instruction);
        }

        final List<Integer> ret = new ArrayList<>();

        ret.add(instructionToCode.get(key));

        final int parameterByteCount = key.getAddressMode().getParameterByteCount();
        if (parameterByteCount == 1) {
            ret.add(Util.checkUnsignedByte(parm));
        } else if (parameterByteCount == 2) {
            Util.checkUnsignedWord(parm);
            ret.add(parm & 0xff);
            ret.add((parm >>> 8) & 0xff);
        }
        return ret;
    }

    public boolean isAddressModeSupported(final Instruction instruction, final AddressMode addressMode) {
        return instructionSupportedAddressModes.containsKey(instruction) && instructionSupportedAddressModes.get(instruction).contains(addressMode);
    }

    public Set<AddressMode> getSupportedAddressModes(final Instruction instruction) {
        if (!instructionSupportedAddressModes.containsKey(instruction)) {
            return Collections.emptySet();
        } else {
            return EnumSet.copyOf(instructionSupportedAddressModes.get(instruction));
        }
    }

    public void printAddressModeToInstruction() {
        for (AddressMode am : addressModeToInstructions.keySet()) {
            System.out.println(am);
            final Set<Instruction> instructions = new TreeSet<>(Comparator.comparing(Enum::name));
            instructions.addAll(addressModeToInstructions.get(am));
            instructions.forEach(i -> System.out.println("    " + i));
        }
    }
}
