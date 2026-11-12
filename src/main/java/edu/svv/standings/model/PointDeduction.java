package edu.svv.standings.model;

/**
 * Bir takımın toplam puanından silinecek puan (FR-07; FMT 24/1-a, FDT 33).
 */
public record PointDeduction(Team team, int points) {

    public PointDeduction {
        // V-01: takım null olamaz
        if (team == null) {
            throw new IllegalArgumentException("Team must not be null");
        }
        // V-07: silinecek puan pozitif olmalı
        if (points <= 0) {
            throw new IllegalArgumentException("Deducted points must be positive");
        }
    }
}
