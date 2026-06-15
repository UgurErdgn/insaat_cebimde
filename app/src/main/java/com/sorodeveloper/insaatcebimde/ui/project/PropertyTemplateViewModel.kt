package com.sorodeveloper.insaatcebimde.ui.project

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorodeveloper.insaatcebimde.domain.model.PropertyTemplate
import com.sorodeveloper.insaatcebimde.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PropertyTemplateViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _templates = mutableStateOf<List<PropertyTemplate>>(emptyList())
    val templates: State<List<PropertyTemplate>> = _templates

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _isSaving = mutableStateOf(false)
    val isSaving: State<Boolean> = _isSaving

    fun loadTemplates(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getPropertyTemplates(projectId).onSuccess {
                _templates.value = it
            }
            _isLoading.value = false
        }
    }

    fun saveTemplate(
        templateId: String? = null,
        projectId: String,
        name: String,
        nodeType: String,
        jobTemplateIds: List<String>,
        onSuccess: () -> Unit
    ) {
        val finalName = name.trim()
        if (finalName.isBlank()) return

        viewModelScope.launch {
            _isSaving.value = true
            val template = PropertyTemplate(
                id = templateId ?: "",
                projectId = projectId,
                name = finalName,
                nodeType = nodeType,
                jobTemplateIds = jobTemplateIds
            )

            val result = if (templateId.isNullOrEmpty()) {
                repository.createPropertyTemplate(template)
            } else {
                repository.updatePropertyTemplate(template)
            }

            result.onSuccess {
                loadTemplates(projectId)
                onSuccess()
            }
            _isSaving.value = false
        }
    }
}
