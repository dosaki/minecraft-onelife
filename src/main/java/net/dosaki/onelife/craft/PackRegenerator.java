package net.dosaki.onelife.craft;

import net.dosaki.onelife.Settings;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** CraftEngine only builds and uploads the pack on "/ce reload pack"; run it once after startup. */
public final class PackRegenerator implements Listener {

    private final Settings settings;

    public PackRegenerator(Settings settings) {
        this.settings = settings;
    }

    @EventHandler
    public void onReload(CraftEngineReloadEvent event) {
        if (event.isFirstReload() && settings.regeneratePackOnStart()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ce reload pack");
        }
    }
}
