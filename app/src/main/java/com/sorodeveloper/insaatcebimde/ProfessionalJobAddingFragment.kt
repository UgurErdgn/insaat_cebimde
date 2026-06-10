package com.sorodeveloper.insaatcebimde

import android.net.Uri
import android.util.Log
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.sorodeveloper.insaatcebimde.model.ImagePreviewDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.sorodeveloper.insaatcebimde.adapter.ProfessionalJobAdapter
import com.sorodeveloper.insaatcebimde.databinding.FragmentIsEkleBinding
import com.sorodeveloper.insaatcebimde.viewmodel.JobViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.sorodeveloper.insaatcebimde.viewmodel.SaveStatus


import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfessionalJobAddingFragment : Fragment() {

    private var _binding: FragmentIsEkleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JobViewModel by viewModels()
    private lateinit var jobAdapter: ProfessionalJobAdapter
    
    private var pendingCardIdForImage: String? = null

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingCardIdForImage?.let { cardId ->
                viewModel.addLocalImage(cardId, it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIsEkleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeState()
    }

    private fun setupUI() {
        val insaatId = arguments?.getString("insaatID") ?: return
        val mulkTuru = arguments?.getString("crLevel") ?: return

        // VM'e başlangıç verilerini ver
        viewModel.setInitialData(insaatId, mulkTuru)

        // Job Name etMainJobName input'unu dinle
        binding.etMainJobName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.updateJobName(binding.etMainJobName.text.toString())
            }
        }

        binding.btnGetKalems.setOnClickListener {
            viewModel.updateJobName(binding.etMainJobName.text.toString())
            viewModel.fetchSpinnerItems()
        }

        binding.btnAddSubItem.setOnClickListener {
            viewModel.addNewJobCard()
        }

        // Kütüphaneden gelen düzenleme veya kalem ekleme isteği varsa İş Adını önceden doldur
        val initialJobName = arguments?.getString("initialJobName")
        val initialCategory = arguments?.getString("initialCategory")
        val initialType = arguments?.getString("initialType")

        if (!initialJobName.isNullOrEmpty()) {
            binding.etMainJobName.setText(initialJobName)
            binding.etMainJobName.isEnabled = false // Kategori ismini kilitle
            viewModel.updateJobName(initialJobName)
            
            // Eğer düzenleme modundaysak (kategori ve tür varsa) kalemleri hemen getir
            if (!initialCategory.isNullOrEmpty() && !initialType.isNullOrEmpty()) {
                binding.etMainJobName.isEnabled = false // Kategori alanını kilitle
                binding.btnAddSubItem.visibility = View.GONE // Kalem ekle butonunu gizle
                viewModel.fetchSpinnerItems(initialCategory, initialType)
            } else {
                // Sadece kalem ekleme butonuyla gelindiyse (yeni iş), kullanıcıyı bir adet hazır boş kalemle karşıla
                // ve 'Yeni İş veya Tür' kutucuğu işaretli olsun.
                if (viewModel.uiState.value.jobCards.isEmpty()) {
                    viewModel.addNewJobCard(isCustom = true)
                }
            }
        }

        binding.btnSaveWholeJob.setOnClickListener {
            val fPath = arguments?.getString("fPath") ?: return@setOnClickListener
            val isEditing = !arguments?.getString("initialCategory").isNullOrEmpty()
            val mulkTuru = arguments?.getString("crLevel") ?: ""

            if (mulkTuru == "daireler") {
                // Mülk bazlı ekleme: Şablon Klonlama Akışı
                val editText = android.widget.EditText(requireContext())
                editText.hint = "Örn: Salonu Geniş Villa"
                
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Yeni Şablon Oluştur")
                    .setMessage("Bu mülke özel iş eklediğiniz için yeni bir şablon oluşturulacaktır. Bu şablon için bir isim giriniz:")
                    .setView(editText)
                    .setPositiveButton("Kaydet ve Klonla") { _, _ ->
                        val name = editText.text.toString().trim()
                        if (name.isNotEmpty()) {
                            viewModel.saveWholeJob(requireContext().contentResolver, fPath, newTemplateName = name)
                        } else {
                            Toast.makeText(requireContext(), "İsim boş olamaz", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("İptal", null)
                    .show()
            } else if (isEditing) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Şablon Güncelleme")
                    .setMessage("Mevcut kütüphane şablonunu değiştirmek üzeresiniz. Onaylıyor musunuz?")
                    .setPositiveButton("Evet") { _, _ ->
                        viewModel.saveWholeJob(requireContext().contentResolver, fPath)
                    }
                    .setNegativeButton("Hayır", null)
                    .show()
            } else {
                viewModel.saveWholeJob(requireContext().contentResolver, fPath)
            }
        }

        // Adapter initialize
        jobAdapter = ProfessionalJobAdapter(
            spinnerItems = emptyList(), // Observe ile güncellenecek
            onItemSelected = { cardId, item ->
                viewModel.updateCardSelection(cardId, item, insaatId, mulkTuru)
            },
            onAddMaterial = { cardId ->
                viewModel.addMaterial(cardId)
            },
            onMaterialDelete = { cardId, material ->
                viewModel.removeMaterial(cardId, material.id)
            },
            onMaterialUpdate = { cardId, material ->
                viewModel.updateMaterial(cardId, material)
            },
            onUploadClick = { cardId ->
                pendingCardIdForImage = cardId
                selectImageLauncher.launch("image/*")
            },
            onImageClick = { cardId, item, pos ->
                previewImage(cardId, item)
            },
            onDeleteCard = { cardId ->
                viewModel.removeJobCard(cardId)
            },
            onDeleteImage = { cardId, item ->
                viewModel.removeImage(cardId, item)
            },
            onToggleCustom = { cardId, isCustom ->
                viewModel.toggleCustomJob(cardId, isCustom)
            },
            onToggleEditTemplate = { cardId, isEdit ->
                viewModel.toggleEditTemplate(cardId, isEdit)
            },
            onCustomNameChanged = { cardId, name ->
                viewModel.updateCustomJobName(cardId, name)
            },
            onCustomTypeChanged = { cardId, type ->
                viewModel.updateCustomJobType(cardId, type)
            }
        )

        binding.rvKalemler.layoutManager = LinearLayoutManager(requireContext())
        binding.rvKalemler.adapter = jobAdapter
        binding.rvKalemler.itemAnimator = null
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Spinner listesini ayrı gözlemle
                launch {
                    viewModel.uiState
                        .map { it.availableSpinnerItems }
                        .distinctUntilChanged()
                        .collect { items ->
                            jobAdapter.updateSpinnerItems(items)
                        }
                }

                // Kartları ayrı gözlemle
                launch {
                    viewModel.uiState
                        .map { it.jobCards }
                        .distinctUntilChanged()
                        .collect { cards ->
                            jobAdapter.submitList(cards)
                        }
                }

                // Loading vs UI feedback
                launch {
                    viewModel.uiState
                        .map { it.isFetchingSpinnerItems }
                        .distinctUntilChanged()
                        .collect { isFetching ->
                            binding.btnGetKalems.isEnabled = !isFetching
                        }
                }

                launch {
                    viewModel.uiState
                        .map { it.saveStatus }
                        .distinctUntilChanged()
                        .collect { status ->
                            when (status) {
                                is SaveStatus.Loading -> {
                                    binding.btnSaveWholeJob.isEnabled = false
                                    // İsteğe bağlı: Loading indicator göster
                                }
                                is SaveStatus.Success -> {
                                    Toast.makeText(requireContext(), "İş başarıyla kaydedildi!", Toast.LENGTH_SHORT).show()
                                    parentFragmentManager.popBackStack()
                                }
                                is SaveStatus.Error -> {
                                    binding.btnSaveWholeJob.isEnabled = true
                                    Toast.makeText(requireContext(), "Hata: ${status.message}", Toast.LENGTH_LONG).show()
                                }
                                is SaveStatus.Idle -> {
                                    binding.btnSaveWholeJob.isEnabled = true
                                }
                            }
                        }
                }
            }
        }
    }

    private fun previewImage(cardId: String, item: Any) {
        val isLocal = item is Uri
        val uri = if (isLocal) item as Uri else Uri.parse(item as String)

        val isCustom = viewModel.uiState.value.jobCards.find { it.id == cardId }?.isCustom ?: false
        val canDelete = isLocal || isCustom

        ImagePreviewDialog(
            imageUri = uri,
            canDelete = canDelete,
            onDelete = {
                viewModel.removeImage(cardId, item)
            }
        ).show(childFragmentManager, "ImagePreviewDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
