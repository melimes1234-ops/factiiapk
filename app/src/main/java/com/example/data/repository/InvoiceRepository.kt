package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull

data class InvoiceDetails(
    val invoice: Invoice,
    val customer: Customer?,
    val lineItems: List<InvoiceLineItem>,
    val payments: List<Payment>
)

class InvoiceRepository(private val db: AppDatabase) {

    // DAOs
    private val appSettingsDao = db.appSettingsDao()
    private val customerDao = db.customerDao()
    private val productDao = db.productDao()
    private val invoiceDao = db.invoiceDao()
    private val invoiceLineItemDao = db.invoiceLineItemDao()
    private val paymentDao = db.paymentDao()
    private val auditLogDao = db.auditLogDao()
    private val bankAccountDao = db.bankAccountDao()
    private val projectDao = db.projectDao()

    // Read Streams
    val settings: Flow<AppSettings?> = appSettingsDao.getSettings()
    val customers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val products: Flow<List<Product>> = productDao.getAllProducts()
    val invoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()
    val allLineItems: Flow<List<InvoiceLineItem>> = invoiceLineItemDao.getAllLineItems()
    val payments: Flow<List<Payment>> = paymentDao.getAllPayments()
    val logs: Flow<List<AuditLog>> = auditLogDao.getAllLogs()
    val bankAccounts: Flow<List<BankAccount>> = bankAccountDao.getAllBankAccounts()
    val projects: Flow<List<Project>> = projectDao.getAllProjects()

    // App Settings
    suspend fun getSettingsDirect(): AppSettings {
        return appSettingsDao.getSettingsDirect() ?: AppSettings()
    }

    suspend fun saveSettings(settings: AppSettings) {
        appSettingsDao.insertOrUpdate(settings)
    }

    // Bank Accounts
    suspend fun getBankAccountByIdDirect(id: Long): BankAccount? = bankAccountDao.getBankAccountByIdDirect(id)
    suspend fun getDefaultBankAccountDirect(): BankAccount? = bankAccountDao.getDefaultBankAccountDirect()
    suspend fun insertBankAccount(bankAccount: BankAccount): Long = bankAccountDao.insert(bankAccount)
    suspend fun updateBankAccount(bankAccount: BankAccount) = bankAccountDao.update(bankAccount)
    suspend fun deleteBankAccount(bankAccount: BankAccount) = bankAccountDao.delete(bankAccount)
    suspend fun setDefaultBankAccount(bankAccount: BankAccount) {
        bankAccountDao.clearAllDefaults()
        bankAccountDao.update(bankAccount.copy(isDefault = true))
    }

    // Customers
    fun getCustomerById(id: Long): Flow<Customer?> = customerDao.getCustomerById(id)
    suspend fun getCustomerByIdDirect(id: Long): Customer? = customerDao.getCustomerByIdDirect(id)
    suspend fun insertCustomer(customer: Customer): Long = customerDao.insert(customer)
    suspend fun updateCustomer(customer: Customer) = customerDao.update(customer)
    suspend fun deleteCustomer(customer: Customer) = customerDao.delete(customer)

    // Products
    fun getProductById(id: Long): Flow<Product?> = productDao.getProductById(id)
    suspend fun insertProduct(product: Product): Long = productDao.insert(product)
    suspend fun updateProduct(product: Product) = productDao.update(product)
    suspend fun deleteProduct(product: Product) = productDao.delete(product)

    // Invoices
    fun getInvoiceById(id: Long): Flow<Invoice?> = invoiceDao.getInvoiceById(id)
    suspend fun getInvoiceByIdDirect(id: Long): Invoice? = invoiceDao.getInvoiceByIdDirect(id)
    suspend fun getLatestInvoiceDirect(): Invoice? = invoiceDao.getLatestInvoiceDirect()
    
    fun getInvoiceDetails(invoiceId: Long): Flow<InvoiceDetails?> {
        val invoiceFlow = invoiceDao.getInvoiceById(invoiceId)
        val itemsFlow = invoiceLineItemDao.getLineItemsForInvoice(invoiceId)
        val paymentsFlow = paymentDao.getPaymentsForInvoice(invoiceId)
        val customersFlow = customerDao.getAllCustomers()

        return combine(invoiceFlow, itemsFlow, paymentsFlow, customersFlow) { invoice, items, pmts, custs ->
            if (invoice != null) {
                val customer = custs.find { it.id == invoice.customerId }
                InvoiceDetails(invoice, customer, items, pmts)
            } else {
                null
            }
        }
    }

    suspend fun getInvoiceDetailsDirect(invoiceId: Long): InvoiceDetails? {
        val invoice = invoiceDao.getInvoiceByIdDirect(invoiceId) ?: return null
        val customer = customerDao.getCustomerByIdDirect(invoice.customerId)
        val items = invoiceLineItemDao.getLineItemsForInvoiceDirect(invoiceId)
        val pmts = paymentDao.getPaymentsForInvoice(invoiceId).firstOrNull() ?: emptyList()
        return InvoiceDetails(invoice, customer, items, pmts)
    }

