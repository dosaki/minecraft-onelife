package net.dosaki.onelife;

import org.bukkit.NamespacedKey;

/** Persistent-data keys. Namespace is fixed so data survives a plugin rename. */
public final class Keys {
    /** Encoded GraveData, on gravestone items and furniture meta entities. */
    public static final NamespacedKey GRAVE = new NamespacedKey("onelife", "grave");
    /** Grave id (UUID string), on the floating text above a grave. */
    public static final NamespacedKey GRAVE_ID = new NamespacedKey("onelife", "grave_id");
    /** Player: has chosen One Life on or off for this life. */
    public static final NamespacedKey CHOSEN = new NamespacedKey("onelife", "chosen");
    /** Player: One Life is on. */
    public static final NamespacedKey ENABLED = new NamespacedKey("onelife", "enabled");
    /** Player: last chat message, plain text. */
    public static final NamespacedKey LAST_MESSAGE = new NamespacedKey("onelife", "last_message");

    private Keys() {}
}
