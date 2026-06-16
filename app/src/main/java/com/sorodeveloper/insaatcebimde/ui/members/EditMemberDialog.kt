package com.sorodeveloper.insaatcebimde.ui.members

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sorodeveloper.insaatcebimde.domain.model.MemberInfo
import com.sorodeveloper.insaatcebimde.domain.model.MemberScopes
import com.sorodeveloper.insaatcebimde.domain.model.Permission
import com.sorodeveloper.insaatcebimde.domain.model.ProjectNode
import com.sorodeveloper.insaatcebimde.ui.invitation.PermissionPickerDialog
import com.sorodeveloper.insaatcebimde.ui.invitation.ScopeSelectionSheetContent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditMemberDialog(
    member: MemberInfo,
    availableNodes: List<ProjectNode>,
    availableCategories: List<String>,
    grantablePermissions: Set<Permission>,
    currentUserScopes: MemberScopes,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (MemberScopes, Set<Permission>, String) -> Unit
) {
    var isRestricted by remember { mutableStateOf(member.scopes.restricted) }
    var nodeCategories by remember { mutableStateOf(member.scopes.nodeCategories) }
    var selectedPermissions by remember { mutableStateOf(Permission.fromKeys(member.permissions).toSet()) }
    var selectedRoleName by remember { mutableStateOf(member.roleName) }

    var showPermissionPicker by remember { mutableStateOf(false) }
    var showScopeSheet by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TopAppBar(
                    title = { Text("Yetkileri Düzenle") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Kapat")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    // Kullanıcı Bilgisi Özeti
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.displayName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(""),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(member.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }

                    // Rol ve Temel İzinler
                    item {
                        Text("Temel Rol ve İzinler", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = selectedRoleName,
                            onValueChange = { selectedRoleName = it },
                            label = { Text("Rol Adı (Örn: Boya Ustası)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Yetkiler", fontWeight = FontWeight.SemiBold)
                                    TextButton(onClick = { showPermissionPicker = true }) {
                                        Text("Düzenle")
                                    }
                                }
                                
                                if (selectedPermissions.isEmpty()) {
                                    Text("Hiçbir yetki seçilmedi (Sadece salt okunur).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                } else {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        selectedPermissions.forEach { perm ->
                                            InputChip(
                                                selected = true,
                                                onClick = {
                                                    selectedPermissions = selectedPermissions - perm
                                                },
                                                label = { Text(perm.displayName, style = MaterialTheme.typography.labelSmall) },
                                                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

                    // Kapsam (Scope) Yönetimi
                    item {
                        Text("Mülk ve İş Kısıtlamaları", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRestricted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isRestricted) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Kısıtlı Erişim",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRestricted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isRestricted) "Sadece aşağıda belirtilen mülkleri ve işleri görebilir." else "Kısıtlama yok. Projedeki tüm mülkleri ve işleri görebilir (Tam Erişim).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isRestricted,
                                    onCheckedChange = { kısıtlı -> isRestricted = kısıtlı }
                                )
                            }
                        }
                    }

                    if (isRestricted) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Mevcut Kurallar", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                
                                Button(
                                    onClick = { showScopeSheet = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Yeni Kural Ekle", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        if (nodeCategories.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Hiçbir kural eklenmedi. Kullanıcı projede hiçbir şey göremez.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            items(nodeCategories.entries.toList()) { (nodeId, categories) ->
                                val nodeName = availableNodes.find { it.id == nodeId }?.name ?: "Bilinmeyen Mülk"
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(nodeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            }
                                            IconButton(
                                                onClick = {
                                                    val newMap = nodeCategories.toMutableMap()
                                                    newMap.remove(nodeId)
                                                    nodeCategories = newMap
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Mülkü Sil", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                        
                                        Spacer(Modifier.height(8.dp))
                                        
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (categories.isEmpty()) {
                                                AssistChip(
                                                    onClick = {},
                                                    label = { Text("Tüm İşler") },
                                                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                                    border = null
                                                )
                                            } else {
                                                categories.forEach { cat ->
                                                    InputChip(
                                                        selected = true,
                                                        onClick = {
                                                            // İş kategorisini sil
                                                            val newMap = nodeCategories.toMutableMap()
                                                            val updatedCats = categories - cat
                                                            if (updatedCats.isEmpty()) {
                                                                // Eger kategoriler biterse, kurali sil? Yoksa "Tum isler" mi olur?
                                                                // Tum isler olmamasi icin kurali komple silebiliriz, veya hata verebiliriz.
                                                                // Biz komple kurali silelim mantikli olan o
                                                                newMap.remove(nodeId)
                                                            } else {
                                                                newMap[nodeId] = updatedCats
                                                            }
                                                            nodeCategories = newMap
                                                        },
                                                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Sil", modifier = Modifier.size(14.dp)) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }

                // Footer
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss, enabled = !isSaving) {
                            Text("İptal")
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                onSave(
                                    MemberScopes(restricted = isRestricted, nodeCategories = nodeCategories),
                                    selectedPermissions,
                                    selectedRoleName
                                )
                            },
                            enabled = !isSaving && (!isRestricted || nodeCategories.isNotEmpty()),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Değişiklikleri Kaydet")
                            }
                        }
                    }
                }
            }
        }
    }

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
        ModalBottomSheet(
            onDismissRequest = { showScopeSheet = false }
        ) {
            ScopeSelectionSheetContent(
                availableNodes = availableNodes,
                availableCategories = availableCategories,
                currentUserScopes = currentUserScopes,
                onAddRule = { selectedNodeIds, selectedCats ->
                    val newMap = nodeCategories.toMutableMap()
                    selectedNodeIds.forEach { nId ->
                        val existing = newMap[nId] ?: emptyList()
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
