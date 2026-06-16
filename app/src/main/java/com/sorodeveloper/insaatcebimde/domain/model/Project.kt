package com.sorodeveloper.insaatcebimde.domain.model

/**
 * İnşaat Projesi (Saha) modeli.
 *
 * members Map'i bu dökümanın içinde tutulur:
 * - Kullanıcı mülke erişim yetkisine sahip mi? → members[uid] != null
 * - Kullanıcının yetkileri neler? → members[uid].permissions
 * - Firestore Rules tarafında 0 maliyetle kontrol edilir
 */
data class Project(
    val id: String = "",
    val name: String = "",
    val city: String = "",
    val district: String = "",
    val neighborhood: String = "",
    val fullAddress: String = "",
    val contractorName: String = "",
    val contractorEmail: String = "",
    val contractorPhone: String = "",
    val ownerId: String = "", // Projeyi oluşturan kişinin (müteahhit) UID'si
    val nodeTypes: List<String> = listOf("Blok", "Kat", "Daire"), // Şablon mülk türleri
    val members: Map<String, MemberInfo> = emptyMap(), // UID → MemberInfo
    val createdAt: Long = System.currentTimeMillis()
)
