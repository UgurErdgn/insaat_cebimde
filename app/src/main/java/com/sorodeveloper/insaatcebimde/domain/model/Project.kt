package com.sorodeveloper.insaatcebimde.domain.model

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
    val createdAt: Long = System.currentTimeMillis()
)
