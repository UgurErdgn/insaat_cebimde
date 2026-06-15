package com.sorodeveloper.insaatcebimde.ui.project

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorodeveloper.insaatcebimde.domain.model.JobMaterial
import com.sorodeveloper.insaatcebimde.domain.model.JobTemplate
import com.sorodeveloper.insaatcebimde.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JobTemplateViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _templates = mutableStateOf<List<JobTemplate>>(emptyList())
    val templates: State<List<JobTemplate>> = _templates

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _isSaving = mutableStateOf(false)
    val isSaving: State<Boolean> = _isSaving

    fun loadTemplates(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getJobTemplates(projectId).onSuccess {
                _templates.value = it
            }
            _isLoading.value = false
        }
    }

    fun saveTemplate(
        templateId: String? = null,
        projectId: String,
        category: String,
        name: String,
        type: String,
        materials: List<JobMaterial>,
        images: List<String>,
        onSuccess: () -> Unit
    ) {
        val finalCategory = category.trim()
        val finalName = name.trim()
        val finalType = type.trim()

        if (finalCategory.isBlank() || finalName.isBlank()) return

        val finalMaterials = materials
            .filter { it.name.trim().isNotBlank() }
            .map { it.copy(name = it.name.trim(), quantity = it.quantity.trim()) }

        viewModelScope.launch {
            _isSaving.value = true
            
            val urlsToKeep = images.filter { it.startsWith("http") }
            val urisToUpload = images.filter { !it.startsWith("http") }
            
            var newUploadedUrls = emptyList<String>()
            
            // Eğer yeni seçili resimler varsa, önce Firebase Storage'a yükle
            if (urisToUpload.isNotEmpty()) {
                val uploadResult = repository.uploadImages(urisToUpload, projectId)
                if (uploadResult.isSuccess) {
                    newUploadedUrls = uploadResult.getOrNull() ?: emptyList()
                } else {
                    // Hata yönetimi
                }
            }

            val finalImages = urlsToKeep + newUploadedUrls

            val template = JobTemplate(
                id = templateId ?: "",
                projectId = projectId,
                category = finalCategory,
                name = finalName,
                type = finalType,
                materials = finalMaterials,
                images = finalImages
            )
            
            val result = if (templateId.isNullOrEmpty()) {
                repository.createJobTemplate(template)
            } else {
                repository.updateJobTemplate(template)
            }
            
            result.onSuccess {
                // Kayıt başarılı, listeyi yenile
                loadTemplates(projectId)
                onSuccess()
            }
            _isSaving.value = false
        }
    }
}
