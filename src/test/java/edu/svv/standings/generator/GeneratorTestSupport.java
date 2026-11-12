package edu.svv.standings.generator;

import edu.svv.standings.StandingRow;
import edu.svv.standings.StandingsCalculator;
import edu.svv.standings.model.Forfeit;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Üreteç testleri için ortak yardımcılar: sezon kaynakları ve test tarafı (bağımsız) puan/gol oracle'ı. */
final class GeneratorTestSupport {

    /** Rastgele sezon sayısı (TC-100..TC-107 için tohum 1..N). */
    static final int RANDOM_SEASONS = 50;

    private GeneratorTestSupport() {
    }

    /** Parametreli testlerde görünen adı etiket olan sezon. */
    record SeasonCase(String label, GeneratedSeason season) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static List<SeasonCase> libraryCache;
    private static List<SeasonCase> randomCache;

    static synchronized List<SeasonCase> libraryCases() {
        if (libraryCache == null) {
            ScenarioLibrary library = new ScenarioLibrary();
            libraryCache = library.ids().stream()
                    .map(id -> new SeasonCase("library " + id, library.load(id)))
                    .toList();
        }
        return libraryCache;
    }

    /** Tohum 1..50; her üçüncü tohum sezon ortası (1..34 arası hafta), diğerleri tam sezon. */
    static synchronized List<SeasonCase> randomCases() {
        if (randomCache == null) {
            List<SeasonCase> list = new ArrayList<>();
            for (int seed = 1; seed <= RANDOM_SEASONS; seed++) {
                int rounds = seed % 3 == 0 ? (seed * 7) % 34 + 1 : SeasonGenerator.ROUNDS;
                list.add(new SeasonCase("random seed " + seed + ", rounds " + rounds,
                        new SeasonGenerator(seed).generate(rounds)));
            }
            randomCache = list;
        }
        return randomCache;
    }

    static Stream<SeasonCase> allCases() {
        return Stream.concat(libraryCases().stream(), randomCases().stream());
    }

    static Stream<SeasonCase> libraryStream() {
        return libraryCases().stream();
    }

    static List<StandingRow> table(GeneratedSeason s) {
        return new StandingsCalculator().calculate(s.teams(), s.matches(), s.deductions());
    }

    /** Takım başına beklenen istatistik: [played, won, drawn, lost, gf, ga, deducted]. */
    static Map<String, int[]> oracle(GeneratedSeason s) {
        Map<String, int[]> stats = new HashMap<>();
        s.teams().forEach(t -> stats.put(t.name(), new int[7]));
        for (MatchResult m : s.matches()) {
            int hg = m.homeGoals();
            int ag = m.awayGoals();
            // FMT 23/4: hükmen yenilenin golleri geçersiz, galibe en az 3 gol
            if (m.forfeit() == Forfeit.HOME) {
                hg = 0;
                ag = Math.max(3, ag);
            } else if (m.forfeit() == Forfeit.AWAY) {
                ag = 0;
                hg = Math.max(3, hg);
            }
            add(stats.get(m.homeTeam().name()), hg, ag);
            add(stats.get(m.awayTeam().name()), ag, hg);
        }
        for (PointDeduction d : s.deductions()) {
            stats.get(d.team().name())[6] += d.points();
        }
        return stats;
    }

    private static void add(int[] st, int scored, int conceded) {
        st[0]++;
        if (scored > conceded) {
            st[1]++;
        } else if (scored == conceded) {
            st[2]++;
        } else {
            st[3]++;
        }
        st[4] += scored;
        st[5] += conceded;
    }

    static int oraclePoints(int[] st) {
        return 3 * st[1] + st[2] - st[6];
    }

    static StandingRow row(List<StandingRow> table, String team) {
        return table.stream().filter(r -> r.getTeam().name().equals(team)).findFirst().orElseThrow();
    }

    static IntStream seeds(int from, int toInclusive) {
        return IntStream.rangeClosed(from, toInclusive);
    }
}
