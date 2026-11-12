# AI Usage Log

Araç: Claude Code (Claude Opus 5.5). Rapordaki "AI kullanımı" bölümü için.

| Tarih | Adım | AI ne yaptı | Öğrenci ne yaptı / değiştirdi |
|---|---|---|---|
| 2026-11-12 | Konu analizi | Ödev PDF'ini özetledi; TFF Disiplin Talimatı ve Gelişim Ligleri Statüsü'nden maddeleri çıkardı; iki konu için test edilebilirlik karşılaştırması ve öneri yazdı | İki konuyu önerdi; son kararı verdi (B); sarı kart sayacının sıfırlandığı bilgisini düzeltti; Süper Lig statüsünü kendisi arıyor |
| 2026-11-12 | Gereksinimler v0.1 | FDT Md. 33'ü kontrol etti; `docs/requirements.md` taslağını yazdı (13 FR, 7 doğrulama kuralı, 4 varsayım, kapsam dışı, açık kararlar) | İnceledi, taslağı onayladı; D-01 ve D-02 önerilerini kabul etti |
| 2026-11-12 | Kural kaynağı | Öğrencinin getirdiği statüleri taradı, eşitlik kuralının FMT'de olduğunu bulup FMT Md. 9/23/24'ü çıkardı; daha önce bir haber sitesinden aktardığı "sezon içi genel averaj" iddiasının TFF metninde olmadığını kabul etti | Süper Lig 2026–27 statüsü ve TFF statüsünü buldu; ikili averaj, hükmen 3-0, puan silme ve sıralamanın her zaman aynı kuralla yapıldığı bilgilerini verdi |
| 2026-11-12 | Araç kurulumu | Homebrew ile openjdk@21 ve Maven'ı kurdu, sürümleri doğruladı | Kurulumu onayladı |
| 2026-11-12 | Maven iskeleti | pom.xml ve .gitignore yazdı; geçici bir smoke test ile JUnit + JaCoCo'nun çalıştığını doğrulayıp sildi | Onayladı |
| 2026-11-12 | Proje kurulumu | Klasör, git repo, CLAUDE.md ve bu log dosyasını oluşturdu | Klasör/repo kurulmasını istedi, main dalında çalışmaya karar verdi |
