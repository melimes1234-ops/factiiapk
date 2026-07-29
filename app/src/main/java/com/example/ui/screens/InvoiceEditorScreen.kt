package com.example.ui.screens

import com.example.ui.components.SelectOnFocusTextField

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.InvoiceLineItem
import com.example.data.model.Product
import com.example.data.model.WoodPresets
import com.example.data.model.AccessoryPresets
import com.example.ui.InvoiceViewModel
import com.example.ui.components.AccordionPickerField
import com.example.ui.components.SelectOnFocusTextField
import com.example.util.Helper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorScreen(
    viewModel: InvoiceViewModel,
    onNavigateBack: () -> Unit,
    onSaveSuccess: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val colorCodesList by viewModel.colorCodesList.collectAsStateWithLifecycle()
    val surfaceTreatmentsList by viewModel.surfaceTreatmentsList.collectAsStateWithLifecycle()
    val isRtl = viewModel.selectedLanguage == "fa"

    val accentColor = Color(0xFF1A73E8)
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val cardBackground = if (isDark) Color(0xFF1E1E2E) else Color(0xFFFAFAFC)

    var showProductDialog by remember { mutableStateOf(false) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var showTermsPopup by remember { mutableStateOf(false) }

    // Accordion Expansion States
    var isGeneralInfoExpanded by remember { mutableStateOf(true) }
    var isFinancialExpanded by remember { mutableStateOf(true) }
    var isSettlementExpanded by remember { mutableStateOf(true) }

    // Auto-suggestions for Staff (Sales & Support) and Projects
    val allInvoices by viewModel.invoices.collectAsStateWithLifecycle(initialValue = emptyList())
    val allProjectsList by viewModel.projects.collectAsStateWithLifecycle(initialValue = emptyList())
    val suggestedStaffList = remember(allInvoices) {
        (allInvoices.map { it.salesperson } + allInvoices.map { it.supportPerson }).filter { it.isNotBlank() }.distinct()
    }


    var showQuickCustomerDialog by remember { mutableStateOf(false) }
    var quickCustName by remember { mutableStateOf("") }
    var quickCustCompany by remember { mutableStateOf("") }
    var quickCustPhone by remember { mutableStateOf("") }
    var quickCustTaxNumber by remember { mutableStateOf("") }
    var quickCustNationalId by remember { mutableStateOf("") }
    var quickCustAddress by remember { mutableStateOf("") }

    val defaultSalesTermsList = remember {
        mutableStateListOf(
            "۵۰ درصد مبلغ کل در ابتدا و مابقی قبل از خروج سفارش از کارخانه دریافت می‌گردد.",
            "رنگ به هیچ عنوان و تحت هیچ شرایطی شامل گارانتی نمی‌گردد. سفارش کالا و رنگ انتخابی بر اساس نمونه محصول بوده و به دلیل پودر چوب طبیعی احتمال تغییر رنگ وجود دارد.",
            "لطفاً در انتخاب محصول و متراژ دقت لازم را مبذول بفرمایید. در صورت خرید در دو مرحله، تغییرات رنگ اجتناب‌ناپذیر است.",
            "شروع تولید پس از واریز مبلغ پیش‌پرداخت، دریافت رسید واریز و ارائه تاییدیه مالی شرکت می‌باشد.",
            "تایید پیش‌فاکتور و واریز پیش‌پرداخت به منزله قبول شرایط و خرید قطعی بوده و در صورت انصراف خریدار، خسارات احتمالی کسر می‌گردد.",
            "زمان تحویل کالا در صورت عدم موجودی در انبار با هماهنگی قبلی به خریدار اعلام می‌شود.",
            "انتقال کالا در طبقات و تخلیه بار بر عهده خریدار می‌باشد.",
            "ارسال بار منوط به تسویه حساب کامل مالی فاکتور و واریز به حساب اعلام شده می‌باشد.",
            "تمامی اقلام و متراژهای مندرج در فاکتور تقریبی بوده و امکان تغییر نهایی در آن وجود دارد.",
            "اعتبار این پیش‌فاکتور از تاریخ صدور حداکثر ۲۴ ساعت می‌باشد.",
            "محصولات نست صرفاً در صورت نصب توسط تیم‌های اجرایی دارای گواهینامه و استفاده از کلیپس مخصوص شامل ۲ سال گارانتی می‌گردد.",
            "هزینه حمل و نقل و باربری از کارخانه تا محل پروژه بر عهده خریدار می‌باشد.",
            "کلیه چک‌های پرداختی باید ثبت شده در سامانه صیاد و بنام خریدار باشد."
        )
    }

    val selectedTermsIndices = remember { mutableStateListOf<Int>().apply { addAll(0..10) } }
    var customTermInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.editorInvoiceId == null) {
                            if (isRtl) "صدور فاکتور جدید" else "New Invoice"
                        } else {
                            if (isRtl) "ویرایش فاکتور" else "Edit Invoice"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (viewModel.editorCustomerId == null) {
                                // Fallback select first customer if empty
                                if (customers.isNotEmpty()) {
                                    viewModel.editorCustomerId = customers.first().id
                                }
                            }
                            viewModel.saveInvoice { savedId ->
                                onSaveSuccess(savedId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("save_invoice_button")
                    ) {
                        Text(text = if (isRtl) "ثبت نهایی" else "Save & Publish", color = Color.White, fontSize = 12.sp)
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Customer Selector ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRtl) "طرف حساب / خریدار" else "Client / Customer",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(
                                onClick = {
                                    quickCustName = ""
                                    quickCustCompany = ""
                                    quickCustPhone = ""
                                    quickCustTaxNumber = ""
                                    quickCustNationalId = ""
                                    quickCustAddress = ""
                                    showQuickCustomerDialog = true
                                }
                            ) {
                                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isRtl) "خریدار جدید" else "New Client",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Selector Box
                        val selectedCustomer = customers.find { it.id == viewModel.editorCustomerId }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { customerDropdownExpanded = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCustomer?.let { "${it.name} - ${it.company}" }
                                        ?: (if (isRtl) "انتخاب خریدار از لیست..." else "Select Customer..."),
                                    color = if (selectedCustomer != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "مشتری‌ها")
                            }

                            DropdownMenu(
                                expanded = customerDropdownExpanded,
                                onDismissRequest = { customerDropdownExpanded = false }
                            ) {
                                customers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = { Text("${cust.name} (${cust.company})") },
                                        onClick = {
                                            viewModel.editorCustomerId = cust.id
                                            customerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Template Selector ---
            item {
                var templateDropdownExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isRtl) "قالب فاکتور" else "Invoice Template",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { templateDropdownExpanded = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (viewModel.editorTemplate == "Nest") "Nest (صنایع چوبی)" else (if (isRtl) "عمومی" else "General"),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = templateDropdownExpanded,
                                onDismissRequest = { templateDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isRtl) "عمومی" else "General") },
                                    onClick = {
                                        viewModel.editorTemplate = "General"
                                        templateDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nest (صنایع چوبی)") },
                                    onClick = {
                                        viewModel.editorTemplate = "Nest"
                                        templateDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- Invoice Type Selector ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isRtl) "نوع فاکتور" else "Invoice Type",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isPre = viewModel.editorInvoiceType == "پیش‌فاکتور"
                            val isSales = viewModel.editorInvoiceType == "فاکتور فروش" || viewModel.editorInvoiceType == "فاکتور"
                            val isInst = viewModel.editorInvoiceType == "فاکتور نصب و اجرا" || viewModel.editorInvoiceType == "نصب و اجرا"

                            FilterChip(
                                selected = isPre,
                                onClick = { viewModel.editorInvoiceType = "پیش‌فاکتور" },
                                label = { Text(if (isRtl) "پیش‌فاکتور" else "Pre-Invoice", fontSize = 11.sp) },
                                leadingIcon = {
                                    if (isPre) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                modifier = Modifier.weight(1f).testTag("invoice_type_preinvoice")
                            )

                            FilterChip(
                                selected = isSales,
                                onClick = { viewModel.editorInvoiceType = "فاکتور فروش" },
                                label = { Text(if (isRtl) "فاکتور فروش" else "Sales Invoice", fontSize = 11.sp) },
                                leadingIcon = {
                                    if (isSales) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                modifier = Modifier.weight(1f).testTag("invoice_type_salesinvoice")
                            )

                            FilterChip(
                                selected = isInst,
                                onClick = { viewModel.editorInvoiceType = "فاکتور نصب و اجرا" },
                                label = { Text(if (isRtl) "نصب و اجرا" else "Installation", fontSize = 11.sp) },
                                leadingIcon = {
                                    if (isInst) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                modifier = Modifier.weight(1f).testTag("invoice_type_installation")
                            )
                        }
                    }
                }
            }

            // --- Invoice Info Form (Expandable Accordion) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isGeneralInfoExpanded = !isGeneralInfoExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isRtl) "اطلاعات کلی، مسئول فروش و پشتیبانی" else "Invoice Specification & Staff",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (!isGeneralInfoExpanded) {
                                    val summaryText = buildString {
                                        append("شماره: ${viewModel.editorInvoiceNumber}")
                                        if (viewModel.editorSalesperson.isNotBlank()) append(" | مسئول فروش: ${viewModel.editorSalesperson}")
                                        if (viewModel.editorSupportPerson.isNotBlank()) append(" | پشتیبانی: ${viewModel.editorSupportPerson}")
                                    }
                                    Text(
                                        text = summaryText,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { isGeneralInfoExpanded = !isGeneralInfoExpanded }) {
                                Icon(
                                    imageVector = if (isGeneralInfoExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Accordion Toggle",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isGeneralInfoExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Invoice number & Ref
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SelectOnFocusTextField(
                                        value = viewModel.editorInvoiceNumber,
                                        onValueChange = { viewModel.editorInvoiceNumber = it },
                                        label = { Text(if (isRtl) "شماره فاکتور" else "Invoice Number") },
                                        supportingText = { Text(if (isRtl) "پیش‌فرض تنظیمات (قابل تغییر)" else "Default from settings (editable)", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    SelectOnFocusTextField(
                                        value = viewModel.editorReferenceNo,
                                        onValueChange = { viewModel.editorReferenceNo = it },
                                        label = { Text(if (isRtl) "کد مرجع" else "Ref Code") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                // Project and PO
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        SelectOnFocusTextField(
                                            value = viewModel.editorPoNumber,
                                            onValueChange = { viewModel.editorPoNumber = it },
                                            label = { Text(if (isRtl) "شماره سفارش (PO)" else "PO Number") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        SelectOnFocusTextField(
                                            value = viewModel.editorProjectNumber,
                                            onValueChange = { viewModel.editorProjectNumber = it },
                                            label = { Text(if (isRtl) "کد / نام پروژه" else "Project Code / Name") },
                                            placeholder = { Text(if (isRtl) "مثلاً پروژه برج آرمون" else "Project name...") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    if (allProjectsList.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isRtl) "انتخاب پروژه:" else "Select Project:",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            allProjectsList.forEach { proj ->
                                                val projDisp = if (proj.code.isNotBlank()) "${proj.name} (${proj.code})" else proj.name
                                                val isSelected = viewModel.editorProjectNumber == proj.code || viewModel.editorProjectNumber == proj.name
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        viewModel.editorProjectNumber = proj.code.ifBlank { proj.name }
                                                        if (proj.customerId != null && viewModel.editorCustomerId == null) {
                                                            viewModel.editorCustomerId = proj.customerId
                                                        }
                                                    },
                                                    label = { Text(projDisp, fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Staff Field (Sales & Support Representative)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    SelectOnFocusTextField(
                                        value = viewModel.editorSalesperson,
                                        onValueChange = { viewModel.editorSalesperson = it },
                                        label = { Text(if (isRtl) "مسئول فروش / کارشناس پشتیبانی" else "Sales & Support Staff") },
                                        placeholder = { Text(if (isRtl) "نام مسئول فروش یا پشتیبانی را وارد یا انتخاب کنید" else "Staff name...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    if (suggestedStaffList.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isRtl) "پیشنهاد کارشناسان:" else "Suggested Staff:",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            suggestedStaffList.forEach { person ->
                                                FilterChip(
                                                    selected = viewModel.editorSalesperson == person,
                                                    onClick = { viewModel.editorSalesperson = person },
                                                    label = { Text(person, fontSize = 11.sp) }
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

            // --- Line Items Title ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRtl) "اقلام فاکتور / کالا و خدمات" else "Line Items",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showProductDialog = true }) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isRtl) "درج از کالاها" else "Import Item", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.addLineItem() },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.1f), contentColor = accentColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isRtl) "افزودن ردیف" else "Add Row", fontSize = 11.sp)
                        }
                    }
                }
            }



            itemsIndexed(viewModel.editorLineItems) { index, item ->
                LineItemEditorRow(
                    index = index,
                    item = item,
                    isRtl = isRtl,
                    colorCodes = colorCodesList,
                    surfaceTreatments = surfaceTreatmentsList,
                    allProducts = products,
                    onUpdate = { updated -> viewModel.updateLineItem(index, updated) },
                    onDelete = { viewModel.removeLineItem(index) },
                    onDuplicate = { viewModel.duplicateLineItem(index) },
                    onSaveProductPriceToDb = { updatedProd -> viewModel.saveProduct(updatedProd) },
                    cardBackground = cardBackground
                )
            }

            // --- Financial Inputs (Expandable Accordion) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isFinancialExpanded = !isFinancialExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isRtl) "مالیات، تخفیف و هزینه‌های جانبی (امور مالی)" else "Discounts, Taxes & Logistics",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (!isFinancialExpanded) {
                                    val summaryText = buildString {
                                        if (viewModel.editorTaxRate > 0) append("ارزش افزوده: ۱۰٪ | ")
                                        if (viewModel.editorDiscountType == "Percent" && viewModel.editorDiscountRate > 0) append("تخفیف: ${viewModel.editorDiscountRate}%")
                                        else if (viewModel.editorDiscountAmount > 0) append("تخفیف: ${Helper.formatCurrency(viewModel.editorDiscountAmount, "تومان", true)}")
                                        else append("بدون تخفیف")
                                    }
                                    Text(
                                        text = summaryText,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { isFinancialExpanded = !isFinancialExpanded }) {
                                Icon(
                                    imageVector = if (isFinancialExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Accordion Toggle",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isFinancialExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Tax & VAT Switch
                                val hasTax = viewModel.editorTaxRate > 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isRtl) "محاسبه مالیات بر ارزش افزوده (%۱۰)" else "Include VAT & Tax (10%)",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.5.sp
                                    )
                                    Switch(
                                        checked = hasTax,
                                        onCheckedChange = { isChecked ->
                                            viewModel.setOverallTaxRate(if (isChecked) 10.0 else 0.0)
                                        },
                                        modifier = Modifier.testTag("vat_tax_switch")
                                    )
                                }

                                // Discount Type Toggle
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (isRtl) "نوع تخفیف کل فاکتور:" else "Discount Mode:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = viewModel.editorDiscountType == "Percent",
                                            onClick = { viewModel.editorDiscountType = "Percent" },
                                            label = { Text(if (isRtl) "درصدی (%)" else "Percent (%)", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = viewModel.editorDiscountType == "Amount",
                                            onClick = { viewModel.editorDiscountType = "Amount" },
                                            label = { Text(if (isRtl) "مبلغ ثابت (تومان)" else "Fixed Amount", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (viewModel.editorDiscountType == "Percent") {
                                        SelectOnFocusTextField(
                                            value = Helper.formatDouble(viewModel.editorDiscountRate, false),
                                            onValueChange = { viewModel.editorDiscountRate = it.toDoubleOrNull() ?: 0.0 },
                                            label = { Text(if (isRtl) "درصد تخفیف کل" else "Discount %") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true
                                        )
                                    } else {
                                        SelectOnFocusTextField(
                                            value = Helper.formatWithCommas(viewModel.editorDiscountAmount, false),
                                            onValueChange = { viewModel.editorDiscountAmount = Helper.parseFormattedToDouble(it) },
                                            label = { Text(if (isRtl) "مبلغ تخفیف (تومان)" else "Discount Amount") },
                                            supportingText = {
                                                if (viewModel.editorDiscountAmount > 0) {
                                                    Text(text = Helper.formatCurrency(viewModel.editorDiscountAmount, "تومان", isRtl), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true
                                        )
                                    }

                                    if (hasTax) {
                                        SelectOnFocusTextField(
                                            value = Helper.formatDouble(viewModel.editorTaxRate, false),
                                            onValueChange = { viewModel.setOverallTaxRate(it.toDoubleOrNull() ?: 0.0) },
                                            label = { Text(if (isRtl) "درصد مالیات" else "Tax %") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SelectOnFocusTextField(
                                        value = Helper.formatWithCommas(viewModel.editorShipping, false),
                                        onValueChange = { viewModel.editorShipping = Helper.parseFormattedToDouble(it) },
                                        label = { Text(if (isRtl) "هزینه حمل (تومان)" else "Shipping Cost") },
                                        supportingText = {
                                            if (viewModel.editorShipping > 0) {
                                                Text(text = Helper.formatCurrency(viewModel.editorShipping, "تومان", isRtl), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                    SelectOnFocusTextField(
                                        value = Helper.formatWithCommas(viewModel.editorHandling, false),
                                        onValueChange = { viewModel.editorHandling = Helper.parseFormattedToDouble(it) },
                                        label = { Text(if (isRtl) "سایر هزینه‌ها (تومان)" else "Logistics / Handling") },
                                        supportingText = {
                                            if (viewModel.editorHandling > 0) {
                                                Text(text = Helper.formatCurrency(viewModel.editorHandling, "تومان", isRtl), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Settlement & Terms Inputs (Expandable Accordion) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSettlementExpanded = !isSettlementExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isRtl) "تسویه، پیش‌پرداخت و شرایط پرداخت" else "Settlement & Terms",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (!isSettlementExpanded) {
                                    val summaryText = buildString {
                                        if (viewModel.editorAdvancePayment > 0) append("پیش‌پرداخت: ${Helper.formatCurrency(viewModel.editorAdvancePayment, "تومان", true)}")
                                        else append("بدون پیش‌پرداخت")
                                    }
                                    Text(
                                        text = summaryText,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { isSettlementExpanded = !isSettlementExpanded }) {
                                Icon(
                                    imageVector = if (isSettlementExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Accordion Toggle",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isSettlementExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SelectOnFocusTextField(
                                    value = Helper.formatWithCommas(viewModel.editorAdvancePayment, false),
                                    onValueChange = { viewModel.editorAdvancePayment = Helper.parseFormattedToDouble(it) },
                                    label = { Text(if (isRtl) "مبلغ پیش‌پرداخت (تومان)" else "Prepayment Amount") },
                                    supportingText = {
                                        if (viewModel.editorAdvancePayment > 0) {
                                            Text(text = Helper.formatCurrency(viewModel.editorAdvancePayment, "تومان", isRtl), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )

                                // Terms Checklist Popup Button
                                OutlinedButton(
                                    onClick = { showTermsPopup = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isRtl) "انتخاب شرایط و مقررات فروش و پرداخت (پاپ‌آپ)" else "Select Terms & Sales Conditions (Popup)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                SelectOnFocusTextField(
                                    value = viewModel.editorPaymentTerms,
                                    onValueChange = { viewModel.editorPaymentTerms = it },
                                    label = { Text(if (isRtl) "شرایط پرداخت" else "Payment Terms") },
                                    placeholder = { Text(if (isRtl) "مثلاً ۵۰٪ نقد، ۵۰٪ چک صیادی..." else "Payment conditions...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = false,
                                    maxLines = 2
                                )

                                SelectOnFocusTextField(
                                    value = viewModel.editorShippingTerms,
                                    onValueChange = { viewModel.editorShippingTerms = it },
                                    label = { Text(if (isRtl) "شرایط حمل و تحویل بار" else "Shipping & Delivery Terms") },
                                    placeholder = { Text(if (isRtl) "مثلاً تحویل درب کارخانه / ارسال با باربری..." else "Delivery conditions...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = false,
                                    maxLines = 2
                                )

                                SelectOnFocusTextField(
                                    value = viewModel.editorNotes,
                                    onValueChange = { viewModel.editorNotes = it },
                                    label = { Text(if (isRtl) "توضیحات و ملاحظات فاکتور" else "Additional Terms and Notes") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = false,
                                    maxLines = 4
                                )
                            }
                        }
                    }
                }
            }

            // --- Bank Account Selector ---
            item {
                val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
                var bankDropdownExpanded by remember { mutableStateOf(false) }
                val selectedBank = bankAccounts.find { it.id == viewModel.editorBankAccountId } ?: bankAccounts.find { it.isDefault } ?: bankAccounts.firstOrNull()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isRtl) "حساب بانکی واریز وجه (پایین فاکتور)" else "Payment Bank Account",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { bankDropdownExpanded = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedBank != null) "${selectedBank.bankName} - ${selectedBank.accountHolderName} (${selectedBank.accountNumber})" else (if (isRtl) "انتخاب حساب بانکی" else "Select Bank Account"),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = bankDropdownExpanded,
                                onDismissRequest = { bankDropdownExpanded = false }
                            ) {
                                bankAccounts.forEach { bank ->
                                    DropdownMenuItem(
                                        text = { Text("${bank.bankName} - ${bank.accountNumber} ${if(bank.isDefault) "(پیش‌فرض)" else ""}") },
                                        onClick = {
                                            viewModel.editorBankAccountId = bank.id
                                            bankDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                PaymentCard(viewModel, isRtl)
            }

            // --- Summary Totals Preview ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isRtl) "خلاصه محاسبات مالی" else "Calculation Balance",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        SummaryRow(
                            label = if (isRtl) "جمع کل اقلام (قبل تخفیف)" else "Subtotal",
                            value = Helper.formatCurrency(viewModel.getDraftSubtotal(), viewModel.selectedCurrency, viewModel.usePersianDigits)
                        )
                        SummaryRow(
                            label = if (isRtl) "مجموع تخفیف اعمال شده" else "Total Discount",
                            value = Helper.formatCurrency(viewModel.getDraftDiscountVal(), viewModel.selectedCurrency, viewModel.usePersianDigits),
                            color = Color(0xFFEF4444)
                        )
                        SummaryRow(
                            label = if (isRtl) "مالیات بر ارزش افزوده" else "Value-Added Tax",
                            value = Helper.formatCurrency(viewModel.getDraftTaxVal(), viewModel.selectedCurrency, viewModel.usePersianDigits)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SummaryRow(
                            label = if (isRtl) "جمع کل فاکتور" else "Grand Total",
                            value = Helper.formatCurrency(viewModel.getDraftTotal(), viewModel.selectedCurrency, viewModel.usePersianDigits),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        SummaryRow(
                            label = if (isRtl) "مانده حساب خالص" else "Net Balance Due",
                            value = Helper.formatCurrency(viewModel.getDraftOutstanding(), viewModel.selectedCurrency, viewModel.usePersianDigits),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Extra padding
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- Product Import Dialog ---
    if (showProductDialog) {
        Dialog(onDismissRequest = { showProductDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isRtl) "انتخاب و درج از کاتالوگ کالا" else "Select from Product Catalog",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        itemsIndexed(products) { _, prod ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        viewModel.addLineItem(prod)
                                        showProductDialog = false
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "کد: ${prod.sku.ifEmpty { "-" }} | رنگ: ${prod.colorCode.ifEmpty { "-" }} | سطح: ${prod.surfaceTreatment.ifEmpty { "-" }} | شاخه: ${Helper.formatDouble(prod.branchCount, viewModel.usePersianDigits)}",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = Helper.formatCurrency(prod.price, viewModel.selectedCurrency, viewModel.usePersianDigits),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { showProductDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (isRtl) "بستن" else "Close")
                    }
                }
            }
        }
    }

    // --- Quick Customer Add Dialog ---
    if (showQuickCustomerDialog) {
        Dialog(onDismissRequest = { showQuickCustomerDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isRtl) "ثبت سریع خریدار جدید" else "Quick Add Customer",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    SelectOnFocusTextField(
                        value = quickCustName,
                        onValueChange = { quickCustName = it },
                        label = { Text(if (isRtl) "نام و نام خانوادگی خریدار *" else "Customer Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    SelectOnFocusTextField(
                        value = quickCustCompany,
                        onValueChange = { quickCustCompany = it },
                        label = { Text(if (isRtl) "نام شرکت / پروژه / مجموعه" else "Company / Project") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    SelectOnFocusTextField(
                        value = quickCustPhone,
                        onValueChange = { quickCustPhone = it },
                        label = { Text(if (isRtl) "شماره همراه / تماس" else "Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectOnFocusTextField(
                            value = quickCustTaxNumber,
                            onValueChange = { quickCustTaxNumber = it },
                            label = { Text(if (isRtl) "کد اقتصادی" else "Economic Code") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        SelectOnFocusTextField(
                            value = quickCustNationalId,
                            onValueChange = { quickCustNationalId = it },
                            label = { Text(if (isRtl) "شناسه/کد ملی" else "National ID") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    SelectOnFocusTextField(
                        value = quickCustAddress,
                        onValueChange = { quickCustAddress = it },
                        label = { Text(if (isRtl) "آدرس خریدار" else "Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showQuickCustomerDialog = false }) {
                            Text(if (isRtl) "انصراف" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (quickCustName.isNotBlank()) {
                                    val newCustomer = Customer(
                                        name = quickCustName.trim(),
                                        company = quickCustCompany.trim(),
                                        phone = quickCustPhone.trim(),
                                        taxId = quickCustTaxNumber.trim(),
                                        nationalId = quickCustNationalId.trim(),
                                        billingAddress = quickCustAddress.trim()
                                    )
                                    viewModel.saveCustomerAndSelect(newCustomer) { newId ->
                                        showQuickCustomerDialog = false
                                    }
                                }
                            },
                            enabled = quickCustName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text(if (isRtl) "ذخیره و انتخاب" else "Save & Select", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // --- Terms Checklist Popup Dialog ---
    if (showTermsPopup) {
        AlertDialog(
            onDismissRequest = { showTermsPopup = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.FactCheck, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRtl) "مدیریت شرایط و مقررات فروش و پرداخت" else "Sales & Payment Terms Checklist",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isRtl) "شرایط مورد نظر را جهت درج در فاکتور انتخاب بفرمایید:" else "Select terms to include on invoice:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                selectedTermsIndices.clear()
                                selectedTermsIndices.addAll(defaultSalesTermsList.indices)
                            }
                        ) {
                            Text(if (isRtl) "انتخاب همه" else "Select All", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = { selectedTermsIndices.clear() }
                        ) {
                            Text(if (isRtl) "عدم انتخاب" else "Deselect All", fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(defaultSalesTermsList) { idx, termText ->
                            val isChecked = selectedTermsIndices.contains(idx)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isChecked) selectedTermsIndices.remove(idx)
                                        else selectedTermsIndices.add(idx)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked == true) {
                                            if (!selectedTermsIndices.contains(idx)) selectedTermsIndices.add(idx)
                                        } else {
                                            selectedTermsIndices.remove(idx)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${Helper.formatDouble((idx + 1).toDouble(), viewModel.usePersianDigits)}- $termText",
                                    fontSize = 11.5.sp,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Add Custom Term
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SelectOnFocusTextField(
                            value = customTermInput,
                            onValueChange = { customTermInput = it },
                            placeholder = { Text(if (isRtl) "افزودن بند / شرط سفارشی..." else "Add custom term...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (customTermInput.isNotBlank()) {
                                    defaultSalesTermsList.add(customTermInput.trim())
                                    selectedTermsIndices.add(defaultSalesTermsList.size - 1)
                                    customTermInput = ""
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add", tint = accentColor)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formattedNotes = selectedTermsIndices.sorted().mapIndexed { i, origIdx ->
                            val termText = defaultSalesTermsList.getOrNull(origIdx) ?: ""
                            "${Helper.formatDouble((i + 1).toDouble(), viewModel.usePersianDigits)}- $termText"
                        }.filter { it.isNotBlank() }.joinToString("\n")

                        viewModel.editorNotes = formattedNotes
                        showTermsPopup = false
                    }
                ) {
                    Text(if (isRtl) "تایید و اعمال روی فاکتور" else "Apply Terms")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTermsPopup = false }) {
                    Text(if (isRtl) "انصراف" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun LineItemEditorRow(
    index: Int,
    item: InvoiceLineItem,
    isRtl: Boolean,
    colorCodes: List<String> = emptyList(),
    surfaceTreatments: List<String> = emptyList(),
    allProducts: List<com.example.data.model.Product> = emptyList(),
    onUpdate: (InvoiceLineItem) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onSaveProductPriceToDb: ((com.example.data.model.Product) -> Unit)? = null,
    cardBackground: Color
) {
    val isWood = item.categoryType == "Wood" || item.categoryType == "چوب پلاست"
    val isAccessory = item.categoryType == "Accessory" || item.categoryType == "پیچ و کلیپس"
    val isInstallation = item.categoryType == "Installation" || item.categoryType == "نصب"

    val allProductNames = remember(allProducts) {
        (allProducts.map { it.name } + WoodPresets.profiles.map { it.name } + AccessoryPresets.items.map { it.name }).filter { it.isNotBlank() }.distinct()
    }
    val allColorCodes = remember(allProducts, colorCodes) {
        (allProducts.map { it.colorCode } + colorCodes + listOf("N1", "N2", "N3", "C1", "C2", "W1", "W2", "G1", "G2")).filter { it.isNotBlank() }.distinct()
    }
    val allSurfaceTreatments = remember(allProducts, surfaceTreatments) {
        (allProducts.map { it.surfaceTreatment } + surfaceTreatments + listOf("BR (برس خورده)", "Emboss (طرح چوب / امبوس)", "Sanded (سنباده خورده)", "Smooth (صیقلی)")).filter { it.isNotBlank() }.distinct()
    }
    val requestSubjects = remember {
        listOf("کفپوش دور استخر", "پوشش نما و دیواره", "سقف کاذب و آلاچیق", "اجرای پله و محوطه", "زیرسازی و شاسی‌کشی", "ترمووود و چوب پلاست", "نصب ترامپولین / فلاورباکس")
    }
    val standardUnits = remember {
        listOf("متر طول", "قطعه", "عدد", "مترمربع", "شاخه", "کیلوگرم", "بسته", "رول", "متر", "ساعت", "دستگاه", "ست", "کارتن", "تن")
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("line_item_editor_$index"),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with Row # and category selector (Wood vs Accessory vs Installation)
            // 1. Top Bar: Row Badge + Duplicate & Delete Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isRtl) "ردیف #${Helper.toPersianDigits((index + 1).toString())}" else "Row #${index + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duplicate Button
                    Surface(
                        onClick = onDuplicate,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = if (isRtl) "کپی ردیف" else "Duplicate",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isRtl) "کپی" else "Copy",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Delete Button - High Visibility Red Button with Text Label
                    Surface(
                        onClick = onDelete,
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = if (isRtl) "حذف ردیف" else "Delete",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isRtl) "حذف" else "Delete",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            // 2. Category Toggle Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isWood,
                    onClick = {
                        if (!isWood) {
                            val b = if (item.branchCount > 0) item.branchCount else item.quantity
                            val q = b * 3.0
                            onUpdate(item.copy(categoryType = "Wood", unit = "متر طول", branchCount = b, quantity = q))
                        }
                    },
                    label = { Text(if (isRtl) "چوب پلاست" else "WPC Wood", fontSize = 11.sp) },
                    modifier = Modifier.height(30.dp)
                )
                FilterChip(
                    selected = isAccessory,
                    onClick = {
                        if (!isAccessory) {
                            val q = if (item.branchCount > 0) item.branchCount else (item.quantity / 3.0).coerceAtLeast(1.0)
                            onUpdate(item.copy(categoryType = "Accessory", unit = "قطعه", branchCount = 0.0, quantity = q))
                        }
                    },
                    label = { Text(if (isRtl) "پیچ و کلیپس" else "Accessory / Clip", fontSize = 11.sp) },
                    modifier = Modifier.height(30.dp)
                )
                FilterChip(
                    selected = isInstallation,
                    onClick = {
                        if (!isInstallation) {
                            onUpdate(item.copy(categoryType = "Installation", unit = "مترمربع", requestSubject = if (item.requestSubject.isNotEmpty()) item.requestSubject else "کفپوش دور استخر"))
                        }
                    },
                    label = { Text(if (isRtl) "نصب و اجرا" else "Installation", fontSize = 11.sp) },
                    modifier = Modifier.height(30.dp)
                )
            }

            if (isInstallation) {
                // --- Installation / Execution Category Fields ---
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccordionPickerField(
                        value = item.requestSubject.ifEmpty { "کفپوش دور استخر" },
                        onValueChange = { inputSubject ->
                            onUpdate(item.copy(requestSubject = inputSubject))
                        },
                        label = if (isRtl) "موضوع درخواست" else "Request Subject",
                        placeholder = "کفپوش دور استخر",
                        options = requestSubjects,
                        isRtl = isRtl,
                        modifier = Modifier.weight(1.5f)
                    )
                    AccordionPickerField(
                        value = item.name,
                        onValueChange = { inputName ->
                            val foundProd = allProducts.find { it.name == inputName || it.sku == inputName }
                            if (foundProd != null) {
                                onUpdate(
                                    item.copy(
                                        name = foundProd.name,
                                        sku = foundProd.sku,
                                        colorCode = if (foundProd.colorCode.isNotEmpty()) foundProd.colorCode else item.colorCode,
                                        surfaceTreatment = if (foundProd.surfaceTreatment.isNotEmpty()) foundProd.surfaceTreatment else item.surfaceTreatment,
                                        unitPrice = if (foundProd.price > 0) foundProd.price else item.unitPrice
                                    )
                                )
                            } else {
                                val presetWood = WoodPresets.findPresetByNameOrSku(inputName)
                                if (presetWood != null) {
                                    onUpdate(item.copy(name = presetWood.name, sku = presetWood.sku))
                                } else {
                                    onUpdate(item.copy(name = inputName))
                                }
                            }
                        },
                        label = if (isRtl) "نام کالا" else "Product Name",
                        placeholder = "انتخاب کالا...",
                        options = allProductNames,
                        isRtl = isRtl,
                        modifier = Modifier.weight(1.5f).testTag("line_item_name_input_$index")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccordionPickerField(
                        value = item.colorCode,
                        onValueChange = { onUpdate(item.copy(colorCode = it)) },
                        label = if (isRtl) "کد رنگ" else "Color Code",
                        placeholder = "N3",
                        options = allColorCodes,
                        isRtl = isRtl,
                        modifier = Modifier.weight(1f)
                    )
                    AccordionPickerField(
                        value = item.surfaceTreatment,
                        onValueChange = { onUpdate(item.copy(surfaceTreatment = it)) },
                        label = if (isRtl) "عملیات سطحی" else "Surface Finish",
                        placeholder = "BR",
                        options = allSurfaceTreatments,
                        isRtl = isRtl,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectOnFocusTextField(
                        value = if (item.branchCount > 0) Helper.formatDouble(item.branchCount, false) else "",
                        onValueChange = { input ->
                            val b = input.toDoubleOrNull() ?: 0.0
                            onUpdate(item.copy(branchCount = b))
                        },
                        textStyle = TextStyle(fontSize = 11.sp),
                        label = { Text(if (isRtl) "تعداد شاخه / قطعه" else "Branch Count", fontSize = 10.sp, maxLines = 1) },
                        placeholder = { Text("70", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    SelectOnFocusTextField(
                        value = if (item.quantity > 0) Helper.formatDouble(item.quantity, false) else "",
                        onValueChange = { input ->
                            val q = input.toDoubleOrNull() ?: 0.0
                            onUpdate(item.copy(quantity = q))
                        },
                        textStyle = TextStyle(fontSize = 11.sp),
                        label = { Text(if (isRtl) "متراژ / تعداد (m²)" else "Area / Qty", fontSize = 10.sp, maxLines = 1) },
                        placeholder = { Text("70", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    SelectOnFocusTextField(
                        value = Helper.formatWithCommas(item.unitPrice, false),
                        onValueChange = { input ->
                            val parsed = Helper.parseFormattedToDouble(input)
                            onUpdate(item.copy(unitPrice = parsed))
                        },
                        textStyle = TextStyle(fontSize = 11.sp),
                        label = { Text(if (isRtl) "قیمت واحد (تومان)" else "Unit Price", fontSize = 10.sp, maxLines = 1) },
                        placeholder = { Text("750,000", fontSize = 10.sp) },
                        modifier = Modifier.weight(1.2f).testTag("line_item_price_input_$index"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isRtl) "اطلاعات و هزینه‌های اجرایی پروژه:" else "Execution Specs & Expenses:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectOnFocusTextField(
                                value = if (item.executionDays > 0) item.executionDays.toString() else "",
                                onValueChange = { input ->
                                    onUpdate(item.copy(executionDays = input.toIntOrNull() ?: 0))
                                },
                                textStyle = TextStyle(fontSize = 11.sp),
                                label = { Text(if (isRtl) "مدت زمان اجرا (روز کاری)" else "Duration (Days)", fontSize = 9.5.sp, maxLines = 1) },
                                placeholder = { Text("3", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            SelectOnFocusTextField(
                                value = if (item.teamSize > 0) item.teamSize.toString() else "",
                                onValueChange = { input ->
                                    onUpdate(item.copy(teamSize = input.toIntOrNull() ?: 0))
                                },
                                textStyle = TextStyle(fontSize = 11.sp),
                                label = { Text(if (isRtl) "تعداد نفرات تیم" else "Team Members", fontSize = 9.5.sp, maxLines = 1) },
                                placeholder = { Text("2", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectOnFocusTextField(
                                value = if (item.accommodationCost > 0) Helper.formatWithCommas(item.accommodationCost, false) else "",
                                onValueChange = { input ->
                                    onUpdate(item.copy(accommodationCost = Helper.parseFormattedToDouble(input)))
                                },
                                textStyle = TextStyle(fontSize = 11.sp),
                                label = { Text(if (isRtl) "هزینه اسکان (تومان)" else "Accommodation", fontSize = 9.5.sp, maxLines = 1) },
                                placeholder = { Text("0", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            SelectOnFocusTextField(
                                value = if (item.transportationCost > 0) Helper.formatWithCommas(item.transportationCost, false) else "",
                                onValueChange = { input ->
                                    onUpdate(item.copy(transportationCost = Helper.parseFormattedToDouble(input)))
                                },
                                textStyle = TextStyle(fontSize = 11.sp),
                                label = { Text(if (isRtl) "ایاب و ذهاب (تومان)" else "Travel", fontSize = 9.5.sp, maxLines = 1) },
                                placeholder = { Text("0", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            SelectOnFocusTextField(
                                value = if (item.consumablesCost > 0) Helper.formatWithCommas(item.consumablesCost, false) else "",
                                onValueChange = { input ->
                                    onUpdate(item.copy(consumablesCost = Helper.parseFormattedToDouble(input)))
                                },
                                textStyle = TextStyle(fontSize = 11.sp),
                                label = { Text(if (isRtl) "ضد زنگ و اقلام مصرفی" else "Consumables", fontSize = 9.5.sp, maxLines = 1) },
                                placeholder = { Text("0", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
            } else {
                // Name & SKU
                val woodProfileNames = WoodPresets.profiles.map { it.name }
                val woodProfileSkus = WoodPresets.profiles.map { it.sku }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isWood) {
                        AccordionPickerField(
                            value = item.name,
                            onValueChange = { inputName ->
                                val preset = WoodPresets.findPresetByNameOrSku(inputName)
                                if (preset != null) {
                                    var b = item.branchCount
                                    var q = item.quantity
                                    if (item.initialAreaSqm > 0.0) {
                                        val res = WoodPresets.calculateBranches(item.initialAreaSqm, preset.crossSectionFactor)
                                        b = res.first
                                        q = res.second
                                    }
                                    val dbProd = allProducts.find { (it.name.isNotBlank() && it.name.trim().equals(preset.name.trim(), ignoreCase = true)) || (it.sku.isNotBlank() && it.sku.trim().equals(preset.sku.trim(), ignoreCase = true)) }
                                    val autoPrice = if (dbProd != null && dbProd.price > 0) dbProd.price else (if (preset.defaultPrice > 0) preset.defaultPrice else item.unitPrice)
                                    onUpdate(
                                        item.copy(
                                            name = preset.name,
                                            sku = preset.sku,
                                            crossSectionFactor = preset.crossSectionFactor,
                                            branchCount = if (b > 0) b else item.branchCount,
                                            quantity = if (q > 0) q else item.quantity,
                                            unitPrice = if (autoPrice > 0) autoPrice else item.unitPrice,
                                            unit = "متر طول"
                                        )
                                    )
                                } else {
                                    val dbProd = allProducts.find { (it.name.isNotBlank() && it.name.trim().equals(inputName.trim(), ignoreCase = true)) || (it.sku.isNotBlank() && it.sku.trim().equals(inputName.trim(), ignoreCase = true)) }
                                    if (dbProd != null) {
                                        onUpdate(item.copy(name = dbProd.name, sku = dbProd.sku, unitPrice = if (dbProd.price > 0) dbProd.price else item.unitPrice))
                                    } else {
                                        onUpdate(item.copy(name = inputName))
                                    }
                                }
                            },
                            label = if (isRtl) "مدل / پروفیل (پیش‌فرض)" else "Product / Profile",
                            placeholder = "FEEL / ONCE",
                            options = woodProfileNames,
                            isRtl = isRtl,
                            modifier = Modifier.weight(1.8f).testTag("line_item_name_input_$index")
                        )
                        AccordionPickerField(
                            value = item.sku,
                            onValueChange = { inputSku ->
                                val preset = WoodPresets.findPresetByNameOrSku(inputSku)
                                if (preset != null) {
                                    var b = item.branchCount
                                    var q = item.quantity
                                    if (item.initialAreaSqm > 0.0) {
                                        val res = WoodPresets.calculateBranches(item.initialAreaSqm, preset.crossSectionFactor)
                                        b = res.first
                                        q = res.second
                                    }
                                    val dbProd = allProducts.find { (it.name.isNotBlank() && it.name.trim().equals(preset.name.trim(), ignoreCase = true)) || (it.sku.isNotBlank() && it.sku.trim().equals(preset.sku.trim(), ignoreCase = true)) }
                                    val autoPrice = if (dbProd != null && dbProd.price > 0) dbProd.price else (if (preset.defaultPrice > 0) preset.defaultPrice else item.unitPrice)
                                    onUpdate(
                                        item.copy(
                                            name = preset.name,
                                            sku = preset.sku,
                                            crossSectionFactor = preset.crossSectionFactor,
                                            branchCount = if (b > 0) b else item.branchCount,
                                            quantity = if (q > 0) q else item.quantity,
                                            unitPrice = if (autoPrice > 0) autoPrice else item.unitPrice,
                                            unit = "متر طول"
                                        )
                                    )
                                } else {
                                    val dbProd = allProducts.find { (it.sku.isNotBlank() && it.sku.trim().equals(inputSku.trim(), ignoreCase = true)) }
                                    if (dbProd != null) {
                                        onUpdate(item.copy(name = dbProd.name, sku = dbProd.sku, unitPrice = if (dbProd.price > 0) dbProd.price else item.unitPrice))
                                    } else {
                                        onUpdate(item.copy(sku = inputSku))
                                    }
                                }
                            },
                            label = if (isRtl) "کد کالا" else "Code / SKU",
                            placeholder = "FC140",
                            options = woodProfileSkus,
                            isRtl = isRtl,
                            modifier = Modifier.weight(1.2f)
                        )
                    } else {
                        val accessoryNames = AccessoryPresets.items.map { it.name }
                        val accessorySkus = AccessoryPresets.items.map { it.sku }

                        AccordionPickerField(
                            value = item.name,
                            onValueChange = { inputName ->
                                val preset = AccessoryPresets.findPresetByNameOrSku(inputName)
                                val dbProd = allProducts.find { (it.name.isNotBlank() && it.name.trim().equals(inputName.trim(), ignoreCase = true)) || (preset != null && ((it.name.isNotBlank() && it.name.trim().equals(preset.name.trim(), ignoreCase = true)) || (it.sku.isNotBlank() && it.sku.trim().equals(preset.sku.trim(), ignoreCase = true)))) }
                                if (preset != null) {
                                    val autoPrice = if (dbProd != null && dbProd.price > 0) dbProd.price else (if (preset.defaultPrice > 0) preset.defaultPrice else item.unitPrice)
                                    onUpdate(
                                        item.copy(
                                            name = preset.name,
                                            sku = preset.sku,
                                            unitPrice = autoPrice,
                                            unit = preset.unit,
                                            categoryType = "Accessory"
                                        )
                                    )
                                } else if (dbProd != null) {
                                    onUpdate(
                                        item.copy(
                                            name = dbProd.name,
                                            sku = dbProd.sku,
                                            unitPrice = if (dbProd.price > 0) dbProd.price else item.unitPrice,
                                            unit = dbProd.unit,
                                            categoryType = "Accessory"
                                        )
                                    )
                                } else {
                                    onUpdate(item.copy(name = inputName))
                                }
                            },
                            label = if (isRtl) "نام کالا / کلیپس (پیش‌فرض)" else "Accessory / Clip",
                            placeholder = "Clicker - H",
                            options = accessoryNames,
                            isRtl = isRtl,
                            modifier = Modifier.weight(1.8f).testTag("line_item_name_input_$index")
                        )
                        AccordionPickerField(
                            value = item.sku,
                            onValueChange = { inputSku ->
                                val preset = AccessoryPresets.findPresetByNameOrSku(inputSku)
                                val dbProd = allProducts.find { (it.sku.isNotBlank() && it.sku.trim().equals(inputSku.trim(), ignoreCase = true)) || (preset != null && ((it.name.isNotBlank() && it.name.trim().equals(preset.name.trim(), ignoreCase = true)) || (it.sku.isNotBlank() && it.sku.trim().equals(preset.sku.trim(), ignoreCase = true)))) }
                                if (preset != null) {
                                    val autoPrice = if (dbProd != null && dbProd.price > 0) dbProd.price else (if (preset.defaultPrice > 0) preset.defaultPrice else item.unitPrice)
                                    onUpdate(
                                        item.copy(
                                            name = preset.name,
                                            sku = preset.sku,
                                            unitPrice = autoPrice,
                                            unit = preset.unit,
                                            categoryType = "Accessory"
                                        )
                                    )
                                } else if (dbProd != null) {
                                    onUpdate(
                                        item.copy(
                                            name = dbProd.name,
                                            sku = dbProd.sku,
                                            unitPrice = if (dbProd.price > 0) dbProd.price else item.unitPrice,
                                            unit = dbProd.unit,
                                            categoryType = "Accessory"
                                        )
                                    )
                                } else {
                                    onUpdate(item.copy(sku = inputSku))
                                }
                            },
                            label = if (isRtl) "کد کالا" else "Code / SKU",
                            placeholder = "HCL",
                            options = accessorySkus,
                            isRtl = isRtl,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }

                // Attributes & Calculation Fields
                if (isWood) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccordionPickerField(
                            value = item.colorCode,
                            onValueChange = { onUpdate(item.copy(colorCode = it)) },
                            label = if (isRtl) "کد رنگ" else "Color Code",
                            placeholder = "N3",
                            options = colorCodes,
                            isRtl = isRtl,
                            modifier = Modifier.weight(1f)
                        )
                        AccordionPickerField(
                            value = item.surfaceTreatment,
                            onValueChange = { onUpdate(item.copy(surfaceTreatment = it)) },
                            label = if (isRtl) "عملیات سطحی" else "Surface Finish",
                            placeholder = "BR",
                            options = surfaceTreatments,
                            isRtl = isRtl,
                            modifier = Modifier.weight(1f)
                        )
                        SelectOnFocusTextField(
                            value = if (item.initialAreaSqm > 0) Helper.formatDouble(item.initialAreaSqm, false) else "",
                            onValueChange = { input ->
                                val area = input.toDoubleOrNull() ?: 0.0
                                val factor = if (item.crossSectionFactor > 0) item.crossSectionFactor else (WoodPresets.findPresetByNameOrSku(item.name)?.crossSectionFactor ?: 7.1428)
                                if (area > 0) {
                                    val res = WoodPresets.calculateBranches(area, factor)
                                    onUpdate(item.copy(initialAreaSqm = area, crossSectionFactor = factor, branchCount = res.first, quantity = res.second, unit = item.unit.ifEmpty { "متر طول" }))
                                } else {
                                    onUpdate(item.copy(initialAreaSqm = area))
                                }
                            },
                            textStyle = TextStyle(fontSize = 11.sp),
                            label = { Text(if (isRtl) "متراژ اولیه (m²)" else "Area (m²)", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("100", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    // Branch count & Total Meters calculation for Wood
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectOnFocusTextField(
                            value = if (item.crossSectionFactor > 0) Helper.formatDouble(item.crossSectionFactor, false) else "",
                            onValueChange = { input ->
                                val factor = input.toDoubleOrNull() ?: 0.0
                                if (item.initialAreaSqm > 0) {
                                    val res = WoodPresets.calculateBranches(item.initialAreaSqm, factor)
                                    onUpdate(item.copy(crossSectionFactor = factor, branchCount = res.first, quantity = res.second, unit = item.unit.ifEmpty { "متر طول" }))
                                } else {
                                    onUpdate(item.copy(crossSectionFactor = factor))
                                }
                            },
                            textStyle = TextStyle(fontSize = 11.sp),
                            label = { Text(if (isRtl) "ضریب مقطع" else "Factor", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("7.1428", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        SelectOnFocusTextField(
                            value = if (item.branchCount > 0) Helper.formatDouble(item.branchCount, false) else "",
                            onValueChange = { input ->
                                val b = input.toDoubleOrNull() ?: 0.0
                                val factor = if (item.crossSectionFactor > 0) item.crossSectionFactor else (WoodPresets.findPresetByNameOrSku(item.name)?.crossSectionFactor ?: 7.1428)
                                val q = b * 3.0
                                val area = if (factor > 0) q / factor else 0.0
                                onUpdate(item.copy(branchCount = b, initialAreaSqm = area, quantity = q, unit = item.unit.ifEmpty { "متر طول" }))
                            },
                            textStyle = TextStyle(fontSize = 11.sp),
                            label = { Text(if (isRtl) "تعداد شاخه (گرد)" else "Branches", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("239", fontSize = 10.sp) },
                            supportingText = {
                                if (item.branchCount > 0) {
                                    Text(
                                        text = if (isRtl) "${Helper.formatDouble(item.branchCount, false)} شاخه" else "${Helper.formatDouble(item.branchCount, false)} pcs",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        SelectOnFocusTextField(
                            value = if (item.quantity > 0) Helper.formatDouble(kotlin.math.ceil(item.quantity), false) else "",
                            onValueChange = { input ->
                                val rawQ = input.toDoubleOrNull() ?: 0.0
                                val q = kotlin.math.ceil(rawQ)
                                val factor = if (item.crossSectionFactor > 0) item.crossSectionFactor else (WoodPresets.findPresetByNameOrSku(item.name)?.crossSectionFactor ?: 7.1428)
                                val area = if (factor > 0) q / factor else 0.0
                                val res = WoodPresets.calculateBranches(area, factor)
                                onUpdate(item.copy(quantity = q, initialAreaSqm = area, branchCount = res.first, unit = item.unit.ifEmpty { "متر طول" }))
                            },
                            textStyle = TextStyle(fontSize = 11.sp),
                            label = { Text(if (isRtl) "متراژ کل (گرد به بالا)" else "Length (m)", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("715", fontSize = 10.sp) },
                            supportingText = {
                                if (item.quantity > 0) {
                                    val qCeil = kotlin.math.ceil(item.quantity)
                                    Text(
                                        text = if (isRtl) "${Helper.formatDouble(qCeil, false)} ${item.unit.ifEmpty { "متر طول" }}" else "${Helper.formatDouble(qCeil, false)} ${item.unit.ifEmpty { "m" }}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                } else {
                    // Accessory / Clip / Screws Row
                    val accPreset = AccessoryPresets.findPresetByNameOrSku(item.name) ?: AccessoryPresets.findPresetByNameOrSku(item.sku)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectOnFocusTextField(
                            value = accPreset?.specialFor ?: "-",
                            onValueChange = {},
                            enabled = false,
                            label = { Text(if (isRtl) "مخصوص پروفایل (SPECIAL FOR)" else "SPECIAL FOR") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                        SelectOnFocusTextField(
                            value = if (item.quantity > 0) Helper.formatDouble(item.quantity, false) else "",
                            onValueChange = { input ->
                                val parsed = input.toDoubleOrNull() ?: 0.0
                                onUpdate(item.copy(quantity = parsed, unit = item.unit.ifEmpty { "قطعه" }))
                            },
                            label = { Text(if (isRtl) "تعداد / مقدار" else "Quantity") },
                            supportingText = {
                                if (item.quantity > 0) {
                                    Text(
                                        text = if (isRtl) "${Helper.formatDouble(item.quantity, false)} ${item.unit.ifEmpty { "قطعه" }}" else "${Helper.formatDouble(item.quantity, false)} ${item.unit.ifEmpty { "pcs" }}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("line_item_qty_input_$index"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                // Unit Price & Unit Picker Row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccordionPickerField(
                        value = item.unit.ifEmpty { if (isWood) "متر طول" else "قطعه" },
                        onValueChange = { newUnit ->
                            onUpdate(item.copy(unit = newUnit))
                        },
                        label = if (isRtl) "واحد اندازه‌گیری" else "Measurement Unit",
                        placeholder = "متر طول / قطعه...",
                        options = standardUnits,
                        isRtl = isRtl,
                        modifier = Modifier.weight(1f).testTag("line_item_unit_input_$index")
                    )
                    SelectOnFocusTextField(
                        value = Helper.formatWithCommas(item.unitPrice, false),
                        onValueChange = { input ->
                            val parsed = Helper.parseFormattedToDouble(input)
                            onUpdate(item.copy(unitPrice = parsed))
                        },
                        label = { Text(if (isRtl) "قیمت واحد (تومان)" else "Unit Price (IRT)") },
                        supportingText = {
                            if (item.unitPrice > 0) {
                                Text(
                                    text = Helper.formatCurrency(item.unitPrice, "تومان", isRtl),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.weight(1.3f).testTag("line_item_price_input_$index"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Prompt to save manual price change to Product Catalog / Warehouse
                val matchedDbProduct = remember(item.name, item.sku, allProducts) {
                    allProducts.find { 
                        (it.name.isNotBlank() && item.name.isNotBlank() && it.name.trim().equals(item.name.trim(), ignoreCase = true)) ||
                        (it.sku.isNotBlank() && item.sku.isNotBlank() && it.sku.trim().equals(item.sku.trim(), ignoreCase = true))
                    }
                }
                if (matchedDbProduct != null && item.unitPrice > 0 && matchedDbProduct.price != item.unitPrice && onSaveProductPriceToDb != null) {
                    var showPriceUpdatePrompt by remember(item.unitPrice, matchedDbProduct.id) { mutableStateOf(true) }
                    if (showPriceUpdatePrompt) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = if (isRtl) "تغییر قیمت دستی کالا" else "Manual Price Modification",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isRtl) "قیمت قبلی انبار: ${Helper.formatCurrency(matchedDbProduct.price, "تومان", true)}  ➜  قیمت جدید فاکتور: ${Helper.formatCurrency(item.unitPrice, "تومان", true)}\nآیا می‌خواهید قیمت جدید به عنوان قیمت ثابت کالا «${matchedDbProduct.name}» در انبار ذخیره شود؟"
                                           else "DB Price: ${Helper.formatCurrency(matchedDbProduct.price, "IRT", false)}  ➜  New Price: ${Helper.formatCurrency(item.unitPrice, "IRT", false)}\nSave as default product price in warehouse?",
                                    fontSize = 9.5.sp,
                                    lineHeight = 13.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { showPriceUpdatePrompt = false },
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text(if (isRtl) "خیر (انصراف)" else "No", fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            onSaveProductPriceToDb.invoke(matchedDbProduct.copy(price = item.unitPrice))
                                            showPriceUpdatePrompt = false
                                        },
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(if (isRtl) "بله، ثبت در انبار" else "Yes, Save in Warehouse", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val lineGross = if (isInstallation) {
                val qty = if (item.quantity > 0) item.quantity else (if (item.branchCount > 0) item.branchCount else 1.0)
                (qty * item.unitPrice) + item.accommodationCost + item.transportationCost + item.consumablesCost
            } else {
                item.quantity * item.unitPrice
            }
            val lineDisc = if (item.discountAmount > 0) item.discountAmount else lineGross * (item.discountPercent / 100.0)
            val lineNet = (lineGross - lineDisc).coerceAtLeast(0.0)
            val lineTax = if (item.taxPercent > 0.0) lineNet * (item.taxPercent / 100.0) else 0.0
            val lineFinal = lineNet + lineTax

            // Row 1: Discount & Line Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectOnFocusTextField(
                    value = if (item.discountPercent > 0) Helper.formatDouble(item.discountPercent, false) else "",
                    onValueChange = { onUpdate(item.copy(discountPercent = it.toDoubleOrNull() ?: 0.0)) },
                    textStyle = TextStyle(fontSize = 11.sp),
                    label = { Text(if (isRtl) "تخفیف ردیف٪" else "Disc %", fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("0%", fontSize = 9.5.sp) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Column(
                    modifier = Modifier
                        .weight(1.4f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (isRtl) "مبلغ کل ردیف:" else "Row Total:",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Helper.formatCurrency(lineFinal, "تومان", isRtl),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (lineTax > 0) {
                        Text(
                            text = if (isRtl) "+ ${Helper.formatCurrency(lineTax, "تومان", isRtl)} مالیات ردیف" else "+ ${Helper.formatCurrency(lineTax, "IRT", isRtl)} Item Tax",
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Row 2: Dedicated Line-Item Tax Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = item.taxPercent > 0.0,
                        onClick = {
                            val newTax = if (item.taxPercent > 0.0) 0.0 else 10.0
                            onUpdate(item.copy(taxPercent = newTax))
                        },
                        label = {
                            Text(
                                text = if (item.taxPercent > 0.0) {
                                    if (isRtl) "مالیات این ردیف (${Helper.formatDouble(item.taxPercent, false)}٪)" else "Line Tax (${Helper.formatDouble(item.taxPercent, false)}%)"
                                } else {
                                    if (isRtl) "بدون مالیات ردیف" else "No Line Tax"
                                },
                                fontSize = 9.5.sp,
                                fontWeight = if (item.taxPercent > 0.0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.height(34.dp)
                    )

                    if (item.taxPercent > 0.0) {
                        SelectOnFocusTextField(
                            value = Helper.formatDouble(item.taxPercent, false),
                            onValueChange = { onUpdate(item.copy(taxPercent = it.toDoubleOrNull() ?: 0.0)) },
                            textStyle = TextStyle(fontSize = 11.sp),
                            label = { Text(if (isRtl) "درصد مالیات" else "Tax %", fontSize = 9.sp) },
                            modifier = Modifier.width(90.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                if (lineTax > 0) {
                    Text(
                        text = if (isRtl) "مالیات ردیف: ${Helper.formatCurrency(lineTax, "تومان", isRtl)}" else "Tax: ${Helper.formatCurrency(lineTax, "IRT", isRtl)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Normal,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = fontSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = fontSize, fontWeight = fontWeight, color = color)
    }
}
