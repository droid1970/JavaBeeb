package org.javabeeb.teletext;

import org.javabeeb.screen.SystemPalette;

import java.awt.*;

final class TeletextRenderer {

    private final CellProcessorSet alphaProcessorSet = new CompoundCellProcessorSet(
            new ControlCodeProcessorSet(),
            new TextCellProcessorSet()
    );

    private final CellProcessorSet graphicsProcessorSet = new CompoundCellProcessorSet(
            new ControlCodeProcessorSet(),
            new GraphicsCellProcessorSet()
    );

    private CellProcessorSet cellProcessorSet;
    private int foregroundIndex;
    private Color background;
    private boolean graphicsEnabled;
    private Color graphicsColour;
    private boolean flashing;
    private boolean doubleHeight;
    private boolean conceal;
    private boolean contiguousGraphics;
    private boolean holdGraphics;
    private boolean textShowing = true;
    private boolean bottom = true;

    private SystemPalette systemPalette = SystemPalette.DEFAULT;

    public TeletextRenderer() {
        resetToDefaults();
    }

    public void resetToDefaults() {
        this.cellProcessorSet = alphaProcessorSet;
        this.foregroundIndex = 7;
        this.background = SystemPalette.BLACK;
        this.graphicsEnabled = false;
        this.graphicsColour = SystemPalette.WHITE;
        this.flashing = false;
        this.doubleHeight = false;
        this.conceal = false;
        this.contiguousGraphics = true;
        this.holdGraphics = false;
    }

    public int getForegroundIndex() {
        return foregroundIndex;
    }

    public Color getGraphicsColour() {
        return graphicsColour;
    }

    public void paintCell(final Graphics2D g, final int v, final int x, final int y, final int width, final int height) {
        //
        // Paint background
        //
        g.setColor(background);
        g.fillRect(x, y, width, height);

        final CellProcessor processor = cellProcessorSet.getProcessor(v);
        if (processor != null) {
            processor.process(this, g, x, y, width, height);
        }
    }

    public void enableText(final int colourIndex) {
        foregroundIndex = colourIndex;
        graphicsColour = null;
        graphicsEnabled = false;
        cellProcessorSet = alphaProcessorSet;
    }

    public void enableGraphics(final int colourIndex) {
        graphicsColour = TeletextConstants.getColour(colourIndex);
        graphicsEnabled = true;
        cellProcessorSet = graphicsProcessorSet;
    }

    public void setFlashing(final boolean flashing) {
        this.flashing = flashing;
    }

    public void setDoubleHeight(final boolean doubleHeight) {
        this.doubleHeight = doubleHeight;
        if (doubleHeight) {
            bottom = !bottom;
        } else {
            bottom = false;
        }
    }

    public void concealDisplay() {
        this.conceal = true;
    }

    public boolean isContiguousGraphics() {
        return contiguousGraphics;
    }

    public void setContiguousGraphics(final boolean contiguousGraphics) {
        this.contiguousGraphics = contiguousGraphics;
    }

    public void blackBackground() {
        background = SystemPalette.BLACK;
    }

    public void newBackground() {
        background = TeletextConstants.getColour(foregroundIndex);
    }

    public void setHoldGraphics(boolean holdGraphics) {
        this.holdGraphics = holdGraphics;
    }

    public boolean isTextSteady() {
        return !flashing;
    }

    public boolean isTextShowing() {
        return textShowing;
    }

    public void setTextShowing(final boolean textShowing) {
        this.textShowing = textShowing;
    }

    public boolean isDoubleHeight() {
        return doubleHeight;
    }

    public boolean isBottom() {
        return bottom;
    }

    public void setBottom(boolean bottom) {
        this.bottom = bottom;
    }
}
