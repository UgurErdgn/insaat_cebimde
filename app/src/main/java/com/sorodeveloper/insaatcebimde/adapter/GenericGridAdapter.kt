package com.sorodeveloper.insaatcebimde.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.R

class GenericGridAdapter(
    private val items: List<String>,
    private val onItemClick: (String) -> Unit,
    private val onDoubleClick: (String) -> Unit,
) : RecyclerView.Adapter<GenericGridAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var clickCount = 0
        private val handler = Handler(Looper.getMainLooper())
        val text: TextView = itemView.findViewById(R.id.tvTitle)

        init {
            itemView.setOnClickListener {
                clickCount++

                handler.postDelayed({
                    if (clickCount == 1) {
                        onItemClick(items[adapterPosition])
                    } else if (clickCount == 2) {
                        onDoubleClick(items[adapterPosition])
                    }
                    clickCount = 0
                }, 300) // 300 ms: çift tık algılama süresi
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.squarecardview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.text.text = item
       /* holder.itemView.setOnClickListener {
            onItemClick(item)
        }*/
    }

    override fun getItemCount(): Int = items.size
}

