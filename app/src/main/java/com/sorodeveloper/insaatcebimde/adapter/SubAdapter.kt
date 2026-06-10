package com.sorodeveloper.insaatcebimde.adapter

import android.R
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayoutMediator
import com.sorodeveloper.insaatcebimde.databinding.SubItemBinding

class SubAdapter(
    private val subList: List<isKollari.SubItem>,
    private val isTemplateMode: Boolean = false,
    private val onEditClicked: ((item: isKollari.SubItem) -> Unit)? = null,
    private val onUploadImageClicked: ((isKollari.SubItem) -> Unit)? = null,
    private val onSaveClicked: ((item: isKollari.SubItem, category: String, type: String, materials: Map<String, String>) -> Unit)? = null,
    private val onPercentSelected: (isKollari.SubItem, String) -> Unit
) : RecyclerView.Adapter<SubAdapter.SubViewHolder>() {

    inner class SubViewHolder(val binding: SubItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubViewHolder {
        val binding = SubItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubViewHolder, position: Int) {
        val item = subList[position]
        val context = holder.itemView.context
        holder.binding.tvSubTitle.text = item.title
        if (isTemplateMode) {
            holder.binding.linearProgress.visibility = View.GONE
            holder.binding.textView21.visibility = View.GONE
            holder.binding.textView20.visibility = View.GONE
            holder.binding.layoutPercentPicker.visibility = View.GONE
            holder.binding.root.setCardBackgroundColor(android.graphics.Color.parseColor("#343A40")) // Şık bir şablon fümesi
            
            if (item.isEditing) {
                holder.binding.btnEditSubItem.text = "VAZGEÇ"
                holder.binding.btnEditSubItem.icon = null
                holder.binding.btnEditSubItem.setTextColor(android.graphics.Color.parseColor("#F44336")) // Vazgeç için kırmızı
            } else {
                holder.binding.btnEditSubItem.text = null
                holder.binding.btnEditSubItem.setIconResource(com.sorodeveloper.insaatcebimde.R.drawable.ic_edit)
                holder.binding.btnEditSubItem.setIconTint(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E91E63")))
            }

            holder.binding.btnEditSubItem.visibility = if (item.isTabOpen || item.isEditing) View.VISIBLE else View.GONE
            holder.binding.btnEditSubItem.setOnClickListener {
                onEditClicked?.invoke(item)
            }

            // 8dp vertical padding for SubItem in template mode
            val paddingPx = (8 * holder.itemView.resources.displayMetrics.density).toInt()
            holder.binding.root.setContentPadding(0, paddingPx, 0, paddingPx)
        } else {
            val prg = item.progress
            val adet = item.kacAdet
            val percent = prg.toInt() / adet.toInt()
            holder.binding.linearProgress.progress = percent
            holder.binding.textView21.text = "$percent%"
            
            holder.binding.linearProgress.visibility = View.VISIBLE
            holder.binding.textView21.visibility = View.VISIBLE
            holder.binding.textView20.visibility = View.VISIBLE
            holder.binding.layoutPercentPicker.visibility = (if (item.isEditable) View.VISIBLE else View.GONE)
            holder.binding.root.setCardBackgroundColor(android.graphics.Color.parseColor("#585C5F"))
            holder.binding.btnEditSubItem.visibility = View.GONE
        }


        val isVisible = if (item.isTabOpen && !item.isEditing) View.VISIBLE else View.GONE
        holder.binding.tabLayout.visibility = isVisible
        holder.binding.viewPager.visibility = isVisible

        // 🔥 DÜZENLEME MODU KONTROLÜ
        if (item.isEditing) {
            holder.binding.layoutEditor.visibility = View.VISIBLE
            holder.binding.linearLayout3.visibility = View.GONE
            holder.binding.textView20.visibility = View.GONE
            holder.binding.textView21.visibility = View.GONE
            holder.binding.linearProgress.visibility = View.GONE
            holder.binding.layoutPercentPicker.visibility = View.GONE
            holder.binding.iconCategory2.visibility = View.GONE
            
            setupEditor(holder, item)
        } else {
            holder.binding.layoutEditor.visibility = View.GONE
            holder.binding.iconCategory2.visibility = View.VISIBLE
            // existing trade mode logic handles other visibilities
        }

        // iconCategory2'yi (ok işareti) döndürme işlemi
        val rotationDegree = if (item.isTabOpen) 90f else 0f
        holder.binding.iconCategory2.animate()
            .rotation(rotationDegree)
            .setDuration(550) // 550ms yumuşak bir geçiş sağlar
            .start()

        toggleLayout(item.isTabOpen, holder.itemView, holder.binding.linearLayout3)

        if (item.isTabOpen) {
            val adapter = TabsPagerAdapter(context, item)
            holder.binding.viewPager.adapter = adapter
            TabLayoutMediator(holder.binding.tabLayout, holder.binding.viewPager) { tab, pos ->
                tab.text = when (pos) {
                    0 -> "Genel"
                    1 -> "Detay"
                    else -> "Ekstra"
                }
            }.attach()
        }

        // Yüzde seçici görünür mü? (Eğer şablon modu değilse ve düzenlenebilirse)
        if (!isTemplateMode) {
            holder.binding.layoutPercentPicker.visibility =
                if (item.isEditable) View.VISIBLE else View.GONE
        }

        // Önceden kayıtlı yüzde varsa UI’ya yansıt
        item.progress.let { percent ->
            val button = holder.binding.toggleGroup.children
                .filterIsInstance<MaterialButton>()
                .firstOrNull { it.text.toString() == percent }
            button?.let { holder.binding.toggleGroup.check(it.id) }
        }

        holder.binding.toggleGroup.clearOnButtonCheckedListeners()
        holder.binding.toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val button = group.findViewById<MaterialButton>(checkedId)
                val percent = button.text.toString()

                item.progress = percent
                onPercentSelected(item, percent)
            }
        }


        holder.binding.root.setOnClickListener {
            if (item.isEditing) return@setOnClickListener // Düzenleme modunda kart kapanmasın
            
            val currentStatus = item.isTabOpen

            // Diğer her şeyi kapat, tıklananın durumunu tersine çevir
            subList.forEach { it.isTabOpen = false }
            item.isTabOpen = !currentStatus
            Toast.makeText(context, item.firebasePath, Toast.LENGTH_SHORT).show()

            // Sadece tüm listeyi yenilemek yerine daha performanslı olabilir ama
            // tüm item'ların durumunu değiştirdiğimiz için şimdilik kalsın.
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = subList.size

    private fun setupEditor(holder: SubViewHolder, item: isKollari.SubItem) {
        val binding = holder.binding
        val context = holder.itemView.context

        // Başlıkları ata
        binding.etEditorCategory.setText(item.category)
        binding.etEditorType.setText(item.type)

        // --- RESİM LİSTESİ ---
        val allImages = (item.drawingUrls.toMutableList() as MutableList<Any>)
        allImages.addAll(item.localDrawingUris)
        
        val galleryAdapter = InlineGalleryAdapter(allImages) { deletedItem ->
            if (deletedItem is String) {
                item.drawingUrls.remove(deletedItem)
            } else if (deletedItem is android.net.Uri) {
                item.localDrawingUris.remove(deletedItem)
            }
        }
        binding.rvEditorImages.adapter = galleryAdapter

        binding.btnEditorUploadImage.setOnClickListener {
            onUploadImageClicked?.invoke(item)
        }

        // --- MALZEME LİSTESİ ---
        binding.editorMaterialContainer.removeAllViews()
        item.materials?.forEach { (name, amount) ->
            addMaterialRow(binding.editorMaterialContainer, name, amount)
        }

        binding.btnEditorAddMaterial.setOnClickListener {
            addMaterialRow(binding.editorMaterialContainer)
        }

        // --- KAYDET BUTONU ---
        binding.btnEditorSave.setOnClickListener {
            val finalCategory = binding.etEditorCategory.text.toString().trim()
            val finalType = binding.etEditorType.text.toString().trim()
            
            val finalMaterials = mutableMapOf<String, String>()
            for (i in 0 until binding.editorMaterialContainer.childCount) {
                val row = binding.editorMaterialContainer.getChildAt(i)
                val etName = row.findViewById<android.widget.EditText>(com.sorodeveloper.insaatcebimde.R.id.etMaterialType)
                val etAmount = row.findViewById<android.widget.EditText>(com.sorodeveloper.insaatcebimde.R.id.etMaterialAmount)
                
                val name = etName.text.toString().trim()
                val amount = etAmount.text.toString().trim()
                
                if (name.isNotEmpty() && amount.isNotEmpty()) {
                    finalMaterials[name] = amount
                }
            }

            // Kaydetme öncesi uyarı
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Şablonu Güncelle")
                .setMessage("Bu işlem tüm projelerdeki ilgili kalemleri etkileyecektir. Onaylıyor musunuz?")
                .setPositiveButton("Güncelle") { _, _ ->
                    onSaveClicked?.invoke(item, finalCategory, finalType, finalMaterials)
                }
                .setNegativeButton("İptal", null)
                .show()
        }
    }

    private fun addMaterialRow(container: LinearLayout, name: String = "", amount: String = "") {
        val inflater = LayoutInflater.from(container.context)
        val rowView = inflater.inflate(com.sorodeveloper.insaatcebimde.R.layout.item_malzeme_ekle_row, container, false)
        
        val etName = rowView.findViewById<android.widget.EditText>(com.sorodeveloper.insaatcebimde.R.id.etMaterialType)
        val etAmount = rowView.findViewById<android.widget.EditText>(com.sorodeveloper.insaatcebimde.R.id.etMaterialAmount)
        val btnDelete = rowView.findViewById<android.widget.ImageView>(com.sorodeveloper.insaatcebimde.R.id.btnDeleteMaterial)
        
        etName.setText(name)
        etAmount.setText(amount)
        
        btnDelete.setOnClickListener {
            container.removeView(rowView)
        }
        
        container.addView(rowView)
    }

    private fun toggleLayout(isExpanded: Boolean, view: View, layout: LinearLayout) {
        if (isExpanded) {
            layout.visibility = View.VISIBLE
            layout.alpha = 0f
            layout.translationY = -80f // Hafif yukarıdan başlasın
            layout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .start()
        } else {
            layout.animate()
                .alpha(0f)
                .translationY(0f)
                .setDuration(500)
                .withEndAction { layout.visibility = View.GONE }
                .start()
        }
    }
}
