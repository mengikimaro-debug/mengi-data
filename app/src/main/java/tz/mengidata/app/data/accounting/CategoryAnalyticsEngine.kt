package tz.mengidata.app.data.accounting

import tz.mengidata.app.data.db.TransactionCategoryEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter
import kotlin.math.max

data class CategoryBreakdownItem(
    val categoryName: String,
    val type: String, // "income" | "expense"
    val iconKey: String,
    val colorHex: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val sharePercent: Float, // 0f..100f
    val monthlyBudget: Double = 0.0
) {
    val budgetRatio: Float
        get() = if (monthlyBudget <= 0.0) 0f else (totalAmount / monthlyBudget).toFloat()

    val isOverBudget: Boolean
        get() = monthlyBudget > 0.0 && totalAmount > monthlyBudget
}

data class CategoryAnalyticsReport(
    val period: PeriodFilter = PeriodFilter.MONTH,
    val incomeBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val expenseBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalTransactionsAnalyzed: Int = 0
) {
    val topIncomeCategory: CategoryBreakdownItem? get() = incomeBreakdown.firstOrNull()
    val topExpenseCategory: CategoryBreakdownItem? get() = expenseBreakdown.firstOrNull()
    val netResult: Double get() = totalIncome - totalExpense
}

object CategoryAnalyticsEngine {

    /**
     * Accessible, non-red color swatches for user-created business categories.
     */
    val paletteHexOptions: List<String> = listOf(
        "#047857", // Emerald
        "#1D4ED8", // Royal Cobalt
        "#4338CA", // Deep Indigo
        "#0E7490", // Ocean Teal
        "#B45309", // Warm Amber
        "#5E4D82", // Soft Muted Lavender
        "#334155", // Executive Slate
        "#0369A1"  // Sapphire Sky
    )

    val iconKeyOptions: List<String> = listOf(
        "store",
        "work",
        "receipt",
        "inventory",
        "home",
        "people",
        "bolt",
        "wifi",
        "truck",
        "food",
        "build",
        "campaign",
        "shield",
        "savings",
        "tag"
    )

    fun defaultCategories(today: String = FormatUtils.todayIso()): List<TransactionCategoryEntity> = listOf(
        // Income categories
        TransactionCategoryEntity("cat_inc_sales", "Mauzo ya bidhaa", "income", "store", "#047857", 0.0, true, today),
        TransactionCategoryEntity("cat_inc_services", "Huduma", "income", "work", "#1D4ED8", 0.0, true, today),
        TransactionCategoryEntity("cat_inc_consulting", "Ushauri", "income", "campaign", "#4338CA", 0.0, true, today),
        TransactionCategoryEntity("cat_inc_invoices", "Malipo ya ankara", "income", "receipt", "#0E7490", 0.0, true, today),
        TransactionCategoryEntity("cat_inc_other", "Mapato mengine", "income", "tag", "#0369A1", 0.0, true, today),
        // Expense categories
        TransactionCategoryEntity("cat_exp_stock", "Manunuzi / stock", "expense", "inventory", "#1D4ED8", 0.0, true, today),
        TransactionCategoryEntity("cat_exp_rent", "Kodi ya pango", "expense", "home", "#5E4D82", 0.0, true, today),
        TransactionCategoryEntity("cat_exp_payroll", "Mishahara", "expense", "people", "#4338CA", 0.0, true, today),
        TransactionCategoryEntity("cat_exp_utilities", "Umeme & maji", "expense", "bolt", "#B45309", 0.0, true, today),
        TransactionCategoryEntity("cat_exp_internet", "Internet & simu", "expense", "wifi", "#0E7490", 0.0, true, today),
        TransactionCategoryEntity("cat_exp_transport", "Usafiri", "expense", "truck", "#0369A1", 0.0, true, today),
        TransactionCategoryEntity("cat_exp_food", "Chakula", "expense", "food", "#B45309", 0.0, true, today),
        TransactionCategoryEntity("cat_exp_maintenance", "Matengenezo", "expense", "build", "#334155", 0.0, true, today),
        TransactionCategoryEntity("cat_exp_marketing", "Masoko & matangazo", "expense", "campaign", "#4338CA", 0.0, true, today),
        TransactionCategoryEntity("cat_exp_tax", "Kodi & vibali", "expense", "shield", "#5E4D82", 0.0, true, today),
        TransactionCategoryEntity("cat_exp_health", "Afya & bima", "expense", "shield", "#0E7490", 0.0, true, today),
        TransactionCategoryEntity("cat_exp_savings", "Akiba", "expense", "savings", "#1D4ED8", 0.0, true, today),
        TransactionCategoryEntity("cat_exp_other", "Matumizi mengine", "expense", "tag", "#334155", 0.0, true, today)
    )

    fun analyze(
        transactions: List<TransactionEntity>,
        categories: List<TransactionCategoryEntity>,
        period: PeriodFilter = PeriodFilter.ALL
    ): CategoryAnalyticsReport {
        val inPeriod = transactions.filter { FormatUtils.isDateInPeriod(it.date, period) }
        val categoryLookup = categories.associateBy { "${it.type}:${it.name.trim().lowercase()}" }

        val incomeTxns = inPeriod.filter { it.isIncome }
        val expenseTxns = inPeriod.filter { !it.isIncome }

        val totalIncome = incomeTxns.sumOf { max(it.amount, 0.0) }
        val totalExpense = expenseTxns.sumOf { max(it.amount, 0.0) }

        val incomeBreakdown = buildBreakdown(
            txns = incomeTxns,
            type = "income",
            total = totalIncome,
            fallbackName = "Mapato mengine",
            fallbackColor = "#047857",
            lookup = categoryLookup
        )

        val expenseBreakdown = buildBreakdown(
            txns = expenseTxns,
            type = "expense",
            total = totalExpense,
            fallbackName = "Matumizi mengine",
            fallbackColor = "#5E4D82",
            lookup = categoryLookup
        )

        return CategoryAnalyticsReport(
            period = period,
            incomeBreakdown = incomeBreakdown,
            expenseBreakdown = expenseBreakdown,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalTransactionsAnalyzed = inPeriod.size
        )
    }

    private fun buildBreakdown(
        txns: List<TransactionEntity>,
        type: String,
        total: Double,
        fallbackName: String,
        fallbackColor: String,
        lookup: Map<String, TransactionCategoryEntity>
    ): List<CategoryBreakdownItem> {
        if (txns.isEmpty()) return emptyList()

        return txns
            .groupBy { it.category.trim().ifBlank { fallbackName } }
            .map { (catName, group) ->
                val meta = lookup["$type:${catName.lowercase()}"]
                val sum = group.sumOf { max(it.amount, 0.0) }
                val share = if (total > 0.0) ((sum / total) * 100.0).toFloat() else 0f
                CategoryBreakdownItem(
                    categoryName = catName,
                    type = type,
                    iconKey = meta?.iconKey ?: "tag",
                    colorHex = meta?.colorHex ?: fallbackColor,
                    totalAmount = sum,
                    transactionCount = group.size,
                    sharePercent = share,
                    monthlyBudget = meta?.monthlyBudget ?: 0.0
                )
            }
            .sortedByDescending { it.totalAmount }
    }
}
