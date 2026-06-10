package com.sorodeveloper.insaatcebimde

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ListView
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sorodeveloper.insaatcebimde.adapter.IsKollariAdapter
import com.sorodeveloper.insaatcebimde.databinding.FragmentInsaatislerKollariBinding
import com.sorodeveloper.insaatcebimde.databinding.ItemBreadcrumbSpinnerBinding
import androidx.lifecycle.ViewModelProvider
import com.sorodeveloper.insaatcebimde.auth.AuthViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.filterNotNull


class insaatislerKollariFragment : Fragment() {
    private lateinit var binding: FragmentInsaatislerKollariBinding

    private lateinit var adapter: IsKollariAdapter
    private val mainList = mutableListOf<isKollari.MainItem>()

    private var insaatPath = ""
    private var insaatId : String ?= null
    var selectedMainItemId: String? = null
    private var gorusAlani = "AltKademeler" // varsayılan
    private var crLevel = "root" // Sahalar, Etaplat, Bloklar, Daireler //mulk ve iş eklemesi yaparken
    private var currentBasePath = "" // Toggle hariç path
    private var mulkPath = "" //

    private lateinit var authViewModel: AuthViewModel
    private var authorizedRoot: String = "" // e.g., "anaSaha/etap1"




    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentInsaatislerKollariBinding.inflate(inflater, container, false)


        insaatId = arguments?.getString("insaatID") ?: ""

        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        insaatPath = "insaatlar/$insaatId"

        // Observe authorization to set authorizedRoot and start navigation
        authViewModel.currentUser.filterNotNull().onEach { user ->
            authorizedRoot = authViewModel.getAuthorizedRoot(insaatId ?: "")
            startNavigation()
        }.launchIn(lifecycleScope)



        binding.myaddbtn.setOnClickListener {
            if (crLevel != "root") {
                replaceFragment(mulkEkleFragment(), mulkPath)
            }
        }

        binding.btnMassManagement.setOnClickListener {
            val fragment = MassManagementFragment()
            val bundle = Bundle()
            bundle.putString("insaatID", insaatId)
            bundle.putString("basePath", mulkPath)
            fragment.arguments = bundle
            
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in, R.anim.slide_out, R.anim.slide_in, R.anim.slide_out)
                .replace(R.id.frame, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.btnExtra.setOnClickListener {
            if (crLevel != "root") {
                replaceFragment(ProfessionalJobAddingFragment(), mulkPath)
            }
        }


        adapter = IsKollariAdapter(mainList){ mainItem ->
            selectedMainItemId = mainItem.title
        }
        binding.rvMain.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMain.adapter = adapter


        setupToggleGroup()

        return binding.root
    }

