package net.dosaki.onelife.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.dosaki.onelife.craft.GraveStore;
import net.dosaki.onelife.craft.ItemCodec;
import net.dosaki.onelife.grave.GraveData;
import net.dosaki.onelife.grave.GraveText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/** The grave window. One shared inventory per open grave, like a chest; lootable changes are written back. */
public final class GraveViewer implements Listener {

    private final Plugin plugin;
    private final Map<UUID, Holder> open = new HashMap<>();

    public GraveViewer(Plugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Entity meta) {
        Optional<GraveData> data = GraveStore.read(meta, plugin.getLogger());
        if (data.isEmpty()) {
            viewer.sendMessage(Component.text("This gravestone is too worn to read.", NamedTextColor.GRAY));
            return;
        }
        UUID graveId = data.get().graveId();
        Holder existing = open.get(graveId);
        if (existing != null && (existing.getInventory().getViewers().isEmpty() || !existing.entityResolves())) {
            retire(existing, "stale grave window replaced");
            existing = null;
        }
        Holder holder = existing != null ? existing : new Holder(meta, data.get());
        open.put(graveId, holder);
        if (viewer.openInventory(holder.getInventory()) == null && holder.getInventory().getViewers().isEmpty()) {
            holder.retired = true;
            open.remove(graveId, holder);
        }
    }

    /** Retires a holder without flushing and closes its viewers. Used when its grave can no longer be resolved. */
    private void retire(Holder holder, String reason) {
        holder.retired = true;
        open.remove(holder.data.graveId(), holder);
        for (HumanEntity v : new ArrayList<>(holder.getInventory().getViewers())) v.closeInventory();
        if (reason != null) plugin.getLogger().warning("Grave " + holder.data.graveId() + ": " + reason);
    }

    public void closeAll(UUID graveId) {
        Holder holder = open.remove(graveId);
        if (holder == null) return;
        if (!holder.flush()) plugin.getLogger().warning("Grave " + graveId + ": entity unresolved on closeAll, changes not saved");
        holder.retired = true;
        for (HumanEntity viewer : new ArrayList<>(holder.getInventory().getViewers())) viewer.closeInventory();
    }

    /** Flushes and closes every open grave window (plugin shutdown). */
    public void closeEverything() {
        for (UUID graveId : new ArrayList<>(open.keySet())) closeAll(graveId);
    }

    // Fail closed: cancel first, un-cancel only when the policy allows. An exception leaves the event cancelled.
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        boolean wasCancelled = event.isCancelled();
        event.setCancelled(true);
        try {
            int rawSlot = event.getRawSlot();
            boolean clickedTop = rawSlot >= 0 && rawSlot < SlotLayout.WINDOW_SIZE;
            boolean allowed = ViewerPolicy.allowClick(holder.data.mode(), holder.readable, event.getAction(),
                    clickedTop, rawSlot, hotbarTargetEmpty(event));
            if (!allowed || wasCancelled) return;
            if (!holder.entityResolves()) {
                retire(holder, "grave entity is gone or unloaded, click refused");
                return;
            }
            event.setCancelled(false);
            if (clickedTop) Bukkit.getScheduler().runTask(plugin, () -> flush(holder));
        } catch (RuntimeException e) {
            event.setCancelled(true);
            plugin.getLogger().log(Level.WARNING, "Grave click rejected after an error", e);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        boolean wasCancelled = event.isCancelled();
        event.setCancelled(true);
        try {
            if (!wasCancelled && ViewerPolicy.allowDrag(holder.data.mode(), holder.readable, event.getRawSlots())) {
                if (!holder.entityResolves()) {
                    retire(holder, "grave entity is gone or unloaded, drag refused");
                    return;
                }
                event.setCancelled(false);
            }
        } catch (RuntimeException e) {
            event.setCancelled(true);
            plugin.getLogger().log(Level.WARNING, "Grave drag rejected after an error", e);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) return;
        flush(holder);
        if (event.getInventory().getViewers().size() <= 1) {
            holder.retired = true;
            open.remove(holder.data.graveId(), holder);
        }
    }

    /** Flushes a holder; if its grave can't be resolved, fails closed by retiring it and closing its viewers. */
    private void flush(Holder holder) {
        if (holder.retired) return;
        if (!holder.flush()) retire(holder, "grave entity is gone or unloaded, window closed without saving");
    }

    private static boolean hotbarTargetEmpty(InventoryClickEvent event) {
        ItemStack target;
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            target = event.getWhoClicked().getInventory().getItemInOffHand();
        } else if (event.getHotbarButton() >= 0) {
            target = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
        } else {
            return true;
        }
        return target == null || target.isEmpty();
    }

    private static final class Holder implements InventoryHolder {
        private final UUID metaId;
        volatile boolean retired;
        private final Inventory inventory;
        private final boolean readable;
        private GraveData data;

        Holder(Entity meta, GraveData data) {
            this.metaId = meta.getUniqueId();
            this.data = data;
            Optional<ItemStack[]> items = ItemCodec.decode(data.items());
            this.readable = items.isPresent();
            Component title = readable ? GraveText.windowTitle(data) : Component.text(data.ownerName() + "'s Grave");
            this.inventory = Bukkit.createInventory(this, SlotLayout.WINDOW_SIZE, title);
            if (readable) {
                ItemStack[] slots = items.get();
                for (int g = 0; g < SlotLayout.GRAVE_SLOTS; g++) inventory.setItem(SlotLayout.windowSlot(g), slots[g]);
                ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
                for (int w = 0; w < SlotLayout.WINDOW_SIZE; w++) {
                    if (SlotLayout.graveSlot(w) < 0) inventory.setItem(w, filler);
                }
            } else {
                inventory.setItem(22, named(Material.BARRIER, Component.text("Contents unreadable", NamedTextColor.RED)));
            }
        }

        /** Writes a lootable grave's window back to the furniture. One Life and unreadable graves never change. */
        boolean entityResolves() {
            Entity e = Bukkit.getEntity(metaId);
            return e != null && e.isValid();
        }

        /** Returns false only when a write was needed but the entity could not be resolved. */
        boolean flush() {
            if (retired || !readable || data.mode() == GraveData.Mode.ONE_LIFE) return true;
            Entity meta = Bukkit.getEntity(metaId);
            if (meta == null || !meta.isValid()) return false;
            ItemStack[] slots = ItemCodec.emptySlots();
            for (int g = 0; g < SlotLayout.GRAVE_SLOTS; g++) {
                ItemStack item = inventory.getItem(SlotLayout.windowSlot(g));
                if (item != null) slots[g] = item;
            }
            data = data.withItems(ItemCodec.encode(slots));
            GraveStore.write(meta, data);
            return true;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private static ItemStack named(Material material, Component name) {
            ItemStack item = ItemStack.of(material);
            item.editMeta(m -> m.itemName(name));
            return item;
        }
    }
}
