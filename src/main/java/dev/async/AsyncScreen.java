package dev.async;

import net.minecraft.client.input.MouseInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.input.MouseInput;

import java.util.Locale;

public final class AsyncScreen extends Screen {

    private int ACCENT;
    private int ACCENT_LIGHT;
    private int ACCENT_DARK;

    private static final int PANEL = 0xFF100C19;
    private static final int HEADER = 0xFF171020;
    private static final int CARD = 0xFF171220;
    private static final int CARD_HOVER = 0xFF281936;
    private static final int BUTTON = 0xFF21172D;
    private static final int BUTTON_HOVER = 0xFF39234C;

    private static final int WHITE = 0xFFF8F0FF;
    private static final int MUTED = 0xFFA69AB5;
    private static final int GREEN = 0xFFBCA9D8;
    private static final int RED = 0xFF8F7BAA;
    private static final int BORDER = 0xFF493D58;

    private enum Page {
        HOME, COMBAT, HUD, SETTINGS
    }

    private final TickRate tickRate;
    private final AsyncConfig config;
    private final AsyncAppearanceConfig appearance;

    private Page page = Page.HOME;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private int contentX;
    private int contentWidth;

    private int cardHeight;
    private int cardGap;
    private int firstCardY;
    private int secondCardY;
    private int thirdCardY;
    private int rightCardX;
    private int cardWidth;

    private int currentMouseX;
    private int currentMouseY;

    private boolean listeningForKey;
    private boolean listeningForMenuKey;
    private boolean distanceError;

    private TextFieldWidget distanceField;

    public AsyncScreen(TickRate tickRate, AsyncConfig config) {
        super(Text.literal("Async"));
        this.tickRate = tickRate;
        this.config = config;
        this.appearance = new AsyncAppearanceConfig();
        updatePalette();
    }

    private void updatePalette() {
        ACCENT = appearance.getAccentColor();
        ACCENT_LIGHT = blend(ACCENT, 0xFFFFFFFF, 0.48F);
        ACCENT_DARK = blend(ACCENT, 0xFF090610, 0.58F);
    }

