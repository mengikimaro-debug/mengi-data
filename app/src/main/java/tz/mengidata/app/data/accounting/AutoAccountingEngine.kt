package tz.mengidata.app.data.accounting

import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.SavingsLogEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import java.util.Locale
import kotlin.math.max

/**
 * Automatic financial classification buckets with standardized report colors.
 * Every shilling entering or leaving the system falls into exactly one bucket,
 * keeping Household Bills clearly tagged and separated from Personal/System Expenses.
 */
enum class AutoBucket(
    val code: String,
    val labelSw: String,
    val labelEn: String,
    val colorNameSw: String,
    val colorNameEn: String,
    val colorHex: String,
    val bgHex: String,
    val textHex: String
) {
    MONEY_IN(
        code = "MONEY_IN",
        labelSw = "Pesa Kuingia (Mapato)",
        labelEn = "Money In (Income)",
        colorNameSw = "KIJANI - Pesa Kuingia",
        colorNameEn = "GREEN - Money In",
        colorHex = "#10B981",
        bgHex = "#D1FAE5",
        textHex = "#065F46"
    ),
    SAVINGS(
        code = "SAVINGS",
        labelSw = "Akiba",
        labelEn = "Savings",
        colorNameSw = "BLUU - Akiba",
        colorNameEn = "BLUE - Savings",
        colorHex = "#2563EB",
        bgHex = "#DBEAFE",
        textHex = "#1E40AF"
    ),
    BILL(
        code = "BILL",
        labelSw = "Bili Zangu",
        labelEn = "My Bills",
        colorNameSw = "MANJANO - Bili Zangu",
        colorNameEn = "AMBER - My Bills",
        colorHex = "#7D5D3B",
        bgHex = "#F0EAE1",
        textHex = "#5B432A"
    ),
    HIGH_BILL_TODAY(
        code = "HIGH_BILL_TODAY",
        labelSw = "Bili Kubwa Leo",
        labelEn = "High Bill Today",
        colorNameSw = "ZAMBARAU - Bili Kubwa Leo",
        colorNameEn = "LAVENDER - High Bill Today",
        colorHex = "#665B7A",
        bgHex = "#ECE9F2",
        textHex = "#4A4159"
    ),
    HOUSEHOLD_BILL(
        code = "HOUSEHOLD_BILL",
        labelSw = "Bili ya Nyumbani",
        labelEn = "Household Bill",
        colorNameSw = "SAMAWATI - Bili ya Nyumbani",
        colorNameEn = "TEAL - Household Bill",
        colorHex = "#3F6570",
        bgHex = "#E2ECEF",
        textHex = "#2C4A52"
    ),
    EXPENSE(
        code = "EXPENSE",
        labelSw = "Matumizi Yangu",
        labelEn = "My Expenses",
        colorNameSw = "MACHUNGWA - Matumizi Yangu",
        colorNameEn = "ORANGE - My Expenses",
        colorHex = "#EA580C",
        bgHex = "#FFEDD5",
        textHex = "#9A3412"
    );

    fun label(language: AppLanguage): String = if (language == AppLanguage.EN) labelEn else labelSw
    fun colorMeaning(language: AppLanguage): String = if (language == AppLanguage.EN) colorNameEn else colorNameSw
}

data class SlashTagParseResult(
    val hasSlashTag: Boolean = false,
    val cleanName: String = "",
    val rawTag: String = "",
    val detectedBucket: AutoBucket? = null,
    val isInvoiceTag: Boolean = false,
    val suggestedCategory: String? = null,
    val suggestedSavingsGoalTitle: String? = null
)

data class AutoClassificationResult(
    val bucket: AutoBucket,
    val suggestedTxnType: String, // "income" or "expense"
    val suggestedCategory: String,
    val reasonSw: String,
    val reasonEn: String,
    val slashTag: SlashTagParseResult = SlashTagParseResult()
) {
    val isSavings: Boolean get() = bucket == AutoBucket.SAVINGS
    val isBill: Boolean get() = bucket == AutoBucket.BILL || bucket == AutoBucket.HIGH_BILL_TODAY
    val isHousehold: Boolean get() = bucket == AutoBucket.HOUSEHOLD_BILL
    val isInvoiceTagged: Boolean get() = slashTag.isInvoiceTag
    val suggestedSavingsGoalTitle: String? get() = slashTag.suggestedSavingsGoalTitle
    fun reason(language: AppLanguage): String = if (language == AppLanguage.EN) reasonEn else reasonSw
}

/**
 * Auditable, zero-loss accounting snapshot across Transactions, Bills, Household Bills,
 * Invoices, and Savings. Guarantees no money entering or leaving is silently dropped,
 * while clearly separating Household Bills from Personal Expenses.
 */
