# TFF Standings Engine: JUnit 5 ile Bir Puan Durumu Hesaplayıcısının Doğrulama ve Geçerleme Çalışması

| | |
|---|---|
| **Öğrenci** | Ozan Serhan Denizhan |
| **Öğrenci No** | 220911758 |
| **Ders** | SWE411 - Yazılım Doğrulama ve Geçerleme — bireysel JUnit ödevi |
| **Tarih** | 13.11.2026 |
| **Depo** | https://github.com/serhandenizhan/tff-standings-engine |

> Bu dosya, teslim edilecek İngilizce `docs/report.md` raporunun öğrencinin okuması için hazırlanmış Türkçe çevirisidir. Raporun tam taslağı 2026-11-12 tarihinde depodaki dosyalara dayanılarak yazılmıştır. Aşağıdaki her sayı depodaki bir dosyadan gelir (bkz. Ek). `[ÖĞRENCİ ...]` ile işaretlenen yerler öğrenci tarafından tamamlanmalı veya gözden geçirilmelidir.

---

## Özet

Proje, komut satırı arayüzü olan küçük bir Java 21 kütüphanesidir. Takımları, oynanmış maç sonuçlarını ve puan silme cezalarını alır; Türkiye Futbol Federasyonu'nun Futbol Müsabaka Talimatı'nın (FMT) 9. maddesine göre sıralanmış bir puan durumu üretir. Zor kısım eşitlik bozmadır. İki takımın ve üç ya da daha fazla takımın puan eşitliği farklı usullere tabidir; 3+ takım durumunda kurallar, hâlâ eşit kalan alt grubun kendi aralarındaki maçlara yeniden bakılmasını yasaklar. Yazılım, 133 test senaryosundan oluşan bir kara kutu test planıyla doğrulanmıştır (eşdeğerlik sınıfları, sınır değer analizi, karar tabloları, negatif ve regresyon testleri; ayrıca üretilmiş sezonlar üzerinde özellik tabanlı ve metamorfik testler). Bu senaryolar JUnit 5 testleri olarak gerçeklenmiş ve 655 test çalıştırmasına açılmıştır. El ile tasarlanan paketin ilk çalıştırmasında (125 çalıştırma) 2 hatadan kaynaklanan 3 başarısız çalıştırma görülmüştür. İki hata da düzeltilmiş, son paket 655/655 geçmiştir. Dal (branch) kapsamı genel olarak %71,0'dir; testler tarafından hiç çalıştırılmayan iki geliştirici aracı hariç tutulduğunda %97,5'tir. Rapor ayrıca bu sonuçların neyi göstermediğini de tartışır.

---

## 1. Giriş ve Problem Tanımı

### 1.1 Problem

Puan durumu ilk bakışta basit bir hesap gibi görünür: galibiyete 3, beraberliğe 1 puan, puana göre sırala. Zorluk, takımlar puanda eşit bittiğinde başlar. 2026–27 Süper Lig statüsü müsabaka formatını tanımlar (18 takım, çift devreli lig) ancak kendi başına bir eşitlik bozma kuralı içermez; dolayısıyla TFF Futbol Müsabaka Talimatı'nın (FMT) genel kuralı uygulanır [1], [2]. FMT Madde 9/2 iki farklı usul tanımlar [2]:

- **İki takım eşit (9/2-a):** (I) aralarındaki maçlardaki puan, (II) aralarındaki maçlardaki averaj (deplasman golüne ek ağırlık verilmez), (III) genel averaj, (IV) genel atılan gol, (V) hükmen mağlubiyeti olmayan takım üstte yer alır, (VI) tek maçlık eleme maçı.
- **Üç veya daha fazla takım eşit (9/2-b):** yalnızca eşit takımlar arasındaki maçlardan bir mini tablo oluşturulur ve takımlar sırasıyla mini tablo puanı, mini tablo averajı, mini tablo atılan golü, ardından genel averaj, genel atılan gol, hükmen kriteri ve son olarak TFF'nin belirleyeceği maç(lar) ile sıralanır. Metin parantez içinde, eşit takımlardan bazıları hâlâ eşitse bunların maçlarına **tekrar bakılmayacağını** ekler. Usul, kalan bir çift için iki takımlı kurala geri dönmez.

İki madde daha girdi verisini değiştirir. FMT 23/4, hükmen yenilen bir maçı 3-0'lık galibiyete çevirir; galip 3'ten fazla gol atmışsa onun gerçek golleri korunur [2]. FMT 24/1-a ve Disiplin Talimatı'nın (FDT) 33. maddesi puan silmeye izin verir; bu da bir kulübün toplamını sıfırın altına düşürebilir [2], [3].

Bu kurallar, yanlış anlaşılması kolay biçimlerde birbiriyle etkileşir. Aynı iki takım, üçüncü bir takımın onlarla eşit olup olmamasına göre farklı sıralanabilir. Hükmen yenilgi hem puanı hem averajı, mini tablo dahil, değiştirir. Puan silme, aksi hâlde var olmayacak bir eşitlik yaratabilir.

### 1.2 Motivasyon: gerçek bir görüş ayrılığı

Bu projenin veri toplama aşamasında (bkz. `docs/data-sources.md`) gerçek bir örnek ortaya çıktı. 2024–25 Süper Lig'inde Alanyaspor, Kayserispor ve Gaziantep FK üçü de 45 puanla bitirdi. Veri toplandığı sırada İngilizce Wikipedia Gaziantep FK'yi 12., Alanyaspor'u 13., Kayserispor'u 14. olarak, Türkçe Wikipedia ise Alanyaspor'u 12., Kayserispor'u 13., Gaziantep FK'yi 14. olarak listeliyordu [4], [5]. Üç kulübün mini tablosu, İngilizce sayfadaki sonuç matrisinden hesaplandığında Alanyaspor 7, Kayserispor 5 ve Gaziantep FK 4 puan verir. Bu, Türkçe sıralamayı destekler. Diğer tüm değerler (G/B/M/AG/YG/puan) iki kaynakta da aynıydı. Yani yaygın olarak okunan iki kaynak, gerçek bir final tablosunda, tam da bu projenin gerçekleştirdiği 3+ takımlı usul yüzünden ayrıştı. Bu, eşitlik bozması doğrulanmış bir araca duyulan ihtiyacın motivasyonudur. (Proje, 2024–25 sonuçlarının tamamı üzerinde kendi motorunu çalıştırmadı; çünkü maç düzeyinde veri değil yalnızca final tabloları toplandı; bkz. Bölüm 8.3.)

### 1.3 Önerilen çözüm

Çözüm, şunları yapan saf bir Java kütüphanesidir (`edu.svv.standings`):

1. takımları, oynanmış (veya hükmen tescil edilmiş) maç sonuçlarını ve puan silme cezalarını kabul eder ve doğrular;
2. her takımın istatistiklerini FMT 23/4 hükmen düzeltmesi ve puan silmeler ile hesaplar;
3. takımları puana göre sıralar ve eşitlikleri, "tekrar bakılmaz" kuralı dahil, FMT 9/2-a/b ile çözer;
4. hesaplanabilir tüm kriterlerden sonra hâlâ eşit kalan takımları keyfi biçimde sıralamak yerine *çözülemedi — eleme maçı gerekir* olarak işaretler.

İnce bir komut satırı programı verileri CSV dosyalarından okur ve tabloyu basar. On gerçek Süper Lig sezonuna dayanan, seed ile çalışan bir sezon üreteci gerçekçi, tam boyutlu test verisi sağlar. Ödevin ve bu raporun vurgusu doğrulamadır: kurallardan türetilmiş bir test planı, bunun JUnit gerçeklemesi, kapsam ve hata analizi.

---

## 2. Arka Plan ve Araştırma

### 2.1 Yazılım testinin yazılım kalite güvencesindeki rolü

Yazılım testi ile kalite güvencesi (QA) ilişkilidir ama aynı şey değildir. ISTQB Foundation Level müfredatı testi, ürün odaklı bir faaliyet olarak tanımlar: uygun kalite düzeylerine ulaşmayı destekler ve kalite kontrolünün başlıca biçimlerinden biridir. QA'yı ise süreç odaklı ve önleyici olarak tanımlar: iyi bir süreç doğru izlenirse iyi bir ürün ortaya çıkmalıdır [6, Sec. 1.2.2]. Test sonuçları her iki amaca da hizmet eder. Testte hataları düzeltmek için kullanılırlar. QA'da ise geliştirme ve test süreçlerinin ne kadar iyi çalıştığını gösterirler [6, Sec. 1.2.2].

Test, yazılımın doğru olduğunu gösteremez. Müfredat, kapsamlı (exhaustive) testi imkânsız olarak listeler; bu yüzden çabayı odaklamak için test teknikleri ve risk temelli önceliklendirme kullanılır. Müfredat ayrıca "hatasızlık yanılgısını" da sayar: belirtilen tüm gereksinimleri doğrulamak ve bulunan tüm hataları düzeltmek, yine de kullanıcıların ihtiyacını karşılamayan bir sistem ortaya çıkarabilir; doğrulamaya ek olarak geçerlemenin gerekmesinin nedeni budur [6, Sec. 1.3]. Dersin adındaki "V&V" bu yüzdendir. Doğrulama (verification), ürünün belirtime göre doğru yapılıp yapılmadığını sorar. Geçerleme (validation) ise doğru ürünün yapılıp yapılmadığını sorar.

Bu projede belirtim, FMT'den türetilen `docs/requirements.md` dosyasıdır (FR-01..FR-15, doğrulama kuralları V-01..V-08). Süreç, QA'nın önerdiği sırayı izledi: gereksinimler, ilk sürüm kod, test planı, testler, hata kaydı, düzeltme, regresyon çalıştırması. Commit geçmişi bu sırayı gösterir (Bölüm 6.4). Test, ürün üzerinde kalite kontrolü işlevi gördü. İzlenebilirlik matrisi ve hata kaydı süreç kanıtı işlevi görür. Test iki gerçek hata buldu (DEF-001, DEF-002); DEF-001 kod incelemesi sırasında şüphelenilmişti, DEF-002 ise şüphelenilmemişti (Bölüm 7.3).

### 2.2 Otomatik birim testinin amacı ve faydaları

Bir birim testi, bir sınıf ya da metot gibi küçük bir kod parçasını çalıştırır ve gözlenen sonucu önceden yazılmış beklenen sonuçla karşılaştırır. Bunu otomatikleştirmek, karşılaştırmayı bir aracın yapması ve paketin istenildiği an yeniden çalıştırılabilmesi demektir. ISTQB müfredatı, otomatik regresyon testinin aynı testleri tekrarlamanın yararlı kaldığı durumlardan biri olduğunu belirtir [6, Sec. 1.3, principle 5]. Bir hatayı düzelten ya da özellik ekleyen her değişiklikten sonra doğrulama ve regresyon testini gerektirir [6, Sec. 2.2.3].

Bu projenin destekleyebileceği faydalar mütevazı ve somuttur:

