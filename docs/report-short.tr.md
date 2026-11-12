# TFF Puan Durumu Motoru: JUnit 5 ile Doğrulama

**Ozan Serhan Denizhan** · 220911758 · SWE411 – Software Verification and Validation · 13.11.2026
Kaynak kod ve tüm test çıktıları: https://github.com/serhandenizhan/tff-standings-engine

> Bu dosya, İngilizce raporun içerik rehberidir. Hocanın sınırı **en fazla 7 sayfa** ve **kendi cümlelerin**. İngilizceyi bu taslağa bakarak kendi cümlelerinle yaz. Köşeli parantezli `[...]` yerleri senin dolduracağın kısımlar.

---

## 1. Problem ve Çözüm

Bir lig tablosu, takımlar puanda eşit kalana kadar basittir. Eşitlik olunca sıralama, TFF Futbol Müsabaka Talimatı (FMT) Madde 9'daki kriter sırasına bağlıdır. Zor olan kısım, **iki takımın** eşitliği ile **üç veya daha fazla takımın** eşitliğinin farklı çözülmesidir. Üç takımlı durumda yalnızca eşit takımların kendi aralarındaki maçlardan bir mini tablo kurulur. Mini tabloda hâlâ eşit kalan takımlar için o maçlara **tekrar bakılmaz**.

Bu hatanın gerçek hayatta da yapıldığını gördüm. 2024–25 sezonunda Alanyaspor, Kayserispor ve Gaziantep FK 45 puanda eşit bitirdi. İngilizce ve Türkçe Wikipedia bu üç takımı farklı sıralıyordu. Mini tabloyu hesaplayınca (7, 5, 4 puan) Türkçe sıralamanın doğru olduğu çıktı.

Çözüm, takımları, maç sonuçlarını ve puan silmelerini alıp FMT'ye göre sıralı tabloyu üreten bir Java 21 kütüphanesi ve küçük bir komut satırı programıdır. Veriler CSV dosyalarından okunur. GUI ve veritabanı yoktur.

## 2. Gereksinimler

Kural kaynağı zinciri: 2026–27 Süper Lig Statüsü kendi eşitlik kuralını içermez, bu yüzden FMT geçerlidir.

| Kural | İçerik |
|---|---|
| FMT 9/1 | Galibiyet 3, beraberlik 1, mağlubiyet 0 puan |
| FMT 9/2-a | 2 takım: ikili puan → ikili averaj (deplasman golü avantajı yok) → genel averaj → atılan gol → hükmen yenilgisi olmayan |
| FMT 9/2-b | 3+ takım: mini tablo puanı → mini averaj → mini atılan gol → genel averaj → genel atılan gol → hükmen; alt eşitlikte tekrar bakılmaz |
| FMT 23/4 | Hükmen: galibe 3 gol, yenilenin golleri sayılmaz; galip 3'ten fazla attıysa o goller kalır (5-2 → 5-0) |
| FMT 24 / FDT 33 | Puan silme; toplam puan negatife düşebilir |

Bunlardan **15 fonksiyonel gereksinim** (FR-01..15) ve **8 doğrulama kuralı** (V-01..08) çıkardım. Örnek: negatif gol, aynı takımın iki tarafta olması ve kayıtlı olmayan takım reddedilir. Hiçbir kriterle ayrılamayan takımlar keyfi sıralanmaz, "eleme maçı gerekli" olarak işaretlenir (FR-12). Kaynakta yazmayan iki konuyu açıkça **varsayım** olarak kaydettim. Biri averajın "atılan − yenilen" olması. Diğeri puan silmenin ikili puanı etkilememesi. Tam liste: `docs/requirements.md`.

## 3. Tasarım ve İlgili Kod

| Sınıf | Görevi |
|---|---|
| `Team`, `MatchResult`, `PointDeduction` | Değiştirilemez `record`'lar; tek nesneye bakan doğrulama burada |
| `StandingsCalculator` | İstatistik, hükmen düzeltmesi, puan silme, takımlar arası doğrulama |
| `TieBreaker` | Aynı puandaki grubu FMT 9/2'ye göre sıralar |
| `CsvLoader`, `Main` | CSV okuma (hatalı satırda dosya adı + satır no) ve tabloyu yazdırma |

Üç tasarım kararı:

1. **Hükmen düzeltmesi tek yerde.** Maçın sahadaki skoru saklanır, FMT 23/4 tek bir metotta uygulanır. Böylece istatistikte düzeltilmiş skor, ikili maçta ham skor kullanılması gibi bir tutarsızlık olamaz.
2. **Grup başına tek mini tablo.** "Tekrar bakılmaz" kuralı ayrıca kontrol edilmiyor; kod alt gruplar için mini tabloyu yeniden kurmadığı için kural kendiliğinden sağlanıyor:

