package edu.svv.standings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static edu.svv.standings.TieFixtures.assertTable;
import static edu.svv.standings.TieFixtures.calculate;
import static edu.svv.standings.TieFixtures.m;
import static edu.svv.standings.TieFixtures.teams;
import static edu.svv.standings.model.Forfeit.AWAY;

/**
 * Üç ve daha fazla takımlı puan eşitliği: FMT 9/2-b karar tablosu (test planı 4.2, TC-065..079).
 */
@DisplayName("Tie-break: üç ve daha fazla takım (FMT 9/2-b)")
class TieBreakMultiTeamTest {

    @Nested
    @DisplayName("M1..M5: kriter sırası (mini puan, mini averaj, mini gol, genel averaj, genel gol)")
    class CriterionOrder {

        // M1: mini puan tek başına ayırır (C 6, B 3, A 0); X'e karşı maçlar mini tabloya girmez, genel averaj ters sıra verirdi.
        @Test
        @DisplayName("TC-065 mini-table points decide, matches vs X ignored")
        void tc065() {
            assertTable(calculate(teams("A", "B", "C", "X"),
                            m("C", "B", 1, 0), m("C", "A", 1, 0), m("B", "A", 1, 0),
                            m("C", "X", 0, 1), m("X", "C", 1, 0), m("B", "X", 1, 0),
                            m("X", "B", 1, 0), m("A", "X", 3, 0), m("X", "A", 0, 3)),
                    "X", "C", "B", "A");
        }

        // M2: mini puanlar eşit (3'er); mini averaj A +2, C 0, B -2. Genel averaj B > A > C olduğundan sıra ayrışır.
        @Test
        @DisplayName("TC-066 mini GD before overall GD")
        void tc066() {
            assertTable(calculate(teams("A", "B", "C", "X"),
                            m("A", "B", 3, 0), m("B", "C", 1, 0), m("C", "A", 1, 0),
                            m("A", "X", 1, 0), m("B", "X", 6, 0), m("C", "X", 2, 0)),
                    "A", "C", "B", "X");
        }

        // M3: mini puan ve mini averaj eşit; mini atılan gol C 4 > B 3 > A 2 (genel averajın tam tersi).
        @Test
        @DisplayName("TC-067 mini GF before overall criteria")
        void tc067() {
            assertTable(calculate(teams("A", "B", "C", "X"),
                            m("A", "B", 1, 0), m("B", "C", 3, 2), m("C", "A", 2, 1),
                            m("A", "X", 5, 0), m("B", "X", 2, 0), m("C", "X", 1, 0)),
                    "C", "B", "A", "X");
        }

        // M4: mini tablo tamamen eşit; genel averaja düşülür (B +3 > C +2 > A +1).
        @Test
        @DisplayName("TC-068 mini all equal: falls to overall GD")
        void tc068() {
            assertTable(calculate(teams("A", "B", "C", "X"),
                            m("A", "B", 1, 0), m("B", "C", 1, 0), m("C", "A", 1, 0),
                            m("A", "X", 1, 0), m("B", "X", 3, 0), m("C", "X", 2, 0)),
                    "B", "C", "A", "X");
        }

        // M5: mini tablo ve genel averaj (+1) eşit; genel atılan gol karar verir (C 4 > A 3 > B 2).
        @Test
        @DisplayName("TC-069 overall GF as last numeric criterion")
        void tc069() {
            assertTable(calculate(teams("A", "B", "C", "X"),
                            m("A", "B", 1, 0), m("B", "C", 1, 0), m("C", "A", 1, 0),
                            m("A", "X", 2, 1), m("B", "X", 1, 0), m("C", "X", 3, 2)),
                    "C", "A", "B", "X");
        }
    }

    @Nested
    @DisplayName("M6/M7: hükmen kriteri ve çözülemeyen gruplar")
    class ForfeitAndUnresolved {

        // M6: mini ve genel kriterler eşit; yalnızca A hükmen yenik -> A sona gider, B/C çözülemedi (ortada blok), A bayraksız.
        @Test
        @DisplayName("TC-070 partial resolution: unresolved block in the middle")
        void tc070() {
            assertTable(calculate(teams("C", "A", "X", "B"),
                            m("A", "B", 0, 0), m("B", "C", 0, 0), m("C", "A", 0, 0),
                            m("X", "A", 0, 0, AWAY), m("X", "B", 3, 0), m("X", "C", 3, 0)),
                    "X", "B!", "C!", "A");
        }

        // M7: mükemmel döngü (A>B>C>A); hiçbir kriter ayıramaz -> üçü de çözülemedi, alfabetik görüntü.
        @Test
        @DisplayName("TC-071 perfect cycle: all unresolved")
        void tc071() {
            assertTable(calculate(teams("C", "B", "A"),
                            m("A", "B", 1, 0), m("B", "C", 1, 0), m("C", "A", 1, 0)),
                    "A!", "B!", "C!");
        }
    }

    @Nested
    @DisplayName("M8/M9 (FR-11): alt eşitlikte maçlara tekrar bakılmaz")
    class NoReLookAtSubTies {

