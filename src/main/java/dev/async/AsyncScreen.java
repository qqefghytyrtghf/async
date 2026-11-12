
package dev.async;

import net.minecraft.client.input.MouseInput;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.widget.SliderWidget;

import java.util.Locale;

public final class AsyncScreen extends Screen {

    private int ACCENT;
    private int ACCENT_LIGHT;
    private int ACCENT_DARK;

    private int PANEL;
    private int HEADER;
    private int CARD;
    private int CARD_HOVER;
    private int BUTTON;
    private int BUTTON_HOVER;
    private int BORDER;

    private static final int WHITE = 0xFFF8F0FF;
    private static final int MUTED = 0xFFA69AB5;
    private static final int GREEN = 0xFFBCA9D8;
    private static final int RED = 0xFF8F7BAA;

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
    private DistanceSlider distanceSlider;

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

        PANEL = blend(0xFF100C19, ACCENT, 0.12F);
        HEADER = blend(0xFF171020, ACCENT, 0.20F);
        CARD = blend(0xFF171220, ACCENT, 0.16F);
        CARD_HOVER = blend(0xFF281936, ACCENT, 0.34F);
        BUTTON = blend(0xFF21172D, ACCENT, 0.24F);
        BUTTON_HOVER = blend(0xFF39234C, ACCENT, 0.48F);
        BORDER = blend(0xFF493D58, ACCENT, 0.62F);
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

        addNavigationButton("Главная", Page.HOME, contentX, tabY, tabWidth);
        addNavigationButton("Бой", Page.COMBAT,
                contentX + tabWidth + tabGap, tabY, tabWidth);
        addNavigationButton("Интерфейс", Page.HUD,
                contentX + (tabWidth + tabGap) * 2, tabY, tabWidth);
        addNavigationButton("Настройки", Page.SETTINGS,
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
        distanceSlider = null;
        distanceError = false;
        listeningForKey = false;
        listeningForMenuKey = false;
        init();
    }

    private int controlY(int cardY) {
        return cardY + cardHeight - 27;
    }

