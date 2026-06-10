package com.sorodeveloper.insaatcebimde.viewmodel

import android.net.Uri
import com.sorodeveloper.insaatcebimde.model.SpinnerItem

/**
 * Bir iş kaleminin (Job Card) UI durumunu temsil eden immutable veri sınıfı.
 */
data class ProfessionalJobState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val selectedItem: SpinnerItem? = null,
    val materials: List<ProfessionalMaterial> = emptyList(),
    val localImages: List<Uri> = emptyList(),
    val remoteImages: List<String> = emptyList(),
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val isCustom: Boolean = false,
    val isEditMode: Boolean = false,
    val isModeLocked: Boolean = false,
    val customJobName: String = "",
    val customJobType: String = ""
)

data class ProfessionalMaterial(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val amount: String = "",
    val isReadOnly: Boolean = false
)

/**
 * Sayfanın genel durumunu temsil eder.
 */
data class JobEntryUiState(
    val insaatId: String = "",
    val mulkTuru: String = "",
    val jobName: String = "",
    val availableSpinnerItems: List<SpinnerItem> = emptyList(),
    val jobCards: List<ProfessionalJobState> = emptyList(),
    val error: String? = null,
    val isFetchingSpinnerItems: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.Idle
)

sealed class SaveStatus {
    object Idle : SaveStatus()
    object Loading : SaveStatus()
    object Success : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}
