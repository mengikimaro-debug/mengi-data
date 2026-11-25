package tz.mengidata.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tz.mengidata.app.data.accounting.AutoAccountingEngine
import tz.mengidata.app.data.accounting.AutoBucket
import tz.mengidata.app.data.accounting.BusinessHealthEngine
import tz.mengidata.app.data.accounting.CategoryAnalyticsEngine
import tz.mengidata.app.data.accounting.PartyDirectoryEngine
import tz.mengidata.app.data.accounting.PartyRole
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.InvoiceEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.SavingsLogEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter

class HouseholdSeparationTest {

    @Test
    fun householdBillTextAndHomeKeywordsClassifyAsHouseholdBill() {
        val r1 = AutoAccountingEngine.classifyText("Household bill ya leo")
        assertEquals(AutoBucket.HOUSEHOLD_BILL, r1.bucket)
        assertEquals(AutoAccountingEngine.HOUSEHOLD_CATEGORY, r1.suggestedCategory)

        val r2 = AutoAccountingEngine.classifyText("Nimeacha pesa home")
        assertEquals(AutoBucket.HOUSEHOLD_BILL, r2.bucket)

        val r3 = AutoAccountingEngine.classifyText("Pesa ya nyumbani")
        assertEquals(AutoBucket.HOUSEHOLD_BILL, r3.bucket)
    }

    @Test
    fun noHardcodedExampleNamesAndSystemLearnsRealSavedHouseholdNames() {
        // Hardcoded example names are NOT pre-seeded or special-cased anymore
        assertFalse(AutoAccountingEngine.isHouseholdParty("Ma Areni"))
        assertFalse(AutoAccountingEngine.isHouseholdParty("Z Ma Areni"))
        assertTrue(PartyDirectoryEngine.suggestParties("", emptyList()).isEmpty())

        // Once the user saves a real Household Bill for any recipient, the system learns & recognizes it
        val today = FormatUtils.todayIso()
        val savedTxns = listOf(
            TransactionEntity(
                id = "t_home_1",
                type = "expense",
                date = today,
                description = "Bili ya Nyumbani",
                party = "Mama Juma",
                category = AutoAccountingEngine.HOUSEHOLD_CATEGORY,
                amount = 45_000.0,
                method = "Cash",
                source = AutoAccountingEngine.HOUSEHOLD_SOURCE
            )
        )
        val directory = PartyDirectoryEngine.buildDirectory(
            transactions = savedTxns,
            invoices = emptyList(),
            bills = emptyList()
        )
        val learnedHousehold = PartyDirectoryEngine.householdNames(directory)
        assertTrue(learnedHousehold.contains("Mama Juma"))

        val recognized = AutoAccountingEngine.classifyText(
            description = "Pesa ya leo",
            party = "mama juma",
            learnedHouseholdNames = learnedHousehold
        )
        assertEquals(AutoBucket.HOUSEHOLD_BILL, recognized.bucket)
        assertEquals(AutoAccountingEngine.HOUSEHOLD_CATEGORY, recognized.suggestedCategory)
    }

    @Test
    fun transportFoodInternetClassifyAsPersonalExpenses() {
        val transport = AutoAccountingEngine.classifyText("Transport kwenda kazini")
        assertEquals(AutoBucket.EXPENSE, transport.bucket)
        assertEquals("Usafiri", transport.suggestedCategory)

        val food = AutoAccountingEngine.classifyText("Food / Lunch kazini")
        assertEquals(AutoBucket.EXPENSE, food.bucket)
        assertEquals("Chakula", food.suggestedCategory)

        val internet = AutoAccountingEngine.classifyText("Internet bando la simu")
        assertEquals(AutoBucket.EXPENSE, internet.bucket)
        assertEquals("Internet & simu", internet.suggestedCategory)
    }

    @Test
    fun explicitBillsClassifyAsMyBills() {
        val luku = AutoAccountingEngine.classifyText("LUKU", party = "TANESCO")
        assertEquals(AutoBucket.BILL, luku.bucket)
        assertEquals("Umeme & maji", luku.suggestedCategory)

        val rent = AutoAccountingEngine.classifyText("Kodi ya pango", party = "Mwenye Nyumba")
        assertEquals(AutoBucket.BILL, rent.bucket)
        assertEquals("Kodi ya pango", rent.suggestedCategory)
    }

