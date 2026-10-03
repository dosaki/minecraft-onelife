package net.dosaki.onelife.mode;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** /onelife shows the mode; /onelife on turns it on. There is no "off". */
public final class OneLifeCommand {

    private OneLifeCommand() {}

    public static void register(JavaPlugin plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        Commands.literal("onelife")
                                .requires(source -> source.getExecutor() instanceof Player)
                                .executes(ctx -> status(ctx.getSource()))
                                .then(Commands.literal("on").executes(ctx -> turnOn(ctx.getSource())))
                                .build(),
                        "Show or turn on One Life for this life",
                        List.of()));
    }

    private static int status(CommandSourceStack source) {
        Player player = (Player) source.getExecutor();
        ModeState state = PlayerModeStore.get(player);
        player.sendMessage(state.enabled()
                ? Component.text("One Life is on. Only death turns it off.", NamedTextColor.DARK_RED)
                : Component.text("One Life is off. /onelife on to turn it on.", NamedTextColor.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int turnOn(CommandSourceStack source) {
        Player player = (Player) source.getExecutor();
        ModeState state = PlayerModeStore.get(player);
        if (state.enabled()) {
            player.sendMessage(Component.text("One Life is already on.", NamedTextColor.GRAY));
            return Command.SINGLE_SUCCESS;
        }
        PlayerModeStore.set(player, state.turnOn());
        player.sendMessage(Component.text("One Life is on. Only death turns it off.", NamedTextColor.DARK_RED));
        return Command.SINGLE_SUCCESS;
    }
}
