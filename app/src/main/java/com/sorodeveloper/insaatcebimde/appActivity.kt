package com.sorodeveloper.insaatcebimde

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.sorodeveloper.insaatcebimde.databinding.ActivityAppBinding
import com.google.firebase.auth.FirebaseAuth
import com.sorodeveloper.insaatcebimde.auth.AuthActivity
import com.sorodeveloper.insaatcebimde.ui.ProfileFragment

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class appActivity : AppCompatActivity() {
    private var doubleBackToExitPressedOnce = false
    lateinit var binding: ActivityAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Redirect to AuthActivity if not logged in
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        
        binding = ActivityAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default to FirstFragment (Ana Menü)
        replaceFragment(firstFragment())
        binding.navigationView.selectedItemId = R.id.nav_home

        binding.navigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(firstFragment())
                    true
                }
                R.id.nav_profile -> {
                    // ProfileFragment will be created next
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        super.onBackPressed()
        if (doubleBackToExitPressedOnce ) {
            // super.onBackPressed()
            onBackPressedDispatcher.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true

        Toast.makeText(this, "Uygulamadan Çıkmak için Tekrar Tıklayınız", Toast.LENGTH_SHORT).show()

        android.os.Handler(Looper.getMainLooper())
            .postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }
}