package edu.svv.standings.io;

import edu.svv.standings.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kapsam analizinden sonra eklenen testler (TC-144..TC-147).
 * CsvLoader'daki okunamayan dosya / okuyucu hatası dalları V-08 ("eksik dosya" ve genel olarak okunamayan girdi)
 * gereksiniminden türetildi: her durumda ham IOException değil InputFormatException dönmelidir.
 */
class CoverageGapTest {

    @TempDir
    Path dir;

    private final CsvLoader loader = new CsvLoader();

    /** read() sırasında IOException fırlatan okuyucu. */
    private static final class FailingReader extends Reader {
        @Override
        public int read(char[] buf, int off, int len) throws IOException {
            throw new IOException("disk failure");
        }

        @Override
        public void close() {
        }
    }

    /** Boş içerik veren ama close() sırasında hata fırlatan okuyucu. */
    private static final class FailOnCloseReader extends Reader {
        @Override
        public int read(char[] buf, int off, int len) {
            return -1;
        }

        @Override
        public void close() throws IOException {
            throw new IOException("close failure");
        }
    }

    @Test
    @DisplayName("TC-144 – geçersiz UTF-8 baytları içeren dosya InputFormatException (okunamıyor) ile reddedilir")
    void tc144_invalidUtf8File() throws IOException {
        // FR-14: dosyalar UTF-8'dir; V-08: okunamayan girdi sessizce bozuk takım adına dönüşmemeli
        Path file = dir.resolve("teams.csv");
        Files.write(file, new byte[] {'A', '\n', (byte) 0xC3, (byte) 0x28, '\n'});
        InputFormatException e = assertThrows(InputFormatException.class, () -> loader.loadTeams(file));
        assertTrue(e.getMessage().contains("Cannot read file"), e.getMessage());
    }

    @Test
    @DisplayName("TC-145 – dosya yerine dizin verilirse InputFormatException fırlatılır (ham IOException sızmaz)")
    void tc145_directoryInsteadOfFile() {
        InputFormatException e = assertThrows(InputFormatException.class, () -> loader.loadTeams(dir));
        assertTrue(e.getMessage().contains("Cannot read file"), e.getMessage());
    }

    @Test
    @DisplayName("TC-146 – okuyucu okuma sırasında IOException verirse kaynak adıyla InputFormatException fırlatılır")
    void tc146_readerFailsWhileReading() {
        InputFormatException e = assertThrows(InputFormatException.class,
                () -> loader.parseTeams(new FailingReader(), "teams.csv"));
        assertTrue(e.getMessage().contains("Cannot read teams.csv"), e.getMessage());
    }

    @Test
    @DisplayName("TC-147 – okuyucu kapatılırken IOException verirse kaynak adıyla InputFormatException fırlatılır")
    void tc147_readerFailsOnClose() {
        List<Team> teams = List.of(new Team("A"), new Team("B"));
        InputFormatException e = assertThrows(InputFormatException.class,
                () -> loader.parseMatches(new FailOnCloseReader(), teams, "matches.csv"));
        assertTrue(e.getMessage().contains("Cannot read matches.csv"), e.getMessage());
    }
}
