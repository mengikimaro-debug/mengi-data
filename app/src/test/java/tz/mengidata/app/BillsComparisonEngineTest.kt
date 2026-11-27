package tz.mengidata.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tz.mengidata.app.data.accounting.AutoAccountingEngine
import tz.mengidata.app.data.accounting.BillCategoryTab
import tz.mengidata.app.data.accounting.BillsComparisonEngine
import tz.mengidata.app.data.accounting.HigherBillCategory
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage

class BillsComparisonEngineTest {

    @Test
    fun separatesHouseholdAndPersonalAutomatically_withZeroDuplicates() {
        val today = "2026-05-15"
        val yesterday = "2026-05-14"

        val bills = listOf(
            // Paid household bill linked to txn_h1 (must NOT be double-counted)
            BillEntity(
                id = "bill_h1",
                name = "Bili ya Nyumbani",
                category = AutoAccountingEngine.HOUSEHOLD_CATEGORY,
                payee = "Mama",
                amount = 60_000.0,
                dueDate = today,
                paid = true,
                paidDate = today,
                linkedTxnId = "txn_h1"
            ),
            // Unpaid personal bill
            BillEntity(
                id = "bill_p1",
                name = "LUKU Ofisi",
                category = "Umeme & maji",
                payee = "TANESCO",
                amount = 45_000.0,
                dueDate = today,
                paid = false
            )
        )

        val transactions = listOf(
            // Linked to bill_h1 -> MUST be skipped to avoid duplicate counting
            TransactionEntity(
                id = "txn_h1",
                type = "expense",
                date = today,
                description = "Bili ya Nyumbani",
                party = "Mama",
                category = AutoAccountingEngine.HOUSEHOLD_CATEGORY,
                amount = 60_000.0,
                source = "bill",
                linkedId = "bill_h1"
            ),
            // Unlinked direct household expense on yesterday -> MUST appear in Household tab automatically
            TransactionEntity(
                id = "txn_h2",
                type = "expense",
                date = yesterday,
                description = "Pesa ya home sokoni",
                party = "Nyumbani",
                category = "Bili ya Nyumbani",
                amount = 40_000.0,
                source = AutoAccountingEngine.HOUSEHOLD_SOURCE
            ),
            // Unlinked personal expense today -> MUST appear in Personal tab automatically
            TransactionEntity(
                id = "txn_p2",
                type = "expense",
                date = today,
                description = "Usafiri kazini",
                party = "Bolt",
                category = "Usafiri",
                amount = 25_000.0,
                source = "manual"
            ),
            // Income -> MUST be ignored
            TransactionEntity(
                id = "txn_inc",
                type = "income",
                date = today,
                description = "Mauzo ya duka",
                category = "Mauzo ya bidhaa",
                amount = 500_000.0
            ),
            // Savings deposit -> MUST be ignored
            TransactionEntity(
                id = "txn_sav",
                type = "expense",
                date = today,
                description = "Weka akiba kibubu",
                category = "Akiba",
                amount = 80_000.0,
                source = "savings"
            )
        )

        val summary = BillsComparisonEngine.analyze(
            bills = bills,
            transactions = transactions,
            todayIso = today
        )

        // Household: bill_h1 (60k) + txn_h2 (40k) = 100k (no duplicate from txn_h1!)
        assertEquals(100_000.0, summary.householdTotal, 0.01)
        assertEquals(2, summary.householdCount)
        assertEquals(1, summary.householdAutoCount)

        // Personal: bill_p1 (45k) + txn_p2 (25k) = 70k (income & savings ignored)
        assertEquals(70_000.0, summary.personalTotal, 0.01)
        assertEquals(2, summary.personalCount)
        assertEquals(1, summary.personalAutoCount)

        // Overall higher: Household by 30,000
        assertEquals(HigherBillCategory.HOUSEHOLD, summary.higherOverall)
        assertEquals(30_000.0, summary.overallDifference, 0.01)

        // Today higher: Personal (70k) vs Household (60k) -> Personal higher by 10,000
        assertEquals(60_000.0, summary.householdTodayTotal, 0.01)
        assertEquals(70_000.0, summary.personalTodayTotal, 0.01)
        assertEquals(HigherBillCategory.PERSONAL, summary.higherToday)
        assertEquals(10_000.0, summary.todayDifference, 0.01)

        // Daily averages: Household 100k / 2 days = 50k; Personal 70k / 1 day = 70k
        assertEquals(2, summary.householdDaysCount)
        assertEquals(1, summary.personalDaysCount)
        assertEquals(50_000.0, summary.householdDailyAvg, 0.01)
        assertEquals(70_000.0, summary.personalDailyAvg, 0.01)
    }

    @Test
    fun groupsAutomaticallyPerDayNewestFirst_withCrossCategoryComparisonAndFiltering() {
        val today = "2026-05-20"
        val day2 = "2026-05-19"
        val day3 = "2026-05-17"

        val bills = listOf(
            BillEntity(
                id = "p_old",
                name = "Kodi ya Pango",
                category = "Kodi ya pango",
                amount = 200_000.0,
                dueDate = day3,
                paid = true,
                paidDate = day3
            ),
            BillEntity(
                id = "p_today",
                name = "Mtandao Fiber",
                category = "Mtandao & simu",
                amount = 80_000.0,
                dueDate = today,
                paid = false
            )
        )

        val transactions = listOf(
            TransactionEntity(
                id = "p_day2",
                type = "expense",
                date = day2,
                description = "Chakula mchana",
                category = "Chakula",
                amount = 15_000.0
            ),
            TransactionEntity(
                id = "h_today",
                type = "expense",
                date = today,
                description = "Pesa ya nyumbani",
                category = "Bili ya Nyumbani",
                amount = 30_000.0,
                source = AutoAccountingEngine.HOUSEHOLD_SOURCE
            )
        )

        val summary = BillsComparisonEngine.analyze(
            bills = bills,
            transactions = transactions,
            todayIso = today
        )

        val personalGroups = summary.dailyGroupsFor(BillCategoryTab.PERSONAL)
        assertEquals(listOf(today, day2, day3), personalGroups.map { it.dayIso })

        // Today personal (80k) vs household (30k) -> personal higher by 50k
        val todayGroup = personalGroups.first()
        assertEquals(80_000.0, todayGroup.dayTotal, 0.01)
        assertEquals(30_000.0, todayGroup.otherCategoryDayTotal, 0.01)
        assertTrue(todayGroup.isHigherThanOtherOnDay)
        assertEquals(50_000.0, todayGroup.dayDifference, 0.01)

        // Filter to "auto" -> only day2 transaction remains with recalculated dayTotal
        val autoOnly = BillsComparisonEngine.filterDailyGroups(
            groups = personalGroups,
            filterKey = "auto",
            query = "",
            language = AppLanguage.SW
        )
        assertEquals(1, autoOnly.size)
        assertEquals(day2, autoOnly.first().dayIso)
        assertEquals(15_000.0, autoOnly.first().dayTotal, 0.01)
        assertFalse(autoOnly.first().entries.isEmpty())

        // Filter by search query "fiber" -> only today's Fiber bill remains
        val fiberOnly = BillsComparisonEngine.filterDailyGroups(
            groups = personalGroups,
            filterKey = "all",
            query = "fiber",
            language = AppLanguage.SW
        )
        assertEquals(1, fiberOnly.size)
        assertEquals("Mtandao Fiber", fiberOnly.first().entries.first().title)
    }
}
