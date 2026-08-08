package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "شرکت نمونه",
    val companyAddress: String = "تهران، خیابان ولیعصر، ساختمان نگین",
    val companyPhone: String = "۰۲۱-۸۸۸۸۸۸۸۸",
    val companyEmail: String = "info@example.com",
    val companyWebsite: String = "www.example.com",
    val companyTaxId: String = "Economic123456", // کد اقتصادی
    val companyVatNumber: String = "Reg789012", // شماره ثبت
    val companyNationalId: String = "10100000000", // شناسه ملی
    val companyPostalCode: String = "1234567890",
    val logoPath: String? = null,
    val stampPath: String? = null,
    val signaturePath: String? = null,
    val usePersianDigits: Boolean = true,
    val useJalaliCalendar: Boolean = true,
    val defaultCurrency: String = "تومان",
    val defaultLanguage: String = "fa", // "fa" or "en"
    val themeMode: String = "light", // "system", "light", "dark"
    val primaryColorHex: String = "#FF1A73E8", // Premium Indigo/Blue
    val autoIncrementNumber: Int = 1001,
    val invoiceNumberPrefix: String = "MK",
    val defaultPaymentTerms: String = "۵۰٪ نقد هنگام ثبت سفارش، ۵۰٪ هنگام تحویل کالا",
    val defaultShippingTerms: String = "ارسال توسط باربری / تحویل در محل کارخانه",
    val defaultColorCodes: String = "N1, N2, N3, C1, C2, W1, W2, G1, G2",
    val defaultSurfaceTreatments: String = "BR (برس خورده), Emboss (طرح چوب / امبوس), Sanded (سنباده خورده), Smooth (صیقلی)",
    val nestLogoStyle: String = "light", // "light" (لوگوی روشن / سفید) or "dark" (لوگوی تیره / مشکی)
    val appVersion: String = "1.2.0",
    val updateDownloadUrl: String = "https://github.com/melimes1234-ops/factiiapk/releases"
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val company: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val mobile: String = "",
    val website: String = "",
    val billingAddress: String = "",
    val shippingAddress: String = "",
    val country: String = "ایران",
    val state: String = "تهران",
    val city: String = "تهران",
    val postalCode: String = "",
    val taxId: String = "", // شناسه ملی / کد اقتصادی
    val nationalId: String = "", // کد ملی شخص حقیقی
    val preferredCurrency: String = "تومان",
    val notes: String = "",
    val isFavorite: Boolean = false
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String = "",
    val barcode: String = "",
    val name: String = "",
    val description: String = "",
    val unit: String = "عدد", // عدد، کیلوگرم، ساعت و غیره
    val category: String = "عمومی",
    val price: Double = 0.0,
    val cost: Double = 0.0,
    val taxRate: Double = 0.0,
    val stock: Double = 0.0,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val colorCode: String = "",
    val surfaceTreatment: String = "",
    val branchCount: Double = 0.0,
    val categoryType: String = "Wood", // "Wood" (چوب پلاست) or "Accessory" (پیچ و کلیپس)
    val crossSectionFactor: Double = 0.0,
    val initialAreaSqm: Double = 0.0,
    val weight: Double = 0.0
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val invoiceType: String = "پیش‌فاکتور", // "پیش‌فاکتور", "فاکتور فروش"
    val issueDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L, // 7 days later
    val referenceNo: String = "",
    val poNumber: String = "",
    val projectNumber: String = "",
    val salesperson: String = "",
    val supportPerson: String = "",
    val currency: String = "تومان",
    val exchangeRate: Double = 1.0,
    val language: String = "fa",
    val status: String = "Draft", // Draft, Pending, Sent, Paid, PartiallyPaid, Overdue, Cancelled, Refunded, Void
    val template: String = "General", // "General", "Nest", etc.
    val customerId: Long,
    val notes: String = "",
    val shipping: Double = 0.0,
    val handling: Double = 0.0,
    val discountRate: Double = 0.0, // Overall percentage
    val discountAmount: Double = 0.0, // Flat discount
    val taxRate: Double = 0.0, // Overall tax percentage
    val taxType: String = "Exclusive", // Inclusive, Exclusive, VAT
    val advancePayment: Double = 0.0, // Amount prepaid
    val paymentMethod: String = "",
    val paymentDetails: String = "",
    val isStarred: Boolean = false,
    val isArchived: Boolean = false,
    val bankAccountId: Long = 0L,
    val paymentTerms: String = "",
    val shippingTerms: String = "",
    val discountType: String = "Percent", // "Percent" or "Amount"
    val paymentDocumentPaths: String = "" // Comma-separated URIs/paths for payment proof images
)

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankName: String = "",
    val accountHolderName: String = "",
    val accountNumber: String = "",
    val cardNumber: String = "",
    val shabaNumber: String = "",
    val notes: String = "",
    val isDefault: Boolean = false
)

