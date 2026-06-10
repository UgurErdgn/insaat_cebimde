package com.sorodeveloper.insaatcebimde.adapter

import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProfessionalGalleryAdapter(
    private val onImageClick: (Any, Int) -> Unit,
    private val onDeleteClick: (Any) -> Unit
) : ListAdapter<Any, ProfessionalGalleryAdapter.GalleryViewHolder>(GalleryDiffCallback()) {

    private var canDelete: Boolean = false

    fun setCanDelete(enabled: Boolean) {
        if (this.canDelete != enabled) {
            this.canDelete = enabled
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val screenWidth = parent.resources.displayMetrics.widthPixels
        val imageSize = screenWidth / 4

        val frameLayout = android.widget.FrameLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(imageSize, imageSize)
        }

        val imageView = ImageView(parent.context).apply {
            id = android.view.View.generateViewId()
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(4, 4, 4, 4)
        }

        val deleteIcon = ImageView(parent.context).apply {
            id = android.view.View.generateViewId()
            val size = (24 * parent.context.resources.displayMetrics.density).toInt()
            layoutParams = android.widget.FrameLayout.LayoutParams(size, size).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                setMargins(8, 8, 8, 8)
            }
            setImageResource(android.R.drawable.ic_menu_delete)
            imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
            setBackgroundResource(android.R.drawable.presence_offline) // Small circular bg
        }

        frameLayout.addView(imageView)
        frameLayout.addView(deleteIcon)

        return GalleryViewHolder(frameLayout, imageView, deleteIcon)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val item = getItem(position)
        android.util.Log.d("PERF_LOG", "GalleryAdapter: onBindViewHolder for position: $position")
        Glide.with(holder.itemView.context)
            .load(item)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.imageView)

        val isDeletable = item is Uri || canDelete
        holder.deleteIcon.visibility = if (isDeletable) android.view.View.VISIBLE else android.view.View.GONE

        holder.imageView.setOnClickListener {
            onImageClick(item, holder.bindingAdapterPosition)
        }

        holder.deleteIcon.setOnClickListener {
            onDeleteClick(item)
        }
    }

    inner class GalleryViewHolder(
        val frameLayout: android.view.View,
        val imageView: ImageView,
        val deleteIcon: ImageView
    ) : RecyclerView.ViewHolder(frameLayout)

    class GalleryDiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return oldItem == newItem
        }
    }
}
