package net.dosaki.onelife.grave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OrdinalsTest {

    @Test
    void smallNumbersAreWords() {
        assertEquals("First", Ordinals.of(1));
        assertEquals("Second", Ordinals.of(2));
        assertEquals("Third", Ordinals.of(3));
        assertEquals("Fourth", Ordinals.of(4));
        assertEquals("Eleventh", Ordinals.of(11));
        assertEquals("Twelfth", Ordinals.of(12));
        assertEquals("Thirteenth", Ordinals.of(13));
        assertEquals("Nineteenth", Ordinals.of(19));
    }

    @Test
    void tensAndCompounds() {
        assertEquals("Twentieth", Ordinals.of(20));
        assertEquals("Twenty-First", Ordinals.of(21));
        assertEquals("Twenty-Second", Ordinals.of(22));
        assertEquals("Twenty-Third", Ordinals.of(23));
        assertEquals("Thirtieth", Ordinals.of(30));
        assertEquals("Fortieth", Ordinals.of(40));
        assertEquals("Ninety-Ninth", Ordinals.of(99));
    }

    @Test
    void hundredAndUpUseDigits() {
        assertEquals("100th", Ordinals.of(100));
        assertEquals("101st", Ordinals.of(101));
        assertEquals("102nd", Ordinals.of(102));
        assertEquals("103rd", Ordinals.of(103));
        assertEquals("104th", Ordinals.of(104));
        assertEquals("111th", Ordinals.of(111));
        assertEquals("112th", Ordinals.of(112));
        assertEquals("113th", Ordinals.of(113));
        assertEquals("121st", Ordinals.of(121));
    }

    @Test
    void nonPositiveIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Ordinals.of(0));
        assertThrows(IllegalArgumentException.class, () -> Ordinals.of(-5));
    }
}