@Entity(tableName = "invoice_line_items")
data class InvoiceLineItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long = 0,
    val sku: String = "",
    val name: String = "", 
    val description: String = "",
    val quantity: Double = 1.0,
    val unit: String = "عدد",
    val unitPrice: Double = 0.0,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val colorTag: String? = null,
    val colorCode: String = "",
    val surfaceTreatment: String = "",
    val branchCount: Double = 0.0,
    val categoryType: String = "Wood", // "Wood" (چوب پلاست), "Accessory" (پیچ و کلیپس) or "Installation" (نصب)
    val crossSectionFactor: Double = 0.0,
    val initialAreaSqm: Double = 0.0,
    val requestSubject: String = "", // موضوع درخواست (e.g. کفپوش دور استخر)
    val executionDays: Int = 0, // مدت زمان اجرا (روز کاری)
    val teamSize: Int = 0, // تعداد نفرات تیم اجرا
    val accommodationCost: Double = 0.0, // هزینه اسکان (تومان)
    val transportationCost: Double = 0.0, // هزینه رفت و آمد (تومان)
    val consumablesCost: Double = 0.0, // ضد زنگ و اقلام مصرفی (تومان)
    val weight: Double = 0.0 // وزن هر واحد (کیلوگرم)
)

data class WoodProfilePreset(
    val name: String,
    val sku: String,
    val crossSectionFactor: Double, // cross_section_sqm_per_m
    val category: String, // Cladding, Decking, Board
    val defaultPrice: Double = 320000.0,
    val defaultWeight: Double = 0.0 // kg per meter
)

object WoodPresets {
    val profiles = listOf(
        WoodProfilePreset("FEEL", "FC140", 7.1428, "Cladding", 320000.0, 2.45),
        WoodProfilePreset("LEAD", "FC103", 9.7, "Cladding", 320000.0, 1.85),
        WoodProfilePreset("EXPORT", "FC150", 6.6666, "Cladding", 320000.0, 2.80),
        WoodProfilePreset("ROMANCE", "FC142", 7.1428, "Cladding", 320000.0, 2.50),
        WoodProfilePreset("SHINE", "FC21", 7.1428, "Decking", 320000.0, 2.30),
        WoodProfilePreset("POND", "FD155", 6.25, "Decking", 320000.0, 3.10),
        WoodProfilePreset("FAIR", "FD26", 6.25, "Decking", 320000.0, 2.90),
        WoodProfilePreset("ONCE", "FD142", 6.9444, "Decking", 320000.0, 2.61),
        WoodProfilePreset("T-ONCE", "FD142T", 6.9444, "Decking", 320000.0, 2.61),
        WoodProfilePreset("FLEX", "FD140", 6.897, "Decking", 320000.0, 2.75),
        WoodProfilePreset("CLAN", "FD92", 10.8696, "Decking", 320000.0, 2.10),
        WoodProfilePreset("T-CLAN", "FD92T", 10.8696, "Decking", 320000.0, 2.10),
        WoodProfilePreset("FAME", "FD72", 13.8888, "Decking", 320000.0, 1.60),
        WoodProfilePreset("FAME-T", "FD72T", 13.8888, "Decking", 320000.0, 1.60),
        WoodProfilePreset("FATE", "FB290", 3.3898, "Board", 320000.0, 5.20),
        WoodProfilePreset("FLAT", "FB130", 7.6923, "Board", 320000.0, 2.40),
        WoodProfilePreset("PETAL", "FB68", 14.7058, "Board", 320000.0, 1.25),
        WoodProfilePreset("TAIL", "FB55", 18.1818, "Board", 320000.0, 1.05),
        WoodProfilePreset("VIVA", "FB92", 10.99, "Board", 320000.0, 1.70)
    )

    fun findPresetByNameOrSku(query: String): WoodProfilePreset? {
        if (query.isBlank()) return null
        val trimmed = query.trim()
        return profiles.find {
            it.name.equals(trimmed, ignoreCase = true) ||
            it.sku.equals(trimmed, ignoreCase = true) ||
            trimmed.contains(it.name, ignoreCase = true) ||
            trimmed.contains(it.sku, ignoreCase = true)
        }
    }

    /**
     * Calculates branch count and total linear meters given initial area (m²) and cross_section_sqm_per_m factor.
     * Formula:
     * 1) Total linear meters (quantity) = initialAreaSqm * factor
     * 2) Branch count = ceil(total linear meters / 3.0)
     */
    fun calculateBranches(initialAreaSqm: Double, factor: Double): Pair<Double, Double> {
        if (initialAreaSqm <= 0.0) return Pair(0.0, 0.0)
        val actualFactor = if (factor > 0.0) factor else 7.1428
        val rawMeters = initialAreaSqm * actualFactor
        val totalMeters = kotlin.math.ceil(rawMeters)
        val rawBranches = totalMeters / 3.0
        val branchCount = kotlin.math.ceil(rawBranches)
        return Pair(branchCount, totalMeters)
    }
}

data class AccessoryPreset(
    val name: String,
    val sku: String,
    val objectType: String, // ACCESSORIES, FINISHERS, SCREW, TILES
    val specialFor: String, // Special profile compatibility e.g. "ONCE - CLAN-FAME-POND - FAIR"
    val defaultPrice: Double = 1000.0,
    val unit: String = "عدد"
)

