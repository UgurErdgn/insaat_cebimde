package com.sorodeveloper.insaatcebimde.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.ItemProjectApplicationBinding
import com.sorodeveloper.insaatcebimde.model.ProjectApplication

class ApplicationAdapter(
    private val applicationList: List<ProjectApplication>,
    private val onApproveClicked: (ProjectApplication) -> Unit,
    private val onRejectClicked: (ProjectApplication) -> Unit
) : RecyclerView.Adapter<ApplicationAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemProjectApplicationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProjectApplicationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val application = applicationList[position]

        holder.binding.tvUserName.text = application.userName
        holder.binding.tvUserEmail.text = application.userEmail
        
        val phoneText = if (application.userPhone.isNotEmpty()) application.userPhone else "Telefon belirtilmemiş"
        holder.binding.tvUserPhone.text = phoneText

        holder.binding.btnApprove.setOnClickListener {
            onApproveClicked(application)
        }

        holder.binding.btnReject.setOnClickListener {
            onRejectClicked(application)
        }
    }

    override fun getItemCount(): Int = applicationList.size
}
