package org.javabeeb.screen;

import org.javabeeb.clock.ClockSpeed;
import org.javabeeb.device.Crtc6845;
import org.javabeeb.device.SystemVIA;
import org.javabeeb.device.VideoULA;
import org.javabeeb.memory.Memory;
import org.javabeeb.util.Util;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.util.Objects;

public class GraphicsModeScreenRenderer extends AbstractScreenRenderer {

    private static final int CLOCK_RATE = ClockSpeed.TWO_MHZ;

    private final Screen screen;

    private long inputCycleCount = 0L;
    private long myCycleCount = 0L;

    private long myCyclesSinceSync = 0L;

    private Rectangle cursorRect;

    private int startAddress;
    private int cursorAddress;
    private boolean cursorOn;
    private int baseAddress;

    //
    // Timings
    //
    private int horizontalTotalChars;
    private int horizontalDisplayedChars;

    private int verticalTotalChars;
    private int verticalDisplayedChars;
    private int verticalAdjust;

    private boolean fastClock;

    private int scanLinesPerChar;
    private int pixelsPerLine;
    private int bitsPerPixel;
    private int pixelsPerChar;
    private int pixelWidth;
    private int pixelHeight;

    private int charPos = 0;
    private int scanLine;
    private int scanLineCount;

    private long paintStart;
    private int rasterWidth = -1;
    private int rasterHeight = -1;

    public GraphicsModeScreenRenderer(Screen screen, Memory memory, SystemVIA systemVIA, Crtc6845 crtc6845, VideoULA videoULA) {
        super(memory, systemVIA, crtc6845, videoULA);
        this.screen = Objects.requireNonNull(screen);
    }

    @Override
    public void newFrame() {

        startAddress = crtc6845.getScreenStartAddress() * 8;
        baseAddress = systemVIA.getScreenStartAddress();
        cursorAddress = wrapAddress(baseAddress, crtc6845.getCursorAddress() * 8);
        cursorOn = crtc6845.isCursorOn() && videoULA.isCursorEnabled() && crtc6845.isCursorEnabled();

        horizontalTotalChars = crtc6845.getHorizontalTotalChars();
        horizontalDisplayedChars = crtc6845.getHorizontalDisplayedChars();
        verticalTotalChars = crtc6845.getVerticalTotalChars();
        verticalDisplayedChars = crtc6845.getVerticalDisplayedChars();
        verticalAdjust = crtc6845.getVerticalAdjust();

        scanLinesPerChar = crtc6845.getScanlinesPerCharacter();
        scanLineCount = verticalDisplayedChars * 8;

        pixelsPerChar = videoULA.getPixelsPerCharacter();
        pixelsPerLine = horizontalDisplayedChars * pixelsPerChar;
        bitsPerPixel = videoULA.getBitsPerPixel();
        fastClock = videoULA.isFastClockRate();
        pixelWidth = bitsPerPixel * (fastClock ? 1 : 2);

        pixelHeight = 2;

        myCyclesSinceSync = 0L;
        charPos = 0;
        scanLine = 0;
        cursorRect = null;

        rasterWidth = -1;
        rasterHeight = -1;
    }

    @Override
    public boolean isClockBased() {
        return true;
    }

    @Override
    public void tick(final BufferedImage image, final ClockSpeed clockSpeed, final long elapsedNanos) {
        final int cycles = clockSpeed.computeElapsedCycles(CLOCK_RATE, inputCycleCount, myCycleCount, elapsedNanos);
        inputCycleCount++;
        myCycleCount += cycles;
        if (cycles <= 0) {
            // Nothing to do yet
            return;
        }

        for (int i = 0; i < cycles; i++) {
            if (fastClock || ((myCyclesSinceSync & 1) == 0)) {
                if ((myCyclesSinceSync % horizontalTotalChars) < horizontalDisplayedChars) {
                    try {
                        paintNextCharacter(image);
                    } catch (Exception ex) {
                        // TODO: Avoid having to catch this
                        // Deliberately ignored
                    }
                }
            }
            myCyclesSinceSync++;
        }
    }

