package com.sorodeveloper.insaatcebimde.domain.model

/**
 * Davetiye durumları (State Machine)
 * PENDING → ACCEPTED veya REJECTED veya CANCELLED
 */
enum class InvitationStatus {
    PENDING,   // Davet gönderildi, yanıt bekleniyor
    ACCEPTED,  // Davet kabul edildi → Cloud Function üyeyi mülke ekler
    REJECTED,  // Davet reddedildi
    CANCELLED  // Cloud Function güvenlik ihlali saptadı veya davet eden iptal etti
}

/**
 * Firestore `invitations/{invitationId}` koleksiyonundaki davetiye modeli.
 *
 * Akış:
 * 1. Şef, Usta'nın Davetiye ID'sini girer → Android `invitations` koleksiyonuna yazar (PENDING)
 * 2. Cloud Function tetiklenir: Şef'in INVITE yetkisi + Subset kontrolü yapar
 *    - İhlal varsa status = CANCELLED yapılır
 * 3. Usta uygulamayı açar, PENDING davetlerini görür, Kabul/Red eder
 * 4. ACCEPTED → Cloud Function üyeyi mülkün members Map'ine kaydeder
 */
data class Invitation(
    val id: String = "",
    val projectId: String = "",
    val projectName: String = "",        // UI'da gösterim kolaylığı için (denormalize)
    val inviterId: String = "",          // Daveti gönderen kişinin UID'si
    val inviterName: String = "",        // Daveti gönderenin adı (denormalize)
    val inviteeId: String = "",          // Davetiye ID'den çözümlenmiş hedef UID
    val inviteeName: String = "",        // Davet edilen kişinin adı (denormalize)
    val status: String = InvitationStatus.PENDING.name,
    val grantedPermissions: List<String> = emptyList(),
    val grantedDelegablePermissions: List<String> = emptyList(),
    val grantedScopes: MemberScopes = MemberScopes(),
    val grantedRoleName: String = "",    // Davet edenin atadığı rol etiketi
    val statusMessage: String = "",      // İptal/Red sebebi (opsiyonel)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun statusEnum(): InvitationStatus {
        return try {
            InvitationStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            InvitationStatus.PENDING
        }
    }

    fun isPending(): Boolean = statusEnum() == InvitationStatus.PENDING
    fun isAccepted(): Boolean = statusEnum() == InvitationStatus.ACCEPTED
    fun isRejected(): Boolean = statusEnum() == InvitationStatus.REJECTED
    fun isCancelled(): Boolean = statusEnum() == InvitationStatus.CANCELLED
}
