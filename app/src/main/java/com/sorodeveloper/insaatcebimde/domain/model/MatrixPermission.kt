package com.sorodeveloper.insaatcebimde.domain.model

data class MatrixPermission(
    val locationPath: String = "",
    val jobPath: String = "",
    val canDelegate: Boolean = false
)
