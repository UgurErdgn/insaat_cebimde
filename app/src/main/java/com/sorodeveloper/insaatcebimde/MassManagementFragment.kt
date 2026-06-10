package com.sorodeveloper.insaatcebimde

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.sorodeveloper.insaatcebimde.adapter.IsKollariAdapter
import com.sorodeveloper.insaatcebimde.databinding.FragmentOperationCenterBinding
import isKollari
import java.util.UUID

class MassManagementFragment : Fragment() {

    private var _binding: FragmentOperationCenterBinding? = null
    private val binding get() = _binding!!

    private lateinit var insaatId: String
    private val rulesList = mutableListOf<SelectionRule>()
    private val matchedUnitPaths = mutableSetOf<String>()
    
    // UI Data
    private lateinit var commonJobsAdapter: IsKollariAdapter
    private val commonJobsList = mutableListOf<isKollari.MainItem>()
    private var cachedTemplates: DataSnapshot? = null
    private var cachedSahaSnapshot: DataSnapshot? = null
    private var lastSahaId: String? = null

    data class SelectionRule(
        val id: String = UUID.randomUUID().toString(),
        val saha: String,
        val etap: String? = null,
        val blok: String? = null,
        val daire: String? = null,
        val targetDepth: Int,
        val start: Int?,
        val end: Int?,
        val displayLabel: String
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOperationCenterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        insaatId = arguments?.getString("insaatID") ?: ""

        setupRecyclerView()
        fetchTemplates()

        binding.btnAddRule.setOnClickListener { showAddRuleDialog() }
        binding.btnProgressPlus.setOnClickListener { handleMassProgressUpdate() }
    }

    private fun setupRecyclerView() {
        commonJobsAdapter = IsKollariAdapter(
            list = commonJobsList,
            onSaveClicked = { subItem, _, _, _ ->
                // This is triggered when the "Save" (tick) button is clicked on a common job
                applyMassProgress(subItem.templateId, subItem.progress)
            },
            onMainItemClicked = { item ->
                // Optional: expand/collapse logic is handled by adapter internally
            }
        )
        binding.rvCommonJobs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCommonJobs.adapter = commonJobsAdapter
    }

    private fun fetchTemplates() {
        FirebaseDatabase.getInstance().getReference("insaatlar/$insaatId/templates/jobs")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    cachedTemplates = snapshot
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showAddRuleDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_selection_rule, null)
        val spSaha = dialogView.findViewById<Spinner>(R.id.spSaha)
        val spEtap = dialogView.findViewById<Spinner>(R.id.spEtap)
        val spBlok = dialogView.findViewById<Spinner>(R.id.spBlok)
        val spDaire = dialogView.findViewById<Spinner>(R.id.spDaire)
        val etStart = dialogView.findViewById<EditText>(R.id.etStartRange)
        val etEnd = dialogView.findViewById<EditText>(R.id.etEndRange)
        val cbAllUnits = dialogView.findViewById<CheckBox>(R.id.cbAllUnits)

