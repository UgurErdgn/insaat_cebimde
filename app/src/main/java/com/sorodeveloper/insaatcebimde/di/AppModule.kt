package com.sorodeveloper.insaatcebimde.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sorodeveloper.insaatcebimde.data.repository.FirebaseAuthRepositoryImpl
import com.sorodeveloper.insaatcebimde.data.repository.ProjectRepositoryImpl
import com.sorodeveloper.insaatcebimde.domain.repository.AuthRepository
import com.sorodeveloper.insaatcebimde.domain.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val app = com.google.firebase.FirebaseApp.getInstance()
        return FirebaseFirestore.getInstance(app, "default")
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): com.google.firebase.storage.FirebaseStorage {
        return com.google.firebase.storage.FirebaseStorage.getInstance()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(
        impl: ProjectRepositoryImpl
    ): ProjectRepository
}
