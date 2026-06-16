package com.sorodeveloper.insaatcebimde.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.sorodeveloper.insaatcebimde.domain.model.Invitation
import com.sorodeveloper.insaatcebimde.domain.model.InvitationStatus
import com.sorodeveloper.insaatcebimde.domain.model.MemberScopes
import com.sorodeveloper.insaatcebimde.domain.repository.InvitationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Davet sistemi Firestore implementasyonu.
 *
 * Güvenlik Katmanı:
 * - Davet gönderme: Client → invitations koleksiyonuna yazar → Cloud Function doğrular
 * - Kabul/Red: Client status güncellemesi yapar → Cloud Function (onUpdate) members'a ekler
 *
 * Zero-Bill Politikası:
 * - Pending davetler Snapshot Listener ile dinlenir (ilk yüklemeden sonra delta-sync)
 * - Geçmiş davetler Cache-First stratejisiyle okunur
 */
class FirebaseInvitationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : InvitationRepository {

    private val invitationsRef = firestore.collection("invitations")

    override suspend fun sendInvitation(
        projectId: String,
        projectName: String,
        inviteeInviteId: String,
        grantedPermissions: List<String>,
        grantedScopes: MemberScopes,
        grantedRoleName: String
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Kullanıcı girişi bulunamadı!")

            // Hedef kullanıcıyı Davetiye ID'si ile bul
            val usersQuery = firestore.collection("users")
                .whereEqualTo("publicInviteId", inviteeInviteId)
                .get(Source.SERVER).await()

            if (usersQuery.isEmpty) {
                return Result.failure(Exception("Bu davet kodu ile eşleşen kullanıcı bulunamadı."))
            }

            val inviteeDoc = usersQuery.documents.first()
            val inviteeUid = inviteeDoc.getString("uid") ?: inviteeDoc.id
            val inviteeName = inviteeDoc.getString("name") ?: ""

            // Mevcut kullanıcının adını cache'den al
            val inviterSnapshot = try {
                firestore.collection("users").document(currentUser.uid)
                    .get(Source.CACHE).await()
            } catch (e: Exception) {
                firestore.collection("users").document(currentUser.uid)
                    .get(Source.SERVER).await()
            }
            val inviterName = inviterSnapshot.getString("name") ?: ""

            val docRef = invitationsRef.document()
            val invitation = Invitation(
                id = docRef.id,
                projectId = projectId,
                projectName = projectName,
                inviterId = currentUser.uid,
                inviterName = inviterName,
                inviteeId = inviteeUid,
                inviteeName = inviteeName,
                status = InvitationStatus.PENDING.name,
                grantedPermissions = grantedPermissions,
                grantedScopes = grantedScopes,
                grantedRoleName = grantedRoleName
            )

            docRef.set(invitation).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observePendingInvitations(): Flow<List<Invitation>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            close(Exception("Kullanıcı girişi bulunamadı!"))
            return@callbackFlow
        }

        val query = invitationsRef
            .whereEqualTo("inviteeId", currentUser.uid)
            .whereEqualTo("status", InvitationStatus.PENDING.name)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val invitations = snapshot.documents.mapNotNull {
                    it.toObject(Invitation::class.java)
                }
                trySend(invitations.sortedByDescending { it.createdAt })
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun acceptInvitation(invitationId: String): Result<Unit> {
        return try {
            invitationsRef.document(invitationId).update(
                mapOf(
                    "status" to InvitationStatus.ACCEPTED.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectInvitation(invitationId: String): Result<Unit> {
        return try {
            invitationsRef.document(invitationId).update(
                mapOf(
                    "status" to InvitationStatus.REJECTED.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInvitationHistory(): Result<List<Invitation>> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Kullanıcı girişi bulunamadı!")

            // Bana gelen geçmiş davetler (Cache-First)
            val receivedQuery = invitationsRef
                .whereEqualTo("inviteeId", currentUser.uid)

            var snapshot = try {
                receivedQuery.get(Source.CACHE).await()
            } catch (e: Exception) { null }

            if (snapshot == null || snapshot.isEmpty) {
                snapshot = receivedQuery.get(Source.SERVER).await()
            }

            val invitations = snapshot.documents.mapNotNull {
                it.toObject(Invitation::class.java)
            }.filter { !it.isPending() } // Sadece sonuçlanmış olanlar

            Result.success(invitations.sortedByDescending { it.updatedAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProjectInvitations(projectId: String): Result<List<Invitation>> {
        return try {
            val query = invitationsRef.whereEqualTo("projectId", projectId)

            var snapshot = try {
                query.get(Source.CACHE).await()
            } catch (e: Exception) { null }

            if (snapshot == null || snapshot.isEmpty) {
                snapshot = query.get(Source.SERVER).await()
            }

            val invitations = snapshot.documents.mapNotNull {
                it.toObject(Invitation::class.java)
            }
            Result.success(invitations.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
