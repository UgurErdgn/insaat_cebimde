package com.sorodeveloper.insaatcebimde.domain.model

/**
 * Granüler Yetki Sabitleri (Granular Permissions)
 *
 * Sistem "sabit roller" yerine granüler yetki listeleri kullanır.
 * Her kullanıcının bir mülkteki yetkisi, bu enum değerlerinin bir alt kümesidir.
 * Hiyerarşi, kümelerin alt küme (subset) mantığıyla doğal olarak oluşur.
 *
 * Örnek: Şef = [VIEW_JOBS, EDIT_JOBS, UPDATE_PROGRESS, INVITE, MANAGE_MEMBERS]
 *        Usta = [VIEW_JOBS, UPDATE_PROGRESS]
 *        Usta'nın yetkileri Şef'in alt kümesidir → Şef, Usta'yı yönetebilir.
 */
enum class Permission(val key: String, val displayName: String) {
    VIEW_JOBS("VIEW_JOBS", "İşleri Görüntüle"),
    EDIT_JOBS("EDIT_JOBS", "İşleri Düzenle"),
    UPDATE_PROGRESS("UPDATE_PROGRESS", "İlerleme Güncelle"),
    VIEW_NOTES("VIEW_NOTES", "Notları Görüntüle"),
    EDIT_NOTES("EDIT_NOTES", "Not Ekle/Düzenle"),
    VIEW_FINANCIALS("VIEW_FINANCIALS", "Maliyetleri Görüntüle"),
    INVITE("INVITE", "Davet Gönder"),
    MANAGE_MEMBERS("MANAGE_MEMBERS", "Üyeleri Yönet");

    companion object {
        /** Firestore'dan gelen String listesini Permission Set'ine çevirir */
        fun fromKeys(keys: List<String>): Set<Permission> {
            return keys.mapNotNull { key -> entries.find { it.key == key } }.toSet()
        }

        /** Permission Set'ini Firestore'a yazılacak String listesine çevirir */
        fun toKeys(permissions: Set<Permission>): List<String> {
            return permissions.map { it.key }
        }

        /** Kullanıcıya "Roller" şablonu olarak sunulacak hazır yetki setleri */
        val OWNER_PRESET: Set<Permission> = entries.toSet()
        val FOREMAN_PRESET: Set<Permission> = setOf(
            VIEW_JOBS, EDIT_JOBS, UPDATE_PROGRESS,
            VIEW_NOTES, EDIT_NOTES, INVITE, MANAGE_MEMBERS
        )
        val WORKER_PRESET: Set<Permission> = setOf(VIEW_JOBS, UPDATE_PROGRESS)
    }
}
