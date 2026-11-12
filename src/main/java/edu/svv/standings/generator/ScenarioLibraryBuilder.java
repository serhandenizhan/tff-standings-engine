package edu.svv.standings.generator;

import edu.svv.standings.model.Forfeit;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Senaryo kütüphanesini oluşturur: seed'leri ve hafta sayılarını tarar, her sezonu küçük ve bağımsız bir
 * sınıflandırıcıyla etiketler, çeşitliliği sağlayan 12 sezonu seçip CSV olarak yazar.
 *
 * Çalıştırma (proje kökünden, önce mvn package):
 * <pre>java -cp target/classes edu.svv.standings.generator.ScenarioLibraryBuilder [çıktı klasörü]</pre>
 * Varsayılan çıktı: src/main/resources/scenarios
 *
 * Sınıflandırıcı StandingsCalculator / TieBreaker'ı BİLEREK çağırmaz (test edilen kodla kütüphane seçimi
 * birbirine bağlanmasın). Yalnızca ham puanı (3G + B − silinen) ve kaba bayrakları hesaplar.
 */
public final class ScenarioLibraryBuilder {

    // --- bayraklar (katalogdaki yazımlarıyla) ---
    public static final String NO_TIE = "NO_TIE";
    public static final String TIE_2 = "TIE_2";
    public static final String TIE_3PLUS = "TIE_3PLUS";
    public static final String FORFEIT = "FORFEIT";
    public static final String DEDUCTION = "DEDUCTION";
    public static final String LIKELY_UNRESOLVED = "LIKELY_UNRESOLVED";

    /** Taranacak en büyük seed (1..MAX_SEED). */
    private static final int MAX_SEED = 20_000;

    /**
     * Seçim kuralı: her senaryo için izin verilen hafta sayıları, zorunlu ve yasak bayraklar.
     * Seed'ler 1'den başlayarak taranır, ilk uyan ve daha önce kullanılmamış seed alınır (deterministik).
     */
    private record Spec(String id, int[] rounds, Set<String> required, Set<String> forbidden, String purpose) {
    }

