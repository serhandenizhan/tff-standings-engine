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
| 2026-11-12 | Kaynak: Süper Lig statüsü → FMT Md. 9, 23/4, 24 | Statü eşitlik için FMT'ye bırakıyor; fair-play/kura FMT'de yok |
| 2026-11-12 | Son kriter (eleme maçı) hesaplanmaz → "çözülemedi" döner | Sahada oynanacak maç, yazılım karar veremez |
| 2026-11-12 | Veri girişi: CSV (`;`, UTF-8, başlıksız) + komut satırı (D-03) | Ödev çalıştırılabilir uygulama istiyor; CSV ayrıştırma ek negatif test alanı |
| 2026-11-12 | GitHub reposu public | Kullanıcı tercihi |

## Kural kaynağı
Zincir: **2026–27 Süper Lig Müsabakaları Statüsü** (Md. 3: 18 takım, çift devreli lig; eşitlik kuralı yok, aksine hüküm yok) → **TFF Futbol Müsabaka Talimatı (FMT)** — https://www.tff.org/Resources/TFF/Documents/TALIMATLAR/Futbol-Musabaka-Talimati.pdf (PDF 18.08.2025). `TFF-Statusu.pdf` (federasyon ana statüsü) bu konuyla ilgisiz.
- **FMT 9/1:** galibiyet 3, beraberlik 1, mağlubiyet 0.
- **FMT 9/2-a (2 takım):** I ikili puan → II ikili averaj (deplasman golü üstünlük sağlamaz) → III genel averaj → IV genel atılan gol → V hükmen yenilgisi olmayan → VI tek maçlı eleme (hesaplanamaz).
- **FMT 9/2-b (3+ takım):** I eşitler arası mini tablo, *alt eşitlikte o takımların maçlarına tekrar bakılmaz* (ikili kurala dönülmez) → II mini puan → III mini averaj → IV mini atılan gol → V genel averaj → VI genel atılan gol → VII hükmen yenilgisi olmayan → VIII TFF'nin belirleyeceği maç(lar).
- **FMT 9/3:** TFF averajı uygulamamaya karar verebilir (kapsam dışı).
- **FMT 23/4:** hükmen: galibe 3 gol, mağlubun golleri geçersiz, galip 3'ten fazla attıysa o goller geçerli (5-2 → 5-0; 1-2 → 3-0).
- **FMT 24/1-a:** sahaya çıkmama/terk: hükmen mağlubiyet + galibiyet puanı kadar (3) puan silinir.
- Kullanıcı bilgisi: ikili averaj iki maçın toplamı; sıralama her zaman (sezon içi de) aynı kurala göre; silinen puan sonrası eşitlikte kriterlere bakılır.
- **FDT Md. 33:** puan indirme = kulübün "mevcut ya da ileride kazanacağı" puanlarından indirim → puan negatif olabilir.
- **Varsayım (kaynaksız):** puan silme yalnızca genel puanı etkiler, ikili/mini tablo puanları maç sonuçlarından hesaplanır.
- **Varsayım:** gol averajı = atılan − yenilen (FMT tanımlamıyor; Türkiye'deki yerleşik kullanım).
- Kaynakta yok / kapsam dışı: ligden çekilen takımın önceki maçlarının akıbeti, ertelenmiş maçlar (tabloya sadece oynanmış/tescilli maçlar girer).

## Durum
- [x] Konu seçildi, klasör + git repo (main) kuruldu
- [x] Süper Lig statüsü + FMT okundu, açık sorular cevaplandı
- [x] Java + Maven kuruldu (Homebrew): openjdk@21 (keg-only), Maven 3.10.0. `~/.zshrc` içinde JAVA_HOME Java 21'e sabitlendi (yedek: `~/.zshrc.bak-tff`); `java` ve `mvn` Java 21 kullanıyor. Sistem locale'i tr_TR.
- [x] Gereksinim belirtimi: `docs/requirements.md` v1.0 onaylandı (D-01 birebir isim, D-02 alfabetik + işaret)
- [x] Maven iskeleti: pom.xml (Java 21, JUnit 5.11.4, Surefire 3.5.2, JaCoCo 0.8.13), paket `edu.svv.standings`. `mvn verify` → testler + `target/site/jacoco/index.html`. Geçici smoke test ile doğrulandı, sonra silindi.
- [ ] İlk sürüm kod (v1, bilerek cilalanmaz, bilerek hata da eklenmez):
  - [x] Model: `model/` paketi — `Team`, `Forfeit`, `MatchResult`, `PointDeduction` (record; nesne düzeyi doğrulama V-01/02/04/05/07)
  - [ ] İstatistik + puan hesabı (FR-04..08), çapraz doğrulama (V-03, V-06)
  - [ ] Eşitlik bozma (FR-09..12) + çıktı (FR-13)
  - [ ] CSV okuyucu + komut satırı (FR-14, FR-15, V-08) — requirements v1.1 onaylandı
- GitHub: https://github.com/serhandenizhan/tff-standings-engine (public, remote `origin`). Push yalnızca onayla.
- [ ] Test planı → JUnit → defect log → fix → regresyon → coverage → rapor

## Tasarım notları
- Nesne düzeyi doğrulama record constructor'larında; takımlar arası doğrulama (bilinmeyen takım, tekrarlanan maç) hesaplayıcıda.
- `MatchResult` sahadaki gerçek skoru tutar; hükmen düzeltmesi (FMT 23/4) hesaplayıcıda yapılır.
- AI log'a yeni satır **en alta** eklenir.
