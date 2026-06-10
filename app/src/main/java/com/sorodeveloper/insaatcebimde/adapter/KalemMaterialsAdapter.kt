package com.sorodeveloper.insaatcebimde.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.MaterialItem
import com.sorodeveloper.insaatcebimde.databinding.ItemMaterialsBinding

class KalemMaterialsAdapter(
    private val materials: MutableList<MaterialItem>
) : RecyclerView.Adapter<KalemMaterialsAdapter.MaterialViewHolder>() {

    inner class MaterialViewHolder(val binding: ItemMaterialsBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        val binding = ItemMaterialsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MaterialViewHolder(binding)
    }

    override fun getItemCount(): Int = materials.size

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {

        val item = materials[position]

        holder.binding.turAdi.setText(item.name)
        holder.binding.turAdedi.setText(item.quantity)

        holder.binding.turAdi.doAfterTextChanged {
            item.name = it.toString()
        }

        holder.binding.turAdedi.doAfterTextChanged {
            item.quantity = it.toString()
        }

        holder.binding.deleteButton.setOnClickListener {
            materials.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun addItem() {
        materials.add(0, MaterialItem())   // 🔥 0. index'e ekle
        notifyItemInserted(0)
    }

    fun refresh() {
        notifyDataSetChanged()
    }
}