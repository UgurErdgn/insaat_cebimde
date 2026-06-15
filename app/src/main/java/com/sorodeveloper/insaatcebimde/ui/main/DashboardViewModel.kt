package com.sorodeveloper.insaatcebimde.ui.main

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorodeveloper.insaatcebimde.domain.model.Project
import com.sorodeveloper.insaatcebimde.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _allProjects = mutableStateOf<List<Project>>(emptyList())
    
    private val _projects = mutableStateOf<List<Project>>(emptyList())
    val projects: State<List<Project>> = _projects

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = projectRepository.getUserProjects()
            result.onSuccess { 
                _allProjects.value = it
                filterProjects(_searchQuery.value)
            }
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterProjects(query)
    }

    private fun filterProjects(query: String) {
        if (query.isBlank()) {
            _projects.value = _allProjects.value
        } else {
            val lowerQuery = query.lowercase()
            _projects.value = _allProjects.value.filter {
                it.name.lowercase().contains(lowerQuery) ||
                it.city.lowercase().contains(lowerQuery) ||
                it.district.lowercase().contains(lowerQuery) ||
                it.contractorName.lowercase().contains(lowerQuery)
            }
        }
    }
}
