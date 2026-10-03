package net.dosaki.onelife.listen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.dosaki.onelife.Settings;
import net.dosaki.onelife.craft.GraveItems;
import net.dosaki.onelife.craft.GravePlacer;
import net.dosaki.onelife.craft.ItemCodec;
import net.dosaki.onelife.grave.GraveData;
import net.dosaki.onelife.grave.GraveDataException;
import net.dosaki.onelife.grave.SizeCap;
import net.dosaki.onelife.mode.LastMessageTracker;
import net.dosaki.onelife.mode.ModeState;
import net.dosaki.onelife.mode.PlayerModeStore;
import net.dosaki.onelife.view.SlotLayout;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/** Replaces death drops with a grave (plus one grave per carried gravestone). */
public final class DeathListener implements Listener {

    private final Plugin plugin;
    private final Settings settings;
    private final LastMessageTracker lastMessages;

    public DeathListener(Plugin plugin, Settings settings, LastMessageTracker lastMessages) {
        this.plugin = plugin;
        this.settings = settings;
        this.lastMessages = lastMessages;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        ModeState state = PlayerModeStore.get(player);
        PlayerModeStore.set(player, state.afterDeath());

        if (event.getKeepInventory()) return;
        if (event.getDamageSource().getDamageType() == DamageType.OUT_OF_WORLD) return;

        List<ItemStack> carried = new ArrayList<>();
        List<ItemStack> spill = new ArrayList<>();
        List<Block> spaces;
        float yaw;
        try {
            ItemStack[] contents = player.getInventory().getContents();
            ItemStack[] stored = ItemCodec.emptySlots();
            for (int i = 0; i < Math.min(contents.length, SlotLayout.GRAVE_SLOTS); i++) {
                ItemStack item = contents[i];
                if (item == null || item.isEmpty() || item.containsEnchantment(Enchantment.VANISHING_CURSE)) continue;
                if (GraveItems.isGravestone(item)) {
                    (GraveItems.rawData(item).isPresent() ? carried : spill).add(item.clone());
                    continue;
                }
                stored[i] = item.clone();
            }

            List<SizeCap.Slot> sizes = new ArrayList<>();
            for (int i = 0; i < stored.length; i++) {
                if (stored[i].isEmpty()) continue;
                sizes.add(new SizeCap.Slot(i, ItemCodec.size(stored[i]), Tag.SHULKER_BOXES.isTagged(stored[i].getType())));
            }
            Set<Integer> spilled = SizeCap.spill(sizes, settings.sizeCapBytes());
            for (int i : spilled) {
                spill.add(stored[i]);
                stored[i] = ItemStack.empty();
            }

            int xp = player.calculateTotalExperiencePoints();
            GraveData grave = new GraveData(UUID.randomUUID(), player.getUniqueId(), player.getName(),
                    state.graveMode(), causeJson(event, player), lastMessages.get(player), xp, false,
                    ItemCodec.encode(stored));

            spaces = GravePlacer.findSpaces(player.getLocation(), 1 + carried.size(), settings.searchRadius());
            yaw = player.getLocation().getYaw();
            if (spaces.isEmpty() || GravePlacer.place(spaces.get(0), yaw, grave.encode(), grave) == null) {
                plugin.getLogger().severe("Could not place a grave for " + player.getName() + " at "
                        + player.getLocation() + "; leaving vanilla drops.");
                return;
            }
        } catch (RuntimeException e) {
            // Nothing has touched the event's drops or XP yet, so the vanilla death still happens.
            plugin.getLogger().log(Level.WARNING, "Could not build a grave for " + player.getName()
                    + " at " + player.getLocation() + "; leaving vanilla drops.", e);
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);
        event.getDrops().addAll(spill);

        for (int j = 0; j < carried.size(); j++) {
            ItemStack item = carried.get(j);
            try {
                byte[] raw = GraveItems.rawData(item).orElseThrow();
                if (j + 1 >= spaces.size()
                        || GravePlacer.place(spaces.get(j + 1), yaw, raw, decodeOrNull(raw)) == null) {
                    event.getDrops().add(item);
                }
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING,
                        "Could not re-place a carried gravestone; dropping it instead.", e);
                event.getDrops().add(item);
            }
        }
    }

    private static String causeJson(PlayerDeathEvent event, Player player) {
        Component message = event.deathMessage();
        if (message == null) {
            message = Component.translatable("death.attack.generic", Component.text(player.getName()));
        }
        return GsonComponentSerializer.gson().serialize(message);
    }

    private static @Nullable GraveData decodeOrNull(byte[] raw) {
        try {
            return GraveData.decode(raw);
        } catch (GraveDataException e) {
            return null;
        }
    }
}
