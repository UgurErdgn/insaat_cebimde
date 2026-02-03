package com.sorodeveloper.insaatcebimde.adapter


import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.isBolumleri
import com.sorodeveloper.insaatcebimde.R
import com.sorodeveloper.insaatcebimde.insaatDetayActivity
import com.sorodeveloper.insaatcebimde.kalemDetayActivity

class InsaatDetayAdapter(
    private val liste: MutableList<isBolumleri>,
    private val mContext: Context,
    private val onBaslikClick: (String, Int) -> Unit
) : RecyclerView.Adapter<InsaatDetayAdapter.ViewHolder>() {

    companion object {
        private const val TYPE_BASLIK = 0
        private const val TYPE_ALT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (liste[position]) {
            is isBolumleri.Baslik -> TYPE_BASLIK
            is isBolumleri.Kalem -> TYPE_ALT
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.maincardview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = liste[position]
        val tv = holder.itemView.findViewById<TextView>(R.id.rcyinsaatadi)
        val card = holder.itemView as ViewGroup

        if (item is isBolumleri.Baslik) {
            tv.text = item.ad
            tv.textSize = 16f
            tv.setTypeface(null, Typeface.BOLD)

            val params = card.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(8.dp(), 0.dp(), 8.dp(), 4.dp())
            card.layoutParams = params

            holder.itemView.setOnClickListener {
                onBaslikClick(item.ad, position)
            }

        } else if (item is isBolumleri.Kalem) {
            tv.text = item.ad
            tv.textSize = 14f
            tv.setTypeface(null, Typeface.NORMAL)

            val params = card.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(12.dp(), 0.dp(), 64.dp(), 4.dp())
            card.layoutParams = params

            holder.itemView.setOnClickListener {
                val detaygecis = Intent(mContext, kalemDetayActivity::class.java)
                detaygecis.putExtra("kalemadi", item.ad)
                detaygecis.putExtra("bolumadi", item.bolumAdi)
                mContext.startActivity(detaygecis)
            }
        }
    }

    override fun getItemCount() = liste.size
}

fun Int.dp(): Int =
    (this * Resources.getSystem().displayMetrics.density).toInt()
