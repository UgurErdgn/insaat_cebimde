package com.sorodeveloper.insaatcebimde.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val publicInviteId: String = "", // Used for invitation discovery
    // Key is projectId, Value is the list of matrix permissions for that specific project
    val projectPermissions: Map<String, List<MatrixPermission>> = emptyMap()
)
