package tz.mengidata.app.data.accounting

import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.SavingsLogEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class HealthGrade {
    EXCELLENT,
    GOOD,
    CAUTION,
    CRITICAL;

    fun label(language: AppLanguage): String = when (this) {
        EXCELLENT -> tr(language, "Imara Sana", "Excellent")
        GOOD -> tr(language, "Nzuri", "Healthy")
        CAUTION -> tr(language, "Angalizo", "Watch Closely")
        CRITICAL -> tr(language, "Hatua Haraka", "Needs Action")
    }
}

enum class InsightSeverity {
    POSITIVE,
    INFO,
    WARNING,
    ACTION
}

data class BusinessInsight(
    val id: String,
    val titleSw: String,
    val titleEn: String,
    val detailSw: String,
    val detailEn: String,
    val severity: InsightSeverity
) {
    fun title(language: AppLanguage): String = tr(language, titleSw, titleEn)
    fun detail(language: AppLanguage): String = tr(language, detailSw, detailEn)
}

data class BusinessHealthReport(
    val score: Int = 75,
    val grade: HealthGrade = HealthGrade.GOOD,
    val profitMarginPct: Double = 0.0,
    val incomeMoMDeltaPct: Double? = null,
    val expenseMoMDeltaPct: Double? = null,
    val collectionRatePct: Double = 100.0,
    val cashRunwayMonths: Double = 0.0,
    val overdueBillsCount: Int = 0,
    val overdueInvoicesCount: Int = 0,
    val budgetAlertsCount: Int = 0,
    val insights: List<BusinessInsight> = emptyList()
)

enum class PartyRole {
    HOUSEHOLD,
    OWNER_BILLS,
    SAVINGS,
    CUSTOMER,
    VENDOR,
    BOTH;

    fun label(language: AppLanguage): String = when (this) {
        HOUSEHOLD -> tr(language, "Bili ya Nyumbani", "Household Bill")
        OWNER_BILLS -> tr(language, "Bili Zangu", "My Bills")
        SAVINGS -> tr(language, "Akiba", "Savings")
        CUSTOMER -> tr(language, "Mteja", "Customer")
        VENDOR -> tr(language, "Msambazaji", "Vendor")
        BOTH -> tr(language, "Mteja na Mdau", "Customer & Vendor")
    }
}

data class PartySummary(
    val name: String,
    val role: PartyRole,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val totalReceived: Double = 0.0,
    val totalPaidOut: Double = 0.0,
    val savingsTotal: Double = 0.0,
    val unpaidReceivables: Double = 0.0,
    val unpaidBills: Double = 0.0,
    val invoiceCount: Int = 0,
    val billCount: Int = 0,
    val savingsCount: Int = 0,
    val transactionCount: Int = 0,
    val modules: Set<String> = emptySet(),
    val lastActivityDate: String = ""
) {
    val netFlow: Double get() = totalReceived - totalPaidOut
    val totalRecordCount: Int get() = transactionCount + invoiceCount + billCount + savingsCount

    fun modulesLabel(language: AppLanguage): String {
        val ordered = listOf("transaction", "bill", "savings", "invoice").filter { it in modules }
        return ordered.joinToString(" · ") { mod ->
            when (mod) {
                "transaction" -> tr(language, "Miamala", "Transactions")
                "bill" -> tr(language, "Bili", "Bills")
                "savings" -> tr(language, "Akiba", "Savings")
                "invoice" -> tr(language, "Ankara", "Invoices")
                else -> mod
            }
        }
    }
}

object BusinessHealthEngine {

