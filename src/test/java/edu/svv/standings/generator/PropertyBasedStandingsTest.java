package edu.svv.standings.generator;

import edu.svv.standings.StandingRow;
import edu.svv.standings.generator.GeneratorTestSupport.SeasonCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static edu.svv.standings.generator.GeneratorTestSupport.oracle;
import static edu.svv.standings.generator.GeneratorTestSupport.oraclePoints;
import static edu.svv.standings.generator.GeneratorTestSupport.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Özellik (invariant) testleri: 12 kütüphane senaryosu + 50 tohumlu rastgele sezon. */
class PropertyBasedStandingsTest {

    static Stream<SeasonCase> seasons() {
        return GeneratorTestSupport.allCases();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("seasons")
    @DisplayName("TC-100 – korunum: ΣG = ΣM, ΣB çift, ΣAtılan = ΣYenilen")
    void tc100_conservation(SeasonCase c) {
        // Her maçta bir G + bir M ya da iki B vardır; goller iki tarafta birer kez sayılır
        List<StandingRow> t = table(c.season());
        int won = t.stream().mapToInt(StandingRow::getWon).sum();
        int lost = t.stream().mapToInt(StandingRow::getLost).sum();
        int drawn = t.stream().mapToInt(StandingRow::getDrawn).sum();
        int gf = t.stream().mapToInt(StandingRow::getGoalsFor).sum();
        int ga = t.stream().mapToInt(StandingRow::getGoalsAgainst).sum();
        assertEquals(won, lost, "ΣW ≠ ΣL");
        assertEquals(0, drawn % 2, "ΣD tek: " + drawn);
        assertEquals(gf, ga, "ΣGF ≠ ΣGA");
        // maç sayısı ile de tutarlı: her maç 2 oynanmış satır üretir
        assertEquals(2 * c.season().matches().size(), t.stream().mapToInt(StandingRow::getPlayed).sum());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("seasons")
    @DisplayName("TC-101 – satır tutarlılığı: O = G+B+M, averaj, puan = 3G+B−silinen; değerler bağımsız oracle ile aynı")
    void tc101_rowConsistency(SeasonCase c) {
        // Beklenen değerler test içinde ham maç listesinden (FMT 23/4 dahil) yeniden hesaplanır
        Map<String, int[]> expected = oracle(c.season());
        for (StandingRow r : table(c.season())) {
            String name = r.getTeam().name();
            int[] e = expected.get(name);
            assertEquals(r.getWon() + r.getDrawn() + r.getLost(), r.getPlayed(), name + " oynanan");
            assertEquals(r.getGoalsFor() - r.getGoalsAgainst(), r.getGoalDifference(), name + " averaj");
            assertEquals(3 * r.getWon() + r.getDrawn() - r.getDeductedPoints(), r.getPoints(), name + " puan formülü");
            assertEquals(e[0], r.getPlayed(), name + " oynanan (oracle)");
            assertEquals(e[1], r.getWon(), name + " galibiyet (oracle)");
            assertEquals(e[2], r.getDrawn(), name + " beraberlik (oracle)");
            assertEquals(e[3], r.getLost(), name + " mağlubiyet (oracle)");
            assertEquals(e[4], r.getGoalsFor(), name + " atılan (oracle)");
            assertEquals(e[5], r.getGoalsAgainst(), name + " yenilen (oracle)");
            assertEquals(e[6], r.getDeductedPoints(), name + " silinen (oracle)");
            assertEquals(oraclePoints(e), r.getPoints(), name + " puan (oracle)");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("seasons")
    @DisplayName("TC-102 – sıralar tam 1..n ve puan aşağı doğru artmaz")
    void tc102_positionsAndOrder(SeasonCase c) {
        List<StandingRow> t = table(c.season());
        assertEquals(c.season().teams().size(), t.size());
        for (int i = 0; i < t.size(); i++) {
            assertEquals(i + 1, t.get(i).getPosition(), "sıra numarası");
            if (i > 0) {
                assertTrue(t.get(i - 1).getPoints() >= t.get(i).getPoints(),
                        "puan artıyor: " + t.get(i - 1) + " -> " + t.get(i));
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("seasons")
    @DisplayName("TC-103 – çözülemeyen takımlar eşit puanlı en az ikili gruplar halinde; benzersiz puanlı takım çözülemez olmaz")
    void tc103_unresolvedGroups(SeasonCase c) {
        List<StandingRow> t = table(c.season());
        Map<Integer, Integer> teamsPerPoints = new HashMap<>();
        t.forEach(r -> teamsPerPoints.merge(r.getPoints(), 1, Integer::sum));

        int i = 0;
        while (i < t.size()) {
            if (!t.get(i).isUnresolved()) {
                i++;
                continue;
            }
            // aynı puandaki ardışık çözülemez satırlar bir grup oluşturur
            int j = i;
            while (j < t.size() && t.get(j).isUnresolved() && t.get(j).getPoints() == t.get(i).getPoints()) {
                j++;
            }
            assertTrue(j - i >= 2, "tek başına çözülemez takım: " + t.get(i));
            assertTrue(teamsPerPoints.get(t.get(i).getPoints()) >= 2, "benzersiz puanlı çözülemez: " + t.get(i));
            i = j;
        }
    }
}
