package com.sorodeveloper.insaatcebimde.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.ItemMaterialsBinding
import com.sorodeveloper.insaatcebimde.viewmodel.ProfessionalMaterial

class ProfessionalMaterialAdapter(
    private val onMaterialDelete: (ProfessionalMaterial) -> Unit,
    private val onMaterialUpdate: (ProfessionalMaterial) -> Unit
) : ListAdapter<ProfessionalMaterial, ProfessionalMaterialAdapter.MaterialViewHolder>(MaterialDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        val binding = ItemMaterialsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MaterialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        val item = getItem(position)
        android.util.Log.d("PERF_LOG", "MaterialAdapter: onBindViewHolder for position: $position, id: ${item.id}")
        holder.bind(item)
    }

    inner class MaterialViewHolder(private val binding: ItemMaterialsBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.turAdi.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val currentPos = bindingAdapterPosition
                    if (currentPos != RecyclerView.NO_POSITION) {
                        val item = getItem(currentPos)
                        val currentText = s?.toString() ?: ""
                        if (!item.isReadOnly && currentText != item.name) {
                            onMaterialUpdate(item.copy(name = currentText))
                        }
                    }
                }
            })

            binding.turAdedi.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val currentPos = bindingAdapterPosition
                    if (currentPos != RecyclerView.NO_POSITION) {
                        val item = getItem(currentPos)
                        val currentText = s?.toString() ?: ""
                        if (!item.isReadOnly && currentText != item.amount) {
                            onMaterialUpdate(item.copy(amount = currentText))
                        }
                    }
                }
            })

            binding.deleteButton.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onMaterialDelete(getItem(currentPos))
                }
            }
        }

        fun bind(item: ProfessionalMaterial) {
            android.util.Log.d("PERF_LOG", "MaterialAdapter: bind() called for id: ${item.id}, name: ${item.name}")
            // Infinite loop ve fokus kaybını önlemek için Focus-Aware Binding:
            if (binding.turAdi.text.toString() != item.name && !binding.turAdi.hasFocus()) {
                binding.turAdi.setText(item.name)
                binding.turAdi.setSelection(binding.turAdi.text?.length ?: 0)
            }
            if (binding.turAdedi.text.toString() != item.amount && !binding.turAdedi.hasFocus()) {
                binding.turAdedi.setText(item.amount)
                binding.turAdedi.setSelection(binding.turAdedi.text?.length ?: 0)
            }

            val isEnabled = !item.isReadOnly
            binding.turAdi.isEnabled = isEnabled
            binding.turAdedi.isEnabled = isEnabled
            binding.deleteButton.visibility = if (isEnabled) View.VISIBLE else View.GONE
        }
    }

    class MaterialDiffCallback : DiffUtil.ItemCallback<ProfessionalMaterial>() {
        override fun areItemsTheSame(oldItem: ProfessionalMaterial, newItem: ProfessionalMaterial) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ProfessionalMaterial, newItem: ProfessionalMaterial) = oldItem == newItem
    }
}
