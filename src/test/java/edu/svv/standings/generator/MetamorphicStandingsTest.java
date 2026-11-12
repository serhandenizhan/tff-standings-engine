package edu.svv.standings.generator;

import edu.svv.standings.StandingRow;
import edu.svv.standings.StandingsCalculator;
import edu.svv.standings.generator.GeneratorTestSupport.SeasonCase;
import edu.svv.standings.model.Forfeit;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.svv.standings.generator.GeneratorTestSupport.row;
import static edu.svv.standings.generator.GeneratorTestSupport.table;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Metamorfik ilişkiler: girdiyi bilinen bir etkiyle değiştir, çıktıdaki etkiyi kontrol et. */
class MetamorphicStandingsTest {

    /** 12 kütüphane senaryosu + 20 rastgele sezon (runtime için 50'nin bir kısmı). */
    static Stream<SeasonCase> seasons() {
        return Stream.concat(GeneratorTestSupport.libraryStream(),
                GeneratorTestSupport.randomCases().stream().limit(20));
    }

    /** Yalnızca tam sezonlar: 34 hafta (hafta silme ilişkisi için). */
    static Stream<SeasonCase> fullSeasons() {
        return GeneratorTestSupport.allCases()
                .filter(c -> c.season().matches().size() == 306);
    }

    private static List<StandingRow> calc(List<Team> teams, List<MatchResult> matches, List<PointDeduction> ded) {
        return new StandingsCalculator().calculate(teams, matches, ded);
    }

