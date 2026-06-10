package com.sorodeveloper.insaatcebimde.adapter

import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UploadedImagesAdapter(
    private val images: MutableList<String>,
    private val onImageClick: (url: String) -> Unit
) : RecyclerView.Adapter<UploadedImagesAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val imageView: ImageView) :
        RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {

        val screenWidth = parent.resources.displayMetrics.widthPixels
        val imageSize = screenWidth / 4

        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(imageSize, imageSize)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }
        return ImageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val url = images[position]
        holder.imageView.setOnClickListener {
            onImageClick(url)
        }
        Glide.with(holder.imageView.context)
            .load(url)
            .into(holder.imageView)
    }

    override fun getItemCount() = images.size

    fun updateList(newList: List<String>) {
        images.clear()
        images.addAll(newList)
        notifyDataSetChanged()
    }
}