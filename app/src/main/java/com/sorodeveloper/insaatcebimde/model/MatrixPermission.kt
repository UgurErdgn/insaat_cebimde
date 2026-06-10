package com.sorodeveloper.insaatcebimde.model

data class MatrixPermission(
    val locationPath: String = "", // e.g., "SahaA", "SahaA/Etap1", "SahaA/Etap1/Blok1"
    val jobPath: String = "", // e.g., "Elektrik", "Mobilya", "Mobilya/Kapi"
    val canDelegate: Boolean = false
)
