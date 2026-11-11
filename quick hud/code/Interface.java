package tech.renatclient.module.list.render.hud;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import tech.renatclient.event.list.EventHUD;
import tech.renatclient.event.list.EventPacket;
import tech.renatclient.event.list.EventTick;
import tech.renatclient.module.Module;
import tech.renatclient.module.ModuleCategory;
import tech.renatclient.module.ModuleInformation;
import tech.renatclient.module.list.render.hud.renderers.HotbarRenderer;
import tech.renatclient.module.list.render.hud.renderers.KeyBindsRenderer;
import tech.renatclient.module.list.render.hud.renderers.LowHealthAlertRenderer;
import tech.renatclient.module.list.render.hud.renderers.MediaPlayerRenderer;
import tech.renatclient.module.list.render.hud.renderers.PotionsRenderer;
import tech.renatclient.module.list.render.hud.renderers.QuickElementMenu;
import tech.renatclient.module.list.render.hud.renderers.QuickHudRenderer;
import tech.renatclient.module.list.render.hud.renderers.QuickStyle;
import tech.renatclient.module.list.render.hud.renderers.SpeedRenderer;
import tech.renatclient.module.list.render.hud.renderers.StaffListRenderer;
import tech.renatclient.module.list.render.hud.renderers.TargetHudRenderer;
import tech.renatclient.module.list.render.hud.renderers.TotemCounterRenderer;
import tech.renatclient.module.settings.BooleanSetting;
import tech.renatclient.module.settings.ModeListSetting;
import tech.renatclient.module.settings.ModeSetting;
import tech.renatclient.module.settings.SliderSetting;
import tech.renatclient.util.chat.SpekTracker;
import tech.renatclient.util.base.Instance;
import tech.renatclient.util.draggable.DragManager;
import tech.renatclient.util.draggable.Draggable;
import tech.renatclient.util.render.helper.HoverUtil;
import tech.renatclient.util.render.renderers.IRenderer;
import tech.renatclient.util.render.math.Animation;
import tech.renatclient.util.render.math.Easing;
import tech.renatclient.util.render.msdf.Fonts;
import tech.renatclient.util.render.providers.ColorProvider;
import tech.renatclient.util.render.renderers.DrawUtil;

import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInformation(moduleName = "Interface", moduleCategory = ModuleCategory.RENDER)
public class Interface extends Module {
    private final ModeSetting hudStyle = new ModeSetting("Стиль HUD", "Quick", "Quick");
    private final ModeListSetting elements = new ModeListSetting("Элементы",
            new BooleanSetting("Ватермарка", true),
            new BooleanSetting("Активный таргет", true),
            new BooleanSetting("Привязанные модули", true),
            new BooleanSetting("Активные модераторы", true),
            new BooleanSetting("Бафы", true),
            new BooleanSetting("Скорость", true),
            new BooleanSetting("Счетчик тотемов", true),
            new BooleanSetting("Нотификации", true),
            new BooleanSetting("СпекТрекер", true),
            new BooleanSetting("MediaPlayer", true),
            new BooleanSetting("Хотбар", true),
            new BooleanSetting("Кулдауны", true),
            new BooleanSetting("Список модулей", true),
            new BooleanSetting("Тики", true),
            new BooleanSetting("Пинг", true)
    );
    private final ModeSetting targetStyle = new ModeSetting("Стиль таргета", "Quick", "Quick");
    private final SliderSetting backgroundIntensityTheme = new SliderSetting("Интенсивность темы", 0.15f, 0.05f, 1.0f, 0.01f);
    private final SliderSetting backgroundIntensityDef = new SliderSetting("Интенсивность основы", 0.15f, 0.05f, 1.0f, 0.01f);
    private final ModeListSetting blurBackgroundElements = new ModeListSetting("Блюр фона",
            new BooleanSetting("Ватермарка", true),
            new BooleanSetting("Активный таргет", true),
            new BooleanSetting("Привязанные модули", true),
            new BooleanSetting("Активные модераторы", true),
            new BooleanSetting("Бафы", true),
            new BooleanSetting("Счетчик тотемов", true),
            new BooleanSetting("Нотификации", true),
            new BooleanSetting("MediaPlayer", true),
            new BooleanSetting("Хотбар", true),
            new BooleanSetting("Оповещение о хп", true)
    ).setVisible(() -> false);
    private final ModeListSetting themedBackgroundElements = new ModeListSetting("Задний фон от темы",
            new BooleanSetting("Ватермарка", false),
            new BooleanSetting("Активный таргет", false),
            new BooleanSetting("Привязанные модули", false),
            new BooleanSetting("Активные модераторы", false),
            new BooleanSetting("Бафы", false),
            new BooleanSetting("Счетчик тотемов", false),
            new BooleanSetting("Нотификации", false),
            new BooleanSetting("MediaPlayer", false),
            new BooleanSetting("Хотбар", false),
            new BooleanSetting("Оповещение о хп", false)
    ).setVisible(() -> false);
    private final ModeSetting targetHudColorMode = new ModeSetting("Цвет таргет худа", "От здоровья", "От здоровья", "От темы")
            .setVisible(() -> false);

    private final SliderSetting lowHpAlertThreshold = new SliderSetting("Порог ХП оповещения", 8f, 1f, 20f, 0.5f);

    private static final SoundEvent SPEK_SOUND = SoundEvent.of(Identifier.of("mre", "spek"));

