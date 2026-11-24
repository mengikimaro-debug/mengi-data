package tz.mengidata.app.data.export

import tz.mengidata.app.data.accounting.AutoAccountingEngine
import tz.mengidata.app.data.accounting.AutoBucket
import tz.mengidata.app.data.accounting.CategoryAnalyticsEngine
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.BusinessSettingsEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.SavingsLogEntity
import tz.mengidata.app.data.db.TransactionCategoryEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter
import tz.mengidata.app.i18n.tr
import java.util.Locale

/**
 * Generates:
 * 1. Color-coded multi-sheet Excel SpreadsheetML (.xls) with REAL colored rows/cells
 *    and strictly separated tabs: Muhtasari_Auto, Miamala_Transactions, Bili_Bills, Akiba_Savings.
 * 2. Strictly separated CSV files (Transactions-only, Bills-only, Savings-only, and Master Separated CSV)
 *    with explicit color indicator codes, auto-classification buckets, high-bill-today alerts, and running balances.
 */
object ColorReportExporter {

    private const val UTF8_BOM = "\uFEFF"

    // --- 1. Color-Coded Excel Spreadsheet (.xls SpreadsheetML XML) ---

    fun buildColorExcelXml(
        settings: BusinessSettingsEntity,
        transactions: List<TransactionEntity>,
        bills: List<BillEntity>,
        invoices: List<InvoiceWithItems>,
        savingsGoals: List<SavingsGoalEntity>,
        savingsLogs: List<SavingsLogEntity>,
        language: AppLanguage
    ): String {
        val currency = settings.currency.ifBlank { "TZS" }
        val today = FormatUtils.todayIso()
        val summary = AutoAccountingEngine.computeZeroLossSummary(
            transactions = transactions,
            bills = bills,
            invoices = invoices,
            savingsGoals = savingsGoals,
            savingsLogs = savingsLogs,
            currency = currency
        )

        // Strictly separate direct transactions (excluding bills & savings)
        val pureTransactions = transactions
            .filter { it.source != "bill" && it.source != "savings" }
            .sortedBy { it.date }

        val goalsMap = savingsGoals.associateBy { it.id }

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append("""<?mso-application progid="Excel.Sheet"?>""").append('\n')
        sb.append(
            """<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">"""
        ).append('\n')

        // Styles with REAL Excel/Google Sheets colors
        sb.append(
            """
 <Styles>
  <Style ss:ID="Title">
   <Font ss:Bold="1" ss:Size="14" ss:Color="#FFFFFF"/>
   <Interior ss:Color="#0F172A" ss:Pattern="Solid"/>
   <Alignment ss:Vertical="Center"/>
  </Style>
  <Style ss:ID="Header">
   <Font ss:Bold="1" ss:Size="11" ss:Color="#FFFFFF"/>
   <Interior ss:Color="#1D4ED8" ss:Pattern="Solid"/>
   <Alignment ss:Vertical="Center"/>
  </Style>
  <Style ss:ID="MoneyIn">
   <Font ss:Bold="1" ss:Color="#065F46"/>
   <Interior ss:Color="#D1FAE5" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="MoneyInNum">
   <Font ss:Bold="1" ss:Color="#065F46"/>
   <Interior ss:Color="#D1FAE5" ss:Pattern="Solid"/>
   <NumberFormat ss:Format="#,##0.00"/>
  </Style>
  <Style ss:ID="HighBillRed">
   <Font ss:Bold="1" ss:Color="#4C3E6B"/>
   <Interior ss:Color="#F4F1F9" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="HighBillRedNum">
   <Font ss:Bold="1" ss:Color="#4C3E6B"/>
   <Interior ss:Color="#F4F1F9" ss:Pattern="Solid"/>
   <NumberFormat ss:Format="#,##0.00"/>
  </Style>
  <Style ss:ID="BillAmber">
   <Font ss:Color="#92400E"/>
   <Interior ss:Color="#FEF3C7" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="BillAmberNum">
   <Font ss:Color="#92400E"/>
   <Interior ss:Color="#FEF3C7" ss:Pattern="Solid"/>
   <NumberFormat ss:Format="#,##0.00"/>
  </Style>
  <Style ss:ID="ExpenseOrange">
   <Font ss:Color="#9A3412"/>
   <Interior ss:Color="#FFEDD5" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="ExpenseOrangeNum">
   <Font ss:Color="#9A3412"/>
   <Interior ss:Color="#FFEDD5" ss:Pattern="Solid"/>
   <NumberFormat ss:Format="#,##0.00"/>
  </Style>
  <Style ss:ID="SavingsBlue">
   <Font ss:Bold="1" ss:Color="#1E40AF"/>
   <Interior ss:Color="#DBEAFE" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="SavingsBlueNum">
   <Font ss:Bold="1" ss:Color="#1E40AF"/>
   <Interior ss:Color="#DBEAFE" ss:Pattern="Solid"/>
   <NumberFormat ss:Format="#,##0.00"/>
  </Style>
  <Style ss:ID="TotalRow">
   <Font ss:Bold="1" ss:Size="11" ss:Color="#0D2818"/>
   <Interior ss:Color="#ECFDF5" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="TotalRowNum">
   <Font ss:Bold="1" ss:Size="11" ss:Color="#0D2818"/>
   <Interior ss:Color="#ECFDF5" ss:Pattern="Solid"/>
   <NumberFormat ss:Format="#,##0.00"/>
  </Style>
 </Styles>
            """.trimIndent()
        ).append('\n')

        // ==================== SHEET 1: AUTO SUMMARY & COLOR LEGEND ====================
        sb.append(""" <Worksheet ss:Name="Muhtasari_Auto">""").append('\n')
        sb.append("  <Table>\n")
        xmlRow(sb, listOf(
            XmlCell("${settings.businessName.ifBlank { "Mengidata" }} — ${tr(language, "Ripoti ya Kiotomatiki (0% Upotevu)", "Zero-Loss Auto Financial Report")}", "Title"),
            XmlCell("${tr(language, "Tarehe", "Date")}: $today", "Title"),
            XmlCell("${tr(language, "Sarafu", "Currency")}: $currency", "Title")
        ))
        xmlRow(sb, emptyList())

        // Color Legend
        xmlRow(sb, listOf(
            XmlCell(tr(language, "MWONGOZO WA RANGI (COLOR LEGEND)", "COLOR LEGEND"), "Header"),
            XmlCell(tr(language, "MAANA YAKE", "MEANING"), "Header"),
            XmlCell(tr(language, "MSIMBO WA RANGI", "COLOR HEX"), "Header")
        ))
        xmlRow(sb, listOf(
            XmlCell(tr(language, "KIJANI (Green)", "GREEN"), "MoneyIn"),
            XmlCell(tr(language, "Pesa Kuingia / Mapato / Malipo ya Ankara", "Money In / Income / Invoice Payments"), "MoneyIn"),
            XmlCell("#D1FAE5 / #10B981", "MoneyIn")
        ))
        xmlRow(sb, listOf(
            XmlCell(tr(language, "NYEKUNDU (Red)", "RED"), "HighBillRed"),
            XmlCell(tr(language, "TAHADHARI: Bili Imekuwa Kubwa Sana Leo / Imechelewa", "ALERT: Today's Bill Is Very High / Overdue"), "HighBillRed"),
            XmlCell("#FEE2E2 / #DC2626", "HighBillRed")
        ))
        xmlRow(sb, listOf(
            XmlCell(tr(language, "MANJANO (Amber)", "AMBER"), "BillAmber"),
            XmlCell(tr(language, "Bili ya Kawaida (Imetenganishwa kwenye Tab ya Bili)", "Regular Bill (Separated in Bills Tab)"), "BillAmber"),
            XmlCell("#FEF3C7 / #D97706", "BillAmber")
        ))
        xmlRow(sb, listOf(
            XmlCell(tr(language, "MACHUNGWA (Orange)", "ORANGE"), "ExpenseOrange"),
            XmlCell(tr(language, "Matumizi ya Kawaida (Pesa Kutoka)", "Direct Expenses (Money Out)"), "ExpenseOrange"),
            XmlCell("#FFEDD5 / #EA580C", "ExpenseOrange")
        ))
        xmlRow(sb, listOf(
            XmlCell(tr(language, "BLUU (Blue)", "BLUE"), "SavingsBlue"),
            XmlCell(tr(language, "Akiba (Malengo na Mihamala ya Akiba)", "Savings (Goals & Savings Logs)"), "SavingsBlue"),
            XmlCell("#DBEAFE / #2563EB", "SavingsBlue")
        ))
        xmlRow(sb, emptyList())

        // High Bill Today Alert Banner Row
        if (summary.isTodayBillHigh) {
            xmlRow(sb, listOf(
                XmlCell(tr(language, "TAHADHARI YA LEO", "TODAY'S ALERT"), "HighBillRed"),
                XmlCell(tr(language, summary.highBillAlertSw, summary.highBillAlertEn), "HighBillRed"),
                XmlCell(summary.todayBillsTotal, "HighBillRedNum")
            ))
            xmlRow(sb, emptyList())
        }

        // Zero-Loss Balance Reconciliation Table
        xmlRow(sb, listOf(
            XmlCell(tr(language, "HESABU YA KIOTOMATIKI (ZERO-LOSS)", "AUTO LEDGER BREAKDOWN"), "Header"),
            XmlCell(tr(language, "AINA", "BUCKET"), "Header"),
            XmlCell("${tr(language, "KIASI", "AMOUNT")} ($currency)", "Header")
        ))
        xmlRow(sb, listOf(
            XmlCell(tr(language, "Jumla ya Pesa Kuingia (Mapato + Ankara)", "Total Money In (Income + Invoices)"), "MoneyIn"),
            XmlCell("MONEY_IN", "MoneyIn"),
            XmlCell(summary.totalMoneyIn, "MoneyInNum")
        ))
        xmlRow(sb, listOf(
            XmlCell(tr(language, "Matumizi ya Moja kwa Moja (Bila Bili/Akiba)", "Direct Expenses (Excl. Bills/Savings)"), "ExpenseOrange"),
            XmlCell("EXPENSE", "ExpenseOrange"),
            XmlCell(summary.totalDirectExpenses, "ExpenseOrangeNum")
        ))
        xmlRow(sb, listOf(
            XmlCell(tr(language, "Bili Zilizolipwa (Tab ya Bili)", "Paid Bills (Bills Tab)"), if (summary.isTodayBillHigh) "HighBillRed" else "BillAmber"),
            XmlCell("BILL_PAID", if (summary.isTodayBillHigh) "HighBillRed" else "BillAmber"),
            XmlCell(summary.totalBillsPaid, if (summary.isTodayBillHigh) "HighBillRedNum" else "BillAmberNum")
        ))
        xmlRow(sb, listOf(
            XmlCell(tr(language, "Bili Zinazodaiwa (Bado Kulipwa)", "Unpaid Bills Pending"), if (summary.totalBillsUnpaid > 0) "HighBillRed" else "BillAmber"),
            XmlCell("BILL_UNPAID", if (summary.totalBillsUnpaid > 0) "HighBillRed" else "BillAmber"),
            XmlCell(summary.totalBillsUnpaid, if (summary.totalBillsUnpaid > 0) "HighBillRedNum" else "BillAmberNum")
        ))
        xmlRow(sb, listOf(
            XmlCell(tr(language, "Jumla ya Akiba Iliyopo (Tab ya Akiba)", "Current Savings Balance (Savings Tab)"), "SavingsBlue"),
            XmlCell("SAVINGS", "SavingsBlue"),
            XmlCell(summary.netSavingsBalance, "SavingsBlueNum")
        ))
        xmlRow(sb, listOf(
            XmlCell(tr(language, "SALIO HALISI LA FEDHA (CASH BALANCE)", "VERIFIED CASH BALANCE"), "TotalRow"),
            XmlCell(tr(language, "0% UPOTEVU", "0% LOSS VERIFIED"), "TotalRow"),
            XmlCell(summary.verifiedCashBalance, "TotalRowNum")
        ))
        xmlRow(sb, listOf(
            XmlCell(tr(language, "JUMLA YA MALI YOTE (SALIO + AKIBA)", "TOTAL NET WORTH (CASH + SAVINGS)"), "TotalRow"),
            XmlCell("${summary.totalRecordsCount} ${tr(language, "rekodi", "records")}", "TotalRow"),
            XmlCell(summary.totalNetWorthWithSavings, "TotalRowNum")
        ))
        sb.append("  </Table>\n </Worksheet>\n")

        // ==================== SHEET 2: TRANSACTIONS ONLY (SEPARATED FROM BILLS) ====================
        sb.append(""" <Worksheet ss:Name="Miamala_Transactions">""").append('\n')
        sb.append("  <Table>\n")
        xmlRow(sb, listOf(
            XmlCell(tr(language, "Tarehe", "Date"), "Header"),
            XmlCell(tr(language, "Uchambuzi Auto", "Auto Class"), "Header"),
            XmlCell(tr(language, "Maelezo", "Description"), "Header"),
            XmlCell(tr(language, "Mhusika", "Party"), "Header"),
            XmlCell(tr(language, "Kundi", "Category"), "Header"),
            XmlCell(tr(language, "Pesa Kuingia (+)", "Money In (+)"), "Header"),
            XmlCell(tr(language, "Pesa Kutoka (-)", "Money Out (-)"), "Header"),
            XmlCell(tr(language, "Salio Linaloendelea", "Running Balance"), "Header"),
            XmlCell(tr(language, "Njia", "Method"), "Header"),
            XmlCell(tr(language, "Maana ya Rangi", "Color Indicator"), "Header")
        ))

        var runningBalance = 0.0
        pureTransactions.forEach { t ->
            val isIn = t.isIncome
            val inAmt = if (isIn) t.amount else 0.0
            val outAmt = if (!isIn) t.amount else 0.0
            runningBalance += (inAmt - outAmt)
            val bucket = if (isIn) AutoBucket.MONEY_IN else AutoBucket.EXPENSE
            val textStyle = if (isIn) "MoneyIn" else "ExpenseOrange"
            val numStyle = if (isIn) "MoneyInNum" else "ExpenseOrangeNum"
            xmlRow(sb, listOf(
                XmlCell(t.date, textStyle),
                XmlCell(bucket.label(language), textStyle),
                XmlCell(t.description, textStyle),
                XmlCell(t.party, textStyle),
                XmlCell(t.category, textStyle),
                XmlCell(inAmt, numStyle),
                XmlCell(outAmt, numStyle),
                XmlCell(runningBalance, numStyle),
                XmlCell(t.method, textStyle),
                XmlCell(bucket.colorMeaning(language), textStyle)
            ))
        }
        xmlRow(sb, listOf(
            XmlCell(tr(language, "JUMLA MIAMALA", "TRANSACTIONS TOTAL"), "TotalRow"),
            XmlCell("${pureTransactions.size}", "TotalRow"),
            XmlCell("", "TotalRow"),
            XmlCell("", "TotalRow"),
            XmlCell("", "TotalRow"),
            XmlCell(pureTransactions.filter { it.isIncome }.sumOf { it.amount }, "TotalRowNum"),
            XmlCell(pureTransactions.filter { !it.isIncome }.sumOf { it.amount }, "TotalRowNum"),
            XmlCell(runningBalance, "TotalRowNum"),
            XmlCell("", "TotalRow"),
            XmlCell(tr(language, "Miamala imetenganishwa na Bili", "Separated from Bills"), "TotalRow")
        ))
        sb.append("  </Table>\n </Worksheet>\n")

        // ==================== SHEET 3: BILLS ONLY (SEPARATED FROM TRANSACTIONS) ====================
        sb.append(""" <Worksheet ss:Name="Bili_Bills">""").append('\n')
        sb.append("  <Table>\n")
        xmlRow(sb, listOf(
            XmlCell(tr(language, "Jina la Bili", "Bill Name"), "Header"),
            XmlCell(tr(language, "Kundi", "Category"), "Header"),
            XmlCell(tr(language, "Mlipwaji", "Payee"), "Header"),
            XmlCell(tr(language, "Tarehe ya Mwisho", "Due Date"), "Header"),
            XmlCell(tr(language, "Tarehe ya Kulipwa", "Paid Date"), "Header"),
            XmlCell(tr(language, "Hali", "Status"), "Header"),
            XmlCell(tr(language, "Kiasi", "Amount"), "Header"),
            XmlCell(tr(language, "Njia", "Method"), "Header"),
            XmlCell(tr(language, "Tahadhari & Rangi", "Alert & Color"), "Header")
        ))

        bills.sortedBy { it.dueDate }.forEach { b ->
            val isHighToday = b.id in summary.highBillIds ||
                ((b.dueDate == today || b.paidDate == today) && summary.isTodayBillHigh)
            val isOverdue = b.isOverdue
            val (textStyle, numStyle, alertLabel) = when {
                isHighToday -> Triple(
                    "HighBillRed",
                    "HighBillRedNum",
                    AutoBucket.HIGH_BILL_TODAY.colorMeaning(language)
                )
                isOverdue -> Triple(
                    "HighBillRed",
                    "HighBillRedNum",
                    tr(language, "NYEKUNDU - Bili Imechelewa!", "RED - Overdue Bill!")
                )
                else -> Triple(
                    "BillAmber",
                    "BillAmberNum",
                    AutoBucket.BILL.colorMeaning(language)
                )
            }
            val statusText = when {
                b.paid -> tr(language, "Imelipwa", "Paid")
                b.isOverdue -> tr(language, "Imechelewa", "Overdue")
                else -> tr(language, "Inadaiwa", "Unpaid")
            }
            xmlRow(sb, listOf(
                XmlCell(b.name, textStyle),
                XmlCell(b.category, textStyle),
                XmlCell(b.payee, textStyle),
                XmlCell(b.dueDate, textStyle),
                XmlCell(b.paidDate.ifBlank { "-" }, textStyle),
                XmlCell(statusText, textStyle),
                XmlCell(b.amount, numStyle),
                XmlCell(b.method, textStyle),
                XmlCell(alertLabel, textStyle)
            ))
        }
        xmlRow(sb, listOf(
            XmlCell(tr(language, "JUMLA BILI ZOTE", "ALL BILLS TOTAL"), "TotalRow"),
            XmlCell("${bills.size}", "TotalRow"),
            XmlCell("", "TotalRow"),
            XmlCell("", "TotalRow"),
            XmlCell("", "TotalRow"),
            XmlCell(tr(language, "Zilizolipwa / Zinazodaiwa", "Paid / Unpaid"), "TotalRow"),
            XmlCell(bills.sumOf { it.amount }, "TotalRowNum"),
            XmlCell("", "TotalRow"),
            XmlCell(
                if (summary.isTodayBillHigh) {
                    tr(language, "NYEKUNDU: Bili za leo kubwa (${FormatUtils.formatMoney(summary.todayBillsTotal, currency)})", "RED: High bills today")
                } else {
                    tr(language, "Bili za leo zipo sawa", "Today's bills normal")
                },
                if (summary.isTodayBillHigh) "HighBillRed" else "TotalRow"
            )
        ))
        sb.append("  </Table>\n </Worksheet>\n")

        // ==================== SHEET 4: SAVINGS ONLY (AKIBA) ====================
        sb.append(""" <Worksheet ss:Name="Akiba_Savings">""").append('\n')
        sb.append("  <Table>\n")
        xmlRow(sb, listOf(
            XmlCell(tr(language, "Tarehe", "Date"), "Header"),
            XmlCell(tr(language, "Lengo la Akiba", "Savings Goal"), "Header"),
            XmlCell(tr(language, "Aina", "Action"), "Header"),
            XmlCell(tr(language, "Kiasi", "Amount"), "Header"),
            XmlCell(tr(language, "Maelezo", "Note"), "Header"),
            XmlCell(tr(language, "Rangi", "Color Indicator"), "Header")
        ))
        savingsLogs.sortedBy { it.date }.forEach { log ->
            val goalName = goalsMap[log.savingsId]?.title ?: tr(language, "Akiba", "Savings")
            val isDeposit = log.type == "deposit"
            val actionLabel = if (isDeposit) tr(language, "Weka Akiba (+)", "Deposit (+)") else tr(language, "Toa Akiba (-)", "Withdraw (-)")
            xmlRow(sb, listOf(
                XmlCell(log.date, "SavingsBlue"),
                XmlCell(goalName, "SavingsBlue"),
                XmlCell(actionLabel, "SavingsBlue"),
                XmlCell(log.amount, "SavingsBlueNum"),
                XmlCell(log.note, "SavingsBlue"),
                XmlCell(AutoBucket.SAVINGS.colorMeaning(language), "SavingsBlue")
            ))
        }
        xmlRow(sb, listOf(
            XmlCell(tr(language, "JUMLA YA AKIBA ILIYOPO", "TOTAL SAVINGS BALANCE"), "TotalRow"),
            XmlCell("${savingsGoals.size} ${tr(language, "malengo", "goals")}", "TotalRow"),
            XmlCell("", "TotalRow"),
            XmlCell(summary.netSavingsBalance, "TotalRowNum"),
            XmlCell("", "TotalRow"),
            XmlCell(AutoBucket.SAVINGS.colorMeaning(language), "SavingsBlue")
        ))
        sb.append("  </Table>\n </Worksheet>\n")

        sb.append("</Workbook>\n")
        return sb.toString()
    }

