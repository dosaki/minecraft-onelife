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
        return new GraveData(UUID.randomUUID(), UUID.randomUUID(), "Steve", mode, CAUSE, last, 10, false, new byte[0]);
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
                "{\"translate\":", null, 0, false, new byte[0]);
        assertEquals("Died", plain(GraveText.cause(d)));
    }

    @Test
    void hologramLines() {
        String text = plain(GraveText.hologram(grave(GraveData.Mode.ONE_LIFE, "see you")));
        List<String> lines = List.of(text.split("\n"));
        assertEquals("☠ Steve", lines.get(0));
        assertEquals("\"see you\"", lines.get(2));
        assertEquals("One Life", lines.get(3));
    }

    @Test
    void hologramOmitsMissingLastMessage() {
        String text = plain(GraveText.hologram(grave(GraveData.Mode.LOOTABLE, null)));
        assertEquals(3, text.split("\n").length);
        assertTrue(text.endsWith("Lootable"));
    }

    @Test
    void lastMessageIsLiteral() {
        String text = plain(GraveText.hologram(grave(GraveData.Mode.LOOTABLE, "<red>hi</red> §cthere")));
        assertTrue(text.contains("\"<red>hi</red> §cthere\""));
    }

    @Test
    void itemName() {
        assertEquals("Steve's Gravestone", plain(GraveText.itemName(grave(GraveData.Mode.LOOTABLE, null))));
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
