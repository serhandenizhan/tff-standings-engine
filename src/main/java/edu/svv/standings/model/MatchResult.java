package edu.svv.standings.model;

/**
 * Oynanmış (ya da hükmen tescil edilmiş) bir maçın sonucu (FR-02).
 * Goller sahada atılan gerçek gollerdir; hükmen düzeltmesi hesaplayıcıda yapılır (FR-06).
 */
public record MatchResult(Team homeTeam, Team awayTeam, int homeGoals, int awayGoals, Forfeit forfeit) {

    public MatchResult {
        // V-01: alanlar null olamaz
        if (homeTeam == null || awayTeam == null) {
            throw new IllegalArgumentException("Teams must not be null");
        }
        // V-04: takım kendisiyle oynayamaz
        if (homeTeam.equals(awayTeam)) {
            throw new IllegalArgumentException("A team cannot play against itself: " + homeTeam.name());
        }
        // V-05: gol sayısı negatif olamaz
        if (homeGoals < 0 || awayGoals < 0) {
            throw new IllegalArgumentException("Goals must not be negative");
        }
        // hükmen bilgisi verilmemişse normal maç kabul edilir
        if (forfeit == null) {
            forfeit = Forfeit.NONE;
        }
    }

    /** Hükmen olmayan normal maç için kısa yol. */
    public MatchResult(Team homeTeam, Team awayTeam, int homeGoals, int awayGoals) {
        this(homeTeam, awayTeam, homeGoals, awayGoals, Forfeit.NONE);
    }
}