data class ZeroLossLedgerSummary(
    val totalMoneyIn: Double = 0.0,
    val operatingIncome: Double = 0.0,
    val savingsWithdrawalsIn: Double = 0.0,
    val totalDirectExpenses: Double = 0.0,
    val totalBillsPaid: Double = 0.0,
    val totalBillsUnpaid: Double = 0.0,
    val totalHouseholdBills: Double = 0.0,
    val todayHouseholdBills: Double = 0.0,
    val householdBillsCount: Int = 0,
    val totalSavingsDeposited: Double = 0.0,
    val netSavingsBalance: Double = 0.0,
    val totalMoneyOut: Double = 0.0,
    val verifiedCashBalance: Double = 0.0,
    val totalNetWorthWithSavings: Double = 0.0,
    val todayMoneyIn: Double = 0.0,
    val todayExpensesTotal: Double = 0.0,
    val todayBillsTotal: Double = 0.0,
    val todayBillsCount: Int = 0,
    val averageBillAmount: Double = 0.0,
    val highBillThreshold: Double = 150_000.0,
    val isTodayBillHigh: Boolean = false,
    val highBillIds: Set<String> = emptySet(),
    val highBillAlertSw: String = "",
    val highBillAlertEn: String = "",
    val totalRecordsCount: Int = 0,
    val missingSyncCount: Int = 0
) {
    val integrityVerified: Boolean get() = missingSyncCount == 0
}

object AutoAccountingEngine {

    const val HOUSEHOLD_CATEGORY = "Household Bill"
    const val HOUSEHOLD_SOURCE = "household"

    private val homeWordRegex = Regex("\\bhome\\b")

    private val householdKeywords = listOf(
        "household bill", "household", "pesa ya home", "pesa home", "pesa nyumbani",
        "pesa ya nyumbani", "matumizi ya home", "matumizi ya nyumbani", "matumizi home",
        "matumizi nyumbani", "bili ya nyumbani", "bili nyumbani", "nyumbani",
        "home bill", "home allowance", "home money", "familia", "family",
        "housekeeping", "kuacha pesa home", "nimeacha home", "kuacha nyumbani",
        "nimeacha nyumbani", "pesa ya familia"
    )

    private val savingsKeywords = listOf(
        "akiba", "kibubu", "save", "saving", "savings", "mfuko", "dharura",
        "hisa", "utt", "uwekezaji", "invest", "investment", "reserve",
        "weka akiba", "lengo", "deposit", "fungu la akiba"
    )

    private val billKeywords = listOf(
        "bili", "bill", "luku", "umeme", "tanesco", "maji", "dawasa",
        "kodi", "pango", "rent", "fiber", "dstv", "azam", "startimes",
        "bima", "insurance", "tra", "vibali", "leseni", "license",
        "ada", "school", "tuition", "mshahara", "mishahara", "salary",
        "payroll", "subscription"
    )

    /**
     * Personal / daily work expenses taken when going to work:
     * transport, food, internet, airtime, fuel, lunch, etc.
     */
    private val personalDailyExpenseKeywords = listOf(
        "transport", "usafiri", "nauli", "mafuta", "fuel", "petrol", "diesel",
        "bodaboda", "daladala", "bajaji", "uber", "bolt",
        "food", "chakula", "msosi", "lunch", "breakfast", "dinner", "chai", "kahawa",
        "internet", "wifi", "bando", "bundle", "data", "vocha", "airtime", "simu",
        "kazini", "kazi", "matumizi yangu", "matumizi binafsi", "binafsi", "personal"
    )

    private val incomeKeywords = listOf(
        "mauzo", "sale", "sales", "mapato", "income", "malipo", "payment",
        "imeingia", "pesa imeingia", "kuingia", "faida", "profit", "ankara",
        "invoice", "mteja", "client", "ushauri", "consulting", "huduma",
        "service", "kamisheni", "commission", "bonus", "zawadi", "gift",
        "mshahara wangu", "received", "pokea", "imepokelewa", "refund"
    )

    private val otherExpenseKeywords = listOf(
        "matumizi", "expense", "manunuzi", "stock", "duka", "matengenezo", "repair",
        "garage", "matibabu", "hospital", "dawa", "vifaa", "supplies", "stationery",
        "printer", "toner", "nunua", "buy", "bought", "lipa", "paid"
    )

