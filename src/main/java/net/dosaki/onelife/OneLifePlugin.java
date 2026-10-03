package net.dosaki.onelife;

import org.bukkit.plugin.java.JavaPlugin;

public final class OneLifePlugin extends JavaPlugin {

    private Settings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = Settings.from(getConfig());
    }

    public Settings settings() {
        return settings;
    }
}
