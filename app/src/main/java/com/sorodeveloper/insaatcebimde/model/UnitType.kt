package com.sorodeveloper.insaatcebimde.model

/**
 * Daire/Mülk Tiplerini (Şablonun Şablonu) temsil eden model.
 * @param id Benzersiz barkod (UUID)
 * @param name Daire Tipi Adı (Örn: A Tipi 3+1)
 * @param jobIds Bu daire tipine bağlı olan İş Şablonu Barkodları (Barcode -> True)
 */
data class UnitType(
    val id: String = "",
    val name: String = "",
    val jobIds: Map<String, Boolean> = emptyMap()
)
