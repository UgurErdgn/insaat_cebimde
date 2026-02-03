package com.sorodeveloper.insaatcebimde

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.sorodeveloper.insaatcebimde.databinding.ActivityAppBinding

class appActivity : AppCompatActivity() {
    private var doubleBackToExitPressedOnce = false
    lateinit var binding: ActivityAppBinding
    private lateinit var auth: FirebaseAuth
    var databaseReference : DatabaseReference?=null
    var isletmeReference : DatabaseReference?=null
    var database: FirebaseDatabase?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(firstFragment())


    }

    private fun  replaceFragment(fragment: Fragment){

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout,fragment)
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