package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: AppSettings)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY isFavorite DESC, name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllList(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerByIdDirect(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY isFavorite DESC, name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllList(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun getProductById(id: Long): Flow<Product?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY issueDate DESC, id DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices ORDER BY id ASC")
    suspend fun getAllList(): List<Invoice>

    @Query("SELECT * FROM invoices ORDER BY id DESC LIMIT 1")
    suspend fun getLatestInvoiceDirect(): Invoice?

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    fun getInvoiceById(id: Long): Flow<Invoice?>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceByIdDirect(id: Long): Invoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)
}

@Dao
interface InvoiceLineItemDao {
    @Query("SELECT * FROM invoice_line_items")
    fun getAllLineItems(): Flow<List<InvoiceLineItem>>

    @Query("SELECT * FROM invoice_line_items ORDER BY id ASC")
    suspend fun getAllList(): List<InvoiceLineItem>

    @Query("SELECT * FROM invoice_line_items WHERE invoiceId = :invoiceId ORDER BY id ASC")
    fun getLineItemsForInvoice(invoiceId: Long): Flow<List<InvoiceLineItem>>

    @Query("SELECT * FROM invoice_line_items WHERE invoiceId = :invoiceId ORDER BY id ASC")
    suspend fun getLineItemsForInvoiceDirect(invoiceId: Long): List<InvoiceLineItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<InvoiceLineItem>)

    @Query("DELETE FROM invoice_line_items WHERE invoiceId = :invoiceId")
    suspend fun deleteLineItemsForInvoice(invoiceId: Long)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY date DESC")
    fun getPaymentsForInvoice(invoiceId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments")
    suspend fun getAllList(): List<Payment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Query("DELETE FROM payments WHERE invoiceId = :invoiceId")
    suspend fun deletePaymentsForInvoice(invoiceId: Long)

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs WHERE invoiceId = :invoiceId ORDER BY timestamp DESC")
    fun getLogsForInvoice(invoiceId: Long): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog): Long
}

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY isDefault DESC, id ASC")
    fun getAllBankAccounts(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts ORDER BY id ASC")
    suspend fun getAllList(): List<BankAccount>

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    suspend fun getBankAccountByIdDirect(id: Long): BankAccount?

    @Query("SELECT * FROM bank_accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultBankAccountDirect(): BankAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bankAccount: BankAccount): Long

    @Update
    suspend fun update(bankAccount: BankAccount)

    @Delete
    suspend fun delete(bankAccount: BankAccount)

    @Query("UPDATE bank_accounts SET isDefault = 0")
    suspend fun clearAllDefaults()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY id DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects ORDER BY id ASC")
    suspend fun getAllList(): List<Project>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: Long): Flow<Project?>

    @Query("SELECT * FROM projects WHERE name = :name OR (code != '' AND code = :code) LIMIT 1")
    suspend fun findByNameOrCode(name: String, code: String): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)
}

