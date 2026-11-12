package edu.svv.standings;

import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.Team;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Puanı eşit takımları FMT 9/2'ye göre sıralar (FR-09..FR-12).
 */
class TieBreaker {

    /** Eşit takımlar arasındaki maçlardan hesaplanan mini tablo değerleri. */
    private static final class MiniStats {
        int points;
        int goalsFor;
        int goalsAgainst;

        int goalDifference() {
            return goalsFor - goalsAgainst;
        }
    }

    /**
     * Puanı eşit bir grubu sıralar ve hiçbir kriterle ayrılamayanları işaretler.
     * Grup sadece bir kez ele alınır; alt eşitlikte mini tablo yeniden kurulmaz (FR-11).
     */
    List<StandingRow> order(List<StandingRow> tiedGroup, List<MatchResult> matches) {
        if (tiedGroup.size() < 2) {
            return tiedGroup;
        }

        Map<Team, MiniStats> mini = buildMiniTable(tiedGroup, matches);

        // FMT 9/2-b: mini puan → mini averaj → mini atılan gol → genel averaj → genel atılan gol → hükmen yenilgisi olmayan
        // İki takımda (9/2-a) mini tablo = ikili maçlar. İkili averaj eşitse iki takımın ikili attığı goller
        // de zorunlu olarak eşittir, bu yüzden aynı sıra 9/2-a ile aynı sonucu verir.
        Comparator<StandingRow> criteria = Comparator
                .comparingInt((StandingRow r) -> mini.get(r.getTeam()).points).reversed()
                .thenComparing(Comparator.comparingInt((StandingRow r) -> mini.get(r.getTeam()).goalDifference()).reversed())
                .thenComparing(Comparator.comparingInt((StandingRow r) -> mini.get(r.getTeam()).goalsFor).reversed())
                .thenComparing(Comparator.comparingInt(StandingRow::getGoalDifference).reversed())
                .thenComparing(Comparator.comparingInt(StandingRow::getGoalsFor).reversed())
                .thenComparing(StandingRow::hasForfeitLoss);

        List<StandingRow> ordered = new ArrayList<>(tiedGroup);
        // D-02: kriterlerle ayrılamayanlar sadece görüntü için alfabetik dizilir
        // DEF-001: String.compareTo kod noktası sırası kullanıyordu (Ç, İ, Ş, Ü Z'den sonra geliyordu);
        // Türkçe alfabe için tr-TR Collator kullanılır. FMT kriter karşılaştırıcısı (criteria) değişmedi.
        Collator turkish = Collator.getInstance(Locale.forLanguageTag("tr-TR"));
        ordered.sort(criteria.thenComparing(r -> r.getTeam().name(), turkish));

        // FR-12: komşusuyla tüm kriterlerde eşit olan takım "çözülemedi" olarak işaretlenir
        for (int i = 0; i < ordered.size() - 1; i++) {
            if (criteria.compare(ordered.get(i), ordered.get(i + 1)) == 0) {
                ordered.get(i).setUnresolved(true);
                ordered.get(i + 1).setUnresolved(true);
            }
        }
        return ordered;
    }

    private Map<Team, MiniStats> buildMiniTable(List<StandingRow> tiedGroup, List<MatchResult> matches) {
        Set<Team> tiedTeams = new HashSet<>();
        Map<Team, MiniStats> mini = new HashMap<>();
        for (StandingRow row : tiedGroup) {
            tiedTeams.add(row.getTeam());
            mini.put(row.getTeam(), new MiniStats());
        }

        for (MatchResult match : matches) {
            // sadece eşit takımların kendi aralarındaki maçlar sayılır
            if (!tiedTeams.contains(match.homeTeam()) || !tiedTeams.contains(match.awayTeam())) {
                continue;
            }
            // hükmen düzeltilmiş skor kullanılır (FR-06)
            int homeGoals = StandingsCalculator.adjustedHomeGoals(match);
            int awayGoals = StandingsCalculator.adjustedAwayGoals(match);

            MiniStats home = mini.get(match.homeTeam());
            MiniStats away = mini.get(match.awayTeam());
            home.goalsFor += homeGoals;
            home.goalsAgainst += awayGoals;
            away.goalsFor += awayGoals;
            away.goalsAgainst += homeGoals;

            if (homeGoals > awayGoals) {
                home.points += 3;
            } else if (homeGoals < awayGoals) {
                away.points += 3;
            } else {
                home.points++;
                away.points++;
            }
        }
        return mini;
    }
}
