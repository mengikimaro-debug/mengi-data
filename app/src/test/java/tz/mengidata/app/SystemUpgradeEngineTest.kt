package tz.mengidata.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tz.mengidata.app.data.accounting.BusinessHealthEngine
import tz.mengidata.app.data.accounting.CategoryAnalyticsEngine
import tz.mengidata.app.data.accounting.HealthGrade
import tz.mengidata.app.data.accounting.PartyDirectoryEngine
import tz.mengidata.app.data.accounting.PartyRole
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.InvoiceEntity
import tz.mengidata.app.data.db.InvoiceItemEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.data.prefs.PinSecurityHelper
import tz.mengidata.app.i18n.PeriodFilter

class SystemUpgradeEngineTest {

    @Test
    fun pinSecurityHelper_hashesAndVerifiesWithSalt() {
        assertTrue(PinSecurityHelper.isValidPin("2580"))
        assertFalse(PinSecurityHelper.isValidPin("123"))
        assertFalse(PinSecurityHelper.isValidPin("12a4"))

        val salt1 = PinSecurityHelper.generateSalt()
        val salt2 = PinSecurityHelper.generateSalt()
        assertNotEquals(salt1, salt2)

        val hash1 = PinSecurityHelper.hashPin("2580", salt1)
        val hash2 = PinSecurityHelper.hashPin("2580", salt2)
        assertNotEquals(hash1, hash2)
        assertTrue(PinSecurityHelper.verifyPin("2580", salt1, hash1))
        assertFalse(PinSecurityHelper.verifyPin("0000", salt1, hash1))
    }

    @Test
    fun businessHealthEngine_computesStrongScoreAndGrowthInsight() {
        val today = "2026-11-20"
        val txns = listOf(
            TransactionEntity(id = "t1", type = "income", date = "2026-11-10", description = "Mauzo", party = "Tumaini", category = "Mauzo ya bidhaa", amount = 2_000_000.0),
            TransactionEntity(id = "t2", type = "expense", date = "2026-11-12", description = "Stock", party = "Kariakoo", category = "Manunuzi / stock", amount = 500_000.0),
            TransactionEntity(id = "t3", type = "income", date = "2026-10-10", description = "Mauzo Oct", party = "Tumaini", category = "Mauzo ya bidhaa", amount = 1_000_000.0)
        )
        val goals = listOf(
            SavingsGoalEntity(id = "g1", title = "Dharura", targetAmount = 2_000_000.0, currentAmount = 1_600_000.0)
        )
        val catReport = CategoryAnalyticsEngine.analyze(txns, CategoryAnalyticsEngine.defaultCategories(), PeriodFilter.ALL)

        val report = BusinessHealthEngine.evaluate(
            transactions = txns,
            invoices = emptyList(),
            bills = emptyList(),
            savingsGoals = goals,
            categoryAnalytics = catReport,
            currency = "TZS",
            todayIso = today
        )

        assertTrue("Expected high score >= 80, got ${report.score}", report.score >= 80)
        assertEquals(HealthGrade.EXCELLENT, report.grade)
        assertTrue(report.profitMarginPct > 70.0)
        assertNotNull(report.incomeMoMDeltaPct)
        assertEquals(100.0, report.incomeMoMDeltaPct!!, 0.01)
        assertTrue(report.insights.any { it.id == "income_growth" })
    }

    @Test
    fun partyDirectoryEngine_aggregatesCustomersVendorsAndReceivables() {
        val txns = listOf(
            TransactionEntity(id = "t1", type = "income", date = "2026-11-05", description = "POS", party = "Tumaini Stores", category = "Mauzo ya bidhaa", amount = 800_000.0),
            TransactionEntity(id = "t2", type = "expense", date = "2026-11-06", description = "Umeme", party = "TANESCO", category = "Umeme & maji", amount = 120_000.0)
        )
        val inv = InvoiceWithItems(
            invoice = InvoiceEntity(
                id = "inv1",
                number = "MD-2026-001",
                customerName = "Tumaini Stores",
                customerPhone = "+255754000111",
                issueDate = "2026-11-08",
                dueDate = "2026-11-22",
                state = "issued",
                amountPaid = 300_000.0
            ),
            items = listOf(
                InvoiceItemEntity(invoiceId = "inv1", description = "Mfumo", qty = 1.0, price = 1_000_000.0)
            )
        )
        val bill = BillEntity(
            id = "b1",
            name = "LUKU",
            payee = "TANESCO",
            amount = 85_000.0,
            dueDate = "2026-11-25",
            paid = false
        )

        val parties = PartyDirectoryEngine.buildDirectory(txns, listOf(inv), listOf(bill))
        assertEquals(2, parties.size)

        val tumaini = parties.first { it.name == "Tumaini Stores" }
        assertEquals(PartyRole.CUSTOMER, tumaini.role)
        assertEquals("+255754000111", tumaini.phone)
        assertEquals(800_000.0, tumaini.totalReceived, 0.01)
        assertEquals(700_000.0, tumaini.unpaidReceivables, 0.01)

        val tanesco = parties.first { it.name == "TANESCO" }
        assertEquals(PartyRole.VENDOR, tanesco.role)
        assertEquals(120_000.0, tanesco.totalPaidOut, 0.01)
        assertEquals(85_000.0, tanesco.unpaidBills, 0.01)
    }
}
