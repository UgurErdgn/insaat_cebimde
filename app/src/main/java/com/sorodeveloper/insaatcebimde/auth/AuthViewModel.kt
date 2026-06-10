package com.sorodeveloper.insaatcebimde.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.sorodeveloper.insaatcebimde.model.MatrixPermission
import com.sorodeveloper.insaatcebimde.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("users")

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Optimization: projectId -> normalizedLocationPath -> Set<normalizedJobPath>
    private val _permissionIndex = MutableStateFlow<Map<String, Map<String, Set<String>>>>(emptyMap())
    val permissionIndex: StateFlow<Map<String, Map<String, Set<String>>>> = _permissionIndex.asStateFlow()

    init {
        // Automatically fetch profile if already logged in
        auth.currentUser?.let { fetchUserProfile(it.uid) }
    }

    fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val snapshot = database.child(uid).get().await()
                var user = snapshot.getValue(User::class.java)
                
                // Migration: If user exists but has no publicInviteId, generate and save it
                _currentUser.value = user
                rebuildPermissionIndex(user!!)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun rebuildPermissionIndex(user: User) {
        val newIndex = mutableMapOf<String, MutableMap<String, MutableSet<String>>>()
        user.projectPermissions.forEach { (projectId, perms) ->
            val projectMap = newIndex.getOrPut(projectId) { mutableMapOf() }
            perms.forEach { perm ->
                val loc = normalizeLoc(perm.locationPath)
                val job = perm.jobPath // Normalized implicitly or via check
                projectMap.getOrPut(loc) { mutableSetOf() }.add(job)
            }
        }
        _permissionIndex.value = newIndex
    }

    // --- Matrix Permission Helpers ---

    /**
     * Checks if a user has any permission that covers the given location AND job paths.
     * Use this to check if a specific action or view is allowed.
     */
    fun hasPermission(projectId: String, locationPath: String, jobPath: String): Boolean {
        val projectMap = _permissionIndex.value[projectId] ?: return false
        val normalizedLoc = normalizeLoc(locationPath)

        return projectMap.any { (pLoc, jobs) ->
            // Pass-through check
            val locMatch = normalizedLoc.startsWith(pLoc) || pLoc == ""
            if (!locMatch) return@any false

            // Correct job or general permission
            jobs.any { pJob -> pJob == "" || pJob == jobPath }
        }
    }

    /**
     * Determines the "locked" root for a user in a specific project.
     * If a user is only authorized for a specific sub-node, their navigation starts there.
     */
    fun getAuthorizedRoot(projectId: String): String {
        val user = _currentUser.value ?: return ""
        val permissions = user.projectPermissions[projectId] ?: return ""
        
        if (permissions.isEmpty()) return ""
        
        // If there's only one permission, that's the root.
        if (permissions.size == 1) return permissions[0].locationPath
        
        // If multiple, find the common longest prefix.
        // For simplicity, if they have multiple distinct roots, we start at the highest common point.
        var commonPrefix = permissions[0].locationPath
        for (i in 1 until permissions.size) {
            val path = permissions[i].locationPath
            var j = 0
            while (j < commonPrefix.length && j < path.length && commonPrefix[j] == path[j]) {
                j++
            }
            commonPrefix = commonPrefix.substring(0, j).removeSuffix("/")
        }
        return commonPrefix
    }

    /**
     * Checks if a specific node should be visible based on its parent path.
     */
    /**
     * Checks if a navigation node (location) should be visible in 'AltKademeler'.
     * Logic: Visible if (Location Match) AND (Has at least one compatible job at this level).
     */
    fun isNodeVisible(projectId: String, parentPath: String, nodeName: String, nodeSnapshot: DataSnapshot): Boolean {
        val projectMap = _permissionIndex.value[projectId] ?: return false
        val fullPath = if (parentPath.isEmpty()) nodeName else "$parentPath/$nodeName"
        val logicalLoc = normalizeLoc(fullPath)

        return projectMap.any { (pLoc, jobs) ->
            // PASS THROUGH MATCH: Current location is parent of authority OR authority is parent of current
            val locMatch = pLoc == "" || logicalLoc.startsWith(pLoc) || pLoc.startsWith(logicalLoc)
            if (!locMatch) return@any false

            // Branch filtering: If the node Name matches our branch OR it has deep authorized content.
            val jobMatch = jobs.any { pJob -> pJob == "" || pJob == nodeName }
            jobMatch || hasAuthorizedWorkDeepByIndex(projectId, nodeSnapshot)
        }
    }

    private fun normalizeLoc(path: String): String {
        return path.substringBefore("/AltKademeler")
            .substringBefore("/isler")
            .removeSuffix("/AltKademeler")
            .removeSuffix("/isler")
    }

    private fun hasAuthorizedWorkDeepByIndex(projectId: String, snapshot: DataSnapshot): Boolean {
        val projectMap = _permissionIndex.value[projectId] ?: return false
        
        // 1. Check current level jobs (under "isler")
        val islerSn = snapshot.child("isler")
        if (islerSn.exists()) {
            val hasJobMatch = islerSn.children.any { jobSn ->
                val jobName = jobSn.key ?: ""
                projectMap.any { (pLoc, jobs) ->
                    // For jobs at this exact location, we only check the jobPath
                    // No, wait! We need to know if pLoc covers this snapshot's location?
                    // This recursive helper is a bit tricky with Index.
                    // Actually, let's simplify: Does the user have ANY permission that matches this job name?
                    jobs.any { pJob -> pJob == "" || pJob == jobName }
                }
            }
            if (hasJobMatch) return true
        }

        // 2. Check all possible structural sub-nodes
        val subNodes = listOf("sahalar", "etaplar", "bloklar", "daireler", "AltKademeler")
        for (nodeName in subNodes) {
            val childLevel = snapshot.child(nodeName)
            if (childLevel.exists()) {
                for (childSn in childLevel.children) {
                    val childKey = childSn.key ?: ""
                    val jobMatchByChildKey = projectMap.any { (_, jobs) ->
                        jobs.any { pJob -> pJob == "" || pJob == childKey }
                    }
                    if (jobMatchByChildKey || hasAuthorizedWorkDeepByIndex(projectId, childSn)) return true
                }
            }
        }
        return false
    }

    /**
     * Checks if the user has direct location authority (regardless of jobPath).
     * Returns true if any permission covers this location.
     */
    fun hasLocationAuthority(projectId: String, locationPath: String): Boolean {
        val projectMap = _permissionIndex.value[projectId] ?: return false
        val normalizedLoc = normalizeLoc(locationPath)

        return projectMap.keys.any { pLoc ->
            normalizedLoc.startsWith(pLoc) || pLoc == ""
        }
    }

    fun isJobVisible(projectId: String, relativeLocPath: String, jobPath: String): Boolean {
        val projectMap = _permissionIndex.value[projectId] ?: return false
        val normalizedLoc = normalizeLoc(relativeLocPath)

        return projectMap.any { (pLoc, jobs) ->
            // STRICT MATCH: Only show if authority is at this level or above
            val locMatch = pLoc == "" || normalizedLoc.startsWith(pLoc)
            if (!locMatch) return@any false

            jobs.any { pJob -> pJob == "" || pJob == jobPath }
        }
    }

    fun hasDeepAuthority(projectId: String, relativePath: String): Boolean {
        val projectMap = _permissionIndex.value[projectId] ?: return false
        val normalizedRel = normalizeLoc(relativePath)

        return projectMap.keys.any { pLoc ->
            // Case 1: Permission is inside the relative path (path="etap1", perm="etap1/blok1")
            (pLoc.startsWith(normalizedRel) && pLoc != normalizedRel) ||
            // Case 2: Parent authority but specifically for a deeper job? 
            (normalizedRel == "" && pLoc != "")
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                result.user?.let { fetchUserProfile(it.uid) }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Giriş başarısız oldu")
            }
        }
    }

    fun register(email: String, pass: String, name: String, phone: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = authResult.user?.uid
                
                if (uid != null) {
                    val newUser = User(
                        uid = uid,
                        email = email,
                        name = name,
                        phone = phone,
                        publicInviteId = generateInviteId(),
                        projectPermissions = emptyMap()
                    )
                    database.child(uid).setValue(newUser).await()
                    _currentUser.value = newUser
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Kullanıcı oluşturulamadı")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Kayıt başarısız oldu")
            }
        }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    private fun generateInviteId(): String {
        return UUID.randomUUID().toString().substring(0, 8).uppercase()
    }
}
