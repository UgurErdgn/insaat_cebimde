package com.sorodeveloper.insaatcebimde

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.sorodeveloper.insaatcebimde.databinding.ActivityInsaateklemeBinding

class insaatEklemeActivity : AppCompatActivity() {
    lateinit var binding: ActivityInsaateklemeBinding
    private lateinit var auth: FirebaseAuth
    var databaseReference: DatabaseReference? = null
    var database: FirebaseDatabase? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsaateklemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        databaseReference = database?.reference!!.child("insaatlar")

        binding.anasayfadon.setOnClickListener {
            val appgecis = Intent(this, appActivity::class.java)
            startActivity(appgecis)
            finish()
        }

        binding.insaatolustur.setOnClickListener {
            var insaatadi = binding.kytinsaatadi.text.toString().trimEnd()
            var serino = binding.kytinsaatserino.text.toString().trimEnd()
            var il = binding.kyinsaatil.text.toString().trimEnd()
            var ilce = binding.kytinsaatilce.text.toString().trimEnd()
            var mahalle = binding.kyinsaatmahalle.text.toString().trimEnd()
            var adres = binding.kyttamadres.text.toString().trimEnd()
            var muteahhitAdi = binding.kyttamadres.text.toString().trimEnd()
            var muteahhitMail = binding.kyttamadres.text.toString().trimEnd()
            var muteahhitTel = binding.kyttamadres.text.toString().trimEnd()


            if (TextUtils.isEmpty(insaatadi)) {
                binding.kytinsaatadi.error = "Lütfen İnşaat Adını Giriniz"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(serino)) {
                binding.kytinsaatserino.error = "Lütfen Seri Numarası Giriniz"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(il)) {
                binding.kyinsaatil.error = "Lütfen İl Giriniz"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(ilce)) {
                binding.kytinsaatilce.error = "Lütfen İlçe Giriniz"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(mahalle)) {
                binding.kyinsaatmahalle.error = "Lütfen Mahalle Giriniz"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(adres)) {
                binding.kyttamadres.error = "Lütfen Tam Adres Giriniz"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(muteahhitAdi)) {
                binding.kytmuteahhitadi.error = "Lütfen Muteahhit Bilgisini Giriniz"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(muteahhitMail)) {
                binding.kytmuteahhitmail.error = "Lütfen Muteahhit Bilgisini Giriniz"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(muteahhitTel)) {
                binding.kytmuteahhittelno.error = "Lütfen Muteahhit Bilgisini Giriniz"
                return@setOnClickListener
            }
            else {
                val orjinalText = insaatadi
                val result = capitalizeWords(orjinalText)

                val currentUserDb =
                    result.let { task1 -> databaseReference?.child(serino)?.child("genelBilgiler") }

                val currentUserDsb =
                    result.let { task1 -> databaseReference?.child(serino) }


                currentUserDb?.child("adres")?.setValue(adres).toString()
                currentUserDb?.child("il")?.setValue(il).toString()
                currentUserDb?.child("ilce")?.setValue(ilce).toString()
                currentUserDb?.child("insaatAdi")?.setValue(insaatadi).toString()
                currentUserDb?.child("mahalle")?.setValue(mahalle).toString()
                currentUserDb?.child("muteahhitAdi")?.setValue(muteahhitAdi).toString()
                currentUserDb?.child("muteahhitMail")?.setValue(muteahhitMail).toString()
                currentUserDb?.child("muteahhitTel")?.setValue(muteahhitTel).toString()


                currentUserDb?.child("seriNo")?.setValue(serino).toString()
                currentUserDsb?.child("insaatAdi")?.setValue(insaatadi).toString()


                Toast.makeText(this, "${result} İnşaat Oluşturuldu", Toast.LENGTH_LONG).show()
                binding.insaatolustur.isClickable = false

            }


        }
    }

    fun capitalizeWords(input: String): String {
        val words = input.split(" ")
        val capitalizedWords = words.map { it.capitalize() }
        return capitalizedWords.joinToString(" ")
    }

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        val appgecis = Intent(this, appActivity::class.java)
        startActivity(appgecis)
        finish()
    }
}