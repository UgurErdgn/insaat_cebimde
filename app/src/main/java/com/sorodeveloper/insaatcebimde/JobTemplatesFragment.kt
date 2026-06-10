package com.sorodeveloper.insaatcebimde

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.sorodeveloper.insaatcebimde.adapter.IsKollariAdapter
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.lifecycleScope
import java.io.ByteArrayOutputStream
import java.util.UUID
import com.sorodeveloper.insaatcebimde.databinding.FragmentJobTemplatesBinding
import isKollari

class JobTemplatesFragment : Fragment() {

    private var _binding: FragmentJobTemplatesBinding? = null
    private val binding get() = _binding!!

    private var insaatId: String = ""
    private var templatesRef: com.google.firebase.database.DatabaseReference? = null
    private var templatesListener: com.google.firebase.database.ValueEventListener? = null
    
    // 🔥 Listeyi yerinde yönetmek için sınıf düzeyine taşıyalım
    private val fullTemplateList = mutableListOf<isKollari.MainItem>()
    
    // 🔥 Geçici olarak düzenlenen öğe (resim seçimi için)
    private var currentlyEditingItem: isKollari.SubItem? = null

    private val imagePicker = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let { selectedUris ->
            currentlyEditingItem?.let { item ->
                item.localDrawingUris.addAll(selectedUris)
                binding.rvJobTemplates.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobTemplatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        insaatId = arguments?.getString("insaatID") ?: ""

        binding.rvJobTemplates.layoutManager = LinearLayoutManager(requireContext())

        fetchTemplates()

        binding.fabAddTemplate.setOnClickListener {
            // Şablon Modunda (fPath =="") yeni şablon ekleme açılır
            openJobAddingFragment()
        }

        binding.btnCreateGroup.setOnClickListener {
            val editText = android.widget.EditText(requireContext())
            editText.hint = "Örn: Mobilya İşleri"
            
            // padding vermek için küçük bir hile veya direkt layout:
            val container = android.widget.FrameLayout(requireContext())
            val params = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(60, 20, 60, 0)
            editText.layoutParams = params
            container.addView(editText)

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Yeni Grup Oluştur")
                .setMessage("Yeni şablonların ekleneceği ana grup adını giriniz:")
                .setView(container)
                .setPositiveButton("Oluştur") { _, _ ->
                    val groupName = editText.text.toString().trim()
                    if (groupName.isNotEmpty()) {
                        createNewGroupInline(groupName)
                    } else {
                        Toast.makeText(requireContext(), "Grup adı boş olamaz", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("İptal", null)
                .show()
        }
    }

    private fun fetchTemplates() {
        if (insaatId.isEmpty()) return

        templatesRef = FirebaseDatabase.getInstance().getReference("insaatlar/$insaatId/templates/jobs")
        templatesListener = templatesRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null || !isAdded) return

                val templates = mutableListOf<isKollari.MainItem>()

                val groupedTemplates = mutableMapOf<String, MutableList<isKollari.SubItem>>()

                // Ağacı çözümleyip Hiyerarşik listeye (MainItem -> SubItem) dönüştürüyoruz
                for (templateNode in snapshot.children) {
                    val templateId = templateNode.key ?: continue
                    val branchTitle = templateNode.child("branch").getValue(String::class.java)
                    
                    if (branchTitle != null) {
                        // YENİ (FLAT) SİSTEM
                        val categoryTitle = templateNode.child("category").getValue(String::class.java) ?: continue
                        val typeTitle = templateNode.child("type").getValue(String::class.java) ?: continue
                        
                        val dUrls = templateNode.child("cizimler").children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
                        val mats = templateNode.child("malzemeler").children.associate { it.key!! to it.getValue(String::class.java)!! }.toMutableMap()
                        
                        val subItem = isKollari.SubItem(
                            templateId = templateId, // 🔥 ID eklendi
                            title = "$categoryTitle ($typeTitle)",
                            trade = branchTitle,
                            category = categoryTitle,
                            type = typeTitle,
                            progress = "0",
                            kacAdet = "1",
                            firebasePath = "",
                            isEditable = false,
                            drawingUrls = dUrls,
                            materials = mats
                        )
                        groupedTemplates.getOrPut(branchTitle) { mutableListOf() }.add(subItem)
                    }
                }
                
                fullTemplateList.clear()
                
                // Gruplanmış listeyi MainItem'lara dönüştür
                for ((branch, subItems) in groupedTemplates) {
                    fullTemplateList.add(
                        isKollari.MainItem(
                            title = branch,
                            subItems = subItems.toMutableList(),
                            ilerleme = "0",
                            isExpanded = false,
                            kacAdet = subItems.size.toString()
                        )
                    )
                }

                // Alfabetik sıralama (Opsiyonel ama düzenli durur)
                fullTemplateList.sortBy { it.title }

                // Aynı mükemmel açılır kapanır IsKollariAdapter'ı Kütüphane ekranı için kullanıyoruz
                binding.rvJobTemplates.adapter = IsKollariAdapter(fullTemplateList, true, { jobTitle ->
                    // Kart içindeki "Kalem Ekle" tıklandığında (ŞİMDİ INLINE):
                    addNewSubItemInline(jobTitle)
                }, { subItem ->
                    // Alt kalem yanındaki "Düzenle" tıklandığında:
                    handleEditAction(subItem)
                }, { subItem ->
                    // Resim Yükle tıklandığında:
                    currentlyEditingItem = subItem
                    imagePicker.launch("image/*")
                }, { subItem, cat, typ, mat ->
                    // Kaydet tıklandığında:
                    saveTemplateToFirebase(subItem, cat, typ, mat)
                }) { mainItem ->
                    // Ana iteme tıklandığında özel bir eyleme gerek yok
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("JobTemplatesFragment", "Error fetching templates: ${error.message}")
            }
        })
    }

    private fun handleEditAction(subItem: isKollari.SubItem) {
        if (subItem.isEditing) {
            // Zaten düzenleme modunda, çıkmak istiyor mu sor
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Düzenlemeyi İptal Et")
                .setMessage("Yapılan değişiklikler geri alınacak. Emin misiniz?")
                .setPositiveButton("Evet") { _, _ ->
                    // EĞER BU YENİ EKLENEN (KAYDEDİLMEMİŞ) BİR KALEMSE LİSTEDEN SİL
                    if (subItem.category.isEmpty() && subItem.type.isEmpty() && subItem.drawingUrls.isEmpty()) {
                        removeSubItemFromLocalList(subItem)
                    } else {
                        subItem.isEditing = false
                        binding.rvJobTemplates.adapter?.notifyDataSetChanged()
                    }
                }
                .setNegativeButton("Hayır", null)
                .show()
        } else {
            // Düzenleme moduna gir - Başlangıç durumunu yedekle
            subItem.initialDrawingUrls.clear()
            subItem.initialDrawingUrls.addAll(subItem.drawingUrls)
            
            subItem.isEditing = true
            binding.rvJobTemplates.adapter?.notifyDataSetChanged()
        }
    }

    private fun createNewGroupInline(groupName: String) {
        // Eğer varsa onu kullan, yoksa yeni oluştur
        var mainItem = fullTemplateList.find { it.title.equals(groupName, ignoreCase = true) }
        
        if (mainItem == null) {
            mainItem = isKollari.MainItem(
                title = groupName,
                subItems = mutableListOf(),
                ilerleme = "0",
                isExpanded = true,
                kacAdet = "0"
            )
            fullTemplateList.add(0, mainItem) // En üste ekle
        } else {
            mainItem.isExpanded = true
        }
        
        // Diğer sekmeleri kapat
        fullTemplateList.forEach { if (it.title != mainItem.title) it.isExpanded = false }
        
        binding.rvJobTemplates.adapter?.notifyDataSetChanged()
        
        // Yeni grubun içine boş bir kalem yerleştir
        addNewSubItemInline(mainItem.title)
    }

    private fun addNewSubItemInline(jobTitle: String) {
        val mainItem = fullTemplateList.find { it.title == jobTitle } ?: return
        
        val newSubItem = isKollari.SubItem(
            templateId = UUID.randomUUID().toString(),
            title = "Yeni Kalem (Kaydedilmedi)",
            trade = jobTitle,
            category = "",
            type = "",
            progress = "0",
            kacAdet = "1",
            firebasePath = "",
            isEditable = false,
            isEditing = true,
            isTabOpen = true
        )
        
        // Kategori sonuna ekle
        mainItem.subItems.add(newSubItem)
        binding.rvJobTemplates.adapter?.notifyDataSetChanged()
        
        // Odaklanması için (Opsiyonel: Scroll yapılabilir)
        Toast.makeText(requireContext(), "$jobTitle grubuna yeni satır eklendi", Toast.LENGTH_SHORT).show()
    }

    private fun removeSubItemFromLocalList(subItem: isKollari.SubItem) {
        fullTemplateList.forEach { mainItem ->
            if (mainItem.subItems.remove(subItem)) {
                binding.rvJobTemplates.adapter?.notifyDataSetChanged()
                return
            }
        }
    }

    private fun saveTemplateToFirebase(
        item: isKollari.SubItem,
        category: String,
        type: String,
        materials: Map<String, String>
    ) {
        if (insaatId.isEmpty() || item.templateId.isEmpty()) return

        // 🔥 VALIDASYON: Kategori ve Tür boş olamaz
        if (category.isBlank() || type.isBlank()) {
            Toast.makeText(requireContext(), "Kategori ve Tür alanları boş bırakılamaz!", Toast.LENGTH_LONG).show()
            return
        }

        // 1. Önce yeni resimleri yükle (Sıkıştırılmış ve Paralel)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uploadedUrls = uploadImagesParallel(item)
                val finalUrls = (item.drawingUrls + uploadedUrls).distinct()
                
                // 2. Silinen resimleri Storage'dan temizle (Garbage Collection)
                val deletedUrls = item.initialDrawingUrls.filter { !finalUrls.contains(it) }
                deleteOrphanedImages(deletedUrls)

                // 3. Şablon verisini hazırla
                val templateData = mapOf(
                    "branch" to item.trade,
                    "category" to category,
                    "type" to type,
                    "cizimler" to finalUrls,
                    "malzemeler" to materials
                )

                // 4. Firebase Database'e yaz
                FirebaseDatabase.getInstance().getReference("insaatlar/$insaatId/templates/jobs/${item.templateId}")
                    .setValue(templateData)
                    .await()

                Toast.makeText(requireContext(), "Şablon başarıyla güncellendi", Toast.LENGTH_SHORT).show()
                item.isEditing = false
                item.localDrawingUris.clear()
                item.initialDrawingUrls.clear()
                binding.rvJobTemplates.adapter?.notifyDataSetChanged()

            } catch (e: Exception) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                }
                Log.e("SAVE_ERROR", "Error: ", e)
            }
        }
    }

    private suspend fun uploadImagesParallel(item: isKollari.SubItem): List<String> = coroutineScope {
        if (item.localDrawingUris.isEmpty()) return@coroutineScope emptyList()

        item.localDrawingUris.map { uri ->
            async(Dispatchers.IO) {
                val fileName = "img_${UUID.randomUUID()}.webp"
                val ref = FirebaseStorage.getInstance().getReference("insaat_cebimde/templates/images/${item.templateId}/$fileName")
                
                // WebP Sıkıştırma
                val compressedData = compressImage(uri)
                val metadata = com.google.firebase.storage.storageMetadata { contentType = "image/webp" }
                
                ref.putBytes(compressedData, metadata).await()
                ref.downloadUrl.await().toString()
            }
        }.awaitAll()
    }

    private suspend fun compressImage(uri: android.net.Uri): ByteArray = withContext(Dispatchers.Default) {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        
        // Android sürümüne göre WebP formatı seçimi
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            originalBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, outputStream)
        } else {
            originalBitmap.compress(Bitmap.CompressFormat.WEBP, 85, outputStream)
        }
        outputStream.toByteArray()
    }

    private fun deleteOrphanedImages(urls: List<String>) {
        urls.forEach { url ->
            try {
                FirebaseStorage.getInstance().getReferenceFromUrl(url).delete()
                    .addOnFailureListener { Log.w("GC_ERROR", "Could not delete orphan: $url") }
            } catch (e: Exception) {
                Log.e("GC_ERROR", "Error parsing URL: $url", e)
            }
        }
    }

    private fun openJobAddingFragment(
        subItem: isKollari.SubItem? = null,
        initialJobName: String? = null
    ) {
        // Eğer bir subItem gelmişse (düzenleme), navigasyon yapmak yerine handleEditAction çağrılabilir
        // Ancak FAB butonu hala eski usul yeni bir ekran açmak isteyebilir. Tutanak gereği navigasyon kalabilir 
        // ama düzenleme artık inline olduğu için handleEditAction asıl kurgudur.
        
        if (subItem != null) {
            handleEditAction(subItem)
            return
        }

        val fragment = ProfessionalJobAddingFragment()
        val bundle = Bundle()
        bundle.putString("insaatID", insaatId)
        bundle.putString("crLevel", "Template")
        bundle.putString("fPath", "") // fPath boş gidince sistem Şablon moduna geçer
        
        if (initialJobName != null) {
            bundle.putString("initialJobName", initialJobName)
        }

        fragment.arguments = bundle
        
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(R.id.frame, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        templatesListener?.let { templatesRef?.removeEventListener(it) }
        _binding = null
    }
}
