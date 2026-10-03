package net.dosaki.onelife.craft;

import java.util.Optional;
import java.util.logging.Logger;
import net.dosaki.onelife.Keys;
import net.dosaki.onelife.grave.GraveData;
import net.dosaki.onelife.grave.GraveDataException;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

/** Grave data on a placed grave: the persistent data of the CraftEngine furniture's meta entity. */
public final class GraveStore {

    private GraveStore() {}

    public static Optional<byte[]> raw(Entity meta) {
        return Optional.ofNullable(meta.getPersistentDataContainer().get(Keys.GRAVE, PersistentDataType.BYTE_ARRAY));
    }

    /** Empty if there is no data or it can't be decoded (logged; the raw bytes are left untouched). */
    public static Optional<GraveData> read(Entity meta, Logger log) {
        return raw(meta).flatMap(bytes -> {
            try {
                return Optional.of(GraveData.decode(bytes));
            } catch (GraveDataException e) {
                log.warning("Unreadable grave data on entity " + meta.getUniqueId() + " at " + meta.getLocation()
                        + ": " + e.getMessage());
                return Optional.empty();
            }
        });
    }

    public static void write(Entity meta, GraveData data) {
        writeRaw(meta, data.encode());
    }

    public static void writeRaw(Entity meta, byte[] raw) {
        meta.getPersistentDataContainer().set(Keys.GRAVE, PersistentDataType.BYTE_ARRAY, raw);
    }
}
