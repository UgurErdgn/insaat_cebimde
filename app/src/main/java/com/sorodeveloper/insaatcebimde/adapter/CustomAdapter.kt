package com.sorodeveloper.insaatcebimde.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.R
import com.sorodeveloper.insaatcebimde.databinding.CardviewInsaatlarBinding
import com.sorodeveloper.insaatcebimde.insaat
import com.sorodeveloper.insaatcebimde.insaatIslerActivity

class CustomAdapter(private val insaatistesi: List<insaat>, private  val mContext : Context) :
    RecyclerView.Adapter<CustomAdapter.ViewHolder>() {


    inner class ViewHolder(val binding: CardviewInsaatlarBinding) :
        RecyclerView.ViewHolder(binding.root)


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val binding = CardviewInsaatlarBinding.inflate(
            LayoutInflater.from(viewGroup.context),
            viewGroup,
            false
        )
        return ViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        viewHolder.binding.rcyinsaatadi.text = insaatistesi[position].insaatAdi
        val selectedInsaat = insaatistesi[position] // Tıklanan öğeyi al

        /*  viewHolder.itemView.setOnClickListener {

            val detaygecis = Intent(mContext, insaatDetayActivity::class.java)
            detaygecis.putExtra("id" , selectedIsletme.serino)
            detaygecis.putExtra("insaatadi" , selectedIsletme.insaatAdi)
            mContext.startActivity(detaygecis)

        }*/

        viewHolder.binding.layoutOne.setOnClickListener {
            val intent = Intent(mContext, insaatIslerActivity::class.java)
            intent.putExtra("insaatID", selectedInsaat.serino)
            intent.putExtra("insaatAdi", selectedInsaat.insaatAdi)
            mContext.startActivity(intent)
        }

        viewHolder.binding.layoutTwo.setOnClickListener {
           /* val intent = Intent(mContext, insaatKusBakisiActivity::class.java)
            intent.putExtra("id", selectedInsaat.serino)
            intent.putExtra("insaatadi", selectedInsaat.insaatAdi)
            mContext.startActivity(intent)*/
        }

       /* viewHolder.binding.layoutThree.setOnClickListener {
            val intent = Intent(mContext, ActivityThree::class.java)
            intent.putExtra("id", selectedInsaat.serino)
            intent.putExtra("insaatadi", selectedInsaat.insaatAdi)
            mContext.startActivity(intent)
        }

        viewHolder.binding.layoutFour.setOnClickListener {
            val intent = Intent(mContext, ActivityFour::class.java)
            intent.putExtra("id", selectedInsaat.serino)
            intent.putExtra("insaatadi", selectedInsaat.insaatAdi)
            mContext.startActivity(intent)
        }*/

    }

    class InsaatDiffCallback : DiffUtil.ItemCallback<insaat>() {
        override fun areItemsTheSame(oldItem: insaat, newItem: insaat): Boolean {
            return oldItem.serino == newItem.serino // ID kontrolü
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: insaat, newItem: insaat): Boolean {
            return oldItem == newItem // İçerik kontrolü (data class ise)
        }
    }


    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = insaatistesi.size

}