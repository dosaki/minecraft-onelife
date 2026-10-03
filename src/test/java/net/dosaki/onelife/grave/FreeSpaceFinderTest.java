package net.dosaki.onelife.grave;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FreeSpaceFinderTest {

    /** A world that is solid stone except for listed free positions; bedrock where listed. */
    private static final class FakeSpace implements FreeSpaceFinder.Space {
        final Set<FreeSpaceFinder.Pos> free = new HashSet<>();
        final Set<FreeSpaceFinder.Pos> bedrock = new HashSet<>();

        public boolean isFree(int x, int y, int z) { return free.contains(new FreeSpaceFinder.Pos(x, y, z)); }
        public boolean isBedrock(int x, int y, int z) { return bedrock.contains(new FreeSpaceFinder.Pos(x, y, z)); }
        public int minY() { return -64; }
        public int maxY() { return 320; }
    }

    private static FreeSpaceFinder.Pos p(int x, int y, int z) {
        return new FreeSpaceFinder.Pos(x, y, z);
    }

    @Test
    void originWhenFree() {
        FakeSpace s = new FakeSpace();
        s.free.add(p(0, 70, 0));
        s.free.add(p(1, 70, 0));
        assertEquals(List.of(p(0, 70, 0)), FreeSpaceFinder.find(s, p(0, 70, 0), 1, 3));
    }

    @Test
    void nearestNeighbourWhenOriginTaken() {
        FakeSpace s = new FakeSpace();
        s.free.add(p(2, 70, 0));
        s.free.add(p(0, 71, 0));
        assertEquals(List.of(p(0, 71, 0)), FreeSpaceFinder.find(s, p(0, 70, 0), 1, 3));
    }

    @Test
    void aboveBeatsBelowAtSameDistance() {
        FakeSpace s = new FakeSpace();
        s.free.add(p(0, 69, 0));
        s.free.add(p(0, 71, 0));
        assertEquals(List.of(p(0, 71, 0)), FreeSpaceFinder.find(s, p(0, 70, 0), 1, 3));
    }

    @Test
    void severalSpacesNearestFirst() {
        FakeSpace s = new FakeSpace();
        s.free.add(p(0, 70, 0));
        s.free.add(p(3, 70, 0));
        s.free.add(p(1, 70, 0));
        assertEquals(List.of(p(0, 70, 0), p(1, 70, 0), p(3, 70, 0)), FreeSpaceFinder.find(s, p(0, 70, 0), 3, 3));
    }

    @Test
    void outsideRadiusIsIgnoredAndFallbackGoesUpSkippingBedrock() {
        FakeSpace s = new FakeSpace();
        s.free.add(p(4, 70, 0)); // outside radius 3
        s.bedrock.add(p(0, 71, 0));
        assertEquals(List.of(p(0, 72, 0), p(0, 73, 0)), FreeSpaceFinder.find(s, p(0, 70, 0), 2, 3));
    }

    @Test
    void mixesFreeAndFallback() {
        FakeSpace s = new FakeSpace();
        s.free.add(p(0, 70, 0));
        assertEquals(List.of(p(0, 70, 0), p(0, 71, 0)), FreeSpaceFinder.find(s, p(0, 70, 0), 2, 1));
    }

    @Test
    void fallbackDoesNotReuseAChosenFreeSpace() {
        FakeSpace s = new FakeSpace();
        s.free.add(p(0, 71, 0));
        assertEquals(List.of(p(0, 71, 0), p(0, 72, 0)), FreeSpaceFinder.find(s, p(0, 70, 0), 2, 1));
    }

    @Test
    void originBelowWorldIsClamped() {
        FakeSpace s = new FakeSpace();
        s.free.add(p(0, -64, 0));
        assertEquals(List.of(p(0, -64, 0)), FreeSpaceFinder.find(s, p(0, -80, 0), 1, 0));
    }

    @Test
    void nothingWhenAtTopAndNothingFree() {
        // origin clamps to y=319; the fallback scans strictly above the origin, so there is nowhere to go
        FakeSpace s = new FakeSpace();
        assertEquals(List.of(), FreeSpaceFinder.find(s, p(0, 400, 0), 1, 0));
    }
}
