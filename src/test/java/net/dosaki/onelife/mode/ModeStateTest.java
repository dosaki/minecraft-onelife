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

    @Test
    void requiredMakesEveryGraveOneLife() {
        assertEquals(Mode.ONE_LIFE, ModeState.UNCHOSEN.graveMode(true));
        assertEquals(Mode.ONE_LIFE, ModeState.UNCHOSEN.choose(false).graveMode(true));
        assertEquals(Mode.ONE_LIFE, ModeState.UNCHOSEN.turnOn().graveMode(true));
    }

    @Test
    void notRequiredFollowsTheChoice() {
        assertEquals(Mode.LOOTABLE, ModeState.UNCHOSEN.choose(false).graveMode(false));
        assertEquals(Mode.ONE_LIFE, ModeState.UNCHOSEN.turnOn().graveMode(false));
    }
}
