package com.sorodeveloper.insaatcebimde.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.ItemKalemCheckboxBinding
import com.sorodeveloper.insaatcebimde.model.JobNode

class KalemCheckboxAdapter(
    private var items: List<JobNode>
) : RecyclerView.Adapter<KalemCheckboxAdapter.ItemViewHolder>() {

    val checkedItems = mutableSetOf<JobNode>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemKalemCheckboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.binding.cbKalem.text = item.name
        
        // Selection logic
        holder.binding.cbKalem.setOnCheckedChangeListener(null)
        holder.binding.cbKalem.isChecked = checkedItems.contains(item)
        holder.binding.cbKalem.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) checkedItems.add(item) else checkedItems.remove(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<JobNode>) {
        items = newItems
        checkedItems.clear()
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(val binding: ItemKalemCheckboxBinding) : RecyclerView.ViewHolder(binding.root)
}