```java
Comparator<StandingRow> criteria = Comparator
    .comparingInt((StandingRow r) -> mini.get(r.getTeam()).points).reversed()
    .thenComparing(comparingInt(r -> mini.get(r.getTeam()).goalDifference()).reversed())
    .thenComparing(comparingInt(r -> mini.get(r.getTeam()).goalsFor).reversed())
    .thenComparing(comparingInt(StandingRow::getGoalDifference).reversed())
    .thenComparing(comparingInt(StandingRow::getGoalsFor).reversed())
    .thenComparing(StandingRow::hasForfeitLoss);
```

3. **İki takım için ayrı kod yok.** İki takımın ikili averajları her zaman birbirinin ters işaretlisidir. Averajlar eşitse ikisi de 0'dır ve attıkları goller de eşittir. Bu yüzden 3+ takım sırası iki takımda da 9/2-a ile aynı sonucu verir.

Ek olarak, son 10 Süper Lig sezonunun gerçek verisiyle takım gücü hesaplayan bir **sezon üreteci** yazdım. Bununla 12 gerçekçi sezon üretip test verisi olarak kullandım (5 tam sezon, 7 sezon ortası tablo).

## 4. Test Planı ve Teknikler

Testleri koda bakmadan, **gereksinimlerden** türettim. Beklenen değerleri kurallardan elle hesapladım. Planda **138 test case** var, her birinde girdi, beklenen ve gerçekleşen sonuç yazılı (`docs/test-plan.md`).

| Teknik | Nasıl uyguladım | Örnek |
|---|---|---|
| Eşdeğerlik sınıfları | 59 sınıf: takım adı, gol, hükmen, eşit takım sayısı, CSV satırı | "Türkçe harfli adlar" sınıfı DEF-001'i buldu |
| Sınır değer analizi | 16 sınır | Gol −1/0/1; hükmende galibin golü 2/3/4 (kural max(3, g)); eşit takım sayısı 2→3 |
| Karar tablosu | 2 takım (R1–R7), 3+ takım (M1–M9), hükmen, puan silme | TC-073 (aşağıda) |
| Negatif test | V-01..V-08'in her biri için | Null liste, aynı maçın iki kez girilmesi, bozuk CSV satırı |
| Regresyon | Her düzeltmeden sonra tüm paket | run-01 → run-02 → run-03 |
| Özellik / metamorfik | 12 senaryo + 50 rastgele sezon | Maç sırası karıştırılınca tablo değişmemeli |

**En önemli test: TC-073.** A, B ve C 6'şar puanla eşit. Mini tabloda C atılan golle birinci olur, A ve B hâlâ eşittir. Kurala göre A–B maçına tekrar bakılmaz, genel averaja geçilir: B (+3) > A (+1). Doğru sıra **C, B, A**. Kuralı yanlış uygulayan iki olası kod farklı sonuç verir: ikili maça geri dönen kod C, A, B; mini atılan golü atlayan kod B, C, A. Yani tek test üç farklı uygulamayı birbirinden ayırıyor.

**Sınır değerde bir ayrıntı (BV-06).** İki takımın ikili averaj farkı hep çift sayıdır, 1 olamaz. Bu yüzden gerçek sınır 0 ile 2'dir. Sınır değer analizinde sadece "±1" almadım, değer aralığını inceledim.

## 5. JUnit Uygulaması ve Sonuçlar

Testler JUnit 5 ile yazıldı: `@Nested` ile gruplama, `@ParameterizedTest` ile sınır değer satırları, `assertThrows` ile negatif testler, `@TempDir` ile CSV dosyaları. Her testin adı TC numarasıyla başlıyor:

```java
@Test
@DisplayName("TC-073 FR-11: no re-look at A vs B, overall GD decides")
void tc073() {
    assertTable(calculate(teams("A", "B", "C", "D"),
            m("A", "B", 1, 0), m("C", "A", 2, 1), m("B", "C", 2, 1),
            m("A", "D", 1, 0), m("B", "D", 3, 0), m("C", "D", 1, 0)),
        "C", "B", "A", "D");
}
```

| Koşu | Durum | Sonuç |
|---|---|---|
| run-01 | İlk kod, elle tasarlanan testler | 125 çalıştırma, **3 başarısız** |
| run-02 | İki düzeltmeden sonra | 125 / 125 |
| run-03 | Üreteç, özellik ve kapsam testleri eklendi | 655 / 655 |
| run-04 | Bağımsız kod incelemesinin bulduğu 3 hata için yeni testler | 663 çalıştırma, **8 başarısız** |
| run-05 | Üç düzeltmeden sonra | **664 / 664** |

