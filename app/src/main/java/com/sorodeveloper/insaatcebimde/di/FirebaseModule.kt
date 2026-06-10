package com.sorodeveloper.insaatcebimde.di

import android.content.ContentResolver
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * FirebaseModule: Firebase servislerinin uygulama genelinde paylaşılmasını sağlar.
 * 
 * Neden Yazıldı?: Firebase örneklerini (instance) her sınıfta manuel olarak oluşturmak yerine, 
 * merkezi bir yerden yönetmek ve enjekte etmek için yazıldı.
 * 
 * Neyi Etkiliyor?: Uygulama içindeki tüm Firebase etkileşimlerini etkiler. 
 * Test sırasında bu modülü kolayca "mock" (sahte) servislerle değiştirmemize olanak sağlar.
 * 
 * Nasıl Yazıldı?: Dagger-Hilt @Module ve @Provides anotasyonları kullanıldı. 
 * Nesnelerin uygulama boyunca tek bir örnek (Singleton) olması sağlandı.
 * 
 * Amacı Ne Değil?: Firebase üzerinde herhangi bir veri işlemi yapmaz, sadece servis nesnelerini sunar.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideContentResolver(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): ContentResolver = 
        context.contentResolver
}
