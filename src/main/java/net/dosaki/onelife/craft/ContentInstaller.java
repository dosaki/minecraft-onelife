package net.dosaki.onelife.craft;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Copies the bundled gravestone content into CraftEngine's resources folder. CraftEngine reads its resources
 * on the first tick after every plugin has enabled, so copying in onEnable needs no reload.
 */
public final class ContentInstaller {

    public static final List<String> FILES = List.of(
            "pack.yml",
            "configuration/gravestone.yml",
            "resourcepack/assets/onelife/models/item/gravestone.json",
            "resourcepack/assets/onelife/textures/item/gravestone.png");

    private ContentInstaller() {}

    public static void install(JavaPlugin plugin) throws IOException {
        Plugin craftEngine = Bukkit.getPluginManager().getPlugin("CraftEngine");
        if (craftEngine == null) throw new IllegalStateException("CraftEngine is not loaded");
        Path target = craftEngine.getDataFolder().toPath().resolve("resources").resolve("onelife");
        for (String file : FILES) {
            Path dest = target.resolve(file);
            Files.createDirectories(dest.getParent());
            try (InputStream in = plugin.getResource("craftengine/onelife/" + file)) {
                if (in == null) throw new IOException("missing bundled resource " + file);
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
