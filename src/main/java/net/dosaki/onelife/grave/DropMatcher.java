package net.dosaki.onelife.grave;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Matches inventory slots against a pool of death drops, so the grave only stores what is really still dropping.
 * Pure logic on items and amounts; the caller supplies the similarity test.
 */
public final class DropMatcher {

    private DropMatcher() {}

    /**
     * Walks the slots in order and takes up to each slot's amount from the matching drops.
     *
     * @param slots       slot items in order; {@code null} entries and amounts of 0 are empty slots
     * @param slotAmounts amount held by each slot, parallel to {@code slots}
     * @param drops       the drop stacks
     * @param dropAmounts amount of each drop, parallel to {@code drops}; reduced in place to what is left over
     * @param similar     whether a slot item and a drop stack are the same kind of item
     * @return the amount taken for each slot (0 when the slot is empty or nothing matched)
     */
    public static <T> int[] take(List<? extends T> slots, int[] slotAmounts, List<? extends T> drops,
            int[] dropAmounts, BiPredicate<? super T, ? super T> similar) {
        if (slots.size() != slotAmounts.length || drops.size() != dropAmounts.length) {
            throw new IllegalArgumentException("items and amounts must have the same length");
        }
        int[] taken = new int[slots.size()];
        for (int i = 0; i < taken.length; i++) {
            T slot = slots.get(i);
            int want = slotAmounts[i];
            if (slot == null || want <= 0) continue;
            for (int d = 0; d < dropAmounts.length && want > 0; d++) {
                if (dropAmounts[d] <= 0 || !similar.test(slot, drops.get(d))) continue;
                int n = Math.min(want, dropAmounts[d]);
                dropAmounts[d] -= n;
                want -= n;
                taken[i] += n;
            }
        }
        return taken;
    }
}
