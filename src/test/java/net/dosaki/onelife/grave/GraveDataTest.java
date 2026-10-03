package net.dosaki.onelife.grave;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GraveDataTest {

    private static final UUID GRAVE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static GraveData sample(String lastMessage) {
        return new GraveData(GRAVE, OWNER, "Steve", GraveData.Mode.ONE_LIFE,
                "{\"translate\":\"death.attack.generic\",\"with\":[\"Steve\"]}",
                lastMessage, 1234, false, 7, new byte[] {1, 2, 3, 0, 5});
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
        GraveData d = new GraveData(GRAVE, OWNER, "Steve", GraveData.Mode.LOOTABLE, "{}", null, 7, false, 0, new byte[0]);
        assertEquals(3, d.payout(0.5));
        assertEquals(0, d.payout(0.0));
        assertEquals(7, d.payout(1.0));
    }

    @Test
    void itemsAreDefensivelyCopied() {
        byte[] items = {1, 2, 3};
        GraveData d = new GraveData(GRAVE, OWNER, "Steve", GraveData.Mode.LOOTABLE, "{}", null, 0, false, 0, items);
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
                new GraveData(GRAVE, OWNER, "Steve", GraveData.Mode.LOOTABLE, "{}", null, -1, false, 0, new byte[0]));
    }

    @Test
    void deathNumberRoundTrips() {
        assertEquals(7, GraveData.decode(sample("x").encode()).deathNumber());
    }

    @Test
    void rejectsNegativeDeathNumber() {
        assertThrows(IllegalArgumentException.class, () ->
                new GraveData(GRAVE, OWNER, "Steve", GraveData.Mode.LOOTABLE, "{}", null, 0, false, -1, new byte[0]));
    }

    @Test
    void decodesFormatOneWithUnknownDeathNumber() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(1);
            out.writeLong(GRAVE.getMostSignificantBits());
            out.writeLong(GRAVE.getLeastSignificantBits());
            out.writeLong(OWNER.getMostSignificantBits());
            out.writeLong(OWNER.getLeastSignificantBits());
            out.writeUTF("Steve");
            out.writeByte(GraveData.Mode.LOOTABLE.ordinal());
            out.writeUTF("{}");
            out.writeBoolean(true);
            out.writeUTF("hi");
            out.writeInt(12);
            out.writeBoolean(true);
            out.writeInt(2);
            out.write(new byte[] {4, 5});
        }
        GraveData d = GraveData.decode(bytes.toByteArray());
        assertEquals(0, d.deathNumber());
        assertEquals("Steve", d.ownerName());
        assertEquals("hi", d.lastMessage());
        assertEquals(12, d.xp());
        assertTrue(d.mined());
        assertArrayEquals(new byte[] {4, 5}, d.items());
    }

    @Test
    void decodeRejectsFormatThree() {
        byte[] three = sample("x").encode();
        three[0] = 3;
        assertThrows(GraveDataException.class, () -> GraveData.decode(three));
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
