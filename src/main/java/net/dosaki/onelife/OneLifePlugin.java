package net.dosaki.onelife;

import java.io.IOException;
import net.dosaki.onelife.craft.ContentInstaller;
import net.dosaki.onelife.craft.PackRegenerator;
import net.dosaki.onelife.listen.DeathListener;
import net.dosaki.onelife.listen.GraveFurnitureListener;
import net.dosaki.onelife.listen.HologramRestorer;
import net.dosaki.onelife.mode.LastMessageTracker;
import net.dosaki.onelife.mode.OneLifeCommand;
import net.dosaki.onelife.mode.OneLifePrompt;
import net.dosaki.onelife.view.GraveViewer;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class OneLifePlugin extends JavaPlugin {

    private Settings settings;
    private GraveViewer viewer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = Settings.from(getConfig());
        try {
            ContentInstaller.install(this);
        } catch (IOException | IllegalStateException e) {
            getLogger().severe("Could not install gravestone content into CraftEngine: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        LastMessageTracker lastMessages = new LastMessageTracker(this);
        viewer = new GraveViewer(this);
        register(lastMessages);
        register(viewer);
        register(new PackRegenerator(settings));
        register(new DeathListener(this, settings, lastMessages));
        register(new GraveFurnitureListener(this, settings, viewer));
        register(new HologramRestorer(this));
        register(new OneLifePrompt(this));
        OneLifeCommand.register(this);
    }

    @Override
    public void onDisable() {
        if (viewer != null) viewer.closeEverything();
    }

    public Settings settings() {
        return settings;
    }

    private void register(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }
}
