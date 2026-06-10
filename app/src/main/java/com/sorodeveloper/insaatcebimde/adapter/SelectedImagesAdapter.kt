package com.sorodeveloper.insaatcebimde.adapter

import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import androidx.core.net.toUri

class SelectedImagesAdapter(
    private val imageList: MutableList<Uri>,
    private val onImageClick: (uri: Uri, position: Int) -> Unit
) : RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val imageView: ImageView) :
        RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {

        val screenWidth = parent.resources.displayMetrics.widthPixels
        val imageSize = screenWidth / 4

        val img = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(imageSize, imageSize)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }

        return ImageViewHolder(img)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {

        val item = imageList[position]

        holder.imageView.setOnClickListener {
            onImageClick(item, position) // 🔥 String değil direkt Uri
        }

        Glide.with(holder.imageView.context)
            .load(item)
            .into(holder.imageView)
    }

    override fun getItemCount() = imageList.size

    fun updateList(newList: List<Uri>) {
        imageList.clear()
        imageList.addAll(newList)
        notifyDataSetChanged()
    }

    // 🔥 Profesyonel silme
    fun removeAt(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            imageList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}