package com.sorodeveloper.insaatcebimde

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sorodeveloper.insaatcebimde.databinding.ActivityInsaatDetayBinding
import com.sorodeveloper.insaatcebimde.databinding.ActivityKalemDetayBinding

class kalemDetayActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKalemDetayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKalemDetayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bolum = intent.getSerializableExtra("bolumadi").toString()
        val kalem = intent.getSerializableExtra("kalemadi").toString()
        binding.textView.text = bolum +"  "+ kalem
        }
    }
