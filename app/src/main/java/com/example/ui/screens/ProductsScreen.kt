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
import com.example.data.model.Product
import com.example.data.model.WoodPresets
import com.example.data.model.AccessoryPresets
import com.example.ui.InvoiceViewModel
import com.example.ui.components.AccordionPickerField
import com.example.ui.components.SelectOnFocusTextField
import com.example.util.CsvImportExportUtil
import com.example.util.Helper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val isRtl = viewModel.selectedLanguage == "fa"
    val context = LocalContext.current

    val accentColor = Color(0xFF1A73E8)
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val cardBackground = if (isDark) Color(0xFF1E1E2E) else Color(0xFFFAFAFC)

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<Product?>(null) }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { u ->
            try {
                val inputStream = context.contentResolver.openInputStream(u)
                val csvText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val importedProducts = CsvImportExportUtil.parseProductsCsv(csvText)
                if (importedProducts.isNotEmpty()) {
                    viewModel.bulkInsertProducts(importedProducts) { count ->
                        Toast.makeText(context, if (isRtl) "$count کالا با موفقیت وارد گردید" else "Imported $count products", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, if (isRtl) "هیچ کالای معتبری در فایل پیدا نشد" else "No valid products found", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "خطا در خواندن فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Form states
    var sku by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("عدد") }
    var category by remember { mutableStateOf("عمومی") }
    var price by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var colorCode by remember { mutableStateOf("") }
    var surfaceTreatment by remember { mutableStateOf("") }
    var branchCount by remember { mutableStateOf("") }
    var categoryType by remember { mutableStateOf("Wood") }

    fun resetForm() {
        sku = ""
        name = ""
        description = ""
        unit = if (isRtl) "متر طول" else "Meter"
        category = if (isRtl) "چوب پلاست" else "WPC"
        price = ""
        cost = ""
        stock = ""
        colorCode = ""
        surfaceTreatment = ""
        branchCount = ""
        categoryType = "Wood"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isRtl) "کاتالوگ خدمات و کالا" else "Product Catalog",
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
                    selectedProductForEdit = null
                    showAddDialog = true
                },
                containerColor = accentColor,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن کالا")
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
                        CsvImportExportUtil.exportProductsToCsv(context, products)
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
                        CsvImportExportUtil.generateSampleProductsCsv(context)
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
                        text = if (isRtl) "جستجوی کالا با نام یا کد کالا..." else "Search products by name or SKU...",
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = "جستجو")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Products List ---
            val filteredProducts = products.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.sku.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
            }

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isRtl) "کالایی یافت نشد." else "No products found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredProducts, key = { it.id }) { prod ->
                        val stockColor = when {
                            prod.stock <= 5.0 -> Color(0xFFEF4444) // Red
                            prod.stock <= 20.0 -> Color(0xFFF59E0B) // Orange
                            else -> Color(0xFF10B981) // Green
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProductForEdit = prod
                                    sku = prod.sku
                                    name = prod.name
                                    description = prod.description
                                    unit = prod.unit
                                    category = prod.category
                                    price = Helper.formatWithCommas(prod.price, false)
                                    cost = if (prod.cost > 0) Helper.formatWithCommas(prod.cost, false) else ""
                                    stock = prod.stock.toString()
                                    colorCode = prod.colorCode
                                    surfaceTreatment = prod.surfaceTreatment
                                    branchCount = if (prod.branchCount > 0) prod.branchCount.toString() else ""
                                    categoryType = prod.categoryType
                                    showAddDialog = true
                                }
                                .testTag("product_card_${prod.id}"),
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
                                        Text(text = prod.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        if (prod.isFavorite) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.Favorite, contentDescription = "موردعلاقه", tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${prod.sku} • ${if (prod.categoryType == "Wood" || prod.categoryType == "چوب پلاست") "چوب پلاست" else "پیچ و کلیپس"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (prod.colorCode.isNotEmpty() || prod.surfaceTreatment.isNotEmpty() || prod.branchCount > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "کد رنگ: ${prod.colorCode.ifEmpty { "-" }} | سطح: ${prod.surfaceTreatment.ifEmpty { "-" }} | شاخه: ${Helper.formatDouble(prod.branchCount, viewModel.usePersianDigits)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = (if (isRtl) "موجودی انبار: " else "Stock: ") + Helper.formatDouble(prod.stock, viewModel.usePersianDigits) + " " + prod.unit,
                                        color = stockColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = Helper.formatCurrency(prod.price, viewModel.selectedCurrency, viewModel.usePersianDigits),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = accentColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteProduct(prod) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف کالا", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
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
                            text = if (selectedProductForEdit == null) {
                                if (isRtl) "ثبت کالا / محصول جدید (قالب نست)" else "Add Nest Product"
                            } else {
                                if (isRtl) "ویرایش کالا در کاتالوگ" else "Edit catalog details"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    item {
                        // Category Type selector (Wood vs Accessory)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRtl) "نوع کالا:" else "Type:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val isWood = categoryType == "Wood"
                            FilterChip(
                                selected = isWood,
                                onClick = {
                                    categoryType = "Wood"
                                    category = if (isRtl) "چوب پلاست" else "WPC"
                                    unit = "متر طول"
                                },
                                label = { Text(if (isRtl) "چوب پلاست" else "WPC Wood", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = !isWood,
                                onClick = {
                                    categoryType = "Accessory"
                                    category = if (isRtl) "پیچ و کلیپس" else "Accessory"
                                    unit = "قطعه"
                                },
                                label = { Text(if (isRtl) "پیچ و کلیپس" else "Accessory", fontSize = 11.sp) }
                            )
                        }
                    }

                    if (categoryType == "Wood") {
                        item {
                            var expandedProfileMenu by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedProfileMenu,
                                onExpandedChange = { expandedProfileMenu = !expandedProfileMenu }
                            ) {
                                SelectOnFocusTextField(
                                    value = name,
                                    onValueChange = { input ->
                                        name = input
                                        val p = WoodPresets.findPresetByNameOrSku(input)
                                        if (p != null) {
                                            sku = p.sku
                                        }
                                    },
                                    label = { Text(if (isRtl) "انتخاب پروفیل پیش‌فرض چوب پلاست" else "Default Wood Profile") },
                                    placeholder = { Text("FEEL, ONCE, LEAD...") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProfileMenu) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("product_name_input"),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedProfileMenu,
                                    onDismissRequest = { expandedProfileMenu = false }
                                ) {
                                    WoodPresets.profiles.forEach { profile ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${profile.name} (${profile.sku}) • ضریب: ${profile.crossSectionFactor} • ${profile.category}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            onClick = {
                                                name = profile.name
                                                sku = profile.sku
                                                expandedProfileMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            SelectOnFocusTextField(
                                value = sku,
                                onValueChange = { input ->
                                    sku = input
                                    val p = WoodPresets.findPresetByNameOrSku(input)
                                    if (p != null) {
                                        name = p.name
                                    }
                                },
                                label = { Text(if (isRtl) "کد کالا / SKU" else "SKU Reference") },
                                placeholder = { Text("FC140 / FD142") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    } else {
                        item {
                            var expandedAccessoryMenu by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedAccessoryMenu,
                                onExpandedChange = { expandedAccessoryMenu = !expandedAccessoryMenu }
                            ) {
                                SelectOnFocusTextField(
                                    value = name,
                                    onValueChange = { input ->
                                        name = input
                                        val p = AccessoryPresets.findPresetByNameOrSku(input)
                                        if (p != null) {
                                            sku = p.sku
                                            if (p.defaultPrice > 0) price = Helper.formatWithCommas(p.defaultPrice, false)
                                        }
                                    },
                                    label = { Text(if (isRtl) "انتخاب قطعه / کلیپس پیش‌فرض" else "Default Accessory / Clip") },
                                    placeholder = { Text("Clicker - H, Starter...") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAccessoryMenu) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("product_name_input"),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedAccessoryMenu,
                                    onDismissRequest = { expandedAccessoryMenu = false }
                                ) {
                                    AccessoryPresets.items.forEach { acc ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${acc.name} (${acc.sku}) • ${if (acc.defaultPrice > 0) Helper.formatCurrency(acc.defaultPrice, "تومان", isRtl) else "استعلام قیمت"}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            onClick = {
                                                name = acc.name
                                                sku = acc.sku
                                                if (acc.defaultPrice > 0) price = Helper.formatWithCommas(acc.defaultPrice, false)
                                                expandedAccessoryMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            SelectOnFocusTextField(
                                value = sku,
                                onValueChange = { input ->
                                    sku = input
                                    val p = AccessoryPresets.findPresetByNameOrSku(input)
                                    if (p != null) {
                                        name = p.name
                                        if (p.defaultPrice > 0) price = Helper.formatWithCommas(p.defaultPrice, false)
                                    }
                                },
                                label = { Text(if (isRtl) "کد کالا / SKU" else "SKU Reference") },
                                placeholder = { Text("HCL, SCL...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    if (categoryType == "Wood") {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SelectOnFocusTextField(
                                    value = colorCode,
                                    onValueChange = { colorCode = it },
                                    label = { Text(if (isRtl) "کد رنگ (Color Code)" else "Color Code") },
                                    placeholder = { Text("N3") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                SelectOnFocusTextField(
                                    value = surfaceTreatment,
                                    onValueChange = { surfaceTreatment = it },
                                    label = { Text(if (isRtl) "عملیات سطحی" else "Surface Finish") },
                                    placeholder = { Text("BR / Emboss") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SelectOnFocusTextField(
                                    value = branchCount,
                                    onValueChange = { branchCount = it },
                                    label = { Text(if (isRtl) "تعداد شاخه" else "Branch Count") },
                                    placeholder = { Text("40") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                SelectOnFocusTextField(
                                    value = price,
                                    onValueChange = { input ->
                                        val parsed = Helper.parseFormattedToDouble(input)
                                        price = if (parsed > 0) Helper.formatWithCommas(parsed, false) else input
                                    },
                                    label = { Text(if (isRtl) "قیمت واحد (تومان)" else "Unit Price (IRT)") },
                                    supportingText = {
                                        val pVal = Helper.parseFormattedToDouble(price)
                                        if (pVal > 0) {
                                            Text(
                                                text = Helper.formatCurrency(pVal, "تومان", isRtl),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("product_price_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                    } else {
                        item {
                            val accPreset = AccessoryPresets.findPresetByNameOrSku(name) ?: AccessoryPresets.findPresetByNameOrSku(sku)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SelectOnFocusTextField(
                                    value = accPreset?.specialFor ?: "-",
                                    onValueChange = {},
                                    enabled = false,
                                    label = { Text(if (isRtl) "مخصوص پروفایل (SPECIAL FOR)" else "SPECIAL FOR") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                SelectOnFocusTextField(
                                    value = price,
                                    onValueChange = { input ->
                                        val parsed = Helper.parseFormattedToDouble(input)
                                        price = if (parsed > 0) Helper.formatWithCommas(parsed, false) else input
                                    },
                                    label = { Text(if (isRtl) "قیمت واحد (تومان)" else "Unit Price (IRT)") },
                                    supportingText = {
                                        val pVal = Helper.parseFormattedToDouble(price)
                                        if (pVal > 0) {
                                            Text(
                                                text = Helper.formatCurrency(pVal, "تومان", isRtl),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("product_price_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    item {
                        val standardUnits = remember {
                            listOf("متر طول", "قطعه", "عدد", "مترمرحب", "مترمربع", "شاخه", "کیلوگرم", "بسته", "رول", "متر", "ساعت", "دستگاه", "ست", "کارتن", "تن")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccordionPickerField(
                                value = unit.ifEmpty { if (categoryType == "Wood") "متر طول" else "قطعه" },
                                onValueChange = { unit = it },
                                label = if (isRtl) "واحد اندازه‌گیری" else "Measurement Unit",
                                placeholder = "متر طول / قطعه / عدد...",
                                options = standardUnits,
                                isRtl = isRtl,
                                modifier = Modifier.weight(1f).testTag("product_unit_input")
                            )
                            SelectOnFocusTextField(
                                value = stock,
                                onValueChange = { stock = it },
                                label = { Text(if (isRtl) "موجودی انبار" else "Stock count") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        SelectOnFocusTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(if (isRtl) "شرح کالا / توضیحات تکمیلی" else "Description") },
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
                                        val p = Product(
                                            id = selectedProductForEdit?.id ?: 0L,
                                            sku = sku,
                                            name = name,
                                            description = description,
                                            unit = unit,
                                            category = category,
                                            price = Helper.parseFormattedToDouble(price),
                                            cost = Helper.parseFormattedToDouble(cost),
                                            stock = stock.toDoubleOrNull() ?: 0.0,
                                            isFavorite = selectedProductForEdit?.isFavorite ?: false,
                                            colorCode = colorCode,
                                            surfaceTreatment = surfaceTreatment,
                                            branchCount = branchCount.toDoubleOrNull() ?: 0.0,
                                            categoryType = categoryType
                                        )
                                        viewModel.saveProduct(p)
                                        showAddDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("product_save_submit")
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
