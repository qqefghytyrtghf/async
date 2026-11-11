package tech.renatclient.module.list.render.hud.renderers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import tech.renatclient.util.draggable.DragManager;
import tech.renatclient.util.draggable.Draggable;
import tech.renatclient.util.render.providers.ColorProvider;
import tech.renatclient.util.render.renderers.DrawUtil;

public class QuickElementMenu {
    private static final float WIDTH = 116.0f;
    private static final float PADDING = 6.0f;
    private static final float TITLE_HEIGHT = 14.0f;
    private static final float SLIDER_BLOCK = 21.0f;
    private static final float TRACK_HEIGHT = 3.0f;
    private static final float KNOB_SIZE = 6.0f;
    private static final float BUTTON_HEIGHT = 12.0f;
    private static final float MENU_TEXT = 6.5f;
    private static final int SLIDER_COUNT = 3;
    private static final float HEIGHT = PADDING + TITLE_HEIGHT + SLIDER_BLOCK * SLIDER_COUNT + BUTTON_HEIGHT + PADDING;

    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2.0f;
    private static final float MIN_OPACITY = 0.1f;
    private static final float MAX_OPACITY = 1.0f;
    private static final float MIN_GLOW = 0.0f;
    private static final float MAX_GLOW = 2.0f;

    private static final int SLIDER_NONE = 0;
    private static final int SLIDER_SCALE = 1;
    private static final int SLIDER_OPACITY = 2;
    private static final int SLIDER_GLOW = 3;

    public record Target(String title, Draggable drag) {
    }

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private Target target;
    private float x;
    private float y;
    private int activeSlider = SLIDER_NONE;

    public boolean isOpen() {
        return target != null;
    }

    public void close() {
        if (activeSlider != SLIDER_NONE) {
            DragManager.save();
        }
        target = null;
        activeSlider = SLIDER_NONE;
    }

    public boolean handleClick(double mouseX, double mouseY, int button, Target hovered) {
        if (isOpen() && isInside(mouseX, mouseY)) {
            if (button == 0) {
                clickInside((float) mouseX, (float) mouseY);
            }
            return true;
        }
        if (button == 1 && hovered != null) {
            if (isOpen() && hovered.drag() == target.drag()) {
                close();
            } else {
                open(hovered, (float) mouseX, (float) mouseY);
            }
            return true;
        }
        if (isOpen()) {
            close();
        }
        return false;
    }

    public void render() {
        if (!isOpen()) return;
        if (!(mc.currentScreen instanceof ChatScreen)) {
            close();
            return;
        }
        clampToScreen();
        updateActiveSlider();

        Draggable drag = target.drag();
        QuickStyle.reset();
        QuickStyle.drawPanel(x, y, WIDTH, HEIGHT, QuickStyle.RADIUS, 255);
        drawMenuText(target.title(), x + PADDING, y + PADDING + 1.0f, QuickStyle.white(255));

        float sliderY = y + PADDING + TITLE_HEIGHT;
        drawSlider("Размер", Math.round(drag.getScale() * 100.0f) + "%",
                (drag.getScale() - MIN_SCALE) / (MAX_SCALE - MIN_SCALE), sliderY);
        drawSlider("Прозрачность", Math.round(drag.getOpacity() * 100.0f) + "%",
                (drag.getOpacity() - MIN_OPACITY) / (MAX_OPACITY - MIN_OPACITY), sliderY + SLIDER_BLOCK);
        drawSlider("Свечение", Math.round(drag.getGlow() * 100.0f) + "%",
                (drag.getGlow() - MIN_GLOW) / (MAX_GLOW - MIN_GLOW), sliderY + SLIDER_BLOCK * 2.0f);

        float buttonY = sliderY + SLIDER_BLOCK * SLIDER_COUNT;
        DrawUtil.drawRound(x + PADDING, buttonY, WIDTH - PADDING * 2.0f, BUTTON_HEIGHT, 3.0f, ColorProvider.rgba(255, 255, 255, 22));
        String resetText = "Сбросить";
        float resetWidth = QuickStyle.text().getWidth(resetText, MENU_TEXT);
        drawMenuText(resetText, x + (WIDTH - resetWidth) / 2.0f, buttonY + (BUTTON_HEIGHT - MENU_TEXT) / 2.0f - 0.5f, QuickStyle.white(230));
    }

