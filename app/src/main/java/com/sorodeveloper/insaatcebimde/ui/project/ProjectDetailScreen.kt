package com.sorodeveloper.insaatcebimde.ui.project

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.sorodeveloper.insaatcebimde.domain.model.JobMaterial
import com.sorodeveloper.insaatcebimde.domain.model.JobTemplate
import com.sorodeveloper.insaatcebimde.domain.model.PropertyTemplate
import com.sorodeveloper.insaatcebimde.domain.model.ProjectNode
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val project by viewModel.project
    val isLoading by viewModel.isLoading

    // 0: İlerleme, 1: Şablonlar
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("İlerleme", "Şablonlar")

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                CenterAlignedTopAppBar(
                    title = { 
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(
                                text = project?.name ?: "Bilinmeyen İnşaat", 
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            ) 
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
                // Ana TabRow
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 3.dp
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else Color.Gray
                                ) 
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut()
                    }
                },
                label = "Main Tab Transition"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> ProgressTabContent(
                        projectId = projectId,
                        nodeTypes = project?.nodeTypes ?: listOf("Blok", "Kat", "Daire"),
                        onAddNodeType = { viewModel.addNodeType(it) }
                    )
                    1 -> TemplatesTabContent(
                        projectId = projectId,
                        nodeTypes = project?.nodeTypes ?: listOf("Blok", "Kat", "Daire"),
                        onAddNodeType = { viewModel.addNodeType(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressTabContent(
    projectId: String,
    nodeTypes: List<String>,
    onAddNodeType: (String) -> Unit
) {
    val nodeViewModel: ProjectNodeViewModel = hiltViewModel()
    val propertyTemplateViewModel: PropertyTemplateViewModel = hiltViewModel()
    val jobTemplateViewModel: JobTemplateViewModel = hiltViewModel()

    val currentPath by nodeViewModel.currentPath
    val childNodes by nodeViewModel.childNodes
    val isLoading by nodeViewModel.isLoading

    val propertyTemplates by propertyTemplateViewModel.templates
    val jobTemplates by jobTemplateViewModel.templates

    val nodeJobs by nodeViewModel.nodeJobs

    var selectedTab by remember { mutableIntStateOf(1) } // 0: İşleri, 1: Bağlı Mülkler
    var showAddNodeSheet by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    var showBatchCreateSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(projectId) {
        nodeViewModel.loadRootNode(projectId)
        propertyTemplateViewModel.loadTemplates(projectId)
        jobTemplateViewModel.loadTemplates(projectId)
    }

    val currentNode = currentPath.lastOrNull()

    LaunchedEffect(currentNode?.id) {
        currentNode?.let {
            nodeViewModel.loadNodeJobs(projectId, it.id)
        }
        selectedTab = 1
    }



    Column(modifier = Modifier.fillMaxSize()) {
        if (currentPath.isNotEmpty()) {
            // Ekmek Kırıntısı (Breadcrumb) Navigasyonu + Sihirbaz Butonu
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScrollableTabRow(
                    modifier = Modifier.weight(1f),
                    selectedTabIndex = currentPath.size - 1,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    indicator = {}
                ) {
                    currentPath.forEach { node ->
                        Tab(
                            selected = currentNode == node,
                            onClick = { nodeViewModel.navigateToNode(node) },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        node.name,
                                        fontWeight = if (currentNode == node) FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentNode == node) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                    if (node != currentPath.last()) {
                                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.padding(start = 8.dp).size(16.dp))
                                    }
                                }
                            }
                        )
                    }
                }
                IconButton(onClick = { showBatchCreateSheet = true }, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Filled.AddCircle, contentDescription = "Mülk Üretim Sihirbazı", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val activeTab = if (childNodes.isEmpty()) 0 else selectedTab

            // Seçili Düğüm Başlığı
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                val purpleColor = Color(0xFF8E639A) // Eski UI'a benzer morumsu renk
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, purpleColor, RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (activeTab == 0) purpleColor else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${currentNode?.name ?: ""} İşleri", 
                            color = if (activeTab == 0) Color.White else purpleColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    if (childNodes.isNotEmpty()) {
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(purpleColor))
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (activeTab == 1) purpleColor else Color.Transparent)
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Bağlı Mülkler", 
                                color = if (activeTab == 1) Color.White else purpleColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Alt İçerikler
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                if (isLoading && childNodes.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    if (activeTab == 0) {
                        NodeJobsContent(
                            currentNode = currentNode,
                            propertyTemplates = propertyTemplates,
                            jobTemplates = jobTemplates,
                            nodeJobs = nodeJobs,
                            onAssignTemplateClick = { showTemplateSheet = true },
                            onUpdateJobProgress = { jobId, progress ->
                                currentNode?.let {
                                    nodeViewModel.updateNodeJobProgress(projectId, it.id, jobId, progress)
                                }
                            }
                        )
                    } else {
                        var nodeToDeleteForDialog by remember { mutableStateOf<com.sorodeveloper.insaatcebimde.domain.model.ProjectNode?>(null) }
                        
                        NodeChildrenContent(
                            currentNode = currentNode,
                            childNodes = childNodes,
                            onNodeClick = { nodeViewModel.navigateToNode(it) },
                            onAddClick = { showAddNodeSheet = true },
                            onDeleteNode = { nodeToDelete ->
                                nodeToDeleteForDialog = nodeToDelete
                            }
                        )
                        
                        if (nodeToDeleteForDialog != null) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { nodeToDeleteForDialog = null },
                                title = { Text("Emin misiniz?") },
                                text = { Text("${nodeToDeleteForDialog?.name} mülkünü silmek istediğinize emin misiniz? Altındaki tüm işler de iptal edilecektir.") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val node = nodeToDeleteForDialog!!
                                        nodeToDeleteForDialog = null
                                        nodeViewModel.toggleNodeDelete(projectId, node.id, true) {
                                            // Başarıyla silindi, liste Firebase SnapshotListener ile otomatik güncellenecek.
                                        }
                                    }) { Text("Evet, Sil", color = Color.Red) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { nodeToDeleteForDialog = null }) { Text("İptal") }
                                }
                            )
                        }
                    }
                }
            }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // --- Bottom Sheets ---
        if (showAddNodeSheet && currentNode != null) {
            ModalBottomSheet(onDismissRequest = { showAddNodeSheet = false }, sheetState = sheetState) {
                AddNodeForm(
                    onSave = { name, type ->
                        nodeViewModel.addNode(projectId, currentNode.id, name, type) {
                            showAddNodeSheet = false
                        }
                    },
                    isSaving = isLoading
                )
            }
        }

        if (showTemplateSheet && currentNode != null) {
            ModalBottomSheet(onDismissRequest = { showTemplateSheet = false }, sheetState = sheetState) {
                AssignTemplateForm(
                    propertyTemplates = propertyTemplates,
                    onAssign = { templateId ->
                        nodeViewModel.updateNodeTemplate(currentNode, templateId) {
                            showTemplateSheet = false
                        }
                    },
                    isSaving = isLoading
                )
            }
        }

        if (showBatchCreateSheet && currentNode != null) {
            ModalBottomSheet(onDismissRequest = { showBatchCreateSheet = false }, sheetState = sheetState) {
                BatchCreateNodeForm(
                    propertyTemplates = propertyTemplates,
                    nodeTypes = nodeTypes,
                    onAddNodeType = onAddNodeType,
                    onSave = { prefix, count, isLetter, startVal, step, type, templateId ->
                        val names = generateNames(prefix, count, isLetter, startVal, step)
                        nodeViewModel.addMultipleNodes(projectId, currentNode.id, names, type, templateId) {
                            showBatchCreateSheet = false
                        }
                    },
                    isSaving = isLoading
                )
            }
        }
    }
}