    @Override
    public boolean isImageReady() {
        return scanLine >= scanLineCount;
    }

    private void paintNextCharacter(final BufferedImage img) {

        // TODO: Only fetch these after a material change in Videa ULA
        pixelsPerChar = videoULA.getPixelsPerCharacter();
        pixelsPerLine = horizontalDisplayedChars * pixelsPerChar;
        bitsPerPixel = videoULA.getBitsPerPixel();
        fastClock = videoULA.isFastClockRate();
        pixelWidth = bitsPerPixel * (fastClock ? 1 : 2);

        final DataBuffer dataBuffer = img.getWritableTile(0, 0).getDataBuffer();
        final int imageWidth = img.getWidth();

        if (scanLine >= scanLineCount) {
            return;
        }

        if (scanLine == 0 && charPos == 0) {
            cursorRect = null;
            paintStart = System.nanoTime();
        }

        final int byteWidth = pixelsPerChar * pixelWidth;
        final int scanLineAddress = startAddress + ((scanLine >>> 3) * horizontalDisplayedChars * 8) + (scanLine & 0x7);

        final int address = wrapAddress(baseAddress, scanLineAddress + (charPos << 3));
        final int v = memory.readByte(address);
        final int x = charPos * byteWidth;
        int px = x;
        int py = computeCharY(scanLine, scanLinesPerChar, pixelHeight);
        for (int b = 0; b < pixelsPerChar; b++) {
            final int rgb = videoULA.getPhysicalColor(v, b, bitsPerPixel).getRGB() & 0xFFFFFF;
            Util.fillRect(dataBuffer, rgb, px, py, pixelWidth, pixelHeight, imageWidth);
            px += pixelWidth;
        }

        final int maxY = py + pixelHeight;
        if (px > rasterWidth) {
            rasterWidth = px;
        }
        if (maxY > rasterHeight) {
            rasterHeight = maxY;
        }

        if (cursorOn && cursorRect == null && address == cursorAddress) {
            final int cx = x;
            final int cy = computeCharY(scanLine & 0xf8, scanLinesPerChar, pixelHeight);
            cursorRect = new Rectangle(cx, cy, byteWidth * bitsPerPixel, pixelHeight * scanLinesPerChar);
        }

        charPos++;
        if (charPos == horizontalDisplayedChars) {
            charPos = 0;
            scanLine++;
        }
        if (scanLine >= scanLineCount) {
            final int ox = Math.max(0, (imageWidth - rasterWidth) / 2);
            final int oy = Math.max(0, (img.getHeight() - rasterHeight) / 2);
            if (cursorRect != null) {
                final int cursorStart = crtc6845.getCursorStartLine();;
                final int cursorHeight = (crtc6845.getCursorEndLine() - cursorStart) * pixelHeight;
                if (cursorHeight > 0) {
                    Util.fillRectXOR(dataBuffer, Color.WHITE.getRGB(),
                            cursorRect.x,
                            cursorRect.y + cursorStart * pixelHeight,
                            cursorRect.width,
                            cursorHeight,
                            imageWidth
                    );
                }
                cursorRect = null;
            }
            final Point origin = new Point(ox, oy);
            screen.imageReady(origin, System.nanoTime() - paintStart);
        }
    }

    private static int computeCharY(final int scanLine, final int scanLinesPerChar, final int pixelHeight) {
        return ((scanLine >>> 3) * scanLinesPerChar * pixelHeight) + ((scanLine & 0x7) * pixelHeight);
    }

    private static int wrapAddress(final int baseAddress, int address) {
        if (address >= 0x8000) {
            address -= (0x8000 - baseAddress);
        }
        return address;
    }

    @Override
    public void refreshWholeImage(final BufferedImage img) {
        throw new UnsupportedOperationException();
    }
}
