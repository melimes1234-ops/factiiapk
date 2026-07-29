package com.example.ui.screens

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import com.example.ui.components.SelectOnFocusTextField
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Invoice
import com.example.ui.InvoiceViewModel
import com.example.util.Helper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: InvoiceViewModel,
    onNavigateToNewInvoice: () -> Unit,
    onNavigateToAddCustomer: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToInvoiceDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val recentInvoices by viewModel.invoices.collectAsStateWithLifecycle()
    val allLineItems by viewModel.allLineItems.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val isRtl = viewModel.selectedLanguage == "fa"

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val cardBackground = MaterialTheme.colorScheme.surface
    val accentColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isRtl) "پیشخوان مالی" else "Financial Dashboard",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    lineHeight = 16.sp
                                )
                                Text(
                                    text = Helper.formatCurrentHeaderDate(
                                        timestamp = System.currentTimeMillis(),
                                        useJalali = viewModel.useJalaliCalendar,
                                        usePersianDigits = viewModel.usePersianDigits
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Search */ }) {
                            Icon(Icons.Outlined.Search, contentDescription = "جستجو")
                        }
                        IconButton(onClick = { /* Notifications */ }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "اعلان‌ها")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            }
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
            // --- Global Search Bar ---
            item {
                var localSearchText by remember { mutableStateOf(viewModel.searchQuery) }
                SelectOnFocusTextField(
                    value = localSearchText,
                    onValueChange = {
                        localSearchText = it
                        viewModel.searchQuery = it
                    },
                    placeholder = {
                        Text(
                            text = if (isRtl) "جستجو در بین فاکتورها، مشتریان و کالاها..." else "Search invoices, customers, services...",
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = "جستجو")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )
            }

            // --- Stats Dashboard Overview ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Revenue Card
                    StatCard(
                        title = if (isRtl) "کل دریافتی / تسویه شده" else "Total Received / Paid",
                        value = Helper.formatCurrency(stats.revenue, viewModel.selectedCurrency, viewModel.usePersianDigits),
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF14532D),
                        cardBackground = cardBackground,
                        modifier = Modifier.weight(1f),
                        isPrimaryHighlight = true
                    )
                    // Pending Card
                    StatCard(
                        title = if (isRtl) "در انتظار پرداخت (فاکتور فروش)" else "Pending Sales Balance",
                        value = Helper.formatCurrency(stats.pendingAmount, viewModel.selectedCurrency, viewModel.usePersianDigits),
                        icon = Icons.Default.HourglassEmpty,
                        color = Color(0xFF92400E),
                        cardBackground = cardBackground,
                        modifier = Modifier.weight(1f),
                        isPrimaryHighlight = false
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Proforma Card
                    StatCard(
                        title = if (isRtl) "پیش‌فاکتورها" else "Proforma Invoices",
                        value = Helper.formatCurrency(stats.proformaAmount, viewModel.selectedCurrency, viewModel.usePersianDigits),
                        icon = Icons.Default.Description,
                        color = Color(0xFF1E40AF),
                        cardBackground = cardBackground,
                        modifier = Modifier.weight(1f),
                        isPrimaryHighlight = false
                    )
                    // Overdue Card
                    StatCard(
                        title = if (isRtl) "سررسید گذشته" else "Overdue",
                        value = Helper.formatCurrency(stats.overdueAmount, viewModel.selectedCurrency, viewModel.usePersianDigits),
                        icon = Icons.Default.ErrorOutline,
                        color = Color(0xFF8C1D18),
                        cardBackground = cardBackground,
                        modifier = Modifier.weight(1f),
                        isPrimaryHighlight = false
                    )
                }
            }

            // --- Quick Actions Row ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isRtl) "دسترسی سریع" else "Quick Actions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            QuickActionButton(
                                label = if (isRtl) "فاکتور جدید" else "New Invoice",
                                icon = Icons.Default.Receipt,
                                containerColor = accentColor.copy(alpha = 0.1f),
                                iconColor = accentColor,
                                onClick = onNavigateToNewInvoice
                            )
                            QuickActionButton(
                                label = if (isRtl) "مشتری جدید" else "New Customer",
                                icon = Icons.Default.PersonAdd,
                                containerColor = Color(0xFF10B981).copy(alpha = 0.1f),
                                iconColor = Color(0xFF10B981),
                                onClick = onNavigateToAddCustomer
                            )
                            QuickActionButton(
                                label = if (isRtl) "ثبت خدمات" else "Add Service",
                                icon = Icons.Default.AddShoppingCart,
                                containerColor = Color(0xFFF59E0B).copy(alpha = 0.1f),
                                iconColor = Color(0xFFF59E0B),
                                onClick = onNavigateToAddProduct
                            )
                        }
                    }
                }
            }

            // --- Recent Invoices Section ---

            // --- Recent Invoices Section ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRtl) "آخرین فاکتورهای صادر شده" else "Recent Invoices",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (recentInvoices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isRtl) "هیچ فاکتوری یافت نشد." else "No invoices yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentInvoices.take(4), key = { "invoice_${it.id}" }) { invoice ->
                    val customer = customers.find { it.id == invoice.customerId }
                    val itemsForInv = allLineItems.filter { it.invoiceId == invoice.id }
                    val invAmount = viewModel.calculateInvoiceTotal(invoice, itemsForInv)
                    RecentInvoiceItem(
                        invoice = invoice,
                        customerName = customer?.name ?: (if (isRtl) "نامشخص" else "Unknown"),
                        companyName = customer?.company ?: "",
                        currencySymbol = viewModel.selectedCurrency,
                        usePersian = viewModel.usePersianDigits,
                        isRtl = isRtl,
                        amount = invAmount,
                        onClick = { onNavigateToInvoiceDetails(invoice.id) }
                    )
                }
            }

            // --- Recent System Activity Logs ---
            item {
                Text(
                    text = if (isRtl) "فعالیت‌های اخیر سیستم" else "Recent Activity Log",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (logs.isEmpty()) {
                item {
                    Text(
                        text = if (isRtl) "هیچ فعالیتی ثبت نشده است." else "No system activity yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            } else {
                items(logs.take(3), key = { "log_${it.id}" }) { log ->
                    ActivityLogItem(log = log, isRtl = isRtl, usePersian = viewModel.usePersianDigits)
                }
            }

            // Spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    cardBackground: Color,
    modifier: Modifier = Modifier,
    isPrimaryHighlight: Boolean = false
) {
    val containerColor = if (isPrimaryHighlight) MaterialTheme.colorScheme.primaryContainer else cardBackground
    val contentColor = if (isPrimaryHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isPrimaryHighlight) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconBgColor = if (isPrimaryHighlight) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f) else color.copy(alpha = 0.1f)
    val iconTintColor = if (isPrimaryHighlight) MaterialTheme.colorScheme.onPrimaryContainer else color
    val borderStroke = if (isPrimaryHighlight) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTintColor, modifier = Modifier.size(18.dp))
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
            .widthIn(max = 70.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label, 
            fontSize = 9.sp, 
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RevenueBarChart(
    revenueData: Map<String, Double>,
    usePersian: Boolean,
    isDark: Boolean
) {
    // We will render 5 sample months or dynamic months in a beautiful Compose Canvas
    val dataList = revenueData.toList().sortedBy { it.first }.takeLast(5)
    val finalData = if (dataList.isEmpty()) {
        listOf(
            "۱۴۰۵/۰۱" to 15000000.0,
            "۱۴۰۵/۰۲" to 22000000.0,
            "۱۴۰۵/۰۳" to 18000000.0,
            "۱۴۰۵/۰۴" to 31000000.0,
            "۱۴۰۵/۰۵" to 28000000.0
        )
    } else {
        dataList
    }

    val maxAmount = finalData.maxOfOrNull { it.second } ?: 1.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 8.dp)
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val spacing = 35.dp.toPx()
        val graphHeight = (size.height - spacing).coerceAtLeast(10f)
        val barWidth = 28.dp.toPx()
        val barsCount = finalData.size.coerceAtLeast(1)
        val availableWidth = (size.width - (barWidth * barsCount)).coerceAtLeast(0f)
        val widthBetweenBars = availableWidth / (barsCount + 1)

        // Draw helper background lines
        val linePaint = Color.Gray.copy(alpha = 0.1f)
        drawLine(
            color = linePaint,
            start = Offset(0f, graphHeight * 0.25f),
            end = Offset(size.width, graphHeight * 0.25f),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = linePaint,
            start = Offset(0f, graphHeight * 0.5f),
            end = Offset(size.width, graphHeight * 0.5f),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = linePaint,
            start = Offset(0f, graphHeight * 0.75f),
            end = Offset(size.width, graphHeight * 0.75f),
            strokeWidth = 1.dp.toPx()
        )

        finalData.forEachIndexed { index, pair ->
            val month = pair.first
            val amount = pair.second
            
            val ratio = if (maxAmount > 0) (amount / maxAmount).coerceIn(0.0, 1.0) else 0.0
            val barHeight = (ratio * graphHeight).toFloat().coerceAtLeast(4f)
            
            val x = widthBetweenBars + index * (barWidth + widthBetweenBars)
            val y = (graphHeight - barHeight).coerceAtLeast(0f)

            // Draw rounded bar with gradient
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1A73E8), Color(0xFF34A853))
            )
            val cornerRad = (6.dp.toPx()).coerceAtMost(barHeight / 2f)
            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(cornerRad, cornerRad)
            )

            // Draw label
            drawIntoCanvas { canvas ->
                val textPaint = android.graphics.Paint().apply {
                    color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.DKGRAY
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                
                val monthShort = if (month.length >= 2) month.takeLast(2) else month
                val label = if (usePersian) Helper.toPersianDigits("$monthShort تیر") else monthShort
                canvas.nativeCanvas.drawText(
                    label,
                    x + (barWidth / 2),
                    (size.height - 8.dp.toPx()).coerceAtLeast(graphHeight + 12.dp.toPx()),
                    textPaint
                )
            }
        }
    }
}

