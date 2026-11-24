package tz.mengidata.app.data.accounting

import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.SavingsLogEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import kotlin.math.max

/**
 * Automatic financial classification buckets with standardized report colors.
 * Every shilling entering or leaving the system falls into exactly one bucket.
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
        labelSw = "Bili",
        labelEn = "Bill",
        colorNameSw = "MANJANO - Bili ya Kawaida",
        colorNameEn = "AMBER - Normal Bill",
        colorHex = "#D97706",
        bgHex = "#FEF3C7",
        textHex = "#92400E"
    ),
    HIGH_BILL_TODAY(
        code = "HIGH_BILL_TODAY",
        labelSw = "Bili Kubwa Sana Leo!",
        labelEn = "High Bill Today!",
        colorNameSw = "ZAMBARAU HAFIFU - Bili Imekuwa Kubwa Sana Leo",
        colorNameEn = "SOFT LAVENDER - Today's Bill Is Very High",
        colorHex = "#7B6B9E",
        bgHex = "#F4F1F9",
        textHex = "#4C3E6B"
    ),
    EXPENSE(
        code = "EXPENSE",
        labelSw = "Matumizi",
        labelEn = "Expense",
        colorNameSw = "MACHUNGWA - Matumizi",
        colorNameEn = "ORANGE - Expense",
        colorHex = "#EA580C",
        bgHex = "#FFEDD5",
        textHex = "#9A3412"
    );

    fun label(language: AppLanguage): String = if (language == AppLanguage.EN) labelEn else labelSw
    fun colorMeaning(language: AppLanguage): String = if (language == AppLanguage.EN) colorNameEn else colorNameSw
}

data class AutoClassificationResult(
    val bucket: AutoBucket,
    val suggestedTxnType: String, // "income" or "expense"
    val suggestedCategory: String,
    val reasonSw: String,
    val reasonEn: String
) {
    val isSavings: Boolean get() = bucket == AutoBucket.SAVINGS
    val isBill: Boolean get() = bucket == AutoBucket.BILL || bucket == AutoBucket.HIGH_BILL_TODAY
    fun reason(language: AppLanguage): String = if (language == AppLanguage.EN) reasonEn else reasonSw
}

/**
 * Auditable, zero-loss accounting snapshot across Transactions, Bills, Invoices, and Savings.
 * Guarantees no money entering or leaving is silently dropped.
 */
