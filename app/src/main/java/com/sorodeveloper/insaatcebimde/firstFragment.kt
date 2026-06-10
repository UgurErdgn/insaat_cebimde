package com.sorodeveloper.insaatcebimde

import android.content.Intent
import android.os.Bundle
import android.os.Debug
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.sorodeveloper.insaatcebimde.model.ProjectApplication
import com.sorodeveloper.insaatcebimde.model.User

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sorodeveloper.insaatcebimde.adapter.CustomAdapter
import com.sorodeveloper.insaatcebimde.databinding.ActivityFirstFragmentBinding
import kotlin.jvm.java


class firstFragment : Fragment() {
    private lateinit var binding: ActivityFirstFragmentBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var insaatlarRecyclerview: RecyclerView
    private lateinit var insaatlarArrayList: ArrayList<insaat>
    var databaseReference : DatabaseReference?=null
    var database: FirebaseDatabase?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ActivityFirstFragmentBinding.inflate(inflater, container, false)


        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        databaseReference = database?.reference!!.child("insaatlar")

        insaatlarRecyclerview=binding.rcyhizmet
        insaatlarRecyclerview.layoutManager = LinearLayoutManager(context)
        insaatlarRecyclerview.setHasFixedSize(true)
        insaatlarArrayList = arrayListOf<insaat>()
        
        binding.btnInsaatekle.setOnClickListener {
            startNewActivity()
        }

        arama()
        getInsaatData()

        return binding.root
    }


    private fun arama(){
        binding.apparamacubuk.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                getInsaatData() // Her metin değiştiğinde veritabanı sorgusunu tekrar yap
            }
        })

    }

    private fun startNewActivity() {
        // Bağlı olduğu aktiviteyi al
        val activity = requireActivity()
        // Yeni bir aktivite başlatmak için Intent oluştur
        val intent = Intent(activity, insaatEklemeActivity::class.java)

        // Yeni aktiviteyi başlat
        activity.startActivity(intent)
    }
    private fun getInsaatData() {
        val currentUser = auth.currentUser ?: return
        
        // Önce kullanıcının yetki map'ini çekelim
        FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    val user = userSnapshot.getValue(User::class.java)
                    val userPermissions = user?.projectPermissions ?: emptyMap()
                    
                    // Kullanıcı verisini çektikten sonra projeleri dinlemeye başla
                    listenToProjects(userPermissions)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TAG", "User verisi alınamadı: ${error.message}")
                }
            })
    }

    private fun listenToProjects(userPermissions: Map<String, List<com.sorodeveloper.insaatcebimde.model.MatrixPermission>>) {
      databaseReference?.addValueEventListener(object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
              if (!isAdded) return

              insaatlarArrayList.clear()

              val aramaa = binding.apparamacubuk.text.toString().toLowerCase()

              for (insaatSnapshot in snapshot.children) {
                      // Since properties are under "genelBilgiler", let's parse it correctly
                      val genelBilgiler = insaatSnapshot.child("genelBilgiler")
                      
                      val insaatModel = insaat(
                          insaatAdi = genelBilgiler.child("insaatAdi").getValue(String::class.java),
                          serino = genelBilgiler.child("seriNo").getValue(String::class.java),
                          muteahhitAdi = genelBilgiler.child("muteahhitAdi").getValue(String::class.java),
                          muteahhitMail = genelBilgiler.child("muteahhitMail").getValue(String::class.java),
                          muteahhitTel = genelBilgiler.child("muteahhitTel").getValue(String::class.java),
                          il = genelBilgiler.child("il").getValue(String::class.java),
                          ilce = genelBilgiler.child("ilce").getValue(String::class.java),
                          mahalle = genelBilgiler.child("mahalle").getValue(String::class.java),
                          adres = genelBilgiler.child("adres").getValue(String::class.java)
                      )
                      
                      val insaatAdiStr = insaatModel.insaatAdi?.toLowerCase()
                      val projectId = insaatModel.serino

                      if (projectId != null) {
                          // Matrix Yetkilendirme Kontrolü: 
                          // Kullanıcının bu projede yetkisi var mı? 
                          // Eğer userPermissions map'inde bu projectId key'i varsa projeyi listeye ekle.
                          // Ayrıca arama filtresine de uymalı.
                          val hasPermissionForProject = userPermissions.containsKey(projectId)
                          
                          if (hasPermissionForProject && insaatAdiStr != null && insaatAdiStr.contains(aramaa)) {
                              insaatlarArrayList.add(insaatModel)
                          }
                      }
              }
              insaatlarRecyclerview.adapter = CustomAdapter(insaatlarArrayList, requireContext())
              insaatlarRecyclerview.adapter?.notifyDataSetChanged()
          }

          override fun onCancelled(error: DatabaseError) {
              Log.e("TAG", "Firebase hata: ${error.message}")
          }

      })
    }


}