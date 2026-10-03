package net.dosaki.onelife.mode;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.dosaki.onelife.Keys;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/**
 * Remembers each player's last chat message. Chat arrives off the main thread, so the message goes into a
 * concurrent cache immediately and into the player's persistent data on the next tick.
 */
public final class LastMessageTracker implements Listener {

    private static final int MAX_LENGTH = 256;

    private final Plugin plugin;
    private final Map<UUID, String> recent = new ConcurrentHashMap<>();

    public LastMessageTracker(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (text.length() > MAX_LENGTH) text = text.substring(0, MAX_LENGTH);
        Player player = event.getPlayer();
        String message = text;
        recent.put(player.getUniqueId(), message);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.getPersistentDataContainer().set(Keys.LAST_MESSAGE, PersistentDataType.STRING, message);
            }
        });
    }

    /** Main thread only. */
    public @Nullable String get(Player player) {
        String cached = recent.get(player.getUniqueId());
        if (cached != null) return cached;
        return player.getPersistentDataContainer().get(Keys.LAST_MESSAGE, PersistentDataType.STRING);
    }
}
