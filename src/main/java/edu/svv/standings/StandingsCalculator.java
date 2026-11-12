package edu.svv.standings;

import edu.svv.standings.model.Forfeit;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Takımlar, maç sonuçları ve puan silmelerinden puan tablosunu üretir.
 */
public class StandingsCalculator {

    private final TieBreaker tieBreaker = new TieBreaker();

    public List<StandingRow> calculate(List<Team> teams, List<MatchResult> matches, List<PointDeduction> deductions) {
        // V-01: listeler null olamaz
        if (teams == null || matches == null || deductions == null) {
            throw new IllegalArgumentException("Teams, matches and deductions must not be null");
        }

        Map<Team, StandingRow> rows = createRows(teams);
        validateMatches(matches, rows);

        for (MatchResult match : matches) {
            applyMatch(match, rows);
        }

        for (PointDeduction deduction : deductions) {
            if (deduction == null) {
                throw new IllegalArgumentException("Deduction must not be null");
            }
            StandingRow row = rows.get(deduction.team());
            // V-07: kayıtlı olmayan takıma puan silme yapılamaz
            if (row == null) {
                throw new IllegalArgumentException("Unknown team in deduction: " + deduction.team().name());
            }
            row.addDeduction(deduction.points());
        }

        List<StandingRow> sorted = new ArrayList<>(rows.values());
        // FR-08: önce puana göre azalan sıralama
        sorted.sort(Comparator.comparingInt(StandingRow::getPoints).reversed());

        // FR-09..FR-12: aynı puandaki ardışık takımlar grup olarak eşitlik bozmaya gönderilir
        List<StandingRow> table = new ArrayList<>();
        int start = 0;
        while (start < sorted.size()) {
            int end = start + 1;
            while (end < sorted.size() && sorted.get(end).getPoints() == sorted.get(start).getPoints()) {
                end++;
            }
            table.addAll(tieBreaker.order(sorted.subList(start, end), matches));
            start = end;
        }

        for (int i = 0; i < table.size(); i++) {
            table.get(i).setPosition(i + 1);
        }
        return table;
    }

    private Map<Team, StandingRow> createRows(List<Team> teams) {
        Map<Team, StandingRow> rows = new LinkedHashMap<>();
        for (Team team : teams) {
            if (team == null) {
                throw new IllegalArgumentException("Team must not be null");
            }
            // V-02: aynı isimli takım iki kez eklenemez
            if (rows.containsKey(team)) {
                throw new IllegalArgumentException("Duplicate team: " + team.name());
            }
            rows.put(team, new StandingRow(team));
        }
        return rows;
    }

    private void validateMatches(List<MatchResult> matches, Map<Team, StandingRow> rows) {
        Set<String> fixtures = new HashSet<>();
        Map<String, Integer> pairCounts = new HashMap<>();

        for (MatchResult match : matches) {
            if (match == null) {
                throw new IllegalArgumentException("Match must not be null");
            }
            // V-03: maçtaki takımlar kayıtlı olmalı
            if (!rows.containsKey(match.homeTeam()) || !rows.containsKey(match.awayTeam())) {
                throw new IllegalArgumentException("Match references an unknown team: "
                        + match.homeTeam().name() + " - " + match.awayTeam().name());
            }

            // V-06: aynı ev sahibi/deplasman eşleşmesi bir kez oynanır
            String fixture = match.homeTeam().name() + "|" + match.awayTeam().name();
            if (!fixtures.add(fixture)) {
                throw new IllegalArgumentException("Duplicate fixture: " + fixture);
            }

            // V-06: iki takım arasında en fazla iki maç olabilir
            String pair = pairKey(match.homeTeam(), match.awayTeam());
            int count = pairCounts.getOrDefault(pair, 0) + 1;
            if (count > 2) {
                throw new IllegalArgumentException("More than two matches between " + pair);
            }
            pairCounts.put(pair, count);
        }
    }

    private String pairKey(Team a, Team b) {
        // eşleşme yönünden bağımsız anahtar: isimler alfabetik sırayla
        if (a.name().compareTo(b.name()) < 0) {
            return a.name() + "|" + b.name();
        }
        return b.name() + "|" + a.name();
    }

    private void applyMatch(MatchResult match, Map<Team, StandingRow> rows) {
        int homeGoals = adjustedHomeGoals(match);
        int awayGoals = adjustedAwayGoals(match);

        rows.get(match.homeTeam()).addMatch(homeGoals, awayGoals, match.forfeit() == Forfeit.HOME);
        rows.get(match.awayTeam()).addMatch(awayGoals, homeGoals, match.forfeit() == Forfeit.AWAY);
    }

    // FR-06 / FMT 23/4: hükmen yenilen tarafın golleri geçersiz, galibe en az 3 gol
    static int adjustedHomeGoals(MatchResult match) {
        if (match.forfeit() == Forfeit.HOME) {
            return 0;
        }
        if (match.forfeit() == Forfeit.AWAY) {
            return Math.max(3, match.homeGoals());
        }
        return match.homeGoals();
    }

    static int adjustedAwayGoals(MatchResult match) {
        if (match.forfeit() == Forfeit.AWAY) {
            return 0;
        }
        if (match.forfeit() == Forfeit.HOME) {
            return Math.max(3, match.awayGoals());
        }
        return match.awayGoals();
    }
}
