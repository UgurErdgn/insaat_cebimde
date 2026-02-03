package com.sorodeveloper.insaatcebimde.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.R
import com.sorodeveloper.insaatcebimde.databinding.MaincardviewBinding
import com.sorodeveloper.insaatcebimde.databinding.SquarecardviewBinding
import com.sorodeveloper.insaatcebimde.insaat
import com.sorodeveloper.insaatcebimde.insaatDetayActivity

class CustomAdapter(private val insaatistesi: List<insaat>, private  val mContext : Context) :
    RecyclerView.Adapter<CustomAdapter.ViewHolder>() {


    inner class ViewHolder(val binding: MaincardviewBinding) :
        RecyclerView.ViewHolder(binding.root)


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val binding = MaincardviewBinding.inflate(
            LayoutInflater.from(viewGroup.context),
            viewGroup,
            false
        )
        return ViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        viewHolder.binding.rcyinsaatadi.text = insaatistesi[position].insaatAdi
        viewHolder.itemView.setOnClickListener {
            val selectedIsletme = insaatistesi[position] // Tıklanan öğeyi al

            val detaygecis = Intent(mContext, insaatDetayActivity::class.java)
            detaygecis.putExtra("id" , selectedIsletme.serino)
            detaygecis.putExtra("insaatadi" , selectedIsletme.insaatAdi)
            mContext.startActivity(detaygecis)

        }

    }


    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = insaatistesi.size

}