package edu.svv.standings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static edu.svv.standings.TieFixtures.assertTable;
import static edu.svv.standings.TieFixtures.calculate;
import static edu.svv.standings.TieFixtures.m;
import static edu.svv.standings.TieFixtures.teams;
import static edu.svv.standings.model.Forfeit.AWAY;
import static edu.svv.standings.model.Forfeit.HOME;

/**
 * İki takımlı puan eşitliği: FMT 9/2-a karar tablosu (test planı 4.1, TC-050..061).
 */
@DisplayName("Tie-break: iki takım (FMT 9/2-a)")
class TieBreakTwoTeamTest {

    @Nested
    @DisplayName("R1: ikili puan (kriter I)")
    class HeadToHeadPoints {

        // R1: iki maçlık ikili puan (A 4, B 1), genel averaj B lehine (+2 > 0) olsa da ikili puan önce gelir.
        @Test
        @DisplayName("TC-050 h2h points over two legs beat better overall GD")
        void tc050() {
            assertTable(calculate(teams("A", "B", "X"),
                            m("A", "B", 2, 1), m("B", "A", 1, 1), m("X", "A", 1, 0), m("B", "X", 3, 0)),
                    "A", "B", "X");
        }

        // R1: tek karşılaşma (B kazandı); genel averaj A lehine (+3 > +1) olduğundan yanlış kriter sırası ayrışır.
        @Test
        @DisplayName("TC-051 single mutual match decides, GD would pick A")
        void tc051() {
            assertTable(calculate(teams("A", "B", "X"), m("B", "A", 1, 0), m("A", "X", 4, 0)),
                    "B", "A", "X");
        }
    }

    @Nested
    @DisplayName("R2/R2': ikili averaj (kriter II)")
    class HeadToHeadGoalDifference {

        // R2: ikili puanlar eşit (3-3), ikili averaj A +2 / B -2; genel averaj ise B lehine (+4 > +3).
        @Test
        @DisplayName("TC-052 h2h GD before overall GD")
        void tc052() {
            assertTable(calculate(teams("A", "B", "X"),
                            m("A", "B", 3, 0), m("B", "A", 1, 0), m("A", "X", 1, 0), m("B", "X", 6, 0)),
                    "A", "B", "X");
        }

        // R2': ikili averaj 0-0 eşit (deplasman golü sayılmaz); genel averaj A +2 > B +1 karar verir.
        // Deplasman golü kuralı uygulansaydı B öne geçerdi.
        @Test
        @DisplayName("TC-053 away goals do not count in h2h")
        void tc053() {
            assertTable(calculate(teams("A", "B", "X"),
                            m("A", "B", 2, 1), m("B", "A", 1, 0), m("A", "X", 2, 0), m("B", "X", 1, 0)),
                    "A", "B", "X");
        }
    }

    @Nested
    @DisplayName("R3/R4: genel averaj ve genel atılan gol (kriter III, IV)")
    class OverallCriteria {

        // R3: ikili eşit; genel averaj B +2 > A +1 (1 farkla). Atılan gol A lehine (5 > 3) olduğundan sıra yanlışsa ayrışır.
        @Test
        @DisplayName("TC-054 overall GD before overall GF")
        void tc054() {
            assertTable(calculate(teams("A", "B", "X"),
                            m("A", "B", 1, 1), m("A", "X", 4, 3), m("B", "X", 2, 0)),
                    "B", "A", "X");
        }

        // R4: ikili ve genel averaj eşit (+1); atılan gol B 3 > A 2 (1 farkla) karar verir.
        @Test
        @DisplayName("TC-055 overall GF decides when GD equal")
        void tc055() {
            assertTable(calculate(teams("A", "B", "X"),
                            m("A", "B", 1, 1), m("A", "X", 1, 0), m("B", "X", 2, 1)),
                    "B", "A", "X");
        }
    }

    @Nested
    @DisplayName("R5/R6/R7: hükmen yenilgisi ve çözülemeyen durumlar (kriter V, VI)")
    class ForfeitAndUnresolved {

        // R5: A ve B tüm sayısal kriterlerde eşit (hükmen 0-0 -> 3-0 olur); yalnızca A'nın hükmen yenilgisi var, B önde.
        @Test
        @DisplayName("TC-056 team without forfeit loss ranks higher")
        void tc056() {
            assertTable(calculate(teams("A", "B", "X", "Y"),
                            m("A", "B", 0, 0), m("X", "A", 0, 0, AWAY), m("Y", "B", 3, 0), m("X", "Y", 1, 0)),
                    "X", "Y", "B", "A");
        }

        // Hükmen kriteri en sonda uygulanır: A hükmen yenik olsa da genel averajı (+2 > +1) daha iyi, A önde.
        @Test
        @DisplayName("TC-057 forfeit criterion is applied last, after GD")
        void tc057() {
            assertTable(calculate(teams("A", "B", "X", "Y"),
                            m("A", "B", 0, 0), m("X", "A", 0, 0, AWAY), m("A", "Y", 5, 0), m("B", "Y", 1, 0)),
                    "A", "B", "X", "Y");
        }

        // R6: iki takımın da hükmen yenilgisi var; hükmen kriteri ayıramaz -> çözülemedi, görüntü alfabetik (D-02).
        @Test
        @DisplayName("TC-058 both teams with forfeit loss stay unresolved")
        void tc058() {
            assertTable(calculate(teams("B", "A", "X"),
                            m("A", "B", 0, 0), m("X", "A", 0, 0, AWAY), m("X", "B", 0, 0, AWAY)),
                    "X", "A!", "B!");
        }

        // R7: tüm kriterler eşit, hükmen yok -> çözülemedi; girdi sırası (B, X, A) görüntü sırasını etkilememeli.
        @Test
        @DisplayName("TC-059 all criteria equal: unresolved, alphabetical display")
        void tc059() {
            assertTable(calculate(teams("B", "X", "A"),
                            m("A", "B", 1, 1), m("A", "X", 2, 0), m("B", "X", 2, 0)),
                    "A!", "B!", "X");
        }
    }

    @Nested
    @DisplayName("Ek kurallar: sıfır ortak maç, bağımsız gruplar")
    class ExtraRules {

        // A-03: A ve B hiç karşılaşmadı; ikili kriterler eşit sayılır, genel averaja (B +2 > A +1) geçilir.
        @Test
        @DisplayName("TC-060 no mutual match: skip to overall GD")
        void tc060() {
            assertTable(calculate(teams("A", "B", "X"), m("A", "X", 1, 0), m("X", "B", 0, 2)),
                    "B", "A", "X");
        }

        // Aynı tabloda iki bağımsız eşitlik grubu (A/B 7 puan, C/D 1 puan) ayrı ayrı çözülür.
        @Test
        @DisplayName("TC-061 two independent tie groups in one table")
        void tc061() {
            assertTable(calculate(teams("A", "B", "C", "D"),
                            m("A", "C", 2, 0), m("B", "D", 1, 0), m("A", "D", 1, 0),
                            m("B", "C", 3, 0), m("A", "B", 0, 0), m("C", "D", 1, 1)),
                    "B", "A", "D", "C");
        }
    }
}