    // --- 2. Separated Transactions-Only CSV ---

    fun buildTransactionsOnlyCsv(
        transactions: List<TransactionEntity>,
        language: AppLanguage
    ): String {
        val pureTransactions = transactions
            .filter { it.source != "bill" && it.source != "savings" }
            .sortedBy { it.date }
        val sb = StringBuilder(UTF8_BOM)
        sb.append("Date,Auto_Bucket,Direction,Description,Party,Category,Money_In,Money_Out,Running_Balance,Method,Reference,Color_Hex,Color_Meaning\n")
        var running = 0.0
        pureTransactions.forEach { t ->
            val isIn = t.isIncome
            val inAmt = if (isIn) t.amount else 0.0
            val outAmt = if (!isIn) t.amount else 0.0
            running += (inAmt - outAmt)
            val bucket = if (isIn) AutoBucket.MONEY_IN else AutoBucket.EXPENSE
            sb.append(csv(t.date)).append(',')
                .append(csv(bucket.label(language))).append(',')
                .append(csv(if (isIn) "PESA_KUINGIA" else "PESA_KUTOKA")).append(',')
                .append(csv(t.description)).append(',')
                .append(csv(t.party)).append(',')
                .append(csv(t.category)).append(',')
                .append(num(inAmt)).append(',')
                .append(num(outAmt)).append(',')
                .append(num(running)).append(',')
                .append(csv(t.method)).append(',')
                .append(csv(t.reference)).append(',')
                .append(csv(bucket.colorHex)).append(',')
                .append(csv(bucket.colorMeaning(language))).append('\n')
        }
        return sb.toString()
    }

