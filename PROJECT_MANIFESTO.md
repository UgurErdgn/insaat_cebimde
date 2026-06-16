---
trigger: always_on
---

# InsaatCebimde - Project Manifesto & AI Knowledge Base
Bu dosya, projenin en kritik mimari kurallarını, veritabanı ayarlarını ve geçmişte yaşanmış kronik hataların çözümlerini barındırır. Aynı zamanda Uğur'un çalışma prensiplerini ve yapay zekanın (Antigravity) problemlere nasıl yaklaşması gerektiğini tanımlar. Her yeni işlemde BU KURALLARI DİKKATE ALMAK ZORUNDAYIM.

## 1. Mimari Prensipler & Veritabanı Konfigürasyonu
- **Database Name:** Projenin veritabanı ismi `(default)` DEĞİL, **doğrudan `default`** kelimesidir (parantez yok). CLI komutları veya REST API aramaları yapılırken buna dikkat edilmelidir. Firebase CLI hata verirse, `firebase.json` içerisinde `"database": "default"` tanımı kullanılmalıdır.
- **Maliyet Yönetimi (Sıfır Fatura Prensibi):** Firestore Read/Write limitlerini korumak her şeyden önemlidir. Gereksiz hiçbir veri okunamaz, hiçbir veri gereksiz yere iki kere yazılamaz.
- **Hesaplamalar Sunucuda (Event-Driven Aggregation):** İlerleme yüzdeleri (%50, %100 vb.) ASLA telefonlarda (istemcide) hesaplanmaz. Sadece `Cloud Functions` tetiklenir ve sunucuda hesaplanır.
- **Offline-First & Snapshot Listener:** Veri çekme işlemleri her zaman Cache öncelikli veya `SnapshotListener` tabanlı olmalıdır. Şantiye/Bodrum gibi internetsiz ortamlarda sistem çalışmaya devam etmelidir.
- **Bileşik Endeksler (Composite Indexes):** Cloud Functions içinde `.aggregate()` kullanılarak hem `.where("isDeleted", "==", false)` hem de `.sum("progress")` yapılan her sorgu **Bileşik Endeks (Composite Index)** gerektirir. Endeks eksikse Cloud Function çöker ve veritabanı işlemleri iptal olur.

## 2. Sıfır Hard-Delete (Soft Delete) Mimarisi
- Sistemde **hiçbir veri fiziksel olarak silinmez (`.delete()` YAPILAMAZ)**.
- Mülk şablonundan bir iş çıkarıldığında veya bir node silindiğinde dökümana `isDeleted: true` flag'i eklenerek gizlenir. Şablon geri alınırsa `isDeleted: false` yapılarak eski yüzdesiyle geri döner.
- UI (Mobil Uygulama) tarafındaki listeler her zaman `.filter { !it.isDeleted }` ile gösterilir.
- **Kotlin & Firestore Veri Çekme Kuralı:** Android tarafında `toObject(NodeJob::class.java)` kullanıldığında `isDeleted` alanı Firestore'da yoksa Kotlin varsayılan değeri ezebilir. Bu yüzden `isDeleted` flag'i HER ZAMAN manuel alınmalıdır: `val isDeleted = doc.getBoolean("isDeleted") ?: false`

## 3. Hibrit Denormalizasyon Mimarisi
- Mülk şablonu bir mülke atandığında, şablondaki işlerin isim ve kategori bilgileri Mülkün altındaki `/jobs` koleksiyonuna **kopyalanır (denormalize edilir)**.
- Amaç: İstemcinin sadece `jobs` koleksiyonunu dinleyerek işleri görebilmesi (JobTemplate tablosuna gereksiz okuma yapmamak).
- Yerel ilerleme (`localProgress`) güncellemeleri Firebase Cloud Functions üzerinden sunucu tarafında batch olarak yönetilir.

