package tech.renatclient.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffectInstance;
import tech.renatclient.module.list.render.hud.Interface;
import tech.renatclient.util.render.math.Animation;
import tech.renatclient.util.render.math.Easing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class PotionsRenderer {
    private final Interface owner;
    private final Animation widthAnim = new Animation(Easing.EXPO_OUT, 200);
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);
    private final List<PotionItem> potionItems = new CopyOnWriteArrayList<>();

    public PotionsRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;
        renderQuick(context);
    }

    public void updatePotions() {
        Map<String, StatusEffectInstance> currentEffects = owner.mc.player.getStatusEffects().stream()
                .collect(Collectors.toMap(
                        e -> net.minecraft.text.Text.translatable(e.getTranslationKey()).getString() + ":" + e.getAmplifier(),
                        e -> e,
                        (e1, e2) -> e1
                ));

        potionItems.forEach(item -> {
            String key = item.name + ":" + item.amplifier;
            StatusEffectInstance effect = currentEffects.get(key);
            if (effect != null) {
                item.durationTicks = effect.getDuration();
                if (!item.active) item.animation.setValue(1.0f);
                item.active = true;
                currentEffects.remove(key);
            } else {
                item.active = false;
            }
        });

        currentEffects.forEach((key, effect) -> potionItems.add(new PotionItem(
                net.minecraft.text.Text.translatable(effect.getTranslationKey()).getString(),
                effect.getAmplifier(),
                effect.getDuration(),
                effect.getEffectType()
        )));

        potionItems.removeIf(item -> !item.active && item.animation.getValue() == 0);
    }

    private void renderQuick(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        List<QuickStyle.Row> rows = new ArrayList<>();
        for (PotionItem item : potionItems) {
            float anim = item.animation.run(item.active ? 1f : 0f);
            if (anim <= 0.01f) continue;
            String level = item.amplifier > 0 ? String.valueOf(item.amplifier + 1) : null;
            String time = item.durationTicks < 0 ? "**:**" : formatPotionTime(item.durationTicks);
            boolean expiring = item.durationTicks >= 0 && item.durationTicks < 200;
            Sprite sprite = owner.mc.getStatusEffectSpriteManager().getSprite(item.effect);
            rows.add(QuickStyle.Row.effect(item.name, level, time, expiring ? QuickStyle.LEVEL_COLOR : 0, anim, sprite));
        }

        int a = QuickStyle.toAlpha(alpha.run(rows.isEmpty() && !chatOpen ? 0f : 1f));
        if (a <= 4) return;
        QuickStyle.drawList(context, owner.getPotionsDrag(), widthAnim, QuickStyle.ICON_POTIONS, "Potions", rows, a);
    }

    private String formatPotionTime(int durationTicks) {
        int totalSec = Math.max(0, durationTicks / 20);
        return String.format("%d:%02d", totalSec / 60, totalSec % 60);
    }

    private static class PotionItem {
        String name;
        int amplifier;
        int durationTicks;
        boolean active;
        net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect;
        Animation animation = new Animation(Easing.EXPO_OUT, 233);

        PotionItem(String name, int amplifier, int durationTicks, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
            this.name = name;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
            this.effect = effect;
            this.active = true;
        }
    }
}
