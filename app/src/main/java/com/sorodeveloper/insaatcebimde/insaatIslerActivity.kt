package com.sorodeveloper.insaatcebimde

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.sorodeveloper.insaatcebimde.databinding.ActivityInsaatIslerBinding
import com.sorodeveloper.insaatcebimde.databinding.FragmentInsaatislerKollariBinding

class insaatIslerActivity : AppCompatActivity() {
      lateinit var binding: ActivityInsaatIslerBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsaatIslerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val insaatadi = intent.getSerializableExtra("insaatAdi").toString()
        binding.textView2.text = insaatadi
        val myData = intent.getSerializableExtra("insaatID").toString()

        val islerFragment = insaatislerKollariFragment().apply {
            arguments = Bundle().apply {
                putString("insaatID", myData)
                putString("insaatAdi", insaatadi)
            }
        }
        
            val yonetimFragment = ProjectApplicationsFragment().apply {
                arguments = Bundle().apply {
                    putString("insaatID", myData)
                    putString("insaatAdi", insaatadi)
                }
            }

            val sablonlarFragment = TemplateLibraryFragment().apply {
                arguments = Bundle().apply {
                    putString("insaatID", myData)
                    putString("insaatAdi", insaatadi)
                }
            }

            // Initialize with first tab
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame, islerFragment)
                .commit()

            binding.tabLayout2.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val selectedFragment = when (tab?.position) {
                        0 -> islerFragment
                        1 -> yonetimFragment
                        2 -> sablonlarFragment
                        else -> islerFragment
                    }
                
                supportFragmentManager.beginTransaction()
                    .replace(R.id.frame, selectedFragment)
                    .commit()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
}