    private static final List<Spec> SPECS = List.of(
            new Spec("S01", new int[] {34}, Set.of(NO_TIE), Set.of(FORFEIT, DEDUCTION),
                    "full season, no points tie at all"),
            new Spec("S02", new int[] {34}, Set.of(TIE_2), Set.of(TIE_3PLUS, FORFEIT, DEDUCTION),
                    "full season, only 2-team ties"),
            new Spec("S03", new int[] {34}, Set.of(TIE_3PLUS), Set.of(DEDUCTION),
                    "full season with a 3+-team tie"),
            new Spec("S04", new int[] {34}, Set.of(TIE_2, TIE_3PLUS, FORFEIT, DEDUCTION, LIKELY_UNRESOLVED), Set.of(),
                    "full season containing everything"),
            new Spec("S05", new int[] {34}, Set.of(FORFEIT, DEDUCTION), Set.of(TIE_3PLUS),
                    "full season, forfeit + deduction, no 3+-team tie"),
            new Spec("S06", new int[] {1}, Set.of(LIKELY_UNRESOLVED), Set.of(),
                    "round 1, likely fully unresolved group"),
            new Spec("S07", new int[] {2}, Set.of(TIE_3PLUS, FORFEIT), Set.of(),
                    "round 2, early table with a forfeit"),
            new Spec("S08", new int[] {3}, Set.of(LIKELY_UNRESOLVED, DEDUCTION), Set.of(),
                    "round 3, likely unresolved group and an early deduction"),
            new Spec("S09", new int[] {9, 10, 11, 12}, Set.of(TIE_2), Set.of(TIE_3PLUS),
                    "first third, only 2-team ties"),
            new Spec("S10", new int[] {17}, Set.of(TIE_3PLUS, FORFEIT, DEDUCTION), Set.of(),
                    "half season (end of first half), forfeit + deduction"),
            new Spec("S11", new int[] {25, 26, 27}, Set.of(LIKELY_UNRESOLVED), Set.of(),
                    "late mid-season, likely unresolved group"),
            new Spec("S12", new int[] {30, 31, 32, 33}, Set.of(TIE_2, TIE_3PLUS, FORFEIT), Set.of(DEDUCTION),
                    "run-in, several ties and a forfeit"));

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "src/main/resources/scenarios");
        StrengthModel model = StrengthModel.loadDefault();

        Set<Long> usedSeeds = new HashSet<>();
        List<String> catalogue = new ArrayList<>();
        catalogue.add("id;seed;rounds;flags;description");
        StringBuilder table = new StringBuilder();
        table.append("| ID | Seed | Rounds | Flags | Description |\n|---|---|---|---|---|\n");

        for (Spec spec : SPECS) {
            Selection found = search(spec, model, usedSeeds);
            if (found == null) {
                throw new IllegalStateException("No season found for " + spec.id() + " (" + spec.purpose()
                        + ") within seeds 1.." + MAX_SEED);
            }
            usedSeeds.add(found.seed());
            Classification c = found.classification();
            // açıklamada ";" olamaz (katalog ayırıcısı)
            String description = spec.purpose() + " - " + c.summary();
            found.season().writeCsv(out.resolve(spec.id()));
            String flags = String.join(",", c.flags());
            catalogue.add(spec.id() + ";" + found.seed() + ";" + found.rounds() + ";" + flags + ";" + description);
            table.append("| ").append(spec.id()).append(" | ").append(found.seed()).append(" | ")
                    .append(found.rounds()).append(" | ").append(flags.replace(",", ", ")).append(" | ")
                    .append(description).append(" |\n");
        }

        Files.createDirectories(out);
        Files.writeString(out.resolve("catalogue.csv"), String.join("\n", catalogue) + "\n", StandardCharsets.UTF_8);
        System.out.println("Wrote " + SPECS.size() + " scenarios to " + out.toAbsolutePath());
        System.out.print(table);
    }

    private record Selection(long seed, int rounds, GeneratedSeason season, Classification classification) {
    }

    private static Selection search(Spec spec, StrengthModel model, Set<Long> usedSeeds) {
        for (long seed = 1; seed <= MAX_SEED; seed++) {
            if (usedSeeds.contains(seed)) {
                continue;
            }
            SeasonGenerator generator = new SeasonGenerator(seed, model);
            for (int rounds : spec.rounds()) {
                GeneratedSeason season = generator.generate(rounds);
                Classification c = classify(season);
                if (c.flags().containsAll(spec.required())
                        && spec.forbidden().stream().noneMatch(c.flags()::contains)) {
                    return new Selection(seed, rounds, season, c);
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ bağımsız sınıflandırıcı

    /** Sınıflandırma sonucu: bayraklar ve okunabilir özet. */
    record Classification(Set<String> flags, String summary) {
    }

    // takım başına ham istatistik
    private static final class Raw {
        int points;
        int goalsFor;
        int goalsAgainst;
        boolean forfeitLoss;
    }

    /**
     * Kaba sınıflandırma:
     * - Ham puan = 3G + B − silinen. Hükmen maçta hükmen sayılan taraf yenik, diğeri galip sayılır
     *   ve gol FMT 23/4'e göre düzeltilir (galibe 3, galip 3'ten fazla attıysa kendi golü; yenilenin golü 0).
     *   Bu kısa mantık, puanın doğru olması için hesaplayıcıdan bağımsız olarak burada yeniden yazıldı.
     * - TIE_2 / TIE_3PLUS: aynı ham puanda tam 2 / en az 3 takım olan grup var. NO_TIE: hiç böyle grup yok.
     * - LIKELY_UNRESOLVED: bir puan grubunun TÜM üyeleri genel averaj, atılan gol ve hükmen yenilgi durumunda
     *   aynı ve grup içi maçlardan oluşan mini tabloda (puan, averaj, atılan gol) da aynı. Grup içi maç hiç
     *   yoksa mini tablo herkes için 0'dır. Bu durumda FMT 9/2'deki hiçbir hesaplanabilir kriter grubu ayıramaz.
     *   Kısmi çözülemezlik (grubun yalnızca bir alt kümesi) bilerek aranmaz; bayrak "muhtemel" olarak adlandırıldı.
     */
    static Classification classify(GeneratedSeason season) {
        Map<Team, Raw> raw = new LinkedHashMap<>();
        for (Team t : season.teams()) {
            raw.put(t, new Raw());
        }
        int forfeits = 0;
        for (MatchResult m : season.matches()) {
            int[] g = adjustedGoals(m);
            if (m.forfeit() != Forfeit.NONE) {
                forfeits++;
                raw.get(m.forfeit() == Forfeit.HOME ? m.homeTeam() : m.awayTeam()).forfeitLoss = true;
            }
            addResult(raw.get(m.homeTeam()), g[0], g[1]);
            addResult(raw.get(m.awayTeam()), g[1], g[0]);
        }
        for (PointDeduction d : season.deductions()) {
            raw.get(d.team()).points -= d.points();
        }

        // ham puana göre gruplar (yüksekten düşüğe)
        Map<Integer, List<Team>> groups = new TreeMap<>(Comparator.reverseOrder());
        for (Map.Entry<Team, Raw> e : raw.entrySet()) {
            groups.computeIfAbsent(e.getValue().points, k -> new ArrayList<>()).add(e.getKey());
        }

        int twoGroups = 0;
        int bigGroups = 0;
        int largest = 1;
        List<String> unresolved = new ArrayList<>();
        for (Map.Entry<Integer, List<Team>> e : groups.entrySet()) {
            List<Team> group = e.getValue();
            if (group.size() < 2) {
                continue;
            }
            largest = Math.max(largest, group.size());
            if (group.size() == 2) {
                twoGroups++;
            } else {
                bigGroups++;
            }
            if (isLikelyUnresolved(group, raw, season.matches())) {
                unresolved.add(group.size() + " teams on " + e.getKey() + " pts");
            }
        }

        Set<String> flags = new LinkedHashSet<>();
        if (twoGroups == 0 && bigGroups == 0) {
            flags.add(NO_TIE);
        }
        if (twoGroups > 0) {
            flags.add(TIE_2);
        }
        if (bigGroups > 0) {
            flags.add(TIE_3PLUS);
        }
        if (forfeits > 0) {
            flags.add(FORFEIT);
        }
        if (!season.deductions().isEmpty()) {
            flags.add(DEDUCTION);
        }
        if (!unresolved.isEmpty()) {
            flags.add(LIKELY_UNRESOLVED);
        }

        StringBuilder s = new StringBuilder();
        s.append(season.matches().size()).append(" matches, ")
                .append(twoGroups).append(" two-team tie(s), ")
                .append(bigGroups).append(" 3+-team tie(s)");
        if (bigGroups > 0) {
            s.append(" (largest ").append(largest).append(")");
        }
        s.append(", ").append(forfeits).append(" forfeit(s), ")
                .append(season.deductions().size()).append(" deduction(s)");
        if (!unresolved.isEmpty()) {
            s.append(", likely unresolved: ").append(String.join(" / ", unresolved));
        }
        return new Classification(flags, s.toString());
    }

    private static boolean isLikelyUnresolved(List<Team> group, Map<Team, Raw> raw, List<MatchResult> matches) {
        Raw first = raw.get(group.get(0));
        for (Team t : group) {
            Raw r = raw.get(t);
            if (r.goalsFor - r.goalsAgainst != first.goalsFor - first.goalsAgainst
                    || r.goalsFor != first.goalsFor || r.forfeitLoss != first.forfeitLoss) {
                return false;
            }
        }
        // grup içi mini tablo
        Set<Team> members = new HashSet<>(group);
        Map<Team, Raw> mini = new LinkedHashMap<>();
        for (Team t : group) {
            mini.put(t, new Raw());
        }
        for (MatchResult m : matches) {
            if (members.contains(m.homeTeam()) && members.contains(m.awayTeam())) {
                int[] g = adjustedGoals(m);
                addResult(mini.get(m.homeTeam()), g[0], g[1]);
                addResult(mini.get(m.awayTeam()), g[1], g[0]);
            }
        }
        Raw m0 = mini.get(group.get(0));
        for (Raw r : mini.values()) {
            if (r.points != m0.points || r.goalsFor != m0.goalsFor || r.goalsAgainst != m0.goalsAgainst) {
                return false;
            }
        }
        return true;
    }

    // FMT 23/4: hükmen galibe 3 gol (kendi golü 3'ten fazlaysa o geçerli), yenilenin golleri geçersiz
    private static int[] adjustedGoals(MatchResult m) {
        return switch (m.forfeit()) {
            case NONE -> new int[] {m.homeGoals(), m.awayGoals()};
            case HOME -> new int[] {0, Math.max(3, m.awayGoals())};
            case AWAY -> new int[] {Math.max(3, m.homeGoals()), 0};
        };
    }

    private static void addResult(Raw r, int scored, int conceded) {
        r.goalsFor += scored;
        r.goalsAgainst += conceded;
        if (scored > conceded) {
            r.points += 3;
        } else if (scored == conceded) {
            r.points += 1;
        }
    }
}
