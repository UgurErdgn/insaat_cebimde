package com.sorodeveloper.insaatcebimde

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sorodeveloper.insaatcebimde.databinding.FragmentTemplateLibraryBinding

class TemplateLibraryFragment : Fragment() {

    private var _binding: FragmentTemplateLibraryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemplateLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var insaatId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        insaatId = arguments?.getString("insaatID") ?: ""

        val jobTemplatesFragment = JobTemplatesFragment().apply {
            arguments = Bundle().apply {
                putString("insaatID", insaatId)
            }
        }

        // Başlangıçta İş Şablonları sekmesini yükle
        childFragmentManager.beginTransaction()
            .replace(com.sorodeveloper.insaatcebimde.R.id.libraryFrame, jobTemplatesFragment)
            .commit()

        binding.tabLayoutLibrary.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                val selectedFragment = when (tab?.position) {
                    0 -> jobTemplatesFragment
                    1 -> UnitTemplatesFragment().apply {
                        arguments = Bundle().apply {
                            putString("insaatID", insaatId)
                        }
                    }
                    else -> jobTemplatesFragment
                }
                childFragmentManager.beginTransaction()
                    .replace(com.sorodeveloper.insaatcebimde.R.id.libraryFrame, selectedFragment)
                    .commit()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
