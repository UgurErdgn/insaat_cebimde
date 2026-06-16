package com.sorodeveloper.insaatcebimde.ui.invitation

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sorodeveloper.insaatcebimde.domain.model.MemberInfo
import com.sorodeveloper.insaatcebimde.domain.model.MemberScopes
import com.sorodeveloper.insaatcebimde.domain.model.Permission

/**
 * Davet Gönderme Dialogu.
 *
 * Akış:
 * 1. Şef, hedef kişinin Davetiye ID'sini girer (Örn: "UGR-842-193")
 * 2. Rol şablonu seçer (Usta/Kalfa/Özel) → İlgili yetki setini otomatik doldurur
 * 3. Opsiyonel: Düğüm ve Kategori kapsamı belirler
 * 4. "Davet Gönder" → InvitationRepository.sendInvitation → Cloud Function kontrolü
 *
 * currentUserMember: Davet eden kişinin bilgileri (grantable yetkiler için)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendInvitationDialog(
    currentUserMember: MemberInfo,
    projectId: String,
    projectName: String,
    availableNodes: List<Pair<String, String>>, // (nodeId, nodeName)
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
    var selectedNodes by remember { mutableStateOf(listOf<String>()) }
    var selectedCategories by remember { mutableStateOf(listOf<String>()) }
    var showPermissionPicker by remember { mutableStateOf(false) }
    var showNodePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val grantablePermissions = currentUserMember.grantablePermissions()

    // Hazır rol şablonları (sadece kullanıcının sahip olduğu yetkilerle)
    val rolePresets = listOf(
        "Proje Müdürü" to Permission.FOREMAN_PRESET.intersect(grantablePermissions),
        "Usta" to Permission.WORKER_PRESET.intersect(grantablePermissions),
        "Özel" to setOf<Permission>()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ekibe Davet Et",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .widthIn(min = 280.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Davetiye ID girişi
                OutlinedTextField(
                    value = inviteeId,
                    onValueChange = { inviteeId = it.uppercase() },
                    label = { Text("Davetiye Kodu") },
                    placeholder = { Text("Örn: UGR-842-193") },
                    leadingIcon = {
                        Icon(Icons.Outlined.QrCode, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // 2. Rol seçimi (Hazır Şablonlar)
                Text(
                    "Rol Seçin",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rolePresets.forEach { (roleName, permissions) ->
                        FilterChip(
                            selected = selectedRoleName == roleName,
                            onClick = {
                                selectedRoleName = roleName
                                if (roleName != "Özel") {
                                    selectedPermissions = permissions
                                }
                            },
                            label = { Text(roleName, maxLines = 1) },
                            leadingIcon = if (selectedRoleName == roleName) {
                                { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Yetki listesi (Özel rol seçildiğinde veya düzenlenmek istendiğinde)
                OutlinedCard(
                    onClick = { showPermissionPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Yetkiler",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                if (selectedPermissions.isEmpty()) "Yetki seçilmedi"
                                else "${selectedPermissions.size} yetki seçili",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // 4. Kapsam (Node ve Kategori sınırlaması)
                if (availableNodes.isNotEmpty()) {
                    OutlinedCard(
                        onClick = { showNodePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.AccountTree,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Mülk Kapsamı",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    if (selectedNodes.isEmpty()) "Tüm mülkler"
                                    else "${selectedNodes.size} mülk seçili",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                if (availableCategories.isNotEmpty()) {
                    OutlinedCard(
                        onClick = { showCategoryPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Kategori Kapsamı",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    if (selectedCategories.isEmpty()) "Tüm kategoriler"
                                    else "${selectedCategories.size} kategori seçili",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSend(
                        inviteeId.trim(),
                        Permission.toKeys(selectedPermissions),
                        MemberScopes(nodes = selectedNodes, categories = selectedCategories),
                        selectedRoleName
                    )
                },
                enabled = inviteeId.trim().isNotEmpty() && selectedPermissions.isNotEmpty() && !isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Davet Gönder")
                }
            }
        }
    )

    // Yetki Seçimi Bottom Sheet
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

    // Node Seçimi Bottom Sheet
    if (showNodePicker) {
        MultiSelectDialog(
            title = "Mülk Kapsamı",
            items = availableNodes.map { it.first to it.second },
            selectedItems = selectedNodes,
            onDismiss = { showNodePicker = false },
            onConfirm = { selected ->
                selectedNodes = selected
                showNodePicker = false
            }
        )
    }

    // Kategori Seçimi Bottom Sheet
    if (showCategoryPicker) {
        MultiSelectDialog(
            title = "Kategori Kapsamı",
            items = availableCategories.map { it to it },
            selectedItems = selectedCategories,
            onDismiss = { showCategoryPicker = false },
            onConfirm = { selected ->
                selectedCategories = selected
                showCategoryPicker = false
            }
        )
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = permission in tempSelected,
                            onCheckedChange = { checked ->
                                tempSelected = if (checked) {
                                    tempSelected + permission
                                } else {
                                    tempSelected - permission
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = permission.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
        confirmButton = {
            Button(onClick = { onConfirm(tempSelected) }) { Text("Tamam") }
        }
    )
}

@Composable
private fun MultiSelectDialog(
    title: String,
    items: List<Pair<String, String>>, // (id, displayName)
    selectedItems: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var tempSelected by remember { mutableStateOf(selectedItems.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // "Tümü" seçeneği
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = tempSelected.isEmpty(),
                        onCheckedChange = { checked ->
                            if (checked) tempSelected = emptySet()
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Tümü (Kısıtlama yok)", fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                items.forEach { (id, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = id in tempSelected,
                            onCheckedChange = { checked ->
                                tempSelected = if (checked) {
                                    tempSelected + id
                                } else {
                                    tempSelected - id
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(name)
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
        confirmButton = {
            Button(onClick = { onConfirm(tempSelected.toList()) }) { Text("Tamam") }
        }
    )
}
