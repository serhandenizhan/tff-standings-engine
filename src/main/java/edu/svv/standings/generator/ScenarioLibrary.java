package edu.svv.standings.generator;

import edu.svv.standings.io.CsvLoader;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Repoya önceden konmuş senaryo kütüphanesi (src/main/resources/scenarios/).
 * Katalog: scenarios/catalogue.csv (başlıklı: id;seed;rounds;flags;description, flags virgülle ayrılır).
 * Her senaryo klasöründe projenin CSV biçiminde teams.csv, matches.csv, deductions.csv bulunur;
 * dosyalar CsvLoader'ın parse yöntemleriyle okunur, yani CLI ile aynı doğrulamadan geçer.
 */
public final class ScenarioLibrary {

    public static final String BASE = "/scenarios/";
    public static final String CATALOGUE = BASE + "catalogue.csv";

    /** Katalogdaki bir satır. */
    public record Entry(String id, long seed, int rounds, Set<String> flags, String description) {
    }

    private final Map<String, Entry> entries;

    /** Kataloğu sınıf yolundan okur. */
    public ScenarioLibrary() {
        this.entries = parseCatalogue(readLines(CATALOGUE));
    }

    /** Senaryo kimlikleri, katalog sırasıyla (S01, S02, ...). */
    public List<String> ids() {
        return List.copyOf(entries.keySet());
    }

    public List<Entry> entries() {
        return List.copyOf(entries.values());
    }

    public Entry entry(String id) {
        Entry e = entries.get(id);
        if (e == null) {
            throw new IllegalArgumentException("Unknown scenario: " + id + " (known: " + ids() + ")");
        }
        return e;
    }

    /** Senaryoyu sınıf yolundan okur; açıklama katalogdan gelir. */
    public GeneratedSeason load(String id) {
        Entry e = entry(id);
        String dir = BASE + id + "/";
        CsvLoader loader = new CsvLoader();
        List<Team> teams;
        List<MatchResult> matches;
        List<PointDeduction> deductions;
        try (Reader r = open(dir + "teams.csv")) {
            teams = loader.parseTeams(r, id + "/teams.csv");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        try (Reader r = open(dir + "matches.csv")) {
            matches = loader.parseMatches(r, teams, id + "/matches.csv");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        try (Reader r = open(dir + "deductions.csv")) {
            deductions = loader.parseDeductions(r, teams, id + "/deductions.csv");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return new GeneratedSeason(teams, matches, deductions, e.description());
    }

    /** Seed ile tekrarlanabilir rastgele senaryo seçimi. */
    public String pickRandomId(long seed) {
        List<String> ids = ids();
        return ids.get(new Random(seed).nextInt(ids.size()));
    }

    // --- yardımcılar ---

    private static Map<String, Entry> parseCatalogue(List<String> lines) {
        Map<String, Entry> result = new LinkedHashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            // ilk satır başlık
            String line = lines.get(i).strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] f = line.split(";", -1);
            if (f.length != 5) {
                throw new IllegalStateException("catalogue.csv, line " + (i + 1) + ": expected 5 fields");
            }
            Set<String> flags = f[3].isBlank()
                    ? Set.of()
                    : new LinkedHashSet<>(Arrays.asList(f[3].strip().split(",")));
            Entry e = new Entry(f[0].strip(), Long.parseLong(f[1].strip()), Integer.parseInt(f[2].strip()),
                    Set.copyOf(flags), f[4].strip());
            result.put(e.id(), e);
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("Scenario catalogue is empty");
        }
        return result;
    }

    private static Reader open(String resource) {
        InputStream in = ScenarioLibrary.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("Resource not found on classpath: " + resource);
        }
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }

    private static List<String> readLines(String resource) {
        try (BufferedReader reader = new BufferedReader(open(resource))) {
            return new ArrayList<>(reader.lines().toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
