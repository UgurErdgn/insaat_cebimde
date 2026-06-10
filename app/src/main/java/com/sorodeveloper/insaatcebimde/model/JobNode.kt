package com.sorodeveloper.insaatcebimde.model

enum class JobNodeType {
    CATEGORY,   // Ana İş (Örn: Mobilya, Elektrik, Tesisat)
    ITEM        // Alt Kalem (Örn: Vestiyer, Mutfak Dolabı)
}

data class JobNode(
    val id: String = "",
    val projectId: String = "", // Tied to the main project
    val locationId: String = "", // The ProjectNode ID this job belongs to (Usually a Flat ID, but could be Block ID)
    val name: String = "",
    val type: JobNodeType = JobNodeType.CATEGORY,
    val parentId: String? = null, // Null if CATEGORY, populated if ITEM
    val jobPath: String = "", // e.g. "Mobilya/Kapi"
    var progress: Double = 0.0, // 0-100. Manual for ITEMs, computed for CATEGORYs
    val childrenIds: List<String> = emptyList() // For categories to hold items
)
