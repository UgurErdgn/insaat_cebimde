package com.sorodeveloper.insaatcebimde.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorodeveloper.insaatcebimde.domain.repository.JobRepository
import com.sorodeveloper.insaatcebimde.model.SpinnerItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * JobViewModel: İş ekleme (Job Entry) ekranının tüm durumunu ve mantığını yönetir.
 * 
 * Neden Yazıldı?: UI durumunu (State) korumak, kullanıcı etkileşimlerini işlemek ve 
 * Repository üzerinden veri akışını yönetmek için yazıldı.
 * 
 * Neyi Etkiliyor?: ProfessionalJobAddingFragment'ın tüm içeriğini ve davranışını kontrol eder.
 * 
 * Nasıl Yazıldı?: MVVM patternine uygun olarak @HiltViewModel ile tanımlandı. 
 * Repository bağımlılığı constructor injection ile sağlandı. StateFlow ile UI'a veri akışı yapıldı.
 * 
 * Amacı Ne Değil?: Doğrudan Firebase ile konuşmaz. Resim sıkıştırmaz. Sadece Repository'den gelen sonuçları UI State'e yansıtır.
 */
@HiltViewModel
class JobViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobEntryUiState())
    val uiState: StateFlow<JobEntryUiState> = _uiState.asStateFlow()

    fun setInitialData(insaatId: String, mulkTuru: String) {
        _uiState.update { it.copy(insaatId = insaatId, mulkTuru = mulkTuru) }
    }

    fun updateJobName(name: String) {
        _uiState.update { 
            if (it.jobName == name) it else it.copy(jobName = name) 
        }
    }

    fun fetchSpinnerItems(targetCategory: String? = null, targetType: String? = null) {
        val state = _uiState.value
        if (state.insaatId.isBlank()) return

        _uiState.update { it.copy(isFetchingSpinnerItems = true) }

        viewModelScope.launch {
            repository.getJobTemplates(state.insaatId, state.jobName).onSuccess { list ->
                _uiState.update { current ->
                    val updatedCards = current.jobCards.toMutableList()
                    
                    if (targetCategory != null && targetType != null) {
                        val targetItem = list.find { it.kalemAdi == targetCategory && it.turAdi == targetType }
                        if (targetItem != null) {
                            val newCard = ProfessionalJobState(
                                selectedItem = targetItem, 
                                isLoading = true,
                                isEditMode = true,
                                isModeLocked = true,
                                customJobName = targetItem.kalemAdi,
                                customJobType = targetItem.turAdi
                            )
                            fetchJobDetails(newCard.id, targetItem.templateId)
                            updatedCards.add(0, newCard)
                        }
                    } else {
                        updatedCards.forEachIndexed { index, card ->
                            if (card.selectedItem == null && list.isNotEmpty()) {
                                fetchJobDetails(card.id, list[0].templateId)
                                updatedCards[index] = card.copy(selectedItem = list[0], isLoading = true)
                            }
                        }
                    }

                    current.copy(
                        availableSpinnerItems = list,
                        jobCards = updatedCards,
                        isFetchingSpinnerItems = false
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isFetchingSpinnerItems = false) }
            }
        }
    }

    fun addNewJobCard(isCustom: Boolean = false) {
        _uiState.update { state ->
            val firstItem = if (state.availableSpinnerItems.isNotEmpty()) state.availableSpinnerItems[0] else null
            
            val newCard = if (isCustom) {
                ProfessionalJobState(
                    id = UUID.randomUUID().toString(), 
                    isCustom = true,
                    materials = listOf(ProfessionalMaterial(isReadOnly = false))
                )
            } else {
                ProfessionalJobState(selectedItem = firstItem, isLoading = firstItem != null)
            }
            
            if (!isCustom && firstItem != null) {
                fetchJobDetails(newCard.id, firstItem.templateId)
            }
            
            state.copy(jobCards = listOf(newCard) + state.jobCards)
        }
    }

    fun updateCardSelection(cardId: String, selected: SpinnerItem, insaatId: String, mulkTuru: String) {
        _uiState.update { state ->
            val updatedCards = state.jobCards.map { card ->
                if (card.id == cardId) {
                    if (card.selectedItem == selected && !card.isLoading) return@map card
                    fetchJobDetails(cardId, selected.templateId)
                    card.copy(selectedItem = selected, isLoading = true)
                } else card
            }
            state.copy(jobCards = updatedCards)
        }
    }

    private fun fetchJobDetails(cardId: String, templateId: String) {
        val insaatId = _uiState.value.insaatId
        if (insaatId.isBlank()) return

        viewModelScope.launch {
            repository.getJobDetails(insaatId, templateId).onSuccess { details ->
                val remoteImages = details["remoteImages"] as? List<String> ?: emptyList()
                val materialsMap = details["materials"] as? Map<String, String> ?: emptyMap()
                
                val materials = materialsMap.map { (name, amount) ->
                    ProfessionalMaterial(name = name, amount = amount, isReadOnly = true)
                }

                updateCardState(cardId) { card ->
                    val materialsEditable = card.isEditMode || card.isCustom
                    card.copy(
                        remoteImages = remoteImages,
                        materials = materials.map { it.copy(isReadOnly = !materialsEditable) },
                        isLoading = false
                    )
                }
            }.onFailure {
                updateCardState(cardId) { it.copy(isLoading = false) }
            }
        }
    }

    fun saveWholeJob(contentResolver: Any, fPath: String, newTemplateName: String? = null) {
        val state = _uiState.value
        if (state.jobName.isBlank()) {
            _uiState.update { it.copy(saveStatus = SaveStatus.Error("İş adı boş olamaz")) }
            return
        }

        _uiState.update { it.copy(saveStatus = SaveStatus.Loading) }

        viewModelScope.launch {
            repository.saveJob(
                insaatId = state.insaatId,
                jobName = state.jobName,
                jobCards = state.jobCards,
                fPath = fPath,
                newTemplateName = newTemplateName
            ).onSuccess {
                _uiState.update { it.copy(saveStatus = SaveStatus.Success) }
            }.onFailure { e ->
                _uiState.update { it.copy(saveStatus = SaveStatus.Error(e.message ?: "Hata oluştu")) }
            }
        }
    }

    fun addLocalImage(cardId: String, uri: Uri) {
        updateCardState(cardId) { 
            if (it.localImages.contains(uri)) it 
            else it.copy(localImages = listOf(uri) + it.localImages) 
        }
    }

    fun removeLocalImage(cardId: String, uri: Uri) {
        updateCardState(cardId) { 
            val filtered = it.localImages.filter { u -> u != uri }
            it.copy(localImages = filtered) 
        }
    }

    fun removeImage(cardId: String, item: Any) {
        if (item is Uri) {
            removeLocalImage(cardId, item)
        } else if (item is String) {
            updateCardState(cardId) { 
                it.copy(remoteImages = it.remoteImages.filter { u -> u != item }) 
            }
        }
    }

    fun addMaterial(cardId: String) {
        updateCardState(cardId) { it.copy(materials = listOf(ProfessionalMaterial()) + it.materials) }
    }

    fun removeMaterial(cardId: String, materialId: String) {
        updateCardState(cardId) { 
            it.copy(materials = it.materials.filter { m -> m.id != materialId }) 
        }
    }

    fun updateMaterial(cardId: String, updatedMaterial: ProfessionalMaterial) {
        updateCardState(cardId) { card ->
            val updatedMaterials = card.materials.map { 
                if (it.id == updatedMaterial.id) updatedMaterial else it 
            }
            card.copy(materials = updatedMaterials)
        }
    }

    fun toggleCustomJob(cardId: String, isCustom: Boolean) {
        updateCardState(cardId) { card ->
            if (card.isModeLocked) return@updateCardState card
            if (!isCustom) {
                card.selectedItem?.let {
                    fetchJobDetails(cardId, it.templateId)
                    card.copy(isCustom = false, isEditMode = false, isLoading = true)
                } ?: card.copy(isCustom = false, isEditMode = false)
            } else {
                card.copy(
                    isCustom = true,
                    isEditMode = false,
                    localImages = emptyList(),
                    remoteImages = emptyList(),
                    materials = listOf(ProfessionalMaterial(isReadOnly = false))
                )
            }
        }
    }

    fun toggleEditTemplate(cardId: String, isEdit: Boolean) {
        updateCardState(cardId) { card ->
            if (card.isModeLocked) return@updateCardState card
            if (isEdit) {
                card.copy(
                    isEditMode = true,
                    isCustom = false,
                    customJobName = card.selectedItem?.kalemAdi ?: "",
                    customJobType = card.selectedItem?.turAdi ?: "",
                    materials = card.materials.map { it.copy(isReadOnly = false) }
                )
            } else {
                card.selectedItem?.let {
                    fetchJobDetails(cardId, it.templateId)
                    card.copy(isEditMode = false, isLoading = true, localImages = emptyList())
                } ?: card.copy(isEditMode = false, localImages = emptyList())
            }
        }
    }

    fun updateCustomJobName(cardId: String, name: String) {
        updateCardState(cardId) { 
            if ((it.isCustom || it.isEditMode)) it.copy(customJobName = name) else it
        }
    }

    fun updateCustomJobType(cardId: String, type: String) {
        updateCardState(cardId) { 
            if ((it.isCustom || it.isEditMode)) it.copy(customJobType = type) else it
        }
    }

    fun removeJobCard(cardId: String) {
        _uiState.update { state ->
            state.copy(jobCards = state.jobCards.filter { it.id != cardId })
        }
    }

    private fun updateCardState(cardId: String, block: (ProfessionalJobState) -> ProfessionalJobState) {
        _uiState.update { state ->
            val updatedCards = state.jobCards.map { if (it.id == cardId) block(it) else it }
            state.copy(jobCards = updatedCards)
        }
    }
}
