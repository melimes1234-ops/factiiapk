package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.database.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.InvoiceLineItem
import com.example.data.model.Product
import com.example.data.model.BankAccount
import com.example.data.repository.InvoiceDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object CsvImportExportUtil {

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)
        if (contentUri != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        }
    }

    private fun escapeCsv(value: String?): String {
        val str = value ?: ""
        return if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }

    // --- PRODUCTS IMPORT / EXPORT ---

    fun exportProductsToCsv(context: Context, products: List<Product>) {
        val sb = StringBuilder()
        sb.append("\uFEFF") // UTF-8 BOM for Microsoft Excel
        sb.append("SKU,Name,CategoryType,ColorCode,SurfaceTreatment,BranchCount,Weight,Unit,Price,Cost,Stock,Description\n")

        products.forEach { p ->
            sb.append("${escapeCsv(p.sku)},")
            sb.append("${escapeCsv(p.name)},")
            sb.append("${escapeCsv(p.categoryType)},")
            sb.append("${escapeCsv(p.colorCode)},")
            sb.append("${escapeCsv(p.surfaceTreatment)},")
            sb.append("${p.branchCount},")
            sb.append("${p.weight},")
            sb.append("${escapeCsv(p.unit)},")
            sb.append("${p.price},")
            sb.append("${p.cost},")
            sb.append("${p.stock},")
            sb.append("${escapeCsv(p.description)}\n")
        }

        val cacheDir = File(context.cacheDir, "exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, "Products_Catalog.csv")
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }

        shareFile(context, file, "text/csv", "اشتراک‌گذاری لیست کالاها (اکسل)")
    }

    fun generateSampleProductsCsv(context: Context) {
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("SKU,Name,CategoryType,ColorCode,SurfaceTreatment,BranchCount,Weight,Unit,Price,Cost,Stock,Description\n")
        sb.append("FD142,پروفیل چوب پلاست دک ONCE,Wood,N3,BR,40.0,2.61,متر طول,320000.0,210000.0,500.0,پروفیل مخصوص کفپوش outdoor\n")
        sb.append("ST140,پروفیل چوب پلاست نما ST,Wood,N1,Emboss,25.0,2.10,متر طول,285000.0,180000.0,300.0,پروفیل مخصوص نمای ساختمان\n")
        sb.append("HCL,کلیپس T شکل فلزی (بست نصب),Accessory,-,Galvanized,0.0,0.015,عدد,12000.0,7000.0,5000.0,کلیپس نصب زیرسازی\n")

        val cacheDir = File(context.cacheDir, "exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, "Sample_Products_Template.csv")
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }

        shareFile(context, file, "text/csv", "دانلود نمونه فایل ورود کالاها")
    }

    fun parseProductsCsv(csvText: String): List<Product> {
        val list = mutableListOf<Product>()
        val lines = csvText.replace("\uFEFF", "").split("\n")
        if (lines.size <= 1) return list

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.size >= 2) {
                val sku = parts.getOrNull(0) ?: ""
                val name = parts.getOrNull(1) ?: ""
                if (name.isBlank()) continue

                val categoryType = parts.getOrNull(2)?.ifEmpty { "Wood" } ?: "Wood"
                val colorCode = parts.getOrNull(3) ?: ""
                val surfaceTreatment = parts.getOrNull(4) ?: ""
                val branchCount = parts.getOrNull(5)?.toDoubleOrNull() ?: 0.0

                val col6 = parts.getOrNull(6) ?: ""
                var weight = col6.toDoubleOrNull() ?: 0.0
                var unitIndex = 7
                var priceIndex = 8
                var costIndex = 9
                var stockIndex = 10
                var descIndex = 11

                if (col6.toDoubleOrNull() == null && col6.isNotBlank()) {
                    weight = 0.0
                    unitIndex = 6
                    priceIndex = 7
                    costIndex = 8
                    stockIndex = 9
                    descIndex = 10
                }

                val unit = parts.getOrNull(unitIndex)?.ifEmpty { if (categoryType == "Wood") "متر طول" else "عدد" } ?: "عدد"
                val price = parts.getOrNull(priceIndex)?.toDoubleOrNull() ?: 0.0
                val cost = parts.getOrNull(costIndex)?.toDoubleOrNull() ?: 0.0
                val stock = parts.getOrNull(stockIndex)?.toDoubleOrNull() ?: 0.0
                val description = parts.getOrNull(descIndex) ?: ""

                list.add(
                    Product(
                        sku = sku,
                        name = name,
                        category = if (categoryType == "Wood" || categoryType == "چوب پلاست") "چوب پلاست" else "پیچ و کلیپس",
                        categoryType = categoryType,
                        colorCode = colorCode,
                        surfaceTreatment = surfaceTreatment,
                        branchCount = branchCount,
                        weight = weight,
                        unit = unit,
                        price = price,
                        cost = cost,
                        stock = stock,
                        description = description
                    )
                )
            }
        }
        return list
    }

    // --- CUSTOMERS IMPORT / EXPORT ---

    fun exportCustomersToCsv(context: Context, customers: List<Customer>) {
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("Name,Company,Phone,Mobile,Email,EconomicCode,NationalId,BillingAddress,PostalCode,Notes\n")

        customers.forEach { c ->
            sb.append("${escapeCsv(c.name)},")
            sb.append("${escapeCsv(c.company)},")
            sb.append("${escapeCsv(c.phone)},")
            sb.append("${escapeCsv(c.mobile)},")
            sb.append("${escapeCsv(c.email)},")
            sb.append("${escapeCsv(c.taxId)},")
            sb.append("${escapeCsv(c.nationalId)},")
            sb.append("${escapeCsv(c.billingAddress)},")
            sb.append("${escapeCsv(c.postalCode)},")
            sb.append("${escapeCsv(c.notes)}\n")
        }

        val cacheDir = File(context.cacheDir, "exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, "Customers_List.csv")
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }

        shareFile(context, file, "text/csv", "اشتراک‌گذاری لیست خریداران (اکسل)")
    }

    fun generateSampleCustomersCsv(context: Context) {
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("Name,Company,Phone,Mobile,Email,EconomicCode,NationalId,BillingAddress,PostalCode,Notes\n")
        sb.append("رضا محمدی,صنایع چوبی نست,02188888888,09121112233,reza@nest.ir,4111222333,1010203040,تهران - خیابان ولیعصر - پلاک ۱۲,1987654321,مشتری خوش‌حساب پروژه‌های چوب پلاست\n")

        val cacheDir = File(context.cacheDir, "exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, "Sample_Customers_Template.csv")
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }

        shareFile(context, file, "text/csv", "دانلود نمونه فایل ورود خریداران")
    }

    fun parseCustomersCsv(csvText: String): List<Customer> {
        val list = mutableListOf<Customer>()
        val lines = csvText.replace("\uFEFF", "").split("\n")
        if (lines.size <= 1) return list

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.isNotEmpty()) {
                val name = parts.getOrNull(0) ?: ""
                if (name.isBlank()) continue

                val company = parts.getOrNull(1) ?: ""
                val phone = parts.getOrNull(2) ?: ""
                val mobile = parts.getOrNull(3) ?: ""
                val email = parts.getOrNull(4) ?: ""
                val taxId = parts.getOrNull(5) ?: ""
                val nationalId = parts.getOrNull(6) ?: ""
                val billingAddress = parts.getOrNull(7) ?: ""
                val postalCode = parts.getOrNull(8) ?: ""
                val notes = parts.getOrNull(9) ?: ""

                list.add(
                    Customer(
                        name = name,
                        company = company,
                        phone = phone,
                        mobile = mobile,
                        email = email,
                        taxId = taxId,
                        nationalId = nationalId,
                        billingAddress = billingAddress,
                        postalCode = postalCode,
                        notes = notes
                    )
                )
            }
        }
        return list
    }

    // --- PROJECTS IMPORT / EXPORT ---

    fun exportProjectsToCsv(context: Context, projects: List<com.example.data.model.Project>) {
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("Code,Name,CustomerName,Status,Description\n")

        projects.forEach { p ->
            sb.append("${escapeCsv(p.code)},")
            sb.append("${escapeCsv(p.name)},")
            sb.append("${escapeCsv(p.customerName)},")
            sb.append("${escapeCsv(p.status)},")
            sb.append("${escapeCsv(p.description)}\n")
        }

        val cacheDir = File(context.cacheDir, "exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, "Projects_List.csv")
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }

        shareFile(context, file, "text/csv", "اشتراک‌گذاری لیست پروژه‌ها (اکسل)")
    }

    fun generateSampleProjectsCsv(context: Context) {
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("Code,Name,CustomerName,Status,Description\n")
        sb.append("PRJ-101,پروژه ویلایی لواسان - روف گاردن,رضا محمدی,فعال,اجرای چوب پلاست کفپوش و زیرسازی روف گاردن\n")
        sb.append("PRJ-102,پروژه تجاری آرمون,صنایع چوبی نست,تکمیل شده,نمای چوب پلاست و چوب ترمو\n")

        val cacheDir = File(context.cacheDir, "exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, "Sample_Projects_Template.csv")
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }

        shareFile(context, file, "text/csv", "دانلود نمونه فایل ورود پروژه‌ها")
    }

    fun parseProjectsCsv(csvText: String): List<com.example.data.model.Project> {
        val list = mutableListOf<com.example.data.model.Project>()
        val lines = csvText.replace("\uFEFF", "").split("\n")
        if (lines.size <= 1) return list

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.isNotEmpty()) {
                val code = parts.getOrNull(0) ?: ""
                val name = parts.getOrNull(1) ?: ""
                if (name.isBlank() && code.isBlank()) continue

                val customerName = parts.getOrNull(2) ?: ""
                val status = parts.getOrNull(3)?.ifEmpty { "فعال" } ?: "فعال"
                val description = parts.getOrNull(4) ?: ""

                list.add(
                    com.example.data.model.Project(
                        code = code,
                        name = if (name.isNotBlank()) name else code,
                        customerName = customerName,
                        status = status,
                        description = description
                    )
                )
            }
        }
        return list
    }

    // --- INVOICE EXCEL CSV EXPORT ---

    fun exportInvoiceToCsv(context: Context, details: InvoiceDetails) {
        val invoice = details.invoice
        val customer = details.customer
        val items = details.lineItems

        val subtotal = items.sumOf { it.quantity * it.unitPrice }
        val taxTotal = if (invoice.taxType == "Exclusive") subtotal * (invoice.taxRate / 100.0) else 0.0
        val discountTotal = subtotal * (invoice.discountRate / 100.0) + invoice.discountAmount
        val grandTotal = subtotal + taxTotal - discountTotal + invoice.shipping + invoice.handling

        val sb = StringBuilder()
        sb.append("\uFEFF") // UTF-8 BOM
        sb.append("اطلاعات فاکتور / پیش‌فاکتور\n")
        sb.append("شماره فاکتور,${invoice.invoiceNumber},نوع فاکتور,${invoice.invoiceType},تاریخ,${Helper.formatJalaliShort(invoice.issueDate)}\n")
        sb.append("نام خریدار,${customer?.name ?: "-"},نام شرکت,${customer?.company ?: "-"},شماره تماس,${customer?.phone ?: "-"}\n")
        sb.append("شناسه/کد اقتصادی,${customer?.taxId ?: "-"},کد ملی,${customer?.nationalId ?: "-"},آدرس,${escapeCsv(customer?.billingAddress ?: "-")}\n\n")

        sb.append("کد کالا (SKU),نام کالا / شرح,دسته‌بندی,کد رنگ,پوشش سطح,تعداد شاخه,وزن (kg),متراژ/تعداد,واحد,قیمت واحد (تومان),مالیات (%),تخفیف (تومان),مبلغ کل (تومان)\n")

        items.forEach { item ->
            val itemDisc = item.quantity * item.unitPrice * (item.discountPercent / 100.0)
            val total = item.quantity * item.unitPrice * (1 + item.taxPercent / 100.0) - itemDisc
            sb.append("${escapeCsv(item.sku)},")
            sb.append("${escapeCsv(item.name)},")
            sb.append("${escapeCsv(if (item.categoryType == "Wood" || item.categoryType == "چوب پلاست") "چوب پلاست" else "پیچ و کلیپس")},")
            sb.append("${escapeCsv(item.colorCode)},")
            sb.append("${escapeCsv(item.surfaceTreatment)},")
            sb.append("${item.branchCount},")
            sb.append("${item.weight},")
            sb.append("${item.quantity},")
            sb.append("${escapeCsv(item.unit)},")
            sb.append("${item.unitPrice},")
            sb.append("${item.taxPercent},")
            sb.append("${item.discountPercent},")
            sb.append("${total}\n")
        }

        sb.append("\n")
        sb.append("جمع کل اقلام,${subtotal}\n")
        sb.append("تخفیف کل,${discountTotal}\n")
        sb.append("مالیات و عوارض,${taxTotal}\n")
        sb.append("مبلغ قابل پرداخت,${grandTotal}\n\n")

        if (invoice.notes.isNotBlank()) {
            sb.append("شرایط و توضیحات فاکتور:\n")
            sb.append("${escapeCsv(invoice.notes)}\n")
        }

        val cacheDir = File(context.cacheDir, "exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, "Invoice_${invoice.invoiceNumber}.csv")
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }

        shareFile(context, file, "text/csv", "خروجی اکسل فاکتور شماره ${invoice.invoiceNumber}")
    }

    // --- REPORTS CSV EXPORT ---

    fun exportReportsToCsv(
        context: Context,
        invoices: List<Invoice>,
        customers: List<Customer>,
        products: List<Product>
    ) {
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("گزارش جامع سیستم فاکتور و انبار چوب پلاست\n\n")

        sb.append(" خلاصه آمار کلی\n")
        sb.append("تعداد کل فاکتورها,${invoices.size}\n")
        sb.append("تعداد مشتریان,${customers.size}\n")
        sb.append("تعداد انواع کالاها,${products.size}\n\n")

        sb.append("لیست فاکتورها\n")
        sb.append("شماره فاکتور,نوع,مشتری,تاریخ,وضعیت\n")
        invoices.forEach { inv ->
            val custName = customers.find { it.id == inv.customerId }?.name ?: "-"
            sb.append("${inv.invoiceNumber},${inv.invoiceType},${escapeCsv(custName)},${Helper.formatJalaliShort(inv.issueDate)},${inv.status}\n")
        }

        sb.append("\nموجودی انبار محصولات\n")
        sb.append("SKU,نام کالا,نوع,کد رنگ,پوشش سطح,تعداد شاخه,وزن (kg),موجودی,واحد,قیمت فروش\n")
        products.forEach { p ->
            sb.append("${escapeCsv(p.sku)},${escapeCsv(p.name)},${escapeCsv(p.categoryType)},${escapeCsv(p.colorCode)},${escapeCsv(p.surfaceTreatment)},${p.branchCount},${p.weight},${p.stock},${escapeCsv(p.unit)},${p.price}\n")
        }

        val cacheDir = File(context.cacheDir, "exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, "Comprehensive_Report.csv")
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }

        shareFile(context, file, "text/csv", "اشتراک‌گذاری گزارش جامع سیستم (اکسل)")
    }

    // --- FULL DATABASE BACKUP & RESTORE (JSON) ---

    suspend fun exportFullBackupJson(context: Context, db: AppDatabase) {
        withContext(Dispatchers.IO) {
            val rootJson = JSONObject()
            rootJson.put("version", 1)
            rootJson.put("timestamp", System.currentTimeMillis())

            // Customers
            val customers = db.customerDao().getAllList()
            val custArray = JSONArray()
            customers.forEach { c ->
                val o = JSONObject()
                o.put("id", c.id)
                o.put("name", c.name)
                o.put("company", c.company)
                o.put("phone", c.phone)
                o.put("email", c.email)
                o.put("taxId", c.taxId)
                o.put("nationalId", c.nationalId)
                o.put("billingAddress", c.billingAddress)
                custArray.put(o)
            }
            rootJson.put("customers", custArray)

            // Products
            val products = db.productDao().getAllList()
            val prodArray = JSONArray()
            products.forEach { p ->
                val o = JSONObject()
                o.put("id", p.id)
                o.put("sku", p.sku)
                o.put("barcode", p.barcode)
                o.put("name", p.name)
                o.put("description", p.description)
                o.put("unit", p.unit)
                o.put("category", p.category)
                o.put("price", p.price)
                o.put("cost", p.cost)
                o.put("taxRate", p.taxRate)
                o.put("stock", p.stock)
                o.put("colorCode", p.colorCode)
                o.put("surfaceTreatment", p.surfaceTreatment)
                o.put("branchCount", p.branchCount)
                o.put("categoryType", p.categoryType)
                prodArray.put(o)
            }
            rootJson.put("products", prodArray)

            // Invoices
            val invoices = db.invoiceDao().getAllList()
            val invArray = JSONArray()
            invoices.forEach { inv ->
                val o = JSONObject()
                o.put("id", inv.id)
                o.put("invoiceNumber", inv.invoiceNumber)
                o.put("invoiceType", inv.invoiceType)
                o.put("customerId", inv.customerId)
                o.put("issueDate", inv.issueDate)
                o.put("dueDate", inv.dueDate)
                o.put("referenceNo", inv.referenceNo)
                o.put("poNumber", inv.poNumber)
                o.put("projectNumber", inv.projectNumber)
                o.put("salesperson", inv.salesperson)
                o.put("supportPerson", inv.supportPerson)
                o.put("currency", inv.currency)
                o.put("language", inv.language)
                o.put("status", inv.status)
                o.put("template", inv.template)
                o.put("notes", inv.notes)
                o.put("shipping", inv.shipping)
                o.put("handling", inv.handling)
                o.put("discountRate", inv.discountRate)
                o.put("discountAmount", inv.discountAmount)
                o.put("discountType", inv.discountType)
                o.put("taxRate", inv.taxRate)
                o.put("taxType", inv.taxType)
                o.put("advancePayment", inv.advancePayment)
                o.put("paymentMethod", inv.paymentMethod)
                o.put("paymentDetails", inv.paymentDetails)
                o.put("paymentDocumentPaths", inv.paymentDocumentPaths)
                o.put("paymentTerms", inv.paymentTerms)
                o.put("shippingTerms", inv.shippingTerms)
                o.put("bankAccountId", inv.bankAccountId)
                invArray.put(o)
            }
            rootJson.put("invoices", invArray)

            // Export Payment Proof Image Files as Base64 in JSON
            val paymentImagesJson = JSONObject()
            invoices.forEach { inv ->
                if (inv.paymentDocumentPaths.isNotBlank()) {
                    val paths = inv.paymentDocumentPaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    paths.forEach { pathStr ->
                        try {
                            val uri = android.net.Uri.parse(pathStr)
                            val inputStream = if (uri.scheme == "content" || uri.scheme == "file") {
                                context.contentResolver.openInputStream(uri)
                            } else {
                                val localF = java.io.File(pathStr)
                                if (localF.exists()) java.io.FileInputStream(localF) else null
                            }
                            
                            inputStream?.use { stream ->
                                val bytes = stream.readBytes()
                                if (bytes.isNotEmpty()) {
                                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                    val fileName = uri.lastPathSegment ?: "doc_${pathStr.hashCode()}.jpg"
                                    paymentImagesJson.put(fileName, base64)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            rootJson.put("paymentImages", paymentImagesJson)

            // Bank Accounts
            val bankAccounts = db.bankAccountDao().getAllList()
            val bankArray = JSONArray()
            bankAccounts.forEach { b ->
                val o = JSONObject()
                o.put("id", b.id)
                o.put("bankName", b.bankName)
                o.put("accountHolderName", b.accountHolderName)
                o.put("accountNumber", b.accountNumber)
                o.put("cardNumber", b.cardNumber)
                o.put("shabaNumber", b.shabaNumber)
                o.put("notes", b.notes)
                o.put("isDefault", b.isDefault)
                bankArray.put(o)
            }
            rootJson.put("bankAccounts", bankArray)

            // Line items
            val lineItems = db.invoiceLineItemDao().getAllList()
            val itemArray = JSONArray()
            lineItems.forEach { item ->
                val o = JSONObject()
                o.put("id", item.id)
                o.put("invoiceId", item.invoiceId)
                o.put("name", item.name)
                o.put("sku", item.sku)
                o.put("quantity", item.quantity)
                o.put("unit", item.unit)
                o.put("unitPrice", item.unitPrice)
                o.put("discountPercent", item.discountPercent)
                o.put("taxPercent", item.taxPercent)
                o.put("colorCode", item.colorCode)
                o.put("surfaceTreatment", item.surfaceTreatment)
                o.put("branchCount", item.branchCount)
                o.put("categoryType", item.categoryType)
                o.put("requestSubject", item.requestSubject)
                o.put("executionDays", item.executionDays)
                o.put("teamSize", item.teamSize)
                o.put("accommodationCost", item.accommodationCost)
                o.put("transportationCost", item.transportationCost)
                o.put("consumablesCost", item.consumablesCost)
                o.put("crossSectionFactor", item.crossSectionFactor)
                o.put("initialAreaSqm", item.initialAreaSqm)
                o.put("discountAmount", item.discountAmount)
                itemArray.put(o)
            }
            rootJson.put("lineItems", itemArray)

            // Payments
            val payments = db.paymentDao().getAllList()
            val payArray = JSONArray()
            payments.forEach { pay ->
                val o = JSONObject()
                o.put("id", pay.id)
                o.put("invoiceId", pay.invoiceId)
                o.put("date", pay.date)
                o.put("amount", pay.amount)
                o.put("method", pay.method)
                o.put("receiptNo", pay.receiptNo)
                o.put("notes", pay.notes)
                o.put("chequeNumber", pay.chequeNumber)
                if (pay.chequeDueDate != null) o.put("chequeDueDate", pay.chequeDueDate)
                o.put("bankName", pay.bankName)
                o.put("chequeBranch", pay.chequeBranch)
                payArray.put(o)
            }
            rootJson.put("payments", payArray)

            // Projects
            val projects = db.projectDao().getAllList()
            val projArray = JSONArray()
            projects.forEach { proj ->
                val o = JSONObject()
                o.put("id", proj.id)
                o.put("name", proj.name)
                o.put("code", proj.code)
                if (proj.customerId != null) o.put("customerId", proj.customerId)
                o.put("customerName", proj.customerName)
                o.put("status", proj.status)
                o.put("description", proj.description)
                o.put("createdAt", proj.createdAt)
                projArray.put(o)
            }
            rootJson.put("projects", projArray)

            // AppSettings
            val settings = db.appSettingsDao().getSettingsDirect()
            if (settings != null) {
                val o = JSONObject()
                o.put("companyName", settings.companyName)
                o.put("companyAddress", settings.companyAddress)
                o.put("companyPhone", settings.companyPhone)
                o.put("companyEmail", settings.companyEmail)
                o.put("companyWebsite", settings.companyWebsite)
                o.put("companyTaxId", settings.companyTaxId)
                o.put("companyVatNumber", settings.companyVatNumber)
                o.put("companyNationalId", settings.companyNationalId)
                o.put("companyPostalCode", settings.companyPostalCode)
                o.put("usePersianDigits", settings.usePersianDigits)
                o.put("useJalaliCalendar", settings.useJalaliCalendar)
                o.put("defaultCurrency", settings.defaultCurrency)
                o.put("defaultLanguage", settings.defaultLanguage)
                o.put("themeMode", settings.themeMode)
                o.put("primaryColorHex", settings.primaryColorHex)
                o.put("autoIncrementNumber", settings.autoIncrementNumber)
                rootJson.put("appSettings", o)
            }

            val cacheDir = File(context.cacheDir, "backups")
            cacheDir.mkdirs()
            val dateStr = Helper.formatGregorian(System.currentTimeMillis()).replace("/", "")
            val file = File(cacheDir, "InvoiceApp_Backup_$dateStr.json")
            FileOutputStream(file).use { it.write(rootJson.toString(2).toByteArray(Charsets.UTF_8)) }

            withContext(Dispatchers.Main) {
                shareFile(context, file, "application/json", "پشتیبان‌گیری کامل دیتابیس")
            }
        }
    }

    suspend fun restoreFullBackupJson(context: Context, db: AppDatabase, jsonText: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonText)
                
                db.clearAllTables()

                // Restore AppSettings
                if (root.has("appSettings")) {
                    val o = root.getJSONObject("appSettings")
                    db.appSettingsDao().insertOrUpdate(
                        com.example.data.model.AppSettings(
                            id = 1,
                            companyName = o.optString("companyName", ""),
                            companyAddress = o.optString("companyAddress", ""),
                            companyPhone = o.optString("companyPhone", ""),
                            companyEmail = o.optString("companyEmail", ""),
                            companyWebsite = o.optString("companyWebsite", ""),
                            companyTaxId = o.optString("companyTaxId", ""),
                            companyVatNumber = o.optString("companyVatNumber", ""),
                            companyNationalId = o.optString("companyNationalId", ""),
                            companyPostalCode = o.optString("companyPostalCode", ""),
                            usePersianDigits = o.optBoolean("usePersianDigits", true),
                            useJalaliCalendar = o.optBoolean("useJalaliCalendar", true),
                            defaultCurrency = o.optString("defaultCurrency", "تومان"),
                            defaultLanguage = o.optString("defaultLanguage", "fa"),
                            themeMode = o.optString("themeMode", "light"),
                            primaryColorHex = o.optString("primaryColorHex", "#FF1A73E8"),
                            autoIncrementNumber = o.optInt("autoIncrementNumber", 1001)
                        )
                    )
                }

                // Restore Customers
                if (root.has("customers")) {
                    val custArray = root.getJSONArray("customers")
                    for (i in 0 until custArray.length()) {
                        val o = custArray.getJSONObject(i)
                        db.customerDao().insert(
                            Customer(
                                id = o.optLong("id", 0L),
                                name = o.optString("name", ""),
                                company = o.optString("company", ""),
                                phone = o.optString("phone", ""),
                                email = o.optString("email", ""),
                                taxId = o.optString("taxId", ""),
                                nationalId = o.optString("nationalId", ""),
                                billingAddress = o.optString("billingAddress", "")
                            )
                        )
                    }
                }

                // Restore Products
                if (root.has("products")) {
                    val prodArray = root.getJSONArray("products")
                    for (i in 0 until prodArray.length()) {
                        val o = prodArray.getJSONObject(i)
                        db.productDao().insert(
                            Product(
                                id = o.optLong("id", 0L),
                                sku = o.optString("sku", ""),
                                barcode = o.optString("barcode", ""),
                                name = o.optString("name", ""),
                                description = o.optString("description", ""),
                                unit = o.optString("unit", "عدد"),
                                category = o.optString("category", "عمومی"),
                                price = o.optDouble("price", 0.0),
                                cost = o.optDouble("cost", 0.0),
                                taxRate = o.optDouble("taxRate", 0.0),
                                stock = o.optDouble("stock", 0.0),
                                colorCode = o.optString("colorCode", ""),
                                surfaceTreatment = o.optString("surfaceTreatment", ""),
                                branchCount = o.optDouble("branchCount", 0.0),
                                categoryType = o.optString("categoryType", "Wood"),
                                weight = o.optDouble("weight", 0.0)
                            )
                        )
                    }
                }

                // Restore Payment Document Image Files from JSON
                val docsDir = java.io.File(context.filesDir, "payment_docs")
                if (!docsDir.exists()) docsDir.mkdirs()

                if (root.has("paymentImages")) {
                    val imgJson = root.getJSONObject("paymentImages")
                    val keys = imgJson.keys()
                    while (keys.hasNext()) {
                        val fileName = keys.next()
                        val base64Str = imgJson.optString(fileName, "")
                        if (base64Str.isNotEmpty()) {
                            try {
                                val bytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                                val destFile = java.io.File(docsDir, fileName)
                                java.io.FileOutputStream(destFile).use { out ->
                                    out.write(bytes)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                // Restore Invoices
                if (root.has("invoices")) {
                    val invArray = root.getJSONArray("invoices")
                    for (i in 0 until invArray.length()) {
                        val o = invArray.getJSONObject(i)
                        val rawPaths = o.optString("paymentDocumentPaths", "")
                        val restoredPaths = if (rawPaths.isNotBlank()) {
                            rawPaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { pathStr ->
                                val uri = try { android.net.Uri.parse(pathStr) } catch (e: Exception) { null }
                                val fileName = uri?.lastPathSegment ?: "doc_${pathStr.hashCode()}.jpg"
                                val localFile = java.io.File(docsDir, fileName)
                                if (localFile.exists()) {
                                    android.net.Uri.fromFile(localFile).toString()
                                } else {
                                    pathStr
                                }
                            }.distinct().joinToString(",")
                        } else ""

                        db.invoiceDao().insertInvoice(
                            Invoice(
                                id = o.optLong("id", 0L),
                                invoiceNumber = o.optString("invoiceNumber", ""),
                                invoiceType = o.optString("invoiceType", "پیش‌فاکتور"),
                                customerId = o.optLong("customerId", 0L),
                                issueDate = o.optLong("issueDate", System.currentTimeMillis()),
                                dueDate = o.optLong("dueDate", System.currentTimeMillis()),
                                referenceNo = o.optString("referenceNo", ""),
                                poNumber = o.optString("poNumber", ""),
                                projectNumber = o.optString("projectNumber", ""),
                                salesperson = o.optString("salesperson", ""),
                                supportPerson = o.optString("supportPerson", ""),
                                currency = o.optString("currency", "تومان"),
                                language = o.optString("language", "fa"),
                                status = o.optString("status", "Draft"),
                                template = o.optString("template", "General"),
                                notes = o.optString("notes", ""),
                                shipping = o.optDouble("shipping", 0.0),
                                handling = o.optDouble("handling", 0.0),
                                discountRate = o.optDouble("discountRate", 0.0),
                                discountAmount = o.optDouble("discountAmount", 0.0),
                                discountType = o.optString("discountType", "Percent"),
                                taxRate = o.optDouble("taxRate", 0.0),
                                taxType = o.optString("taxType", "Exclusive"),
                                advancePayment = o.optDouble("advancePayment", 0.0),
                                paymentMethod = o.optString("paymentMethod", ""),
                                paymentDetails = o.optString("paymentDetails", ""),
                                paymentDocumentPaths = restoredPaths,
                                paymentTerms = o.optString("paymentTerms", ""),
                                shippingTerms = o.optString("shippingTerms", ""),
                                bankAccountId = o.optLong("bankAccountId", 0L)
                            )
                        )
                    }
                }

                // Restore Bank Accounts
                if (root.has("bankAccounts")) {
                    val bankArray = root.getJSONArray("bankAccounts")
                    for (i in 0 until bankArray.length()) {
                        val o = bankArray.getJSONObject(i)
                        db.bankAccountDao().insert(
                            BankAccount(
                                id = o.optLong("id", 0L),
                                bankName = o.optString("bankName", ""),
                                accountHolderName = o.optString("accountHolderName", ""),
                                accountNumber = o.optString("accountNumber", ""),
                                cardNumber = o.optString("cardNumber", ""),
                                shabaNumber = o.optString("shabaNumber", ""),
                                notes = o.optString("notes", ""),
                                isDefault = o.optBoolean("isDefault", false)
                            )
                        )
                    }
                }

                // Restore Line Items
                if (root.has("lineItems")) {
                    val itemArray = root.getJSONArray("lineItems")
                    val itemsToInsert = mutableListOf<InvoiceLineItem>()
                    for (i in 0 until itemArray.length()) {
                        val o = itemArray.getJSONObject(i)
                        itemsToInsert.add(
                            InvoiceLineItem(
                                id = o.optLong("id", 0L),
                                invoiceId = o.optLong("invoiceId", 0L),
                                name = o.optString("name", ""),
                                sku = o.optString("sku", ""),
                                quantity = o.optDouble("quantity", 1.0),
                                unit = o.optString("unit", "عدد"),
                                unitPrice = o.optDouble("unitPrice", 0.0),
                                discountPercent = o.optDouble("discountPercent", 0.0),
                                taxPercent = o.optDouble("taxPercent", 0.0),
                                colorCode = o.optString("colorCode", ""),
                                surfaceTreatment = o.optString("surfaceTreatment", ""),
                                branchCount = o.optDouble("branchCount", 0.0),
                                categoryType = o.optString("categoryType", "Wood"),
                                requestSubject = o.optString("requestSubject", ""),
                                executionDays = o.optInt("executionDays", 0),
                                teamSize = o.optInt("teamSize", 0),
                                accommodationCost = o.optDouble("accommodationCost", 0.0),
                                transportationCost = o.optDouble("transportationCost", 0.0),
                                consumablesCost = o.optDouble("consumablesCost", 0.0),
                                crossSectionFactor = o.optDouble("crossSectionFactor", 1.0),
                                initialAreaSqm = o.optDouble("initialAreaSqm", 0.0),
                                discountAmount = o.optDouble("discountAmount", 0.0),
                                weight = o.optDouble("weight", 0.0)
                            )
                        )
                    }
                    if (itemsToInsert.isNotEmpty()) {
                        db.invoiceLineItemDao().insertLineItems(itemsToInsert)
                    }
                }

                // Restore Payments
                if (root.has("payments")) {
                    val payArray = root.getJSONArray("payments")
                    for (i in 0 until payArray.length()) {
                        val o = payArray.getJSONObject(i)
                        db.paymentDao().insertPayment(
                            com.example.data.model.Payment(
                                id = o.optLong("id", 0L),
                                invoiceId = o.optLong("invoiceId", 0L),
                                date = o.optLong("date", System.currentTimeMillis()),
                                amount = o.optDouble("amount", 0.0),
                                method = o.optString("method", "نقد"),
                                receiptNo = o.optString("receiptNo", ""),
                                notes = o.optString("notes", ""),
                                chequeNumber = o.optString("chequeNumber", ""),
                                chequeDueDate = if (o.has("chequeDueDate")) o.optLong("chequeDueDate") else null,
                                bankName = o.optString("bankName", ""),
                                chequeBranch = o.optString("chequeBranch", "")
                            )
                        )
                    }
                }

                // Restore Projects
                if (root.has("projects")) {
                    val projArray = root.getJSONArray("projects")
                    for (i in 0 until projArray.length()) {
                        val o = projArray.getJSONObject(i)
                        db.projectDao().insert(
                            com.example.data.model.Project(
                                id = o.optLong("id", 0L),
                                name = o.optString("name", ""),
                                code = o.optString("code", ""),
                                customerId = if (o.has("customerId")) o.optLong("customerId") else null,
                                customerName = o.optString("customerName", ""),
                                status = o.optString("status", "فعال"),
                                description = o.optString("description", ""),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val ch = line[i]
            if (ch == '\"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                result.add(cur.toString().trim())
                cur = StringBuilder()
            } else {
                cur.append(ch)
            }
        }
        result.add(cur.toString().trim())
        return result
    }
}
