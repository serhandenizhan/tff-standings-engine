# TFF Standings Engine — SVV JUnit Ödevi

Bireysel SVV ödevi: TFF puan eşitliği kurallarına göre lig tablosu üreten saf Java kütüphanesi + JUnit 5 testleri + rapor.

## Kurallar (zorunlu)
- Kod, dosya, sınıf/metot isimleri İngilizce. Kod içi yorumlar **sadece Türkçe**.
- Stack: Java, Maven, JUnit 5, JaCoCo. GUI ve veritabanı yok.
- Commit öncesi kullanıcıya sor, onayla commit at. Onaysız push yok.
- Her adım küçük; kullanıcı anlayıp onaylamadan ilerleme. Her test / sınır değer için "neden" açıklanır.
- Süreç gerçek yaşanır: gereksinim → ilk sürüm kod (bilerek "temiz" yazılmaz) → test planı + JUnit → defect log → düzeltme → regresyon. Commit geçmişi bunu göstermeli, hatalar saklanmaz.
- AI kullanımı `docs/ai-usage-log.md` dosyasına her adımda işlenir.
- TFF kural/madde numarası uydurulmaz; kaynaksızsa "varsayım" olarak işaretlenir.
- Kullanıcıyla Türkçe, kısa, doğal iletişim.

## Ödev özeti (Assignment.pdf)
- Teknikler: eşdeğerlik sınıfları, sınır değer, karar tablosu, negatif, regresyon + her seçimin gerekçesi.
- Test case'ler girdi / beklenen / gerçekleşen ile; defect analizi; coverage raporu.
- Not: Gereksinim 10, Kod 15, Test planı 20, JUnit 25, Coverage+defect 10, Araştırma 15, Rapor 5 (test tarafı %55).
- Teslim: büyük ihtimalle **24 Kasım 2026** (PDF'te 17 çizilip 24 yazılmış; teyit edilmedi).

## Kararlar
| Tarih | Karar | Gerekçe |
|---|---|---|
| 2026-11-12 | Konu B (puan durumu/averaj) seçildi, A bırakıldı | Algoritmik zorluk → gerçek bug; kurallar deterministik ve tek maddede; süreye sığar |
| 2026-11-12 | Fair-play ve kura kapsam dışı (önerilen, kesinleşmedi) | Kart verisi ve rastgelelik kapsamı şişirir; kura gerekirse "çözülemedi" |

## Kural kaynağı
- Şu an: TFF 2025–26 Gelişim Ligleri Statüsü, Md. 17/A — https://www.tff.org/Resources/TFF/Auto/f2fa7c08703241dca712a036a3bb1085.pdf
- 2 takım: ikili puan → ikili averaj (deplasman golü yok) → genel averaj → atılan gol → hükmen yenilgisi olmayan → fair-play → kura.
- 3+ takım: eşit takımlar arası mini tablo (alt eşitlikte maçlara tekrar bakılmaz) → mini puan → mini averaj → mini gol → genel averaj → genel gol → hükmen → fair-play → kura.
- Beklenen: Süper Lig statüsü (kullanıcı arıyor). Gelirse kaynak onunla değişir.

## Durum
- [x] Konu seçildi, klasör + git repo (main) kuruldu
- [ ] Süper Lig statüsü / eksik kurallar netleşecek
- [x] Java + Maven kuruldu (Homebrew): openjdk@21 (keg-only), Maven 3.10.0. `~/.zshrc` içinde JAVA_HOME Java 21'e sabitlendi (yedek: `~/.zshrc.bak-tff`); `java` ve `mvn` Java 21 kullanıyor. Sistem locale'i tr_TR.
- [ ] Gereksinim belirtimi (FR'ler, madde bağlantılı)
- [x] Maven iskeleti: pom.xml (Java 21, JUnit 5.11.4, Surefire 3.5.2, JaCoCo 0.8.13), paket `edu.svv.standings`. `mvn verify` → testler + `target/site/jacoco/index.html`. Geçici smoke test ile doğrulandı, sonra silindi.
- [ ] İlk sürüm kod → test planı → JUnit → defect log → fix → regresyon → coverage → rapor
