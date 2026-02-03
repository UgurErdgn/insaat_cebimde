package com.sorodeveloper.insaatcebimde

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sorodeveloper.insaatcebimde.adapter.GenericGridAdapter
import com.sorodeveloper.insaatcebimde.databinding.FragmentInsaatdetayKusBakisiBinding

class insaatdetayKusBakisiFragment : Fragment() {
    private lateinit var binding: FragmentInsaatdetayKusBakisiBinding
    var SERVER_KEY: String? = null

    var AdatabaseReference : DatabaseReference?=null
    var BdatabaseReference : DatabaseReference?=null
    var database: FirebaseDatabase?=null

    private lateinit var adapter: GenericGridAdapter
    private val itemList = mutableListOf<String>()

    private val tabTitles = mutableListOf<String>()

    private var currentPath = ""
    private var lastPath = ""
    private var currentisPath = ""
    private var nextflags = "Saha"
    private var gelenVeri =""

    data class NavState(
        val currentPath: String,
        val currentisPath: String,
        val nextflags: String,
        val pathText: String
    )

    private val navStack = mutableListOf<NavState>()




    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentInsaatdetayKusBakisiBinding.inflate(inflater, container, false)

        gelenVeri = arguments?.getString("insaatID").toString()

       /* val viewadapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 5

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> deneme()

                    else -> deneme()
                }
            }
        }

        binding.viewPager.adapter = viewadapter

        TabLayoutMediator(binding.tabLayout2, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Kalem1" else  "DiğerKalem"
        }.attach()*/

        database = FirebaseDatabase.getInstance()

     /*   adapter = GenericGridAdapter(itemList) { clickedItem ->
            onItemClicked(clickedItem)
        }*/
        
        adapter = GenericGridAdapter(
            itemList,
            onItemClick = { item: String ->
                Toast.makeText(requireContext(), "Mülke giriş için çift tıklayınız", Toast.LENGTH_SHORT).show()
                onDoubleClick(item)
            },
            onDoubleClick = { item: String ->
                onItemClicked(item)
            }
        )


        binding.rvEtaplar.layoutManager = GridLayoutManager(requireContext(), 4)//geri dönülecek ve 4 değil her kattaa kaç tane varsa o eklenecek vye farklı bir mantık
        binding.rvEtaplar.adapter = adapter


        currentPath = "insaatlar/$gelenVeri/sahalar"
        loadData("boş")

