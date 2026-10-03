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
        Holder holder = open.computeIfAbsent(data.get().graveId(), id -> new Holder(meta, data.get()));
        viewer.openInventory(holder.getInventory());
    }

    public void closeAll(UUID graveId) {
        Holder holder = open.remove(graveId);
        if (holder == null) return;
        holder.flush();
        for (HumanEntity viewer : new ArrayList<>(holder.getInventory().getViewers())) viewer.closeInventory();
    }

    // Fail closed: cancel first, un-cancel only when the policy allows. An exception leaves the event cancelled.
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);
        try {
            int rawSlot = event.getRawSlot();
            boolean clickedTop = rawSlot >= 0 && rawSlot < SlotLayout.WINDOW_SIZE;
            boolean allowed = ViewerPolicy.allowClick(holder.data.mode(), holder.readable, event.getAction(),
                    clickedTop, rawSlot, hotbarTargetEmpty(event));
            if (!allowed) return;
            event.setCancelled(false);
            if (clickedTop) Bukkit.getScheduler().runTask(plugin, holder::flush);
        } catch (RuntimeException e) {
            event.setCancelled(true);
            plugin.getLogger().log(Level.WARNING, "Grave click rejected after an error", e);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);
        try {
            if (ViewerPolicy.allowDrag(holder.data.mode(), holder.readable, event.getRawSlots())) {
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
        holder.flush();
        if (event.getInventory().getViewers().size() <= 1) open.remove(holder.data.graveId(), holder);
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
        private final Entity meta;
        private final Inventory inventory;
        private final boolean readable;
        private GraveData data;

        Holder(Entity meta, GraveData data) {
            this.meta = meta;
            this.data = data;
            Component title = Component.text(data.ownerName() + "'s Grave");
            this.inventory = Bukkit.createInventory(this, SlotLayout.WINDOW_SIZE, title);
            Optional<ItemStack[]> items = ItemCodec.decode(data.items());
            this.readable = items.isPresent();
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
        void flush() {
            if (!readable || data.mode() == GraveData.Mode.ONE_LIFE || !meta.isValid()) return;
            ItemStack[] slots = ItemCodec.emptySlots();
            for (int g = 0; g < SlotLayout.GRAVE_SLOTS; g++) {
                ItemStack item = inventory.getItem(SlotLayout.windowSlot(g));
                if (item != null) slots[g] = item;
            }
            data = data.withItems(ItemCodec.encode(slots));
            GraveStore.write(meta, data);
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
