# TFF Standings Engine — SVV JUnit Ödevi

Bireysel SVV ödevi: TFF puan eşitliği kurallarına göre lig tablosu üreten saf Java kütüphanesi + JUnit 5 testleri + rapor.

## Kurallar (zorunlu)
- Kod, dosya, sınıf/metot isimleri İngilizce. Kod içi yorumlar **sadece Türkçe**.
- Stack: Java, Maven, JUnit 5, JaCoCo. GUI ve veritabanı yok.
- Git: 2026-11-12'den itibaren **her faz sonunda izin almadan commit + push** (kullanıcı talimatı). Faz içi ara commit'ler serbest.
- Otonom mod (kullanıcı PC başında değil): orkestratör = ana oturum (Opus), alt ajanlar çoğunlukla Sonnet, zor işler Opus; tüm alt ajanlar low/medium effort. Ajanlar commit atmaz, orkestratöre rapor verir; orkestratör kontrol edip commit'ler. Her test / sınır değer için "neden" dokümanlara yazılır.
- Alt ajanlar mevcut çekirdek kodda (model/, StandingsCalculator, TieBreaker, StandingRow) hata görürse **düzeltmez, raporlar** (hatalar testlerle bulunup defect log'a girecek).
- Süreç gerçek yaşanır: gereksinim → ilk sürüm kod (bilerek "temiz" yazılmaz) → test planı + JUnit → defect log → düzeltme → regresyon. Commit geçmişi bunu göstermeli, hatalar saklanmaz.
- AI kullanımı `docs/ai-usage-log.md` dosyasına her adımda işlenir.
- TFF kural/madde numarası uydurulmaz; kaynaksızsa "varsayım" olarak işaretlenir.
- Kullanıcıyla Türkçe, kısa, doğal iletişim.

## Ödev özeti (Assignment.pdf)
- Teknikler: eşdeğerlik sınıfları, sınır değer, karar tablosu, negatif, regresyon + her seçimin gerekçesi.
- Test case'ler girdi / beklenen / gerçekleşen ile; defect analizi; coverage raporu.
- Not: Gereksinim 10, Kod 15, Test planı 20, JUnit 25, Coverage+defect 10, Araştırma 15, Rapor 5 (test tarafı %55).
- Teslim: **24 Kasım 2026, basılı (hard copy)**, sonrası kesinlikle kabul edilmez (hoca maili, 2026-11-13).
- Hoca maili: öneriler kabul; rapor **en fazla 7 sayfa**; **sadece öğrencinin kendi cümleleri ve kodu**; raporda yalnızca ilgili kod ve test bölümleri.

## Kararlar
| Tarih | Karar | Gerekçe |
|---|---|---|
| 2026-11-12 | Konu B (puan durumu/averaj) seçildi, A bırakıldı | Algoritmik zorluk → gerçek bug; kurallar deterministik ve tek maddede; süreye sığar |
| 2026-11-12 | Kaynak: Süper Lig statüsü → FMT Md. 9, 23/4, 24 | Statü eşitlik için FMT'ye bırakıyor; fair-play/kura FMT'de yok |
| 2026-11-12 | Son kriter (eleme maçı) hesaplanmaz → "çözülemedi" döner | Sahada oynanacak maç, yazılım karar veremez |
| 2026-11-12 | Veri girişi: CSV (`;`, UTF-8, başlıksız) + komut satırı (D-03) | Ödev çalıştırılabilir uygulama istiyor; CSV ayrıştırma ek negatif test alanı |
| 2026-11-12 | GitHub reposu public | Kullanıcı tercihi |
| 2026-11-12 | Rapor: tam taslak, İngilizce Markdown (`docs/report.md`); kişisel bölümler (Java öğrenme zorlukları) ve ad/no yer tutucu | Kullanıcı tercihi |
| 2026-11-12 | Hata bulan testler düzeltmeden önce kırmızı commit'lenir | Süreç commit geçmişinde görünsün |
| 2026-11-12 | Senaryo üreteci: garantili eşitlik YOK. Gerçekçi rastgele sezonlar (18 takım, gerçekçi gol dağılımı, ~%1 hükmen, nadir puan silme, tam + sezon ortası); bunlardan 10–12'si önceden seçilip CSV olarak repoya konur (çeşitlilik: eşitlik yok / 2'li / 3'lü / hükmen+silme / hepsi / en az bir tamamen çözülemez). Program ve testler kütüphaneden seed ile rastgele birini seçer | Kullanıcı kararı |
| 2026-11-12 | Takım gücü: güncel 2026–27 Süper Lig kulüpleri; son 10 sezonun (2016–17..2025–26) gerçek puan/gol/sıra verisi; düşüp çıkanlar için sadece Süper Lig sezonlarının ortalaması; pencerede hiç Süper Lig sezonu olmayanlar için yeni çıkan takımların ilk sezon ortalaması | Kullanıcı kararı: şampiyon/küme düşen gerçekçi olsun |
| 2026-11-13 | Rapor ≤7 sayfa. Önce kısa Türkçe taslak (`docs/report-short.tr.md`); İngilizceyi öğrenci kendi cümleleriyle yazar, Claude yalnızca yardım eder (düzeltme, öneri, kontrol) | Hoca: 7 sayfa sınırı + kendi cümleleri |
| 2026-11-12 | Test planı belirsizlikleri (Q1–Q13) orkestratör tarafından çözüldü; Q3 (null forfeit) FR-14 ile tutarlı olarak NONE kabul edildi | Kullanıcı yokken; test-plan.md ekinde kayıtlı |

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
- [x] İlk sürüm kod (v1, bilerek cilalanmaz, bilerek hata da eklenmez):
  - [x] Model: `model/` paketi — `Team`, `Forfeit`, `MatchResult`, `PointDeduction` (record; nesne düzeyi doğrulama V-01/02/04/05/07)
  - [x] İstatistik + puan hesabı (FR-04..08), çapraz doğrulama (V-01/02/03/06/07): `StandingRow`, `StandingsCalculator` 
  - [x] Eşitlik bozma (FR-09..12): `TieBreaker` — tek mini tablo + genel kriterler, komşu eşitse unresolved, görüntü için alfabetik. FR-13 çıktısı `StandingRow` ile karşılanıyor.
  - [x] CSV okuyucu + komut satırı (FR-14, FR-15, V-08)
- GitHub: https://github.com/serhandenizhan/tff-standings-engine (public, remote `origin`). Push yalnızca onayla.
- [x] Test planı → JUnit → defect log → fix → regresyon → coverage → rapor taslağı

## Faz planı (otonom)
1. ✅ CSV okuyucu + CLI + örnek veri + README (e2ef8f4). Gerçek veri (5618900). Üreteç: güç modeli (son 10 sezon, Poisson, H=1.10), çift devreli fikstür, 12 senaryoluk kütüphane `src/main/resources/scenarios/` (S01–S12), CLI `--scenario <id|random> [seed]`, gerçekçilik kontrolü `docs/generator.md`
2. ✅ Test planı `docs/test-plan.md` (a86b88b, 95 TC): EP, BVA, karar tablosu, negatif, regresyon, rastgele/metamorfik, izlenebilirlik
3. ✅ JUnit testleri: 125 çalıştırma, 3 kırmızı (TC-078, TC-079 Türkçe alfabe; TC-087 boş 5. alan) → kırmızı commit. Üreteç + özellik/metamorfik testleri eklendi (TC-100..134, toplam 640 çalıştırma, hepsi geçti; yeni defect yok)
4. ✅ Defect log: DEF-001 Türkçe alfabe (`37b9388`), DEF-002 boş hükmen alanı (`e03aea7`); regresyon 125/125 (`docs/results/run-02-after-fixes`). Üreteç testleri sonrası yeni defect çıkarsa DEF-003+
5. ✅ Coverage: `docs/coverage-analysis.md`, `docs/results/coverage/`, `docs/results/run-03-full-suite/`. 655 çalıştırma yeşil. Toplam branch %71.0 (araçlar hariç %97.5); kalan boşluklar ulaşılamaz/savunmacı kod veya araç kodu. Yeni defect yok
6. ✅ Test planı Actual sütunu dolduruldu, README dokümantasyon tablosu. (Uzun İngilizce/Türkçe rapor taslakları 2026-11-13'te kullanıcı isteğiyle silindi; git geçmişinde duruyor.)
9. ✅ Bağımsız kod incelemesi (4 ajan): DEF-003 `|` içeren adlar (b35e03b), DEF-004 int taşması (838b916, yeni V-09), DEF-005 fikstür dengesi (5af729c, senaryolar yeniden üretildi). Kırmızı commit bf4be97 (run-04, 663/8 fail) → run-05 664/664. Branch %70.8 (araçlar hariç %97.4). requirements v1.2.
8. ✅ Kısa Türkçe rapor taslağı `docs/report-short.tr.md` (~1.350 kelime, 6 tablo, 3 kod bloğu ≈ 5 sayfa; İngilizcesi ≈ 5–6 sayfa olur). İngilizceyi öğrenci yazacak.
7. ✅ Kontrol artifact'i (senaryo verisi DEF-005 öncesine ait, yeniden yayımlanmadı): https://claude.ai/artifact/LaSP9N711DAh45vbVppmzV (ödev maddeleri, tahmini puan 86/100, savunma kartları, S01–S12 tabloları ve maçları)

## Öğrenciye kalanlar (teslim öncesi)
- Raporu gerekirse kısalt (hedef 5–7k kelime idi), Word/PDF'e çevir.
- Test planı Q1–Q13 çözümlerini ve varsayımları (A-01..A-04) gözden geçir; sözlü savunmaya hazırlan.
- ~~Teslim tarihini teyit et~~ → 24 Kasım, basılı, teyitli.
- İngilizce raporu kendi cümlelerinle yaz (`docs/report-short.tr.md` rehber; kişisel değerlendirme ve AI beyanı yer tutucu).
- "Kendi kodu" şartı: kodun büyük kısmı AI ile yazıldı; hocaya AI kullanımını açıkça beyan et, gerekirse ne kadarına izin verdiğini sor; kodu satır satır anlatabilecek hale gel.

## Tasarım notları
- Nesne düzeyi doğrulama record constructor'larında; takımlar arası doğrulama (bilinmeyen takım, tekrarlanan maç) hesaplayıcıda.
- `MatchResult` sahadaki gerçek skoru tutar; hükmen düzeltmesi (FMT 23/4) hesaplayıcıda yapılır.
- AI log'a yeni satır **en alta** eklenir.
- 2 takım için ayrı kod yok: 9/2-b sırası 2 takımda 9/2-a ile aynı sonucu verir (ikili averaj eşitse ikili goller de eşit).
- D-02 alfabetik sıralama tr-TR Collator ile (DEF-001).