        // M8: mini puanla C ayrılır, A/B mini averajla (-1 > -3) ayrılır; A/B'nin ikili maçına (0-0) dönülseydi genel averajla B öne geçerdi.
        @Test
        @DisplayName("TC-072 sub-tie A/B resolved inside the same mini-table")
        void tc072() {
            assertTable(calculate(teams("A", "B", "C", "X"),
                            m("A", "B", 0, 0), m("C", "A", 1, 0), m("C", "B", 3, 0),
                            m("A", "X", 1, 0), m("X", "A", 0, 1), m("B", "X", 5, 0),
                            m("X", "B", 0, 3), m("C", "X", 0, 0), m("X", "C", 1, 0)),
                    "C", "A", "B", "X");
        }

        // M9 (anahtar vaka): mini gol C'yi öne alır; A/B hâlâ eşitken ikili maça (A 1-0 B) DÖNÜLMEZ, genel averaj (B +3 > A +1) karar verir.
        // Özyinelemeli ikili kural C, A, B; mini golü atlayan hata B, C, A verirdi.
        @Test
        @DisplayName("TC-073 FR-11: no re-look at A vs B, overall GD decides")
        void tc073() {
            assertTable(calculate(teams("A", "B", "C", "D"),
                            m("A", "B", 1, 0), m("C", "A", 2, 1), m("B", "C", 2, 1),
                            m("A", "D", 1, 0), m("B", "D", 3, 0), m("C", "D", 1, 0)),
                    "C", "B", "A", "D");
        }

        // Boyut sınırı 3 -> 2: tek sonuç (C-D 1-1) C'yi 4 puana düşürür, yalnızca A/B eşit kalır; ikili kural (A 1-0 B) A'yı öne alır.
        // TC-073 ile yan yana: tek maç sonucu algoritmayı değiştirir ve A/B sırasını çevirir.
        @Test
        @DisplayName("TC-074 group size 3 to 2 switches algorithm and flips A/B")
        void tc074() {
            assertTable(calculate(teams("A", "B", "C", "D"),
                            m("A", "B", 1, 0), m("C", "A", 2, 1), m("B", "C", 2, 1),
                            m("A", "D", 1, 0), m("B", "D", 3, 0), m("C", "D", 1, 1)),
                    "A", "B", "C", "D");
        }
    }

    @Nested
    @DisplayName("Ek kurallar: eksik mini tablo, 4 takım, hükmen düzeltmesi")
    class ExtraRules {

        // A-03: mini tabloda yalnızca A-B ve B-C maçları var (A-C yok); eksik tablo çalışmalı (A +2, B -1, C 0 puan). Genel averaj A, C, B verirdi.
        @Test
        @DisplayName("TC-075 incomplete mini-table")
        void tc075() {
            assertTable(calculate(teams("A", "B", "C", "X"),
                            m("A", "B", 2, 0), m("B", "C", 1, 0), m("C", "X", 1, 0)),
                    "A", "B", "C", "X");
        }

        // Dört takımlı eşitlik (hepsi 4 puan): prosedür 3 takıma sabitlenmemeli; mini averaj A +2, D 0, C -1 (GF 3), B -1 (GF 1).
        @Test
        @DisplayName("TC-076 four-team tie")
        void tc076() {
            assertTable(calculate(teams("A", "B", "C", "D"),
                            m("A", "B", 0, 0), m("C", "D", 1, 1), m("A", "C", 3, 0),
                            m("C", "B", 2, 0), m("B", "D", 1, 0), m("D", "A", 1, 0)),
                    "A", "D", "C", "B");
        }

        // FMT 23/4: B-C 0-0 hükmen 3-0 olur; mini tablo düzeltilmiş skoru kullanmalı (B +2, A 0, C -2). Ham skorla C, A, B çıkardı.
        @Test
        @DisplayName("TC-077 mini-table uses forfeit-adjusted score")
        void tc077() {
            assertTable(calculate(teams("A", "B", "C"),
                            m("A", "B", 1, 0), m("B", "C", 0, 0, AWAY), m("C", "A", 1, 0)),
                    "B", "A", "C");
        }
    }

    @Nested
    @DisplayName("D-02: Türkçe alfabetik görüntü sırası")
    class TurkishAlphabeticalOrder {

        // D-02: maçsız 11 takım, hepsi çözülemedi; ters girdi sırasından Türkçe alfabeye (Ç<D, I<İ<K, S<Ş, U<Ü) göre dizilmeli.
        // Unicode kod noktası sırası Ç, İ, Ş, Ü'yü Z'den sonra koyardı.
        @Test
        @DisplayName("TC-078 eleven teams in Turkish alphabetical order")
        void tc078() {
            assertTable(calculate(teams("Vanspor", "Ümraniyespor", "Trabzonspor", "Şanlıurfaspor", "Kasımpaşa",
                            "İstanbul Başakşehir", "Iğdır FK", "Denizlispor", "Çaykur Rizespor", "Beşiktaş",
                            "Ankaragücü")),
                    "Ankaragücü!", "Beşiktaş!", "Çaykur Rizespor!", "Denizlispor!", "Iğdır FK!",
                    "İstanbul Başakşehir!", "Kasımpaşa!", "Şanlıurfaspor!", "Trabzonspor!", "Ümraniyespor!",
                    "Vanspor!");
        }

        // D-02: TC-078'in en küçük hali; Ç harfi D'den önce gelir (kod noktası sırasında ise sonra gelirdi).
        @Test
        @DisplayName("TC-079 Ç sorts before D in a two-team tie")
        void tc079() {
            assertTable(calculate(teams("Denizlispor", "Çaykur Rizespor"),
                            m("Denizlispor", "Çaykur Rizespor", 1, 1)),
                    "Çaykur Rizespor!", "Denizlispor!");
        }
    }
}
