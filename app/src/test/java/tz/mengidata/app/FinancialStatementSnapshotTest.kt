package tz.mengidata.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tz.mengidata.app.data.accounting.AutoAccountingEngine
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.InvoiceEntity
import tz.mengidata.app.data.db.InvoiceItemEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.data.pdf.FinancialStatementPdfGenerator
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter

class FinancialStatementSnapshotTest {

    @Test
    fun emptyDatasetProducesZeroSafeSnapshot() {
        val snap = FinancialStatementPdfGenerator.computeSnapshot(
            transactions = emptyList(),
            bills = emptyList(),
            invoices = emptyList(),
            savingsGoals = emptyList(),
            period = PeriodFilter.MONTH
        )

        assertTrue(snap.periodTransactions.isEmpty())
        assertEquals(0.0, snap.incomeTotal, 0.001)
        assertEquals(0.0, snap.totalOutflow, 0.001)
        assertEquals(0.0, snap.netBalance, 0.001)
        assertNull(snap.netMarginPercent)
        assertEquals(0, snap.incomeFlowSharePct)
        assertEquals(0, snap.averageGoalCompletionPct)
        assertTrue(snap.topCategories.isEmpty())
    }

    @Test
    fun computesFourBucketsFlowSharesObligationsAndTopCategories() {
        val today = FormatUtils.todayIso()
        val txns = listOf(
            TransactionEntity(
                id = "t1",
                type = "income",
                date = today,
                description = "Client Retainer",
                party = "Kilimanjaro Co",
                category = "Huduma",
                amount = 600_000.0
            ),
            TransactionEntity(
                id = "t2",
                type = "expense",
                date = today,
                description = "Office Supplies",
                party = "Stationery Hub",
                category = "Vifaa vya ofisi",
                amount = 200_000.0
            ),
            TransactionEntity(
                id = "t3",
                type = "expense",
                date = today,
                description = "Pesa ya nyumbani",
                party = "Familia",
                category = AutoAccountingEngine.HOUSEHOLD_CATEGORY,
                amount = 100_000.0,
                source = AutoAccountingEngine.HOUSEHOLD_SOURCE
            ),
            TransactionEntity(
                id = "t4",
                type = "expense",
                date = today,
                description = "Akiba Kibubu",
                party = "Vault",
                category = "Akiba",
                amount = 100_000.0
            )
        )

        val bills = listOf(
            BillEntity(id = "b1", name = "TANESCO", amount = 85_000.0, paid = false),
            BillEntity(id = "b2", name = "DAWASA", amount = 30_000.0, paid = true)
        )

        val invoices = listOf(
            InvoiceWithItems(
                invoice = InvoiceEntity(id = "i1", number = "MD-101", customerName = "Alpha Ltd", state = "issued", amountPaid = 50_000.0),
                items = listOf(InvoiceItemEntity(invoiceId = "i1", description = "Consulting", qty = 1.0, price = 250_000.0))
            ),
            InvoiceWithItems(
                invoice = InvoiceEntity(id = "i2", number = "MD-102", customerName = "Draft Co", state = "draft", amountPaid = 0.0),
                items = listOf(InvoiceItemEntity(invoiceId = "i2", description = "Draft Item", qty = 1.0, price = 100_000.0))
            )
        )

        val goals = listOf(
            SavingsGoalEntity(id = "g1", title = "Emergency", targetAmount = 1_000_000.0, currentAmount = 500_000.0),
            SavingsGoalEntity(id = "g2", title = "Equipment", targetAmount = 500_000.0, currentAmount = 500_000.0)
        )

        val snap = FinancialStatementPdfGenerator.computeSnapshot(
            transactions = txns,
            bills = bills,
            invoices = invoices,
            savingsGoals = goals,
            period = PeriodFilter.ALL
        )

        assertEquals(4, snap.periodTransactions.size)
        assertEquals(600_000.0, snap.incomeTotal, 0.001)
        assertEquals(200_000.0, snap.personalExpenseTotal, 0.001)
        assertEquals(100_000.0, snap.householdTotal, 0.001)
        assertEquals(100_000.0, snap.savingsTotal, 0.001)
        assertEquals(400_000.0, snap.totalOutflow, 0.001)
        assertEquals(1_000_000.0, snap.totalFlowVolume, 0.001)
        assertEquals(200_000.0, snap.netBalance, 0.001)
        assertEquals(33.333, snap.netMarginPercent!!, 0.1)

        assertEquals(60, snap.incomeFlowSharePct)
        assertEquals(20, snap.personalFlowSharePct)
        assertEquals(10, snap.householdFlowSharePct)
        assertEquals(10, snap.savingsFlowSharePct)

        assertEquals(85_000.0, snap.unpaidBillsAmount, 0.001)
        assertEquals(1, snap.unpaidBillsCount)
        assertEquals(200_000.0, snap.receivablesAmount, 0.001)
        assertEquals(1, snap.openInvoicesCount)
        assertEquals(1_000_000.0, snap.totalSavingsVault, 0.001)
        assertEquals(75, snap.averageGoalCompletionPct)

        assertEquals(3, snap.topCategories.size)
        assertEquals("Huduma", snap.topCategories.first().categoryName)
        assertTrue(snap.topCategories.first().isIncome)
    }
}
