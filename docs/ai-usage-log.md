# AI Usage Log

Araç: Claude Code (Claude Opus 5.5). Rapordaki "AI kullanımı" bölümü için. Satırlar kronolojik; yeni satır en alta eklenir.

| Tarih | Adım | AI ne yaptı | Öğrenci ne yaptı / değiştirdi |
|---|---|---|---|
| 2026-11-12 | Konu analizi | Ödev PDF'ini özetledi; TFF Disiplin Talimatı ve Gelişim Ligleri Statüsü'nden maddeleri çıkardı; iki konu için test edilebilirlik karşılaştırması ve öneri yazdı | İki konuyu önerdi; son kararı verdi (B); sarı kart sayacının sıfırlandığı bilgisini düzeltti; Süper Lig statüsünü kendisi arıyor |
| 2026-11-12 | Proje kurulumu | Klasör, git repo, CLAUDE.md ve bu log dosyasını oluşturdu | Klasör/repo kurulmasını istedi, main dalında çalışmaya karar verdi |
| 2026-11-12 | Araç kurulumu | Homebrew ile openjdk@21 ve Maven'ı kurdu, sürümleri doğruladı | Kurulumu onayladı |
| 2026-11-12 | Maven iskeleti | pom.xml ve .gitignore yazdı; geçici bir smoke test ile JUnit + JaCoCo'nun çalıştığını doğrulayıp sildi | Onayladı |
| 2026-11-12 | Kural kaynağı | Öğrencinin getirdiği statüleri taradı, eşitlik kuralının FMT'de olduğunu bulup FMT Md. 9/23/24'ü çıkardı; daha önce bir haber sitesinden aktardığı "sezon içi genel averaj" iddiasının TFF metninde olmadığını kabul etti | Süper Lig 2026–27 statüsü ve TFF statüsünü buldu; ikili averaj, hükmen 3-0, puan silme ve sıralamanın her zaman aynı kuralla yapıldığı bilgilerini verdi |
| 2026-11-12 | Gereksinimler v1.0 | FDT Md. 33'ü kontrol etti; `docs/requirements.md` taslağını yazdı (13 FR, 7 doğrulama kuralı, 4 varsayım, kapsam dışı, açık kararlar) | İnceledi, taslağı onayladı; D-01 ve D-02 önerilerini kabul etti |
| 2026-11-12 | İlk sürüm: model | `Team`, `Forfeit`, `MatchResult`, `PointDeduction` sınıflarını (nesne düzeyi doğrulamalarla) yazdı | İnceledi ve onayladı |
| 2026-11-12 | GitHub + veri girişi | Public GitHub reposunu açıp `origin` olarak ekledi (push yok); CSV + komut satırı için FR-14, FR-15, V-08 taslağını yazdı (requirements v1.1) | Repo açılmasını istedi, public seçti; veri girişi için CSV + komut satırını seçti; v1.1 eklerini onayladı |
| 2026-11-12 | İlk sürüm: puan hesabı | `StandingRow` ve `StandingsCalculator` yazdı (istatistik, hükmen düzeltmesi, puan silme, puana göre sıralama, takımlar arası doğrulama); elle bir örnekle çalıştırıp sonucu kontrol etti | İnceledi ve onayladı |
| 2026-11-12 | İlk sürüm: eşitlik bozma | `TieBreaker` sınıfını yazıp hesaplayıcıya bağladı; 3 elle örnekle çalıştırdı; Türkçe harflerle alfabetik sıralamada olası bir hata fark edip düzeltmeden not etti | İnceledi ve onayladı |
