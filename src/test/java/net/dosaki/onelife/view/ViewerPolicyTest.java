package net.dosaki.onelife.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.dosaki.onelife.grave.GraveData.Mode;
import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

class ViewerPolicyTest {

    @Test
    void oneLifeBlocksEveryActionEverywhere() {
        for (InventoryAction action : InventoryAction.values()) {
            for (boolean top : new boolean[] {true, false}) {
                for (boolean empty : new boolean[] {true, false}) {
                    assertFalse(ViewerPolicy.allowClick(Mode.ONE_LIFE, true, action, top, 0, empty), action + " top=" + top);
                }
            }
        }
        assertFalse(ViewerPolicy.allowDrag(Mode.ONE_LIFE, true, Set.of(50)));
    }

    @Test
    void unreadableBlocksEverything() {
        for (InventoryAction action : InventoryAction.values()) {
            assertFalse(ViewerPolicy.allowClick(Mode.LOOTABLE, false, action, true, 0, true));
            assertFalse(ViewerPolicy.allowClick(Mode.LOOTABLE, false, action, false, 0, true));
        }
        assertFalse(ViewerPolicy.allowDrag(Mode.LOOTABLE, false, Set.of(50)));
    }

    @Test
    void lootableAllowsTakingFromContentSlots() {
        for (InventoryAction a : new InventoryAction[] {
                InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_SOME, InventoryAction.PICKUP_HALF,
                InventoryAction.PICKUP_ONE, InventoryAction.MOVE_TO_OTHER_INVENTORY,
                InventoryAction.DROP_ALL_SLOT, InventoryAction.DROP_ONE_SLOT}) {
            assertTrue(ViewerPolicy.allowClick(Mode.LOOTABLE, true, a, true, 0, false), a.name());
            assertFalse(ViewerPolicy.allowClick(Mode.LOOTABLE, true, a, true, 41, false), a + " on filler");
        }
    }

    @Test
    void lootableBlocksPuttingIn() {
        for (InventoryAction a : new InventoryAction[] {
                InventoryAction.PLACE_ALL, InventoryAction.PLACE_SOME, InventoryAction.PLACE_ONE,
                InventoryAction.SWAP_WITH_CURSOR, InventoryAction.COLLECT_TO_CURSOR, InventoryAction.CLONE_STACK,
                InventoryAction.PLACE_ALL_INTO_BUNDLE, InventoryAction.PICKUP_FROM_BUNDLE, InventoryAction.UNKNOWN}) {
            assertFalse(ViewerPolicy.allowClick(Mode.LOOTABLE, true, a, true, 0, true), a.name());
        }
    }

    @Test
    void lootableHotbarSwapOnlyIntoEmptySlot() {
        assertTrue(ViewerPolicy.allowClick(Mode.LOOTABLE, true, InventoryAction.HOTBAR_SWAP, true, 0, true));
        assertFalse(ViewerPolicy.allowClick(Mode.LOOTABLE, true, InventoryAction.HOTBAR_SWAP, true, 0, false));
    }

    @Test
    void lootableOwnInventoryIsFreeExceptCrossMoves() {
        assertTrue(ViewerPolicy.allowClick(Mode.LOOTABLE, true, InventoryAction.PLACE_ALL, false, 0, false));
        assertTrue(ViewerPolicy.allowClick(Mode.LOOTABLE, true, InventoryAction.PICKUP_ALL, false, 0, false));
        assertFalse(ViewerPolicy.allowClick(Mode.LOOTABLE, true, InventoryAction.MOVE_TO_OTHER_INVENTORY, false, 0, false));
        assertFalse(ViewerPolicy.allowClick(Mode.LOOTABLE, true, InventoryAction.COLLECT_TO_CURSOR, false, 0, false));
    }

    @Test
    void lootableDragsOnlyInOwnInventory() {
        assertTrue(ViewerPolicy.allowDrag(Mode.LOOTABLE, true, Set.of(45, 60)));
        assertFalse(ViewerPolicy.allowDrag(Mode.LOOTABLE, true, Set.of(44, 60)));
    }
}
