package edu.svv.standings.generator;

import edu.svv.standings.StandingRow;
import edu.svv.standings.StandingsCalculator;
import edu.svv.standings.model.MatchResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Üretecin gerçekçilik kontrolü (yalnızca belgeleme / doğrulama; sonuçlar docs/generator.md'ye kopyalanır).
 * N tam sezon üretir, her kulübün ortalama simülasyon sırasını gerçek ortalama sıra / puanla yan yana koyar
 * ve güç sıralamasında son 5'teki bir kulübün şampiyon olma sıklığını sayar.
 *
 * Sıralama için StandingsCalculator yalnızca BURADA kullanılır; üreteç ve kütüphane sınıflandırıcısı onu çağırmaz.
 *
 * Çalıştırma: java -cp target/classes edu.svv.standings.generator.RealismCheck [sezon sayısı, varsayılan 200]
 */
public final class RealismCheck {

    public static void main(String[] args) {
        int seasons = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        StrengthModel model = StrengthModel.loadDefault();
        StandingsCalculator calculator = new StandingsCalculator();

        Map<String, Integer> positionSum = new HashMap<>();
        Map<String, Integer> pointsSum = new HashMap<>();
        Map<String, Integer> titles = new HashMap<>();
        Map<String, Integer> bottomThree = new HashMap<>();
        long goals = 0;
        long matches = 0;
        long homeGoals = 0;

        for (long seed = 1; seed <= seasons; seed++) {
            GeneratedSeason s = new SeasonGenerator(seed, model).generateFullSeason();
            for (MatchResult m : s.matches()) {
                goals += m.homeGoals() + m.awayGoals();
                homeGoals += m.homeGoals();
                matches++;
            }
            List<StandingRow> table = calculator.calculate(s.teams(), s.matches(), s.deductions());
            int n = table.size();
            for (int i = 0; i < n; i++) {
                StandingRow row = table.get(i);
                String club = row.getTeam().name();
                // çözülemeyen eşitlikte konum aynı olabilir; listede gerçek sıra (i+1) kullanılır
                positionSum.merge(club, i + 1, Integer::sum);
                pointsSum.merge(club, row.getPoints(), Integer::sum);
                if (i == 0) {
                    titles.merge(club, 1, Integer::sum);
                }
                if (i >= n - 3) {
                    bottomThree.merge(club, 1, Integer::sum);
                }
            }
        }

        // güç sıralaması: maç başına gol farkı (hücum − savunma), yüksekten düşüğe
        List<TeamStrength> byStrength = new ArrayList<>(model.strengths());
        byStrength.sort(Comparator.comparingDouble(TeamStrength::goalDifferencePerGame).reversed());

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "Seasons: %d, mu = %.3f goals per team per match, H = %.2f%n",
                seasons, model.leagueMeanGoals(), SeasonGenerator.HOME_ADVANTAGE));
        sb.append(String.format(Locale.ROOT,
                "Simulated goals per match: %.2f (real window: %.2f), home share of goals: %.1f%%%n%n",
                (double) goals / matches, 2 * model.leagueMeanGoals(), 100.0 * homeGoals / goals));
        sb.append("| Strength rank | Club | Real seasons | Attack | Defence | Real avg pos | Real PPG "
                + "| Sim avg pos | Sim PPG | Titles | Bottom 3 |\n");
        sb.append("|---|---|---|---|---|---|---|---|---|---|---|\n");
        int titlesBottomFive = 0;
        for (int i = 0; i < byStrength.size(); i++) {
            TeamStrength t = byStrength.get(i);
            String club = t.club();
            int played = 2 * (byStrength.size() - 1);
            String seasonsText = t.fromPromotedAverage() ? "0 (promoted avg)" : String.valueOf(t.seasons());
            int clubTitles = titles.getOrDefault(club, 0);
            if (i >= byStrength.size() - 5) {
                titlesBottomFive += clubTitles;
            }
            sb.append(String.format(Locale.ROOT, "| %d | %s | %s | %.2f | %.2f | %.1f | %.2f | %.1f | %.2f | %d | %d |%n",
                    i + 1, club, seasonsText, t.attack(), t.defence(), t.averagePosition(), t.pointsPerGame(),
                    (double) positionSum.getOrDefault(club, 0) / seasons,
                    (double) pointsSum.getOrDefault(club, 0) / seasons / played,
                    clubTitles, bottomThree.getOrDefault(club, 0)));
        }
        sb.append(String.format(Locale.ROOT, "%nTitles won by a bottom-5-strength club: %d of %d seasons (%.1f%%)%n",
                titlesBottomFive, seasons, 100.0 * titlesBottomFive / seasons));
        System.out.print(sb);
    }

    private RealismCheck() {
    }
}
