package com.sorodeveloper.insaatcebimde.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sorodeveloper.insaatcebimde.R

class InlineGalleryAdapter(
    private val images: MutableList<Any>,
    private val onDelete: (Any) -> Unit
) : RecyclerView.Adapter<InlineGalleryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inline_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = images[position]
        
        Glide.with(holder.itemView.context)
            .load(item)
            .into(holder.ivImage)

        holder.btnDelete.setOnClickListener {
            val p = images.indexOf(item)
            if (p != -1) {
                images.removeAt(p)
                notifyItemRemoved(p)
                onDelete(item)
            }
        }
    }

    override fun getItemCount() = images.size

    fun addImage(uri: Uri) {
        images.add(0, uri)
        notifyItemInserted(0)
    }
}
