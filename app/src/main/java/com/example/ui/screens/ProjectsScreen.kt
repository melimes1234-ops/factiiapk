package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.Project
import com.example.ui.InvoiceViewModel
import com.example.ui.components.SelectOnFocusTextField
import com.example.util.Helper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: InvoiceViewModel,
    onNavigateToNewInvoiceWithProject: (projectName: String, customerId: Long?) -> Unit
) {
    val isRtl = viewModel.selectedLanguage == "fa"
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val projects by viewModel.projects.collectAsStateWithLifecycle(initialValue = emptyList())
    val customers by viewModel.customers.collectAsStateWithLifecycle(initialValue = emptyList())
    val invoices by viewModel.invoices.collectAsStateWithLifecycle(initialValue = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") } // All, فعال, تکمیل شده, معلق

    var showProjectDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }

    var projName by remember { mutableStateOf("") }
    var projCode by remember { mutableStateOf("") }
    var projCustomerId by remember { mutableStateOf<Long?>(null) }
    var projStatus by remember { mutableStateOf("فعال") }
    var projDescription by remember { mutableStateOf("") }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    fun openAddDialog() {
        editingProject = null
        projName = ""
        projCode = ""
        projCustomerId = null
        projStatus = "فعال"
        projDescription = ""
        showProjectDialog = true
    }

    fun openEditDialog(project: Project) {
        editingProject = project
        projName = project.name
        projCode = project.code
        projCustomerId = project.customerId
        projStatus = project.status
        projDescription = project.description
        showProjectDialog = true
    }

    val filteredProjects = remember(projects, searchQuery, selectedStatusFilter) {
        projects.filter { proj ->
            val matchesSearch = searchQuery.isBlank() ||
                    proj.name.contains(searchQuery, ignoreCase = true) ||
                    proj.code.contains(searchQuery, ignoreCase = true) ||
                    proj.customerName.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (selectedStatusFilter) {
                "Active", "فعال" -> proj.status == "فعال" || proj.status == "Active"
                "Completed", "تکمیل شده" -> proj.status == "تکمیل شده" || proj.status == "Completed"
                "OnHold", "معلق" -> proj.status == "معلق" || proj.status == "OnHold"
                else -> true
            }

            matchesSearch && matchesStatus
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isRtl) "مدیریت پروژه‌ها" else "Project Management",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    actions = {
                        Button(
                            onClick = { openAddDialog() },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRtl) "پروژه جدید" else "New Project", fontSize = 12.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Bar
                SelectOnFocusTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isRtl) "جستجوی نام پروژه، کد یا مشتری..." else "Search projects...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Filter Status Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filterItems = listOf(
                        "All" to if (isRtl) "همه (${projects.size})" else "All (${projects.size})",
                        "فعال" to if (isRtl) "فعال" else "Active",
                        "تکمیل شده" to if (isRtl) "تکمیل شده" else "Completed",
                        "معلق" to if (isRtl) "معلق" else "On Hold"
                    )

                    filterItems.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedStatusFilter == key,
                            onClick = { selectedStatusFilter = key },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                if (filteredProjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty())
                                    (if (isRtl) "پروژه‌ای با این مشخصات یافت نشد." else "No projects match your search.")
                                else
                                    (if (isRtl) "هنوز هیچ پروژه‌ای ثبت نشده است." else "No projects created yet."),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { openAddDialog() },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(if (isRtl) "افزودن اولین پروژه" else "Add First Project")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredProjects, key = { it.id }) { project ->
                            val linkedInvoices = remember(invoices, project) {
                                invoices.filter { inv ->
                                    inv.projectNumber.equals(project.code, ignoreCase = true) ||
                                            inv.projectNumber.equals(project.name, ignoreCase = true)
                                }
                            }

                            ProjectCardItem(
                                project = project,
                                linkedInvoicesCount = linkedInvoices.size,
                                isRtl = isRtl,
                                onEdit = { openEditDialog(project) },
                                onDelete = { viewModel.deleteProject(project) },
                                onCreateInvoice = {
                                    val codeOrName = project.code.ifBlank { project.name }
                                    onNavigateToNewInvoiceWithProject(codeOrName, project.customerId)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Add / Edit Project Dialog
        if (showProjectDialog) {
            AlertDialog(
                onDismissRequest = { showProjectDialog = false },
                title = {
                    Text(
                        text = if (editingProject == null)
                            (if (isRtl) "افزودن پروژه جدید" else "Add New Project")
                        else
                            (if (isRtl) "ویرایش پروژه" else "Edit Project"),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectOnFocusTextField(
                            value = projName,
                            onValueChange = { projName = it },
                            label = { Text(if (isRtl) "نام پروژه *" else "Project Name *") },
                            placeholder = { Text(if (isRtl) "مثلاً پروژه ویلایی لواسان" else "Project name...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        SelectOnFocusTextField(
                            value = projCode,
                            onValueChange = { projCode = it },
                            label = { Text(if (isRtl) "کد / شناسه پروژه" else "Project Code") },
                            placeholder = { Text(if (isRtl) "مثلاً PRJ-101" else "e.g. PRJ-101") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Customer Selection Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedCustName = customers.find { it.id == projCustomerId }?.name
                                ?: if (isRtl) "انتخاب مشتری / خریدار (اختیاری)" else "Select Customer (Optional)"

                            OutlinedTextField(
                                value = selectedCustName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (isRtl) "مشتری مرتبط" else "Associated Customer") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { customerDropdownExpanded = true },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            DropdownMenu(
                                expanded = customerDropdownExpanded,
                                onDismissRequest = { customerDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isRtl) "-- بدون مشتری --" else "-- None --") },
                                    onClick = {
                                        projCustomerId = null
                                        customerDropdownExpanded = false
                                    }
                                )
                                customers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = { Text("${cust.name} ${if (cust.company.isNotBlank()) "(${cust.company})" else ""}") },
                                        onClick = {
                                            projCustomerId = cust.id
                                            customerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Status Selection
                        Text(
                            text = if (isRtl) "وضعیت پروژه:" else "Status:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("فعال", "تکمیل شده", "معلق").forEach { st ->
                                FilterChip(
                                    selected = projStatus == st,
                                    onClick = { projStatus = st },
                                    label = { Text(st, fontSize = 12.sp) }
                                )
                            }
                        }

                        SelectOnFocusTextField(
                            value = projDescription,
                            onValueChange = { projDescription = it },
                            label = { Text(if (isRtl) "توضیحات پروژه" else "Description") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (projName.isBlank()) return@Button
                            val custName = customers.find { it.id == projCustomerId }?.name ?: ""
                            val projToSave = Project(
                                id = editingProject?.id ?: 0L,
                                name = projName.trim(),
                                code = projCode.trim(),
                                customerId = projCustomerId,
                                customerName = custName,
                                status = projStatus,
                                description = projDescription.trim()
                            )
                            viewModel.saveProject(projToSave)
                            showProjectDialog = false
                        }
                    ) {
                        Text(if (isRtl) "ذخیره" else "Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProjectDialog = false }) {
                        Text(if (isRtl) "انصراف" else "Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ProjectCardItem(
    project: Project,
    linkedInvoicesCount: Int,
    isRtl: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCreateInvoice: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val statusColor = when (project.status) {
        "فعال", "Active" -> Color(0xFF10B981) // Green
        "تکمیل شده", "Completed" -> Color(0xFF3B82F6) // Blue
        else -> Color(0xFFF59E0B) // Amber
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = project.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (project.code.isNotBlank()) {
                            Text(
                                text = "کد: ${project.code}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = project.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (project.customerName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "مشتری: ${project.customerName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (project.description.isNotBlank()) {
                Text(
                    text = project.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isRtl) "$linkedInvoicesCount فاکتور ثبت شده" else "$linkedInvoicesCount Invoices",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onCreateInvoice,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRtl) "صدور فاکتور" else "Invoice", fontSize = 11.sp)
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (isRtl) "حذف پروژه" else "Delete Project") },
            text = { Text(if (isRtl) "آیا از حذف پروژه '${project.name}' اطمینان دارید؟" else "Are you sure you want to delete '${project.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text(if (isRtl) "حذف" else "Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (isRtl) "انصراف" else "Cancel")
                }
            }
        )
    }
}
