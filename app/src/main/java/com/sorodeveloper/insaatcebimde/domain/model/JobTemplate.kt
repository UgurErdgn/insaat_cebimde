package com.sorodeveloper.insaatcebimde.domain.model

import com.google.firebase.firestore.PropertyName

data class JobTemplate(
    val id: String = "",
    val projectId: String = "",
    val category: String = "",
    val name: String = "",
    val type: String = "",
    val images: List<String> = emptyList(), // Şimdilik cihaz içi URI, Firebase Storage'a geçince Download URL olacak
    val materials: List<JobMaterial> = emptyList(),
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class JobMaterial(
    val name: String = "",
    val quantity: String = ""
)
