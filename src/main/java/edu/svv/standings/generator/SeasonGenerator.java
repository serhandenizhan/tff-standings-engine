package edu.svv.standings.generator;

import edu.svv.standings.model.Forfeit;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Gerçekçi rastgele Süper Lig sezonu üreteci (2026-27 kulüpleri, çift devreli lig, 34 hafta).
 *
 * Eşitlik garanti edilmez ya da kurgulanmaz; sezonlar yalnızca güç modeli ve rastlantı ile oluşur.
 * Yalnızca seed ile kurulan java.util.Random kullanılır: aynı seed (ve aynı güç verisi) her zaman aynı sezonu verir.
 * Üreteç test edilen koda (StandingsCalculator, TieBreaker) bağımlı değildir.
 *
 * Modelleme kararları (ayrıntı ve gerekçeler: docs/generator.md):
 * - Gol modeli: bağımsız Poisson. λ_ev = μ·(hücum_ev/μ)·(savunma_dep/μ)·H, λ_dep = μ·(hücum_dep/μ)·(savunma_ev/μ)/H.
 * - Fikstür: daire (circle) yöntemi; ilk devre 17 hafta, ikinci devre aynı eşleşmeler ev/deplasman yer değiştirmiş.
 * - Sezon ortası tablo = ilk k hafta. Sezon her zaman 34 hafta üretilip kesilir; böylece generate(k),
 *   aynı seed'li generate(34)'ün ilk k haftasıdır ve rastgele sayı tüketimi k'dan bağımsızdır.
 * - Hükmen: her maç için %1 olasılık, taraf yazı-tura. Sahadaki skor saklanır, FMT 23/4 düzeltmesini hesaplayıcı yapar.
 * - Puan silme: her kulüp için sezon başına %2 olasılık, 3 puan; silmenin geldiği hafta 1..34 arasında düzgün dağılır,
 *   sezon ortası tabloda yalnızca o haftaya kadar gelen silmeler yer alır.
 */
public final class SeasonGenerator {

    /** Çift devreli 18 takımlı ligde hafta sayısı. */
    public static final int ROUNDS = 34;

    /**
     * Ev sahibi avantajı çarpanı H. Eşit güçte iki takımda λ_ev/λ_dep = H² ≈ 1,21, yani ev sahibi
     * gollerin yaklaşık %55'ini atar. Varsayım: avrupa liglerinde ve Süper Lig'de yaygın görülen ev payı (~%54–56);
     * kullanılan CSV'de iç saha / deplasman ayrımı olmadığı için veriden ölçülmedi.
     */
    public static final double HOME_ADVANTAGE = 1.10;

    /** Maç başına hükmen olasılığı (kullanıcı kararı: ~%1). */
    public static final double FORFEIT_PROBABILITY = 0.01;

    /** Kulüp başına sezonluk puan silme olasılığı (kullanıcı kararı: nadir, ~%2). */
    public static final double DEDUCTION_PROBABILITY = 0.02;

    /** Bir puan silmede silinen puan (FMT 24/1-a'daki galibiyet puanı kadar). */
    public static final int DEDUCTION_POINTS = 3;

    /** Fikstürdeki tek bir maç (ev sahibi, deplasman). */
    public record Fixture(Team home, Team away) {
    }

    private final long seed;
    private final StrengthModel model;

    public SeasonGenerator(long seed) {
        this(seed, StrengthModel.loadDefault());
    }