data class ZeroLossLedgerSummary(
    val totalMoneyIn: Double = 0.0,
    val operatingIncome: Double = 0.0,
    val savingsWithdrawalsIn: Double = 0.0,
    val totalDirectExpenses: Double = 0.0,
    val totalBillsPaid: Double = 0.0,
    val totalBillsUnpaid: Double = 0.0,
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

    private val savingsKeywords = listOf(
        "akiba", "kibubu", "save", "saving", "savings", "mfuko", "dharura",
        "hisa", "utt", "uwekezaji", "invest", "investment", "reserve",
        "weka akiba", "lengo", "deposit", "fungu la akiba"
    )

    private val billKeywords = listOf(
        "bili", "bill", "luku", "umeme", "tanesco", "maji", "dawasa",
        "kodi", "pango", "rent", "internet", "wifi", "fiber", "dstv",
        "azam", "startimes", "simu", "airtime", "bima", "insurance",
        "tra", "vibali", "leseni", "license", "ada", "school", "tuition",
        "mshahara", "mishahara", "salary", "payroll", "subscription"
    )

    private val incomeKeywords = listOf(
        "mauzo", "sale", "sales", "mapato", "income", "malipo", "payment",
        "imeingia", "pesa imeingia", "kuingia", "faida", "profit", "ankara",
        "invoice", "mteja", "client", "ushauri", "consulting", "huduma",
        "service", "kamisheni", "commission", "bonus", "zawadi", "gift",
        "mshahara wangu", "received", "pokea", "imepokelewa", "refund"
    )

    private val expenseKeywords = listOf(
        "matumizi", "expense", "chakula", "food", "lunch", "chai", "nauli",
        "mafuta", "fuel", "petrol", "diesel", "usafiri", "transport", "bodaboda",
        "uber", "bolt", "manunuzi", "stock", "duka", "matengenezo", "repair",
        "garage", "matibabu", "hospital", "dawa", "vifaa", "supplies", "stationery",
        "printer", "toner", "nunua", "buy", "bought", "lipa", "paid"
    )

    fun classifyText(
        description: String,
        category: String = "",
        explicitType: String? = null
    ): AutoClassificationResult {
        val text = "$description $category".lowercase()

        // 1. Check Savings (Akiba) first
        if (savingsKeywords.any { text.contains(it) }) {
            return AutoClassificationResult(
                bucket = AutoBucket.SAVINGS,
                suggestedTxnType = "expense",
                suggestedCategory = "Akiba",
                reasonSw = "Imetambuliwa kiotomatiki kama AKIBA (itaongezwa kwenye mfuko wa Akiba na leja).",
                reasonEn = "Auto-detected as SAVINGS (logged in Savings vault & zero-loss ledger)."
            )
        }

        // 2. Check Bills (Bili)
        if (billKeywords.any { text.contains(it) }) {
            val cat = inferBillCategory(text)
            return AutoClassificationResult(
                bucket = AutoBucket.BILL,
                suggestedTxnType = "expense",
                suggestedCategory = cat,
                reasonSw = "Imetambuliwa kiotomatiki kama BILI ($cat) — itatenganishwa na miamala ya kawaida.",
                reasonEn = "Auto-detected as BILL ($cat) — kept separate from regular transactions."
            )
        }

        // 3. Check Money In (Income)
        if (incomeKeywords.any { text.contains(it) }) {
            val cat = inferIncomeCategory(text)
            return AutoClassificationResult(
                bucket = AutoBucket.MONEY_IN,
                suggestedTxnType = "income",
                suggestedCategory = cat,
                reasonSw = "Imetambuliwa kiotomatiki kama PESA KUINGIA ($cat).",
                reasonEn = "Auto-detected as MONEY IN ($cat)."
            )
        }

        // 4. Check Expense (Matumizi)
        if (expenseKeywords.any { text.contains(it) }) {
            val cat = inferExpenseCategory(text)
            return AutoClassificationResult(
                bucket = AutoBucket.EXPENSE,
                suggestedTxnType = "expense",
                suggestedCategory = cat,
                reasonSw = "Imetambuliwa kiotomatiki kama MATUMIZI ($cat).",
                reasonEn = "Auto-detected as EXPENSE ($cat)."
            )
        }

        // 5. Fallback to explicit type or default
        return if (explicitType == "expense") {
            AutoClassificationResult(
                bucket = AutoBucket.EXPENSE,
                suggestedTxnType = "expense",
                suggestedCategory = category.ifBlank { "Matumizi mengine" },
                reasonSw = "Imeainishwa kama MATUMIZI (pesa kutoka).",
                reasonEn = "Classified as EXPENSE (money out)."
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
        if (txn.source == "savings" || txn.category.equals("Akiba", ignoreCase = true)) {
            return AutoBucket.SAVINGS
        }
        if (txn.source == "bill") {
            return AutoBucket.BILL
        }
        if (txn.isIncome) {
            return AutoBucket.MONEY_IN
        }
        val text = "${txn.description} ${txn.category}".lowercase()
        if (savingsKeywords.any { text.contains(it) }) return AutoBucket.SAVINGS
        if (billKeywords.any { text.contains(it) }) return AutoBucket.BILL
        return AutoBucket.EXPENSE
    }

    /**
     * Computes a complete zero-loss summary across every financial table in Room.
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

        val directExpenses = transactions
            .filter { !it.isIncome && it.source != "bill" && it.source != "savings" }
            .sumOf { it.amount }

        val billsPaidViaTxns = transactions
            .filter { !it.isIncome && it.source == "bill" }
            .sumOf { it.amount }

        // Ensure even if a paid bill wasn't synced yet, we never lose it in the display total
        val unsyncedPaidBills = bills
            .filter { it.paid && (it.linkedTxnId.isNullOrBlank() || it.linkedTxnId !in txnIds) }
            .sumOf { it.amount }
        val totalBillsPaid = billsPaidViaTxns + unsyncedPaidBills
        val totalBillsUnpaid = bills.filter { !it.paid }.sumOf { it.amount }

        val savingsDepositsViaTxns = transactions
            .filter { !it.isIncome && it.source == "savings" }
            .sumOf { it.amount }
        val savingsDepositsFromLogs = savingsLogs.filter { it.type == "deposit" }.sumOf { it.amount }
        val totalSavingsDeposited = max(savingsDepositsViaTxns, savingsDepositsFromLogs)
        val netSavingsBalance = savingsGoals.sumOf { it.currentAmount }

        val totalMoneyOut = directExpenses + totalBillsPaid + savingsDepositsViaTxns
        val verifiedCashBalance = totalMoneyIn - totalMoneyOut
        val totalNetWorthWithSavings = verifiedCashBalance + netSavingsBalance

        val todayMoneyIn = transactions
            .filter { it.isIncome && it.date == today }
            .sumOf { it.amount }
        val todayExpensesTotal = transactions
            .filter { !it.isIncome && it.source != "bill" && it.date == today }
            .sumOf { it.amount }

        // High Bill Today detection: bills due today, paid today, or created today
        val todayBills = bills.filter { b ->
            b.dueDate == today || b.paidDate == today || (b.createdAt == today && !b.paid)
        }
        val todayBillsTotal = todayBills.sumOf { it.amount }
        val averageBillAmount = if (bills.isNotEmpty()) bills.sumOf { it.amount } / bills.size else 0.0
        val highBillThreshold = max(100_000.0, averageBillAmount * 1.25)

        val highBillIds = mutableSetOf<String>()
        bills.forEach { b ->
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
        text.contains("internet") || text.contains("wifi") || text.contains("fiber") || text.contains("simu") || text.contains("airtime") -> "Internet & simu"
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
        text.contains("mafuta") || text.contains("fuel") || text.contains("nauli") || text.contains("usafiri") || text.contains("transport") -> "Usafiri"
        text.contains("matengenezo") || text.contains("repair") || text.contains("garage") -> "Matengenezo"
        text.contains("matangazo") || text.contains("ads") || text.contains("marketing") -> "Matangazo"
        else -> "Matumizi mengine"
    }
}
