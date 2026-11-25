package tz.mengidata.app.i18n

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val isoTimestampFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)

    private val numberFormat: DecimalFormat
        get() = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))

    fun todayIso(): String = isoFormat.format(Date())

    fun nowIsoTimestamp(): String = isoTimestampFormat.format(Date())

    fun addDaysIso(baseIso: String, days: Int): String {
        val cal = Calendar.getInstance()
        val parsed = runCatching { isoFormat.parse(baseIso) }.getOrNull()
        if (parsed != null) cal.time = parsed
        cal.add(Calendar.DAY_OF_YEAR, days)
        return isoFormat.format(cal.time)
    }

    fun addMonthsIso(baseIso: String, months: Int): String {
        val cal = Calendar.getInstance()
        val parsed = runCatching { isoFormat.parse(baseIso) }.getOrNull()
        if (parsed != null) cal.time = parsed
        cal.add(Calendar.MONTH, months)
        return isoFormat.format(cal.time)
    }

    fun monthKey(isoDate: String): String =
        if (isoDate.length >= 7) isoDate.substring(0, 7) else todayIso().substring(0, 7)

    fun shortMonthLabel(monthKey: String, lang: AppLanguage): String {
        val monthNum = monthKey.takeLast(2).toIntOrNull() ?: return monthKey
        val sw = listOf("Jan", "Feb", "Mac", "Apr", "Mei", "Jun", "Jul", "Ago", "Sep", "Okt", "Nov", "Des")
        val en = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val list = if (lang == AppLanguage.EN) en else sw
        return list.getOrElse(monthNum - 1) { monthKey }
    }

    fun friendlyDateLabel(isoDate: String, lang: AppLanguage): String {
        val clean = isoDate.trim()
        if (clean.length < 10) return clean
        val today = todayIso()
        val yesterday = addDaysIso(today, -1)
        val tomorrow = addDaysIso(today, 1)
        val day = clean.substring(8, 10).toIntOrNull()
        val monthShort = shortMonthLabel(clean.substring(0, 7), lang)
        val year = clean.substring(0, 4)
        val compact = if (day != null) "$day $monthShort" else clean
        return when (clean.substring(0, 10)) {
            today -> "${tr(lang, "Leo", "Today")} · $compact"
            yesterday -> "${tr(lang, "Jana", "Yesterday")} · $compact"
            tomorrow -> "${tr(lang, "Kesho", "Tomorrow")} · $compact"
            else -> if (year == today.substring(0, 4)) compact else "$compact $year"
        }
    }

    fun formatMoney(amount: Double, currency: String = "TZS"): String {
        val rounded = amount.roundToLong()
        val prefix = if (rounded < 0) "-" else ""
        return "$prefix$currency ${numberFormat.format(abs(rounded))}"
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
        val parsed = runCatching { isoFormat.parse(isoDate) }.getOrNull() ?: return false
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = parsed }

        return when (period) {
            PeriodFilter.DAY -> {
                today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
            }
            PeriodFilter.WEEK -> {
                val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                !target.before(cutoff)
            }
            PeriodFilter.MONTH -> {
                today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    today.get(Calendar.MONTH) == target.get(Calendar.MONTH)
            }
            PeriodFilter.MONTHS_6 -> {
                val cutoff = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }
                !target.before(cutoff)
            }
            PeriodFilter.YEAR -> {
                today.get(Calendar.YEAR) == target.get(Calendar.YEAR)
            }
            PeriodFilter.ALL -> true
        }
    }

    fun isOverdue(dueDateIso: String): Boolean {
        if (dueDateIso.isBlank()) return false
        return dueDateIso < todayIso()
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
        "Mishahara",
        "Umeme & maji",
        "Internet & simu",
        "Usafiri",
        "Chakula",
        "Matengenezo",
        "Masoko & matangazo",
        "Kodi & vibali",
        "Afya & bima",
        "Matumizi mengine"
    )

    val paymentMethods = listOf(
        "M-Pesa",
        "Benki",
        "Cash",
        "Tigo Pesa",
        "Airtel Money",
        "HaloPesa"
    )

    fun localizedCategory(raw: String, lang: AppLanguage): String {
        val clean = raw.trim()
        if (clean.isEmpty()) return clean
        return when (clean.lowercase(Locale.ROOT)) {
            "mauzo ya bidhaa", "product sales" -> tr(lang, "Mauzo ya Bidhaa", "Product Sales")
            "huduma", "services" -> tr(lang, "Huduma", "Services")
            "ushauri", "consulting" -> tr(lang, "Ushauri", "Consulting")
            "malipo ya ankara", "invoice payments" -> tr(lang, "Malipo ya Ankara", "Invoice Payments")
            "kamisheni", "commission" -> tr(lang, "Kamisheni", "Commission")
            "mapato mengine", "other income" -> tr(lang, "Mapato Mengine", "Other Income")
            "manunuzi / stock", "manunuzi ya stock", "manunuzi ya bidhaa", "stock & purchases" -> tr(lang, "Manunuzi ya Bidhaa", "Stock & Purchases")
            "kodi ya pango", "rent" -> tr(lang, "Kodi ya Pango", "Rent")
            "mishahara", "payroll", "salaries" -> tr(lang, "Mishahara", "Payroll")
            "umeme & maji", "umeme na maji", "electricity & water", "utilities" -> tr(lang, "Umeme na Maji", "Electricity & Water")
            "internet & simu", "intaneti na simu", "internet & phone" -> tr(lang, "Intaneti na Simu", "Internet & Phone")
            "usafiri", "transport" -> tr(lang, "Usafiri", "Transport")
            "chakula", "food" -> tr(lang, "Chakula", "Food")
            "matengenezo", "maintenance" -> tr(lang, "Matengenezo", "Maintenance")
            "masoko & matangazo", "masoko na matangazo", "marketing & ads" -> tr(lang, "Masoko na Matangazo", "Marketing & Ads")
            "kodi & vibali", "kodi na vibali", "taxes & permits" -> tr(lang, "Kodi na Vibali", "Taxes & Permits")
            "afya & bima", "afya na bima", "health & insurance" -> tr(lang, "Afya na Bima", "Health & Insurance")
            "bili", "bills", "bill" -> tr(lang, "Bili", "Bills")
            "akiba", "savings" -> tr(lang, "Akiba", "Savings")
            "household bill", "bili ya nyumbani" -> tr(lang, "Bili ya Nyumbani", "Household Bill")
            "matumizi mengine", "other expenses" -> tr(lang, "Matumizi Mengine", "Other Expenses")
            "mengineyo", "other" -> tr(lang, "Mengineyo", "Other")
            else -> clean
        }
    }

    fun localizedMethod(raw: String, lang: AppLanguage): String {
        val clean = raw.trim()
        if (clean.isEmpty()) return clean
        return when (clean.lowercase(Locale.ROOT)) {
            "benki", "bank", "bank transfer", "uhamisho wa benki" -> tr(lang, "Benki", "Bank")
            "cash", "taslimu" -> tr(lang, "Taslimu", "Cash")
            else -> clean
        }
    }
}
