package net.dosaki.onelife.grave;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SizeCapTest {

    @Test
    void nothingSpillsUnderTheLimit() {
        assertEquals(Set.of(), SizeCap.spill(List.of(new SizeCap.Slot(0, 10, false), new SizeCap.Slot(1, 20, true)), 30));
    }

    @Test
    void emptyInventory() {
        assertEquals(Set.of(), SizeCap.spill(List.of(), 1));
    }

    @Test
    void largestShulkerSpillsFirst() {
        List<SizeCap.Slot> slots = List.of(
                new SizeCap.Slot(0, 50, false),
                new SizeCap.Slot(1, 100, true),
                new SizeCap.Slot(2, 300, true),
                new SizeCap.Slot(3, 400, false));
        // total 850, limit 600 -> removing slot 2 (300, shulker) is enough
        assertEquals(Set.of(2), SizeCap.spill(slots, 600));
    }

    @Test
    void shulkersBeforeLargerOrdinaryItems() {
        List<SizeCap.Slot> slots = List.of(
                new SizeCap.Slot(0, 500, false),
                new SizeCap.Slot(1, 100, true),
                new SizeCap.Slot(2, 100, true));
        // total 700, limit 550 -> both shulkers (200) go before the 500-byte ordinary item
        assertEquals(Set.of(1, 2), SizeCap.spill(slots, 550));
    }

    @Test
    void ordinaryItemsSpillWhenShulkersAreNotEnough() {
        List<SizeCap.Slot> slots = List.of(
                new SizeCap.Slot(0, 500, false),
                new SizeCap.Slot(1, 200, false),
                new SizeCap.Slot(2, 100, true));
        // total 800, limit 250 -> shulker (100) then largest ordinary (500)
        assertEquals(Set.of(0, 2), SizeCap.spill(slots, 250));
    }

    @Test
    void tiesSpillLowerIndexFirst() {
        List<SizeCap.Slot> slots = List.of(new SizeCap.Slot(5, 100, true), new SizeCap.Slot(3, 100, true));
        assertEquals(Set.of(3), SizeCap.spill(slots, 100));
    }
}
