package com.sorodeveloper.insaatcebimde

sealed class isBolumleri {
    data class Baslik(
        val ad: String,
        val altlar: List<String>,
        var acikMi: Boolean = false
    ) : isBolumleri()

    data class Kalem(
        val ad: String,
        val bolumAdi: String
    ) : isBolumleri()
}
