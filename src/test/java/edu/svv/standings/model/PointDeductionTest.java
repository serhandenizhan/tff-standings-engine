package edu.svv.standings.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PointDeductionTest {

    private static final Team A = new Team("A");

    // BVA: V-07 sınırı; 1 geçerli ilk değer, 0 ve -1 geçersiz
    @ParameterizedTest(name = "points={0} valid={1}")
    @CsvSource({"1,true", "0,false", "-1,false"})
    @DisplayName("TC-008 deducted points must be positive")
    void deductedPointsBoundary(int points, boolean valid) {
        if (valid) {
            assertDoesNotThrow(() -> new PointDeduction(A, points));
        } else {
            assertThrows(IllegalArgumentException.class, () -> new PointDeduction(A, points));
        }
    }

    // NEG: V-01 null takım
    @Test
    @DisplayName("TC-009 null team is rejected")
    void nullTeamIsRejected() {
        assertThrows(RuntimeException.class, () -> new PointDeduction(null, 3));
    }
}
