package com.sorodeveloper.insaatcebimde.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.SquarecardviewBinding

class DaireAdapter( private val daireListesi: List<String>,
                    private val onClick: (String) -> Unit

) : RecyclerView.Adapter<DaireAdapter.DaireViewHolder>() {

    inner class DaireViewHolder(val binding: SquarecardviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DaireViewHolder {
        val binding = SquarecardviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DaireViewHolder(binding)
    }

    override fun getItemCount(): Int = daireListesi.size

    override fun onBindViewHolder(holder: DaireViewHolder, position: Int) {
        val daireAdi = daireListesi[position]

        holder.binding.tvTitle.text = daireAdi
        holder.binding.root.setOnClickListener { onClick(daireAdi) }
    }

}