- **Tekrarlanabilirlik.** Paket tek bir Maven komutuyla çalışır. Düzeltmelerden sonraki regresyon çalıştırması (`docs/results/run-02-after-fixes`), hata tespit çalıştırmasıyla aynı komutu kullandı; bu da önce/sonra karşılaştırmasını adil kılar.
- **Düzeltme üzerine hızlı geri bildirim.** DEF-001 ve DEF-002 düzeltildikten sonra tek bir komut, daha önce başarısız olan testlerin geçtiğini ve başka hiçbir şeyin bozulmadığını gösterdi (125/125).
- **Çalıştırılabilir dokümantasyon.** Her test, test senaryosu numarasını ve test planından kısa bir gerekçeyi taşır; örneğin `@DisplayName("TC-073 FR-11: no re-look at A vs B, overall GD decides")`. Böylece test bir gereksinimi ifade eder.
- **Erken ve yerel başarısızlık.** `Team`, `MatchResult` ve `PointDeduction` birim testleri, hatayı sonradan yanlış bir tablo olarak değil, hatalı sınıfın kendisinde yakalar.

Maliyetler de vardır. Testlerin bakımı gerekir, yanlış bir beklenen değer yanlış alarm verir ve otomatik kontroller yalnızca yazarlarının kontrol etmeyi düşündüğü şeyleri yakalar [6, Sec. 1.3]. Bu projede, oracle'ın test edilen koda bağımlı olmaması için beklenen değerler test planında el ile hesaplandı (Bölüm 5.1).

### 2.3 JUnit 5: ana özellikler ve bu projedeki kullanımları

**Mimari.** JUnit 5 Kullanıcı Kılavuzu, JUnit 5'i üç parça olarak tanımlar [7]. **Platform**, JVM üzerinde test çerçevelerini başlatmanın temelidir ve `TestEngine` API'sini tanımlar. **Jupiter**, test yazmak için programlama ve eklenti modelidir. **Vintage**, JUnit 3 ve 4 testlerini Platform üzerinde çalıştırır. Bu proje yalnızca `junit-jupiter` bağımlılığını kullanır ve `pom.xml` içinde `junit-bom` ile 5.11.4 sürümünden yönetilir; dolayısıyla Vintage'a gerek yoktur.

**Maven Surefire.** Maven Surefire eklentisi, test bağımlılıklarına bakarak bir test motoru seçer: JUnit 5 Platform artifact'leri varsa Jupiter motoruyla JUnit Platform'u kullanır [8]. Proje `maven-surefire-plugin` 3.5.2'yi yapılandırır; böylece `mvn test` tüm Jupiter testlerini bulur, çalıştırır ve raporları `target/surefire-reports` altına yazar. `mvn verify` ayrıca JaCoCo 0.8.13'ü çalıştırır ve HTML kapsam raporunu oluşturur. Kopyalar `docs/results/` içinde tutulur.

| Özellik | Ne yapar [7] | Bu projede nerede kullanılıyor |
|---|---|---|
| `@Test`, `@DisplayName` | bir test metodunu işaretler; okunabilir rapor adı verir | 119 `@Test` metodu; her ad TC numarasıyla başlar, böylece rapor satırları test planına eşlenir |
| `@Nested` | ilgili testleri gruplamak için static olmayan iç sınıf | 14 iç içe sınıf; örn. `StandingsCalculatorTest` → `Stats`, `ForfeitAdjustment`, `Deductions`, `Validation`; `TieBreakMultiTeamTest` → `CriterionOrder`, `ForfeitAndUnresolved`, `NoReLookAtSubTies`, `ExtraRules`, `TurkishAlphabeticalOrder` |
| `@ParameterizedTest` | tek metot, çok sayıda argüman kümesi | 34 parametreli metot; birlikte 655 çalıştırmanın çoğunu üretirler |
| `@ValueSource`, `@NullSource`, `@CsvSource`, `@MethodSource` | argüman kaynakları | `TeamTest` (geçerli / boş / null adlar); `MatchResultTest`, `PointDeductionTest` (sınır satırları); `StandingsCalculatorTest.awayForfeitCases()` (3 gol çevresindeki hükmen sınırı); üreteç testleri sezonları `GeneratorTestSupport` üzerinden alır |
| `assertEquals`, `assertTrue`, `assertAll` | doğrulamalar (assertion); `assertAll` bir satırın başarısız olan tüm alanlarını birlikte raporlar | 25 `assertAll` bloğu, çoğunlukla istatistik satırları için |
| `assertThrows` | istisna türünü doğrular ve mesaj kontrolleri için istisnayı döndürür | negatif testlerde 48 çağrı yeri; örn. TC-035 mesajın bilinmeyen takımı adlandırdığını kontrol eder |
| `@TempDir` | testten sonra silinen geçici dizin | `CsvLoaderTest`, `MainTest` ve üç `CoverageGapTest` sınıfı CSV dosyalarını buraya yazar |

**Yaşam döngüsü ve eklentiler.** Varsayılan olarak Jupiter her test metodu için test sınıfının yeni bir örneğini oluşturur [7]. Proje bu varsayılana dayanır. Modeller değişmez (immutable) record'lar olduğundan ve her test kendi verisini kurduğundan, testler değiştirilebilir durum paylaşmaz ve `@BeforeEach`/`@BeforeAll` geri çağrımları kullanmaz. Proje kendi eklentisini yazmaz; yalnızca `@TempDir` ve `@ParameterizedTest` arkasındaki yerleşik eklentileri kullanır [7].

### 2.4 Test tasarım teknikleri ve uygulanabilirlikleri

Teknikler, ISTQB müfredatında [6, Sec. 4.2] ve klasik test literatüründe [9] anlatılan kara kutu ailesinden gelir. Eşdeğerlik sınıfları (equivalence partitioning, EP) veriyi aynı biçimde işlenmesi beklenen bölümlere ayırır. Sınır değer analizi (boundary value analysis, BVA) bu bölümlerin kenarlarını sınar. Karar tabloları koşul kombinasyonlarını ve ortaya çıkan eylemi listeler; bu da eksik kombinasyonları görünür kılar [6, Sec. 4.2]. Negatif test, geçersiz girdinin reddedildiğini kontrol eder. ISTQB müfredatı doğrulama testini (asıl hata düzeltildi mi) regresyon testinden (başka hiçbir şey bozulmadı mı) ayırır [6, Sec. 2.2.3]. Bölüm 5, her tekniğin nasıl uygulandığını gösterir.

El yapımı senaryolar küçüktür ve yalnızca tasarımcının düşündüğü şeyleri kontrol eder. Rastgele test, çok sayıda üretilmiş girdi üzerinde genel özellikleri kontrol eder. Bu yaklaşımı QuickCheck yaygınlaştırdı: test eden kişi sağlanması gereken özellikleri belirtir, araç da bunları rastgele üretilmiş veri üzerinde kontrol eder [10]. Metamorfik test *oracle problemini* ele alır: tam beklenen çıktı bilinmediğinde, ilişkili girdilerin çıktıları arasındaki ilişkiler kontrol edilir [11]. 18 takımlı tam bir sezonun el ile hesaplanmış beklenen tablosu yoktur; bu yüzden proje her iki fikri de kullanır (Bölüm 5.7). 515 üreteç, özellik ve metamorfik çalıştırmanın hepsi ilk çalıştırmada geçti; Bölüm 7.4 bunun ne anlama geldiğini tartışır.

### 2.5 Kod kapsamı (coverage) ve sınırlılıkları

Kapsam, testlerin kodun ne kadarını çalıştırdığını ölçer. JaCoCo birkaç sayaç raporlar. Komut (instruction) kapsamı bytecode komutlarını sayar. Bir satır, komutlarından en az biri çalıştıysa kapsanmış sayılır. Dal (branch) kapsamı, `if` ve `switch` ifadelerinin çalıştırılan ve kaçırılan dallarını sayar [12]. ISTQB müfredatı deyim (statement) ve dal kapsamını aynı ruhla tanımlar [6, Sec. 4.3]. Kapsam, negatif bir işaret olarak yararlıdır: hiçbir testin çalıştırmadığı kod kesinlikle test edilmemiştir. Proje JaCoCo'yu tam olarak bunun için, kara kutu tasarımdan sonra kullandı ve bulunan boşluklar için 15 test ekledi (Bölüm 7.1).

Kapsam zayıf bir pozitif işarettir. Müfredat, bir deyimi çalıştırmanın her zaman bir hatayı yakalamadığı konusunda uyarır; örneğin veriye bağımlı bir hata [6, Sec. 4.3]. Inozemtseva ve Holmes, paket boyutu kontrol edildiğinde kapsam ile test paketi etkinliği arasındaki korelasyonun düşük ile orta arasında olduğunu buldu. Kapsamın tek başına güvenilir bir kalite hedefi olmadığı, ancak yeterince test edilmemiş kodu bulmaya yardım edebileceği sonucuna vardılar [13]. Bu, tek bir çalışmanın bulgusudur, genel bir yasa değildir. Proje aynı sınırlılıkları gösterir. DEF-001, tablo kuran her testin çalıştırdığı bir deyimde yer alıyordu, ancak onu yalnızca Türkçe harf içeren testler ortaya çıkardı. Ayrıca geriye kalan kapsanmamış dallardan biri tasarım gereği ulaşılamazdır, bir test boşluğu değildir (Bölüm 7.2). Kapsam, eksik kod hakkında da hiçbir şey söylemez: gerçeklenmemiş bir gereksinimin kapsanacak kodu yoktur. Bu nedenle izlenebilirlik matrisi, kapsam rakamlarından bağımsız olarak her gereksinimi test senaryolarına bağlar.

### 2.6 Java öğrenme ve test gerçekleme zorlukları

`[ÖĞRENCİ YAZACAK]` Kişisel değerlendirme, yaklaşık 150–250 kelime. Olası yönlendirici sorular:

- Bildiğiniz dillere kıyasla Java'da yeni olan neydi (record'lar, değişmezlik, generic'ler, denetlenen (checked) ve denetlenmeyen (unchecked) istisnalar, stream'ler)?
- Başlangıçta en zor JUnit özelliği hangisiydi (`@Nested` örnekleri, `@MethodSource` fabrika metotları, `@TempDir`, `assertThrows` neden bir lambda ister)?
- Beklenen değerleri el ile nasıl kontrol ettiniz; herhangi bir el hesabının, koddan değil kendisinin yanlış çıktığı oldu mu?
- Şüphelendiğiniz bir hatanın (DEF-001) doğrulanması ile kimsenin şüphelenmediği bir hatanın (DEF-002) ortaya çıkması nasıl bir deneyimdi?
- Türkçe yerel ayar ortamı (tr_TR) ya da UTF-8 işleme size ne öğretti?
- Bir dahaki sefere neyi farklı yapardınız?

---

## 3. Gereksinim Belirtimi

Tam belirtim `docs/requirements.md` dosyasıdır (v1.1, öğrenci tarafından 2026-11-12 tarihinde onaylandı). Bu bölüm onu özetler.

### 3.1 Kapsam

Takımların listesini, oynanmış maç sonuçlarını ve puan silme cezalarını alıp FMT Md. 9'a göre sıralanmış puan durumunu üreten bir Java uygulaması. Çekirdek saf bir kütüphanedir. Küçük bir komut satırı arayüzü girdileri CSV dosyalarından okur ve tabloyu basar. GUI, veritabanı veya ağ G/Ç'si yoktur.

### 3.2 Kural kaynağı zinciri

