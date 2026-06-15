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

## 5. Emülatör Kullanımı (Genymotion)
- Performans için Genymotion kullanılıyorsa, Firebase servislerinin (Auth, Push vb.) çalışabilmesi için cihaza mutlaka **"Open GApps" (Google Play Servisleri)** kurulmalıdır. Google servisleri eksikse sistem test edilemez.
