package com.sorodeveloper.insaatcebimde.domain.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val publicInviteId: String = "",
    val projectPermissions: Map<String, ProjectRole> = emptyMap()
)
