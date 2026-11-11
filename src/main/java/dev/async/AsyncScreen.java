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
    private enum Page { HOME, COMBAT, HUD, SETTINGS }
    private final TickRate tickRate;
    private final AsyncConfig config;
    private Page page = Page.HOME;
    private int panelX, panelY, panelWidth, panelHeight, contentX, contentWidth;
    private int cardsTop, cardWidth, cardHeight, gap, rightX;
    private boolean listeningForKey, listeningForMenuKey, distanceError;
    private TextFieldWidget distanceField;

    public AsyncScreen(TickRate tickRate, AsyncConfig config) {
        super(Text.literal("Async"));
        this.tickRate = tickRate;
        this.config = config;
    }

    @Override
    protected void init() {
        clearChildren();
        distanceField = null;
        panelWidth = Math.min(940, Math.max(300, width - 28));
        panelHeight = Math.min(640, Math.max(340, height - 20));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        contentX = panelX + 22;
        contentWidth = panelWidth - 44;
        gap = contentWidth < 430 ? 8 : 12;
        cardWidth = (contentWidth - gap) / 2;
        rightX = contentX + cardWidth + gap;
        cardsTop = panelY + 145;
        int usable = panelHeight - 203;
        cardHeight = Math.max(42, Math.min(82, (usable - 2 * gap) / 3));

        int tabGap = 7;
        int tabWidth = (contentWidth - tabGap * 3) / 4;
        int tabY = panelY + 65;
        addNav("HOME", Page.HOME, contentX, tabY, tabWidth);
        addNav("COMBAT", Page.COMBAT, contentX + tabWidth + tabGap, tabY, tabWidth);
        addNav("HUD", Page.HUD, contentX + (tabWidth + tabGap) * 2, tabY, tabWidth);
        addNav("SETTINGS", Page.SETTINGS, contentX + (tabWidth + tabGap) * 3, tabY, tabWidth);

        switch (page) {
            case HOME -> buildHome();
            case COMBAT -> buildCombat();
            case HUD -> buildHud();
            case SETTINGS -> buildSettings();
        }
    }

    private void addNav(String label, Page target, int x, int y, int w) {
        addButton(Text.literal(label), x, y, w, 28, b -> changePage(target));
    }

    private void changePage(Page target) {
        page = target;
        distanceError = false;
        listeningForKey = listeningForMenuKey = false;
        init();
    }

    private void buildHome() {
        int y = cardsTop;
        addButton(enabledText(), contentX + 16, y + 61, contentWidth - 32, 27, b -> {
            tickRate.toggleEnabled();
            b.setMessage(enabledText());
        });
        int row = y + 119;
        addButton(Text.literal("OPEN COMBAT  →"), contentX + 10, row + cardHeight - 32,
                cardWidth - 20, 25, b -> changePage(Page.COMBAT));
        addButton(Text.literal("OPEN HUD  →"), rightX + 10, row + cardHeight - 32,
                cardWidth - 20, 25, b -> changePage(Page.HUD));
    }

    private void buildCombat() {
        int h = Math.max(22, Math.min(27, cardHeight - 30));
        int inset = 10, first = cardsTop, second = first + cardHeight + gap, third = second + cardHeight + gap;
        int buttonY1 = first + cardHeight - h - 8;
        int buttonY2 = second + cardHeight - h - 8;
        int buttonY3 = third + cardHeight - h - 8;
        addButton(attackText(), contentX + inset, buttonY1, cardWidth - 20, h, b -> {
            tickRate.toggleAttackMode(); b.setMessage(attackText());
        });
        addButton(targetText(), rightX + inset, buttonY1, cardWidth - 20, h, b -> {
            tickRate.toggleTargetMode(); b.setMessage(targetText());
        });
        addButton(weaponText(), contentX + inset, buttonY2, cardWidth - 20, h, b -> {
            tickRate.toggleWeaponMode(); b.setMessage(weaponText());
        });
        int fieldW = Math.max(52, cardWidth - 83);
        distanceField = new TextFieldWidget(textRenderer, rightX + inset, buttonY2, fieldW, h,
                Text.literal("Attack distance"));
        distanceField.setMaxLength(5);
        distanceField.setText(String.format(Locale.ROOT, "%.1f", config.getAttackDistance()));
        addDrawableChild(distanceField);
        addButton(Text.literal("OK"), rightX + cardWidth - 62, buttonY2, 52, h, b -> applyDistance());
        addButton(keyToggleText(), contentX + inset, buttonY3, cardWidth - 20, h, b -> {
            tickRate.toggleKeyToggleEnabled(); b.setMessage(keyToggleText());
        });
        addButton(triggerKeyText(), rightX + inset, buttonY3, cardWidth - 20, h, b -> {
            listeningForKey = true; listeningForMenuKey = false; b.setMessage(Text.literal("PRESS A KEY..."));
        });
    }

    private void buildHud() {
        int h = Math.max(22, Math.min(27, cardHeight - 30));
        int y = cardsTop + cardHeight - h - 8;
        addButton(statusText(), contentX + 10, y, cardWidth - 20, h, b -> {
            config.setShowStatus(!config.isShowStatus()); b.setMessage(statusText());
        });
        addButton(menuKeyText(), rightX + 10, y, cardWidth - 20, h, b -> {
            listeningForMenuKey = true; listeningForKey = false; b.setMessage(Text.literal("PRESS A KEY..."));
        });
    }

    private void buildSettings() {
        int h = Math.max(22, Math.min(27, cardHeight - 30));
        int y1 = cardsTop + cardHeight - h - 8;
        int y2 = cardsTop + cardHeight + gap + cardHeight - h - 8;
        addButton(enabledText(), contentX + 10, y1, cardWidth - 20, h, b -> {
            tickRate.toggleEnabled(); b.setMessage(enabledText());
        });
        addButton(keyToggleText(), rightX + 10, y1, cardWidth - 20, h, b -> {
            tickRate.toggleKeyToggleEnabled(); b.setMessage(keyToggleText());
        });
        addButton(triggerKeyText(), contentX + 10, y2, cardWidth - 20, h, b -> {
            listeningForKey = true; listeningForMenuKey = false; b.setMessage(Text.literal("PRESS A KEY..."));
        });
        addButton(menuKeyText(), rightX + 10, y2, cardWidth - 20, h, b -> {
            listeningForMenuKey = true; listeningForKey = false; b.setMessage(Text.literal("PRESS A KEY..."));
        });
    }

    private void addButton(Text message, int x, int y, int w, int h, ButtonWidget.PressAction action) {
        addDrawableChild(ButtonWidget.builder(message, action).dimensions(x, y, w, h).build());
    }

    private void applyDistance() {
        if (distanceField == null) return;
        try {
            double d = Double.parseDouble(distanceField.getText().trim().replace(',', '.'));
            if (Double.isFinite(d) && d >= 0.5 && d <= 4.0) {
                config.setAttackDistance(d);
                distanceError = false;
                distanceField.setText(String.format(Locale.ROOT, "%.1f", d));
            } else distanceError = true;
        } catch (NumberFormatException ignored) { distanceError = true; }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (listeningForKey || listeningForMenuKey) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                listeningForKey = listeningForMenuKey = false;
                init();
                return true;
            }
            int chosen = key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE
                    ? GLFW.GLFW_KEY_UNKNOWN : key;
            if (listeningForMenuKey) config.setMenuKey(chosen);
            else tickRate.setToggleKey(chosen);
            listeningForKey = listeningForMenuKey = false;
            init();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER && page == Page.COMBAT && distanceField != null) {
            applyDistance();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, AsyncTheme.BACKDROP);
        AsyncRender.panel(ctx, panelX, panelY, panelWidth, panelHeight);
        drawHeader(ctx);
        drawNavRail(ctx);
        drawHeading(ctx);
        drawCards(ctx);
        drawFooter(ctx);
        drawTextFieldSurface(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        drawButtonSkins(ctx);
        if (distanceError && page == Page.COMBAT) {
            AsyncRender.text(ctx, textRenderer, "Use a value from 0.5 to 4.0",
                    rightX + 10, cardsTop + 2 * (cardHeight + gap) + cardHeight - 2, AsyncTheme.RED);
        }
        if (listeningForKey || listeningForMenuKey) {
            String msg = "PRESS A KEY  •  ESC TO CANCEL";
            int w = textRenderer.getWidth(msg) + 30, x = (width - w) / 2, y = height - 35;
            AsyncRender.glow(ctx, x, y, w, 23, AsyncTheme.PURPLE);
            AsyncRender.rounded(ctx, x, y, w, 23, 6, AsyncTheme.PANEL_TOP);
            AsyncRender.centered(ctx, textRenderer, msg, width / 2, y + 7, AsyncTheme.PURPLE_LIGHT);
        }
    }

    private void drawHeader(DrawContext ctx) {
        int x = panelX, y = panelY;
        AsyncRender.text(ctx, textRenderer, "✦", x + 22, y + 11, AsyncTheme.PURPLE);
        AsyncRender.text(ctx, textRenderer, "ASYNC", x + 42, y + 9, AsyncTheme.WHITE);
        AsyncRender.text(ctx, textRenderer, "NEON OPTIMIZATION CLIENT", x + 42, y + 23, AsyncTheme.MUTED);
        String v = "V1.0.0  /  1.21.11";
        AsyncRender.text(ctx, textRenderer, v, x + panelWidth - textRenderer.getWidth(v) - 22, y + 17, AsyncTheme.PURPLE_LIGHT);
        AsyncRender.rounded(ctx, x + 20, y + 39, 52, 3, 2, AsyncTheme.PURPLE);
        AsyncRender.rounded(ctx, x + 76, y + 39, 18, 3, 2, AsyncTheme.CYAN);
    }

    private void drawNavRail(DrawContext ctx) {
        int x = contentX - 5, y = panelY + 59;
        AsyncRender.rounded(ctx, x, y, contentWidth + 10, 38, 7, AsyncTheme.PANEL_BOTTOM);
        ctx.fill(x + 8, y + 36, x + contentWidth + 2, y + 37, AsyncTheme.BORDER);
    }

    private void drawHeading(DrawContext ctx) {
        String title = switch (page) {
            case HOME -> "CONTROL CENTER";
            case COMBAT -> "COMBAT MODULE";
            case HUD -> "HUD & INTERFACE";
            case SETTINGS -> "CLIENT SETTINGS";
        };
        String sub = switch (page) {
            case HOME -> "A clean overview of your active modules";
            case COMBAT -> "Targeting, attack behavior and hotkeys";
            case HUD -> "On-screen status and menu access";
            case SETTINGS -> "Client information and configuration";
        };
        AsyncRender.text(ctx, textRenderer, title, contentX, panelY + 111, AsyncTheme.WHITE);
        AsyncRender.text(ctx, textRenderer, sub, contentX, panelY + 126, AsyncTheme.MUTED);
        AsyncRender.rounded(ctx, contentX, panelY + 139, 24, 2, 1, AsyncTheme.PURPLE);
    }

    private void drawCards(DrawContext ctx) {
        int a = cardsTop, b = a + cardHeight + gap, c = b + cardHeight + gap;
        switch (page) {
            case HOME -> {
                drawCard(ctx, contentX, a, contentWidth, 104, "TRIGGERBOT",
                        "Main module status and quick control", tickRate.isEnabled() ? "ACTIVE" : "STANDBY",
                        tickRate.isEnabled() ? AsyncTheme.GREEN : AsyncTheme.RED, tickRate.isEnabled());
                drawCard(ctx, contentX, a + 119, cardWidth, cardHeight, "COMBAT",
                        "Attack and target behavior", "OPEN MODULE", AsyncTheme.PURPLE, false);
                drawCard(ctx, rightX, a + 119, cardWidth, cardHeight, "HUD",
                        "Interface and status options", "OPEN MODULE", AsyncTheme.CYAN, false);
                int oy = a + 119 + cardHeight + 16;
                AsyncRender.text(ctx, textRenderer, "LIVE CONFIGURATION", contentX, oy, AsyncTheme.PURPLE_LIGHT);
                AsyncRender.text(ctx, textRenderer, attackText().getString(), contentX, oy + 17, AsyncTheme.TEXT);
                AsyncRender.text(ctx, textRenderer, targetText().getString(), contentX + 145, oy + 17, AsyncTheme.TEXT);
                AsyncRender.text(ctx, textRenderer, weaponText().getString(), contentX + 290, oy + 17, AsyncTheme.TEXT);
                AsyncRender.text(ctx, textRenderer, String.format(Locale.ROOT, "RANGE: %.1f", config.getAttackDistance()),
                        contentX, oy + 32, AsyncTheme.MUTED);
            }
            case COMBAT -> {
                drawCard(ctx, contentX, a, cardWidth, cardHeight, "ATTACK MODE", "Normal or fast behavior",
                        tickRate.getAttackMode().name(), AsyncTheme.PURPLE, true);
                drawCard(ctx, rightX, a, cardWidth, cardHeight, "TARGET FILTER", "Choose target category",
                        tickRate.getMode().name(), AsyncTheme.CYAN, true);
                drawCard(ctx, contentX, b, cardWidth, cardHeight, "WEAPON FILTER", "Allowed held item type",
                        tickRate.getWeaponMode() == TickWeapon.WEAPONS_ONLY ? "WEAPONS ONLY" : "ANY ITEM", AsyncTheme.PURPLE, true);
                drawCard(ctx, rightX, b, cardWidth, cardHeight, "ATTACK DISTANCE", "Valid range: 0.5 to 4.0 blocks",
                        String.format(Locale.ROOT, "%.1f BLOCKS", config.getAttackDistance()), AsyncTheme.CYAN, true);
                drawCard(ctx, contentX, c, cardWidth, cardHeight, "KEY TOGGLE", "Allow the assigned hotkey",
                        tickRate.isKeyToggleEnabled() ? "ENABLED" : "DISABLED",
                        tickRate.isKeyToggleEnabled() ? AsyncTheme.GREEN : AsyncTheme.RED, tickRate.isKeyToggleEnabled());
                drawCard(ctx, rightX, c, cardWidth, cardHeight, "TRIGGER HOTKEY", "Press below to rebind",
                        keyName(tickRate.getToggleKey()), AsyncTheme.PURPLE, false);
            }
            case HUD -> {
                drawCard(ctx, contentX, a, cardWidth, cardHeight, "STATUS NOTIFICATION",
                        "Show short enabled / disabled text", config.isShowStatus() ? "VISIBLE" : "HIDDEN",
                        config.isShowStatus() ? AsyncTheme.GREEN : AsyncTheme.RED, config.isShowStatus());
                drawCard(ctx, rightX, a, cardWidth, cardHeight, "MENU HOTKEY",
                        "Key used to open this interface", keyName(config.getMenuKey()), AsyncTheme.CYAN, false);
                AsyncRender.text(ctx, textRenderer, "INTERFACE", contentX, a + cardHeight + 22, AsyncTheme.PURPLE_LIGHT);
                AsyncRender.text(ctx, textRenderer, "Status messages appear briefly below the crosshair when toggled.",
                        contentX, a + cardHeight + 39, AsyncTheme.MUTED);
            }
            case SETTINGS -> {
                drawCard(ctx, contentX, a, cardWidth, cardHeight, "CLIENT VERSION", "Async optimization client",
                        "V1.0.0", AsyncTheme.PURPLE, false);
                drawCard(ctx, rightX, a, cardWidth, cardHeight, "GAME VERSION", "Supported release",
                        "1.21.11", AsyncTheme.CYAN, false);
                drawCard(ctx, contentX, b, cardWidth, cardHeight, "CONFIGURATION", "Settings saved automatically",
                        "ASYNC.JSON", AsyncTheme.GREEN, false);
                drawCard(ctx, rightX, b, cardWidth, cardHeight, "RENDERER", "Native Minecraft drawing",
                        "FABRIC GUI", AsyncTheme.PURPLE, false);
            }
        }
    }

    private void drawCard(DrawContext ctx, int x, int y, int w, int h, String title, String subtitle,
                          String value, int accent, boolean active) {
        AsyncRender.card(ctx, x, y, w, h, accent, active);
        AsyncRender.text(ctx, textRenderer, title, x + 20, y + 9, AsyncTheme.WHITE);
        if (h >= 62) AsyncRender.text(ctx, textRenderer, subtitle, x + 20, y + 24, AsyncTheme.MUTED);
        AsyncRender.text(ctx, textRenderer, value, x + 20, y + h - 18, accent);
        if (active) AsyncRender.rounded(ctx, x + w - 19, y + 11, 7, 7, 3, accent);
    }

    private void drawFooter(DrawContext ctx) {
        int y = panelY + panelHeight - 39;
        ctx.fill(panelX + 18, y, panelX + panelWidth - 18, y + 1, AsyncTheme.BORDER);
        AsyncRender.rounded(ctx, panelX + 21, y + 15, 7, 7, 3,
                tickRate.isEnabled() ? AsyncTheme.GREEN : AsyncTheme.RED);
        AsyncRender.text(ctx, textRenderer, tickRate.isEnabled() ? "MODULE ACTIVE" : "MODULE INACTIVE",
                panelX + 35, y + 14, AsyncTheme.TEXT);
        AsyncRender.centered(ctx, textRenderer, "MINECRAFT 1.21.11", panelX + panelWidth / 2, y + 14, AsyncTheme.MUTED);
        String right = "ASYNC  /  NEON UI";
        AsyncRender.text(ctx, textRenderer, right, panelX + panelWidth - textRenderer.getWidth(right) - 21,
                y + 14, AsyncTheme.PURPLE_LIGHT);
    }

    private void drawTextFieldSurface(DrawContext ctx) {
        if (distanceField == null) return;
        int x = distanceField.getX(), y = distanceField.getY(), w = distanceField.getWidth(), h = distanceField.getHeight();
        AsyncRender.rounded(ctx, x - 1, y - 1, w + 2, h + 2, 5, AsyncTheme.BORDER_BRIGHT);
        AsyncRender.rounded(ctx, x, y, w, h, 4, AsyncTheme.PANEL_TOP);
    }

    private void drawButtonSkins(DrawContext ctx) {
        for (var child : children()) {
            if (!(child instanceof ButtonWidget button)) continue;
            int x = button.getX(), y = button.getY(), w = button.getWidth(), h = button.getHeight();
            boolean active = isSelected(button), hover = button.isHovered();
            AsyncRender.rounded(ctx, x - 1, y - 1, w + 2, h + 2, 6,
                    hover || active ? AsyncTheme.BORDER_BRIGHT : AsyncTheme.BORDER);
            AsyncRender.rounded(ctx, x, y, w, h, 5,
                    hover || active ? AsyncTheme.SURFACE_HOVER : AsyncTheme.SURFACE_ALT);
            if (active) AsyncRender.rounded(ctx, x + 5, y + 5, 3, h - 10, 2, AsyncTheme.PURPLE);
            ctx.drawCenteredTextWithShadow(textRenderer, button.getMessage(), x + w / 2,
                    y + (h - 8) / 2, hover || active ? AsyncTheme.PURPLE_LIGHT : AsyncTheme.TEXT);
        }
    }

    private boolean isSelected(ButtonWidget b) {
        String m = b.getMessage().getString();
        return switch (page) {
            case HOME -> m.equals("HOME");
            case COMBAT -> m.equals("COMBAT");
            case HUD -> m.equals("HUD");
            case SETTINGS -> m.equals("SETTINGS");
        };
    }

    private Text enabledText() { return Text.literal("TRIGGERBOT  " + (tickRate.isEnabled() ? "ON" : "OFF")); }
    private Text attackText() { return Text.literal("ATTACK: " + tickRate.getAttackMode().name()); }
    private Text targetText() { return Text.literal("TARGET: " + tickRate.getMode().name()); }
    private Text weaponText() { return Text.literal("WEAPON: " + (tickRate.getWeaponMode() == TickWeapon.WEAPONS_ONLY ? "WEAPONS" : "ANY ITEM")); }
    private Text keyToggleText() { return Text.literal("KEY TOGGLE: " + (tickRate.isKeyToggleEnabled() ? "ON" : "OFF")); }
    private Text triggerKeyText() { return Text.literal("TRIGGER KEY: " + keyName(tickRate.getToggleKey())); }
    private Text menuKeyText() { return Text.literal("MENU KEY: " + keyName(config.getMenuKey())); }
    private Text statusText() { return Text.literal("STATUS: " + (config.isShowStatus() ? "ON" : "OFF")); }

    private String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "NONE";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase(Locale.ROOT);
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
    public void close() {
        tickRate.suppressNextAttack();
        super.close();
    }
}