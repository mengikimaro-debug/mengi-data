package tz.mengidata.app.data.accounting

import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import java.util.Locale
import kotlin.math.abs

enum class BillCategoryTab {
    HOUSEHOLD,
    PERSONAL
}

enum class HigherBillCategory {
    HOUSEHOLD,
    PERSONAL,
    EQUAL,
    NONE
}

sealed interface BillLedgerEntry {
    val stableKey: String
    val title: String
    val party: String
    val category: String
    val method: String
    val amount: Double
    val dayIso: String
    val sortTimestamp: String
    val tab: BillCategoryTab
    val isPaid: Boolean
    val isOverdue: Boolean
    val isAutoFromLedger: Boolean

    data class ScheduledBill(
        val bill: BillEntity,
        override val dayIso: String,
        override val tab: BillCategoryTab
    ) : BillLedgerEntry {
        override val stableKey: String get() = "bill_${bill.id}"
        override val title: String get() = bill.name
        override val party: String get() = bill.payee
        override val category: String get() = bill.category
        override val method: String get() = bill.method
        override val amount: Double get() = bill.amount
        override val sortTimestamp: String get() = bill.createdAt.ifBlank { dayIso }
        override val isPaid: Boolean get() = bill.paid
        override val isOverdue: Boolean get() = bill.isOverdue
        override val isAutoFromLedger: Boolean get() = false
    }

    data class AutoExpense(
        val txn: TransactionEntity,
        override val dayIso: String,
        override val tab: BillCategoryTab
    ) : BillLedgerEntry {
        override val stableKey: String get() = "txn_${txn.id}"
        override val title: String get() = txn.description.ifBlank { txn.category }
        override val party: String get() = txn.party
        override val category: String get() = txn.category
        override val method: String get() = txn.method
        override val amount: Double get() = txn.amount
        override val sortTimestamp: String get() = txn.createdAt.ifBlank { dayIso }
        override val isPaid: Boolean get() = true
        override val isOverdue: Boolean get() = false
        override val isAutoFromLedger: Boolean get() = true
    }
}

data class DailyBillGroup(
    val dayIso: String,
    val entries: List<BillLedgerEntry>,
    val dayTotal: Double,
    val otherCategoryDayTotal: Double,
    val billCount: Int,
    val autoTxnCount: Int
) {
    val totalCount: Int get() = entries.size
    val isHigherThanOtherOnDay: Boolean get() = dayTotal > otherCategoryDayTotal + 0.005 && otherCategoryDayTotal > 0.005
    val isLowerThanOtherOnDay: Boolean get() = otherCategoryDayTotal > dayTotal + 0.005 && dayTotal > 0.005
    val dayDifference: Double get() = abs(dayTotal - otherCategoryDayTotal)
}

data class BillsComparisonSummary(
    val todayIso: String,
    val householdTotal: Double = 0.0,
    val personalTotal: Double = 0.0,
    val householdTodayTotal: Double = 0.0,
    val personalTodayTotal: Double = 0.0,
    val householdDailyAvg: Double = 0.0,
    val personalDailyAvg: Double = 0.0,
    val householdCount: Int = 0,
    val personalCount: Int = 0,
    val householdUnpaidCount: Int = 0,
    val personalUnpaidCount: Int = 0,
    val householdOverdueCount: Int = 0,
    val personalOverdueCount: Int = 0,
    val householdPaidCount: Int = 0,
    val personalPaidCount: Int = 0,
    val householdAutoCount: Int = 0,
    val personalAutoCount: Int = 0,
    val householdDaysCount: Int = 0,
    val personalDaysCount: Int = 0,
    val householdShare: Float = 0.5f,
    val personalShare: Float = 0.5f,
    val higherOverall: HigherBillCategory = HigherBillCategory.NONE,
    val overallDifference: Double = 0.0,
    val higherToday: HigherBillCategory = HigherBillCategory.NONE,
    val todayDifference: Double = 0.0,
    val householdDailyGroups: List<DailyBillGroup> = emptyList(),
    val personalDailyGroups: List<DailyBillGroup> = emptyList(),
    val dueNowCount: Int = 0
) {
    val combinedTotal: Double get() = householdTotal + personalTotal

    fun dailyGroupsFor(tab: BillCategoryTab): List<DailyBillGroup> =
        if (tab == BillCategoryTab.HOUSEHOLD) householdDailyGroups else personalDailyGroups

    fun countFor(tab: BillCategoryTab): Int =
        if (tab == BillCategoryTab.HOUSEHOLD) householdCount else personalCount

    fun unpaidCountFor(tab: BillCategoryTab): Int =
        if (tab == BillCategoryTab.HOUSEHOLD) householdUnpaidCount else personalUnpaidCount

    fun overdueCountFor(tab: BillCategoryTab): Int =
        if (tab == BillCategoryTab.HOUSEHOLD) householdOverdueCount else personalOverdueCount

    fun paidCountFor(tab: BillCategoryTab): Int =
        if (tab == BillCategoryTab.HOUSEHOLD) householdPaidCount else personalPaidCount

    fun autoCountFor(tab: BillCategoryTab): Int =
        if (tab == BillCategoryTab.HOUSEHOLD) householdAutoCount else personalAutoCount
}

