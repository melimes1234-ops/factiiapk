package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.ui.components.SelectOnFocusTextField
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Invoice
import com.example.ui.InvoiceViewModel
import com.example.util.Helper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    viewModel: InvoiceViewModel,
    onNavigateToNewInvoice: () -> Unit,
    onNavigateToInvoiceDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val invoices by viewModel.filteredInvoices.collectAsStateWithLifecycle()
    val allLineItems by viewModel.allLineItems.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val isRtl = viewModel.selectedLanguage == "fa"

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val accentColor = MaterialTheme.colorScheme.primary
    val cardBackground = MaterialTheme.colorScheme.surface

    var invoiceToDelete by remember { mutableStateOf<Invoice?>(null) }

    if (invoiceToDelete != null) {
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = {
                Text(
                    text = if (isRtl) "حذف فاکتور" else "Delete Invoice",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isRtl)
                        "آیا از حذف فاکتور شماره ${Helper.toPersianDigits(invoiceToDelete!!.invoiceNumber)} اطمینان دارید؟ این عمل غیرقابل بازگشت است."
                    else
                        "Are you sure you want to delete invoice #${invoiceToDelete!!.invoiceNumber}? This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        invoiceToDelete?.let { viewModel.deleteInvoice(it) }
                        invoiceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isRtl) "حذف" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) {
                    Text(if (isRtl) "انصراف" else "Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isRtl) "مدیریت فاکتورها" else "Invoices Management",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewInvoice,
                containerColor = accentColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_invoice_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "فاکتور جدید")
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
            // --- Search Field ---
            var localSearchText by remember { mutableStateOf(viewModel.searchQuery) }
            SelectOnFocusTextField(
                value = localSearchText,
                onValueChange = {
                    localSearchText = it
                    viewModel.searchQuery = it
                },
                placeholder = {
                    Text(
                        text = if (isRtl) "جستجو با شماره فاکتور یا نام مشتری..." else "Search by ID or client name...",
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = "جستجو")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invoices_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- Status Filter Scrollable Tabs ---
            val tabs = listOf(
                "All" to (if (isRtl) "همه فاکتورها" else "All Invoices"),
                "Sales" to (if (isRtl) "فاکتورهای فروش" else "Sales Invoices"),
                "Proforma" to (if (isRtl) "پیش‌فاکتورها" else "Proforma Invoices"),
                "Installation" to (if (isRtl) "فاکتورهای نصب و اجرا" else "Installation Invoices"),
                "Paid" to (if (isRtl) "تسویه شده" else "Paid"),
                "Pending" to (if (isRtl) "تسویه نشده" else "Unpaid"),
                "PartiallyPaid" to (if (isRtl) "پرداخت ناقص" else "Partially Paid")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { (status, label) ->
                    val isSelected = viewModel.filterStatus == status
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.filterStatus = status },
                        label = { Text(text = label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Invoice List ---
            if (invoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isRtl) "هیچ فاکتوری در این دسته‌بندی یافت نشد" else "No invoices found in this section",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(invoices, key = { it.id }) { invoice ->
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
                            onClick = { onNavigateToInvoiceDetails(invoice.id) },
                            onDelete = { invoiceToDelete = invoice }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Extra margin for FAB
                    }
                }
            }
        }
    }
}
