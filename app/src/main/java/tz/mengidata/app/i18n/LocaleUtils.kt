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

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val nameSw: String,
    val nameEn: String,
    val isInternational: Boolean = true
)

fun tr(lang: AppLanguage, sw: String, en: String): String =
    if (lang == AppLanguage.EN) en else sw

object FormatUtils {
    val supportedCurrencies: List<CurrencyOption> = listOf(
        CurrencyOption("TZS", "TSh", "Shilingi ya Tanzania", "Tanzanian Shilling", isInternational = false),
        CurrencyOption("USD", "$", "Dola ya Marekani", "US Dollar", isInternational = true),
        CurrencyOption("EUR", "€", "Yuro ya Ulaya", "Euro", isInternational = true),
        CurrencyOption("GBP", "£", "Pauni ya Uingereza", "British Pound", isInternational = true),
        CurrencyOption("AED", "د.إ", "Dirham ya UAE", "UAE Dirham", isInternational = true),
        CurrencyOption("CNY", "¥", "Yuan ya China", "Chinese Yuan", isInternational = true)
    )

    val internationalCurrencies: List<CurrencyOption>
        get() = supportedCurrencies.filter { it.isInternational }

    fun normalizeCurrencyCode(raw: String?): String {
        val clean = raw?.trim()?.uppercase(Locale.ROOT).orEmpty()
        return supportedCurrencies.firstOrNull { it.code == clean }?.code ?: "TZS"
    }

    fun findCurrency(code: String?): CurrencyOption {
        val clean = code?.trim()?.uppercase(Locale.ROOT).orEmpty()
        return supportedCurrencies.firstOrNull { it.code == clean } ?: supportedCurrencies.first()
    }

