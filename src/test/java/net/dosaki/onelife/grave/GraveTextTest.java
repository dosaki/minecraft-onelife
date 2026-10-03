package net.dosaki.onelife.grave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class GraveTextTest {

    private static final String CAUSE = "{\"translate\":\"death.attack.drown\",\"with\":[{\"text\":\"Steve\"}]}";

    private static GraveData grave(GraveData.Mode mode, String last) {
        return grave(mode, last, 3);
    }

    private static GraveData grave(GraveData.Mode mode, String last, int deathNumber) {
        return new GraveData(UUID.randomUUID(), UUID.randomUUID(), "Steve", mode, CAUSE, last, 10, false,
                deathNumber, new byte[0]);
    }

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    @Test
    void causeKeepsTranslationKey() {
        Component cause = GraveText.cause(grave(GraveData.Mode.ONE_LIFE, null));
        assertTrue(cause instanceof TranslatableComponent t && t.key().equals("death.attack.drown"));
    }

    @Test
    void badCauseJsonFallsBack() {
        GraveData d = new GraveData(UUID.randomUUID(), UUID.randomUUID(), "Steve", GraveData.Mode.LOOTABLE,
                "{\"translate\":", null, 0, false, 0, new byte[0]);
        assertEquals("Died", plain(GraveText.cause(d)));
    }

    @Test
    void hologramLines() {
        String text = plain(GraveText.hologram(grave(GraveData.Mode.ONE_LIFE, "see you")));
        List<String> lines = List.of(text.split("\n"));
        assertEquals(3, lines.size());
        assertEquals("☠ Steve, the Third", lines.get(0));
        assertEquals("\"see you\"", lines.get(2));
    }

    @Test
    void hologramHasNoModeLine() {
        for (GraveData.Mode mode : GraveData.Mode.values()) {
            String text = plain(GraveText.hologram(grave(mode, null)));
            assertEquals(2, text.split("\n").length);
            assertFalse(text.contains("One Life"));
            assertFalse(text.contains("Lootable"));
        }
    }

    @Test
    void titleWithAndWithoutNumber() {
        assertEquals("Steve, the Third", GraveText.title(grave(GraveData.Mode.ONE_LIFE, null, 3)));
        assertEquals("Steve, the 101st", GraveText.title(grave(GraveData.Mode.ONE_LIFE, null, 101)));
        assertEquals("Steve", GraveText.title(grave(GraveData.Mode.ONE_LIFE, null, 0)));
    }

    @Test
    void windowTitleDependsOnMode() {
        assertEquals("One Life Grave - unable to loot",
                plain(GraveText.windowTitle(grave(GraveData.Mode.ONE_LIFE, null))));
        assertEquals("Grave of Steve, the Third",
                plain(GraveText.windowTitle(grave(GraveData.Mode.LOOTABLE, null))));
    }

    @Test
    void causeWithoutNameStripsOwnerAndCapitalises() {
        assertEquals("Was slain by Zombie", GraveText.causeWithoutName("Steve was slain by Zombie", "Steve"));
        assertEquals("Tried to swim in lava", GraveText.causeWithoutName("  Steve tried to swim in lava ", "Steve"));
    }

    @Test
    void causeWithoutNameLeavesOtherTextAlone() {
        assertEquals("Zombie killed Steve", GraveText.causeWithoutName("Zombie killed Steve", "Steve"));
        assertEquals("Dosaki was slain", GraveText.causeWithoutName("Dosaki was slain", "Dos"));
        assertEquals("Steve", GraveText.causeWithoutName("Steve", "Steve"));
    }

    @Test
    void causeWithoutNameDefaultsToDied() {
        assertEquals("Died", GraveText.causeWithoutName(null, "Steve"));
        assertEquals("Died", GraveText.causeWithoutName("   ", "Steve"));
        assertEquals("Died", GraveText.causeWithoutName("", "Steve"));
    }

    @Test
    void lastMessageIsLiteral() {
        String text = plain(GraveText.hologram(grave(GraveData.Mode.LOOTABLE, "<red>hi</red> §cthere")));
        assertTrue(text.contains("\"<red>hi</red> §cthere\""));
    }

    @Test
    void itemName() {
        assertEquals("Grave of Steve, the Third", plain(GraveText.itemName(grave(GraveData.Mode.LOOTABLE, null))));
        assertEquals("Grave of Steve", plain(GraveText.itemName(grave(GraveData.Mode.LOOTABLE, null, 0))));
    }

    @Test
    void loreIsNotItalicByDefaultAndWraps() {
        String longMessage = "this is a long last message that must wrap onto several tooltip lines";
        List<Component> lore = GraveText.lore(grave(GraveData.Mode.ONE_LIFE, longMessage));
        assertFalse(lore.get(0).decoration(TextDecoration.ITALIC) == TextDecoration.State.TRUE);
        String joined = lore.stream().map(GraveTextTest::plain).collect(Collectors.joining("|"));
        assertTrue(joined.endsWith("One Life"));
        assertTrue(lore.size() >= 5, joined);
    }

    @Test
    void wrap() {
        assertEquals(List.of("aaa bbb", "ccc"), GraveText.wrap("aaa bbb ccc", 7));
        assertEquals(List.of("abcdefgh", "ij"), GraveText.wrap("abcdefghij", 8));
        assertEquals(List.of(""), GraveText.wrap("", 8));
    }
}
