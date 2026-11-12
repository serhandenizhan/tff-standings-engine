package edu.svv.standings.model;

/**
 * Ligdeki bir takım. Takımlar yalnızca isimleriyle tanımlanır (FR-01).
 */
public record Team(String name) {

    public Team {
        // V-01, V-02: isim null ya da boş olamaz
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name must not be blank");
        }
        // D-01: baştaki/sondaki boşluklar atılır, gerisi birebir karşılaştırılır
        name = name.trim();
    }
}
