package com.sorodeveloper.insaatcebimde.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.SquarecardviewBinding

class EtapAdapter(
    private val etapListesi: List<String>,
    private val onClick: (String) -> Unit

) : RecyclerView.Adapter<EtapAdapter.EtapViewHolder>() {

    inner class EtapViewHolder(val binding: SquarecardviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EtapViewHolder {
        val binding = SquarecardviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EtapViewHolder(binding)
    }

    override fun getItemCount(): Int = etapListesi.size

    override fun onBindViewHolder(holder: EtapViewHolder, position: Int) {
        val etapAdi = etapListesi[position]

        holder.binding.tvTitle.text = etapAdi
        holder.binding.root.setOnClickListener { onClick(etapAdi) }
    }

}

