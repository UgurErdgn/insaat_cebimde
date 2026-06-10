package com.sorodeveloper.insaatcebimde.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.ItemJobTemplateBinding

data class JobTemplateModel(
    val branch: String,
    val category: String,
    val type: String,
    val materialCount: Int,
    val drawingCount: Int
)

class JobTemplateAdapter(
    private val templates: List<JobTemplateModel>,
    private val onEditClicked: (JobTemplateModel) -> Unit
) : RecyclerView.Adapter<JobTemplateAdapter.TemplateViewHolder>() {

    inner class TemplateViewHolder(val binding: ItemJobTemplateBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemJobTemplateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TemplateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        val item = templates[position]
        
        holder.binding.tvTemplatePath.text = "${item.branch} / ${item.category} / ${item.type}"
        holder.binding.tvMaterialCount.text = "${item.materialCount} Malzeme"
        holder.binding.tvDrawingCount.text = "${item.drawingCount} Çizim"

        holder.binding.btnEditTemplate.setOnClickListener {
            onEditClicked(item)
        }
    }

    override fun getItemCount(): Int = templates.size
}
