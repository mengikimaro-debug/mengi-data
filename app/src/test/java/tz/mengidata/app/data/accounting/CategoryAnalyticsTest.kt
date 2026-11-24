package tz.mengidata.app.data.accounting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tz.mengidata.app.data.db.TransactionCategoryEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter

class CategoryAnalyticsTest {

    @Test
    fun analyze_groupsIncomeAndExpenseWithExactZeroLossTotals() {
        val today = FormatUtils.todayIso()
        val customCat = TransactionCategoryEntity(
            id = "cat_custom_fuel",
            name = "Fuel & Delivery",
            type = "expense",
            iconKey = "truck",
            colorHex = "#0369A1",
            monthlyBudget = 200_000.0
        )
        val categories = CategoryAnalyticsEngine.defaultCategories(today) + customCat

        val txns = listOf(
            TransactionEntity("t1", "income", today, "POS Sales", category = "Mauzo ya bidhaa", amount = 600_000.0),
            TransactionEntity("t2", "income", today, "Shop Sales", category = "Mauzo ya bidhaa", amount = 400_000.0),
            TransactionEntity("t3", "income", today, "Consulting", category = "Ushauri", amount = 250_000.0),
            TransactionEntity("t4", "expense", today, "Diesel van", category = "Fuel & Delivery", amount = 250_000.0),
            TransactionEntity("t5", "expense", today, "Office rent", category = "Kodi ya pango", amount = 500_000.0),
            TransactionEntity("t6", "expense", today, "Uncategorized misc", category = "", amount = 50_000.0)
        )

        val report = CategoryAnalyticsEngine.analyze(txns, categories, PeriodFilter.ALL)

        assertEquals(1_250_000.0, report.totalIncome, 0.01)
        assertEquals(800_000.0, report.totalExpense, 0.01)
        assertEquals(450_000.0, report.netResult, 0.01)

        // Zero-loss total conservation across grouped buckets
        assertEquals(report.totalIncome, report.incomeBreakdown.sumOf { it.totalAmount }, 0.01)
        assertEquals(report.totalExpense, report.expenseBreakdown.sumOf { it.totalAmount }, 0.01)
        assertEquals(100f, report.incomeBreakdown.sumOf { it.sharePercent.toDouble() }.toFloat(), 0.2f)
        assertEquals(100f, report.expenseBreakdown.sumOf { it.sharePercent.toDouble() }.toFloat(), 0.2f)

        // Sorted descending by amount
        assertEquals("Mauzo ya bidhaa", report.topIncomeCategory?.categoryName)
        assertEquals(2, report.topIncomeCategory?.transactionCount)
        assertEquals("Kodi ya pango", report.topExpenseCategory?.categoryName)

        // Custom category metadata and budget tracking
        val fuel = report.expenseBreakdown.first { it.categoryName == "Fuel & Delivery" }
        assertEquals("truck", fuel.iconKey)
        assertTrue(fuel.isOverBudget)
        assertEquals(1.25f, fuel.budgetRatio, 0.01f)

        // Fallback category for blank category string
        val fallback = report.expenseBreakdown.first { it.categoryName == "Matumizi mengine" }
        assertEquals(50_000.0, fallback.totalAmount, 0.01)
        assertFalse(fallback.isOverBudget)
    }
}
