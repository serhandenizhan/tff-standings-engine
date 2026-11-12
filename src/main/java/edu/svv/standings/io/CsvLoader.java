package edu.svv.standings.io;

import edu.svv.standings.model.Forfeit;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Noktalı virgülle ayrılmış, başlıksız, UTF-8 CSV dosyalarını okur (FR-14, V-08).
 * Her hata, dosya adı ve 1 tabanlı satır numarasıyla InputFormatException olarak bildirilir.
 */
public class CsvLoader {

    private static final String SEPARATOR = ";";

    // --- Path tabanlı yöntemler ---

    public List<Team> loadTeams(Path file) {
        return parseTeams(readLines(file), file.getFileName().toString());
    }

    public List<MatchResult> loadMatches(Path file, List<Team> teams) {
        return parseMatches(readLines(file), teams, file.getFileName().toString());
    }

    public List<PointDeduction> loadDeductions(Path file, List<Team> teams) {
        return parseDeductions(readLines(file), teams, file.getFileName().toString());
    }

    // --- Reader tabanlı yöntemler (testlerde dosyasız kullanım için) ---

    public List<Team> parseTeams(Reader reader, String sourceName) {
        return parseTeams(readLines(reader, sourceName), sourceName);
    }

    public List<MatchResult> parseMatches(Reader reader, List<Team> teams, String sourceName) {
        return parseMatches(readLines(reader, sourceName), teams, sourceName);
    }

    public List<PointDeduction> parseDeductions(Reader reader, List<Team> teams, String sourceName) {
        return parseDeductions(readLines(reader, sourceName), teams, sourceName);
    }

    // --- Satır listesi tabanlı yöntemler ---

    public List<Team> parseTeams(List<String> lines, String sourceName) {
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = cleanLine(lines.get(i), i);
            if (line.isBlank()) {
                continue;
            }
            String[] fields = split(line, 1, 1, sourceName, i + 1);
            try {
                teams.add(new Team(fields[0]));
            } catch (IllegalArgumentException e) {
                throw error(sourceName, i + 1, e.getMessage());
            }
        }
        return teams;
    }

    /**
     * Bilinmeyen takım adları burada, dosya adı ve satır numarasıyla reddedilir;
     * böylece kullanıcı hatanın hangi satırda olduğunu görür. (Hesaplayıcının V-03 kontrolü ek güvence olarak kalır.)
     */
    public List<MatchResult> parseMatches(List<String> lines, List<Team> teams, String sourceName) {
        Set<Team> known = new HashSet<>(teams);
        List<MatchResult> matches = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = cleanLine(lines.get(i), i);
            if (line.isBlank()) {
                continue;
            }
            int lineNo = i + 1;
            String[] fields = split(line, 4, 5, sourceName, lineNo);
            try {
                Team home = new Team(fields[0]);
                Team away = new Team(fields[1]);
                requireKnown(home, known, sourceName, lineNo);
                requireKnown(away, known, sourceName, lineNo);
                int homeGoals = parseInt(fields[2], sourceName, lineNo);
                int awayGoals = parseInt(fields[3], sourceName, lineNo);
                Forfeit forfeit = fields.length == 5 ? parseForfeit(fields[4], sourceName, lineNo) : Forfeit.NONE;
                matches.add(new MatchResult(home, away, homeGoals, awayGoals, forfeit));
            } catch (IllegalArgumentException e) {
                // model doğrulaması (boş isim, negatif gol, aynı takım) satır bilgisiyle sarılır
                throw error(sourceName, lineNo, e.getMessage());
            }
        }
        return matches;
    }

    public List<PointDeduction> parseDeductions(List<String> lines, List<Team> teams, String sourceName) {
        Set<Team> known = new HashSet<>(teams);
        List<PointDeduction> deductions = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = cleanLine(lines.get(i), i);
            if (line.isBlank()) {
                continue;
            }
            int lineNo = i + 1;
            String[] fields = split(line, 2, 2, sourceName, lineNo);
            try {
                Team team = new Team(fields[0]);
                requireKnown(team, known, sourceName, lineNo);
                int points = parseInt(fields[1], sourceName, lineNo);
                deductions.add(new PointDeduction(team, points));
            } catch (IllegalArgumentException e) {
                throw error(sourceName, lineNo, e.getMessage());
            }
        }
        return deductions;
    }

    // --- yardımcılar ---

    private List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new InputFormatException("File not found: " + file, e);
        } catch (IOException e) {
            // geçersiz UTF-8 de buraya düşer
            throw new InputFormatException("Cannot read file " + file + ": " + e, e);
        }
    }

    private List<String> readLines(Reader reader, String sourceName) {
        try (BufferedReader buffered = new BufferedReader(reader)) {
            return buffered.lines().toList();
        } catch (UncheckedIOException e) {
            throw new InputFormatException("Cannot read " + sourceName + ": " + e.getCause(), e);
        } catch (IOException e) {
            throw new InputFormatException("Cannot read " + sourceName + ": " + e, e);
        }
    }

    // ilk satırdaki UTF-8 BOM karakteri atılır; başka bir temizlik yapılmaz
    private String cleanLine(String line, int index) {
        if (index == 0 && line.startsWith("﻿")) {
            return line.substring(1);
        }
        return line;
    }

    private String[] split(String line, int min, int max, String sourceName, int lineNo) {
        // -1: sondaki boş alanlar da sayılsın ("a;b;" 3 alan olur)
        String[] fields = line.split(SEPARATOR, -1);
        if (fields.length < min || fields.length > max) {
            String expected = min == max ? String.valueOf(min) : min + " or " + max;
            throw error(sourceName, lineNo, "expected " + expected + " fields but found " + fields.length);
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
        }
        return fields;
    }

    private int parseInt(String text, String sourceName, int lineNo) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw error(sourceName, lineNo, "not an integer: '" + text + "'");
        }
    }

    private Forfeit parseForfeit(String text, String sourceName, int lineNo) {
        try {
            return Forfeit.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw error(sourceName, lineNo, "unknown forfeit value '" + text + "' (expected NONE, HOME or AWAY)");
        }
    }

    private void requireKnown(Team team, Set<Team> known, String sourceName, int lineNo) {
        if (!known.contains(team)) {
            throw error(sourceName, lineNo, "unknown team '" + team.name() + "'");
        }
    }

    private InputFormatException error(String sourceName, int lineNo, String detail) {
        return new InputFormatException(sourceName + ", line " + lineNo + ": " + detail);
    }
}
