package com.sorodeveloper.insaatcebimde.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.ItemPendingPermissionBinding
import com.sorodeveloper.insaatcebimde.model.MatrixPermission

class PendingPermissionAdapter(
    private val permissions: MutableList<MatrixPermission>,
    private val onRemoveClicked: (Int) -> Unit
) : RecyclerView.Adapter<PendingPermissionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPendingPermissionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPendingPermissionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val perm = permissions[position]
        
        val locText = if (perm.locationPath.isEmpty()) "Tüm Saha" else perm.locationPath
        val jobText = if (perm.jobPath.isEmpty()) "Tüm İşler" else perm.jobPath
        
        holder.binding.tvLocJob.text = "Lokasyon: $locText | İş: $jobText"
        holder.binding.tvDelegation.text = "Delege Edebilir: " + if(perm.canDelegate) "Evet" else "Hayır"

        holder.binding.ivRemove.setOnClickListener {
            onRemoveClicked(position)
        }
    }

    override fun getItemCount(): Int = permissions.size
}
