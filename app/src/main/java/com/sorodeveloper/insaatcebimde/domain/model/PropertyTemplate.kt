package com.sorodeveloper.insaatcebimde.domain.model

import com.google.firebase.firestore.PropertyName

data class PropertyTemplate(
    val id: String = "",
    val projectId: String = "",
    val name: String = "", // Örn: "3+1 A Tipi Daire", "A Blok Zemin Kat"
    val nodeType: String = "", // Hangi tür için olduğu: "Daire", "Blok", vs.
    val jobTemplateIds: List<String> = emptyList(), // Bu mülk tipine ait olan iş şablonlarının ID'leri
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
