package tz.mengidata.app.data.accounting

import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.SavingsGoalEntity
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
    CUSTOMER,
    VENDOR,
    BOTH;

    fun label(language: AppLanguage): String = when (this) {
        CUSTOMER -> tr(language, "Mteja", "Customer")
        VENDOR -> tr(language, "Msambazaji", "Vendor")
        BOTH -> tr(language, "Mteja & Mdau", "Customer & Vendor")
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
    val unpaidReceivables: Double = 0.0,
    val unpaidBills: Double = 0.0,
    val invoiceCount: Int = 0,
    val transactionCount: Int = 0,
    val lastActivityDate: String = ""
) {
    val netFlow: Double get() = totalReceived - totalPaidOut
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

        val operationalTxns = transactions.filter { it.source != "savings" }
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
            insights += BusinessInsight(
                id = "budget_overage",
                titleSw = "Bajeti ya \"${topOver.categoryName}\" imevuka",
                titleEn = "\"${topOver.categoryName}\" exceeded budget",
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
            insights += BusinessInsight(
                id = "top_revenue_driver",
                titleSw = "Chanzo kikuu: ${topIncomeCat.categoryName}",
                titleEn = "Top revenue driver: ${topIncomeCat.categoryName}",
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
        var isCustomer: Boolean = false,
        var isVendor: Boolean = false,
        var totalReceived: Double = 0.0,
        var totalPaidOut: Double = 0.0,
        var unpaidReceivables: Double = 0.0,
        var unpaidBills: Double = 0.0,
        var invoiceCount: Int = 0,
        var transactionCount: Int = 0,
        var lastActivityDate: String = ""
    )

    fun buildDirectory(
        transactions: List<TransactionEntity>,
        invoices: List<InvoiceWithItems>,
        bills: List<BillEntity>
    ): List<PartySummary> {
        val map = LinkedHashMap<String, MutableParty>()

        fun getOrCreate(rawName: String): MutableParty? {
            val clean = rawName.trim()
            if (clean.isBlank()) return null
            val key = clean.lowercase(Locale.ROOT)
            return map.getOrPut(key) { MutableParty(displayName = clean) }
        }

        transactions.filter { it.source != "savings" }.forEach { txn ->
            val p = getOrCreate(txn.party) ?: return@forEach
            p.transactionCount++
            if (txn.isIncome) {
                p.isCustomer = true
                p.totalReceived += txn.amount
            } else {
                p.isVendor = true
                p.totalPaidOut += txn.amount
            }
            if (txn.date > p.lastActivityDate) p.lastActivityDate = txn.date
        }

        invoices.forEach { inv ->
            val p = getOrCreate(inv.invoice.customerName) ?: return@forEach
            p.isCustomer = true
            p.invoiceCount++
            if (p.phone.isBlank() && inv.invoice.customerPhone.isNotBlank()) p.phone = inv.invoice.customerPhone
            if (p.email.isBlank() && inv.invoice.customerEmail.isNotBlank()) p.email = inv.invoice.customerEmail
            if (p.address.isBlank() && inv.invoice.customerAddress.isNotBlank()) p.address = inv.invoice.customerAddress
            if (!inv.isDraft && inv.balance > 0.005) {
                p.unpaidReceivables += inv.balance
            }
            if (inv.invoice.issueDate > p.lastActivityDate) p.lastActivityDate = inv.invoice.issueDate
        }

        bills.forEach { bill ->
            val p = getOrCreate(bill.payee) ?: return@forEach
            p.isVendor = true
            if (!bill.paid) {
                p.unpaidBills += bill.amount
            }
            if (bill.dueDate > p.lastActivityDate) p.lastActivityDate = bill.dueDate
        }

        return map.values.map { p ->
            val role = when {
                p.isCustomer && p.isVendor -> PartyRole.BOTH
                p.isCustomer -> PartyRole.CUSTOMER
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
                unpaidReceivables = p.unpaidReceivables,
                unpaidBills = p.unpaidBills,
                invoiceCount = p.invoiceCount,
                transactionCount = p.transactionCount,
                lastActivityDate = p.lastActivityDate
            )
        }.sortedWith(
            compareByDescending<PartySummary> { it.unpaidReceivables > 0.0 }
                .thenByDescending { it.unpaidReceivables + it.totalReceived + it.totalPaidOut }
        )
    }
}
