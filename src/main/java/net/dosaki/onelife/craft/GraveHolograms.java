package net.dosaki.onelife.craft;

import java.util.UUID;
import net.dosaki.onelife.Keys;
import net.dosaki.onelife.grave.GraveData;
import net.dosaki.onelife.grave.GraveText;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

/** Floating text above a grave, tagged with the grave id so it can be found and removed. */
public final class GraveHolograms {

    private static final double HEIGHT = 1.3;

    private GraveHolograms() {}

    public static void spawn(Location graveLocation, GraveData data) {
        Location at = graveLocation.clone().add(0, HEIGHT, 0);
        at.getWorld().spawn(at, TextDisplay.class, td -> {
            td.text(GraveText.hologram(data));
            td.setBillboard(Display.Billboard.CENTER);
            td.setLineWidth(200);
            td.setPersistent(true);
            td.getPersistentDataContainer().set(Keys.GRAVE_ID, PersistentDataType.STRING, data.graveId().toString());
        });
    }

    public static void remove(Location graveLocation, UUID graveId) {
        Location at = graveLocation.clone().add(0, HEIGHT, 0);
        for (TextDisplay td : at.getWorld().getNearbyEntitiesByType(TextDisplay.class, at, 1.0)) {
            if (isHologramFor(td, graveId)) td.remove();
        }
    }

    public static boolean isHologramFor(Entity entity, UUID graveId) {
        return entity instanceof TextDisplay
                && graveId.toString().equals(
                        entity.getPersistentDataContainer().get(Keys.GRAVE_ID, PersistentDataType.STRING));
    }
}