    @Getter
    private final Draggable watermarkDrag = DragManager.installDrag(this, "Watermark", 4, 4);
    @Getter
    private final Draggable keyBindsDrag = DragManager.installDrag(this, "HotKeys", 100, 50);
    @Getter
    private final Draggable staffListDrag = DragManager.installDrag(this, "StaffList", 200, 50);
    @Getter
    private final Draggable potionsDrag = DragManager.installDrag(this, "Potions", 300, 50);
    @Getter
    private final Draggable targetHUDDrag = DragManager.installDrag(this, "TargetHUD", 130, 130);
    @Getter
    private final Draggable totemCounterDrag = DragManager.installDrag(this, "TotemCounter", 200, 200);
    @Getter
    private final Draggable mediaPlayerDrag = DragManager.installDrag(this, "MediaPlayer", 20, 60);
    @Getter
    private final Draggable coolDownsDrag = DragManager.installDrag(this, "CoolDowns", 400, 50);
    @Getter
    private final Draggable arrayListDrag = DragManager.installDrag(this, "ArrayList", 4, 26);
    @Getter
    private final Draggable ticksDrag = DragManager.installDrag(this, "Ticks", 4000, 4000);
    @Getter
    private final Draggable pingDrag = DragManager.installDrag(this, "Ping", 2, 4000);
    private final Draggable notificationsStyleDrag = DragManager.installFixed(this, "NotificationsStyle");
    private final Draggable lowHpStyleDrag = DragManager.installFixed(this, "LowHpStyle");
    private final Draggable speedStyleDrag = DragManager.installFixed(this, "SpeedStyle");
    private final Draggable hotbarStyleDrag = DragManager.installFixed(this, "HotbarStyle");

    private final KeyBindsRenderer keyBindsRenderer = new KeyBindsRenderer(this);
    private final TargetHudRenderer targetHudRenderer = new TargetHudRenderer(this);
    private final LowHealthAlertRenderer lowHealthAlertRenderer = new LowHealthAlertRenderer(this);
    private final TotemCounterRenderer totemCounterRenderer = new TotemCounterRenderer(this);
    private final StaffListRenderer staffListRenderer = new StaffListRenderer(this);
    private final PotionsRenderer potionsRenderer = new PotionsRenderer(this);
    private final SpeedRenderer speedRenderer = new SpeedRenderer(this);
    private final MediaPlayerRenderer mediaPlayerRenderer = new MediaPlayerRenderer(this);
    private final HotbarRenderer hotbarRenderer = new HotbarRenderer(this);
    private final QuickHudRenderer quickHudRenderer = new QuickHudRenderer(this);
    private final QuickElementMenu quickElementMenu = new QuickElementMenu();

    private String activeBackgroundElement;
    private boolean elementContextMenuOpen;
    private String elementContextMenuElement;
    private float elementContextMenuX;
    private float elementContextMenuY;
    private float elementContextMenuWidth;
    private float elementContextMenuHeight;
    private float elementContextMenuAnchorX = Float.NaN;
    private float elementContextMenuAnchorY = Float.NaN;
    private float elementContextMenuBlurProgress;
    private float elementContextMenuThemeProgress;
    private float elementContextMenuTargetModeProgress;
    private float notificationPreviewX;
    private float notificationPreviewY;
    private float notificationPreviewWidth;
    private float notificationPreviewHeight;
    private final Animation notificationPreviewAnimation = new Animation(Easing.BACK_OUT, 300);

    private static final float ELEMENT_CONTEXT_MENU_WIDTH = 94.0f;
    private static final float ELEMENT_CONTEXT_MENU_WIDTH_COMPACT = 88.0f;
    private static final float ELEMENT_CONTEXT_MENU_PADDING = 4.0f;
    private static final float ELEMENT_CONTEXT_MENU_ROW_HEIGHT = 10.0f;
    private static final float ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH = 22.0f;
    private static final float ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT = 9.5f;
    private static final float ELEMENT_CONTEXT_MENU_TOGGLE_RADIUS = 3.2f;
    private static final float ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_PADDING = 0.55f;
    private static final float ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_REDUCTION = 0.5f;
    private static final float ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_RADIUS_DIVISOR = 2.5f;
    private static final float ELEMENT_CONTEXT_MENU_MODE_TOP = 1.5f;
    private static final float ELEMENT_CONTEXT_MENU_MODE_HEIGHT = 11.5f;
    private static final float ELEMENT_CONTEXT_MENU_MODE_GAP = 3.0f;
    private static final float ELEMENT_CONTEXT_MENU_MODE_BOTTOM = 0.5f;

    public void drawBackground(float x, float y, float w, float h, float radius, int alpha) {
        QuickStyle.drawPanel(x, y, w, h, radius, alpha);
    }

    private boolean isBackgroundOptionEnabled(ModeListSetting setting, boolean defaultValue) {
        if (activeBackgroundElement == null) {
            return defaultValue;
        }
        BooleanSetting option = setting.getOption(activeBackgroundElement);
        return option != null ? option.getValue() : defaultValue;
    }

    private void withBackgroundElement(String elementName, Runnable renderTask) {
        String previousElement = activeBackgroundElement;
        activeBackgroundElement = elementName;
        try {
            renderTask.run();
        } finally {
            activeBackgroundElement = previousElement;
        }
    }

