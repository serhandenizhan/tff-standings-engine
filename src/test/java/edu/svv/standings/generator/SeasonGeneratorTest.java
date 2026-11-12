package edu.svv.standings.generator;

import edu.svv.standings.model.Forfeit;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static edu.svv.standings.generator.GeneratorTestSupport.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Üreteç doğruluğu (TC-111..TC-119): üreteç de ilk sürüm koddur, kendi testi gerekir. */
class SeasonGeneratorTest {

    private static final int MATCHES_PER_ROUND = 9;

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {0, 1, 42, 988, 10463, -7})
    @DisplayName("TC-111 – aynı tohum ve tur sayısı özdeş sezon verir; üretilen sezon hesaplayıcı doğrulamasından geçer")
    void tc111_sameSeedIdentical(long seed) {
        // Sözleşme: aynı seed + aynı veri => aynı sezon (tekrarlanabilir test verisi)
        GeneratedSeason a = new SeasonGenerator(seed).generate(34);
        GeneratedSeason b = new SeasonGenerator(seed).generate(34);
        assertEquals(a, b);
        // üretilen sezon V-01..V-07 doğrulamasından hata fırlatmadan geçmeli
        assertEquals(18, table(a).size());
    }

    @Test
    @DisplayName("TC-112 – farklı tohumlar (1..40) farklı sezon verir: maç listeleri ikişer ikişer farklı")
    void tc112_differentSeedsDiffer() {
        // Tohum gerçekten kullanılıyor mu: 40 tohumun hepsi farklı sonuç listesi üretmeli
        Set<List<MatchResult>> distinct = new HashSet<>();
        for (int seed = 1; seed <= 40; seed++) {
            distinct.add(new SeasonGenerator(seed).generate(34).matches());
        }
        assertEquals(40, distinct.size());
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 2, 3, 42, 988})
    @DisplayName("TC-113 – fikstür geçerliliği: 18 takım, 306 maç, haftada 9, her sıralı çift tam bir kez, hafta içinde takım tekrarı yok")
    void tc113_scheduleValidity(long seed) {
        GeneratedSeason s = new SeasonGenerator(seed).generate(34);
        List<MatchResult> m = s.matches();
        assertEquals(18, s.teams().size());
        assertEquals(306, m.size());

        // her sıralı (ev, deplasman) çifti tam bir kez: 18*17 = 306 farklı çift
        Set<String> ordered = new HashSet<>();
        for (MatchResult r : m) {
            assertTrue(ordered.add(r.homeTeam().name() + ">" + r.awayTeam().name()), "tekrarlanan fikstür: " + r);
        }
        assertEquals(306, ordered.size());

        // hafta bloğu = 9 ardışık maç; bir takım bir haftada yalnız bir maç oynar
        for (int round = 0; round < 34; round++) {
            Set<String> inRound = new HashSet<>();
            for (MatchResult r : m.subList(round * MATCHES_PER_ROUND, (round + 1) * MATCHES_PER_ROUND)) {
                assertTrue(inRound.add(r.homeTeam().name()), "hafta " + (round + 1) + ": " + r.homeTeam().name() + " iki kez");
                assertTrue(inRound.add(r.awayTeam().name()), "hafta " + (round + 1) + ": " + r.awayTeam().name() + " iki kez");
            }
            assertEquals(18, inRound.size());
        }

        // belgelenen yapı: ikinci devre, ilk devrenin ev/deplasman yer değiştirmiş hali
        for (int round = 0; round < 17; round++) {
            for (int i = 0; i < MATCHES_PER_ROUND; i++) {
                MatchResult first = m.get(round * MATCHES_PER_ROUND + i);
                MatchResult second = m.get((round + 17) * MATCHES_PER_ROUND + i);
                assertEquals(first.homeTeam(), second.awayTeam(), "ikinci devre eşleşmesi, hafta " + (round + 1));
                assertEquals(first.awayTeam(), second.homeTeam(), "ikinci devre eşleşmesi, hafta " + (round + 1));
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 5, 42, 988})
    @DisplayName("TC-114 – generate(k), aynı tohumlu generate(34)'ün ilk k haftasıdır; puan silmeler onun alt kümesidir")
    void tc114_prefixProperty(long seed) {
        GeneratedSeason full = new SeasonGenerator(seed).generate(34);
        for (int k : new int[] {1, 2, 10, 17, 33}) {
            GeneratedSeason part = new SeasonGenerator(seed).generate(k);
            assertEquals(full.matches().subList(0, k * MATCHES_PER_ROUND), part.matches(), "k=" + k);
            assertEquals(full.teams(), part.teams(), "takım listesi k=" + k);
            assertTrue(full.deductions().containsAll(part.deductions()), "silme alt küme değil k=" + k);
        }
    }

    @ParameterizedTest(name = "rounds {0}")
    @ValueSource(ints = {0, 35, -1, 36, Integer.MIN_VALUE, Integer.MAX_VALUE})
    @DisplayName("TC-115 – BVA geçersiz tur sayısı: 0, 35 ve ötesi reddedilir")
    void tc115_invalidRounds(int rounds) {
        // Geçerli aralık 1..34; 0 ve 35 sınırın hemen dışı
        SeasonGenerator generator = new SeasonGenerator(1);
        assertThrows(IllegalArgumentException.class, () -> generator.generate(rounds));
    }

    @ParameterizedTest(name = "rounds {0}")
    @ValueSource(ints = {1, 2, 33, 34})
    @DisplayName("TC-116 – BVA geçerli tur sayısı: 1 ve 34 (ve komşuları) kabul edilir, maç sayısı 9·k")
    void tc116_validRounds(int rounds) {
        GeneratedSeason s = new SeasonGenerator(1).generate(rounds);
        assertEquals(MATCHES_PER_ROUND * rounds, s.matches().size());
        assertEquals(34, new SeasonGenerator(1).generateFullSeason().matches().size() / MATCHES_PER_ROUND);
    }

    @Test
    @DisplayName("TC-117 – hükmen ve puan silme nadir ama var: 200 sezonda hükmen ≈ %1, silme ≈ takım-sezon başına %2, her silme 3 puan")
    void tc117_forfeitsAndDeductionsAreRareButPresent() {
        // Belgelenen olasılıklar: hükmen %1 / maç, silme %2 / kulüp-sezon (3 puan); geniş tolerans, rastlantı payı için
        int seasons = 200;
        int matches = 0;
        int forfeits = 0;
        int deductions = 0;
        int seasonsWithDeduction = 0;
        for (int seed = 1; seed <= seasons; seed++) {
            GeneratedSeason s = new SeasonGenerator(seed).generate(34);
            matches += s.matches().size();
            forfeits += (int) s.matches().stream().filter(m -> m.forfeit() != Forfeit.NONE).count();
            deductions += s.deductions().size();
            if (!s.deductions().isEmpty()) {
                seasonsWithDeduction++;
            }
            assertTrue(s.deductions().stream().allMatch(d -> d.points() == 3), "silme 3 puan değil");
        }
        double forfeitRate = (double) forfeits / matches;
        double deductionRate = (double) deductions / (seasons * 18);
        double seasonShare = (double) seasonsWithDeduction / seasons;
        assertTrue(forfeits > 0, "hiç hükmen yok");
        assertTrue(forfeitRate > 0.005 && forfeitRate < 0.015, "hükmen oranı " + forfeitRate);
        assertTrue(deductionRate > 0.01 && deductionRate < 0.03, "silme oranı " + deductionRate);
        assertTrue(seasonShare > 0.15 && seasonShare < 0.45, "silmeli sezon payı " + seasonShare);
    }

    @ParameterizedTest(name = "lambda {0}")
    @ValueSource(doubles = {0.5, 1.4, 3.0})
    @DisplayName("TC-118 – Poisson örnekleyici: 200000 çekimde ortalama ve varyans ≈ λ (sabit tohum)")
    void tc118_poissonMean(double lambda) {
        // Poisson dağılımında ortalama = varyans = λ; kendi yazdığımız örnekleyici bu özelliği sağlamalı
        Random random = new Random(12345);
        int n = 200_000;
        double sum = 0;
        double sumSq = 0;
        for (int i = 0; i < n; i++) {
            int k = SeasonGenerator.poisson(lambda, random);
            sum += k;
            sumSq += (double) k * k;
        }
        double mean = sum / n;
        double variance = sumSq / n - mean * mean;
        assertEquals(lambda, mean, 0.02, "ortalama");
        assertEquals(lambda, variance, 0.03 * lambda + 0.02, "varyans");
    }

    @Test
    @DisplayName("TC-118b – Poisson örnekleyici λ ≤ 0 için reddeder")
    void tc118b_poissonInvalidLambda() {
        // λ pozitif olmalı; 0 sınırın kendisi geçersiz
        assertThrows(IllegalArgumentException.class, () -> SeasonGenerator.poisson(0, new Random(1)));
        assertThrows(IllegalArgumentException.class, () -> SeasonGenerator.poisson(-1, new Random(1)));
    }

    @Test
    @DisplayName("TC-119 – ev sahibi avantajı: 60 tam sezonda ev gollerinin payı belgelenen ~%55 civarında (%53–%57)")
    void tc119_homeGoalShare() {
        // H = 1,10 → eşit takımlarda ev payı ≈ %55 (docs/generator.md)
        long home = 0;
        long total = 0;
        for (int seed = 1; seed <= 60; seed++) {
            for (MatchResult m : new SeasonGenerator(seed).generate(34).matches()) {
                home += m.homeGoals();
                total += m.homeGoals() + m.awayGoals();
            }
        }
        double share = (double) home / total;
        assertTrue(share > 0.53 && share < 0.57, "ev payı " + share);
    }

    @Test
    @DisplayName("TC-111b – null güç modeli reddedilir; üretilen takım adları güç modelindeki kulüplerle aynı")
    void tc111b_nullModelAndTeams() {
        assertThrows(IllegalArgumentException.class, () -> new SeasonGenerator(1, null));
        List<Team> teams = new SeasonGenerator(1).generate(1).teams();
        List<String> names = StrengthModel.loadDefault().strengths().stream().map(TeamStrength::club).toList();
        assertEquals(names, teams.stream().map(Team::name).toList());
        assertNotNull(new SeasonGenerator(1).generate(1).description());
    }
}
