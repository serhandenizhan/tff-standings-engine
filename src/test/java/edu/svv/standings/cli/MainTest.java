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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Komut satırı uygulaması uçtan uca testleri (FR-14, FR-15, D-03). */
class MainTest {

    @TempDir
    Path dir;

    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

    private int run(String... args) {
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);
        return Main.run(args, out, err);
    }

    private String out() {
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }

    private String write(String name, String content) throws IOException {
        Path p = dir.resolve(name);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p.toString();
    }

    private String[] tokens(String line) {
        return line.trim().split("\\s+");
    }

    /** Başlık hariç, ilk sütunu sayı olan veri satırlarının takım adlarını sırayla döndürür. */
    private List<String> teamOrder(String output) {
        return Arrays.stream(output.split("\\R"))
                .map(String::trim)
                .filter(l -> l.matches("\\d+\\s+.*"))
                .map(l -> tokens(l)[1])
                .toList();
    }

    private String teamsAbc() throws IOException {
        return write("teams.csv", "A\nB\nC\n");
    }

    private String matchesTc012() throws IOException {
        return write("matches.csv", "A;B;2;0\nB;A;1;1\nA;C;0;1\nC;A;2;2\nB;C;3;1\nC;B;0;1\n");
    }

    @Test
    @DisplayName("TC-095: iki dosyayla çalışır; tablo B, A, C; çıkış 0; stderr boş (puan silme dosyası isteğe bağlı)")
    void tc095_twoArguments() throws IOException {
        int code = run(teamsAbc(), matchesTc012());
        assertEquals(0, code, err());
        assertEquals(List.of("B", "A", "C"), teamOrder(out()), out());
        assertEquals("", err());
    }

    @Test
    @DisplayName("TC-096: üçüncü argüman kullanılır; B 7-3=4 puana düşer, sıra A, B, C")
    void tc096_deductionsFileUsed() throws IOException {
        String deductions = write("deductions.csv", "B;3\n");
        int code = run(teamsAbc(), matchesTc012(), deductions);
        assertEquals(0, code, err());
        // B ve C 4 puanda eşit; B-C ikili puanında B önde, bu yüzden B, C'nin önünde
        assertEquals(List.of("A", "B", "C"), teamOrder(out()), out());
        String bLine = Arrays.stream(out().split("\\R")).filter(l -> l.trim().matches("\\d+\\s+B\\s.*")).findFirst().orElseThrow();
        String[] tk = tokens(bLine);
        assertEquals("4", tk[9], out());
    }

    @Test
    @DisplayName("TC-097: 0, 1 ve 4 argüman kullanım mesajı + sıfırdan farklı çıkış, tablo yok")
    void tc097_wrongArgumentCount() throws IOException {
        String f = teamsAbc();
        // Sınır: geçerli aralık 2..3; 1 ve 4 hemen dışı, 0 uç durum
        for (String[] args : new String[][] {{}, {f}, {f, f, f, f}}) {
            outBytes.reset();
            errBytes.reset();
            int code = run(args);
            assertNotEquals(0, code, "argüman sayısı " + args.length);
            assertFalse(err().isBlank(), "kullanım mesajı yok, argüman sayısı " + args.length);
            assertEquals("", out(), "tablo basılmamalı, argüman sayısı " + args.length);
        }
    }

    @Test
    @DisplayName("TC-098a: olmayan dosya stderr'e dosya adıyla tek satır hata, sıfırdan farklı çıkış, tablo yok")
    void tc098a_missingFile() throws IOException {
        int code = run(teamsAbc(), dir.resolve("nope.csv").toString());
        assertNotEquals(0, code);
        assertTrue(err().contains("nope.csv"), err());
        assertEquals("", out());
        assertNoStackTrace();
    }

    @Test
    @DisplayName("TC-098b: matches 2. satırında 'A;B;x;0' hatası dosya adı ve satır 2 ile bildirilir")
    void tc098b_malformedLine() throws IOException {
        String matches = write("matches.csv", "A;B;1;0\nA;B;x;0\n");
        int code = run(teamsAbc(), matches);
        assertNotEquals(0, code);
        assertTrue(err().contains("matches.csv"), err());
        assertTrue(err().contains("2"), err());
        assertEquals("", out());
        assertNoStackTrace();
    }

    @Test
    @DisplayName("TC-098c: kayıtlı olmayan takım 'Z' hata mesajında adlandırılır; tablo yok")
    void tc098c_unknownTeam() throws IOException {
        String matches = write("matches.csv", "A;Z;1;0\n");
        int code = run(teamsAbc(), matches);
        assertNotEquals(0, code);
        assertTrue(err().contains("Z"), err());
        assertEquals("", out());
        assertNoStackTrace();
    }

    private void assertNoStackTrace() {
        // Kullanıcıya ham yığın izi gösterilmemeli; tek satırlık temiz hata beklenir
        assertFalse(err().contains("\tat "), err());
        assertFalse(err().contains("Exception in thread"), err());
        assertEquals(1, err().strip().split("\\R").length, err());
    }

    @Test
    @DisplayName("TC-099: Türkçe adlar çıktıda bozulmaz, ikisi de çözülemedi işaretli, İstanbul Başakşehir önde")
    void tc099_turkishNamesEndToEnd() throws IOException {
        String teams = write("teams.csv", "Şanlıurfaspor\nİstanbul Başakşehir\n");
        String matches = write("matches.csv", "İstanbul Başakşehir;Şanlıurfaspor;1;1\n");
        int code = run(teams, matches);
        assertEquals(0, code, err());
        String o = out();
        int ist = o.indexOf("İstanbul Başakşehir");
        int san = o.indexOf("Şanlıurfaspor");
        assertTrue(ist >= 0 && san >= 0, o);
        // D-02: alfabetik görüntü sırası, İ Ş'den önce
        assertTrue(ist < san, o);
        List<String> rows = Arrays.stream(o.split("\\R")).filter(l -> l.contains("Başakşehir") || l.contains("Şanlıurfaspor")).toList();
        assertEquals(2, rows.size(), o);
        // Her iki satır da çözülemedi işareti taşımalı
        assertTrue(rows.stream().allMatch(r -> r.contains("*")), o);
    }
}
