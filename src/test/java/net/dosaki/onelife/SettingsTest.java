package net.dosaki.onelife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SettingsTest {

    @Test
    void defaultsWhenEmpty() {
        Settings s = Settings.from(new YamlConfiguration());
        assertEquals(524288, s.sizeCapBytes());
        assertEquals(3, s.searchRadius());
        assertEquals(0.5, s.xpFraction());
        assertTrue(s.regeneratePackOnStart());
    }

    @Test
    void readsValues() {
        YamlConfiguration c = new YamlConfiguration();
        c.set("size-cap-bytes", 1000);
        c.set("search-radius", 5);
        c.set("xp-fraction", 0.25);
        c.set("regenerate-pack-on-start", false);
        Settings s = Settings.from(c);
        assertEquals(new Settings(1000, 5, 0.25, false), s);
    }

    @Test
    void rejectsOutOfRange() {
        YamlConfiguration c = new YamlConfiguration();
        c.set("xp-fraction", 1.5);
        assertThrows(IllegalArgumentException.class, () -> Settings.from(c));
        c.set("xp-fraction", 0.5);
        c.set("search-radius", -1);
        assertThrows(IllegalArgumentException.class, () -> Settings.from(c));
        c.set("search-radius", 3);
        c.set("size-cap-bytes", 0);
        assertThrows(IllegalArgumentException.class, () -> Settings.from(c));
    }
}
