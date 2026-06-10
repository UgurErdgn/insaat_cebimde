package com.sorodeveloper.insaatcebimde.model

enum class ProjectNodeType {
    PROJECT,    // Saha/Proje (En üst)
    PHASE,      // Etap
    BLOCK,      // Blok
    FLAT        // Daire/Oda (En alt lokasyon)
}

data class ProjectNode(
    val id: String = "",
    val projectId: String = "", // Tying this node to a specific Project (Saha)
    val name: String = "",
    val type: ProjectNodeType = ProjectNodeType.PROJECT,
    val parentId: String? = null,
    val path: String = "", // Full path e.g. "projectId/phaseId/blockId"
    var progress: Double = 0.0, // Computed automatically from below
    val childrenIds: List<String> = emptyList() // List of child node IDs for fast querying
)
