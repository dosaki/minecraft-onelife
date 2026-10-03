package net.dosaki.onelife.view;

/** Maps grave slots (player-inventory order) to the 5-row grave window and back. */
public final class SlotLayout {
    public static final int WINDOW_SIZE = 45;
    public static final int GRAVE_SLOTS = 41;

    private SlotLayout() {}

    /** Grave slot shown in a window slot, or -1 for a filler slot. */
    public static int graveSlot(int windowSlot) {
        if (windowSlot >= 0 && windowSlot < 27) return windowSlot + 9;     // main inventory
        if (windowSlot >= 27 && windowSlot < 36) return windowSlot - 27;   // hotbar
        if (windowSlot >= 36 && windowSlot < 40) return 75 - windowSlot;   // helmet..boots -> 39..36
        if (windowSlot == 40) return 40;                                   // offhand
        return -1;
    }

    public static int windowSlot(int graveSlot) {
        if (graveSlot >= 9 && graveSlot < 36) return graveSlot - 9;
        if (graveSlot >= 0 && graveSlot < 9) return graveSlot + 27;
        if (graveSlot >= 36 && graveSlot < 40) return 75 - graveSlot;
        if (graveSlot == 40) return 40;
        throw new IllegalArgumentException("not a grave slot: " + graveSlot);
    }
}
