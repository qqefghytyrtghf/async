package dev.async;

public final class AsyncTheme {
    private AsyncTheme() {}

    public static final int BACKDROP = 0xD9080710;
    public static final int PANEL = 0xFF0D0B16;
    public static final int PANEL_TOP = 0xFF151020;
    public static final int PANEL_BOTTOM = 0xFF100D19;
    public static final int SURFACE = 0xFF171321;
    public static final int SURFACE_ALT = 0xFF1D172B;
    public static final int SURFACE_HOVER = 0xFF292039;
    public static final int BORDER = 0xFF38294B;

    private static final int DEFAULT_ACCENT = 0xFFB65CFF;
    private static final int[] PALETTE = {
            0xFFB65CFF,
            0xFF43D9E8,
            0xFF4F8CFF,
            0xFF4DE0A1,
            0xFFFFA34D,
            0xFFFF5B72,
            0xFFFF5FC8
    };
    private static final String[] PALETTE_NAMES = {
            "PURPLE", "CYAN", "BLUE", "GREEN", "ORANGE", "RED", "PINK"
    };

    public static int PURPLE = DEFAULT_ACCENT;
    public static int PURPLE_LIGHT = lighten(DEFAULT_ACCENT, 0.34f);
    public static int PURPLE_DARK = darken(DEFAULT_ACCENT, 0.34f);
    public static int CYAN = DEFAULT_ACCENT;
    public static int GREEN = DEFAULT_ACCENT;
    public static int RED = DEFAULT_ACCENT;
    public static int BORDER_BRIGHT = lighten(DEFAULT_ACCENT, 0.12f);

    public static void setAccent(int color) {
        int selected = normalizeColor(color);
        PURPLE = selected;
        CYAN = selected;
        GREEN = selected;
        RED = selected;
        PURPLE_LIGHT = lighten(selected, 0.38f);
        PURPLE_DARK = darken(selected, 0.34f);
        BORDER_BRIGHT = lighten(selected, 0.14f);
    }

    public static int getDefaultAccent() {
        return DEFAULT_ACCENT;
    }

    public static int nextAccent(int current) {
        int selected = normalizeColor(current);
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == selected) {
                return PALETTE[(i + 1) % PALETTE.length];
            }
        }
        return PALETTE[0];
    }

    public static String getAccentName(int color) {
        int selected = normalizeColor(color);
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == selected) {
                return PALETTE_NAMES[i];
            }
        }
        return PALETTE_NAMES[0];
    }

    public static int normalizeColor(int color) {
        for (int paletteColor : PALETTE) {
            if (paletteColor == color) {
                return paletteColor;
            }
        }
        return DEFAULT_ACCENT;
    }

    private static int lighten(int color, float amount) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        red += Math.round((255 - red) * amount);
        green += Math.round((255 - green) * amount);
        blue += Math.round((255 - blue) * amount);

        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int darken(int color, float amount) {
        int red = Math.round(((color >> 16) & 0xFF) * (1.0f - amount));
        int green = Math.round(((color >> 8) & 0xFF) * (1.0f - amount));
        int blue = Math.round((color & 0xFF) * (1.0f - amount));
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }
}
