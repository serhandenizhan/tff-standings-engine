package edu.svv.standings;

import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bağımsız kod incelemesinde (2026-11-13) bulunan kenar durumları için testler (TC-160, TC-161).
 * Testler düzeltmeden önce yazıldı; beklenen davranış gereksinimlerden gelir.
 */
class ReviewDefectsTest {

    private final StandingsCalculator calculator = new StandingsCalculator();

    @Test
    @DisplayName("TC-160 – adında '|' olan farklı eşleşmeler tekrar eden maç sayılmamalı (V-06, EP: özel karakterli ad)")
    void tc160_pipeInNamesIsNotDuplicateFixture() {
        // "A|B"–"C" ve "A"–"B|C" farklı eşleşmelerdir; isimleri birleştiren anahtar ikisini de "A|B|C" yapar
        Team ab = new Team("A|B"), c = new Team("C"), a = new Team("A"), bc = new Team("B|C");
        List<MatchResult> matches = List.of(new MatchResult(ab, c, 1, 0), new MatchResult(a, bc, 2, 2));
        List<StandingRow> table = assertDoesNotThrow(() -> calculator.calculate(List.of(ab, c, a, bc), matches, List.of()));
        assertEquals(4, table.size());
    }

    @Test
    @DisplayName("TC-160b – '|' içeren adlarla iki takım arasındaki yasal iki maç ve başka bir maç birlikte kabul edilmeli (V-06)")
    void tc160b_pipeInNamesPairCount() {
        // ikili sayaç da aynı birleştirme anahtarını kullanıyor; üç yasal maç yanlışlıkla "ikiden fazla" sayılmamalı
        Team ab = new Team("A|B"), c = new Team("C"), a = new Team("A"), bc = new Team("B|C");
        List<MatchResult> matches = List.of(
                new MatchResult(ab, c, 1, 0), new MatchResult(c, ab, 0, 0), new MatchResult(a, bc, 3, 1));
        assertDoesNotThrow(() -> calculator.calculate(List.of(ab, c, a, bc), matches, List.of()));
    }

    @Test
    @DisplayName("TC-161 – toplam puan silme int aralığını aşarsa açık bir hatayla reddedilmeli, puan pozitife dönmemeli (V-09, BVA: Integer.MAX_VALUE)")
    void tc161_deductionOverflowRejected() {
        // sınır değer: tek silme Integer.MAX_VALUE geçerli bir pozitif sayıdır; iki tanesinin toplamı taşar
        Team p = new Team("P"), q = new Team("Q");
        List<MatchResult> matches = List.of(new MatchResult(p, q, 1, 0));
        List<PointDeduction> deductions = List.of(
                new PointDeduction(p, Integer.MAX_VALUE), new PointDeduction(p, Integer.MAX_VALUE));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(List.of(p, q), matches, deductions));
        assertTrue(e.getMessage().contains("P"), "mesaj takımı adlandırmalı: " + e.getMessage());
    }

    @Test
    @DisplayName("TC-161b – toplam gol int aralığını aşarsa reddedilmeli, negatif gol sayısı oluşmamalı (V-09, BVA)")
    void tc161b_goalOverflowRejected() {
        // her maç tek başına geçerli; iki maçın gol toplamı taşar
        Team p = new Team("P"), q = new Team("Q");
        List<MatchResult> matches = List.of(
                new MatchResult(p, q, Integer.MAX_VALUE, 0), new MatchResult(q, p, 0, 1));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(List.of(p, q), matches, List.of()));
        // P'nin attığı her gol Q'nun yediği goldür: iki takımın toplamı da taşar, hangisi önce işlenirse o adlandırılır
        assertTrue(e.getMessage().contains("P") || e.getMessage().contains("Q"),
                "mesaj taşan takımı adlandırmalı: " + e.getMessage());
    }
}
