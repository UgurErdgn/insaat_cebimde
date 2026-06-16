package com.sorodeveloper.insaatcebimde.domain.model

import com.google.firebase.firestore.PropertyName

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
    // Eğer true ise, kullanıcının erişimi kısıtlıdır ve SADECE nodeCategories içindeki kurallar geçerlidir.
    // Eğer false ise, projedeki tüm düğüm ve kategorilere erişebilir (kısıtlama yoktur).
    @get:PropertyName("restricted")
    @set:PropertyName("restricted")
    var isRestricted: Boolean = false,
    
    // Key: Node ID, Value: O Node'da izin verilen kategorilerin listesi.
    // Eğer Value (Liste) BOŞ ise, o Node içindeki TÜM kategorilere erişebilir.
    // Örnek 1: {"node_15": ["Elektrik"]} -> 15'te sadece Elektrik
    // Örnek 2: {"A_Blok": []} -> A Blok'taki her şeye tam erişim
    var nodeCategories: Map<String, List<String>> = emptyMap()
) {
    fun hasCategoryRestriction(nodeId: String, ancestors: List<String>): Boolean {
        if (!isRestricted) return false
        
        // Önce kendi Node'unda bir kısıtlama var mı ona bakalım
        val categories = nodeCategories[nodeId]
        if (categories != null) {
            return categories.isNotEmpty() // Eğer liste doluysa kısıtlama var demektir
        }
        
        // Kendi Node'unda yoksa atalarından (Ancestors) gelen bir kısıtlama var mı?
        // En yakından (en son eklenen ata) en uzağa doğru kontrol etmek mantıklı olabilir
        for (ancestorId in ancestors.reversed()) {
            val ancestorCategories = nodeCategories[ancestorId]
            if (ancestorCategories != null) {
                return ancestorCategories.isNotEmpty()
            }
        }
        
        return false // Eğer kendisine veya atasına hiçbir yetki atanmamışsa zaten göremeyecek, o yüzden kısıtlama durumu belirsiz (varsayılan false)
    }

    fun getAllowedCategories(nodeId: String, ancestors: List<String>): List<String> {
        if (!isRestricted) return emptyList()
        
        val categories = nodeCategories[nodeId]
        if (categories != null) return categories
        
        for (ancestorId in ancestors.reversed()) {
            val ancestorCategories = nodeCategories[ancestorId]
            if (ancestorCategories != null) return ancestorCategories
        }
        
        return emptyList()
    }

    fun isNodeSelectable(nodeId: String, ancestors: List<String>): Boolean {
        if (!isRestricted) return true
        if (nodeCategories.containsKey(nodeId)) return true
        for (ancestorId in ancestors) {
            if (nodeCategories.containsKey(ancestorId)) return true
        }
        return false
    }

    fun isNodeVisible(nodeId: String, ancestors: List<String>, allNodes: List<ProjectNode>): Boolean {
        if (!isRestricted) return true
        if (isNodeSelectable(nodeId, ancestors)) return true
        
        // Atası olduğu child'lar kullanıcıya açıksa visible yap
        return nodeCategories.keys.any { allowedId ->
            val allowedNode = allNodes.find { it.id == allowedId }
            allowedNode?.ancestors?.contains(nodeId) == true
        }
    }

    fun hasAccessToJob(jobCategory: String, nodeId: String, ancestors: List<String>): Boolean {
        if (!isRestricted) return true
        if (!isNodeSelectable(nodeId, ancestors)) return false
        
        val allowedCats = getAllowedCategories(nodeId, ancestors)
        if (allowedCats.isEmpty()) return true // Kısıtlama listesi boşsa tüm kategorilere erişebilir
        
        return allowedCats.contains(jobCategory)
    }
}
