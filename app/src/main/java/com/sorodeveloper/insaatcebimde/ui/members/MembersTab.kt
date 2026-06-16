package com.sorodeveloper.insaatcebimde.ui.members

import androidx.compose.animation.*
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
import com.sorodeveloper.insaatcebimde.domain.model.MemberInfo
import com.sorodeveloper.insaatcebimde.domain.model.Permission

/**
 * Çalışanlar Sekmesi (ProjectDetailScreen TabRow'undaki "Ekip" sekmesi).
 *
 * Bu ekran:
 * 1. Projedeki tüm üyeleri hiyerarşik olarak listeler
 * 2. Mevcut kullanıcının yetkisine göre düzenleme/kovma butonlarını gösterir/gizler
 * 3. Davet gönderme butonu gösterir (INVITE yetkisi varsa)
 *
 * Zero-Bill: Üye listesi proje dökümanındaki members Map'inden okunur.
 * Ekstra Read yok — proje zaten dinleniyor.
 */
@Composable
fun MembersTab(
    members: List<MemberInfo>,
    currentUserMember: MemberInfo?,
    isSaving: Boolean,
    onInviteClick: () -> Unit,
    onEditMember: (MemberInfo) -> Unit,
    onRemoveMember: (MemberInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Üst başlık + Davet butonu
        if (currentUserMember?.hasPermission(Permission.INVITE) == true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onInviteClick,
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                ) {
                    Icon(
                        Icons.Filled.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Ekibe Davet Et")
                }
            }
        }

        if (members.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Henüz ekip üyesi yok",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Davet göndererek ekibinizi oluşturun",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Üye sayısı header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${members.size} Ekip Üyesi",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Owner'ları en üstte göster
                val sortedMembers = members.sortedByDescending { it.isOwner }
                items(sortedMembers, key = { it.uid }) { member ->
                    MemberCard(
                        member = member,
                        canManage = currentUserMember?.canManage(member) ?: false,
                        hasManagePermission = currentUserMember?.hasPermission(Permission.MANAGE_MEMBERS) ?: false,
                        isSaving = isSaving,
                        isCurrentUser = member.uid == currentUserMember?.uid,
                        onEdit = { onEditMember(member) },
                        onRemove = { onRemoveMember(member) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberCard(
    member: MemberInfo,
    canManage: Boolean,
    hasManagePermission: Boolean,
    isSaving: Boolean,
    isCurrentUser: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    var showRemoveConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (member.isOwner)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (member.isOwner) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (member.isOwner) {
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFFFD700), // Altın
                                        Color(0xFFFFA500)  // Turuncu
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.displayName
                            .split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .joinToString(""),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Spacer(Modifier.width(12.dp))

                // İsim & Rol
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.displayName.ifEmpty { "İsimsiz Kullanıcı" },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCurrentUser) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    "Sen",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (member.isOwner) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFD700)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = member.roleName.ifEmpty { "Çalışan" },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (member.isOwner)
                                Color(0xFFB8860B)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Yönetim butonları
                if (canManage && hasManagePermission && !isCurrentUser && !member.isOwner) {
                    IconButton(onClick = onEdit, enabled = !isSaving) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Yetkilerini Düzenle",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { showRemoveConfirm = true },
                        enabled = !isSaving
                    ) {
                        Icon(
                            Icons.Outlined.PersonRemove,
                            contentDescription = "Projeden Çıkar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Yetki etiketleri (ilk 4 yetki)
            if (member.permissions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayPerms = Permission.fromKeys(member.permissions).take(4)
                    displayPerms.forEach { perm ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = perm.displayName,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                maxLines = 1
                            )
                        }
                    }
                    if (member.permissions.size > 4) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = "+${member.permissions.size - 4}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Kapsam bilgisi
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.FilterAlt,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (member.scopes.restricted) {
                        "${member.scopes.nodeCategories.size} mülk/iş kısıtlaması"
                    } else {
                        "Projeye Tam Erişim"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    // Çıkarma onay dialogu
    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Üyeyi Çıkar", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "${member.displayName} bu projeden çıkarılacak ve tüm erişim yetkileri silinecek. Bu işlem geri alınamaz.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text("Vazgeç")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveConfirm = false
                        onRemove()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Çıkar")
                }
            }
        )
    }
}
