package edu.svv.standings.cli;

import edu.svv.standings.generator.ScenarioLibrary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Komut satırı `--scenario <id|random> [seed]` testleri (TC-130..TC-134). */
class MainScenarioTest {

    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

    private int run(String... args) {
        outBytes.reset();
        errBytes.reset();
        return Main.run(args, new PrintStream(outBytes, true, StandardCharsets.UTF_8),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8));
    }

    private String out() {
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }

    /** Sıra numarasıyla başlayan veri satırları (başlık ve senaryo satırı sayılmaz). */
    private long dataRows(String output) {
        return Arrays.stream(output.split("\\R")).map(String::trim).filter(l -> l.matches("\\d+\\s+.*")).count();
    }

    @Test
    @DisplayName("TC-130 – --scenario S01: çıkış 0, stderr boş, 18 tablo satırı, senaryo başlığı basılır")
    void tc130_scenarioS01() {
        int code = run("--scenario", "S01");
        assertEquals(0, code, err());
        assertEquals("", err());
        assertEquals(18, dataRows(out()), out());
        assertTrue(out().startsWith("Scenario S01"), out());
    }

    @Test
    @DisplayName("TC-130b – eşitlik bayraklı senaryo (S04) çözülemez açıklamasını basar; 18 satır")
    void tc130b_scenarioWithUnresolved() {
        // S04 katalogda LIKELY_UNRESOLVED; tablo biçimleyici açıklama satırını eklemeli
        assertEquals(0, run("--scenario", "S04"), err());
        assertEquals(18, dataRows(out()));
        assertTrue(out().contains("unresolved"), out());
    }

    @Test
    @DisplayName("TC-131 – --scenario random 42 deterministik: iki çalıştırma aynı çıktıyı verir, seçilen kimlik pickRandomId(42) ile aynı")
    void tc131_randomSeeded() {
        assertEquals(0, run("--scenario", "random", "42"), err());
        String first = out();
        assertEquals(0, run("--scenario", "random", "42"), err());
        assertEquals(first, out());
        assertEquals(18, dataRows(first));
        String expectedId = new ScenarioLibrary().pickRandomId(42);
        assertTrue(first.startsWith("Scenario " + expectedId + " (picked with seed 42)"), first);
    }

    @ParameterizedTest
    @ValueSource(strings = {"S99", "s01", "S00"})
    @DisplayName("TC-132 – bilinmeyen senaryo: çıkış 1, hata mesajı stderr'de, tablo yok")
    void tc132_unknownScenario(String id) {
        int code = run("--scenario", id);
        assertEquals(1, code);
        assertFalse(err().isBlank());
        assertTrue(err().contains(id), err());
        assertEquals("", out());
    }

    @Test
    @DisplayName("TC-133 – --scenario S01 5: seed yalnız random ile verilebilir; çıkış 2 (kullanım), tablo yok")
    void tc133_seedWithFixedScenario() {
        int code = run("--scenario", "S01", "5");
        assertEquals(2, code);
        assertTrue(err().contains("Usage"), err());
        assertEquals("", out());
    }

    @Test
    @DisplayName("TC-134 – diğer kullanım hataları: --scenario tek başına, seed sayı değil, fazla argüman → çıkış 2")
    void tc134_otherUsageErrors() {
        for (String[] args : new String[][] {
                {"--scenario"}, {"--scenario", "random", "abc"}, {"--scenario", "random", "42", "7"}}) {
            int code = run(args);
            assertEquals(2, code, Arrays.toString(args));
            assertEquals("", out(), Arrays.toString(args));
            assertFalse(err().isBlank(), Arrays.toString(args));
        }
    }
}