    private int blend(int first, int second, float amount) {
        int a = (int) (((first >>> 24) & 255) * (1.0F - amount) + ((second >>> 24) & 255) * amount);
        int r = (int) (((first >>> 16) & 255) * (1.0F - amount) + ((second >>> 16) & 255) * amount);
        int g = (int) (((first >>> 8) & 255) * (1.0F - amount) + ((second >>> 8) & 255) * amount);
        int b = (int) ((first & 255) * (1.0F - amount) + (second & 255) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void init() {
        clearChildren();

        panelWidth = Math.min(880, width - 28);
        panelWidth = Math.max(300, panelWidth);

        panelHeight = Math.min(560, height - 24);
        panelHeight = Math.max(300, panelHeight);

        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        contentX = panelX + 22;
        contentWidth = panelWidth - 44;

        cardGap = 10;
        cardWidth = (contentWidth - cardGap) / 2;
        rightCardX = contentX + cardWidth + cardGap;

        cardHeight = panelHeight < 480 ? 60 : 70;

        firstCardY = panelY + 139;
        secondCardY = firstCardY + cardHeight + cardGap;
        thirdCardY = secondCardY + cardHeight + cardGap;

        int tabGap = 7;
        int tabWidth = (contentWidth - tabGap * 3) / 4;
        int tabY = panelY + 65;

        addNavigationButton("HOME", Page.HOME, contentX, tabY, tabWidth);
        addNavigationButton("COMBAT", Page.COMBAT,
                contentX + tabWidth + tabGap, tabY, tabWidth);
        addNavigationButton("HUD", Page.HUD,
                contentX + (tabWidth + tabGap) * 2, tabY, tabWidth);
        addNavigationButton("SETTINGS", Page.SETTINGS,
                contentX + (tabWidth + tabGap) * 3, tabY, tabWidth);

        switch (page) {
            case HOME -> buildHome();
            case COMBAT -> buildCombat();
            case HUD -> buildHud();
            case SETTINGS -> buildSettings();
        }
    }

    private void addNavigationButton(
            String label,
            Page target,
            int x,
            int y,
            int buttonWidth
    ) {
        addButton(Text.literal(label), x, y, buttonWidth, 29,
                button -> changePage(target));
    }

    private void changePage(Page newPage) {
        page = newPage;
        distanceField = null;
        distanceError = false;
        listeningForKey = false;
        listeningForMenuKey = false;
        init();
    }

    private int controlY(int cardY) {
        return cardY + cardHeight - 27;
    }

    private void buildHome() {
        int mainY = firstCardY;

        addButton(
                enabledText(),
                contentX + 12,
                mainY + cardHeight - 34,
                contentWidth - 24,
                25,
                button -> {
                    tickRate.toggleEnabled();
                    button.setMessage(enabledText());
                }
        );

        int buttonY = secondCardY + cardHeight - 29;

        addButton(Text.literal("COMBAT  →"),
                contentX + 9, buttonY, cardWidth - 18, 22,
                button -> changePage(Page.COMBAT));

        addButton(Text.literal("HUD  →"),
                rightCardX + 9, buttonY, cardWidth - 18, 22,
                button -> changePage(Page.HUD));
    }

    private void buildCombat() {
        int buttonHeight = 23;

        addButton(attackText(), contentX + 9,
                controlY(firstCardY), cardWidth - 18, buttonHeight,
                button -> {
                    tickRate.toggleAttackMode();
                    button.setMessage(attackText());
                });

        addButton(targetText(), rightCardX + 9,
                controlY(firstCardY), cardWidth - 18, buttonHeight,
                button -> {
                    tickRate.toggleTargetMode();
                    button.setMessage(targetText());
                });

        addButton(weaponText(), contentX + 9,
                controlY(secondCardY), cardWidth - 18, buttonHeight,
                button -> {
                    tickRate.toggleWeaponMode();
                    button.setMessage(weaponText());
                });

        int distanceY = controlY(secondCardY);

        distanceField = new TextFieldWidget(
                textRenderer,
                rightCardX + 9,
                distanceY,
                Math.max(35, cardWidth - 66),
                buttonHeight,
                Text.literal("Distance")
        );

        distanceField.setMaxLength(5);
        distanceField.setText(String.format(
                Locale.ROOT, "%.1f", config.getAttackDistance()));

        addDrawableChild(distanceField);

        addButton(Text.literal("OK"),
                rightCardX + cardWidth - 51,
                distanceY, 42, buttonHeight,
                button -> applyDistance());

        addButton(keyToggleText(), contentX + 9,
                controlY(thirdCardY), cardWidth - 18, buttonHeight,
                button -> {
                    tickRate.toggleKeyToggleEnabled();
                    button.setMessage(keyToggleText());
                });

        addButton(triggerKeyText(), rightCardX + 9,
                controlY(thirdCardY), cardWidth - 18, buttonHeight,
                button -> {
                    listeningForKey = true;
                    listeningForMenuKey = false;
                    button.setMessage(Text.literal("PRESS A KEY..."));
                });
    }

    private void buildHud() {
        addButton(statusText(), contentX + 9,
                controlY(firstCardY), cardWidth - 18, 23,
                button -> {
                    config.setShowStatus(!config.isShowStatus());
                    button.setMessage(statusText());
                });

        addButton(menuKeyText(), rightCardX + 9,
                controlY(firstCardY), cardWidth - 18, 23,
                button -> {
                    listeningForMenuKey = true;
                    listeningForKey = false;
                    button.setMessage(Text.literal("PRESS A KEY..."));
                });
    }

    private void buildSettings() {
        String color = String.format(Locale.ROOT, "#%06X",
                appearance.getAccentColor() & 0xFFFFFF);

        addButton(Text.literal("CLICKGUI COLOR: " + color),
                contentX + 9, controlY(firstCardY), contentWidth - 18, 23,
                button -> {
                    appearance.nextAccentColor();
                    updatePalette();
                    button.setMessage(Text.literal("CLICKGUI COLOR: "
                            + String.format(Locale.ROOT, "#%06X",
                            appearance.getAccentColor() & 0xFFFFFF)));
                });

        int sizeY = controlY(secondCardY);
        int smallWidth = Math.min(70, Math.max(48, contentWidth / 5));

        addButton(Text.literal("SIZE  -"),
                contentX + 9, sizeY, smallWidth, 23,
                button -> appearance.changeScale(-0.05F));

        addButton(Text.literal("SIZE  +"),
                contentX + contentWidth - smallWidth - 9,
                sizeY, smallWidth, 23,
                button -> appearance.changeScale(0.05F));
    }

    private void addButton(
            Text message,
            int x,
            int y,
            int buttonWidth,
            int buttonHeight,
            ButtonWidget.PressAction action
    ) {
        addDrawableChild(
                ButtonWidget.builder(message, action)
                        .dimensions(x, y, buttonWidth, buttonHeight)
                        .build()
        );
    }

    private void applyDistance() {
        if (distanceField == null) return;

        String value = distanceField.getText()
                .trim()
                .replace(',', '.');

        try {
            double distance = Double.parseDouble(value);

            if (Double.isFinite(distance)
                    && distance >= 0.5
                    && distance <= 4.0) {

                config.setAttackDistance(distance);
                distanceError = false;

                distanceField.setText(String.format(
                        Locale.ROOT, "%.1f", distance));
            } else {
                distanceError = true;
            }
        } catch (NumberFormatException ignored) {
            distanceError = true;
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();

        if (listeningForKey || listeningForMenuKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listeningForKey = false;
                listeningForMenuKey = false;
                init();
                return true;
            }

            int key = (
                    keyCode == GLFW.GLFW_KEY_BACKSPACE
                            || keyCode == GLFW.GLFW_KEY_DELETE
            ) ? GLFW.GLFW_KEY_UNKNOWN : keyCode;

            if (listeningForMenuKey) {
                config.setMenuKey(key);
                listeningForMenuKey = false;
            } else {
                tickRate.setToggleKey(key);
                listeningForKey = false;
            }

            init();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        int x = panelX;
        int y = panelY;
        int right = x + panelWidth;
        int bottom = y + panelHeight;

        double scale = appearance.getScale();

        double scaledMouseX = width / 2.0
                + (mouseX - width / 2.0) / scale;

        double scaledMouseY = height / 2.0
                + (mouseY - height / 2.0) / scale;

        currentMouseX = (int) scaledMouseX;
        currentMouseY = (int) scaledMouseY;

        context.getMatrices().pushMatrix();

        context.getMatrices().translate(
                width / 2.0f,
                height / 2.0f
        );

        context.getMatrices().scale(
                (float) scale,
                (float) scale
        );

        context.getMatrices().translate(
                -width / 2.0f,
                -height / 2.0f
        );

        drawRounded(context, x, y, panelWidth, panelHeight, 12, PANEL);
        context.fill(x + 2, y + 2, x + panelWidth - 2, y + 50, HEADER);

        drawSimpleBorder(context, x, y, panelWidth, panelHeight);
        drawHeader(context, x, y, right);

        drawRounded(context,
                contentX - 7, y + 59,
                contentWidth + 14, 39, 7, HEADER);

        drawSimpleBorder(context,
                contentX - 7, y + 59,
                contentWidth + 14, 39);

        context.fill(contentX - 2, y + 97,
                contentX + contentWidth + 2, y + 98,
                ACCENT_DARK);

        drawPageHeading(context, y);

        switch (page) {
            case HOME -> drawHome(context);
            case COMBAT -> drawCombat(context);
            case HUD -> drawHud(context);
            case SETTINGS -> drawSettings(context);
        }

        drawFooter(context, x, y, right, bottom);

        super.render(
                context,
                (int) scaledMouseX,
                (int) scaledMouseY,
                delta
        );

        drawCustomButtons(context);

        if (distanceError && page == Page.COMBAT) {
            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("Allowed distance: 0.5 - 4.0"),
                    rightCardX + 10,
                    secondCardY + cardHeight + 2,
                    RED
            );
        }

        if (listeningForKey || listeningForMenuKey) {
            drawKeyListening(context);
        }

        context.getMatrices().popMatrix();
    }

    private void drawSimpleBorder(
            DrawContext context,
            int x,
            int y,
            int w,
            int h
    ) {
        context.fill(x, y, x + w, y + 1, BORDER);
        context.fill(x, y + h - 1, x + w, y + h, BORDER);
        context.fill(x, y, x + 1, y + h, BORDER);
        context.fill(x + w - 1, y, x + w, y + h, BORDER);
    }

    private void drawHeader(
            DrawContext context,
            int x,
            int y,
            int right
    ) {
        drawRounded(context,
                x + 17, y + 10, 20, 20, 6,
                0xFF39214D);

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("A"),
                x + 27,
                y + 16,
                ACCENT_LIGHT
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("ASYNC"),
                x + 45,
                y + 10,
                WHITE
        );

        context.drawText(
                textRenderer,
                Text.literal("CLICKGUI"),
                x + 45,
                y + 24,
                ACCENT_LIGHT,
                false
        );

        String version = "v1.0.0";

        context.drawText(
                textRenderer,
                Text.literal(version),
                right - textRenderer.getWidth(version) - 19,
                y + 16,
                ACCENT_LIGHT,
                false
        );

        context.drawText(
                textRenderer,
                Text.literal("OPTIMIZATION CLIENT"),
                x + 19,
                y + 39,
                MUTED,
                false
        );

        context.fill(x + 2, y + 49,
                x + panelWidth - 2, y + 51, ACCENT_DARK);
    }

    private void drawPageHeading(DrawContext context, int y) {
        String title = switch (page) {
            case HOME -> "CONTROL CENTER";
            case COMBAT -> "COMBAT";
            case HUD -> "HUD CONTROL";
            case SETTINGS -> "SETTINGS";
        };

        String subtitle = switch (page) {
            case HOME -> "Your game. Your settings. Your control.";
            case COMBAT -> "Configure your existing Async options.";
            case HUD -> "Customize your interface.";
            case SETTINGS -> "Manage your Async configuration.";
        };

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(title),
                contentX,
                y + 110,
                ACCENT_LIGHT
        );

        context.drawText(
                textRenderer,
                Text.literal(subtitle),
                contentX,
                y + 124,
                MUTED,
                false
        );
    }