    @Test
    fun zeroLossLedgerSeparatesHouseholdFromPersonalExpensesWhileDeductingNormally() {
        val today = FormatUtils.todayIso()
        val txns = listOf(
            TransactionEntity("t_in", "income", today, "Mauzo", "Mteja", "Mauzo ya bidhaa", 500_000.0, "Cash", source = "manual"),
            TransactionEntity("t_tr", "expense", today, "Transport", "Binafsi", "Usafiri", 15_000.0, "Cash", source = "manual"),
            TransactionEntity("t_fd", "expense", today, "Food & Internet", "Binafsi", "Chakula", 25_000.0, "Cash", source = "manual"),
            TransactionEntity("t_bill", "expense", today, "Malipo: LUKU", "TANESCO", "Umeme & maji", 50_000.0, "M-Pesa", source = "bill", linkedId = "b1"),
            TransactionEntity("t_home", "expense", today, "Bili ya Nyumbani", "Nyumbani", "Household Bill", 40_000.0, "Cash", source = "household")
        )
        val bills = listOf(
            BillEntity("b1", "LUKU", "Umeme & maji", "TANESCO", 50_000.0, today, paid = true, paidDate = today, linkedTxnId = "t_bill")
        )

        val summary = AutoAccountingEngine.computeZeroLossSummary(
            transactions = txns,
            bills = bills,
            invoices = emptyList(),
            savingsGoals = emptyList(),
            savingsLogs = emptyList()
        )

        assertEquals(40_000.0, summary.totalDirectExpenses, 0.01)
        assertEquals(50_000.0, summary.totalBillsPaid, 0.01)
        assertEquals(40_000.0, summary.totalHouseholdBills, 0.01)
        assertEquals(40_000.0, summary.todayHouseholdBills, 0.01)
        assertEquals(130_000.0, summary.totalMoneyOut, 0.01)
        assertEquals(370_000.0, summary.verifiedCashBalance, 0.01)

        val analytics = CategoryAnalyticsEngine.analyze(txns, CategoryAnalyticsEngine.defaultCategories(), PeriodFilter.ALL)
        assertEquals(130_000.0, analytics.totalExpense, 0.01)
        assertEquals(90_000.0, analytics.totalPersonalExpense, 0.01)
        assertEquals(40_000.0, analytics.totalHouseholdBills, 0.01)
        assertEquals(370_000.0, analytics.netResult, 0.01)
        val householdCat = analytics.expenseBreakdown.firstOrNull { it.categoryName.equals("Household Bill", ignoreCase = true) }
        assertTrue(householdCat != null)
        assertEquals(40_000.0, householdCat!!.totalAmount, 0.01)
        assertEquals("home", householdCat.iconKey)

        val health = BusinessHealthEngine.evaluate(
            transactions = txns,
            invoices = emptyList(),
            bills = bills,
            savingsGoals = emptyList(),
            categoryAnalytics = analytics,
            todayIso = today
        )
        assertEquals(74.0, health.profitMarginPct, 0.1)
    }