        cbAllUnits.setOnCheckedChangeListener { _, isChecked ->
            dialogView.findViewById<View>(R.id.llRangeInputs).visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        // Helper to check if a selection is a terminator
        fun isTerminator(selection: String?): Boolean {
            return selection == null || selection == "Hiçbiri" || selection.startsWith("Seçiniz")
            // "Tüm..." selections no longer terminate the UI flow
        }

        // 1. Saha Populate
        val sahalar = mutableListOf("Saha Seçiniz")
        FirebaseDatabase.getInstance().getReference("insaatlar/$insaatId/sahalar")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { child -> child.key?.let { sahalar.add(it) } }
                    val sahaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sahalar)
                    sahaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spSaha.adapter = sahaAdapter
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // 2. Etap Listener
        spSaha.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (position > 0) {
                    val saha = sahalar[position]
                    
                    // PERFORMANCE: Fetch and Cache the whole Saha node once
                    binding.progressBar.visibility = View.VISIBLE
                    FirebaseDatabase.getInstance().getReference("insaatlar/$insaatId/sahalar/$saha")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                cachedSahaSnapshot = snapshot
                                lastSahaId = saha
                                binding.progressBar.visibility = View.GONE

                                val etaplar = mutableListOf("Hiçbiri", "Tüm Etaplar")
                                snapshot.child("etaplar").children.forEach { child -> child.key?.let { etaplar.add(it) } }
                                spEtap.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, etaplar)
                                spEtap.visibility = View.VISIBLE
                                spEtap.setSelection(0)
                            }
                            override fun onCancelled(error: DatabaseError) {
                                binding.progressBar.visibility = View.GONE
                            }
                        })
                } else {
                    cachedSahaSnapshot = null
                    spEtap.visibility = View.GONE
                    spBlok.visibility = View.GONE
                    spDaire.visibility = View.GONE
                }
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        // 3. Blok Listener
        spEtap.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val selection = spEtap.selectedItem?.toString()
                if (selection != null && !isTerminator(selection)) {
                    if (selection.startsWith("Tüm")) {
                        val bloklar = listOf("Hiçbiri", "Tüm Bloklar")
                        spBlok.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, bloklar)
                        spBlok.visibility = View.VISIBLE
                        spBlok.setSelection(0)
                    } else {
                        // PERFORMANCE: Use cached snapshot instead of network
                        val bloklar = mutableListOf("Hiçbiri", "Tüm Bloklar")
                        cachedSahaSnapshot?.child("etaplar/$selection/bloklar")?.children?.forEach { child -> 
                            child.key?.let { bloklar.add(it) } 
                        }
                        spBlok.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, bloklar)
                        spBlok.visibility = View.VISIBLE
                        spBlok.setSelection(0)
                    }
                } else {
                    spBlok.visibility = View.GONE
                    spDaire.visibility = View.GONE
                }
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        // 4. Daire Listener
        spBlok.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val selection = spBlok.selectedItem?.toString()
                if (selection != null && !isTerminator(selection)) {
                    if (selection.startsWith("Tüm")) {
                        val daireler = listOf("Hiçbiri", "Tüm Daireler")
                        spDaire.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, daireler)
                        spDaire.visibility = View.VISIBLE
                        spDaire.setSelection(0)
                    } else {
                        // PERFORMANCE: Use cached snapshot
                        val etap = spEtap.selectedItem.toString()
                        val daireler = mutableListOf("Hiçbiri", "Tüm Daireler")
                        cachedSahaSnapshot?.child("etaplar/$etap/bloklar/$selection/daireler")?.children?.forEach { child -> 
                            child.key?.let { daireler.add(it) } 
                        }
                        spDaire.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, daireler)
                        spDaire.visibility = View.VISIBLE
                        spDaire.setSelection(0)
                    }
                } else {
                    spDaire.visibility = View.GONE
                }
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Yeni Seçim Kuralı")
            .setView(dialogView)
            .setPositiveButton("Ekle") { _, _ ->
                val sSaha = spSaha.selectedItem?.toString() ?: ""
                if (sSaha.startsWith("Saha Seçiniz")) return@setPositiveButton
                
                // Determine target depth based on selection
                val targetDepth = when {
                    spDaire.visibility == View.VISIBLE && spDaire.selectedItem.toString() != "Hiçbiri" -> 3
                    spBlok.visibility == View.VISIBLE && spBlok.selectedItem.toString() != "Hiçbiri" -> 2
                    spEtap.visibility == View.VISIBLE && spEtap.selectedItem.toString() != "Hiçbiri" -> 1
                    else -> 0
                }

                // If it starts with "Tüm", we store null in the rule to signify "Wildcard"
                val sEtap = spEtap.selectedItem?.toString()?.takeIf { !isTerminator(it) && !it.startsWith("Tüm") }
                val sBlok = spBlok.selectedItem?.toString()?.takeIf { !isTerminator(it) && !it.startsWith("Tüm") }
                val sDaire = spDaire.selectedItem?.toString()?.takeIf { !isTerminator(it) && !it.startsWith("Tüm") }
                
                val start = etStart.text.toString().toIntOrNull()
                val end = etEnd.text.toString().toIntOrNull()
                
                val label = buildString {
                    append(sSaha)
                    spEtap.selectedItem?.toString()?.takeIf { it != "Hiçbiri" && !it.startsWith("Seçiniz") }?.let { append(" > $it") }
                    spBlok.selectedItem?.toString()?.takeIf { it != "Hiçbiri" && !it.startsWith("Seçiniz") }?.let { append(" > $it") }
                    spDaire.selectedItem?.toString()?.takeIf { it != "Hiçbiri" && !it.startsWith("Seçiniz") }?.let { append(" > $it") }
                    if (start != null || end != null) append(" (${start ?: 0}-${end ?: "∞"})")
                }

                val rule = SelectionRule(
                    saha = sSaha,
                    etap = sEtap,
                    blok = sBlok,
                    daire = sDaire,
                    targetDepth = targetDepth,
                    start = start,
                    end = end,
                    displayLabel = label
                )
                addRule(rule)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun addRule(rule: SelectionRule) {
        rulesList.add(rule)
        val chip = Chip(requireContext()).apply {
            text = rule.displayLabel
            isCloseIconVisible = true
            setOnCloseIconClickListener { 
                rulesList.remove(rule)
                binding.cgRules.removeView(this)
                triggerScan()
            }
        }
        binding.cgRules.addView(chip)
        triggerScan()
    }

    private fun triggerScan() {
        if (rulesList.isEmpty()) {
            matchedUnitPaths.clear()
            commonJobsList.clear()
            updateUI()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        matchedUnitPaths.clear()
        commonJobsList.clear() // Yeni tarama başında temizle
        updateUI()
        
        var completedScans = 0
        for (rule in rulesList) {
            scanRule(rule)
        }
        binding.progressBar.visibility = View.GONE
        fetchCommonJobs()
    }

    private fun scanRule(rule: SelectionRule) {
        // PERFORMANCE: No network call needed. Use local cachedSahaSnapshot.
        cachedSahaSnapshot?.let { snapshot ->
            recursiveFindUnits(snapshot, rule, "insaatlar/$insaatId/sahalar/${rule.saha}", 0, rule.targetDepth)
        }
    }

    private fun recursiveFindUnits(snapshot: DataSnapshot, rule: SelectionRule, currentPath: String, depth: Int, targetDepth: Int) {
        val nodeName = snapshot.key ?: ""
        
        // Depth-based traversal: 
        // 0: Saha node
        // 1: Etap node (inside /etaplar)
        // 2: Blok node (inside /bloklar)
        // 3: Daire node (inside /daireler)

        // If we reached target depth and it has isler, it's a match
        if (depth == targetDepth) {
            if (snapshot.hasChild("isler")) {
                val unitNumber = nodeName.filter { it.isDigit() }.toIntOrNull()
                val inRange = when {
                    rule.start == null && rule.end == null -> true
                    rule.start != null && rule.end != null -> unitNumber in rule.start..rule.end
                    rule.start != null -> unitNumber != null && unitNumber >= rule.start
                    rule.end != null -> unitNumber != null && unitNumber <= rule.end
                    else -> true
                }
                if (inRange) {
                    matchedUnitPaths.add(currentPath)
                }
            }
            return // Stop recursion at target level
        }

        // Traversal
        if (depth < targetDepth) {
            when (depth) {
                0 -> { // Saha -> etaplar
                    val etaplarSnapshot = snapshot.child("etaplar")
                    if (rule.etap == null) { // Wildcard
                        etaplarSnapshot.children.forEach { recursiveFindUnits(it, rule, "$currentPath/etaplar/${it.key}", 1, targetDepth) }
                    } else { // Specific
                        etaplarSnapshot.child(rule.etap).takeIf { it.exists() }?.let { 
                            recursiveFindUnits(it, rule, "$currentPath/etaplar/${rule.etap}", 1, targetDepth) 
                        }
                    }
                }
                1 -> { // Etap -> bloklar
                    val bloklarSnapshot = snapshot.child("bloklar")
                    if (rule.blok == null) { // Wildcard
                        bloklarSnapshot.children.forEach { recursiveFindUnits(it, rule, "$currentPath/bloklar/${it.key}", 2, targetDepth) }
                    } else { // Specific
                        bloklarSnapshot.child(rule.blok).takeIf { it.exists() }?.let { 
                            recursiveFindUnits(it, rule, "$currentPath/bloklar/${rule.blok}", 2, targetDepth) 
                        }
                    }
                }
                2 -> { // Blok -> daireler
                    val dairelerSnapshot = snapshot.child("daireler")
                    if (rule.daire == null) { // Wildcard
                        dairelerSnapshot.children.forEach { recursiveFindUnits(it, rule, "$currentPath/daireler/${it.key}", 3, targetDepth) }
                    } else { // Specific
                        dairelerSnapshot.child(rule.daire).takeIf { it.exists() }?.let { 
                            recursiveFindUnits(it, rule, "$currentPath/daireler/${rule.daire}", 3, targetDepth) 
                        }
                    }
                }
            }
        }
    }

    private fun fetchCommonJobs() {
        if (matchedUnitPaths.isEmpty() || cachedSahaSnapshot == null) {
            commonJobsList.clear()
            updateUI()
            return
        }

        // PERFORMANCE: Local search in the cached snapshot. Zero network calls!
        val jobOccurrences = mutableMapOf<String, Int>()
        val baseSahaPath = "insaatlar/$insaatId/sahalar/${lastSahaId}"

        for (fullPath in matchedUnitPaths) {
            // Path structure: insaatlar/ID/sahalar/NAME/etaplar/E1/bloklar/B1/daireler/D1
            // We need the relative path inside the Saha snapshot
            val relativePath = fullPath.removePrefix("$baseSahaPath/").removePrefix("/")
            val unitSnapshot = if (relativePath.isEmpty()) cachedSahaSnapshot else cachedSahaSnapshot?.child(relativePath)
            
            unitSnapshot?.child("isler")?.children?.forEach { child ->
                val jobId = child.key ?: return@forEach
                jobOccurrences[jobId] = jobOccurrences.getOrDefault(jobId, 0) + 1
            }
        }

        // Jobs that appear in ALL matched units are "Common"
        val commonIds = jobOccurrences.filter { it.value == matchedUnitPaths.size }.keys
        mapJobsToUI(commonIds)
    }

    private fun mapJobsToUI(commonIds: Set<String>) {
        commonJobsList.clear()
        val groupedJobs = mutableMapOf<String, MutableList<isKollari.SubItem>>()

        for (jobId in commonIds) {
            val template = cachedTemplates?.child(jobId) ?: continue
            val branch = template.child("branch").getValue(String::class.java) ?: "Diğer"
            val category = template.child("category").getValue(String::class.java) ?: ""
            val type = template.child("type").getValue(String::class.java) ?: ""

            val subItem = isKollari.SubItem(
                templateId = jobId,
                title = "$category ($type)",
                progress = "0",
                kacAdet = matchedUnitPaths.size.toString(),
                firebasePath = "",
                isEditable = true, // Mass view'da düzenlenebilir olsun
                trade = branch,
                category = category,
                type = type
            )
            groupedJobs.getOrPut(branch) { mutableListOf() }.add(subItem)
        }

        for ((branch, items) in groupedJobs) {
            commonJobsList.add(isKollari.MainItem(
                title = branch,
                subItems = items.toMutableList(),
                ilerleme = "Toplu Müdahale",
                isExpanded = false,
                kacAdet = items.size.toString()
            ))
        }
        updateUI()
    }

    private fun updateUI() {
        binding.tvFoundCount.text = "${matchedUnitPaths.size} Mülk Eşleşti"
        binding.cardActions.visibility = if (matchedUnitPaths.isNotEmpty() && commonJobsList.isNotEmpty()) View.VISIBLE else View.GONE
        commonJobsAdapter.notifyDataSetChanged()
    }

    private fun handleMassProgressUpdate() {
        // This is a generic button click handler. 
        // We'll use the per-item save callback in the adapter instead for better UX,
        // but this could be used for a "Apply +10% to all visible" feature.
        Toast.makeText(requireContext(), "Lütfen listeden bir iş kaleminin yanındaki onay butonuna basarak güncelleyin.", Toast.LENGTH_LONG).show()
    }

    private fun applyMassProgress(templateId: String, newValue: String) {
        val newInt = newValue.toIntOrNull() ?: return
        val cachedSaha = cachedSahaSnapshot ?: return
        val baseSahaPath = "insaatlar/$insaatId/sahalar/$lastSahaId"
        
        val bulkUpdates = HashMap<String, Any?>()
        val parentDeltas = mutableMapOf<String, Int>()

        binding.progressBar.visibility = View.VISIBLE

        // 1. Calculate updates for each unit and track parent deltas
        matchedUnitPaths.forEach { unitPath ->
            val relativePath = unitPath.removePrefix(baseSahaPath).removePrefix("/")
            val unitSnapshot = if (relativePath.isEmpty()) cachedSaha else cachedSaha.child(relativePath)
            
            val oldValueStr = unitSnapshot.child("isler/$templateId/progress").getValue(String::class.java) ?: "0"
            val oldInt = oldValueStr.toIntOrNull() ?: 0
            val fark = newInt - oldInt
            
            if (fark != 0) {
                bulkUpdates["$unitPath/isler/$templateId/progress"] = newValue
                
                // Track bubbling deltas
                val altKademelerPaths = getAllUpperAltKademelerPaths("$unitPath/isler/$templateId/progress")
                for (parentPath in altKademelerPaths) {
                    parentDeltas[parentPath] = (parentDeltas[parentPath] ?: 0) + fark
                }
            }
        }

        if (bulkUpdates.isEmpty()) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Herhangi bir değişiklik yok.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Add parent (summary) updates to the batch
        for ((parentPath, totalDelta) in parentDeltas) {
            val relativePath = parentPath.removePrefix(baseSahaPath).removePrefix("/")
            val currentValStr = cachedSaha.child(relativePath).child("$templateId/progress").getValue(String::class.java) ?: "0"
            val currentValInt = currentValStr.toIntOrNull() ?: 0
            val newVal = (currentValInt + totalDelta).toString()
            
            bulkUpdates["$parentPath/$templateId/progress"] = newVal
        }

        // 3. Execute Atomic Batch Update
        FirebaseDatabase.getInstance().reference.updateChildren(bulkUpdates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "${matchedUnitPaths.size} mülk tek seferde güncellendi! 🚀", Toast.LENGTH_LONG).show()
                
                // Refresh cache to match new DB state
                refreshSahaCache {
                    triggerScan()
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Güncelleme hatası: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun refreshSahaCache(onDone: () -> Unit) {
        val saha = lastSahaId ?: return
        FirebaseDatabase.getInstance().getReference("insaatlar/$insaatId/sahalar/$saha")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    cachedSahaSnapshot = snapshot
                    onDone()
                }
                override fun onCancelled(error: DatabaseError) { onDone() }
            })
    }

    private fun getAllUpperAltKademelerPaths(fullProgressPath: String): List<String> {
        val parts = fullProgressPath.split("/").toMutableList()
        val results = mutableListOf<String>()
        
        // Find "isler" and move backwards
        val islerIndex = parts.indexOfLast { it == "isler" }
        if (islerIndex == -1) return emptyList()
        
        var idx = islerIndex - 1 // Start at the unit node
        while (idx >= 3) {
            val currentNode = parts[idx]
            
            if (currentNode == lastSahaId || parts.getOrNull(idx-1) == "sahalar") {
                // We are at Saha level
                val sahalarIndex = parts.indexOf("sahalar")
                if (sahalarIndex != -1) {
                    val sahalarPath = parts.subList(0, sahalarIndex + 1).joinToString("/")
                    results.add("$sahalarPath/AltKademeler")
                }
            } else {
                // We are at Etap or Blok level
                val parentPath = parts.subList(0, idx).joinToString("/")
                results.add("$parentPath/AltKademeler")
            }
            idx -= 2 // Skip technical nodes like "etaplar", "bloklar", etc.
        }
        return results.distinct()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
