package com.sorodeveloper.insaatcebimde.data.repository

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import com.sorodeveloper.insaatcebimde.domain.repository.JobRepository
import com.sorodeveloper.insaatcebimde.model.SpinnerItem
import com.sorodeveloper.insaatcebimde.viewmodel.ProfessionalJobState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirebaseJobRepositoryImpl: JobRepository arayüzünün Firebase tabanlı gerçeklemesidir.
 * 
 * Neden Yazıldı?: Firebase ile ilgili tüm karmaşık sorguları, veri yazma işlemlerini 
 * ve resim yükleme mantığını tek bir yerde toplamak için yazıldı.
 * 
 * Neyi Etkiliyor?: Uygulamanın veri katmanını temsil eder. ViewModel'lar artık Firebase'i tanımaz, 
 * sadece bu sınıfı kullanır.
 * 
 * Nasıl Yazıldı?: @Inject constructor kullanılarak Firebase servisleri Hilt tarafından sağlandı. 
 * Coroutine yapıları (suspend, async) ile asenkron işlemler yönetildi.
 * 
 * Amacı Ne Değil?: UI ile ilgili herhangi bir işlem yapmaz (Toast göstermek vb.). Sadece veri döner veya hata fırlatır.
 */
@Singleton
class FirebaseJobRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val storage: FirebaseStorage,
    private val contentResolver: ContentResolver // Hilt üzerinden sağlanmalı
) : JobRepository {

    override suspend fun getJobTemplates(insaatId: String, jobName: String): Result<List<SpinnerItem>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.getReference("insaatlar")
                .child(insaatId)
                .child("templates")
                .child("jobs")
                .get().await()

            val list = mutableListOf<SpinnerItem>()
            snapshot.children.forEach { templateNode ->
                val templateId = templateNode.key ?: ""
                val branch = templateNode.child("branch").getValue(String::class.java)
                
                if (branch == jobName) {
                    val category = templateNode.child("category").getValue(String::class.java) ?: ""
                    val type = templateNode.child("type").getValue(String::class.java) ?: ""
                    list.add(SpinnerItem(category, type, templateId))
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getJobDetails(insaatId: String, templateId: String): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.getReference("insaatlar")
                .child(insaatId).child("templates").child("jobs")
                .child(templateId).get().await()

            if (!snapshot.exists()) return@withContext Result.failure(Exception("Template not found"))

            val remoteImages = snapshot.child("cizimler").children.mapNotNull { it.getValue(String::class.java) }
            val materials = snapshot.child("malzemeler").children.associate { 
                it.key!! to (it.getValue(String::class.java) ?: "") 
            }

            Result.success(mapOf(
                "remoteImages" to remoteImages,
                "materials" to materials
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveJob(
        insaatId: String,
        jobName: String,
        jobCards: List<Any>,
        fPath: String,
        newTemplateName: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = hashMapOf<String, Any>()
            val newJobIdsAdded = mutableListOf<String>()

            for (card in jobCards) {
                if (card !is ProfessionalJobState) continue

                val kalemAdi = if (card.isCustom || card.isEditMode) card.customJobName else card.selectedItem?.kalemAdi ?: ""
                val turAdi = if (card.isCustom || card.isEditMode) card.customJobType else card.selectedItem?.turAdi ?: ""

                if (kalemAdi.isBlank()) continue

                // 1. Resimleri Yükle
                val newUploadedUrls = uploadImagesParallel(card.localImages, jobName, kalemAdi, turAdi)
                val allImages = card.remoteImages + newUploadedUrls

                // 2. ID Belirle
                val templateId = if (card.isCustom) UUID.randomUUID().toString() else card.selectedItem?.templateId ?: UUID.randomUUID().toString()
                newJobIdsAdded.add(templateId)

                // 3. Şablon Verisi
                val templateJobData = mapOf(
                    "branch" to jobName,
                    "category" to kalemAdi,
                    "type" to turAdi,
                    "malzemeler" to card.materials.associate { it.name to it.amount },
                    "cizimler" to allImages
                )

                updates["insaatlar/$insaatId/templates/jobs/$templateId"] = templateJobData
                
                if (newTemplateName == null && fPath.isNotBlank()) {
                    updates["$fPath/isler/$templateId/progress"] = card.progress.toString()
                }
            }

            // Klonlama Mantığı
            if (newTemplateName != null && fPath.isNotBlank()) {
                val currentTypeId = database.getReference(fPath).child("unitTypeID").get().await().getValue(String::class.java)
                val oldJobIds = mutableMapOf<String, Boolean>()
                
                if (!currentTypeId.isNullOrEmpty()) {
                    val oldTypeSn = database.getReference("insaatlar/$insaatId/templates/unitTypes/$currentTypeId").get().await()
                    oldTypeSn.child("jobIds").children.forEach { child ->
                        child.key?.let { oldJobIds[it] = true }
                    }
                }

                newJobIdsAdded.forEach { oldJobIds[it] = true }

                val newTypeId = UUID.randomUUID().toString()
                val newUnitType = mapOf(
                    "id" to newTypeId,
                    "name" to newTemplateName,
                    "jobIds" to oldJobIds
                )
                
                updates["insaatlar/$insaatId/templates/unitTypes/$newTypeId"] = newUnitType
                updates["$fPath/unitTypeID"] = newTypeId
            }

            database.reference.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadImage(uri: Uri, path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ref = storage.reference.child(path)
            val compressedData = compressImage(uri)
            val metadata = storageMetadata { contentType = "image/webp" }
            ref.putBytes(compressedData, metadata).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadImagesParallel(uris: List<Uri>, job: String, kalem: String, tur: String): List<String> = coroutineScope {
        uris.map { uri ->
            async {
                val fileName = "${UUID.randomUUID()}.webp"
                val path = "isler/$job/$kalem/$tur/$fileName"
                uploadImage(uri, path).getOrThrow()
            }
        }.awaitAll()
    }

    private suspend fun compressImage(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val inputStream = contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            originalBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, outputStream)
        } else {
            originalBitmap.compress(Bitmap.CompressFormat.WEBP, 85, outputStream)
        }
        outputStream.toByteArray()
    }
}
