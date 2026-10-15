package net.dosaki.onelife.mode;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import java.util.Optional;
import net.dosaki.onelife.Settings;
import net.dosaki.onelife.craft.GraveHolograms;
import net.dosaki.onelife.craft.GraveItems;
import net.dosaki.onelife.craft.GraveStore;
import net.dosaki.onelife.grave.GraveData;
import net.dosaki.onelife.grave.GraveText;
import net.dosaki.onelife.view.GraveViewer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * /onelife shows the mode; /onelife on turns it on. There is no "off". When One Life is required, both just say so.
 * /onelife lastwords &lt;text&gt; (ops) rewrites the last words of the gravestone you're looking at.
 */
public final class OneLifeCommand {

    public static final String LAST_WORDS_PERMISSION = "onelife.lastwords";

    private OneLifeCommand() {}

    public static void register(JavaPlugin plugin, Settings settings, GraveViewer viewer) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        Commands.literal("onelife")
                                .requires(source -> source.getExecutor() instanceof Player)
                                .executes(ctx -> status(ctx.getSource(), settings))
                                .then(Commands.literal("on").executes(ctx -> turnOn(ctx.getSource(), settings)))
                                .then(Commands.literal("lastwords")
                                        .requires(source -> source.getSender().hasPermission(LAST_WORDS_PERMISSION))
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> setLastWords(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "text"), plugin, viewer))))
                                .build(),
                        "Show or turn on One Life for this life",
                        List.of()));
    }

    private static int status(CommandSourceStack source, Settings settings) {
        Player player = (Player) source.getExecutor();
        if (settings.oneLifeRequired()) return required(player);
        ModeState state = PlayerModeStore.get(player);
        player.sendMessage(state.enabled()
                ? Component.text("One Life is on. Only death turns it off.", NamedTextColor.DARK_RED)
                : Component.text("One Life is off. /onelife on to turn it on.", NamedTextColor.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int turnOn(CommandSourceStack source, Settings settings) {
        Player player = (Player) source.getExecutor();
        if (settings.oneLifeRequired()) return required(player);
        ModeState state = PlayerModeStore.get(player);
        if (state.enabled()) {
            player.sendMessage(Component.text("One Life is already on.", NamedTextColor.GRAY));
            return Command.SINGLE_SUCCESS;
        }
        PlayerModeStore.set(player, state.turnOn());
        player.sendMessage(Component.text("One Life is on. Only death turns it off.", NamedTextColor.DARK_RED));
        return Command.SINGLE_SUCCESS;
    }

    private static int required(Player player) {
        player.sendMessage(Component.text("One Life is required on this server.", NamedTextColor.DARK_RED));
        return Command.SINGLE_SUCCESS;
    }

    private static int setLastWords(CommandSourceStack source, String text, JavaPlugin plugin, GraveViewer viewer) {
        Player player = (Player) source.getExecutor();
        String words = text.strip();
        if (words.length() > LastMessageTracker.MAX_LENGTH) {
            player.sendMessage(Component.text("Last words can be at most " + LastMessageTracker.MAX_LENGTH
                    + " characters.", NamedTextColor.RED));
            return 0;
        }
        BukkitFurniture furniture = CraftEngineFurniture.rayTrace(player);
        if (furniture == null || !GraveItems.FURNITURE.equals(furniture.id())) {
            player.sendMessage(Component.text("Look at a gravestone first.", NamedTextColor.RED));
            return 0;
        }
        Entity meta = furniture.bukkitEntity();
        Optional<GraveData> before = GraveStore.read(meta, plugin.getLogger());
        if (before.isEmpty()) {
            player.sendMessage(Component.text("This gravestone's data can't be read.", NamedTextColor.RED));
            return 0;
        }
        // An open window writes its own copy back on close, which would undo the edit; close it and re-read.
        viewer.closeAll(before.get().graveId());
        GraveData data = GraveStore.read(meta, plugin.getLogger()).orElse(before.get()).withLastMessage(words);
        GraveStore.write(meta, data);
        GraveHolograms.remove(furniture.location(), data.graveId());
        GraveHolograms.spawn(furniture.location(), data);
        player.sendMessage(Component.text("Changed the last words on " + GraveText.title(data) + ".",
                NamedTextColor.GRAY));
        return Command.SINGLE_SUCCESS;
    }
}
