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
import org.bukkit.event.EventPriority;
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
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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

    /** We drop our own item (with the up-to-date data) instead of CraftEngine's. */
    @EventHandler(ignoreCancelled = true)
    public void onBreakSuppressDrops(FurnitureBreakEvent event) {
        if (!GraveItems.FURNITURE.equals(event.furniture().id())) return;
        event.setDropItems(false);
    }

    /**
     * Pays the first-break XP once, then drops our item. Runs at MONITOR so a later cancel by
     * CraftEngine or another plugin can't leave both the furniture and a dropped copy.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(FurnitureBreakEvent event) {
        BukkitFurniture furniture = event.furniture();
        if (!GraveItems.FURNITURE.equals(furniture.id())) return;
        event.setDropItems(false); // a higher-priority plugin may have re-enabled CraftEngine's own drop
        Entity meta = furniture.bukkitEntity();

        ItemStack item;
        GraveData mined = null;
        int payout = 0;
        try {
            Optional<byte[]> raw = GraveStore.raw(meta);
            if (raw.isEmpty()) {
                item = GraveItems.blank();
            } else {
                Optional<GraveData> before = GraveStore.read(meta, plugin.getLogger());
                if (before.isEmpty()) {
                    item = GraveItems.build(raw.get(), null);
                } else {
                    viewer.closeAll(before.get().graveId());
                    GraveData data = GraveStore.read(meta, plugin.getLogger()).orElse(before.get());
                    if (!data.mined()) {
                        payout = data.payout(settings.xpFraction());
                        data = data.withMined();
                        GraveStore.write(meta, data);
                    }
                    mined = data;
                    item = GraveItems.build(data.encode(), data);
                }
            }
        } catch (RuntimeException e) {
            // MONITOR: cancelling is still honoured by CraftEngine, which then keeps the furniture (and the grave).
            event.setCancelled(true);
            plugin.getLogger().warning("Could not break gravestone at " + furniture.location() + ": " + e);
            return;
        }

        if (payout > 0) event.getPlayer().giveExp(payout);
        Location drop = furniture.getDropLocation();
        if (furniture.location().getBlock().isLiquid()) {
            // Dropped items burn in lava or drift away; hand the grave straight to the breaker.
            Player player = event.getPlayer();
            for (ItemStack left : player.getInventory().addItem(item).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        } else {
            drop.getWorld().dropItemNaturally(drop, item);
        }
        if (mined != null) GraveHolograms.remove(furniture.location(), mined.graveId());
    }
}
