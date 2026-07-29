package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.ui.InvoiceViewModel
import com.example.ui.components.SelectOnFocusTextField
import com.example.util.CsvImportExportUtil
import com.example.util.Helper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val isRtl = viewModel.selectedLanguage == "fa"
    val context = LocalContext.current

    val accentColor = Color(0xFF1A73E8)
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val cardBackground = if (isDark) Color(0xFF1E1E2E) else Color(0xFFFAFAFC)

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomerForEdit by remember { mutableStateOf<Customer?>(null) }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { u ->
            try {
                val inputStream = context.contentResolver.openInputStream(u)
                val csvText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val imported = CsvImportExportUtil.parseCustomersCsv(csvText)
                if (imported.isNotEmpty()) {
                    viewModel.bulkInsertCustomers(imported) { count ->
                        Toast.makeText(context, if (isRtl) "$count مشتری با موفقیت وارد گردید" else "Imported $count customers", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, if (isRtl) "هیچ خریدار معتبری در فایل پیدا نشد" else "No valid customers found", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "خطا در خواندن فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Form states
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var billingAddress by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") } // Economic code
    var nationalId by remember { mutableStateOf("") } // National code

    fun resetForm() {
        name = ""
        company = ""
        email = ""
        phone = ""
        mobile = ""
        billingAddress = ""
        taxId = ""
        nationalId = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isRtl) "مدیریت مشتریان" else "Customers Ledger",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    resetForm()
                    selectedCustomerForEdit = null
                    showAddDialog = true
                },
                containerColor = accentColor,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "مشتری جدید")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // --- Excel Import / Export Toolbar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        CsvImportExportUtil.exportCustomersToCsv(context, customers)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isRtl) "خروجی اکسل" else "Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        csvPickerLauncher.launch("text/*")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isRtl) "ورود از اکسل" else "Import", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        CsvImportExportUtil.generateSampleCustomersCsv(context)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isRtl) "فایل نمونه" else "Sample CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // --- Search Field ---
            SelectOnFocusTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (isRtl) "جستجوی نام مشتری یا شرکت..." else "Search customer or business name...",
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = "جستجو")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Customers List ---
            val filteredCustomers = customers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.company.contains(searchQuery, ignoreCase = true) ||
                        it.email.contains(searchQuery, ignoreCase = true)
            }

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isRtl) "مشتری‌ای یافت نشد." else "No customers found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredCustomers, key = { it.id }) { cust ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCustomerForEdit = cust
                                    name = cust.name
                                    company = cust.company
                                    email = cust.email
                                    phone = cust.phone
                                    mobile = cust.mobile
                                    billingAddress = cust.billingAddress
                                    taxId = cust.taxId
                                    nationalId = cust.nationalId
                                    showAddDialog = true
                                }
                                .testTag("customer_card_${cust.id}"),
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = cust.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        if (cust.isFavorite) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.Star, contentDescription = "برگزیده", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = cust.company,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (cust.email.isNotEmpty()) {
                                        Text(
                                            text = cust.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = Helper.toPersianDigits(cust.mobile), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    IconButton(
                                        onClick = { viewModel.deleteCustomer(cust) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف مشتری", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // --- Add/Edit Dialog ---
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = if (selectedCustomerForEdit == null) {
                                if (isRtl) "افزودن مشتری جدید" else "Add New Customer"
                            } else {
                                if (isRtl) "ویرایش مشخصات مشتری" else "Edit Customer details"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    item {
                        SelectOnFocusTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(if (isRtl) "نام و نام خانوادگی" else "Customer Name") },
                            modifier = Modifier.fillMaxWidth().testTag("customer_name_input"),
                            singleLine = true
                        )
                    }

                    item {
                        SelectOnFocusTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = { Text(if (isRtl) "نام شرکت / ارگان" else "Company Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        SelectOnFocusTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text(if (isRtl) "شماره تلفن همراه" else "Mobile Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                    }

                    item {
                        SelectOnFocusTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(if (isRtl) "آدرس ایمیل" else "Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true
                        )
                    }

                    item {
                        SelectOnFocusTextField(
                            value = taxId,
                            onValueChange = { taxId = it },
                            label = { Text(if (isRtl) "شناسه ملی حقوقی / اقتصادی" else "Economic Code / VAT ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        SelectOnFocusTextField(
                            value = billingAddress,
                            onValueChange = { billingAddress = it },
                            label = { Text(if (isRtl) "آدرس کامل صادرکننده" else "Billing Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 2
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showAddDialog = false }) {
                                Text(if (isRtl) "انصراف" else "Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (name.isNotEmpty()) {
                                        val c = Customer(
                                            id = selectedCustomerForEdit?.id ?: 0L,
                                            name = name,
                                            company = company,
                                            email = email,
                                            phone = phone,
                                            mobile = mobile,
                                            billingAddress = billingAddress,
                                            taxId = taxId,
                                            nationalId = nationalId,
                                            isFavorite = selectedCustomerForEdit?.isFavorite ?: false
                                        )
                                        viewModel.saveCustomer(c)
                                        showAddDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("customer_save_submit")
                            ) {
                                Text(text = if (isRtl) "ذخیره" else "Save", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