    // --- 3. Separated Bills-Only CSV ---

    fun buildBillsOnlyCsv(
        bills: List<BillEntity>,
        transactions: List<TransactionEntity>,
        invoices: List<InvoiceWithItems>,
        savingsGoals: List<SavingsGoalEntity>,
        savingsLogs: List<SavingsLogEntity>,
        currency: String,
        language: AppLanguage
    ): String {
        val today = FormatUtils.todayIso()
        val summary = AutoAccountingEngine.computeZeroLossSummary(
            transactions, bills, invoices, savingsGoals, savingsLogs, currency
        )
        val sb = StringBuilder(UTF8_BOM)
        sb.append("Bill_Name,Category,Payee,Due_Date,Paid_Date,Status,Amount,Method,Recurring,High_Bill_Alert,Color_Hex,Color_Meaning\n")
        bills.sortedBy { it.dueDate }.forEach { b ->
            val isHighToday = b.id in summary.highBillIds ||
                ((b.dueDate == today || b.paidDate == today) && summary.isTodayBillHigh)
            val bucket = if (isHighToday || b.isOverdue) AutoBucket.HIGH_BILL_TODAY else AutoBucket.BILL
            val alertText = when {
                isHighToday -> tr(language, "TAHADHARI: BILI KUBWA SANA LEO", "ALERT: VERY HIGH BILL TODAY")
                b.isOverdue -> tr(language, "BILI IMECHELEWA", "OVERDUE BILL")
                else -> tr(language, "KAWAIDA", "NORMAL")
            }
            val status = when {
                b.paid -> tr(language, "Imelipwa", "Paid")
                b.isOverdue -> tr(language, "Imechelewa", "Overdue")
                else -> tr(language, "Inadaiwa", "Unpaid")
            }
            sb.append(csv(b.name)).append(',')
                .append(csv(b.category)).append(',')
                .append(csv(b.payee)).append(',')
                .append(csv(b.dueDate)).append(',')
                .append(csv(b.paidDate)).append(',')
                .append(csv(status)).append(',')
                .append(num(b.amount)).append(',')
                .append(csv(b.method)).append(',')
                .append(csv(if (b.recurring) "YES" else "NO")).append(',')
                .append(csv(alertText)).append(',')
                .append(csv(bucket.colorHex)).append(',')
                .append(csv(bucket.colorMeaning(language))).append('\n')
        }
        return sb.toString()
    }

