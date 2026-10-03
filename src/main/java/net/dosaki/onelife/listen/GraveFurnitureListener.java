package net.dosaki.onelife.listen;

import java.util.Optional;
import net.dosaki.onelife.Settings;
import net.dosaki.onelife.craft.GraveHolograms;
import net.dosaki.onelife.craft.GraveItems;
import net.dosaki.onelife.craft.GraveStore;
import net.dosaki.onelife.grave.GraveData;
import net.dosaki.onelife.grave.GraveDataException;
import net.dosaki.onelife.view.GraveViewer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.api.event.FurnitureAttemptPlaceEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureInteractEvent;
import net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/** Placing, opening and breaking gravestone furniture. */
public final class GraveFurnitureListener implements Listener {

    private final Plugin plugin;
    private final Settings settings;
    private final GraveViewer viewer;

    public GraveFurnitureListener(Plugin plugin, Settings settings, GraveViewer viewer) {
        this.plugin = plugin;
        this.settings = settings;
        this.viewer = viewer;
    }

    /** A gravestone item without grave data (e.g. from /ce give) can't be placed. */
    @EventHandler(ignoreCancelled = true)
    public void onAttemptPlace(FurnitureAttemptPlaceEvent event) {
        if (!GraveItems.FURNITURE.equals(event.furniture().id())) return;
        Player player = event.getPlayer();
        ItemStack hand = event.hand() == InteractionHand.MAIN_HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
        if (GraveItems.rawData(hand).isEmpty()) {
            event.setCancelled(true);
            player.sendMessage(Component.text("This gravestone has no one to remember.", NamedTextColor.GRAY));
        }
    }

    /** Moves the grave data from the placing item onto the furniture and shows the text. */
    @EventHandler(ignoreCancelled = true)
    public void onPlace(FurniturePlaceEvent event) {
        BukkitFurniture furniture = event.furniture();
        if (!GraveItems.FURNITURE.equals(furniture.id())) return;
        var source = furniture.sourceItem();
        if (source == null) return;
        ItemStack item = ((ItemStack) source.platformItem()).clone();
        GraveItems.rawData(item).ifPresent(raw -> {
            GraveStore.writeRaw(furniture.bukkitEntity(), raw);
            try {
                GraveHolograms.spawn(furniture.location(), GraveData.decode(raw));
            } catch (GraveDataException e) {
                plugin.getLogger().warning("Placed a gravestone with unreadable data at " + furniture.location());
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(FurnitureInteractEvent event) {
        if (event.hand() != InteractionHand.MAIN_HAND) return;
        BukkitFurniture furniture = event.furniture();
        if (!GraveItems.FURNITURE.equals(furniture.id())) return;
        viewer.open(event.getPlayer(), furniture.bukkitEntity());
    }

    /** Pays the first-break XP once, then drops our own item with the up-to-date data. */
    @EventHandler(ignoreCancelled = true)
    public void onBreak(FurnitureBreakEvent event) {
        BukkitFurniture furniture = event.furniture();
        if (!GraveItems.FURNITURE.equals(furniture.id())) return;
        event.setDropItems(false);
        Entity meta = furniture.bukkitEntity();
        Location drop = furniture.getDropLocation();

        Optional<byte[]> raw = GraveStore.raw(meta);
        if (raw.isEmpty()) {
            drop.getWorld().dropItemNaturally(drop, GraveItems.blank());
            return;
        }
        Optional<GraveData> before = GraveStore.read(meta, plugin.getLogger());
        if (before.isEmpty()) {
            drop.getWorld().dropItemNaturally(drop, GraveItems.build(raw.get(), null));
            return;
        }
        viewer.closeAll(before.get().graveId());
        GraveData data = GraveStore.read(meta, plugin.getLogger()).orElse(before.get());
        if (!data.mined()) {
            int payout = data.payout(settings.xpFraction());
            data = data.withMined();
            GraveStore.write(meta, data);
            event.getPlayer().giveExp(payout);
        }
        GraveHolograms.remove(furniture.location(), data.graveId());
        drop.getWorld().dropItemNaturally(drop, GraveItems.build(data.encode(), data));
    }
}
