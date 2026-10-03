package net.dosaki.onelife.grave;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/** The words on a grave: floating text, item name and tooltip. */
public final class GraveText {

    private static final int LORE_WIDTH = 32;

    private GraveText() {}

    public static Component cause(GraveData d) {
        try {
            return GsonComponentSerializer.gson().deserialize(d.causeJson());
        } catch (RuntimeException e) {
            return Component.text("Died");
        }
    }

    public static Component hologram(GraveData d) {
        List<Component> lines = new ArrayList<>();
        lines.add(title(d));
        lines.add(cause(d).colorIfAbsent(NamedTextColor.WHITE));
        if (d.lastMessage() != null) lines.add(quote(d.lastMessage()));
        lines.add(modeLabel(d));
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    public static Component itemName(GraveData d) {
        return Component.text(d.ownerName() + "'s Gravestone");
    }

    public static List<Component> lore(GraveData d) {
        List<Component> lines = new ArrayList<>();
        lines.add(cause(d).colorIfAbsent(NamedTextColor.WHITE));
        if (d.lastMessage() != null) {
            List<String> wrapped = wrap("\"" + d.lastMessage() + "\"", LORE_WIDTH);
            for (String line : wrapped) {
                lines.add(Component.text(line, NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
            }
        }
        lines.add(modeLabel(d));
        return lines.stream().map(c -> c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)).toList();
    }

    /** Splits at spaces into lines of at most {@code width} characters; long words are cut. */
    public static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ", -1)) {
            while (word.length() > width) {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (line.isEmpty()) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= width) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty() || lines.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private static Component title(GraveData d) {
        return Component.text("☠ " + d.ownerName(), NamedTextColor.GRAY).decorate(TextDecoration.BOLD);
    }

    private static Component quote(String message) {
        return Component.text("\"" + message + "\"", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC);
    }

    private static Component modeLabel(GraveData d) {
        return d.mode() == GraveData.Mode.ONE_LIFE
                ? Component.text("One Life", NamedTextColor.DARK_RED)
                : Component.text("Lootable", NamedTextColor.GREEN);
    }
}
