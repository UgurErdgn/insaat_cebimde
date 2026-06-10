package com.sorodeveloper.insaatcebimde.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.sorodeveloper.insaatcebimde.R
import com.sorodeveloper.insaatcebimde.auth.AuthActivity
import com.sorodeveloper.insaatcebimde.auth.AuthViewModel
import com.sorodeveloper.insaatcebimde.databinding.FragmentProfileBinding
import kotlinx.coroutines.flow.collectLatest

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            authViewModel.currentUser.collectLatest { user ->
                user?.let {
                    binding.tvUserName.text = it.name
                    binding.tvUserEmail.text = it.email
                    binding.tvInviteId.text = it.publicInviteId
                }
            }
        }

        binding.btnCopyInviteId.setOnClickListener {
            val inviteId = binding.tvInviteId.text.toString()
            if (inviteId.isNotEmpty() && !inviteId.contains("Seçiniz")) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = android.content.ClipData.newPlainText("Public Invite ID", inviteId)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Davet ID kopyalandı", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnInvites.setOnClickListener {
            // InvitesFragment will be created next
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, InvitesFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
