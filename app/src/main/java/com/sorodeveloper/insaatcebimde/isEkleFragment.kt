package com.sorodeveloper.insaatcebimde

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.FirebaseDatabase.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import com.sorodeveloper.insaatcebimde.adapter.KalemEkleAdapter
import com.sorodeveloper.insaatcebimde.databinding.CardviewKalemEklemeBinding

import com.sorodeveloper.insaatcebimde.databinding.FragmentIsEkleBinding
import com.sorodeveloper.insaatcebimde.databinding.ItemMalzemeEkleRowBinding
import com.sorodeveloper.insaatcebimde.model.SpinnerItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID


class isEkleFragment : Fragment() {
    private var currentImagesRecycler: RecyclerView? = null
    private val selectedImagesMap = mutableMapOf<RecyclerView, MutableList<Uri>>()
    private var globalSpinnerList = listOf<SpinnerItem>()
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private var selectedPosition: Int = -1

    private lateinit var kalemEkleAdapter: KalemEkleAdapter


    private fun setupSpinner(list: List<SpinnerItem>) {
        globalSpinnerList = list
    }

    private val pickImagesLauncher =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
            if (!uris.isNullOrEmpty()) {
                val list = selectedImagesMap[currentImagesRecycler] ?: mutableListOf()
                list.addAll(uris)
                selectedImagesMap[currentImagesRecycler!!] = list
                currentImagesRecycler!!.adapter?.notifyDataSetChanged()
            }
        }
    private lateinit var binding: FragmentIsEkleBinding


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentFragmentManager.popBackStack()
                }
            }
        )
    }

    private var insaatId : String ?= null
    private var crLevel : String ?= null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentIsEkleBinding.inflate(inflater, container, false)

        insaatId = arguments?.getString("insaatID")
        crLevel = arguments?.getString("crLevel")



        kalemEkleAdapter = KalemEkleAdapter(
            globalSpinnerList,
            requireContext(),
            insaatId!!,
            crLevel!!
        ){ position ->

            selectedPosition = position
            imagePickerLauncher.launch("image/*")
        }

        binding.rvKalemler.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvKalemler.adapter = kalemEkleAdapter

        binding.rvKalemler.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvKalemler.adapter = kalemEkleAdapter

        binding.btnAddSubItem.setOnClickListener {
            kalemEkleAdapter.addNewItem()
        }

        binding.btnGetKalems.setOnClickListener{
           fetchKalems()
        }

        binding.btnSaveWholeJob.setOnClickListener {
            saveWholeJob()
        }

        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->

                if (selectedPosition != -1 && uris.isNotEmpty()) {

                    val model = kalemEkleAdapter.getItem(selectedPosition)

                    model.localCizimler.addAll(uris.map {it})

                    kalemEkleAdapter.notifyItemChanged(selectedPosition)
                }
            }

        return binding.root
    }


    private fun saveWholeJob() {
        val jobName = binding.etMainJobName.text?.toString()?.trim() ?: return
        val fPath = arguments?.getString("fPath") ?: "genel_isler" // Firebase veritabanı yolu

        // UI'da bir Loading Spinner başlat (Örn: binding.progressBar.show())

        lifecycleScope.launch {
            try {
                val kalemlerMap = mutableMapOf<String, Any>()
                val kalemMap = mutableMapOf<String, Any>()
                var kalemAdi = ""

                // 1. Her bir Kalem için işlemleri başlat
                for (i in 0 until binding.rvKalemler.childCount) {
                    val kalemView = binding.rvKalemler.getChildAt(i)
                    val kalemBinding = CardviewKalemEklemeBinding.bind(kalemView)
                    kalemAdi = kalemBinding.etkalemAdi.text?.toString()?.trim() ?: continue

                    // 2. Resimleri paralel olarak yükle (Performansın kalbi burası)
                    val imageUris = selectedImagesMap[kalemBinding.rvImages] ?: mutableListOf()
                    val uploadedUrls = uploadImagesParallel(imageUris, jobName, kalemAdi)

                    // 3. Malzemeleri topla
                    val malzemeler = collectMaterials(kalemBinding.materialContainer)

                    // 4. Kalem verisini oluştur
                    val kalemData = mapOf(
                        "malzemeler" to malzemeler,
                        "cizimler" to uploadedUrls,
                        "progress" to "0"
                    )
                    kalemMap[kalemAdi] = kalemData
                    val turMap = mapOf(
                        "a" to kalemData
                    )
                    kalemlerMap[kalemAdi] = turMap

                }

                // 👇 Tek seferde yazım
                getInstance().getReference(fPath)
                    .child("isler")
                    .child(jobName)
                    .setValue(kalemMap)
                    .await()

                getInstance().getReference("insaatlar/$insaatId/insaatIsleri/$crLevel")
                    .child(jobName)
                    .setValue(kalemlerMap)
                    .await()

                Toast.makeText(requireContext(), "İş başarıyla kaydedildi!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                Log.e("UPLOAD_ERROR", "Hata: ${e.message} $fPath")
                Toast.makeText(requireContext(), "Yükleme başarısız!", Toast.LENGTH_SHORT).show()
            } finally {
                // Loading'i kapat
            }
        }
    }

    // Resimleri paralel yükleyen yardımcı fonksiyon
    private suspend fun uploadImagesParallel(uris: List<Uri>, job: String, kalem: String): List<String> = coroutineScope {
        Log.e("UPLOAD_URI", "URI: $uris")
        uris.map { uri ->
            async(Dispatchers.IO) {
                val fileName = "${UUID.randomUUID()}.webp"
                val ref = FirebaseStorage.getInstance().reference.child("isler/$job/$kalem/$fileName")

                // 1. Önce resmi sıkıştır (Boyut burada düşüyor!)
                val compressedData = compressImage(uri)

                // 2. Metadata ekleyerek tarayıcıda/appde düzgün görünmesini sağla
                val metadata = storageMetadata {
                    contentType = "image/webp"
                }

                // 3. Sıkıştırılmış ByteArray'i yükle
                ref.putBytes(compressedData, metadata).await()
                ref.downloadUrl.await().toString()
            }
        }.awaitAll()
    }
    private suspend fun compressImage(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        val outputStream = ByteArrayOutputStream()

        // WebP formatı kaliteyi korurken boyutu en çok düşüren formattır.
        // 100 tam kalite, 85-90 ise gözle fark edilmeyen ama boyutu devasa düşüren kalitedir.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            originalBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, outputStream)
        } else {
            originalBitmap.compress(Bitmap.CompressFormat.WEBP, 90, outputStream)
        }

        outputStream.toByteArray()
    }
    private fun collectMaterials(container: LinearLayout): Map<String, String> {
        val map = mutableMapOf<String, String>()

        for (i in 0 until container.childCount) {
            val binding = ItemMalzemeEkleRowBinding.bind(container.getChildAt(i))
            val amount = binding.etMaterialAmount.text?.toString()?.trim()
            val type = binding.etMaterialType.text?.toString()?.trim()

            if (!amount.isNullOrEmpty() && !type.isNullOrEmpty()) {
                map[type] = amount
            }
        }

        return map
    }
    private  fun fetchKalems () {
        val jobName = binding.etMainJobName.text.toString().trim()

        Log.d("KONTROL", "Aranan iş: $jobName")

        val ref = FirebaseDatabase.getInstance()
            .getReference("insaatlar")
            .child(insaatId!!)
            .child("insaatIsleri")
            .child(crLevel!!)
            .child(jobName)

        ref.get().addOnSuccessListener { snapshot ->

            Log.d("KONTROL", "Snapshot geldi. Çocuk sayısı: ${snapshot.childrenCount}")

            val spinnerList = mutableListOf<SpinnerItem>()

            for (kalemSnap in snapshot.children) {

                val kalemAdi = kalemSnap.key ?: continue
                Log.d("KONTROL", "Kalem bulundu: $kalemAdi")

                if (kalemAdi == "progress") {
                    Log.d("KONTROL", "Kalem seviyesinde progress atlandı")
                    continue
                }

                for (turSnap in kalemSnap.children) {

                    val turAdi = turSnap.key ?: continue
                    Log.d("KONTROL", "   Tür bulundu: $turAdi")

                    if (turAdi == "progress") {
                        Log.d("KONTROL", "   Tür seviyesinde progress atlandı")
                        continue
                    }

                    spinnerList.add(SpinnerItem(kalemAdi, turAdi))

                    Log.d(
                        "KONTROL",
                        "   ✅ Listeye eklendi: $kalemAdi - $turAdi"
                    )
                }
            }

            Log.d("KONTROL", "Toplam Spinner Elemanı: ${spinnerList.size}")

            kalemEkleAdapter.updateSpinnerList(spinnerList)
        }.addOnFailureListener {
            Log.e("KONTROL", "Firebase hata: ${it.message}")
        }
    }
}
