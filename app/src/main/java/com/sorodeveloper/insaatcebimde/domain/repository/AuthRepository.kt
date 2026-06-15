package com.sorodeveloper.insaatcebimde.domain.repository

import com.sorodeveloper.insaatcebimde.domain.model.ProjectRole
import com.sorodeveloper.insaatcebimde.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun getCurrentUser(): Result<User?>
    suspend fun registerUser(name: String, email: String, pass: String): Result<User>
    suspend fun loginUser(email: String, pass: String): Result<User>
    suspend fun logout(): Result<Unit>
    
    // Matrix Yetkilendirme indeksini Flow olarak dinlemek için
    fun getUserPermissionsFlow(): Flow<Map<String, ProjectRole>>
}
