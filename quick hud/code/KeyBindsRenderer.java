package tech.renatclient.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import tech.renatclient.RenatClient;
import tech.renatclient.module.Module;
import tech.renatclient.module.list.render.hud.Interface;
import tech.renatclient.module.settings.BooleanSetting;
import tech.renatclient.module.settings.Setting;
import tech.renatclient.util.keyboard.KeyStorage;
import tech.renatclient.util.render.math.Animation;
import tech.renatclient.util.render.math.Easing;

import java.util.ArrayList;
import java.util.List;

public class KeyBindsRenderer {
    private final Interface owner;
    private final Animation widthAnim = new Animation(Easing.EXPO_OUT, 200);
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);

    public KeyBindsRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;
        renderQuick(context);
    }

    private void renderQuick(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        List<KeyBindEntry> activeBinds = collectActiveKeyBindEntries();
        int a = QuickStyle.toAlpha(alpha.run(activeBinds.isEmpty() && !chatOpen ? 0f : 1f));
        if (a <= 4) return;

        List<QuickStyle.Row> rows = new ArrayList<>(activeBinds.size());
        for (KeyBindEntry entry : activeBinds) {
            rows.add(QuickStyle.Row.of(entry.name, "[" + KeyStorage.getKey(entry.key) + "]", entry.animation));
        }
        QuickStyle.drawList(context, owner.getKeyBindsDrag(), widthAnim, QuickStyle.ICON_HOTKEYS, "HotKeys", rows, a);
    }

    private List<KeyBindEntry> collectActiveKeyBindEntries() {
        List<KeyBindEntry> entries = new ArrayList<>();
        for (Module module : RenatClient.getInstance().getModuleStorage().getModules()) {
            float moduleAnim = MathHelper.clamp((float) module.getAnimation().getValue(), 0f, 1f);
            if (module.getKey() != -1 && moduleAnim > 0.01f) {
                entries.add(new KeyBindEntry(module.getName(), module.getKey(), moduleAnim));
            }

            for (Setting setting : module.getSettings()) {
                if (!(setting instanceof BooleanSetting boolSetting)) continue;
                float settingAnim = MathHelper.clamp((float) boolSetting.getAnimation().getValue(), 0f, 1f);
                if (boolSetting.getKey() == -1 || settingAnim <= 0.01f) continue;
                entries.add(new KeyBindEntry(boolSetting.getName(), boolSetting.getKey(), settingAnim));
            }
        }
        return entries;
    }

    private static class KeyBindEntry {
        private final String name;
        private final int key;
        private final float animation;

        private KeyBindEntry(String name, int key, float animation) {
            this.name = name;
            this.key = key;
            this.animation = animation;
        }
    }
}
