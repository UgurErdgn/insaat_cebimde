package com.sorodeveloper.insaatcebimde

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.logger.Logger
import com.sorodeveloper.insaatcebimde.adapter.InsaatDetayAdapter
import com.sorodeveloper.insaatcebimde.databinding.FragmentInsaatdetayIsbolumleriBinding


class insaatdetayIsBolumleriFragment : Fragment() {

    private lateinit var binding: FragmentInsaatdetayIsbolumleriBinding
    var SERVER_KEY: String? = null
    private lateinit var insaatlarRecyclerview: RecyclerView
    private lateinit var adapter: InsaatDetayAdapter
    private val bolumListesi = mutableListOf<isBolumleri>()
    var databaseReference : DatabaseReference?=null
    var database: FirebaseDatabase?=null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentInsaatdetayIsbolumleriBinding.inflate(inflater, container, false)


        val gelenVeri = arguments?.getString("insaatID").toString()
        database = FirebaseDatabase.getInstance()
        databaseReference = database?.reference!!.child("insaatlar").child(gelenVeri).child("isBolumleri")
        insaatlarRecyclerview = binding.rcyhizmet


        adapter = InsaatDetayAdapter(bolumListesi, requireContext()) { baslikAdi, position ->

            val item = bolumListesi[position] as isBolumleri.Baslik

            // Eğer zaten açık ise → kapat
            if (position + 1 < bolumListesi.size && bolumListesi[position + 1] is isBolumleri.Kalem) {
                val silinecekler = mutableListOf<Int>()
                var i = position + 1
                while (i < bolumListesi.size && bolumListesi[i] is isBolumleri.Kalem) {
                    silinecekler.add(i)
                    i++
                }

                for (index in silinecekler.reversed()) {
                    bolumListesi.removeAt(index)
                }
                adapter.notifyDataSetChanged()
            }
            // Kapalıysa → aç
            else {
                val altlar = item.altlar
                val eklenecekler = altlar.map {
                    isBolumleri.Kalem(it, baslikAdi)
                }
                bolumListesi.addAll(position + 1, eklenecekler)
                adapter.notifyDataSetChanged()
            }
        }

        insaatlarRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        insaatlarRecyclerview.adapter = adapter


        getInsaatData()

        //binding.apphizmetekle.setOnClickListener{
        //startNewActivity()
        //}

        return binding.root
    }



    /*  private fun startNewActivity() {
          // Bağlı olduğu aktiviteyi al
          val activity = requireActivity()
          // Yeni bir aktivite başlatmak için Intent oluştur
          val intent = Intent(activity, appActivity::class.java)
          // Yeni aktiviteyi başlat
          activity.startActivity(intent)
      }*/
    private fun getInsaatData() {

        databaseReference?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bolumListesi.clear()

                for (isSnapshot in snapshot.children) {
                    val altlar = mutableListOf<String>()

                    val baslikAdi = isSnapshot.key ?: continue
                    for (kalemSnapshot in isSnapshot.children) {
                        kalemSnapshot.key?.let { altlar.add(it) }
                    }

                    bolumListesi.add(isBolumleri.Baslik(baslikAdi, altlar))
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun altBasliklariAcKapat(baslikAdi: String, pozisyon: Int) {

        // Eğer zaten açıksa → kapat
        if (pozisyon + 1 < bolumListesi.size && bolumListesi[pozisyon + 1] is isBolumleri.Kalem) {
            while (pozisyon + 1 < bolumListesi.size && bolumListesi[pozisyon + 1] is isBolumleri.Kalem) {
                bolumListesi.removeAt(pozisyon + 1)
            }
            adapter.notifyDataSetChanged()
            return
        }

        databaseReference?.child("${baslikAdi.lowercase()}")?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var insertPos = pozisyon + 1

                for (altSnapshot in snapshot.children) {
                    val altBaslikAdi = altSnapshot.key ?: continue
                    bolumListesi.add(insertPos, isBolumleri.Kalem(altBaslikAdi.replaceFirstChar { it.uppercase() }, baslikAdi.replaceFirstChar { it.uppercase() }))
                    insertPos++
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}



