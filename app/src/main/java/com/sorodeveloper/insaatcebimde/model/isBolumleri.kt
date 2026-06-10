package com.sorodeveloper.insaatcebimde

import android.net.Uri
import com.sorodeveloper.insaatcebimde.model.SpinnerItem

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

data class KalemCardModel(
    var selectedKalem: SpinnerItem? = null,
    var localCizimler: MutableList<Uri> = mutableListOf(),  // Sol RecyclerView
    var uploadedCizimler: MutableList<String> = mutableListOf(), // Sağ RecyclerView
    var malzemeler: MutableList<MaterialItem> = mutableListOf(),
    var progress: String = "0"
)

data class MaterialItem(
    var name: String = "",
    var quantity: String = ""
)

