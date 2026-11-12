package edu.svv.standings;

import edu.svv.standings.model.Forfeit;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.Team;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Eşitlik bozma testleri için ortak (test kapsamlı) yardımcı: takım/maç üretimi ve tablo doğrulaması.
 */
final class TieFixtures {

    private TieFixtures() {
    }

    /** Takım listesini verilen sırayla üretir. */
    static List<Team> teams(String... names) {
        List<Team> result = new ArrayList<>();
        for (String name : names) {
            result.add(new Team(name));
        }
        return result;
    }

    /** Hükmensiz maç: ev sahibi, deplasman, ev golü, deplasman golü. */
    static MatchResult m(String home, String away, int homeGoals, int awayGoals) {
        return new MatchResult(new Team(home), new Team(away), homeGoals, awayGoals, Forfeit.NONE);
    }

    /** Hükmen sonuçlu maç. */
    static MatchResult m(String home, String away, int homeGoals, int awayGoals, Forfeit forfeit) {
        return new MatchResult(new Team(home), new Team(away), homeGoals, awayGoals, forfeit);
    }

    static List<StandingRow> calculate(List<Team> teams, MatchResult... matches) {
        return new StandingsCalculator().calculate(teams, List.of(matches), List.of());
    }

    /**
     * Tabloyu sıralı olarak doğrular. Beklenen ad sonuna "!" konulursa o takım "çözülemedi" bayraklı olmalıdır.
     * Pozisyonlar ardışık 1..n olmalıdır (D-02).
     */
    static void assertTable(List<StandingRow> table, String... expected) {
        List<String> actual = new ArrayList<>();
        for (StandingRow row : table) {
            actual.add(row.getTeam().name() + (row.isUnresolved() ? "!" : ""));
        }
        assertEquals(List.of(expected), actual, "sıra / çözülemedi bayrağı");
        for (int i = 0; i < table.size(); i++) {
            assertEquals(i + 1, table.get(i).getPosition(), "pozisyon " + table.get(i).getTeam().name());
        }
    }
}
