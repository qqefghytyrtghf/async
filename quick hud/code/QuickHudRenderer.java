package tech.renatclient.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import tech.renatclient.RenatClient;
import tech.renatclient.module.Module;
import tech.renatclient.module.list.render.hud.Interface;
import tech.renatclient.util.draggable.Draggable;
import tech.renatclient.util.math.Counter;
import tech.renatclient.util.player.CooldownTracker;
import tech.renatclient.util.render.math.Animation;
import tech.renatclient.util.render.math.Easing;
import tech.renatclient.util.render.msdf.Fonts;
import tech.renatclient.util.render.providers.ColorProvider;
import tech.renatclient.util.render.renderers.DrawUtil;
import tech.renatclient.util.server.Server;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class QuickHudRenderer {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String SEPARATOR = "»";
    private static final float PILL_HEIGHT = 16.0f;
    private static final float ARRAY_ROW_HEIGHT = 11.0f;
    private static final float SCREEN_MARGIN = 2.0f;

    private final Interface owner;
    private final Map<Identifier, CooldownEntry> cooldowns = new LinkedHashMap<>();
    private final Animation cooldownsAlpha = new Animation(Easing.EXPO_OUT, 200);
    private final Animation cooldownsWidth = new Animation(Easing.EXPO_OUT, 200);
    private final Animation watermarkWidth = new Animation(Easing.EXPO_OUT, 200);

    public QuickHudRenderer(Interface owner) {
        this.owner = owner;
    }

    public void renderWatermark(DrawContext context) {
        Counter.updateFPS();
        Draggable drag = owner.getWatermarkDrag();
        QuickStyle.begin(drag);

        String playerName = owner.mc.player != null ? owner.mc.player.getName().getString() : "Player";
        String[] parts = {
                "Quick",
                playerName,
                "Fps " + Counter.getCurrentFPS(),
                LocalTime.now().format(TIME_FORMAT)
        };
        float gap = QuickStyle.s(4.0f);
        float padding = QuickStyle.s(QuickStyle.PADDING);
        float height = QuickStyle.s(PILL_HEIGHT);
        float iconSize = QuickStyle.s(QuickStyle.ICON_SIZE);
        float iconWidth = QuickStyle.icons().getWidth(QuickStyle.ICON_LOGO, iconSize);
        float separatorWidth = Fonts.SFMEDIUM.get().getWidth(SEPARATOR, QuickStyle.s(QuickStyle.TEXT_SIZE));

        float targetWidth = padding + iconWidth + padding;
        for (String part : parts) {
            targetWidth += gap + separatorWidth + gap + QuickStyle.textWidth(part);
        }
        float width = Math.max(targetWidth, watermarkWidth.run(targetWidth));

        float x = drag.getX();
        float y = drag.getY();
        QuickStyle.drawPanel(x, y, width, height, QuickStyle.s(QuickStyle.RADIUS), 255);

        float cursor = x + padding;
        DrawUtil.drawText(QuickStyle.icons(), QuickStyle.ICON_LOGO, cursor, QuickStyle.iconY(y, height), QuickStyle.accent(255), iconSize);
        cursor += iconWidth;

        float textY = QuickStyle.textY(y, height);
        for (int i = 0; i < parts.length; i++) {
            cursor += gap;
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), SEPARATOR, cursor, textY, QuickStyle.gray(255), QuickStyle.s(QuickStyle.TEXT_SIZE));
            cursor += separatorWidth + gap;
            QuickStyle.drawText(parts[i], cursor, textY, i == 0 ? QuickStyle.accent(255) : QuickStyle.white(255));
            cursor += QuickStyle.textWidth(parts[i]);
        }

        drag.setWidth(width);
        drag.setHeight(height);
        QuickStyle.reset();
    }

    public void renderCooldowns(DrawContext context) {
        if (owner.mc.player == null) return;
        updateCooldowns();

        List<QuickStyle.Row> rows = new ArrayList<>(cooldowns.size());
        for (CooldownEntry entry : cooldowns.values()) {
            float anim = entry.animation.run(entry.active ? 1f : 0f);
            if (anim <= 0.01f) continue;
            rows.add(QuickStyle.Row.of(entry.name, entry.timeText, anim));
        }

        boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        int a = QuickStyle.toAlpha(cooldownsAlpha.run(rows.isEmpty() && !chatOpen ? 0f : 1f));
        if (a <= 4) return;
        QuickStyle.drawList(context, owner.getCoolDownsDrag(), cooldownsWidth, QuickStyle.ICON_COOLDOWNS, "CoolDowns", rows, a);
    }

    private void updateCooldowns() {
        ItemCooldownManager manager = owner.mc.player.getItemCooldownManager();
        Set<Identifier> seen = new HashSet<>();
        List<ItemStack> stacks = new ArrayList<>(owner.mc.player.getInventory().main);
        stacks.addAll(owner.mc.player.getInventory().offHand);

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            Identifier group = manager.getGroup(stack);
            if (seen.contains(group)) continue;
            float progress = manager.getCooldownProgress(stack, 0.0f);
            if (progress <= 0.0f) continue;

            seen.add(group);
            CooldownEntry entry = cooldowns.computeIfAbsent(group, g -> new CooldownEntry());
            entry.active = true;
            entry.name = stack.getItem().getName().getString();
            int duration = CooldownTracker.getDuration(group);
            entry.timeText = duration > 0
                    ? String.format(Locale.US, "%.1fs", progress * duration / 20.0f)
                    : (int) (progress * 100.0f) + "%";
        }

        for (Map.Entry<Identifier, CooldownEntry> entry : cooldowns.entrySet()) {
            if (!seen.contains(entry.getKey())) entry.getValue().active = false;
        }
        cooldowns.values().removeIf(entry -> !entry.active && entry.animation.getValue() <= 0.0f);
    }

    public void renderArrayList(DrawContext context) {
        Draggable drag = owner.getArrayListDrag();
        QuickStyle.begin(drag);

        List<Module> modules = new ArrayList<>();
        for (Module module : RenatClient.getInstance().getModuleStorage().getModules()) {
            if (module == owner) continue;
            if (MathHelper.clamp(module.getAnimation().getValue(), 0f, 1f) > 0.01f) modules.add(module);
        }
        modules.sort(Comparator.comparingDouble((Module m) -> QuickStyle.textWidth(m.getName())).reversed());

        float x = drag.getX();
        float y = drag.getY();
        float currentY = y;
        float maxWidth = 0.0f;
        float textPadding = QuickStyle.s(4.0f);
        int themeOne = ColorProvider.getThemeColor();
        int themeTwo = ColorProvider.getThemeColorTwo();

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            float anim = MathHelper.clamp(module.getAnimation().getValue(), 0f, 1f);
            int a = QuickStyle.toAlpha(anim);
            float rowWidth = QuickStyle.textWidth(module.getName()) + textPadding * 2.0f;
            float rowHeight = QuickStyle.s(ARRAY_ROW_HEIGHT) * anim;
            float rowX = x - rowWidth * (1.0f - anim);

            int backgroundAlpha = (int) (a * drag.getOpacity());
            DrawUtil.drawRoundBlur(rowX, currentY, rowWidth, rowHeight, QuickStyle.s(2.0f),
                    ColorProvider.rgba(255, 255, 255, backgroundAlpha), 45);
            DrawUtil.drawRound(rowX, currentY, rowWidth, rowHeight, QuickStyle.s(2.0f),
                    ColorProvider.rgba(0, 0, 0, (int) (backgroundAlpha * 0.4f)));

            int gradient = ColorProvider.gradient(10, i * 20, themeOne, themeTwo);
            int textColor = ColorProvider.setAlpha(ColorProvider.interpolateColor(gradient, ColorProvider.rgba(255, 255, 255, 255), 0.35f), a);
            QuickStyle.drawText(module.getName(), rowX + textPadding, QuickStyle.textY(currentY, rowHeight), textColor);

            currentY += rowHeight;
            maxWidth = Math.max(maxWidth, rowWidth);
        }

        drag.setWidth(Math.max(QuickStyle.s(40.0f), maxWidth));
        drag.setHeight(Math.max(QuickStyle.s(ARRAY_ROW_HEIGHT), currentY - y));
        QuickStyle.reset();
    }

    public void renderTicks(DrawContext context) {
        String value = String.format(Locale.US, "%.2f", RenatClient.getInstance().getTpsGetter().getTPS());
        renderInfoPill(owner.getTicksDrag(), value, "Ticks");
    }

    public void renderPing(DrawContext context) {
        if (owner.mc.player == null) return;
        renderInfoPill(owner.getPingDrag(), String.valueOf(Server.getPing(owner.mc.player)), "Ping");
    }

    private void renderInfoPill(Draggable drag, String value, String label) {
        QuickStyle.begin(drag);
        float padding = QuickStyle.s(QuickStyle.PADDING);
        float gap = QuickStyle.s(3.0f);
        float height = QuickStyle.s(PILL_HEIGHT);
        float valueWidth = QuickStyle.textWidth(value);
        float width = padding + valueWidth + gap + QuickStyle.textWidth(label) + padding;

        drag.setWidth(width);
        drag.setHeight(height);
        keepOnScreen(drag);
        float x = drag.getX();
        float y = drag.getY();

        QuickStyle.drawPanel(x, y, width, height, QuickStyle.s(QuickStyle.RADIUS), 255);
        float textY = QuickStyle.textY(y, height);
        QuickStyle.drawText(value, x + padding, textY, QuickStyle.white(255));
        QuickStyle.drawText(label, x + padding + valueWidth + gap, textY, QuickStyle.accent(255));
        QuickStyle.reset();
    }

    private void keepOnScreen(Draggable drag) {
        if (drag.isDragging()) return;
        float maxX = owner.mc.getWindow().getScaledWidth() - drag.getWidth() - SCREEN_MARGIN;
        float maxY = owner.mc.getWindow().getScaledHeight() - drag.getHeight() - SCREEN_MARGIN;
        drag.setX(MathHelper.clamp(drag.getX(), 0.0f, Math.max(0.0f, maxX)));
        drag.setY(MathHelper.clamp(drag.getY(), 0.0f, Math.max(0.0f, maxY)));
    }

    private static class CooldownEntry {
        String name = "";
        String timeText = "";
        boolean active;
        final Animation animation = new Animation(Easing.EXPO_OUT, 233);
    }
}
