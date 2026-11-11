package tech.renatclient.module.list.render.hud.renderers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import tech.renatclient.util.draggable.Draggable;
import tech.renatclient.util.render.builders.Builder;
import tech.renatclient.util.render.builders.states.QuadColorState;
import tech.renatclient.util.render.builders.states.QuadRadiusState;
import tech.renatclient.util.render.builders.states.SizeState;
import tech.renatclient.util.render.math.Animation;
import tech.renatclient.util.render.msdf.Fonts;
import tech.renatclient.util.render.msdf.MsdfFont;
import tech.renatclient.util.render.providers.ColorProvider;
import tech.renatclient.util.render.renderers.DrawUtil;

import java.util.List;
import java.util.function.Supplier;

public final class QuickStyle {
    public static final float TEXT_SIZE = 7.0f;
    public static final float ICON_SIZE = 8.0f;
    public static final float HEADER_HEIGHT = 16.0f;
    public static final float ROW_HEIGHT = 11.0f;
    public static final float PADDING = 6.0f;
    public static final float RADIUS = 4.5f;
    public static final float HEAD_SIZE = 8.0f;
    public static final float EFFECT_SIZE = 9.0f;

    public static final String ICON_LOGO = "N";
    public static final String ICON_STAFF = "A";
    public static final Identifier ICON_HOTKEYS = Identifier.of("mre", "images/quick/util.png");
    public static final GlyphIcon ICON_POTIONS = new GlyphIcon(Fonts.ICONS_HUD, "E");
    public static final GlyphIcon ICON_COOLDOWNS = new GlyphIcon(Fonts.ICONS_HUD, "V");
    public static final String ICON_WARNING = "E";

    public static final int LEVEL_COLOR = ColorProvider.rgba(235, 60, 70, 255);

    private static final float GLOW_RADIUS = 8.0f;
    private static boolean textureFilterApplied;
    private static float scale = 1.0f;
    private static float opacity = 1.0f;
    private static float glow = 1.0f;

    private QuickStyle() {
    }

    public static void begin(Draggable drag) {
        scale = drag.getScale();
        opacity = drag.getOpacity();
        glow = drag.getGlow();
    }

    public static void beginAppearance(Draggable drag) {
        scale = 1.0f;
        opacity = drag.getOpacity();
        glow = drag.getGlow();
    }

    public static void reset() {
        scale = 1.0f;
        opacity = 1.0f;
        glow = 1.0f;
    }

    public static float s(float value) {
        return value * scale;
    }

    public static MsdfFont text() {
        return Fonts.SFBOLD.get();
    }

    public static MsdfFont icons() {
        return Fonts.QUICK.get();
    }

    public static float textWidth(String text) {
        return text().getWidth(text, s(TEXT_SIZE));
    }

    public static void drawText(String text, float x, float y, int color) {
        DrawUtil.drawText(text(), text, x, y, color, s(TEXT_SIZE));
    }

    public static void drawPanel(float x, float y, float w, float h, float radius, int alpha) {
        int backgroundAlpha = (int) (alpha * opacity);
        drawGlow(x, y, w, h, radius, backgroundAlpha);
        DrawUtil.drawRoundBlur(x, y, w, h, radius, ColorProvider.rgba(255, 255, 255, backgroundAlpha), 45);
        DrawUtil.drawRound(x, y, w, h, radius, ColorProvider.rgba(0, 0, 0, (int) (backgroundAlpha * 0.4f)));
    }

