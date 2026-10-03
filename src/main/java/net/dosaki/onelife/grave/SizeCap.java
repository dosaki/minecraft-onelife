package net.dosaki.onelife.grave;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Decides which slots spill on the ground when a grave would be too large. */
public final class SizeCap {

    /** One non-empty grave slot and its encoded size. */
    public record Slot(int index, int bytes, boolean shulker) {}

    private static final Comparator<Slot> SPILL_ORDER = Comparator
            .comparing((Slot s) -> !s.shulker())
            .thenComparing(Comparator.comparingInt(Slot::bytes).reversed())
            .thenComparingInt(Slot::index);

    private SizeCap() {}

    /** Indices to remove so the remaining total is at most {@code limit}. */
    public static Set<Integer> spill(List<Slot> slots, int limit) {
        long total = slots.stream().mapToLong(Slot::bytes).sum();
        List<Slot> order = new ArrayList<>(slots);
        order.sort(SPILL_ORDER);
        Set<Integer> spilled = new TreeSet<>();
        for (Slot slot : order) {
            if (total <= limit) break;
            spilled.add(slot.index());
            total -= slot.bytes();
        }
        return spilled;
    }
}
