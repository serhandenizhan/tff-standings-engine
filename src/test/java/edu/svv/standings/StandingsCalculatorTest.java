package edu.svv.standings;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.svv.standings.model.Forfeit;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StandingsCalculatorTest {

    private final StandingsCalculator calculator = new StandingsCalculator();

    // ---- test yardımcıları (yalnızca test kapsamı) ----

    private static Team t(String name) {
        return new Team(name);
    }

    private static List<Team> teams(String... names) {
        return Arrays.stream(names).map(Team::new).collect(Collectors.toList());
    }

    private static MatchResult m(String home, String away, int hg, int ag) {
        return new MatchResult(t(home), t(away), hg, ag);
    }

    private static MatchResult m(String home, String away, int hg, int ag, Forfeit forfeit) {
        return new MatchResult(t(home), t(away), hg, ag, forfeit);
    }

    private static PointDeduction d(String team, int points) {
        return new PointDeduction(t(team), points);
    }

    private List<StandingRow> calc(List<Team> teams, List<MatchResult> matches, List<PointDeduction> deductions) {
        return calculator.calculate(teams, matches, deductions);
    }

    private List<StandingRow> calc(List<Team> teams, MatchResult... matches) {
        return calc(teams, List.of(matches), List.of());
    }

    private static StandingRow row(List<StandingRow> table, String name) {
        return table.stream().filter(r -> r.getTeam().name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("no row for " + name));
    }

    private static List<String> order(List<StandingRow> table) {
        return table.stream().map(r -> r.getTeam().name()).collect(Collectors.toList());
    }

    /** Bir satırın P W D L GF GA GD puan değerlerini tek seferde doğrular. */
    private static void assertStats(StandingRow r, int p, int w, int dr, int l, int gf, int ga, int gd, int pts) {
        assertAll("stats of " + r.getTeam().name(),
                () -> assertEquals(p, r.getPlayed(), "played"),
                () -> assertEquals(w, r.getWon(), "won"),
                () -> assertEquals(dr, r.getDrawn(), "drawn"),
                () -> assertEquals(l, r.getLost(), "lost"),
                () -> assertEquals(gf, r.getGoalsFor(), "goalsFor"),
                () -> assertEquals(ga, r.getGoalsAgainst(), "goalsAgainst"),
                () -> assertEquals(gd, r.getGoalDifference(), "goalDifference"),
                () -> assertEquals(pts, r.getPoints(), "points"));
    }

    @Nested
    class Stats {

        // EP: galibiyet/mağlubiyet sınıfı; ev-deplasman karıştırılmamalı
        @Test
        @DisplayName("TC-010 win and loss statistics for home and away winners")
        void winAndLoss() {
            List<StandingRow> a = calc(teams("A", "B"), m("A", "B", 2, 1));
            List<StandingRow> b = calc(teams("A", "B"), m("A", "B", 0, 3));
            assertAll(
                    () -> assertEquals(List.of("A", "B"), order(a)),
                    () -> assertEquals(1, row(a, "A").getPosition()),
                    () -> assertStats(row(a, "A"), 1, 1, 0, 0, 2, 1, 1, 3),
                    () -> assertEquals(2, row(a, "B").getPosition()),
                    () -> assertStats(row(a, "B"), 1, 0, 0, 1, 1, 2, -1, 0),
                    () -> assertEquals(List.of("B", "A"), order(b)),
                    () -> assertStats(row(b, "B"), 1, 1, 0, 0, 3, 0, 3, 3),
                    () -> assertStats(row(b, "A"), 1, 0, 0, 1, 0, 3, -3, 0));
        }

        // EP + BVA: beraberlik 1 puan; 0-0 gol alt sınırı; takım listesi [B, A] iken eşitlik çözülemez
        @Test
        @DisplayName("TC-011 draws give one point each; fully level pair is unresolved")
        void draws() {
            List<StandingRow> a = calc(teams("B", "A"), m("A", "B", 1, 1));
            List<StandingRow> b = calc(teams("B", "A"), m("A", "B", 0, 0));
            assertAll(
                    () -> assertStats(row(a, "A"), 1, 0, 1, 0, 1, 1, 0, 1),
                    () -> assertStats(row(a, "B"), 1, 0, 1, 0, 1, 1, 0, 1),
                    () -> assertStats(row(b, "A"), 1, 0, 1, 0, 0, 0, 0, 1),
                    () -> assertStats(row(b, "B"), 1, 0, 1, 0, 0, 0, 0, 1),
                    () -> assertEquals(List.of("A", "B"), order(a)),
                    () -> assertEquals(List.of("A", "B"), order(b)),
                    () -> assertTrue(row(a, "A").isUnresolved() && row(a, "B").isUnresolved()),
                    () -> assertTrue(row(b, "A").isUnresolved() && row(b, "B").isUnresolved()));
        }

        // EP: tam çift devre, eşitlik yok; sıra alfabetik değil
        @Test
        @DisplayName("TC-012 full double round-robin without ties")
        void doubleRoundRobin() {
            List<StandingRow> table = calc(teams("A", "B", "C"),
                    m("A", "B", 2, 0), m("B", "A", 1, 1),
                    m("A", "C", 0, 1), m("C", "A", 2, 2),
                    m("B", "C", 3, 1), m("C", "B", 0, 1));
            assertAll(
                    () -> assertEquals(List.of("B", "A", "C"), order(table)),
                    () -> assertStats(row(table, "B"), 4, 2, 1, 1, 5, 4, 1, 7),
                    () -> assertStats(row(table, "A"), 4, 1, 2, 1, 5, 4, 1, 5),
                    () -> assertStats(row(table, "C"), 4, 1, 1, 2, 4, 6, -2, 4),
                    () -> assertFalse(table.stream().anyMatch(StandingRow::isUnresolved)));
        }

        // EP: maç oynamayan takım da tabloda; B ve C hiç eşleşmediği için h2h eşit, averaj belirler
        @Test
        @DisplayName("TC-013 team without matches is listed")
        void teamWithoutMatches() {
            List<StandingRow> table = calc(teams("A", "B", "C"), m("A", "B", 1, 0));
            assertAll(
                    () -> assertEquals(List.of("A", "C", "B"), order(table)),
                    () -> assertStats(row(table, "C"), 0, 0, 0, 0, 0, 0, 0, 0),
                    () -> assertEquals(3, row(table, "A").getPoints()),
                    () -> assertEquals(-1, row(table, "B").getGoalDifference()));
        }

        // BVA: 1 puan fark, daha iyi averajı yener (B averajı +2 ama 3 puan; A +1 ile 4 puan)
        @Test
        @DisplayName("TC-014 one point difference beats better goal difference")
        void pointsBeforeGoalDifference() {
            List<StandingRow> table = calc(teams("A", "B", "X", "Y"),
                    m("A", "X", 1, 0), m("A", "Y", 0, 0), m("B", "X", 5, 0), m("Y", "B", 3, 0));
            assertAll(
                    () -> assertEquals(List.of("Y", "A", "B", "X"), order(table)),
                    () -> assertEquals(4, row(table, "Y").getPoints()),
                    () -> assertEquals(3, row(table, "Y").getGoalDifference()),
                    () -> assertEquals(4, row(table, "A").getPoints()),
                    () -> assertEquals(1, row(table, "A").getGoalDifference()),
                    () -> assertEquals(3, row(table, "B").getPoints()),
                    () -> assertEquals(2, row(table, "B").getGoalDifference()),
                    () -> assertEquals(0, row(table, "X").getPoints()));
        }

        // EP: sezon başı, hiç maç yok; FR-12 gereği tüm kriterler eşit -> hepsi çözülemedi
        @Test
        @DisplayName("TC-015 no matches played leaves everything unresolved")
        void noMatches() {
            List<StandingRow> table = calc(teams("B", "A"));
            assertAll(
                    () -> assertEquals(List.of("A", "B"), order(table)),
                    () -> assertEquals(0, row(table, "A").getPoints()),
                    () -> assertEquals(0, row(table, "B").getPoints()),
                    () -> assertTrue(row(table, "A").isUnresolved()),
                    () -> assertTrue(row(table, "B").isUnresolved()));
        }

        // EP: boş girdi geçerli, boş tablo döner (Q4)
        @Test
        @DisplayName("TC-016 empty input gives empty table")
        void emptyInput() {
            List<StandingRow> table = assertDoesNotThrow(() -> calc(List.of(), List.of(), List.of()));
            assertTrue(table.isEmpty());
        }
    }

    @Nested
    class ForfeitAdjustment {

        // BVA: max(3, g) sınırı 3/4 çevresinde; FMT örneği 5-2 -> 5-0
        @ParameterizedTest(name = "AWAY forfeit {0}-{1} -> winner goals {2}")
        @MethodSource("awayForfeitCases")
        @DisplayName("TC-020 away forfeit: winner gets max(3, goals)")
        void awayForfeit(int hg, int ag, int expectedGf) {
            List<StandingRow> table = calc(teams("A", "B"), m("A", "B", hg, ag, Forfeit.AWAY));
            StandingRow a = row(table, "A");
            StandingRow b = row(table, "B");
            assertAll(
                    () -> assertEquals(expectedGf, a.getGoalsFor()),
                    () -> assertEquals(0, a.getGoalsAgainst()),
                    () -> assertEquals(1, a.getWon()),
                    () -> assertEquals(3, a.getPoints()),
                    () -> assertEquals(0, b.getGoalsFor()),
                    () -> assertEquals(expectedGf, b.getGoalsAgainst()),
                    () -> assertEquals(1, b.getLost()),
                    () -> assertEquals(0, b.getPoints()),
                    () -> assertTrue(b.hasForfeitLoss()),
                    () -> assertFalse(a.hasForfeitLoss()));
        }

        static Stream<Arguments> awayForfeitCases() {
            return Stream.of(Arguments.of(2, 1, 3), Arguments.of(3, 1, 3), Arguments.of(4, 1, 4),
                    Arguments.of(5, 2, 5));
        }

        // BVA: ev sahibi hükmen yenik (ayna taraf); 0-0 kayıtlı skor da 0-3 olur
        @ParameterizedTest(name = "HOME forfeit {0}-{1} -> adjusted 0-{2}")
        @MethodSource("homeForfeitCases")
        @DisplayName("TC-021 home forfeit mirror cases")
        void homeForfeit(int hg, int ag, int expectedAwayGoals) {
            List<StandingRow> table = calc(teams("A", "B"), m("A", "B", hg, ag, Forfeit.HOME));
            StandingRow a = row(table, "A");
            StandingRow b = row(table, "B");
            assertAll(
                    () -> assertEquals(0, a.getGoalsFor()),
                    () -> assertEquals(expectedAwayGoals, a.getGoalsAgainst()),
                    () -> assertEquals(expectedAwayGoals, b.getGoalsFor()),
                    () -> assertEquals(1, b.getWon()),
                    () -> assertEquals(3, b.getPoints()),
                    () -> assertEquals(1, a.getLost()),
                    () -> assertEquals(0, a.getPoints()),
                    () -> assertTrue(a.hasForfeitLoss()));
        }

        static Stream<Arguments> homeForfeitCases() {
            return Stream.of(Arguments.of(1, 2, 3), Arguments.of(2, 5, 5), Arguments.of(0, 0, 3));
        }

        // DT F5: hükmen yenik taraf sahada öndeydi/berabereydi, yine de yenik sayılır
        @Test
        @DisplayName("TC-022 forfeiter leading or level on the pitch still loses")
        void forfeiterLeadingOrLevel() {
            List<StandingRow> a = calc(teams("A", "B"), m("A", "B", 5, 2, Forfeit.HOME));
            List<StandingRow> b = calc(teams("A", "B"), m("A", "B", 2, 2, Forfeit.AWAY));
            assertAll(
                    () -> assertStats(row(a, "B"), 1, 1, 0, 0, 3, 0, 3, 3),
                    () -> assertStats(row(a, "A"), 1, 0, 0, 1, 0, 3, -3, 0),
                    () -> assertStats(row(b, "A"), 1, 1, 0, 0, 3, 0, 3, 3),
                    () -> assertStats(row(b, "B"), 1, 0, 0, 1, 0, 3, -3, 0));
        }

        // EP: hükmen otomatik puan silmez (Q1)
        @Test
        @DisplayName("TC-023 forfeit does not deduct points automatically")
        void noAutomaticDeduction() {
            List<StandingRow> table = calc(teams("A", "B"), m("A", "B", 1, 0, Forfeit.AWAY));
            assertAll(
                    () -> assertEquals(0, row(table, "B").getPoints()),
                    () -> assertEquals(0, row(table, "B").getDeductedPoints()));
        }

        // DT: düzeltilmiş skor h2h'te kullanılmalı; ham h2h A'yı, genel averaj A'yı seçer, doğrusu B
        @Test
        @DisplayName("TC-024 adjusted forfeit score is used in head-to-head")
        void adjustedScoreInHeadToHead() {
            List<StandingRow> table = calc(teams("A", "B", "X"),
                    m("A", "B", 2, 1, Forfeit.HOME),
                    m("A", "X", 5, 0), m("X", "A", 1, 0),
                    m("B", "X", 0, 4), m("X", "B", 4, 0));
            assertAll(
                    () -> assertStats(row(table, "A"), 3, 1, 0, 2, 5, 4, 1, 3),
                    () -> assertTrue(row(table, "A").hasForfeitLoss()),
                    () -> assertStats(row(table, "B"), 3, 1, 0, 2, 3, 8, -5, 3),
                    () -> assertEquals(9, row(table, "X").getPoints()),
                    () -> assertEquals(List.of("X", "B", "A"), order(table)));
        }
    }

    @Nested
    class Deductions {

        // EP + BVA: en küçük silme (1) ve birikim (iki kez 1)
        @Test
        @DisplayName("TC-030 smallest deduction and accumulation")
        void smallestAndAccumulated() {
            List<StandingRow> a = calc(teams("A", "B"), List.of(m("A", "B", 1, 0)), List.of(d("A", 1)));
            List<StandingRow> b = calc(teams("A", "B"), List.of(m("A", "B", 1, 0)), List.of(d("A", 1), d("A", 1)));
            assertAll(
                    () -> assertEquals(2, row(a, "A").getPoints()),
                    () -> assertEquals(1, row(a, "A").getDeductedPoints()),
                    () -> assertEquals(1, row(b, "A").getPoints()),
                    () -> assertEquals(2, row(b, "A").getDeductedPoints()),
                    () -> assertEquals(List.of("A", "B"), order(b)),
                    () -> assertEquals(0, row(b, "B").getPoints()));
        }

        // BVA + DT D4: sonuç puan 0 ve -1; negatif puan 0'ın altında sıralanır (FDT 33)
        @Test
        @DisplayName("TC-031 resulting points 0 and -1 (negative total allowed)")
        void zeroAndNegativeTotals() {
            List<StandingRow> a = calc(teams("A", "B", "C"), List.of(m("A", "B", 0, 0)), List.of(d("A", 1)));
            List<StandingRow> b = calc(teams("A", "B", "C"), List.of(m("A", "B", 0, 0)), List.of(d("A", 2)));
            assertAll(
                    () -> assertEquals("B", order(a).get(0)),
                    () -> assertEquals(0, row(a, "A").getPoints()),
                    () -> assertEquals(0, row(a, "C").getPoints()),
                    () -> assertEquals(List.of("A", "C"), order(a).subList(1, 3)),
                    () -> assertTrue(row(a, "A").isUnresolved() && row(a, "C").isUnresolved()),
                    () -> assertEquals(List.of("B", "C", "A"), order(b)),
                    () -> assertEquals(-1, row(b, "A").getPoints()));
        }

        // DT D1: silme olmasaydı A h2h ile B'yi geçerdi; silme sonrası B önde
        @Test
        @DisplayName("TC-032 deduction drops a team below a former rival")
        void deductionChangesOrder() {
            List<StandingRow> table = calc(teams("A", "B", "X"),
                    List.of(m("A", "B", 1, 0), m("B", "X", 1, 0)), List.of(d("A", 1)));
            assertAll(
                    () -> assertEquals(List.of("B", "A", "X"), order(table)),
                    () -> assertEquals(3, row(table, "B").getPoints()),
                    () -> assertEquals(2, row(table, "A").getPoints()),
                    () -> assertEquals(1, row(table, "A").getDeductedPoints()),
                    () -> assertEquals(0, row(table, "X").getPoints()));
        }

        // DT D2: silme eşitlik yaratır, h2h devreye girer; yalnızca averaja bakılsa A önde olurdu
        @Test
        @DisplayName("TC-033 deduction creates a tie resolved by head-to-head")
        void deductionCreatesTie() {
            List<StandingRow> table = calc(teams("A", "B", "X"),
                    List.of(m("B", "A", 1, 0), m("A", "X", 2, 0), m("X", "A", 0, 2),
                            m("X", "B", 1, 0), m("B", "X", 0, 1)),
                    List.of(d("A", 3)));
            assertAll(
                    () -> assertEquals(3, row(table, "A").getPoints()),
                    () -> assertEquals(3, row(table, "A").getGoalDifference()),
                    () -> assertEquals(3, row(table, "B").getPoints()),
                    () -> assertEquals(-1, row(table, "B").getGoalDifference()),
                    () -> assertEquals(6, row(table, "X").getPoints()),
                    () -> assertEquals(List.of("X", "B", "A"), order(table)));
        }

        // DT D3 + varsayım A-02: silinen puan h2h puanına sızmamalı (A h2h 4 puan, B 1 puan)
        @Test
        @DisplayName("TC-034 deduction does not leak into head-to-head points")
        void deductionDoesNotLeakIntoHeadToHead() {
            List<StandingRow> table = calc(teams("A", "B", "X"),
                    List.of(m("A", "B", 1, 0), m("B", "A", 0, 0), m("A", "X", 2, 0), m("X", "A", 1, 0),
                            m("X", "B", 1, 0), m("B", "X", 0, 1)),
                    List.of(d("A", 6)));
            assertAll(
                    () -> assertEquals(1, row(table, "A").getPoints()),
                    () -> assertEquals(1, row(table, "B").getPoints()),
                    () -> assertEquals(9, row(table, "X").getPoints()),
                    () -> assertEquals(List.of("X", "A", "B"), order(table)));
        }

        // NEG: V-07 kayıtsız takıma silme; mesajda takım adı olmalı (Q5)
        @Test
        @DisplayName("TC-035 deduction for unregistered team is rejected")
        void unknownTeamDeduction() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> calc(teams("A", "B"), List.of(), List.of(d("Z", 3))));
            assertTrue(ex.getMessage().contains("Z"), ex.getMessage());
        }
    }

    @Nested
    class Validation {

        // NEG: V-01 null listeler ve null elemanlar; her satır ayrı çağrı
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("nullInputs")
        @DisplayName("TC-040 null lists and null elements are rejected")
        void nullInputsRejected(String label, List<Team> teams, List<MatchResult> matches,
                List<PointDeduction> deductions) {
            assertThrows(RuntimeException.class, () -> calc(teams, matches, deductions), label);
        }

        static Stream<Arguments> nullInputs() {
            return Stream.of(
                    Arguments.of("teams null", null, List.of(), List.of()),
                    Arguments.of("matches null", teams("A", "B"), null, List.of()),
                    Arguments.of("team element null", nullTeamList(), List.of(), List.of()),
                    Arguments.of("match element null", teams("A", "B"), nullMatchList(), List.of()),
                    Arguments.of("deduction element null", teams("A", "B"), List.of(), nullDeductionList()));
        }

        private static List<Team> nullTeamList() {
            List<Team> list = new ArrayList<>(teams("A"));
            list.add(null);
            return list;
        }

        private static List<MatchResult> nullMatchList() {
            List<MatchResult> list = new ArrayList<>();
            list.add(m("A", "B", 1, 0));
            list.add(null);
            return list;
        }

        private static List<PointDeduction> nullDeductionList() {
            List<PointDeduction> list = new ArrayList<>();
            list.add(null);
            return list;
        }

        // NEG: V-01, null puan silme listesi reddedilir (Q2)
        @Test
        @DisplayName("TC-041 null deductions list is rejected")
        void nullDeductionsList() {
            assertThrows(RuntimeException.class, () -> calc(teams("A", "B"), List.of(), null));
        }

        // NEG: V-02 + D-01; birebir ve trim sonrası aynı isim yinelenemez
        @Test
        @DisplayName("TC-042 duplicate teams are rejected (exact and after trim)")
        void duplicateTeams() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> calc(teams("A", "A"), List.of(), List.of())),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> calc(teams("Beşiktaş", " Beşiktaş "), List.of(), List.of())));
        }

        // EP: D-01 büyük/küçük harf duyarlı, farklı iki takım
        @Test
        @DisplayName("TC-043 names differing only in case are distinct teams")
        void caseSensitiveTeams() {
            List<StandingRow> table = calc(teams("Beşiktaş", "beşiktaş"), m("Beşiktaş", "beşiktaş", 1, 0));
            assertAll(
                    () -> assertEquals(3, row(table, "Beşiktaş").getPoints()),
                    () -> assertEquals(0, row(table, "beşiktaş").getPoints()));
        }

        // NEG: V-03 hem ev sahibi hem deplasman tarafı kontrol edilmeli; mesaj Z'yi içermeli (Q5)
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("unknownTeamMatches")
        @DisplayName("TC-044 match with unknown team is rejected on either side")
        void unknownTeamInMatch(String label, MatchResult match) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> calc(teams("A", "B"), match));
            assertTrue(ex.getMessage().contains("Z"), ex.getMessage());
        }

        static Stream<Arguments> unknownTeamMatches() {
            return Stream.of(Arguments.of("home unknown", m("Z", "A", 1, 0)),
                    Arguments.of("away unknown", m("A", "Z", 1, 0)));
        }

        // EP: D-01 boşluklu isimler kayıtlı takıma çözülmeli
        @Test
        @DisplayName("TC-045 padded names resolve to registered teams")
        void paddedNamesResolve() {
            List<StandingRow> table = calc(teams("Beşiktaş", "Göztepe"),
                    new MatchResult(new Team("Beşiktaş "), new Team(" Göztepe"), 2, 0));
            assertEquals(3, row(table, "Beşiktaş").getPoints());
        }

        // NEG: V-06 aynı yön iki kez; skor veya hükmen farkı etkilemez
        @Test
        @DisplayName("TC-046 same fixture twice is rejected regardless of score or forfeit")
        void duplicateFixture() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> calc(teams("A", "B"), m("A", "B", 1, 0), m("A", "B", 2, 2))),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> calc(teams("A", "B"), m("A", "B", 1, 0), m("A", "B", 0, 0, Forfeit.HOME))));
        }

        // BVA: iki maç, ters yönlerde = geçerli üst sınır
        @Test
        @DisplayName("TC-047 two matches in opposite directions are accepted")
        void twoMatchesAccepted() {
            List<StandingRow> table = calc(teams("A", "B"), m("A", "B", 1, 0), m("B", "A", 0, 0));
            assertAll(
                    () -> assertEquals(4, row(table, "A").getPoints()),
                    () -> assertEquals(1, row(table, "B").getPoints()));
        }

        // NEG + BVA: 3. maç ilk geçersiz sayı
        @Test
        @DisplayName("TC-048 third match between a pair is rejected")
        void thirdMatchRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> calc(teams("A", "B"), m("A", "B", 1, 0), m("B", "A", 0, 0), m("A", "B", 2, 0)));
        }
    }
}
