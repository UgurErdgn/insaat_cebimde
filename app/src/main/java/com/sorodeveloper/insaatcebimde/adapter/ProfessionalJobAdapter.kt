package com.sorodeveloper.insaatcebimde.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sorodeveloper.insaatcebimde.model.ImagePreviewDialog
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.CardviewKalemEklemeBinding
import com.sorodeveloper.insaatcebimde.model.SpinnerItem
import com.sorodeveloper.insaatcebimde.viewmodel.ProfessionalJobState
import com.sorodeveloper.insaatcebimde.viewmodel.ProfessionalMaterial

class ProfessionalJobAdapter(
    private var spinnerItems: List<SpinnerItem>,
    private val onItemSelected: (String, SpinnerItem) -> Unit,
    private val onAddMaterial: (String) -> Unit,
    private val onMaterialDelete: (String, ProfessionalMaterial) -> Unit,
    private val onMaterialUpdate: (String, ProfessionalMaterial) -> Unit,
    private val onUploadClick: (String) -> Unit,
    private val onImageClick: (String, Any, Int) -> Unit,
    private val onDeleteCard: (String) -> Unit,
    private val onDeleteImage: (String, Any) -> Unit,
    private val onToggleCustom: (String, Boolean) -> Unit,
    private val onToggleEditTemplate: (String, Boolean) -> Unit,
    private val onCustomNameChanged: (String, String) -> Unit,
    private val onCustomTypeChanged: (String, String) -> Unit
) : ListAdapter<ProfessionalJobState, ProfessionalJobAdapter.JobViewHolder>(JobDiffCallback()) {

    fun updateSpinnerItems(newList: List<SpinnerItem>) {
        if (this.spinnerItems != newList) {
            this.spinnerItems = newList
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = CardviewKalemEklemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val item = getItem(position)
        Log.d("PERF_LOG", "JobAdapter: onBindViewHolder (FULL) for position: $position, id: ${item.id}")
        holder.bind(item)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val item = getItem(position)
            Log.d("PERF_LOG", "JobAdapter: onBindViewHolder (PARTIAL) for position: $position, id: ${item.id}, payloads: $payloads")
            holder.partialBind(item, payloads)
        }
    }

    inner class JobViewHolder(private val binding: CardviewKalemEklemeBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val galleryAdapter = ProfessionalGalleryAdapter(
            onImageClick = { item, pos ->
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onImageClick(getItem(currentPos).id, item, pos)
                }
            },
            onDeleteClick = { item ->
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onDeleteImage(getItem(currentPos).id, item)
                }
            }
        )
        
        private val materialAdapter = ProfessionalMaterialAdapter(
            onMaterialDelete = { material ->
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onMaterialDelete(getItem(currentPos).id, material)
                }
            },
            onMaterialUpdate = { material ->
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onMaterialUpdate(getItem(currentPos).id, material)
                }
            }
        )

        init {
            binding.rvImages.layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
            binding.rvImages.adapter = galleryAdapter
            
            binding.rvMaterials.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvMaterials.adapter = materialAdapter
            binding.rvMaterials.itemAnimator = null

            binding.btnAddSubItem.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onAddMaterial(getItem(currentPos).id)
                }
            }
            binding.btnUploadImage.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onUploadClick(getItem(currentPos).id)
                }
            }

            binding.etkalemAdi.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val currentPos = bindingAdapterPosition
                    if (currentPos != RecyclerView.NO_POSITION) {
                        val item = getItem(currentPos)
                        val newText = s?.toString() ?: ""
                        if ((item.isCustom || item.isEditMode) && item.customJobName != newText) {
                            onCustomNameChanged(item.id, newText)
                        }
                    }
                }
            })

            binding.etTurAdi.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val currentPos = bindingAdapterPosition
                    if (currentPos != RecyclerView.NO_POSITION) {
                        val item = getItem(currentPos)
                        val newText = s?.toString() ?: ""
                        if ((item.isCustom || item.isEditMode) && item.customJobType != newText) {
                            onCustomTypeChanged(item.id, newText)
                        }
                    }
                }
            })

            binding.btnDeleteCard.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onDeleteCard(getItem(currentPos).id)
                }
            }

            binding.cbEditTemplate.setOnCheckedChangeListener { _, isChecked ->
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    val item = getItem(currentPos)
                    if (item.isEditMode != isChecked) {
                        onToggleEditTemplate(item.id, isChecked)
                    }
                }
            }
        }

        fun bind(state: ProfessionalJobState) {
            setupCustomMode(state)
            setupSpinner(state)
            setupSelection(state)
            updateSubLists(state)
        }

        fun partialBind(state: ProfessionalJobState, payloads: List<Any>) {
            val diffs = payloads.flatMap { if (it is Set<*>) it else emptySet<Any>() }
            
            if (diffs.contains("isCustom") || diffs.contains("isEditMode") || diffs.contains("isModeLocked")) {
                setupCustomMode(state)
                updateSubLists(state)
            }
            
            if (diffs.contains("selection") || diffs.contains("isCustom") || diffs.contains("isEditMode")) {
                setupSelection(state)
            }
            
            if (diffs.contains("customName") && (state.isCustom || state.isEditMode)) {
                if (binding.etkalemAdi.text.toString() != state.customJobName && !binding.etkalemAdi.hasFocus()) {
                    binding.etkalemAdi.setText(state.customJobName)
                    binding.etkalemAdi.setSelection(binding.etkalemAdi.text?.length ?: 0)
                }
            }
            
            if (diffs.contains("customType") && (state.isCustom || state.isEditMode)) {
                if (binding.etTurAdi.text.toString() != state.customJobType && !binding.etTurAdi.hasFocus()) {
                    binding.etTurAdi.setText(state.customJobType)
                    binding.etTurAdi.setSelection(binding.etTurAdi.text?.length ?: 0)
                }
            }
            
            if (diffs.contains("materials") || diffs.contains("images")) {
                updateSubLists(state)
            }
        }

        private fun setupCustomMode(state: ProfessionalJobState) {
            // cbIsCustom sync
            binding.cbIsCustom.setOnCheckedChangeListener(null)
            binding.cbIsCustom.isChecked = state.isCustom
            binding.cbIsCustom.setOnCheckedChangeListener { _, isChecked ->
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    if (state.isCustom != isChecked) {
                        onToggleCustom(getItem(currentPos).id, isChecked)
                    }
                }
            }

            // cbEditTemplate sync
            binding.cbEditTemplate.setOnCheckedChangeListener(null)
            binding.cbEditTemplate.isChecked = state.isEditMode
            binding.cbEditTemplate.setOnCheckedChangeListener { _, isChecked ->
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    if (state.isEditMode != isChecked) {
                        onToggleEditTemplate(getItem(currentPos).id, isChecked)
                    }
                }
            }

            // Mod Kilidi Durumu (isModeLocked ise etkileşimi kapat)
            val isLocked = state.isModeLocked
            binding.cbIsCustom.isEnabled = !isLocked
            binding.cbEditTemplate.isEnabled = !isLocked

            // UI Visibility (Custom veya Edit mode durumunda EditText'leri göster, Spinner'ı gizle)
            val showEditTexts = state.isCustom || state.isEditMode
            if (showEditTexts) {
                if (binding.tilJobName.visibility != View.VISIBLE) binding.tilJobName.visibility = View.VISIBLE
                if (binding.tilJobType.visibility != View.VISIBLE) binding.tilJobType.visibility = View.VISIBLE
                if (binding.spinner2.visibility != View.GONE) binding.spinner2.visibility = View.GONE

                // Karşılıklı Görünürlük (Biri seçiliyken diğeri gizlensin - Kilitli değilse)
                if (!isLocked) {
                    binding.cbIsCustom.visibility = if (state.isCustom) View.VISIBLE else View.GONE
                    binding.cbEditTemplate.visibility = if (state.isEditMode) View.VISIBLE else View.GONE
                } else {
                    // Kilitliyse sadece aktif olanı göster (EditMode)
                    binding.cbIsCustom.visibility = View.GONE
                    binding.cbEditTemplate.visibility = View.VISIBLE
                }
                
                if (binding.etkalemAdi.text.toString() != state.customJobName && !binding.etkalemAdi.hasFocus()) {
                    binding.etkalemAdi.setText(state.customJobName)
                    binding.etkalemAdi.setSelection(binding.etkalemAdi.text?.length ?: 0)
                }
                if (binding.etTurAdi.text.toString() != state.customJobType && !binding.etTurAdi.hasFocus()) {
                    binding.etTurAdi.setText(state.customJobType)
                    binding.etTurAdi.setSelection(binding.etTurAdi.text?.length ?: 0)
                }
            } else {
                if (binding.tilJobName.visibility != View.GONE) binding.tilJobName.visibility = View.GONE
                if (binding.tilJobType.visibility != View.GONE) binding.tilJobType.visibility = View.GONE
                if (binding.spinner2.visibility != View.VISIBLE) binding.spinner2.visibility = View.VISIBLE

                // İkisi de boşsa ikisini de göster (Tabii düzenlenecek bir şey varsa)
                binding.cbIsCustom.visibility = View.VISIBLE
                
                val canEdit = state.selectedItem != null || state.isModeLocked
                binding.cbEditTemplate.visibility = if (canEdit) View.VISIBLE else View.GONE
            }
        }

        private fun setupSpinner(state: ProfessionalJobState) {
            var spinnerAdapter = binding.spinner2.adapter as? ArrayAdapter<SpinnerItem>
            if (spinnerAdapter == null || spinnerAdapter.count != spinnerItems.size) {
                spinnerAdapter = ArrayAdapter(binding.root.context, android.R.layout.simple_spinner_dropdown_item, spinnerItems)
                binding.spinner2.adapter = spinnerAdapter
            }
        }

        private fun setupSelection(state: ProfessionalJobState) {
            binding.spinner2.onItemSelectedListener = null
            if (!state.isCustom && !state.isEditMode) {
                state.selectedItem?.let { selected ->
                    val index = spinnerItems.indexOf(selected)
                    if (index != -1 && binding.spinner2.selectedItemPosition != index) {
                        binding.spinner2.setSelection(index, false)
                    }
                }

                binding.spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                        val currentPos = bindingAdapterPosition
                        if (currentPos != RecyclerView.NO_POSITION) {
                            val selected = (binding.spinner2.adapter as ArrayAdapter<SpinnerItem>).getItem(pos) ?: return
                            if (state.selectedItem != selected) {
                                onItemSelected(getItem(currentPos).id, selected)
                            }
                        }
                    }
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                }
            }
        }

        private fun updateSubLists(state: ProfessionalJobState) {
            val allImages = state.remoteImages + state.localImages
            galleryAdapter.setCanDelete(state.isCustom || state.isEditMode)
            galleryAdapter.submitList(allImages)
            materialAdapter.submitList(state.materials)
        }
    }

    class JobDiffCallback : DiffUtil.ItemCallback<ProfessionalJobState>() {
        override fun areItemsTheSame(oldItem: ProfessionalJobState, newItem: ProfessionalJobState) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ProfessionalJobState, newItem: ProfessionalJobState) = oldItem == newItem

        override fun getChangePayload(oldItem: ProfessionalJobState, newItem: ProfessionalJobState): Any? {
            val diffs = mutableSetOf<String>()
            if (oldItem.materials != newItem.materials) diffs.add("materials")
            if (oldItem.remoteImages != newItem.remoteImages || oldItem.localImages != newItem.localImages) diffs.add("images")
            if (oldItem.customJobName != newItem.customJobName) diffs.add("customName")
            if (oldItem.customJobType != newItem.customJobType) diffs.add("customType")
            if (oldItem.isCustom != newItem.isCustom) diffs.add("isCustom")
            if (oldItem.isEditMode != newItem.isEditMode) diffs.add("isEditMode")
            if (oldItem.isModeLocked != newItem.isModeLocked) diffs.add("isModeLocked")
            if (oldItem.selectedItem != newItem.selectedItem) diffs.add("selection")
            return if (diffs.isEmpty()) null else diffs
        }
    }
}
