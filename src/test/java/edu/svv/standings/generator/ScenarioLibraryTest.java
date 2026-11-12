package edu.svv.standings.generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static edu.svv.standings.generator.GeneratorTestSupport.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Senaryo kütüphanesi testleri (TC-124..TC-128). */
class ScenarioLibraryTest {

    private static final ScenarioLibrary LIBRARY = new ScenarioLibrary();

    static Stream<String> ids() {
        return LIBRARY.ids().stream();
    }

    @Test
    @DisplayName("TC-124 – kütüphane S01..S12 kimliklerini sırayla döndürür")
    void tc124_ids() {
        List<String> expected = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(i -> String.format("S%02d", i)).toList();
        assertEquals(expected, LIBRARY.ids());
        assertEquals(12, LIBRARY.entries().size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ids")
    @DisplayName("TC-125 – her senaryo yüklenir, 18 takım ve 9·tur maç içerir, hesaplayıcı doğrulamasından geçer")
    void tc125_everyScenarioLoadsAndValidates(String id) {
        GeneratedSeason s = LIBRARY.load(id);
        assertEquals(18, s.teams().size());
        assertEquals(9 * LIBRARY.entry(id).rounds(), s.matches().size());
        assertEquals(18, table(s).size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ids")
    @DisplayName("TC-126 – kütüphane senaryosu, katalogdaki tohum ve tur sayısıyla yeniden üretilen sezonla aynı (belgelenen deterministik üretim)")
    void tc126_reproducibleFromCatalogue(String id) {
        // docs/generator.md: aynı seed => aynı dosyalar; CSV'ye yazılıp geri okunan sezon üretilenle birebir aynı olmalı
        ScenarioLibrary.Entry e = LIBRARY.entry(id);
        GeneratedSeason fromLibrary = LIBRARY.load(id);
        GeneratedSeason regenerated = new SeasonGenerator(e.seed()).generate(e.rounds());
        assertEquals(regenerated.teams(), fromLibrary.teams(), "takımlar");
        assertEquals(regenerated.matches(), fromLibrary.matches(), "maçlar");
        assertEquals(regenerated.deductions(), fromLibrary.deductions(), "puan silmeler");
    }

    @Test
    @DisplayName("TC-127 – pickRandomId(seed) deterministik, daima geçerli kimlik; 2000 tohumda 12 kimliğin hepsi görülür")
    void tc127_pickRandomId() {
        Set<String> seen = new HashSet<>();
        for (long seed = -500; seed < 1500; seed++) {
            String id = LIBRARY.pickRandomId(seed);
            assertTrue(LIBRARY.ids().contains(id), "geçersiz kimlik: " + id + " seed " + seed);
            assertEquals(id, LIBRARY.pickRandomId(seed), "deterministik değil, seed " + seed);
            seen.add(id);
        }
        assertEquals(new HashSet<>(LIBRARY.ids()), seen);
        assertEquals(LIBRARY.pickRandomId(42), new ScenarioLibrary().pickRandomId(42));
    }

    @ParameterizedTest
    @ValueSource(strings = {"S99", "S00", "S13", "", "s01", "S1", " S01"})
    @NullSource
    @DisplayName("TC-128 – bilinmeyen senaryo kimliği reddedilir (load ve entry), büyük/küçük harf ve boşluk dahil")
    void tc128_unknownId(String id) {
        assertThrows(IllegalArgumentException.class, () -> LIBRARY.load(id));
        assertThrows(IllegalArgumentException.class, () -> LIBRARY.entry(id));
    }
}