    // --- 4. Separated Savings-Only CSV (Akiba) ---

    fun buildSavingsOnlyCsv(
        savingsGoals: List<SavingsGoalEntity>,
        savingsLogs: List<SavingsLogEntity>,
        language: AppLanguage
    ): String {
        val goalsMap = savingsGoals.associateBy { it.id }
        val sb = StringBuilder(UTF8_BOM)
        sb.append("Date,Savings_Goal,Action,Amount,Goal_Current_Balance,Goal_Target,Note,Color_Hex,Color_Meaning\n")
        savingsLogs.sortedBy { it.date }.forEach { log ->
            val goal = goalsMap[log.savingsId]
            sb.append(csv(log.date)).append(',')
                .append(csv(goal?.title ?: "Akiba")).append(',')
                .append(csv(if (log.type == "deposit") "WEKA_AKIBA" else "TOA_AKIBA")).append(',')
                .append(num(log.amount)).append(',')
                .append(num(goal?.currentAmount ?: 0.0)).append(',')
                .append(num(goal?.targetAmount ?: 0.0)).append(',')
                .append(csv(log.note)).append(',')
                .append(csv(AutoBucket.SAVINGS.colorHex)).append(',')
                .append(csv(AutoBucket.SAVINGS.colorMeaning(language))).append('\n')
        }
        return sb.toString()
    }

