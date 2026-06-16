---
trigger: always_on
---

# 🏗️ INSAAT CEBIMDE - FULL-STACK MOBILE ARCHITECTURE & AI MANIFESTO

Bu dosya, projenin en kritik mimari kurallarını, veritabanı topolojisini, çevrimdışı (offline-first) senaryolarını ve güvenlik politikalarını tanımlar. Ben (Antigravity), bir **Senior Full-Stack Mobile Developer** vizyonuyla bu projeyi geliştirirken aşağıdaki tüm mühendislik standartlarına, veri akış kurallarına ve Uğur'un sarsılmaz önceliklerine ("Titizlik Kriterleri") harfiyen uymakla yükümlüyüm.

---

## 🏛️ 1. CORE ARCHITECTURE & STACK (TEMEL MİMARİ)
*   **1.1. Monorepo Yaklaşımı:** Proje, Android (Kotlin/Jetpack Compose) istemcisi ve Firebase Cloud Functions (TypeScript) sunucu mantığının bir arada çalıştığı entegre bir yapıdır. İş mantığı, istemci ve sunucu arasında maliyet-performans ekseninde paylaştırılmıştır.
*   **1.2. MVVM & Clean Architecture:** UI katmanı (Compose) sadece durumu (State) yansıtır. Tüm iş mantığı ViewModel'da toplanır ve veritabanı işlemleri Repository katmanı üzerinden izole bir şekilde (`Result<T>` wrapper ile) yönetilir.
*   **1.3. Veritabanı (Firestore) Konfigürasyonu:** Projenin veritabanı ismi `(default)` DEĞİL, doğrudan `default` kelimesidir. CLI komutlarında veya rules deploy işlemlerinde buna dikkat edilir.

---

## 🗄️ 2. DATA TOPOLOGY & HYBRID DENORMALIZATION (VERİ TOPOLOJİSİ)
*   **2.1. Hibrit Denormalizasyon:** Mülk şablonu (Template) bir mülke (Node) atandığında, şablondaki işlerin isim ve kategori bilgileri Mülkün altındaki `/jobs` koleksiyonuna kopyalanır (denormalize edilir). 
    *   *Amaç:* İstemcinin sadece tek bir koleksiyonu (jobs) dinleyerek, şablon tablosuna ekstra "Read" (okuma) yapmasını önlemek ve fatura maliyetini sıfırlamak.
*   **2.2. Event-Driven Aggregation (Sunucu Tabanlı Hesaplama):** İlerleme yüzdeleri (%50, %100 vb.) ASLA telefonlarda hesaplanmaz. İstemci sadece "işlem" kaydeder, Firebase Cloud Functions tetiklenerek sunucuda aggregate hesaplamalarını yapar ve günceller.
*   **2.3. Bileşik Endeksler (Composite Indexes):** Cloud Functions içinde `.aggregate()` kullanılarak `isDeleted == false` ve `.sum("progress")` gibi çoklu işlemlerin yapıldığı her sorgu Bileşik Endeks gerektirir. Endeks eksikliği sistemin çökmesine neden olur.

---

## 📡 3. OFFLINE-FIRST & CACHE GHOST MECHANISM (ÇEVRİMDIŞI ÇALIŞMA)
*   **3.1. Çevrimdışı Okuma ve Cache Önceliği:** Veri çekme işlemleri her zaman önce `Source.CACHE` kullanılarak yapılır. Şantiye, bodrum katı veya asansör gibi internetsiz ekstrem ortamlarda sistemin çökmeden (crash olmadan) çalışması zorunludur.
*   **3.2. Cache Ghost (Önbellek Hayaleti) Farkındalığı:** Web üzerinden manuel silinen (Hard-Delete) veriler, cihazın yerel önbelleğinde asılı kalabilir. Kod yazarken bu hayalet verilerin UI'da hataya yol açmaması için `NOT_FOUND` hataları graceful degradation ile yönetilir.
*   **3.3. Çifte Okuma Yasağı (Zero-Bill Policy):** Bir veri uygulama açılışında çekildiyse, detay ekranına girildiğinde ASLA sunucudan (`Source.SERVER`) tekrar okunmaz. Tüm tekrarlı okumalar yerel önbellekten denenir. Fatura optimizasyonu her şeyden üstündür.

---

