package edu.svv.standings.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MatchResultTest {

    private static final Team A = new Team("A");
    private static final Team B = new Team("B");

    // EP + BVA: 0 gol geçerli alt sınır; 4 argümanlı kurucu varsayılan olarak NONE vermeli
    @Test
    @DisplayName("TC-004 zero goals are valid and default forfeit is NONE")
    void zeroGoalsAndDefaultForfeit() {
        MatchResult plain = new MatchResult(A, B, 0, 0);
        MatchResult home = new MatchResult(A, B, 0, 0, Forfeit.HOME);
        MatchResult away = new MatchResult(A, B, 0, 0, Forfeit.AWAY);
        assertAll(
                () -> assertEquals(Forfeit.NONE, plain.forfeit()),
                () -> assertEquals(Forfeit.HOME, home.forfeit()),
                () -> assertEquals(Forfeit.AWAY, away.forfeit()));
    }

    // NEG + BVA: V-05, -1 her iki tarafta ilk geçersiz değer; 0 ise hâlâ geçerli
    @ParameterizedTest(name = "{0}-{1} valid={2}")
    @CsvSource({"-1,0,false", "0,-1,false", "0,0,true"})
    @DisplayName("TC-005 negative goals are rejected")
    void negativeGoalsBoundary(int home, int away, boolean valid) {
        if (valid) {
            assertDoesNotThrow(() -> new MatchResult(A, B, home, away));
        } else {
            assertThrows(IllegalArgumentException.class, () -> new MatchResult(A, B, home, away));
        }
    }

    // NEG: V-04 + D-01; aynı takım, yalnızca boşluk farkı olsa bile reddedilmeli
    @Test
    @DisplayName("TC-006 a team cannot play itself, even with padded name")
    void teamCannotPlayItself() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new MatchResult(A, A, 1, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MatchResult(new Team("A"), new Team(" A "), 1, 0)));
    }

    // NEG + karar (Q3): null ev/deplasman reddedilir, null forfeit NONE sayılır
    @Test
    @DisplayName("TC-007 null teams are rejected, null forfeit becomes NONE")
    void nullFieldsHandling() {
        assertAll(
                () -> assertThrows(RuntimeException.class, () -> new MatchResult(null, B, 1, 0)),
                () -> assertThrows(RuntimeException.class, () -> new MatchResult(A, null, 1, 0)),
                () -> assertEquals(Forfeit.NONE, new MatchResult(A, B, 1, 0, null).forfeit()));
    }
}