    public SeasonGenerator(long seed, StrengthModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Strength model must not be null");
        }
        this.seed = seed;
        this.model = model;
    }

    public long seed() {
        return seed;
    }

    /** Tam sezon (34 hafta). */
    public GeneratedSeason generateFullSeason() {
        return generate(ROUNDS);
    }

    /**
     * İlk {@code rounds} haftası oynanmış sezonu üretir.
     * Her çağrı yeni bir Random(seed) ile başlar; aynı nesnede tekrar çağırmak aynı sonucu verir.
     *
     * @param rounds 1..34
     */
    public GeneratedSeason generate(int rounds) {
        List<TeamStrength> strengths = model.strengths();
        int totalRounds = 2 * (strengths.size() - 1);
        if (rounds < 1 || rounds > totalRounds) {
            throw new IllegalArgumentException("rounds must be between 1 and " + totalRounds + ": " + rounds);
        }
        Random random = new Random(seed);

        // takım listesi kulüp dosyasının sırasıyla; fikstür sırası ise seed'e göre karıştırılır
        List<Team> teams = strengths.stream().map(s -> new Team(s.club())).toList();
        List<Team> drawOrder = new ArrayList<>(teams);
        Collections.shuffle(drawOrder, random);
        List<List<Fixture>> schedule = doubleRoundRobin(drawOrder);

        // tüm 34 hafta üretilir, sonra kesilir (rastgele sayı tüketimi rounds'tan bağımsız)
        List<MatchResult> matches = new ArrayList<>();
        int forfeits = 0;
        for (int r = 0; r < schedule.size(); r++) {
            for (Fixture f : schedule.get(r)) {
                MatchResult m = playMatch(f, random);
                if (r < rounds) {
                    matches.add(m);
                    if (m.forfeit() != Forfeit.NONE) {
                        forfeits++;
                    }
                }
            }
        }

        List<PointDeduction> deductions = new ArrayList<>();
        for (Team t : teams) {
            boolean deducted = random.nextDouble() < DEDUCTION_PROBABILITY;
            // silmenin geldiği hafta her durumda çekilir ki sonraki takımların rastgele sayıları kaymasın
            int round = random.nextInt(totalRounds) + 1;
            if (deducted && round <= rounds) {
                deductions.add(new PointDeduction(t, DEDUCTION_POINTS));
            }
        }

        String description = "Random season, seed " + seed + ", " + rounds + "/" + totalRounds + " rounds, "
                + matches.size() + " matches, " + forfeits + " forfeit(s), " + deductions.size() + " deduction(s)";
        return new GeneratedSeason(teams, List.copyOf(matches), List.copyOf(deductions), description);
    }

    /**
     * Daire yöntemiyle çift devreli fikstür. İlk takım sabit kalır, diğerleri her hafta bir adım döner.
     * İlk devrede her çift bir kez karşılaşır; ikinci devre aynı haftalar ev/deplasman değiştirilmiş hâlidir.
     * Ev sahibi seçimi konum paritesiyle yapılır: ilk devrede her takımın iç saha sayısı dengelidir (DEF-005); "break" optimizasyonu yok.
     *
     * @param order takımlar (çift sayıda); sıra fikstürü belirler
     * @return 2·(n−1) hafta, her hafta n/2 maç
     */
    public static List<List<Fixture>> doubleRoundRobin(List<Team> order) {
        int n = order.size();
        if (n < 2 || n % 2 != 0) {
            throw new IllegalArgumentException("An even number of at least 2 teams is required: " + n);
        }
        List<Team> ring = new ArrayList<>(order);
        List<List<Fixture>> firstHalf = new ArrayList<>();
        for (int r = 0; r < n - 1; r++) {
            List<Fixture> round = new ArrayList<>();
            for (int i = 0; i < n / 2; i++) {
                Team a = ring.get(i);
                Team b = ring.get(n - 1 - i);
                // sabit takım haftadan haftaya ev/deplasman değiştirir.
                // DEF-005: eski kural (hafta + konum paritesi) dönen halkada bir takımı ilk devrede hep deplasmana
                // bırakıyordu (0..16 iç saha). Konum paritesi tek başına kullanılınca her takım ⌊(n−1)/2⌋..⌈(n−1)/2⌉ iç saha oynar.
                boolean aHome = (i == 0) ? (r % 2 == 0) : (i % 2 == 1);
                round.add(aHome ? new Fixture(a, b) : new Fixture(b, a));
            }
            firstHalf.add(round);
            // döndürme: son eleman 1. konuma geçer, 0. konum sabit
            ring.add(1, ring.remove(n - 1));
        }
        List<List<Fixture>> all = new ArrayList<>(firstHalf);
        for (List<Fixture> round : firstHalf) {
            all.add(round.stream().map(f -> new Fixture(f.away(), f.home())).toList());
        }
        return all;
    }

    private MatchResult playMatch(Fixture f, Random random) {
        double mu = model.leagueMeanGoals();
        TeamStrength home = model.strengthOf(f.home().name());
        TeamStrength away = model.strengthOf(f.away().name());
        // μ·(hücum/μ)·(savunma/μ) = hücum·savunma/μ
        double lambdaHome = home.attack() * away.defence() / mu * HOME_ADVANTAGE;
        double lambdaAway = away.attack() * home.defence() / mu / HOME_ADVANTAGE;
        int homeGoals = poisson(lambdaHome, random);
        int awayGoals = poisson(lambdaAway, random);

        Forfeit forfeit = Forfeit.NONE;
        if (random.nextDouble() < FORFEIT_PROBABILITY) {
            forfeit = random.nextBoolean() ? Forfeit.HOME : Forfeit.AWAY;
        }
        return new MatchResult(f.home(), f.away(), homeGoals, awayGoals, forfeit);
    }

    /**
     * Knuth yöntemiyle Poisson örneklemesi: e^{-λ} eşiğinin altına inene kadar düzgün sayılar çarpılır.
     * λ burada en fazla ~4 olduğu için yöntem hem doğru hem yeterince hızlıdır.
     */
    static int poisson(double lambda, Random random) {
        if (lambda <= 0) {
            throw new IllegalArgumentException("lambda must be positive: " + lambda);
        }
        double limit = Math.exp(-lambda);
        double product = random.nextDouble();
        int k = 0;
        while (product > limit) {
            k++;
            product *= random.nextDouble();
        }
        return k;
    }
}
