package org.javabeeb.teletext;

import java.util.HashMap;
import java.util.Map;

final class GraphicsCellProcessorSet extends AbstractCellProcessorSet {

    private static final Map<Integer, CellProcessor> MAP = new HashMap<>();
    static {

        int bits = 0;
        int code = 32;
        for (int i = 0; i < 3; i++) {
            registerGraphics(code++, bits++);
        }

        // skip 35 (which is the HASH character)
        registerText(35,
                TeletextAlphaDefinition.HASH
        );

        code = 36;
        bits = 4;
        for (int i = 0; i < 28; i++) {
            registerGraphics(code++, bits++);
        }

        // Is there a bug here?
        registerGraphics(95, 3);
        registerGraphics(96, 32);

        code = 97;
        bits = 33;
        for (int i = 0; i < 31; i++) {
            registerGraphics(code++, bits++);
        }

        bits = 0;
        code = 160;

        for (int i = 0; i < 32; i++) {
            registerGraphics(code++, bits++);
        }

        code = 224;
        for (int i = 0; i < 32; i++) {
            registerGraphics(code++, bits++);
        }

        registerText(64,
                TeletextAlphaDefinition.AT,
                TeletextAlphaDefinition.UPPER_A,
                TeletextAlphaDefinition.UPPER_B,
                TeletextAlphaDefinition.UPPER_C,
                TeletextAlphaDefinition.UPPER_D,
                TeletextAlphaDefinition.UPPER_E,
                TeletextAlphaDefinition.UPPER_F,
                TeletextAlphaDefinition.UPPER_G,
                TeletextAlphaDefinition.UPPER_H,
                TeletextAlphaDefinition.UPPER_I,
                TeletextAlphaDefinition.UPPER_J,
                TeletextAlphaDefinition.UPPER_K,
                TeletextAlphaDefinition.UPPER_L,
                TeletextAlphaDefinition.UPPER_M,
                TeletextAlphaDefinition.UPPER_N,
                TeletextAlphaDefinition.UPPER_O,
                TeletextAlphaDefinition.UPPER_P,
                TeletextAlphaDefinition.UPPER_Q,
                TeletextAlphaDefinition.UPPER_R,
                TeletextAlphaDefinition.UPPER_S,
                TeletextAlphaDefinition.UPPER_T,
                TeletextAlphaDefinition.UPPER_U,
                TeletextAlphaDefinition.UPPER_V,
                TeletextAlphaDefinition.UPPER_W,
                TeletextAlphaDefinition.UPPER_X,
                TeletextAlphaDefinition.UPPER_Y,
                TeletextAlphaDefinition.UPPER_Z,

                TeletextAlphaDefinition.LEFT_ARROW,
                TeletextAlphaDefinition.HALF,
                TeletextAlphaDefinition.RIGHT_ARROW,
                TeletextAlphaDefinition.UP_ARROW
        );

        registerText(192,
                TeletextAlphaDefinition.AT,
                TeletextAlphaDefinition.UPPER_A,
                TeletextAlphaDefinition.UPPER_B,
                TeletextAlphaDefinition.UPPER_C,
                TeletextAlphaDefinition.UPPER_D,
                TeletextAlphaDefinition.UPPER_E,
                TeletextAlphaDefinition.UPPER_F,
                TeletextAlphaDefinition.UPPER_G,
                TeletextAlphaDefinition.UPPER_H,
                TeletextAlphaDefinition.UPPER_I,
                TeletextAlphaDefinition.UPPER_J,
                TeletextAlphaDefinition.UPPER_K,
                TeletextAlphaDefinition.UPPER_L,
                TeletextAlphaDefinition.UPPER_M,
                TeletextAlphaDefinition.UPPER_N,
                TeletextAlphaDefinition.UPPER_O,
                TeletextAlphaDefinition.UPPER_P,
                TeletextAlphaDefinition.UPPER_Q,
                TeletextAlphaDefinition.UPPER_R,
                TeletextAlphaDefinition.UPPER_S,
                TeletextAlphaDefinition.UPPER_T,
                TeletextAlphaDefinition.UPPER_U,
                TeletextAlphaDefinition.UPPER_V,
                TeletextAlphaDefinition.UPPER_W,
                TeletextAlphaDefinition.UPPER_X,
                TeletextAlphaDefinition.UPPER_Y,
                TeletextAlphaDefinition.UPPER_Z,

                TeletextAlphaDefinition.LEFT_ARROW,
                TeletextAlphaDefinition.HALF,
                TeletextAlphaDefinition.RIGHT_ARROW,
                TeletextAlphaDefinition.UP_ARROW,

                TeletextAlphaDefinition.HASH
        );

    }

    private static void registerGraphics(final int code, final int bits) {
        MAP.put(code, new GraphicsCellProcessor(bits));
    }

    private static void registerGraphics(final int code, AlphaDefinition characterSpec) {
        MAP.put(code, new TextCellProcessor(characterSpec));
    }

    private static void registerText(final int startCode, AlphaDefinition... specs) {
        int code = startCode;
        for (AlphaDefinition def : specs) {
            registerGraphics(code++, def);
        }
    }

    GraphicsCellProcessorSet() {
        super(MAP);
    }
}
