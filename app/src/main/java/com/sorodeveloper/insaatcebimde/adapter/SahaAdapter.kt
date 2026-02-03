package com.sorodeveloper.insaatcebimde.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.SquarecardviewBinding

class SahaAdapter(
    private val sahaListesi: List<String>,
    private val onClick: (String) -> Unit

) : RecyclerView.Adapter<SahaAdapter.SahaViewHolder>() {

    inner class SahaViewHolder(val binding: SquarecardviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SahaViewHolder {
        val binding = SquarecardviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SahaViewHolder(binding)
    }

    override fun getItemCount(): Int = sahaListesi.size

    override fun onBindViewHolder(holder: SahaViewHolder, position: Int) {
        val sahaAdi = sahaListesi[position]

        holder.binding.tvTitle.text = sahaAdi
        holder.binding.root.setOnClickListener { onClick(sahaAdi) }

    }
}