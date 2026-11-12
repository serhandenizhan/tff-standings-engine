package edu.svv.standings.io;

import edu.svv.standings.model.Forfeit;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CSV okuyucu testleri (FR-14, V-08). Beklenen değerler gereksinim ve test planından alınmıştır. */
class CsvLoaderTest {

    private static final Team A = new Team("A");
    private static final Team B = new Team("B");
    private static final Team C = new Team("C");
    private static final List<Team> ABC = List.of(A, B, C);

    private final CsvLoader loader = new CsvLoader();

    @TempDir
    Path dir;

    private Path write(String name, String content) throws IOException {
        Path p = dir.resolve(name);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p;
    }

    // ---------------------------------------------------------------- TC-080

    @Test
    @DisplayName("TC-080: UTF-8 Türkçe takım adları dosyadan birebir ve dosya sırasıyla okunur")
    void tc080_turkishTeamNamesReadAsUtf8() throws IOException {
        // Platform karakter kümesine bağımlılık olmamalı; Ç, İ, Ş bozulursa test kırılır
        Path file = write("teams.csv", "Çaykur Rizespor\nİstanbul Başakşehir\nŞanlıurfaspor\n");
        List<Team> teams = loader.loadTeams(file);
        assertEquals(List.of(new Team("Çaykur Rizespor"), new Team("İstanbul Başakşehir"),
                new Team("Şanlıurfaspor")), teams);
    }

    @Test
    @DisplayName("TC-080b: dosya başındaki BOM ilk takım adına yapışmaz")
    void tc080b_leadingBomIgnored() throws IOException {
        // Windows editörleri BOM ekler; "﻿A" ile "A" farklı takım olurdu
        Path file = write("teams.csv", "﻿Beşiktaş\nGöztepe\n");
        assertEquals(List.of(new Team("Beşiktaş"), new Team("Göztepe")), loader.loadTeams(file));
    }

    // ---------------------------------------------------------------- TC-081

    @Test
    @DisplayName("TC-081: 4 alanlı satır NONE varsayılır, 5 alanlı satırda NONE/HOME/AWAY okunur")
    void tc081_fourAndFiveFieldsAreValid() {
        // Sınır değer: 4 ve 5 alan geçerli (BV-13)
        List<MatchResult> result = loader.parseMatches(
                List.of("A;B;2;1", "A;C;1;0;NONE", "B;C;0;0;HOME", "C;A;5;2;AWAY"), ABC, "matches.csv");
        assertEquals(List.of(
                new MatchResult(A, B, 2, 1, Forfeit.NONE),
                new MatchResult(A, C, 1, 0, Forfeit.NONE),
                new MatchResult(B, C, 0, 0, Forfeit.HOME),
                new MatchResult(C, A, 5, 2, Forfeit.AWAY)), result);
    }

    // ---------------------------------------------------------------- TC-082

    @Test
    @DisplayName("TC-082: takım adları ve sayılar etrafındaki boşluklar kırpılır (D-01, Q9)")
    void tc082_whitespaceIsTrimmed() {
        // D-01: ' Beşiktaş ' ile 'Beşiktaş' aynı takım; sayı çevresindeki boşluk da kırpılır
        List<Team> teams = List.of(new Team("Beşiktaş"), new Team("Göztepe"));
        List<MatchResult> result = loader.parseMatches(List.of(" Beşiktaş ;Göztepe; 2 ;1"), teams, "matches.csv");
        assertEquals(1, result.size());
        MatchResult m = result.get(0);
        assertEquals(new Team("Beşiktaş"), m.homeTeam());
        assertEquals(new Team("Göztepe"), m.awayTeam());
        assertEquals(2, m.homeGoals());
        assertEquals(1, m.awayGoals());
    }

    // ---------------------------------------------------------------- TC-083

    @Test
    @DisplayName("TC-083: boş satır, yalnızca boşluklu satır ve CRLF sonuçları değiştirmez")
    void tc083_blankLinesAndCrlfIgnored() throws IOException {
        // CRLF'te '\r' son alana yapışmamalı ("1\r" sayı değil, "A\r" != "A")
        Path clean = write("clean.csv", "A;B;2;1\nB;C;0;0;HOME\n");
        Path messy = write("messy.csv", "A;B;2;1\r\n\r\n   \t \r\nB;C;0;0;HOME\r\n");
        assertEquals(loader.loadMatches(clean, ABC), loader.loadMatches(messy, ABC));
    }