    private void open(Target hovered, float mouseX, float mouseY) {
        if (activeSlider != SLIDER_NONE) {
            DragManager.save();
        }
        target = hovered;
        activeSlider = SLIDER_NONE;
        x = mouseX + 4.0f;
        y = mouseY + 4.0f;
        clampToScreen();
    }

    private void clickInside(float mouseX, float mouseY) {
        float sliderY = y + PADDING + TITLE_HEIGHT;
        float buttonY = sliderY + SLIDER_BLOCK * SLIDER_COUNT;
        if (mouseY >= sliderY && mouseY < buttonY) {
            activeSlider = SLIDER_SCALE + (int) ((mouseY - sliderY) / SLIDER_BLOCK);
            applySlider(mouseX);
        } else if (mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT) {
            target.drag().setScale(1.0f);
            target.drag().setOpacity(1.0f);
            target.drag().setGlow(1.0f);
            DragManager.save();
        }
    }

    private void updateActiveSlider() {
        if (activeSlider == SLIDER_NONE) return;
        boolean held = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!held) {
            activeSlider = SLIDER_NONE;
            DragManager.save();
            return;
        }
        applySlider((float) (mc.mouse.getX() / mc.getWindow().getScaleFactor()));
    }

    private void applySlider(float mouseX) {
        float trackX = x + PADDING;
        float trackWidth = WIDTH - PADDING * 2.0f;
        float progress = MathHelper.clamp((mouseX - trackX) / trackWidth, 0.0f, 1.0f);
        if (activeSlider == SLIDER_SCALE) {
            target.drag().setScale(step(MIN_SCALE + (MAX_SCALE - MIN_SCALE) * progress));
        } else if (activeSlider == SLIDER_OPACITY) {
            target.drag().setOpacity(step(MIN_OPACITY + (MAX_OPACITY - MIN_OPACITY) * progress));
        } else if (activeSlider == SLIDER_GLOW) {
            target.drag().setGlow(step(MIN_GLOW + (MAX_GLOW - MIN_GLOW) * progress));
        }
    }

    private static float step(float value) {
        return Math.round(value * 20.0f) / 20.0f;
    }

    private void drawSlider(String label, String valueText, float progress, float blockY) {
        progress = MathHelper.clamp(progress, 0.0f, 1.0f);
        drawMenuText(label, x + PADDING, blockY, QuickStyle.gray(255));
        float valueWidth = QuickStyle.text().getWidth(valueText, MENU_TEXT);
        drawMenuText(valueText, x + WIDTH - PADDING - valueWidth, blockY, QuickStyle.white(255));

        float trackX = x + PADDING;
        float trackY = blockY + 11.0f;
        float trackWidth = WIDTH - PADDING * 2.0f;
        DrawUtil.drawRound(trackX, trackY, trackWidth, TRACK_HEIGHT, TRACK_HEIGHT / 2.0f, ColorProvider.rgba(255, 255, 255, 35));
        DrawUtil.drawRound(trackX, trackY, Math.max(TRACK_HEIGHT, trackWidth * progress), TRACK_HEIGHT, TRACK_HEIGHT / 2.0f, QuickStyle.accent(220));
        float knobX = trackX + trackWidth * progress - KNOB_SIZE / 2.0f;
        DrawUtil.drawRound(knobX, trackY + (TRACK_HEIGHT - KNOB_SIZE) / 2.0f, KNOB_SIZE, KNOB_SIZE, KNOB_SIZE / 2.0f, QuickStyle.white(255));
    }

    private void drawMenuText(String text, float textX, float textY, int color) {
        DrawUtil.drawText(QuickStyle.text(), text, textX, textY, color, MENU_TEXT);
    }

    private boolean isInside(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEIGHT;
    }

    private void clampToScreen() {
        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();
        x = MathHelper.clamp(x, 2.0f, Math.max(2.0f, screenWidth - WIDTH - 2.0f));
        y = MathHelper.clamp(y, 2.0f, Math.max(2.0f, screenHeight - HEIGHT - 2.0f));
    }
}
