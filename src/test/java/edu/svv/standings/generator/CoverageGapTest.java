package edu.svv.standings.generator;

import edu.svv.standings.io.CsvLoader;
import edu.svv.standings.model.Forfeit;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kapsam analizinden sonra eklenen testler (TC-148..TC-154).
 * GeneratedSeason.writeCsv hiç çalıştırılmamıştı; StrengthModel / SeasonGenerator'da hata dalları
 * ve bazı erişimciler kapsanmamıştı. Beklenenler docs/generator.md ve CSV biçiminden (FR-14) türetildi.
 */
class CoverageGapTest {

    @TempDir
    Path dir;

    private static final String HEADER = "season;club;position;played;won;drawn;lost;goalsFor;goalsAgainst;points";

    @Test
    @DisplayName("TC-148 – GeneratedSeason.writeCsv: yazılan CSV'ler CsvLoader ile okununca aynı sezonu verir (hükmen ve puan silme dahil)")
    void tc148_writeCsvRoundTrip() throws IOException {
        Team a = new Team("Çaykur Rizespor");
        Team b = new Team("Beşiktaş");
        GeneratedSeason season = new GeneratedSeason(
                List.of(a, b),
                List.of(new MatchResult(a, b, 2, 1), new MatchResult(b, a, 0, 3, Forfeit.HOME)),
                List.of(new PointDeduction(b, 3)),
                "round trip");
        Path out = dir.resolve("nested/season");
        season.writeCsv(out);

        CsvLoader loader = new CsvLoader();
        List<Team> teams = loader.loadTeams(out.resolve("teams.csv"));
        assertEquals(season.teams(), teams);
        assertEquals(season.matches(), loader.loadMatches(out.resolve("matches.csv"), teams));
        assertEquals(season.deductions(), loader.loadDeductions(out.resolve("deductions.csv"), teams));
    }

    @Test
    @DisplayName("TC-149 – GeneratedSeason.writeCsv: puan silme yoksa deductions.csv boş dosya olur (sınıf belgesi: boşsa dosya boş kalır)")
    void tc149_writeCsvEmptyDeductions() throws IOException {
        Team a = new Team("A");
        Team b = new Team("B");
        GeneratedSeason season = new GeneratedSeason(List.of(a, b), List.of(new MatchResult(a, b, 1, 1)), List.of(), "x");
        season.writeCsv(dir);
        assertEquals(0, Files.size(dir.resolve("deductions.csv")));
        assertTrue(Files.size(dir.resolve("matches.csv")) > 0);
    }

    @Test
    @DisplayName("TC-150 – StrengthModel.fromLines: sezon ve kulüp listesindeki boş satırlar atlanır")
    void tc150_blankLinesIgnored() {
        List<String> seasons = List.of(HEADER,
                "2016-17;A;1;10;5;3;2;20;10;18",
                "",
                "2016-17;B;2;10;4;3;3;15;12;15",
                "2017-18;A;1;10;6;2;2;22;9;20",
                "   ",
                "2017-18;C;2;10;3;3;4;12;14;12");
        StrengthModel model = StrengthModel.fromLines(seasons, List.of("A", "", "C", "  "));
        assertEquals(2, model.strengths().size());
        assertEquals(42.0 / 20.0, model.strengthOf("A").attack(), 1e-12);
    }

    @Test
    @DisplayName("TC-151 – StrengthModel.fromLines: yanlış alan sayısı ya da tamsayı olmayan değer IllegalArgumentException verir")
    void tc151_malformedSeasonLines() {
        List<String> clubs = List.of("A");
        assertThrows(IllegalArgumentException.class, () -> StrengthModel.fromLines(
                List.of(HEADER, "2016-17;A;1;10;5;3;2;20;10"), clubs));
        assertThrows(IllegalArgumentException.class, () -> StrengthModel.fromLines(
                List.of(HEADER, "2016-17;A;1;ten;5;3;2;20;10;18"), clubs));
    }

    @Test
    @DisplayName("TC-152 – StrengthModel.fromLines: tek sezonluk veri (yeni çıkan takım yok) ve boş kulüp listesi reddedilir")
    void tc152_noPromotedAndEmptyClubList() {
        // generator.md: yeni çıkan ortalaması t >= 2. sezon gerektirir; tek sezonda tanımlanamaz
        assertThrows(IllegalArgumentException.class, () -> StrengthModel.fromLines(
                List.of(HEADER, "2016-17;A;1;10;5;3;2;20;10;18"), List.of("A")));
        List<String> twoSeasons = List.of(HEADER,
                "2016-17;A;1;10;5;3;2;20;10;18",
                "2017-18;A;1;10;5;3;2;20;10;18",
                "2017-18;B;2;10;5;3;2;20;10;18");
        assertThrows(IllegalArgumentException.class, () -> StrengthModel.fromLines(twoSeasons, List.of("", " ")));
    }

    @Test
    @DisplayName("TC-153 – StrengthModel.promotedAverage: yeni çıkan takım satırlarının havuzlanmış ortalamasını verir")
    void tc153_promotedAverageAccessor() {
        List<String> seasons = List.of(HEADER,
                "2016-17;A;1;10;5;3;2;20;10;18",
                "2017-18;A;1;10;6;2;2;22;9;20",
                "2017-18;C;2;10;3;3;4;12;14;12");
        StrengthModel model = StrengthModel.fromLines(seasons, List.of("A", "D"));
        // yeni çıkan: yalnızca C'nin 2017-18 satırı => hücum 12/10, savunma 14/10
        assertEquals(1.2, model.promotedAverage().attack(), 1e-12);
        assertEquals(1.4, model.promotedAverage().defence(), 1e-12);
        // geçmişi olmayan D bu değeri alır
        assertTrue(model.strengthOf("D").fromPromotedAverage());
        assertEquals(1.2, model.strengthOf("D").attack(), 1e-12);
    }

    @Test
    @DisplayName("TC-154 – SeasonGenerator.seed() verilen seed'i döndürür; doubleRoundRobin 0, 1, 3, 17 takımı reddeder, 2 takımı kabul eder")
    void tc154_seedAccessorAndRoundRobinBoundaries() {
        assertEquals(12345L, new SeasonGenerator(12345L, StrengthModel.loadDefault()).seed());

        for (int n : new int[] {0, 1, 3, 17}) {
            List<Team> teams = java.util.stream.IntStream.range(0, n).mapToObj(i -> new Team("T" + i)).toList();
            assertThrows(IllegalArgumentException.class, () -> SeasonGenerator.doubleRoundRobin(teams), "n=" + n);
        }
        // alt sınır: 2 takım => 2 hafta, her hafta 1 maç, ev/deplasman değişir
        var rounds = SeasonGenerator.doubleRoundRobin(List.of(new Team("A"), new Team("B")));
        assertEquals(2, rounds.size());
        assertEquals(1, rounds.get(0).size());
        assertEquals(rounds.get(0).get(0).home(), rounds.get(1).get(0).away());
    }
}