    private void drawHome(DrawContext context) {
        drawControlCard(context,
                contentX, firstCardY, contentWidth, cardHeight,
                "TRIGGERBOT", "Current module status",
                tickRate.isEnabled() ? GREEN : RED);

        drawControlCard(context,
                contentX, secondCardY, cardWidth, cardHeight,
                "COMBAT", "Existing combat settings", ACCENT);

        drawControlCard(context,
                rightCardX, secondCardY, cardWidth, cardHeight,
                "HUD", "Interface and status settings", ACCENT);

        int overviewY = secondCardY + cardHeight + 15;

        context.drawText(
                textRenderer,
                Text.literal("SYSTEM OVERVIEW"),
                contentX,
                overviewY,
                ACCENT_LIGHT,
                false
        );

        context.drawText(
                textRenderer,
                attackText(),
                contentX,
                overviewY + 15,
                MUTED,
                false
        );

        context.drawText(
                textRenderer,
                targetText(),
                contentX + 145,
                overviewY + 15,
                MUTED,
                false
        );

        context.drawText(
                textRenderer,
                weaponText(),
                contentX + 285,
                overviewY + 15,
                MUTED,
                false
        );

        String distance = String.format(
                Locale.ROOT,
                "%.1f",
                config.getAttackDistance()
        );

        context.drawText(
                textRenderer,
                Text.literal("DISTANCE: " + distance),
                contentX,
                overviewY + 29,
                ACCENT_LIGHT,
                false
        );
    }

