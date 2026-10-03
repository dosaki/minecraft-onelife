package net.dosaki.onelife.grave;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GraveDataTest {

    private static final UUID GRAVE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static GraveData sample(String lastMessage) {
        return new GraveData(GRAVE, OWNER, "Steve", GraveData.Mode.ONE_LIFE,
                "{\"translate\":\"death.attack.generic\",\"with\":[\"Steve\"]}",
                lastMessage, 1234, false, new byte[] {1, 2, 3, 0, 5});
    }

    @Test
    void roundTrip() {
        GraveData original = sample("brb, going to the nether");
        assertEquals(original, GraveData.decode(original.encode()));
    }

    @Test
    void roundTripWithoutLastMessage() {
        GraveData decoded = GraveData.decode(sample(null).encode());
        assertNull(decoded.lastMessage());
        assertEquals(sample(null), decoded);
    }

    @Test
    void roundTripLargeItems() {
        byte[] big = new byte[300_000];
        Arrays.fill(big, (byte) 7);
        GraveData d = sample("x").withItems(big);
        assertArrayEquals(big, GraveData.decode(d.encode()).items());
    }

    @Test
    void minedSurvivesEncoding() {
        GraveData mined = sample("x").withMined();
        assertTrue(mined.mined());
        assertTrue(GraveData.decode(mined.encode()).mined());
        assertFalse(sample("x").mined());
    }

    @Test
    void payoutRoundsDown() {
        GraveData d = new GraveData(GRAVE, OWNER, "Steve", GraveData.Mode.LOOTABLE, "{}", null, 7, false, new byte[0]);
        assertEquals(3, d.payout(0.5));
        assertEquals(0, d.payout(0.0));
        assertEquals(7, d.payout(1.0));
    }

    @Test
    void itemsAreDefensivelyCopied() {
        byte[] items = {1, 2, 3};
        GraveData d = new GraveData(GRAVE, OWNER, "Steve", GraveData.Mode.LOOTABLE, "{}", null, 0, false, items);
        items[0] = 9;
        assertEquals(1, d.items()[0]);
        d.items()[1] = 9;
        assertEquals(2, d.items()[1]);
    }

    @Test
    void equalityUsesItemContents() {
        assertEquals(sample("a"), sample("a"));
        assertEquals(sample("a").hashCode(), sample("a").hashCode());
        assertNotEquals(sample("a"), sample("a").withItems(new byte[] {9}));
    }

    @Test
    void rejectsNegativeXp() {
        assertThrows(IllegalArgumentException.class, () ->
                new GraveData(GRAVE, OWNER, "Steve", GraveData.Mode.LOOTABLE, "{}", null, -1, false, new byte[0]));
    }

    @Test
    void decodeRejectsGarbage() {
        byte[] good = sample("x").encode();
        assertThrows(GraveDataException.class, () -> GraveData.decode(new byte[0]));
        byte[] wrongFormat = good.clone();
        wrongFormat[0] = 99;
        assertThrows(GraveDataException.class, () -> GraveData.decode(wrongFormat));
        assertThrows(GraveDataException.class, () -> GraveData.decode(Arrays.copyOf(good, good.length - 2)));
        assertThrows(GraveDataException.class, () -> GraveData.decode(Arrays.copyOf(good, good.length + 1)));
    }
}
