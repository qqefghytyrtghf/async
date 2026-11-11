
package dev.async;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class AsyncScreen extends Screen {

    private static final int PURPLE = 0xFFAD45FF;
    private static final int PURPLE_DARK = 0xFF7132A8;
    private static final int LIGHT_PURPLE = 0xFFE4C5FF;
    private static final int BG = 0xFF09080F;
    private static final int HEADER = 0xFF100D19;
    private static final int CARD = 0xFF15121F;
    private static final int CARD_HOVER = 0xFF21182F;
    private static final int BUTTON = 0xFF1C1728;
    private static final int WHITE = 0xFFF8F3FF;
    private static final int MUTED = 0xFF968BA9;
    private static final int GREEN = 0xFF55EBA6;
    private static final int RED = 0xFFFF647C;
    private static final int BORDER = 0xFF39244F;

    private enum Page {
        HOME, COMBAT, HUD, SETTINGS
    }

    private final TickRate tickRate;
    private final AsyncConfig config;

    private Page page = Page.HOME;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentWidth;

    private boolean listeningForKey;
    private boolean listeningForMenuKey;
    private boolean distanceError;

    private TextFieldWidget distanceField;

    private long openedAt;

    public AsyncScreen(TickRate tickRate, AsyncConfig config) {
        super(Text.literal("Async"));
        this.tickRate = tickRate;
        this.config = config;
    }

    @Override
    protected void init() {
        clearChildren();

        panelWidth = Math.min(900, width - 24);
        panelWidth = Math.max(300, panelWidth);

        panelHeight = Math.min(570, height - 20);
        panelHeight = Math.max(340, panelHeight);

        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        contentX = panelX + 22;
        contentWidth = panelWidth - 44;

        int gap = 8;
        int tabWidth = (contentWidth - gap * 3) / 4;
        int tabY = panelY + 65;

        addNavigationButton("HOME", Page.HOME,
                contentX, tabY, tabWidth);

        addNavigationButton("COMBAT", Page.COMBAT,
                contentX + tabWidth + gap, tabY, tabWidth);

        addNavigationButton("HUD", Page.HUD,
                contentX + (tabWidth + gap) * 2,
                tabY, tabWidth);

        addNavigationButton("SETTINGS", Page.SETTINGS,
                contentX + (tabWidth + gap) * 3,
                tabY, tabWidth);

        int top = panelY + 139;
        int cardGap = 12;
        int cardWidth = (contentWidth - cardGap) / 2;
        int cardHeight = 76;
        int rightX = contentX + cardWidth + cardGap;

        switch (page) {
            case HOME -> buildHome(top, cardWidth, cardHeight, cardGap);
            case COMBAT -> buildCombat(top, cardWidth, cardHeight, cardGap);
            case HUD -> buildHud(top, cardWidth, cardHeight, cardGap);
            case SETTINGS -> buildSettings(top, cardWidth, cardHeight, cardGap);
        }

        if (openedAt == 0L) {
            openedAt = System.currentTimeMillis();
        }
    }

    private void addNavigationButton(
            String label,
            Page target,
            int x,
            int y,
            int buttonWidth
    ) {
        addButton(
                Text.literal(label),
                x, y, buttonWidth, 29,
                button -> changePage(target)
        );
    }

    private void changePage(Page newPage) {
        page = newPage;
        distanceField = null;
        distanceError = false;
        listeningForKey = false;
        listeningForMenuKey = false;
        init();
    }

    // --------------------------------------------------
    // HOME
    // --------------------------------------------------

    private void buildHome(
            int top,
            int cardWidth,
            int cardHeight,
            int gap
    ) {
        int rightX = contentX + cardWidth + gap;

        addButton(
                enabledText(),
                contentX + 14,
                top + 55,
                contentWidth - 28,
                29,
                button -> {
                    tickRate.toggleEnabled();
                    button.setMessage(enabledText());
                }
        );

        int secondRow = top + 123;

        addButton(
                Text.literal("COMBAT   OPEN ->"),
                contentX + 10,
                secondRow + 39,
                cardWidth - 20,
                25,
                button -> changePage(Page.COMBAT)
        );

        addButton(
                Text.literal("HUD   OPEN ->"),
                rightX + 10,
                secondRow + 39,
                cardWidth - 20,
                25,
                button -> changePage(Page.HUD)
        );
    }

    // --------------------------------------------------
    // COMBAT
    // --------------------------------------------------

    private void buildCombat(
            int top,
            int cardWidth,
            int cardHeight,
            int gap
    ) {
        int rightX = contentX + cardWidth + gap;

        // Attack mode

        addButton(
                attackText(),
                contentX + 10,
                top + 43,
                cardWidth - 20,
                25,
                button -> {
                    tickRate.toggleAttackMode();
                    button.setMessage(attackText());
                }
        );

        // Target mode

        addButton(
                targetText(),
                rightX + 10,
                top + 43,
                cardWidth - 20,
                25,
                button -> {
                    tickRate.toggleTargetMode();
                    button.setMessage(targetText());
                }
        );

        int secondRow = top + cardHeight + gap;

        // Weapon mode

        addButton(
                weaponText(),
                contentX + 10,
                secondRow + 43,
                cardWidth - 20,
                25,
                button -> {
                    tickRate.toggleWeaponMode();
                    button.setMessage(weaponText());
                }
        );

        // Distance

        distanceField = new TextFieldWidget(
                textRenderer,
                rightX + 10,
                secondRow + 43,
                cardWidth - 85,
                25,
                Text.literal("Distance")
        );

        distanceField.setMaxLength(5);
        distanceField.setText(
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        config.getAttackDistance()
                )
        );

        addDrawableChild(distanceField);

        addButton(
                Text.literal("OK"),
                rightX + cardWidth - 65,
                secondRow + 43,
                55,
                25,
                button -> applyDistance()
        );

        int thirdRow = secondRow + cardHeight + gap;

        // Key toggle

        addButton(
                keyToggleText(),
                contentX + 10,
                thirdRow + 43,
                cardWidth - 20,
                25,
                button -> {
                    tickRate.toggleKeyToggleEnabled();
                    button.setMessage(keyToggleText());
                }
        );

        // Trigger key

        addButton(
                triggerKeyText(),
                rightX + 10,
                thirdRow + 43,
                cardWidth - 20,
                25,
                button -> {
                    listeningForKey = true;
                    listeningForMenuKey = false;
                    button.setMessage(
                            Text.literal("PRESS A KEY...")
                    );
                }
        );
    }

    // --------------------------------------------------
    // HUD
    // --------------------------------------------------

    private void buildHud(
            int top,
            int cardWidth,
            int cardHeight,
            int gap
    ) {
        int rightX = contentX + cardWidth + gap;

        addButton(
                statusText(),
                contentX + 10,
                top + 43,
                cardWidth - 20,
                25,
                button -> {
                    config.setShowStatus(!config.isShowStatus());
                    button.setMessage(statusText());
                }
        );

        addButton(
                menuKeyText(),
                rightX + 10,
                top + 43,
                cardWidth - 20,
                25,
                button -> {
                    listeningForMenuKey = true;
                    listeningForKey = false;
                    button.setMessage(
                            Text.literal("PRESS A KEY...")
                    );
                }
        );
    }

    // --------------------------------------------------
    // SETTINGS
    // --------------------------------------------------

    private void buildSettings(
            int top,
            int cardWidth,
            int cardHeight,
            int gap
    ) {
        int rightX = contentX + cardWidth + gap;

        addButton(
                Text.literal(
                        "TriggerBot: "
                                + (tickRate.isEnabled() ? "ON" : "OFF")
                ),
                contentX + 10,
                top + 43,
                cardWidth - 20,
                25,
                button -> {
                    tickRate.toggleEnabled();
                    button.setMessage(Text.literal(
                            "TriggerBot: "
                                    + (tickRate.isEnabled() ? "ON" : "OFF")
                    ));
                }
        );

        addButton(
                keyToggleText(),
                rightX + 10,
                top + 43,
                cardWidth - 20,
                25,
                button -> {
                    tickRate.toggleKeyToggleEnabled();
                    button.setMessage(keyToggleText());
                }
        );
    }

    // --------------------------------------------------
    // BUTTONS
    // --------------------------------------------------

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
        if (distanceField == null) {
            return;
        }

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

                distanceField.setText(
                        String.format(Locale.ROOT, "%.1f", distance)
                );

            } else {
                distanceError = true;
            }

        } catch (NumberFormatException ignored) {
            distanceError = true;
        }
    }

    // --------------------------------------------------
    // KEYBOARD
    // --------------------------------------------------

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

    // --------------------------------------------------
    // RENDER
    // --------------------------------------------------

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        float progress = Math.min(
                1.0F,
                (System.currentTimeMillis() - openedAt) / 250.0F
        );

        int x = panelX;
        int y = panelY + (int) ((1.0F - progress) * 7);
        int right = x + panelWidth;
        int bottom = y + panelHeight;

        // Background

        context.fill(
                0, 0, width, height,
                0xE6000000
        );

        // Outer glow

        context.fill(
                x - 3, y - 3,
                right + 3, bottom + 3,
                0xFF54227D
        );

        context.fill(
                x - 1, y - 1,
                right + 1, bottom + 1,
                PURPLE_DARK
        );

        // Main panel

        context.fill(
                x, y,
                right, bottom,
                BG
        );

        // Header

        context.fill(
                x + 1, y + 1,
                right - 1, y + 51,
                HEADER
        );

        context.fill(
                x + 1, y + 1,
                right - 1, y + 3,
                PURPLE
        );

        // Logo

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("A"),
                x + 20,
                y + 10,
                PURPLE
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("ASYNC"),
                x + 42,
                y + 9,
                WHITE
        );

        context.drawText(
                textRenderer,
                Text.literal("ULTRA DARK"),
                x + 42,
                y + 23,
                LIGHT_PURPLE,
                false
        );

        String version = "v1.0.0";

        context.drawText(
                textRenderer,
                Text.literal(version),
                right - textRenderer.getWidth(version) - 17,
                y + 16,
                LIGHT_PURPLE,
                false
        );

        // Header description

        context.drawText(
                textRenderer,
                Text.literal("OPTIMIZATION CLIENT"),
                x + 20,
                y + 37,
                MUTED,
                false
        );

        // Header bottom line

        context.fill(
                x + 1, y + 51,
                right - 1, y + 52,
                BORDER
        );

        // Navigation background

        context.fill(
                contentX - 5,
                y + 59,
                contentX + contentWidth + 5,
                y + 98,
                HEADER
        );

        // Navigation bottom line

        context.fill(
                contentX - 5,
                y + 98,
                contentX + contentWidth + 5,
                y + 99,
                BORDER
        );

        // Page heading

        String title = switch (page) {
            case HOME -> "CONTROL CENTER";
            case COMBAT -> "COMBAT";
            case HUD -> "HUD CONTROL";
            case SETTINGS -> "SETTINGS";
        };

        String subtitle = switch (page) {
            case HOME -> "Your game. Your settings. Your control.";
            case COMBAT -> "Configure your combat preferences.";
            case HUD -> "Customize your interface.";
            case SETTINGS -> "Manage your Async configuration.";
        };

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(title),
                contentX,
                y + 112,
                WHITE
        );

        context.drawText(
                textRenderer,
                Text.literal(subtitle),
                contentX,
                y + 126,
                MUTED,
                false
        );

        int gap = 12;
        int cardWidth = (contentWidth - gap) / 2;
        int cardHeight = 76;
        int rightX = contentX + cardWidth + gap;
        int firstY = y + 139;
        int secondY = firstY + cardHeight + gap;
        int thirdY = secondY + cardHeight + gap;

        switch (page) {
            case HOME -> drawHome(
                    context,
                    firstY,
                    secondY,
                    cardWidth,
                    cardHeight,
                    rightX
            );

            case COMBAT -> drawCombat(
                    context,
                    firstY,
                    secondY,
                    thirdY,
                    cardWidth,
                    cardHeight,
                    rightX
            );

            case HUD -> drawHud(
                    context,
                    firstY,
                    cardWidth,
                    cardHeight,
                    rightX
            );

            case SETTINGS -> drawSettings(
                    context,
                    firstY,
                    secondY,
                    cardWidth,
                    cardHeight,
                    rightX
            );
        }

        // Footer

        context.fill(
                x + 1,
                bottom - 39,
                right - 1,
                bottom - 38,
                BORDER
        );

        context.fill(
                x + 19,
                bottom - 23,
                x + 27,
                bottom - 15,
                tickRate.isEnabled() ? GREEN : RED
        );

        String status = tickRate.isEnabled()
                ? "MOD ENABLED"
                : "MOD DISABLED";

        context.drawText(
                textRenderer,
                Text.literal(status),
                x + 34,
                bottom - 23,
                WHITE,
                false
        );

        String minecraft = "Minecraft 1.21.11";

        context.drawText(
                textRenderer,
                Text.literal(minecraft),
                x + panelWidth / 2
                        - textRenderer.getWidth(minecraft) / 2,
                bottom - 23,
                MUTED,
                false
        );

        String footer = "ASYNC v1.0.0";

        context.drawText(
                textRenderer,
                Text.literal(footer),
                right - textRenderer.getWidth(footer) - 19,
                bottom - 23,
                LIGHT_PURPLE,
                false
        );

        // Render widgets

        super.render(context, mouseX, mouseY, delta);

        // Custom button rendering

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

            int fill = hovered || selected
                    ? CARD_HOVER
                    : BUTTON;

            int edge = hovered || selected
                    ? PURPLE
                    : BORDER;

            context.fill(
                    bx - 1,
                    by - 1,
                    bx + bw + 1,
                    by + bh + 1,
                    edge
            );

            context.fill(
                    bx,
                    by,
                    bx + bw,
                    by + bh,
                    fill
            );

            if (selected) {
                context.fill(
                        bx + 5,
                        by + 4,
                        bx + 7,
                        by + bh - 4,
                        PURPLE
                );
            }

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    button.getMessage(),
                    bx + bw / 2,
                    by + (bh - 8) / 2,
                    hovered || selected
                            ? LIGHT_PURPLE
                            : WHITE
            );
        }

        // Distance validation

        if (distanceError && page == Page.COMBAT) {
            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("Allowed distance: 0.5 - 4.0"),
                    contentX + cardWidth + 15,
                    secondY + 70,
                    RED
            );
        }

        // Key listening overlay

        if (listeningForKey || listeningForMenuKey) {
            String message = "PRESS A KEY  |  ESC TO CANCEL";

            int messageWidth =
                    textRenderer.getWidth(message) + 24;

            int messageX = (width - messageWidth) / 2;
            int messageY = height - 32;

            context.fill(
                    messageX - 1,
                    messageY - 1,
                    messageX + messageWidth + 1,
                    messageY + 20,
                    PURPLE
            );

            context.fill(
                    messageX,
                    messageY,
                    messageX + messageWidth,
                    messageY + 19,
                    BG
            );

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal(message),
                    width / 2,
                    messageY + 6,
                    LIGHT_PURPLE
            );
        }
    }

    // --------------------------------------------------
    // HOME CARDS
    // --------------------------------------------------

    private void drawHome(
            DrawContext context,
            int firstY,
            int secondY,
            int cardWidth,
            int cardHeight,
            int rightX
    ) {
        drawCard(
                context,
                contentX,
                firstY,
                contentWidth,
                94,
                "TriggerBot",
                "Control your combat automation",
                tickRate.isEnabled() ? "ENABLED" : "DISABLED",
                tickRate.isEnabled() ? GREEN : RED
        );

        drawCard(
                context,
                contentX,
                secondY,
                cardWidth,
                cardHeight,
                "COMBAT",
                "Attack and target settings",
                "OPEN ->",
                PURPLE
        );

        drawCard(
                context,
                rightX,
                secondY,
                cardWidth,
                cardHeight,
                "HUD",
                "Interface and status settings",
                "OPEN ->",
                PURPLE
        );

        int overviewY = secondY + cardHeight + 12;

        context.drawText(
                textRenderer,
                Text.literal("SYSTEM OVERVIEW"),
                contentX,
                overviewY,
                LIGHT_PURPLE,
                false
        );

        String attack = attackText().getString();
        String target = targetText().getString();

        context.drawText(
                textRenderer,
                Text.literal(attack),
                contentX,
                overviewY + 16,
                MUTED,
                false
        );

        context.drawText(
                textRenderer,
                Text.literal(target),
                contentX + 145,
                overviewY + 16,
                MUTED,
                false
        );

        context.drawText(
                textRenderer,
                Text.literal(weaponText().getString()),
                contentX + 285,
                overviewY + 16,
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
                overviewY + 31,
                LIGHT_PURPLE,
                false
        );
    }

    // --------------------------------------------------
    // COMBAT CARDS
    // --------------------------------------------------

    private void drawCombat(
            DrawContext context,
            int firstY,
            int secondY,
            int thirdY,
            int cardWidth,
            int cardHeight,
            int rightX
    ) {
        drawCard(
                context,
                contentX,
                firstY,
                cardWidth,
                cardHeight,
                "ATTACK MODE",
                "Select attack behavior",
                attackText().getString(),
                PURPLE
        );

        drawCard(
                context,
                rightX,
                firstY,
                cardWidth,
                cardHeight,
                "TARGET MODE",
                "Select target category",
                targetText().getString(),
                PURPLE
        );

        drawCard(
                context,
                contentX,
                secondY,
                cardWidth,
                cardHeight,
                "WEAPON MODE",
                "Select allowed weapons",
                weaponText().getString(),
                PURPLE
        );

        drawCard(
                context,
                rightX,
                secondY,
                cardWidth,
                cardHeight,
                "ATTACK DISTANCE",
                "Maximum distance: 0.5 - 4.0",
                String.format(
                        Locale.ROOT,
                        "%.1f blocks",
                        config.getAttackDistance()
                ),
                PURPLE
        );

        drawCard(
                context,
                contentX,
                thirdY,
                cardWidth,
                cardHeight,
                "KEY TOGGLE",
                "Enable keyboard switching",
                tickRate.isKeyToggleEnabled() ? "ENABLED" : "DISABLED",
                tickRate.isKeyToggleEnabled() ? GREEN : RED
        );

        drawCard(
                context,
                rightX,
                thirdY,
                cardWidth,
                cardHeight,
                "TRIGGER KEY",
                "Assigned activation key",
                keyName(tickRate.getToggleKey()),
                PURPLE
        );
    }

    // --------------------------------------------------
    // HUD CARDS
    // --------------------------------------------------

    private void drawHud(
            DrawContext context,
            int firstY,
            int cardWidth,
            int cardHeight,
            int rightX
    ) {
        drawCard(
                context,
                contentX,
                firstY,
                cardWidth,
                cardHeight,
                "STATUS DISPLAY",
                "Отображение Надписи вкл/выкл",
                config.isShowStatus() ? "ENABLED" : "DISABLED",
                config.isShowStatus() ? GREEN : RED
        );

        drawCard(
                context,
                rightX,
                firstY,
                cardWidth,
                cardHeight,
                "MENU KEY",
                "Assigned menu opening key",
                keyName(config.getMenuKey()),
                PURPLE
        );
    }

    // --------------------------------------------------
    // SETTINGS CARDS
    // --------------------------------------------------

    private void drawSettings(
            DrawContext context,
            int firstY,
            int secondY,
            int cardWidth,
            int cardHeight,
            int rightX
    ) {
        drawCard(
                context,
                contentX,
                firstY,
                cardWidth,
                cardHeight,
                "CLIENT VERSION",
                "Async optimization client",
                "v1.0.0",
                PURPLE
        );

        drawCard(
                context,
                rightX,
                firstY,
                cardWidth,
                cardHeight,
                "GAME VERSION",
                "Supported Minecraft version",
                "1.21.11",
                PURPLE
        );

        drawCard(
                context,
                contentX,
                secondY,
                cardWidth,
                cardHeight,
                "CONFIGURATION",
                "Settings are saved automatically",
                "async.json",
                GREEN
        );

        drawCard(
                context,
                rightX,
                secondY,
                cardWidth,
                cardHeight,
                "RENDERER",
                "Interface rendering system",
                "DRAW CONTEXT + FABRIC",
                PURPLE
        );
    }

    // --------------------------------------------------
    // CARD RENDERER
    // --------------------------------------------------

    private void drawCard(
            DrawContext context,
            int x,
            int y,
            int cardWidth,
            int cardHeight,
            String title,
            String subtitle,
            String value,
            int accent
    ) {
        context.fill(
                x - 1,
                y - 1,
                x + cardWidth + 1,
                y + cardHeight + 1,
                BORDER
        );

        context.fill(
                x,
                y,
                x + cardWidth,
                y + cardHeight,
                CARD
        );

        context.fill(
                x + 10,
                y + 10,
                x + 12,
                y + cardHeight - 10,
                accent
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(title),
                x + 21,
                y + 10,
                WHITE
        );

        context.drawText(
                textRenderer,
                Text.literal(subtitle),
                x + 21,
                y + 25,
                MUTED,
                false
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(value),
                x + 21,
                y + 47,
                accent
        );
    }

    // --------------------------------------------------
    // SELECTION
    // --------------------------------------------------

    private boolean isSelected(ButtonWidget button) {
        String message = button.getMessage().getString();

        return switch (page) {
            case HOME -> message.equals("HOME");
            case COMBAT -> message.equals("COMBAT");
            case HUD -> message.equals("HUD");
            case SETTINGS -> message.equals("SETTINGS");
        };
    }

    // --------------------------------------------------
    // TEXT VALUES
    // --------------------------------------------------

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
                        + (tickRate.isKeyToggleEnabled()
                        ? "ON"
                        : "OFF")
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

    // --------------------------------------------------
    // CLOSE
    // --------------------------------------------------

    @Override
    public void close() {
        tickRate.suppressNextAttack();
        super.close();
    }
}
