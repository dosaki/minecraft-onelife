package net.dosaki.onelife.grave;

/** Grave bytes that can't be decoded. */
public final class GraveDataException extends RuntimeException {
    public GraveDataException(String message) {
        super(message);
    }

    public GraveDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
