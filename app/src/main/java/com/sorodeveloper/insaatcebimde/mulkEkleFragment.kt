package com.sorodeveloper.insaatcebimde

import android.R
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.RecyclerView
import com.google.android.play.core.integrity.p
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sorodeveloper.insaatcebimde.databinding.FragmentMulkEkleBinding


class mulkEkleFragment : Fragment() {
    private lateinit var binding: FragmentMulkEkleBinding
    var SERVER_KEY: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var insaatlarRecyclerview: RecyclerView
    private lateinit var insaatlarArrayList: ArrayList<insaat>
    var databaseReference : DatabaseReference?=null
    var database: FirebaseDatabase?=null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentFragmentManager.popBackStack()
                }
            }
        )

    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentMulkEkleBinding.inflate(inflater, container, false)


        val baslik = arguments?.getString("nextflags")
        binding.tvBaslik.text = "$baslik Ekle"

        val pth = arguments?.getString("currentpath")
        binding.textView25.text = "$pth"


        // Yeni değişkenler
        val yeniBaslik = "${baslik}Turleri"
        val yeniPath = "$pth/$yeniBaslik"

        loadSpinnerData(yeniPath)

        return binding.root
    }

    private fun loadSpinnerData(path: String) {
        val ref = FirebaseDatabase.getInstance().reference.child(path)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val spinnerList = mutableListOf<String>()

                for (child in snapshot.children) {
                    child.key?.let { spinnerList.add(it) }
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    R.layout.simple_spinner_item,
                    spinnerList
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinner.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }



}