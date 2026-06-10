package com.sorodeveloper.insaatcebimde

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sorodeveloper.insaatcebimde.databinding.FragmentMulkEkleBinding
import com.sorodeveloper.insaatcebimde.model.UnitType

class mulkEkleFragment : Fragment() {
    private var _binding: FragmentMulkEkleBinding? = null
    private val binding get() = _binding!!

    private lateinit var insaatId: String
    private lateinit var fPath: String // Current location path (e.g., insaatlar/ID/sahalar/AltKademeler/BlokX)
    private lateinit var crLevel: String // Current level (e.g., Blok)

    private val unitTypeTemplates = mutableListOf<UnitType>()
    private var selectedUnitType: UnitType? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMulkEkleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        insaatId = arguments?.getString("insaatID") ?: ""
        fPath = arguments?.getString("fPath") ?: ""
        crLevel = arguments?.getString("crLevel") ?: ""

        fetchUnitTypeTemplates()
        setupListeners()
    }

    private fun setupListeners() {
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btnModeNumeric) {
                    binding.etStartValue.setText("1")
                } else {
                    binding.etStartValue.setText("A")
                }
            }
        }

        binding.btnProduce.setOnClickListener {
            handleProduction()
        }
    }

    private fun fetchUnitTypeTemplates() {
        FirebaseDatabase.getInstance().getReference("insaatlar/$insaatId/templates/unitTypes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    unitTypeTemplates.clear()
                    val names = mutableListOf<String>()
                    names.add("Şablon Seçiniz (Opsiyonel)")
                    
                    for (child in snapshot.children) {
                        val type = child.getValue(UnitType::class.java)
                        if (type != null) {
                            unitTypeTemplates.add(type)
                            names.add(type.name)
                        }
                    }

                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerUnitType.adapter = adapter
                    
                    binding.spinnerUnitType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                            selectedUnitType = if (position > 0) unitTypeTemplates[position - 1] else null
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun handleProduction() {
        val prefix = binding.etPrefix.text.toString().trim()
        val startValStr = binding.etStartValue.text.toString().trim()
        val stepVal = binding.etStepValue.text.toString().toIntOrNull() ?: 1
        val count = binding.etCount.text.toString().toIntOrNull() ?: 0

        if (count <= 0) {
            Toast.makeText(requireContext(), "Lütfen geçerli bir adet giriniz", Toast.LENGTH_SHORT).show()
            return
        }

        val isNumeric = binding.toggleMode.checkedButtonId == R.id.btnModeNumeric
        val updates = hashMapOf<String, Any>()

        try {
            for (i in 0 until count) {
                val unitNameSuffix = if (isNumeric) {
                    val startNum = startValStr.toIntOrNull() ?: 1
                    (startNum + (i * stepVal)).toString()
                } else {
                    val startIdx = alphaToNumber(startValStr)
                    numberToAlpha(startIdx + (i * stepVal))
                }

                val unitTypeNode = getPluralChildNode()
                val unitName = "$prefix$unitNameSuffix"
                val unitPath = "$fPath/$unitTypeNode/$unitName"

                // Create Unit Node
                updates["$unitPath/name"] = unitName
                updates["$unitPath/type"] = getNextLevelType()
                
                // Inject Jobs from Template
                selectedUnitType?.let { type ->
                    updates["$unitPath/unitTypeLabel"] = type.name
                    updates["$unitPath/unitTypeID"] = type.id
                    for (jobBarcode in type.jobIds.keys) {
                        updates["$unitPath/isler/$jobBarcode/progress"] = "0"
                    }
                }
            }

            FirebaseDatabase.getInstance().reference.updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "$count mülk başarıyla üretildi", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Üretim hatası: ${it.message}", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPluralChildNode(): String {
        return when (crLevel) {
            "root" -> "sahalar"
            "sahalar" -> "etaplar"
            "etaplar" -> "bloklar"
            "bloklar" -> "daireler"
            else -> "altKademeler"
        }
    }

    private fun getNextLevelType(): String {
        return when (crLevel) {
            "root" -> "Saha"
            "sahalar" -> "Etap"
            "etaplar" -> "Blok"
            "bloklar" -> "Daire"
            else -> "Birim"
        }
    }

    /**
     * Converts alphabetical string (A, B... Z, AA) to numeric index (0-based).
     */
    private fun alphaToNumber(alpha: String): Int {
        var res = 0
        for (char in alpha.uppercase()) {
            res = res * 26 + (char - 'A' + 1)
        }
        return res - 1
    }

    /**
     * Converts numeric index back to alphabetical string (0 -> A, 25 -> Z, 26 -> AA).
     */
    private fun numberToAlpha(number: Int): String {
        var num = number + 1
        var res = ""
        while (num > 0) {
            val rem = (num - 1) % 26
            res = (rem + 'A'.toInt()).toChar() + res
            num = (num - 1) / 26
        }
        return res
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}