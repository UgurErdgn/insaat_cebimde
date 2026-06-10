package com.sorodeveloper.insaatcebimde.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorodeveloper.insaatcebimde.databinding.ItemInviteBinding
import com.sorodeveloper.insaatcebimde.model.Invite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InviteAdapter(
    private val invites: List<Invite>,
    private val onAccept: (Invite) -> Unit,
    private val onReject: (Invite) -> Unit
) : RecyclerView.Adapter<InviteAdapter.InviteViewHolder>() {

    class InviteViewHolder(val binding: ItemInviteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteViewHolder {
        val binding = ItemInviteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InviteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InviteViewHolder, position: Int) {
        val invite = invites[position]
        val binding = holder.binding

        binding.tvSenderName.text = invite.senderName
        binding.tvInsaatName.text = invite.insaatName
        binding.tvLocationPath.text = "Lokasyon: ${invite.locationPath.ifEmpty { "Tümü (Root)" }}"
        binding.tvJobPath.text = "İş/Kalem: ${invite.jobPath.ifEmpty { "Tümü (Root)" }}"
        binding.tvCanDelegate.text = "Yetki Devri: ${if (invite.canDelegate) "VAR" else "YOK"}"
        
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        binding.tvDate.text = sdf.format(Date(invite.createdAt))

        binding.btnAccept.setOnClickListener { onAccept(invite) }
        binding.btnReject.setOnClickListener { onReject(invite) }
    }

    override fun getItemCount() = invites.size
}
