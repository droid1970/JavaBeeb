package org.javabeeb.cpu;

import org.javabeeb.cpu.Cpu;
import org.javabeeb.memory.AbstractMemory;
import org.javabeeb.memory.MemoryUtils;
import org.javabeeb.memory.RandomAccessMemory;
import org.javabeeb.util.DefaultScheduler;
import org.javabeeb.util.InterruptSource;
import org.javabeeb.util.SystemStatus;
import org.javabeeb.util.Util;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class InterruptTest {

    @Test
    void interruptTests() throws Exception {
        final AbstractMemory memory = new RandomAccessMemory(0, 65536);
        MemoryUtils.loadS19(memory, getClass().getResourceAsStream("/6502_interrupt_test.s19"), 0);
        memory.addModifyWatch(0x200, v -> {});
        memory.addModifyWatch(0x201, v -> {});
        memory.addModifyWatch(0x202, v -> {});

        final int interruptRegister = 0xbffc;

        final InterruptSource interruptSource = new InterruptSource() {
            @Override
            public boolean isIRQ() {
                return (memory.readByte(interruptRegister) & 1) != 0;
            }
        };

        final Cpu cpu = new Cpu(new SystemStatus(), new DefaultScheduler(), memory);
        cpu.setInterruptSource(interruptSource);
        memory.addModifyWatch(interruptRegister, v -> {
            if ((v & 2) != 0) {
                cpu.requestNMI(true);
            }
        });
        cpu.setHaltIfPCLoop(true);
        cpu.setPC(0x400);
        cpu.run();
        System.out.println("halt code = " + cpu.getHaltCode());
        System.out.println("0x200 = " + Util.formatHexByte(memory.readByte(0x200)));
        System.out.println("0x201 = " + Util.formatHexByte(memory.readByte(0x201)));
        System.out.println("0x202 = " + Util.formatHexByte(memory.readByte(0x202)));
        assertThat(memory.readByte(0x200)).isEqualTo(0x01);
        assertThat(memory.readByte(0x201)).isEqualTo(0x02);
        assertThat(memory.readByte(0x202)).isEqualTo(0x03);
        assertThat(cpu.getPC()).isEqualTo(0x06f5);
    }
}