/**
 * Automatically separates all Bills and unlinked Expense Transactions into two
 * comparable categories — Household Bill vs Personal Bill — grouped automatically per day
 * with zero duplicate counting and instant higher-spending detection.
 */
object BillsComparisonEngine {

    private val isoDatePrefixRegex = Regex("^\\d{4}-\\d{2}-\\d{2}")

    fun normalizeDayIso(raw: String, fallback: String): String {
        val clean = raw.trim()
        return if (clean.length >= 10 && isoDatePrefixRegex.containsMatchIn(clean)) {
            clean.substring(0, 10)
        } else {
            fallback
        }
    }

    fun analyze(
        bills: List<BillEntity>,
        transactions: List<TransactionEntity>,
        todayIso: String = FormatUtils.todayIso()
    ): BillsComparisonSummary {
        val linkedTxnIds = bills
            .mapNotNull { it.linkedTxnId?.takeIf { id -> id.isNotBlank() } }
            .toSet()
        val billIds = bills.map { it.id }.toSet()

        val allEntries = ArrayList<BillLedgerEntry>(bills.size + transactions.size)

        // 1. Scheduled / recorded bills (automatically routed into Household vs Personal)
        for (bill in bills) {
            val tab = if (AutoAccountingEngine.isHouseholdBill(bill)) {
                BillCategoryTab.HOUSEHOLD
            } else {
                BillCategoryTab.PERSONAL
            }
            val rawDate = when {
                bill.paid && bill.paidDate.isNotBlank() -> bill.paidDate
                bill.dueDate.isNotBlank() -> bill.dueDate
                else -> bill.createdAt
            }
            val dayIso = normalizeDayIso(rawDate, todayIso)
            allEntries += BillLedgerEntry.ScheduledBill(
                bill = bill,
                dayIso = dayIso,
                tab = tab
            )
        }

        // 2. Unlinked ledger expense transactions (auto-pulled per day, zero duplicates)
        for (txn in transactions) {
            if (txn.isIncome) continue
            if (AutoAccountingEngine.isSavingsTransaction(txn)) continue
            if (txn.id in linkedTxnIds) continue
            if (txn.source == "bill" && txn.linkedId != null && txn.linkedId in billIds) continue

            val tab = if (AutoAccountingEngine.isHouseholdTransaction(txn)) {
                BillCategoryTab.HOUSEHOLD
            } else {
                BillCategoryTab.PERSONAL
            }
            val dayIso = normalizeDayIso(txn.date.ifBlank { txn.createdAt }, todayIso)
            allEntries += BillLedgerEntry.AutoExpense(
                txn = txn,
                dayIso = dayIso,
                tab = tab
            )
        }

        val householdEntries = allEntries.filter { it.tab == BillCategoryTab.HOUSEHOLD }
        val personalEntries = allEntries.filter { it.tab == BillCategoryTab.PERSONAL }

        val householdByDay = householdEntries.groupBy { it.dayIso }
        val personalByDay = personalEntries.groupBy { it.dayIso }

        val householdDayTotals = householdByDay.mapValues { (_, list) -> list.sumOf { it.amount } }
        val personalDayTotals = personalByDay.mapValues { (_, list) -> list.sumOf { it.amount } }

        val householdDailyGroups = buildDailyGroups(
            grouped = householdByDay,
            otherDayTotals = personalDayTotals
        )
        val personalDailyGroups = buildDailyGroups(
            grouped = personalByDay,
            otherDayTotals = householdDayTotals
        )

        val householdTotal = householdEntries.sumOf { it.amount }
        val personalTotal = personalEntries.sumOf { it.amount }
        val householdTodayTotal = householdDayTotals[todayIso] ?: 0.0
        val personalTodayTotal = personalDayTotals[todayIso] ?: 0.0

        val householdDaysCount = householdDailyGroups.size
        val personalDaysCount = personalDailyGroups.size
        val householdDailyAvg = if (householdDaysCount > 0) householdTotal / householdDaysCount else 0.0
        val personalDailyAvg = if (personalDaysCount > 0) personalTotal / personalDaysCount else 0.0

        val combined = householdTotal + personalTotal
        val householdShare = if (combined > 0.005) (householdTotal / combined).toFloat().coerceIn(0f, 1f) else 0.5f
        val personalShare = if (combined > 0.005) (1f - householdShare).coerceIn(0f, 1f) else 0.5f

        val higherOverall = comparePair(householdTotal, personalTotal)
        val higherToday = comparePair(householdTodayTotal, personalTodayTotal)

        val dueNowCount = bills.count { !it.paid && (it.isOverdue || it.dueDate <= todayIso) }

        return BillsComparisonSummary(
            todayIso = todayIso,
            householdTotal = householdTotal,
            personalTotal = personalTotal,
            householdTodayTotal = householdTodayTotal,
            personalTodayTotal = personalTodayTotal,
            householdDailyAvg = householdDailyAvg,
            personalDailyAvg = personalDailyAvg,
            householdCount = householdEntries.size,
            personalCount = personalEntries.size,
            householdUnpaidCount = householdEntries.count { !it.isPaid },
            personalUnpaidCount = personalEntries.count { !it.isPaid },
            householdOverdueCount = householdEntries.count { it.isOverdue },
            personalOverdueCount = personalEntries.count { it.isOverdue },
            householdPaidCount = householdEntries.count { it.isPaid },
            personalPaidCount = personalEntries.count { it.isPaid },
            householdAutoCount = householdEntries.count { it.isAutoFromLedger },
            personalAutoCount = personalEntries.count { it.isAutoFromLedger },
            householdDaysCount = householdDaysCount,
            personalDaysCount = personalDaysCount,
            householdShare = householdShare,
            personalShare = personalShare,
            higherOverall = higherOverall,
            overallDifference = abs(householdTotal - personalTotal),
            higherToday = higherToday,
            todayDifference = abs(householdTodayTotal - personalTodayTotal),
            householdDailyGroups = householdDailyGroups,
            personalDailyGroups = personalDailyGroups,
            dueNowCount = dueNowCount
        )
    }

