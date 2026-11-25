package tz.mengidata.app.i18n

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

enum class AppLanguage(val code: String) {
    SW("sw"),
    EN("en")
}

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class PeriodFilter(val key: String, val labelSw: String, val labelEn: String) {
    DAY("day", "Leo", "Today"),
    WEEK("week", "Wiki", "Week"),
    MONTH("month", "Mwezi", "Month"),
    MONTHS_6("months6", "Miezi 6", "6 Months"),
    YEAR("year", "Mwaka", "Year"),
    ALL("all", "Yote", "All")
}

fun tr(lang: AppLanguage, sw: String, en: String): String =
    if (lang == AppLanguage.EN) en else sw

object FormatUtils {
    private val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val isoTimestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)

    private val numberFormatLocal: ThreadLocal<DecimalFormat> = ThreadLocal.withInitial {
        DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    }

    private val monthsSw = arrayOf("Jan", "Feb", "Mac", "Apr", "Mei", "Jun", "Jul", "Ago", "Sep", "Okt", "Nov", "Des")
    private val monthsEn = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun todayIso(): String = LocalDate.now().format(isoDateFormatter)

    fun nowIsoTimestamp(): String = LocalDateTime.now().format(isoTimestampFormatter)

    fun addDaysIso(baseIso: String, days: Int): String {
        val date = parseLocalDateFast(baseIso) ?: LocalDate.now()
        return date.plusDays(days.toLong()).format(isoDateFormatter)
    }

    fun addMonthsIso(baseIso: String, months: Int): String {
        val date = parseLocalDateFast(baseIso) ?: LocalDate.now()
        return date.plusMonths(months.toLong()).format(isoDateFormatter)
    }

    fun monthKey(isoDate: String): String =
        if (isoDate.length >= 7) isoDate.substring(0, 7) else todayIso().substring(0, 7)

    fun shortMonthLabel(monthKey: String, lang: AppLanguage): String {
        val monthNum = if (monthKey.length >= 2) monthKey.substring(monthKey.length - 2).toIntOrNull() else null
        if (monthNum == null || monthNum !in 1..12) return monthKey
        val arr = if (lang == AppLanguage.EN) monthsEn else monthsSw
        return arr[monthNum - 1]
    }

    fun friendlyDateLabel(isoDate: String, lang: AppLanguage): String {
        val clean = isoDate.trim()
        if (clean.length < 10) return clean
        val datePart = clean.substring(0, 10)
        val today = LocalDate.now()
        val todayStr = today.format(isoDateFormatter)
        val day = datePart.substring(8, 10).toIntOrNull()
        val monthShort = shortMonthLabel(datePart.substring(0, 7), lang)
        val year = datePart.substring(0, 4)
        val compact = if (day != null) "$day $monthShort" else clean
        return when (datePart) {
            todayStr -> "${tr(lang, "Leo", "Today")} · $compact"
            today.minusDays(1).format(isoDateFormatter) -> "${tr(lang, "Jana", "Yesterday")} · $compact"
            today.plusDays(1).format(isoDateFormatter) -> "${tr(lang, "Kesho", "Tomorrow")} · $compact"
            else -> if (year == todayStr.substring(0, 4)) compact else "$compact $year"
        }
    }

    fun formatMoney(amount: Double, currency: String = "TZS"): String {
        val rounded = amount.roundToLong()
        val prefix = if (rounded < 0) "-" else ""
        val formatted = numberFormatLocal.get()?.format(abs(rounded)) ?: abs(rounded).toString()
        return "$prefix$currency $formatted"
    }

    fun formatCompactMoney(amount: Double, currency: String = "TZS"): String {
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 1_000_000_000 -> String.format(Locale.US, "%s%s %.1fB", sign, currency, absVal / 1_000_000_000.0)
            absVal >= 1_000_000 -> String.format(Locale.US, "%s%s %.1fM", sign, currency, absVal / 1_000_000.0)
            absVal >= 10_000 -> String.format(Locale.US, "%s%s %.0fK", sign, currency, absVal / 1_000.0)
            else -> formatMoney(amount, currency)
        }
    }

    fun isDateInPeriod(isoDate: String, period: PeriodFilter): Boolean {
        if (period == PeriodFilter.ALL) return true
        if (isoDate.length < 10) return false
        val datePart = isoDate.substring(0, 10)
        val today = LocalDate.now()
        return when (period) {
            PeriodFilter.DAY -> datePart == today.format(isoDateFormatter)
            PeriodFilter.WEEK -> datePart >= today.minusDays(7).format(isoDateFormatter)
            PeriodFilter.MONTH -> {
                val monthPrefix = today.format(isoDateFormatter).substring(0, 7)
                datePart.startsWith(monthPrefix)
            }
            PeriodFilter.MONTHS_6 -> datePart >= today.minusMonths(6).format(isoDateFormatter)
            PeriodFilter.YEAR -> datePart.startsWith(today.year.toString())
            PeriodFilter.ALL -> true
        }
    }

    fun isOverdue(dueDateIso: String): Boolean {
        if (dueDateIso.isBlank()) return false
        return dueDateIso < todayIso()
    }

    private fun parseLocalDateFast(iso: String): LocalDate? {
        if (iso.length < 10) return null
        return runCatching { LocalDate.parse(iso.substring(0, 10), isoDateFormatter) }.getOrNull()
    }

    val incomeCategories = listOf(
        "Mauzo ya bidhaa",
        "Huduma",
        "Ushauri",
        "Malipo ya ankara",
        "Mapato mengine"
    )

    val expenseCategories = listOf(
        "Manunuzi / stock",
        "Kodi ya pango",
        "Umeme & maji",
        "Mishahara",
        "Usafiri",
        "Masoko",
        "Bili za nyumbani",
        "Matumizi mengine"
    )

    val paymentMethods = listOf("cash", "mobile", "bank", "card")

    fun localizedMethod(code: String, lang: AppLanguage): String = when (code.trim().lowercase(Locale.ROOT)) {
        "cash", "taslimu", "pesa taslimu" -> tr(lang, "Taslimu", "Cash")
        "mobile", "simu", "mobile money" -> tr(lang, "Simu (M-Pesa/Tigo/Airtel)", "Mobile Money")
        "bank", "benki", "bank transfer", "uhamisho wa benki" -> tr(lang, "Uhamisho wa Benki", "Bank Transfer")
        "card", "kadi" -> tr(lang, "Kadi", "Card")
        else -> code
    }

    fun localizedCategory(category: String, lang: AppLanguage): String {
        val clean = category.trim()
        return when (clean.lowercase(Locale.ROOT)) {
            "mauzo ya bidhaa", "product sales", "sales" -> tr(lang, "Mauzo ya bidhaa", "Product Sales")
            "huduma", "services" -> tr(lang, "Huduma", "Services")
            "ushauri", "consulting" -> tr(lang, "Ushauri", "Consulting")
            "malipo ya ankara", "invoice payment", "invoice payments" -> tr(lang, "Malipo ya ankara", "Invoice Payment")
            "kamisheni", "commission" -> tr(lang, "Kamisheni", "Commission")
            "mapato mengine", "other income" -> tr(lang, "Mapato mengine", "Other Income")
            "manunuzi / stock", "manunuzi ya stock", "manunuzi ya bidhaa", "inventory / stock", "inventory" ->
                tr(lang, "Manunuzi ya bidhaa", "Inventory & Stock")
            "kodi ya pango", "rent" -> tr(lang, "Kodi ya pango", "Rent")
            "umeme & maji", "utilities" -> tr(lang, "Umeme & maji", "Utilities")
            "mishahara", "salaries", "payroll" -> tr(lang, "Mishahara", "Salaries")
            "usafiri", "transport" -> tr(lang, "Usafiri", "Transport")
            "chakula", "food", "meals" -> tr(lang, "Chakula", "Food & Meals")
            "internet & simu", "mtandao & simu", "internet & phone", "communication" ->
                tr(lang, "Mtandao & simu", "Internet & Phone")
            "masoko", "marketing" -> tr(lang, "Masoko", "Marketing")
            "household bill", "bili ya nyumbani", "bili za nyumbani", "matumizi ya nyumbani", "household bills" ->
                tr(lang, "Bili ya Nyumbani", "Household Bill")
            "bili", "bili zangu", "my bills", "bills" -> tr(lang, "Bili Zangu", "My Bills")
            "akiba", "akiba / akiba vault", "savings", "savings transfer" -> tr(lang, "Akiba", "Savings")
            "matumizi mengine", "mengineyo", "other expense", "other" -> tr(lang, "Mengineyo", "Other Expense")
            else -> clean
        }
    }

    fun localizedBillFrequency(freq: String, lang: AppLanguage): String = when (freq.lowercase(Locale.ROOT)) {
        "weekly" -> tr(lang, "Kila wiki", "Weekly")
        "monthly" -> tr(lang, "Kila mwezi", "Monthly")
        "quarterly" -> tr(lang, "Kila robo mwaka", "Quarterly")
        "yearly" -> tr(lang, "Kila mwaka", "Yearly")
        else -> tr(lang, "Mara moja", "One-time")
    }
}