    private void drawCombat(DrawContext context) {
        drawControlCard(context,
                contentX, firstCardY, cardWidth, cardHeight,
                "ATTACK MODE", "Current attack setting", ACCENT);

        drawControlCard(context,
                rightCardX, firstCardY, cardWidth, cardHeight,
                "TARGET MODE", "Current target setting", ACCENT);

        drawControlCard(context,
                contentX, secondCardY, cardWidth, cardHeight,
                "WEAPON MODE", "Current weapon setting", ACCENT);

        drawControlCard(context,
                rightCardX, secondCardY, cardWidth, cardHeight,
                "ATTACK DISTANCE", "Allowed range: 0.5 - 4.0",
                ACCENT);

        drawControlCard(context,
                contentX, thirdCardY, cardWidth, cardHeight,
                "KEY TOGGLE", "Keyboard switching",
                tickRate.isKeyToggleEnabled() ? GREEN : RED);

        drawControlCard(context,
                rightCardX, thirdCardY, cardWidth, cardHeight,
                "TRIGGER KEY", "Assigned activation key",
                ACCENT);
    }

    private void drawHud(DrawContext context) {
        drawControlCard(context,
                contentX, firstCardY, cardWidth, cardHeight,
                "STATUS DISPLAY", "Отображение Надписи вкл/выкл",
                config.isShowStatus() ? GREEN : RED);

        drawControlCard(context,
                rightCardX, firstCardY, cardWidth, cardHeight,
                "MENU KEY", "Assigned menu opening key",
                ACCENT);
    }

