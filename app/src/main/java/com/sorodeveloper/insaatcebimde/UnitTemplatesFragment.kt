package com.sorodeveloper.insaatcebimde

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sorodeveloper.insaatcebimde.adapter.UnitTypeAdapter
import com.sorodeveloper.insaatcebimde.databinding.FragmentUnitTemplatesBinding
import com.sorodeveloper.insaatcebimde.model.UnitType

class UnitTemplatesFragment : Fragment() {

    private var _binding: FragmentUnitTemplatesBinding? = null
    private val binding get() = _binding!!

    private lateinit var insaatId: String
    private lateinit var adapter: UnitTypeAdapter
    private val unitTypeList = mutableListOf<UnitType>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUnitTemplatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        insaatId = arguments?.getString("insaatID") ?: ""

        setupRecyclerView()
        fetchUnitTypes()

        binding.fabAddUnitTemplate.setOnClickListener {
            openUnitTypeEditor(null)
        }
    }

    private fun setupRecyclerView() {
        adapter = UnitTypeAdapter(unitTypeList) { unitType ->
            openUnitTypeEditor(unitType)
        }
        binding.rvUnitTemplates.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUnitTemplates.adapter = adapter
    }

    private fun fetchUnitTypes() {
        if (insaatId.isEmpty()) return

        val ref = FirebaseDatabase.getInstance().getReference("insaatlar/$insaatId/templates/unitTypes")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                unitTypeList.clear()
                for (child in snapshot.children) {
                    val type = child.getValue(UnitType::class.java)
                    if (type != null) {
                        unitTypeList.add(type)
                    }
                }
                adapter.updateList(unitTypeList)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun openUnitTypeEditor(unitType: UnitType?) {
        val fragment = UnitTypeEditorFragment()
        val bundle = Bundle().apply {
            putString("insaatID", insaatId)
            unitType?.let {
                putString("typeID", it.id)
                putString("typeName", it.name)
                // Serialization of Map for selection state (future improvement: use ViewModel)
            }
        }
        fragment.arguments = bundle

        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in, R.anim.slide_out, R.anim.slide_in, R.anim.slide_out)
            .replace(R.id.frame, fragment) // Using main frame for full screen editor feel
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