    @Subscribe
    public void onEventHUD(EventHUD e) {
        if (mc.player == null || mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;

        DrawContext context = e.getDrawContext();
        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();

        if (elements.isEnabled("Нотификации")) {
            float pivotY = screenHeight / 2.0f + 20.0f;
            setFixedBounds(notificationsStyleDrag, screenWidth / 2.0f - 60.0f, pivotY, 120.0f, 18.0f, screenWidth / 2.0f, pivotY);
            renderQuickScaled(context, notificationsStyleDrag, screenWidth / 2.0f, pivotY, () -> withBackgroundElement("Нотификации", () -> {
                NotificationManager.render(context);
                renderNotificationPreview(context);
            }));
        } else {
            resetNotificationPreviewBounds();
        }
        boolean lowHpVisible = mc.player.getHealth() + mc.player.getAbsorptionAmount() <= lowHpAlertThreshold.getFloatValue() && !mc.player.isDead();
        setFixedBounds(lowHpStyleDrag, screenWidth / 2.0f - 75.0f, 100.0f, lowHpVisible ? 150.0f : 0.0f, lowHpVisible ? 15.0f : 0.0f, screenWidth / 2.0f, 100.0f);
        renderQuickScaled(context, lowHpStyleDrag, screenWidth / 2.0f, 100.0f,
                () -> withBackgroundElement("Оповещение о хп", () -> lowHealthAlertRenderer.render(context)));
        if (elements.isEnabled("Счетчик тотемов")) {
            renderQuickScaled(context, totemCounterDrag, totemCounterDrag.getX(), totemCounterDrag.getY(),
                    () -> withBackgroundElement("Счетчик тотемов", () -> totemCounterRenderer.render(context)));
        }
        if (elements.isEnabled("Ватермарка")) {
            quickHudRenderer.renderWatermark(context);
        }
        if (elements.isEnabled("Список модулей")) quickHudRenderer.renderArrayList(context);
        if (elements.isEnabled("Кулдауны")) quickHudRenderer.renderCooldowns(context);
        if (elements.isEnabled("Тики")) quickHudRenderer.renderTicks(context);
        if (elements.isEnabled("Пинг")) quickHudRenderer.renderPing(context);
        if (elements.isEnabled("Активный таргет")) {
            renderQuickScaled(context, targetHUDDrag, targetHUDDrag.getX(), targetHUDDrag.getY(),
                    () -> withBackgroundElement("Активный таргет", () -> targetHudRenderer.render(context)));
        }
        if (elements.isEnabled("Привязанные модули")) {
            withBackgroundElement("Привязанные модули", () -> keyBindsRenderer.render(context));
        }
        if (elements.isEnabled("Активные модераторы")) {
            withBackgroundElement("Активные модераторы", () -> staffListRenderer.render(context));
        }
        if (elements.isEnabled("Бафы")) {
            withBackgroundElement("Бафы", () -> potionsRenderer.render(context));
        }
        if (elements.isEnabled("Скорость")) {
            float pivotY = screenHeight / 2.0f + 8.0f;
            setFixedBounds(speedStyleDrag, screenWidth / 2.0f - 17.0f, pivotY, 34.0f, 13.0f, screenWidth / 2.0f, pivotY);
            renderQuickScaled(context, speedStyleDrag, screenWidth / 2.0f, pivotY, () -> speedRenderer.render(context));
        }
        if (elements.isEnabled("MediaPlayer")) {
            try {
                renderQuickScaled(context, mediaPlayerDrag, mediaPlayerDrag.getX(), mediaPlayerDrag.getY(),
                        () -> withBackgroundElement("MediaPlayer", () -> mediaPlayerRenderer.render(context)));
            } catch (Throwable ignored) { }
        }
        if (isHotbarEnabled()) {
            setFixedBounds(hotbarStyleDrag, screenWidth / 2.0f - 91.0f, screenHeight - 23.0f, 182.0f, 22.0f, screenWidth / 2.0f, screenHeight - 1.0f);
            renderQuickScaled(context, hotbarStyleDrag, screenWidth / 2.0f, screenHeight - 1.0f,
                    () -> withBackgroundElement("Хотбар", () -> hotbarRenderer.render(context)));
        }

        quickElementMenu.render();
    }

    private record QuickElement(String setting, String title, Draggable drag) {
    }

    private QuickElementMenu.Target getHoveredQuickElement(double mouseX, double mouseY) {
        QuickElement[] elementsToCheck = {
                new QuickElement("Пинг", "Ping", pingDrag),
                new QuickElement("Тики", "Ticks", ticksDrag),
                new QuickElement("Кулдауны", "CoolDowns", coolDownsDrag),
                new QuickElement("Бафы", "Potions", potionsDrag),
                new QuickElement("Активные модераторы", "StaffList", staffListDrag),
                new QuickElement("Привязанные модули", "HotKeys", keyBindsDrag),
                new QuickElement("Счетчик тотемов", "Totems", totemCounterDrag),
                new QuickElement("MediaPlayer", "MediaPlayer", mediaPlayerDrag),
                new QuickElement("Активный таргет", "TargetHUD", targetHUDDrag),
                new QuickElement("Список модулей", "ArrayList", arrayListDrag),
                new QuickElement("Ватермарка", "Watermark", watermarkDrag),
                new QuickElement(null, "Low HP", lowHpStyleDrag),
                new QuickElement("Скорость", "Speed", speedStyleDrag),
                new QuickElement("Нотификации", "Notifications", notificationsStyleDrag),
                new QuickElement("Хотбар", "Hotbar", hotbarStyleDrag)
        };
        for (QuickElement element : elementsToCheck) {
            boolean enabled = element.setting() == null || elements.isEnabled(element.setting());
            if (isDraggableHovered(enabled, element.drag(), mouseX, mouseY)) {
                return new QuickElementMenu.Target(element.title(), element.drag());
            }
        }
        return null;
    }

    private void renderQuickScaled(DrawContext context, Draggable drag, float pivotX, float pivotY, Runnable task) {
        float scale = drag.getScale();
        boolean scaled = Math.abs(scale - 1.0f) > 0.001f;
        float widthBefore = drag.getWidth();
        float heightBefore = drag.getHeight();

        QuickStyle.beginAppearance(drag);
        context.getMatrices().push();
        if (scaled) {
            context.getMatrices().translate(pivotX, pivotY, 0.0f);
            context.getMatrices().scale(scale, scale, 1.0f);
            context.getMatrices().translate(-pivotX, -pivotY, 0.0f);
            IRenderer.DEFAULT_MATRIX.identity().translate(pivotX, pivotY, 0.0f).scale(scale, scale, 1.0f).translate(-pivotX, -pivotY, 0.0f);
        }
        try {
            task.run();
        } finally {
            context.getMatrices().pop();
            IRenderer.DEFAULT_MATRIX.identity();
            QuickStyle.reset();
        }

        if (scaled && !drag.isFixed()) {
            if (drag.getWidth() != widthBefore) drag.setWidth(drag.getWidth() * scale);
            if (drag.getHeight() != heightBefore) drag.setHeight(drag.getHeight() * scale);
        }
    }

    private void setFixedBounds(Draggable drag, float x, float y, float width, float height, float pivotX, float pivotY) {
        float scale = drag.getScale();
        drag.setX(pivotX + (x - pivotX) * scale);
        drag.setY(pivotY + (y - pivotY) * scale);
        drag.setWidth(width * scale);
        drag.setHeight(height * scale);
    }

    @Subscribe
    private void onUpdate(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        if (elements.isEnabled("СпекТрекер")) {
            SpekTracker.tick(mc);
        }

        if (elements.isEnabled("Активные модераторы")) staffListRenderer.update();
        if (elements.isEnabled("Бафы")) potionsRenderer.updatePotions();
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (!elements.isEnabled("СпекТрекер") || mc.player == null) return;
        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            String rawContent = packet.content().getString();
            String triggerKeyword = SpekTracker.findTriggerKeyword(rawContent);
            String trackedSuspect = SpekTracker.findMatchingSuspect(rawContent);
            String suffix = triggerKeyword != null ? " [" + triggerKeyword + "]" : "";
            if (trackedSuspect != null) trackedSuspect += suffix;
            if (trackedSuspect != null) {
                NotificationManager.postWarning("Просят наблюдение: " + trackedSuspect);
                playSpekSound();
                return;
            }
            if (SpekTracker.isOwnCallout(rawContent)) {
                NotificationManager.postWarning("Пишут про тебя: " + mc.player.getName().getString());
                playSpekSound();
            }
        }
    }