## 4. Uğur'un Titizlik Kriterleri ve AI Davranış Kuralları
- **Geçici Yama Yasaktır:** Uğur "yamaları" (geçici fix'leri) sevmez. Bir bug varsa o bug'ın UI kısmındaki üstü örtülmez; sistemin köküne inilip matematiksel, deterministik ve kalıcı olarak çözülür.
- **Ekstrem Şantiye Senaryoları:** Sistemi test ederken en ince detayı, "usta günde 50 kere ekrana bakarsa", "bodrumda internet kesilirse" gibi en uç senaryoları düşünürüm. Yazılacak her kod, bu paranoyak koşullara %100 dayanıklı olmalıdır.
- **UX Tutarlılığı:** Ekranında saçma şeyler (Mülk yokken işleri gösteren boş sekme olması vb.) çıkmasına tahammül edilmez.
- **Kelebek Etkisi (Butterfly Effect Debugging):** Bir hata olduğunda, sığ bir şekilde sadece hata veren koda bakmam. Her zaman büyük resim ve silsile (Bölgesel DNS hataları, Firebase asenkron mimarisi vb.) hesaba katılır ve kök neden (root-cause) analiziyle Uğur'a rapor edilir.
- **İletişim ve Anlatım Dili:** Uğur derin kod terminolojisine girmeyi sevmez. HER mesajın sonunda mutlaka yapılan değişikliğin özetini çıkaran bir yapı kullanılmalıdır: Neyi değiştirdik? Neden değiştirdik? Neleri etkiledi? Yapılan her teknik değişiklik, *gerçek hayattan (şantiyeden)* 1, 2 veya duruma göre 3 farklı senaryo örneği ile detaylandırılmalıdır. **Ayrıca verilen bu örneklerde; eski ve yeni durum arasındaki MALİYET (Read/Write belgesi sayısı), PERFORMANS (Hız/İnternet kullanımı) ve GÜVENLİK (Kötüye kullanım riski) farkları kesinlikle rakamsal veya somut değerlerle (Örn: "Eskiden 1000 okuma yapıyordu, şimdi 1 okuma yapıyor") kıyaslanmalıdır.** Bu kural istisnasız HER cevapta uygulanacaktır.

## 5. GitHub & İş Akışı (Workflow) Kuralları
- **Otomatik Süreç Yönetimi:** Yapılacak her task, eklenecek her özellik veya çözülecek her hata (bug) için geliştirme sürecine başlarken **istisnasız olarak** `.gemini/skills/github-workflow.md` dosyasındaki kurallar uygulanmalıdır.
- AI asistan (Antigravity), Uğur'dan yeni bir talep geldiğinde otomatik olarak bu skill dosyasını okumalı; işe başlamadan önce GitHub Issue oluşturmalı, feature branch açmalı ve commit standartlarına (feat:, fix:, vb.) harfiyen uymalıdır. Bu kural sayesinde Uğur'un her seferinde workflow hatırlatmasına gerek kalmaz.

## 6. Proaktif AI Davranışı & Veri Akışı Doğrulaması (Data Flow Tracing)
- **Kör İtimat Yasaktır:** AI, mevcut repoda (repository katmanında) hazır bulduğu bir fonksiyona (örn. `getCurrentUser()`) körü körüne güvenip UI'a veri bağlayamaz. Eğer UI'da `publicInviteId`, `permissions` gibi ek özellikler gösterilecekse, AI **zorunlu olarak** Repository katmanından başlayıp Firestore veritabanına kadar inerek o verinin GERÇEKTEN çekilip çekilmediğini (Auth tablosu mu yoksa Users koleksiyonu mu olduğunu) doğrulamalıdır. Uğur'un ekran görüntüsü atıp "bu neden gelmiyor" demesine gerek bırakmadan, proaktif şekilde eksikleri bulup tamamlamalıdır.

## 7. UI Güvenliği & Suistimal Koruması (Abuse Protection)
- **Tıklama Suistimali (Spam Clicks):** Tüm kritik işlemlerde (kayıt, veri ekleme, çıkış yapma, ilerleme güncelleme) mutlaka `isSaving` veya `isLoading` mantığı ile butonlara basılması **engellenmeli (debounce/disable)**. Bir kullanıcının butona saniyede 10 kere basıp Firestore'a 10 gereksiz yazma isteği yollamasına ASLA müsaade edilemez. Performans, Firestore faturası ve sistem güvenliği hep ilk planda tutulur.

## 8. Veri Modeli ve Tür (Type) Güvenliği (Silent Failure Koruması)
- **Hata Yutma (Swallowing Exceptions) Yasaktır:** Firestore'dan veri çekerken `.toObject()` kullanıldığında, veritabanındaki JSON yapısı ile Kotlin data class yapısı (Örn: `Map<String, Obje>` vs `Map<String, Liste>`) en ufak bir uyumsuzluk gösterirse Firestore exception fırlatır. `try-catch` bloklarında bu exception'lar ASLA sessizce geçiştirilip varsayılan (boş) objeler dönülmemelidir. AI, yeni bir özellik geliştirirken data class'ların Firestore'daki gerçek JSON karşılığını kesin olarak teyit etmeli, uyuşmazlık kaynaklı "Kelebek Etkisi" hatalarını önceden öngörmelidir.

## 9. Sıfır Fatura Prensibi & Gereksiz Okuma Koruması (Read Optimization)
- **Duplicate Firestore Read (Çifte Okuma) Yasaktır:** Bir veri (örneğin kullanıcı bilgisi) uygulama açılışında zaten çekildiyse, Profil veya başka bir sekmeye girildiğinde ASLA tekrar varsayılan `.get()` ile (sunucudan) okunamaz. Tüm tekrarlı okumalar istisnasız olarak **önce `.get(Source.CACHE)`** ile yerel bellekten denenmeli, sadece boş dönerse `Source.SERVER` kullanılmalıdır. Bu kural, Offline-First mimarisinin ve Firestore fatura optimizasyonunun bel kemiğidir. Her yeni yazılan Repository fonksiyonunda AI bu bağlamı (veri daha önce önbelleğe alındı mı?) düşünmek zorundadır.
