package net.dosaki.onelife.listen;

import java.util.List;
import net.dosaki.onelife.craft.GraveHolograms;
import net.dosaki.onelife.craft.GraveStore;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.Plugin;

/** Puts back a grave's floating text if it went missing. */
public final class HologramRestorer implements Listener {

    private final Plugin plugin;

    public HologramRestorer(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        List<Entity> entities = event.getEntities();
        for (Entity entity : entities) {
            if (!CraftEngineFurniture.isFurniture(entity)) continue;
            GraveStore.read(entity, plugin.getLogger()).ifPresent(data -> {
                boolean hasText = entities.stream().anyMatch(e -> GraveHolograms.isHologramFor(e, data.graveId()));
                if (!hasText) GraveHolograms.spawn(entity.getLocation(), data);
            });
        }
    }
}