object AccessoryPresets {
    val items = listOf(
        AccessoryPreset("Clicker - H", "HCL", "ACCESSORIES", "ONCE - CLAN-FAME-POND - FAIR", 8500.0),
        AccessoryPreset("Clicker - H", "HCLS", "ACCESSORIES", "FEEL-LEAD - SHINE", 8500.0),
        AccessoryPreset("Starter Clicker", "SCL", "ACCESSORIES", "ONCE-CLAN-FAME-POND-FAIR-FEEL-LEAD - SHINE", 8500.0),
        AccessoryPreset("Clicker - T", "TCL", "ACCESSORIES", "POND-FAIR - FLEX", 8500.0),
        AccessoryPreset("Sub Deck Clicker", "SD", "ACCESSORIES", "COFFIN-DOWN - MELA", 55000.0),
        AccessoryPreset("L-Clicker", "SD1", "ACCESSORIES", "COFFIN-DOWN - MELA", 55000.0),
        AccessoryPreset("1- Clicker", "ICL", "ACCESSORIES", "IMP", 75000.0),
        AccessoryPreset("U", "U1(3x3x3x3)", "FINISHERS", "-", 1000.0),
        AccessoryPreset("EL", "L1(5x3x3)cm", "FINISHERS", "-", 1000.0),
        AccessoryPreset("OMEGA", "2(2x10x10x2)cm", "FINISHERS", "-", 1000.0),
        AccessoryPreset("پیچ ۲ سرمته غیر رنگی (4.2mm x 19mm)", "-", "SCREW", "-", 1000.0),
        AccessoryPreset("پیچ ۴ سرمته رنگی (4.2mm x 38mm)", "-", "SCREW", "-", 1000.0),
        AccessoryPreset("پیچ ۲ ام دی اف رنگی (4.2mm x 19mm)", "-", "SCREW", "-", 1000.0),
        AccessoryPreset("پیچ ۲ سرمته رنگی (4.2mm x 19mm)", "-", "SCREW", "-", 1000.0),
        AccessoryPreset("پیچ ۲.۵ سرمته (4.2mm x 25mm)", "-", "SCREW", "-", 1000.0),
        AccessoryPreset("پیچ ۴ سرمته (4.2mm x 38mm)", "-", "SCREW", "-", 1000.0),
        AccessoryPreset("TILE FAME 4 (30x30)", "LAVAN", "TILES", "-", 1000.0),
        AccessoryPreset("TILE CLAN 3 (30x30)", "SHIDOR", "TILES", "-", 1000.0)
    )

    fun findPresetByNameOrSku(query: String): AccessoryPreset? {
        if (query.isBlank()) return null
        val trimmed = query.trim()
        
        // Exact SKU match if valid
        items.find { it.sku != "-" && it.sku.equals(trimmed, ignoreCase = true) }?.let { return it }

        // Exact Name match
        val nameMatches = items.filter { it.name.equals(trimmed, ignoreCase = true) }
        if (nameMatches.size == 1) return nameMatches.first()

        // Match formatted string like "Clicker - H (HCL)" or "Clicker - H (HCLS)"
        items.find { acc ->
            val label = "${acc.name} (${acc.sku})"
            label.contains(trimmed, ignoreCase = true) || trimmed.contains(label, ignoreCase = true)
        }?.let { return it }

        // SKU substring match
        items.find { acc -> acc.sku != "-" && (trimmed.contains(acc.sku, ignoreCase = true) || acc.sku.contains(trimmed, ignoreCase = true)) }?.let { return it }

        // Name substring match
        return items.find { acc -> acc.name.contains(trimmed, ignoreCase = true) || trimmed.contains(acc.name, ignoreCase = true) }
    }
}

data class CabinetPreset(
    val name: String,
    val defaultDimensions: String,
    val defaultPrice: Double = 0.0
)

object CabinetPresets {
    val items = emptyList<CabinetPreset>()

    fun findPresetByName(query: String): CabinetPreset? {
        if (query.isBlank()) return null
        val trimmed = query.trim()
        return items.find { it.name.contains(trimmed, ignoreCase = true) || trimmed.contains(it.name, ignoreCase = true) }
    }
}

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val date: Long = System.currentTimeMillis(),
    val amount: Double = 0.0,
    val method: String = "نقد", // نقد، کارت، حواله بانکی، درگاه آنلاین، چک
    val receiptNo: String = "",
    val notes: String = "",
    val chequeNumber: String = "",
    val chequeDueDate: Long? = null,
    val bankName: String = "",
    val chequeBranch: String = "",
    val chequeImagePath: String? = null,
    val documentPaths: String = "" // Comma-separated URIs/paths for payment documents
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // Created, Edited, Sent, Viewed, Paid, Voided, etc.
    val details: String = ""
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val customerId: Long? = null,
    val customerName: String = "",
    val status: String = "فعال", // "فعال", "تکمیل شده", "معلق"
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
