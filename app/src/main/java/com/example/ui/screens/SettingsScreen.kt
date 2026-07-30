package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseSeeder
import com.example.data.model.AppSettings
import com.example.data.model.BankAccount
import com.example.ui.InvoiceViewModel
import com.example.ui.components.SelectOnFocusTextField
import com.example.util.CsvImportExportUtil
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isRtl = viewModel.selectedLanguage == "fa"
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { u ->
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(u)
                    val jsonText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                    val db = AppDatabase.getDatabase(context)
                    val success = CsvImportExportUtil.restoreFullBackupJson(context, db, jsonText)
                    withContext(Dispatchers.Main) {
                        if (success) {
                            Toast.makeText(context, if (isRtl) "بازیابی اطلاعات با موفقیت انجام شد" else "Backup restored successfully", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, if (isRtl) "خطا در بازیابی فایل پشتیبان" else "Restore failed", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "خطا: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val cardBackground = MaterialTheme.colorScheme.surface

    // Form Local States
    var companyName by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var companyEmail by remember { mutableStateOf("") }
    var companyWebsite by remember { mutableStateOf("") }
    var companyTaxId by remember { mutableStateOf("") } // Economic code
    var companyVatNumber by remember { mutableStateOf("") } // Registration code
    var companyNationalId by remember { mutableStateOf("") } // National ID
    var companyPostalCode by remember { mutableStateOf("") }

    var usePersianDigits by remember { mutableStateOf(true) }
    var useJalaliCalendar by remember { mutableStateOf(true) }
    var defaultCurrency by remember { mutableStateOf("تومان") }
    var defaultLanguage by remember { mutableStateOf("fa") }
    var themeMode by remember { mutableStateOf("light") }
    var invoiceNumberPrefix by remember { mutableStateOf("MK") }
    var autoIncrementNumber by remember { mutableStateOf("1001") }
    var defaultColorCodes by remember { mutableStateOf("N1, N2, N3, C1, C2, W1, W2, G1, G2") }
    var defaultSurfaceTreatments by remember { mutableStateOf("BR (برس خورده), Emboss (طرح چوب / امبوس), Sanded (سنباده خورده), Smooth (صیقلی)") }
    var nestLogoStyle by remember { mutableStateOf("light") }

    // Update state when settings load
    LaunchedEffect(settings) {
        settings?.let { s ->
            companyName = s.companyName
            companyAddress = s.companyAddress
            companyPhone = s.companyPhone
            companyEmail = s.companyEmail
            companyWebsite = s.companyWebsite
            companyTaxId = s.companyTaxId
            companyVatNumber = s.companyVatNumber
            companyNationalId = s.companyNationalId
            companyPostalCode = s.companyPostalCode
            usePersianDigits = s.usePersianDigits
            useJalaliCalendar = s.useJalaliCalendar
            defaultCurrency = s.defaultCurrency
            defaultLanguage = s.defaultLanguage
            themeMode = s.themeMode
            invoiceNumberPrefix = s.invoiceNumberPrefix
            autoIncrementNumber = s.autoIncrementNumber.toString()
            defaultColorCodes = s.defaultColorCodes
            defaultSurfaceTreatments = s.defaultSurfaceTreatments
            nestLogoStyle = s.nestLogoStyle
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isRtl) "تنظیمات نرم‌افزار" else "System Preferences",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
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
            // --- App Branding & Developer Credits Info Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_app_icon_1784805000072),
                                    contentDescription = "App Logo",
                                    modifier = Modifier.size(68.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }
                        }

                        Text(
                            text = if (isRtl) "سیستم مدیریت فاکتور و پیش‌فاکتور" else "Invoice & Quotation System",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isRtl) "نسخه ۲.۵.۰ | نسخه حرفه‌ای و فروشگاهی" else "Version 2.5.0 | Professional Edition",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Made by Mr.Code",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = if (isRtl) "طراحی و توسعه: میثم اسم (Meisam Esm)" else "Developer: Meisam Esm",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // --- Core System Toggles ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isRtl) "ترجیحات بومی‌سازی" else "Localization & Calendar",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Language Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isRtl) "زبان پیش‌فرض" else "Default Language", fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = defaultLanguage == "fa",
                                    onClick = { defaultLanguage = "fa" },
                                    label = { Text("فارسی") }
                                )
                                FilterChip(
                                    selected = defaultLanguage == "en",
                                    onClick = { defaultLanguage = "en" },
                                    label = { Text("English") }
                                )
                            }
                        }

                        // Calendar Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isRtl) "نوع تقویم پیش‌فرض" else "System Calendar Type", fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = useJalaliCalendar,
                                    onClick = { useJalaliCalendar = true },
                                    label = { Text("شمسی (جلالی)") }
                                )
                                FilterChip(
                                    selected = !useJalaliCalendar,
                                    onClick = { useJalaliCalendar = false },
                                    label = { Text("Gregorian (میلادی)") }
                                )
                            }
                        }

                        // Digit Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isRtl) "نمایش ارقام فارسی (۱۲۳)" else "Convert numbers to Persian (۱۲۳)", fontSize = 13.sp)
                            Switch(
                                checked = usePersianDigits,
                                onCheckedChange = { usePersianDigits = it },
                                modifier = Modifier.testTag("persian_digits_switch")
                            )
                        }

                        // Currency Dropdown mock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isRtl) "واحد پولی معامله" else "Transaction Currency", fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("تومان", "ریال", "USD", "EUR").forEach { curr ->
                                    FilterChip(
                                        selected = defaultCurrency == curr,
                                        onClick = { defaultCurrency = curr },
                                        label = { Text(curr, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // Theme Mode Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isRtl) "پوسته برنامه" else "Application Theme", fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    "system" to (if (isRtl) "سیستم" else "System"),
                                    "light" to (if (isRtl) "روشن" else "Light"),
                                    "dark" to (if (isRtl) "تاریک" else "Dark")
                                ).forEach { (mode, label) ->
                                    FilterChip(
                                        selected = themeMode == mode,
                                        onClick = { 
                                            themeMode = mode
                                            viewModel.selectedThemeMode = mode
                                        },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Company profile details form ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isRtl) "پروفایل و سربرگ حقوقی صادرکننده" else "Merchant Legal Identity",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        SelectOnFocusTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text(if (isRtl) "نام شرکت یا فروشگاه" else "Merchant Company Name") },
                            modifier = Modifier.fillMaxWidth().testTag("company_name_field"),
                            singleLine = true
                        )

                        SelectOnFocusTextField(
                            value = companyPhone,
                            onValueChange = { companyPhone = it },
                            label = { Text(if (isRtl) "شماره تماس رسمی" else "Official Phone") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        SelectOnFocusTextField(
                            value = companyEmail,
                            onValueChange = { companyEmail = it },
                            label = { Text(if (isRtl) "ایمیل رسمی" else "Official Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectOnFocusTextField(
                                value = companyTaxId,
                                onValueChange = { companyTaxId = it },
                                label = { Text(if (isRtl) "کد اقتصادی" else "Economic Code") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            SelectOnFocusTextField(
                                value = companyNationalId,
                                onValueChange = { companyNationalId = it },
                                label = { Text(if (isRtl) "شناسه ملی" else "National ID") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        SelectOnFocusTextField(
                            value = companyAddress,
                            onValueChange = { companyAddress = it },
                            label = { Text(if (isRtl) "آدرس پستی صادرکننده" else "Billing Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 2
                        )
                    }
                }
            }

            // --- Automatic Invoice Numbering Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isRtl) "تنظیمات شماره‌گذاری خودکار فاکتورها" else "Automatic Invoice Numbering",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = if (isRtl) "تعیین حروف اولیه (پیشوند) و شماره عددی برای صدور فاکتورهای بعدی" else "Set base prefix letters and starting numeric sequence",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SelectOnFocusTextField(
                                value = invoiceNumberPrefix,
                                onValueChange = { invoiceNumberPrefix = it },
                                label = { Text(if (isRtl) "حروف اول (پیشوند)" else "Prefix Letters") },
                                placeholder = { Text("MK") },
                                modifier = Modifier.weight(1f).testTag("invoice_prefix_field"),
                                singleLine = true
                            )

                            SelectOnFocusTextField(
                                value = autoIncrementNumber,
                                onValueChange = { autoIncrementNumber = it },
                                label = { Text(if (isRtl) "شماره عددی شروع" else "Starting Number") },
                                placeholder = { Text("1001") },
                                modifier = Modifier.weight(1.2f).testTag("auto_increment_number_field"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        // Live Preview Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isRtl) "پیش‌نمایش شماره فاکتور بعدی:" else "Next Invoice Number Preview:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$invoiceNumberPrefix$autoIncrementNumber",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // --- List Management for Color Codes & Surface Finishes ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isRtl) "مدیریت لیست کدهای رنگ و عملیات سطحی" else "Color Code & Surface Finish Lists",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = if (isRtl) "مقادیر را با ویرگول (، یا ,) از هم جدا کنید تا در لیست آکاردئونی فاکتور قابل انتخاب باشند." else "Separate options with commas to populate invoice dropdown lists.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        SelectOnFocusTextField(
                            value = defaultColorCodes,
                            onValueChange = { defaultColorCodes = it },
                            label = { Text(if (isRtl) "کدهای رنگ (با ویرگول جدا کنید)" else "Color Codes (comma separated)") },
                            placeholder = { Text("N1, N2, N3, C1, C2, W1") },
                            modifier = Modifier.fillMaxWidth().testTag("color_codes_setting_input"),
                            singleLine = false,
                            maxLines = 3
                        )

                        SelectOnFocusTextField(
                            value = defaultSurfaceTreatments,
                            onValueChange = { defaultSurfaceTreatments = it },
                            label = { Text(if (isRtl) "عملیات‌های سطحی (با ویرگول جدا کنید)" else "Surface Finishes (comma separated)") },
                            placeholder = { Text("BR (برس خورده), Emboss (طرح چوب), Sanded (سنباده)") },
                            modifier = Modifier.fillMaxWidth().testTag("surface_treatments_setting_input"),
                            singleLine = false,
                            maxLines = 3
                        )
                    }
                }
            }

            // --- NEST Invoice Logo Selection Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isRtl) "انتخاب لوگوی سربرگ فاکتور NEST" else "NEST Invoice Header Logo Selection",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = if (isRtl) "یکی از ۲ حالت لوگوی نست (پس‌زمینه تیره / پس‌زمینه روشن) را جهت درج در سربرگ فاکتور رسمی انتخاب نمایید:" else "Choose one of the 2 NEST logo themes (Dark or Light) for official invoice print headers:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Dark Logo Option
                            val isDarkSelected = nestLogoStyle == "dark"
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { nestLogoStyle = "dark" },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                border = if (isDarkSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(84.dp)
                                            .background(Color.Black, shape = RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(Color(0xFF525252), shape = RoundedCornerShape(3.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Mini geometric vector visualization
                                                Text(text = "📐", fontSize = 20.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "N E S T",
                                                color = Color(0xFFCCCCCC),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.5.sp
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        RadioButton(
                                            selected = isDarkSelected,
                                            onClick = { nestLogoStyle = "dark" }
                                        )
                                        Text(
                                            text = if (isRtl) "لوگوی تیره (Dark)" else "Dark Theme Logo",
                                            fontSize = 12.sp,
                                            fontWeight = if (isDarkSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            // Light Logo Option
                            val isLightSelected = nestLogoStyle == "light"
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { nestLogoStyle = "light" },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLightSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                border = if (isLightSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(84.dp)
                                            .background(Color.White, shape = RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(Color(0xFF5D5D5D), shape = RoundedCornerShape(3.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "📐", fontSize = 20.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "N E S T",
                                                color = Color(0xFF333333),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.5.sp
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        RadioButton(
                                            selected = isLightSelected,
                                            onClick = { nestLogoStyle = "light" }
                                        )
                                        Text(
                                            text = if (isRtl) "لوگوی روشن (Light)" else "Light Theme Logo",
                                            fontSize = 12.sp,
                                            fontWeight = if (isLightSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Save Preferences ---
            item {
                Button(
                    onClick = {
                        val s = AppSettings(
                            companyName = companyName,
                            companyAddress = companyAddress,
                            companyPhone = companyPhone,
                            companyEmail = companyEmail,
                            companyWebsite = companyWebsite,
                            companyTaxId = companyTaxId,
                            companyVatNumber = companyVatNumber,
                            companyNationalId = companyNationalId,
                            companyPostalCode = companyPostalCode,
                            usePersianDigits = usePersianDigits,
                            useJalaliCalendar = useJalaliCalendar,
                            defaultCurrency = defaultCurrency,
                            defaultLanguage = defaultLanguage,
                            themeMode = themeMode,
                            invoiceNumberPrefix = invoiceNumberPrefix,
                            autoIncrementNumber = autoIncrementNumber.toIntOrNull() ?: 1001,
                            defaultColorCodes = defaultColorCodes,
                            defaultSurfaceTreatments = defaultSurfaceTreatments,
                            nestLogoStyle = nestLogoStyle
                        )
                        viewModel.saveAppSettings(s)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = if (isRtl) "ذخیره تنظیمات" else "Save Settings", color = Color.White)
                }
            }

            // --- Backup and Restore ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isRtl) "پشتیبان‌گیری و بازیابی اطلاعات" else "Data Backup & Restore",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isRtl) "خروجی کامل دیتابیس شامل مشتریان، محصولات، فاکتورها و سوابق یا بازیابی از فایل پشتیبان JSON:" else "Export complete database backup or restore from JSON file:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val db = AppDatabase.getDatabase(context)
                                            CsvImportExportUtil.exportFullBackupJson(context, db)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isRtl) "تهیه فایل پشتیبان" else "Backup JSON", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    restoreBackupLauncher.launch("application/json")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isRtl) "بازیابی پشتیبان" else "Restore JSON", fontSize = 11.sp)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text(
                            text = if (isRtl) "دانلود نمونه فایل‌های ورودی اکسل (CSV):" else "Download CSV Import Sample Templates:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { CsvImportExportUtil.generateSampleProductsCsv(context) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = if (isRtl) "نمونه کالاها" else "Products CSV", fontSize = 10.sp, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { CsvImportExportUtil.generateSampleCustomersCsv(context) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = if (isRtl) "نمونه مشتریان" else "Customers CSV", fontSize = 10.sp, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { CsvImportExportUtil.generateSampleProjectsCsv(context) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = if (isRtl) "نمونه پروژه‌ها" else "Projects CSV", fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // --- Bank Accounts Management Section ---
            item {
                val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
                var showAddEditDialog by remember { mutableStateOf(false) }
                var editingBankAccount by remember { mutableStateOf<BankAccount?>(null) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = if (isRtl) "مدیریت حساب‌های بانکی فاکتور" else "Bank Accounts Management",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isRtl) "اطلاعات بانکی، شماره کارت، شبا و حساب‌های نمایش داده شده در پایین فاکتور" else "Manage bank accounts shown at invoice payment footer",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = {
                                    editingBankAccount = null
                                    showAddEditDialog = true
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = if (isRtl) "افزودن حساب" else "Add Bank", fontSize = 10.sp)
                            }
                        }

                        if (bankAccounts.isEmpty()) {
                            Text(
                                text = if (isRtl) "هیچ حساب بانکی ثبت نشده است." else "No bank accounts added yet.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            bankAccounts.forEach { bank ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = bank.bankName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp
                                                )
                                                if (bank.isDefault) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        shape = RoundedCornerShape(3.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isRtl) "پیش‌فرض" else "Default",
                                                            fontSize = 8.sp,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        editingBankAccount = bank
                                                        showAddEditDialog = true
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "ویرایش", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteBankAccount(bank) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                                                }
                                            }
                                        }
                                        Text(text = "صاحب حساب: ${bank.accountHolderName}", fontSize = 12.sp)
                                        Text(text = "شماره کارت: ${bank.cardNumber}", fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        Text(text = "شماره شبا: ${bank.shabaNumber}", fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        Text(text = "شماره حساب: ${bank.accountNumber}", fontSize = 12.sp)
                                        if (!bank.isDefault) {
                                            TextButton(
                                                onClick = { viewModel.setDefaultBankAccount(bank) },
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Text(text = if (isRtl) "تنظیم به عنوان پیش‌فرض فاکتور" else "Set as Default", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Add / Edit Bank Account Dialog
                if (showAddEditDialog) {
                    var bankName by remember { mutableStateOf(editingBankAccount?.bankName ?: "") }
                    var accountHolder by remember { mutableStateOf(editingBankAccount?.accountHolderName ?: "") }
                    var accountNumber by remember { mutableStateOf(editingBankAccount?.accountNumber ?: "") }
                    var cardNumber by remember { mutableStateOf(editingBankAccount?.cardNumber ?: "") }
                    var shabaNumber by remember { mutableStateOf(editingBankAccount?.shabaNumber ?: "") }
                    var notes by remember { mutableStateOf(editingBankAccount?.notes ?: "") }
                    var isDefault by remember { mutableStateOf(editingBankAccount?.isDefault ?: bankAccounts.isEmpty()) }

                    AlertDialog(
                        onDismissRequest = { showAddEditDialog = false },
                        title = { Text(text = if (editingBankAccount == null) (if (isRtl) "افزودن حساب بانکی" else "Add Bank Account") else (if (isRtl) "ویرایش حساب بانکی" else "Edit Bank Account")) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SelectOnFocusTextField(
                                    value = bankName,
                                    onValueChange = { bankName = it },
                                    label = { Text(if (isRtl) "نام بانک (مثلا بانک صنعت و معدن)" else "Bank Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                SelectOnFocusTextField(
                                    value = accountHolder,
                                    onValueChange = { accountHolder = it },
                                    label = { Text(if (isRtl) "نام صاحب حساب / شرکت" else "Account Holder") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                SelectOnFocusTextField(
                                    value = accountNumber,
                                    onValueChange = { accountNumber = it },
                                    label = { Text(if (isRtl) "شماره حساب" else "Account Number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                SelectOnFocusTextField(
                                    value = cardNumber,
                                    onValueChange = { cardNumber = it },
                                    label = { Text(if (isRtl) "شماره کارت (۱۶ رقمی)" else "Card Number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                SelectOnFocusTextField(
                                    value = shabaNumber,
                                    onValueChange = { shabaNumber = it },
                                    label = { Text(if (isRtl) "شماره شبا (بدون IR یا با IR)" else "Shaba / IBAN") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                SelectOnFocusTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text(if (isRtl) "توضیحات و شرایط واریز" else "Payment Notes") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = false,
                                    maxLines = 2
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (isRtl) "پیش‌فرض فاکتورها" else "Make Default")
                                    Switch(
                                        checked = isDefault,
                                        onCheckedChange = { isDefault = it }
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (isDefault) {
                                        bankAccounts.forEach { b ->
                                            if (b.id != editingBankAccount?.id && b.isDefault) {
                                                viewModel.updateBankAccount(b.copy(isDefault = false))
                                            }
                                        }
                                    }
                                    val newAccount = BankAccount(
                                        id = editingBankAccount?.id ?: 0L,
                                        bankName = bankName,
                                        accountHolderName = accountHolder,
                                        accountNumber = accountNumber,
                                        cardNumber = cardNumber,
                                        shabaNumber = shabaNumber,
                                        notes = notes,
                                        isDefault = isDefault
                                    )
                                    if (editingBankAccount == null) {
                                        viewModel.insertBankAccount(newAccount)
                                    } else {
                                        viewModel.updateBankAccount(newAccount)
                                    }
                                    showAddEditDialog = false
                                }
                            ) {
                                Text(if (isRtl) "ذخیره" else "Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddEditDialog = false }) {
                                Text(if (isRtl) "انصراف" else "Cancel")
                            }
                        }
                    )
                }
            }

            // --- Diagnostic / Seed Utilities ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isRtl) "منطقه ابزارهای توسعه و خطایابی" else "Diagnostic Utilities Area",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (isRtl) "جهت پر کردن مجدد پایگاه داده با نمونه رکوردهای سازمانی روی دکمه زیر کلیک کنید." else "Reset and pre-seed SQLite database with high-fidelity corporate records:",
                            fontSize = 11.sp
                        )

                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val db = AppDatabase.getDatabase(context)
                                        db.clearAllTables()
                                        DatabaseSeeder.seedDatabase(db)
                                        viewModel.logAction("RESET_DATABASE", "اطلاعات دیتابیس به حالت اولیه بازنشانی شد.")
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(
                                                context,
                                                if (isRtl) "اطلاعات با موفقیت بازنشانی و مجدداً بارگذاری شد." else "Database successfully reset and re-seeded.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(
                                                context,
                                                if (isRtl) "خطا در بازنشانی: ${e.localizedMessage}" else "Reset error: ${e.localizedMessage}",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp).testTag("db_reset_seed_button")
                        ) {
                            Text(text = if (isRtl) "بازنشانی و تغذیه دیتابیس" else "Reset & Re-Seed SQLite", color = Color.White, fontSize = 11.sp)
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