    /** Sıra, takım adı ve çözülemez bayrağından oluşan karşılaştırma dizisi. */
    private static List<String> signature(List<StandingRow> t) {
        return t.stream().map(r -> r.getPosition() + ":" + r.getTeam().name() + ":" + r.isUnresolved()).toList();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("seasons")
    @DisplayName("TC-104 – MR karıştırma: takım ve maç listesi permütasyonu aynı (takım, sıra, çözülemez) dizisini verir")
    void tc104_shuffle(SeasonCase c) {
        // Sıralama girdi sırasına bağlı olmamalı (kararlı sıralama gizli eşitlik bozucu olmamalı)
        GeneratedSeason s = c.season();
        Random random = new Random(c.label().hashCode());
        List<Team> teams = new ArrayList<>(s.teams());
        List<MatchResult> matches = new ArrayList<>(s.matches());
        List<PointDeduction> ded = new ArrayList<>(s.deductions());
        Collections.shuffle(teams, random);
        Collections.shuffle(matches, random);
        Collections.shuffle(ded, random);

        assertEquals(signature(table(s)), signature(calc(teams, matches, ded)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("seasons")
    @DisplayName("TC-105 – MR yeniden adlandırma: tüm takımlara \"X \" öneki; çözülenler aynı sırada, çözülemez grupların üyeleri aynı")
    void tc105_rename(SeasonCase c) {
        // İsimler yalnızca D-02 görüntü sırasını etkiler; bu yüzden çözülemez gruplar küme olarak karşılaştırılır
        GeneratedSeason s = c.season();
        Map<String, Team> renamed = s.teams().stream()
                .collect(Collectors.toMap(Team::name, t -> new Team("X " + t.name())));
        List<Team> teams = s.teams().stream().map(t -> renamed.get(t.name())).toList();
        List<MatchResult> matches = s.matches().stream()
                .map(m -> new MatchResult(renamed.get(m.homeTeam().name()), renamed.get(m.awayTeam().name()),
                        m.homeGoals(), m.awayGoals(), m.forfeit()))
                .toList();
        List<PointDeduction> ded = s.deductions().stream()
                .map(d -> new PointDeduction(renamed.get(d.team().name()), d.points()))
                .toList();

        List<StandingRow> before = table(s);
        List<StandingRow> after = calc(teams, matches, ded);
        assertEquals(before.size(), after.size());
        int i = 0;
        while (i < before.size()) {
            StandingRow b = before.get(i);
            StandingRow a = after.get(i);
            assertEquals(b.isUnresolved(), a.isUnresolved(), "çözülemez bayrağı, sıra " + (i + 1));
            assertEquals(b.getPoints(), a.getPoints(), "puan, sıra " + (i + 1));
            if (!b.isUnresolved()) {
                assertEquals("X " + b.getTeam().name(), a.getTeam().name(), "çözülen takım, sıra " + (i + 1));
                i++;
                continue;
            }
            int j = i;
            while (j < before.size() && before.get(j).isUnresolved() && before.get(j).getPoints() == b.getPoints()) {
                j++;
            }
            Set<String> expected = new HashSet<>();
            Set<String> actual = new HashSet<>();
            for (int k = i; k < j; k++) {
                expected.add("X " + before.get(k).getTeam().name());
                actual.add(after.get(k).getTeam().name());
            }
            assertEquals(expected, actual, "çözülemez grup üyeleri, sıralar " + (i + 1) + ".." + j);
            i = j;
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("seasons")
    @DisplayName("TC-106 – MR yansıtma: her maçta ev/deplasman ve goller yer değiştirir, HOME↔AWAY; tablo aynı kalır")
    void tc106_mirror(SeasonCase c) {
        // FMT 9/2-a II: deplasman golü ekstra ağırlık taşımaz; hiçbir kriter sahaya bağlı değil
        GeneratedSeason s = c.season();
        List<MatchResult> mirrored = s.matches().stream()
                .map(m -> new MatchResult(m.awayTeam(), m.homeTeam(), m.awayGoals(), m.homeGoals(), flip(m.forfeit())))
                .toList();
        assertEquals(signature(table(s)), signature(calc(s.teams(), mirrored, s.deductions())));
    }

    private static Forfeit flip(Forfeit f) {
        return switch (f) {
            case HOME -> Forfeit.AWAY;
            case AWAY -> Forfeit.HOME;
            default -> Forfeit.NONE;
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("seasons")
    @DisplayName("TC-107 – MR puan silme: T'ye (T,d) eklenince yalnızca T'nin puanı tam d düşer; diğer istatistikler ve diğer takımların puanı aynı")
    void tc107_deduction(SeasonCase c) {
        // Diğer takımların göreli sırası ASSERT EDİLMEZ: yeni 3'lü eşitlik FMT 9/2-b nedeniyle sırayı değiştirebilir
        GeneratedSeason s = c.season();
        int index = Math.floorMod(c.label().hashCode(), s.teams().size());
        Team target = s.teams().get(index);
        int d = 1 + Math.floorMod(c.label().hashCode(), 5);

        List<PointDeduction> ded = new ArrayList<>(s.deductions());
        ded.add(new PointDeduction(target, d));
        List<StandingRow> before = table(s);
        List<StandingRow> after = calc(s.teams(), s.matches(), ded);

        for (StandingRow b : before) {
            StandingRow a = row(after, b.getTeam().name());
            boolean isTarget = b.getTeam().equals(target);
            assertEquals(isTarget ? b.getPoints() - d : b.getPoints(), a.getPoints(), b.getTeam().name() + " puan");
            assertEquals(b.getPlayed(), a.getPlayed());
            assertEquals(b.getWon(), a.getWon());
            assertEquals(b.getDrawn(), a.getDrawn());
            assertEquals(b.getLost(), a.getLost());
            assertEquals(b.getGoalsFor(), a.getGoalsFor());
            assertEquals(b.getGoalsAgainst(), a.getGoalsAgainst());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fullSeasons")
    @DisplayName("TC-129 – MR hafta silme: tam sezondan bir haftanın 9 maçı çıkarılınca her takımın oynanan sayısı tam 1 azalır")
    void tc129_removeRound(SeasonCase c) {
        // Her haftada her takım tam bir maç oynar (üreteç sözleşmesi); aradaki hafta etiketten türetilir
        GeneratedSeason s = c.season();
        int round = Math.floorMod(c.label().hashCode(), SeasonGenerator.ROUNDS);
        List<MatchResult> reduced = new ArrayList<>(s.matches());
        reduced.subList(round * 9, round * 9 + 9).clear();

        List<StandingRow> before = table(s);
        List<StandingRow> after = calc(s.teams(), reduced, s.deductions());
        for (StandingRow b : before) {
            assertEquals(b.getPlayed() - 1, row(after, b.getTeam().name()).getPlayed(),
                    b.getTeam().name() + " oynanan, silinen hafta " + (round + 1));
        }
    }
}