    private void buildHome() {
        addButton(
                enabledText(),
                contentX + 12,
                firstCardY + cardHeight - 34,
                contentWidth - 24,
                25,
                button -> {
                    tickRate.toggleEnabled();
                    button.setMessage(enabledText());
                }
        );

        int buttonY = secondCardY + cardHeight - 29;

        addButton(Text.literal("Бой  →"),
                contentX + 9, buttonY, cardWidth - 18, 22,
                button -> changePage(Page.COMBAT));

        addButton(Text.literal("Интерфейс  →"),
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

        int distanceY = secondCardY + cardHeight - 24;
        int distanceX = rightCardX + 9;

        int fieldWidth = 112;
        int applyWidth = 66;
        int applyX = rightCardX + cardWidth - applyWidth - 9;

        distanceField = new TextFieldWidget(
                textRenderer,
                distanceX,
                distanceY - 1,
                fieldWidth,
                25,
                Text.literal("Дистанция атаки")
        );

        distanceField.setMaxLength(5);
        distanceField.setDrawsBackground(false);
        distanceField.setEditableColor(WHITE);

        distanceField.setText(String.format(
                Locale.ROOT,
                "%.1f",
                config.getAttackDistance()
        ));

        addDrawableChild(distanceField);

        addButton(
                Text.literal("Применить"),
                applyX,
                distanceY,
                applyWidth,
                buttonHeight,
                button -> applyDistance()
        );

        int sliderX = distanceX + fieldWidth + 9;
        int sliderRight = applyX - 7;
        int sliderWidth = Math.max(1, sliderRight - sliderX);

        distanceSlider = new DistanceSlider(
                sliderX,
                distanceY,
                sliderWidth,
                buttonHeight,
                config.getAttackDistance()
        );

        addDrawableChild(distanceSlider);

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
                    button.setMessage(Text.literal("Нажмите клавишу..."));
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
                    button.setMessage(Text.literal("Нажмите клавишу..."));
                });
    }

    private void buildSettings() {
        addButton(Text.literal("Цвет интерфейса: " + appearance.getAccentColorName()),
                contentX + 9, controlY(firstCardY), contentWidth - 18, 23,
                button -> {
                    appearance.nextAccentColor();
                    updatePalette();
                    button.setMessage(Text.literal("Цвет интерфейса: "
                            + appearance.getAccentColorName()));
                });

        int sizeY = controlY(secondCardY);
        int smallWidth = Math.min(90, Math.max(65, contentWidth / 5));

        addButton(Text.literal("Размер  −"),
                contentX + 9, sizeY, smallWidth, 23,
                button -> appearance.changeScale(-0.05F));

        addButton(Text.literal("Размер  +"),
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

                distance = Math.round(distance * 10.0) / 10.0;

                config.setAttackDistance(distance);
                distanceError = false;

                distanceField.setText(String.format(
                        Locale.ROOT, "%.1f", distance));

                if (distanceSlider != null) {
                    distanceSlider.setDistance(distance);
                }
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

        drawSimpleBorder(context, x, y + 50, panelWidth, panelHeight - 50);
        drawHeader(context, x, y, right);

        drawRounded(context,
                contentX - 7, y + 59,
                contentWidth + 14, 39, 7, HEADER);

        drawSimpleBorder(context,
                contentX - 7, y + 59,
                contentWidth + 14, 39);

        drawPageHeading(context, y);

        switch (page) {
            case HOME -> drawHome(context);
            case COMBAT -> drawCombat(context);
            case HUD -> drawHud(context);
            case SETTINGS -> drawSettings(context);
        }

        drawFooter(context, x, y, right, bottom);

        if (page == Page.COMBAT && distanceField != null) {
            drawDistanceField(context);
        }

        super.render(
                context,
                (int) scaledMouseX,
                (int) scaledMouseY,
                delta
        );

        drawCustomButtons(context);

        if (page == Page.COMBAT && distanceSlider != null) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal(String.format(
                            Locale.ROOT,
                            "Дистанция: %.1f",
                            distanceSlider.getDistance()
                    )),
                    distanceSlider.getX() + distanceSlider.getWidth() / 2,
                    distanceSlider.getY() - 13,
                    WHITE
            );
        }

        if (distanceError && page == Page.COMBAT) {
            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("Допустимое значение: 0.5–4.0"),
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

    private void drawDistanceField(DrawContext context) {
        int x = distanceField.getX();
        int y = distanceField.getY();
        int w = distanceField.getWidth();
        int h = distanceField.getHeight();

        int background = distanceField.isFocused()
                ? CARD_HOVER : BUTTON;

        drawRounded(
                context,
                x - 1, y - 1,
                w + 2, h + 2,
                6, BORDER
        );

        drawRounded(
                context,
                x, y,
                w, h,
                5, background
        );
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
                ACCENT_DARK);

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("A"),
                x + 27,
                y + 16,
                ACCENT_LIGHT
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Async"),
                x + 45,
                y + 10,
                WHITE
        );

        context.drawText(
                textRenderer,
                Text.literal("ClickGUI"),
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
                Text.literal("Клиент оптимизации"),
                x + 19,
                y + 39,
                MUTED,
                false
        );
    }

    private void drawPageHeading(DrawContext context, int y) {
        String title = switch (page) {
            case HOME -> "Главная";
            case COMBAT -> "Бой";
            case HUD -> "Интерфейс";
            case SETTINGS -> "Настройки";
        };

        String subtitle = switch (page) {
            case HOME -> "Управление модом и его функциями.";
            case COMBAT -> "Настройка параметров боя.";
            case HUD -> "Настройка отображения интерфейса.";
            case SETTINGS -> "Настройки внешнего вида Async.";
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
                "TriggerBot", "Текущее состояние модуля",
                tickRate.isEnabled() ? GREEN : RED);

        drawControlCard(context,
                contentX, secondCardY, cardWidth, cardHeight,
                "Бой", "Параметры атаки", ACCENT);

        drawControlCard(context,
                rightCardX, secondCardY, cardWidth, cardHeight,
                "Интерфейс", "Отображение и статус", ACCENT);

        int overviewY = secondCardY + cardHeight + 15;

        context.drawText(
                textRenderer,
                Text.literal("Состояние системы"),
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
                Text.literal("Дистанция атаки: " + distance),
                contentX,
                overviewY + 29,
                ACCENT_LIGHT,
                false
        );
    }

    private void drawCombat(DrawContext context) {
        drawControlCard(context,
                contentX, firstCardY, cardWidth, cardHeight,
                "Режим", "Выбранный режим атаки", ACCENT);

        drawControlCard(context,
                rightCardX, firstCardY, cardWidth, cardHeight,
                "Цель", "Выбранная цель", ACCENT);

        drawControlCard(context,
                contentX, secondCardY, cardWidth, cardHeight,
                "Режим оружия", "Выбранное оружие", ACCENT);

        drawControlCard(context,
                rightCardX, secondCardY, cardWidth, cardHeight,
                "Дистанция атаки", "Допустимо: 0.5–4.0", ACCENT);

        drawControlCard(context,
                contentX, thirdCardY, cardWidth, cardHeight,
                "Переключение клавишей", "Включение и выключение",
                tickRate.isKeyToggleEnabled() ? GREEN : RED);

        drawControlCard(context,
                rightCardX, thirdCardY, cardWidth, cardHeight,
                "Клавиша активации", "Назначенная клавиша", ACCENT);
    }

    private void drawHud(DrawContext context) {
        drawControlCard(context,
                contentX, firstCardY, cardWidth, cardHeight,
                "Отображение статуса", "Показывать надпись вкл./выкл.",
                config.isShowStatus() ? GREEN : RED);

        drawControlCard(context,
                rightCardX, firstCardY, cardWidth, cardHeight,
                "Клавиша меню", "Клавиша открытия меню", ACCENT);
    }

    private void drawSettings(DrawContext context) {
        String color = String.format(
                Locale.ROOT,
                "#%06X",
                appearance.getAccentColor() & 0xFFFFFF
        );

        String size = Math.round(
                appearance.getScale() * 100.0F
        ) + "%";

        drawControlCard(context,
                contentX, firstCardY, contentWidth, cardHeight,
                "Цвет ClickGUI", "Выбранный цвет: " + color, ACCENT);

        drawControlCard(context,
                contentX, secondCardY, contentWidth, cardHeight,
                "Размер ClickGUI", "Текущий размер: " + size, ACCENT);
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
    }

    private void drawFooter(
            DrawContext context,
            int x,
            int y,
            int right,
            int bottom
    ) {
        drawRounded(context,
                x + 19, bottom - 24, 8, 8, 3,
                tickRate.isEnabled() ? GREEN : RED);

        String status = tickRate.isEnabled()
                ? "Мод включён"
                : "Мод выключен";

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

        String footer = "Async v1.0.0";

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
        String message = "Нажмите клавишу  |  ESC — отмена";

        int messageWidth = textRenderer.getWidth(message) + 30;
        int messageX = (width - messageWidth) / 2;
        int messageY = height - 37;

        drawRounded(context,
                messageX, messageY,
                messageWidth, 21,
                6, PANEL);

        drawSimpleBorder(context,
                messageX, messageY,
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
            case HOME -> message.equals("Главная");
            case COMBAT -> message.equals("Бой");
            case HUD -> message.equals("Интерфейс");
            case SETTINGS -> message.equals("Настройки");
        };
    }

    private Text enabledText() {
        return Text.literal(
                tickRate.isEnabled()
                        ? "TriggerBot: Вкл."
                        : "TriggerBot: Выкл."
        );
    }

    private Text attackText() {
        String mode = switch (tickRate.getAttackMode()) {
            case NORMAL -> "Комбо";
            case FAST -> "Криты";
        };

        return Text.literal("Режим: " + mode);
    }

    private Text targetText() {
        return Text.literal(
                "Цель: " + (
                        tickRate.getMode() == TargetMode.MOBS
                                ? "Мобы"
                                : "Игроки"
                )
        );
    }

    private Text weaponText() {
        return Text.literal(
                "Оружие: " + (
                        tickRate.getWeaponMode()
                                == TickWeapon.WEAPONS_ONLY
                                ? "Только оружие"
                                : "Любой предмет"
                )
        );
    }

    private Text keyToggleText() {
        return Text.literal(
                "Переключение: "
                        + (tickRate.isKeyToggleEnabled() ? "Вкл." : "Выкл.")
        );
    }

    private Text triggerKeyText() {
        return Text.literal(
                "Клавиша: " + keyName(tickRate.getToggleKey())
        );
    }

    private Text menuKeyText() {
        return Text.literal(
                "Клавиша меню: " + keyName(config.getMenuKey())
        );
    }

    private Text statusText() {
        return Text.literal(
                "Статус: "
                        + (config.isShowStatus() ? "Вкл." : "Выкл.")
        );
    }

    private String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            return "Не назначена";
        }

        String name = GLFW.glfwGetKeyName(key, 0);

        if (name != null) {
            return name.toUpperCase(Locale.ROOT);
        }

        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "Правый Shift";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "Левый Shift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "Правый Ctrl";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "Левый Ctrl";
            case GLFW.GLFW_KEY_SPACE -> "Пробел";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            default -> "Клавиша " + key;
        };
    }

    private Click transformClick(Click click) {
        double scale = appearance.getScale();

        double x = width / 2.0
                + (click.x() - width / 2.0) / scale;

        double y = height / 2.0
                + (click.y() - height / 2.0) / scale;

        return new Click(x, y,
                new MouseInput(click.button(), 0));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return super.mouseClicked(transformClick(click), doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        return super.mouseReleased(transformClick(click));
    }

    @Override
    public boolean mouseDragged(
            Click click,
            double deltaX,
            double deltaY
    ) {
        double scale = appearance.getScale();

        return super.mouseDragged(
                transformClick(click),
                deltaX / scale,
                deltaY / scale
        );
    }

    @Override
    public void close() {
        tickRate.suppressNextAttack();
        super.close();
    }

    private final class DistanceSlider extends SliderWidget {

        private DistanceSlider(
                int x,
                int y,
                int width,
                int height,
                double distance
        ) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Text.empty(),
                    (distance - 0.5) / 3.5
            );

            updateMessage();
        }

        @Override
        public void renderWidget(
                DrawContext context,
                int mouseX,
                int mouseY,
                float delta
        ) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();

            int background = isHovered()
                    ? BUTTON_HOVER : BUTTON;

            drawRounded(
                    context,
                    x, y, width, height,
                    6, background
            );

            int trackLeft = x + 7;
            int trackRight = x + width - 7;
            int trackWidth = Math.max(1, trackRight - trackLeft);
            int trackY = y + height - 5;

            int filledWidth = (int) Math.round(trackWidth * value);

            drawRounded(
                    context,
                    trackLeft, trackY,
                    trackWidth, 3,
                    2, BORDER
            );

            if (filledWidth > 0) {
                drawRounded(
                        context,
                        trackLeft, trackY,
                        filledWidth, 3,
                        2, ACCENT
                );
            }

            int thumbX = trackLeft + filledWidth - 4;

            thumbX = Math.max(
                    trackLeft - 1,
                    Math.min(trackRight - 7, thumbX)
            );

            drawRounded(
                    context,
                    thumbX - 1, y + 3,
                    10, height - 7,
                    4, ACCENT_DARK
            );

            drawRounded(
                    context,
                    thumbX, y + 4,
                    8, height - 9,
                    3, ACCENT_LIGHT
            );
        }

        private double getDistance() {
            int step = (int) Math.round(value * 35.0);
            return 0.5 + step * 0.1;
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(String.format(
                    Locale.ROOT,
                    "Дистанция: %.1f",
                    getDistance()
            )));
        }

        @Override
        protected void applyValue() {
            double distance = getDistance();

            config.setAttackDistance(distance);

            if (distanceField != null) {
                distanceField.setText(String.format(
                        Locale.ROOT,
                        "%.1f",
                        distance
                ));
            }
        }

        private void setDistance(double distance) {
            double normalized = (distance - 0.5) / 3.5;
            setValue(normalized);
            updateMessage();
        }
    }
}