    private val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val isoTimestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)

    private val numberFormatLocal: ThreadLocal<DecimalFormat> = ThreadLocal.withInitial {
        DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    }

    private val decimalFormatLocal: ThreadLocal<DecimalFormat> = ThreadLocal.withInitial {
        DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
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
        val cur = currency.trim().ifBlank { "TZS" }.uppercase(Locale.ROOT)
        if (cur == "TZS") {
            val rounded = amount.roundToLong()
            val prefix = if (rounded < 0) "-" else ""
            val formatted = numberFormatLocal.get()?.format(abs(rounded)) ?: abs(rounded).toString()
            return "$prefix$cur $formatted"
        }
        val absVal = abs(amount)
        val prefix = if (amount < -0.004) "-" else ""
        val hasDecimals = abs(absVal - absVal.roundToLong()) > 0.004
        val formatted = if (hasDecimals) {
            decimalFormatLocal.get()?.format(absVal) ?: String.format(Locale.US, "%.2f", absVal)
        } else {
            val rounded = absVal.roundToLong()
            numberFormatLocal.get()?.format(rounded) ?: rounded.toString()
        }
        return "$prefix$cur $formatted"
    }

    fun formatCompactMoney(amount: Double, currency: String = "TZS"): String {
        val cur = currency.trim().ifBlank { "TZS" }.uppercase(Locale.ROOT)
        val absVal = abs(amount)
        val sign = if (amount < -0.004) "-" else ""
        return when {
            absVal >= 1_000_000_000 -> String.format(Locale.US, "%s%s %.1fB", sign, cur, absVal / 1_000_000_000.0)
            absVal >= 1_000_000 -> String.format(Locale.US, "%s%s %.1fM", sign, cur, absVal / 1_000_000.0)
            absVal >= 10_000 -> String.format(Locale.US, "%s%s %.0fK", sign, cur, absVal / 1_000.0)
            else -> formatMoney(amount, cur)
        }
    }

    fun formatEditableAmount(amount: Double, currency: String = "TZS"): String {
        if (!amount.isFinite() || amount <= 0.0) return ""
        val cur = currency.trim().ifBlank { "TZS" }.uppercase(Locale.ROOT)
        if (cur == "TZS") {
            return amount.roundToLong().toString()
        }
        val roundedTwo = (amount * 100.0).roundToLong() / 100.0
        return if (abs(roundedTwo - roundedTwo.roundToLong()) <= 0.004) {
            roundedTwo.roundToLong().toString()
        } else {
            String.format(Locale.US, "%.2f", roundedTwo)
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
        "Malipo ya ankara",
        "Ushauri",
        "Kamisheni",
        "Pango & kodi",
        "Marejesho",
        "Mapato mengine"
    )

    val expenseCategories = listOf(
        "Kodi ya pango",
        "Umeme & maji",
        "Mtandao & simu",
        "Mishahara",
        "Manunuzi ya bidhaa",
        "Usafiri",
        "Chakula",
        "Matengenezo",
        "Masoko & matangazo",
        "Kodi & vibali",
        "Afya & bima",
        "Ulinzi & usafi",
        "Mkopo & marejesho",
        "Vifaa vya ofisi",
        "Bili Zangu",
        "Bili ya Nyumbani",
        "Akiba",
        "Mengineyo"
    )

    val billCategories = listOf(
        "Kodi ya pango",
        "Umeme & maji",
        "Mtandao & simu",
        "Mishahara",
        "Kodi & vibali",
        "Ulinzi & usafi",
        "Afya & bima",
        "Mkopo & marejesho",
        "Manunuzi ya bidhaa",
        "Matengenezo",
        "Usafiri",
        "Chakula",
        "Masoko & matangazo",
        "Vifaa vya ofisi",
        "Bili ya Nyumbani",
        "Mengineyo"
    )

    val paymentMethods = listOf(
        "Taslimu",
        "M-Pesa",
        "Tigo Pesa",
        "Airtel Money",
        "HaloPesa",
        "AzamPesa",
        "T-Pesa",
        "Benki",
        "CRDB / NMB",
        "Lipa Namba",
        "Kadi",
        "Hundi"
    )

    fun normalizePaymentMethod(raw: String): String {
        val clean = raw.trim()
        return when (clean.lowercase(Locale.ROOT)) {
            "cash", "taslimu", "pesa taslimu" -> "Taslimu"
            "m-pesa", "mpesa" -> "M-Pesa"
            "tigo pesa", "tigopesa", "mixx by yas", "tigo" -> "Tigo Pesa"
            "airtel money", "airtel" -> "Airtel Money"
            "halopesa", "halo pesa" -> "HaloPesa"
            "azampesa", "azam pesa" -> "AzamPesa"
            "t-pesa", "tpesa" -> "T-Pesa"
            "mobile", "simu", "mobile money" -> "M-Pesa"
            "bank", "benki", "bank transfer", "uhamisho wa benki" -> "Benki"
            "crdb", "nmb", "crdb / nmb", "benki (crdb/nmb)" -> "CRDB / NMB"
            "lipa namba", "lipanamba", "selcom", "masterpass" -> "Lipa Namba"
            "card", "kadi", "visa", "mastercard" -> "Kadi"
            "cheque", "check", "hundi" -> "Hundi"
            else -> clean.ifBlank { "Taslimu" }
        }
    }

    fun samePaymentMethod(a: String, b: String): Boolean {
        return normalizePaymentMethod(a).equals(normalizePaymentMethod(b), ignoreCase = true)
    }

    fun localizedMethod(code: String, lang: AppLanguage): String = when (code.trim().lowercase(Locale.ROOT)) {
        "cash", "taslimu", "pesa taslimu" -> tr(lang, "Taslimu", "Cash")
        "m-pesa", "mpesa" -> "M-Pesa"
        "tigo pesa", "tigopesa", "mixx by yas" -> "Tigo Pesa"
        "airtel money", "airtel" -> "Airtel Money"
        "halopesa", "halo pesa" -> "HaloPesa"
        "azampesa", "azam pesa" -> "AzamPesa"
        "t-pesa", "tpesa" -> "T-Pesa"
        "mobile", "simu", "mobile money" -> tr(lang, "Simu (M-Pesa/Tigo/Airtel)", "Mobile Money")
        "bank", "benki", "bank transfer", "uhamisho wa benki" -> tr(lang, "Uhamisho wa Benki", "Bank Transfer")
        "crdb / nmb", "crdb", "nmb", "benki (crdb/nmb)" -> tr(lang, "CRDB / NMB", "CRDB / NMB Bank")
        "lipa namba", "lipanamba" -> tr(lang, "Lipa Namba", "Merchant Till (Lipa Namba)")
        "card", "kadi" -> tr(lang, "Kadi", "Card")
        "hundi", "cheque", "check" -> tr(lang, "Hundi", "Cheque")
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
            "pango & kodi", "kodi / pango", "rental income" -> tr(lang, "Pango & kodi", "Rental Income")
            "marejesho", "refunds & returns", "refunds" -> tr(lang, "Marejesho", "Refunds & Returns")
            "mapato mengine", "other income" -> tr(lang, "Mapato mengine", "Other Income")
            "manunuzi / stock", "manunuzi ya stock", "manunuzi ya bidhaa", "inventory / stock", "inventory" ->
                tr(lang, "Manunuzi ya bidhaa", "Inventory & Stock")
            "kodi ya pango", "rent" -> tr(lang, "Kodi ya pango", "Rent")
            "umeme & maji", "utilities" -> tr(lang, "Umeme & maji", "Utilities")
            "mishahara", "salaries", "payroll" -> tr(lang, "Mishahara", "Salaries")
            "usafiri", "transport" -> tr(lang, "Usafiri", "Transport")
            "chakula", "food", "meals", "food & meals" -> tr(lang, "Chakula", "Food & Meals")
            "internet & simu", "mtandao & simu", "internet & phone", "communication" ->
                tr(lang, "Mtandao & simu", "Internet & Phone")
            "matengenezo", "ukarabati & vifaa", "maintenance", "repairs & maintenance" ->
                tr(lang, "Matengenezo", "Repairs & Maintenance")
            "masoko", "masoko & matangazo", "marketing", "marketing & ads" ->
                tr(lang, "Masoko & matangazo", "Marketing & Ads")
            "kodi & vibali", "kodi & leseni", "taxes & permits", "tax & licenses" ->
                tr(lang, "Kodi & vibali", "Taxes & Permits")
            "afya & bima", "bima", "health & insurance", "insurance" ->
                tr(lang, "Afya & bima", "Health & Insurance")
            "ulinzi & usafi", "security & cleaning" ->
                tr(lang, "Ulinzi & usafi", "Security & Cleaning")
            "mkopo & marejesho", "loan repayment", "loans & credit" ->
                tr(lang, "Mkopo & marejesho", "Loan Repayment")
            "vifaa vya ofisi", "office supplies" ->
                tr(lang, "Vifaa vya ofisi", "Office Supplies")
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
