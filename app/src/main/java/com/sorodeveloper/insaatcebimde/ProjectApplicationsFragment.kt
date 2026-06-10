package com.sorodeveloper.insaatcebimde

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sorodeveloper.insaatcebimde.adapter.InviteAdapter
import com.sorodeveloper.insaatcebimde.adapter.PendingPermissionAdapter
import com.sorodeveloper.insaatcebimde.databinding.DialogFindUserByIdBinding
import com.sorodeveloper.insaatcebimde.model.Invite
import com.sorodeveloper.insaatcebimde.databinding.FragmentProjectApplicationsBinding
import com.sorodeveloper.insaatcebimde.model.MatrixPermission
import com.sorodeveloper.insaatcebimde.model.ProjectNode
import com.sorodeveloper.insaatcebimde.model.JobNode
import com.sorodeveloper.insaatcebimde.model.User
import com.sorodeveloper.insaatcebimde.adapter.KalemCheckboxAdapter
import com.sorodeveloper.insaatcebimde.databinding.ItemKalemCheckboxBinding
import com.sorodeveloper.insaatcebimde.model.JobNodeType

class ProjectApplicationsFragment : Fragment() {

    private var _binding: FragmentProjectApplicationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var insaatID: String
    private lateinit var insaatName: String
    private val invitesList = mutableListOf<Invite>()
    private lateinit var adapter: InviteAdapter

    private lateinit var db : FirebaseDatabase
    private var cachedTemplates: DataSnapshot? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectApplicationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        insaatID = arguments?.getString("insaatID") ?: return
        insaatName = arguments?.getString("insaatAdi") ?: return
        Toast.makeText(requireContext(),insaatName, Toast.LENGTH_SHORT).show()
        db = FirebaseDatabase.getInstance()

        setupRecyclerView()
        fetchSentInvites()

        binding.btnInviteUser.setOnClickListener {
            showFindUserDialog()
        }
        
