package dev.async;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class AsyncRender {
    private AsyncRender() {}

    public static void rounded(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(width / 2, height / 2)));
        if (r == 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }
        context.fill(x + r, y, x + width - r, y + height, color);
        for (int row = 0; row < r; row++) {
            int inset = r - row;
            context.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
            context.fill(x + inset, y + height - row - 1, x + width - inset, y + height - row, color);
        }
    }

    public static void glow(DrawContext context, int x, int y, int width, int height, int accent) {
        rounded(context, x - 5, y - 5, width + 10, height + 10, 12, withAlpha(accent, 22));
        rounded(context, x - 2, y - 2, width + 4, height + 4, 10, withAlpha(accent, 45));
    }

    public static void panel(DrawContext context, int x, int y, int width, int height) {
        glow(context, x, y, width, height, AsyncTheme.PURPLE);
        rounded(context, x, y, width, height, 9, AsyncTheme.BORDER);
        rounded(context, x + 1, y + 1, width - 2, height - 2, 8, AsyncTheme.PANEL);
        rounded(context, x + 2, y + 2, width - 4, 49, 7, AsyncTheme.PANEL_TOP);
        context.fill(x + 18, y + 51, x + width - 18, y + 52, AsyncTheme.BORDER);
        context.fill(x + 18, y + 2, x + width - 18, y + 3, withAlpha(AsyncTheme.PURPLE, 180));
    }

    public static void card(DrawContext context, int x, int y, int width, int height, int accent, boolean active) {
        int edge = active ? AsyncTheme.BORDER_BRIGHT : AsyncTheme.BORDER;
        rounded(context, x, y, width, height, 7, edge);
        rounded(context, x + 1, y + 1, width - 2, height - 2, 6,
                active ? AsyncTheme.SURFACE_ALT : AsyncTheme.SURFACE);
        rounded(context, x + 8, y + 10, 3, height - 20, 2, accent);
        if (active) {
            context.fill(x + 15, y + 1, x + width - 12, y + 2, withAlpha(accent, 130));
        }
    }

    public static void text(DrawContext context, TextRenderer renderer, String value, int x, int y, int color) {
        context.drawText(renderer, Text.literal(value), x, y, color, false);
    }

    public static void centered(DrawContext context, TextRenderer renderer, String value, int centerX, int y, int color) {
        context.drawText(renderer, Text.literal(value), centerX - renderer.getWidth(value) / 2, y, color, false);
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}