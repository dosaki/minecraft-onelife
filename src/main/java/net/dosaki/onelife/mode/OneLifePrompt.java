package net.dosaki.onelife.mode;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/** Asks "One Life?" when a player first joins and on every respawn. */
public final class OneLifePrompt implements Listener {

    private final Plugin plugin;

    public OneLifePrompt(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * CraftEngine sends the pack during configuration and cancels the client's status packet, so no
     * PlayerResourcePackStatusEvent fires; ask shortly after join instead.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (PlayerModeStore.get(player).chosen()) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !PlayerModeStore.get(player).chosen()) show(player);
        }, 20L);
    }

    @EventHandler
    public void onRespawn(PlayerPostRespawnEvent event) {
        show(event.getPlayer());
    }

    public void show(Player player) {
        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofHours(1)).build();
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("One Life?"))
                        .canCloseWithEscape(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                "In One Life, your grave is sealed forever when you die: anyone can look at "
                                        + "your items, nobody can take them. You can turn One Life on later "
                                        + "with /onelife on, but only death turns it off."))))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("One Life", NamedTextColor.DARK_RED))
                                .action(DialogAction.customClick((response, audience) -> choose(audience, true), once))
                                .build(),
                        ActionButton.builder(Component.text("Not this life"))
                                .action(DialogAction.customClick((response, audience) -> choose(audience, false), once))
                                .build())));
        player.showDialog(dialog);
    }

    private void choose(Audience audience, boolean oneLife) {
        if (!(audience instanceof Player player)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            // A stale click (dialog answered after dying or leaving) must not change the mode.
            if (!player.isOnline() || player.isDead()) return;
            ModeState state = PlayerModeStore.get(player).choose(oneLife);
            PlayerModeStore.set(player, state);
            player.sendMessage(state.enabled()
                    ? Component.text("One Life is on. Only death turns it off.", NamedTextColor.DARK_RED)
                    : Component.text("One Life is off. Your grave can be looted. /onelife on to change.",
                            NamedTextColor.GRAY));
        });
    }
}
