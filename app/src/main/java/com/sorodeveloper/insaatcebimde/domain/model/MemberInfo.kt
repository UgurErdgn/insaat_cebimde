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
    val delegablePermissions: List<String> = emptyList(),
    val scopes: MemberScopes = MemberScopes(),
    @get:PropertyName("owner")
    @set:PropertyName("owner")
    var isOwner: Boolean = false
) {
    /** Firestore'dan gelen String listesini Permission Set'ine çevirir. Owner ise tüm yetkileri döner. */
    fun permissionSet(): Set<Permission> {
        return if (isOwner) Permission.entries.toSet() else Permission.fromKeys(permissions)
    }

    /** Firestore'dan gelen Devredilebilir String listesini Permission Set'ine çevirir. Owner ise tüm yetkileri döner. */
    fun delegablePermissionSet(): Set<Permission> {
        return if (isOwner) Permission.entries.toSet() else Permission.fromKeys(delegablePermissions)
    }

    /** Bu üyenin belirli bir yetkisi var mı? */
    fun hasPermission(permission: Permission): Boolean = permission.key in permissions

    /** A kullanıcısı B kullanıcısını yönetebilir mi? (Subset kontrolü) */
    fun canManage(other: MemberInfo): Boolean {
        if (isOwner) return true // Owner her şeyi yönetebilir
        if (other.isOwner) return false // Kimse Owner'ı yönetemez
        // B'nin tüm yetkileri A'nın DEVREDEBİLECEĞİ yetkilerin alt kümesi mi?
        return other.permissionSet().all { it in delegablePermissionSet() }
    }

    /**
     * A'nın B'ye verebileceği yetkilerin listesi.
     * Owner ise tüm yetkiler. Değilse, sadece devretmesine izin verilen yetkiler.
     */
    fun grantablePermissions(): Set<Permission> {
        return if (isOwner) Permission.entries.toSet() else delegablePermissionSet()
    }
}



/**
 * Üyenin görebileceği düğüm (Node) ve kategori kısıtlamaları.
 * Boş liste = Tüm düğümler/kategoriler görülebilir (kısıtlama yok).
 */
data class MemberScopes(
    // Eğer true ise, kullanıcının erişimi kısıtlıdır ve SADECE nodeCategories içindeki kurallar geçerlidir.
    // Eğer false ise, projedeki tüm düğüm ve kategorilere erişebilir (kısıtlama yoktur).
    @get:PropertyName("isRestricted")
    @set:PropertyName("isRestricted")
    var restricted: Boolean = false,
    
    // Key: Node ID, Value: O Node'da izin verilen kategorilerin listesi.
    // Eğer Value (Liste) BOŞ ise, o Node içindeki TÜM kategorilere erişebilir.
    // Örnek 1: {"node_15": ["Elektrik"]} -> 15'te sadece Elektrik
    // Örnek 2: {"A_Blok": []} -> A Blok'taki her şeye tam erişim
    var nodeCategories: Map<String, List<String>> = emptyMap()
) {
    fun isNodeSelectable(nodeId: String, ancestors: List<String>): Boolean {
        if (!restricted) return true
        if (nodeCategories.containsKey(nodeId)) return true
        for (ancestorId in ancestors) {
            if (nodeCategories.containsKey(ancestorId)) return true
        }
        return false
    }

    fun isNodeVisible(nodeId: String, ancestors: List<String>, allNodes: List<ProjectNode>): Boolean {
        if (!restricted) return true
        if (isNodeSelectable(nodeId, ancestors)) return true
        
        // Atası olduğu child'lar kullanıcıya açıksa visible yap
        return nodeCategories.keys.any { allowedId ->
            val allowedNode = allNodes.find { it.id == allowedId }
            allowedNode?.ancestors?.contains(nodeId) == true
        }
    }

    fun hasAccessToJob(jobCategory: String, nodeId: String, ancestors: List<String>): Boolean {
        if (!restricted) return true
        if (!isNodeSelectable(nodeId, ancestors)) return false
        
        // 1. Önce Node'un kendi üzerindeki kuralı kontrol et
        if (nodeCategories.containsKey(nodeId)) {
            val cats = nodeCategories[nodeId]!!
            if (cats.isEmpty() || cats.contains(jobCategory)) return true
        }
        
        // 2. Kendi üzerinde yoksa veya o kuralda bu iş yoksa, atalarından (üst mülklerden) miras alınan yetkilere bak
        // Atalarından herhangi birisi "Tüm İşler" (boş liste) veya bu spesifik iş için yetki verdiyse erişim sağlar.
        for (ancestorId in ancestors) {
            if (nodeCategories.containsKey(ancestorId)) {
                val cats = nodeCategories[ancestorId]!!
                if (cats.isEmpty() || cats.contains(jobCategory)) return true
            }
        }
        
        return false
    }

    /**
     * Kullanıcının bu mülkte (doğrudan veya miras yoluyla) HANGİ kategorilere yetkili olduğunu döndürür.
     * Bu fonksiyon, makro (genel) ilerleme hesaplanırken kullanılır.
     * @return null -> Tüm işlere (kategorilere) yetkili.
     *         emptySet() -> Bu mülke yetkisi yok (sadece child'larına var, o yüzden genel ilerleme GİZLENMELİ).
     *         Set<String> -> Sadece bu spesifik kategorilere yetkili.
     */
    fun getAllowedCategoriesForNode(nodeId: String, ancestors: List<String>): Set<String>? {
        if (!restricted) return null // Kısıtlama yok, tam yetki
        if (!isNodeSelectable(nodeId, ancestors)) return emptySet() // Bu mülkte yetkisi yok

        val allowedCats = mutableSetOf<String>()
        var hasUnlimitedAccess = false

        // Hem kendi üzerindeki hem de atalarından gelen yetkileri topla (Additive)
        val allNodesToCheck = ancestors + nodeId
        for (id in allNodesToCheck) {
            if (nodeCategories.containsKey(id)) {
                val cats = nodeCategories[id]!!
                if (cats.isEmpty()) {
                    hasUnlimitedAccess = true
                } else {
                    allowedCats.addAll(cats)
                }
            }
        }

        if (hasUnlimitedAccess) return null // Herhangi bir seviyeden "Tüm İşler" yetkisi geldiyse
        return allowedCats
    }
}
