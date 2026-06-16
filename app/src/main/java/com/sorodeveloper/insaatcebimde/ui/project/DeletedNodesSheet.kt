package com.sorodeveloper.insaatcebimde.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun DeletedNodesSheet(
    projectId: String,
    nodeViewModel: ProjectNodeViewModel,
    onClose: () -> Unit
) {
    val deletedNodes by nodeViewModel.deletedNodes
    val isLoading by nodeViewModel.isLoading

    var nodeToRestore by remember { mutableStateOf<com.sorodeveloper.insaatcebimde.domain.model.DeletedNodeDetail?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Silinen Mülkler / Geçmiş",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (deletedNodes.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text(text = "Silinmiş herhangi bir mülk bulunamadı.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(deletedNodes) { detail ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${detail.node.type}: ${detail.node.name}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (detail.fullPath.isNotEmpty()) {
                                        Text(
                                            text = "Yol: ${detail.fullPath}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                                
                                IconButton(
                                    onClick = { nodeToRestore = detail },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Filled.Refresh, 
                                        contentDescription = "Geri Getir", 
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            if (detail.deletedChildrenNames.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Bununla Birlikte Silinenler:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = detail.deletedChildrenNames.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Geri Getirme Onay Diyaloğu
    if (nodeToRestore != null) {
        val detail = nodeToRestore!!
        AlertDialog(
            onDismissRequest = { nodeToRestore = null },
            title = { Text("Mülkü Geri Getir") },
            text = { 
                Text("Senin sildiğin yer:\n${if (detail.fullPath.isNotEmpty()) detail.fullPath + " > " else ""}${detail.node.name}\n\nEmin misiniz? Geri getirdiğinizde, sistemdeki yüzdeler ve iş sayıları otomatik olarak ebeveynlerine aktarılmaya devam edecektir.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        nodeViewModel.toggleNodeDelete(projectId, detail.node.id, false) {
                            nodeToRestore = null
                            nodeViewModel.loadDeletedNodes(projectId) // Listeyi yenile
                            if (nodeViewModel.deletedNodes.value.size <= 1) {
                                onClose() // Son öğe silindiyse pencereyi kapat
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Geri Getir")
                }
            },
            dismissButton = {
                TextButton(onClick = { nodeToRestore = null }, enabled = !isLoading) {
                    Text("İptal")
                }
            }
        )
    }
}
