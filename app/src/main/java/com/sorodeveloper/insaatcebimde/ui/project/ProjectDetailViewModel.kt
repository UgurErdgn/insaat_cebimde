package com.sorodeveloper.insaatcebimde.ui.project

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorodeveloper.insaatcebimde.domain.model.Project
import com.sorodeveloper.insaatcebimde.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: String = checkNotNull(savedStateHandle["projectId"])

    private val _project = mutableStateOf<Project?>(null)
    val project: State<Project?> = _project

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    init {
        loadProjectDetails()
    }

    private fun loadProjectDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = projectRepository.getProjectById(projectId)
            result.onSuccess {
                _project.value = it
            }
            _isLoading.value = false
        }
    }

    fun addNodeType(newType: String) {
        val currentProject = _project.value ?: return
        val trimmed = newType.trim()
        if (trimmed.isBlank() || currentProject.nodeTypes.contains(trimmed)) return
        
        val updatedTypes = currentProject.nodeTypes + trimmed
        val updatedProject = currentProject.copy(nodeTypes = updatedTypes)
        _project.value = updatedProject
        
        viewModelScope.launch {
            projectRepository.updateProject(updatedProject)
        }
    }
}
