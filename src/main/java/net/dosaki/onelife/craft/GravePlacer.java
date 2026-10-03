package net.dosaki.onelife.craft;

import java.util.List;
import java.util.logging.Level;
import net.dosaki.onelife.grave.FreeSpaceFinder;
import net.dosaki.onelife.grave.GraveData;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.Nullable;

/** Finds spaces for graves and places the CraftEngine furniture with grave data. */
public final class GravePlacer {

    private static final String VARIANT = "ground";

    private GravePlacer() {}

    public static List<Block> findSpaces(Location death, int count, int radius) {
        World world = death.getWorld();
        FreeSpaceFinder.Space space = new FreeSpaceFinder.Space() {
            public boolean isFree(int x, int y, int z) { return isFreeBlock(world.getBlockAt(x, y, z)); }
            // The finder's upward fallback skips only "bedrock"; also skip blocks holding furniture so
            // a fallback never replaces a block that already has a grave or holds a container.
            public boolean isBedrock(int x, int y, int z) {
                Block block = world.getBlockAt(x, y, z);
                return block.getType() == Material.BEDROCK || hasFurniture(block) || holdsTileEntity(block);
            }
            public int minY() { return world.getMinHeight(); }
            public int maxY() { return world.getMaxHeight(); }
        };
        FreeSpaceFinder.Pos origin = new FreeSpaceFinder.Pos(death.getBlockX(), death.getBlockY(), death.getBlockZ());
        return FreeSpaceFinder.find(space, origin, count, radius).stream()
                .map(p -> world.getBlockAt(p.x(), p.y(), p.z()))
                .toList();
    }

    /** Air or liquid, with no furniture (e.g. another grave) already in the block. */
    public static boolean isFreeBlock(Block block) {
        if (!(block.isEmpty() || block.isLiquid())) return false;
        return !hasFurniture(block);
    }

    /** The furniture's meta entity is a zero-size point at the block floor, so expand the box to touch it. */
    private static boolean hasFurniture(Block block) {
        return !block.getWorld()
                .getNearbyEntities(BoundingBox.of(block).expand(0.01),
                        e -> CraftEngineFurniture.isFurniture(e) && e.getLocation().getBlock().equals(block))
                .isEmpty();
    }

    /** Chests, barrels, hoppers, signs and the like: replacing them would destroy contents. */
    private static boolean holdsTileEntity(Block block) {
        return block.getState(false) instanceof TileState;
    }

    /** Places a grave in {@code block}, replacing it if it isn't free. Null if CraftEngine refused. */
    public static @Nullable BukkitFurniture place(Block block, float yaw, byte[] raw, @Nullable GraveData data) {
        // Remember what the fallback clears so a failed placement doesn't leave the terrain deleted.
        BlockData saved = block.getBlockData();
        if (!block.isEmpty() && !block.isLiquid()) block.setType(Material.AIR, false);
        Location at = block.getLocation().add(0.5, 0, 0.5);
        at.setYaw(Math.round(yaw / 90f) * 90f + 180f); // face the player who died
        BukkitFurniture furniture;
        try {
            furniture = CraftEngineFurniture.place(at, GraveItems.FURNITURE, VARIANT, false);
        } catch (RuntimeException e) {
            block.setBlockData(saved, false);
            throw e;
        }
        if (furniture == null) {
            block.setBlockData(saved, false);
            return null;
        }
        GraveStore.writeRaw(furniture.bukkitEntity(), raw);
        if (data != null) {
            // The grave exists now; its text is best-effort (HologramRestorer recreates it on chunk load).
            try {
                GraveHolograms.spawn(furniture.location(), data);
            } catch (RuntimeException e) {
                Bukkit.getLogger().log(Level.WARNING, "Placed a grave at " + furniture.location()
                        + " but could not spawn its text.", e);
            }
        }
        return furniture;
    }
}
