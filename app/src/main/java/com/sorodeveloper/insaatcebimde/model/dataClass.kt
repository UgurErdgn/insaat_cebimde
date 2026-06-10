package com.sorodeveloper.insaatcebimde.model

data class KalemTurItem(
    val kalemAdi: String = "",
    val turAdi: String = "",
    val cizimler: List<String> = emptyList(),
    val malzemeler: Map<String, String> = emptyMap(),
    val progress: String = "0"
)

data class SpinnerItem(
    val kalemAdi: String,
    val turAdi: String,
    val templateId: String = ""
) {
    override fun toString(): String {
        return "$kalemAdi - $turAdi"
    }
}

