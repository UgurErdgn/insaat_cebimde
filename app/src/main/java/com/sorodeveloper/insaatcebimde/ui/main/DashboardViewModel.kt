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
        observeProjects()
    }

    private fun observeProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                projectRepository.observeUserProjects().collect { projectList ->
                    _allProjects.value = projectList
                    filterProjects(_searchQuery.value)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                // Hata durumu loglanabilir
                _isLoading.value = false
            }
        }
    }

    fun loadProjects() {
        // DashboardScreen her açıldığında çağrılıyor.
        // Artık Flow ile dinlediğimiz için burada tekrar sunucuya gitmeye gerek yok.
        // Veri zaten anlık güncelleniyor.
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
