package com.sorodeveloper.insaatcebimde.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.ItemUnitTypeBinding
import com.sorodeveloper.insaatcebimde.model.UnitType

class UnitTypeAdapter(
    private var list: List<UnitType>,
    private val onEditClick: (UnitType) -> Unit
) : RecyclerView.Adapter<UnitTypeAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemUnitTypeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUnitTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.tvUnitTypeName.text = item.name
        holder.binding.tvJobCount.text = "${item.jobIds.size} İş Şablonu"

        holder.binding.btnEditUnitType.setOnClickListener {
            onEditClick(item)
        }
        
        holder.binding.root.setOnClickListener {
            onEditClick(item)
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<UnitType>) {
        list = newList
        notifyDataSetChanged()
    }
}
