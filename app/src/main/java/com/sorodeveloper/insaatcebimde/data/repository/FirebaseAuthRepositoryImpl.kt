package com.sorodeveloper.insaatcebimde.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sorodeveloper.insaatcebimde.domain.model.MatrixPermission
import com.sorodeveloper.insaatcebimde.domain.model.User
import com.sorodeveloper.insaatcebimde.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : AuthRepository {

    override suspend fun getCurrentUser(): Result<User?> {
        val firebaseUser = auth.currentUser
        return if (firebaseUser != null) {
            // İhtiyaca göre Firestore'dan detaylı veriyi de çekebiliriz
            Result.success(User(uid = firebaseUser.uid, email = firebaseUser.email ?: ""))
        } else {
            Result.success(null)
        }
    }

    override suspend fun registerUser(name: String, email: String, pass: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = authResult.user
            if (user != null) {
                val newUser = User(
                    uid = user.uid,
                    name = name,
                    email = email,
                    publicInviteId = java.util.UUID.randomUUID().toString().substring(0, 8).uppercase()
                )
                // Firestore'a kaydet
                db.collection("users").document(user.uid).set(newUser).await()
                Result.success(newUser)
            } else {
                Result.failure(Exception("Kullanıcı oluşturulamadı."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginUser(email: String, pass: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            val user = authResult.user
            if (user != null) {
                Result.success(User(uid = user.uid, email = user.email ?: ""))
            } else {
                Result.failure(Exception("Giriş yapılamadı."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        auth.signOut()
        return Result.success(Unit)
    }

    override fun getUserPermissionsFlow(): Flow<Map<String, List<MatrixPermission>>> = flow {
        // TODO: Realtime database'den anlık yetki güncellemelerini dinle
    }
}
