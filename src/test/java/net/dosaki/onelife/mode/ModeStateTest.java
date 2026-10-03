package net.dosaki.onelife.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.dosaki.onelife.grave.GraveData.Mode;
import org.junit.jupiter.api.Test;

class ModeStateTest {

    @Test
    void choosingNoIsLootable() {
        ModeState s = ModeState.UNCHOSEN.choose(false);
        assertEquals(new ModeState(true, false), s);
        assertEquals(Mode.LOOTABLE, s.graveMode());
    }

    @Test
    void choosingYesIsOneLife() {
        ModeState s = ModeState.UNCHOSEN.choose(true);
        assertEquals(new ModeState(true, true), s);
        assertEquals(Mode.ONE_LIFE, s.graveMode());
    }

    @Test
    void cannotChooseOffOnceOn() {
        assertEquals(new ModeState(true, true), ModeState.UNCHOSEN.turnOn().choose(false));
    }

    @Test
    void turnOnAnyTime() {
        assertEquals(new ModeState(true, true), ModeState.UNCHOSEN.turnOn());
        assertEquals(new ModeState(true, true), ModeState.UNCHOSEN.choose(false).turnOn());
    }

    @Test
    void deathClearsEverything() {
        assertEquals(ModeState.UNCHOSEN, ModeState.UNCHOSEN.turnOn().afterDeath());
    }

    @Test
    void unchosenDiesLootable() {
        assertEquals(Mode.LOOTABLE, ModeState.UNCHOSEN.graveMode());
    }
}