    fun evaluate(
        transactions: List<TransactionEntity>,
        invoices: List<InvoiceWithItems>,
        bills: List<BillEntity>,
        savingsGoals: List<SavingsGoalEntity>,
        categoryAnalytics: CategoryAnalyticsReport,
        currency: String = "TZS",
        todayIso: String = FormatUtils.todayIso()
    ): BusinessHealthReport {
        val currentMonthKey = FormatUtils.monthKey(todayIso)
        val prevMonthKey = FormatUtils.monthKey(FormatUtils.addMonthsIso(todayIso, -1))

        // Personal expenses, Household Bills, and Savings deposits are ALL deducted normally from system cashflow
        val operationalTxns = transactions
        val totalIncome = operationalTxns.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = operationalTxns.filter { !it.isIncome }.sumOf { it.amount }
        val netCash = totalIncome - totalExpense

        val currentMonthTxns = operationalTxns.filter { FormatUtils.monthKey(it.date) == currentMonthKey }
        val prevMonthTxns = operationalTxns.filter { FormatUtils.monthKey(it.date) == prevMonthKey }

        val curIncome = currentMonthTxns.filter { it.isIncome }.sumOf { it.amount }
        val prevIncome = prevMonthTxns.filter { it.isIncome }.sumOf { it.amount }
        val curExpense = currentMonthTxns.filter { !it.isIncome }.sumOf { it.amount }
        val prevExpense = prevMonthTxns.filter { !it.isIncome }.sumOf { it.amount }

        val incomeMoM = if (prevIncome > 0.0) ((curIncome - prevIncome) / prevIncome) * 100.0 else null
        val expenseMoM = if (prevExpense > 0.0) ((curExpense - prevExpense) / prevExpense) * 100.0 else null

        val profitMarginPct = if (totalIncome > 0.0) {
            ((totalIncome - totalExpense) / totalIncome) * 100.0
        } else if (totalExpense > 0.0) {
            -100.0
        } else {
            0.0
        }

        // Invoice collection rate
        val issuedInvoices = invoices.filter { !it.isDraft }
        val totalInvoiced = issuedInvoices.sumOf { it.total }
        val totalCollected = issuedInvoices.sumOf { it.invoice.amountPaid }
        val collectionRatePct = if (totalInvoiced > 0.0) {
            ((totalCollected / totalInvoiced) * 100.0).coerceIn(0.0, 100.0)
        } else {
            100.0
        }
        val overdueInvoices = issuedInvoices.filter { it.isOverdue }
        val totalReceivables = issuedInvoices.sumOf { it.balance }

        // Bills punctuality
        val unpaidBills = bills.filter { !it.paid }
        val overdueBills = unpaidBills.filter { it.isOverdue }

        // Savings progress
        val totalSaved = savingsGoals.sumOf { it.currentAmount }
        val totalSavingsTarget = savingsGoals.sumOf { it.targetAmount }
        val savingsProgressRatio = if (totalSavingsTarget > 0.0) {
            (totalSaved / totalSavingsTarget).coerceIn(0.0, 1.0)
        } else {
            0.5
        }

        // Cash runway in months (using 3-month average expense or current expense)
        val last3MonthKeys = (-2..0).map { FormatUtils.monthKey(FormatUtils.addMonthsIso(todayIso, it)) }.toSet()
        val recent3MonthExpense = operationalTxns
            .filter { !it.isIncome && FormatUtils.monthKey(it.date) in last3MonthKeys }
            .sumOf { it.amount }
        val monthlyBurn = max(recent3MonthExpense / 3.0, curExpense)
        val availableLiquidBuffer = max(netCash, 0.0) + totalSaved
        val runwayMonths = if (monthlyBurn > 0.0) {
            (availableLiquidBuffer / monthlyBurn).coerceIn(0.0, 36.0)
        } else if (availableLiquidBuffer > 0.0) {
            12.0
        } else {
            0.0
        }

        // Category budget overages
        val overBudgetCategories = categoryAnalytics.expenseBreakdown.filter { it.isOverBudget }

        // Weighted 0–100 Health Score:
        // 1. Profit margin (up to 35 pts)
        val marginPts = when {
            totalIncome <= 0.0 && totalExpense <= 0.0 -> 24.0
            profitMarginPct >= 30.0 -> 35.0
            profitMarginPct >= 15.0 -> 28.0 + ((profitMarginPct - 15.0) / 15.0) * 7.0
            profitMarginPct >= 0.0 -> 15.0 + (profitMarginPct / 15.0) * 13.0
            else -> max(0.0, 15.0 + profitMarginPct * 0.25)
        }
        // 2. Receivables collection efficiency (up to 20 pts)
        val collectionPts = (collectionRatePct / 100.0) * 20.0 - min(overdueInvoices.size * 3.0, 10.0)
        // 3. Bill punctuality (up to 20 pts)
        val billPts = max(0.0, 20.0 - overdueBills.size * 6.0)
        // 4. Savings & runway resilience (up to 15 pts)
        val resiliencePts = min(runwayMonths / 3.0, 1.0) * 10.0 + savingsProgressRatio * 5.0
        // 5. Budget discipline (up to 10 pts)
        val budgetPts = max(0.0, 10.0 - overBudgetCategories.size * 4.0)

        val finalScore = (marginPts + max(0.0, collectionPts) + billPts + resiliencePts + budgetPts)
            .roundToInt()
            .coerceIn(0, 100)

        val grade = when {
            finalScore >= 80 -> HealthGrade.EXCELLENT
            finalScore >= 62 -> HealthGrade.GOOD
            finalScore >= 42 -> HealthGrade.CAUTION
            else -> HealthGrade.CRITICAL
        }

        // Actionable Business Insights (prioritized, max 4)
        val insights = mutableListOf<BusinessInsight>()

        if (overdueBills.isNotEmpty()) {
            val sum = overdueBills.sumOf { it.amount }
            insights += BusinessInsight(
                id = "overdue_bills",
                titleSw = "Bili ${overdueBills.size} zimechelewa kulipwa",
                titleEn = "${overdueBills.size} overdue bill(s) need payment",
                detailSw = "Jumla ${FormatUtils.formatCompactMoney(sum, currency)} inasubiri malipo kuepuka adhabu.",
                detailEn = "Total ${FormatUtils.formatCompactMoney(sum, currency)} pending to avoid late penalties.",
                severity = InsightSeverity.ACTION
            )
        }

        if (totalReceivables > 0.0) {
            val dueCount = issuedInvoices.count { !it.isPaid }
            insights += BusinessInsight(
                id = "receivables_followup",
                titleSw = "Fuatilia madai ya ankara ($dueCount)",
                titleEn = "Follow up on $dueCount unpaid invoice(s)",
                detailSw = "Kuna ${FormatUtils.formatCompactMoney(totalReceivables, currency)} nje kwa wateja.",
                detailEn = "${FormatUtils.formatCompactMoney(totalReceivables, currency)} waiting to be collected.",
                severity = if (overdueInvoices.isNotEmpty()) InsightSeverity.WARNING else InsightSeverity.INFO
            )
        }

        if (overBudgetCategories.isNotEmpty()) {
            val topOver = overBudgetCategories.first()
            val catSw = FormatUtils.localizedCategory(topOver.categoryName, AppLanguage.SW)
            val catEn = FormatUtils.localizedCategory(topOver.categoryName, AppLanguage.EN)
            insights += BusinessInsight(
                id = "budget_overage",
                titleSw = "Bajeti ya \"$catSw\" imevuka",
                titleEn = "\"$catEn\" exceeded budget",
                detailSw = "Matumizi ${FormatUtils.formatCompactMoney(topOver.totalAmount, currency)} dhidi ya bajeti ${FormatUtils.formatCompactMoney(topOver.monthlyBudget, currency)}.",
                detailEn = "Spent ${FormatUtils.formatCompactMoney(topOver.totalAmount, currency)} vs ${FormatUtils.formatCompactMoney(topOver.monthlyBudget, currency)} budget.",
                severity = InsightSeverity.WARNING
            )
        }

        if (incomeMoM != null) {
            val rounded = abs(incomeMoM).roundToInt()
            if (incomeMoM >= 5.0) {
                insights += BusinessInsight(
                    id = "income_growth",
                    titleSw = "Mapato yamekua kwa +$rounded% mwezi huu",
                    titleEn = "Revenue grew +$rounded% vs last month",
                    detailSw = "Mwenendo mzuri! Tenga angalau 15% kwenye Mfuko wa Akiba.",
                    detailEn = "Strong momentum! Consider sweeping 15% into your Savings Vault.",
                    severity = InsightSeverity.POSITIVE
                )
            } else if (incomeMoM <= -15.0) {
                insights += BusinessInsight(
                    id = "income_dip",
                    titleSw = "Mapato yameshuka kwa -$rounded% mwezi huu",
                    titleEn = "Revenue dipped -$rounded% vs last month",
                    detailSw = "Kagua ankara zinazodaiwa na punguza matumizi yasiyo ya lazima.",
                    detailEn = "Review outstanding invoices and trim non-essential spending.",
                    severity = InsightSeverity.WARNING
                )
            }
        }

        val topIncomeCat = categoryAnalytics.incomeBreakdown.firstOrNull()
        if (topIncomeCat != null && insights.size < 4) {
            val catSw = FormatUtils.localizedCategory(topIncomeCat.categoryName, AppLanguage.SW)
            val catEn = FormatUtils.localizedCategory(topIncomeCat.categoryName, AppLanguage.EN)
            insights += BusinessInsight(
                id = "top_revenue_driver",
                titleSw = "Chanzo kikuu: $catSw",
                titleEn = "Top revenue driver: $catEn",
                detailSw = "Kinachangia ${topIncomeCat.sharePercent}% (${FormatUtils.formatCompactMoney(topIncomeCat.totalAmount, currency)}) ya mapato.",
                detailEn = "Generates ${topIncomeCat.sharePercent}% (${FormatUtils.formatCompactMoney(topIncomeCat.totalAmount, currency)}) of total income.",
                severity = InsightSeverity.POSITIVE
            )
        }

        return BusinessHealthReport(
            score = finalScore,
            grade = grade,
            profitMarginPct = profitMarginPct,
            incomeMoMDeltaPct = incomeMoM,
            expenseMoMDeltaPct = expenseMoM,
            collectionRatePct = collectionRatePct,
            cashRunwayMonths = runwayMonths,
            overdueBillsCount = overdueBills.size,
            overdueInvoicesCount = overdueInvoices.size,
            budgetAlertsCount = overBudgetCategories.size,
            insights = insights.take(4)
        )
    }
}