| Düzey | Belge | Sağladığı |
|---|---|---|
| 1 | 2026–27 Süper Lig Müsabakaları Statüsü [1] | Md. 3: 18 takım, çift devreli lig. Kendine ait eşitlik bozma kuralı yok, aksine hüküm yok; dolayısıyla genel talimat uygulanır. |
| 2 | FMT Md. 9/1 [2] | galibiyet 3, beraberlik 1, mağlubiyet 0 |
| 2 | FMT Md. 9/2-a, 9/2-b [2] | iki takımlı ve 3+ takımlı eşitlik bozma usulleri (Bölüm 1.1) |
| 2 | FMT Md. 23/4 [2] | hükmen skor: galip 3 gol alır, mağlubun golleri geçersiz, galip 3'ten fazla attıysa kendi golleri korunur (5-2 → 5-0; 1-2 → 3-0) |
| 2 | FMT Md. 24/1-a [2] | sahaya çıkmama veya sahayı terk: hükmen mağlubiyet ve 3 puan silme |
| 3 | FDT Md. 33 [3] | "mevcut ya da ileride kazanılacak" puanlardan indirim; dolayısıyla toplam negatif olabilir |

Projede hiçbir madde numarası ikincil bir kaynaktan alınmamıştır. Kaynağı olmayan kurallar varsayım olarak kaydedilmiştir (Bölüm 3.4).

### 3.3 Gereksinim özeti

| ID | Özet | Kaynak |
|---|---|---|
| FR-01 | Bir takım kümesini kabul et; adlar boş olmamalı ve benzersiz olmalı | — |
| FR-02 | Kayıtlı takımlar arasındaki sonuçları kabul et, goller negatif olmamalı | SLS Md. 3 |
| FR-03 | Çift başına en fazla iki maç, iç saha/deplasman yönü başına en fazla bir | SLS Md. 3(1) |
| FR-04 | 3 / 1 / 0 puan | FMT 9/1 |
| FR-05 | O, G, B, M, AG, YG, averaj, puanı hesapla | FMT 9/1–2 |
| FR-06 | Hükmen düzeltmesi, her yerde kullanılır (istatistik, ikili maçlar, mini tablo) | FMT 23/4 |
| FR-07 | Puan silmeler düşülür, birikimlidir, toplam negatif olabilir | FMT 24/1-a, FDT 33 |
| FR-08 | Puana göre azalan sırala | FMT 9/2 |
| FR-09 | İki takım eşitliği: ikili puan, ikili averaj (deplasman golü yok), genel averaj, genel atılan gol, hükmen | FMT 9/2-a I–V |
| FR-10 | 3+ takım eşitliği: mini puan, mini averaj, mini atılan gol, genel averaj, genel atılan gol, hükmen | FMT 9/2-b I–VII |
| FR-11 | 3+ usulünde, kalan bir alt grup için mini tablo **yeniden** kurulmaz | FMT 9/2-b I |
| FR-12 | Tüm kriterlerden sonra hâlâ eşit kalan takımlar "çözülemedi — eleme maçı gerekir" olarak işaretlenir | FMT 9/2-a VI, 9/2-b VIII |
| FR-13 | Çıktı: sıra, FR-05 istatistikleri, puan silme sonrası puan, çözülemedi bayrağı | — |
| FR-14 | CLI UTF-8, `;` ile ayrılmış, başlıksız CSV okur (takımlar, isteğe bağlı hükmen alanlı maçlar, isteğe bağlı puan silmeler) | — |
| FR-15 | CLI hizalı sütunlar basar ve çözülemeyen takımları işaretler | — |

| ID | Net bir mesajla reddedilmesi gereken geçersiz girdi |
|---|---|
| V-01 | null takım listesi, maç listesi, takım, maç veya puan silme |
| V-02 | boş ya da yinelenen takım adı |
| V-03 | kayıtlı olmayan bir takıma başvuran maç |
| V-04 | iki tarafta da aynı takım |
| V-05 | negatif gol sayısı |
| V-06 | yinelenen fikstür veya iki takım arasında ikiden fazla maç |
| V-07 | sıfır ya da negatif puan silme ya da kayıtlı olmayan bir takım için puan silme |
| V-08 | hatalı biçimli CSV satırı veya eksik dosya; dosya adı ve satır numarasıyla bildirilir |

### 3.4 Varsayımlar

| ID | Varsayım | Gerekçe |
|---|---|---|
| A-01 | Averaj = atılan gol − yenilen gol | FMT tanımlamıyor; Türkiye'deki standart kullanım |
| A-02 | Puan silmeler yalnızca toplam puanı etkiler; ikili ve mini tablo puanları maç sonuçlarından gelir | FMT/FDT'de belirtilmemiş |
| A-03 | Sezon içinde ve sonunda aynı sıralama kuralları; henüz karşılaşmamış takımların kendi aralarındaki maç kriterleri eşittir | FMT tek bir sistem tanımlar; öğrenci tarafından teyit edildi |
| A-04 | Yalnızca oynanmış (veya hükmen tescil edilmiş) maçlar girilir; ertelenmiş maçlar yoktur | — |

### 3.5 Kapsam dışı

