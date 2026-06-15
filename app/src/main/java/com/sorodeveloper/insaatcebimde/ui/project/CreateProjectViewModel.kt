package com.sorodeveloper.insaatcebimde.ui.project

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
class CreateProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _success = mutableStateOf(false)
    val success: State<Boolean> = _success

    fun createProject(
        name: String,
        city: String,
        district: String,
        neighborhood: String,
        fullAddress: String,
        contractorName: String,
        contractorEmail: String,
        contractorPhone: String
    ) {
        if (name.isBlank() || city.isBlank() || contractorName.isBlank()) {
            _error.value = "Lütfen zorunlu alanları doldurun."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val project = Project(
                name = name,
                city = city,
                district = district,
                neighborhood = neighborhood,
                fullAddress = fullAddress,
                contractorName = contractorName,
                contractorEmail = contractorEmail,
                contractorPhone = contractorPhone
            )

            val result = projectRepository.createProject(project)
            result.onSuccess {
                _success.value = true
                _isLoading.value = false
            }.onFailure { exception ->
                _error.value = exception.message ?: "İnşaat oluşturulurken bir hata oluştu."
                _isLoading.value = false
            }
        }
    }
}