object PartyDirectoryEngine {

    private data class MutableParty(
        var displayName: String,
        var phone: String = "",
        var email: String = "",
        var address: String = "",
        var isHousehold: Boolean = false,
        var isCustomer: Boolean = false,
        var isVendor: Boolean = false,
        var isSavings: Boolean = false,
        var totalReceived: Double = 0.0,
        var totalPaidOut: Double = 0.0,
        var savingsTotal: Double = 0.0,
        var unpaidReceivables: Double = 0.0,
        var unpaidBills: Double = 0.0,
        var invoiceCount: Int = 0,
        var billCount: Int = 0,
        var savingsCount: Int = 0,
        var transactionCount: Int = 0,
        val modules: MutableSet<String> = mutableSetOf(),
        var lastActivityDate: String = ""
    )

    /**
     * Extracts a recognized person/party name from a savings log note formatted as "Name · Note".
     */
    fun extractPartyFromSavingsNote(note: String): String {
        val clean = note.trim()
        if (clean.isEmpty()) return ""
        val sepIndex = clean.indexOf("·")
        if (sepIndex > 0) {
            return clean.substring(0, sepIndex).trim()
        }
        return ""
    }

    /**
     * Indexes EVERY name entered anywhere across Transactions, Bills, Savings, and Invoices
     * so the entire app recognizes and remembers every person or entity automatically.
     */
    fun buildDirectory(
        transactions: List<TransactionEntity>,
        invoices: List<InvoiceWithItems>,
        bills: List<BillEntity>,
        savingsGoals: List<SavingsGoalEntity> = emptyList(),
        savingsLogs: List<SavingsLogEntity> = emptyList()
    ): List<PartySummary> {
        val map = LinkedHashMap<String, MutableParty>()

        fun getOrCreate(rawName: String): MutableParty? {
            val clean = rawName.trim()
            if (clean.isBlank()) return null
            val key = clean.lowercase(Locale.ROOT)
            return map.getOrPut(key) { MutableParty(displayName = clean) }
        }

        // 1. Transactions (including manual, invoice, bill, household, and savings-linked entries)
        val savingsLogIdsSeen = mutableSetOf<String>()
        transactions.forEach { txn ->
            val p = getOrCreate(txn.party) ?: return@forEach
            if (AutoAccountingEngine.isHouseholdTransaction(txn)) {
                p.isHousehold = true
            }
            if (txn.source == "savings") {
                p.isSavings = true
                p.savingsCount++
                p.savingsTotal += txn.amount
                p.modules += "savings"
                txn.linkedId?.let { savingsLogIdsSeen += it }
            } else {
                p.transactionCount++
                p.modules += "transaction"
                if (txn.isIncome) {
                    p.isCustomer = true
                    p.totalReceived += txn.amount
                } else {
                    p.isVendor = true
                    p.totalPaidOut += txn.amount
                }
            }
            if (txn.date > p.lastActivityDate) p.lastActivityDate = txn.date
        }

        // 2. Invoices (Customer names + contact details)
        invoices.forEach { inv ->
            val p = getOrCreate(inv.invoice.customerName) ?: return@forEach
            p.isCustomer = true
            p.invoiceCount++
            p.modules += "invoice"
            if (p.phone.isBlank() && inv.invoice.customerPhone.isNotBlank()) p.phone = inv.invoice.customerPhone
            if (p.email.isBlank() && inv.invoice.customerEmail.isNotBlank()) p.email = inv.invoice.customerEmail
            if (p.address.isBlank() && inv.invoice.customerAddress.isNotBlank()) p.address = inv.invoice.customerAddress
            if (!inv.isDraft && inv.balance > 0.005) {
                p.unpaidReceivables += inv.balance
            }
            if (inv.invoice.issueDate > p.lastActivityDate) p.lastActivityDate = inv.invoice.issueDate
        }

        // 3. Bills (Payee names)
        bills.forEach { bill ->
            val p = getOrCreate(bill.payee) ?: return@forEach
            if (AutoAccountingEngine.isHouseholdBill(bill)) {
                p.isHousehold = true
            }
            p.isVendor = true
            p.billCount++
            p.modules += "bill"
            if (!bill.paid) {
                p.unpaidBills += bill.amount
            }
            if (bill.dueDate > p.lastActivityDate) p.lastActivityDate = bill.dueDate
        }

        // 4. Savings Goals & Logs (Contributor names and goal titles not already covered by linked txns)
        val goalsById = savingsGoals.associateBy { it.id }
        savingsLogs.forEach { log ->
            if (log.id in savingsLogIdsSeen) return@forEach
            val parsedParty = extractPartyFromSavingsNote(log.note)
            val name = parsedParty.ifBlank { goalsById[log.savingsId]?.title.orEmpty() }
            val p = getOrCreate(name) ?: return@forEach
            p.isSavings = true
            p.savingsCount++
            p.savingsTotal += log.amount
            p.modules += "savings"
            if (log.date > p.lastActivityDate) p.lastActivityDate = log.date
        }

        savingsGoals.forEach { goal ->
            val key = goal.title.trim().lowercase(Locale.ROOT)
            if (key.isNotBlank() && key !in map) {
                val p = getOrCreate(goal.title) ?: return@forEach
                p.isSavings = true
                p.savingsTotal += goal.currentAmount
                p.modules += "savings"
                if (goal.updatedAt > p.lastActivityDate) p.lastActivityDate = goal.updatedAt
            }
        }

        return map.values.map { p ->
            val role = when {
                p.isHousehold -> PartyRole.HOUSEHOLD
                p.isCustomer && p.isVendor -> PartyRole.BOTH
                p.isCustomer -> PartyRole.CUSTOMER
                p.isVendor -> PartyRole.VENDOR
                p.isSavings -> PartyRole.SAVINGS
                else -> PartyRole.VENDOR
            }
            PartySummary(
                name = p.displayName,
                role = role,
                phone = p.phone,
                email = p.email,
                address = p.address,
                totalReceived = p.totalReceived,
                totalPaidOut = p.totalPaidOut,
                savingsTotal = p.savingsTotal,
                unpaidReceivables = p.unpaidReceivables,
                unpaidBills = p.unpaidBills,
                invoiceCount = p.invoiceCount,
                billCount = p.billCount,
                savingsCount = p.savingsCount,
                transactionCount = p.transactionCount,
                modules = p.modules.toSet(),
                lastActivityDate = p.lastActivityDate
            )
        }.sortedWith(
            compareByDescending<PartySummary> { it.unpaidReceivables > 0.0 }
                .thenByDescending { it.unpaidReceivables + it.totalReceived + it.totalPaidOut + it.savingsTotal }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }

    /**
     * Finds an exact case-insensitive match for any typed name in the directory.
     */
    fun matchParty(query: String, directory: List<PartySummary>): PartySummary? {
        val clean = query.trim().lowercase(Locale.ROOT)
        if (clean.isEmpty()) return null
        return directory.firstOrNull { it.name.trim().lowercase(Locale.ROOT) == clean }
    }

    /**
     * Returns all names the user has saved under Household Bills so the system
     * automatically recognizes them in future entries.
     */
    fun householdNames(directory: List<PartySummary>): Set<String> =
        directory
            .filter { it.role == PartyRole.HOUSEHOLD }
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    /**
     * Suggests matching names across all modules using real user-saved records only.
     */
    fun suggestParties(
        query: String,
        directory: List<PartySummary>,
        limit: Int = 8
    ): List<PartySummary> {
        val q = query.trim().lowercase(Locale.ROOT)
        val combined = LinkedHashMap<String, PartySummary>()

        directory.forEach { p ->
            val key = p.name.trim().lowercase(Locale.ROOT)
            if (key.isNotBlank()) {
                combined[key] = p
            }
        }

        val all = combined.values.toList()
        if (q.isEmpty()) return all.take(limit)

        val startsWith = all.filter { it.name.lowercase(Locale.ROOT).startsWith(q) }
        val contains = all.filter {
            val low = it.name.lowercase(Locale.ROOT)
            !low.startsWith(q) && low.contains(q)
        }
        return (startsWith + contains).take(limit)
    }
}
