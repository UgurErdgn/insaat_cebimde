package com.sorodeveloper.insaatcebimde.domain.model

/**
 * Bir mülk (proje) içindeki üyenin bilgilerini tutar.
 * Firestore'da `projects/{projectId}` dökümanı içinde `members` Map'inde saklanır.
 *
 * Firestore Yapısı:
 * ```
 * members: {
 *   "uid_ali_usta": {
 *     "displayName": "Ali Yılmaz",
 *     "roleName": "Usta",
 *     "permissions": ["VIEW_JOBS", "UPDATE_PROGRESS"],
 *     "scopes": { "nodes": ["node_etap1"], "categories": ["Boya"] },
 *     "isOwner": false
 *   }
 * }
 * ```
 */
data class MemberInfo(
    val uid: String = "",
    val displayName: String = "",
    val roleName: String = "", // UI'da gösterilecek etiket (Şef, Usta, Kalfa vb.)
    val permissions: List<String> = emptyList(),
    val scopes: MemberScopes = MemberScopes(),
    val isOwner: Boolean = false
) {
    /** Firestore'dan gelen String listesini Permission Set'ine çevirir */
    fun permissionSet(): Set<Permission> = Permission.fromKeys(permissions)

    /** Bu üyenin belirli bir yetkisi var mı? */
    fun hasPermission(permission: Permission): Boolean = permission.key in permissions

    /** A kullanıcısı B kullanıcısını yönetebilir mi? (Subset kontrolü) */
    fun canManage(other: MemberInfo): Boolean {
        if (isOwner) return true // Owner her şeyi yönetebilir
        if (other.isOwner) return false // Kimse Owner'ı yönetemez
        // B'nin tüm yetkileri A'nın alt kümesi mi?
        return other.permissionSet().all { it in permissionSet() }
    }

    /**
     * A'nın B'ye verebileceği yetkilerin listesi.
     * A sadece kendi sahip olduğu yetkileri verebilir.
     */
    fun grantablePermissions(): Set<Permission> = permissionSet()
}

/**
 * Üyenin görebileceği düğüm (Node) ve kategori kısıtlamaları.
 * Boş liste = Tüm düğümler/kategoriler görülebilir (kısıtlama yok).
 */
data class MemberScopes(
    val nodes: List<String> = emptyList(),     // Boş = tüm düğümler
    val categories: List<String> = emptyList() // Boş = tüm kategoriler
) {
    /** Kapsam kısıtlaması var mı? */
    fun hasNodeRestriction(): Boolean = nodes.isNotEmpty()
    fun hasCategoryRestriction(): Boolean = categories.isNotEmpty()
}