Başarısız testleri düzeltmeden önce kırmızı haliyle commit'ledim, böylece süreç commit geçmişinde görünüyor. İlk koşudaki gerçek hata çıktısı:

```
TC-079 expected: <[Çaykur Rizespor!, Denizlispor!]> but was: <[Denizlispor!, Çaykur Rizespor!]>
TC-087 InputFormatException: matches.csv, line 1: unknown forfeit value ''
```

## 6. Bulunan Hatalar ve Kapsam

| ID | Hata | Bulan test | Kök neden | Düzeltme |
|---|---|---|---|---|
| DEF-001 | Ayrılamayan takımlar Türkçe alfabeye göre sıralanmıyordu; Ç, İ, Ş, Ü Z'den sonra geliyordu | TC-078, TC-079 | `String.compareTo` harfleri Unicode koduna göre karşılaştırıyor | Türkçe `Collator` |
| DEF-002 | `A;B;1;0;` satırı reddediliyordu; boş hükmen alanı NONE sayılmalı | TC-087 | Boş alan geçersiz bir değer gibi işleniyordu | Boş alan = NONE |
| DEF-003 | Adında `\|` olan takımlarda farklı maçlar "tekrar eden maç" sayılıyordu | TC-160 | Maç anahtarı isimler `\|` ile birleştirilerek kuruluyordu | Anahtar `Team` nesnelerinden |
| DEF-004 | Çok büyük puan silmede sayı taşıyor, takım pozitif puan alıyordu | TC-161 | `int` toplamı taşma kontrolü olmadan yapılıyordu | `Math.addExact`, girdi reddedilir |
| DEF-005 | Üreteçte fikstür dengesizdi: bazı takımlar ilk devrede hiç iç sahada oynamıyordu | TC-162 | Ev sahibi kuralı halka dönüşüyle birlikte kayıyordu | Konum paritesi; senaryolar yeniden üretildi |

DEF-001'den kodu okurken şüphelenmiştim, test bunu doğruladı. DEF-002'yi yalnızca test buldu. DEF-003..005'i, bütün testler yeşilken yaptığım bağımsız bir kod incelemesi buldu: planımda adlarda ayraç karakteri, `int` sınırı ve fikstür dengesi için bir sınıf yoktu. Her biri için önce başarısız bir test yazıp kırmızı haliyle commit'ledim, sonra düzelttim.

**Kapsam (JaCoCo):** Branch kapsamı toplamda %70,8, testlerin hiç çalıştırmadığı iki geliştirici aracı hariç tutulunca %97,4. Kapsam raporuna bakıp 15 test daha ekledim; bunlar test edilmemiş gerçek yolları kapattı (G/Ç hataları, CLI hata çıktıları). Kalan boşluklardan biri hiçbir girdiyle çalıştırılamayan bir dal: üçüncü maç, daha önceki "tekrar eden maç" kontrolüne takılıyor.

**Kapsamın sınırı:** DEF-001, her testin zaten çalıştırdığı bir satırdaydı. Satır %100 kapsanmıştı ama yanlıştı; hatayı sadece Türkçe harfli test verisi ortaya çıkardı. Yani yüksek kapsam, kodun doğru olduğunu göstermez [3].

## 7. Değerlendirme ve Sonuç

Testler ve sonradan yapılan kod incelemesi toplam beş gerçek hata buldu; süreç baştan sona belgelendi. Kısıtlar da var:

- Beş hatanın hiçbiri eşitlik bozma kuralının kendisinde değil. Eşitlik testlerinin hepsi ilk seferde geçti. Bu, çekirdeğin doğru olduğunu kanıtlamaz. Gereksinimi ve testleri aynı kişi yazdığı için kuralı ikisinde de aynı şekilde yanlış anlamış olabilirim.
- Metamorfik testler istatistikleri kontrol ediyor ama eşitlik sırasını bağımsız olarak doğrulamıyor.
- İyileştirme olarak mutation testing (ör. PIT) ve gerçek sezon maç sonuçlarıyla bir kabul testi eklenebilir.

[Kendi değerlendirmen: Java ve JUnit öğrenirken nelerde zorlandın, 3–4 cümle.]

**AI kullanımı:** [Kendi sözlerinle yaz: Claude Code'u nerede kullandın, hangi kararları sen verdin, neleri kontrol edip değiştirdin. Ayrıntılı kayıt `docs/ai-usage-log.md` içinde.]

## Kaynaklar

[1] TFF, *Futbol Müsabaka Talimatı*, tff.org, 2025.
[2] ISTQB, *Certified Tester Foundation Level Syllabus* v4.0.1, 2024.
[3] L. Inozemtseva, R. Holmes, "Coverage is not strongly correlated with test suite effectiveness," ICSE 2014.
[4] JUnit 5 User Guide, v5.11.4, junit.org.
