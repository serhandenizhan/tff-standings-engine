package edu.svv.standings.generator;

import edu.svv.standings.StandingRow;
import edu.svv.standings.model.Forfeit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static edu.svv.standings.generator.GeneratorTestSupport.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Katalog bayrakları (catalogue.csv) ile hesaplayıcı çıktısının tutarlılığı. */
class CatalogueFlagsTest {

    private static final ScenarioLibrary LIBRARY = new ScenarioLibrary();

    static Stream<ScenarioLibrary.Entry> entries() {
        return LIBRARY.entries().stream();
    }

    static Stream<ScenarioLibrary.Entry> unresolvedEntries() {
        return LIBRARY.entries().stream().filter(e -> e.flags().contains("LIKELY_UNRESOLVED"));
    }

    static Stream<ScenarioLibrary.Entry> noTieEntries() {
        return LIBRARY.entries().stream().filter(e -> e.flags().contains("NO_TIE"));
    }

    private static Map<Integer, Integer> pointGroups(List<StandingRow> t) {
        Map<Integer, Integer> m = new HashMap<>();
        t.forEach(r -> m.merge(r.getPoints(), 1, Integer::sum));
        return m;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unresolvedEntries")
    @DisplayName("TC-108 – LIKELY_UNRESOLVED bayraklı senaryoda hesaplayıcı en az bir çözülemez grup işaretler")
    void tc108_likelyUnresolved(ScenarioLibrary.Entry e) {
        // Katalog sınıflandırıcısı bağımsızdır ve temkinlidir: işaretlediği her grubu hesaplayıcı da işaretlemeli
        List<StandingRow> t = table(LIBRARY.load(e.id()));
        long unresolved = t.stream().filter(StandingRow::isUnresolved).count();
        assertTrue(unresolved >= 2, e.id() + ": çözülemez satır sayısı " + unresolved);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("noTieEntries")
    @DisplayName("TC-109 – NO_TIE bayraklı senaryoda hiçbir iki takımın puanı eşit değil ve çözülemez takım yok")
    void tc109_noTie(ScenarioLibrary.Entry e) {
        List<StandingRow> t = table(LIBRARY.load(e.id()));
        assertTrue(pointGroups(t).values().stream().allMatch(n -> n == 1), e.id() + ": puan eşitliği var");
        assertTrue(t.stream().noneMatch(StandingRow::isUnresolved), e.id() + ": çözülemez takım var");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entries")
    @DisplayName("TC-110 – TIE_2, TIE_3PLUS, FORFEIT, DEDUCTION bayrakları veri ve hesaplayıcı çıktısıyla bire bir uyumlu; tur sayısı maç sayısını verir")
    void tc110_flagsMatchData(ScenarioLibrary.Entry e) {
        GeneratedSeason s = LIBRARY.load(e.id());
        Map<Integer, Integer> groups = pointGroups(table(s));
        boolean hasTwo = groups.values().stream().anyMatch(n -> n == 2);
        boolean hasThreePlus = groups.values().stream().anyMatch(n -> n >= 3);
        boolean hasForfeit = s.matches().stream().anyMatch(m -> m.forfeit() != Forfeit.NONE);
        boolean hasDeduction = !s.deductions().isEmpty();

        assertEquals(e.flags().contains("TIE_2"), hasTwo, e.id() + " TIE_2");
        assertEquals(e.flags().contains("TIE_3PLUS"), hasThreePlus, e.id() + " TIE_3PLUS");
        assertEquals(e.flags().contains("FORFEIT"), hasForfeit, e.id() + " FORFEIT");
        assertEquals(e.flags().contains("DEDUCTION"), hasDeduction, e.id() + " DEDUCTION");
        assertEquals(e.flags().contains("NO_TIE"), !hasTwo && !hasThreePlus, e.id() + " NO_TIE");
        assertEquals(9 * e.rounds(), s.matches().size(), e.id() + " maç sayısı");
        assertFalse(e.flags().isEmpty() && s.matches().isEmpty());
    }
}
