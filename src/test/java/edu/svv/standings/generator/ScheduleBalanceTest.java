package edu.svv.standings.generator;

import edu.svv.standings.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fikstür dengesi (TC-162): kod yorumu fikstürün "dengeli" olduğunu söylüyor; sezon ortası tablolar
 * (S06–S12) için ilk devrede her takımın iç saha maç sayısı birbirine yakın olmalı.
 */
class ScheduleBalanceTest {

    @ParameterizedTest(name = "{0} takım")
    @ValueSource(ints = {4, 6, 18, 20})
    @DisplayName("TC-162 – ilk devrede her takımın iç saha maçı ⌊(n−1)/2⌋ veya ⌈(n−1)/2⌉ olmalı (BVA: 0 ve n−1 olmamalı)")
    void tc162_firstHalfHomeGamesBalanced(int n) {
        // dengeli fikstürde n−1 maçın yarısı iç sahada oynanır; 18 takımda 8 veya 9
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            teams.add(new Team("T" + i));
        }
        List<List<SeasonGenerator.Fixture>> rounds = SeasonGenerator.doubleRoundRobin(teams);
        Map<Team, Integer> home = new HashMap<>();
        for (int r = 0; r < n - 1; r++) {
            for (SeasonGenerator.Fixture f : rounds.get(r)) {
                home.merge(f.home(), 1, Integer::sum);
            }
        }
        int low = (n - 1) / 2, high = n / 2;
        for (Team t : teams) {
            int h = home.getOrDefault(t, 0);
            assertTrue(h >= low && h <= high,
                    t.name() + " ilk devrede " + h + " iç saha maçı oynuyor, beklenen " + low + ".." + high);
        }
    }
}
