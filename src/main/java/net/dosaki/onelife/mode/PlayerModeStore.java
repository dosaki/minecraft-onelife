package net.dosaki.onelife.mode;

import net.dosaki.onelife.Keys;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** ModeState in the player's persistent data, so it survives logouts and restarts. Main thread only. */
public final class PlayerModeStore {

    private PlayerModeStore() {}

    public static ModeState get(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        return new ModeState(
                pdc.getOrDefault(Keys.CHOSEN, PersistentDataType.BOOLEAN, false),
                pdc.getOrDefault(Keys.ENABLED, PersistentDataType.BOOLEAN, false));
    }

    public static void set(Player player, ModeState state) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(Keys.CHOSEN, PersistentDataType.BOOLEAN, state.chosen());
        pdc.set(Keys.ENABLED, PersistentDataType.BOOLEAN, state.enabled());
    }
}