    fun filterDailyGroups(
        groups: List<DailyBillGroup>,
        filterKey: String,
        query: String,
        language: AppLanguage
    ): List<DailyBillGroup> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (filterKey == "all" && q.isEmpty()) return groups

        return groups.mapNotNull { group ->
            val filteredEntries = group.entries.filter { entry ->
                val filterOk = when (filterKey) {
                    "unpaid" -> !entry.isPaid
                    "overdue" -> entry.isOverdue
                    "paid" -> entry.isPaid
                    "auto" -> entry.isAutoFromLedger
                    else -> true
                }
                if (!filterOk) return@filter false
                if (q.isEmpty()) return@filter true

                entry.title.lowercase(Locale.ROOT).contains(q) ||
                    entry.party.lowercase(Locale.ROOT).contains(q) ||
                    entry.category.lowercase(Locale.ROOT).contains(q) ||
                    FormatUtils.localizedCategory(entry.category, language).lowercase(Locale.ROOT).contains(q) ||
                    FormatUtils.localizedMethod(entry.method, language).lowercase(Locale.ROOT).contains(q) ||
                    entry.dayIso.contains(q)
            }
            if (filteredEntries.isEmpty()) {
                null
            } else {
                DailyBillGroup(
                    dayIso = group.dayIso,
                    entries = filteredEntries,
                    dayTotal = filteredEntries.sumOf { it.amount },
                    otherCategoryDayTotal = group.otherCategoryDayTotal,
                    billCount = filteredEntries.count { !it.isAutoFromLedger },
                    autoTxnCount = filteredEntries.count { it.isAutoFromLedger }
                )
            }
        }
    }

    private fun buildDailyGroups(
        grouped: Map<String, List<BillLedgerEntry>>,
        otherDayTotals: Map<String, Double>
    ): List<DailyBillGroup> {
        return grouped.entries
            .sortedByDescending { it.key }
            .map { (dayIso, entries) ->
                val ordered = entries.sortedWith(
                    compareBy<BillLedgerEntry> { it.isPaid }
                        .thenByDescending { it.sortTimestamp }
                        .thenByDescending { it.amount }
                )
                DailyBillGroup(
                    dayIso = dayIso,
                    entries = ordered,
                    dayTotal = ordered.sumOf { it.amount },
                    otherCategoryDayTotal = otherDayTotals[dayIso] ?: 0.0,
                    billCount = ordered.count { !it.isAutoFromLedger },
                    autoTxnCount = ordered.count { it.isAutoFromLedger }
                )
            }
    }

    private fun comparePair(household: Double, personal: Double): HigherBillCategory {
        if (household <= 0.005 && personal <= 0.005) return HigherBillCategory.NONE
        val diff = household - personal
        return when {
            diff > 0.005 -> HigherBillCategory.HOUSEHOLD
            diff < -0.005 -> HigherBillCategory.PERSONAL
            else -> HigherBillCategory.EQUAL
        }
    }
}
