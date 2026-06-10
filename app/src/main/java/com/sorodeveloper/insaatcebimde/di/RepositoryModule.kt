package com.sorodeveloper.insaatcebimde.di

import com.sorodeveloper.insaatcebimde.data.repository.FirebaseJobRepositoryImpl
import com.sorodeveloper.insaatcebimde.domain.repository.JobRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * RepositoryModule: Arayüzlerin (Interface) gerçeklemeleri (Implementation) ile eşleştirilmesini sağlar.
 * 
 * Neden Yazıldı?: Hilt'e bir JobRepository istendiğinde hangi sınıfı (FirebaseJobRepositoryImpl) 
 * vermesi gerektiğini söylemek için yazıldı.
 * 
 * Neyi Etkiliyor?: Tüm uygulamanın veri erişim katmanını etkiler. 
 * İleride Firebase yerine başka bir yapıya geçilirse sadece buradaki eşleştirme değiştirilir.
 * 
 * Nasıl Yazıldı?: @Binds anotasyonu kullanılarak soyut eşleştirme yapıldı. 
 * Bu yöntem @Provides'dan daha performanslıdır.
 * 
 * Amacı Ne Değil?: Nesne üretimi yapmaz, sadece "bu arayüzü sorana şu sınıfı ver" der.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindJobRepository(
        firebaseJobRepositoryImpl: FirebaseJobRepositoryImpl
    ): JobRepository
}
