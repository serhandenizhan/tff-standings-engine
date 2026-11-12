package edu.svv.standings.generator;

import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Üretilmiş (ya da senaryo kütüphanesinden okunmuş) bir sezon: takımlar, maçlar, puan silmeler ve kısa açıklama.
 * Maçlar sahadaki gerçek skoru ve hükmen bilgisini taşır; tablo hesabı StandingsCalculator'a bırakılır.
 */
public record GeneratedSeason(List<Team> teams, List<MatchResult> matches,
                              List<PointDeduction> deductions, String description) {

    /**
     * Sezonu projenin CSV biçiminde yazar: UTF-8, noktalı virgül, başlıksız.
     * teams.csv, matches.csv ve deductions.csv dosyaları oluşturulur (boşsa dosya boş kalır).
     */
    public void writeCsv(Path dir) throws IOException {
        Files.createDirectories(dir);

        Files.writeString(dir.resolve("teams.csv"),
                lines(teams.stream().map(Team::name).collect(Collectors.toList())),
                StandardCharsets.UTF_8);

        Files.writeString(dir.resolve("matches.csv"),
                lines(matches.stream()
                        .map(m -> m.homeTeam().name() + ";" + m.awayTeam().name() + ";"
                                + m.homeGoals() + ";" + m.awayGoals() + ";" + m.forfeit().name())
                        .collect(Collectors.toList())),
                StandardCharsets.UTF_8);

        Files.writeString(dir.resolve("deductions.csv"),
                lines(deductions.stream()
                        .map(d -> d.team().name() + ";" + d.points())
                        .collect(Collectors.toList())),
                StandardCharsets.UTF_8);
    }

    private static String lines(List<String> rows) {
        return rows.isEmpty() ? "" : String.join("\n", rows) + "\n";
    }
}
