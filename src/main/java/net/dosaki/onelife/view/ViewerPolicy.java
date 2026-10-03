package net.dosaki.onelife.view;

import java.util.EnumSet;
import java.util.Set;
import net.dosaki.onelife.grave.GraveData.Mode;
import org.bukkit.event.inventory.InventoryAction;

/** Which clicks and drags a grave window allows. One Life graves allow nothing. */
public final class ViewerPolicy {

    private static final Set<InventoryAction> TAKE_FROM_TOP = EnumSet.of(
            InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_SOME, InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE, InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.DROP_ALL_SLOT, InventoryAction.DROP_ONE_SLOT);

    /** Bottom-inventory actions that reach into the grave. */
    private static final Set<InventoryAction> CROSS_INVENTORY = EnumSet.of(
            InventoryAction.MOVE_TO_OTHER_INVENTORY, InventoryAction.COLLECT_TO_CURSOR);

    private ViewerPolicy() {}

    public static boolean allowClick(Mode mode, boolean readable, InventoryAction action,
                                     boolean clickedTop, int topSlot, boolean hotbarTargetEmpty) {
        if (!readable || mode == Mode.ONE_LIFE) return false;
        if (!clickedTop) return !CROSS_INVENTORY.contains(action);
        if (SlotLayout.graveSlot(topSlot) < 0) return false;
        if (action == InventoryAction.HOTBAR_SWAP) return hotbarTargetEmpty;
        return TAKE_FROM_TOP.contains(action);
    }

    public static boolean allowDrag(Mode mode, boolean readable, Set<Integer> rawSlots) {
        if (!readable || mode == Mode.ONE_LIFE) return false;
        return rawSlots.stream().allMatch(slot -> slot >= SlotLayout.WINDOW_SIZE);
    }
}
