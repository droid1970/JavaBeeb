package org.javabeeb.screen;

import org.javabeeb.BBCMicro;
import org.javabeeb.clock.ClockListener;
import org.javabeeb.clock.ClockDefinition;
import org.javabeeb.device.Crtc6845;
import org.javabeeb.device.SystemVIA;
import org.javabeeb.device.VideoULA;
import org.javabeeb.memory.Memory;
import org.javabeeb.teletext.TeletextScreenRenderer;
import org.javabeeb.util.SystemStatus;
import org.javabeeb.util.Util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public final class Screen implements ClockListener {

    private static final boolean DOUBLE_BUFFERED = true;

    private static final int IMAGE_WIDTH = 640;
    private static final int IMAGE_HEIGHT = 512;

    private final SystemStatus systemStatus;
    private final BBCMicro bbc;
    private final VideoULA videoULA;
    private final SystemVIA systemVIA;
    private final SystemPalette palette;
    private final List<IntConsumer> keyUpListeners = new ArrayList<>();
    private final List<BiConsumer<Integer, Boolean>> keyDownListeners = new ArrayList<>();

    private ScreenRenderer renderer;
    private final ScreenRenderer graphicsRenderer;
    private final TeletextScreenRenderer teletextRenderer;

    private final BufferedImage image0 = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final BufferedImage image1 = DOUBLE_BUFFERED ? new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB) : null;

    private ScreenImageConsumer imageConsumer;

    private volatile boolean paused;

    public Screen(
            final SystemStatus systemStatus,
            final BBCMicro bbc,
            final Memory memory,
            final VideoULA videoULA,
            final Crtc6845 crtc6845,
            final SystemVIA systemVIA
    ) {
        this.systemStatus = Objects.requireNonNull(systemStatus);
        this.bbc = Objects.requireNonNull(bbc);
        this.palette = Objects.requireNonNull(bbc.getPalette());
        this.videoULA = Objects.requireNonNull(videoULA);
        this.systemVIA = Objects.requireNonNull(systemVIA);
        this.graphicsRenderer = new GraphicsModeScreenRenderer(this, memory, systemVIA, crtc6845, videoULA);
        this.teletextRenderer = new TeletextScreenRenderer(memory, systemVIA, crtc6845, videoULA);
    }

    private int imageIndex;
    private Point imageOrigin;

    private BufferedImage getImageToPaint() {
        return !DOUBLE_BUFFERED || (imageIndex & 1) == 0 ? image0 : image1;
    }

    private BufferedImage getImageToShow() {
        return !DOUBLE_BUFFERED || (imageIndex & 1) != 0 ? image0 : image1;
    }

    @Override
    public void tick(final ClockDefinition clockDefinition, final long elapsedNanos) {
        if (renderer != null && renderer.isClockBased()) {
            renderer.tick(getImageToPaint(), clockDefinition, elapsedNanos);
        }
    }

    public ScreenImageConsumer getScreenImageConsumert() {
        return imageConsumer;
    }

    public void setScreenImageConsumer(final ScreenImageConsumer screenImageConsumer) {
        this.imageConsumer = screenImageConsumer;
    }

    public int getImageWidth() {
        return IMAGE_WIDTH;
    }

    public int getImageHeight() {
        return IMAGE_HEIGHT;
    }

    public void imageReady(final Point origin, final long timeNanos) {
        swapImages();
        this.imageOrigin = origin;
        imageConsumer.setImage(getImageToShow(), imageOrigin);
    }

    private void swapImages() {
        imageIndex++;
    }

    public void newFrame() {
        if (videoULA.isTeletext()) {
            renderer = teletextRenderer;
        } else {
            renderer = graphicsRenderer;
        }

        if (renderer != null) {
            if (renderer.isClockBased()) {
                final BufferedImage image = getImageToPaint();
                Util.fillRect(image.getWritableTile(0, 0).getDataBuffer(), palette.getColour(0).getRGB(), 0, 0, image.getWidth(), image.getHeight(), image.getWidth());
                renderer.newFrame();
            } else {
                renderer.refreshWholeImage(getImageToPaint());
                swapImages();
                if (imageConsumer != null) {
                    imageConsumer.setImage(getImageToShow(), null);
                }
            }
        }
    }

    @Override
    public void setPaused(boolean paused) {
        this.paused = paused;
        if (imageConsumer != null) {
            imageConsumer.setImage(getImageToShow(), imageOrigin);
            imageConsumer.setPaused(paused);
        }
    }
}
