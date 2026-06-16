package com.sorodeveloper.insaatcebimde.ui.project

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorodeveloper.insaatcebimde.domain.model.ProjectNode
import com.sorodeveloper.insaatcebimde.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectNodeViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _currentPath = mutableStateOf<List<ProjectNode>>(emptyList())
    val currentPath: State<List<ProjectNode>> = _currentPath

    private val _childNodes = mutableStateOf<List<ProjectNode>>(emptyList())
    val childNodes: State<List<ProjectNode>> = _childNodes

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _deletedNodes = mutableStateOf<List<com.sorodeveloper.insaatcebimde.domain.model.DeletedNodeDetail>>(emptyList())
    val deletedNodes: State<List<com.sorodeveloper.insaatcebimde.domain.model.DeletedNodeDetail>> = _deletedNodes

    private var childNodesJob: kotlinx.coroutines.Job? = null
    private var currentNodeJob: kotlinx.coroutines.Job? = null

    private fun observeCurrentNode(projectId: String, nodeId: String) {
        currentNodeJob?.cancel()
        currentNodeJob = viewModelScope.launch {
            repository.observeProjectNode(projectId, nodeId).collect { node ->
                if (node != null) {
                    val currentList = _currentPath.value.toMutableList()
                    val index = currentList.indexOfFirst { it.id == nodeId }
                    if (index != -1) {
                        currentList[index] = node
                        _currentPath.value = currentList
                    }
                }
            }
        }
    }

    fun loadRootNode(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Kök düğümü (Saha) bul
            repository.getProjectNodes(projectId, null).onSuccess { nodes ->
                val rootNode = nodes.firstOrNull()
                if (rootNode != null) {
                    _currentPath.value = listOf(rootNode)
                    observeCurrentNode(projectId, rootNode.id)
                    loadChildren(projectId, rootNode.id)
                }
            }
            _isLoading.value = false
        }
    }

    fun loadDeletedNodes(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getDeletedNodesWithDetails(projectId).onSuccess { details ->
                _deletedNodes.value = details
            }
            _isLoading.value = false
        }
    }

    fun loadChildren(projectId: String, parentId: String) {
        childNodesJob?.cancel()
        childNodesJob = viewModelScope.launch {
            _isLoading.value = true
            repository.observeProjectNodes(projectId, parentId).collect { nodes ->
                _childNodes.value = nodes
                _isLoading.value = false
            }
        }
    }

    fun navigateToNode(node: ProjectNode) {
        val currentList = _currentPath.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == node.id }
        if (index != -1) {
            // Geriye doğru navigasyon yapıldıysa, o noktadan sonrakileri kes
            _currentPath.value = currentList.subList(0, index + 1)
        } else {
            // İleri gidiliyorsa listeye ekle
            _currentPath.value = currentList + node
        }
        observeCurrentNode(node.projectId, node.id)
        loadChildren(node.projectId, node.id)
    }

    fun addNode(projectId: String, parentId: String, name: String, type: String, onSuccess: () -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val parentNode = _currentPath.value.lastOrNull()
            val ancestors = parentNode?.ancestors ?: emptyList()
            
            val newNode = ProjectNode(
                projectId = projectId,
                parentId = parentId,
                name = name.trim(),
                type = type,
                ancestors = ancestors
            )
            repository.createProjectNode(newNode).onSuccess {
                loadChildren(projectId, parentId)
                onSuccess()
            }
            _isLoading.value = false
        }
    }

    fun addMultipleNodes(
        projectId: String, 
        parentId: String, 
        names: List<String>, 
        type: String, 
        templateId: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val parentNode = _currentPath.value.lastOrNull()
            val ancestors = parentNode?.ancestors ?: emptyList()
            
            names.forEach { name ->
                val newNode = ProjectNode(
                    projectId = projectId,
                    parentId = parentId,
                    name = name.trim(),
                    type = type,
                    propertyTemplateId = templateId,
                    ancestors = ancestors
                )
                repository.createProjectNode(newNode) // İleride Batch Write ile optimize edilebilir
            }
            
            loadChildren(projectId, parentId)
            onSuccess()
            _isLoading.value = false
        }
    }

    fun updateNodeTemplate(node: ProjectNode, templateId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val updatedNode = node.copy(propertyTemplateId = templateId)
            repository.updateProjectNode(updatedNode).onSuccess {
                // Mevcut path içindeki node'u da güncelle
                val updatedPath = _currentPath.value.map { if (it.id == node.id) updatedNode else it }
                _currentPath.value = updatedPath
                onSuccess()
            }
            _isLoading.value = false
        }
    }

    fun toggleNodeDelete(projectId: String, nodeId: String, isDeleted: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.toggleNodeDelete(projectId, nodeId, isDeleted).onSuccess {
                onSuccess()
            }
            _isLoading.value = false
        }
    }

    private val _nodeJobs = mutableStateOf<List<com.sorodeveloper.insaatcebimde.domain.model.NodeJob>>(emptyList())
    val nodeJobs: State<List<com.sorodeveloper.insaatcebimde.domain.model.NodeJob>> = _nodeJobs

    private var jobsJob: kotlinx.coroutines.Job? = null

    fun loadNodeJobs(projectId: String, nodeId: String) {
        jobsJob?.cancel()
        jobsJob = viewModelScope.launch {
            _isLoading.value = true
            repository.observeNodeJobs(projectId, nodeId).collect { jobs ->
                _nodeJobs.value = jobs
                _isLoading.value = false
            }
        }
    }

    fun updateNodeJobProgress(projectId: String, nodeId: String, jobId: String, progress: Int) {
        viewModelScope.launch {
            repository.updateNodeJobProgress(projectId, nodeId, jobId, progress).onSuccess {
                // Update local state to reflect UI instantly
                val updatedJobs = _nodeJobs.value.map {
                    if (it.id == jobId) it.copy(progress = progress) else it
                }
                _nodeJobs.value = updatedJobs
            }
        }
    }
}
