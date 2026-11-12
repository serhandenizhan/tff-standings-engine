package edu.svv.standings.generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Güç modeli testleri (TC-120..TC-123): beklenen değerler CSV'den testin kendi kodu ile bağımsız hesaplanır. */
class StrengthModelTest {

    /** CSV satırı: season;club;position;played;won;drawn;lost;goalsFor;goalsAgainst;points. */
    private record SeasonRow(String season, String club, int played, int goalsFor, int goalsAgainst) {
    }

    private static List<String> readLines(String resource) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                StrengthModelTest.class.getResourceAsStream(resource), StandardCharsets.UTF_8))) {
            return r.lines().toList();
        }
    }

    private static List<SeasonRow> rows() throws IOException {
        List<SeasonRow> rows = new ArrayList<>();
        List<String> lines = readLines(StrengthModel.SEASONS_RESOURCE);
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split(";");
            rows.add(new SeasonRow(f[0], f[1], Integer.parseInt(f[3]), Integer.parseInt(f[7]), Integer.parseInt(f[8])));
        }
        return rows;
    }

    private static List<String> clubs() throws IOException {
        return readLines(StrengthModel.CLUBS_RESOURCE).stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    @Test
    @DisplayName("TC-120 – model 2026-27 kulüp listesindeki 18 kulübü, aynı sırada ve tekil olarak içerir")
    void tc120_eighteenClubs() throws IOException {
        // Statü Md. 3: 18 takım
        List<String> names = StrengthModel.loadDefault().strengths().stream().map(TeamStrength::club).toList();
        assertEquals(18, names.size());
        assertEquals(18, new HashSet<>(names).size());
        assertEquals(clubs(), names);
    }

    @Test
    @DisplayName("TC-121 – terfi ortalaması yalnızca pencerede Süper Lig sezonu olmayan kulüplere (Amed SFK, Çorum FK) uygulanır")
    void tc121_promotedAverageOnlyForClubsWithoutSeasons() throws IOException {
        List<SeasonRow> rows = rows();
        StrengthModel model = StrengthModel.loadDefault();

        // Bağımsız hesap: t >= ikinci sezon için, bir önceki sezonda olmayan kulüp satırları
        List<String> seasons = new ArrayList<>(new TreeSet<>(rows.stream().map(SeasonRow::season).toList()));
        double played = 0;
        double gf = 0;
        double ga = 0;
        for (SeasonRow r : rows) {
            int idx = seasons.indexOf(r.season());
            if (idx < 1) {
                continue;
            }
            Set<String> previous = new HashSet<>();
            rows.stream().filter(x -> x.season().equals(seasons.get(idx - 1))).forEach(x -> previous.add(x.club()));
            if (!previous.contains(r.club())) {
                played += r.played();
                gf += r.goalsFor();
                ga += r.goalsAgainst();
            }
        }
        double expectedAttack = gf / played;
        double expectedDefence = ga / played;

        Set<String> withData = new HashSet<>();
        rows.forEach(r -> withData.add(r.club()));
        for (TeamStrength s : model.strengths()) {
            if (withData.contains(s.club())) {
                assertFalse(s.fromPromotedAverage(), s.club() + " verisi var, terfi ortalaması kullanılmamalı");
                assertTrue(s.seasons() > 0, s.club());
            } else {
                assertTrue(s.fromPromotedAverage(), s.club() + " verisi yok, terfi ortalaması bekleniyor");
                assertEquals(0, s.seasons(), s.club());
                assertEquals(expectedAttack, s.attack(), 1e-9, s.club() + " hücum");
                assertEquals(expectedDefence, s.defence(), 1e-9, s.club() + " savunma");
            }
        }
        assertTrue(model.strengthOf("Amed SFK").fromPromotedAverage());
        assertTrue(model.strengthOf("Çorum FK").fromPromotedAverage());
        // docs/generator.md'de belgelenen değerler (yaklaşık): hücum 1,22 / savunma 1,45
        assertEquals(1.22, expectedAttack, 0.005);
        assertEquals(1.45, expectedDefence, 0.005);
    }

    @Test
    @DisplayName("TC-122 – lig ortalaması μ = toplam atılan gol / toplam oynanan maç (CSV'den bağımsız hesap, ≈ 1,407)")
    void tc122_leagueMean() throws IOException {
        List<SeasonRow> rows = rows();
        double gf = rows.stream().mapToInt(SeasonRow::goalsFor).sum();
        double played = rows.stream().mapToInt(SeasonRow::played).sum();
        assertEquals(gf / played, StrengthModel.loadDefault().leagueMeanGoals(), 1e-9);
        assertEquals(1.407, StrengthModel.loadDefault().leagueMeanGoals(), 0.001);
    }

    @Test
    @DisplayName("TC-123 – verisi olan her kulüpte hücum = ΣAtılan/ΣOynanan, savunma = ΣYenilen/ΣOynanan (maç ağırlıklı), sezon sayısı doğru")
    void tc123_attackAndDefenceFromData() throws IOException {
        List<SeasonRow> rows = rows();
        StrengthModel model = StrengthModel.loadDefault();
        for (String club : clubs()) {
            List<SeasonRow> own = rows.stream().filter(r -> r.club().equals(club)).toList();
            if (own.isEmpty()) {
                continue;
            }
            double played = own.stream().mapToInt(SeasonRow::played).sum();
            TeamStrength s = model.strengthOf(club);
            assertEquals(own.stream().mapToInt(SeasonRow::goalsFor).sum() / played, s.attack(), 1e-9, club + " hücum");
            assertEquals(own.stream().mapToInt(SeasonRow::goalsAgainst).sum() / played, s.defence(), 1e-9, club + " savunma");
            assertEquals(own.size(), s.seasons(), club + " sezon sayısı");
            assertEquals(s.attack() - s.defence(), s.goalDifferencePerGame(), 1e-12);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bilinmeyen FK", "", "galatasaray"})
    @DisplayName("TC-123b – bilinmeyen kulüp adı reddedilir (büyük/küçük harf duyarlı)")
    void tc123b_unknownClub(String name) {
        assertThrows(IllegalArgumentException.class, () -> StrengthModel.loadDefault().strengthOf(name));
    }

    @Test
    @DisplayName("TC-123c – boş sezon verisi, terfi eden kulüp bulunmayan veri ve boş kulüp listesi reddedilir")
    void tc123c_invalidSources() {
        List<String> header = List.of("season;club;position;played;won;drawn;lost;goalsFor;goalsAgainst;points");
        assertThrows(IllegalArgumentException.class, () -> StrengthModel.fromLines(header, List.of("A")));
        List<String> oneSeason = List.of(header.get(0), "2020-21;A;1;34;20;7;7;60;30;67");
        assertThrows(IllegalArgumentException.class, () -> StrengthModel.fromLines(oneSeason, List.of("A")));
    }
}