    private fun normalize(raw: String): String = raw
        .lowercase(Locale.ROOT)
        .replace(Regex("[._\\-/]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private val slashCommandRegex = Regex("""(?i)^(.*?)(?:\s+my)?\s*/\s*([a-z0-9_\-]+)\s*$""")

    /**
     * Parses universal slash-tag shorthand on ANY random name:
     * - "<name> my/mybill", "<name>/mybilll", "<name>/bill", "<name>/bili" -> My Bills (BILL)
     * - "<name>/household", "<name>/home", "<name>/nyumbani" -> Household Bill (HOUSEHOLD_BILL)
     * - "<name>/invoice", "<name>/ankara", "<name>/inv" -> Invoice / Money In (MONEY_IN + isInvoiceTag)
     * - "<name>/saving", "<name>/savings", "<name>/akiba" -> Savings (SAVINGS)
     * - "<name>/<goal>saving" (e.g. "<name>/rentsaving", "<name>/carsaving") -> Savings with specific goal
     * - "<name>/expense", "<name>/matumizi", "<name>/personal" -> My Expenses (EXPENSE)
     * - "<name>/income", "<name>/mapato", "<name>/mauzo" -> Money In (MONEY_IN)
     *
     * Unrecognized slashes (e.g. "Food / Lunch") are ignored so normal text is never altered.
     */
    fun parseSlashTag(raw: String): SlashTagParseResult {
        val trimmed = raw.trim()
        if (!trimmed.contains('/')) return SlashTagParseResult(cleanName = trimmed)

        val match = slashCommandRegex.find(trimmed) ?: return SlashTagParseResult(cleanName = trimmed)
        val rawBefore = match.groupValues[1].trim()
        val rawTag = match.groupValues[2].trim()
        val lowerTag = rawTag.lowercase(Locale.ROOT)
        // Collapse repeated letters at the end for typo tolerance (e.g. "mybilll" -> "mybill")
        val compactTag = lowerTag
            .replace(Regex("[^a-z0-9]"), "")
            .replace(Regex("([a-z])\\1{2,}"), "$1$1")

        // If the prefix ended with standalone "my" (e.g. "Juma my" or "my"), clean it off
        val cleanedPrefix = rawBefore
            .replace(Regex("(?i)\\s+my$"), "")
            .let { if (it.equals("my", ignoreCase = true)) "" else it }
            .trim()

        // 1. Household tags: /household, /house, /home, /nyumbani, /homebill, /familia
        if (compactTag in setOf("household", "house", "home", "nyumbani", "nyumba", "familia", "family", "homebill", "householdbill") ||
            compactTag.startsWith("household") ||
            compactTag.startsWith("nyumban")
        ) {
            return SlashTagParseResult(
                hasSlashTag = true,
                cleanName = cleanedPrefix,
                rawTag = rawTag,
                detectedBucket = AutoBucket.HOUSEHOLD_BILL,
                suggestedCategory = HOUSEHOLD_CATEGORY
            )
        }

        // 2. Savings & Named Goal Savings tags: /saving, /savings, /akiba, /rentsaving, /carsaving, /akiba_kodi
        val isPlainSaving = compactTag in setOf("saving", "savings", "save", "akiba", "kibubu")
        val goalPrefixFromSaving = when {
            isPlainSaving -> null
            lowerTag.endsWith("savings") -> lowerTag.removeSuffix("savings").trim('_', '-')
            lowerTag.endsWith("saving") -> lowerTag.removeSuffix("saving").trim('_', '-')
            lowerTag.startsWith("saving_") || lowerTag.startsWith("saving-") ->
                lowerTag.removePrefix("saving").trim('_', '-')
            lowerTag.startsWith("akiba_") || lowerTag.startsWith("akiba-") ->
                lowerTag.removePrefix("akiba").trim('_', '-')
            lowerTag.endsWith("akiba") && lowerTag.length > 5 ->
                lowerTag.removeSuffix("akiba").trim('_', '-')
            else -> null
        }
        if (isPlainSaving || !goalPrefixFromSaving.isNullOrBlank()) {
            val formattedGoal = goalPrefixFromSaving
                ?.takeIf { it.isNotBlank() }
                ?.let { formatSavingsGoalTitle(it) }
            return SlashTagParseResult(
                hasSlashTag = true,
                cleanName = cleanedPrefix,
                rawTag = rawTag,
                detectedBucket = AutoBucket.SAVINGS,
                suggestedCategory = "Akiba",
                suggestedSavingsGoalTitle = formattedGoal
            )
        }

        // 3. Invoice tags: /invoice, /invoices, /inv, /ankara, /invoce, /invoise
        if (compactTag in setOf("invoice", "invoices", "inv", "ankara", "invoce", "invoise", "invoic") ||
            compactTag.startsWith("invoic") ||
            compactTag.startsWith("ankar")
        ) {
            return SlashTagParseResult(
                hasSlashTag = true,
                cleanName = cleanedPrefix,
                rawTag = rawTag,
                detectedBucket = AutoBucket.MONEY_IN,
                isInvoiceTag = true,
                suggestedCategory = "Malipo ya ankara"
            )
        }

        // 4. My Bill tags: my/mybill, /mybill, /mybilll, /mybills, /bill, /bills, /bili, /my
        if (compactTag in setOf("mybill", "mybills", "mybil", "bill", "bills", "bili", "my", "luku", "umeme", "maji", "kodi", "rent") ||
            compactTag.startsWith("mybil") ||
            compactTag.startsWith("bill")
        ) {
            val cat = inferBillCategory("$lowerTag $cleanedPrefix")
            return SlashTagParseResult(
                hasSlashTag = true,
                cleanName = cleanedPrefix,
                rawTag = rawTag,
                detectedBucket = AutoBucket.BILL,
                suggestedCategory = cat
            )
        }

        // 5. Personal Expense tags: /expense, /expenses, /matumizi, /personal, /binafsi, /myexpense
        if (compactTag in setOf("expense", "expenses", "exp", "matumizi", "personal", "binafsi", "myexpense")) {
            val cat = inferExpenseCategory(cleanedPrefix)
            return SlashTagParseResult(
                hasSlashTag = true,
                cleanName = cleanedPrefix,
                rawTag = rawTag,
                detectedBucket = AutoBucket.EXPENSE,
                suggestedCategory = cat
            )
        }

        // 6. Income tags: /income, /mapato, /mauzo, /sale, /sales
        if (compactTag in setOf("income", "mapato", "mauzo", "sale", "sales")) {
            return SlashTagParseResult(
                hasSlashTag = true,
                cleanName = cleanedPrefix,
                rawTag = rawTag,
                detectedBucket = AutoBucket.MONEY_IN,
                suggestedCategory = "Mauzo ya bidhaa"
            )
        }

        return SlashTagParseResult(cleanName = trimmed)
    }

    /**
     * Strips recognized slash-tag suffixes cleanly from any name or description.
     */
    fun stripSlashTag(raw: String): String {
        val parsed = parseSlashTag(raw)
        return if (parsed.hasSlashTag) parsed.cleanName else raw.trim()
    }

    /**
     * Matches an existing SavingsGoalEntity title against a slash-tag goal name (e.g. "Rent Savings" matches "Kodi ya Pango").
     */
    fun matchesSavingsGoal(existingTitle: String, suggestedGoalTitle: String): Boolean {
        val existingNorm = normalize(existingTitle)
        val suggestedNorm = normalize(suggestedGoalTitle)
        if (existingNorm.isBlank() || suggestedNorm.isBlank()) return false
        if (existingNorm == suggestedNorm || existingNorm.contains(suggestedNorm) || suggestedNorm.contains(existingNorm)) {
            return true
        }
        val keyWord = suggestedNorm
            .replace("savings", "")
            .replace("akiba", "")
            .replace("ya", "")
            .trim()
        if (keyWord.length >= 3 && existingNorm.contains(keyWord)) return true
        val synonymGroups = listOf(
            setOf("rent", "kodi", "pango"),
            setOf("emergency", "dharura"),
            setOf("car", "gari", "vehicle"),
            setOf("school", "shule", "ada", "tuition", "education"),
            setOf("house", "nyumba", "ujenzi", "home"),
            setOf("business", "biashara", "mtaji", "capital"),
            setOf("land", "kiwanja", "ardhi", "plot"),
            setOf("equipment", "vifaa", "tools"),
            setOf("travel", "safari")
        )
        val matchedGroup = synonymGroups.firstOrNull { group -> group.any { suggestedNorm.contains(it) } }
        return matchedGroup?.any { existingNorm.contains(it) } == true
    }

    private fun formatSavingsGoalTitle(rawGoalKey: String): String {
        val cleanKey = rawGoalKey.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), " ").trim()
        return when (cleanKey) {
            "rent", "kodi", "pango" -> "Rent Savings"
            "emergency", "dharura" -> "Emergency Savings"
            "car", "gari" -> "Car Savings"
            "school", "shule", "ada" -> "School Savings"
            "house", "nyumba" -> "House Savings"
            "business", "biashara" -> "Business Savings"
            "land", "kiwanja" -> "Land Savings"
            else -> {
                val pretty = cleanKey.split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }
                if (pretty.isBlank()) "Akiba Kuu" else "$pretty Savings"
            }
        }
    }

    /**
     * Recognizes Household Bill recipients dynamically from user-saved household names
     * or explicit household wording / slash tags (zero hardcoded example names).
     */
    fun isHouseholdParty(
        party: String,
        learnedHouseholdNames: Set<String> = emptySet()
    ): Boolean {
        val slash = parseSlashTag(party)
        if (slash.hasSlashTag) {
            return slash.detectedBucket == AutoBucket.HOUSEHOLD_BILL
        }
        val norm = normalize(party)
        if (norm.isEmpty()) return false
        if (learnedHouseholdNames.any { normalize(it) == norm }) return true
        return isHouseholdText(norm)
    }

    /**
     * Checks whether description/category text explicitly denotes a Household Bill (money left at home).
     */
    fun isHouseholdText(text: String): Boolean {
        val slash = parseSlashTag(text)
        if (slash.hasSlashTag) {
            return slash.detectedBucket == AutoBucket.HOUSEHOLD_BILL
        }
        val norm = normalize(text)
        if (norm.isEmpty()) return false
        return householdKeywords.any { norm.contains(it) } || homeWordRegex.containsMatchIn(norm)
    }

    /**
     * True if a stored transaction is a Household Bill.
     */
    fun isHouseholdTransaction(txn: TransactionEntity): Boolean {
        if (txn.isIncome) return false
        if (txn.source == HOUSEHOLD_SOURCE) return true
        if (txn.category.equals(HOUSEHOLD_CATEGORY, ignoreCase = true)) return true
        return isHouseholdText("${txn.description} ${txn.category} ${txn.party}")
    }

    /**
     * True if a bill entry belongs to Household Bills.
     */
    fun isHouseholdBill(bill: BillEntity): Boolean {
        if (bill.category.equals(HOUSEHOLD_CATEGORY, ignoreCase = true)) return true
        return isHouseholdText("${bill.name} ${bill.category} ${bill.payee}")
    }

    /**
     * True if a stored transaction is a Savings deposit/withdrawal (Akiba).
     * Savings deposits are deducted normally from the system balance while clearly tagged as Akiba.
     */
    fun isSavingsTransaction(txn: TransactionEntity): Boolean {
        if (isHouseholdTransaction(txn)) return false
        if (txn.source == "savings") return true
        if (txn.category.equals("Akiba", ignoreCase = true) || txn.category.equals("Savings", ignoreCase = true)) {
            return true
        }
        val norm = normalize("${txn.description} ${txn.category}")
        return savingsKeywords.any { norm.contains(it) }
    }

    fun classifyText(
        description: String,
        category: String = "",
        party: String = "",
        explicitType: String? = null,
        learnedHouseholdNames: Set<String> = emptySet()
    ): AutoClassificationResult {
        // 0. Universal Slash-Tag Auto-Routing (works with ANY random name in Party OR Description!)
        val partySlash = parseSlashTag(party)
        val descSlash = parseSlashTag(description)
        val activeSlash = when {
            partySlash.hasSlashTag && partySlash.detectedBucket != null -> partySlash
            descSlash.hasSlashTag && descSlash.detectedBucket != null -> descSlash
            else -> null
        }
        if (activeSlash != null && activeSlash.detectedBucket != null) {
            val tagBucket = activeSlash.detectedBucket
            val cleanName = activeSlash.cleanName.ifBlank {
                if (activeSlash === descSlash) stripSlashTag(party) else stripSlashTag(description)
            }
            val namePrefixSw = if (cleanName.isNotBlank()) "($cleanName) " else ""
            val namePrefixEn = if (cleanName.isNotBlank()) "($cleanName) " else ""
            val txnType = if (tagBucket == AutoBucket.MONEY_IN) "income" else "expense"
            val resolvedCat = activeSlash.suggestedCategory ?: when (tagBucket) {
                AutoBucket.HOUSEHOLD_BILL -> HOUSEHOLD_CATEGORY
                AutoBucket.SAVINGS -> "Akiba"
                AutoBucket.MONEY_IN -> if (activeSlash.isInvoiceTag) "Malipo ya ankara" else "Mauzo ya bidhaa"
                AutoBucket.BILL, AutoBucket.HIGH_BILL_TODAY -> inferBillCategory("$description $party")
                AutoBucket.EXPENSE -> inferExpenseCategory("$description $party")
            }
            val (reasonSw, reasonEn) = when {
                activeSlash.isInvoiceTag -> Pair(
                    "Alama /${activeSlash.rawTag} ${namePrefixSw}imetambuliwa kiotomatiki kama ANKARA — itarekodiwa kwenye Ankara na Mapato.",
                    "Tag /${activeSlash.rawTag} ${namePrefixEn}auto-routed as INVOICE — recorded under Invoices and Income."
                )
                tagBucket == AutoBucket.SAVINGS && !activeSlash.suggestedSavingsGoalTitle.isNullOrBlank() -> Pair(
                    "Alama /${activeSlash.rawTag} ${namePrefixSw}imetambuliwa kiotomatiki kama AKIBA (${activeSlash.suggestedSavingsGoalTitle}).",
                    "Tag /${activeSlash.rawTag} ${namePrefixEn}auto-routed to SAVINGS goal (${activeSlash.suggestedSavingsGoalTitle})."
                )
                tagBucket == AutoBucket.SAVINGS -> Pair(
                    "Alama /${activeSlash.rawTag} ${namePrefixSw}imetambuliwa kiotomatiki kama AKIBA — itakatwa kawaida na kuhifadhiwa kwenye Akiba.",
                    "Tag /${activeSlash.rawTag} ${namePrefixEn}auto-routed to SAVINGS — deducted and stored in Savings vault."
                )
                tagBucket == AutoBucket.HOUSEHOLD_BILL -> Pair(
                    "Alama /${activeSlash.rawTag} ${namePrefixSw}imetambuliwa kiotomatiki kama BILI YA NYUMBANI.",
                    "Tag /${activeSlash.rawTag} ${namePrefixEn}auto-routed to HOUSEHOLD BILL."
                )
                tagBucket == AutoBucket.BILL -> Pair(
                    "Alama /${activeSlash.rawTag} ${namePrefixSw}imetambuliwa kiotomatiki kama BILI ZANGU.",
                    "Tag /${activeSlash.rawTag} ${namePrefixEn}auto-routed to MY BILLS."
                )
                tagBucket == AutoBucket.EXPENSE -> Pair(
                    "Alama /${activeSlash.rawTag} ${namePrefixSw}imetambuliwa kiotomatiki kama MATUMIZI YANGU.",
                    "Tag /${activeSlash.rawTag} ${namePrefixEn}auto-routed to MY EXPENSES."
                )
                else -> Pair(
                    "Alama /${activeSlash.rawTag} ${namePrefixSw}imetambuliwa kiotomatiki kama PESA KUINGIA.",
                    "Tag /${activeSlash.rawTag} ${namePrefixEn}auto-routed to MONEY IN."
                )
            }
            return AutoClassificationResult(
                bucket = tagBucket,
                suggestedTxnType = txnType,
                suggestedCategory = resolvedCat,
                reasonSw = reasonSw,
                reasonEn = reasonEn,
                slashTag = activeSlash
            )
        }

        val text = normalize("$description $category")
        val partyIsHousehold = isHouseholdParty(party, learnedHouseholdNames)

        // 1. Household Bill ALWAYS has highest priority:
        // deducted normally from system balance while clearly tagged & saved as Household Bill
        if (partyIsHousehold || isHouseholdText(text)) {
            val reasonSw = if (partyIsHousehold && party.isNotBlank()) {
                "Jina \"${party.trim()}\" limetambuliwa kama BILI YA NYUMBANI — inakatwa kwenye mfumo na kuhifadhiwa kama Bili ya Nyumbani."
            } else {
                "Imetambuliwa kama BILI YA NYUMBANI — inakatwa kwenye mfumo na kuhifadhiwa kama Bili ya Nyumbani."
            }
            val reasonEn = if (partyIsHousehold && party.isNotBlank()) {
                "Name \"${party.trim()}\" recognized as HOUSEHOLD BILL — deducted in the system and saved under Household Bills."
            } else {
                "Auto-detected as HOUSEHOLD BILL — deducted in the system and saved under Household Bills."
            }
            return AutoClassificationResult(
                bucket = AutoBucket.HOUSEHOLD_BILL,
                suggestedTxnType = "expense",
                suggestedCategory = HOUSEHOLD_CATEGORY,
                reasonSw = reasonSw,
                reasonEn = reasonEn
            )
        }

        // 2. Savings (Akiba) — deducted normally from system balance & stored in Savings vault
        if (savingsKeywords.any { text.contains(it) }) {
            return AutoClassificationResult(
                bucket = AutoBucket.SAVINGS,
                suggestedTxnType = "expense",
                suggestedCategory = "Akiba",
                reasonSw = "Imetambuliwa kama AKIBA — inakatwa kwenye mfumo kawaida na kuhifadhiwa kwenye mfuko wa akiba.",
                reasonEn = "Auto-detected as SAVINGS — deducted normally from system balance and stored in Savings vault."
            )
        }

        // 3. Personal Daily Expenses (Transport, Food, Internet, Work pocket money)
        if (personalDailyExpenseKeywords.any { text.contains(it) }) {
            val cat = inferExpenseCategory(text)
            val catSw = FormatUtils.localizedCategory(cat, AppLanguage.SW)
            val catEn = FormatUtils.localizedCategory(cat, AppLanguage.EN)
            return AutoClassificationResult(
                bucket = AutoBucket.EXPENSE,
                suggestedTxnType = "expense",
                suggestedCategory = cat,
                reasonSw = "Imetambuliwa kama MATUMIZI YANGU ($catSw) — matumizi binafsi ya kazi na kila siku.",
                reasonEn = "Auto-detected as MY EXPENSES ($catEn) — personal daily/work spending."
            )
        }

        // 4. Explicit Bills (LUKU, Kodi, Maji, Bili, Fiber, TRA, n.k.)
        if (billKeywords.any { text.contains(it) }) {
            val cat = inferBillCategory(text)
            val catSw = FormatUtils.localizedCategory(cat, AppLanguage.SW)
            val catEn = FormatUtils.localizedCategory(cat, AppLanguage.EN)
            return AutoClassificationResult(
                bucket = AutoBucket.BILL,
                suggestedTxnType = "expense",
                suggestedCategory = cat,
                reasonSw = "Imetambuliwa kiotomatiki kama BILI ZANGU ($catSw) — imetenganishwa na Bili ya Nyumbani.",
                reasonEn = "Auto-detected as MY BILLS ($catEn) — separated from Household Bills."
            )
        }

        // 6. Money In (Income)
        if (incomeKeywords.any { text.contains(it) }) {
            val cat = inferIncomeCategory(text)
            val catSw = FormatUtils.localizedCategory(cat, AppLanguage.SW)
            val catEn = FormatUtils.localizedCategory(cat, AppLanguage.EN)
            return AutoClassificationResult(
                bucket = AutoBucket.MONEY_IN,
                suggestedTxnType = "income",
                suggestedCategory = cat,
                reasonSw = "Imetambuliwa kiotomatiki kama PESA KUINGIA ($catSw).",
                reasonEn = "Auto-detected as MONEY IN ($catEn)."
            )
        }

        // 7. Other Personal/Business Expenses
        if (otherExpenseKeywords.any { text.contains(it) }) {
            val cat = inferExpenseCategory(text)
            val catSw = FormatUtils.localizedCategory(cat, AppLanguage.SW)
            val catEn = FormatUtils.localizedCategory(cat, AppLanguage.EN)
            return AutoClassificationResult(
                bucket = AutoBucket.EXPENSE,
                suggestedTxnType = "expense",
                suggestedCategory = cat,
                reasonSw = "Imetambuliwa kiotomatiki kama MATUMIZI YANGU ($catSw).",
                reasonEn = "Auto-detected as MY EXPENSES ($catEn)."
            )
        }

        // 8. Fallback to explicit type or default
        return if (explicitType == "expense") {
            AutoClassificationResult(
                bucket = AutoBucket.EXPENSE,
                suggestedTxnType = "expense",
                suggestedCategory = category.ifBlank { "Matumizi mengine" },
                reasonSw = "Imeainishwa kama MATUMIZI YANGU (pesa kutoka).",
                reasonEn = "Classified as MY EXPENSES (money out)."
            )
        } else {
            AutoClassificationResult(
                bucket = AutoBucket.MONEY_IN,
                suggestedTxnType = "income",
                suggestedCategory = category.ifBlank { "Mauzo ya bidhaa" },
                reasonSw = "Imeainishwa kama PESA KUINGIA (mapato).",
                reasonEn = "Classified as MONEY IN (income)."
            )
        }
    }

    /**
     * Classifies an existing stored transaction into its true accounting bucket.
     */
    fun classifyTransaction(txn: TransactionEntity): AutoBucket {
        if (isHouseholdTransaction(txn)) {
            return AutoBucket.HOUSEHOLD_BILL
        }
        if (isSavingsTransaction(txn)) {
            return AutoBucket.SAVINGS
        }
        if (txn.source == "bill") {
            return AutoBucket.BILL
        }
        if (txn.isIncome) {
            return AutoBucket.MONEY_IN
        }
        return classifyText(
            description = txn.description,
            category = txn.category,
            party = txn.party,
            explicitType = "expense"
        ).bucket
    }

    /**
     * Computes a complete zero-loss summary across every financial table in Room.
     * Clearly tags and separates Household Bills from Personal Expenses and Personal Bills,
     * while keeping 0% cash balance loss.
     */
    fun computeZeroLossSummary(
        transactions: List<TransactionEntity>,
        bills: List<BillEntity>,
        invoices: List<InvoiceWithItems>,
        savingsGoals: List<SavingsGoalEntity>,
        savingsLogs: List<SavingsLogEntity>,
        currency: String = "TZS"
    ): ZeroLossLedgerSummary {
        val today = FormatUtils.todayIso()
        val txnIds = transactions.map { it.id }.toSet()

        // Verify zero-loss sync integrity for paid bills & paid invoices
        val missingBillLinks = bills.count { it.paid && (it.linkedTxnId.isNullOrBlank() || it.linkedTxnId !in txnIds) }
        val missingInvoiceLinks = invoices.count {
            !it.isDraft && it.invoice.amountPaid > 0.005 &&
                (it.invoice.linkedTxnId.isNullOrBlank() || it.invoice.linkedTxnId !in txnIds)
        }

        val operatingIncome = transactions
            .filter { it.isIncome && it.source != "savings" }
            .sumOf { it.amount }

        val savingsWithdrawalsTxn = transactions
            .filter { it.isIncome && it.source == "savings" }
            .sumOf { it.amount }

        val totalMoneyIn = operatingIncome + savingsWithdrawalsTxn

        // Household Bills — clearly tagged and tracked
        val householdTxns = transactions.filter { isHouseholdTransaction(it) }
        val unsyncedPaidHouseholdBills = bills.filter {
            it.paid && isHouseholdBill(it) && (it.linkedTxnId.isNullOrBlank() || it.linkedTxnId !in txnIds)
        }
        val totalHouseholdBills = householdTxns.sumOf { it.amount } + unsyncedPaidHouseholdBills.sumOf { it.amount }
        val todayHouseholdBills = householdTxns.filter { it.date == today }.sumOf { it.amount } +
            unsyncedPaidHouseholdBills.filter { it.paidDate == today || it.dueDate == today }.sumOf { it.amount }
        val householdBillsCount = householdTxns.size + unsyncedPaidHouseholdBills.size

        // Personal / System Direct Expenses (EXCLUDING household, bills, savings)
        val directExpenses = transactions
            .filter { !it.isIncome && it.source != "bill" && !isSavingsTransaction(it) && !isHouseholdTransaction(it) }
            .sumOf { it.amount }

        // Personal / Business Bills Paid (EXCLUDING household bills)
        val billsPaidViaTxns = transactions
            .filter { !it.isIncome && it.source == "bill" && !isHouseholdTransaction(it) && !isSavingsTransaction(it) }
            .sumOf { it.amount }

        val unsyncedPaidBills = bills
            .filter { it.paid && !isHouseholdBill(it) && (it.linkedTxnId.isNullOrBlank() || it.linkedTxnId !in txnIds) }
            .sumOf { it.amount }
        val totalBillsPaid = billsPaidViaTxns + unsyncedPaidBills
        val totalBillsUnpaid = bills.filter { !it.paid }.sumOf { it.amount }

        val savingsDepositsViaTxns = transactions
            .filter { !it.isIncome && isSavingsTransaction(it) }
            .sumOf { it.amount }
        val savingsDepositsFromLogs = savingsLogs.filter { it.type == "deposit" }.sumOf { it.amount }
        val totalSavingsDeposited = max(savingsDepositsViaTxns, savingsDepositsFromLogs)
        val netSavingsBalance = savingsGoals.sumOf { it.currentAmount }

        // Zero-loss total money out includes every bucket (Personal, Bills, Household, Savings) so cash balance never loses a shilling
        val totalMoneyOut = directExpenses + totalBillsPaid + totalHouseholdBills + savingsDepositsViaTxns
        val verifiedCashBalance = totalMoneyIn - totalMoneyOut
        val totalNetWorthWithSavings = verifiedCashBalance + netSavingsBalance

        val todayMoneyIn = transactions
            .filter { it.isIncome && it.date == today }
            .sumOf { it.amount }
        val todayExpensesTotal = transactions
            .filter { !it.isIncome && it.source != "bill" && !isSavingsTransaction(it) && !isHouseholdTransaction(it) && it.date == today }
            .sumOf { it.amount }

        // High Bill Today detection (for personal/business bills — excluding household bills)
        val nonHouseholdBills = bills.filter { !isHouseholdBill(it) }
        val todayBills = nonHouseholdBills.filter { b ->
            b.dueDate == today || b.paidDate == today || (b.createdAt == today && !b.paid)
        }
        val todayBillsTotal = todayBills.sumOf { it.amount }
        val averageBillAmount = if (nonHouseholdBills.isNotEmpty()) nonHouseholdBills.sumOf { it.amount } / nonHouseholdBills.size else 0.0
        val highBillThreshold = max(100_000.0, averageBillAmount * 1.25)

        val highBillIds = mutableSetOf<String>()
        nonHouseholdBills.forEach { b ->
            val isToday = b.dueDate == today || b.paidDate == today || b.createdAt == today
            if (isToday && (b.amount >= highBillThreshold || todayBillsTotal >= highBillThreshold)) {
                highBillIds += b.id
            } else if (b.isOverdue && b.amount >= highBillThreshold) {
                highBillIds += b.id
            }
        }

        val isTodayBillHigh = todayBills.isNotEmpty() && (
            todayBillsTotal >= highBillThreshold ||
                (todayMoneyIn > 0.0 && todayBillsTotal > todayMoneyIn * 0.5)
            )

        val highBillAlertSw = if (isTodayBillHigh) {
            "Tahadhari: Bili za leo (${FormatUtils.formatMoney(todayBillsTotal, currency)}) zimekuwa kubwa sana kuliko wastani!"
        } else {
            ""
        }
        val highBillAlertEn = if (isTodayBillHigh) {
            "Alert: Today's bills (${FormatUtils.formatMoney(todayBillsTotal, currency)}) are unusually high!"
        } else {
            ""
        }

        return ZeroLossLedgerSummary(
            totalMoneyIn = totalMoneyIn,
            operatingIncome = operatingIncome,
            savingsWithdrawalsIn = savingsWithdrawalsTxn,
            totalDirectExpenses = directExpenses,
            totalBillsPaid = totalBillsPaid,
            totalBillsUnpaid = totalBillsUnpaid,
            totalHouseholdBills = totalHouseholdBills,
            todayHouseholdBills = todayHouseholdBills,
            householdBillsCount = householdBillsCount,
            totalSavingsDeposited = totalSavingsDeposited,
            netSavingsBalance = netSavingsBalance,
            totalMoneyOut = totalMoneyOut,
            verifiedCashBalance = verifiedCashBalance,
            totalNetWorthWithSavings = totalNetWorthWithSavings,
            todayMoneyIn = todayMoneyIn,
            todayExpensesTotal = todayExpensesTotal,
            todayBillsTotal = todayBillsTotal,
            todayBillsCount = todayBills.size,
            averageBillAmount = averageBillAmount,
            highBillThreshold = highBillThreshold,
            isTodayBillHigh = isTodayBillHigh,
            highBillIds = highBillIds,
            highBillAlertSw = highBillAlertSw,
            highBillAlertEn = highBillAlertEn,
            totalRecordsCount = transactions.size + bills.size + invoices.size + savingsLogs.size,
            missingSyncCount = missingBillLinks + missingInvoiceLinks
        )
    }

    private fun inferBillCategory(text: String): String = when {
        text.contains("luku") || text.contains("umeme") || text.contains("maji") || text.contains("tanesco") || text.contains("dawasa") -> "Umeme & maji"
        text.contains("kodi") && (text.contains("pango") || text.contains("nyumba") || text.contains("ofisi") || text.contains("rent")) -> "Kodi ya pango"
        text.contains("rent") || text.contains("pango") -> "Kodi ya pango"
        text.contains("tra") || text.contains("kodi") || text.contains("leseni") || text.contains("vibali") -> "Kodi & vibali"
        text.contains("fiber") || text.contains("dstv") || text.contains("azam") || text.contains("subscription") -> "Internet & simu"
        text.contains("mshahara") || text.contains("mishahara") || text.contains("salary") -> "Mishahara"
        else -> "Matumizi mengine"
    }

    private fun inferIncomeCategory(text: String): String = when {
        text.contains("ankara") || text.contains("invoice") -> "Malipo ya ankara"
        text.contains("ushauri") || text.contains("consult") -> "Ushauri"
        text.contains("huduma") || text.contains("service") -> "Huduma"
        text.contains("kamisheni") || text.contains("commission") -> "Kamisheni"
        text.contains("mauzo") || text.contains("sale") -> "Mauzo ya bidhaa"
        else -> "Mapato mengine"
    }

    private fun inferExpenseCategory(text: String): String = when {
        text.contains("stock") || text.contains("manunuzi") || text.contains("bidhaa") -> "Manunuzi / stock"
        text.contains("mafuta") || text.contains("fuel") || text.contains("nauli") || text.contains("usafiri") || text.contains("transport") ||
            text.contains("bodaboda") || text.contains("daladala") || text.contains("bajaji") || text.contains("uber") || text.contains("bolt") -> "Usafiri"
        text.contains("food") || text.contains("chakula") || text.contains("msosi") || text.contains("lunch") ||
            text.contains("breakfast") || text.contains("dinner") || text.contains("chai") || text.contains("kahawa") -> "Chakula"
        text.contains("internet") || text.contains("wifi") || text.contains("bando") || text.contains("bundle") ||
            text.contains("data") || text.contains("vocha") || text.contains("airtime") || text.contains("simu") -> "Internet & simu"
        text.contains("matengenezo") || text.contains("repair") || text.contains("garage") -> "Matengenezo"
        text.contains("matangazo") || text.contains("ads") || text.contains("marketing") -> "Masoko & matangazo"
        else -> "Matumizi mengine"
    }
}
