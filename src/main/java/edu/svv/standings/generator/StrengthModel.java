package edu.svv.standings.generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Takım gücü modeli: son 10 Süper Lig sezonunun (2016-17..2025-26) gerçek tablolarından
 * 2026-27 kulüplerinin hücum / savunma gücünü hesaplar.
 *
 * Modelleme kararları (ayrıntı: docs/generator.md):
 * - Hücum = toplam atılan gol / toplam oynanan maç, savunma = toplam yenilen gol / toplam oynanan maç,
 *   yalnızca kulübün pencere içindeki Süper Lig sezonları üzerinden (havuzlanmış ortalama; sezonlar
 *   34–40 maç sürdüğü için sezon ortalamalarının ortalaması yerine maç ağırlıklı ortalama alınır).
 * - Pencerede hiç Süper Lig sezonu olmayan kulüpler (Amed SFK, Çorum FK) için "yeni çıkan takım" ortalaması:
 *   t ≥ 2017-18 olmak üzere t sezonunda olup t-1 sezonunda olmayan her kulübün t sezonundaki satırı.
 *   Bu satırların aynı şekilde havuzlanmış ortalaması kullanılır.
 * - Lig ortalaması μ = penceredeki tüm satırların toplam golü / toplam maçı (takım başına maç başına gol).
 * - Veri tablolardaki gibi alınır; 2022-23'te ligden çekilen Gaziantep FK ve Hatayspor'un satırları
 *   (hükmen yenilgiler dahil) değiştirilmeden kullanılır. Bu, Gaziantep FK'nin gücünü biraz düşürür (varsayım: kabul edildi).
 */
public final class StrengthModel {

    public static final String SEASONS_RESOURCE = "/strength/super-lig-seasons.csv";
    public static final String CLUBS_RESOURCE = "/strength/super-lig-2026-27-clubs.txt";

    private final Map<String, TeamStrength> strengths;
    private final double leagueMeanGoals;
    private final TeamStrength promotedAverage;

    private StrengthModel(Map<String, TeamStrength> strengths, double leagueMeanGoals, TeamStrength promotedAverage) {
        this.strengths = strengths;
        this.leagueMeanGoals = leagueMeanGoals;
        this.promotedAverage = promotedAverage;
    }

    /** Sınıf yolundaki gerçek veriden modeli kurar; sonuç önbelleğe alınır (veri değişmez). */
    public static StrengthModel loadDefault() {
        return DefaultHolder.INSTANCE;
    }

    // tembel ve iş parçacığı güvenli tek seferlik yükleme
    private static final class DefaultHolder {
        private static final StrengthModel INSTANCE =
                fromLines(readResource(SEASONS_RESOURCE), readResource(CLUBS_RESOURCE));
    }

    /**
     * Satır listelerinden model kurar.
     *
     * @param seasonLines başlıklı sezon CSV'si: season;club;position;played;won;drawn;lost;goalsFor;goalsAgainst;points
     * @param clubLines   her satırda bir kulüp adı (boş satırlar atlanır)
     */
    public static StrengthModel fromLines(List<String> seasonLines, List<String> clubLines) {
        List<Row> rows = parseRows(seasonLines);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Season data is empty");
        }

        // lig ortalaması: tüm satırların toplamı
        double mu = sum(rows, Row::goalsFor) / sum(rows, Row::played);

        // sezon -> o sezondaki kulüpler (sezon adları "2016-17" biçiminde, sözlük sırası kronolojiktir)
        Map<String, Set<String>> clubsBySeason = new LinkedHashMap<>();
        for (String season : new TreeSet<>(rows.stream().map(Row::season).toList())) {
            clubsBySeason.put(season, new HashSet<>());
        }
        for (Row r : rows) {
            clubsBySeason.get(r.season()).add(r.club());
        }
        List<String> seasons = new ArrayList<>(clubsBySeason.keySet());

