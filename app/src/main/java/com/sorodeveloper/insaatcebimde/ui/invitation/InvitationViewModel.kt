package com.sorodeveloper.insaatcebimde.ui.invitation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorodeveloper.insaatcebimde.domain.model.Invitation
import com.sorodeveloper.insaatcebimde.domain.model.MemberScopes
import com.sorodeveloper.insaatcebimde.domain.repository.InvitationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvitationUiState(
    val pendingInvitations: List<Invitation> = emptyList(),
    val invitationHistory: List<Invitation> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false, // Spam click koruması
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * Davet sistemi ViewModel.
 *
 * Bu ViewModel iki ana göreve hizmet eder:
 * 1. Bana gelen bekleyen davetleri (PENDING) anlık dinleme ve Kabul/Red etme
 * 2. Geçmiş davetleri (ACCEPTED/REJECTED/CANCELLED) listeleme
 *
 * Anti-Spam: isSaving flag'i ile çift tıklama korunması sağlanır.
 * (Usta butona 10 kere basarsa, tek istek gider)
 */
@HiltViewModel
class InvitationViewModel @Inject constructor(
    private val invitationRepository: InvitationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvitationUiState())
    val uiState: StateFlow<InvitationUiState> = _uiState.asStateFlow()

    init {
        observePendingInvitations()
        loadInvitationHistory()
    }

    private fun observePendingInvitations() {
        viewModelScope.launch {
            invitationRepository.observePendingInvitations().collect { invitations ->
                _uiState.update { it.copy(pendingInvitations = invitations) }
            }
        }
    }

    fun loadInvitationHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            invitationRepository.getInvitationHistory()
                .onSuccess { history ->
                    _uiState.update { it.copy(
                        invitationHistory = history,
                        isLoading = false
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        errorMessage = error.message,
                        isLoading = false
                    )}
                }
        }
    }

    fun acceptInvitation(invitationId: String) {
        if (_uiState.value.isSaving) return // Spam koruması
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            invitationRepository.acceptInvitation(invitationId)
                .onSuccess {
                    _uiState.update { it.copy(
                        isSaving = false,
                        successMessage = "Davet kabul edildi!"
                    )}
                    loadInvitationHistory() // Geçmişi güncelle
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isSaving = false,
                        errorMessage = error.message
                    )}
                }
        }
    }

    fun rejectInvitation(invitationId: String) {
        if (_uiState.value.isSaving) return // Spam koruması
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            invitationRepository.rejectInvitation(invitationId)
                .onSuccess {
                    _uiState.update { it.copy(
                        isSaving = false,
                        successMessage = "Davet reddedildi."
                    )}
                    loadInvitationHistory()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isSaving = false,
                        errorMessage = error.message
                    )}
                }
        }
    }

    fun sendInvitation(
        projectId: String,
        projectName: String,
        inviteeInviteId: String,
        permissions: List<String>,
        scopes: MemberScopes,
        roleName: String
    ) {
        if (_uiState.value.isSaving) return // Spam koruması
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            invitationRepository.sendInvitation(
                projectId = projectId,
                projectName = projectName,
                inviteeInviteId = inviteeInviteId,
                grantedPermissions = permissions,
                grantedScopes = scopes,
                grantedRoleName = roleName
            )
                .onSuccess {
                    _uiState.update { it.copy(
                        isSaving = false,
                        successMessage = "Davet başarıyla gönderildi!"
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isSaving = false,
                        errorMessage = error.message
                    )}
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
