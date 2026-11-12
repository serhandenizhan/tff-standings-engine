package edu.svv.standings.cli;

import edu.svv.standings.StandingRow;
import edu.svv.standings.StandingsCalculator;
import edu.svv.standings.generator.GeneratedSeason;
import edu.svv.standings.generator.ScenarioLibrary;
import edu.svv.standings.io.CsvLoader;
import edu.svv.standings.io.InputFormatException;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Komut satırı girişi: teams.csv, matches.csv ve isteğe bağlı deductions.csv okuyup tabloyu yazdırır (FR-14, FR-15).
 * Ek olarak {@code --scenario <id|random> [seed]} ile senaryo kütüphanesinden bir sezonun tablosunu yazdırır.
 */
public class Main {

    static final int EXIT_OK = 0;
    static final int EXIT_INPUT_ERROR = 1;
    static final int EXIT_USAGE = 2;

    static final String SCENARIO_OPTION = "--scenario";

    private static final String USAGE =
            "Usage: java -jar tff-standings-engine.jar <teams.csv> <matches.csv> [deductions.csv]\n"
            + "   or: java -jar tff-standings-engine.jar --scenario <id|random> [seed]";

    public static void main(String[] args) {
        // Türkçe karakterler için çıktı UTF-8 yazılır
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        int code = run(args, out, err);
        // System.exit yalnızca burada çağrılır
        System.exit(code);
    }

    /** Mantığın tamamı burada; çıkış kodu döndürür, süreci sonlandırmaz. */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args != null && args.length > 0 && SCENARIO_OPTION.equals(args[0])) {
            return runScenario(args, out, err);
        }
        if (args == null || args.length < 2 || args.length > 3) {
            err.println("Error: expected 2 or 3 arguments.");
            err.println(USAGE);
            return EXIT_USAGE;
        }

        try {
            CsvLoader loader = new CsvLoader();
            List<Team> teams = loader.loadTeams(Path.of(args[0]));
            List<MatchResult> matches = loader.loadMatches(Path.of(args[1]), teams);
            // puan silme dosyası isteğe bağlı
            List<PointDeduction> deductions = args.length == 3
                    ? loader.loadDeductions(Path.of(args[2]), teams)
                    : List.of();

            List<StandingRow> table = new StandingsCalculator().calculate(teams, matches, deductions);
            out.print(new TableFormatter().format(table));
            return EXIT_OK;
        } catch (InputFormatException e) {
            err.println("Input error: " + e.getMessage());
            return EXIT_INPUT_ERROR;
        } catch (IllegalArgumentException e) {
            // hesaplayıcının doğrulama hataları (tekrarlanan takım/maç vb.)
            err.println("Invalid data: " + e.getMessage());
            return EXIT_INPUT_ERROR;
        }
    }

    /**
     * Senaryo kütüphanesinden bir sezonu yükleyip tablosunu yazdırır.
     * "random" verilirse seed ile tekrarlanabilir seçim yapılır; seed verilmezse sistem saatinden alınır
     * ve tekrar üretilebilsin diye çıktının ilk satırına yazılır. Kimlik verildiğinde seed kabul edilmez.
     */
    private static int runScenario(String[] args, PrintStream out, PrintStream err) {
        if (args.length < 2 || args.length > 3) {
            err.println("Error: " + SCENARIO_OPTION + " expects <id|random> [seed].");
            err.println(USAGE);
            return EXIT_USAGE;
        }
        String id = args[1];
        boolean random = "random".equals(id);
        if (!random && args.length == 3) {
            err.println("Error: a seed can only be given with '" + SCENARIO_OPTION + " random'.");
            err.println(USAGE);
            return EXIT_USAGE;
        }
        long seed = 0;
        if (random) {
            if (args.length == 3) {
                try {
                    seed = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    err.println("Error: seed is not an integer: '" + args[2] + "'");
                    err.println(USAGE);
                    return EXIT_USAGE;
                }
            } else {
                seed = System.currentTimeMillis();
            }
        }

        try {
            ScenarioLibrary library = new ScenarioLibrary();
            if (random) {
                id = library.pickRandomId(seed);
            }
            GeneratedSeason season = library.load(id);
            out.println("Scenario " + id + (random ? " (picked with seed " + seed + ")" : "")
                    + ": " + season.description());
            List<StandingRow> table = new StandingsCalculator()
                    .calculate(season.teams(), season.matches(), season.deductions());
            out.print(new TableFormatter().format(table));
            return EXIT_OK;
        } catch (IllegalArgumentException e) {
            // bilinmeyen senaryo kimliği ya da hesaplayıcı doğrulama hatası
            err.println("Invalid data: " + e.getMessage());
            return EXIT_INPUT_ERROR;
        } catch (InputFormatException | IllegalStateException e) {
            // kütüphane dosyası eksik ya da bozuk
            err.println("Scenario library error: " + e.getMessage());
            return EXIT_INPUT_ERROR;
        }
    }
}
