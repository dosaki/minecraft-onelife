package net.dosaki.onelife.grave;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Picks where graves go: nearest free spaces first, then blocks straight above to replace. */
public final class FreeSpaceFinder {

    public record Pos(int x, int y, int z) {}

    /** What the finder needs to know about the world. {@code maxY} is exclusive. */
    public interface Space {
        boolean isFree(int x, int y, int z);

        boolean isBedrock(int x, int y, int z);

        int minY();

        int maxY();
    }

    private FreeSpaceFinder() {}

    public static List<Pos> find(Space space, Pos origin, int count, int radius) {
        int oy = Math.clamp(origin.y(), space.minY(), space.maxY() - 1);
        Pos o = new Pos(origin.x(), oy, origin.z());

        List<Pos> candidates = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int y = oy + dy;
                    if (y < space.minY() || y >= space.maxY()) continue;
                    candidates.add(new Pos(o.x() + dx, y, o.z() + dz));
                }
            }
        }
        candidates.sort(Comparator
                .comparingInt((Pos p) -> distanceSquared(o, p))
                .thenComparingInt(p -> Math.abs(p.y() - oy))
                .thenComparing(Comparator.comparingInt((Pos p) -> p.y() - oy).reversed())
                .thenComparingInt(p -> p.x() - o.x())
                .thenComparingInt(p -> p.z() - o.z()));

        Set<Pos> chosen = new LinkedHashSet<>();
        for (Pos p : candidates) {
            if (chosen.size() == count) break;
            if (space.isFree(p.x(), p.y(), p.z())) chosen.add(p);
        }
        for (int y = oy + 1; chosen.size() < count && y < space.maxY(); y++) {
            Pos p = new Pos(o.x(), y, o.z());
            if (!chosen.contains(p) && !space.isBedrock(p.x(), p.y(), p.z())) chosen.add(p);
        }
        return List.copyOf(chosen);
    }

    private static int distanceSquared(Pos a, Pos b) {
        int dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz;
    }
}
