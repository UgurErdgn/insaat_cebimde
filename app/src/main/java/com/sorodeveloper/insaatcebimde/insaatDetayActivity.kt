package com.sorodeveloper.insaatcebimde

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.FirebaseDatabase
import com.sorodeveloper.insaatcebimde.databinding.ActivityInsaatDetayBinding

class insaatDetayActivity : AppCompatActivity() {
    lateinit var binding: ActivityInsaatDetayBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsaatDetayBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val insaatadi = intent.getSerializableExtra("insaatadi").toString()
        binding.textView2.text = insaatadi
        val myData = intent.getSerializableExtra("id").toString()

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> insaatdetayIsBolumleriFragment().apply {
                        arguments = Bundle().apply {
                            putString("insaatID", myData)
                        }
                    }
                    1 -> insaatdetayKusBakisiFragment().apply {
                        arguments = Bundle().apply {
                            putString("insaatID", myData)
                        }
                    }

                    else -> insaatdetayOzelliklerFragment()
                }
            }
        }




        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout2, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "İş Bölümleri" else  "Özellikler"
        }.attach()

    }
}