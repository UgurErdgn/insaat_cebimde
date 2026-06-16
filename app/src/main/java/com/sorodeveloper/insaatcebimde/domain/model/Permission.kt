package com.sorodeveloper.insaatcebimde.domain.model

/**
 * Granüler Yetki Sabitleri (Granular Permissions)
 *
 * Sistem "sabit roller" yerine granüler yetki listeleri kullanır.
 * Her kullanıcının bir mülkteki yetkisi, bu enum değerlerinin bir alt kümesidir.
 * Hiyerarşi, kümelerin alt küme (subset) mantığıyla doğal olarak oluşur.
 */
enum class Permission(val key: String, val displayName: String, val category: String) {
    // 1. Mülk Yönetimi
    MANAGE_NODES("MANAGE_NODES", "Mülk Ekle/Düzenle/Sil", "Mülk Yönetimi"),

    // 2. İş Yönetimi
    VIEW_JOBS("VIEW_JOBS", "İşleri Görüntüle", "İş Yönetimi"),
    CREATE_JOBS("CREATE_JOBS", "Yeni İş Ekle", "İş Yönetimi"),
    EDIT_JOBS("EDIT_JOBS", "İşleri Düzenle/Sil", "İş Yönetimi"),
    UPDATE_PROGRESS("UPDATE_PROGRESS", "İlerleme Gir (% Yüzde)", "İş Yönetimi"),

    // 3. Notlar ve Medya
    VIEW_NOTES("VIEW_NOTES", "Notları Görüntüle", "Medya ve Notlar"),
    MANAGE_NOTES("MANAGE_NOTES", "Not Ekle/Düzenle/Sil", "Medya ve Notlar"),
    UPLOAD_MEDIA("UPLOAD_MEDIA", "Fotoğraf/Video Yükle", "Medya ve Notlar"),
    DELETE_MEDIA("DELETE_MEDIA", "Medya Sil", "Medya ve Notlar"),

    // 4. Finans
    VIEW_FINANCIALS("VIEW_FINANCIALS", "Maliyetleri Görüntüle", "Finans"),
    MANAGE_FINANCIALS("MANAGE_FINANCIALS", "Maliyet Gir/Düzenle", "Finans"),

    // 5. Ekip
    INVITE_MEMBERS("INVITE_MEMBERS", "Davet Gönder", "Ekip Yönetimi"),
    MANAGE_MEMBERS("MANAGE_MEMBERS", "Üyeleri ve Yetkileri Yönet", "Ekip Yönetimi");

    companion object {
        fun fromKeys(keys: List<String>): Set<Permission> {
            return keys.mapNotNull { key -> entries.find { it.key == key } }.toSet()
        }

        fun toKeys(permissions: Set<Permission>): List<String> {
            return permissions.map { it.key }
        }

        // Hazır Şablonlar
        val OWNER_PRESET: Set<Permission> = entries.toSet()
        
        val FOREMAN_PRESET: Set<Permission> = setOf(
            MANAGE_NODES,
            VIEW_JOBS, CREATE_JOBS, EDIT_JOBS, UPDATE_PROGRESS,
            VIEW_NOTES, MANAGE_NOTES, UPLOAD_MEDIA, DELETE_MEDIA,
            INVITE_MEMBERS
        )
        
        val WORKER_PRESET: Set<Permission> = setOf(
            VIEW_JOBS, UPDATE_PROGRESS, VIEW_NOTES, UPLOAD_MEDIA
        )
    }
}