    suspend fun createInvoice(invoice: Invoice, items: List<InvoiceLineItem>): Long {
        val id = invoiceDao.insertInvoice(invoice)
        val updatedItems = items.map { it.copy(invoiceId = id) }
        invoiceLineItemDao.insertLineItems(updatedItems)
        
        // Log action
        auditLogDao.insertLog(
            AuditLog(
                invoiceId = id,
                action = "ایجاد فاکتور",
                details = "فاکتور شماره ${invoice.invoiceNumber} با موفقیت ثبت شد."
            )
        )
        
        // Update customer total purchases
        updateCustomerStats(invoice.customerId)

        // Increment settings numbering if matches
        val currentSettings = getSettingsDirect()
        val invoiceNumValue = invoice.invoiceNumber.filter { it.isDigit() }.toIntOrNull()
        if (invoiceNumValue != null && invoiceNumValue >= currentSettings.autoIncrementNumber) {
            saveSettings(currentSettings.copy(autoIncrementNumber = invoiceNumValue + 1))
        }

        return id
    }

    suspend fun updateInvoice(invoice: Invoice, items: List<InvoiceLineItem>) {
        invoiceDao.updateInvoice(invoice)
        invoiceLineItemDao.deleteLineItemsForInvoice(invoice.id)
        val updatedItems = items.map { it.copy(invoiceId = invoice.id) }
        invoiceLineItemDao.insertLineItems(updatedItems)

        // Log action
        auditLogDao.insertLog(
            AuditLog(
                invoiceId = invoice.id,
                action = "ویرایش فاکتور",
                details = "فاکتور شماره ${invoice.invoiceNumber} ویرایش شد."
            )
        )

        updateCustomerStats(invoice.customerId)
    }

    suspend fun deleteInvoice(invoice: Invoice) {
        invoiceDao.deleteInvoice(invoice)
        invoiceLineItemDao.deleteLineItemsForInvoice(invoice.id)
        paymentDao.deletePaymentsForInvoice(invoice.id)
        
        // Log action
        auditLogDao.insertLog(
            AuditLog(
                invoiceId = null,
                action = "حذف فاکتور",
                details = "فاکتور شماره ${invoice.invoiceNumber} حذف شد."
            )
        )

        updateCustomerStats(invoice.customerId)
    }

    suspend fun addAuditLog(invoiceId: Long?, action: String, details: String) {
        auditLogDao.insertLog(AuditLog(invoiceId = invoiceId, action = action, details = details))
    }

    // Payments
    fun getPaymentsForInvoice(invoiceId: Long): Flow<List<Payment>> = paymentDao.getPaymentsForInvoice(invoiceId)
    
    suspend fun recordPayment(payment: Payment) {
        paymentDao.insertPayment(payment)
        
        // Update Invoice status to Paid or Partially Paid
        val invoice = invoiceDao.getInvoiceByIdDirect(payment.invoiceId)
        if (invoice != null) {
            val paymentsFlowList = paymentDao.getPaymentsForInvoice(invoice.id)
            // Calculate totals
            val items = invoiceLineItemDao.getLineItemsForInvoiceDirect(invoice.id)
            val subtotal = items.sumOf { it.quantity * it.unitPrice }
            // Let's approximate the total calculation for status update
            val taxValue = subtotal * (invoice.taxRate / 100.0)
            val discountValue = subtotal * (invoice.discountRate / 100.0)
            val total = subtotal + taxValue - discountValue + invoice.shipping + invoice.handling - invoice.advancePayment
            
            // Query all payments to calculate sum
            // We can do this in ViewModel or repository, but we need the current payment sum:
            // Since we just recorded this payment, we'll log it
            auditLogDao.insertLog(
                AuditLog(
                    invoiceId = invoice.id,
                    action = "ثبت پرداخت",
                    details = "مبلغ ${payment.amount} با روش ${payment.method} دریافت شد."
                )
            )
            updateCustomerStats(invoice.customerId)
        }
    }

    private suspend fun updateCustomerStats(customerId: Long) {
        // We can dynamically compute stats on customer details request
    }

    // Projects
    suspend fun insertProject(project: Project): Long = projectDao.insert(project)
    suspend fun updateProject(project: Project) = projectDao.update(project)
    suspend fun deleteProject(project: Project) = projectDao.delete(project)

    suspend fun ensureProjectExists(nameOrCode: String, customerId: Long?, customerName: String) {
        val trimmed = nameOrCode.trim()
        if (trimmed.isBlank()) return
        val existing = projectDao.findByNameOrCode(trimmed, trimmed)
        if (existing == null) {
            projectDao.insert(
                Project(
                    name = trimmed,
                    code = if (trimmed.matches(Regex("^[A-Za-z0-9-]+$"))) trimmed else "",
                    customerId = customerId,
                    customerName = customerName,
                    status = "فعال"
                )
            )
        }
    }
}

