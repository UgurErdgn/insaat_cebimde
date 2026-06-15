package com.sorodeveloper.insaatcebimde.domain.model

import com.google.firebase.firestore.PropertyName

data class NodeJob(
    val id: String = "",
    val nodeId: String = "",
    val jobTemplateId: String = "",
    val category: String = "",   // Denormalize: JobTemplate.category
    val name: String = "",       // Denormalize: JobTemplate.name
    val progress: Int = 0,       // 0, 25, 50, 75, 100
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false // Şablondan çıkarılırsa gizlemek için
)