        // yeni çıkan takımların ilk sezonu: t sezonunda olup t-1'de olmayanlar (ilk sezonun öncesi bilinmediği için t ≥ 2. sezon)
        List<Row> promotedRows = new ArrayList<>();
        for (Row r : rows) {
            int idx = seasons.indexOf(r.season());
            if (idx >= 1 && !clubsBySeason.get(seasons.get(idx - 1)).contains(r.club())) {
                promotedRows.add(r);
            }
        }
        if (promotedRows.isEmpty()) {
            throw new IllegalArgumentException("No promoted clubs found in the season data");
        }
        TeamStrength promoted = aggregate("(promoted average)", promotedRows);

        Map<String, TeamStrength> result = new LinkedHashMap<>();
        for (String line : clubLines) {
            String club = line.strip();
            if (club.isEmpty()) {
                continue;
            }
            List<Row> own = rows.stream().filter(r -> r.club().equals(club)).toList();
            if (own.isEmpty()) {
                // geçmişi olmayan kulüp: yeni çıkan takım ortalaması, kulübün kendi adıyla
                result.put(club, new TeamStrength(club, promoted.attack(), promoted.defence(),
                        promoted.pointsPerGame(), promoted.averagePosition(), 0, true));
            } else {
                result.put(club, aggregate(club, own));
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Club list is empty");
        }
        return new StrengthModel(Collections.unmodifiableMap(result), mu, promoted);
    }

    /** Kulüpler, kulüp dosyasındaki sırayla. */
    public List<TeamStrength> strengths() {
        return List.copyOf(strengths.values());
    }

    public TeamStrength strengthOf(String club) {
        TeamStrength s = strengths.get(club);
        if (s == null) {
            throw new IllegalArgumentException("Unknown club: " + club);
        }
        return s;
    }

    /** μ: takım başına maç başına ortalama gol (pencerenin tamamı). */
    public double leagueMeanGoals() {
        return leagueMeanGoals;
    }

    /** Yeni çıkan takımların ilk Süper Lig sezonu ortalaması (geçmişi olmayan kulüpler için kullanılan değer). */
    public TeamStrength promotedAverage() {
        return promotedAverage;
    }

    // --- yardımcılar ---

    private record Row(String season, String club, int position, int played, int won, int drawn,
                       int goalsFor, int goalsAgainst) {
    }

    private static TeamStrength aggregate(String club, List<Row> rows) {
        double played = sum(rows, Row::played);
        double attack = sum(rows, Row::goalsFor) / played;
        double defence = sum(rows, Row::goalsAgainst) / played;
        // puan silmeler gücü değil disiplini yansıttığı için puan 3G + B olarak yeniden hesaplanır
        double ppg = (3 * sum(rows, Row::won) + sum(rows, Row::drawn)) / played;
        double avgPos = sum(rows, Row::position) / rows.size();
        return new TeamStrength(club, attack, defence, ppg, avgPos, rows.size(), false);
    }

    private static double sum(List<Row> rows, java.util.function.ToIntFunction<Row> f) {
        return rows.stream().mapToInt(f).sum();
    }

    private static List<Row> parseRows(List<String> lines) {
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            // ilk satır başlıktır; boş satırlar atlanır
            if (i == 0 || line.isEmpty()) {
                continue;
            }
            String[] f = line.split(";", -1);
            if (f.length != 10) {
                throw new IllegalArgumentException("Season data line " + (i + 1) + ": expected 10 fields");
            }
            try {
                rows.add(new Row(f[0].strip(), f[1].strip(), Integer.parseInt(f[2].strip()),
                        Integer.parseInt(f[3].strip()), Integer.parseInt(f[4].strip()),
                        Integer.parseInt(f[5].strip()), Integer.parseInt(f[7].strip()),
                        Integer.parseInt(f[8].strip())));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Season data line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return rows;
    }

    private static List<String> readResource(String name) {
        InputStream in = StrengthModel.class.getResourceAsStream(name);
        if (in == null) {
            throw new IllegalStateException("Resource not found on classpath: " + name);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