    // --- 5. Master Separated CSV (All 4 sections clearly separated + color legend) ---

    fun buildMasterSeparatedCsv(
        settings: BusinessSettingsEntity,
        transactions: List<TransactionEntity>,
        bills: List<BillEntity>,
        invoices: List<InvoiceWithItems>,
        savingsGoals: List<SavingsGoalEntity>,
        savingsLogs: List<SavingsLogEntity>,
        language: AppLanguage
    ): String {
        val currency = settings.currency.ifBlank { "TZS" }
        val summary = AutoAccountingEngine.computeZeroLossSummary(
            transactions, bills, invoices, savingsGoals, savingsLogs, currency
        )
        val sb = StringBuilder(UTF8_BOM)
        sb.append("=== SECTION 1: MUHTASARI WA AUTO & MWONGOZO WA RANGI (ZERO-LOSS) ===\n")
        sb.append("Metric,Amount_Currency,Color_Hex,Color_Meaning\n")
        sb.append(csv("Pesa Kuingia (Money In)")).append(',').append(num(summary.totalMoneyIn)).append(',').append(csv(AutoBucket.MONEY_IN.colorHex)).append(',').append(csv(AutoBucket.MONEY_IN.colorMeaning(language))).append('\n')
        sb.append(csv("Matumizi Moja kwa Moja (Direct Expenses)")).append(',').append(num(summary.totalDirectExpenses)).append(',').append(csv(AutoBucket.EXPENSE.colorHex)).append(',').append(csv(AutoBucket.EXPENSE.colorMeaning(language))).append('\n')
        sb.append(csv("Bili Zilizolipwa (Paid Bills)")).append(',').append(num(summary.totalBillsPaid)).append(',').append(csv(if (summary.isTodayBillHigh) AutoBucket.HIGH_BILL_TODAY.colorHex else AutoBucket.BILL.colorHex)).append(',').append(csv(if (summary.isTodayBillHigh) AutoBucket.HIGH_BILL_TODAY.colorMeaning(language) else AutoBucket.BILL.colorMeaning(language))).append('\n')
        sb.append(csv("Bili za Leo (Today Bills)")).append(',').append(num(summary.todayBillsTotal)).append(',').append(csv(if (summary.isTodayBillHigh) AutoBucket.HIGH_BILL_TODAY.colorHex else AutoBucket.BILL.colorHex)).append(',').append(csv(if (summary.isTodayBillHigh) summary.highBillAlertSw else "KAWAIDA")).append('\n')
        sb.append(csv("Akiba Iliyopo (Savings Balance)")).append(',').append(num(summary.netSavingsBalance)).append(',').append(csv(AutoBucket.SAVINGS.colorHex)).append(',').append(csv(AutoBucket.SAVINGS.colorMeaning(language))).append('\n')
        sb.append(csv("SALIO HALISI (Verified Cash Balance)")).append(',').append(num(summary.verifiedCashBalance)).append(',').append(csv("#0D2818")).append(',').append(csv("0% UPOTEVU")).append('\n')
        sb.append('\n')

        sb.append("=== SECTION 2: MIAMALA PEKEE (TRANSACTIONS ONLY - SEPARATED FROM BILLS) ===\n")
        sb.append(buildTransactionsOnlyCsv(transactions, language).removePrefix(UTF8_BOM))
        sb.append('\n')

        sb.append("=== SECTION 3: BILI PEKEE (BILLS ONLY - SEPARATED FROM TRANSACTIONS) ===\n")
        sb.append(buildBillsOnlyCsv(bills, transactions, invoices, savingsGoals, savingsLogs, currency, language).removePrefix(UTF8_BOM))
        sb.append('\n')

        sb.append("=== SECTION 4: AKIBA PEKEE (SAVINGS ONLY) ===\n")
        sb.append(buildSavingsOnlyCsv(savingsGoals, savingsLogs, language).removePrefix(UTF8_BOM))
        return sb.toString()
    }

