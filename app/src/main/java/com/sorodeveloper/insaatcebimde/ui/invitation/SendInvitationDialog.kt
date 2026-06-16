package com.sorodeveloper.insaatcebimde.ui.invitation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sorodeveloper.insaatcebimde.domain.model.MemberInfo
import com.sorodeveloper.insaatcebimde.domain.model.MemberScopes
import com.sorodeveloper.insaatcebimde.domain.model.Permission
import com.sorodeveloper.insaatcebimde.domain.model.ProjectNode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendInvitationDialog(
    currentUserMember: MemberInfo,
    projectId: String,
    projectName: String,
    availableNodes: List<ProjectNode>,
    availableCategories: List<String>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSend: (
        inviteeId: String,
        permissions: List<String>,
        scopes: MemberScopes,
        roleName: String
    ) -> Unit
) {
    var inviteeId by remember { mutableStateOf("") }
    var selectedRoleName by remember { mutableStateOf("Çalışan") }
    var selectedPermissions by remember { mutableStateOf(setOf<Permission>()) }
    
    // Kombine Scope Haritası: Key: NodeId, Value: Kategori Listesi
    var nodeCategories by remember { mutableStateOf(mapOf<String, List<String>>()) }
    var isRestricted by remember { mutableStateOf(true) } // Varsayılan olarak kısıtlı, sonradan "Tümüne İzin Ver" yapılabilir
    
    var showPermissionPicker by remember { mutableStateOf(false) }
    var showScopeSheet by remember { mutableStateOf(false) }

    val grantablePermissions = currentUserMember.grantablePermissions()

    val rolePresets = listOf(
        "Proje Müdürü" to Permission.FOREMAN_PRESET.intersect(grantablePermissions),
        "Usta" to Permission.WORKER_PRESET.intersect(grantablePermissions),
        "Özel" to setOf<Permission>()
    )

    // Tümüne izin ver seçeneği için kısıtlama durumu değiştirildiğinde temizle
    LaunchedEffect(isRestricted) {
        if (!isRestricted) {
            nodeCategories = emptyMap()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Ekibe Davet Et", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .widthIn(min = 300.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = inviteeId,
                    onValueChange = { inviteeId = it.uppercase() },
                    label = { Text("Davetiye Kodu") },
                    placeholder = { Text("Örn: UGR-842-193") },
                    leadingIcon = { Icon(Icons.Outlined.QrCode, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Rol Seçin", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rolePresets.forEach { (roleName, permissions) ->
                        FilterChip(
                            selected = selectedRoleName == roleName,
                            onClick = {
                                selectedRoleName = roleName
                                if (roleName != "Özel") selectedPermissions = permissions
                            },
                            label = { Text(roleName, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedCard(
                    onClick = { showPermissionPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Yetkiler", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium))
                            Text(if (selectedPermissions.isEmpty()) "Yetki seçilmedi" else "${selectedPermissions.size} yetki seçili", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }

                // Scope Section
                Text("Mülk & Kategori Kısıtlamaları", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Switch(checked = !isRestricted, onCheckedChange = { isRestricted = !it })
                    Spacer(Modifier.width(8.dp))
                    Text("Tüm Projeye Tam Erişim", style = MaterialTheme.typography.bodyMedium)
                }

                if (isRestricted) {
                    if (nodeCategories.isEmpty()) {
                        Text("Henüz kural eklenmedi. Kullanıcı hiçbir mülkü göremez.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else {
                        nodeCategories.forEach { (nodeId, categories) ->
                            val nodeName = availableNodes.find { it.id == nodeId }?.name ?: "Bilinmeyen Mülk"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(nodeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        val catsText = if (categories.isEmpty()) "Tüm İşler" else categories.joinToString(", ")
                                        Text(catsText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = {
                                            val newMap = nodeCategories.toMutableMap()
                                            newMap.remove(nodeId)
                                            nodeCategories = newMap
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showScopeSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Yeni Kural Ekle")
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
        confirmButton = {
            Button(
                onClick = {
                    onSend(
                        inviteeId.trim(),
                        Permission.toKeys(selectedPermissions),
                        MemberScopes(isRestricted = isRestricted, nodeCategories = nodeCategories),
                        selectedRoleName
                    )
                },
                enabled = inviteeId.trim().isNotEmpty() && selectedPermissions.isNotEmpty() && (!isRestricted || nodeCategories.isNotEmpty()) && !isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Davet Gönder")
                }
            }
        }
    )

    if (showPermissionPicker) {
        PermissionPickerDialog(
            grantablePermissions = grantablePermissions,
            selectedPermissions = selectedPermissions,
            onDismiss = { showPermissionPicker = false },
            onConfirm = { selected ->
                selectedPermissions = selected
                showPermissionPicker = false
            }
        )
    }

    if (showScopeSheet) {
        ModalBottomSheet(onDismissRequest = { showScopeSheet = false }) {
            ScopeSelectionSheetContent(
                availableNodes = availableNodes,
                availableCategories = availableCategories,
                currentUserScopes = currentUserMember.scopes,
                onAddRule = { selectedNodeIds, selectedCats ->
                    val newMap = nodeCategories.toMutableMap()
                    selectedNodeIds.forEach { nId ->
                        val existing = newMap[nId] ?: emptyList()
                        // Eger mevcut veya yeni liste bos ise (Tüm kategoriler), sonuc da bos liste olur.
                        val merged = if (existing.isEmpty() && newMap.containsKey(nId)) {
                            emptyList() // Zaten tum yetkiler var
                        } else if (selectedCats.isEmpty()) {
                            emptyList() // Yeni kural tum yetkileri veriyor
                        } else {
                            (existing + selectedCats).distinct()
                        }
                        newMap[nId] = merged
                    }
                    nodeCategories = newMap
                    showScopeSheet = false
                }
            )
        }
    }
}

@Composable
fun ScopeSelectionSheetContent(
    availableNodes: List<ProjectNode>,
    availableCategories: List<String>,
    currentUserScopes: MemberScopes,
    onAddRule: (List<String>, List<String>) -> Unit
) {
    var currentPath by remember { mutableStateOf(listOf<ProjectNode>()) }
    var selectedNodes by remember { mutableStateOf(setOf<String>()) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }

    // Subset Math Helper Functions
    fun isNodeSelectable(node: ProjectNode): Boolean {
        if (!currentUserScopes.isRestricted) return true
        if (currentUserScopes.nodeCategories.containsKey(node.id)) return true
        for (ancestorId in node.ancestors) {
            if (currentUserScopes.nodeCategories.containsKey(ancestorId)) return true
        }
        return false
    }

    fun isNodeVisible(node: ProjectNode): Boolean {
        if (!currentUserScopes.isRestricted) return true
        if (isNodeSelectable(node)) return true
        // Atasi oldugu child'lar kullaniciya aciksa visible yap
        return currentUserScopes.nodeCategories.keys.any { allowedId ->
            val allowedNode = availableNodes.find { it.id == allowedId }
            allowedNode?.ancestors?.contains(node.id) == true
        }
    }

    val currentParentId = currentPath.lastOrNull()?.id
    val levelNodes = availableNodes.filter { it.parentId == currentParentId && isNodeVisible(it) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
        Text("Kural Ekle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Breadcrumb
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Saha", modifier = Modifier.clickable { currentPath = emptyList() }, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            currentPath.forEachIndexed { index, node ->
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Text(node.name, modifier = Modifier.clickable { currentPath = currentPath.take(index + 1) }, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("Mülk Seçimi (Çoklu Seçim)", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(levelNodes) { node ->
                val hasChildren = availableNodes.any { it.parentId == node.id && isNodeVisible(it) }
                val selectable = isNodeSelectable(node)
                val isSelected = selectedNodes.contains(node.id)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
                        if (selectable) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedNodes = selectedNodes + node.id
                                    else selectedNodes = selectedNodes - node.id
                                },
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Spacer(Modifier.width(12.dp)) // Checkbox padding
                        }
                        
                        Text(node.name, modifier = Modifier.weight(1f).clickable {
                            if (selectable) {
                                if (isSelected) selectedNodes = selectedNodes - node.id
                                else selectedNodes = selectedNodes + node.id
                            }
                        }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                        
                        if (hasChildren) {
                            IconButton(onClick = { currentPath = currentPath + node }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "İçine Gir", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Text("Hangi İşleri Görebilsin?", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selectedCategories.isEmpty(), onCheckedChange = { if (it) selectedCategories = emptySet() })
            Text("Tüm İşler (Kısıtlama Yok)", style = MaterialTheme.typography.bodyMedium)
        }
        
        // Subset math for categories: Intersection of allowed categories for selected nodes
        // Simplified for UI: we just show all availableCategories, but server will reject if trying to give more than allowed.
        // Actually, we can just use availableCategories.
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(availableCategories) { cat ->
                FilterChip(
                    selected = selectedCategories.contains(cat),
                    onClick = {
                        if (selectedCategories.contains(cat)) selectedCategories = selectedCategories - cat
                        else selectedCategories = selectedCategories + cat
                    },
                    label = { Text(cat) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onAddRule(selectedNodes.toList(), selectedCategories.toList()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedNodes.isNotEmpty(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Kuralı Ekle (${selectedNodes.size} Mülk)")
        }
    }
}

@Composable
private fun PermissionPickerDialog(
    grantablePermissions: Set<Permission>,
    selectedPermissions: Set<Permission>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Permission>) -> Unit
) {
    var tempSelected by remember { mutableStateOf(selectedPermissions) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Yetkileri Seçin", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                grantablePermissions.forEach { permission ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = permission in tempSelected, onCheckedChange = { checked -> tempSelected = if (checked) tempSelected + permission else tempSelected - permission })
                        Spacer(Modifier.width(8.dp))
                        Text(permission.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
        confirmButton = { Button(onClick = { onConfirm(tempSelected) }) { Text("Tamam") } }
    )
}
