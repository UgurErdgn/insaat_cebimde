package com.sorodeveloper.insaatcebimde

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sorodeveloper.insaatcebimde.databinding.ActivityInsaateklemeBinding
import com.sorodeveloper.insaatcebimde.model.MatrixPermission
import com.sorodeveloper.insaatcebimde.model.ProjectNode
import com.sorodeveloper.insaatcebimde.model.ProjectNodeType
import com.sorodeveloper.insaatcebimde.model.User

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
            var muteahhitAdi = binding.kytmuteahhitadi.text.toString().trimEnd()
            var muteahhitMail = binding.kytmuteahhitmail.text.toString().trimEnd()
            var muteahhitTel = binding.kytmuteahhittelno.text.toString().trimEnd()


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

                val currentUserDb = databaseReference?.child(serino)?.child("genelBilgiler")
                val currentUserDsb = databaseReference?.child(serino)

                currentUserDb?.child("adres")?.setValue(adres)
                currentUserDb?.child("il")?.setValue(il)
                currentUserDb?.child("ilce")?.setValue(ilce)
                currentUserDb?.child("insaatAdi")?.setValue(result)
                currentUserDb?.child("mahalle")?.setValue(mahalle)
                currentUserDb?.child("muteahhitAdi")?.setValue(muteahhitAdi)
                currentUserDb?.child("muteahhitMail")?.setValue(muteahhitMail)
                currentUserDb?.child("muteahhitTel")?.setValue(muteahhitTel)
                currentUserDb?.child("seriNo")?.setValue(serino)
                
                currentUserDsb?.child("insaatAdi")?.setValue(result)

                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val userRef = database?.reference?.child("users")?.child(uid)
                    userRef?.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val user = snapshot.getValue(User::class.java)
                                if (user != null) {
                                    val updatedPermissions = user.projectPermissions.toMutableMap()
                                    val projectList = updatedPermissions[serino]?.toMutableList() ?: mutableListOf()
                                    
                                    // Grant full access (root location, root job, can delegate)
                                    projectList.add(MatrixPermission(locationPath = "", jobPath = "", canDelegate = true))
                                    updatedPermissions[serino] = projectList
                                    
                                    userRef.setValue(user.copy(projectPermissions = updatedPermissions))
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                    
                    // Create the root location node for the Universal Project Tree
                    val rootNode = ProjectNode(
                        id = serino,
                        projectId = serino,
                        name = result,
                        type = ProjectNodeType.PROJECT,
                        parentId = null,
                        path = serino
                    )
                    database?.reference?.child("insaatlar")?.child(serino)?.child("locations")?.child(serino)?.setValue(rootNode)
                }

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