    /**
     * Grouped Category Analytical Report CSV (Income by Category & Expenses by Category with share % and budget status).
     */
    fun buildCategoryAnalyticsCsv(
        transactions: List<TransactionEntity>,
        categories: List<TransactionCategoryEntity>,
        period: PeriodFilter,
        currency: String,
        language: AppLanguage
    ): String {
        val report = CategoryAnalyticsEngine.analyze(transactions, categories, period)
        val sb = StringBuilder(UTF8_BOM)
        val periodLabel = if (language == AppLanguage.EN) period.labelEn else period.labelSw

        sb.append(csv(tr(language, "RIPOTI YA UCHAMBUZI WA MAKUNDI (CATEGORY ANALYTICS)", "CATEGORY ANALYTICAL REPORT"))).append(',')
        sb.append(csv(periodLabel)).append(',')
        sb.append(csv(currency)).append('\n')
        sb.append(
            listOf(
                tr(language, "Aina", "Type"),
                tr(language, "Kundi", "Category"),
                tr(language, "Idadi ya Miamala", "Transactions"),
                tr(language, "Jumla", "Total Amount"),
                tr(language, "Asilimia (%)", "Share (%)"),
                tr(language, "Bajeti ya Mwezi", "Monthly Budget"),
                tr(language, "Hali ya Bajeti", "Budget Status")
            ).joinToString(",") { csv(it) }
        ).append('\n')

        report.incomeBreakdown.forEach { item ->
            sb.append(
                listOf(
                    csv(tr(language, "Mapato", "Income")),
                    csv(item.categoryName),
                    item.transactionCount.toString(),
                    num(item.totalAmount),
                    String.format(Locale.US, "%.1f%%", item.sharePercent),
                    num(item.monthlyBudget),
                    csv(if (item.monthlyBudget > 0.0) String.format(Locale.US, "%.0f%%", item.budgetRatio * 100f) else "-")
                ).joinToString(",")
            ).append('\n')
        }

        report.expenseBreakdown.forEach { item ->
            val budgetStatus = when {
                item.monthlyBudget <= 0.0 -> "-"
                item.isOverBudget -> tr(language, "IMEZIDI BAJETI", "OVER BUDGET")
                else -> String.format(Locale.US, "%.0f%%", item.budgetRatio * 100f)
            }
            sb.append(
                listOf(
                    csv(tr(language, "Matumizi", "Expense")),
                    csv(item.categoryName),
                    item.transactionCount.toString(),
                    num(item.totalAmount),
                    String.format(Locale.US, "%.1f%%", item.sharePercent),
                    num(item.monthlyBudget),
                    csv(budgetStatus)
                ).joinToString(",")
            ).append('\n')
        }

        sb.append('\n')
        sb.append(listOf(csv(tr(language, "JUMLA YA MAPATO", "TOTAL INCOME")), csv(""), "", num(report.totalIncome), csv("100.0%"), "", "").joinToString(",")).append('\n')
        sb.append(listOf(csv(tr(language, "JUMLA YA MATUMIZI", "TOTAL EXPENSES")), csv(""), "", num(report.totalExpense), csv("100.0%"), "", "").joinToString(",")).append('\n')
        sb.append(listOf(csv(tr(language, "SALIO HALISI", "NET RESULT")), csv(""), "", num(report.netResult), csv(""), "", "").joinToString(",")).append('\n')
        return sb.toString()
    }

    // --- Helpers ---

    private data class XmlCell(
        val value: Any,
        val styleId: String
    )

    private fun xmlRow(sb: StringBuilder, cells: List<XmlCell>) {
        sb.append("   <Row>\n")
        cells.forEach { cell ->
            if (cell.value is Number) {
                val formatted = String.format(Locale.US, "%.2f", cell.value.toDouble())
                sb.append("""    <Cell ss:StyleID="${cell.styleId}"><Data ss:Type="Number">$formatted</Data></Cell>""").append('\n')
            } else {
                sb.append("""    <Cell ss:StyleID="${cell.styleId}"><Data ss:Type="String">${escapeXml(cell.value.toString())}</Data></Cell>""").append('\n')
            }
        }
        sb.append("   </Row>\n")
    }

    private fun escapeXml(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun csv(raw: String): String = "\"${raw.replace("\"", "\"\"")}\""

    private fun num(value: Double): String = String.format(Locale.US, "%.2f", value)
}