    @Test
    fun universalPartyDirectoryRecognizesRealSavedNamesAcrossAllFourModules() {
        val today = FormatUtils.todayIso()
        val txns = listOf(
            TransactionEntity("t1", "income", today, "Mauzo", "Amina Juma", "Mauzo ya bidhaa", 120_000.0, "Cash", source = "manual"),
            TransactionEntity("t2", "expense", today, "Pesa ya nyumbani", "Mama Asha", "Household Bill", 35_000.0, "Cash", source = "household")
        )
        val bills = listOf(
            BillEntity("b1", "LUKU", "Umeme & maji", "TANESCO", 50_000.0, today, paid = false)
        )
        val invoices = listOf(
            InvoiceWithItems(
                invoice = InvoiceEntity(
                    id = "inv1",
                    number = "MD-0001",
                    customerName = "Juma Hamisi",
                    customerPhone = "+255712000111",
                    customerEmail = "juma@example.com",
                    customerAddress = "Kinondoni",
                    issueDate = today,
                    dueDate = today,
                    vatRate = 0.0,
                    discount = 0.0,
                    state = "issued"
                ),
                items = emptyList()
            )
        )
        val savingsGoals = listOf(
            SavingsGoalEntity(
                id = "sg1",
                title = "Mfuko wa Dharura",
                targetAmount = 500_000.0,
                currentAmount = 150_000.0,
                targetDate = today,
                notes = "Neema Paul · Akiba ya mwezi"
            )
        )
        val savingsLogs = listOf(
            SavingsLogEntity(
                id = "sl1",
                savingsId = "sg1",
                type = "deposit",
                amount = 150_000.0,
                date = today,
                note = "Neema Paul · Mchango wa kwanza"
            )
        )

        val directory = PartyDirectoryEngine.buildDirectory(
            transactions = txns,
            invoices = invoices,
            bills = bills,
            savingsGoals = savingsGoals,
            savingsLogs = savingsLogs
        )

        val names = directory.map { it.name }.toSet()
        assertTrue(names.contains("Amina Juma"))
        assertTrue(names.contains("Mama Asha"))
        assertTrue(names.contains("TANESCO"))
        assertTrue(names.contains("Juma Hamisi"))
        assertTrue(names.contains("Neema Paul"))

        val neema = PartyDirectoryEngine.matchParty("neema paul", directory)
        assertTrue(neema != null)
        assertTrue(neema!!.modules.contains("savings"))
        assertEquals(150_000.0, neema.savingsTotal, 0.01)

        val juma = PartyDirectoryEngine.matchParty("Juma Hamisi", directory)
        assertTrue(juma != null)
        assertTrue(juma!!.modules.contains("invoice"))
        assertEquals("+255712000111", juma.phone)

        val homeContact = PartyDirectoryEngine.matchParty("mama asha", directory)
        assertTrue(homeContact != null)
        assertEquals(PartyRole.HOUSEHOLD, homeContact!!.role)

        val tanesco = PartyDirectoryEngine.matchParty("tanesco", directory)
        assertTrue(tanesco != null)
        assertTrue(tanesco!!.modules.contains("bill"))

        val filtered = PartyDirectoryEngine.suggestParties("nee", directory)
        assertEquals(1, filtered.size)
        assertEquals("Neema Paul", filtered.first().name)
    }

    @Test
    fun languagePurityHasNoCrossLanguageLeaksOrExampleNamesInCoreLabels() {
        val bannedEnglishInSw = listOf("household", "home", "cash", "stock", "internet", "bill", "savings", "customer", "vendor", "invoice", "bank", "areni", "mengi")
        val bannedSwahiliInEn = listOf("miamala", "bili", "akiba", "ankara", "matumizi", "mapato", "benki", "usafiri", "chakula", "taslimu", "nyumbani", "kodi", "mshahara", "areni", "mengi")

        AutoBucket.entries.forEach { bucket ->
            val sw = bucket.label(AppLanguage.SW).lowercase()
            val en = bucket.label(AppLanguage.EN).lowercase()
            bannedEnglishInSw.forEach { word ->
                assertFalse("SW AutoBucket '${bucket.name}' leaked '$word': $sw", Regex("\\b$word\\b").containsMatchIn(sw))
            }
            bannedSwahiliInEn.forEach { word ->
                assertFalse("EN AutoBucket '${bucket.name}' leaked '$word': $en", Regex("\\b$word\\b").containsMatchIn(en))
            }
        }

        PartyRole.entries.forEach { role ->
            val sw = role.label(AppLanguage.SW).lowercase()
            val en = role.label(AppLanguage.EN).lowercase()
            bannedEnglishInSw.forEach { word ->
                assertFalse("SW PartyRole '${role.name}' leaked '$word': $sw", Regex("\\b$word\\b").containsMatchIn(sw))
            }
            bannedSwahiliInEn.forEach { word ->
                assertFalse("EN PartyRole '${role.name}' leaked '$word': $en", Regex("\\b$word\\b").containsMatchIn(en))
            }
        }

        val sampleCategories = listOf(
            "Mauzo ya bidhaa", "Huduma", "Malipo ya ankara", "Kamisheni",
            "Household Bill", "Chakula", "Usafiri", "Internet & simu",
            "Kodi ya pango", "Umeme & maji", "Manunuzi ya stock", "Mishahara",
            "Bili", "Akiba", "Mengineyo"
        )
        sampleCategories.forEach { raw ->
            val sw = FormatUtils.localizedCategory(raw, AppLanguage.SW).lowercase()
            val en = FormatUtils.localizedCategory(raw, AppLanguage.EN).lowercase()
            bannedEnglishInSw.forEach { word ->
                assertFalse("SW category for '$raw' leaked '$word': $sw", Regex("\\b$word\\b").containsMatchIn(sw))
            }
            bannedSwahiliInEn.forEach { word ->
                assertFalse("EN category for '$raw' leaked '$word': $en", Regex("\\b$word\\b").containsMatchIn(en))
            }
        }

        val sampleMethods = listOf("Cash", "Taslimu", "M-Pesa", "Tigo Pesa", "Airtel Money", "HaloPesa", "Benki", "Bank Transfer")
        sampleMethods.forEach { raw ->
            val sw = FormatUtils.localizedMethod(raw, AppLanguage.SW).lowercase()
            val en = FormatUtils.localizedMethod(raw, AppLanguage.EN).lowercase()
            assertFalse("SW method for '$raw' leaked 'cash': $sw", Regex("\\bcash\\b").containsMatchIn(sw))
            assertFalse("SW method for '$raw' leaked 'bank': $sw", Regex("\\bbank\\b").containsMatchIn(sw))
            assertFalse("EN method for '$raw' leaked 'taslimu': $en", Regex("\\btaslimu\\b").containsMatchIn(en))
            assertFalse("EN method for '$raw' leaked 'benki': $en", Regex("\\bbenki\\b").containsMatchIn(en))
        }
    }