        fetchTemplates()
    }

    private fun fetchTemplates() {
        db.getReference("insaatlar").child(insaatID).child("templates").child("jobs")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    cachedTemplates = snapshot
                    if (snapshot.childrenCount == 0L) {
                        // Toast alert if library is empty
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }


    private fun setupRecyclerView() {
        adapter = InviteAdapter(
            invites = invitesList,
            onAccept = { /* Admin might not need to accept their own sent invite */ },
            onReject = { invite -> revokeInvite(invite) }
        )
        binding.rvApplications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApplications.adapter = adapter
    }

    private fun fetchSentInvites() {
        val invitesRef = db.getReference("insaatlar").child(insaatID).child("invites")

        invitesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                invitesList.clear()
                for (inviteSnapshot in snapshot.children) {
                    val invite = inviteSnapshot.getValue(Invite::class.java)
                    if (invite != null && invite.status == "pending") {
                        invitesList.add(invite)
                    }
                }
                invitesList.sortByDescending { it.createdAt }
                adapter.notifyDataSetChanged()
                
                if (invitesList.isEmpty()) {
                    binding.tvEmptyApplications.visibility = View.VISIBLE
                    binding.rvApplications.visibility = View.GONE
                } else {
                    binding.tvEmptyApplications.visibility = View.GONE
                    binding.rvApplications.visibility = View.VISIBLE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showFindUserDialog() {
        val dialog = Dialog(requireContext())
        val dialogBinding = DialogFindUserByIdBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        var foundUser: User? = null

        dialogBinding.btnSearchUser.setOnClickListener {
            val inviteId = dialogBinding.etInviteId.text.toString().uppercase()
            if (inviteId.length < 8) {
                Toast.makeText(requireContext(), "Lütfen 8 haneli kodu girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.getReference("users").orderByChild("publicInviteId").equalTo(inviteId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userSnap = snapshot.children.firstOrNull()
                        if (userSnap != null) {
                            foundUser = userSnap.getValue(User::class.java)
                            dialogBinding.llUserResult.visibility = View.VISIBLE
                            dialogBinding.tvFoundUserName.text = foundUser?.name
                            dialogBinding.tvFoundUserEmail.text = foundUser?.email
                            dialogBinding.btnNextStep.visibility = View.VISIBLE
                            dialogBinding.btnSearchUser.visibility = View.GONE
                        } else {
                            Toast.makeText(requireContext(), "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
                            dialogBinding.llUserResult.visibility = View.GONE
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        dialogBinding.btnNextStep.setOnClickListener {
            foundUser?.let { user ->
                dialog.dismiss()
                showGrantPermissionDialog(user)
            }
        }

        dialogBinding.btnCancelSearch.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showGrantPermissionDialog(targetUser: User) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_grant_multi_permission)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // ... (window setup code same as before)
        val lp = android.view.WindowManager.LayoutParams()
        lp.copyFrom(dialog.window?.attributes)
        lp.width = (resources.displayMetrics.widthPixels * 0.95).toInt()
        lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = lp

        val tvApplicantName = dialog.findViewById<TextView>(R.id.tvApplicantName)
        val spSaha = dialog.findViewById<Spinner>(R.id.spSaha)
        val spEtap = dialog.findViewById<Spinner>(R.id.spEtap)
        val spBlok = dialog.findViewById<Spinner>(R.id.spBlok)
        val spDaire = dialog.findViewById<Spinner>(R.id.spDaire)
        val spAnaIs = dialog.findViewById<Spinner>(R.id.spAnaIs)
        val spCategory = dialog.findViewById<Spinner>(R.id.spCategory)
        val tvCategoryLabel = dialog.findViewById<TextView>(R.id.tvCategoryLabel)
        val cbCanDelegate = dialog.findViewById<CheckBox>(R.id.cbCanDelegate)
        
        val btnAddPermission = dialog.findViewById<Button>(R.id.btnAddPermission)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelGrant)
        val btnSubmit = dialog.findViewById<Button>(R.id.btnSubmitGrant)
        val rvPendingPermissions = dialog.findViewById<RecyclerView>(R.id.rvPendingPermissions)
        val rvKalemler = dialog.findViewById<RecyclerView>(R.id.rvKalemler)

        tvApplicantName.text = "${targetUser.name} için davet yetkisi tanımlıyorsunuz."

        val pendingList = mutableListOf<MatrixPermission>()
        val pendingAdapter = PendingPermissionAdapter(pendingList) { position ->
            pendingList.removeAt(position)
            rvPendingPermissions.adapter?.notifyItemRemoved(position)
            rvPendingPermissions.adapter?.notifyItemRangeChanged(position, pendingList.size)
        }
        rvPendingPermissions.layoutManager = LinearLayoutManager(requireContext())
        rvPendingPermissions.adapter = pendingAdapter

        val kalemAdapter = KalemCheckboxAdapter(emptyList())
        rvKalemler.layoutManager = LinearLayoutManager(requireContext())
        rvKalemler.adapter = kalemAdapter

        val sahalar = mutableListOf<ProjectNode>()
        val etaps = mutableListOf<ProjectNode>()
        val bloks = mutableListOf<ProjectNode>()
        val daires = mutableListOf<ProjectNode>()

        fun setupSpinner(spinner: Spinner, items: List<String>) {
            val adapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, items)
            spinner.adapter = adapter
        }

        fun populateCategories(branch: String) {
            val templates = cachedTemplates ?: return
            
            if (branch == "Tüm İşler (ALL)") {
                spCategory.visibility = View.GONE
                tvCategoryLabel.visibility = View.GONE
                kalemAdapter.updateData(emptyList())
                return
            }

            val categories = templates.children.filter {
                val b = it.child("branch").getValue(String::class.java) ?: ""
                b.equals(branch, ignoreCase = true)
            }.mapNotNull { it.child("category").getValue(String::class.java) }.distinct().sorted()
            
            val catItems = mutableListOf("Kategori Seçin (Select)")
            catItems.addAll(categories)
            setupSpinner(spCategory, catItems)
            
            spCategory.visibility = View.VISIBLE
            tvCategoryLabel.visibility = View.VISIBLE
            kalemAdapter.updateData(emptyList())
        }

        fun refreshKalemList() {
            val selectedBranch = spAnaIs.selectedItem?.toString()?.trim() ?: "Tüm İşler (ALL)"
            val selectedCategory = spCategory.selectedItem?.toString()?.trim() ?: ""
            val templates = cachedTemplates ?: return
            
            if (selectedBranch == "Tüm İşler (ALL)") {
                // If All Jobs selected, we allow adding a permission for the whole location with empty jobPath
                kalemAdapter.updateData(emptyList())
                rvKalemler.visibility = View.GONE
                return
            }

            if (selectedCategory == "Kategori Seçin (Select)") {
                kalemAdapter.updateData(emptyList())
                rvKalemler.visibility = View.GONE
                return
            }

            val filteredItems = mutableListOf<JobNode>()
            
            for (child in templates.children) {
                val templateId = child.key ?: continue
                val branch = child.child("branch").getValue(String::class.java)?.trim() ?: ""
                val category = child.child("category").getValue(String::class.java)?.trim() ?: ""
                val type = child.child("type").getValue(String::class.java)?.trim() ?: ""

                val branchMatch = branch.equals(selectedBranch, ignoreCase = true)
                val categoryMatch = category.equals(selectedCategory, ignoreCase = true)

                if (branchMatch && categoryMatch) {
                    val displayName = if (type.isNotEmpty()) "$category ($type)" else category
                    filteredItems.add(JobNode(id = templateId, name = displayName, type = JobNodeType.ITEM))
                }
            }

            kalemAdapter.updateData(filteredItems)
            rvKalemler.visibility = if (filteredItems.isNotEmpty()) View.VISIBLE else View.GONE
        }

        fun populateBranches() {
            val templates = cachedTemplates
            if (templates == null) {
                setupSpinner(spAnaIs, listOf("Veri bekleniyor..."))
                return
            }

            val libraryBranches = templates.children.mapNotNull { 
                it.child("branch").getValue(String::class.java) 
            }.distinct().sorted()
            
            val branchSpinnerItems = mutableListOf("Tüm İşler (ALL)")
            branchSpinnerItems.addAll(libraryBranches)
            setupSpinner(spAnaIs, branchSpinnerItems)
        }

        if (cachedTemplates == null) {
            setupSpinner(spSaha, listOf("Tümü (ALL)"))
            setupSpinner(spAnaIs, listOf("Yükleniyor..."))
            setupSpinner(spCategory, listOf("Bekleniyor..."))
            
            db.getReference("insaatlar").child(insaatID).child("templates").child("jobs")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        cachedTemplates = snapshot
                        if (snapshot.childrenCount == 0L) {
                            Toast.makeText(requireContext(), "Veri tabanında (templates/jobs) hiç iş kaydı yok!", Toast.LENGTH_LONG).show()
                        }
                        populateBranches()
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Şablonlar yüklenemedi!", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            setupSpinner(spSaha, listOf("Tümü (ALL)"))
            populateBranches()
        }

        db.getReference("insaatlar").child(insaatID).child("sahalar")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    sahalar.clear()
                    val sahaNames = mutableListOf("Tümü (ALL)")
                    for (child in snapshot.children) {
                        if (child.key?.contains("turleri", ignoreCase = true) == true) continue
                        val name = child.key ?: ""
                        sahalar.add(ProjectNode(id = name, name = name))
                        sahaNames.add(name)
                    }
                    setupSpinner(spSaha, sahaNames)
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        fun getCurrentLocationPath(): String {
            return when {
                spDaire.visibility == View.VISIBLE && spDaire.selectedItemPosition > 0 -> {
                    val sahaId = sahalar[spSaha.selectedItemPosition-1].id
                    val etapId = etaps[spEtap.selectedItemPosition-1].id
                    val blokId = bloks[spBlok.selectedItemPosition-1].id
                    val daireId = daires[spDaire.selectedItemPosition-1].id
                    "$sahaId/etaplar/$etapId/bloklar/$blokId/daireler/$daireId"
                }
                spBlok.visibility == View.VISIBLE && spBlok.selectedItemPosition > 0 -> {
                    val sahaId = sahalar[spSaha.selectedItemPosition-1].id
                    val etapId = etaps[spEtap.selectedItemPosition-1].id
                    val blokId = bloks[spBlok.selectedItemPosition-1].id
                    "$sahaId/etaplar/$etapId/bloklar/$blokId"
                }
                spEtap.visibility == View.VISIBLE && spEtap.selectedItemPosition > 0 -> {
                    val sahaId = sahalar[spSaha.selectedItemPosition-1].id
                    val etapId = etaps[spEtap.selectedItemPosition-1].id
                    "$sahaId/etaplar/$etapId"
                }
                spSaha.selectedItemPosition > 0 -> {
                    val sahaId = sahalar[spSaha.selectedItemPosition-1].id
                    sahaId
                }
                else -> "anaSaha"
            }
        }

        spSaha.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    spEtap.visibility = View.GONE
                    spBlok.visibility = View.GONE
                    spDaire.visibility = View.GONE
                } else {
                    val sahaId = sahalar[position - 1].id
                    val sahaPath = "insaatlar/$insaatID/sahalar/$sahaId"
                    
                    db.getReference(sahaPath).child("etaplar")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                etaps.clear()
                                val names = mutableListOf("Tümü (ALL)")
                                for (child in snapshot.children) {
                                    val name = child.key ?: ""
                                    etaps.add(ProjectNode(id = name, name = name))
                                    names.add(name)
                                }
                                setupSpinner(spEtap, names)
                                spEtap.visibility = View.VISIBLE
                                spEtap.setSelection(0)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        spEtap.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    spBlok.visibility = View.GONE
                    spDaire.visibility = View.GONE
                } else {
                    val sahaId = sahalar[spSaha.selectedItemPosition - 1].id
                    val etapId = etaps[position - 1].id
                    val etapPath = "insaatlar/$insaatID/sahalar/$sahaId/etaplar/$etapId"

                    db.getReference(etapPath).child("bloklar")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                bloks.clear()
                                val names = mutableListOf("Tümü (ALL)")
                                for (child in snapshot.children) {
                                    val name = child.key ?: ""
                                    bloks.add(ProjectNode(id = name, name = name))
                                    names.add(name)
                                }
                                setupSpinner(spBlok, names)
                                spBlok.visibility = View.VISIBLE
                                spBlok.setSelection(0)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        spBlok.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    spDaire.visibility = View.GONE
                } else {
                    val sahaId = sahalar[spSaha.selectedItemPosition - 1].id
                    val etapId = etaps[spEtap.selectedItemPosition - 1].id
                    val blokId = bloks[position - 1].id
                    val blokPath = "insaatlar/$insaatID/sahalar/$sahaId/etaplar/$etapId/bloklar/$blokId"

                    db.getReference(blokPath).child("daireler")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                daires.clear()
                                val names = mutableListOf("Tümü (ALL)")
                                for (child in snapshot.children) {
                                    val name = child.key ?: ""
                                    daires.add(ProjectNode(id = name, name = name))
                                    names.add(name)
                                }
                                setupSpinner(spDaire, names)
                                spDaire.visibility = View.VISIBLE
                                spDaire.setSelection(0)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        spAnaIs.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                populateCategories(parent?.getItemAtPosition(position).toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                refreshKalemList()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnAddPermission.setOnClickListener {
            val locPath = getCurrentLocationPath()
            // Build Job Paths from selected checkboxes
            val selectedJobs = kalemAdapter.checkedItems.toList()
            
            if (selectedJobs.isEmpty()) {
                val isSel = spAnaIs.selectedItem?.toString() ?: "Tüm İşler (ALL)"
                if (isSel == "Tüm İşler (ALL)") {
                    // Overall project/branch permission logic could go here, 
                    // but per user request we focus on specific IDs.
                    // For now, allow adding "All" if nothing selected.
                    val perm = MatrixPermission(locPath, "", cbCanDelegate.isChecked)
                    if (!pendingList.any { it.locationPath == locPath && it.jobPath == "" }) {
                        pendingList.add(perm)
                        pendingAdapter.notifyItemInserted(pendingList.size - 1)
                    }
                } else {
                    Toast.makeText(requireContext(), "Lütfen en az bir kalem seçin veya 'Tüm İşler'i seçin.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                selectedJobs.forEach { jobNode ->
                    val jobPath = jobNode.id // This is the UUID/Barkod
                    if (!pendingList.any { it.locationPath == locPath && it.jobPath == jobPath }) {
                        pendingList.add(MatrixPermission(locPath, jobPath, cbCanDelegate.isChecked))
                    }
                }
                pendingAdapter.notifyDataSetChanged()
            }

            // Reset checkboxes and selections
            kalemAdapter.checkedItems.clear()
            kalemAdapter.notifyDataSetChanged()
            cbCanDelegate.isChecked = false
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            if (pendingList.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen en az bir yetki ekleyin.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendInvites(targetUser, pendingList)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun sendInvites(targetUser: User, permissions: List<MatrixPermission>) {
        val currentAdminUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentAdminName = "Yetkili Kullanıcı" // Future: Get from AuthViewModel
        
        permissions.forEach { perm ->
            val inviteId = db.getReference("users").child(targetUser.uid).child("invites").push().key ?: return@forEach
            val newInvite = Invite(
                inviteId = inviteId,
                senderUid = currentAdminUid,
                receiverUid = targetUser.uid,
                senderName = currentAdminName,
                insaatId = insaatID,
                insaatName = insaatName,
                locationPath = perm.locationPath,
                jobPath = perm.jobPath,
                canDelegate = perm.canDelegate,
                status = "pending",
                createdAt = System.currentTimeMillis()
            )

            val updates = mapOf<String, Any>(
                "users/${targetUser.uid}/invites/$inviteId" to newInvite,
                "insaatlar/$insaatID/invites/$inviteId" to newInvite
            )

            db.reference.updateChildren(updates)
        }
        Toast.makeText(requireContext(), "Davetiye gönderildi.", Toast.LENGTH_SHORT).show()
    }

    private fun revokeInvite(invite: Invite) {
        val updates = mapOf<String, Any?>(
            "users/${invite.receiverUid}/invites/${invite.inviteId}" to null,
            "insaatlar/${invite.insaatId}/invites/${invite.inviteId}" to null
        )
        db.reference.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(requireContext(), "Davetiye silindi.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
