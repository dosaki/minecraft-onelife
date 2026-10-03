package net.dosaki.onelife.craft;

import io.papermc.paper.datacomponent.DataComponentTypes;
import java.util.Optional;
import net.dosaki.onelife.Keys;
import net.dosaki.onelife.grave.GraveData;
import net.dosaki.onelife.grave.GraveText;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

/** Gravestone items: the CraftEngine item plus our grave bytes in persistent data. */
public final class GraveItems {

    public static final Key FURNITURE = Key.of("onelife:gravestone");

    private GraveItems() {}

    public static boolean isGravestone(ItemStack item) {
        return item != null && !item.isEmpty()
                && CraftEngineItems.isCustomItem(item)
                && FURNITURE.equals(CraftEngineItems.getCustomItemId(item));
    }

    public static Optional<byte[]> rawData(ItemStack item) {
        if (item == null || item.isEmpty()) return Optional.empty();
        return Optional.ofNullable(item.getPersistentDataContainer().get(Keys.GRAVE, PersistentDataType.BYTE_ARRAY));
    }

    /** A gravestone carrying {@code raw}. With {@code data}, it also gets the owner's name and tooltip. */
    public static ItemStack build(byte[] raw, @Nullable GraveData data) {
        ItemStack item = blank();
        item.editPersistentDataContainer(pdc -> pdc.set(Keys.GRAVE, PersistentDataType.BYTE_ARRAY, raw));
        if (data != null) {
            item.editMeta(meta -> {
                meta.itemName(GraveText.itemName(data));
                meta.lore(GraveText.lore(data));
            });
        }
        return item;
    }

    public static ItemStack blank() {
        ItemStack item = CraftEngineItems.byId(FURNITURE).buildBukkitItem();
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        return item;
    }
}