    @Test
    fun savingsIsDeductedNormallyFromSystemWhileClearlySeparated() {
        val today = FormatUtils.todayIso()
        val txns = listOf(
            TransactionEntity("t_in", "income", today, "Mauzo", "Mteja", "Mauzo ya bidhaa", 500_000.0, "Cash", source = "manual"),
            TransactionEntity("t_exp", "expense", today, "Transport", "Binafsi", "Usafiri", 30_000.0, "Cash", source = "manual"),
            TransactionEntity("t_home", "expense", today, "Pesa ya nyumbani", "Nyumbani", "Household Bill", 40_000.0, "Cash", source = "household"),
            TransactionEntity("t_sav", "expense", today, "Akiba ya Dharura", "Akiba", "Akiba", 80_000.0, "M-Pesa", source = "savings")
        )
        val goals = listOf(
            SavingsGoalEntity("sg1", "Akiba ya Dharura", 500_000.0, 80_000.0, today)
        )

        assertTrue(AutoAccountingEngine.isSavingsTransaction(txns[3]))
        assertFalse(AutoAccountingEngine.isSavingsTransaction(txns[1]))
        assertFalse(AutoAccountingEngine.isSavingsTransaction(txns[2]))

        val summary = AutoAccountingEngine.computeZeroLossSummary(
            transactions = txns,
            bills = emptyList(),
            invoices = emptyList(),
            savingsGoals = goals,
            savingsLogs = emptyList()
        )
        assertEquals(30_000.0, summary.totalDirectExpenses, 0.01)
        assertEquals(40_000.0, summary.totalHouseholdBills, 0.01)
        assertEquals(80_000.0, summary.totalSavingsDeposited, 0.01)
        assertEquals(150_000.0, summary.totalMoneyOut, 0.01)
        assertEquals(350_000.0, summary.verifiedCashBalance, 0.01)

        val analytics = CategoryAnalyticsEngine.analyze(txns, CategoryAnalyticsEngine.defaultCategories(), PeriodFilter.ALL)
        assertEquals(150_000.0, analytics.totalExpense, 0.01)
        assertEquals(30_000.0, analytics.totalPersonalExpense, 0.01)
        assertEquals(40_000.0, analytics.totalHouseholdBills, 0.01)
        assertEquals(80_000.0, analytics.totalSavingsOutflow, 0.01)
        assertEquals(350_000.0, analytics.netResult, 0.01)

        val health = BusinessHealthEngine.evaluate(
            transactions = txns,
            invoices = emptyList(),
            bills = emptyList(),
            savingsGoals = goals,
            categoryAnalytics = analytics,
            todayIso = today
        )
        assertEquals(70.0, health.profitMarginPct, 0.1)
    }