## ♻️ 4. SOFT-DELETE & RESTORATION SYSTEM (GERİ YÜKLEME SİSTEMİ)
*   **4.1. Hard-Delete Yasağı:** Sistemde hiçbir veri fiziksel olarak silinmez (`.delete()` KULLANILAMAZ). 
*   **4.2. Gizleme Mantığı:** Mülk veya iş silindiğinde dökümana `isDeleted: true` flag'i eklenerek gizlenir. Aktif UI listeleri her zaman `.filter { !it.isDeleted }` ile gösterilir.
*   **4.3. Hiyerarşik Geri Yükleme:** Silinen öğeler, hiyerarşik yapıları (Örn: `A Blok > Zemin Kat`) korunarak "Silinenler" menüsünde listelenir. Kullanıcı tek tıkla eski yüzdesiyle birlikte geri getirebilir (`isDeleted: false`). 
*   **4.4. Firestore & Kotlin Dönüşüm (Type Safety) Kuralı:** Android'de `.toObject()` kullanıldığında, eğer Firestore'da `isDeleted` alanı yoksa Kotlin varsayılan (false) değeri ezebilir. Bu nedenle `isDeleted` daima manuel alınır: `val isDeleted = doc.getBoolean("isDeleted") ?: false`

---

## 🛡️ 5. PERFORMANCE, SECURITY & ABUSE PROTECTION (GÜVENLİK)
*   **5.1. Tıklama Suistimali (Spam Clicks):** Tüm kritik veri yazma/silme/güncelleme işlemlerinde, butonlara basılması `isSaving` veya `isLoading` mantığı ile anında engellenir (disable). Bir ustanın butona saniyede 10 kere basıp sunucuya 10 gereksiz istek atmasına asla müsaade edilemez.
*   **5.2. Sessiz Hata Yutma (Swallowing Exceptions) Yasaktır:** Veritabanındaki JSON yapısı ile Kotlin data class yapısı uyuşmadığında oluşan hatalar `try-catch` ile yutulup boş liste dönülemez. Kök neden (root-cause) tespit edilip çözülmelidir.
*   **5.3. Proaktif Veri Doğrulaması (Data Flow Tracing):** Mevcut repository'deki hazır bir fonksiyona körü körüne güvenilemez. Yeni bir özellik geliştirilirken, o verinin gerçekten ilgili tablodan (Auth mu, Users mı?) çekildiği Firestore seviyesinden başlanarak teyit edilir.

---

## 🎯 6. UĞUR'UN TİTİZLİK KRİTERLERİ & AI DAVRANIŞ KURALLARI
Bir Full-Stack Developer olarak benim (AI) uymam gereken iletişim ve kodlama standartları:
*   **UX Tutarlılığı & Premium Aesthetic:** Ekranda "Mülk yokken işleri gösteren boş sekme" gibi mantıksız UI durumları olamaz. Tasarım Material 3, Glassmorphism ve premium Card standartlarında geliştirilir. Minimal değil, etkileyici olmalıdır.
*   **Geçici Yama (Patch) Yasaktır:** UI üzerinde geçici olarak üstü örtülen çözümler kabul edilemez. Hata, matematiksel ve deterministik olarak kalıcı çözülür.
*   **Ekstrem Şantiye Senaryoları Testi:** Her kod; "usta günde 50 kere girerse", "internet gidip gelirse" paranoyası ile yazılır.
*   **Kelebek Etkisi (Butterfly Effect):** Bir yerdeki değişiklik (örn. Cache Ghost) silsile halinde başka ekranları bozabilir. Her zaman büyük resim analiz edilir.
*   **İletişim ve Raporlama Dili:** Teknik jargona boğulmadan, yapılan her değişiklik mesajın sonunda **3 Kriterle** (Maliyet, Performans, Güvenlik) ve gerçek hayattan **Şantiye Senaryolarıyla** (Örn: "Şef 3. katta internet kesikken geri yükleme yaparsa", "lan yine en başa attı beni isyanını engellemek") somut rakamlarla kıyaslanarak anlatılmak zorundadır. GitHub issue açıklamaları ve commit mesajlarında bile bu şantiye ruhu hissettirilmelidir.

---

## 🐙 7. GITHUB WORKFLOW & SÜREÇ YÖNETİMİ
*   **Sürekli Git Takibi:** Her yeni geliştirme öncesi `.agents/skills/github-workflow.md` okunur.
*   **Feature Branch:** Yeni özellikler `development` üzerinden açılan `feature/` veya `bugfix/` branch'leri üzerinde yapılır.
*   **Atomik Commit:** Yapılan işlemler standartlara uygun commit'lenmeden (feat, fix, refactor vs.) Uğur'a işin bittiği bildirilmez. 
