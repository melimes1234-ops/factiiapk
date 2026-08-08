package com.example.ui.screens

import android.content.Intent
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Invoice
import com.example.data.model.Customer
import com.example.data.repository.InvoiceDetails
import com.example.ui.InvoiceViewModel
import com.example.util.Helper
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicePreviewScreen(
    invoiceId: Long,
    viewModel: InvoiceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToAllInvoices: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val detailsFlow = remember(invoiceId) { viewModel.getInvoiceDetails(invoiceId) }
    val details by detailsFlow.collectAsStateWithLifecycle(initialValue = null)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isRtl = viewModel.selectedLanguage == "fa"
    val context = LocalContext.current

    val d = details
    val subtotal = d?.lineItems?.sumOf { it.quantity * it.unitPrice } ?: 0.0
    val discountVal = if (d != null) (subtotal * (d.invoice.discountRate / 100.0) + d.invoice.discountAmount) else 0.0
    val taxVal = if (d != null) {
        if (d.invoice.taxRate > 0.0) {
            (subtotal - discountVal) * (d.invoice.taxRate / 100.0)
        } else {
            d.lineItems.sumOf { item ->
                val gross = item.quantity * item.unitPrice
                val itemDisc = if (item.discountAmount > 0) item.discountAmount else gross * (item.discountPercent / 100.0)
                val itemNet = (gross - itemDisc).coerceAtLeast(0.0)
                itemNet * (item.taxPercent / 100.0)
            }
        }
    } else 0.0
    val total = if (d != null) (subtotal - discountVal + taxVal + d.invoice.shipping + d.invoice.handling) else 0.0

    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val bankAccount = remember(d?.invoice?.bankAccountId, bankAccounts) {
        bankAccounts.find { it.id == d?.invoice?.bankAccountId }
            ?: bankAccounts.find { it.isDefault }
            ?: bankAccounts.firstOrNull()
    }
    var selectedTemplate by remember { mutableStateOf("Nest") } // Nest, Corporate, Modern, Minimal, etc.
    
    // Auto sync preview selected template with saved invoice template (default to Nest)
    LaunchedEffect(details?.invoice?.template) {
        details?.invoice?.template?.let { tmpl ->
            if (tmpl.isNotEmpty() && tmpl != "General") {
                selectedTemplate = tmpl
            } else {
                selectedTemplate = "Nest"
            }
        }
    }
    
    // Appearance Settings State
    var customAccentColor by remember { mutableStateOf<String?>(null) }
    var baseFontSize by remember { mutableStateOf(11) }
    var marginSize by remember { mutableStateOf(8) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }

    val accentColor = Color(0xFF1A73E8)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = details?.invoice?.invoiceNumber ?: (if (isRtl) "پیش‌نمایش فاکتور" else "Invoice Detail"),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                    }
                },
                actions = {
                    // Minimal Pastel Edit Button
                    FilledTonalButton(
                        onClick = { details?.invoice?.id?.let { onNavigateToEdit(it) } },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp).testTag("preview_edit_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isRtl) "ویرایش" else "Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Direct Navigation Button to All Invoices
                    if (onNavigateToAllInvoices != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalButton(
                            onClick = { onNavigateToAllInvoices() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp).testTag("preview_all_invoices_button")
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isRtl) "همه فاکتورها" else "All Invoices", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Share Button
                    IconButton(onClick = {
                        val shareText = "فاکتور شماره ${details?.invoice?.invoiceNumber ?: ""}\n" +
                                "خریدار: ${details?.customer?.name ?: ""}\n" +
                                "مبلغ کل: ${Helper.formatCurrency(total, viewModel.selectedCurrency, viewModel.usePersianDigits)}"
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (details == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val d = details!!
            val items = d.lineItems

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                // --- Management Action Panel ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // First Row: Template Select
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Template Select Chip Button
                            OutlinedButton(
                                onClick = { showTemplateDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (selectedTemplate) {
                                        "Nest" -> if (isRtl) "پیش‌فاکتور نست" else "Nest Pre-Invoice"
                                        else -> if (isRtl) "قالب عمومی / استاندارد" else "Standard Invoice"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            // Appearance Settings Button
                            IconButton(
                                onClick = { showAppearanceDialog = true },
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Appearance Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Second Row: Clear Labeled Export Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Print / PDF Button
                            Button(
                                onClick = {
                                    printInvoiceHtml(
                                        context = context,
                                        details = d,
                                        settings = settings,
                                        currency = viewModel.selectedCurrency,
                                        usePersianDigits = viewModel.usePersianDigits,
                                        isRtl = isRtl,
                                        templateName = selectedTemplate,
                                        customAccentColor = customAccentColor,
                                        baseFontSize = baseFontSize,
                                        marginSize = marginSize,
                                        bankAccount = bankAccount
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(36.dp).testTag("print_pdf_button")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isRtl) "خروجی PDF / چاپ" else "Print / PDF",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            // PNG Image Button
                            Button(
                                onClick = {
                                    shareInvoiceAsImage(
                                        context = context,
                                        details = d,
                                        settings = settings,
                                        currency = viewModel.selectedCurrency,
                                        usePersianDigits = viewModel.usePersianDigits,
                                        isRtl = isRtl,
                                        templateName = selectedTemplate,
                                        customAccentColor = customAccentColor,
                                        baseFontSize = baseFontSize,
                                        marginSize = marginSize,
                                        bankAccount = bankAccount
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(36.dp).testTag("share_png_button")
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isRtl) "تصویر PNG" else "PNG Image",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            // Excel CSV Button
                            Button(
                                onClick = {
                                    com.example.util.CsvImportExportUtil.exportInvoiceToCsv(context, d)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(36.dp).testTag("export_excel_button")
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isRtl) "اکسل CSV" else "Excel CSV",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                // --- Scrollable A4 Container ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        val htmlContent = remember(d, settings, viewModel.selectedCurrency, viewModel.usePersianDigits, isRtl, selectedTemplate, customAccentColor, baseFontSize, marginSize, bankAccount) {
                            getInvoiceHtmlContent(
                                details = d,
                                settings = settings,
                                currency = viewModel.selectedCurrency,
                                usePersianDigits = viewModel.usePersianDigits,
                                isRtl = isRtl,
                                templateName = selectedTemplate,
                                customAccentColor = customAccentColor,
                                baseFontSize = baseFontSize,
                                marginSize = marginSize,
                                bankAccount = bankAccount
                            )
                        }
                        
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                android.webkit.WebView(ctx).apply {
                                    this.settings.javaScriptEnabled = true
                                    this.settings.builtInZoomControls = true
                                    this.settings.displayZoomControls = false
                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // --- Template Selection Dialog ---
    if (showTemplateDialog) {
        Dialog(onDismissRequest = { showTemplateDialog = false }) {
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
                        text = if (isRtl) "انتخاب قالب فاکتور" else "Select Invoice Template",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    val templatesList = listOf(
                        "Corporate" to Pair(if (isRtl) "قالب عمومی / استاندارد" else "Standard Invoice", if (isRtl) "طراحی کلاسیک، تمیز و رسمی برای تمامی فاکتورها" else "Clean standard invoice layout"),
                        "Nest" to Pair(if (isRtl) "پیش‌فاکتور نست (Nest WPC / Fika)" else "Nest Pre-Invoice", if (isRtl) "فرمت کاتالوگی، جدول‌بندی شده و شرکتی Nest" else "Official Nest catalog pre-invoice layout")
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        itemsIndexed(templatesList) { _, (key, info) ->
                            val isSelected = selectedTemplate == key
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedTemplate = key
                                        showTemplateDialog = false
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = info.first,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = info.second,
                                            fontSize = 10.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { showTemplateDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (isRtl) "بستن" else "Close")
                    }
                }
            }
        }
    }

    // --- Appearance Settings Dialog ---
    if (showAppearanceDialog) {
        Dialog(onDismissRequest = { showAppearanceDialog = false }) {
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
                        text = if (isRtl) "تنظیمات ظاهر فاکتور" else "Appearance Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Accent Color Selection
                    Text(text = if (isRtl) "رنگ اصلی (Accent Color)" else "Accent Color", fontSize = 12.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val colors = listOf(null, "#1E3A8A", "#4F46E5", "#0D9488", "#D97706", "#DC2626", "#111827")
                        colors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (hex == null) Color.LightGray else Color(android.graphics.Color.parseColor(hex)),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .border(
                                        width = if (customAccentColor == hex) 3.dp else 1.dp,
                                        color = if (customAccentColor == hex) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable { customAccentColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hex == null) {
                                    Text("Aa", fontSize = 10.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Font Size
                    Text(text = if (isRtl) "اندازه فونت" else "Font Size", fontSize = 12.sp, color = Color.Gray)
                    androidx.compose.material3.Slider(
                        value = baseFontSize.toFloat(),
                        onValueChange = { baseFontSize = it.toInt() },
                        valueRange = 10f..16f,
                        steps = 5
                    )
                    Text(text = "$baseFontSize pt", fontSize = 10.sp, modifier = Modifier.align(Alignment.End))

                    Spacer(modifier = Modifier.height(8.dp))

                    // Margins
                    Text(text = if (isRtl) "حاشیه کاغذ (Margins)" else "Paper Margins", fontSize = 12.sp, color = Color.Gray)
                    androidx.compose.material3.Slider(
                        value = marginSize.toFloat(),
                        onValueChange = { marginSize = it.toInt() },
                        valueRange = 8f..32f,
                        steps = 5
                    )
                    Text(text = "$marginSize mm", fontSize = 10.sp, modifier = Modifier.align(Alignment.End))

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showAppearanceDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isRtl) "بستن" else "Close")
                    }
                }
            }
        }
    }
}


fun getInvoiceHtmlContent(
    details: InvoiceDetails,
    settings: com.example.data.model.AppSettings?,
    currency: String,
    usePersianDigits: Boolean,
    isRtl: Boolean,
    templateName: String,
    customAccentColor: String? = null,
    baseFontSize: Int = 12,
    marginSize: Int = 16,
    bankAccount: com.example.data.model.BankAccount? = null
): String {
    val s = settings ?: com.example.data.model.AppSettings()
    val invoice = details.invoice
    val customer = details.customer
    val items = details.lineItems

    val subtotal = items.sumOf { it.quantity * it.unitPrice }
    val discountVal = subtotal * (invoice.discountRate / 100.0) + invoice.discountAmount
    val taxVal = if (invoice.taxRate > 0.0) {
        (subtotal - discountVal) * (invoice.taxRate / 100.0)
    } else {
        items.sumOf { item ->
            val gross = item.quantity * item.unitPrice
            val itemDisc = if (item.discountAmount > 0) item.discountAmount else gross * (item.discountPercent / 100.0)
            val itemNet = (gross - itemDisc).coerceAtLeast(0.0)
            itemNet * (item.taxPercent / 100.0)
        }
    }
    val total = subtotal - discountVal + taxVal + invoice.shipping + invoice.handling

    if (templateName == "Nest") {
        return getNestInvoiceHtmlContent(details, settings, currency, usePersianDigits, isRtl, marginSize, bankAccount)
    }

    // Let's define the color palette based on templateName
    val defaultPrimaryColor = when (templateName) {
        "Corporate" -> "#1E3A8A" // Royal Blue
        "Modern" -> "#4F46E5" // Indigo (Tailwind style)
        "Minimal" -> "#111827" // Charcoal/Black
        "Classic" -> "#0D9488" // Teal
        "Professional" -> "#D97706" // Gold/Amber
        "Luxury" -> "#9CA3AF" // Silver / Platinum
        else -> "#1E3A8A"
    }
    
    val primaryColor = customAccentColor ?: defaultPrimaryColor

    val secondaryColor = when (templateName) {
        "Corporate" -> "#3B82F6"
        "Modern" -> "#6366F1"
        "Minimal" -> "#4B5563"
        "Classic" -> "#0F766E"
        "Professional" -> "#B45309"
        "Luxury" -> "#D1D5DB"
        else -> "#3B82F6"
    }

    val isMinimal = templateName == "Minimal"
    val isClassic = templateName == "Classic"
    val isProfessional = templateName == "Professional"
    val isModern = templateName == "Modern"
    val isLuxury = templateName == "Luxury"
    val isCorporate = templateName == "Corporate" || (!isMinimal && !isClassic && !isProfessional && !isModern && !isLuxury)

    val statusLabel = when (invoice.status) {
        "Paid" -> if (isRtl) "پرداخت شده" else "PAID"
        "Pending" -> if (isRtl) "در انتظار پرداخت" else "PENDING"
        "PartiallyPaid" -> if (isRtl) "پرداخت جزئی" else "PARTIALLY PAID"
        "Overdue" -> if (isRtl) "سررسید گذشته" else "OVERDUE"
        "Draft" -> if (isRtl) "پیش‌نویس" else "DRAFT"
        else -> invoice.status
    }
    val statusClass = when (invoice.status) {
        "Paid" -> "status-paid"
        "Pending" -> "status-pending"
        "PartiallyPaid" -> "status-partially-paid"
        "Overdue" -> "status-overdue"
        "Draft" -> "status-draft"
        else -> "status-draft"
    }

    // Construct the metadata rows conditionally
    val invoiceNumLabel = if (isRtl) "شماره فاکتور" else "Invoice Number"
    val invoiceNumVal = Helper.toPersianDigits(invoice.invoiceNumber)

    val issueDateLabel = if (isRtl) "تاریخ صدور" else "Issue Date"
    val issueDateVal = Helper.formatJalaliShort(invoice.issueDate, usePersianDigits)

    val refNoLabel = if (isRtl) "کد ارجاع / پیگیری" else "Reference No"
    val refNoVal = if (invoice.referenceNo.isNotEmpty()) invoice.referenceNo else ""

    val dueDateLabel = if (isRtl) "تاریخ سررسید" else "Due Date"
    val dueDateVal = Helper.formatJalaliShort(invoice.dueDate, usePersianDigits)

    // Build Metadata Block (Only show rows with non-empty values!)
    val metaRowsHtml = buildString {
        val activeMetadata = mutableListOf<Pair<String, String>>()
        activeMetadata.add((if (isRtl) "شماره فاکتور" else "Invoice No") to invoiceNumVal)
        activeMetadata.add((if (isRtl) "تاریخ صدور" else "Issue Date") to issueDateVal)
        
        if (invoice.dueDate > 0) {
            activeMetadata.add((if (isRtl) "تاریخ سررسید" else "Due Date") to dueDateVal)
        }
        if (refNoVal.isNotEmpty()) {
            activeMetadata.add((if (isRtl) "کد ارجاع / پیگیری" else "Reference No") to Helper.toPersianDigits(refNoVal))
        }
        if (invoice.poNumber.isNotEmpty()) {
            activeMetadata.add((if (isRtl) "شماره سفارش (PO)" else "PO Number") to Helper.toPersianDigits(invoice.poNumber))
        }
        if (invoice.projectNumber.isNotEmpty()) {
            activeMetadata.add((if (isRtl) "کد / شماره پروژه" else "Project Code") to Helper.toPersianDigits(invoice.projectNumber))
        }
        if (invoice.salesperson.isNotEmpty()) {
            val staffText = if (invoice.supportPerson.isNotEmpty() && invoice.supportPerson != invoice.salesperson) {
                "${invoice.salesperson} / ${invoice.supportPerson}"
            } else {
                invoice.salesperson
            }
            activeMetadata.add((if (isRtl) "مسئول فروش / کارشناس" else "Sales & Support Staff") to staffText)
        } else if (invoice.supportPerson.isNotEmpty()) {
            activeMetadata.add((if (isRtl) "مسئول / کارشناس پشتیبانی" else "Support Staff") to invoice.supportPerson)
        }

        activeMetadata.chunked(2).forEach { rowItems ->
            append("<tr>")
            rowItems.forEach { (label, value) ->
                val isNum = label.contains("شماره") || label.contains("No") || label.contains("کد")
                val isDue = label.contains("سررسید") || label.contains("Due")
                val style = if (isNum) "font-weight: 700;" else if (isDue) "color: #EF4444; font-weight: 600;" else ""
                append("<td class=\"meta-label\">$label</td>")
                append("<td class=\"meta-value\" style=\"$style\">$value</td>")
            }
            if (rowItems.size < 2) {
                append("<td class=\"meta-label\"></td><td class=\"meta-value\"></td>")
            }
            append("</tr>")
        }
    }

    // Build Seller Info (Only show non-empty fields!)
    val sellerInfoHtml = buildString {
        append("<strong>${s.companyName}</strong><br>")
        if (s.companyAddress.isNotEmpty()) {
            append("${if (isRtl) "نشانی:" else "Address:"} ${s.companyAddress}<br>")
        }
        if (s.companyPhone.isNotEmpty()) {
            append("${if (isRtl) "تلفن تماس:" else "Phone:"} ${Helper.toPersianDigits(s.companyPhone)}<br>")
        }
        if (s.companyTaxId.isNotEmpty()) {
            append("<strong>${if (isRtl) "کد اقتصادی:" else "Economic Code:"}</strong> ${Helper.toPersianDigits(s.companyTaxId)}<br>")
        }
        if (s.companyNationalId.isNotEmpty()) {
            append("<strong>${if (isRtl) "شناسه ملی:" else "National ID:"}</strong> ${Helper.toPersianDigits(s.companyNationalId)}<br>")
        }
        if (s.companyVatNumber.isNotEmpty()) {
            append("<strong>${if (isRtl) "شماره ثبت:" else "Reg No:"}</strong> ${Helper.toPersianDigits(s.companyVatNumber)}<br>")
        }
        if (s.companyPostalCode.isNotEmpty()) {
            append("<strong>${if (isRtl) "کد پستی:" else "Postal Code:"}</strong> ${Helper.toPersianDigits(s.companyPostalCode)}<br>")
        }
    }

    // Build Buyer Info (Only show non-empty fields!)
    val buyerInfoHtml = buildString {
        append("<strong>${customer?.name ?: (if (isRtl) "مشتری عمومی" else "Walk-in Customer")}</strong><br>")
        if (customer?.company?.isNotEmpty() == true) {
            append("<strong>${if (isRtl) "نام شرکت:" else "Company:"}</strong> ${customer.company}<br>")
        }
        if (customer?.billingAddress?.isNotEmpty() == true) {
            append("${if (isRtl) "نشانی:" else "Address:"} ${customer.billingAddress}<br>")
        }
        if (customer?.phone?.isNotEmpty() == true) {
            append("${if (isRtl) "تلفن تماس:" else "Phone:"} ${Helper.toPersianDigits(customer.phone)}<br>")
        }
        if (customer?.taxId?.isNotEmpty() == true) {
            append("<strong>${if (isRtl) "کد اقتصادی / ملی:" else "Tax / National ID:"}</strong> ${Helper.toPersianDigits(customer.taxId)}<br>")
        }
    }

    val invoiceTitleHeader = if (invoice.invoiceType.isNotEmpty()) invoice.invoiceType else (if (isRtl) "فاکتور فروش" else "INVOICE")

    return buildString {
        append("""
        <!DOCTYPE html>
        <html dir="${if (isRtl) "rtl" else "ltr"}">
        <head>
            <meta charset="utf-8">
            <title>${getFormattedInvoiceFileName(details)}</title>
            <style>
                @import url('https://fonts.googleapis.com/css2?family=Vazirmatn:wght@300;400;500;700;800&display=swap');
                
                body {
                    font-family: 'Vazirmatn', 'Tahoma', 'Arial', sans-serif;
                    margin: 0;
                    padding: ${marginSize}mm;
                    color: #1F2937;
                    background-color: #ffffff;
                    font-size: ${baseFontSize}px;
                    line-height: 1.6;
                }
                
                /* Invoice Container Style */
                .invoice-container {
                    max-width: 800px;
                    margin: 0 auto;
                    position: relative;
                    padding: ${marginSize}mm;
                    background: #ffffff;
                    ${if (isModern) """
                        border: 1px solid #E2E8F0;
                        box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05), 0 4px 6px -4px rgba(0, 0, 0, 0.05);
                        border-radius: 16px;
                        background: #ffffff;
                    """ else if (isLuxury) """
                        border: 2px solid $primaryColor;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
                        border-radius: 4px;
                        background: linear-gradient(135deg, #ffffff 0%, #FAFAFA 100%);
                    """ else if (isProfessional) """
                        border: 2px solid #D97706;
                        box-shadow: 0 4px 20px rgba(217, 119, 6, 0.1);
                        border-radius: 12px;
                    """ else if (isClassic) """
                        border: 6px double #0D9488;
                        padding: 35px;
                    """ else if (isMinimal) """
                        border: 1px solid #E5E7EB;
                        border-radius: 4px;
                    """ else """
                        border: 1px solid #E5E7EB;
                        border-top: 8px solid #1E3A8A;
                        border-radius: 8px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.03);
                    """}
                }
                
                /* Header layout */
                .header-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 30px;
                    ${if (isMinimal) "border-bottom: 2px solid #F3F4F6; padding-bottom: 20px;" else ""}
                }
                .header-table td {
                    vertical-align: top;
                }
                
                .invoice-title {
                    font-size: ${if (isMinimal) "22px" else "26px"};
                    font-weight: 800;
                    color: $primaryColor;
                    margin: 0 0 4px 0;
                    text-transform: uppercase;
                    letter-spacing: 0.8px;
                    ${if (isClassic) "text-align: center; font-family: 'Times New Roman', serif; border-bottom: 2px double $primaryColor; padding-bottom: 8px; margin-bottom: 12px;" else ""}
                }
                
                .company-name {
                    font-size: 20px;
                    font-weight: 800;
                    color: ${if (isProfessional) "#D97706" else "#111827"};
                    margin: 0 0 6px 0;
                    letter-spacing: -0.3px;
                }
                .company-info {
                    font-size: 11px;
                    color: #4B5563;
                    line-height: 1.6;
                }
                
                /* Status Badges */
                .status-badge {
                    display: inline-block;
                    padding: 5px 12px;
                    border-radius: 20px;
                    font-size: 10px;
                    font-weight: 700;
                    margin-top: 8px;
                    letter-spacing: 0.5px;
                }
                .status-paid {
                    background-color: #D1FAE5;
                    color: #065F46;
                    border: 1px solid #A7F3D0;
                }
                .status-pending {
                    background-color: #FEF3C7;
                    color: #92400E;
                    border: 1px solid #FDE68A;
                }
                .status-partially-paid {
                    background-color: #E0F2FE;
                    color: #0369A1;
                    border: 1px solid #BAE6FD;
                }
                .status-overdue {
                    background-color: #FEE2E2;
                    color: #991B1B;
                    border: 1px solid #FCA5A5;
                }
                .status-draft {
                    background-color: #F3F4F6;
                    color: #374151;
                    border: 1px solid #E5E7EB;
                }
                
                /* Meta Table System */
                .meta-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 25px;
                    ${if (isMinimal) "" else "border-radius: 8px; overflow: hidden;"}
                }
                .meta-table td {
                    border: 1px solid #E5E7EB;
                    padding: 10px 12px;
                    font-size: 11.5px;
                }
                .meta-label {
                    background-color: ${if (isMinimal) "#ffffff" else "#F9FAFB"};
                    font-weight: 700;
                    color: #4B5563;
                    width: 18%;
                    ${if (isMinimal) "border: none; border-bottom: 1px solid #F3F4F6; font-weight: 500;" else ""}
                }
                .meta-value {
                    color: #111827;
                    width: 32%;
                    font-weight: 600;
                    ${if (isMinimal) "border: none; border-bottom: 1px solid #F3F4F6;" else ""}
                }
                
                /* Parties details block */
                .parties-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 30px;
                }
                .parties-table td {
                    width: 50%;
                    border: ${if (isMinimal) "none" else "1px solid #E5E7EB"};
                    padding: 16px;
                    vertical-align: top;
                    ${if (isMinimal) "background-color: #FAFAFA; border-radius: 6px;" else if (isModern) "background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px;" else if (isProfessional) "background-color: #FDFBF7; border: 1px solid #FEF3C7; border-radius: 8px;" else "background-color: #F9FAFB; border-radius: 8px;"}
                }
                .party-title {
                    font-weight: 800;
                    font-size: 12px;
                    color: $primaryColor;
                    border-bottom: 2px solid ${if (isMinimal) "#E5E7EB" else primaryColor};
                    padding-bottom: 6px;
                    margin-bottom: 10px;
                    text-transform: uppercase;
                }
                .party-details {
                    font-size: 11px;
                    color: #374151;
                    line-height: 1.7;
                }
                
                /* Main Line Items Table */
                .items-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 30px;
                }
                .items-table th {
                    background-color: $primaryColor;
                    color: #ffffff;
                    font-weight: 700;
                    padding: 12px 14px;
                    font-size: 11px;
                    text-transform: uppercase;
                    border: 1px solid $primaryColor;
                }
                .items-table td {
                    border: 1px solid #E5E7EB;
                    padding: 12px 14px;
                    text-align: center;
                    color: #1F2937;
                    font-size: 11.5px;
                }
                .items-table tr:nth-child(even) {
                    background-color: ${if (isCorporate) "#F0F9FF" else if (isModern) "#F8FAFC" else if (isProfessional) "#FDFBF7" else "#F9FAFB"};
                }
                
                /* Summary Area calculations */
                .summary-container {
                    width: 100%;
                    margin-top: 20px;
                }
                .summary-table {
                    width: 300px;
                    margin-right: ${if (isRtl) "auto" else "0"};
                    margin-left: ${if (isRtl) "0" else "auto"};
                    border-collapse: collapse;
                }
                .summary-table td {
                    padding: 10px 14px;
                    border: 1px solid #E5E7EB;
                    font-size: 11.5px;
                }
                .summary-label {
                    background-color: #F9FAFB;
                    font-weight: 700;
                    color: #4B5563;
                }
                .summary-value {
                    text-align: right;
                    font-weight: 800;
                    color: #111827;
                }
                .total-row {
                    background-color: ${if (isModern) "#EEF2FF" else if (isProfessional) "#FFFBEB" else "#EFF6FF"} !important;
                }
                .total-row td {
                    border: 1.5px solid $primaryColor !important;
                    color: $primaryColor !important;
                    font-size: 13px !important;
                    font-weight: 800 !important;
                }
                .due-row {
                    background-color: #FEF2F2 !important;
                }
                .due-row td {
                    border: 1.5px solid #EF4444 !important;
                    color: #EF4444 !important;
                    font-weight: 800 !important;
                }
                
                /* Ornate and signature blocks */
                .signatures-table {
                    width: 100%;
                    margin-top: 50px;
                    border-collapse: collapse;
                }
                .signatures-table td {
                    width: 50%;
                    text-align: center;
                    vertical-align: top;
                }
                .signature-title {
                    font-weight: 700;
                    font-size: 11.5px;
                    color: #374151;
                    margin-bottom: 60px;
                }
                .signature-line {
                    border-top: 1px dashed #9CA3AF;
                    width: 180px;
                    margin: 0 auto;
                }
                .footer {
                    margin-top: 50px;
                    padding-top: 20px;
                    border-top: 1px solid #E5E7EB;
                    text-align: center;
                    font-size: 10.5px;
                    color: #6B7280;
                    line-height: 1.6;
                }
                .notes-box {
                    background-color: #FAFAFA;
                    border-right: ${if (isRtl) "4px solid $primaryColor" else "none"};
                    border-left: ${if (isRtl) "none" else "4px solid $primaryColor"};
                    padding: 12px 18px;
                    margin-top: 25px;
                    border-radius: 4px;
                    font-size: 11px;
                    text-align: ${if (isRtl) "right" else "left"};
                    line-height: 1.6;
                    color: #4B5563;
                }

                @media print {
                    @page {
                        size: A4 portrait;
                        margin: 6mm 8mm 6mm 8mm !important;
                    }
                    html, body {
                        width: 100% !important;
                        height: auto !important;
                        overflow: visible !important;
                        padding: 0 !important;
                        margin: 0 !important;
                        background: #ffffff !important;
                        color: #000000 !important;
                        font-size: 10px !important;
                        -webkit-print-color-adjust: exact !important;
                        print-color-adjust: exact !important;
                    }
                    .invoice-container {
                        width: 100% !important;
                        max-width: 100% !important;
                        padding: 0 !important;
                        margin: 0 !important;
                        border: none !important;
                        box-shadow: none !important;
                    }
                    .header-table {
                        margin-bottom: 6px !important;
                    }
                    .invoice-title {
                        font-size: 16px !important;
                    }
                    .company-name {
                        font-size: 14px !important;
                    }
                    .meta-table {
                        margin-bottom: 6px !important;
                    }
                    .meta-table td {
                        padding: 3px 5px !important;
                        font-size: 9.5px !important;
                    }
                    .parties-table {
                        margin-bottom: 6px !important;
                    }
                    .parties-table td {
                        padding: 4px 6px !important;
                        font-size: 9.5px !important;
                    }
                    .party-title {
                        font-size: 10px !important;
                        margin-bottom: 3px !important;
                        padding: 2px 4px !important;
                    }
                    .party-details {
                        font-size: 9px !important;
                    }
                    .items-table {
                        margin-bottom: 6px !important;
                    }
                    .items-table th {
                        padding: 5px 6px !important;
                        font-size: 9px !important;
                    }
                    .items-table td {
                        padding: 5px 6px !important;
                        font-size: 9px !important;
                    }
                    .summary-container {
                        margin-top: 6px !important;
                    }
                    .summary-table td {
                        padding: 3px 6px !important;
                        font-size: 9px !important;
                    }
                    .notes-box {
                        margin-top: 6px !important;
                        padding: 4px 8px !important;
                        font-size: 9px !important;
                    }
                    .signatures-table {
                        margin-top: 15px !important;
                    }
                    .signature-title {
                        margin-bottom: 25px !important;
                        font-size: 9.5px !important;
                    }
                    .footer {
                        margin-top: 10px !important;
                        padding-top: 8px !important;
                        font-size: 8.5px !important;
                        line-height: 1.4 !important;
                    }
                }
            </style>
        </head>
        <body>
            <div class="invoice-container">
                <!-- Header -->
                <table class="header-table">
                    <tr>
                        <td style="text-align: ${if (isRtl) "right" else "left"};">
                            ${if (isClassic) """
                            <h1 class="invoice-title">$invoiceTitleHeader</h1>
                            <div style="text-align: center; font-size: 11px; color: #6B7280; margin-top: -10px; margin-bottom: 20px;">${if (isRtl) "بر اساس آئین‌نامه اجرایی قانون مالیات بر ارزش افزوده" else "Official Business Transaction Ledger"}</div>
                            <span class="status-badge $statusClass">$statusLabel</span>
                            """ else """
                            <h1 class="invoice-title">$invoiceTitleHeader</h1>
                            <span style="font-size: 11px; color: #6B7280;">${if (isRtl) "قانون مالیات بر ارزش افزوده" else "Official Business Transaction Ledger"}</span><br>
                            <span class="status-badge $statusClass">$statusLabel</span>
                            """}
                        </td>
                        <td style="text-align: ${if (isRtl) "left" else "right"};">
                            <h2 class="company-name">${s.companyName}</h2>
                            <div class="company-info">
                                ${if (s.companyWebsite.isNotEmpty()) "${s.companyWebsite}<br>" else ""}
                                ${if (s.companyEmail.isNotEmpty()) "${s.companyEmail}<br>" else ""}
                            </div>
                        </td>
                    </tr>
                </table>

                <!-- Metadata block -->
                <table class="meta-table">
                    $metaRowsHtml
                </table>

                <!-- Parties (Seller & Buyer) -->
                <table class="parties-table">
                    <tr>
                        <td style="padding-right: ${if (isRtl) "0" else "10px"}; padding-left: ${if (isRtl) "10px" else "0"};">
                            <div class="party-title">${if (isRtl) "مشخصات فروشنده (صادرکننده)" else "Seller Identity Details"}</div>
                            <div class="party-details">
                                $sellerInfoHtml
                            </div>
                        </td>
                        <td style="padding-left: ${if (isRtl) "0" else "10px"}; padding-right: ${if (isRtl) "10px" else "0"};">
                            <div class="party-title">${if (isRtl) "مشخصات خریدار (مشتری)" else "Buyer Identity Details"}</div>
                            <div class="party-details">
                                $buyerInfoHtml
                            </div>
                        </td>
                    </tr>
                </table>

                <!-- Line Items Table -->
                <table class="items-table">
                    <thead>
                        <tr>
                            <th style="width: 6%;">${if (isRtl) "ردیف" else "Row"}</th>
                            <th style="text-align: ${if (isRtl) "right" else "left"}; width: 44%;">${if (isRtl) "شرح کالا یا خدمات ارائه‌شده" else "Item / Service Description"}</th>
                            <th style="width: 10%;">${if (isRtl) "تعداد" else "Qty"}</th>
                            <th style="width: 18%; text-align: right;">${if (isRtl) "مبلغ واحد" else "Unit Price"}</th>
                            <th style="width: 22%; text-align: right;">${if (isRtl) "مبلغ کل" else "Total Sum"}</th>
                        </tr>
                    </thead>
                    <tbody>
        """)

        items.forEachIndexed { index, item ->
            val isWoodItem = item.categoryType == "Wood" || item.categoryType == "چوب پلاست" || item.unit == "متر طول" || item.unit.contains("متر")
            val isAccessoryItem = item.categoryType == "Accessory" || item.categoryType == "پیچ و کلیپس"
            val isInstallationItem = item.categoryType == "Installation" || item.categoryType == "نصب" || item.categoryType == "خدمات"
            
            val extraDetails = mutableListOf<String>()
            if (isInstallationItem) {
                if (item.executionDays > 0) extraDetails.add("مدت اجرا: ${Helper.formatDouble(item.executionDays.toDouble(), usePersianDigits)} روز")
                if (item.teamSize > 0) extraDetails.add("تعداد نفرات: ${Helper.formatDouble(item.teamSize.toDouble(), usePersianDigits)} نفر")
                if (item.accommodationCost > 0) extraDetails.add("اسکان: ${Helper.formatCurrency(item.accommodationCost, "", usePersianDigits)}")
                if (item.transportationCost > 0) extraDetails.add("ایاب و ذهاب: ${Helper.formatCurrency(item.transportationCost, "", usePersianDigits)}")
                if (item.consumablesCost > 0) extraDetails.add("اقلام مصرفی: ${Helper.formatCurrency(item.consumablesCost, "", usePersianDigits)}")
            }
            val extraSub = if (extraDetails.isNotEmpty()) "<br/><span style='font-size: 10px; color: #555; font-weight: normal;'>${extraDetails.joinToString(" | ")}</span>" else ""

            val qty = if (isWoodItem) kotlin.math.ceil(item.quantity) else item.quantity
            val rowTotal = qty * item.unitPrice
            append("""
                <tr>
                    <td>${Helper.toPersianDigits((index + 1).toString())}</td>
                    <td style="text-align: ${if (isRtl) "right" else "left"}; font-weight: 600;">${item.name}$extraSub</td>
                    <td>${Helper.formatDouble(qty, usePersianDigits)} ${item.unit}</td>
                    <td style="text-align: right;">${Helper.formatCurrency(item.unitPrice, "", usePersianDigits)}</td>
                    <td style="text-align: right; font-weight: 600;">${Helper.formatCurrency(rowTotal, "", usePersianDigits)}</td>
                </tr>
            """)
        }

        append("""
                    </tbody>
                </table>

                <!-- Summary calculations block -->
                <div class="summary-container">
                    <table class="summary-table">
                        <tr>
                            <td class="summary-label">${if (isRtl) "جمع کل خام:" else "Subtotal:"}</td>
                            <td class="summary-value" style="text-align: right;">${Helper.formatCurrency(subtotal, currency, usePersianDigits)}</td>
                        </tr>
        """)

        if (discountVal > 0) {
            append("""
                        <tr>
                            <td class="summary-label" style="color: #EF4444;">${if (isRtl) "تخفیف کل:" else "Discount:"}</td>
                            <td class="summary-value" style="text-align: right; color: #EF4444;">- ${Helper.formatCurrency(discountVal, currency, usePersianDigits)}</td>
                        </tr>
            """)
        }

        if (taxVal > 0) {
            append("""
                        <tr>
                            <td class="summary-label">${if (isRtl) "مالیات و عوارض (۹٪):" else "Tax (9%):"}</td>
                            <td class="summary-value" style="text-align: right;">+ ${Helper.formatCurrency(taxVal, currency, usePersianDigits)}</td>
                        </tr>
            """)
        }

        if (invoice.shipping > 0) {
            append("""
                        <tr>
                            <td class="summary-label">${if (isRtl) "هزینه حمل و نقل:" else "Shipping:"}</td>
                            <td class="summary-value" style="text-align: right;">${Helper.formatCurrency(invoice.shipping, currency, usePersianDigits)}</td>
                        </tr>
            """)
        }

        val advancePayment = invoice.advancePayment
        val netBalance = (total - advancePayment).coerceAtLeast(0.0)

        append("""
                        <tr class="total-row">
                            <td class="summary-label" style="font-weight: 800;">${if (isRtl) "مبلغ کل:" else "Grand Total:"}</td>
                            <td class="summary-value" style="text-align: right; font-weight: 800;">${Helper.formatCurrency(total, currency, usePersianDigits)}</td>
                        </tr>
        """)
        
        if (advancePayment > 0) {
            append("""
                        <tr>
                            <td class="summary-label" style="color: #10B981;">${if (isRtl) "مبلغ پرداخت شده:" else "Advance Payment:"}</td>
                            <td class="summary-value" style="text-align: right; color: #10B981;">- ${Helper.formatCurrency(advancePayment, currency, usePersianDigits)}</td>
                        </tr>
            """)
        }
        
        append("""
                        <tr class="total-row">
                            <td class="summary-label" style="font-weight: 800; color: #EF4444;">${if (isRtl) "مانده جهت تسویه:" else "Net Balance Due:"}</td>
                            <td class="summary-value" style="text-align: right; font-weight: 800; color: #EF4444;">${Helper.formatCurrency(netBalance, currency, usePersianDigits)}</td>
                        </tr>
                    </table>
                </div>

                <div class="notes-box" style="margin-top: 20px; border-top: 1px solid #E5E7EB; padding-top: 15px;">
                    <table style="width: 100%; border: none;">
                        <tr>
                            <td style="width: 50%; padding-right: 15px; vertical-align: top;">
                                <strong>${if (isRtl) "وضعیت پرداخت:" else "Payment Status:"}</strong> 
                                ${
                                    when (invoice.status) {
                                        "Paid" -> if (isRtl) "تسویه کامل" else "Paid"
                                        "PartiallyPaid" -> if (isRtl) "پرداخت بخشی از مبلغ" else "Partially Paid"
                                        else -> if (isRtl) "تسویه نشده (در انتظار پرداخت)" else "Pending"
                                    }
                                }
                                <br>
                                ${if (invoice.paymentMethod.isNotEmpty() && invoice.status != "Pending") "<strong>${if (isRtl) "روش پرداخت:" else "Payment Method:"}</strong> ${invoice.paymentMethod}<br>" else ""}
                                ${if (invoice.paymentDetails.isNotEmpty() && invoice.status != "Pending") "<strong>${if (isRtl) "مشخصات پرداخت:" else "Payment Details:"}</strong> ${invoice.paymentDetails}<br>" else ""}
                            </td>
                            <td style="width: 50%; vertical-align: top;">
                                ${if (invoice.notes.isNotEmpty()) """
                                    <strong>${if (isRtl) "توضیحات و شرایط تسویه:" else "Invoice Notes & Terms:"}</strong><br>
                                    ${invoice.notes.replace("\n", "<br>")}
                                """ else ""}
                            </td>
                        </tr>
                    </table>
                </div>


                <!-- Signatures -->
                <table class="signatures-table">
                    <tr>
                        <td>
                            <div class="signature-title">${if (isRtl) "مهر و امضای صادرکننده (فروشنده)" else "Issuer Stamp & Signature"}</div>
                            <div class="signature-line"></div>
                        </td>
                        <td>
                            <div class="signature-title">${if (isRtl) "مهر و امضای تحویل‌گیرنده (خریدار)" else "Customer Stamp & Signature"}</div>
                            <div class="signature-line"></div>
                        </td>
                    </tr>
                </table>

                <div class="footer">
                    ${if (isRtl) "این فاکتور بر اساس قوانین تجارت الکترونیک صادر گردیده و معتبر می‌باشد." else "This transaction statement is valid and issued under e-commerce standard guidelines."}<br>
                    <strong>${if (isRtl) "با تشکر از اعتماد و خرید شما" else "Thank you for doing business with us!"}</strong>
                </div>
            </div>
        </body>
        </html>
        """)
    }
}

fun getFormattedInvoiceFileName(details: InvoiceDetails): String {
    val invoice = details.invoice
    val mozo = if (invoice.invoiceType.isNotBlank()) {
        invoice.invoiceType.trim()
    } else {
        "فاکتور فروش"
    }
    val number = invoice.invoiceNumber.ifBlank { "0" }
    val dateStr = Helper.formatJalaliShort(invoice.issueDate, false).replace("/", "").replace("-", "")
    val rawCustomer = details.customer?.name?.trim()?.ifBlank { "مشتری" } ?: "مشتری"
    val cleanCustomer = rawCustomer.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    return "${mozo}-${number}-${dateStr}-${cleanCustomer}"
}

fun printInvoiceHtml(
    context: Context,
    details: InvoiceDetails,
    settings: com.example.data.model.AppSettings?,
    currency: String,
    usePersianDigits: Boolean,
    isRtl: Boolean,
    templateName: String = "Corporate",
    customAccentColor: String? = null,
    baseFontSize: Int = 12,
    marginSize: Int = 16,
    bankAccount: com.example.data.model.BankAccount? = null
) {
    val s = settings ?: com.example.data.model.AppSettings()
    val invoice = details.invoice
    val htmlContent = getInvoiceHtmlContent(details, settings, currency, usePersianDigits, isRtl, templateName, customAccentColor, baseFontSize, marginSize, bankAccount)

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = getFormattedInvoiceFileName(details)
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
}

fun shareInvoiceAsImage(
    context: Context,
    details: InvoiceDetails,
    settings: com.example.data.model.AppSettings?,
    currency: String,
    usePersianDigits: Boolean,
    isRtl: Boolean,
    templateName: String = "Corporate",
    customAccentColor: String? = null,
    baseFontSize: Int = 12,
    marginSize: Int = 16,
    bankAccount: com.example.data.model.BankAccount? = null
) {
    val s = settings ?: com.example.data.model.AppSettings()
    val invoice = details.invoice
    val htmlContent = getInvoiceHtmlContent(details, settings, currency, usePersianDigits, isRtl, templateName, customAccentColor, baseFontSize, marginSize, bankAccount)

    val activity = context as? android.app.Activity
    val rootLayout = activity?.findViewById<android.view.ViewGroup>(android.R.id.content)

    val targetWidth = 1240
    val targetHeight = 1754

    val webView = WebView(context)
    webView.visibility = android.view.View.INVISIBLE
    val params = android.view.ViewGroup.LayoutParams(targetWidth, targetHeight)
    rootLayout?.addView(webView, params)

    webView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
    webView.settings.apply {
        javaScriptEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        defaultTextEncodingName = "utf-8"
    }

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            webView.postDelayed({
                try {
                    webView.measure(
                        android.view.View.MeasureSpec.makeMeasureSpec(targetWidth, android.view.View.MeasureSpec.EXACTLY),
                        android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
                    )
                    val contentHeight = Math.max(targetHeight, webView.measuredHeight)
                    webView.layout(0, 0, targetWidth, contentHeight)

                    val bitmap = android.graphics.Bitmap.createBitmap(targetWidth, contentHeight, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    webView.draw(canvas)

                    val cachePath = java.io.File(context.cacheDir, "images")
                    cachePath.mkdirs()
                    val file = java.io.File(cachePath, "${getFormattedInvoiceFileName(details)}.png")
                    val stream = java.io.FileOutputStream(file)
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    stream.close()

                    val authority = "${context.packageName}.fileprovider"
                    val contentUri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

                    if (contentUri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, if (isRtl) "اشتراک‌گذاری تصویر فاکتور" else "Share Invoice"))
                        android.widget.Toast.makeText(context, if (isRtl) "تصویر A4 با کیفیت بالا تولید گردید" else "High quality A4 image prepared", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, if (isRtl) "خطا در تولید عکس فاکتور: ${e.localizedMessage}" else "Error rendering image: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                } finally {
                    rootLayout?.removeView(webView)
                }
            }, 800)
        }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
}

fun getNestInvoiceHtmlContent(
    details: com.example.data.repository.InvoiceDetails,
    settings: com.example.data.model.AppSettings?,
    currency: String,
    usePersianDigits: Boolean,
    isRtl: Boolean,
    marginSize: Int,
    bankAccount: com.example.data.model.BankAccount? = null
): String {
    val invoice = details.invoice
    val customer = details.customer
    val items = details.lineItems
    val s = settings ?: com.example.data.model.AppSettings()

    val df = java.text.DecimalFormat("#,###.##")
    fun formatNum(num: Double): String {
        if (num == 0.0) return "۰"
        var str = df.format(num)
        if (usePersianDigits) {
            val p = arrayOf("۰","۱","۲","۳","۴","۵","۶","۷","۸","۹")
            for (i in 0..9) str = str.replace(i.toString(), p[i])
        }
        return str
    }

    val toRial = 10.0 // Toman to Rial conversion

    // Group items into Wood (چوب پلاست), Accessories (پیچ و کلیپس), Installation (نصب), and Cabinet (کابینت)
    val woodItems = items.filter { (it.categoryType == "Wood" || it.categoryType == "چوب پلاست" || it.categoryType.isEmpty()) && it.categoryType != "Cabinet" && it.categoryType != "کابینت" }
    val accessoryItems = items.filter { it.categoryType == "Accessory" || it.categoryType == "پیچ و کلیپس" }
    val installationItems = items.filter { it.categoryType == "Installation" || it.categoryType == "نصب" }
    val cabinetItems = items.filter { it.categoryType == "Cabinet" || it.categoryType == "کابینت" }
    val isInstInvoice = invoice.invoiceType.contains("نصب") || invoice.invoiceType.contains("اجرا") || (installationItems.isNotEmpty() && woodItems.isEmpty() && accessoryItems.isEmpty() && cabinetItems.isEmpty())

    val hasTax = invoice.taxRate > 0.0
    val taxRateFactor = if (hasTax) 0.07 else 0.0
    val vatRateFactor = if (hasTax) 0.03 else 0.0

    // Wood Items Calculations
    var totalWoodBranch = 0.0
    var totalWoodLength = 0.0
    var totalWoodPriceRial = 0.0
    var totalWoodPayableRial = 0.0
    var totalWoodTaxRial = 0.0
    var totalWoodVatRial = 0.0
    var totalWoodGrandRial = 0.0

    var woodTrs = ""
    for ((idx, item) in woodItems.withIndex()) {
        val branch = item.branchCount
        val qty = kotlin.math.ceil(item.quantity)
        val unitPriceRial = item.unitPrice * toRial
        val totalRial = qty * unitPriceRial
        val discPercent = item.discountPercent
        val payableRial = totalRial * (1.0 - discPercent / 100.0)
        val itemTaxRateFactor = if (hasTax) 0.07 else (item.taxPercent * 0.7 / 100.0)
        val itemVatRateFactor = if (hasTax) 0.03 else (item.taxPercent * 0.3 / 100.0)
        val taxRial = payableRial * itemTaxRateFactor
        val vatRial = payableRial * itemVatRateFactor
        val grandRial = payableRial + taxRial + vatRial

        totalWoodBranch += branch
        totalWoodLength += qty
        totalWoodPriceRial += totalRial
        totalWoodPayableRial += payableRial
        totalWoodTaxRial += taxRial
        totalWoodVatRial += vatRial
        totalWoodGrandRial += grandRial

        woodTrs += """
            <tr>
                <td class="text-center">${formatNum((idx + 1).toDouble())}</td>
                <td class="bold text-center">${item.name}</td>
                <td class="text-center">${if (item.sku.isNotEmpty()) item.sku else "---"}</td>
                <td class="text-center">${if (item.colorCode.isNotEmpty()) item.colorCode else "---"}</td>
                <td class="text-center">${if (item.surfaceTreatment.isNotEmpty()) item.surfaceTreatment else "---"}</td>
                <td class="text-center bold">${if (branch > 0) formatNum(branch) else "---"}</td>
                <td class="text-center bold">${formatNum(qty)}</td>
                <td class="text-center">${formatNum(unitPriceRial)}</td>
                <td class="text-center">${formatNum(totalRial)}</td>
                <td class="text-center">${if (discPercent > 0) formatNum(discPercent) else "۰"}</td>
                <td class="text-center bold">${formatNum(payableRial)}</td>
                <td class="text-center">${formatNum(taxRial)}</td>
                <td class="text-center">${formatNum(vatRial)}</td>
                <td class="text-center bold">${formatNum(grandRial)}</td>
            </tr>
        """
    }

    // Accessory / Clip Items Calculations
    var totalAccQty = 0.0
    var totalAccPriceRial = 0.0
    var totalAccPayableRial = 0.0
    var totalAccTaxRial = 0.0
    var totalAccVatRial = 0.0
    var totalAccGrandRial = 0.0

    val accUnits = accessoryItems.map { if (it.unit.isNotBlank()) it.unit else "قطعه" }.distinct()
    val accUnitLabel = if (accUnits.size == 1) accUnits.first() else "عدد / قطعه"
    val accTotalTitle = if (accUnits.size == 1) "جمع کل (ریال) / ${accUnits.first()} :" else "جمع کل (ریال) / اقلام تکمیلی :"

    var accTrs = ""
    val accStartIdx = woodItems.size + 1
    for ((idx, item) in accessoryItems.withIndex()) {
        val qty = item.quantity
        val unitPriceRial = item.unitPrice * toRial
        val totalRial = qty * unitPriceRial
        val discPercent = item.discountPercent
        val payableRial = totalRial * (1.0 - discPercent / 100.0)
        val itemTaxRateFactor = if (hasTax) 0.07 else (item.taxPercent * 0.7 / 100.0)
        val itemVatRateFactor = if (hasTax) 0.03 else (item.taxPercent * 0.3 / 100.0)
        val taxRial = payableRial * itemTaxRateFactor
        val vatRial = payableRial * itemVatRateFactor
        val grandRial = payableRial + taxRial + vatRial

        totalAccQty += qty
        totalAccPriceRial += totalRial
        totalAccPayableRial += payableRial
        totalAccTaxRial += taxRial
        totalAccVatRial += vatRial
        totalAccGrandRial += grandRial

        accTrs += """
            <tr>
                <td class="text-center">${formatNum((accStartIdx + idx).toDouble())}</td>
                <td class="bold text-center">${item.name}</td>
                <td class="text-center">${if (item.sku.isNotEmpty()) item.sku else "---"}</td>
                <td class="text-center bold">${formatNum(qty)}</td>
                <td class="text-center">${if (item.unit.isNotEmpty()) item.unit else "قطعه"}</td>
                <td class="text-center">${formatNum(unitPriceRial)}</td>
                <td class="text-center">${formatNum(totalRial)}</td>
                <td class="text-center">${if (discPercent > 0) formatNum(discPercent) else "۰"}</td>
                <td class="text-center bold">${formatNum(payableRial)}</td>
                <td class="text-center">${formatNum(taxRial)}</td>
                <td class="text-center">${formatNum(vatRial)}</td>
                <td class="text-center bold">${formatNum(grandRial)}</td>
            </tr>
        """
    }

    // Installation Items Calculations
    var totalInstQty = 0.0
    var totalInstPriceRial = 0.0
    var totalInstPayableRial = 0.0
    var totalInstTaxRial = 0.0
    var totalInstVatRial = 0.0
    var totalInstGrandRial = 0.0

    val hasInstColor = installationItems.any { it.colorCode.isNotBlank() }
    val hasInstSurface = installationItems.any { it.surfaceTreatment.isNotBlank() }
    val hasInstBranch = installationItems.any { it.branchCount > 0 }
    val hasInstDays = installationItems.any { it.executionDays > 0 }
    val hasInstTeam = installationItems.any { it.teamSize > 0 }
    val hasInstAccommodation = installationItems.any { it.accommodationCost > 0 }
    val hasInstTransport = installationItems.any { it.transportationCost > 0 }
    val hasInstConsumables = installationItems.any { it.consumablesCost > 0 }

    val instHeaderCols = StringBuilder()
    instHeaderCols.append("""<th style="width:4%;">ردیف</th>""")
    instHeaderCols.append("""<th style="width:14%;">موضوع درخواست</th>""")
    instHeaderCols.append("""<th style="width:16%;">عنوان خدمات</th>""")
    if (hasInstColor) instHeaderCols.append("""<th style="width:7%;">کد رنگ</th>""")
    if (hasInstSurface) instHeaderCols.append("""<th style="width:8%;">عملیات سطحی</th>""")
    if (hasInstBranch) instHeaderCols.append("""<th style="width:6%;">تعداد</th>""")
    instHeaderCols.append("""<th style="width:7%;">متراژ</th>""")
    instHeaderCols.append("""<th style="width:10%;">قیمت واحد (ریال)</th>""")
    instHeaderCols.append("""<th style="width:10%;">قیمت پایه (ریال)</th>""")
    if (hasInstDays) instHeaderCols.append("""<th style="width:6%;">مدت اجرا</th>""")
    if (hasInstTeam) instHeaderCols.append("""<th style="width:5%;">نفرات</th>""")
    if (hasInstAccommodation) instHeaderCols.append("""<th style="width:9%;">هزینه اسکان (ریال)</th>""")
    if (hasInstTransport) instHeaderCols.append("""<th style="width:9%;">ایاب و ذهاب (ریال)</th>""")
    if (hasInstConsumables) instHeaderCols.append("""<th style="width:9%;">اقلام مصرفی (ریال)</th>""")
    instHeaderCols.append("""<th style="width:11%;">جمع کل (ریال)</th>""")

    var instTrs = ""
    val instStartIdx = woodItems.size + accessoryItems.size + 1
    for ((idx, item) in installationItems.withIndex()) {
        val branch = item.branchCount
        val qty = item.quantity
        val unitPriceRial = item.unitPrice * toRial
        val baseRial = qty * unitPriceRial
        val totalCostsRial = (item.accommodationCost + item.transportationCost + item.consumablesCost) * toRial
        val totalRial = baseRial + totalCostsRial
        val discPercent = item.discountPercent
        val payableRial = totalRial * (1.0 - discPercent / 100.0)
        val itemTaxRateFactor = if (hasTax) 0.07 else (item.taxPercent * 0.7 / 100.0)
        val itemVatRateFactor = if (hasTax) 0.03 else (item.taxPercent * 0.3 / 100.0)
        val taxRial = payableRial * itemTaxRateFactor
        val vatRial = payableRial * itemVatRateFactor
        val grandRial = payableRial + taxRial + vatRial

        totalInstQty += qty
        totalInstPriceRial += totalRial
        totalInstPayableRial += payableRial
        totalInstTaxRial += taxRial
        totalInstVatRial += vatRial
        totalInstGrandRial += grandRial

        val sbRow = StringBuilder()
        sbRow.append("<tr>")
        sbRow.append("""<td class="text-center">${formatNum((instStartIdx + idx).toDouble())}</td>""")
        sbRow.append("""<td class="bold text-center">${item.requestSubject.ifEmpty { "اجرای پروژه" }}</td>""")
        sbRow.append("""<td class="bold text-center">${item.name}</td>""")
        if (hasInstColor) sbRow.append("""<td class="text-center">${if (item.colorCode.isNotEmpty()) item.colorCode else "---"}</td>""")
        if (hasInstSurface) sbRow.append("""<td class="text-center">${if (item.surfaceTreatment.isNotEmpty()) item.surfaceTreatment else "---"}</td>""")
        if (hasInstBranch) sbRow.append("""<td class="text-center bold">${if (branch > 0) formatNum(branch) else "---"}</td>""")
        sbRow.append("""<td class="text-center bold">${if (qty > 0) formatNum(qty) else "---"}</td>""")
        sbRow.append("""<td class="text-center">${formatNum(unitPriceRial)}</td>""")
        sbRow.append("""<td class="text-center">${formatNum(baseRial)}</td>""")
        if (hasInstDays) sbRow.append("""<td class="text-center">${if (item.executionDays > 0) "${formatNum(item.executionDays.toDouble())} روز" else "---"}</td>""")
        if (hasInstTeam) sbRow.append("""<td class="text-center">${if (item.teamSize > 0) "${formatNum(item.teamSize.toDouble())} نفر" else "---"}</td>""")
        if (hasInstAccommodation) sbRow.append("""<td class="text-center">${if (item.accommodationCost > 0) formatNum(item.accommodationCost * toRial) else "---"}</td>""")
        if (hasInstTransport) sbRow.append("""<td class="text-center">${if (item.transportationCost > 0) formatNum(item.transportationCost * toRial) else "---"}</td>""")
        if (hasInstConsumables) sbRow.append("""<td class="text-center">${if (item.consumablesCost > 0) formatNum(item.consumablesCost * toRial) else "---"}</td>""")
        sbRow.append("""<td class="text-center bold">${formatNum(grandRial)}</td>""")
        sbRow.append("</tr>")

        instTrs += sbRow.toString()
    }

    // Cabinet Items Calculations
    var totalCabQty = 0.0
    var totalCabPriceRial = 0.0
    var totalCabPayableRial = 0.0
    var totalCabTaxRial = 0.0
    var totalCabVatRial = 0.0
    var totalCabGrandRial = 0.0

    var cabTrs = ""
    val cabStartIdx = woodItems.size + accessoryItems.size + installationItems.size + 1
    for ((idx, item) in cabinetItems.withIndex()) {
        val qty = item.quantity
        val unitPriceRial = item.unitPrice * toRial
        val totalRial = qty * unitPriceRial
        val discPercent = item.discountPercent
        val payableRial = totalRial * (1.0 - discPercent / 100.0)
        val itemTaxRateFactor = if (hasTax) 0.06 else (if (item.taxPercent > 0) item.taxPercent * 0.6 / 100.0 else 0.0)
        val itemVatRateFactor = if (hasTax) 0.03 else (if (item.taxPercent > 0) item.taxPercent * 0.3 / 100.0 else 0.0)
        val taxRial = payableRial * itemTaxRateFactor
        val vatRial = payableRial * itemVatRateFactor
        val grandRial = payableRial + taxRial + vatRial

        totalCabQty += qty
        totalCabPriceRial += totalRial
        totalCabPayableRial += payableRial
        totalCabTaxRial += taxRial
        totalCabVatRial += vatRial
        totalCabGrandRial += grandRial

        val dimStr = if (item.description.isNotBlank()) item.description else (if (item.sku.isNotBlank()) item.sku else "---")

        cabTrs += """
            <tr>
                <td class="text-center">${formatNum((cabStartIdx + idx).toDouble())}</td>
                <td class="bold text-center">${item.name}</td>
                <td class="text-center bold">${dimStr}</td>
                <td class="text-center bold">${formatNum(qty)}</td>
                <td class="text-center">${formatNum(unitPriceRial)}</td>
                <td class="text-center">${formatNum(totalRial)}</td>
                <td class="text-center">${if (discPercent > 0) formatNum(discPercent) else "۰"}</td>
                <td class="text-center bold">${formatNum(payableRial)}</td>
                <td class="text-center">${formatNum(taxRial)}</td>
                <td class="text-center">${formatNum(vatRial)}</td>
                <td class="text-center bold">${formatNum(grandRial)}</td>
            </tr>
        """
    }

    val instFooterColSpan = 3 + (if (hasInstColor) 1 else 0) + (if (hasInstSurface) 1 else 0)
    val instFooterTrailingColSpan = (if (hasInstDays) 1 else 0) + (if (hasInstTeam) 1 else 0) + (if (hasInstAccommodation) 1 else 0) + (if (hasInstTransport) 1 else 0) + (if (hasInstConsumables) 1 else 0)
    val trailingTd = if (instFooterTrailingColSpan > 0) """<td colspan="$instFooterTrailingColSpan">---</td>""" else ""

    // Combined Totals
    val totalWoodWeight = woodItems.sumOf { (if (it.quantity > 0) it.quantity else it.branchCount * 3.0) * it.weight }
    val totalAccWeight = accessoryItems.sumOf { it.quantity * it.weight }
    val totalInvoiceWeight = totalWoodWeight + totalAccWeight

    val grandTotalWoodAndClips = totalWoodGrandRial + totalAccGrandRial + totalInstGrandRial + totalCabGrandRial
    val totalTaxAndVat = (totalWoodTaxRial + totalWoodVatRial) + (totalAccTaxRial + totalAccVatRial) + (totalInstTaxRial + totalInstVatRial) + (totalCabTaxRial + totalCabVatRial)
    val totalDiscountRial = (totalWoodPriceRial - totalWoodPayableRial) + (totalAccPriceRial - totalAccPayableRial) + (totalInstPriceRial - totalInstPayableRial) + (totalCabPriceRial - totalCabPayableRial) + (invoice.discountAmount * toRial)

    val defaultCabinetTerms = listOf(
        "نقدی",
        "این شرکت هیچگونه مسئولیتی در قبال روکش اچ پی ال که خریدار بر روی سطح صفحه کابینت اعمال میکند نخواهد داشت .",
        "لطفا در انتخاب محصول و تعداد مورد نیاز دقت لازم را مبذول بفرمایید .",
        "در صورت ارایه سفارش ، شروع تولید پس از واریزمبلغ پیش پرداخت و دریافت رسید واریز و ارائه تاییدیه مالی شرکت می باشد .",
        "تایید پیش فاکتور و واریز پیش پرداخت به منزله قبول شرایط و خرید قطعی کالا از طرف خریدار بوده و در صورت انصراف مشتری خسارات احتمالی برآورد و دریافت می گردد .",
        "تمامی هزینه های حمل از محل کارخانه در ابهر تا محل خریدار پروژه به عهده خریدار می باشد .",
        "زمان تحویل کالا در صورت موجود نبودن در انبار با هماهنگی به خریدار اعلام خواهد شد .",
        "ارسال بار منوط به تسویه حساب مالی کامل فاکتور و واریز مبلغ مورد نظر به حساب اعلام شده می باشد ."
    )

    val termsList = if (invoice.notes.isNotBlank()) {
        invoice.notes.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    } else if (cabinetItems.isNotEmpty()) {
        defaultCabinetTerms
    } else {
        listOf(
            "۵۰ درصد مبلغ کل در ابتدا و مابقی قبل از خروج سفارش از کارخانه دریافت می‌گردد.",
            "رنگ به هیچ عنوان و تحت هیچ شرایطی شامل گارانتی نمی‌گردد. سفارش کالا و رنگ انتخابی بر اساس نمونه محصول بوده و به دلیل پودر چوب طبیعی احتمال تغییر رنگ وجود دارد.",
            "لطفاً در انتخاب محصول و متراژ دقت لازم را مبذول بفرمایید. در صورت خرید در دو مرحله، تغییرات رنگ اجتناب‌ناپذیر است.",
            "شروع تولید پس از واریز مبلغ پیش‌پرداخت، دریافت رسید واریز و ارائه تاییدیه مالی شرکت می‌باشد.",
            "تایید پیش‌فاکتور و واریز پیش‌پرداخت به منزله قبول شرایط و خرید قطعی بوده و در صورت انصراف خریدار، خسارات احتمالی کسر می‌گردد.",
            "زمان تحویل کالا در صورت عدم موجودی در انبار با هماهنگی قبلی به خریدار اعلام می‌شود.",
            "انتقال کالا در طبقات و تخلیه بار بر عهده خریدار می‌باشد.",
            "ارسال بار منوط به تسویه حساب کامل مالی فاکتور و واریز به حساب اعلام شده می‌باشد.",
            "تمامی اقلام و متراژهای مندرج در فاکتور تقریبی بوده و امکان تغییر نهایی در آن وجود دارد."
        )
    }

    val dynamicTermsHtml = termsList.mapIndexed { i, line ->
        val hasNumberPrefix = line.matches(Regex("^[۰-۹0-9]+[-–].*"))
        val itemText = if (hasNumberPrefix) line else "${com.example.util.Helper.formatDouble((i + 1).toDouble(), usePersianDigits)}- $line"
        "$itemText<br/>"
    }.joinToString("\n")

    return """
        <!DOCTYPE html>
        <html dir="rtl" lang="fa">
        <head>
            <meta charset="utf-8">
            <title>Nest Invoice - ${invoice.invoiceNumber}</title>
            <style>
                @import url('https://fonts.googleapis.com/css2?family=Vazirmatn:wght@400;600;700;800&display=swap');
                * { box-sizing: border-box; }
                body {
                    font-family: 'Vazirmatn', Tahoma, sans-serif;
                    margin: 0;
                    padding: ${marginSize}px;
                    background-color: #ffffff;
                    color: #000000;
                    font-size: 10px;
                    direction: rtl;
                    line-height: 1.35;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 4px;
                    table-layout: fixed;
                }
                th, td {
                    border: 1.2px solid #000000 !important;
                    padding: 3px 3px;
                    word-wrap: break-word;
                    font-size: 9px;
                    color: #000000;
                }
                .bg-gray {
                    background-color: #e5e7eb !important;
                    -webkit-print-color-adjust: exact !important;
                    print-color-adjust: exact !important;
                }
                .text-center { text-align: center; }
                .text-right { text-align: right; }
                .bold { font-weight: bold; }
                
                @media print {
                    @page {
                        size: A4 portrait;
                        margin: 4mm 5mm 4mm 5mm !important;
                    }
                    body {
                        padding: 0 !important;
                        margin: 0 !important;
                        background: #ffffff !important;
                        color: #000000 !important;
                        -webkit-print-color-adjust: exact !important;
                        print-color-adjust: exact !important;
                    }
                    .bg-gray {
                        background-color: #e5e7eb !important;
                        -webkit-print-color-adjust: exact !important;
                        print-color-adjust: exact !important;
                    }
                    th, td {
                        border: 1.2px solid #000000 !important;
                    }
                }
            </style>
        </head>
        <body>
            <!-- Header Table -->
            <table>
                <tr>
                    <td rowspan="2" style="width:18%; text-align:center; vertical-align:middle; padding: 4px 2px;">
                        ${if (s.nestLogoStyle == "light") """
                        <svg width="50" height="58" viewBox="0 0 100 120" xmlns="http://www.w3.org/2000/svg" style="display:block; margin: 0 auto; background-color:#FFFFFF; padding: 2px;">
                          <rect x="10" y="10" width="80" height="80" fill="#5D5D5D" />
                          <line x1="10" y1="90" x2="90" y2="10" stroke="#FFFFFF" stroke-width="2.5" />
                          <line x1="36.6" y1="10" x2="36.6" y2="63.3" stroke="#FFFFFF" stroke-width="2" />
                          <line x1="50" y1="10" x2="50" y2="50" stroke="#FFFFFF" stroke-width="2" />
                          <line x1="50" y1="50" x2="90" y2="50" stroke="#FFFFFF" stroke-width="2" />
                          <line x1="36.6" y1="63.3" x2="90" y2="63.3" stroke="#FFFFFF" stroke-width="2" />
                          <text x="50" y="112" font-family="'Vazirmatn', sans-serif" font-size="13" font-weight="bold" fill="#333333" text-anchor="middle" letter-spacing="3.5">N E S T</text>
                        </svg>
                        """.trimIndent() else """
                        <svg width="50" height="58" viewBox="0 0 100 120" xmlns="http://www.w3.org/2000/svg" style="display:block; margin: 0 auto; background-color:#000000; padding: 2px; border-radius:2px;">
                          <rect x="10" y="10" width="80" height="80" fill="#525252" />
                          <line x1="10" y1="90" x2="90" y2="10" stroke="#000000" stroke-width="2.5" />
                          <line x1="36.6" y1="10" x2="36.6" y2="63.3" stroke="#000000" stroke-width="2" />
                          <line x1="50" y1="10" x2="50" y2="50" stroke="#000000" stroke-width="2" />
                          <line x1="50" y1="50" x2="90" y2="50" stroke="#000000" stroke-width="2" />
                          <line x1="36.6" y1="63.3" x2="90" y2="63.3" stroke="#000000" stroke-width="2" />
                          <text x="50" y="112" font-family="'Vazirmatn', sans-serif" font-size="13" font-weight="bold" fill="#CCCCCC" text-anchor="middle" letter-spacing="3.5">N E S T</text>
                        </svg>
                        """.trimIndent()}
                    </td>
                    <td rowspan="2" class="bg-gray text-center" style="width:36%; vertical-align:middle; padding: 4px;">
                        <h1 style="margin:0; font-size: 18px; font-weight: 800;">${if (invoice.invoiceType.isNotEmpty()) invoice.invoiceType else "فاکتور"}</h1>
                    </td>
                    <td style="width:46%; padding: 3px 6px;">
                        <div style="display:flex; justify-content:space-between; align-items:center;">
                            <span>شماره :</span>
                            <span class="bold" style="font-size: 10.5px;">${invoice.invoiceNumber}</span>
                            <span style="margin-right:12px;">انبار :</span>
                            <span class="bold">${if (invoice.referenceNo.isNotEmpty()) invoice.referenceNo else "اصلی"}</span>
                        </div>
                    </td>
                </tr>
                <tr>
                    <td style="padding: 3px 6px;">
                        <div style="display:flex; justify-content:space-between; align-items:center;">
                            <span>تاریخ :</span>
                            <span class="bold">${com.example.util.Helper.formatJalaliShort(invoice.issueDate, usePersianDigits)}</span>
                            <span style="margin-right:12px;">خریدار :</span>
                            <span class="bold" style="font-size: 10.5px;">${customer?.name ?: "---"}</span>
                        </div>
                    </td>
                </tr>
                <tr>
                    <td colspan="2" style="padding: 3px 6px; font-size: 9px;">
                        <div style="display:flex; justify-content:space-between;">
                            <span>نشانی : ${s.companyAddress}</span>
                        </div>
                    </td>
                    <td style="padding: 3px 6px; font-size: 9px;">
                        <div style="display:flex; justify-content:space-between;">
                            <span>شماره تماس : ${customer?.phone ?: customer?.mobile ?: "---"}</span>
                        </div>
                    </td>
                </tr>
            </table>

            <!-- Table 1: WPC Wood Items (چوب پلاست) -->
            ${if (woodItems.isNotEmpty()) """
            <table>
                <thead>
                    <tr class="bg-gray text-center bold">
                        <th style="width:4%;">ردیف</th>
                        <th style="width:12%;">نام کالا</th>
                        <th style="width:7%;">کد کالا</th>
                        <th style="width:6%;">کد رنگ</th>
                        <th style="width:8%;">عملیات سطحی</th>
                        <th style="width:6%;">تعداد شاخه</th>
                        <th style="width:8%;">مقدار (متر طول)</th>
                        <th style="width:10%;">قیمت واحد (ریال)</th>
                        <th style="width:10%;">جمع (ریال)</th>
                        <th style="width:5%;">تخفیف٪</th>
                        <th style="width:10%;">قابل پرداخت (ریال)</th>
                        <th style="width:6%;">مالیات٪۷</th>
                        <th style="width:5%;">عوارض٪۳</th>
                        <th style="width:13%;">جمع کل با ارزش افزوده (ریال)</th>
                    </tr>
                </thead>
                <tbody>
                    $woodTrs
                </tbody>
                <tfoot>
                    <tr class="bg-gray bold text-center">
                        <td colspan="5" class="text-right" style="padding-right:8px;">جمع کل (ریال) / متراژ چوب پلاست :</td>
                        <td>${formatNum(totalWoodBranch)}</td>
                        <td>${formatNum(totalWoodLength)}</td>
                        <td colspan="2">${formatNum(totalWoodPriceRial)}</td>
                        <td>---</td>
                        <td>${formatNum(totalWoodPayableRial)}</td>
                        <td>${formatNum(totalWoodTaxRial)}</td>
                        <td>${formatNum(totalWoodVatRial)}</td>
                        <td style="font-size: 10px;">${formatNum(totalWoodGrandRial)}</td>
                    </tr>
                </tfoot>
            </table>
            """ else ""}

            <!-- Table 2: Accessories & Clips (پیچ و کلیپس) -->
            ${if (accessoryItems.isNotEmpty()) """
            <table>
                <thead>
                    <tr class="bg-gray text-center bold">
                        <th style="width:4%;">ردیف</th>
                        <th style="width:23%;">شرح کالا</th>
                        <th style="width:8%;">کد کالا</th>
                        <th style="width:6%;">تعداد</th>
                        <th style="width:6%;">واحد</th>
                        <th style="width:11%;">قیمت واحد کلیپس (ریال)</th>
                        <th style="width:11%;">جمع (ریال)</th>
                        <th style="width:5%;">تخفیف٪</th>
                        <th style="width:11%;">قابل پرداخت (ریال)</th>
                        <th style="width:5%;">مالیات٪۷</th>
                        <th style="width:5%;">عوارض٪۳</th>
                        <th style="width:15%;">جمع کل با ارزش افزوده (ریال)</th>
                    </tr>
                </thead>
                <tbody>
                    $accTrs
                </tbody>
                <tfoot>
                    <tr class="bg-gray bold text-center">
                        <td colspan="3" class="text-right" style="padding-right:8px;">$accTotalTitle</td>
                        <td>${formatNum(totalAccQty)}</td>
                        <td>$accUnitLabel</td>
                        <td colspan="2">${formatNum(totalAccPriceRial)}</td>
                        <td>---</td>
                        <td>${formatNum(totalAccPayableRial)}</td>
                        <td>${formatNum(totalAccTaxRial)}</td>
                        <td>${formatNum(totalAccVatRial)}</td>
                        <td style="font-size: 10px;">${formatNum(totalAccGrandRial)}</td>
                    </tr>
                </tfoot>
            </table>
            """ else ""}

            <!-- Table 3: Installation Services (خدمات نصب و اجرا) -->
            ${if (installationItems.isNotEmpty()) """
            <table>
                <thead>
                    <tr class="bg-gray text-center bold">
                        $instHeaderCols
                    </tr>
                </thead>
                <tbody>
                    $instTrs
                </tbody>
                <tfoot>
                    <tr class="bg-gray bold text-center">
                        <td colspan="$instFooterColSpan" class="text-right" style="padding-right:8px;">جمع کل (ریال) / خدمات نصب و اجرا :</td>
                        ${if (hasInstBranch) "<td>---</td>" else ""}
                        <td>${formatNum(totalInstQty)}</td>
                        <td colspan="2">${formatNum(totalInstPriceRial)}</td>
                        $trailingTd
                        <td style="font-size: 10px;">${formatNum(totalInstGrandRial)}</td>
                    </tr>
                </tfoot>
            </table>
            """ else ""}

            <!-- Table 4: Cabinet Items (صفحه کابینت) -->
            ${if (cabinetItems.isNotEmpty()) """
            <table>
                <thead>
                    <tr class="bg-gray text-center bold">
                        <th style="width:4%;">ردیف</th>
                        <th style="width:25%;">شرح کالا</th>
                        <th style="width:15%;">ابعاد</th>
                        <th style="width:6%;">تعداد</th>
                        <th style="width:11%;">قیمت واحد (ریال)</th>
                        <th style="width:11%;">جمع (ریال)</th>
                        <th style="width:5%;">تخفیف٪</th>
                        <th style="width:11%;">قابل پرداخت (ریال)</th>
                        <th style="width:5%;">مالیات٪۶</th>
                        <th style="width:4%;">عوارض٪۳</th>
                        <th style="width:13%;">جمع کل با ارزش افزوده (ریال)</th>
                    </tr>
                </thead>
                <tbody>
                    $cabTrs
                </tbody>
                <tfoot>
                    <tr class="bg-gray bold text-center">
                        <td colspan="3" class="text-right" style="padding-right:8px;">جمع کل (ریال) / صفحه کابینت :</td>
                        <td>${formatNum(totalCabQty)}</td>
                        <td colspan="2">${formatNum(totalCabPriceRial)}</td>
                        <td>---</td>
                        <td>${formatNum(totalCabPayableRial)}</td>
                        <td>${formatNum(totalCabTaxRial)}</td>
                        <td>${formatNum(totalCabVatRial)}</td>
                        <td style="font-size: 10px;">${formatNum(totalCabGrandRial)}</td>
                    </tr>
                </tfoot>
            </table>
            """ else ""}

            <!-- Calculation Totals Box (Rows 26-29) -->
            <table style="margin-top: 3px;">
                ${if (woodItems.isNotEmpty() && (accessoryItems.isNotEmpty() || installationItems.isNotEmpty() || cabinetItems.isNotEmpty())) """
                <tr>
                    <td class="bold" style="width:70%;">جمع کل خرید چوب پلاست ( ریال ) :</td>
                    <td class="bold text-center" style="width:30%; font-size: 10.5px;">${formatNum(totalWoodGrandRial)}</td>
                </tr>
                """ else ""}
                ${if (accessoryItems.isNotEmpty() && (woodItems.isNotEmpty() || installationItems.isNotEmpty() || cabinetItems.isNotEmpty())) """
                <tr>
                    <td class="bold" style="width:70%;">جمع کل خرید پیچ و کلیپس ( ریال ) :</td>
                    <td class="bold text-center" style="width:30%; font-size: 10.5px;">${formatNum(totalAccGrandRial)}</td>
                </tr>
                """ else ""}
                ${if (installationItems.isNotEmpty() && (woodItems.isNotEmpty() || accessoryItems.isNotEmpty() || cabinetItems.isNotEmpty())) """
                <tr>
                    <td class="bold" style="width:70%;">جمع کل خدمات نصب و اجرا ( ریال ) :</td>
                    <td class="bold text-center" style="width:30%; font-size: 10.5px;">${formatNum(totalInstGrandRial)}</td>
                </tr>
                """ else ""}
                ${if (cabinetItems.isNotEmpty() && (woodItems.isNotEmpty() || accessoryItems.isNotEmpty() || installationItems.isNotEmpty())) """
                <tr>
                    <td class="bold" style="width:70%;">جمع کل خرید صفحه کابینت ( ریال ) :</td>
                    <td class="bold text-center" style="width:30%; font-size: 10.5px;">${formatNum(totalCabGrandRial)}</td>
                </tr>
                """ else ""}
                <tr>
                    <td class="bold" style="width:70%; font-size: 10.5px;">تخفیف ( ریال ) :</td>
                    <td class="bold text-center" style="width:30%; font-size: 10.5px;">${formatNum(totalDiscountRial)}</td>
                </tr>
                <tr>
                    <td class="bold" style="font-size: 10.5px;">ارزش افزوده (مالیات + عوارض) :</td>
                    <td class="bold text-center" style="font-size: 10.5px;">${formatNum(totalTaxAndVat)}</td>
                </tr>
                ${if (totalInvoiceWeight > 0.0) """
                <tr>
                    <td class="bold" style="font-size: 10.5px;">مجموع وزن تقریبی بار :</td>
                    <td class="bold text-center" style="font-size: 10.5px;">${if (totalInvoiceWeight >= 1000) "${formatNum(totalInvoiceWeight / 1000.0)} تن (${formatNum(totalInvoiceWeight)} kg)" else "${formatNum(totalInvoiceWeight)} کیلوگرم"}</td>
                </tr>
                """ else ""}
                <tr class="bg-gray">
                    <td class="bold" style="font-size: 10.5px;">${
                        when {
                            cabinetItems.isNotEmpty() && woodItems.isEmpty() && accessoryItems.isEmpty() && installationItems.isEmpty() -> "جمع کل فاکتور صفحه کابینت ( ریال ) :"
                            woodItems.isNotEmpty() && accessoryItems.isNotEmpty() && installationItems.isNotEmpty() -> "جمع کل فاکتور ( چوب پلاست ، پیچ ، کلیپس و نصب ) ( ریال ) :"
                            woodItems.isNotEmpty() && accessoryItems.isNotEmpty() -> "جمع کل خرید چوب پلاست ، پیچ و کلیپس ( ریال ) :"
                            installationItems.isNotEmpty() && (woodItems.isNotEmpty() || accessoryItems.isNotEmpty()) -> "جمع کل فاکتور ( کالا و خدمات نصب ) ( ریال ) :"
                            installationItems.isNotEmpty() -> "جمع کل خدمات نصب و اجرا ( ریال ) :"
                            woodItems.isNotEmpty() -> "جمع کل خرید چوب پلاست ( ریال ) :"
                            accessoryItems.isNotEmpty() -> "جمع کل خرید پیچ و کلیپس ( ریال ) :"
                            cabinetItems.isNotEmpty() -> "جمع کل فاکتور ( شامل صفحه کابینت ) ( ریال ) :"
                            else -> "جمع کل فاکتور ( ریال ) :"
                        }
                    }</td>
                    <td class="bold text-center" style="font-size: 10.5px;">${formatNum(grandTotalWoodAndClips)}</td>
                </tr>
                
                ${if (invoice.advancePayment > 0) """
                <tr>
                    <td class="bold" style="color: #10B981; font-size: 10.5px;">مبلغ پرداخت شده ( ریال ) :</td>
                    <td class="bold text-center" style="color: #10B981; font-size: 10.5px;">- ${formatNum(invoice.advancePayment * toRial)}</td>
                </tr>
                <tr class="bg-gray">
                    <td class="bold" style="color: #EF4444; font-size: 10.5px;">مانده جهت تسویه ( ریال ) :</td>
                    <td class="bold text-center" style="color: #EF4444; font-size: 10.5px;">${formatNum((grandTotalWoodAndClips - invoice.advancePayment * toRial).coerceAtLeast(0.0))}</td>
                </tr>
                """ else ""}

                <tr>
                    <td class="bold">وضعیت پرداخت :</td>
                    <td class="text-center">${
                        when (invoice.status) {
                            "Paid" -> "تسویه کامل"
                            "PartiallyPaid" -> "پرداخت بخشی از مبلغ"
                            else -> "تسویه نشده (در انتظار پرداخت)"
                        }
                    }</td>
                </tr>
                ${if (invoice.paymentMethod.isNotEmpty() && invoice.status != "Pending") """
                <tr>
                    <td class="bold">روش پرداخت :</td>
                    <td class="text-center">${invoice.paymentMethod}</td>
                </tr>
                """ else ""}
                ${if (invoice.paymentDetails.isNotEmpty() && invoice.status != "Pending") """
                <tr>
                    <td class="bold">مشخصات پرداخت :</td>
                    <td class="text-center">${invoice.paymentDetails}</td>
                </tr>
                """ else ""}
                ${if (!isInstInvoice) """
                <tr>
                    <td class="bold">شرایط ارسال :</td>
                    <td class="text-center">پس از تسویه کامل</td>
                </tr>
                """ else ""}
            </table>

            <!-- Terms & Bank Details -->
            <table>
                <tr>
                    <td style="width:58%; vertical-align:top; padding: 4px; font-size: 8.5px; line-height: 1.36;">
                        <div class="bold" style="margin-bottom:2px;">شرایط پرداخت و فروش :</div>
                        $dynamicTermsHtml
                    </td>
                    <td style="width:42%; vertical-align:top; padding: 4px; font-size: 8.8px;">
                        <div class="bold" style="margin-bottom: 4px;">اطلاعات جهت پرداخت :</div>
                        <div style="background-color:#f9fafb; padding:4px; border:1px dashed #000; text-align:center;">
                            <div dir="ltr" class="bold" style="font-size:10px;">${bankAccount?.shabaNumber ?: "IR160110000000200021893006"}</div>
                            <div style="margin-top:2px;">شماره حساب : <span dir="ltr" class="bold">${bankAccount?.accountNumber ?: "0200021893006"}</span></div>
                            <div style="margin-top:2px;">شماره کارت : <span dir="ltr" class="bold">${bankAccount?.cardNumber ?: "6279 - 6118 - 0002 - 1185"}</span></div>
                            <div class="bold" style="margin-top:3px; color:#1e293b;">${bankAccount?.accountHolderName ?: "وفا چوب ایرانیان ابهر"} - ${bankAccount?.bankName ?: "بانک صنعت و معدن"}</div>
                        </div>
                    </td>
                </tr>
                <tr>
                    <td colspan="2" class="text-center bold" style="padding: 4px; font-size: 9.5px;">
                        توضیحات : اعتبار این فاکتور از تاریخ صدور فقط ۲۴ ساعت می باشد .
                    </td>
                </tr>
                ${if (woodItems.isNotEmpty() || accessoryItems.isNotEmpty() || installationItems.isNotEmpty()) """
                <tr class="bg-gray">
                    <td colspan="2" class="text-center bold" style="padding: 5px; font-size: 9.5px;">
                        محصولات نست صرفا در صورت نصب توسط تیم های اجرایی دارای گواهینامه از این شرکت و استفاده از کلیپس های مخصوص این شرکت، شامل دو سال گارانتی خواهد شد
                    </td>
                </tr>
                """ else ""}
            </table>
        </body>
        </html>
    """
}
