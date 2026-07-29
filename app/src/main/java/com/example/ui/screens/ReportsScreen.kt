package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.InvoiceViewModel
import com.example.util.CsvImportExportUtil
import com.example.util.Helper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val isRtl = viewModel.selectedLanguage == "fa"
    val context = LocalContext.current

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val cardBackground = if (isDark) Color(0xFF1E1E2E) else Color(0xFFFAFAFC)
    val cardBorder = if (isDark) Color(0xFF2E2E3E) else Color(0xFFE5E7EB)

    val woodColor = Color(0xFF1A73E8) // Blue
    val accessoryColor = Color(0xFF10B981) // Green
    val installationColor = Color(0xFFF59E0B) // Amber
    val otherColor = Color(0xFF8B5CF6) // Purple

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRtl) "گزارشات و تحلیل مالی" else "Financial Reports",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        CsvImportExportUtil.exportReportsToCsv(
                            context,
                            viewModel.invoices.value,
                            viewModel.customers.value,
                            viewModel.products.value
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "خروجی اکسل",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
            // --- 1. Top KPI Cards ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Net Sales
                        KpiCard(
                            title = if (isRtl) "مجموع فروش خالص" else "Total Net Sales",
                            value = Helper.formatCurrency(stats.grandNet, viewModel.selectedCurrency, viewModel.usePersianDigits),
                            subtitle = if (isRtl) "${stats.grandItemCount} قلم کالا و خدمات" else "${stats.grandItemCount} Items",
                            icon = Icons.Default.Receipt,
                            iconBg = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            cardBg = cardBackground
                        )

                        // Total Collected
                        KpiCard(
                            title = if (isRtl) "مجموع دریافتی" else "Total Collected",
                            value = Helper.formatCurrency(stats.revenue, viewModel.selectedCurrency, viewModel.usePersianDigits),
                            subtitle = if (isRtl) "نقدینگی واریز شده" else "Cash Deposited",
                            icon = Icons.Default.Payments,
                            iconBg = Color(0xFFD1FAE5),
                            iconTint = Color(0xFF059669),
                            modifier = Modifier.weight(1f),
                            cardBg = cardBackground
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Outstanding
                        KpiCard(
                            title = if (isRtl) "کل معوقات و مطالبات" else "Outstanding Balance",
                            value = Helper.formatCurrency(stats.pendingAmount + stats.overdueAmount, viewModel.selectedCurrency, viewModel.usePersianDigits),
                            subtitle = if (isRtl) "مانده دریافت نشده" else "Uncollected",
                            icon = Icons.Default.TrendingUp,
                            iconBg = Color(0xFFFEE2E2),
                            iconTint = Color(0xFFDC2626),
                            modifier = Modifier.weight(1f),
                            cardBg = cardBackground
                        )

                        // Total Discounts & VAT
                        KpiCard(
                            title = if (isRtl) "تخفیفات و مالیات" else "Discounts & VAT",
                            value = Helper.formatCurrency(stats.grandDiscount, viewModel.selectedCurrency, viewModel.usePersianDigits),
                            subtitle = if (isRtl) "تخفیف | مالیات: ${Helper.formatCurrency(stats.totalTaxAndVat, viewModel.selectedCurrency, viewModel.usePersianDigits)}" else "Total Discounts",
                            icon = Icons.Default.Category,
                            iconBg = Color(0xFFFEF3C7),
                            iconTint = Color(0xFFD97706),
                            modifier = Modifier.weight(1f),
                            cardBg = cardBackground
                        )
                    }
                }
            }

            // --- 2. Category Sales Breakdown (چوب پلاست، پیچ و کلیپس، خدمات نصب و اجرا) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(cardBorder, cardBorder)))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isRtl) "گزارش تفکیکی فروش محصولات و خدمات" else "Detailed Product & Services Breakdown",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isRtl) "تفکیک چوب پلاست، پیچ و کلیپس و خدمات نصب و اجرا" else "Wood Plastic, Accessories & Installation Services",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isRtl) "تعداد کل: ${stats.grandItemCount}" else "Total: ${stats.grandItemCount}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Divider(color = cardBorder, thickness = 0.8.dp)

                        // Category Row 1: Wood Plastic (چوب پلاست)
                        CategoryDetailRow(
                            title = if (isRtl) "۱. چوب پلاست (انواع پروفیل)" else "1. Wood Plastic Profiles",
                            grossAmount = stats.woodGross,
                            discountAmount = stats.woodDiscount,
                            netAmount = stats.woodNet,
                            itemCount = stats.woodItemCount,
                            percentage = stats.woodPct,
                            barColor = woodColor,
                            currency = viewModel.selectedCurrency,
                            usePersianDigits = viewModel.usePersianDigits,
                            isRtl = isRtl,
                            detailsList = stats.woodDetails
                        )

                        HorizontalDivider(color = cardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Category Row 2: Screws & Clips (پیچ و کلیپس)
                        CategoryDetailRow(
                            title = if (isRtl) "۲. پیچ و کلیپس (اتصالات و یراق‌آلات)" else "2. Screws, Clips & Accessories",
                            grossAmount = stats.accessoryGross,
                            discountAmount = stats.accessoryDiscount,
                            netAmount = stats.accessoryNet,
                            itemCount = stats.accessoryItemCount,
                            percentage = stats.accessoryPct,
                            barColor = accessoryColor,
                            currency = viewModel.selectedCurrency,
                            usePersianDigits = viewModel.usePersianDigits,
                            isRtl = isRtl,
                            detailsList = stats.accessoryDetails
                        )

                        HorizontalDivider(color = cardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Category Row 3: Installation Services (خدمات نصب و اجرا)
                        CategoryDetailRow(
                            title = if (isRtl) "۳. خدمات نصب، اجرا و زیرسازی" else "3. Installation & Structural Services",
                            grossAmount = stats.installationGross,
                            discountAmount = stats.installationDiscount,
                            netAmount = stats.installationNet,
                            itemCount = stats.installationItemCount,
                            percentage = stats.installationPct,
                            barColor = installationColor,
                            currency = viewModel.selectedCurrency,
                            usePersianDigits = viewModel.usePersianDigits,
                            isRtl = isRtl,
                            detailsList = stats.installationDetails
                        )

                        // Category Row 4: Other (if any)
                        if (stats.otherNet > 0 || stats.otherDetails.isNotEmpty()) {
                            HorizontalDivider(color = cardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                            CategoryDetailRow(
                                title = if (isRtl) "۴. سایر کالاها و خدمات" else "4. Other Products & Services",
                                grossAmount = stats.otherGross,
                                discountAmount = stats.otherDiscount,
                                netAmount = stats.otherNet,
                                itemCount = stats.otherItemCount,
                                percentage = stats.otherPct,
                                barColor = otherColor,
                                currency = viewModel.selectedCurrency,
                                usePersianDigits = viewModel.usePersianDigits,
                                isRtl = isRtl,
                                detailsList = stats.otherDetails
                            )
                        }

                        // --- Grand Total Footer Card ---
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isRtl) "مجموع کل فروش (همه بخش‌ها)" else "Grand Total Sales (All Categories)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isRtl) "قبل تخفیف: ${Helper.formatCurrency(stats.grandGross, viewModel.selectedCurrency, viewModel.usePersianDigits)}  |  تخفیف: ${Helper.formatCurrency(stats.grandDiscount, viewModel.selectedCurrency, viewModel.usePersianDigits)}"
                                               else "Gross: ${Helper.formatCurrency(stats.grandGross, viewModel.selectedCurrency, viewModel.usePersianDigits)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = Helper.formatCurrency(stats.grandNet, viewModel.selectedCurrency, viewModel.usePersianDigits),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. Income Trend Line Chart ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRtl) "روند رشد درآمد (نمودار ماهانه)" else "Income Growth Trend",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isRtl) "بر اساس دریافتی‌ها" else "Based on Collections",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        IncomeTrendLineChart(revenueData = stats.monthlyRevenueMap, isDark = isDark)
                    }
                }
            }

            // --- 4. Top Selling Products & Profiles ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isRtl) "پرفروش‌ترین کالاها و پروفیل‌ها" else "Top Selling Products & Profiles",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (stats.topSellingProducts.isEmpty()) {
                            Text(
                                text = if (isRtl) "اطلاعاتی برای نمایش وجود ندارد." else "No product sales data available.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            stats.topSellingProducts.forEachIndexed { index, prod ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = prod.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (prod.sku.isNotBlank()) {
                                                Text(
                                                    text = "کد: ${prod.sku}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Helper.formatCurrency(prod.totalRevenue, viewModel.selectedCurrency, viewModel.usePersianDigits),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (isRtl) "مقدار: ${Helper.formatDouble(prod.totalQuantity, true)}" else "Qty: ${prod.totalQuantity}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (index < stats.topSellingProducts.size - 1) {
                                    Divider(color = cardBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }

            // --- 5. Top Customers by Purchase Volume ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isRtl) "مشتریان برتر بر اساس حجم خرید" else "Top Customers by Purchase Volume",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (stats.topSpendingCustomers.isEmpty()) {
                            Text(
                                text = if (isRtl) "اطلاعاتی برای نمایش وجود ندارد." else "No customer sales data available.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            stats.topSpendingCustomers.forEachIndexed { index, cust ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = CircleShape,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = cust.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (cust.company.isNotBlank()) {
                                                Text(
                                                    text = cust.company,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Helper.formatCurrency(cust.totalSpent, viewModel.selectedCurrency, viewModel.usePersianDigits),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (isRtl) "${cust.totalInvoicesCount} فاکتور ثبت شده" else "${cust.totalInvoicesCount} Invoices",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (index < stats.topSpendingCustomers.size - 1) {
                                    Divider(color = cardBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    cardBg: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = iconBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryDetailRow(
    title: String,
    grossAmount: Double,
    discountAmount: Double,
    netAmount: Double,
    itemCount: Int,
    percentage: Float,
    barColor: Color,
    currency: String,
    usePersianDigits: Boolean,
    isRtl: Boolean,
    detailsList: List<com.example.ui.CategoryItemDetail> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(barColor)
                )
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = Helper.formatCurrency(netAmount, currency, usePersianDigits),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (netAmount > 0) barColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Details subtext
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRtl) "قبل تخفیف: ${Helper.formatCurrency(grossAmount, currency, usePersianDigits)}  |  تخفیف: ${Helper.formatCurrency(discountAmount, currency, usePersianDigits)}"
                       else "Gross: ${Helper.formatCurrency(grossAmount, currency, usePersianDigits)} | Disc: ${Helper.formatCurrency(discountAmount, currency, usePersianDigits)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isRtl) "سهم: ${String.format("%.1f", percentage)}٪" else "Share: ${String.format("%.1f", percentage)}%",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )

                if (detailsList.isNotEmpty()) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = if (expanded) "پنهان‌سازی" else "جزئیات (${detailsList.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Percentage progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(3.5.dp))
                .background(Color.Gray.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((percentage / 100f).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.5.dp))
                    .background(barColor)
            )
        }

        // Expanded Itemized Details List
        if (expanded && detailsList.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isRtl) "ریز آیتم‌های ثبت شده در این بخش:" else "Itemized Breakdown:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    detailsList.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${idx + 1}. ${item.itemName}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "فاکتور #${item.invoiceNumber}  •  ${item.customerName}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Helper.formatCurrency(item.netAmount, currency, usePersianDigits),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = barColor
                                )
                                Text(
                                    text = "${Helper.formatDouble(item.quantity, usePersianDigits)} ${item.unit} × ${Helper.formatCurrency(item.unitPrice, currency, usePersianDigits)}",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (idx < detailsList.size - 1) {
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeTrendLineChart(
    revenueData: Map<String, Double>,
    isDark: Boolean
) {
    val values = revenueData.values.toList()
    val maxVal = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)

    if (values.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "هنوز داده‌ی مالی برای رسم نمودار وجود ندارد.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val spacing = 15.dp.toPx()
        val graphHeight = size.height - spacing

        // Draw helper grid lines
        val linePaint = Color.Gray.copy(alpha = 0.08f)
        for (i in 1..3) {
            drawLine(
                color = linePaint,
                start = Offset(0f, graphHeight * (i * 0.25f)),
                end = Offset(size.width, graphHeight * (i * 0.25f)),
                strokeWidth = 1.dp.toPx()
            )
        }

        val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width

        val points = values.mapIndexed { idx, valAmt ->
            val x = idx * stepX
            val ratio = (valAmt / maxVal).toFloat()
            val y = graphHeight * (1f - ratio.coerceIn(0.05f, 0.95f))
            Offset(x, y)
        }

        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            if (points.size == 1) {
                path.lineTo(size.width, points[0].y)
            } else {
                for (i in 1 until points.size) {
                    val prevPoint = points[i - 1]
                    val currPoint = points[i]
                    path.cubicTo(
                        (prevPoint.x + currPoint.x) / 2, prevPoint.y,
                        (prevPoint.x + currPoint.x) / 2, currPoint.y,
                        currPoint.x, currPoint.y
                    )
                }
            }

            drawPath(
                path = path,
                color = Color(0xFF1A73E8),
                style = Stroke(width = 3.dp.toPx())
            )

            val fillPath = Path().apply {
                addPath(path)
                lineTo(if (points.size == 1) size.width else points.last().x, graphHeight)
                lineTo(0f, graphHeight)
                close()
            }

            val areaBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1A73E8).copy(alpha = 0.25f), Color.Transparent)
            )
            drawPath(path = fillPath, brush = areaBrush)

            points.forEach { pt ->
                drawCircle(color = Color(0xFF1A73E8), radius = 4.5.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
            }
        }
    }
}