        binding.myaddbtn.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("nextflags", nextflags)
            bundle.putString("currentpath", currentPath)
            val mfragment = mulkEkleFragment()
            mfragment.arguments = bundle
            replaceFragment(mfragment)
        }

        binding.mybackbtn.setOnClickListener {
            if (navStack.isNotEmpty()) {
                val lastState = navStack.removeAt(navStack.size - 1)

                currentPath = lastState.currentPath
                currentisPath = lastState.currentisPath
                nextflags = lastState.nextflags
                binding.myPath.text = lastState.pathText

                if (binding.myPath.text.isEmpty()) {
                    binding.myPath.visibility = View.GONE
                }

                loadData(nextflags)
            } else {
                Toast.makeText(requireContext(), "En baştadasın", Toast.LENGTH_SHORT).show()
            }        }

        return binding.root
    }
    

    private fun getSahaFromPath(): String {
        // örnek path: insaatlar/x/sahalar/anaSaha/etaplar
        val parts = currentPath.split("/")
        val sahaIndex = parts.indexOf("sahalar") + 1
        return parts.getOrNull(sahaIndex) ?: ""
    }
    private fun getEtapFromPath(): String {
        // örnek path: insaatlar/x/etaplar/Etap1/bloklar
        val parts = currentPath.split("/")
        val etapIndex = parts.indexOf("etaplar") + 1
        return parts.getOrNull(etapIndex) ?: ""
    }
    private fun getBlokFromPath(): String {
        // örnek path: insaatlar/x/etaplar/Etap1/bloklar/BlokA/daireler
        val parts = currentPath.split("/")
        val blokIndex = parts.indexOf("bloklar") + 1
        return parts.getOrNull(blokIndex) ?: ""
    }

    private fun onItemClicked(item: String) {

        navStack.add(
            NavState(
                currentPath = currentPath,
                currentisPath = currentisPath,
                nextflags = nextflags,
                pathText = binding.myPath.text.toString()
            )
        )
        val oldPath = binding.myPath.text.toString()
        binding.myPath.visibility = View.VISIBLE

        when (nextflags) {
            "Saha" -> {
                binding.myPath.text = "$oldPath/$item"
                lastPath = currentPath
                currentPath = "insaatlar/$gelenVeri/sahalar/$item/etaplar"
                currentisPath = "insaatlar/$gelenVeri/sahalar/$item/isler"
                nextflags = "Etap"
            }

            "Etap" -> {
                binding.myPath.text = "$oldPath/$item"
                lastPath = currentPath
                currentPath = "insaatlar/$gelenVeri/sahalar/${getSahaFromPath()}/etaplar/$item/bloklar"
                currentisPath = "insaatlar/$gelenVeri/sahalar/${getSahaFromPath()}/etaplar/$item/isler"
                nextflags = "Blok"
            }

            "Blok" -> {
                lastPath = currentPath
                binding.myPath.text = "$oldPath/$item"
                currentPath = "insaatlar/$gelenVeri/sahalar/${getSahaFromPath()}/etaplar/${getEtapFromPath()}/bloklar/$item/daireler"
                currentisPath = "insaatlar/$gelenVeri/sahalar/${getSahaFromPath()}/etaplar/${getEtapFromPath()}/bloklar/$item/isler"
                nextflags = "Daire"
            }

            "Daire" -> {
                binding.myPath.text = "$oldPath/$item"
                nextflags="boş"
                currentPath = "insaatlar/$gelenVeri/sahalar/${getSahaFromPath()}/etaplar/${getEtapFromPath()}/bloklar/${getBlokFromPath()}/daireler/$item"
                currentisPath = "$currentPath/isler"
                Toast.makeText(requireContext(), "${getBlokFromPath()} seçildi", Toast.LENGTH_SHORT).show()
            }
        }

        loadData(nextflags)
    }
    private fun onDoubleClick(item: String) {
        when (nextflags) {
            "Saha" -> {
                currentisPath = "insaatlar/$gelenVeri/sahalar/$item/isler"
            }

            "Etap" -> {
                currentisPath = "insaatlar/$gelenVeri/sahalar/${getSahaFromPath()}/etaplar/$item/isler"
            }

            "Blok" -> {
                currentisPath = "insaatlar/$gelenVeri/sahalar/${getSahaFromPath()}/etaplar/${getEtapFromPath()}/bloklar/$item/isler"
            }

            "Daire" -> {
                currentisPath = "insaatlar/$gelenVeri/sahalar/${getSahaFromPath()}/etaplar/${getEtapFromPath()}/bloklar/${getBlokFromPath()}/daireler/$item/isler"
            }
        }

        getJob()
    }
    private fun loadData(stage: String) {

        if (stage == lastPath){
            AdatabaseReference = FirebaseDatabase.getInstance().reference
                .child(stage)        }
        else{
        AdatabaseReference = FirebaseDatabase.getInstance().reference
            .child(currentPath)
        }

        AdatabaseReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
              //  if (!isAdded || _binding == null) return

                itemList.clear()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    if (!key.contains("turleri", ignoreCase = true)) {
                        itemList.add(key)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })


        getJob()

    }

    private fun getJob(){
        BdatabaseReference =  FirebaseDatabase.getInstance().reference
            .child(currentisPath)
        BdatabaseReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tabTitles.clear()

                for (child in snapshot.children) {
                    child.key?.let { tabTitles.add(it) } // sadece string alıyoruz
                    Log.e("tabTitles", tabTitles.toString())
                }

                setupTabs(tabTitles) // TabLayout'a gönderiyoruz
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private fun setupTabs(titles: List<String>) {
        val viewadapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = titles.size

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> deneme()

                    else -> deneme()
                }
            }
        }

        binding.viewPager.adapter = viewadapter

        TabLayoutMediator(binding.tabLayout2, binding.viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }

    private fun  replaceFragment(fragment: Fragment){
        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout,fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }




}



