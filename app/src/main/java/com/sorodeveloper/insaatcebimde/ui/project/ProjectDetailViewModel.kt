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

    private val _availableNodes = mutableStateOf<List<com.sorodeveloper.insaatcebimde.domain.model.ProjectNode>>(emptyList())
    val availableNodes: State<List<com.sorodeveloper.insaatcebimde.domain.model.ProjectNode>> = _availableNodes

    private val _availableCategories = mutableStateOf<List<String>>(emptyList())
    val availableCategories: State<List<String>> = _availableCategories

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
            
            // Scope seçenekleri için tüm düğümleri ve kategorileri getir
            val nodesResult = projectRepository.getAllProjectNodes(projectId)
            nodesResult.onSuccess { nodes ->
                _availableNodes.value = nodes
            }
            
            val categoriesResult = projectRepository.getAllJobCategories(projectId)
            categoriesResult.onSuccess { categories ->
                _availableCategories.value = categories.sorted()
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

    fun updateMember(
        targetUserId: String,
        newScopes: com.sorodeveloper.insaatcebimde.domain.model.MemberScopes,
        newPermissions: Set<com.sorodeveloper.insaatcebimde.domain.model.Permission>,
        newRoleName: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = projectRepository.requestMemberUpdate(
                projectId = projectId,
                targetUserId = targetUserId,
                newPermissions = com.sorodeveloper.insaatcebimde.domain.model.Permission.toKeys(newPermissions),
                newScopes = newScopes,
                newRoleName = newRoleName
            )
            onComplete(result.isSuccess)
        }
    }

    fun removeMember(targetUserId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = projectRepository.removeMember(projectId, targetUserId)
            onComplete(result.isSuccess)
        }
    }
}
