package edu.svv.standings.cli;

import edu.svv.standings.StandingRow;
import edu.svv.standings.StandingsCalculator;
import edu.svv.standings.model.Forfeit;
import edu.svv.standings.model.MatchResult;
import edu.svv.standings.model.PointDeduction;
import edu.svv.standings.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tablo biçimlendirici testleri (FR-13, FR-15). */
class TableFormatterTest {

    private static Team t(String name) {
        return new Team(name);
    }

    private static MatchResult m(String h, String a, int hg, int ag) {
        return new MatchResult(t(h), t(a), hg, ag);
    }

    private static List<StandingRow> table(List<Team> teams, List<MatchResult> ms, List<PointDeduction> ds) {
        return new StandingsCalculator().calculate(teams, ms, ds);
    }

    private static List<String> lines(String text) {
        return List.of(text.split("\\R"));
    }

    private static String[] tokens(String line) {
        return line.trim().split("\\s+");
    }

    /** Takım adı tek parçalı olduğunda ilgili veri satırını bulur. */
    private static String rowOf(List<String> lines, String team) {
        return lines.stream().filter(l -> tokens(l).length > 1 && tokens(l)[1].equals(team))
                .findFirst().orElseThrow(() -> new AssertionError("satır yok: " + team));
    }

    @Test
    @DisplayName("TC-091: başlık + 3 satır, sıra B, A, C ve tüm FR-13 alanları (TC-012 verisi)")
    void tc091_headerAndRowsInOrder() {
        List<Team> teams = List.of(t("A"), t("B"), t("C"));
        List<MatchResult> ms = List.of(m("A", "B", 2, 0), m("B", "A", 1, 1), m("A", "C", 0, 1),
                m("C", "A", 2, 2), m("B", "C", 3, 1), m("C", "B", 0, 1));
        String out = new TableFormatter().format(table(teams, ms, List.of()));
        List<String> ls = lines(out).stream().filter(l -> !l.isBlank()).toList();
        // Tüm FR-13 alanları: konum, takım, O, G, B, M, AG, YG, averaj, puan
        assertEquals(4, ls.size(), out);
        assertEquals(List.of("Pos", "Team", "P", "W", "D", "L", "GF", "GA", "GD", "Pts").size(), tokens(ls.get(0)).length);
        assertEquals(List.of("1", "B", "4", "2", "1", "1", "5", "4", "1", "7"), List.of(tokens(ls.get(1))));
        assertEquals(List.of("2", "A", "4", "1", "2", "1", "5", "4", "1", "5"), List.of(tokens(ls.get(2))));
        assertEquals(List.of("3", "C", "4", "1", "1", "2", "4", "6", "-2", "4"), List.of(tokens(ls.get(3))));
    }

    @Test
    @DisplayName("TC-092: uzun Türkçe adda bile her sütun tüm satırlarda aynı karakter indeksinde biter")
    void tc092_alignmentWithTurkishNames() {
        List<Team> teams = List.of(t("A"), t("İstanbul Başakşehir"));
        String out = new TableFormatter().format(table(teams, List.of(m("A", "İstanbul Başakşehir", 2, 0)), List.of()));
        List<String> ls = lines(out).stream().filter(l -> !l.isBlank()).toList();
        assertEquals(3, ls.size(), out);
        // Sayı sütunları sağa hizalı: satırların uzunluğu eşit olmalı (bayt değil karakter genişliği)
        assertEquals(ls.get(0).length(), ls.get(1).length(), out);
        assertEquals(ls.get(0).length(), ls.get(2).length(), out);
        // Başlıktaki "Pts" ile son sütundaki değer aynı indekste bitmeli
        int end = ls.get(0).stripTrailing().length();
        assertEquals(end, ls.get(1).stripTrailing().length(), out);
        assertEquals(end, ls.get(2).stripTrailing().length(), out);
        // Takım adı bozulmadan yazılmalı
        assertTrue(out.contains("İstanbul Başakşehir"));
    }

    @Test
    @DisplayName("TC-093: çözülemeyen satırlar (B, C) işaretlenir, X ve A işaretlenmez (TC-070 verisi)")
    void tc093_markerOnlyOnUnresolvedRows() {
        List<Team> teams = List.of(t("C"), t("A"), t("X"), t("B"));
        List<MatchResult> ms = List.of(m("A", "B", 0, 0), m("B", "C", 0, 0), m("C", "A", 0, 0),
                new MatchResult(t("X"), t("A"), 0, 0, Forfeit.AWAY), m("X", "B", 3, 0), m("X", "C", 3, 0));
        String out = new TableFormatter().format(table(teams, ms, List.of()));
        List<String> ls = lines(out);
        // İşaret metni belirsiz (Q11): yalnızca "*" gibi görünür bir işaret aranır, satır sonunda
        assertTrue(rowOf(ls, "B").contains("*"), out);
        assertTrue(rowOf(ls, "C").contains("*"), out);
        assertFalse(rowOf(ls, "X").contains("*"), out);
        assertFalse(rowOf(ls, "A").contains("*"), out);
    }

    @Test
    @DisplayName("TC-094: negatif puan (-1) ve averaj (-3) işaretli yazılır, sütunlar hizalı kalır")
    void tc094_negativeValuesAlignedWithSign() {
        // (a) TC-031(b): A -1 puan; (b) TC-010(b): A averaj -3
        List<Team> teamsA = List.of(t("A"), t("B"), t("C"));
        String outA = new TableFormatter().format(table(teamsA, List.of(m("A", "B", 0, 0)),
                List.of(new PointDeduction(t("A"), 2))));
        List<String> la = lines(outA).stream().filter(l -> !l.isBlank()).toList();
        String[] rowA = tokens(rowOf(la, "A"));
        assertEquals("-1", rowA[rowA.length - 1], outA);
        assertEquals(la.get(0).length(), la.get(1).length(), outA);
        assertEquals(la.get(0).length(), rowOf(la, "A").length(), outA);

        String outB = new TableFormatter().format(table(List.of(t("A"), t("B")), List.of(m("A", "B", 0, 3)), List.of()));
        List<String> lb = lines(outB).stream().filter(l -> !l.isBlank()).toList();
        String[] rowB = tokens(rowOf(lb, "A"));
        assertEquals("-3", rowB[rowB.length - 2], outB);
        for (String l : lb.subList(0, 3)) {
            assertEquals(lb.get(0).length(), l.length(), outB);
        }
    }

    @Test
    @DisplayName("TC-091b: boş tablo hata vermez ve yalnızca başlık içerir")
    void tc091b_emptyTable() {
        // Sınır: sıfır satır
        String out = new TableFormatter().format(new ArrayList<>());
        assertEquals(1, lines(out).stream().filter(l -> !l.isBlank()).count(), out);
    }
}
