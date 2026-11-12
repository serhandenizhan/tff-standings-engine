package edu.svv.standings.generator;

/**
 * Bir kulübün geçmiş Süper Lig verisinden çıkarılan gücü.
 * Tüm değerler "maç başına" ortalamadır ve yalnızca kulübün pencere içindeki Süper Lig sezonlarından hesaplanır.
 *
 * @param club               kulüp adı (2026-27 listesindeki yazımla)
 * @param attack             maç başına attığı gol ortalaması
 * @param defence            maç başına yediği gol ortalaması (büyük değer = zayıf savunma)
 * @param pointsPerGame      maç başına puan (3G + B; puan silmeler hariç), yalnızca belgeleme / doğrulama için
 * @param averagePosition    sezon sonu sıralarının basit ortalaması, yalnızca belgeleme / doğrulama için
 * @param seasons            pencere içindeki Süper Lig sezonu sayısı (0 = geçmişi yok)
 * @param fromPromotedAverage true ise değerler kulübün kendi verisi değil, yeni çıkan takımların ilk sezon ortalamasıdır
 */
public record TeamStrength(String club, double attack, double defence, double pointsPerGame,
                           double averagePosition, int seasons, boolean fromPromotedAverage) {

    /** Maç başına ortalama gol farkı; güç sıralaması (belgeleme) için kullanılır. */
    public double goalDifferencePerGame() {
        return attack - defence;
    }
}