@Composable
fun RecentInvoiceItem(
    invoice: Invoice,
    customerName: String,
    companyName: String,
    currencySymbol: String,
    usePersian: Boolean,
    isRtl: Boolean,
    amount: Double = 0.0,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val (statusBgColor, statusTextColor) = when (invoice.status) {
        "Paid" -> Color(0xFFCFF7D3) to Color(0xFF14532D)
        "Pending", "PartiallyPaid" -> Color(0xFFFFECC7) to Color(0xFF92400E)
        "Overdue" -> Color(0xFFF9DEDC) to Color(0xFF8C1D18)
        else -> Color(0xFFE1E2E9) to Color(0xFF49454F)
    }

    val statusText = when (invoice.status) {
        "Paid" -> if (isRtl) "پرداخت شده" else "Paid"
        "Pending" -> if (isRtl) "در انتظار پرداخت" else "Pending"
        "PartiallyPaid" -> if (isRtl) "پرداخت شده اندکی" else "Partially Paid"
        "Overdue" -> if (isRtl) "سررسید گذشته" else "Overdue"
        "Draft" -> if (isRtl) "پیش‌نویس" else "Draft"
        else -> invoice.status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("invoice_item_${invoice.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = Helper.toPersianDigits(invoice.invoiceNumber),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    val isTargetType = invoice.invoiceType == "فاکتور فروش" || 
                            invoice.invoiceType.contains("نصب") || 
                            invoice.status == "PartiallyPaid"

                    if (invoice.paymentDocumentPaths.isNotBlank()) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF059669))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (isRtl) "مدارک پیوست" else "Attachment",
                                    color = Color(0xFF047857),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isTargetType) {
                        Surface(
                            color = Color(0xFFFFF7ED),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFF97316))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = Color(0xFFC2410C),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (isRtl) "فاقد پیوست" else "No Attachment",
                                    color = Color(0xFFC2410C),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (invoice.invoiceType.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = invoice.invoiceType,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(statusBgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText, 
                            color = statusTextColor, 
                            fontSize = 8.5.sp, 
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }


                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$customerName • $companyName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    val amountText = Helper.formatCurrency(
                        amount,
                        currencySymbol,
                        usePersian
                    )
                    Text(
                        text = amountText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = Helper.formatJalaliShort(invoice.issueDate, usePersian),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_invoice_btn_${invoice.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isRtl) "حذف فاکتور" else "Delete Invoice",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityLogItem(log: com.example.data.model.AuditLog, isRtl: Boolean, usePersian: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = log.details, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                text = Helper.formatJalaliFull(log.timestamp, usePersian),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
