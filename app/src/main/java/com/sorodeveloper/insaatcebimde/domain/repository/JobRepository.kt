package com.sorodeveloper.insaatcebimde.domain.repository

import android.net.Uri
import com.sorodeveloper.insaatcebimde.model.SpinnerItem
import kotlinx.coroutines.flow.Flow

/**
 * JobRepository: İş kalemleri (Jobs) ile ilgili tüm veri işlemlerinin kontratıdır.
 * 
 * Neden Yazıldı?: UI ve ViewModel katmanının verinin nereden (Firebase, SQL, Local) geldiğini 
 * bilmemesi gerekir. Bu arayüz, veri kaynağını soyutlar.
 * 
 * Neyi Etkiliyor?: ViewModel'ların veriye erişim şeklini etkiler. Kodun test edilebilirliğini artırır.
 * 
 * Nasıl Yazıldı?: Kotlin Interface olarak tanımlandı. Coroutine (suspend) ve Flow yapıları kullanıldı.
 * 
 * Amacı Ne Değil?: Firebase'e özel kod içermez. Sadece "ne yapılacağını" söyler, "nasıl yapılacağını" değil.
 */
interface JobRepository {
    suspend fun getJobTemplates(insaatId: String, jobName: String): Result<List<SpinnerItem>>
    
    suspend fun getJobDetails(insaatId: String, templateId: String): Result<Map<String, Any>>
    
    suspend fun saveJob(
        insaatId: String,
        jobName: String,
        jobCards: List<Any>, // Geçici olarak Any, ileride modelleşecek
        fPath: String,
        newTemplateName: String? = null
    ): Result<Unit>
    
    suspend fun uploadImage(uri: Uri, path: String): Result<String>
}