    @Test
    @DisplayName("TC-083b: takım dosyasında boş ve boşluklu satırlar atlanır")
    void tc083b_blankLinesInTeamsIgnored() {
        List<Team> teams = loader.parseTeams(List.of("A", "", "   ", "B"), "teams.csv");
        assertEquals(List.of(A, B), teams);
    }

    // ---------------------------------------------------------------- TC-084

    private void assertRejectedAtLine3(Runnable action, String fileName) {
        InputFormatException e = assertThrows(InputFormatException.class, action::run);
        assertTrue(e.getMessage().contains(fileName), "dosya adı yok: " + e.getMessage());
        assertTrue(e.getMessage().contains("3"), "satır numarası (3) yok: " + e.getMessage());
    }

    @Test
    @DisplayName("TC-084a: teams dosyasında 2 alanlı satır reddedilir, dosya adı ve satır 3 mesajda")
    void tc084a_teamsTwoFields() {
        // Negatif: teams tam 1 alan olmalı; fiziksel satır numarası boş satırı da sayar
        assertRejectedAtLine3(() -> loader.parseTeams(List.of("A", "", "A;B"), "teams.csv"), "teams.csv");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"A;B;1", "A;B;1;0;NONE;X"})
    @DisplayName("TC-084b: matches dosyasında 3 ve 6 alanlı satırlar reddedilir (BV-13)")
    void tc084b_matchesWrongFieldCount(String badLine) {
        // Sınır: geçerli aralığın (4..5) hemen dışı
        assertRejectedAtLine3(
                () -> loader.parseMatches(List.of("A;B;1;0", "", badLine), ABC, "matches.csv"), "matches.csv");
    }

    @Test
    @DisplayName("TC-084c: matches dosyasında 7 alanlı satır da reddedilir")
    void tc084c_matchesSevenFields() {
        assertRejectedAtLine3(
                () -> loader.parseMatches(List.of("A;B;1;0", "", "A;B;1;0;NONE;X;Y"), ABC, "matches.csv"),
                "matches.csv");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"A", "A;7;X"})
    @DisplayName("TC-084d: deductions dosyasında 1 ve 3 alanlı satırlar reddedilir (BV-14)")
    void tc084d_deductionsWrongFieldCount(String badLine) {
        // Not: plandaki 'A;3;X' yerine 'A;7;X' kullanıldı; mesajdaki '3' satır numarasından gelsin diye
        assertRejectedAtLine3(
                () -> loader.parseDeductions(List.of("A;2", "", badLine), ABC, "deductions.csv"),
                "deductions.csv");
    }

    @Test
    @DisplayName("TC-084e: hatalı satır dosyadan okunduğunda da dosya adı ve satır numarası bildirilir")
    void tc084e_fileBasedMessage() throws IOException {
        Path file = write("matches.csv", "A;B;1;0\n\nA;B;1\n");
        assertRejectedAtLine3(() -> loader.loadMatches(file, ABC), "matches.csv");
    }

    @Test
    @DisplayName("TC-084f: Reader tabanlı aşırı yükleme de aynı hatayı üretir")
    void tc084f_readerOverload() {
        InputFormatException e = assertThrows(InputFormatException.class,
                () -> loader.parseMatches(new StringReader("A;B;1;0\n\nA;B;1\n"), ABC, "matches.csv"));
        assertTrue(e.getMessage().contains("matches.csv"));
        assertTrue(e.getMessage().contains("3"));
    }

    // ---------------------------------------------------------------- TC-085

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"A;B;x;1", "A;B;1.5;1", "A;B;;1", "A;B;1;y", "A;B;1;"})
    @DisplayName("TC-085a: tamsayı olmayan gol (x, 1.5, boş) dosya ve satır ile reddedilir")
    void tc085a_nonIntegerGoals(String badLine) {
        // Negatif: sayı alanı tamsayı olmalı; satır 2 çünkü ilk satır geçerli
        InputFormatException e = assertThrows(InputFormatException.class,
                () -> loader.parseMatches(List.of("A;B;1;0", badLine), ABC, "matches.csv"));
        assertTrue(e.getMessage().contains("matches.csv"), e.getMessage());
        assertTrue(e.getMessage().contains("2"), e.getMessage());
    }

    @Test
    @DisplayName("TC-085b: puan silme dosyasında tamsayı olmayan puan reddedilir")
    void tc085b_nonIntegerDeduction() {
        InputFormatException e = assertThrows(InputFormatException.class,
                () -> loader.parseDeductions(List.of("A;abc"), ABC, "deductions.csv"));
        assertTrue(e.getMessage().contains("deductions.csv"), e.getMessage());
        assertTrue(e.getMessage().contains("1"), e.getMessage());
    }

    // ---------------------------------------------------------------- TC-086

    @Test
    @DisplayName("TC-086a: bilinmeyen hükmen değeri (XYZ) reddedilir")
    void tc086a_unknownForfeit() {
        InputFormatException e = assertThrows(InputFormatException.class,
                () -> loader.parseMatches(List.of("A;B;1;0;XYZ"), ABC, "matches.csv"));
        assertTrue(e.getMessage().contains("matches.csv"), e.getMessage());
    }

    @Test
    @DisplayName("TC-086b: küçük harfli 'home' reddedilir (Q9: büyük/küçük harf duyarlı)")
    void tc086b_lowercaseForfeitRejected() {
        // Belirsizlik Q9: kararlaştırılan çözüm küçük harfi reddetmek
        assertThrows(InputFormatException.class,
                () -> loader.parseMatches(List.of("A;B;1;0;home"), ABC, "matches.csv"));
    }

    // ---------------------------------------------------------------- TC-087

    @Test
    @DisplayName("TC-087: boş 5. alan ('A;B;1;0;') NONE kabul edilir (Q9)")
    void tc087_emptyFifthFieldIsNone() {
        // Boş isteğe bağlı alan: varsayılan NONE
        List<MatchResult> result = loader.parseMatches(List.of("A;B;1;0;"), ABC, "matches.csv");
        assertEquals(List.of(new MatchResult(A, B, 1, 0, Forfeit.NONE)), result);
    }

    // ---------------------------------------------------------------- TC-088

    @Test
    @DisplayName("TC-088: CSV'den gelen negatif gol reddedilir (V-05, Q10: herhangi bir ret kabul)")
    void tc088_negativeGoalsRejected() {
        // Negatif: model katmanı V-05 ile mi, yükleyici V-08 ile mi reddettiği önemli değil
        assertThrows(RuntimeException.class,
                () -> loader.parseMatches(List.of("A;B;-1;0"), ABC, "matches.csv"));
    }

    // ---------------------------------------------------------------- TC-089

    @Test
    @DisplayName("TC-089a: 'Beşiktaş;3' geçerli PointDeduction üretir")
    void tc089a_validDeduction() {
        List<Team> teams = List.of(new Team("Beşiktaş"));
        List<PointDeduction> result = loader.parseDeductions(List.of("Beşiktaş;3"), teams, "deductions.csv");
        assertEquals(List.of(new PointDeduction(new Team("Beşiktaş"), 3)), result);
    }

    @Test
    @DisplayName("TC-089b: 'Beşiktaş;0' sıfır puan sınır değeri reddedilir (V-07)")
    void tc089b_zeroDeductionRejected() {
        // Sınır: 0 geçersiz, 1 geçerli (V-07)
        List<Team> teams = List.of(new Team("Beşiktaş"));
        assertThrows(InputFormatException.class,
                () -> loader.parseDeductions(List.of("Beşiktaş;0"), teams, "deductions.csv"));
    }

    @Test
    @DisplayName("TC-089c: puan silme dosyasında bilinmeyen takım reddedilir (V-03/V-07)")
    void tc089c_unknownTeamInDeductions() {
        assertThrows(RuntimeException.class,
                () -> loader.parseDeductions(List.of("Z;3"), ABC, "deductions.csv"));
    }

    @Test
    @DisplayName("TC-089d: maç dosyasında kayıtlı olmayan takım reddedilir (V-03)")
    void tc089d_unknownTeamInMatches() {
        InputFormatException e = assertThrows(InputFormatException.class,
                () -> loader.parseMatches(List.of("A;Z;1;0"), ABC, "matches.csv"));
        assertTrue(e.getMessage().contains("Z"), e.getMessage());
    }

    // ---------------------------------------------------------------- TC-090

    @Test
    @DisplayName("TC-090: olmayan teams/matches/deductions dosyası InputFormatException, mesajda dosya adı")
    void tc090_missingFiles() {
        Path teams = dir.resolve("no-teams.csv");
        Path matches = dir.resolve("no-matches.csv");
        Path deductions = dir.resolve("no-deductions.csv");
        // Negatif: IOException ham olarak sızmamalı
        assertTrue(assertThrows(InputFormatException.class, () -> loader.loadTeams(teams))
                .getMessage().contains("no-teams.csv"));
        assertTrue(assertThrows(InputFormatException.class, () -> loader.loadMatches(matches, ABC))
                .getMessage().contains("no-matches.csv"));
        assertTrue(assertThrows(InputFormatException.class, () -> loader.loadDeductions(deductions, ABC))
                .getMessage().contains("no-deductions.csv"));
    }
}
