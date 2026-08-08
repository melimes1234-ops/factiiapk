package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.InvoiceRepository
import com.example.data.repository.InvoiceDetails
import com.example.util.Helper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InvoiceViewModel(private val repository: InvoiceRepository) : ViewModel() {

    // --- Core Database Flows ---
    val settings = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val customers = repository.customers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val products = repository.products.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val invoices = repository.invoices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allLineItems = repository.allLineItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val payments = repository.payments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val logs = repository.logs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bankAccounts = repository.bankAccounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val projects = repository.projects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val colorCodesList: StateFlow<List<String>> = settings.map { s ->
        s?.defaultColorCodes?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf("N1", "N2", "N3", "C1", "C2", "W1", "W2", "G1", "G2")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("N1", "N2", "N3", "C1", "C2", "W1", "W2", "G1", "G2"))

    val surfaceTreatmentsList: StateFlow<List<String>> = settings.map { s ->
        s?.defaultSurfaceTreatments?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf("BR (برس خورده)", "Emboss (طرح چوب)", "Sanded (سنباده)", "Smooth (صیقلی)")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("BR (برس خورده)", "Emboss (طرح چوب)", "Sanded (سنباده)", "Smooth (صیقلی)"))


    // --- UI Local Preferences State ---
    var selectedLanguage by mutableStateOf("fa")
    var useJalaliCalendar by mutableStateOf(true)
    var usePersianDigits by mutableStateOf(true)
    var selectedCurrency by mutableStateOf("تومان")
    var selectedThemeMode by mutableStateOf("light") // "system", "light", "dark"

    init {
        // Observe settings flow to update local states dynamically
        viewModelScope.launch {
            settings.collect { s ->
                if (s != null) {
                    selectedLanguage = s.defaultLanguage
                    useJalaliCalendar = s.useJalaliCalendar
                    usePersianDigits = s.usePersianDigits
                    selectedCurrency = s.defaultCurrency
                    selectedThemeMode = s.themeMode
                }
            }
        }
    }

    // --- Search & Filters ---
    var searchQuery by mutableStateOf("")
    var filterStatus by mutableStateOf("All") // All, Paid, Pending, PartiallyPaid, Proforma, Sales
    var userNotificationMessage by mutableStateOf<String?>(null)

    fun showNotification(msg: String) {
        userNotificationMessage = msg
    }

    fun clearNotification() {
        userNotificationMessage = null
    }

    // --- Filtered Invoices Flow ---
    val filteredInvoices = combine(
        invoices,
        customers,
        snapshotFlow { searchQuery },
        snapshotFlow { filterStatus }
    ) { invoiceList, customerList, query, status ->
        invoiceList.filter { invoice ->
            val customer = customerList.find { it.id == invoice.customerId }
            val customerName = customer?.name ?: ""
            val customerCompany = customer?.company ?: ""
            
            val matchesQuery = query.isBlank() || 
                invoice.invoiceNumber.contains(query, ignoreCase = true) ||
                customerName.contains(query, ignoreCase = true) ||
                customerCompany.contains(query, ignoreCase = true) ||
                invoice.referenceNo.contains(query, ignoreCase = true)

            val matchesStatus = when (status) {
                "All" -> true
                "Paid" -> invoice.status == "Paid"
                "Pending" -> invoice.status == "Pending" || invoice.status == "Overdue"
                "PartiallyPaid" -> invoice.status == "PartiallyPaid"
                "Proforma" -> invoice.invoiceType == "پیش‌فاکتور" || invoice.status == "Draft"
                "Sales" -> (invoice.invoiceType == "فاکتور فروش" || invoice.invoiceType == "فاکتور") && invoice.status != "Draft"
                "Installation" -> invoice.invoiceType.contains("نصب") || invoice.invoiceType.contains("اجرا")
                else -> true
            }

            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Invoice Editor State (Draft) ---
    var editorTemplate by mutableStateOf("Nest") // "General", "Nest"
    var editorInvoiceType by mutableStateOf("پیش‌فاکتور") // "پیش‌فاکتور", "فاکتور فروش"
    var editorInvoiceId by mutableStateOf<Long?>(null)
    var editorInvoiceNumber by mutableStateOf("")
    var editorIssueDate by mutableStateOf(System.currentTimeMillis())
    var editorDueDate by mutableStateOf(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
    var editorReferenceNo by mutableStateOf("")
    var editorPoNumber by mutableStateOf("")
    var editorProjectNumber by mutableStateOf("")
    var editorSalesperson by mutableStateOf("")
    var editorSupportPerson by mutableStateOf("")
    var editorCustomerId by mutableStateOf<Long?>(null)
    var editorNotes by mutableStateOf("")
    var editorShipping by mutableStateOf(0.0)
    var editorHandling by mutableStateOf(0.0)
    var editorDiscountType by mutableStateOf("Percent") // "Percent" or "Amount"
    var editorDiscountRate by mutableStateOf(0.0) // percentage
    var editorDiscountAmount by mutableStateOf(0.0) // flat
    var editorTaxRate by mutableStateOf(0.0) // percentage
    var editorTaxType by mutableStateOf("Exclusive") // Inclusive, Exclusive
    var editorAdvancePayment by mutableStateOf(0.0)
    var editorStatus by mutableStateOf("Pending")
    var editorPaymentMethod by mutableStateOf("نقد")
    var editorPaymentDetails by mutableStateOf("")
    var editorPaymentDocumentPaths by mutableStateOf("")
    var editorPaymentTerms by mutableStateOf("")
    var editorShippingTerms by mutableStateOf("")
    var editorBankAccountId by mutableStateOf<Long>(0L)
    val editorLineItems = mutableStateListOf<InvoiceLineItem>()

    // --- Initialize Editor with New Invoice ---
    fun startNewInvoice() {
        viewModelScope.launch {
            val s = repository.getSettingsDirect()
            val defaultBank = repository.getDefaultBankAccountDirect()
            val latestInvoice = repository.getLatestInvoiceDirect()
            val defaultInvNumber = if (latestInvoice != null && latestInvoice.invoiceNumber.isNotBlank()) {
                Helper.getNextInvoiceNumber(latestInvoice.invoiceNumber, s.invoiceNumberPrefix, s.autoIncrementNumber)
            } else {
                "${s.invoiceNumberPrefix}${s.autoIncrementNumber}"
            }

            editorInvoiceId = null
            editorTemplate = "Nest"
            editorInvoiceType = "پیش‌فاکتور"
            editorInvoiceNumber = defaultInvNumber
            editorIssueDate = System.currentTimeMillis()
            editorDueDate = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
            editorReferenceNo = ""
            editorPoNumber = ""
            editorProjectNumber = ""
            editorSalesperson = latestInvoice?.salesperson ?: ""
            editorSupportPerson = latestInvoice?.supportPerson ?: ""
            editorCustomerId = null
            editorNotes = ""
            editorShipping = 0.0
            editorHandling = 0.0
            editorDiscountType = "Percent"
            editorDiscountRate = 0.0
            editorDiscountAmount = 0.0
            editorTaxRate = 10.0 // Standard VAT in Iran (10%)
            editorTaxType = "Exclusive"
            editorAdvancePayment = 0.0
            editorStatus = "Pending"
            editorPaymentMethod = "نقد"
            editorPaymentDetails = ""
            editorPaymentDocumentPaths = ""
            editorPaymentTerms = s.defaultPaymentTerms
            editorShippingTerms = s.defaultShippingTerms
            editorBankAccountId = defaultBank?.id ?: 0L
            editorLineItems.clear()
            // Add initial empty item
            editorLineItems.add(InvoiceLineItem(invoiceId = 0, name = "", quantity = 1.0, unitPrice = 0.0))
        }
    }

    // --- Initialize Editor with Existing Invoice ---
    fun startEditInvoice(invoiceId: Long) {
        viewModelScope.launch {
            val s = repository.getSettingsDirect()
            repository.getInvoiceDetails(invoiceId).firstOrNull()?.let { details ->
                editorInvoiceId = details.invoice.id
                editorTemplate = if (details.invoice.template.isBlank() || details.invoice.template == "General") "Nest" else details.invoice.template
                editorInvoiceType = if (details.invoice.invoiceType.isNotEmpty()) details.invoice.invoiceType else "پیش‌فاکتور"
                editorInvoiceNumber = details.invoice.invoiceNumber
                editorIssueDate = details.invoice.issueDate
                editorDueDate = details.invoice.dueDate
                editorReferenceNo = details.invoice.referenceNo
                editorPoNumber = details.invoice.poNumber
                editorProjectNumber = details.invoice.projectNumber
                editorSalesperson = details.invoice.salesperson
                editorSupportPerson = details.invoice.supportPerson
                editorCustomerId = details.invoice.customerId
                editorNotes = details.invoice.notes
                editorShipping = details.invoice.shipping
                editorHandling = details.invoice.handling
                editorDiscountType = if (details.invoice.discountType.isNotBlank()) details.invoice.discountType else "Percent"
                editorDiscountRate = details.invoice.discountRate
                editorDiscountAmount = details.invoice.discountAmount
                editorTaxRate = details.invoice.taxRate
                editorTaxType = details.invoice.taxType
                editorAdvancePayment = details.invoice.advancePayment
                editorStatus = details.invoice.status
                editorPaymentMethod = details.invoice.paymentMethod
                editorPaymentDetails = details.invoice.paymentDetails
                editorPaymentDocumentPaths = details.invoice.paymentDocumentPaths
                editorPaymentTerms = if (details.invoice.paymentTerms.isNotBlank()) details.invoice.paymentTerms else s.defaultPaymentTerms
                editorShippingTerms = if (details.invoice.shippingTerms.isNotBlank()) details.invoice.shippingTerms else s.defaultShippingTerms
                editorBankAccountId = details.invoice.bankAccountId
                editorLineItems.clear()
                val sanitizedLineItems = details.lineItems.map { rawItem ->
                    var item = rawItem
                    val isWood = item.categoryType == "Wood" || item.categoryType == "چوب پلاست"
                    val isAccessory = item.categoryType == "Accessory" || item.categoryType == "پیچ و کلیپس"
                    if (isWood) {
                        var factor = item.crossSectionFactor
                        val preset = WoodPresets.findPresetByNameOrSku(item.name) ?: WoodPresets.findPresetByNameOrSku(item.sku)
                        if (factor <= 0.0 || (factor == 1.0 && preset != null && preset.crossSectionFactor != 1.0)) {
                            factor = preset?.crossSectionFactor ?: 7.1428
                        }
                        var branch = item.branchCount
                        var qty = item.quantity
                        var initialArea = item.initialAreaSqm

                        if (branch <= 0.0 && qty > 0.0) {
                            branch = kotlin.math.ceil(qty / 3.0)
                        }
                        if (qty <= 0.0 && branch > 0.0) {
                            qty = branch * 3.0
                        }
                        if (initialArea <= 0.0 && qty > 0.0 && factor > 0.0) {
                            initialArea = qty / factor
                        }
                        item = item.copy(
                            unit = "متر طول",
                            crossSectionFactor = factor,
                            branchCount = branch,
                            quantity = qty,
                            initialAreaSqm = initialArea
                        )
                    } else if (isAccessory) {
                        if (item.unit.isBlank()) {
                            val preset = AccessoryPresets.findPresetByNameOrSku(item.name) ?: AccessoryPresets.findPresetByNameOrSku(item.sku)
                            item = item.copy(unit = preset?.unit ?: "قطعه")
                        }
                    }
                    if (item.weight <= 0.0) {
                        val dbProd = products.value.find { 
                            (it.name.isNotBlank() && item.name.isNotBlank() && it.name.trim().equals(item.name.trim(), ignoreCase = true)) ||
                            (it.sku.isNotBlank() && item.sku.isNotBlank() && it.sku.trim().equals(item.sku.trim(), ignoreCase = true))
                        }
                        if (dbProd != null && dbProd.weight > 0.0) {
                            item = item.copy(weight = dbProd.weight)
                        } else {
                            val preset = WoodPresets.findPresetByNameOrSku(item.name) ?: WoodPresets.findPresetByNameOrSku(item.sku)
                            if (preset != null && preset.defaultWeight > 0.0) {
                                item = item.copy(weight = preset.defaultWeight)
                            }
                        }
                    }
                    item
                }
                editorLineItems.addAll(sanitizedLineItems)
            }
        }
    }

    val cabinetDefaultTermsList = listOf(
        "نقدی",
        "این شرکت هیچگونه مسئولیتی در قبال روکش اچ پی ال که خریدار بر روی سطح صفحه کابینت اعمال میکند نخواهد داشت .",
        "لطفا در انتخاب محصول و تعداد مورد نیاز دقت لازم را مبذول بفرمایید .",
        "در صورت ارایه سفارش ، شروع تولید پس از واریزمبلغ پیش پرداخت و دریافت رسید واریز و ارائه تاییدیه مالی شرکت می باشد .",
        "تایید پیش فاکتور و واریز پیش پرداخت به منزله قبول شرایط و خرید قطعی کالا از طرف خریدار بوده و در صورت انصراف مشتری خسارات احتمالی برآورد و دریافت می گردد .",
        "تمامی هزینه های حمل از محل کارخانه در ابهر تا محل خریدار پروژه به عهده خریدار می باشد .",
        "زمان تحویل کالا در صورت موجود نبودن در انبار با هماهنگی به خریدار اعلام خواهد شد .",
        "ارسال بار منوط به تسویه حساب مالی کامل فاکتور و واریز مبلغ مورد نظر به حساب اعلام شده می باشد .",
        "لطفا مبلغ فاکتور را به حساب شماره :\nبانک سپه - به نام محسن نیک زارع\nIR580150000188480276086691"
    )

    fun applyCabinetDefaultTerms() {
        editorPaymentTerms = "نقدی"
        editorShippingTerms = "پس از تسویه کامل"
        editorNotes = cabinetDefaultTermsList.mapIndexed { i, term ->
            "${Helper.formatDouble((i + 1).toDouble(), usePersianDigits)}- $term"
        }.joinToString("\n")
    }

    fun addLineItem(product: Product? = null) {
        if (product != null) {
            val isWoodProd = product.categoryType == "Wood" || product.categoryType == "چوب پلاست"
            val isCabProd = product.categoryType == "Cabinet" || product.categoryType == "کابینت"
            val targetUnit = if (isWoodProd) "متر طول" else if (isCabProd) "عدد" else (if (product.unit.isNotBlank()) product.unit else "قطعه")
            val factor = if (product.crossSectionFactor > 0) product.crossSectionFactor else (WoodPresets.findPresetByNameOrSku(product.name)?.crossSectionFactor ?: 0.0)
            val defaultW = if (product.weight > 0) product.weight else (WoodPresets.findPresetByNameOrSku(product.name)?.defaultWeight ?: 0.0)
            val catType = if (isWoodProd) "Wood" else if (isCabProd) "Cabinet" else "Accessory"
            editorLineItems.add(
                InvoiceLineItem(
                    invoiceId = editorInvoiceId ?: 0,
                    sku = product.sku,
                    name = product.name,
                    description = product.description,
                    quantity = if (isWoodProd && product.branchCount > 0) product.branchCount * 3.0 else 1.0,
                    unit = targetUnit,
                    unitPrice = product.price,
                    taxPercent = 0.0,
                    colorCode = product.colorCode,
                    surfaceTreatment = product.surfaceTreatment,
                    branchCount = product.branchCount,
                    categoryType = catType,
                    crossSectionFactor = factor,
                    initialAreaSqm = product.initialAreaSqm,
                    weight = defaultW
                )
            )
        } else {
            editorLineItems.add(
                InvoiceLineItem(
                    invoiceId = editorInvoiceId ?: 0,
                    name = "",
                    quantity = 3.0,
                    unitPrice = 0.0,
                    unit = "متر طول",
                    colorCode = "N3",
                    surfaceTreatment = "BR",
                    branchCount = 1.0,
                    categoryType = "Wood",
                    crossSectionFactor = 7.1428
                )
            )
        }
    }

    fun removeLineItem(index: Int) {
        if (index in editorLineItems.indices) {
            if (editorLineItems.size > 1) {
                editorLineItems.removeAt(index)
            } else {
                // If it is the only line item, reset it to empty item
                editorLineItems[0] = InvoiceLineItem(invoiceId = 0, name = "", quantity = 1.0, unitPrice = 0.0)
            }
        }
    }

    fun duplicateLineItem(index: Int) {
        val original = editorLineItems[index]
        editorLineItems.add(index + 1, original.copy(id = 0))
    }

    fun updateLineItem(index: Int, updated: InvoiceLineItem) {
        if (updated.taxPercent > 0.0 && editorTaxRate > 0.0) {
            editorTaxRate = 0.0
        }
        if (index in editorLineItems.indices) {
            editorLineItems[index] = updated
        }
    }

    fun getItemGrossPrice(item: InvoiceLineItem): Double {
        val isInstallation = item.categoryType == "Installation" || item.categoryType == "نصب"
        return if (isInstallation) {
            val qty = if (item.quantity > 0) item.quantity else (if (item.branchCount > 0) item.branchCount else 1.0)
            (qty * item.unitPrice) + item.accommodationCost + item.transportationCost + item.consumablesCost
        } else {
            item.quantity * item.unitPrice
        }
    }

    // --- Computed Draft Totals ---
    fun getDraftSubtotal(): Double {
        return editorLineItems.sumOf { item ->
            val gross = getItemGrossPrice(item)
            val itemDisc = if (item.discountAmount > 0) item.discountAmount else gross * (item.discountPercent / 100.0)
            (gross - itemDisc).coerceAtLeast(0.0)
        }
    }

    fun getDraftDiscountVal(): Double {
        val subtotal = getDraftSubtotal()
        return if (editorDiscountType == "Amount") {
            editorDiscountAmount
        } else {
            subtotal * (editorDiscountRate / 100.0)
        }
    }

    fun setOverallTaxRate(rate: Double) {
        editorTaxRate = rate
        if (rate > 0.0) {
            // When overall invoice tax is enabled, turn OFF tax on all individual line items
            for (i in editorLineItems.indices) {
                if (editorLineItems[i].taxPercent > 0.0) {
                    editorLineItems[i] = editorLineItems[i].copy(taxPercent = 0.0)
                }
            }
        }
    }

    fun getDraftTaxVal(): Double {
        return if (editorTaxRate > 0.0) {
            val subtotal = getDraftSubtotal()
            val discount = getDraftDiscountVal()
            val taxableAmount = (subtotal - discount).coerceAtLeast(0.0)
            taxableAmount * (editorTaxRate / 100.0)
        } else {
            // Sum per-line item taxes
            editorLineItems.sumOf { item ->
                val gross = getItemGrossPrice(item)
                val itemDisc = if (item.discountAmount > 0) item.discountAmount else gross * (item.discountPercent / 100.0)
                val itemNet = (gross - itemDisc).coerceAtLeast(0.0)
                itemNet * (item.taxPercent / 100.0)
            }
        }
    }

    fun getDraftTotal(): Double {
        val subtotal = getDraftSubtotal()
        val discount = getDraftDiscountVal()
        val tax = if (editorTaxType == "Exclusive") getDraftTaxVal() else 0.0
        return (subtotal - discount).coerceAtLeast(0.0) + tax + editorShipping + editorHandling
    }

    fun getDraftOutstanding(): Double {
        return (getDraftTotal() - editorAdvancePayment).coerceAtLeast(0.0)
    }

    fun calculateInvoiceTotal(invoice: Invoice, items: List<InvoiceLineItem>): Double {
        val subtotal = items.sumOf { item ->
            val gross = getItemGrossPrice(item)
            val itemDisc = if (item.discountAmount > 0) item.discountAmount else gross * (item.discountPercent / 100.0)
            (gross - itemDisc).coerceAtLeast(0.0)
        }
        val discountVal = if (invoice.discountType == "Amount") invoice.discountAmount else subtotal * (invoice.discountRate / 100.0)
        val taxable = (subtotal - discountVal).coerceAtLeast(0.0)
        val taxValue = if (invoice.taxType == "Exclusive") {
            if (invoice.taxRate > 0.0) {
                taxable * (invoice.taxRate / 100.0)
            } else {
                items.sumOf { item ->
                    val gross = getItemGrossPrice(item)
                    val itemDisc = if (item.discountAmount > 0) item.discountAmount else gross * (item.discountPercent / 100.0)
                    val itemNet = (gross - itemDisc).coerceAtLeast(0.0)
                    itemNet * (item.taxPercent / 100.0)
                }
            }
        } else 0.0
        return (taxable + taxValue + invoice.shipping + invoice.handling).coerceAtLeast(0.0)
    }

    // --- Save/Update Draft Invoice ---
    fun saveInvoice(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            var targetCustomerId = editorCustomerId
            if (targetCustomerId == null) {
                val existingCusts = repository.customers.firstOrNull() ?: emptyList()
                if (existingCusts.isNotEmpty()) {
                    targetCustomerId = existingCusts.first().id
                } else {
                    val defaultCust = Customer(name = "مشتری عمومی", company = "خریدار")
                    targetCustomerId = repository.insertCustomer(defaultCust)
                }
                editorCustomerId = targetCustomerId
            }

            // Auto-convert "پیش‌فاکتور" to "فاکتور فروش" if status leaves "Pending" (تسویه نشده)
            var finalInvoiceType = editorInvoiceType
            if (editorStatus != "Pending" && finalInvoiceType == "پیش‌فاکتور") {
                finalInvoiceType = "فاکتور فروش"
                editorInvoiceType = "فاکتور فروش"
            }

            val invoice = Invoice(
                id = editorInvoiceId ?: 0,
                invoiceNumber = editorInvoiceNumber,
                invoiceType = finalInvoiceType,
                issueDate = editorIssueDate,
                dueDate = editorDueDate,
                referenceNo = editorReferenceNo,
                poNumber = editorPoNumber,
                projectNumber = editorProjectNumber,
                salesperson = editorSalesperson,
                supportPerson = editorSupportPerson,
                currency = selectedCurrency,
                language = selectedLanguage,
                status = editorStatus,
                template = editorTemplate,
                customerId = targetCustomerId,
                notes = editorNotes,
                shipping = editorShipping,
                handling = editorHandling,
                discountRate = editorDiscountRate,
                discountAmount = editorDiscountAmount,
                discountType = editorDiscountType,
                taxRate = editorTaxRate,
                taxType = editorTaxType,
                advancePayment = editorAdvancePayment,
                paymentMethod = editorPaymentMethod,
                paymentDetails = editorPaymentDetails,
                paymentDocumentPaths = editorPaymentDocumentPaths,
                paymentTerms = editorPaymentTerms,
                shippingTerms = editorShippingTerms,
                bankAccountId = editorBankAccountId
            )

            if (editorProjectNumber.isNotBlank()) {
                val custName = customers.value.find { it.id == targetCustomerId }?.name ?: ""
                repository.ensureProjectExists(editorProjectNumber, targetCustomerId, custName)
            }

            val savedId: Long
            if (editorInvoiceId == null) {

                savedId = repository.createInvoice(invoice, editorLineItems.toList())
                val s = repository.getSettingsDirect()
                val nextNoStr = Helper.getNextInvoiceNumber(editorInvoiceNumber, s.invoiceNumberPrefix, s.autoIncrementNumber)
                val nextInt = Helper.toEnglishDigits(nextNoStr).filter { it.isDigit() }.toIntOrNull() ?: (s.autoIncrementNumber + 1)
                repository.saveSettings(s.copy(autoIncrementNumber = nextInt))
                showNotification("فاکتور شماره ${invoice.invoiceNumber} با موفقیت ایجاد شد")
            } else {
                savedId = invoice.id
                repository.updateInvoice(invoice, editorLineItems.toList())
                showNotification("فاکتور شماره ${invoice.invoiceNumber} بروزرسانی گردید")
            }
            onSuccess(savedId)
        }
    }

    // --- Customer Operations ---
    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            if (customer.id == 0L) {
                repository.insertCustomer(customer)
                showNotification("مشتری ${customer.name} با موفقیت افزوده شد")
            } else {
                repository.updateCustomer(customer)
                showNotification("اطلاعات مشتری ${customer.name} بروزرسانی شد")
            }
        }
    }

    fun saveCustomerAndSelect(customer: Customer, onSelected: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertCustomer(customer)
            editorCustomerId = id
            showNotification("خریدار جدید (${customer.name}) ذخیره و انتخاب شد")
            onSelected(id)
        }
    }

    // --- Project Operations ---
    fun saveProject(project: Project) {
        viewModelScope.launch {
            if (project.id == 0L) {
                repository.insertProject(project)
                showNotification("پروژه '${project.name}' با موفقیت ثبت گردید")
            } else {
                repository.updateProject(project)
                showNotification("اطلاعات پروژه '${project.name}' بروزرسانی شد")
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
            showNotification("پروژه '${project.name}' حذف شد")
        }
    }


    fun bulkInsertCustomers(customers: List<Customer>, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            var count = 0
            customers.forEach { c ->
                repository.insertCustomer(c)
                count++
            }
            onComplete(count)
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    // --- Product Operations ---
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            if (product.id == 0L) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
        }
    }

    fun bulkInsertProducts(products: List<Product>, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            var count = 0
            products.forEach { p ->
                repository.insertProduct(p)
                count++
            }
            onComplete(count)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // --- Delete Invoice ---
    fun deleteInvoice(invoice: Invoice, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
            onSuccess()
        }
    }

    // --- Record Payment ---
    fun logAction(action: String, details: String) {
        viewModelScope.launch {
            repository.addAuditLog(null, action, details)
        }
    }

    fun addPayment(payment: Payment) {
        viewModelScope.launch {
            repository.recordPayment(payment)

            // Update status of Invoice dynamically based on payments
            repository.getInvoiceByIdDirect(payment.invoiceId)?.let { invoice ->
                val invoiceDetails = repository.getInvoiceDetailsDirect(payment.invoiceId)
                val items = invoiceDetails?.lineItems ?: emptyList()
                val total = calculateInvoiceTotal(invoice, items)
                
                val existingPayments = repository.getPaymentsForInvoice(payment.invoiceId).firstOrNull() ?: emptyList()
                val totalPaid = existingPayments.sumOf { it.amount }
                
                val newStatus = when {
                    totalPaid >= total -> "Paid"
                    totalPaid > 0.0 -> "PartiallyPaid"
                    else -> "Pending"
                }

                val newType = if (newStatus == "Paid" && invoice.invoiceType == "پیش‌فاکتور") "فاکتور فروش" else invoice.invoiceType
                repository.updateInvoice(invoice.copy(status = newStatus, invoiceType = newType), items)
                showNotification("پرداخت به مبلغ ${Helper.formatCurrency(payment.amount, "تومان", true)} ثبت گردید")
            }
        }
    }

    fun addMultiplePayments(paymentsList: List<Payment>) {
        viewModelScope.launch {
            if (paymentsList.isEmpty()) return@launch
            val invId = paymentsList.first().invoiceId
            paymentsList.forEach { p ->
                repository.recordPayment(p)
            }

            repository.getInvoiceByIdDirect(invId)?.let { invoice ->
                val invoiceDetails = repository.getInvoiceDetailsDirect(invId)
                val items = invoiceDetails?.lineItems ?: emptyList()
                val total = calculateInvoiceTotal(invoice, items)
                
                val existingPayments = repository.getPaymentsForInvoice(invId).firstOrNull() ?: emptyList()
                val totalPaid = existingPayments.sumOf { it.amount }
                
                val newStatus = when {
                    totalPaid >= total -> "Paid"
                    totalPaid > 0.0 -> "PartiallyPaid"
                    else -> "Pending"
                }

                val newType = if (newStatus == "Paid" && invoice.invoiceType == "پیش‌فاکتور") "فاکتور فروش" else invoice.invoiceType
                repository.updateInvoice(invoice.copy(status = newStatus, invoiceType = newType), items)
                showNotification("${paymentsList.size} فقره پرداخت / چک ثبت شد")
            }
        }
    }

    // --- Get Invoice Details Flow ---
    fun updateInvoicePaymentDocuments(invoiceId: Long, documentPaths: String) {
        viewModelScope.launch {
            repository.getInvoiceByIdDirect(invoiceId)?.let { invoice ->
                val details = repository.getInvoiceDetailsDirect(invoiceId)
                val items = details?.lineItems ?: emptyList()
                repository.updateInvoice(invoice.copy(paymentDocumentPaths = documentPaths), items)
                showNotification("مستندات پرداخت با موفقیت ثبت و به روز شد")
            }
        }
    }

    fun getInvoiceDetails(invoiceId: Long): Flow<InvoiceDetails?> {
        return repository.getInvoiceDetails(invoiceId)
    }

    // --- Global Application State Settings Saver ---
    fun saveAppSettings(s: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(s)
            selectedLanguage = s.defaultLanguage
            useJalaliCalendar = s.useJalaliCalendar
            usePersianDigits = s.usePersianDigits
            selectedCurrency = s.defaultCurrency
            selectedThemeMode = s.themeMode
            showNotification("تنظیمات سیستم با موفقیت ذخیره شد")
        }
    }

    fun toggleThemeMode() {
        viewModelScope.launch {
            val currentSettings = settings.value ?: AppSettings()
            val newMode = if (selectedThemeMode == "dark") "light" else "dark"
            val updated = currentSettings.copy(themeMode = newMode)
            repository.saveSettings(updated)
            selectedThemeMode = newMode
        }
    }

    // --- Dashboard Analytics Calculations ---
    val dashboardStats = combine(
        invoices,
        payments,
        customers,
        products,
        allLineItems
    ) { invoiceList, paymentList, customerList, productList, lineItemList ->
        if (invoiceList.isEmpty()) {
            return@combine DashboardStats(
                topCustomersCount = customerList.size,
                topProductsCount = productList.size
            )
        }

        val validInvoiceIds = invoiceList.map { it.id }.toSet()
        val validPayments = paymentList.filter { p -> p.invoiceId in validInvoiceIds }
        val validLineItems = lineItemList.filter { item -> item.invoiceId in validInvoiceIds }
        val itemsByInvoice = validLineItems.groupBy { it.invoiceId }

        var totalRevenue = validPayments.sumOf { it.amount }
        val paidInvoices = invoiceList.filter { it.status == "Paid" }
        val paidInvoicesSum = paidInvoices.sumOf { inv ->
            val items = itemsByInvoice[inv.id] ?: emptyList()
            calculateInvoiceTotal(inv, items)
        }
        if (totalRevenue < paidInvoicesSum) {
            totalRevenue = paidInvoicesSum
        }

        val paidCount = paidInvoices.size

        // Proforma Invoices (پیش‌فاکتورها)
        val proformaInvoices = invoiceList.filter { it.invoiceType == "پیش‌فاکتور" || it.status == "Draft" }
        val proformaCount = proformaInvoices.size
        val proformaAmount = proformaInvoices.sumOf { inv ->
            val items = itemsByInvoice[inv.id] ?: emptyList()
            calculateInvoiceTotal(inv, items)
        }

        // Sales Invoices Pending (در انتظار پرداخت) - ONLY for Sales Invoices (فاکتور فروش)
        val salesInvoices = invoiceList.filter { (it.invoiceType == "فاکتور فروش" || it.invoiceType.isEmpty()) && it.status != "Draft" && it.status != "Cancelled" }
        var pendingSum = 0.0
        var overdueSum = 0.0
        var totalTaxAndVat = 0.0

        salesInvoices.forEach { invoice ->
            val items = itemsByInvoice[invoice.id] ?: emptyList()
            val total = calculateInvoiceTotal(invoice, items)
            val subtotal = items.sumOf { (it.quantity * it.unitPrice) * (1.0 - it.discountPercent / 100.0) }
            val taxVal = subtotal * (invoice.taxRate / 100.0)
            totalTaxAndVat += taxVal

            val paidForThisInvoice = validPayments.filter { it.invoiceId == invoice.id }.sumOf { it.amount }
            val remaining = (total - paidForThisInvoice).coerceAtLeast(0.0)

            when (invoice.status) {
                "Pending", "PartiallyPaid" -> pendingSum += remaining
                "Overdue" -> overdueSum += remaining
            }
        }

        // Monthly Sales grouped
        val monthlySales = mutableMapOf<String, Double>()
        if (validPayments.isNotEmpty()) {
            validPayments.forEach { payment ->
                val dateStr = Helper.formatJalaliShort(payment.date, false).take(7)
                monthlySales[dateStr] = (monthlySales[dateStr] ?: 0.0) + payment.amount
            }
        } else {
            salesInvoices.forEach { invoice ->
                val items = itemsByInvoice[invoice.id] ?: emptyList()
                val total = calculateInvoiceTotal(invoice, items)
                val dateStr = Helper.formatJalaliShort(invoice.issueDate, false).take(7)
                monthlySales[dateStr] = (monthlySales[dateStr] ?: 0.0) + total
            }
        }

        // Line Items breakdown for Sales Invoices ONLY
        val salesInvoiceIds = salesInvoices.map { it.id }.toSet()
        val salesLineItems = validLineItems.filter { it.invoiceId in salesInvoiceIds }

        var woodGross = 0.0; var woodDiscount = 0.0; var woodNet = 0.0; var woodItems = 0
        var accGross = 0.0; var accDiscount = 0.0; var accNet = 0.0; var accItems = 0
        var instGross = 0.0; var instDiscount = 0.0; var instNet = 0.0; var instItems = 0
        var othGross = 0.0; var othDiscount = 0.0; var othNet = 0.0; var othItems = 0

        val categoryTotals = mutableMapOf<String, Double>()
        val salesInvoiceMap = salesInvoices.associateBy { it.id }
        val customerMap = customerList.associateBy { it.id }

        val woodDetailsList = mutableListOf<CategoryItemDetail>()
        val accessoryDetailsList = mutableListOf<CategoryItemDetail>()
        val installationDetailsList = mutableListOf<CategoryItemDetail>()
        val otherDetailsList = mutableListOf<CategoryItemDetail>()

        salesLineItems.forEach { item ->
            val gross = item.quantity * item.unitPrice
            val disc = gross * (item.discountPercent / 100.0)
            val net = gross - disc

            val inv = salesInvoiceMap[item.invoiceId]
            val invNum = inv?.invoiceNumber ?: "-"
            val custName = customerMap[inv?.customerId]?.name ?: "مشتری"
            val invDate = inv?.issueDate ?: 0L

            val isInst = item.categoryType == "Installation" || item.categoryType == "نصب" || item.categoryType == "خدمات" || item.categoryType == "Services" ||
                         item.name.contains("نصب") || item.name.contains("اجرا") || item.name.contains("زیرسازی") || item.name.contains("دستمزد") || item.name.contains("حمل")

            val isAcc = item.categoryType == "Accessory" || item.categoryType == "پیچ و کلیپس" ||
                        item.name.contains("کلیپس") || item.name.contains("پیچ") || item.name.contains("کلیک") || item.name.contains("اسکوتیا") || item.name.contains("HCL") || item.name.contains("SCL") || item.name.contains("TCL")

            val isWood = item.categoryType == "Wood" || item.categoryType == "WoodProfile" || item.categoryType == "چوب پلاست" || 
                         item.name.contains("چوب") || item.name.contains("پروفیل") || item.name.contains("دک") || item.name.contains("نما") || item.name.contains("تایل") || item.name.contains("لوور") || item.name.contains("نرده") ||
                         (!isInst && !isAcc)

            val detail = CategoryItemDetail(
                itemName = item.name,
                sku = item.sku,
                invoiceNumber = invNum,
                customerName = custName,
                quantity = item.quantity,
                unit = item.unit.ifBlank { "عدد" },
                unitPrice = item.unitPrice,
                discountPercent = item.discountPercent,
                netAmount = net,
                categoryName = when {
                    isInst -> "خدمات نصب و اجرا"
                    isAcc -> "پیچ و کلیپس"
                    isWood -> "چوب پلاست"
                    else -> "سایر"
                },
                invoiceDate = invDate
            )

            when {
                isInst -> {
                    instGross += gross; instDiscount += disc; instNet += net; instItems += item.quantity.toInt().coerceAtLeast(1)
                    installationDetailsList.add(detail)
                    categoryTotals["خدمات نصب و اجرا"] = (categoryTotals["خدمات نصب و اجرا"] ?: 0.0) + net
                }
                isAcc -> {
                    accGross += gross; accDiscount += disc; accNet += net; accItems += item.quantity.toInt().coerceAtLeast(1)
                    accessoryDetailsList.add(detail)
                    categoryTotals["پیچ و کلیپس"] = (categoryTotals["پیچ و کلیپس"] ?: 0.0) + net
                }
                isWood -> {
                    woodGross += gross; woodDiscount += disc; woodNet += net; woodItems += item.quantity.toInt().coerceAtLeast(1)
                    woodDetailsList.add(detail)
                    categoryTotals["چوب پلاست"] = (categoryTotals["چوب پلاست"] ?: 0.0) + net
                }
                else -> {
                    othGross += gross; othDiscount += disc; othNet += net; othItems += item.quantity.toInt().coerceAtLeast(1)
                    otherDetailsList.add(detail)
                    categoryTotals["سایر کالاها و خدمات"] = (categoryTotals["سایر کالاها و خدمات"] ?: 0.0) + net
                }
            }
        }

        val grandGross = woodGross + accGross + instGross + othGross
        val grandDiscount = woodDiscount + accDiscount + instDiscount + othDiscount
        val grandNet = woodNet + accNet + instNet + othNet
        val grandItems = woodItems + accItems + instItems + othItems

        val grandForPct = grandNet.coerceAtLeast(1.0)
        val woodPct = ((woodNet / grandForPct) * 100).toFloat()
        val accPct = ((accNet / grandForPct) * 100).toFloat()
        val instPct = ((instNet / grandForPct) * 100).toFloat()
        val othPct = ((othNet / grandForPct) * 100).toFloat()

        // Top Selling Products
        val topProducts = salesLineItems.groupBy { it.name.trim() }
            .map { (name, items) ->
                val qty = items.sumOf { it.quantity }
                val rev = items.sumOf { (it.quantity * it.unitPrice) * (1.0 - it.discountPercent / 100.0) }
                val sku = items.firstOrNull()?.sku ?: ""
                val cat = items.firstOrNull()?.categoryType ?: ""
                TopProductReport(name = name, sku = sku, category = cat, totalQuantity = qty, totalRevenue = rev)
            }
            .sortedByDescending { it.totalRevenue }
            .take(5)

        // Top Customers
        val topCustomers = salesInvoices.groupBy { it.customerId }
            .mapNotNull { (custId, invs) ->
                val cust = customerMap[custId] ?: return@mapNotNull null
                val totalSpent = invs.sumOf { calculateInvoiceTotal(it, itemsByInvoice[it.id] ?: emptyList()) }
                val totalPaid = invs.sumOf { inv -> validPayments.filter { it.invoiceId == inv.id }.sumOf { it.amount } }
                TopCustomerReport(
                    name = cust.name,
                    company = cust.company,
                    totalInvoicesCount = invs.size,
                    totalSpent = totalSpent,
                    totalPaid = totalPaid
                )
            }
            .sortedByDescending { it.totalSpent }
            .take(5)

        DashboardStats(
            revenue = totalRevenue,
            paidCount = paidCount,
            pendingAmount = pendingSum,
            overdueAmount = overdueSum,
            draftsCount = proformaCount,
            proformaCount = proformaCount,
            proformaAmount = proformaAmount,
            monthlyRevenueMap = monthlySales,
            categoryTotalsMap = categoryTotals,
            topCustomersCount = customerList.size,
            topProductsCount = productList.size,
            woodGross = woodGross,
            woodDiscount = woodDiscount,
            woodNet = woodNet,
            woodItemCount = woodItems,
            woodPct = woodPct,
            accessoryGross = accGross,
            accessoryDiscount = accDiscount,
            accessoryNet = accNet,
            accessoryItemCount = accItems,
            accessoryPct = accPct,
            installationGross = instGross,
            installationDiscount = instDiscount,
            installationNet = instNet,
            installationItemCount = instItems,
            installationPct = instPct,
            otherGross = othGross,
            otherDiscount = othDiscount,
            otherNet = othNet,
            otherItemCount = othItems,
            otherPct = othPct,
            grandGross = grandGross,
            grandDiscount = grandDiscount,
            grandNet = grandNet,
            grandItemCount = grandItems,
            totalTaxAndVat = totalTaxAndVat,
            topSellingProducts = topProducts,
            topSpendingCustomers = topCustomers,
            woodDetails = woodDetailsList,
            accessoryDetails = accessoryDetailsList,
            installationDetails = installationDetailsList,
            otherDetails = otherDetailsList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    // --- Bank Account Management Methods ---
    fun insertBankAccount(account: BankAccount, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertBankAccount(account)
            onSuccess()
        }
    }

    fun updateBankAccount(account: BankAccount, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateBankAccount(account)
            onSuccess()
        }
    }

    fun deleteBankAccount(account: BankAccount) {
        viewModelScope.launch {
            repository.deleteBankAccount(account)
        }
    }

    fun setDefaultBankAccount(account: BankAccount) {
        viewModelScope.launch {
            repository.setDefaultBankAccount(account)
        }
    }
}