fun generateNames(prefix: String, count: Int, isLetter: Boolean, startValue: String, step: Int): List<String> {
    val names = mutableListOf<String>()
    if (isLetter) {
        val startChar = startValue.firstOrNull()?.uppercaseChar() ?: 'A'
        for (i in 0 until count) {
            val char = (startChar.code + (i * step)).toChar()
            names.add("$prefix $char".trim())
        }
    } else {
        val startNum = startValue.toIntOrNull() ?: 1
        for (i in 0 until count) {
            val num = startNum + (i * step)
            names.add("$prefix $num".trim())
        }
    }
    return names
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchCreateNodeForm(
    propertyTemplates: List<PropertyTemplate>,
    nodeTypes: List<String>,
    onAddNodeType: (String) -> Unit,
    onSave: (prefix: String, count: Int, isLetter: Boolean, startVal: String, step: Int, type: String, templateId: String?) -> Unit,
    isSaving: Boolean
) {
    var selectedType by remember { mutableStateOf(nodeTypes.firstOrNull() ?: "Daire") }
    var newType by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var count by remember { mutableStateOf("0") }
    var startVal by remember { mutableStateOf("1") }
    var step by remember { mutableStateOf("1") }
    var isLetter by remember { mutableStateOf(false) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
        Text("Mülk Üretim Sihirbazı", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Mülk Türü & Ön Ek") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                nodeTypes.forEach { typeOption ->
                    DropdownMenuItem(
                        text = { Text(typeOption) },
                        onClick = {
                            selectedType = typeOption
                            expanded = false
                        }
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newType,
                        onValueChange = { newType = it },
                        placeholder = { Text("Yeni tür ekle...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newType.isNotBlank()) {
                                onAddNodeType(newType)
                                selectedType = newType
                                newType = ""
                                expanded = false
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Ekle", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("İsimlendirme:")
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !isLetter, onClick = { isLetter = false; startVal = "1" })
                Text("Sayı (1, 2, 3)")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isLetter, onClick = { isLetter = true; startVal = "A" })
                Text("Harf (A, B, C)")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startVal,
                onValueChange = { input -> 
                    if (isLetter) {
                        // Sadece harflere izin ver (tek harf olsun)
                        val letters = input.filter { it.isLetter() }.take(1).uppercase()
                        startVal = letters
                    } else {
                        // Sadece sayılara izin ver
                        startVal = input.filter { it.isDigit() }
                    }
                },
                label = { Text("Başlangıç") },
                keyboardOptions = KeyboardOptions(keyboardType = if (isLetter) KeyboardType.Text else KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = step,
                onValueChange = { input -> step = input.filter { it.isDigit() } },
                label = { Text("Artış") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = count,
                onValueChange = { input -> count = input.filter { it.isDigit() } },
                label = { Text("Adet") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text("Şablon (Opsiyonel):", fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
            val filteredTemplates = propertyTemplates.filter { it.nodeType == selectedType || it.nodeType.isBlank() }
            if (filteredTemplates.isEmpty()) {
                Text("Bu mülk türü için atanabilecek şablon bulunamadı.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            } else {
                LazyColumn {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedTemplateId = null }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedTemplateId == null, onClick = { selectedTemplateId = null })
                            Text("Şablon Atanmasın (Sadece mülk üretilir)")
                        }
                    }
                    items(filteredTemplates) { template ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedTemplateId = template.id }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedTemplateId == template.id, onClick = { selectedTemplateId = template.id })
                            Text(template.name)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val c = count.toIntOrNull() ?: 1
                val s = step.toIntOrNull() ?: 1
                onSave(selectedType, c, isLetter, startVal, s, selectedType, selectedTemplateId)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && count.isNotBlank() && startVal.isNotBlank() && step.isNotBlank()
        ) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Toplu Üret (${count.toIntOrNull() ?: 0} adet)")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun NodeJobsContent(
    currentNode: ProjectNode?,
    propertyTemplates: List<PropertyTemplate>,
    jobTemplates: List<JobTemplate>,
    nodeJobs: List<com.sorodeveloper.insaatcebimde.domain.model.NodeJob>,
    onAssignTemplateClick: () -> Unit,
    onUpdateJobProgress: (String, Int) -> Unit
) {
    if (currentNode?.propertyTemplateId == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Bu mülk için atanmış bir şablon yok.", color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAssignTemplateClick) {
                Text("Mülk Şablonu Ata")
            }
        }
    } else {
        val assignedTemplate = propertyTemplates.find { it.id == currentNode.propertyTemplateId }
        if (assignedTemplate != null) {
            // HİBRİT DENORMALİZASYON SAYESİNDE ARTIK JOB TEMPLATE'LERE İHTİYACIMIZ YOK!
            // Sadece node'un altındaki işleri çekiyoruz (isDeleted: false olanları).
            // İsim ve kategori bilgisi zaten backend tarafından kopyalandığı için direkt okuyoruz.
            val allJobs = nodeJobs.filter { !it.isDeleted }
            val groupedJobs = allJobs.groupBy { it.category }

            var expandedCategories by remember { mutableStateOf(emptySet<String>()) }
            var expandedJobs by remember { mutableStateOf(emptySet<String>()) }

            var showImageDialog by remember { mutableStateOf(false) }
            var selectedJobForDialog by remember { mutableStateOf<JobTemplate?>(null) }
            var selectedImageIndex by remember { mutableIntStateOf(0) }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Atanan Şablon:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(assignedTemplate.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = onAssignTemplateClick) {
                            Text("Değiştir")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // --- PROGRESS BARS ---
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("Yerel İlerleme (Bu Mülkün İşleri)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            LinearProgressIndicator(
                                progress = { currentNode.localProgress / 100f },
                                modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                                color = Color(0xFF4CAF50), // Yeşil
                                trackColor = Color(0xFFE8F5E9)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("%${currentNode.localProgress}", fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                if (allJobs.isEmpty()) {
                    item {
                        Text("Bu mülke ait iş bulunamadı.", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
                    }
                } else {
                    groupedJobs.forEach { (category, jobs) ->
                        val catTotalProgress = jobs.sumOf { it.progress }
                        val catAvg = if (jobs.isNotEmpty()) catTotalProgress / jobs.size else 0

                        item {
                            val isCatExpanded = expandedCategories.contains(category)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .animateContentSize(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedCategories = if (isCatExpanded) expandedCategories - category else expandedCategories + category
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = category.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("%$catAvg", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(end = 8.dp))
                                        Text("(${jobs.size} iş)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = if (isCatExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (isCatExpanded) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            jobs.forEachIndexed { index, job ->
                                                val isJobExpanded = expandedJobs.contains(job.id)
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                expandedJobs = if (isJobExpanded) expandedJobs - job.id else expandedJobs + job.id
                                                            }
                                                            .padding(vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        val jTemplateForType = jobTemplates.find { it.id == job.jobTemplateId }
                                                        val jobType = jTemplateForType?.type ?: ""
                                                        val displayJobName = if (jobType.isNotBlank()) "${job.name} ($jobType)" else job.name

                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(displayJobName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                                            Text("%${job.progress} Tamamlandı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                        Icon(
                                                            imageVector = if (isJobExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                            contentDescription = null,
                                                            tint = Color.Gray,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    if (isJobExpanded) {
                                                        Column(modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)) {
                                                            // 5 Button Progress Selector
                                                            Text("İlerleme Durumu:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                listOf(0, 25, 50, 75, 100).forEach { p ->
                                                                    val isSelected = job.progress == p
                                                                    Button(
                                                                        onClick = { onUpdateJobProgress(job.id, p) },
                                                                        colors = ButtonDefaults.buttonColors(
                                                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                                            contentColor = if (isSelected) Color.White else Color.DarkGray
                                                                        ),
                                                                        contentPadding = PaddingValues(0.dp),
                                                                        modifier = Modifier.size(48.dp),
                                                                        shape = RoundedCornerShape(8.dp)
                                                                    ) {
                                                                        Text("$p", fontWeight = FontWeight.Bold)
                                                                    }
                                                                }
                                                            }

                                                            // Materials & Images
                                                            val jTemplate = jobTemplates.find { it.id == job.jobTemplateId }
                                                            if (jTemplate != null) {
                                                                if (jTemplate.materials.isNotEmpty()) {
                                                                    Text("Malzemeler:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                                                    jTemplate.materials.forEach { mat ->
                                                                        Row(
                                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                                        ) {
                                                                            Text("• ${mat.name}", style = MaterialTheme.typography.bodyMedium)
                                                                            Text(mat.quantity, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                                        }
                                                                    }
                                                                }

                                                                if (jTemplate.images.isNotEmpty()) {
                                                                    Spacer(modifier = Modifier.height(12.dp))
                                                                    Text("Fotoğraflar:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                                                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                        items(jTemplate.images) { url ->
                                                                            AsyncImage(
                                                                                model = url,
                                                                                contentDescription = null,
                                                                                contentScale = ContentScale.Crop,
                                                                                modifier = Modifier
                                                                                    .size(80.dp)
                                                                                    .clip(RoundedCornerShape(8.dp))
                                                                                    .clickable {
                                                                                        selectedJobForDialog = jTemplate
                                                                                        selectedImageIndex = jTemplate.images.indexOf(url)
                                                                                        showImageDialog = true
                                                                                    }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (index < jobs.size - 1) {
                                                        HorizontalDivider(
                                                            modifier = Modifier.padding(vertical = 4.dp),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showImageDialog && selectedJobForDialog != null) {
                FullScreenImageDialog(
                    template = selectedJobForDialog!!,
                    initialIndex = selectedImageIndex,
                    onDismiss = { showImageDialog = false }
                )
            }
        }
    }
}

@Composable
fun NodeChildrenContent(
    currentNode: ProjectNode?,
    childNodes: List<ProjectNode>,
    onNodeClick: (ProjectNode) -> Unit,
    onAddClick: () -> Unit,
    onDeleteNode: (ProjectNode) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (currentNode != null && currentNode.totalDescendantJobs > 0) {
            val generalProgress = currentNode.totalDescendantProgress / currentNode.totalDescendantJobs
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Bağlı Mülklerin Genel İlerlemesi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    LinearProgressIndicator(
                        progress = { generalProgress / 100f },
                        modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("%$generalProgress", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider()
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (childNodes.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Henüz alt birim eklenmemiş.", color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onAddClick) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Alt Birim Ekle")
                    }
                }
            } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(childNodes) { child ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNodeClick(child) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(child.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(child.type, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("Yerel: %${child.localProgress}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                                    if (child.totalDescendantJobs > 0) {
                                        val genProg = child.totalDescendantProgress / child.totalDescendantJobs
                                        Text("Genel: %$genProg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "Seçenekler", tint = Color.Gray)
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Sil", color = Color.Red) },
                                        onClick = {
                                            expanded = false
                                            onDeleteNode(child)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // FAB için boşluk
            }
            
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Alt Birim Ekle")
            }
        }
    }
}
}

@Composable
fun AddNodeForm(
    onSave: (String, String) -> Unit,
    isSaving: Boolean
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Grup") } // Örn: Blok, Kat, Daire, vb.

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
        Text("Yeni Alt Birim Ekle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Adı (Örn: A Blok, 1. Kat, Daire 5)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = type,
            onValueChange = { type = it },
            label = { Text("Türü (Örn: Blok, Kat, Daire)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onSave(name, type) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && name.isNotBlank() && type.isNotBlank()
        ) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Oluştur")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AssignTemplateForm(
    propertyTemplates: List<PropertyTemplate>,
    onAssign: (String) -> Unit,
    isSaving: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
        Text("Mülk Şablonu Seçin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (propertyTemplates.isEmpty()) {
            Text("Mevcut şablon bulunamadı. Önce Şablonlar sekmesinden bir şablon oluşturun.", color = Color.Gray)
        } else {
            LazyColumn {
                items(propertyTemplates) { template ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onAssign(template.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(template.name, fontWeight = FontWeight.Bold)
                            Text("${template.jobTemplateIds.size} iş tanımlı", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TemplatesTabContent(
    projectId: String,
    nodeTypes: List<String>,
    onAddNodeType: (String) -> Unit
) {
    var subTabIndex by remember { mutableIntStateOf(0) }
    val subTabs = listOf("İş Şablonları", "Mülk Şablonları")
    val orangeColor = Color(0xFFFF9800) // Turuncumsu renk

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = orangeColor,
            indicator = { tabPositions ->
                if (subTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[subTabIndex]),
                        color = orangeColor,
                        height = 2.dp
                    )
                }
            }
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = subTabIndex == index,
                    onClick = { subTabIndex = index },
                    text = { 
                        Text(
                            text = title,
                            fontWeight = if (subTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (subTabIndex == index) orangeColor else Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            AnimatedContent(
                targetState = subTabIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut()
                    }
                },
                label = "Sub Tab Transition"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> JobTemplatesTabContent(projectId)
                    1 -> PropertyTemplatesTabContent(projectId, nodeTypes, onAddNodeType)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobTemplatesTabContent(
    projectId: String,
    viewModel: JobTemplateViewModel = hiltViewModel()
) {
    val templates by viewModel.templates
    val isLoading by viewModel.isLoading
    val isSaving by viewModel.isSaving

    LaunchedEffect(projectId) {
        viewModel.loadTemplates(projectId)
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var editingTemplate by remember { mutableStateOf<JobTemplate?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (templates.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Henüz bir şablon bulunmuyor.", color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { 
                    editingTemplate = null
                    showBottomSheet = true 
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Ekle")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Şablon Oluştur")
                }
            }
        } else {
            // Şablonları kategoriye göre grupla
            val groupedTemplates = templates.groupBy { it.category }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                groupedTemplates.forEach { (category, categoryTemplates) ->
                    item {
                        ExpandableCategoryCard(
                            category = category, 
                            templates = categoryTemplates,
                            onEditClick = { template ->
                                editingTemplate = template
                                showBottomSheet = true
                            }
                        )
                    }
                }
            }
            
            FloatingActionButton(
                onClick = { 
                    editingTemplate = null
                    showBottomSheet = true 
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Şablon Ekle")
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showBottomSheet = false
                    editingTemplate = null
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                JobTemplateForm(
                    initialTemplate = editingTemplate,
                    onSave = { category, name, type, materials, images ->
                        viewModel.saveTemplate(
                            templateId = editingTemplate?.id,
                            projectId = projectId,
                            category = category, 
                            name = name, 
                            type = type, 
                            materials = materials, 
                            images = images
                        ) {
                            showBottomSheet = false
                            editingTemplate = null
                        }
                    },
                    isSaving = isSaving
                )
            }
        }
    }
}

@Composable
fun ExpandableCategoryCard(
    category: String, 
    templates: List<JobTemplate>,
    onEditClick: (JobTemplate) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            // Kategori Başlığı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = category.ifBlank { "Diğer" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            // Genişletilmiş İçerik (Alt Kalemler)
            if (expanded) {
                HorizontalDivider()
                Column(modifier = Modifier.padding(16.dp)) {
                    templates.forEach { template ->
                        ExpandableTemplateItem(template, onEditClick = { onEditClick(template) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableTemplateItem(template: JobTemplate, onEditClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

    if (selectedImageIndex != null) {
        FullScreenImageDialog(
            template = template, 
            initialIndex = selectedImageIndex!!, 
            onDismiss = { selectedImageIndex = null }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayName = if (template.type.isNotBlank()) "${template.name} (${template.type})" else template.name
                Text(text = displayName, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Düzenle", tint = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (expanded) {
                Column(modifier = Modifier.padding(start = 12.dp, top = 0.dp, bottom = 12.dp, end = 12.dp)) {
                    if (template.materials.isNotEmpty()) {
                        Text(text = "Malzemeler:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        template.materials.forEach { material ->
                            Text(text = "• ${material.name} - ${material.quantity}", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text(text = "Malzeme eklenmemiş.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    
                    if (template.images.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Örnek Resimler / Çizimler:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(template.images) { index, imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Şablon Resmi",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedImageIndex = index }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JobTemplateForm(
    initialTemplate: JobTemplate? = null,
    onSave: (String, String, String, List<JobMaterial>, List<String>) -> Unit,
    isSaving: Boolean
) {
    var category by remember { mutableStateOf(initialTemplate?.category ?: "") }
    var name by remember { mutableStateOf(initialTemplate?.name ?: "") }
    var type by remember { mutableStateOf(initialTemplate?.type ?: "") }
    var materials by remember { mutableStateOf(initialTemplate?.materials ?: listOf<JobMaterial>()) }
    var images by remember { mutableStateOf(initialTemplate?.images ?: listOf<String>()) } 

    // Re-initialize state if initialTemplate changes
    LaunchedEffect(initialTemplate) {
        category = initialTemplate?.category ?: ""
        name = initialTemplate?.name ?: ""
        type = initialTemplate?.type ?: ""
        materials = initialTemplate?.materials ?: listOf()
        images = initialTemplate?.images ?: listOf()
    } 

    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            images = images + uris.map { it.toString() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .verticalScroll(scrollState),
    ) {
        Text(
            text = if (initialTemplate == null) "Yeni İş Şablonu" else "Şablonu Düzenle", 
            style = MaterialTheme.typography.titleLarge, 
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Kategori (Örn: Mobilya)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("İşin Adı (Örn: Vestiyer)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = type,
            onValueChange = { type = it },
            label = { Text("Türü (Örn: 3+1 Daire)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Resim Ekleme Bölümü
        Text("Şablon Resimleri", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(
                    modifier = Modifier.size(80.dp).clickable {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Filled.Add, contentDescription = "Resim Ekle", modifier = Modifier.size(32.dp))
                    }
                }
            }
            items(images) { uri ->
                Box {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Seçilen Resim",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    IconButton(
                        onClick = { images = images - uri },
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp).padding(4.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = Color.Red)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Malzemeler", fontWeight = FontWeight.Bold)
            TextButton(onClick = { materials = listOf(JobMaterial("", "")) + materials }) {
                Icon(Icons.Filled.Add, contentDescription = "Ekle")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Malzeme Ekle")
            }
        }
        materials.forEachIndexed { index, material ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = material.name,
                    onValueChange = { newName ->
                        val newList = materials.toMutableList()
                        newList[index] = material.copy(name = newName)
                        materials = newList
                    },
                    label = { Text("Adı") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = material.quantity,
                    onValueChange = { newQuantity ->
                        val newList = materials.toMutableList()
                        newList[index] = material.copy(quantity = newQuantity)
                        materials = newList
                    },
                    label = { Text("Miktar") },
                    modifier = Modifier.weight(0.5f)
                )
                IconButton(onClick = {
                    val newList = materials.toMutableList()
                    newList.removeAt(index)
                    materials = newList
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        // Alt boşluk

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSave(category, name, type, materials, images) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && category.isNotBlank() && name.isNotBlank()
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Şablonu Kaydet")
            }
        }
        Spacer(modifier = Modifier.height(32.dp)) // Extra padding for bottom sheet comfort
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyTemplatesTabContent(
    projectId: String,
    nodeTypes: List<String>,
    onAddNodeType: (String) -> Unit
) {
    val viewModel: PropertyTemplateViewModel = hiltViewModel()
    val jobViewModel: JobTemplateViewModel = hiltViewModel()

    val propertyTemplates by viewModel.templates
    val jobTemplates by jobViewModel.templates
    val isLoading by viewModel.isLoading
    val isSaving by viewModel.isSaving

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingTemplate by remember { mutableStateOf<PropertyTemplate?>(null) }

    LaunchedEffect(projectId) {
        viewModel.loadTemplates(projectId)
        jobViewModel.loadTemplates(projectId) // İş şablonlarını da yükle (seçim için)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (propertyTemplates.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Henüz mülk şablonu oluşturulmadı.", color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    editingTemplate = null
                    showBottomSheet = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Ekle")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mülk Şablonu Oluştur")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(propertyTemplates) { template ->
                    PropertyTemplateCard(
                        template = template,
                        jobTemplates = jobTemplates,
                        onEditClick = {
                            editingTemplate = template
                            showBottomSheet = true
                        }
                    )
                }
            }

            FloatingActionButton(
                onClick = {
                    editingTemplate = null
                    showBottomSheet = true
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Mülk Şablonu Ekle")
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    editingTemplate = null
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                PropertyTemplateForm(
                    initialTemplate = editingTemplate,
                    availableJobTemplates = jobTemplates.filter { !it.isDeleted },
                    nodeTypes = nodeTypes,
                    onAddNodeType = onAddNodeType,
                    onSave = { name, nodeType, selectedJobIds ->
                        viewModel.saveTemplate(
                            templateId = editingTemplate?.id,
                            projectId = projectId,
                            name = name,
                            nodeType = nodeType,
                            jobTemplateIds = selectedJobIds
                        ) {
                            showBottomSheet = false
                            editingTemplate = null
                        }
                    },
                    isSaving = isSaving
                )
            }
        }
    }
}

@Composable
fun PropertyTemplateCard(
    template: PropertyTemplate,
    jobTemplates: List<JobTemplate>,
    onEditClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var expandedCategories by remember { mutableStateOf(emptySet<String>()) }
    
    // Şablona bağlı iş şablonlarını bul
    val linkedJobs = jobTemplates.filter { it.id in template.jobTemplateIds }
    val groupedJobs = linkedJobs.groupBy { it.category }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Ana Kart Başlığı (Tıklanabilir alan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val typeText = if (template.nodeType.isNotBlank()) "Tür: ${template.nodeType} • " else ""
                    Text(
                        text = "$typeText${linkedJobs.size} iş tanımlı",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Düzenle", tint = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (expanded) {
                HorizontalDivider()
                if (linkedJobs.isEmpty()) {
                    Text(
                        text = "Bu mülk şablonuna henüz iş bağlanmamış.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        groupedJobs.forEach { (category, jobs) ->
                            val isCategoryExpanded = expandedCategories.contains(category)
                            
                            // Kategori Alt Başlığı (Tıklanabilir)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedCategories = if (isCategoryExpanded) {
                                            expandedCategories - category
                                        } else {
                                            expandedCategories + category
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (!isCategoryExpanded) {
                                        Text(
                                            text = "${jobs.size} iş",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isCategoryExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (isCategoryExpanded) {
                                jobs.forEach { job ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = job.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            val typeText = if (job.type.isNotBlank()) "${job.type} • " else ""
                                            Text(
                                                text = "$typeText${job.materials.size} malzeme",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyTemplateForm(
    initialTemplate: PropertyTemplate? = null,
    availableJobTemplates: List<JobTemplate>,
    nodeTypes: List<String>,
    onAddNodeType: (String) -> Unit,
    onSave: (String, String, List<String>) -> Unit,
    isSaving: Boolean
) {
    var name by remember { mutableStateOf(initialTemplate?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialTemplate?.nodeType?.takeIf { it.isNotBlank() } ?: nodeTypes.firstOrNull() ?: "Daire") }
    var newType by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedJobIds by remember { mutableStateOf(initialTemplate?.jobTemplateIds ?: emptyList()) }
    var expandedCategories by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(initialTemplate) {
        name = initialTemplate?.name ?: ""
        selectedJobIds = initialTemplate?.jobTemplateIds ?: emptyList()
        // İsteğe bağlı: Düzenleme modunda, seçili işleri olan kategorileri otomatik açık başlatabiliriz.
        // Ama şimdilik kapalı başlaması (kullanıcı özetleri görecek) istenildiği için boş set bırakıyoruz.
    }

    val scrollState = rememberScrollState()

    // İş şablonlarını kategoriye göre grupla
    val groupedJobs = availableJobTemplates.groupBy { it.category }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .verticalScroll(scrollState),
    ) {
        Text(
            text = if (initialTemplate == null) "Yeni Mülk Şablonu" else "Mülk Şablonunu Düzenle",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Mülk Türü") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                nodeTypes.forEach { typeOption ->
                    DropdownMenuItem(
                        text = { Text(typeOption) },
                        onClick = {
                            selectedType = typeOption
                            expanded = false
                        }
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newType,
                        onValueChange = { newType = it },
                        placeholder = { Text("Yeni tür ekle...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newType.isNotBlank()) {
                                onAddNodeType(newType)
                                selectedType = newType
                                newType = ""
                                expanded = false
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Ekle", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Şablon Adı (Örn: 3+1 Lüks)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Bu Mülk Tipine Ait İşler", fontWeight = FontWeight.Bold)
        Text(
            text = "${selectedJobIds.size} iş seçildi",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (availableJobTemplates.isEmpty()) {
            Text(
                text = "Henüz iş şablonu oluşturulmadı. Önce 'İş Şablonları' sekmesinden iş şablonları ekleyin.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        } else {
            groupedJobs.forEach { (category, jobs) ->
                val isExpanded = expandedCategories.contains(category)
                val selectedJobsInCategory = jobs.filter { it.id in selectedJobIds }
                
                // Seçili olanlar en üstte, sonra seçili olmayanlar
                val sortedJobs = jobs.sortedByDescending { it.id in selectedJobIds }
                
                Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    // Kategori Başlığı (Tıklanabilir - Accordion)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedCategories = if (isExpanded) {
                                    expandedCategories - category
                                } else {
                                    expandedCategories + category
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            // Kapalıyken özet bilgi göster
                            if (!isExpanded && selectedJobsInCategory.isNotEmpty()) {
                                val summaryText = selectedJobsInCategory.joinToString(", ") { 
                                    if (it.type.isNotBlank()) "${it.name} (${it.type})" else it.name 
                                }
                                Text(
                                    text = "${selectedJobsInCategory.size} adet seçili: $summaryText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // İş Listesi (Sadece açıksa görünür)
                    if (isExpanded) {
                        sortedJobs.forEach { job ->
                            val isSelected = job.id in selectedJobIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedJobIds = if (isSelected) {
                                            selectedJobIds - job.id
                                        } else {
                                            selectedJobIds + job.id
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedJobIds = if (checked) {
                                            selectedJobIds + job.id
                                        } else {
                                            selectedJobIds - job.id
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = job.name,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (job.type.isNotBlank()) {
                                        Text(
                                            text = job.type,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSave(name, selectedType, selectedJobIds) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && name.isNotBlank()
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Şablonu Kaydet")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageDialog(template: JobTemplate, initialIndex: Int, onDismiss: () -> Unit) {
    val images = template.images
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { images.size })
    val coroutineScope = rememberCoroutineScope()
    var isZoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Full screen
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isZoomed,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val imageUrl = images[page]
                
                AndroidView(
                    factory = { context ->
                        PhotoView(context).apply {
                            setOnScaleChangeListener { _, _, _ ->
                                isZoomed = scale > 1.05f
                            }
                            Glide.with(context).load(imageUrl).into(this)
                        }
                    },
                    update = { view ->
                        Glide.with(view.context).load(imageUrl).into(view)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Kategori ve İş Adı Bilgisi (Sadece yakınlaştırma yapılmadığında görünür)
            AnimatedVisibility(
                visible = !isZoomed,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp, start = 16.dp, end = 64.dp) // Kapatma butonuna çarpmaması için sağdan boşluk
            ) {
                Column {
                    Text(
                        text = template.category, 
                        color = Color.White.copy(alpha = 0.7f), 
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = template.name, 
                        color = Color.White, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Kapatma Butonu
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Kapat", tint = Color.White)
            }

            // Alt Navigasyon (Önceki - Gösterge - Sonraki)
            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Önceki Butonu (Küçük Resim)
                    if (pagerState.currentPage > 0) {
                        AsyncImage(
                            model = images[pagerState.currentPage - 1],
                            contentDescription = "Önceki Resim",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                .clickable {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    // Sayfa Göstergesi
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    // Sonraki Butonu (Küçük Resim)
                    if (pagerState.currentPage < images.size - 1) {
                        AsyncImage(
                            model = images[pagerState.currentPage + 1],
                            contentDescription = "Sonraki Resim",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                .clickable {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}
