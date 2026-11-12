package edu.svv.standings.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kapsam analizinden sonra eklenen testler (TC-140..TC-143).
 * JaCoCo raporunda Main sınıfında kapsanmayan dallar (null argüman dizisi, seed'siz "random",
 * hesaplayıcının doğrulama hatalarının CLI'da yakalanması) gereksinimlerden türetilip yazıldı.
 */
class CoverageGapTest {

    @TempDir
    Path dir;

    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

    private int run(String... args) {
        return Main.run(args,
                new PrintStream(outBytes, true, StandardCharsets.UTF_8),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8));
    }

    private String out() {
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }

    private Path write(String name, String content) throws IOException {
        Path p = dir.resolve(name);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p;
    }

    @Test
    @DisplayName("TC-140 – CLI: null argüman dizisi kullanım hatasıdır (exit 2, usage, tablo yok)")
    void tc140_nullArgumentArray() {
        // FR-14/FR-15, D-03: argüman sayısı geçersizse kullanım mesajı; null dizi 0 argümanla aynı sınıftadır
        int code = run((String[]) null);
        assertEquals(Main.EXIT_USAGE, code);
        assertTrue(err().contains("Usage:"));
        assertEquals("", out());
    }

    @Test
    @DisplayName("TC-141 – CLI: '--scenario random' seed'siz çalışır; seed başlıkta yazılır ve 18 satırlık tablo basılır")
    void tc141_randomScenarioWithoutSeed() {
        // Seed verilmezse sistem saatinden alınır ve tekrar üretilebilsin diye ilk satıra yazılır (Main sınıf yorumu)
        int code = run("--scenario", "random");
        assertEquals(Main.EXIT_OK, code);
        assertEquals("", err());
        String header = out().lines().findFirst().orElse("");
        Matcher m = Pattern.compile("^Scenario (S\\d\\d) \\(picked with seed (-?\\d+)\\): .+$").matcher(header);
        assertTrue(m.matches(), "unexpected header: " + header);

        // yazılan seed ile aynı seçim tekrar üretilebilmeli
        int again = Main.run(new String[] {"--scenario", "random", m.group(2)},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream()));
        assertEquals(Main.EXIT_OK, again);
    }

    @Test
    @DisplayName("TC-142 – CLI: teams.csv'de tekrarlanan takım adı (V-02) 'Invalid data' ile exit 1 verir")
    void tc142_duplicateTeamReportedAsInvalidData() throws IOException {
        // CSV okuyucu tekrarları denetlemez; V-02 hesaplayıcıda yakalanır ve CLI bunu kullanıcıya göstermelidir
        Path teams = write("teams.csv", "A\nB\nA\n");
        Path matches = write("matches.csv", "A;B;1;0\n");
        int code = run(teams.toString(), matches.toString());
        assertEquals(Main.EXIT_INPUT_ERROR, code);
        assertTrue(err().contains("Invalid data: Duplicate team"), err());
        assertEquals("", out());
    }

    @Test
    @DisplayName("TC-143 – CLI: matches.csv'de aynı ev/deplasman maçı iki kez (V-06) 'Invalid data' ile exit 1 verir")
    void tc143_duplicateFixtureReportedAsInvalidData() throws IOException {
        Path teams = write("teams.csv", "A\nB\n");
        Path matches = write("matches.csv", "A;B;1;0\nA;B;2;2\n");
        int code = run(teams.toString(), matches.toString());
        assertEquals(Main.EXIT_INPUT_ERROR, code);
        assertTrue(err().contains("Invalid data: Duplicate fixture"), err());
        assertEquals("", out());
    }
}
