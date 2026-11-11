package dev.async;

import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AsyncAppearanceConfig {
    private final Path path = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("async-appearance.properties");

    private int accentColor = 0xFFB45CFF;
    private float scale = 1.0F;

    public AsyncAppearanceConfig() {
        load();
    }

    public int getAccentColor() {
        return accentColor;
    }

    public float getScale() {
        return scale;
    }

    public void nextAccentColor() {
        int[] colors = {
                0xFFB45CFF,
                0xFF55DDF5,
                0xFFFF5CAB,
                0xFF55F5B0,
                0xFFFFC857
        };

        int current = 0;
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == accentColor) {
                current = i;
                break;
            }
        }

        accentColor = colors[(current + 1) % colors.length];
        save();
    }

    public void changeScale(float amount) {
        scale = Math.max(0.80F, Math.min(1.20F, scale + amount));
        scale = Math.round(scale * 100.0F) / 100.0F;
        save();
    }

    private void load() {
        if (!Files.exists(path)) {
            save();
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);

            String color = properties.getProperty("accentColor");
            if (color != null) {
                accentColor = (int) Long.parseLong(color, 16);
            }

            String savedScale = properties.getProperty("scale");
            if (savedScale != null) {
                float parsed = Float.parseFloat(savedScale);
                if (Float.isFinite(parsed)) {
                    scale = Math.max(0.80F, Math.min(1.20F, parsed));
                }
            }
        } catch (Exception ignored) {
            accentColor = 0xFFB45CFF;
            scale = 1.0F;
        }
    }

    private void save() {
        Properties properties = new Properties();
        properties.setProperty("accentColor",
                Integer.toHexString(accentColor));
        properties.setProperty("scale", Float.toString(scale));

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                properties.store(writer, "Async ClickGUI appearance");
            }
        } catch (Exception ignored) {
        }
    }
}
