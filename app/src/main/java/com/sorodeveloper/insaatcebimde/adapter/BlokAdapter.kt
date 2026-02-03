package com.sorodeveloper.insaatcebimde.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.SquarecardviewBinding

class BlokAdapter( private val blokListesi: List<String>,
                   private val onClick: (String) -> Unit

) : RecyclerView.Adapter<BlokAdapter.BlokViewHolder>() {

    inner class BlokViewHolder(val binding: SquarecardviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlokViewHolder {
        val binding = SquarecardviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BlokViewHolder(binding)
    }

    override fun getItemCount(): Int = blokListesi.size

    override fun onBindViewHolder(holder: BlokViewHolder, position: Int) {
        val blokAdi = blokListesi[position]

        holder.binding.tvTitle.text = blokAdi
        holder.binding.root.setOnClickListener { onClick(blokAdi) }
    }

}