    public static void drawGlow(float x, float y, float w, float h, float radius, int alpha) {
        int glowAlpha = MathHelper.clamp((int) (alpha * 0.3f * glow), 0, 255);
        if (glowAlpha <= 2) return;
        int[] colors = ColorProvider.getOrbitalRect(ColorProvider.getThemeColor(), ColorProvider.getThemeColorTwo(), 600.0, glowAlpha);
        Builder.glow()
                .size(new SizeState(w + GLOW_RADIUS * 2.0f - 6.0f, h + GLOW_RADIUS * 2.0f - 6.0f))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(colors[0], colors[1], colors[2], colors[3]))
                .glowRadius(GLOW_RADIUS)
                .softness(0f)
                .intensity(2.0f)
                .additive(true)
                .build()
                .render(x - GLOW_RADIUS + 3.0f, y - GLOW_RADIUS + 3.0f);
    }

    public static void drawIconTexture(DrawContext context, Identifier texture, float x, float y, float size, int color) {
        var abstractTexture = MinecraftClient.getInstance().getTextureManager().getTexture(texture);
        if (!textureFilterApplied) {
            abstractTexture.setFilter(true, false);
            textureFilterApplied = true;
        }
        Builder.texture()
                .size(new SizeState(size, size))
                .radius(new QuadRadiusState(0.0f))
                .color(new QuadColorState(color))
                .texture(0.0f, 0.0f, 1.0f, 1.0f, abstractTexture.getGlId())
                .smoothness(1f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x, y);
    }

    public static void drawSprite(DrawContext context, Sprite sprite, float x, float y, float size, int alpha) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0.0f);
        context.getMatrices().scale(size / 16.0f, size / 16.0f, 1.0f);
        context.drawSpriteStretched(RenderLayer::getGuiTextured, sprite, 0, 0, 16, 16, (alpha << 24) | 0xFFFFFF);
        context.getMatrices().pop();
    }

    public static int accent(int alpha) {
        return ColorProvider.setAlpha(ColorProvider.getThemeColor(), alpha);
    }

    public static int white(int alpha) {
        return ColorProvider.rgba(236, 236, 240, alpha);
    }

    public static int gray(int alpha) {
        return ColorProvider.rgba(150, 150, 158, alpha);
    }

    public static float textY(float y, float height) {
        return y + (height - s(TEXT_SIZE)) / 2.0f - s(0.5f);
    }

    public static float iconY(float y, float height) {
        return y + (height - s(ICON_SIZE)) / 2.0f + s(0.25f);
    }

    public static int toAlpha(float progress) {
        return MathHelper.clamp((int) (255.0f * progress), 0, 255);
    }

    public record Row(String left, String leftExtra, String right, int rightColor, String suffix, float anim, Identifier skin, Sprite sprite) {
        public static Row of(String left, String right, float anim) {
            return new Row(left, null, right, 0, null, anim, null, null);
        }

        public static Row player(String left, String right, int rightColor, String suffix, float anim, Identifier skin) {
            return new Row(left, null, right, rightColor, suffix, anim, skin, null);
        }

        public static Row effect(String left, String level, String right, int rightColor, float anim, Sprite sprite) {
            return new Row(left, level, right, rightColor, null, anim, null, sprite);
        }
    }

    public record GlyphIcon(Supplier<MsdfFont> font, String glyph) {
    }

    public static void drawList(DrawContext context, Draggable drag, Animation widthAnim, String icon, String title, List<Row> rows, int alpha) {
        drawList(context, drag, widthAnim, new GlyphIcon(Fonts.QUICK, icon), null, title, rows, alpha);
    }

    public static void drawList(DrawContext context, Draggable drag, Animation widthAnim, GlyphIcon icon, String title, List<Row> rows, int alpha) {
        drawList(context, drag, widthAnim, icon, null, title, rows, alpha);
    }

    public static void drawList(DrawContext context, Draggable drag, Animation widthAnim, Identifier iconTexture, String title, List<Row> rows, int alpha) {
        drawList(context, drag, widthAnim, null, iconTexture, title, rows, alpha);
    }

    public static void drawRows(DrawContext context, Draggable drag, Animation widthAnim, List<Row> rows, int alpha) {
        drawList(context, drag, widthAnim, null, null, null, rows, alpha);
    }

    private static void drawList(DrawContext context, Draggable drag, Animation widthAnim, GlyphIcon icon, Identifier iconTexture, String title, List<Row> rows, int alpha) {
        begin(drag);
        boolean hasHeader = title != null;
        float padding = s(PADDING);
        float rowHeight = s(ROW_HEIGHT);
        float headerHeight = hasHeader ? s(HEADER_HEIGHT) : s(3.0f);
        float iconWidth = !hasHeader ? 0.0f : iconTexture != null ? s(ICON_SIZE) : icon.font().get().getWidth(icon.glyph(), s(ICON_SIZE));
        float minWidth = hasHeader
                ? padding + iconWidth + s(5.0f) + textWidth(title) + padding + s(24.0f)
                : s(60.0f);

        float targetWidth = minWidth;
        float rowsHeight = 0.0f;
        for (Row row : rows) {
            float rowWidth = padding + leadWidth(row) + leftWidth(row) + s(14.0f) + rightWidth(row) + padding;
            targetWidth = Math.max(targetWidth, rowWidth);
            rowsHeight += rowHeight * MathHelper.clamp(row.anim(), 0.0f, 1.0f);
        }

        float width = Math.max(minWidth, widthAnim.run(targetWidth));
        float height = headerHeight + rowsHeight + (rowsHeight > 0.5f ? s(3.0f) : 0.0f);
        float x = drag.getX();
        float y = drag.getY();

        drawPanel(x, y, width, height, s(RADIUS), alpha);
        if (hasHeader) {
            if (iconTexture != null) {
                drawIconTexture(context, iconTexture, x + padding, y + (headerHeight - s(ICON_SIZE)) / 2.0f, s(ICON_SIZE), accent(alpha));
            } else {
                DrawUtil.drawText(icon.font().get(), icon.glyph(), x + padding, iconY(y, headerHeight), accent(alpha), s(ICON_SIZE));
            }
            drawText(title, x + padding + iconWidth + s(5.0f), textY(y, headerHeight), white(alpha));

            if (rowsHeight > 0.5f) {
                DrawUtil.drawRound(x + padding, y + headerHeight - s(0.5f), width - padding * 2.0f, s(0.5f), 0.0f,
                        ColorProvider.rgba(255, 255, 255, (int) (alpha * 0.08f)));
            }
        }

        float currentY = y + headerHeight + (hasHeader ? s(1.0f) : 0.0f);
        for (Row row : rows) {
            float rowAnim = MathHelper.clamp(row.anim(), 0.0f, 1.0f);
            int rowAlpha = (int) (alpha * rowAnim);
            if (rowAlpha > 4) {
                float textX = x + padding;
                if (row.skin() != null) {
                    drawHead(context, row.skin(), textX, currentY + (rowHeight - s(HEAD_SIZE)) / 2.0f, s(HEAD_SIZE), rowAlpha);
                } else if (row.sprite() != null) {
                    drawSprite(context, row.sprite(), textX, currentY + (rowHeight - s(EFFECT_SIZE)) / 2.0f, s(EFFECT_SIZE), rowAlpha);
                }
                textX += leadWidth(row);

                float textY = textY(currentY, rowHeight);
                drawText(row.left(), textX, textY, white(rowAlpha));
                if (row.leftExtra() != null) {
                    drawText(row.leftExtra(), textX + textWidth(row.left()) + s(3.0f), textY, ColorProvider.setAlpha(LEVEL_COLOR, rowAlpha));
                }

                float rightX = x + width - padding - rightWidth(row);
                int rightColor = row.rightColor() == 0 ? gray(rowAlpha) : ColorProvider.setAlpha(row.rightColor(), rowAlpha);
                drawText(row.right(), rightX, textY, rightColor);
                if (row.suffix() != null) {
                    drawText(row.suffix(), rightX + textWidth(row.right()) + s(4.0f), textY, white(rowAlpha));
                }
            }
            currentY += rowHeight * rowAnim;
        }

        drag.setWidth(width);
        drag.setHeight(height);
        reset();
    }

    private static float leadWidth(Row row) {
        if (row.skin() != null) return s(HEAD_SIZE + 4.0f);
        if (row.sprite() != null) return s(EFFECT_SIZE + 4.0f);
        return 0.0f;
    }

    private static float leftWidth(Row row) {
        float width = textWidth(row.left());
        if (row.leftExtra() != null) {
            width += s(3.0f) + textWidth(row.leftExtra());
        }
        return width;
    }

    private static float rightWidth(Row row) {
        float width = textWidth(row.right());
        if (row.suffix() != null) {
            width += s(4.0f) + textWidth(row.suffix());
        }
        return width;
    }

    private static void drawHead(DrawContext context, Identifier skin, float x, float y, float size, int alpha) {
        int textureId = MinecraftClient.getInstance().getTextureManager().getTexture(skin).getGlId();
        Builder.texture()
                .size(new SizeState(size, size))
                .radius(new QuadRadiusState(s(2.0f)))
                .color(new QuadColorState(ColorProvider.setAlpha(-1, alpha)))
                .texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, textureId)
                .smoothness(1f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x, y);
    }
}
