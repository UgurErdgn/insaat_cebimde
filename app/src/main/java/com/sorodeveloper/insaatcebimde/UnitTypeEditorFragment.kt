package com.sorodeveloper.insaatcebimde

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sorodeveloper.insaatcebimde.adapter.PropertyJobSelectionAdapter
import com.sorodeveloper.insaatcebimde.databinding.FragmentUnitTypeEditorBinding
import com.sorodeveloper.insaatcebimde.model.UnitType
import isKollari
import java.util.UUID

class UnitTypeEditorFragment : Fragment() {

    private var _binding: FragmentUnitTypeEditorBinding? = null
    private val binding get() = _binding!!

    private lateinit var insaatId: String
    private var typeId: String? = null
    private var initialTypeName: String? = null

    private lateinit var adapter: PropertyJobSelectionAdapter
    private val allJobsList = mutableListOf<isKollari.SubItem>()
    private val initialSelectedIds = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUnitTypeEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        insaatId = arguments?.getString("insaatID") ?: ""
        typeId = arguments?.getString("typeID")
        initialTypeName = arguments?.getString("typeName")

        binding.etUnitTypeName.setText(initialTypeName)
        if (typeId != null) {
            binding.tvEditorTitle.text = "Mülk Tipi Düzenle"
            fetchExistingTypeSelection()
        } else {
            binding.tvEditorTitle.text = "Yeni Mülk Tipi Ekle"
            setupRecyclerView()
        }

        fetchAllJobTemplates()

        binding.btnSaveUnitType.setOnClickListener {
            handleSave()
        }
    }

    private fun fetchExistingTypeSelection() {
        val ref = FirebaseDatabase.getInstance().getReference("insaatlar/$insaatId/templates/unitTypes/$typeId")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val unitType = snapshot.getValue(UnitType::class.java)
                unitType?.jobIds?.keys?.let {
                    initialSelectedIds.addAll(it)
                }
                setupRecyclerView()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = PropertyJobSelectionAdapter(allJobsList, initialSelectedIds)
        binding.rvJobSelection.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJobSelection.adapter = adapter
    }

    private fun fetchAllJobTemplates() {
        FirebaseDatabase.getInstance().getReference("insaatlar/$insaatId/templates/jobs")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allJobsList.clear()
                    for (node in snapshot.children) {
                        val id = node.key ?: continue
                        val branch = node.child("branch").getValue(String::class.java) ?: ""
                        val category = node.child("category").getValue(String::class.java) ?: ""
                        val type = node.child("type").getValue(String::class.java) ?: ""

                        allJobsList.add(isKollari.SubItem(
                            templateId = id,
                            title = "$category ($type)",
                            trade = branch,
                            category = category,
                            type = type,
                            progress = "0",
                            kacAdet = "1",
                            firebasePath = "",
                            isEditable = false
                        ))
                    }
                    adapter.updateList(allJobsList)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun handleSave() {
        val name = binding.etUnitTypeName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen bir isim girin", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedIds = adapter.selectedJobIds
        if (selectedIds.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen en az bir iş seçin", Toast.LENGTH_SHORT).show()
            return
        }

        // Değişiklik kontrolü ve uyarı diyaloğu
        if (typeId != null) {
            showConfirmationDialog(name, selectedIds)
        } else {
            saveToFirebase(name, selectedIds)
        }
    }

    private fun showConfirmationDialog(name: String, selectedIds: Set<String>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Değişiklikleri Onayla")
            .setMessage("Bu mülk tipinde yapacağınız değişiklikler, bu tipi kullanan mevcut tüm dairelere uygulanacaktır.\n\nDevam etmek istiyor musunuz?")
            .setPositiveButton("Evet, Güncelle") { _, _ ->
                saveToFirebase(name, selectedIds)
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun saveToFirebase(name: String, selectedIds: Set<String>) {
        val id = typeId ?: UUID.randomUUID().toString()
        val jobIdsMap = selectedIds.associateWith { true }
        val unitType = UnitType(id, name, jobIdsMap)

        FirebaseDatabase.getInstance().getReference("insaatlar/$insaatId/templates/unitTypes/$id")
            .setValue(unitType)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Şablon Kaydedildi", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Hata oluştu", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
