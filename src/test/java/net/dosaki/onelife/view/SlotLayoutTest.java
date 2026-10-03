package net.dosaki.onelife.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SlotLayoutTest {

    @Test
    void mapping() {
        assertEquals(9, SlotLayout.graveSlot(0));
        assertEquals(35, SlotLayout.graveSlot(26));
        assertEquals(0, SlotLayout.graveSlot(27));
        assertEquals(8, SlotLayout.graveSlot(35));
        assertEquals(39, SlotLayout.graveSlot(36)); // helmet
        assertEquals(36, SlotLayout.graveSlot(39)); // boots
        assertEquals(40, SlotLayout.graveSlot(40)); // offhand
        for (int filler = 41; filler < 45; filler++) assertEquals(-1, SlotLayout.graveSlot(filler));
    }

    @Test
    void everyGraveSlotHasExactlyOneWindowSlot() {
        Set<Integer> seen = new HashSet<>();
        for (int g = 0; g < SlotLayout.GRAVE_SLOTS; g++) {
            int w = SlotLayout.windowSlot(g);
            assertEquals(g, SlotLayout.graveSlot(w));
            seen.add(w);
        }
        assertEquals(SlotLayout.GRAVE_SLOTS, seen.size());
    }
}