    private fun setupToggleGroup() {
        binding.toggleGroup.clearOnButtonCheckedListeners()


        // Varsayılan
        binding.toggleGroup.check(binding.toggleAltKademeler.id)
        gorusAlani = "AltKademeler"

        binding.toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            when (checkedId) {
                binding.toggleisler.id -> {
                    gorusAlani ="isler"
                    binding.btnExtra.visibility = View.VISIBLE
                }
                else -> {
                    gorusAlani = "AltKademeler"
                    binding.btnExtra.visibility = View.GONE
                }
            }

            // SADECE path'in sonunu değiştiriyoruz
            refreshDataByToggle()

    }
    }

    private fun refreshDataByToggle() {
        if (currentBasePath.isNotEmpty()) {
            val finalPath = "$currentBasePath/$gorusAlani"
            val lastView = binding.breadcrumbContainer.getChildAt(binding.breadcrumbContainer.childCount - 1)
            val currentLabel = try {
                 (lastView.tag as? String) ?: ""
            } catch (e: Exception) { "" }
            
            fetchTemplatesAndData(finalPath, getNextLabel(currentLabel))
        }
    }

    private fun getNextLabel(currentLabel: String): String? {
        return when (currentLabel) {
            "Saha" -> "Etap"
            "Etap" -> "Blok"
            "Blok" -> "Daire"
            else -> null
        }
    }

    private var cachedTemplates: DataSnapshot? = null
    private var cachedUnitTypes: DataSnapshot? = null

    private fun fetchTemplatesAndData(crPath: String, nextLevelLabel: String? = null) {
        val rootRef = FirebaseDatabase.getInstance().getReference("insaatlar/$insaatId/templates")
        
        if (cachedTemplates == null || cachedUnitTypes == null) {
            rootRef.get().addOnSuccessListener { snapshot ->
                cachedTemplates = snapshot.child("jobs")
                cachedUnitTypes = snapshot.child("unitTypes")
                getData(crPath, nextLevelLabel)
            }.addOnFailureListener {
                getData(crPath, nextLevelLabel)
            }
        } else {
            getData(crPath, nextLevelLabel)
        }
    }

    private fun getData(crPath : String, nextLevelLabel: String? = null) {
        FirebaseDatabase.getInstance().getReference(crPath)
            .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fullRelativeParentPath = crPath.substringAfter("$insaatPath/sahalar/").removePrefix("/")
                val newList = mutableListOf<isKollari.MainItem>()
                val groupedItems = mutableMapOf<String, MutableList<isKollari.SubItem>>()

                if (gorusAlani == "isler") {
                    val propertyPath = crPath.removeSuffix("/isler")
                    FirebaseDatabase.getInstance().getReference(propertyPath).child("unitTypeID")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(idSnapshot: DataSnapshot) {
                                val typeId = idSnapshot.getValue(String::class.java)
                                val unitType = cachedUnitTypes?.child(typeId ?: "")
                                
                                if (unitType == null || !unitType.exists()) {
                                    binding.tvPermissionWarning.visibility = View.VISIBLE
                                    binding.tvPermissionWarning.text = "Bu mülkün şablonu (UnitType) bulunamadı."
                                    binding.layoutPercentPicker.visibility = View.GONE
                                    adapter.updateList(emptyList())
                                    return
                                }

                                val templateJobIds = unitType.child("jobIds").children.mapNotNull { it.key }
                                for (jobId in templateJobIds) {
                                    val templateNode = cachedTemplates?.child(jobId) ?: continue
                                    val branch = templateNode.child("branch").getValue(String::class.java) ?: "Diğer"
                                    
                                    // Yetki Kontrolü: isJobVisible kullanıyoruz
                                    if (!authViewModel.isJobVisible(insaatId ?: "", fullRelativeParentPath, jobId)) {
                                        continue
                                    }

                                    val category = templateNode.child("category").getValue(String::class.java) ?: "Bilinmeyen"
                                    val type = templateNode.child("type").getValue(String::class.java) ?: "Bilinmeyen"
                                    val progress = snapshot.child(jobId).child("progress").getValue(String::class.java) ?: "0"
                                    
                                    val subItem = createSubItem(jobId, templateNode, branch, category, type, progress, crPath)
                                    groupedItems.getOrPut(branch) { mutableListOf() }.add(subItem)
                                }
                                finalizeAndDisplay(groupedItems, newList, nextLevelLabel, fullRelativeParentPath)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                } else {
                    for (childSnapshot in snapshot.children) {
                        val key = childSnapshot.key ?: continue
                        val templateNode = cachedTemplates?.child(key)
                        
                        if (templateNode != null && templateNode.exists()) {
                            val branch = templateNode.child("branch").getValue(String::class.java) ?: "Diğer"
                            if (!authViewModel.isJobVisible(insaatId ?: "", fullRelativeParentPath, key)) continue

                            val category = templateNode.child("category").getValue(String::class.java) ?: "Bilinmeyen"
                            val type = templateNode.child("type").getValue(String::class.java) ?: "Bilinmeyen"
                            val progress = childSnapshot.child("progress").getValue(String::class.java) ?: "0"

                            val subItem = createSubItem(key, templateNode, branch, category, type, progress, crPath)
                            groupedItems.getOrPut(branch) { mutableListOf() }.add(subItem)
                        }
                    }
                    finalizeAndDisplay(groupedItems, newList, nextLevelLabel, fullRelativeParentPath)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun createSubItem(id: String, node: DataSnapshot, branch: String, cat: String, type: String, prog: String, path: String): isKollari.SubItem {
        val drawings = node.child("cizimler").children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
        val mats = mutableMapOf<String, String>()
        node.child("malzemeler").children.forEach { mats[it.key ?: ""] = it.getValue(String::class.java) ?: "1" }

        return isKollari.SubItem(
            templateId = id,
            title = "$cat ($type)",
            progress = prog,
            kacAdet = "1",
            firebasePath = "$path/$id/progress",
            isEditable = gorusAlani != "AltKademeler",
            trade = branch,
            category = cat,
            type = type,
            drawingUrls = drawings,
            materials = if (mats.isNotEmpty()) mats else null
        )
    }

    private fun finalizeAndDisplay(groupedItems: Map<String, List<isKollari.SubItem>>, newList: MutableList<isKollari.MainItem>, nextLevelLabel: String?, fullPath: String) {
        for ((branch, subItems) in groupedItems) {
            val progressSum = subItems.sumOf { it.progress.toIntOrNull() ?: 0 }
            val avgProgress = if (subItems.isNotEmpty()) progressSum / subItems.size else 0
            
            newList.add(isKollari.MainItem(
                title = branch,
                subItems = subItems.toMutableList(),
                ilerleme = avgProgress.toString(),
                isExpanded = false,
                kacAdet = subItems.size.toString()
            ))
        }

        if (newList.isEmpty()) {
            val hasDeep = authViewModel.hasDeepAuthority(insaatId ?: "", fullPath)
            if (hasDeep && nextLevelLabel != null) {
                binding.tvPermissionWarning.visibility = View.VISIBLE
                binding.layoutPercentPicker.visibility = View.GONE
                binding.tvPermissionWarning.text = "Sizin yetkli olduğunuz işler bu kademeden daha aşağıdadır.\nLütfen yukarıdaki menüden bir ${nextLevelLabel} seçiniz."
                binding.rvWorkItems.visibility = View.GONE
            } else {
                binding.tvPermissionWarning.visibility = View.GONE
                binding.layoutPercentPicker.visibility = View.VISIBLE
                if (gorusAlani == "isler") binding.rvWorkItems.visibility = View.VISIBLE else binding.rvMain.visibility = View.VISIBLE
            }
        } else {
            binding.tvPermissionWarning.visibility = View.GONE
            binding.layoutPercentPicker.visibility = View.VISIBLE
            if (gorusAlani == "isler") binding.rvWorkItems.visibility = View.VISIBLE else binding.rvMain.visibility = View.VISIBLE
        }
        adapter.updateList(newList)
    }
    private fun startNavigation() {
        binding.breadcrumbContainer.removeAllViews()
        
        if (authorizedRoot.isEmpty()) {
            addRootStep()
            addSpinnerStep("Saha", "$insaatPath/sahalar")
        } else {
            // Start from the common authorized root
            val commonRoot = authViewModel.getAuthorizedRoot(insaatId ?: "")
            val levels = commonRoot.split("/")
            
            val label = when (levels.size) {
                1 -> "Saha"
                2, 3 -> "Etap"
                4, 5 -> "Blok"
                else -> "Daire"
            }

            // Find the highest common parent to start the FIRST spinner
            val parentPath = levels.dropLast(1).joinToString("/")
            val firebaseParentPath = if (parentPath.isEmpty()) "$insaatPath/sahalar" else "$insaatPath/sahalar/$parentPath"

            // Find all authorized choices at this specific level
            val user = authViewModel.currentUser.value
            val permissions = user?.projectPermissions?.get(insaatId ?: "") ?: emptyList()
            
            // Siblings are those under the same parent with the same depth as the current common root
            val authorizedSiblings = permissions.filter { 
                it.locationPath.startsWith(parentPath) && 
                it.locationPath.split("/").size >= levels.size // Can lead to a deeper node too
            }

            if (authorizedSiblings.isNotEmpty()) {
                // Helper Trick: Always auto-select the first authorized choice to fill the screen immediately.
                // If there are multiple (e.g. Etap 1, Etap 2), the user can still click the spinner to change it.
                
                val firstAuthorizedPath = authorizedSiblings.first().locationPath
                val choiceIndex = if (levels.size % 2 == 0) levels.size else levels.size - 1
                val firstChoiceValue = firstAuthorizedPath.split("/").getOrNull(choiceIndex) ?: levels.last()

                // 1. Add the spinner step
                addSpinnerStep(label, firebaseParentPath)
                
                // 2. Set its initial text to the first authorized element
                val firstStepView = binding.breadcrumbContainer.getChildAt(0)
                val firstStepBinding = ItemBreadcrumbSpinnerBinding.bind(firstStepView)
                firstStepBinding.tvSpinnerText.text = firstChoiceValue
                
                // 3. Update internal paths
                currentBasePath = "$firebaseParentPath/$firstChoiceValue"
                mulkPath = currentBasePath

                // 4. Trigger next navigation level and data fetch (autoDive = false ile kullanıcının durmasına izin ver)
                handleNextStep(label, firstChoiceValue, firebaseParentPath, autoDive = false)
                fetchTemplatesAndData("$currentBasePath/$gorusAlani", getNextLabel(label))
            }
        }
    }

    private fun addRootStep() {
        val rootBinding = ItemBreadcrumbSpinnerBinding.inflate(layoutInflater)

        rootBinding.tvSpinnerText.text = "Tüm İnşaat"

        currentBasePath = "$insaatPath/sahalar"
        mulkPath = "$insaatPath/sahalar/anaSaha"

        //üstteki seçim butonları ilk açılış
        //gorusAlani ataması AltKademeler olarak tayin edildi.
        binding.toggleisler.visibility =View.GONE
        binding.toggleAltKademeler.isChecked =true
        binding.toggleAltKademeler.isClickable=false
        fetchTemplatesAndData("$insaatPath/sahalar/$gorusAlani")

        binding.myaddbtn.visibility = View.GONE
        binding.btnMassManagement.visibility = View.GONE


        rootBinding.root.setOnClickListener {
            crLevel = "root"
            //üstteki seçim butonları
            binding.toggleisler.visibility =View.GONE
            binding.toggleAltKademeler.visibility=View.VISIBLE//buraya dönmeye çalışıyor ne de olsa
            binding.toggleAltKademeler.isClickable=false
            binding.toggleAltKademeler.isChecked =true
            gorusAlani = "AltKademeler"//firebase için

            fetchTemplatesAndData("$insaatPath/sahalar/$gorusAlani")
            mulkPath = "$insaatPath/sahalar/anaSaha"

            // 0. indexten sonrasını (yani tüm seçimleri) temizle
            val childCount = binding.breadcrumbContainer.childCount
            if (childCount > 1) {
                binding.breadcrumbContainer.removeViews(1, childCount - 1)
            }

            // Temizlikten sonra en başa "Saha Seçiniz"i tekrar getir
            addSpinnerStep("Saha", "$insaatPath/sahalar")

            // Varsa aşağıdaki iş kalemleri listesini de temizle/gizle
            binding.rvWorkItems.visibility = View.GONE
            binding.myaddbtn.visibility = View.GONE // Kök seviyesinde mülk eklenemez
            binding.btnMassManagement.visibility = View.GONE // Kök seviyesinde toplu yönetim kapalı
        }
        binding.breadcrumbContainer.addView(rootBinding.root)
    }
    private fun addSpinnerStep(label: String, firebasePath: String, mycurrentPath: String = "", autoDive: Boolean = false) {
        val spinnerBinding = ItemBreadcrumbSpinnerBinding.inflate(layoutInflater)
        spinnerBinding.tvSpinnerText.text = "$label Seçiniz"

        spinnerBinding.root.tag = label // Current level stored in tag for depth tracking

        spinnerBinding.root.setOnClickListener { view ->
            // Firebase'den o path'teki verileri çek ve popup listesi göster
            fetchDataAndShowPopup(view, firebasePath, isManual = true) { selectedValue ->
            //tıklama emri verilince daire içindeysek

                performSelection(spinnerBinding, label, selectedValue, firebasePath, view)
            }
        }

        binding.breadcrumbContainer.addView(spinnerBinding.root)

        // Aggressive Drill-Down: If autoDive is enabled, check children immediately (Manual = false)
        if (autoDive) {
            fetchDataAndShowPopup(spinnerBinding.root, firebasePath, isManual = false) { selectedValue ->
                performSelection(spinnerBinding, label, selectedValue, firebasePath, spinnerBinding.root)
            }
        }
    }

    private fun performSelection(spinnerBinding: ItemBreadcrumbSpinnerBinding, label: String, selectedValue: String, firebasePath: String, view: View) {
        val index = binding.breadcrumbContainer.indexOfChild(view)
        val isLast = index == binding.breadcrumbContainer.childCount - 1

        // redundant selection prevention: only if it's the last one already.
        // If it's NOT the last one, we MUST proceed to perform the trim (back click).
        if (isLast && spinnerBinding.tvSpinnerText.text == selectedValue) return

        // tıklama veya oto seçim emri verilince daire içindeysek
        if (label == "Daire") {
            binding.toggleisler.visibility = View.VISIBLE
            binding.toggleAltKademeler.visibility = View.GONE
            binding.toggleisler.isClickable = false
            binding.toggleisler.isChecked = true
            gorusAlani = "isler"
            binding.myaddbtn.visibility = View.GONE // Daire içindeyken mülk eklenemez (iş eklenir)
            binding.btnMassManagement.visibility = View.GONE // Daire içindeyken mülk filtrelemeye gerek yok
        } else {
            // Daire seviyesinden yukarı (Blok vb.) geri dönüldüğünde Alt Kademeleri geri getir
            binding.toggleAltKademeler.visibility = View.VISIBLE
            binding.toggleisler.visibility = View.VISIBLE
            binding.toggleisler.isClickable = true
            binding.toggleAltKademeler.isClickable = true
            // Varsayılan olarak Alt Kademeler seçili kalsın (navigasyon özeti için)
            binding.toggleAltKademeler.isChecked = true
            gorusAlani = "AltKademeler"
            binding.myaddbtn.visibility = View.VISIBLE // Saha, Etap, Blok seviyelerinde mülk eklenebilir
            binding.btnMassManagement.visibility = View.VISIBLE // Saha, Etap, Blok seviyelerinde toplu yönetim aktif
        }

        currentBasePath = "$firebasePath/$selectedValue"
        mulkPath = currentBasePath
        spinnerBinding.tvSpinnerText.text = selectedValue

        // Reset visibility if the NEWLY selected path provides direct location permission
        val relativeLocPath = currentBasePath.substringAfter("$insaatPath/sahalar/").removePrefix("/")
        if (authViewModel.hasLocationAuthority(insaatId ?: "", relativeLocPath)) {
            binding.tvPermissionWarning.visibility = View.GONE
            binding.layoutPercentPicker.visibility = View.VISIBLE
        }

        removeStepsAfter(index)

        fetchTemplatesAndData("$currentBasePath/$gorusAlani", getNextLabel(label))

        // Auto-Dive is stopped here for manual interaction. 
        // We only call handleNextStep with autoDive=false to allow user to see the placeholder.
        handleNextStep(label, selectedValue, firebasePath, autoDive = false)
    }
    private fun fetchDataAndShowPopup(anchor: View, path: String, isManual: Boolean = true, onSelected: (String) -> Unit) {
        FirebaseDatabase.getInstance().getReference(path)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val fullRelativeParentPath = path.substringAfter("$insaatPath/sahalar/").removePrefix("/")
                    val currentText = ItemBreadcrumbSpinnerBinding.bind(anchor).tvSpinnerText.text.toString()
                    val allAuthorized = snapshot.children.filter { childSn ->
                        val key = childSn.key ?: return@filter false
                        !key.contains("turleri", ignoreCase = true) &&
                        !key.contains("AltKademeler", ignoreCase = true) &&
                        authViewModel.isNodeVisible(insaatId ?: "", fullRelativeParentPath, key, childSn)
                    }.mapNotNull { it.key }

                    // Auto-dive case: ONLY if NOT manual AND only one choice exists.
                    if (!isManual && allAuthorized.size == 1) {
                        onSelected(allAuthorized[0])
                        return
                    }

                    // 2. Manual Popup display logic
                    if (isManual) {
                        val index = binding.breadcrumbContainer.indexOfChild(anchor)
                        val isLast = index == binding.breadcrumbContainer.childCount - 1

                        if (allAuthorized.isNotEmpty()) {
                            showModernPopup(anchor, allAuthorized, onSelected)
                        } else if (!isLast) {
                            onSelected(currentText)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    private fun showModernPopup(anchor: View, list: List<String>, onSelected: (String) -> Unit) {
        val popupView = layoutInflater.inflate(R.layout.dialog_searchable_spinner, null)
        val etSearch = popupView.findViewById<EditText>(R.id.etSearch)
        val listView = popupView.findViewById<ListView>(R.id.listView)

        // Mevcut listeyi mutable olarak kopyala
        val filteredList = list.toMutableList()
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_text, filteredList)
        listView.adapter = adapter

        // Karakter tabanlı arama için genişlik ayarı
        val popup = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, 700, true)
        
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.elevation = 20f
        popup.isOutsideTouchable = true

        // Arama mantığı
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                filteredList.clear()
                filteredList.addAll(list.filter { it.contains(query, ignoreCase = true) })
                adapter.notifyDataSetChanged()
            }
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            onSelected(filteredList[position])
            popup.dismiss()
        }

        popup.showAsDropDown(anchor, 0, 10)
    }
    private fun handleNextStep(currentLabel: String, selectedValue: String, currentPath: String, autoDive: Boolean = false) {
        when (currentLabel) {
            "Saha" -> {
                crLevel = "sahalar"
                addSpinnerStep("Etap", "$currentPath/$selectedValue/etaplar", "$currentPath/$selectedValue", autoDive)
            }
            "Etap" -> {
                crLevel = "etaplar"
                addSpinnerStep("Blok", "$currentPath/$selectedValue/bloklar", "$currentPath/$selectedValue", autoDive)
            }
            "Blok" -> {
                crLevel = "bloklar"
                addSpinnerStep("Daire", "$currentPath/$selectedValue/daireler", "$currentPath/$selectedValue", autoDive)
            }
            "Daire" -> {
                crLevel = "daireler"
            }
        }
    }
    private fun removeStepsAfter(index: Int) {
        // index -1 gelirse "Tüm İnşaat" hariç her şeyi siler (Tüm İnşaat index 0'da)
        val startIndex = index + 1
        val currentChildCount = binding.breadcrumbContainer.childCount
        if (startIndex < currentChildCount) {
            binding.breadcrumbContainer.removeViews(startIndex, currentChildCount - startIndex)
        }
    }
    private fun  replaceFragment(mfragment: Fragment, fPath:String,){

        val fragment = mfragment
        val bundle = Bundle()
        bundle.putString("fPath", fPath)
        bundle.putString("crLevel", crLevel)
        bundle.putString("insaatID", insaatId)
        fragment.arguments = bundle


        childFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(R.id.frame_layout, fragment)
            .addToBackStack(null)
            .commit()
    }


}

