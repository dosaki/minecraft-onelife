package net.dosaki.onelife.grave;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Everything a gravestone remembers. Stored as bytes in persistent data on the gravestone item (carried)
 * and on the furniture's meta entity (placed).
 *
 * @param causeJson the death message as an Adventure component in JSON
 * @param items     the 41 inventory slots, encoded by {@code ItemCodec}; opaque here
 */
public record GraveData(
        UUID graveId,
        UUID ownerId,
        String ownerName,
        Mode mode,
        String causeJson,
        @Nullable String lastMessage,
        int xp,
        boolean mined,
        byte[] items) {

    private static final byte FORMAT = 1;

    public enum Mode { ONE_LIFE, LOOTABLE }

    public GraveData {
        Objects.requireNonNull(graveId, "graveId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(ownerName, "ownerName");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(causeJson, "causeJson");
        Objects.requireNonNull(items, "items");
        if (xp < 0) throw new IllegalArgumentException("xp must be >= 0, was " + xp);
        items = items.clone();
    }

    @Override
    public byte[] items() {
        return items.clone();
    }

    public GraveData withItems(byte[] newItems) {
        return new GraveData(graveId, ownerId, ownerName, mode, causeJson, lastMessage, xp, mined, newItems);
    }

    public GraveData withMined() {
        return new GraveData(graveId, ownerId, ownerName, mode, causeJson, lastMessage, xp, true, items);
    }

    /** XP paid to whoever breaks the grave first, rounded down. */
    public int payout(double fraction) {
        return (int) Math.floor(xp * fraction);
    }

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(FORMAT);
            writeUuid(out, graveId);
            writeUuid(out, ownerId);
            out.writeUTF(ownerName);
            out.writeByte(mode.ordinal());
            out.writeUTF(causeJson);
            out.writeBoolean(lastMessage != null);
            if (lastMessage != null) out.writeUTF(lastMessage);
            out.writeInt(xp);
            out.writeBoolean(mined);
            out.writeInt(items.length);
            out.write(items);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static GraveData decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            byte format = in.readByte();
            if (format != FORMAT) throw new GraveDataException("unknown grave format " + format);
            UUID graveId = readUuid(in);
            UUID ownerId = readUuid(in);
            String ownerName = in.readUTF();
            int modeIndex = in.readUnsignedByte();
            if (modeIndex >= Mode.values().length) throw new GraveDataException("unknown mode " + modeIndex);
            String causeJson = in.readUTF();
            String lastMessage = in.readBoolean() ? in.readUTF() : null;
            int xp = in.readInt();
            boolean mined = in.readBoolean();
            int length = in.readInt();
            if (length < 0 || length > in.available()) throw new GraveDataException("bad items length " + length);
            byte[] items = in.readNBytes(length);
            if (in.available() != 0) throw new GraveDataException("trailing bytes after grave data");
            return new GraveData(graveId, ownerId, ownerName, Mode.values()[modeIndex], causeJson, lastMessage,
                    xp, mined, items);
        } catch (IOException | IllegalArgumentException e) {
            throw new GraveDataException("corrupt grave data", e);
        }
    }

    private static void writeUuid(DataOutputStream out, UUID id) throws IOException {
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GraveData g
                && graveId.equals(g.graveId) && ownerId.equals(g.ownerId) && ownerName.equals(g.ownerName)
                && mode == g.mode && causeJson.equals(g.causeJson) && Objects.equals(lastMessage, g.lastMessage)
                && xp == g.xp && mined == g.mined && Arrays.equals(items, g.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graveId, ownerId, ownerName, mode, causeJson, lastMessage, xp, mined)
                * 31 + Arrays.hashCode(items);
    }

    @Override
    public String toString() {
        return "GraveData[" + graveId + ", owner=" + ownerName + ", mode=" + mode + ", xp=" + xp
                + ", mined=" + mined + ", items=" + items.length + " bytes]";
    }
}
