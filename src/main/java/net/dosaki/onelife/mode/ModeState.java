package net.dosaki.onelife.mode;

import net.dosaki.onelife.grave.GraveData.Mode;

/** A player's One Life choice for the current life. Once on, only death turns it off. */
public record ModeState(boolean chosen, boolean enabled) {

    public static final ModeState UNCHOSEN = new ModeState(false, false);

    public ModeState choose(boolean oneLife) {
        return new ModeState(true, enabled || oneLife);
    }

    public ModeState turnOn() {
        return new ModeState(true, true);
    }

    public ModeState afterDeath() {
        return UNCHOSEN;
    }

    public Mode graveMode() {
        return graveMode(false);
    }

    /** The mode of the grave this death makes. When One Life is required, the stored choice is ignored. */
    public Mode graveMode(boolean oneLifeRequired) {
        return oneLifeRequired || enabled ? Mode.ONE_LIFE : Mode.LOOTABLE;
    }
}
