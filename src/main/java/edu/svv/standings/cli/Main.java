package edu.svv.standings.cli;

import edu.svv.standings.StandingRow;
import edu.svv.standings.StandingsCalculator;
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
 */
public class Main {

    static final int EXIT_OK = 0;
    static final int EXIT_INPUT_ERROR = 1;
    static final int EXIT_USAGE = 2;

    private static final String USAGE =
            "Usage: java -jar tff-standings-engine.jar <teams.csv> <matches.csv> [deductions.csv]";

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
}
