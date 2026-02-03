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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    var SERVER_KEY: String? = null
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
        binding.btnInsaatekle.setOnClickListener{
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

      databaseReference?.addValueEventListener(object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {

              insaatlarArrayList.clear()

              val aramaa = binding.apparamacubuk.text.toString().toLowerCase()

              for (insaatSnapshot in snapshot.children) {
                      val insaat = insaatSnapshot.getValue(insaat::class.java)
                      val insaatAdi = insaat?.insaatAdi?.toLowerCase()

                      if (insaatAdi != null && insaatAdi.contains(aramaa)) {
                          insaatlarArrayList.add(insaat)

                  }
              }
              insaatlarRecyclerview.adapter = CustomAdapter(insaatlarArrayList, requireContext())
              insaatlarRecyclerview.adapter?.notifyDataSetChanged()
            /*  var sıralıList =
                  hizmetlerArrayList.sortedWith(compareBy({ it.HizmetKalanSure }))
              hizmetlerRecyclerview.adapter = CustomAdapter(sıralıList)*/
          }

          override fun onCancelled(error: DatabaseError) {
              Log.e("TAG", "Firebase hata: ${error.message}")
          }

      })
    }

}