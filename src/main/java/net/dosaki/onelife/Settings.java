package net.dosaki.onelife;

import org.bukkit.configuration.ConfigurationSection;

/** The plugin's config.yml, validated. */
public record Settings(int sizeCapBytes, int searchRadius, double xpFraction, boolean regeneratePackOnStart,
        boolean oneLifeRequired) {

    public Settings {
        if (sizeCapBytes <= 0) throw new IllegalArgumentException("size-cap-bytes must be > 0");
        if (searchRadius < 0) throw new IllegalArgumentException("search-radius must be >= 0");
        if (xpFraction < 0 || xpFraction > 1) throw new IllegalArgumentException("xp-fraction must be between 0 and 1");
    }

    public static Settings from(ConfigurationSection c) {
        Object required = c.get("one-life.required", false);
        if (!(required instanceof Boolean oneLifeRequired)) {
            throw new IllegalArgumentException("one-life.required must be true or false, not: " + required);
        }
        return new Settings(
                c.getInt("size-cap-bytes", 524288),
                c.getInt("search-radius", 3),
                c.getDouble("xp-fraction", 0.5),
                c.getBoolean("regenerate-pack-on-start", true),
                oneLifeRequired);
    }
}
