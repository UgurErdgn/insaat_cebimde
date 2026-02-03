package com.sorodeveloper.insaatcebimde.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sorodeveloper.insaatcebimde.insaatdetayIsBolumleriFragment
import com.sorodeveloper.insaatcebimde.insaatdetayKusBakisiFragment
import com.sorodeveloper.insaatcebimde.insaatdetayOzelliklerFragment

open class MainPagerAdapter(activity: FragmentActivity) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> insaatdetayIsBolumleriFragment()
            1 -> insaatdetayKusBakisiFragment()
            2 -> insaatdetayOzelliklerFragment()
            else -> insaatdetayIsBolumleriFragment()
        }
    }
}