    private void drawSettings(DrawContext context) {
        String color = String.format(Locale.ROOT, "#%06X",
                appearance.getAccentColor() & 0xFFFFFF);

        String size = Math.round(appearance.getScale() * 100.0F) + "%";

        drawControlCard(context,
                contentX, firstCardY, contentWidth, cardHeight,
                "CLICKGUI COLOR", "Selected purple shade: " + color,
                ACCENT);

        drawControlCard(context,
                contentX, secondCardY, contentWidth, cardHeight,
                "CLICKGUI SIZE", "Current interface size: " + size,
                ACCENT);
    }

    private void drawControlCard(
            DrawContext context,
            int x,
            int y,
            int w,
            int h,
            String title,
            String subtitle,
            int accent
    ) {
        boolean hovered = mouseInside(x, y, w, h);

        drawRounded(context, x, y, w, h, 8,
                hovered ? CARD_HOVER : CARD);

        drawRounded(context,
                x + 9, y + 9, 3, h - 18, 2,
                accent);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(title),
                x + 19,
                y + 8,
                WHITE
        );

        context.drawText(
                textRenderer,
                Text.literal(subtitle),
                x + 19,
                y + 21,
                MUTED,
                false
        );

        context.fill(
                x + 19,
                y + h - 31,
                x + w - 10,
                y + h - 30,
                accent
        );
    }

    private void drawFooter(
            DrawContext context,
            int x,
            int y,
            int right,
            int bottom
    ) {
        context.fill(
                x + 16,
                bottom - 39,
                right - 16,
                bottom - 38,
                BORDER
        );

        drawRounded(context,
                x + 19, bottom - 24, 8, 8, 3,
                tickRate.isEnabled() ? GREEN : RED);

        String status = tickRate.isEnabled()
                ? "MOD ENABLED"
                : "MOD DISABLED";

        context.drawText(
                textRenderer,
                Text.literal(status),
                x + 34,
                bottom - 24,
                WHITE,
                false
        );

        String minecraft = "Minecraft 1.21.11";

        context.drawText(
                textRenderer,
                Text.literal(minecraft),
                x + panelWidth / 2
                        - textRenderer.getWidth(minecraft) / 2,
                bottom - 24,
                MUTED,
                false
        );

        String footer = "ASYNC v1.0.0";

        context.drawText(
                textRenderer,
                Text.literal(footer),
                right - textRenderer.getWidth(footer) - 19,
                bottom - 24,
                ACCENT_LIGHT,
                false
        );
    }

    private void drawCustomButtons(DrawContext context) {
        for (var child : children()) {
            if (!(child instanceof ButtonWidget button)) {
                continue;
            }

            int bx = button.getX();
            int by = button.getY();
            int bw = button.getWidth();
            int bh = button.getHeight();

            boolean hovered = button.isHovered();
            boolean selected = isSelected(button);

            int fill = hovered ? BUTTON_HOVER : BUTTON;
            int edge = hovered || selected ? ACCENT : BORDER;

            drawRounded(context,
                    bx - 1, by - 1,
                    bw + 2, bh + 2,
                    6, edge);

            drawRounded(context,
                    bx, by, bw, bh, 5, fill);

            if (selected) {
                drawRounded(context,
                        bx + 5, by + 5,
                        3, bh - 10, 2,
                        ACCENT_LIGHT);
            }

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    button.getMessage(),
                    bx + bw / 2,
                    by + (bh - 8) / 2,
                    hovered || selected ? ACCENT_LIGHT : WHITE
            );
        }
    }

    private void drawKeyListening(DrawContext context) {
        String message = "PRESS A KEY  |  ESC TO CANCEL";

        int messageWidth = textRenderer.getWidth(message) + 30;
        int messageX = (width - messageWidth) / 2;
        int messageY = height - 37;

        drawRounded(context,
                messageX, messageY,
                messageWidth, 21,
                6, PANEL);

        drawSimpleBorder(context, messageX, messageY,
                messageWidth, 21);

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(message),
                width / 2,
                messageY + 7,
                ACCENT_LIGHT
        );
    }

    private void drawRounded(
            DrawContext context,
            int x,
            int y,
            int w,
            int h,
            int radius,
            int color
    ) {
        if (w <= 0 || h <= 0) return;

        int r = Math.min(radius, Math.min(w / 2, h / 2));

        for (int row = 0; row < h; row++) {
            int inset = 0;

            if (row < r) {
                int dy = r - row;
                inset = r - (int) Math.sqrt(
                        Math.max(0, r * r - dy * dy));
            } else if (row >= h - r) {
                int dy = row - (h - r - 1);
                inset = r - (int) Math.sqrt(
                        Math.max(0, r * r - dy * dy));
            }

            context.fill(
                    x + inset,
                    y + row,
                    x + w - inset,
                    y + row + 1,
                    color
            );
        }
    }

    private boolean mouseInside(int x, int y, int w, int h) {
        return currentMouseX >= x
                && currentMouseX <= x + w
                && currentMouseY >= y
                && currentMouseY <= y + h;
    }

    private boolean isSelected(ButtonWidget button) {
        String message = button.getMessage().getString();

        return switch (page) {
            case HOME -> message.equals("HOME");
            case COMBAT -> message.equals("COMBAT");
            case HUD -> message.equals("HUD");
            case SETTINGS -> message.equals("SETTINGS");
        };
    }

    private Text enabledText() {
        return Text.literal(
                tickRate.isEnabled()
                        ? "TRIGGERBOT  ON"
                        : "TRIGGERBOT  OFF"
        );
    }

    private Text attackText() {
        String mode = switch (tickRate.getAttackMode()) {
            case NORMAL -> "NORMAL";
            case FAST -> "FAST";
        };

        return Text.literal("ATTACK: " + mode);
    }

    private Text targetText() {
        return Text.literal(
                "TARGET: " + (
                        tickRate.getMode() == TargetMode.MOBS
                                ? "MOBS"
                                : "PLAYERS"
                )
        );
    }

    private Text weaponText() {
        return Text.literal(
                "WEAPON: " + (
                        tickRate.getWeaponMode()
                                == TickWeapon.WEAPONS_ONLY
                                ? "WEAPONS"
                                : "ANY ITEM"
                )
        );
    }

    private Text keyToggleText() {
        return Text.literal(
                "KEY TOGGLE: "
                        + (tickRate.isKeyToggleEnabled() ? "ON" : "OFF")
        );
    }

    private Text triggerKeyText() {
        return Text.literal(
                "TRIGGER KEY: " + keyName(tickRate.getToggleKey())
        );
    }

    private Text menuKeyText() {
        return Text.literal(
                "MENU KEY: " + keyName(config.getMenuKey())
        );
    }

    private Text statusText() {
        return Text.literal(
                "STATUS: "
                        + (config.isShowStatus() ? "ON" : "OFF")
        );
    }

    private String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            return "NONE";
        }

        String name = GLFW.glfwGetKeyName(key, 0);

        if (name != null) {
            return name.toUpperCase(Locale.ROOT);
        }

        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R SHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L SHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R CTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L CTRL";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            default -> "KEY " + key;
        };
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double scale = appearance.getScale();

        double x = width / 2.0
                + (click.x() - width / 2.0) / scale;

        double y = height / 2.0
                + (click.y() - height / 2.0) / scale;

        return super.mouseClicked(
                new Click(x, y, new MouseInput(click.button(), 0)),
                doubled
        );
    }

    @Override
    public boolean mouseReleased(Click click) {
        double scale = appearance.getScale();

        double x = width / 2.0
                + (click.x() - width / 2.0) / scale;

        double y = height / 2.0
                + (click.y() - height / 2.0) / scale;

        return super.mouseReleased(
                new Click(x, y, new MouseInput(click.button(), 0))
        );
    }

    @Override
    public void close() {
        tickRate.suppressNextAttack();
        super.close();
    }
}
