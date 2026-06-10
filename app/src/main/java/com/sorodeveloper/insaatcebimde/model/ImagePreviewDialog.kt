package com.sorodeveloper.insaatcebimde.model

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.sorodeveloper.insaatcebimde.databinding.DialogImagePreviewBinding

class ImagePreviewDialog(
    private val imageUri: Uri,
    private val canDelete: Boolean,
    private val onDelete: (() -> Unit)?
) : DialogFragment() {

    private var _binding: DialogImagePreviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = Dialog(
            requireContext(),
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )

        _binding = DialogImagePreviewBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        Glide.with(requireContext())
            .load(imageUri)
            .into(binding.photoView)

        binding.photoView.maximumScale = 5f
        binding.photoView.mediumScale = 2f
        binding.photoView.minimumScale = 1f

        if (canDelete) {
            binding.btnDelete.visibility = View.VISIBLE
            binding.btnDelete.setOnClickListener {
                onDelete?.invoke()
                dismiss()
            }
        } else {
            binding.btnDelete.visibility = View.GONE
        }

        binding.photoView.setOnViewTapListener { _, _, _ ->
            dismiss()
        }

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}