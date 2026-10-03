package net.dosaki.onelife.grave;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DropMatcherTest {

    private static int[] take(List<String> slots, int[] slotAmounts, List<String> drops, int[] dropAmounts) {
        return DropMatcher.take(slots, slotAmounts, drops, dropAmounts, String::equals);
    }

    @Test
    void everythingMatched() {
        int[] drops = {3, 1};
        int[] taken = take(Arrays.asList("dirt", null, "sword"), new int[] {3, 0, 1}, List.of("dirt", "sword"), drops);
        assertArrayEquals(new int[] {3, 0, 1}, taken);
        assertArrayEquals(new int[] {0, 0}, drops);
    }

    @Test
    void itemMissingFromDropsIsNotStored() {
        int[] drops = {1};
        int[] taken = take(List.of("dirt", "sword"), new int[] {3, 1}, List.of("dirt"), drops);
        assertArrayEquals(new int[] {1, 0}, taken);
        assertArrayEquals(new int[] {0}, drops);
        taken = take(List.of("dirt", "sword"), new int[] {3, 1}, List.of(), new int[0]);
        assertArrayEquals(new int[] {0, 0}, taken);
    }

    @Test
    void extraDropStaysInLeftovers() {
        int[] drops = {3, 5};
        int[] taken = take(List.of("dirt"), new int[] {3}, List.of("dirt", "diamond"), drops);
        assertArrayEquals(new int[] {3}, taken);
        assertArrayEquals(new int[] {0, 5}, drops);
    }

    @Test
    void partialStackStoresOnlyWhatIsInDrops() {
        int[] drops = {2};
        int[] taken = take(List.of("dirt"), new int[] {10}, List.of("dirt"), drops);
        assertArrayEquals(new int[] {2}, taken);
        assertArrayEquals(new int[] {0}, drops);
    }

    @Test
    void twoSlotsShareOneDropPool() {
        int[] drops = {10};
        int[] taken = take(List.of("dirt", "dirt"), new int[] {6, 6}, List.of("dirt"), drops);
        assertArrayEquals(new int[] {6, 4}, taken);
        assertArrayEquals(new int[] {0}, drops);
    }

    @Test
    void oneSlotSpansSeveralDrops() {
        int[] drops = {2, 3, 4};
        int[] taken = take(List.of("dirt"), new int[] {6}, List.of("dirt", "dirt", "dirt"), drops);
        assertArrayEquals(new int[] {6}, taken);
        assertArrayEquals(new int[] {0, 0, 3}, drops);
    }
}
