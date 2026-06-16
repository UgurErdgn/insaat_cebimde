package com.sorodeveloper.insaatcebimde.domain.repository

import com.sorodeveloper.insaatcebimde.domain.model.Invitation
import com.sorodeveloper.insaatcebimde.domain.model.MemberScopes
import kotlinx.coroutines.flow.Flow

/**
 * Davet sistemi repository interface'i.
 *
 * Tüm CRUD operasyonları Cache-First stratejisine uyar.
 * Davet gönderme işlemi doğrudan invitations koleksiyonuna yazılır,
 * Cloud Function güvenlik kontrolünü yapar.
 */
interface InvitationRepository {

    /**
     * Yeni davet gönder.
     * inviteeInviteId: Hedef kullanıcının paylaştığı Davetiye ID'si (Örn: "UGR-842-193")
     * Cloud Function arkaplanda subset kontrolünü yapacak.
     */
    suspend fun sendInvitation(
        projectId: String,
        projectName: String,
        inviteeInviteId: String,
        grantedPermissions: List<String>,
        grantedScopes: MemberScopes,
        grantedRoleName: String
    ): Result<Unit>

    /** Bana gelen bekleyen (PENDING) davetleri dinle — Snapshot Listener (Real-time) */
    fun observePendingInvitations(): Flow<List<Invitation>>

    /** Davetiyeyi kabul et */
    suspend fun acceptInvitation(invitationId: String): Result<Unit>

    /** Davetiyeyi reddet */
    suspend fun rejectInvitation(invitationId: String): Result<Unit>

    /** Geçmiş davetiyelerim (ACCEPTED/REJECTED/CANCELLED) — Cache-First */
    suspend fun getInvitationHistory(): Result<List<Invitation>>

    /** Belirli bir projeye gönderilmiş davetleri listele (Şef görünümü) — Cache-First */
    suspend fun getProjectInvitations(projectId: String): Result<List<Invitation>>
}