FMT 9/3 (TFF'nin averajı uygulamamaya karar vermesi); eleme maçının kendisini oynamak (FR-12 yalnızca işaretler); ligden çekilen veya ihraç edilen takımlar ve önceki sonuçlarının akıbeti; küme düşme ve Avrupa kupası yerleri, fair-play tabloları, kura çekimi.

### 3.6 Kararlar

| ID | Soru | Karar |
|---|---|---|
| D-01 | Takım adları büyük/küçük harfe duyarlı mı? | Kırpma (trim) sonrası birebir eşleşme |
| D-02 | Çözülemeyen takımlar nasıl listelenir? | Ardışık sıralar, yalnızca görüntü için alfabetik sıra, işaretli |
| D-03 | Veri nasıl girilir? | CSV dosyaları + komut satırı arayüzü; yalnızca oynanmış sonuçlar, kütüphane girdisi için fikstür üretimi yok |

Test planı yazılırken bulunan on üç ek belirsizlik (Q1–Q13; örn. hükmenin otomatik olarak 3 puan silip silmediği veya boş bir beşinci CSV alanının `NONE` anlamına gelip gelmediği) çözüldü ve `docs/test-plan.md` ekinde kaydedildi.

---

## 4. Gerçekleme

### 4.1 Mimari

| Paket / sınıf | Sorumluluk |
|---|---|
| `model.Team`, `MatchResult`, `PointDeduction` (record'lar), `Forfeit` (enum) | Nesne düzeyinde doğrulamalı girdi değerleri |
| `StandingsCalculator` | Genel giriş noktası `calculate(teams, matches, deductions)` → `List<StandingRow>`; nesneler arası doğrulama, istatistikler, hükmen düzeltmesi, puan silmeler, puana göre gruplama |
| `StandingRow` | Tablonun bir satırı (FR-05, FR-13); genel getter'lar, paket düzeyinde mutator'lar |
| `TieBreaker` (paket düzeyinde) | Puanda eşit tek bir takım grubunu sıralar (FR-09..FR-12) |
| `io.CsvLoader`, `io.InputFormatException` | FR-14 ayrıştırma; V-08 hataları dosya adı ve 1 tabanlı satır numarasıyla |
| `cli.Main`, `cli.TableFormatter` | FR-15 çıktısı; çıkış kodları 0 (başarı), 1 (girdi hatası), 2 (kullanım) |
| `generator.*` | `StrengthModel`, `TeamStrength`, `SeasonGenerator`, `GeneratedSeason`, `ScenarioLibrary`; geliştirici araçları `ScenarioLibraryBuilder`, `RealismCheck` |

Ana kaynak: 18 dosya, 1.857 satır (yorumlar dahil). Bir proje kuralı gereği kod ve tanımlayıcılar İngilizce, yorumlar Türkçedir.

### 4.2 Temel tasarım kararları

**Değişmez girdi record'ları.** `Team`, `MatchResult` ve `PointDeduction` Java record'larıdır; dolayısıyla oluşturulduktan sonra değişemezler ve eşitlik değer tabanlıdır (iki `Team("Beşiktaş")` nesnesi aynı takımdır). Compact constructor, nesne düzeyindeki doğrulamanın tek yeridir:

```java
public record Team(String name) {
    public Team {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Team name must not be blank");
        name = name.trim();   // D-01: trimmed, otherwise exact
    }
}
```

`StandingRow` bilerek değişmez *değildir*, çünkü istatistikler maç maç biriktirilir. Mutator'ları (`addMatch`, `addDeduction`, `setPosition`, `setUnresolved`) paket düzeyindedir; böylece paket dışındaki kod satırı salt okunur görür.

**Doğrulamanın paylaştırılması.** Yalnızca tek bir nesne gerektiren kurallar (V-01 null alanlar, V-02 boş ad, V-04 aynı takımın iki kez geçmesi, V-05 negatif gol, V-07 pozitif olmayan puan) record constructor'larında yer alır. Girdinin tamamını gerektiren kurallar (V-02 yinelenen adlar, V-03 bilinmeyen takım, V-06 yinelenen fikstür, V-07 bilinmeyen takım için puan silme) `StandingsCalculator` içinde yer alır. Bu nedenle geçersiz bir `MatchResult` oluşturulamaz bile ve hesaplayıcı yalnızca geçerli nesneler arasındaki ilişkileri kontrol eder.

**Hükmen düzeltmesi tek yerde.** `MatchResult` skoru sahada oynandığı gibi saklar. FMT 23/4, hem istatistik kodunun hem de mini tablo kodunun çağırdığı iki küçük static metotla uygulanır:

```java
static int adjustedHomeGoals(MatchResult match) {
    if (match.forfeit() == Forfeit.HOME) return 0;
    if (match.forfeit() == Forfeit.AWAY) return Math.max(3, match.homeGoals());
    return match.homeGoals();
}
```

Bu, tipik bir tutarsızlığı ortadan kaldırır: düzeltilmiş skor istatistiklerde kullanılırken ham skorun ikili maçlara sızması. TC-024 ve TC-077 tam olarak bunu test eder.

**Grup başına tek mini tablo (FR-11).** `TieBreaker.order`, puanda eşit olan her takım grubu için bir kez çağrılır. Grubun *tüm* takımları arasındaki maçlardan tek bir mini tablo kurar ve sabit bir karşılaştırıcı zinciriyle sıralar (kısaltılmış):

```java
Comparator<StandingRow> criteria = Comparator
    .comparingInt((StandingRow r) -> mini.get(r.getTeam()).points).reversed()
    .thenComparing(comparingInt(r -> mini.get(r.getTeam()).goalDifference()).reversed())
    .thenComparing(comparingInt(r -> mini.get(r.getTeam()).goalsFor).reversed())
    .thenComparing(comparingInt(StandingRow::getGoalDifference).reversed())
    .thenComparing(comparingInt(StandingRow::getGoalsFor).reversed())
    .thenComparing(StandingRow::hasForfeitLoss);           // false (no forfeit loss) first
```

Metot hiçbir alt gruba özyineleme yapmaz; dolayısıyla "alt eşitliklerde tekrar bakılmaz" kuralı yapı gereği sağlanır. Bu karşılaştırıcının bir komşuyla 0 döndürdüğü takımlar çözülemedi olarak işaretlenir (FR-12).

**İki takım durumu neden ayrı kod gerektirmez.** İki kişilik bir grup için mini tablo, kendi aralarındaki maçların ta kendisidir; dolayısıyla 9/2-b'nin mini puanı ve mini averajı, 9/2-a'nın I ve II numaralı kriterleridir. 9/2-b'deki tek ek adım mini atılan gol'dür. A ve B'nin kendi aralarındaki maçlarda A'nın attığı her gol B tarafından yenir ve tersi de geçerlidir; dolayısıyla Averaj_A = AG_A − AG_B ve Averaj_B = AG_B − AG_A = −Averaj_A olur. Mini averajlar eşitse Averaj_A = −Averaj_A, yani Averaj_A = 0 ve AG_A = AG_B'dir. Bu nedenle mini atılan gol adımı, o adıma ulaşan iki takımı asla ayıramaz. İki takımla 9/2-b zinciri 9/2-a zinciriyle aynı sonucu verir ve deplasman golünün hiçbir rolü yoktur. Aynı argüman test planındaki BV-06 sınırını da açıklar (Bölüm 5.3). Her iki sıra ayrı ayrı test edilir (iki takım için TC-050..TC-061, üç veya daha fazlası için TC-065..TC-077) ve TC-074 ikisi arasındaki geçişi kontrol eder.

**Görüntü için Türkçe harmanlama (collation).** D-02, çözülemeyen takımları alfabetik listeler. İlk sürüm `String`'in doğal sırasını kullandı; bu sıra UTF-16 kod birimlerini karşılaştırır ve Ç, İ, Ş ve Ü'yü Z'den sonraya koyar. Bu DEF-001 hatasıydı. Düzeltme, `Collator.getInstance(Locale.forLanguageTag("tr-TR"))` ifadesini yalnızca son görüntü eşitlik bozmasında kullanır. Çözülemedi bayraklarını belirleyen FMT karşılaştırıcısı değişmemiştir.

**CLI'nın test edilebilirliği.** `Main.main` yalnızca UTF-8 akışlarını kurar ve `run(args, out, err)` sonucuyla `System.exit` çağırır. Tüm mantık `run` içindedir; `run` çıkış kodunu döndürür ve aldığı akışlara yazar, böylece testler onu doğrudan çağırabilir.

### 4.3 Veriye dayalı sezon üreteci

Öğrenci gereksinimleri belirledi: kurgulanmış eşitlik yok, gerçekçi rastgele sezonlar, önceden üretilmiş küçük sabit bir sezon kütüphanesi, takım güçleri gerçek verilerden (CLAUDE.md, kararlar). Tasarım `docs/generator.md` içinde belgelenmiştir.

**Güç modeli.** Son on Süper Lig sezonunun (2016–17 ile 2025–26 arası, 189 satır) final tabloları İngilizce Wikipedia'dan toplandı ve Türkçe Wikipedia ile satır satır kontrol edildi [4], [5]. Her sezon tutarlılık kontrollerini geçti (ΣAG = ΣYG, ΣG = ΣM, tam çift devre). Belgelenmiş dört puan silme bulundu ve açıklandı (`docs/data-sources.md`). 2026–27'nin 18 kulübünün her biri için hücum = toplam atılan gol ÷ oynanan maç ve savunma = toplam yenilen gol ÷ oynanan maç olarak, kulübün penceredeki Süper Lig sezonları üzerinden havuzlanarak hesaplandı. Amed SFK ve Çorum FK'nin pencerede Süper Lig sezonu yoktur; bu yüzden 27 yeni çıkan kulüp satırının ortalamasını alırlar (hücum 1,22, savunma 1,45). Lig ortalaması takım başına maç başına μ = 1,407 gol'dür.

**Maç modeli.** Her tarafın golleri bağımsız Poisson değişkenleridir: λ_ev = μ·(hücum_ev/μ)·(savunma_dep/μ)·H ve λ_dep = μ·(hücum_dep/μ)·(savunma_ev/μ)/H. H = 1,10 ev sahibi avantajı bir **varsayımdır**: toplanan tablolarda iç saha/deplasman ayrımı yoktur, dolayısıyla H ölçülemedi. Eşit takımlar için, gollerin yaklaşık %55'ini ev sahibine verir. Poisson örneklemesi, `java.util.Random(seed)` ile Knuth'un çarpma yöntemini kullanır; böylece aynı seed aynı sezonu verir. Fikstür, çember yöntemiyle çift devreli bir lig turnuvasıdır (34 hafta, 306 maç). Maçların %1'i hükmen sonuçlanır ve her kulübün sezon başına 3 puan silme cezası alma olasılığı %2'dir. Bilinen sadeleştirmeler: skor korelasyonu yok, beraberlik şişmesi yok, sabit güçler.

**Gerçekçilik kontrolü.** 200 simüle edilmiş sezonda (`RealismCheck`) maç başına simüle edilen gol 2,68 (gerçek pencere 2,81), ev sahibi payı %55,0 idi ve en güçlü beş kulüp bir şampiyonluk hariç hepsini kazandı (bir kez Samsunspor). Maç başına puan, çok sezonu olan kulüpler için gerçek değerlere yakındı. Yalnızca bir iki gerçek sezonu olan kulüpler daha fazla sapar (Kocaelispor, Erzurumspor FK). `docs/generator.md` bu sapmaları açıklar.

**S01–S12 senaryo kütüphanesi.** `ScenarioLibraryBuilder` seed'leri 1'den başlayarak (20.000'e kadar) tarar. Her senaryonun kuralına uyan sezonu üreten, kullanılmamış ilk seed'i tutar; dolayısıyla kütüphane, kurgulanmış sezonlardan değil, çeşitlilik için süzülmüş rastgele sezonlardan oluşur. Kütüphane 5 tam sezon (S01–S05) ve 7 sezon ortası tablo (1, 2, 3, 10, 17, 25 ve 30. haftalardan sonra) içerir. Şunları kapsar: eşitlik yok (S01); yalnızca iki takımlı eşitlikler (S02, S05, S09); 3+ takımlı eşitlikler (S03, S04, S06–S08, S10–S12); hükmen sonuçlar ve puan silmeler; ve "muhtemelen çözülemez" olarak işaretlenen dört sezon (S04, S06, S08, S11).

**Test edilen koddan bağımsızlık.** Üreteç, sezonları kendi küçük sınıflandırıcısıyla sınıflandırır. Bu sınıflandırıcı ham puanları yeniden hesaplar, FMT 23/4'ü birkaç satırda yeniden gerçekler ve hiçbir kriterin ayıramadığı grupları tespit eder. `StandingsCalculator` veya `TieBreaker` çağırmaz; dolayısıyla test verisi seçimi, test edilen koda bağlı değildir. Sınıflandırıcı muhafazakârdır: S06 ve S08'de hesaplayıcı, sınıflandırıcının öngördüğünden daha fazla takımı çözülemedi olarak işaretler ve sınıflandırıcının işaretlediği her grup hesaplayıcı tarafından da işaretlenir (TC-108).

### 4.4 Derleme ve çalıştırma

Gereksinimler: Java 21, Maven 3.9 veya daha yenisi.

```
mvn verify                                   # compile, run all tests, JaCoCo report in target/site/jacoco/
mvn package -DskipTests                      # build the jar only
java -jar target/tff-standings-engine-0.1.0-SNAPSHOT.jar examples/teams.csv examples/matches.csv examples/deductions.csv
java -jar target/tff-standings-engine-0.1.0-SNAPSHOT.jar --scenario S04
java -jar target/tff-standings-engine-0.1.0-SNAPSHOT.jar --scenario random 42
```

Tablonun sütunları Pos, Team, P, W, D, L, GF, GA, GD, Pts'tir; çözülemeyen takımlar `*` ile işaretlenir. `examples/` içindeki örnek veri gerçek kulüp adlarını ve uydurma sonuçları kullanır.

---

## 5. Test Planı ve Test Tasarımı

Tam plan `docs/test-plan.md` dosyasıdır (v1.1). 133 test senaryosu tanımlar: TC-001..TC-099 (83 el ile tasarlanmış senaryo, numaralamada boşluklar var), TC-100..TC-134 (35 üreteç, özellik tabanlı, metamorfik ve CLI senaryosu) ve kapsam analizinden sonra eklenen TC-140..TC-154 (15 senaryo).

### 5.1 Strateji

- **Düzeyler.** Birim (model record'ları), bileşen (`StandingsCalculator`, `CsvLoader`), CLI/sistem (`Main`, CSV dosyalarından stdout'a uçtan uca) ve üreteç/özellik tabanlı/metamorfik testler.
- **Önce kara kutu.** Test senaryoları gereksinimlerden ve FMT metninden türetildi. Kaynak koda yalnızca genel sınıf ve metot adları için bakıldı. JaCoCo yalnızca yürütmeden *sonra*, test edilmemiş kodu bulmak için kullanıldı.
- **Beklenen değerler el ile.** Her beklenen tablo, planda hesabıyla birlikte yazılıdır (örn. "A 6 (GF3 GA2 GD+1), B 6 (GF5 GA2 GD+3) …"); böylece satır satır kontrol edilebilir ve sözlü olarak savunulabilir.
- **Ayırt edici veri.** Her eşitlik bozma senaryosu, yanlış kriteri ya da doğru kriterleri yanlış sırayla uygulamanın *farklı* bir tablo vermesi için kurulmuştur. Tipik bir hile: eşitliği kazanması gereken takımın genel averajı daha kötüdür ya da alfabede daha sonra gelir.
- **Bağımsız oracle kontrolü.** `docs/ai-usage-log.md` dosyasına (faz 3) göre el ile hesaplanan beklenen değerler, Python ile yazılmış ayrı bir kural hesaplayıcısıyla da kontrol edildi. Bu betik depoda yer almaz; dolayısıyla bu kontrol teslim edilen dosyalardan tekrarlanamaz. `[ÖĞRENCİ: bu cümleyi onayla veya sil.]`
- **Çıkış kriterleri.** Her TC çalıştırılmış; her FR, V kuralı ve karar tablosu kuralı için en az bir çalıştırılmış senaryo; her başarısızlık kaydedilmiş; son düzeltmeden sonra tam paket yeşil; kapsanmayan dallar açıklanmış.

### 5.2 Eşdeğerlik sınıfları (equivalence partitioning)

Plan, takım adları, goller, maç katılımcıları, hükmen, puan silmeler, eşitlik durumları, çift başına fikstürler, CSV satır biçimleri ve CLI argümanları için 59 bölüm (EP-01..EP-59) tanımlar. Örnekler:

| Bölüm | Tür | Örnek | TC |
|---|---|---|---|
| EP-02 Türkçe harfli adlar (Ç Ğ İ I Ö Ş Ü ı) | geçerli | `İstanbul Başakşehir` | TC-001, TC-080 |
| EP-09 yalnızca büyük/küçük harf farkı olan adlar | geçerli, ayrı takımlar (D-01) | `Beşiktaş` / `beşiktaş` | TC-043 |
| EP-34 / EP-35 / EP-36 iki / üç / dört veya daha fazla takım eşit | geçerli | — | TC-050.., TC-065.., TC-076 |
| EP-39 iki eşit takım arasındaki maçlar: 0 / 1 / 2 | geçerli | — | TC-060 / TC-051 / TC-050 |
| EP-52 boş beşinci alanlı maç satırı `A;B;1;0;` | belirsiz → geçerli (Q9) | — | TC-087 |

**Gerekçe.** EP, senaryo sayısını küçük tutarken kuralların farklı ele aldığı her sınıfa bir temsilci verir. EP-02, kütüphane Türkçe kulüp adlarını hedeflediği için bilerek seçildi. TC-078 ve TC-079'u üretti; bunlar DEF-001'i ortaya çıkardı. EP-52, isteğe bağlı hükmen alanının her biçimi listelenerek ortaya çıktı ve DEF-002'yi açığa çıkaran TC-087'yi üretti.

### 5.3 Sınır değer analizi (boundary value analysis)

Plan 16 sınır içerir (BV-01..BV-16). Örnekler:

| ID | Değişken | Değerler | Neden |
|---|---|---|---|
| BV-01 | gol sayısı | −1 / 0 / 1 | 0, en küçük geçerli değerdir; `> 0` ile `>= 0` farkını yakalar |
| BV-02 | hükmen galibinin gerçek golleri | 2 / 3 / 4 (ve 0, 5) | kural max(3, g)'dir; davranış 3 ile 4 arasında değişir |
| BV-03 | puan farkı | 0 / 1 | 1 puan her eşitlik kriterini yenmelidir (TC-014: averajı daha iyi ama 1 puan eksik takım daha aşağıda sıralanır) |
| BV-08 | puan silme | −1 / 0 / 1 | V-07 "sıfır ya da negatif" |
| BV-09 | puan silme sonrası puan | 1 / 0 / −1 | negatif toplamlara izin var (FDT 33); 0'da sınırlamak olası bir hatadır |
| BV-10 | eşit grup boyutu | 2 → 3 | algoritma geçişi (TC-073 ve TC-074) |
| BV-13 | CSV alan sayısı (maçlar) | 3 / 4 / 5 / 6 | yalnızca 4 veya 5 kabul edilir |

**BV-06 içgörüsü.** İki takımın ikili averajı için saf sınır, 0 ile 1 farkı olurdu. Ancak iki takımın kendi aralarındaki averajlar her zaman birbirinin ters işaretlisidir (Averaj_B = −Averaj_A, Bölüm 4.2'de gösterildiği gibi); dolayısıyla aralarındaki fark Averaj_A − Averaj_B = 2·Averaj_A her zaman çifttir. 1 farkı imkânsızdır ve gerçek sınır 0 ile 2'dir; bu da bir gollük farktır (TC-052, TC-053). Benzer şekilde olası ikili puan çiftleri (3-3, 1-1, 2-2, 4-1, 3-0, 6-0) sıfırdan farklı en küçük farkı 3 yapar (BV-07). Burada sınır değer analizi, yalnızca "değer ± 1" almayı değil, alanı çözümlemeyi gerektirdi.

### 5.4 Karar tabloları

Eşitlik bozma, karar tablolarının tam olarak tasarlandığı türden bir koşul zinciridir. Planda dört tablo vardır.

**(a) İki takım, FMT 9/2-a**: R1–R7 kuralları ve R2'. Her kuralın belirleyici bir kriteri ve bir testi vardır (TC-050..TC-059). R2', deplasman golünün **belirleyici olmadığını** kontrol eder: TC-053'te iki takımın da ikili averajı 0'dır, ancak yalnızca B deplasman golü atmıştır. Doğru sonuç (genel averajla A birinci) deplasman golü kuralının vereceğinin tersidir.

**(b) Üç veya daha fazla takım, FMT 9/2-b**: M1–M9 kuralları (TC-065..TC-073). M8 ve M9, "kalan çifte iki takımlı kuralı yeniden uygulamak sonucu değiştirir" koşulunu içerir; bu da FR-11'i doğrudan test eder.

**Temel FR-11 senaryosu, TC-073.** Maçlar: A–B 1-0, C–A 2-1, B–C 2-1, A–D 1-0, B–D 3-0, C–D 1-0. A, B ve C'nin her birinin 6 puanı var. Mini tabloda üçünün de 3 puanı ve averajı 0'dır; mini atılan goller A 2, B 2, C 3 olduğundan C birincidir. A ve B hâlâ eşittir. FMT 9/2-b, bunların maçlarına tekrar bakılmayacağını söyler; dolayısıyla sonraki kriter genel averajdır: B +3 > A +1. Beklenen sıra: **C, B, A, D**. A ve B için iki takımlı bir tabloyu yeniden kuran "özyinelemeli" bir gerçekleme A'nın 1-0'lık galibiyetini görür ve C, A, B verir. Mini atılan golü atlayan bir gerçekleme B, C, A verir. Böylece tek bir senaryo, üç makul gerçeklemeyi birbirinden ayırır. TC-074 bir sonucu değiştirir (C–D 1-1); bu da yalnızca A ve B'yi eşit bırakır, dolayısıyla iki takımlı kural uygulanır ve sıra A, B, C, D olur.

**(c) Hükmen düzeltmesi, FMT 23/4**: F1–F6 kuralları; hükmen yenilen tarafa, galibin gollerinin ≤ 3 ya da > 3 olmasına ve hükmen yenilen takımın sahada önde olmasına göre (örn. ev sahibi hükmen yenilirken 5-2 → 0-3).

**(d) Puan silme etkileşimi**: D1–D5. Anahtar senaryo TC-034'tür. 6 puanlık bir silme eşitlik yaratır. Silme yanlışlıkla ikili puana sızsaydı A'nın ikili puanı −2 olur ve B'nin altına düşerdi; A-02 gereği beklenen sonuç A'nın B'nin üstünde olmasıdır.

### 5.5 Negatif test

Her V-01..V-08 doğrulama kuralının negatif senaryoları vardır (plan Bölüm 5): null listeler ve null elemanlar, boş ve yinelenen adlar (kırpma sonrası yinelenenler dahil), kayıtlı olmayan ev sahibi ya da deplasman takımı, `A–A` ve `A–" A "`, −1 goller, aynı fikstürün iki kez girilmesi, üçüncü bir maç, 0 ve −1 puan silmeler, yanlış CSV alan sayıları, tam sayı olmayan sayılar, bilinmeyen hükmen değerleri (`XYZ` ve küçük harfli `home`), eksik dosyalar ve 0/1/4 CLI argümanı. Testler `assertThrows` kullanır ve mesajın hatalı değeri, dosyayı ya da satırı adlandırdığını kontrol eder; çünkü V kuralları "net bir mesaj" gerektirir. DEF-002 CSV tarafında, ters yönde bulundu: *geçerli* bir satır reddediliyordu.

### 5.6 Regresyon testi

Plan, her düzeltmeden sonra yalnızca başarısız sınıfın değil **tam** paketin yeniden çalıştırılmasını gerektirdi; çünkü eşitlik bozma, ilgisiz sıralamaları kolayca bozar. Her hata, tespit eden testini regresyon testi olarak pakette tutar. Başarısız testler düzeltmeden önce kırmızı olarak commit'lendi (commit `f8e8d6b`) ve hiçbir test geçmesi için değiştirilmedi. Özellik tabanlı testler sabit seed'ler kullanır; böylece bir regresyon tekrar üretilebilir.

### 5.7 Özellik tabanlı ve metamorfik test

Tam sezonların el ile hesaplanmış beklenen tablosu yoktur; bu yüzden üç başka oracle kullanıldı (plan Bölüm 6):

- **Değişmezler (TC-100..TC-103)**, 12 kütüphane sezonu ve 50 seed'li rastgele sezon üzerinde (seed'ler 1..50; her üçüncüsü bir sezon ortası tablo). Korunum (ΣG = ΣM, ΣAG = ΣYG, Σoynanan = 2·maç). Satır başına oynanan = G + B + M ve puan = 3G + B − silinen; her satırı ham maç listesinden yeniden hesaplayan ve FMT 23/4'ü kendisi uygulayan bir **test tarafı oracle'ı** ile karşılaştırılır. Sıralar 1..n ve puanlar artmayan sırada. Çözülemeyen takımlar yalnızca puanı eşit, en az iki kişilik gruplarda görünür.
- **Metamorfik ilişkiler (TC-104..TC-107, TC-129)**, 12 kütüphane sezonu ve seed'ler 1..20 üzerinde. Girdi listelerini karıştırmak tabloyu değiştirmez. Tutarlı yeniden adlandırma yalnızca adları değiştirir. Her maçı aynalamak (ev sahibi/deplasmanı ve HOME↔AWAY hükmeni değiştirmek) tabloyu değiştirmez, çünkü hiçbir kriter sahaya bağlı değildir. d puanlık bir puan silme eklemek yalnızca o takımın puanını tam olarak d kadar düşürür. Bir haftayı kaldırmak her takımın oynadığı maç sayısını 1 azaltır. TC-107 diğer takımların göreli sırasını koruduğunu bilerek **ileri sürmez**, çünkü silmenin yarattığı yeni bir 3 takımlı eşitlik onları yasal olarak yeniden sıralayabilir; FMT 9/2-b monoton değildir.
- **Üreteç doğruluğu (TC-111..TC-128)**, çünkü üreteç de ilk sürüm koddur: seed başına belirlenimcilik, fikstür yapısı (306 maç, her sıralı çift bir kez, ikinci yarı birincinin aynası), sezon ortası önek özelliği, hafta sayısı sınırları (0, 1, 34, 35), 200 sezon üzerinde hükmen ve puan silme oranları, 200.000 çekiliş üzerinde Poisson ortalaması ve varyansı, %53–57 aralığında ev sahibi gol payı, CSV'den bağımsız olarak yeniden hesaplanan güç değerleri ve katalog seed'lerinden taze bir yeniden üretimle eşleşen kütüphane dosyaları.
- **Katalog bayrakları ve hesaplayıcı (TC-108..TC-110)**: bağımsız sınıflandırıcının bayrakları hesaplayıcının sonucuyla uyuşmalıdır.

### 5.8 İzlenebilirlik ve dağılım

Planın izlenebilirlik matrisi (Bölüm 9) her FR, V kuralı, varsayım ve kararı test senaryolarına eşler. Her karar tablosu kuralının en az bir senaryosu vardır. Birincil tekniğe göre: EP 30, BVA 9, karar tablosu 25, negatif 19, özellik/metamorfik/üreteç 35 (118 senaryo) ve ek olarak 15 beyaz kutu kapsam boşluğu senaryosu (133). Birçok senaryo ikincil bir teknik de taşır; örneğin TC-050/051/054/055 aynı zamanda BVA satırlarıdır.

---

## 6. Test Uygulaması ve Sonuçlar

### 6.1 JUnit yapısı

Tüm testler `src/test/java/edu/svv/standings/` altındadır (21 dosya, 2.960 satır). Toplam 153 test metodu vardır (119 `@Test`, 34 `@ParameterizedTest`). Her metodun `@DisplayName` değeri TC numarasıyla başlar. Küçük yardımcı metotlar, fikstürlerin test planındaki gösterimle birebir aynı kalmasını sağlar. `TieFixtures.m("A","B",2,1)` bir maç oluşturur. `TieFixtures.assertTable(table, "C", "B!", …)` ise tam sıralamayı, çözülemedi bayrağını (`!`) ve ardışık sıra numaralarını tek bir doğrulamada kontrol eder. `GeneratorTestSupport` sezon kaynaklarını ve test tarafındaki oracle'ı sağlar. Örnek (TC-073):

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

Son çalıştırmada (`docs/results/run-03-full-suite`) sınıf başına test çalıştırma sayıları:

| Test sınıfı | Çalıştırma sayısı | TC'ler |
|---|---|---|
| `model.TeamTest` / `MatchResultTest` / `PointDeductionTest` | 10 / 6 / 4 | TC-001..TC-009 |
| `StandingsCalculatorTest` (`Stats` 7, `ForfeitAdjustment` 10, `Deductions` 6, `Validation` 14) | 37 | TC-010..TC-048 |
| `TieBreakTwoTeamTest` (5 iç içe sınıf) | 12 | TC-050..TC-061 |
| `TieBreakMultiTeamTest` (5 iç içe sınıf) | 15 | TC-065..TC-079 |
| `io.CsvLoaderTest` | 29 | TC-080..TC-090 |
| `cli.TableFormatterTest` / `cli.MainTest` | 5 / 7 | TC-091..TC-099 |
| `generator.PropertyBasedStandingsTest` | 248 | TC-100..TC-103 |
| `generator.MetamorphicStandingsTest` | 167 | TC-104..TC-107, TC-129 |
| `generator.CatalogueFlagsTest` | 17 | TC-108..TC-110 |
| `generator.SeasonGeneratorTest` / `StrengthModelTest` | 33 / 8 | TC-111..TC-123 |
| `generator.ScenarioLibraryTest` | 34 | TC-124..TC-128 |
| `cli.MainScenarioTest` | 8 | TC-130..TC-134 |
| `cli.` / `io.` / `generator.CoverageGapTest` | 4 / 4 / 7 | TC-140..TC-154 |
| **Toplam** | **655** | |

### 6.2 Çalıştırma geçmişi

| Çalıştırma | Commit / durum | Çalıştırma sayısı | Sonuç | Saklandığı yer |
|---|---|---|---|---|
| run-01 | `f8e8d6b` (elle tasarlanmış testler, ilk kod sürümü) | 125 | 122 geçti, 2 başarısızlık, 1 hata | `docs/results/run-01-initial/` |
| run-02 | `37b9388` ve `e03aea7` sonrası (düzeltmeler) | 125 | 125 geçti | `docs/results/run-02-after-fixes/` |
| run-03 | `f5a0ad2` (+ üreteç, özellik tabanlı, metamorfik ve kapsam boşluğu testleri) | 655 | 655 geçti | `docs/results/run-03-full-suite/` |

Üreteçle ilgili testler 515 çalıştırma ekledi (toplam 640) ve hepsi ilk çalıştırmada geçti. 15 kapsam boşluğu çalıştırması toplamı 655'e çıkardı.

### 6.3 Başarısız ve başarılı bir çalıştırma örneği

Run-01 özeti (`docs/results/run-01-initial/mvn-test-output.txt` dosyasından aynen):

```
[ERROR]   TieBreakMultiTeamTest$TurkishAlphabeticalOrder.tc079:191 sıra / çözülemedi bayrağı ==> expected: <[Çaykur Rizespor!, Denizlispor!]> but was: <[Denizlispor!, Çaykur Rizespor!]>
[ERROR] Errors: 
[ERROR]   CsvLoaderTest.tc087_emptyFifthFieldIsNone:216 » InputFormat matches.csv, line 1: unknown forfeit value '' (expected NONE, HOME or AWAY)
[ERROR] Tests run: 125, Failures: 2, Errors: 1, Skipped: 0
[ERROR] BUILD FAILURE
```

TC-079 bir doğrulama **başarısızlığıdır** (failure): kod bir tablo döndürdü ama görüntüleme sırası yanlıştı. TC-087 ise bir **hatadır** (error): kod geçerli girdide beklenmedik bir `InputFormatException` fırlattı. TC-078, 11 takım adı üzerinde TC-079 ile aynı nedenle başarısız oldu.

Başarılı son çalıştırma (`docs/results/run-03-full-suite/mvn-test-output.txt` dosyasından aynen, alıntı):

```
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0 s -- in edu.svv.standings.TieBreakMultiTeamTest$NoReLookAtSubTies
[INFO] Tests run: 248, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.111 s -- in edu.svv.standings.generator.PropertyBasedStandingsTest
[INFO] Tests run: 655, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 6.4 Süreç kanıtları

Commit geçmişi (`git log --oneline`) süreci sırasıyla kaydeder: Maven iskeleti (`2df6762`) → gereksinimler v1.0 (`fe6224d`) → model record'ları (`79c2fad`) → gereksinimler v1.1 (`c3378bb`) → hesaplayıcı (`6973e90`) → eşitlik bozma (`aff5653`) → CSV/CLI (`e2ef8f4`) → test planı (`a86b88b`) → JUnit testleri, 3'ü başarısız (`f8e8d6b`) → DEF-001 düzeltmesi (`37b9388`) → DEF-002 düzeltmesi (`e03aea7`) → defect log güncellemesi (`e30e035`) → gerçek veri (`5618900`) → araştırma taslağı (`3071a5f`) → üreteç (`e86e3fd`) → üreteç ve özellik tabanlı testler (`f1cccec`) → kapsam analizi (`f5a0ad2`) → gerçekleşen sonuçların test planına işlenmesi (`fddf52a`).

---

## 7. Kod Kapsamı (Coverage) ve Defect Analizi

### 7.1 Kapsam sonuçları

JaCoCo 0.8.13, `mvn clean verify`. Rapor `docs/results/coverage/` altındadır (`index.html`, `jacoco.csv`). Aşağıdaki toplamlar `jacoco.csv` dosyasından yeniden hesaplanmış olup `docs/coverage-analysis.md` ile uyumludur.

| Metrik | Boşluk testlerinden önce (640 çalıştırma) | Son durum (655 çalıştırma) | Son durum, araçlar hariç¹ |
|---|---|---|---|
| Instruction | %62,7 (2985/4760) | %66,3 (3155/4760) | %96,7 (3155/3262) |
| Branch | %68,1 (258/379) | %71,0 (269/379) | %97,5 (269/276) |
| Line | %67,7 (550/813) | %71,7 (583/813) | %95,4 (583/611) |
| Method | %82,9 (102/123) | %87,8 (108/123) | %98,2 (108/110) |

¹ `ScenarioLibraryBuilder` ve `RealismCheck` hariç. Bunlar `main` metodu olan, elle çalıştırılan ve hiçbir test tarafından çalıştırılmayan iki geliştirici aracıdır (ikisi birlikte 1.498 instruction ve 103 branch).

| Sınıf (son durum) | Instruction | Branch | Line |
|---|---|---|---|
| `model.*` (Team, MatchResult, PointDeduction, Forfeit) | %100 | %100 | %100 |
| `StandingRow` | %100 (149/149) | %100 (8/8) | %100 (35/35) |
| `StandingsCalculator` | %98 (380/386) | %98 (53/54) | %99 (75/76) |
| `TieBreaker` | %100 (254/254) | %100 (18/18) | %100 (45/45) |
| `io.CsvLoader` | %98 (469/479) | %100 (30/30) | %98 (85/87) |
| `cli.TableFormatter` | %100 (279/279) | %100 (18/18) | %100 (41/41) |
| `cli.Main` | %87 (226/259) | %100 (30/30) | %86 (54/63) |
| `generator.SeasonGenerator` | %100 (446/446) | %100 (48/48) | %100 (79/79) |
| `generator.StrengthModel` | %97 (455/467) | %97 (33/34) | %96 (78/81) |
| `generator.ScenarioLibrary` | %83 (232/278) | %64 (9/14) | %77 (43/56) |

**Kapsam nasıl kullanıldı.** Kara kutu test takımı çalıştırıldıktan sonra JaCoCo raporu sınıf sınıf incelendi. Rapor gerçek boşlukları gösterdi: CLI'nin hesaplayıcı hatalarını ele alışı (V-02 ve V-06'nın `Main`'e ulaşması), `CsvLoader` içindeki G/Ç hata yolları (geçersiz UTF-8, dosya yerine dizin verilmesi, `read` ya da `close` sırasında hata veren okuyucu) ve hiçbir testin çalıştırmadığı `GeneratedSeason.writeCsv` metodu. Her biri bir gereksinime bağlanan on beş beyaz kutu testi (TC-140..TC-154) bu boşlukları kapattı. Hepsi geçti ve üretim kodunda hiçbir değişiklik yapılmadı.

### 7.2 Kalan boşluklar ve nedenleri

| Konum | Kapsanmayan | Neden |
|---|---|---|
| `StandingsCalculator` l. 109–110, 1 branch | V-06 "ikiden fazla maç" | **Ulaşılamaz.** Önce tekrarlanan yön kontrolü çalışır. İki takım arasında yalnızca iki yön vardır; dolayısıyla üçüncü bir maç her zaman bir yönü tekrar eder ve sayı kontrolüne gelmeden "Duplicate fixture" olarak reddedilir. Belirtim kuralı iki biçimde de ifade ettiği için bırakıldı. |
| `CsvLoader` l. 69–70 | `new Team(...)` çevresindeki catch | Ulaşılamayan savunmacı kod: boş satırlar ayrıştırmadan önce atlandığı için yapıcı hata fırlatamaz. |
| `Main.main` (l. 36–41), örtük yapıcı | `System.exit` sarmalayıcısı | Bir test JVM'i sonlandırırdı; tüm mantık `run` içindedir ve `run` %100 branch kapsamına sahiptir. |
| `Main` l. 125–128, `ScenarioLibrary` ve `StrengthModel` kaynak yolları | eksik ya da bozuk paketlenmiş kaynak, classpath akışlarında `IOException` | Bozuk bir jar ya da sınıf yükleyici gerektirir; gerçek kaynaklar TC-120..TC-126 ile doğrulanır. |
| `ScenarioLibrary.parseCatalogue` (4 branch) | boş / hatalı biçimli / boş katalog | Özel (private) metottur; yalnızca paketlenmiş, düzgün biçimli `catalogue.csv` dosyasını okur. |
| `ScenarioLibraryBuilder`, `RealismCheck` | sınıfların tamamı | Geliştirici araçlarıdır. Çıktıları dolaylı olarak kontrol edilir: TC-126 her senaryoyu yeniden üretip saklanan dosyalarla karşılaştırır; TC-117..TC-119 `RealismCheck`'in yazdırdığı istatistikleri doğrular. |

Bu satırlara ulaşmak için yapay testler (bozuk sınıf yükleyiciler, yansıma, `System.exit`'i yakalama) yazmak bakım maliyeti getirir, güven artırmazdı. Nedenini belgelemek daha dürüst görüldü.

### 7.3 Defect'ler

Her iki defect de `docs/defect-log.md` dosyasında ayrıntılı olarak kayıtlıdır.

**DEF-001: çözülemeyen takımlar Türkçe alfabe sırasıyla değil, Unicode kod noktası sırasıyla gösteriliyor.**

- *Tespit eden:* TC-078 ve TC-079 (eşdeğerlik sınıfı "Türkçe harf içeren adlar"), run-01.
- *Şiddet / öncelik:* Orta / P2. Tablo içeriği ve çözülemedi bayrakları doğruydu, ancak Ç, İ, Ş veya Ü ile başlayan kulüpler için D-02 görüntüleme sırası yanlıştı.
- *Kök neden:* `TieBreaker.order`, `criteria.thenComparing(r -> r.getTeam().name())` ifadesini kullanıyordu. Karşılaştırıcı verilmediğinde `thenComparing`, UTF-16 kod birimlerini karşılaştıran `String.compareTo` kullanır; Ç (U+00C7), İ (U+0130), Ş (U+015E) ve Ü (U+00DC) ise `Z`'nin üzerindedir. TC-078'deki gerçek sıra (önce ASCII adlar, sonra Ç, Ü, İ, Ş) tam olarak kod noktası sırasıdır; bu, kod değiştirilmeden önce hipotezi doğruladı.
- *Düzeltme:* commit `37b9388`; ad anahtarı artık `Collator.getInstance(Locale.forLanguageTag("tr-TR"))` kullanıyor. Yalnızca `TieBreaker.java` değişti.
- *Regresyon:* run-02, TC-078/079 geçiyor, 125/125.
- *Not:* bu defect, ilk kod sürümünün incelenmesi sırasında şüphelenilmiş ve herhangi bir test çalışmadan önce proje notlarına yazılmıştı. Testler onu keşfetmedi, **doğruladı**.

**DEF-002: matches satırında boş isteğe bağlı beşinci alan `NONE` varsayılmak yerine reddediliyor.**

- *Tespit eden:* TC-087 (isteğe bağlı hükmen alanı üzerinde EP/sınır değer analizi, EP-52), run-01.
- *Şiddet / öncelik:* Orta / P2. Geçerli bir dosya reddedildiği için tablo üretilmedi. Elektronik tablo dışa aktarımları sıklıkla sondaki boş bir sütunu yazar.
- *Kök neden:* `CsvLoader`, "hükmen verilmiş" bilgisine yalnızca alan sayısına bakarak karar veriyordu. `split` sondaki boş alanları korur; `A;B;1;0;` satırı bu yüzden 5 alana sahipti ve `Forfeit.valueOf("")` hata fırlattı.
- *Düzeltme:* commit `e03aea7`; `parseForfeit`, boş (kırpılmış) alan için `NONE` döndürüyor. Küçük harfli `home` hâlâ reddediliyor (TC-086 hâlâ geçiyor).
- *Regresyon:* run-02, 125/125.
- *Not:* bu defect önceden **şüphelenilmemişti**. Yalnızca test planı boş alanı bir belirsizlik olarak listelediği (Q9) ve beklenen davranışı test öncesinde sabitlediği için bulundu.

Üç başarısız çalıştırma, iki defect. Hiçbir test geçsin diye değiştirilmedi. Defect log ayrıca defect olmayan üç gözlemi de kaydeder: ulaşılamayan V-06 branch'i, FR-12'nin sezon başında tüm tabloyu işaretlemesi ve `TieBreaker`'ın aldığı satırlar üzerindeki bayrağı ayarlaması (hesaplayıcı her çağrıda yeni satırlar oluşturduğu için güvenli).

### 7.4 Tartışma: test hataları nasıl buldu ya da önledi

**Defect'ler ne gösteriyor.** Her iki defect de kapsam ölçümüyle bulunamazdı. DEF-001'in hatalı sıralama ifadesi, ASCII adlı her tablo oluşturma testi tarafından çalıştırılıyordu. Yalnızca bilinçli seçilmiş "Türkçe harfler" sınıfı onu başarısız kıldı. DEF-002, kodun makul görünen bir kontrole sahip olduğu bir yolda yatıyordu; onu yalnızca bir uç durumun (Q9) tam beklenen davranışının yazıya dökülmesi ortaya çıkardı. Her iki durumda da çok sayıda test çalıştırmaktan çok, *girdi alanı üzerinde düşünmek* (EP, sınır değer analizi, belirsizlikleri çözmek) belirleyici oldu.

**Önleme.** Test tasarımının değerinin bir kısmı, hiçbir test çalışmadan önce ortaya çıktı. Karar tablolarını yazmak, belirtimde bulunmayan soruları gündeme getirdi: Hükmen mağlubiyet otomatik olarak 3 puan siler mi (Q1)? Null bir puan silme listesi geçerli mi (Q2)? Sezon başında tüm tablo "çözülemedi" mi (Q12)? Bunları erkenden yanıtlamak, uygulama tahminlerinin sessiz davranışa dönüşmesini önledi. İki takım/3+ takım analizi (Bölüm 4.2) ayrıca ayrı bir iki takım kod yolu olmayan daha sade bir tasarıma yol açtı.

**Yalnızca iki defect ve bunun gerektirdiği dürüstlük.** Yalnızca iki defect bulundu; ikisi de orta şiddette ve ikisi de çekirdek sıralama kurallarının dışında. Tüm eşitlik bozma vakaları (TC-050..TC-077) ilk çalıştırmada geçti; 515 üreteç, özellik tabanlı ve metamorfik çalıştırma da öyle. Bu, çekirdeğin doğru olduğunu **kanıtlamaz**. Yalnızca sonuçlara bakarak ayırt edilemeyen olası açıklamalar şunlardır:

1. Eşitlik bozma mantığı denenen vakalar için doğrudur. Elle kurulan vakalar, makul yanlış uygulamaları ayırt edecek biçimde tasarlandı (ör. TC-073) ve orada doğru sonuç, denenen belirli davranışlar için gerçek bir kanıttır.
2. Testler ve kod, kuralların aynı yanlış okumasını paylaşıyor. Hem belirtimi hem testleri aynı kişi (aynı yapay zekâ desteğiyle; Bölüm 9) yazdı. `requirements.md` içinde FMT 9/2'nin yanlış yorumlanması her ikisine de işlenmiş olur ve hiçbir test başarısız olmaz.
3. Özellik tabanlı ve metamorfik oracle'lar göründüklerinden daha zayıf. Test tarafındaki oracle istatistikleri ve puanları yeniden hesaplar, ancak eşitlik bozma sırasını hesaplamaz. Karıştırma, yeniden adlandırma ve ev sahibi/deplasman aynalama, örneğin kriterlerin yanlış sıralanmasını yakalamaz; çünkü böyle bir hata kendiliğinden girdi sırasından ve sahadan bağımsızdır. Bu testler altyapıyı (istatistikler, hükmen düzeltmesi, belirlilik, üreteç) korur, FMT sıralamasını korumaz.

Hiç başarısız olmayan bir test takımının hata yakalama gücü de bilinmez. Mutasyon testi (Bölüm 8.3) bunu ölçmenin standart yolu olurdu.

---

## 8. Tartışma ve Sonuç

### 8.1 Yaklaşımın etkinliği

Kombinasyon, tam olarak belirtilebilen kısımlar için amaçlandığı gibi çalıştı. FMT kuralları, kaynaklı bir gereksinim tablosuna dönüştürüldü. Her kural eşdeğerlik sınıflarına, sınır değerlere ve karar tablosu kurallarına, elle hesaplanmış beklenen değerlerle dönüştürüldü. Her test bir gereksinime ve bir gerekçeye bağlanabilir. Kırmızıdan yeşile giden commit geçmişi defect döngüsünü görünür kılıyor. Özellik tabanlı ve metamorfik test, kimse beklenen tabloyu yazmadan büyük ve gerçekçi girdiler (18 takım, en fazla 306 maç, hükmen ve puan silmeler) ekledi. Üreteç ve kendi testleri bu girdilerin tekrarlanabilir ve makul olmasını sağlar. Kapsam, asıl amacı için kullanıldı: gereksinimle ilgili, test edilmemiş üç kod yolu grubu buldu ve kalan boşlukların her biri gerekçelendirilebildi.

### 8.2 Sınırlılıklar

- **Belirtim ve testler için tek yazar.** Gereksinimler, test planı, testler ve kodun hepsi, tek bir yapay zekâ aracıyla çalışan tek bir öğrenciden gelir. Bağımsız bir inceleme yapılmadı; bu nedenle kuralların ortak yanlış yorumları (Bölüm 7.4, madde 2) fark edilmez.
- **Üreteç gerçekçiliği güç modeliyle sınırlıdır.** Senaryolar modelin gerçekçiliği kadar gerçekçidir: bağımsız Poisson golleri, sabit güçler, varsayılan ev sahibi avantajı H = 1,10 ve iki kulüp için yeni çıkan takım ortalamaları. Kütüphanedeki eşitlikler bu modelden gelir; bu yüzden gerçek dünyadaki daha nadir örüntüler yeterince temsil edilmemiş olabilir.
- **Veri kaynağı olarak Wikipedia.** Güç verileri İngilizce ve Türkçe Wikipedia'dan gelir; birbirleriyle çapraz kontrol edilmiş, ancak tff.org ile karşılaştırılmamıştır. Gaziantep FK ve Hatayspor'un 2022–23 satırları, ligden çekilmelerinden sonraki maçları da içerir ve yayımlandığı gibi bırakılmıştır.
- **Doğrulanmamış varsayımlar.** A-01..A-04 (averaj tanımı, puan silmenin ikili puanları etkilememesi, sezon içinde aynı kurallar, yalnızca oynanmış maçlar) FMT ya da FDT'de yazılı değildir. Testler uygulamayı bu varsayımlara *göre* doğrular; varsayımların kendisini doğrulayamaz.
- **Sezon başında FR-12.** Hiç maç oynanmamışken, kelimesi kelimesine okuma her takımı "çözülemedi" olarak işaretler (TC-015, TC-078). Bu kurallarla tutarlıdır ama kullanıcıya şaşırtıcı gelebilir. Kabul edildi, gerçek bir kullanıcıyla doğrulanmadı.
- **Mesaj kontrolleri alt dizeyle.** Birkaç negatif test hata mesajlarını alt dizeyle kontrol eder ve ifade değişirse bozulur.
- **Yapay zekâ desteği.** Kodun, planın ve testlerin büyük bölümleri öğrencinin yönlendirmesiyle bir yapay zekâ aracı tarafından taslak olarak yazıldı (Bölüm 9). Bu işi hızlandırır, ancak yukarıdaki 2. maddenin riskini artırır ve öğrencinin her şeyi bizzat gözden geçirmesini gerektirir.

### 8.3 Olası iyileştirmeler

1. **Mutasyon testi**, örneğin PIT ile; test takımının enjekte edilen hataların (değiştirilen karşılaştırmalar, yer değiştiren kriterler, kaldırılan `reversed()`) kaçını yakaladığını ölçmek için. Bu, kapsamın yanıtlayamadığı "testler ne kadar güçlü?" sorusuna cevap verirdi.
2. **Kabul testi olarak gerçek tam sezon sonuçları.** Gerçek sezonların (ör. Alanyaspor–Kayserispor–Gaziantep FK eşitliğinin bulunduğu 2024–25) maç düzeyindeki sonuçlarını resmî bir kaynaktan toplayıp motorun tablosunu resmî final tablosuyla karşılaştırmak. Bu, yalnızca belirtime karşı doğrulama (verification) değil, gerçeğe karşı geçerleme (validation) olurdu.
3. **Üretilen sezonlar için eşitlik bozma oracle'ı.** FMT 9/2'nin ikinci, bağımsız yazılmış bir uygulaması (diferansiyel test), özellik tabanlı testlerin yalnızca istatistikleri değil sırayı da kontrol etmesini sağlardı.
4. **Sürekli entegrasyon.** Regresyonların elle çalıştırmaya güvenmeden yakalanması için her push'ta `mvn verify` komutunu otomatik çalıştırmak (ör. GitHub Actions).
5. TFF talimatlarına hâkim birinin gereksinimleri **bağımsız olarak incelemesi**.

### 8.4 Sonuç

Proje, FMT Md. 9 eşitlik bozma kurallarını, kolay yanlış anlaşılan "alt eşitliklere tekrar bakılmaz" kuralı dahil, uygular ve bunları 655 çalıştırmalık planlı, izlenebilir bir JUnit 5 test takımıyla doğrular. Testler, ikisi de çekirdek sıralama mantığının dışında kalan iki orta şiddette defect buldu ve test takımı artık tamamen geçiyor; toplamda %71,0, iki çevrimdışı geliştirici aracı hariç tutulduğunda %97,5 branch kapsamı var. Sonuçlar, test senaryolarına açıkça tasarlanmış davranışlar için makul bir güven verir. Kuralların yorumu konusunda daha az güven verir ve test takımının gücü henüz ölçülmemiştir. En değerli sonraki adımlar mutasyon testi ve gerçek, resmî olarak yayımlanmış sonuçlara karşı bir kabul testidir.

---

## 9. Yapay Zekâ Aracı Kullanım Beyanı

`[ÖĞRENCİ: gözden geçir ve uyarla]`

Bu proje, yapay zekâ asistanı olarak Claude Code (model Claude Opus 5.5) ile geliştirildi. Her adım `docs/ai-usage-log.md` dosyasına kaydedildi ve commit geçmişi işin sırasını gösteriyor.

**Aracı nasıl kullandım.** Ana Claude Code oturumu orkestratör olarak çalıştı. Sonraki fazlarda ben başında değilken görevleri alt ajanlara devretti; ardından sonuçlarını gözden geçirdi, derledi, çalıştırdı ve commit etti. Proje kuralları (CLAUDE.md), alt ajanların çekirdek kodda fark ettikleri defect'leri düzeltmemesini, raporlamasını şart koşuyordu; böylece defect'ler testlerle bulunup kayda geçecekti. Başarısız testler, düzeltmeden önce commit edildi.

**Yapay zekânın ürettikleri.** Şunların taslakları: gereksinim dokümanı; tüm üretim kodunun ilk sürümü (model, hesaplayıcı, eşitlik bozucu, CSV okuyucu, CLI, üreteç); test planı (ilk sürümde 95, son sürümde 133 test senaryosu); tüm JUnit testleri; DEF-001 ve DEF-002 için kök neden analizi ve düzeltmeler; kapsam analizi ve 15 kapsam boşluğu testi; Wikipedia verilerinin toplanması ve kontrolü; araştırma bölümü taslağı; ve bu rapor taslağı. Log'a göre bir alt ajan, elle hesaplanan beklenen değerleri ayrı bir Python kural hesaplayıcısıyla da kontrol etti.

**Kendi kararlarım ve düzeltmelerim.** Konuyu (alternatif konu yerine puan durumunu) ben seçtim ve 2026–27 Süper Lig statüsünü kendim buldum. Gereksinimlerin dayandığı kural bilgisini ben sağladım: ikili averaj iki maçın toplamıdır, hükmen yenilgi 3-0'dır, silinen puanlar hesaba katılır ve sezon içinde de aynı sıralama kuralı geçerlidir. Yapay zekânın metinde bulamadığı bir kuralı ben ekledim (sarı kart sayacı cezadan sonra sıfırlanır; reddedilen alternatif konuyla ilgiliydi) ve bir haber sitesinden aldığı, TFF metninde olmadığı ortaya çıkan bir iddiaya itiraz ettim. Gereksinimleri ve D-01, D-02 kararlarını onayladım, komut satırı arayüzlü CSV'yi seçtim, herkese açık (public) depoyu seçtim ve başarısız testlerin kırmızı hâliyle commit edilmesine karar verdim. Üreteç için koşulları ben belirledim: kurgulanmış eşitlik yok, önceden üretilmiş 10–12 gerçekçi sezon, güçler son on sezondan, yeni çıkan takım ortalamaları. Test planındaki Q1–Q13 belirsizlikleri, otonom modda orkestratör tarafından çözüldü ve yeniden gözden geçirebilmem için kaydedildi.

**Sorumluluğum.** Gereksinimleri, test planını, defect analizini ve bu raporu gözden geçirdim; her test senaryosunu, beklenen değeri ve tasarım kararını açıklayabilirim. `[ÖĞRENCİ: neleri ayrıntılı incelediğini, neleri değiştirdiğini dürüstçe yaz ve bu bölümü dersin yapay zekâ politikasına göre uyarla.]`

---

## 10. Kaynakça

[1] Türkiye Futbol Federasyonu, *2026–2027 Sezonu Süper Lig Müsabakaları Statüsü*. [Çevrimiçi]. Erişim: https://www.tff.org/Resources/TFF/Documents/STATULER/2026-2027/2026-2027-sezonu-super-lig-musabakalari-statusu.pdf (erişim tarihi: Nov. 12, 2026).

[2] Türkiye Futbol Federasyonu, *Futbol Müsabaka Talimatı* (PDF dated Aug. 18, 2025). [Çevrimiçi]. Erişim: https://www.tff.org/Resources/TFF/Documents/TALIMATLAR/Futbol-Musabaka-Talimati.pdf (erişim tarihi: Nov. 12, 2026).

[3] Türkiye Futbol Federasyonu, *Futbol Disiplin Talimatı*. [Çevrimiçi]. Erişim: https://www.tff.org/Resources/TFF/Documents/TALIMATLAR/Futbol-Disiplin-Talimati.pdf (erişim tarihi: Nov. 12, 2026).

[4] Wikipedia contributors, "2016–17 Süper Lig" through "2025–26 Süper Lig," and "2026–27 Süper Lig," *Wikipedia, The Free Encyclopedia* (English). [Çevrimiçi]. Erişim: https://en.wikipedia.org/wiki/2024%E2%80%9325_S%C3%BCper_Lig (other seasons by the same pattern; full list in `docs/data-sources.md`) (erişim tarihi: Nov. 12, 2026).

[5] Vikipedi katılımcıları, "2016-17 Süper Lig" through "2026-27 Süper Lig," *Vikipedi, Özgür Ansiklopedi* (Turkish). [Çevrimiçi]. Erişim: https://tr.wikipedia.org/wiki/2024-25_S%C3%BCper_Lig (other seasons by the same pattern) (erişim tarihi: Nov. 12, 2026).

[6] International Software Testing Qualifications Board (ISTQB), *Certified Tester Foundation Level Syllabus*, v4.0.1, Sep. 15, 2024. [Çevrimiçi]. Erişim: https://www.istqb.org/wp-content/uploads/2024/11/ISTQB_CTFL_Syllabus_v4.0.1.pdf (erişim tarihi: Nov. 12, 2026).

[7] S. Bechtold, S. Brannen, et al., *JUnit 5 User Guide*, version 5.11.4. [Çevrimiçi]. Erişim: https://docs.junit.org/5.11.4/user-guide/ (erişim tarihi: Nov. 12, 2026).

[8] The Apache Software Foundation, "Using JUnit 5 Platform," *Maven Surefire Plugin documentation*. [Çevrimiçi]. Erişim: https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html (erişim tarihi: Nov. 12, 2026).

[9] G. J. Myers, C. Sandler, and T. Badgett, *The Art of Software Testing*, 3rd ed. Hoboken, NJ, USA: Wiley, 2011, ISBN 978-1-118-03196-4.

[10] K. Claessen and J. Hughes, "QuickCheck: A lightweight tool for random testing of Haskell programs," in *Proc. 5th ACM SIGPLAN Int. Conf. Functional Programming (ICFP '00)*, 2000, pp. 268–279, doi: 10.1145/351240.351266.

[11] T. Y. Chen, F.-C. Kuo, H. Liu, P.-L. Poon, D. Towey, T. H. Tse, and Z. Q. Zhou, "Metamorphic testing: A review of challenges and opportunities," *ACM Comput. Surv.*, vol. 51, no. 1, Art. 4, 2018, doi: 10.1145/3143561.

[12] JaCoCo Project, "Coverage Counters," *JaCoCo Java Code Coverage Library documentation*. [Çevrimiçi]. Erişim: https://www.jacoco.org/jacoco/trunk/doc/counters.html (erişim tarihi: Nov. 12, 2026).

[13] L. Inozemtseva and R. Holmes, "Coverage is not strongly correlated with test suite effectiveness," in *Proc. 36th Int. Conf. Software Engineering (ICSE '14)*, Hyderabad, India, 2014, pp. 435–445, doi: 10.1145/2568225.2568271.

---

## Ek A. Destekleyici Materyaller

Tüm yollar depo kök dizinine görecelidir.

| Materyal | Konum |
|---|---|
| Derleme dosyası (Java 21, JUnit 5.11.4, Surefire 3.5.2, JaCoCo 0.8.13) | `pom.xml` |
| Kullanım yönergeleri | `README.md` |
| Gereksinim belirtimi v1.1 | `docs/requirements.md` |
| Test planı v1.1 (EP, sınır değer, karar tabloları, negatif, özellik tabanlı/metamorfik, izlenebilirlik, Q1–Q13, gerçekleşen sonuçlar) | `docs/test-plan.md` |
| Defect log (DEF-001, DEF-002, gözlemler) | `docs/defect-log.md` |
| Kapsam analizi | `docs/coverage-analysis.md` |
| Üreteç tasarımı ve gerçekçilik kontrolü | `docs/generator.md` |
| Veri kaynakları ve kontroller | `docs/data-sources.md` |
| Yapay zekâ kullanım günlüğü | `docs/ai-usage-log.md` |
| Üretim kaynak kodu | `src/main/java/edu/svv/standings/` (`model/`, `io/`, `cli/`, `generator/`) |
| Güç verileri (189 satır) ve 2026–27 kulüp listesi | `src/main/resources/strength/` |
| Senaryo kütüphanesi S01–S12 ve katalog | `src/main/resources/scenarios/` |
| JUnit testleri | `src/test/java/edu/svv/standings/` |
| Örnek CLI girdisi (uydurma sonuçlar) | `examples/` |
| Test çalıştırması 1, ilk (125 çalıştırma, 3 başarısız) | `docs/results/run-01-initial/` |
| Test çalıştırması 2, düzeltmeler sonrası (125/125) | `docs/results/run-02-after-fixes/` |
| Test çalıştırması 3, tam takım (655/655) | `docs/results/run-03-full-suite/` |
| JaCoCo raporu (HTML, CSV, XML) | `docs/results/coverage/` (`index.html`, `jacoco.csv`, `jacoco.xml`) |
| Süreç geçmişi | `git log --oneline` |
