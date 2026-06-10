package com.sorodeveloper.insaatcebimde.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.ItemKalemCheckboxBinding
import isKollari

class PropertyJobSelectionAdapter(
    private var list: List<isKollari.SubItem>,
    private val initialSelectedIds: Set<String>
) : RecyclerView.Adapter<PropertyJobSelectionAdapter.ViewHolder>() {

    val selectedJobIds = mutableSetOf<String>().apply { addAll(initialSelectedIds) }

    inner class ViewHolder(val binding: ItemKalemCheckboxBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemKalemCheckboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.tvKalemName.text = "${item.trade} / ${item.title}"
        holder.binding.cbKalem.isChecked = selectedJobIds.contains(item.templateId)

        holder.binding.cbKalem.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedJobIds.add(item.templateId)
            } else {
                selectedJobIds.remove(item.templateId)
            }
        }
        
        holder.binding.root.setOnClickListener {
            holder.binding.cbKalem.toggle()
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<isKollari.SubItem>) {
        list = newList
        notifyDataSetChanged()
    }
}