    private void playSpekSound() {
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SPEK_SOUND, 1.0f));
        }
    }

    public int getThemeTint(int alpha) {
        int themeColor = ColorProvider.getThemeColor();
        return ColorProvider.setAlpha(themeColor, (int) (100 * (alpha / 255f) * backgroundIntensityTheme.getFloatValue()));
    }

    public boolean isQuickStyle() {
        return true;
    }

    public boolean handleChatOverlayClick(double mouseX, double mouseY, int button) {
        if (!isEnabled()) {
            return false;
        }
        closeElementContextMenu();
        return quickElementMenu.handleClick(mouseX, mouseY, button, getHoveredQuickElement(mouseX, mouseY));
    }

    public void onChatClosed() {
        quickElementMenu.close();
        closeElementContextMenu();
        resetNotificationPreviewBounds();
    }

    public BooleanSetting getBlurBackgroundOption(String elementName) {
        return blurBackgroundElements.getOption(elementName);
    }

    public BooleanSetting getThemeBackgroundOption(String elementName) {
        return themedBackgroundElements.getOption(elementName);
    }

    public ModeSetting getTargetHudColorModeSetting() {
        return targetHudColorMode;
    }

    private boolean handleElementContextMenuClick(double mouseX, double mouseY, int button) {
        if (!(mc.currentScreen instanceof ChatScreen)) {
            closeElementContextMenu();
            return false;
        }

        String hoveredElement = getHoveredContextElement(mouseX, mouseY);
        if (button == 1 && hoveredElement != null) {
            if (elementContextMenuOpen && hoveredElement.equals(elementContextMenuElement)) {
                closeElementContextMenu();
            } else {
                openElementContextMenu(hoveredElement, (float) mouseX, (float) mouseY);
            }
            return true;
        }

        if (!elementContextMenuOpen) {
            return false;
        }

        updateElementContextMenuBounds();
        if (button == 1) {
            return HoverUtil.isHovered(mouseX, mouseY, elementContextMenuX, elementContextMenuY, elementContextMenuWidth, elementContextMenuHeight);
        }

        if (button != 0 || !HoverUtil.isHovered(mouseX, mouseY, elementContextMenuX, elementContextMenuY, elementContextMenuWidth, elementContextMenuHeight)) {
            return false;
        }

        float toggleX = elementContextMenuX + elementContextMenuWidth - ELEMENT_CONTEXT_MENU_PADDING - ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH;

        BooleanSetting blurOption = getBlurBackgroundOption(elementContextMenuElement);
        float blurRowY = elementContextMenuY + ELEMENT_CONTEXT_MENU_PADDING;
        float blurToggleY = blurRowY + (ELEMENT_CONTEXT_MENU_ROW_HEIGHT - ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT) / 2.0f;
        if (blurOption != null && (
                HoverUtil.isHovered(mouseX, mouseY, elementContextMenuX + ELEMENT_CONTEXT_MENU_PADDING, blurRowY, elementContextMenuWidth - ELEMENT_CONTEXT_MENU_PADDING * 2.0f, ELEMENT_CONTEXT_MENU_ROW_HEIGHT) ||
                        HoverUtil.isHovered(mouseX, mouseY, toggleX, blurToggleY, ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH, ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT))) {
            blurOption.toggle();
            return true;
        }

        BooleanSetting themeOption = getThemeBackgroundOption(elementContextMenuElement);
        float themeRowY = blurRowY + ELEMENT_CONTEXT_MENU_ROW_HEIGHT;
        float themeToggleY = themeRowY + (ELEMENT_CONTEXT_MENU_ROW_HEIGHT - ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT) / 2.0f;
        if (themeOption != null && (
                HoverUtil.isHovered(mouseX, mouseY, elementContextMenuX + ELEMENT_CONTEXT_MENU_PADDING, themeRowY, elementContextMenuWidth - ELEMENT_CONTEXT_MENU_PADDING * 2.0f, ELEMENT_CONTEXT_MENU_ROW_HEIGHT) ||
                        HoverUtil.isHovered(mouseX, mouseY, toggleX, themeToggleY, ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH, ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT))) {
            themeOption.toggle();
            return true;
        }

        if ("Активный таргет".equals(elementContextMenuElement)) {
            float modeY = themeRowY + ELEMENT_CONTEXT_MENU_ROW_HEIGHT + ELEMENT_CONTEXT_MENU_MODE_TOP;
            float modeHeight = ELEMENT_CONTEXT_MENU_MODE_HEIGHT;
            float modeButtonWidth = (elementContextMenuWidth - ELEMENT_CONTEXT_MENU_PADDING * 2.0f - ELEMENT_CONTEXT_MENU_MODE_GAP) / 2.0f;
            float healthModeX = elementContextMenuX + ELEMENT_CONTEXT_MENU_PADDING;
            float themeModeX = healthModeX + modeButtonWidth + ELEMENT_CONTEXT_MENU_MODE_GAP;
            if (HoverUtil.isHovered(mouseX, mouseY, healthModeX, modeY, modeButtonWidth, modeHeight)) {
                targetHudColorMode.setValue("От здоровья");
                return true;
            }
            if (HoverUtil.isHovered(mouseX, mouseY, themeModeX, modeY, modeButtonWidth, modeHeight)) {
                targetHudColorMode.setValue("От темы");
                return true;
            }
        }

        return true;
    }

    private void renderElementContextMenu(DrawContext context) {
        if (!elementContextMenuOpen) {
            return;
        }
        if (!(mc.currentScreen instanceof ChatScreen) || !isEnabled() || elementContextMenuElement == null || !elements.isEnabled(elementContextMenuElement)) {
            closeElementContextMenu();
            return;
        }

        updateElementContextMenuBounds();

        int textColor = ColorProvider.rgba(225, 225, 225, 255);
        int rowTextColorOn = ColorProvider.rgba(210, 210, 210, 255);
        int rowTextColorOff = ColorProvider.rgba(210, 210, 210, 195);
        int themeColor = ColorProvider.getThemeColor();

        withBackgroundElement(elementContextMenuElement, () ->
                drawBackground(elementContextMenuX, elementContextMenuY, elementContextMenuWidth, elementContextMenuHeight, 3.2f, 210)
        );

        float toggleX = elementContextMenuX + elementContextMenuWidth - ELEMENT_CONTEXT_MENU_PADDING - ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH;

        BooleanSetting blurOption = getBlurBackgroundOption(elementContextMenuElement);
        float blurRowY = elementContextMenuY + ELEMENT_CONTEXT_MENU_PADDING;
        int blurTextColor = blurOption != null && blurOption.getValue() ? rowTextColorOn : rowTextColorOff;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Блюр фона", elementContextMenuX + ELEMENT_CONTEXT_MENU_PADDING, blurRowY + 2.6f, blurTextColor, 6.7f);
        if (blurOption != null) {
            elementContextMenuBlurProgress = animateMenuProgress(elementContextMenuBlurProgress, blurOption.getValue());
            float blurToggleY = blurRowY + (ELEMENT_CONTEXT_MENU_ROW_HEIGHT - ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT) / 2.0f;
            drawMenuToggle(toggleX, blurToggleY, ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH, ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT, elementContextMenuBlurProgress, themeColor);
        }

        BooleanSetting themeOption = getThemeBackgroundOption(elementContextMenuElement);
        float themeRowY = blurRowY + ELEMENT_CONTEXT_MENU_ROW_HEIGHT;
        int themeTextColor = themeOption != null && themeOption.getValue() ? rowTextColorOn : rowTextColorOff;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Фон от темы", elementContextMenuX + ELEMENT_CONTEXT_MENU_PADDING, themeRowY + 2.6f, themeTextColor, 6.7f);
        if (themeOption != null) {
            elementContextMenuThemeProgress = animateMenuProgress(elementContextMenuThemeProgress, themeOption.getValue());
            float themeToggleY = themeRowY + (ELEMENT_CONTEXT_MENU_ROW_HEIGHT - ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT) / 2.0f;
            drawMenuToggle(toggleX, themeToggleY, ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH, ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT, elementContextMenuThemeProgress, themeColor);
        }

        if ("Активный таргет".equals(elementContextMenuElement)) {
            float modeY = themeRowY + ELEMENT_CONTEXT_MENU_ROW_HEIGHT + ELEMENT_CONTEXT_MENU_MODE_TOP;
            float modeHeight = ELEMENT_CONTEXT_MENU_MODE_HEIGHT;
            float modeButtonWidth = (elementContextMenuWidth - ELEMENT_CONTEXT_MENU_PADDING * 2.0f - ELEMENT_CONTEXT_MENU_MODE_GAP) / 2.0f;
            float healthModeX = elementContextMenuX + ELEMENT_CONTEXT_MENU_PADDING;
            float themeModeX = healthModeX + modeButtonWidth + ELEMENT_CONTEXT_MENU_MODE_GAP;
            boolean healthActive = targetHudColorMode.is("От здоровья");
            boolean themeActive = targetHudColorMode.is("От темы");

            int activeButtonColor = ColorProvider.setAlpha(themeColor, 95);
            withBackgroundElement(elementContextMenuElement, () -> {
                drawBackground(healthModeX, modeY, modeButtonWidth, modeHeight, 3.0f, 190);
                drawBackground(themeModeX, modeY, modeButtonWidth, modeHeight, 3.0f, 190);
            });
            elementContextMenuTargetModeProgress = animateMenuProgress(elementContextMenuTargetModeProgress, themeActive);
            float modeHighlightX = healthModeX + (themeModeX - healthModeX) * elementContextMenuTargetModeProgress;
            DrawUtil.drawRound(modeHighlightX, modeY, modeButtonWidth, modeHeight, 3.0f, activeButtonColor);

            String healthText = "От хп";
            String themeText = "От темы";
            float modeTextY = modeY + (modeHeight - 6.7f) / 2.0f - 0.3f;
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), healthText, healthModeX + (modeButtonWidth - Fonts.SFMEDIUM.get().getWidth(healthText, 6.7f)) / 2.0f, modeTextY, textColor, 6.7f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), themeText, themeModeX + (modeButtonWidth - Fonts.SFMEDIUM.get().getWidth(themeText, 6.7f)) / 2.0f, modeTextY, textColor, 6.7f);
        }
    }

    private float animateMenuProgress(float currentValue, boolean enabled) {
        float targetValue = enabled ? 1.0f : 0.0f;
        float nextValue = currentValue + (targetValue - currentValue) * 0.24f;
        if (Math.abs(targetValue - nextValue) < 0.001f) {
            nextValue = targetValue;
        }
        return nextValue;
    }

    private void drawMenuToggle(float x, float y, float width, float height, float progress, int themeColor) {
        float clampedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        int offColor = ColorProvider.rgba(58, 62, 78, 210);
        int onColor = ColorProvider.setAlpha(themeColor, 165);
        int bgColor = ColorProvider.interpolateColor(offColor, onColor, clampedProgress);
        DrawUtil.drawRound(x, y, width, height, ELEMENT_CONTEXT_MENU_TOGGLE_RADIUS, bgColor);

        float knobSize = Math.max(
                1.0f,
                height - (ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_PADDING * 2.0f) - ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_REDUCTION
        );
        float minKnobX = x + ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_PADDING;
        float maxKnobX = x + width - knobSize - ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_PADDING;
        float knobX = minKnobX + (maxKnobX - minKnobX) * clampedProgress;
        float knobY = y + (height - knobSize) / 2.0f;
        int knobOffColor = ColorProvider.rgba(206, 206, 206, 255);
        int knobOnColor = ColorProvider.rgba(232, 232, 232, 255);
        int knobColor = ColorProvider.interpolateColor(knobOffColor, knobOnColor, clampedProgress);
        DrawUtil.drawRound(knobX, knobY, knobSize, knobSize, knobSize / ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_RADIUS_DIVISOR, knobColor);
    }

    private String getHoveredContextElement(double mouseX, double mouseY) {
        if (isNotificationPreviewHovered(mouseX, mouseY)) return "Нотификации";
        if (isDraggableHovered(elements.isEnabled("Активный таргет"), targetHUDDrag, mouseX, mouseY)) return "Активный таргет";
        if (isDraggableHovered(elements.isEnabled("Привязанные модули"), keyBindsDrag, mouseX, mouseY)) return "Привязанные модули";
        if (isDraggableHovered(elements.isEnabled("Активные модераторы"), staffListDrag, mouseX, mouseY)) return "Активные модераторы";
        if (isDraggableHovered(elements.isEnabled("Бафы"), potionsDrag, mouseX, mouseY)) return "Бафы";
        if (isDraggableHovered(elements.isEnabled("Счетчик тотемов"), totemCounterDrag, mouseX, mouseY)) return "Счетчик тотемов";
        if (isDraggableHovered(elements.isEnabled("MediaPlayer"), mediaPlayerDrag, mouseX, mouseY)) return "MediaPlayer";
        return null;
    }

    private boolean isDraggableHovered(boolean enabled, Draggable drag, double mouseX, double mouseY) {
        return enabled && drag.getWidth() > 1.0f && drag.getHeight() > 1.0f &&
                HoverUtil.isHovered(mouseX, mouseY, drag.getX(), drag.getY(), drag.getWidth(), drag.getHeight());
    }

    private boolean isNotificationPreviewHovered(double mouseX, double mouseY) {
        return elements.isEnabled("Нотификации")
                && notificationPreviewWidth > 1.0f
                && notificationPreviewHeight > 1.0f
                && HoverUtil.isHovered(mouseX, mouseY, notificationPreviewX, notificationPreviewY, notificationPreviewWidth, notificationPreviewHeight);
    }

    private void renderNotificationPreview(DrawContext context) {
        boolean shouldShow = mc.currentScreen instanceof ChatScreen && !NotificationManager.hasActiveNotifications();
        float scale = Math.max(0.0f, Math.min(1.0f, notificationPreviewAnimation.run(shouldShow ? 1.0f : 0.0f)));
        if (scale <= 0.01f) {
            resetNotificationPreviewBounds();
            return;
        }

        String previewText = "Пример уведомления";
        float centerX = mc.getWindow().getScaledWidth() / 2.0f;
        float y = mc.getWindow().getScaledHeight() / 2.0f + 20.0f;
        float width;
        float height;
        float x;

        float textSize = 7.0f;
        float iconWidth = Fonts.ICONS_HUD.get().getWidth("J", 9.0f);
        height = 14.5f;
        width = iconWidth + Fonts.SFMEDIUM.get().getWidth(previewText, textSize) + 22.0f;
        x = centerX - width / 2.0f;

        context.getMatrices().push();
        context.getMatrices().translate(centerX, y + height / 2.0f, 0.0f);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-centerX, -(y + height / 2.0f), 0.0f);

        int alphaInt = (int) (255 * scale);
        drawBackground(x, y, width, height, 4.0f, alphaInt);
        DrawUtil.drawText(Fonts.ICONS_HUD.get(), "J", x + 5.0f, y + 4.0f, ColorProvider.rgba(125, 255, 125, alphaInt), 9.0f);
        DrawUtil.drawRound(x + 18.0f, y + 2.5f, 0.5f, height - 5.0f, 0.0f, ColorProvider.rgba(255, 255, 255, (int) (120 * scale)));
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), previewText, x + 23.0f, y + 3.0f, ColorProvider.rgba(255, 255, 255, alphaInt), textSize);
        context.getMatrices().pop();

        float scaledWidth = width * scale;
        float scaledHeight = height * scale;
        notificationPreviewX = centerX - scaledWidth / 2.0f;
        notificationPreviewY = y + (height - scaledHeight) / 2.0f;
        notificationPreviewWidth = scaledWidth;
        notificationPreviewHeight = scaledHeight;
    }

    private void resetNotificationPreviewBounds() {
        notificationPreviewX = 0.0f;
        notificationPreviewY = 0.0f;
        notificationPreviewWidth = 0.0f;
        notificationPreviewHeight = 0.0f;
    }

    private void openElementContextMenu(String elementName, float anchorX, float anchorY) {
        elementContextMenuOpen = true;
        elementContextMenuElement = elementName;
        elementContextMenuAnchorX = anchorX;
        elementContextMenuAnchorY = anchorY;
        BooleanSetting blurOption = getBlurBackgroundOption(elementName);
        elementContextMenuBlurProgress = blurOption != null && blurOption.getValue() ? 1.0f : 0.0f;
        BooleanSetting themeOption = getThemeBackgroundOption(elementName);
        elementContextMenuThemeProgress = themeOption != null && themeOption.getValue() ? 1.0f : 0.0f;
        elementContextMenuTargetModeProgress = targetHudColorMode.getIndex() == 1 ? 1.0f : 0.0f;
        updateElementContextMenuBounds();
    }

    private void closeElementContextMenu() {
        elementContextMenuOpen = false;
        elementContextMenuElement = null;
        elementContextMenuAnchorX = Float.NaN;
        elementContextMenuAnchorY = Float.NaN;
    }

    private void updateElementContextMenuBounds() {
        elementContextMenuWidth = getElementContextMenuWidth(elementContextMenuElement);
        float baseHeight = ELEMENT_CONTEXT_MENU_PADDING
                + (ELEMENT_CONTEXT_MENU_ROW_HEIGHT * 2.0f)
                + ELEMENT_CONTEXT_MENU_PADDING;
        if ("Активный таргет".equals(elementContextMenuElement)) {
            baseHeight += ELEMENT_CONTEXT_MENU_MODE_TOP + ELEMENT_CONTEXT_MENU_MODE_HEIGHT + ELEMENT_CONTEXT_MENU_MODE_BOTTOM;
        }
        elementContextMenuHeight = baseHeight;

        float baseX = Float.isNaN(elementContextMenuAnchorX) ? 8.0f : elementContextMenuAnchorX + 4.0f;
        float baseY = Float.isNaN(elementContextMenuAnchorY) ? 8.0f : elementContextMenuAnchorY + 4.0f;
        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();
        elementContextMenuX = Math.max(2.0f, Math.min(baseX, screenWidth - elementContextMenuWidth - 2.0f));
        elementContextMenuY = Math.max(2.0f, Math.min(baseY, screenHeight - elementContextMenuHeight - 2.0f));
    }

    private float getElementContextMenuWidth(String elementName) {
        if ("Бафы".equals(elementName) || "Привязанные модули".equals(elementName)) {
            return ELEMENT_CONTEXT_MENU_WIDTH_COMPACT;
        }
        return ELEMENT_CONTEXT_MENU_WIDTH;
    }

    public ModeSetting getHudStyleSetting() { return hudStyle; }
    public ModeSetting getTargetStyleSetting() { return targetStyle; }
    public ModeListSetting getElementsSetting() { return elements; }
    public boolean isTargetHudThemeMode() { return targetHudColorMode.is("От темы"); }
    public SliderSetting getLowHpAlertThresholdSetting() { return lowHpAlertThreshold; }
    public boolean isHotbarEnabled() { return elements.isEnabled("Хотбар"); }

    public static class NotificationManager {
        private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

        public static void post(String name, boolean enabled) {
            notifications.add(0, new Notification(name, enabled));
        }

        public static void postCheckbox(String name, boolean enabled) {
            Notification notification = new Notification(name, enabled);
            notification.isCheckbox = true;
            notifications.add(0, notification);
        }

        public static void postWarning(String text) {
            notifications.add(0, new Notification(text, true, true));
        }

        public static void postItemWarning(String text, ItemStack stack) {
            notifications.add(0, new Notification(text, true, true, stack));
        }

        public static void postItemWarning(Text text, ItemStack stack) {
            notifications.add(0, new Notification(text, true, true, stack));
        }

        public static void postItemInfo(String text, ItemStack stack) {
            notifications.add(0, new Notification(text, true, false, stack));
        }

        public static void postItemInfo(Text text, ItemStack stack) {
            notifications.add(0, new Notification(text, true, false, stack));
        }

        public static void postAutoSwapInfo(String text, ItemStack stack) {
            Notification notification = new Notification(text, true, false, stack);
            notification.style = NotificationStyle.AUTO_SWAP;
            notifications.add(0, notification);
        }

        public static void postAutoSwapInfo(Text text, ItemStack stack) {
            Notification notification = new Notification(text, true, false, stack);
            notification.style = NotificationStyle.AUTO_SWAP;
            notifications.add(0, notification);
        }

        public static void render(DrawContext context) {
            float centerX = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;
            float startY = (MinecraftClient.getInstance().getWindow().getScaledHeight() / 2f) + 20f;
            float offset = 0;
            Interface interfaceModule = Instance.get(Interface.class);

            for (Notification n : notifications) {
                if (System.currentTimeMillis() - n.time > n.duration && n.anim.getValue() <= 0.01) {
                    notifications.remove(n);
                    continue;
                }

                boolean expiring = System.currentTimeMillis() - n.time > n.duration;
                n.anim.run(expiring ? 0 : 1);
                float clampedScale = (float) Math.max(0.0, Math.min(1.0, n.anim.getValue()));
                if (clampedScale <= 0.01f) continue;

                int alphaInt = (int) (255 * clampedScale);
                float textSize = 7f;
                String fullText;
                Text fullTextComponent;
                boolean drawItemIcon = n.iconStack != null && !n.iconStack.isEmpty();

                if (n.hasCustomText()) {
                    fullText = n.customText;
                    fullTextComponent = n.customTextComponent;
                } else {
                    fullTextComponent = null;
                    fullText =  "«" +n.name + "» " + buildModuleStateText(n);
                }

                String iconCode = n.hasCustomText() ? (n.isWarning ? "G" : "J") : (n.enabled ? "J" : "K");
                int iconColor = n.enabled ? ColorProvider.rgba(125, 255, 125, alphaInt) : ColorProvider.rgba(255, 125, 125, alphaInt);
                float height = 14.5f;
                float textWidth = fullTextComponent != null
                        ? Fonts.SFMEDIUM.get().getWidth(fullTextComponent, textSize)
                        : Fonts.SFMEDIUM.get().getWidth(fullText, textSize);
                float iconWidth = drawItemIcon ? 12f : Fonts.ICONS_HUD.get().getWidth(iconCode, 9f);
                float width = iconWidth + textWidth + n.getRightPadding();

                float x = centerX - (width / 2f);
                float y = startY + offset;

                context.getMatrices().push();
                context.getMatrices().translate(centerX, y + height / 2f, 0);
                context.getMatrices().scale(clampedScale, clampedScale, 1f);
                context.getMatrices().translate(-centerX, -(y + height / 2f), 0);

                interfaceModule.drawBackground(x, y, width, height, 4, alphaInt);

                if (drawItemIcon) {
                    context.getMatrices().push();
                    context.getMatrices().translate(x + n.getItemIconOffsetX(), y + n.getItemIconOffsetY(), 0f);
                    context.getMatrices().scale(0.58f, 0.58f, 1f);
                    context.drawItem(n.iconStack, 0, 0);
                    context.getMatrices().pop();
                } else {
                    DrawUtil.drawText(Fonts.ICONS_HUD.get(), iconCode, x + 5, y + 4, iconColor, 9f);
                }

                DrawUtil.drawRound(x + 18f, y + 2.5f, 0.5f, height - 5f, 0, ColorProvider.rgba(255, 255, 255, (int) (120 * clampedScale)));
                float textY = drawItemIcon ? y + 3.2f : y + 3f;
                if (fullTextComponent != null) {
                    DrawUtil.drawText(Fonts.SFMEDIUM.get(), fullTextComponent, x + 23f, textY, textSize, alphaInt);
                } else {
                    DrawUtil.drawText(Fonts.SFMEDIUM.get(), fullText, x + 23f, textY, ColorProvider.rgba(255, 255, 255, alphaInt), textSize);
                }

                context.getMatrices().pop();
                offset += (height + 3) * clampedScale;
            }
        }

        public static boolean hasActiveNotifications() {
            return !notifications.isEmpty();
        }

        private static String buildToggleText(Notification notification) {
            String prefix = notification.isCheckbox ? "Чекбокс " : "Модуль ";
            String state = notification.enabled ? "Включен!" : "Выключен!";
            return prefix + notification.name + " " + state;
        }

        private static String buildToggleTextStable(Notification notification) {
            String prefix = notification.isCheckbox ? "Чекбокс " : "Модуль ";
            String state = notification.enabled ? "Включен!" : "Выключен!";
            return prefix + notification.name + " " + state;
        }

        private static String buildModuleStateText(Notification notification) {
            return notification.enabled ? "включен!" : "выключен!";
        }

        private enum NotificationStyle {
            DEFAULT,
            AUTO_SWAP
        }

        private static class Notification {
            String name;
            boolean enabled;
            long time;
            long duration = 1000;
            Animation anim = new Animation(Easing.BACK_OUT, 300);
            boolean isWarning = false;
            String customText;
            Text customTextComponent;
            ItemStack iconStack;
            boolean isCheckbox = false;
            NotificationStyle style = NotificationStyle.DEFAULT;

            Notification(String name, boolean enabled) {
                this.name = name;
                this.enabled = enabled;
                this.time = System.currentTimeMillis();
            }

            Notification(String customText, boolean enabled, boolean isWarning) {
                this(customText, enabled, isWarning, null);
            }

            Notification(String customText, boolean enabled, boolean isWarning, ItemStack iconStack) {
                this.customText = customText;
                this.enabled = enabled;
                this.isWarning = isWarning;
                this.iconStack = iconStack == null ? null : iconStack.copy();
                this.time = System.currentTimeMillis();
                this.duration = 2000;
            }

            Notification(Text customTextComponent, boolean enabled, boolean isWarning, ItemStack iconStack) {
                this.customText = customTextComponent.getString();
                this.customTextComponent = customTextComponent.copy();
                this.enabled = enabled;
                this.isWarning = isWarning;
                this.iconStack = iconStack == null ? null : iconStack.copy();
                this.time = System.currentTimeMillis();
                this.duration = 2000;
            }

            boolean hasCustomText() {
                return customText != null && !customText.isEmpty();
            }

            float getItemIconOffsetX() {
                return style == NotificationStyle.AUTO_SWAP ? 4.8f : 4.0f;
            }

            float getItemIconOffsetY() {
                return style == NotificationStyle.AUTO_SWAP ? 2.1f : 1.0f;
            }

            float getRightPadding() {
                return style == NotificationStyle.AUTO_SWAP ? 19f : 22f;
            }
        }
    }
}
