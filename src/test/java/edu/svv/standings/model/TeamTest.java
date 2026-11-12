package edu.svv.standings.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TeamTest {

    // EP + BVA: geçerli isim sınıfları (Türkçe harf, iç boşluk, 1 karakterlik alt sınır); isim aynen korunmalı
    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"X", "Sivasspor", "Beşiktaş", "Çaykur Rizespor", "İstanbul Başakşehir"})
    @DisplayName("TC-001 valid names are accepted and kept exactly")
    void validNamesAreKeptExactly(String name) {
        assertEquals(name, new Team(name).name());
    }

    // NEG + BVA: boşluk kontrolü trim sonrası uzunluk 0 olan girdileri de yakalamalı
    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("TC-002 null, empty and blank names are rejected")
    void invalidNamesAreRejected(String name) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Team(name));
        assertEquals(false, ex.getMessage() == null || ex.getMessage().isBlank(), "message must not be empty");
    }

    // EP: D-01 trim sonrası birebir eşleşme; büyük/küçük harf duyarlı (Q6)
    @Test
    @DisplayName("TC-003 names are trimmed but case-sensitive")
    void namesAreTrimmedButCaseSensitive() {
        Team padded = new Team(" Beşiktaş ");
        Team plain = new Team("Beşiktaş");
        Team lower = new Team("beşiktaş");
        assertAll(
                () -> assertEquals("Beşiktaş", padded.name()),
                () -> assertEquals(plain, padded),
                () -> assertEquals(plain.hashCode(), padded.hashCode()),
                () -> assertNotEquals(plain, lower));
    }
}
