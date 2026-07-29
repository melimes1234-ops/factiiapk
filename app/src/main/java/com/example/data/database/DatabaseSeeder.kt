package com.example.data.database

import com.example.data.model.*
import kotlinx.coroutines.flow.firstOrNull

object DatabaseSeeder {
    suspend fun seedDatabase(db: AppDatabase) {
        val settingsDao = db.appSettingsDao()
        val customerDao = db.customerDao()
        val productDao = db.productDao()
        val invoiceDao = db.invoiceDao()
        val lineItemDao = db.invoiceLineItemDao()
        val paymentDao = db.paymentDao()
        val bankAccountDao = db.bankAccountDao()

        // If app_settings exists, the database is already initialized. Do NOT re-seed on subsequent app restarts.
        if (settingsDao.getSettingsDirect() != null) {
            return
        }

        bankAccountDao.insert(
            BankAccount(
                bankName = "بانک صنعت و معدن",
                accountHolderName = "وفا چوب ایرانیان ابهر",
                accountNumber = "0200021893006",
                cardNumber = "6279 - 6118 - 0002 - 1185",
                shabaNumber = "IR160110000000200021893006",
                notes = "پرداخت از طریق کارت به کارت یا انتقال پایا/ساتنا بلامانع است.",
                isDefault = true
            )
        )
        bankAccountDao.insert(
            BankAccount(
                bankName = "بانک سامان",
                accountHolderName = "وفا چوب ایرانیان ابهر",
                accountNumber = "8121-810-1234567-1",
                cardNumber = "6219-8610-1234-5678",
                shabaNumber = "IR620560081218101234567001",
                notes = "درگاه اختصاصی شرکت",
                isDefault = false
            )
        )

        val defaultSettings = AppSettings(
            companyName = "فن‌آوران شریف نوین",
            companyAddress = "تهران، میدان ونک، کوچه نگار، پلاک ۲۴، واحد ۵",
            companyPhone = "۰۲۱-۸۸۷۷۶۶۵۵",
            companyEmail = "finance@shariftech.com",
            companyWebsite = "www.shariftech.com",
            companyTaxId = "economic103204910", // کد اقتصادی
            companyVatNumber = "reg389201", // شماره ثبت
            companyNationalId = "10103405967", // شناسه ملی
            companyPostalCode = "۱۹۶۹۷۱۵۴۱۳",
            usePersianDigits = true,
            useJalaliCalendar = true,
            defaultCurrency = "تومان",
            defaultLanguage = "fa"
        )
        settingsDao.insertOrUpdate(defaultSettings)
            val customer1 = Customer(
                name = "مهندس محمدی",
                company = "پتروشیمی خلیج فارس",
                email = "mohammadi@pgpic.ir",
                phone = "۰۲۱-۴۴۲۲۰۲۲۰",
                mobile = "۰۹۱۲۳۴۵۶۷۸۹",
                billingAddress = "تهران، بلوار کریمخان، ساختمان پتروشیمی، طبقه ۳",
                country = "ایران",
                state = "تهران",
                city = "تهران",
                postalCode = "۱۵۸۴۸۳۷۱۱۱",
                taxId = "۱۰۳۲۰۰۴۹۵۰۱", // شناسه ملی حقوقی
                isFavorite = true
            )
            val customer2 = Customer(
                name = "خانم علوی",
                company = "تجارت الکترونیک صبا",
                email = "info@saba-holding.com",
                phone = "۰۲۱-۲۲۰۰۳۳۰۰",
                mobile = "۰۹۱۸۷۶۵۴۳۲۱",
                billingAddress = "اصفهان، خیابان چهارباغ بالا، مجتمع پارسیان، واحد ۸",
                country = "ایران",
                state = "اصفهان",
                city = "اصفهان",
                postalCode = "۸۱۷۳۶۴۵۹۲۱",
                taxId = "۳۰۱۴۸۹۲۰۱۸۲",
                isFavorite = false
            )
            val customer3 = Customer(
                name = "امیرحسین رضایی",
                company = "شخص حقیقی (فریلنسر)",
                email = "amir@rezaei.me",
                phone = "۰۵۱-۳۸۸۸۹۹۹۹",
                mobile = "۰۹۳۵۰۰۰۱۱۲۲",
                billingAddress = "مشهد، بلوار هاشمیه، هاشمیه ۱۲، پلاک ۴",
                country = "ایران",
                state = "خراسان رضوی",
                city = "مشهد",
                postalCode = "۹۱۷۷۷۸۸۸۹۹",
                nationalId = "۰۹۴۱۲۳۴۵۶۷", // کد ملی
                isFavorite = true
            )

            val c1Id = customerDao.insert(customer1)
            val c2Id = customerDao.insert(customer2)
            val c3Id = customerDao.insert(customer3)

            // Seed Products (Default Wood Profiles & Accessories)
            WoodPresets.profiles.forEachIndexed { idx, p ->
                productDao.insert(
                    Product(
                        sku = p.sku,
                        barcode = "62610000000${idx + 10}",
                        name = p.name,
                        description = "پروفیل چوب پلاست مدل ${p.name} - دسته ${p.category}",
                        unit = "متر طول",
                        category = "چوب پلاست",
                        price = p.defaultPrice,
                        cost = p.defaultPrice * 0.65,
                        taxRate = 10.0,
                        stock = 500.0,
                        isFavorite = idx < 5,
                        colorCode = "N3",
                        surfaceTreatment = "BR",
                        branchCount = 40.0,
                        categoryType = "Wood",
                        crossSectionFactor = p.crossSectionFactor
                    )
                )
            }

            // Seed Accessories & Clips
            AccessoryPresets.items.forEachIndexed { idx, acc ->
                productDao.insert(
                    Product(
                        sku = acc.sku,
                        barcode = "62620000000${idx + 10}",
                        name = acc.name,
                        description = if (acc.specialFor != "-") "ویژه: ${acc.specialFor} | دسته: ${acc.objectType}" else "دسته: ${acc.objectType}",
                        unit = acc.unit,
                        category = "پیچ و کلیپس",
                        price = acc.defaultPrice,
                        cost = if (acc.defaultPrice > 0) acc.defaultPrice * 0.6 else 0.0,
                        taxRate = 10.0,
                        stock = 2000.0,
                        isFavorite = idx < 4,
                        colorCode = "-",
                        surfaceTreatment = "-",
                        branchCount = 0.0,
                        categoryType = "Accessory"
                    )
                )
            }

            // Seed Invoices
            val invoice1 = Invoice(
                invoiceNumber = "INV-1001",
                issueDate = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L, // 5 days ago
                dueDate = System.currentTimeMillis() + 5 * 24 * 60 * 60 * 1000L,
                referenceNo = "REF-2901",
                poNumber = "PO-893",
                currency = "تومان",
                language = "fa",
                status = "Paid",
                customerId = c1Id,
                notes = "مبلغ کل فاکتور تسویه گردیده و رسید پرداخت صادر شده است. با تشکر از همکاری شما.",
                taxRate = 9.0,
                taxType = "Exclusive"
            )

            val invoice2 = Invoice(
                invoiceNumber = "INV-1002",
                issueDate = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000L, // Yesterday
                dueDate = System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L,
                referenceNo = "REF-2902",
                currency = "تومان",
                language = "fa",
                status = "Pending",
                customerId = c2Id,
                notes = "پرداخت از طریق کارت به کارت یا درگاه بانکی آنلاین بلامانع است.",
                taxRate = 9.0,
                taxType = "Exclusive"
            )

            val inv1Id = invoiceDao.insertInvoice(invoice1)
            val inv2Id = invoiceDao.insertInvoice(invoice2)

            // Seed Line Items
            val item1 = InvoiceLineItem(
                invoiceId = inv1Id,
                sku = "PRD-WEB-001",
                name = "طراحی و توسعه وب‌سایت شرکتی",
                description = "پیاده‌سازی پلتفرم آنلاین با فریمورک کاتلین و جت‌پک کامپوز ریلیز نهایی",
                quantity = 1.0,
                unit = "پروژه",
                unitPrice = 28000000.0,
                taxPercent = 9.0
            )

            val item2 = InvoiceLineItem(
                invoiceId = inv1Id,
                sku = "PRD-CNS-002",
                name = "مشاوره تخصصی معماری سیستم",
                description = "تحلیل زیرساخت ابری",
                quantity = 4.0,
                unit = "ساعت",
                unitPrice = 1500000.0,
                taxPercent = 0.0
            )

            val item3 = InvoiceLineItem(
                invoiceId = inv2Id,
                sku = "PRD-SVR-003",
                name = "سرور اختصاصی ابری ایران",
                description = "پردازنده ۸ هسته‌ای، رم ۱۶ گیگابایت، دیسک SSD پرسرعت سازمانی",
                quantity = 2.0,
                unit = "ماه",
                unitPrice = 4500000.0,
                taxPercent = 9.0
            )

            lineItemDao.insertLineItems(listOf(item1, item2))
            lineItemDao.insertLineItems(listOf(item3))

            // Seed Payments for Invoice 1 (full payment)
            val payment1 = Payment(
                invoiceId = inv1Id,
                date = System.currentTimeMillis() - 4 * 24 * 60 * 60 * 1000L,
                amount = 37060000.0, // (28000000 + 4*1500000) * 1.09 with VAT
                method = "کارت به کارت",
                receiptNo = "RECPT-89201948",
                notes = "توسط بانک سامان پرداخت شد"
            )
            paymentDao.insertPayment(payment1)
    }
}
