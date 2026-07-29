package com.example.util

import java.text.SimpleDateFormat
import java.util.*

object Helper {

    data class JalaliDate(val year: Int, val month: Int, val day: Int) {
        override fun toString() = "$year/$month/$day"
        fun toFormattedString() = "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"
    }

    // Convert Gregorian Calendar to Jalali
    fun g2j(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 335)
        val gy2 = if (gm > 2) gy + 1 else gy
        var gDays = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
        var jy = -1595 + (33 * (gDays / 12053))
        gDays %= 12053
        jy += 4 * (gDays / 1461)
        gDays %= 1461
        if (gDays > 365) {
            jy += ((gDays - 1) / 365)
            gDays = (gDays - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (gDays < 186) {
            jm = 1 + (gDays / 31)
            jd = 1 + (gDays % 31)
        } else {
            jm = 7 + ((gDays - 186) / 30)
            jd = 1 + ((gDays - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    // Convert Epoch Milliseconds to JalaliDate
    fun fromTimestamp(timestamp: Long): JalaliDate {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return g2j(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Full Persian Date formatting with month name
    fun formatJalaliFull(timestamp: Long, usePersianDigits: Boolean = true): String {
        val jd = fromTimestamp(timestamp)
        val monthName = when (jd.month) {
            1 -> "فروردین"
            2 -> "اردیبهشت"
            3 -> "خرداد"
            4 -> "تیر"
            5 -> "مرداد"
            6 -> "شهریور"
            7 -> "مهر"
            8 -> "آبان"
            9 -> "آذر"
            10 -> "دی"
            11 -> "بهمن"
            12 -> "اسفند"
            else -> ""
        }
        val result = "${jd.day} $monthName ${jd.year}"
        return if (usePersianDigits) toPersianDigits(result) else result
    }

    // Formatted String (1405/04/26)
    fun formatJalaliShort(timestamp: Long, usePersianDigits: Boolean = true): String {
        val jd = fromTimestamp(timestamp)
        val result = jd.toFormattedString()
        return if (usePersianDigits) toPersianDigits(result) else result
    }

    // Day of week helpers
    fun getDayOfWeekPersian(cal: Calendar): String {
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یک‌شنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> ""
        }
    }

    fun getDayOfWeekEnglish(cal: Calendar): String {
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            else -> ""
        }
    }

    fun formatCurrentHeaderDate(timestamp: Long = System.currentTimeMillis(), useJalali: Boolean = true, usePersianDigits: Boolean = true): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return if (useJalali) {
            val fullDate = formatJalaliFull(timestamp, usePersianDigits)
            val dayOfWeek = getDayOfWeekPersian(cal)
            "$fullDate | $dayOfWeek"
        } else {
            val dayOfWeek = getDayOfWeekEnglish(cal)
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val formattedDate = sdf.format(cal.time)
            val result = "$dayOfWeek, $formattedDate"
            if (usePersianDigits) toPersianDigits(result) else result
        }
    }

    // Gregorian Formatter
    fun formatGregorian(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Translate English numbers to Persian digits
    fun toPersianDigits(text: String): String {
        var result = text
        val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        for (i in 0..9) {
            result = result.replace(englishDigits[i], persianDigits[i])
        }
        return result
    }

    // Parse Persian digits back to English
    fun toEnglishDigits(text: String): String {
        var result = text
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        for (i in 0..9) {
            result = result.replace(persianDigits[i], englishDigits[i])
        }
        return result
    }

    // Localized currency and digit helper
    fun formatCurrency(amount: Double, currency: String = "تومان", usePersianDigits: Boolean = true): String {
        val formattedNumber = String.format(Locale.US, "%,.0f", amount)
        val result = if (currency.isEmpty()) {
            formattedNumber
        } else {
            "$formattedNumber $currency"
        }
        return if (usePersianDigits) toPersianDigits(result) else result
    }

    // Format numbers with 3-digit comma separators for inputs
    fun formatWithCommas(value: Double, usePersian: Boolean = false): String {
        if (value == 0.0) return ""
        val formatted = if (value % 1 == 0.0) {
            String.format(Locale.US, "%,.0f", value)
        } else {
            String.format(Locale.US, "%,.2f", value)
        }
        return if (usePersian) toPersianDigits(formatted) else formatted
    }

    // Parse inputs containing commas or Persian digits back to double
    fun parseFormattedToDouble(input: String): Double {
        val englishStr = toEnglishDigits(input).replace(",", "").replace(" ", "").trim()
        return englishStr.toDoubleOrNull() ?: 0.0
    }

    // Format double values cleanly without trailing zeros if they are whole
    fun formatDouble(value: Double, usePersian: Boolean = true): String {
        val result = if (value % 1 == 0.0) {
            String.format(Locale.US, "%.0f", value)
        } else {
            String.format(Locale.US, "%.2f", value)
        }
        return if (usePersian) toPersianDigits(result) else result
    }

    // Increment an invoice number string (e.g. "MK123" -> "MK124", "1001" -> "1002", "INV-005" -> "INV-006")
    fun getNextInvoiceNumber(currentInvNo: String, defaultPrefix: String = "", fallbackNumber: Int = 1001): String {
        val normalized = toEnglishDigits(currentInvNo).trim()
        val activePrefix = defaultPrefix.ifBlank { "" }

        if (normalized.isEmpty()) return "$activePrefix$fallbackNumber"

        val regex = Regex("""^(.*?)(?:(\d+))?$""")
        val match = regex.find(normalized)
        if (match != null) {
            val prefix = match.groupValues[1]
            val digits = match.groupValues[2]
            if (digits.isNotEmpty()) {
                val num = digits.toLongOrNull() ?: fallbackNumber.toLong()
                val nextNum = maxOf(num + 1, fallbackNumber.toLong())
                val formattedNext = if (digits.startsWith("0") && digits.length > 1) {
                    String.format("%0${digits.length}d", nextNum)
                } else {
                    nextNum.toString()
                }
                val finalPrefix = if (activePrefix.isNotEmpty()) activePrefix else prefix
                return "$finalPrefix$formattedNext"
            } else if (prefix.isNotEmpty()) {
                val finalPrefix = if (activePrefix.isNotEmpty()) activePrefix else prefix
                return "$finalPrefix$fallbackNumber"
            }
        }
        return "$activePrefix$fallbackNumber"
    }
}