    @Test
    fun slashTagAutoRoutingSupportsAnyRandomNameAndAllShortcuts() {
        // 1. Typo-tolerant my/mybilll -> BILL with clean name stripped
        val billTypo = AutoAccountingEngine.classifyText(description = "Juma my/mybilll")
        assertEquals(AutoBucket.BILL, billTypo.bucket)
        assertTrue(billTypo.slashTag.hasSlashTag)
        assertEquals("Juma", billTypo.slashTag.cleanName)

        val billClean = AutoAccountingEngine.classifyText(description = "Asha/mybill")
        assertEquals(AutoBucket.BILL, billClean.bucket)
        assertEquals("Asha", billClean.slashTag.cleanName)

        // Works even if user types "mengi my/mybilll" (as any random name)
        val billRandom = AutoAccountingEngine.classifyText(description = "mengi my/mybilll")
        assertEquals(AutoBucket.BILL, billRandom.bucket)
        assertEquals("mengi", billRandom.slashTag.cleanName)

        // 2. <anyName>/household -> HOUSEHOLD_BILL
        val household = AutoAccountingEngine.classifyText(description = "Neema/household")
        assertEquals(AutoBucket.HOUSEHOLD_BILL, household.bucket)
        assertEquals(AutoAccountingEngine.HOUSEHOLD_CATEGORY, household.suggestedCategory)
        assertEquals("Neema", household.slashTag.cleanName)
        assertTrue(AutoAccountingEngine.isHouseholdParty("Neema/household"))

        // 3. <anyName>/invoice -> MONEY_IN with isInvoiceTagged = true
        val invoice = AutoAccountingEngine.classifyText(description = "Kaka/invoice")
        assertEquals(AutoBucket.MONEY_IN, invoice.bucket)
        assertTrue(invoice.isInvoiceTagged)
        assertEquals("Malipo ya ankara", invoice.suggestedCategory)
        assertEquals("Kaka", invoice.slashTag.cleanName)

        // 4. <anyName>/saving -> SAVINGS (general)
        val saving = AutoAccountingEngine.classifyText(description = "Mama/saving")
        assertEquals(AutoBucket.SAVINGS, saving.bucket)
        assertEquals("Akiba", saving.suggestedCategory)
        assertEquals("Mama", saving.slashTag.cleanName)
        assertEquals(null, saving.suggestedSavingsGoalTitle)

        // 5. <anyName>/rentsaving & <anyName>/carsaving -> SAVINGS with named goal
        val rentSaving = AutoAccountingEngine.classifyText(description = "Juma/rentsaving")
        assertEquals(AutoBucket.SAVINGS, rentSaving.bucket)
        assertEquals("Juma", rentSaving.slashTag.cleanName)
        assertEquals("Rent Savings", rentSaving.suggestedSavingsGoalTitle)

        val carSaving = AutoAccountingEngine.classifyText(description = "Asha/carsaving")
        assertEquals(AutoBucket.SAVINGS, carSaving.bucket)
        assertEquals("Asha", carSaving.slashTag.cleanName)
        assertEquals("Car Savings", carSaving.suggestedSavingsGoalTitle)

        // 6. Regular slashes that are NOT tags must remain untouched
        val foodSlash = AutoAccountingEngine.parseSlashTag("Food / Lunch")
        assertFalse(foodSlash.hasSlashTag)
        assertEquals("Food / Lunch", AutoAccountingEngine.stripSlashTag("Food / Lunch"))

        // 7. stripSlashTag removes recognized tags cleanly
        assertEquals("Juma", AutoAccountingEngine.stripSlashTag("Juma/rentsaving"))
        assertEquals("Neema", AutoAccountingEngine.stripSlashTag("Neema/household"))
        assertEquals("Asha", AutoAccountingEngine.stripSlashTag("Asha my/mybilll"))

        // 8. Bilingual savings goal synonym matching
        assertTrue(AutoAccountingEngine.matchesSavingsGoal("Kodi ya Pango ya Mwaka Ujao", "Rent Savings"))
        assertTrue(AutoAccountingEngine.matchesSavingsGoal("Mfuko wa Dharura", "Emergency Savings"))
        assertTrue(AutoAccountingEngine.matchesSavingsGoal("Akiba ya Gari", "Car Savings"))
        assertFalse(AutoAccountingEngine.matchesSavingsGoal("Mfuko wa Dharura", "Rent Savings"))
    }
}
