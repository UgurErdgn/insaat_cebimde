package com.sorodeveloper.insaatcebimde.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sorodeveloper.insaatcebimde.adapter.InviteAdapter
import com.sorodeveloper.insaatcebimde.databinding.FragmentInvitesBinding
import com.sorodeveloper.insaatcebimde.model.Invite
import com.sorodeveloper.insaatcebimde.model.MatrixPermission
import com.sorodeveloper.insaatcebimde.model.User

class InvitesFragment : Fragment() {

    private var _binding: FragmentInvitesBinding? = null
    private val binding get() = _binding!!
    
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val invitesList = mutableListOf<Invite>()
    private lateinit var adapter: InviteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInvitesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        fetchInvites()
    }

    private fun setupRecyclerView() {
        adapter = InviteAdapter(
            invites = invitesList,
            onAccept = { invite -> acceptInvite(invite) },
            onReject = { invite -> rejectInvite(invite) }
        )
        binding.rvInvites.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInvites.adapter = adapter
    }

    private fun fetchInvites() {
        val uid = auth.currentUser?.uid ?: return
        val invitesRef = database.getReference("users").child(uid).child("invites")

        invitesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                invitesList.clear()
                for (inviteSnapshot in snapshot.children) {
                    val invite = inviteSnapshot.getValue(Invite::class.java)
                    if (invite != null && invite.status == "pending") {
                        invitesList.add(invite)
                    }
                }
                // Sort by createdAt descending (newest first)
                invitesList.sortByDescending { it.createdAt }
                adapter.notifyDataSetChanged()

                if (invitesList.isEmpty()) {
                    binding.tvEmptyInvites.visibility = View.VISIBLE
                    binding.rvInvites.visibility = View.GONE
                } else {
                    binding.tvEmptyInvites.visibility = View.GONE
                    binding.rvInvites.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Davetiyeler yüklenemedi", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun acceptInvite(invite: Invite) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java) ?: return
                
                // Construct new permission
                val newPermission = MatrixPermission(
                    locationPath = invite.locationPath,
                    jobPath = invite.jobPath,
                    canDelegate = invite.canDelegate
                )

                val currentPermissions = user.projectPermissions[invite.insaatId] ?: emptyList()
                val newList = currentPermissions + newPermission

                val updates = mapOf<String, Any>(
                    "projectPermissions/${invite.insaatId}" to newList,
                    "invites/${invite.inviteId}/status" to "accepted"
                )

                userRef.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Davetiye kabul edildi ve yetki tanımlandı", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun rejectInvite(invite: Invite) {
        val uid = auth.currentUser?.uid ?: return
        database.getReference("users").child(uid).child("invites").child(invite.inviteId)
            .child("status").setValue("rejected")
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Davetiye reddedildi", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
