package com.sorodeveloper.insaatcebimde.domain.model

import com.google.firebase.firestore.PropertyName

data class ProjectNode(
    val id: String = "",
    val projectId: String = "",
    val parentId: String? = null,
    val ancestors: List<String> = emptyList(), // Kökten kendisine kadar olan tüm ID'ler
    val name: String = "",
    val type: String = "Grup", // "Saha", "Blok", "Daire" vb.
    val propertyTemplateId: String? = null,
    val localProgress: Int = 0,              // Kendi işlerinin yüzde ortalaması
    val totalDescendantJobs: Int = 0,        // Altındaki tüm torun işlerin sayısı (kümülatif)
    val totalDescendantProgress: Int = 0,    // Altındaki tüm torun işlerin toplam puanı (kümülatif)
    val jobStats: Map<String, JobStat> = emptyMap(), // İş bazında kümülatif istatistikler
    val totalCost: Double = 0.0,
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class JobStat(
    val name: String = "",
    val category: String = "",
    val totalCount: Int = 0,       // Bu işin alttaki kaç mülkte var olduğu
    val totalProgress: Int = 0     // Bu işin alttaki tüm mülklerdeki toplam puanı
)