data class CategoryItemDetail(
    val itemName: String = "",
    val sku: String = "",
    val invoiceNumber: String = "",
    val customerName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    val unitPrice: Double = 0.0,
    val discountPercent: Double = 0.0,
    val netAmount: Double = 0.0,
    val categoryName: String = "",
    val invoiceDate: Long = 0L
)

data class TopProductReport(
    val name: String,
    val sku: String,
    val category: String,
    val totalQuantity: Double,
    val totalRevenue: Double
)

data class TopCustomerReport(
    val name: String,
    val company: String,
    val totalInvoicesCount: Int,
    val totalSpent: Double,
    val totalPaid: Double
)

data class DashboardStats(
    val revenue: Double = 0.0,
    val paidCount: Int = 0,
    val pendingAmount: Double = 0.0,
    val overdueAmount: Double = 0.0,
    val draftsCount: Int = 0,
    val proformaCount: Int = 0,
    val proformaAmount: Double = 0.0,
    val monthlyRevenueMap: Map<String, Double> = emptyMap(),
    val categoryTotalsMap: Map<String, Double> = emptyMap(),
    val topCustomersCount: Int = 0,
    val topProductsCount: Int = 0,

    // Category Breakdown Fields (چوب پلاست، پیچ و کلیپس، نصب و اجرا، سایر)
    val woodGross: Double = 0.0,
    val woodDiscount: Double = 0.0,
    val woodNet: Double = 0.0,
    val woodItemCount: Int = 0,
    val woodPct: Float = 0f,

    val accessoryGross: Double = 0.0,
    val accessoryDiscount: Double = 0.0,
    val accessoryNet: Double = 0.0,
    val accessoryItemCount: Int = 0,
    val accessoryPct: Float = 0f,

    val installationGross: Double = 0.0,
    val installationDiscount: Double = 0.0,
    val installationNet: Double = 0.0,
    val installationItemCount: Int = 0,
    val installationPct: Float = 0f,

    val otherGross: Double = 0.0,
    val otherDiscount: Double = 0.0,
    val otherNet: Double = 0.0,
    val otherItemCount: Int = 0,
    val otherPct: Float = 0f,

    val grandGross: Double = 0.0,
    val grandDiscount: Double = 0.0,
    val grandNet: Double = 0.0,
    val grandItemCount: Int = 0,

    val totalTaxAndVat: Double = 0.0,
    val topSellingProducts: List<TopProductReport> = emptyList(),
    val topSpendingCustomers: List<TopCustomerReport> = emptyList(),

    val woodDetails: List<CategoryItemDetail> = emptyList(),
    val accessoryDetails: List<CategoryItemDetail> = emptyList(),
    val installationDetails: List<CategoryItemDetail> = emptyList(),
    val otherDetails: List<CategoryItemDetail> = emptyList()
)

class InvoiceViewModelFactory(private val repository: InvoiceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InvoiceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InvoiceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
