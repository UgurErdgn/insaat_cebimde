package com.sorodeveloper.insaatcebimde.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorodeveloper.insaatcebimde.domain.model.MemberInfo
import com.sorodeveloper.insaatcebimde.domain.model.MemberScopes
import com.sorodeveloper.insaatcebimde.domain.model.Permission
import com.sorodeveloper.insaatcebimde.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MembersUiState(
    val members: List<MemberInfo> = emptyList(),
    val currentUserMember: MemberInfo? = null, // Giriş yapan kullanıcının kendi üye bilgisi
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * Üye yönetimi (Çalışanlar sekmesi) ViewModel.
 *
 * Bu ViewModel:
 * 1. Projedeki tüm üyeleri listeler
 * 2. Mevcut kullanıcının yetkilerini State olarak tutar (UI Filtreleme)
 * 3. Yetki düzenleme ve kovma işlemlerini Cloud Functions'a iletir
 *
 * Zero-Bill: Üye listesi proje dökümanından okunur — ekstra Read YOK
 * (members Map zaten Project dökümanının içinde)
 */
@HiltViewModel
class MembersViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MembersUiState())
    val uiState: StateFlow<MembersUiState> = _uiState.asStateFlow()

    private var currentProjectId: String? = null

    fun observeMembers(projectId: String, currentUserId: String) {
        currentProjectId = projectId
        viewModelScope.launch {
            projectRepository.observeProjectMembers(projectId).collect { memberList ->
                val currentMember = memberList.find { it.uid == currentUserId }
                _uiState.update { it.copy(
                    members = memberList,
                    currentUserMember = currentMember,
                    isLoading = false
                )}
            }
        }
    }

    /**
     * Mevcut kullanıcının belirli bir yetkiye sahip olup olmadığını kontrol eder.
     * UI bu fonksiyonu kullanarak butonları gizler/gösterir.
     */
    fun hasPermission(permission: Permission): Boolean {
        return _uiState.value.currentUserMember?.hasPermission(permission) ?: false
    }

    /**
     * Mevcut kullanıcının hedef üyeyi yönetip yönetemeyeceğini kontrol eder.
     * Bu kontrol UI tarafında yapılır (butonları gizle/göster).
     * Asıl güvenlik Cloud Functions'da.
     */
    fun canManageMember(targetMember: MemberInfo): Boolean {
        val currentMember = _uiState.value.currentUserMember ?: return false
        return currentMember.canManage(targetMember)
    }

    /**
     * Hedef üyenin yetkilerini güncelle.
     * Cloud Function subset kontrolünü yapacak.
     */
    fun updateMemberPermissions(
        targetUserId: String,
        newPermissions: List<String>,
        newDelegablePermissions: List<String>,
        newScopes: MemberScopes,
        newRoleName: String
    ) {
        if (_uiState.value.isSaving) return // Spam koruması
        val projectId = currentProjectId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            projectRepository.requestMemberUpdate(
                projectId = projectId,
                targetUserId = targetUserId,
                newPermissions = newPermissions,
                newDelegablePermissions = newDelegablePermissions,
                newScopes = newScopes,
                newRoleName = newRoleName
            )
                .onSuccess {
                    _uiState.update { it.copy(
                        isSaving = false,
                        successMessage = "Yetkiler başarıyla güncellendi."
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

    /**
     * Üyeyi projeden çıkar.
     * Cloud Function hiyerarşi (subset) kontrolünü yapacak.
     */
    fun removeMember(targetUserId: String) {
        if (_uiState.value.isSaving) return // Spam koruması
        val projectId = currentProjectId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            projectRepository.removeMember(projectId, targetUserId)
                .onSuccess {
                    _uiState.update { it.copy(
                        isSaving = false,
                        successMessage = "Üye projeden çıkarıldı."
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
