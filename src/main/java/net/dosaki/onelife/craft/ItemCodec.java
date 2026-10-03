package net.dosaki.onelife.craft;

import java.util.Arrays;
import java.util.Optional;
import net.dosaki.onelife.view.SlotLayout;
import org.bukkit.inventory.ItemStack;

/** The 41 grave slots as bytes. Paper's format records the data version, so items upgrade with the server. */
public final class ItemCodec {

    private ItemCodec() {}

    public static byte[] encode(ItemStack[] slots) {
        if (slots.length != SlotLayout.GRAVE_SLOTS) {
            throw new IllegalArgumentException("expected " + SlotLayout.GRAVE_SLOTS + " slots, got " + slots.length);
        }
        return ItemStack.serializeItemsAsBytes(slots);
    }

    /** Empty when the bytes can't be read (corrupt, or from an unknown future format). */
    public static Optional<ItemStack[]> decode(byte[] bytes) {
        try {
            ItemStack[] items = ItemStack.deserializeItemsFromBytes(bytes);
            if (items.length != SlotLayout.GRAVE_SLOTS) return Optional.empty();
            return Optional.of(items);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Encoded size of one item, 0 for empty. */
    public static int size(ItemStack item) {
        if (item == null || item.isEmpty()) return 0;
        return item.serializeAsBytes().length;
    }

    public static ItemStack[] emptySlots() {
        ItemStack[] slots = new ItemStack[SlotLayout.GRAVE_SLOTS];
        Arrays.fill(slots, ItemStack.empty());
        return slots;
    }
}
