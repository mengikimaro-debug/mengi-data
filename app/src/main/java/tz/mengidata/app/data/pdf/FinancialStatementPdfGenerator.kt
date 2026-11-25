package tz.mengidata.app.data.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import tz.mengidata.app.data.accounting.AutoAccountingEngine
import tz.mengidata.app.data.accounting.AutoBucket
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.BusinessSettingsEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter
import tz.mengidata.app.i18n.tr
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Generates a calm matte executive Financial Statement PDF covering
 * Income, Personal Expenses, Household Bills, Savings, Unpaid Obligations,
 * Savings Goals progress, and the Period Ledger.
 */
object FinancialStatementPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2

    private val COLOR_HEADER_SLATE = Color.parseColor("#242E38")
    private val COLOR_ACCENT_SAGE = Color.parseColor("#346352")
    private val COLOR_ACCENT_SAGE_SOFT = Color.parseColor("#E4EFEA")
    private val COLOR_INK_PRIMARY = Color.parseColor("#1D242C")
    private val COLOR_INK_SECONDARY = Color.parseColor("#54606D")
    private val COLOR_INK_MUTED = Color.parseColor("#7D8894")
    private val COLOR_SURFACE_CARD = Color.parseColor("#F5F7F9")
    private val COLOR_SURFACE_ZEBRA = Color.parseColor("#FAFBFD")
    private val COLOR_BORDER = Color.parseColor("#DCE2E8")
    private val COLOR_WHITE = Color.WHITE
    private val COLOR_HEADER_SUB = Color.parseColor("#C6D0DA")

    private val COLOR_INCOME = Color.parseColor("#2E614E")
    private val COLOR_EXPENSE = Color.parseColor("#7E4949")
    private val COLOR_HOUSEHOLD = Color.parseColor("#345863")
    private val COLOR_SAVINGS = Color.parseColor("#3E5070")

    fun generateStatementPdfBytes(
        settings: BusinessSettingsEntity,
        transactions: List<TransactionEntity>,
        bills: List<BillEntity>,
        invoices: List<InvoiceWithItems>,
        savingsGoals: List<SavingsGoalEntity>,
        period: PeriodFilter,
        language: AppLanguage
    ): ByteArray {
        val doc = PdfDocument()
        val currency = settings.currency.ifBlank { "TZS" }
        val today = FormatUtils.todayIso()
        val periodLabel = if (language == AppLanguage.EN) period.labelEn else period.labelSw

        val periodTxns = transactions
            .filter { FormatUtils.isDateInPeriod(it.date, period) }
            .sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.createdAt })

        val periodIncome = periodTxns.filter { it.isIncome }.sumOf { it.amount }
        val periodHousehold = periodTxns.filter { AutoAccountingEngine.isHouseholdTransaction(it) }.sumOf { it.amount }
        val periodSavings = periodTxns.filter { !it.isIncome && AutoAccountingEngine.isSavingsTransaction(it) }.sumOf { it.amount }
        val periodPersonal = periodTxns.filter {
            !it.isIncome &&
                !AutoAccountingEngine.isHouseholdTransaction(it) &&
                !AutoAccountingEngine.isSavingsTransaction(it)
        }.sumOf { it.amount }
        val netBalance = periodIncome - (periodPersonal + periodHousehold + periodSavings)

        val unpaidBills = bills.filter { !it.paid }.sumOf { it.amount }
        val receivables = invoices.filter { !it.isDraft }.sumOf { it.balance }
        val totalVault = savingsGoals.sumOf { it.currentAmount }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
            color = COLOR_BORDER
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_HEADER_SUB
            textSize = 9.5f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT_SAGE
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_PRIMARY
            textSize = 9f
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_PRIMARY
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_SECONDARY
            textSize = 8.5f
        }
        val smallMutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_MUTED
            textSize = 7.8f
        }
        val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_PRIMARY
            textSize = 9f
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_PRIMARY
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        fun drawFooter(c: Canvas, pNum: Int) {
            fillPaint.color = COLOR_BORDER
            c.drawRect(MARGIN, PAGE_HEIGHT - 42f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 41.2f, fillPaint)
            c.drawText(
                settings.businessName.ifBlank { "Mengidata" },
                MARGIN,
                PAGE_HEIGHT - 26f,
                smallMutedPaint
            )
            val pageText = "${tr(language, "Ukurasa", "Page")} $pNum"
            val pRight = Paint(smallMutedPaint).apply { textAlign = Paint.Align.RIGHT }
            c.drawText(pageText, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 26f, pRight)
        }

        fun drawTableHeader(c: Canvas, yTop: Float): Float {
            fillPaint.color = COLOR_HEADER_SLATE
            c.drawRoundRect(RectF(MARGIN, yTop, PAGE_WIDTH - MARGIN, yTop + 24f), 6f, 6f, fillPaint)
            val th = Paint(bodyBoldPaint).apply { color = COLOR_WHITE; textSize = 8.5f }
            val thRight = Paint(th).apply { textAlign = Paint.Align.RIGHT }
            c.drawText(tr(language, "TAREHE", "DATE"), MARGIN + 8f, yTop + 15.5f, th)
            c.drawText(tr(language, "MAELEZO / MHUSIKA", "DESCRIPTION / PARTY"), MARGIN + 78f, yTop + 15.5f, th)
            c.drawText(tr(language, "AINA", "BUCKET"), MARGIN + 320f, yTop + 15.5f, th)
            c.drawText(tr(language, "KIASI ($currency)", "AMOUNT ($currency)"), PAGE_WIDTH - MARGIN - 8f, yTop + 15.5f, thRight)
            return yTop + 28f
        }

        fun startContinuationPage(): Float {
            drawFooter(canvas, pageNumber)
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            fillPaint.color = COLOR_HEADER_SLATE
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 44f, fillPaint)
            fillPaint.color = COLOR_ACCENT_SAGE
            canvas.drawRect(0f, 44f, PAGE_WIDTH.toFloat(), 47f, fillPaint)
            val contPaint = Paint(titlePaint).apply { textSize = 12f }
            canvas.drawText(
                "${tr(language, "TAARIFA YA FEDHA", "FINANCIAL STATEMENT")} — $periodLabel",
                MARGIN,
                27f,
                contPaint
            )
            return drawTableHeader(canvas, 60f)
        }

        // 1. Calm Matte Executive Header Banner
        fillPaint.color = COLOR_HEADER_SLATE
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 96f, fillPaint)
        fillPaint.color = COLOR_ACCENT_SAGE
        canvas.drawRect(0f, 96f, PAGE_WIDTH.toFloat(), 100f, fillPaint)

        canvas.drawText(settings.businessName.ifBlank { "Mengidata" }, MARGIN, 38f, titlePaint)
        val contactLine = listOf(settings.phone, settings.email, settings.address)
            .filter { it.isNotBlank() }
            .joinToString("  ·  ")
        if (contactLine.isNotBlank()) {
            canvas.drawText(truncateText(contactLine, 330f, subHeaderPaint), MARGIN, 56f, subHeaderPaint)
        }
        canvas.drawText(
            "${tr(language, "Kipindi", "Period")}: $periodLabel   ·   ${tr(language, "Tarehe", "Generated")}: $today",
            MARGIN,
            74f,
            subHeaderPaint
        )

        val rightTitle = Paint(titlePaint).apply {
            textSize = 14f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(
            tr(language, "TAARIFA YA FEDHA", "FINANCIAL STATEMENT"),
            PAGE_WIDTH - MARGIN,
            40f,
            rightTitle
        )
        val badgeText = "${periodTxns.size} ${tr(language, "MIAMALA", "ENTRIES")}"
        val badgePaint = Paint(bodyBoldPaint).apply {
            color = COLOR_ACCENT_SAGE
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        val badgeW = max(84f, badgePaint.measureText(badgeText) + 20f)
        val badgeRect = RectF(PAGE_WIDTH - MARGIN - badgeW, 54f, PAGE_WIDTH - MARGIN, 74f)
        fillPaint.color = COLOR_ACCENT_SAGE_SOFT
        canvas.drawRoundRect(badgeRect, 10f, 10f, fillPaint)
        canvas.drawText(badgeText, badgeRect.centerX(), badgeRect.centerY() + 3f, badgePaint)

        // 2. Executive KPI 4-Card Row
        var y = 116f
        val gap = 8f
        val cardW = (CONTENT_WIDTH - gap * 3) / 4f
        val kpis = listOf(
            Triple(tr(language, "MAPATO", "INCOME"), FormatUtils.formatCompactMoney(periodIncome, currency), COLOR_INCOME),
            Triple(tr(language, "MATUMIZI", "EXPENSES"), FormatUtils.formatCompactMoney(periodPersonal, currency), COLOR_EXPENSE),
            Triple(tr(language, "BILI NYUMBANI", "HOUSEHOLD"), FormatUtils.formatCompactMoney(periodHousehold, currency), COLOR_HOUSEHOLD),
            Triple(tr(language, "AKIBA", "SAVINGS"), FormatUtils.formatCompactMoney(periodSavings, currency), COLOR_SAVINGS)
        )
        kpis.forEachIndexed { idx, (label, value, accent) ->
            val left = MARGIN + idx * (cardW + gap)
            val rect = RectF(left, y, left + cardW, y + 54f)
            fillPaint.color = COLOR_SURFACE_CARD
            canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
            canvas.drawRoundRect(rect, 8f, 8f, strokePaint)
            fillPaint.color = accent
            canvas.drawRoundRect(RectF(left + 1f, y + 1f, left + cardW - 1f, y + 4.5f), 4f, 4f, fillPaint)
            val lblP = Paint(sectionPaint).apply { color = COLOR_INK_SECONDARY; textSize = 7.5f }
            val valP = Paint(bodyBoldPaint).apply { color = accent; textSize = 11f }
            canvas.drawText(label, left + 10f, y + 22f, lblP)
            canvas.drawText(truncateText(value, cardW - 18f, valP), left + 10f, y + 42f, valP)
        }
        y += 66f

        // 3. Net Balance + Obligations Strip
        val stripRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 52f)
        fillPaint.color = COLOR_ACCENT_SAGE_SOFT
        canvas.drawRoundRect(stripRect, 8f, 8f, fillPaint)
        canvas.drawRoundRect(stripRect, 8f, 8f, strokePaint)

        val netLabelPaint = Paint(sectionPaint).apply { color = COLOR_ACCENT_SAGE; textSize = 8.5f }
        val netValuePaint = Paint(bodyBoldPaint).apply { color = COLOR_INK_PRIMARY; textSize = 14f }
        canvas.drawText(tr(language, "SALIO HALISI LA MFUMO", "SYSTEM NET BALANCE"), MARGIN + 14f, y + 19f, netLabelPaint)
        canvas.drawText(FormatUtils.formatMoney(netBalance, currency), MARGIN + 14f, y + 39f, netValuePaint)

        val metaRight = Paint(mutedPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 8.5f }
        canvas.drawText(
            "${tr(language, "Madai ya Ankara", "Receivables")}: ${FormatUtils.formatCompactMoney(receivables, currency)}   ·   ${tr(language, "Bili Zinazodaiwa", "Unpaid Bills")}: ${FormatUtils.formatCompactMoney(unpaidBills, currency)}",
            PAGE_WIDTH - MARGIN - 14f,
            y + 21f,
            metaRight
        )
        canvas.drawText(
            "${tr(language, "Jumla ya Mfuko wa Akiba", "Total Savings Vault")}: ${FormatUtils.formatCompactMoney(totalVault, currency)} (${savingsGoals.size} ${tr(language, "malengo", "goals")})",
            PAGE_WIDTH - MARGIN - 14f,
            y + 38f,
            metaRight
        )
        y += 66f

        // 4. Ledger Table Section
        canvas.drawText(
            tr(language, "ORODHA YA MIAMALA KWA KIPINDI", "PERIOD LEDGER ENTRIES"),
            MARGIN,
            y,
            sectionPaint
        )
        y += 10f
        y = drawTableHeader(canvas, y)

        if (periodTxns.isEmpty()) {
            fillPaint.color = COLOR_SURFACE_CARD
            val emptyRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 38f)
            canvas.drawRoundRect(emptyRect, 6f, 6f, fillPaint)
            canvas.drawText(
                tr(language, "Hakuna miamala katika kipindi hiki.", "No transactions recorded in this period."),
                MARGIN + 12f,
                y + 23f,
                mutedPaint
            )
            y += 48f
        } else {
            periodTxns.forEachIndexed { idx, txn ->
                if (y + 28f > PAGE_HEIGHT - 56f) {
                    y = startContinuationPage()
                }
                val bucket = AutoAccountingEngine.classifyTransaction(txn)
                val rowH = 26f
                if (idx % 2 == 1) {
                    fillPaint.color = COLOR_SURFACE_ZEBRA
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowH, fillPaint)
                }
                val amountColor = when {
                    txn.isIncome -> COLOR_INCOME
                    bucket == AutoBucket.HOUSEHOLD_BILL -> COLOR_HOUSEHOLD
                    bucket == AutoBucket.SAVINGS -> COLOR_SAVINGS
                    else -> COLOR_EXPENSE
                }
                val descParty = listOf(txn.description, txn.party).filter { it.isNotBlank() }.joinToString(" · ")
                canvas.drawText(txn.date, MARGIN + 8f, y + 16.5f, mutedPaint)
                canvas.drawText(truncateText(descParty, 234f, bodyPaint), MARGIN + 78f, y + 16.5f, bodyPaint)
                val bucketPaint = Paint(bodyBoldPaint).apply { color = amountColor; textSize = 8f }
                canvas.drawText(truncateText(bucket.label(language), 115f, bucketPaint), MARGIN + 320f, y + 16.5f, bucketPaint)
                val amtPaint = Paint(rightBoldPaint).apply { color = amountColor; textSize = 9f }
                val sign = if (txn.isIncome) "+" else "-"
                canvas.drawText("$sign${FormatUtils.formatCompactMoney(txn.amount, currency)}", PAGE_WIDTH - MARGIN - 8f, y + 16.5f, amtPaint)

                fillPaint.color = COLOR_BORDER
                canvas.drawRect(MARGIN, y + rowH - 0.6f, PAGE_WIDTH - MARGIN, y + rowH, fillPaint)
                y += rowH
            }
        }

        drawFooter(canvas, pageNumber)
        doc.finishPage(page)

        return ByteArrayOutputStream().use { baos ->
            doc.writeTo(baos)
            doc.close()
            baos.toByteArray()
        }
    }

    private fun truncateText(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }
}
