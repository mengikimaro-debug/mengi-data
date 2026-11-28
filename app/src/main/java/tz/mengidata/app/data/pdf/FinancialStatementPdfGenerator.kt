package tz.mengidata.app.data.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
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
import tz.mengidata.app.security.CopyrightVault
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Top category insight extracted for the Statement Portfolio cover page.
 */
data class StatementCategoryHighlight(
    val categoryName: String,
    val isIncome: Boolean,
    val amount: Double,
    val count: Int
)

/**
 * Pure deterministic analytics snapshot powering the bespoke Swiss Private-Bank
 * Architectural Financial Statement PDF. Kept pure-Kotlin for fast JVM unit testing.
 */
data class StatementAnalyticsSnapshot(
    val periodTransactions: List<TransactionEntity>,
    val incomeTotal: Double,
    val personalExpenseTotal: Double,
    val householdTotal: Double,
    val savingsTotal: Double,
    val totalOutflow: Double,
    val totalFlowVolume: Double,
    val netBalance: Double,
    val netMarginPercent: Double?,
    val incomeCount: Int,
    val personalCount: Int,
    val householdCount: Int,
    val savingsCount: Int,
    val incomeFlowSharePct: Int,
    val personalFlowSharePct: Int,
    val householdFlowSharePct: Int,
    val savingsFlowSharePct: Int,
    val unpaidBillsAmount: Double,
    val unpaidBillsCount: Int,
    val receivablesAmount: Double,
    val openInvoicesCount: Int,
    val totalSavingsVault: Double,
    val savingsGoalsCount: Int,
    val averageGoalCompletionPct: Int,
    val topCategories: List<StatementCategoryHighlight>
)

/**
 * Generates a signature Swiss Private-Bank / Architectural Dossier Financial Statement PDF.
 * Distinctly styled on warm alabaster drafting paper with a full-height Obsidian-Indigo &
 * Matte Bronze spine, asymmetric Split Editorial Masthead, Net Position Monolith,
 * Cashflow Proportion Bar, 2x2 Pillar Matrix, Obligations Vault, and Typographic Ledger.
 * Zero-Green palette strictly enforced across every surface, indicator, and accent.
 */
object FinancialStatementPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val SPINE_WIDTH = 10f
    private const val SPINE_TRIM = 2f
    private const val MARGIN_LEFT = 34f
    private const val MARGIN_RIGHT = 32f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT
    private const val CONTENT_RIGHT = PAGE_WIDTH - MARGIN_RIGHT

    // Architectural Paper & Obsidian Palette (Zero-Green, Matte, Non-Glare)
    private class StatementPalette {
        val paperAlabaster = Color.parseColor("#F7F5F0")
        val cardIvory = Color.parseColor("#FFFFFF")
        val stoneWarm = Color.parseColor("#EFECE4")
        val stoneMuted = Color.parseColor("#EAE5DA")
        val ivoryTint = Color.parseColor("#FAF8F4")

        val obsidian = Color.parseColor("#141D28")
        val obsidianMid = Color.parseColor("#212E3F")
        val obsidianLine = Color.parseColor("#2B3B4F")
        val inkPrimary = Color.parseColor("#141D28")
        val inkSecondary = Color.parseColor("#495666")
        val inkMuted = Color.parseColor("#75808E")

        val borderWarm = Color.parseColor("#DCD6CA")
        val hairlineWarm = Color.parseColor("#E6E1D7")
        val borderStone = Color.parseColor("#CFC5B4")

        val bronze = Color.parseColor("#B68E5D")
        val bronzeLight = Color.parseColor("#DFC7A2")
        val bronzeSoft = Color.parseColor("#F4EDE1")

        // Semantic Bucket Palette (Zero Green)
        val income = Color.parseColor("#24476E")
        val incomeSoft = Color.parseColor("#E1E9F2")
        val personal = Color.parseColor("#7E4949")
        val personalSoft = Color.parseColor("#F2E4E4")
        val household = Color.parseColor("#3A526E")
        val householdSoft = Color.parseColor("#E3EAF1")
        val savings = Color.parseColor("#4E446B")
        val savingsSoft = Color.parseColor("#E7E4F0")

        val monoLabelMuted = Color.parseColor("#9BA8B8")
        val surplusBg = Color.parseColor("#243A52")
        val surplusText = Color.parseColor("#C7D9EC")
        val deficitBg = Color.parseColor("#4F2D32")
        val deficitText = Color.parseColor("#EED0D0")
        val vaultHeaderInk = Color.parseColor("#73532C")
        val sealHeaderInk = Color.parseColor("#7A592F")
    }

    fun computeSnapshot(
        transactions: List<TransactionEntity>,
        bills: List<BillEntity>,
        invoices: List<InvoiceWithItems>,
        savingsGoals: List<SavingsGoalEntity>,
        period: PeriodFilter
    ): StatementAnalyticsSnapshot {
        val periodTxns = transactions
            .filter { FormatUtils.isDateInPeriod(it.date, period) }
            .sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.createdAt })

        val incomeTxns = periodTxns.filter { it.isIncome }
        val householdTxns = periodTxns.filter { !it.isIncome && AutoAccountingEngine.isHouseholdTransaction(it) }
        val savingsTxns = periodTxns.filter {
            !it.isIncome && !AutoAccountingEngine.isHouseholdTransaction(it) && AutoAccountingEngine.isSavingsTransaction(it)
        }
        val personalTxns = periodTxns.filter {
            !it.isIncome && !AutoAccountingEngine.isHouseholdTransaction(it) && !AutoAccountingEngine.isSavingsTransaction(it)
        }

        val incomeTotal = incomeTxns.sumOf { it.amount }
        val personalTotal = personalTxns.sumOf { it.amount }
        val householdTotal = householdTxns.sumOf { it.amount }
        val savingsTotal = savingsTxns.sumOf { it.amount }
        val totalOutflow = personalTotal + householdTotal + savingsTotal
        val totalFlowVolume = incomeTotal + totalOutflow
        val netBalance = incomeTotal - totalOutflow
        val marginPct = if (incomeTotal > 0.005) ((netBalance / incomeTotal) * 100.0) else null

        fun sharePct(part: Double): Int {
            if (totalFlowVolume <= 0.005 || part <= 0.0) return 0
            return ((part / totalFlowVolume) * 100.0).roundToInt().coerceIn(0, 100)
        }

        val unpaidBills = bills.filter { !it.paid }
        val openInvoices = invoices.filter { !it.isDraft && it.balance > 0.005 }
        val totalVault = savingsGoals.sumOf { it.currentAmount }
        val avgCompletion = if (savingsGoals.isEmpty()) {
            0
        } else {
            (savingsGoals.map { it.progressRatio }.average() * 100.0).roundToInt().coerceIn(0, 100)
        }

        val topCats = periodTxns
            .groupBy { it.isIncome to it.category.trim().ifBlank { "Mengineyo" } }
            .map { (key, list) ->
                StatementCategoryHighlight(
                    categoryName = key.second,
                    isIncome = key.first,
                    amount = list.sumOf { it.amount },
                    count = list.size
                )
            }
            .sortedByDescending { it.amount }
            .take(3)

        return StatementAnalyticsSnapshot(
            periodTransactions = periodTxns,
            incomeTotal = incomeTotal,
            personalExpenseTotal = personalTotal,
            householdTotal = householdTotal,
            savingsTotal = savingsTotal,
            totalOutflow = totalOutflow,
            totalFlowVolume = totalFlowVolume,
            netBalance = netBalance,
            netMarginPercent = marginPct,
            incomeCount = incomeTxns.size,
            personalCount = personalTxns.size,
            householdCount = householdTxns.size,
            savingsCount = savingsTxns.size,
            incomeFlowSharePct = sharePct(incomeTotal),
            personalFlowSharePct = sharePct(personalTotal),
            householdFlowSharePct = sharePct(householdTotal),
            savingsFlowSharePct = sharePct(savingsTotal),
            unpaidBillsAmount = unpaidBills.sumOf { it.amount },
            unpaidBillsCount = unpaidBills.size,
            receivablesAmount = openInvoices.sumOf { it.balance },
            openInvoicesCount = openInvoices.size,
            totalSavingsVault = totalVault,
            savingsGoalsCount = savingsGoals.size,
            averageGoalCompletionPct = avgCompletion,
            topCategories = topCats
        )
    }

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
        val pal = StatementPalette()
        val currency = settings.currency.ifBlank { "TZS" }
        val today = FormatUtils.todayIso()
        val businessName = settings.businessName.ifBlank { "VaultLedger" }
        val periodLabel = if (language == AppLanguage.EN) period.labelEn else period.labelSw
        val snapshot = computeSnapshot(transactions, bills, invoices, savingsGoals, period)

        // Core Vector Paints
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val warmBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.85f
            color = pal.borderWarm
        }
        val stoneBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
            color = pal.borderStone
        }
        val bronzeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
            color = pal.bronze
        }
        val hairlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.hairlineWarm
            strokeWidth = 0.8f
        }

        // Editorial Typography Paints
        val heroSerifTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.inkPrimary
            textSize = 23.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val trackedEyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.inkSecondary
            textSize = 7.4f
            letterSpacing = 0.14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val businessPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.inkPrimary
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.inkPrimary
            textSize = 9.0f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.inkPrimary
            textSize = 9.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.inkSecondary
            textSize = 8.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val smallMutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.inkMuted
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val rightBoldPaint = Paint(bodyBoldPaint).apply {
            textAlign = Paint.Align.RIGHT
        }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        fun drawPaperAndArchitecturalSpine(c: Canvas) {
            // Warm architectural alabaster paper canvas
            fillPaint.color = pal.paperAlabaster
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), fillPaint)

            // Subtle architectural corner grid watermark (top-right)
            val wmRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = pal.bronze
                alpha = 20
                style = Paint.Style.STROKE
                strokeWidth = 0.9f
            }
            c.drawCircle(PAGE_WIDTH - 24f, 16f, 64f, wmRing)
            c.drawCircle(PAGE_WIDTH - 24f, 16f, 92f, wmRing)

            // Full-height Obsidian-Indigo + Warm Bronze left binding spine
            fillPaint.color = pal.obsidian
            c.drawRect(0f, 0f, SPINE_WIDTH, PAGE_HEIGHT.toFloat(), fillPaint)
            fillPaint.color = pal.bronze
            c.drawRect(SPINE_WIDTH, 0f, SPINE_WIDTH + SPINE_TRIM, PAGE_HEIGHT.toFloat(), fillPaint)
        }

        fun drawSoftElevation(c: Canvas, rect: RectF, radius: Float) {
            val shadow1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(12, 20, 29, 40) }
            val shadow2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(6, 20, 29, 40) }
            c.drawRoundRect(
                RectF(rect.left + 0.8f, rect.top + 2.5f, rect.right + 0.8f, rect.bottom + 3.8f),
                radius + 1f,
                radius + 1f,
                shadow2
            )
            c.drawRoundRect(
                RectF(rect.left + 0.4f, rect.top + 1.2f, rect.right + 0.4f, rect.bottom + 2.0f),
                radius,
                radius,
                shadow1
            )
        }

        fun drawFooter(c: Canvas, pNum: Int) {
            val ruleY = PAGE_HEIGHT - 40f
            fillPaint.color = pal.borderWarm
            c.drawRect(MARGIN_LEFT, ruleY, CONTENT_RIGHT, ruleY + 0.8f, fillPaint)
            fillPaint.color = pal.bronze
            c.drawRect(MARGIN_LEFT, ruleY - 0.5f, MARGIN_LEFT + 44f, ruleY + 1.4f, fillPaint)

            val footerLeft = "$businessName  ·  ${CopyrightVault.pdfFooterBrand(language)}"
            c.drawText(
                truncateText(footerLeft, CONTENT_WIDTH - 88f, smallMutedPaint),
                MARGIN_LEFT,
                PAGE_HEIGHT - 22f,
                smallMutedPaint
            )

            // Editorial Folio Badge
            val folioLabel = "${tr(language, "FOLIO", "FOLIO")} ${String.format(Locale.US, "%02d", pNum)}"
            val folioTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = pal.cardIvory
                textSize = 7.4f
                letterSpacing = 0.08f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val folioW = max(56f, folioTextPaint.measureText(folioLabel) + 18f)
            val folioRect = RectF(CONTENT_RIGHT - folioW, PAGE_HEIGHT - 32f, CONTENT_RIGHT, PAGE_HEIGHT - 16f)
            fillPaint.color = pal.obsidian
            c.drawRoundRect(folioRect, 4f, 4f, fillPaint)
            c.drawText(folioLabel, folioRect.centerX(), folioRect.centerY() + 2.6f, folioTextPaint)
        }

        fun drawLedgerColumnHeaders(c: Canvas, yTop: Float): Float {
            val headerRect = RectF(MARGIN_LEFT, yTop, CONTENT_RIGHT, yTop + 25f)
            val topRounded = Path().apply {
                addRoundRect(headerRect, floatArrayOf(8f, 8f, 8f, 8f, 0f, 0f, 0f, 0f), Path.Direction.CW)
            }
            fillPaint.color = pal.stoneMuted
            c.drawPath(topRounded, fillPaint)
            // Architectural Obsidian bottom baseline + Bronze notch
            fillPaint.color = pal.obsidian
            c.drawRect(MARGIN_LEFT, yTop + 23.4f, CONTENT_RIGHT, yTop + 25f, fillPaint)
            fillPaint.color = pal.bronze
            c.drawRect(MARGIN_LEFT, yTop + 23.0f, MARGIN_LEFT + 52f, yTop + 25.2f, fillPaint)

            val th = Paint(trackedEyebrowPaint).apply {
                color = pal.inkPrimary
                textSize = 7.3f
                letterSpacing = 0.09f
            }
            val thRight = Paint(th).apply { textAlign = Paint.Align.RIGHT }
            val textY = yTop + 15.8f
            c.drawText(tr(language, "TAREHE", "DATE"), MARGIN_LEFT + 12f, textY, th)
            c.drawText(tr(language, "MAELEZO & MHUSIKA", "ENTRY & COUNTERPARTY"), MARGIN_LEFT + 88f, textY, th)
            c.drawText(tr(language, "AINA · NJIA", "BUCKET · METHOD"), MARGIN_LEFT + 314f, textY, th)
            c.drawText(tr(language, "KIASI ($currency)", "AMOUNT ($currency)"), CONTENT_RIGHT - 12f, textY, thRight)
            return yTop + 25f
        }

        fun startContinuationPage(): Float {
            drawFooter(canvas, pageNumber)
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            drawPaperAndArchitecturalSpine(canvas)

            // Crisp Editorial Running Masthead
            val runTitle = Paint(heroSerifTitlePaint).apply { textSize = 13.5f }
            val runMeta = Paint(trackedEyebrowPaint).apply {
                color = pal.bronze
                textSize = 7.5f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(
                tr(language, "Taarifa ya Fedha", "Financial Statement"),
                MARGIN_LEFT,
                32f,
                runTitle
            )
            canvas.drawText(
                "${businessName.uppercase(Locale.ROOT)}  ·  $periodLabel",
                CONTENT_RIGHT,
                31f,
                runMeta
            )
            fillPaint.color = pal.bronze
            canvas.drawRect(MARGIN_LEFT, 40f, MARGIN_LEFT + 40f, 41.8f, fillPaint)
            fillPaint.color = pal.borderWarm
            canvas.drawRect(MARGIN_LEFT + 44f, 40.5f, CONTENT_RIGHT, 41.3f, fillPaint)

            return drawLedgerColumnHeaders(canvas, 54f)
        }

        // ==================== PAGE 1 CANVAS & SPINE ====================
        drawPaperAndArchitecturalSpine(canvas)

        // ==================== 1. ASYMMETRIC SPLIT EDITORIAL MASTHEAD ====================
        val mastTop = 26f
        val leftMastW = CONTENT_WIDTH * 0.56f
        val monolithLeft = MARGIN_LEFT + leftMastW + 14f
        val monolithW = CONTENT_RIGHT - monolithLeft

        // Left Masthead: Folio Dossier Tag + Serif Hero Title + Identity + Metadata Chips
        val eyebrowText = "${tr(language, "JALADA LA FEDHA", "FINANCIAL DOSSIER")}  ·  ${period.name}"
        val eyebrowBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.bronzeLight
            textSize = 6.9f
            letterSpacing = 0.13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val ebW = eyebrowBadgePaint.measureText(eyebrowText) + 18f
        val ebRect = RectF(MARGIN_LEFT, mastTop, MARGIN_LEFT + ebW, mastTop + 16f)
        fillPaint.color = pal.obsidian
        canvas.drawRoundRect(ebRect, 4f, 4f, fillPaint)
        canvas.drawText(eyebrowText, ebRect.left + 9f, ebRect.centerY() + 2.4f, eyebrowBadgePaint)

        val heroTitle = tr(language, "Taarifa ya Fedha", "Financial Statement")
        canvas.drawText(heroTitle, MARGIN_LEFT, mastTop + 44f, heroSerifTitlePaint)
        fillPaint.color = pal.bronze
        canvas.drawRoundRect(
            RectF(MARGIN_LEFT, mastTop + 50f, MARGIN_LEFT + 54f, mastTop + 52.6f),
            1.3f,
            1.3f,
            fillPaint
        )

        canvas.drawText(truncateText(businessName, leftMastW, businessPaint), MARGIN_LEFT, mastTop + 71f, businessPaint)
        val contactParts = buildList {
            if (settings.ownerName.isNotBlank()) add(settings.ownerName.trim())
            if (settings.phone.isNotBlank()) add(settings.phone.trim())
            if (settings.tin.isNotBlank()) add("TIN ${settings.tin.trim()}")
        }.joinToString("  ·  ")
        if (contactParts.isNotBlank()) {
            canvas.drawText(truncateText(contactParts, leftMastW, mutedPaint), MARGIN_LEFT, mastTop + 85f, mutedPaint)
        } else if (settings.email.isNotBlank()) {
            canvas.drawText(truncateText(settings.email.trim(), leftMastW, mutedPaint), MARGIN_LEFT, mastTop + 85f, mutedPaint)
        }

        // Metadata Pill Chips (Period & Generation Date)
        val chipPaint = Paint(bodyBoldPaint).apply {
            textSize = 7.7f
            color = pal.inkPrimary
        }
        val chip1Text = "${tr(language, "Kipindi", "Period")}: $periodLabel"
        val chip2Text = "${tr(language, "Tarehe", "Issued")}: $today"
        val chip1W = chipPaint.measureText(chip1Text) + 16f
        val chip2W = chipPaint.measureText(chip2Text) + 16f
        val chipY = mastTop + 97f
        val chip1Rect = RectF(MARGIN_LEFT, chipY, MARGIN_LEFT + chip1W, chipY + 18f)
        fillPaint.color = pal.stoneMuted
        canvas.drawRoundRect(chip1Rect, 5f, 5f, fillPaint)
        canvas.drawRoundRect(chip1Rect, 5f, 5f, stoneBorderPaint)
        canvas.drawText(chip1Text, chip1Rect.left + 8f, chip1Rect.centerY() + 2.6f, chipPaint)

        val chip2Rect = RectF(chip1Rect.right + 7f, chipY, chip1Rect.right + 7f + chip2W, chipY + 18f)
        if (chip2Rect.right <= MARGIN_LEFT + leftMastW + 4f) {
            fillPaint.color = pal.stoneMuted
            canvas.drawRoundRect(chip2Rect, 5f, 5f, fillPaint)
            canvas.drawRoundRect(chip2Rect, 5f, 5f, stoneBorderPaint)
            canvas.drawText(chip2Text, chip2Rect.left + 8f, chip2Rect.centerY() + 2.6f, chipPaint)
        }

        // Right Masthead: Dark Obsidian-Indigo "Net Position Monolith"
        val monolithRect = RectF(monolithLeft, mastTop, CONTENT_RIGHT, mastTop + 118f)
        drawSoftElevation(canvas, monolithRect, 12f)
        fillPaint.color = pal.obsidian
        canvas.drawRoundRect(monolithRect, 12f, 12f, fillPaint)

        // Subtle geometric quarter-arc & Bronze top trim clipped inside Monolith
        canvas.save()
        canvas.clipRect(monolithRect)
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.bronze
            alpha = 36
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawCircle(monolithRect.right - 10f, monolithRect.top + 8f, 44f, arcPaint)
        canvas.drawCircle(monolithRect.right - 10f, monolithRect.top + 8f, 64f, arcPaint)
        fillPaint.color = pal.bronze
        canvas.drawRect(monolithRect.left, monolithRect.top, monolithRect.right, monolithRect.top + 3f, fillPaint)
        canvas.restore()

        val monoEyebrow = Paint(trackedEyebrowPaint).apply {
            color = pal.bronzeLight
            textSize = 7.0f
            letterSpacing = 0.13f
        }
        canvas.drawText(
            tr(language, "SALIO HALISI LA KIPINDI", "PERIOD NET POSITION"),
            monolithRect.left + 15f,
            monolithRect.top + 22f,
            monoEyebrow
        )

        val netFormatted = FormatUtils.formatMoney(snapshot.netBalance, currency)
        val netHeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.cardIvory
            textSize = fitTextSize(netFormatted, monolithW - 30f, 17.5f, 10.5f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(netFormatted, monolithRect.left + 15f, monolithRect.top + 49f, netHeroPaint)

        // Monolith internal divider
        fillPaint.color = pal.obsidianLine
        canvas.drawRect(monolithRect.left + 15f, monolithRect.top + 63f, monolithRect.right - 15f, monolithRect.top + 64f, fillPaint)

        // Bottom split micro-metrics inside Monolith
        val monoLbl = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.monoLabelMuted
            textSize = 7.0f
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val monoVal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.cardIvory
            textSize = 10.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val marginStr = snapshot.netMarginPercent?.let {
            val sign = if (it >= 0.0) "+" else ""
            String.format(Locale.US, "%s%.1f%%", sign, it)
        } ?: "—"

        canvas.drawText(tr(language, "KIWANGO CHA SALIO", "NET MARGIN"), monolithRect.left + 15f, monolithRect.top + 81f, monoLbl)
        canvas.drawText(marginStr, monolithRect.left + 15f, monolithRect.top + 98f, monoVal)

        val midX = monolithRect.left + monolithW * 0.52f
        canvas.drawText(tr(language, "MIAMALA", "ENTRIES"), midX, monolithRect.top + 81f, monoLbl)
        canvas.drawText("${snapshot.periodTransactions.size}", midX, monolithRect.top + 98f, monoVal)

        // Status pill in Monolith bottom-right
        val isSurplus = snapshot.netBalance >= 0.0
        val statusText = when {
            snapshot.periodTransactions.isEmpty() -> tr(language, "TUPU", "EMPTY")
            isSurplus -> tr(language, "SALIO ZURI", "SURPLUS")
            else -> tr(language, "NAKISI", "DEFICIT")
        }
        val statusBg = if (isSurplus) pal.surplusBg else pal.deficitBg
        val statusTextColor = if (isSurplus) pal.surplusText else pal.deficitText
        val stPaint = Paint(monoLbl).apply {
            color = statusTextColor
            textSize = 6.8f
            textAlign = Paint.Align.CENTER
        }
        val stW = max(50f, stPaint.measureText(statusText) + 12f)
        val stRect = RectF(monolithRect.right - 13f - stW, monolithRect.top + 85f, monolithRect.right - 13f, monolithRect.top + 101f)
        fillPaint.color = statusBg
        canvas.drawRoundRect(stRect, 5f, 5f, fillPaint)
        canvas.drawText(statusText, stRect.centerX(), stRect.centerY() + 2.4f, stPaint)

        // ==================== 2. CASHFLOW ARCHITECTURE & ALLOCATION RIBBON ====================
        var y = mastTop + 132f
        val allocRect = RectF(MARGIN_LEFT, y, CONTENT_RIGHT, y + 74f)
        drawSoftElevation(canvas, allocRect, 10f)
        fillPaint.color = pal.cardIvory
        canvas.drawRoundRect(allocRect, 10f, 10f, fillPaint)
        canvas.drawRoundRect(allocRect, 10f, 10f, warmBorderPaint)

        canvas.drawText(
            tr(language, "MGAWANYO WA MTIRIRIKO WA FEDHA", "CASHFLOW ARCHITECTURE & ALLOCATION"),
            allocRect.left + 14f,
            allocRect.top + 18f,
            trackedEyebrowPaint
        )
        val flowRightPaint = Paint(bodyBoldPaint).apply {
            textSize = 8.2f
            textAlign = Paint.Align.RIGHT
            color = pal.inkSecondary
        }
        canvas.drawText(
            "${tr(language, "Mzunguko Jumla", "Total Flow")}: ${FormatUtils.formatMoney(snapshot.totalFlowVolume, currency)}",
            allocRect.right - 14f,
            allocRect.top + 18f,
            flowRightPaint
        )

        // Precision Proportional Stacked Bar
        val barLeft = allocRect.left + 14f
        val barRight = allocRect.right - 14f
        val barWidth = barRight - barLeft
        val barTop = allocRect.top + 27f
        val barBottom = barTop + 10.5f

        data class FlowSegment(
            val label: String,
            val amount: Double,
            val pct: Int,
            val color: Int
        )

        val segments = listOf(
            FlowSegment(tr(language, "Mapato", "Money In"), snapshot.incomeTotal, snapshot.incomeFlowSharePct, pal.income),
            FlowSegment(tr(language, "Matumizi", "Expenses"), snapshot.personalExpenseTotal, snapshot.personalFlowSharePct, pal.personal),
            FlowSegment(tr(language, "Nyumbani", "Household"), snapshot.householdTotal, snapshot.householdFlowSharePct, pal.household),
            FlowSegment(tr(language, "Akiba", "Savings"), snapshot.savingsTotal, snapshot.savingsFlowSharePct, pal.savings)
        )

        val activeSegments = segments.filter { it.amount > 0.005 }
        if (activeSegments.isEmpty() || snapshot.totalFlowVolume <= 0.005) {
            fillPaint.color = pal.stoneMuted
            canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barBottom), 5f, 5f, fillPaint)
        } else {
            val segGap = 2.5f
            val usableW = barWidth - segGap * (activeSegments.size - 1)
            var curX = barLeft
            activeSegments.forEachIndexed { idx, seg ->
                val rawW = ((seg.amount / snapshot.totalFlowVolume) * usableW).toFloat()
                val segW = if (idx == activeSegments.lastIndex) (barRight - curX).coerceAtLeast(4f) else rawW.coerceAtLeast(6f)
                val segRect = RectF(curX, barTop, (curX + segW).coerceAtMost(barRight), barBottom)
                fillPaint.color = seg.color
                val r = 4.5f
                val radii = when {
                    activeSegments.size == 1 -> floatArrayOf(r, r, r, r, r, r, r, r)
                    idx == 0 -> floatArrayOf(r, r, 1.5f, 1.5f, 1.5f, 1.5f, r, r)
                    idx == activeSegments.lastIndex -> floatArrayOf(1.5f, 1.5f, r, r, r, r, 1.5f, 1.5f)
                    else -> floatArrayOf(1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f)
                }
                val segPath = Path().apply { addRoundRect(segRect, radii, Path.Direction.CW) }
                canvas.drawPath(segPath, fillPaint)
                curX = segRect.right + segGap
            }
        }

        // 4-Column Architectural Legend Strip under Stacked Bar
        val legColW = barWidth / 4f
        val legLblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.inkSecondary
            textSize = 7.4f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val legValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.inkPrimary
            textSize = 8.4f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        segments.forEachIndexed { idx, seg ->
            val lx = barLeft + idx * legColW
            fillPaint.color = seg.color
            canvas.drawRoundRect(RectF(lx, allocRect.top + 46f, lx + 3.5f, allocRect.top + 64f), 1.5f, 1.5f, fillPaint)
            canvas.drawText("${seg.label} (${seg.pct}%)", lx + 8f, allocRect.top + 53f, legLblPaint)
            canvas.drawText(
                truncateText(FormatUtils.formatCompactMoney(seg.amount, currency), legColW - 12f, legValPaint),
                lx + 8f,
                allocRect.top + 64.5f,
                legValPaint
            )
        }

        // ==================== 3. 2x2 PILLAR MATRIX + OBLIGATIONS & VAULT PANEL ====================
        y = allocRect.bottom + 12f
        val gridLeftW = CONTENT_WIDTH * 0.64f
        val pillarGap = 9f
        val pillarW = (gridLeftW - pillarGap) / 2f
        val pillarH = 66f
        val matrixTotalH = pillarH * 2f + pillarGap

        data class PillarSpec(
            val title: String,
            val amount: Double,
            val count: Int,
            val sharePct: Int,
            val accentColor: Int,
            val softColor: Int
        )

        val pillars = listOf(
            PillarSpec(
                tr(language, "MAPATO YALIYOINGIA", "TOTAL INCOME"),
                snapshot.incomeTotal,
                snapshot.incomeCount,
                snapshot.incomeFlowSharePct,
                pal.income,
                pal.incomeSoft
            ),
            PillarSpec(
                tr(language, "MATUMIZI BINAFSI", "PERSONAL EXPENSES"),
                snapshot.personalExpenseTotal,
                snapshot.personalCount,
                snapshot.personalFlowSharePct,
                pal.personal,
                pal.personalSoft
            ),
            PillarSpec(
                tr(language, "BILI ZA NYUMBANI", "HOUSEHOLD BILLS"),
                snapshot.householdTotal,
                snapshot.householdCount,
                snapshot.householdFlowSharePct,
                pal.household,
                pal.householdSoft
            ),
            PillarSpec(
                tr(language, "AKIBA ILIYOWEKWA", "SAVINGS ALLOCATED"),
                snapshot.savingsTotal,
                snapshot.savingsCount,
                snapshot.savingsFlowSharePct,
                pal.savings,
                pal.savingsSoft
            )
        )

        pillars.forEachIndexed { idx, p ->
            val col = idx % 2
            val row = idx / 2
            val pLeft = MARGIN_LEFT + col * (pillarW + pillarGap)
            val pTop = y + row * (pillarH + pillarGap)
            val pRect = RectF(pLeft, pTop, pLeft + pillarW, pTop + pillarH)

            drawSoftElevation(canvas, pRect, 9f)
            fillPaint.color = pal.cardIvory
            canvas.drawRoundRect(pRect, 9f, 9f, fillPaint)
            canvas.drawRoundRect(pRect, 9f, 9f, warmBorderPaint)

            // Left vertical architectural color notch
            fillPaint.color = p.accentColor
            canvas.drawRoundRect(RectF(pRect.left, pRect.top + 10f, pRect.left + 3.5f, pRect.bottom - 10f), 2f, 2f, fillPaint)

            // Subtle right soft dot badge
            fillPaint.color = p.softColor
            canvas.drawCircle(pRect.right - 16f, pRect.top + 16f, 6.5f, fillPaint)
            fillPaint.color = p.accentColor
            canvas.drawCircle(pRect.right - 16f, pRect.top + 16f, 2.5f, fillPaint)

            val pTitlePaint = Paint(trackedEyebrowPaint).apply {
                color = pal.inkSecondary
                textSize = 6.9f
                letterSpacing = 0.08f
            }
            val pAmountText = FormatUtils.formatMoney(p.amount, currency)
            val pValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = p.accentColor
                textSize = fitTextSize(pAmountText, pillarW - 26f, 12.2f, 8.6f)
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val pSubPaint = Paint(smallMutedPaint).apply { textSize = 7.4f }

            canvas.drawText(truncateText(p.title, pillarW - 38f, pTitlePaint), pRect.left + 12f, pRect.top + 19f, pTitlePaint)
            canvas.drawText(pAmountText, pRect.left + 12f, pRect.top + 40f, pValPaint)
            val metaTxt = "${p.count} ${tr(language, "miamala", "entries")}  ·  ${p.sharePct}% ${tr(language, "mzunguko", "flow")}"
            canvas.drawText(truncateText(metaTxt, pillarW - 20f, pSubPaint), pRect.left + 12f, pRect.top + 55.5f, pSubPaint)
        }

        // Right Column: Obligations & Vault Reserve Architectural Dossier Card
        val vaultLeft = MARGIN_LEFT + gridLeftW + 10f
        val vaultW = CONTENT_RIGHT - vaultLeft
        val vaultRect = RectF(vaultLeft, y, CONTENT_RIGHT, y + matrixTotalH)
        fillPaint.color = pal.stoneWarm
        canvas.drawRoundRect(vaultRect, 10f, 10f, fillPaint)
        canvas.drawRoundRect(vaultRect, 10f, 10f, stoneBorderPaint)

        // Top bronze header bar inside Vault Dossier Card
        val vaultHeaderRect = RectF(vaultRect.left, vaultRect.top, vaultRect.right, vaultRect.top + 23f)
        val vaultHeaderPath = Path().apply {
            addRoundRect(vaultHeaderRect, floatArrayOf(10f, 10f, 10f, 10f, 0f, 0f, 0f, 0f), Path.Direction.CW)
        }
        fillPaint.color = pal.bronzeSoft
        canvas.drawPath(vaultHeaderPath, fillPaint)
        fillPaint.color = pal.bronze
        canvas.drawRect(vaultRect.left, vaultRect.top + 22f, vaultRect.right, vaultRect.top + 23.2f, fillPaint)

        val vaultHdrPaint = Paint(trackedEyebrowPaint).apply {
            color = pal.vaultHeaderInk
            textSize = 7.0f
            letterSpacing = 0.10f
        }
        canvas.drawText(
            tr(language, "MADAI & AKIBA KUU", "OBLIGATIONS & VAULT"),
            vaultRect.left + 12f,
            vaultRect.top + 15f,
            vaultHdrPaint
        )

        val vaultSlotH = (matrixTotalH - 24f) / 3f
        val vRows = listOf(
            Triple(
                tr(language, "Madai ya Ankara", "Receivables"),
                FormatUtils.formatCompactMoney(snapshot.receivablesAmount, currency),
                "${snapshot.openInvoicesCount} ${tr(language, "ankara wazi", "open invoices")}"
            ),
            Triple(
                tr(language, "Bili Zinazodaiwa", "Unpaid Bills"),
                FormatUtils.formatCompactMoney(snapshot.unpaidBillsAmount, currency),
                "${snapshot.unpaidBillsCount} ${tr(language, "bili hazijalipwa", "bills pending")}"
            ),
            Triple(
                tr(language, "Mfuko wa Akiba", "Savings Vault"),
                FormatUtils.formatCompactMoney(snapshot.totalSavingsVault, currency),
                "${snapshot.savingsGoalsCount} ${tr(language, "malengo", "goals")} · ${snapshot.averageGoalCompletionPct}%"
            )
        )
        vRows.forEachIndexed { idx, (lbl, amt, sub) ->
            val slotTop = vaultRect.top + 24f + idx * vaultSlotH
            val vLblP = Paint(smallMutedPaint).apply {
                color = pal.inkSecondary
                textSize = 7.4f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val vAmtP = Paint(bodyBoldPaint).apply {
                color = if (idx == 1 && snapshot.unpaidBillsAmount > 0.0) pal.personal else pal.inkPrimary
                textSize = fitTextSize(amt, vaultW - 24f, 10.6f, 8.2f)
            }
            canvas.drawText(lbl, vaultRect.left + 12f, slotTop + 12f, vLblP)
            canvas.drawText(amt, vaultRect.left + 12f, slotTop + 25f, vAmtP)
            canvas.drawText(truncateText(sub, vaultW - 24f, smallMutedPaint), vaultRect.left + 12f, slotTop + 34.5f, smallMutedPaint)
            if (idx < vRows.lastIndex) {
                canvas.drawLine(
                    vaultRect.left + 12f,
                    slotTop + vaultSlotH,
                    vaultRect.right - 12f,
                    slotTop + vaultSlotH,
                    hairlinePaint
                )
            }
        }

        y = vaultRect.bottom + 12f

        // ==================== 4. TOP CATEGORIES INLINE STRIP ====================
        if (snapshot.topCategories.isNotEmpty()) {
            val stripRect = RectF(MARGIN_LEFT, y, CONTENT_RIGHT, y + 32f)
            fillPaint.color = pal.ivoryTint
            canvas.drawRoundRect(stripRect, 8f, 8f, fillPaint)
            canvas.drawRoundRect(stripRect, 8f, 8f, warmBorderPaint)

            val stripTitle = tr(language, "MAKUNDI KUU:", "TOP ACTIVITY:")
            val sTitleP = Paint(trackedEyebrowPaint).apply {
                color = pal.bronze
                textSize = 7.0f
            }
            canvas.drawText(stripTitle, stripRect.left + 12f, stripRect.centerY() + 2.4f, sTitleP)

            var sx = stripRect.left + 12f + sTitleP.measureText(stripTitle) + 12f
            val availablePerChip = ((stripRect.right - 12f) - sx) / snapshot.topCategories.size
            val catNameP = Paint(bodyBoldPaint).apply { textSize = 7.8f }
            val catAmtP = Paint(mutedPaint).apply { textSize = 7.6f }

            snapshot.topCategories.forEach { cat ->
                val dotColor = if (cat.isIncome) pal.income else pal.personal
                fillPaint.color = dotColor
                canvas.drawCircle(sx + 3f, stripRect.centerY(), 2.8f, fillPaint)
                val locCat = FormatUtils.localizedCategory(cat.categoryName, language)
                val catText = "$locCat · ${FormatUtils.formatCompactMoney(cat.amount, currency)}"
                canvas.drawText(
                    truncateText(catText, (availablePerChip - 16f).coerceAtLeast(60f), catNameP),
                    sx + 9f,
                    stripRect.centerY() + 2.6f,
                    if (cat.isIncome) catNameP else catAmtP
                )
                sx += availablePerChip
            }
            y = stripRect.bottom + 14f
        } else {
            y += 2f
        }

        // ==================== 5. EDITORIAL SWISS LEDGER CHRONICLE ====================
        canvas.drawText(
            tr(language, "DAFTARI LA MIAMALA KWA KIPINDI", "PERIOD LEDGER CHRONICLE"),
            MARGIN_LEFT,
            y,
            trackedEyebrowPaint
        )
        val countRightP = Paint(smallMutedPaint).apply {
            textAlign = Paint.Align.RIGHT
            color = pal.inkSecondary
        }
        canvas.drawText(
            "${snapshot.periodTransactions.size} ${tr(language, "miamala iliyohakikiwa", "verified entries")}",
            CONTENT_RIGHT,
            y,
            countRightP
        )
        y += 8f

        var tableBlockTop = y
        y = drawLedgerColumnHeaders(canvas, y)

        if (snapshot.periodTransactions.isEmpty()) {
            val emptyRect = RectF(MARGIN_LEFT, y, CONTENT_RIGHT, y + 44f)
            val emptyPath = Path().apply {
                addRoundRect(emptyRect, floatArrayOf(0f, 0f, 0f, 0f, 8f, 8f, 8f, 8f), Path.Direction.CW)
            }
            fillPaint.color = pal.cardIvory
            canvas.drawPath(emptyPath, fillPaint)
            canvas.drawRoundRect(RectF(MARGIN_LEFT, tableBlockTop, CONTENT_RIGHT, emptyRect.bottom), 8f, 8f, warmBorderPaint)
            canvas.drawText(
                tr(language, "Hakuna miamala iliyorekodiwa katika kipindi hiki.", "No transactions recorded during this period."),
                MARGIN_LEFT + 14f,
                y + 25f,
                mutedPaint
            )
            y = emptyRect.bottom + 14f
        } else {
            val rowH = 33f
            snapshot.periodTransactions.forEachIndexed { idx, txn ->
                if (y + rowH > PAGE_HEIGHT - 58f) {
                    canvas.drawRoundRect(RectF(MARGIN_LEFT, tableBlockTop, CONTENT_RIGHT, y), 8f, 8f, warmBorderPaint)
                    y = startContinuationPage()
                    tableBlockTop = y - 25f
                }

                val bucket = AutoAccountingEngine.classifyTransaction(txn)
                val (accentColor, softBg) = when {
                    txn.isIncome -> pal.income to pal.incomeSoft
                    bucket == AutoBucket.HOUSEHOLD_BILL -> pal.household to pal.householdSoft
                    bucket == AutoBucket.SAVINGS -> pal.savings to pal.savingsSoft
                    else -> pal.personal to pal.personalSoft
                }

                val isLastRow = idx == snapshot.periodTransactions.lastIndex
                val rowRect = RectF(MARGIN_LEFT, y, CONTENT_RIGHT, y + rowH)
                fillPaint.color = if (idx % 2 == 1) pal.ivoryTint else pal.cardIvory
                if (isLastRow) {
                    val lastPath = Path().apply {
                        addRoundRect(rowRect, floatArrayOf(0f, 0f, 0f, 0f, 8f, 8f, 8f, 8f), Path.Direction.CW)
                    }
                    canvas.drawPath(lastPath, fillPaint)
                } else {
                    canvas.drawRect(rowRect, fillPaint)
                }

                // Bucket color indicator dot + Date
                fillPaint.color = accentColor
                canvas.drawCircle(MARGIN_LEFT + 14f, y + 13f, 3.0f, fillPaint)
                val dateP = Paint(smallMutedPaint).apply {
                    color = pal.inkSecondary
                    textSize = 8.0f
                }
                canvas.drawText(txn.date.take(10), MARGIN_LEFT + 22f, y + 15.5f, dateP)

                // Entry Title + Sub-caption (Party · Category)
                val descMain = txn.description.ifBlank { FormatUtils.localizedCategory(txn.category, language) }
                canvas.drawText(
                    truncateText(descMain, 214f, bodyBoldPaint),
                    MARGIN_LEFT + 88f,
                    y + 13.8f,
                    bodyBoldPaint
                )

                val subParts = listOf(
                    txn.party.trim(),
                    FormatUtils.localizedCategory(txn.category, language)
                ).filter { it.isNotBlank() }.distinct().joinToString(" · ")
                canvas.drawText(
                    truncateText(subParts.ifBlank { "—" }, 214f, smallMutedPaint),
                    MARGIN_LEFT + 88f,
                    y + 25.2f,
                    smallMutedPaint
                )

                // Soft Bucket Pill + Payment Method sub-caption
                val bucketLbl = truncateText(bucket.label(language), 104f, smallMutedPaint)
                val pillTxtP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    textSize = 7.1f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                val pillW = (pillTxtP.measureText(bucketLbl) + 12f).coerceAtMost(116f)
                val pillRect = RectF(MARGIN_LEFT + 314f, y + 5.5f, MARGIN_LEFT + 314f + pillW, y + 18.5f)
                fillPaint.color = softBg
                canvas.drawRoundRect(pillRect, 4f, 4f, fillPaint)
                canvas.drawText(truncateText(bucketLbl, pillW - 10f, pillTxtP), pillRect.left + 6f, pillRect.centerY() + 2.3f, pillTxtP)

                val methodLabel = FormatUtils.localizedMethod(txn.method, language)
                canvas.drawText(
                    truncateText(methodLabel, 110f, smallMutedPaint),
                    MARGIN_LEFT + 314f,
                    y + 27.5f,
                    smallMutedPaint
                )

                // Tabular Right-Aligned Amount
                val amtPaint = Paint(rightBoldPaint).apply {
                    color = accentColor
                    textSize = 9.2f
                }
                val sign = if (txn.isIncome) "+" else "-"
                val amtStr = "$sign${FormatUtils.formatMoney(txn.amount, currency)}"
                canvas.drawText(
                    truncateText(amtStr, 118f, amtPaint),
                    CONTENT_RIGHT - 12f,
                    y + 18.8f,
                    amtPaint
                )

                if (!isLastRow) {
                    canvas.drawLine(MARGIN_LEFT + 12f, y + rowH, CONTENT_RIGHT - 12f, y + rowH, hairlinePaint)
                }
                y += rowH
            }

            // Frame the completed ledger container
            canvas.drawRoundRect(RectF(MARGIN_LEFT, tableBlockTop, CONTENT_RIGHT, y), 8f, 8f, warmBorderPaint)
            y += 10f
        }

        // ==================== 6. CLOSING LEDGER TOTALS BAR & EXECUTIVE ATTESTATION ====================
        if (y + 102f > PAGE_HEIGHT - 48f) {
            drawFooter(canvas, pageNumber)
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            drawPaperAndArchitecturalSpine(canvas)

            val runTitle = Paint(heroSerifTitlePaint).apply { textSize = 13.5f }
            canvas.drawText(tr(language, "Muhtasari wa Mwisho", "Closing Summary"), MARGIN_LEFT, 32f, runTitle)
            fillPaint.color = pal.bronze
            canvas.drawRect(MARGIN_LEFT, 40f, MARGIN_LEFT + 44f, 41.8f, fillPaint)
            y = 56f
        }

        // Dark Obsidian Ledger Summary Strip
        val closeRect = RectF(MARGIN_LEFT, y, CONTENT_RIGHT, y + 36f)
        fillPaint.color = pal.obsidian
        canvas.drawRoundRect(closeRect, 8f, 8f, fillPaint)
        fillPaint.color = pal.bronze
        canvas.drawRect(closeRect.left + 12f, closeRect.top, closeRect.left + 68f, closeRect.top + 2f, fillPaint)

        val sumColW = CONTENT_WIDTH / 3f
        val sumLbl = Paint(trackedEyebrowPaint).apply {
            color = pal.bronzeLight
            textSize = 6.7f
            letterSpacing = 0.09f
        }
        val sumVal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.cardIvory
            textSize = 9.4f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val summaryCols = listOf(
            tr(language, "JUMLA YA MAPATO", "TOTAL INFLOWS") to "+${FormatUtils.formatMoney(snapshot.incomeTotal, currency)}",
            tr(language, "JUMLA YA MATOKEO", "TOTAL OUTFLOWS") to "-${FormatUtils.formatMoney(snapshot.totalOutflow, currency)}",
            tr(language, "SALIO HALISI", "NET BALANCE") to FormatUtils.formatMoney(snapshot.netBalance, currency)
        )
        summaryCols.forEachIndexed { idx, (lbl, valStr) ->
            val cx = closeRect.left + 14f + idx * sumColW
            canvas.drawText(lbl, cx, closeRect.top + 13.5f, sumLbl)
            canvas.drawText(truncateText(valStr, sumColW - 20f, sumVal), cx, closeRect.top + 27.5f, sumVal)
        }
        y = closeRect.bottom + 14f

        // Attestation Signature & Portfolio Seal Row
        if (y + 52f <= PAGE_HEIGHT - 44f) {
            val dashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = pal.borderStone
                style = Paint.Style.STROKE
                strokeWidth = 1f
                pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
            }
            val sigY = y + 24f
            canvas.drawLine(MARGIN_LEFT, sigY, MARGIN_LEFT + 195f, sigY, dashedPaint)
            val signer = settings.ownerName.ifBlank { businessName }
            canvas.drawText(truncateText(signer, 195f, bodyBoldPaint), MARGIN_LEFT, sigY + 12.5f, bodyBoldPaint)
            canvas.drawText(
                "${tr(language, "Muidhinishaji", "Authorized Signatory")}  ·  $today",
                MARGIN_LEFT,
                sigY + 23.5f,
                smallMutedPaint
            )

            // Right Rectangular Architectural Seal Stamp
            val sealBox = RectF(CONTENT_RIGHT - 200f, y + 2f, CONTENT_RIGHT, y + 46f)
            fillPaint.color = pal.stoneWarm
            canvas.drawRoundRect(sealBox, 6f, 6f, fillPaint)
            canvas.drawRoundRect(sealBox, 6f, 6f, bronzeBorderPaint)
            fillPaint.color = pal.obsidian
            canvas.drawRoundRect(RectF(sealBox.left + 6f, sealBox.top + 8f, sealBox.left + 9f, sealBox.bottom - 8f), 1.5f, 1.5f, fillPaint)

            val sealHdr = Paint(trackedEyebrowPaint).apply {
                color = pal.sealHeaderInk
                textSize = 6.8f
                letterSpacing = 0.11f
            }
            val sealCode = Paint(bodyBoldPaint).apply {
                color = pal.inkPrimary
                textSize = 8.2f
            }
            canvas.drawText(
                tr(language, "MUHURI WA MFUMO · 0% UPOTEVU", "VERIFIED DOSSIER · 0% LOSS"),
                sealBox.left + 16f,
                sealBox.top + 18f,
                sealHdr
            )
            canvas.drawText(
                "REG ${CopyrightVault.REGISTRATION_CODE}  ·  ${period.name}",
                sealBox.left + 16f,
                sealBox.top + 33f,
                sealCode
            )
        }

        drawFooter(canvas, pageNumber)
        doc.finishPage(page)

        return ByteArrayOutputStream().use { baos ->
            doc.writeTo(baos)
            doc.close()
            baos.toByteArray()
        }
    }

    private fun fitTextSize(text: String, maxWidth: Float, maxSize: Float, minSize: Float): Float {
        var size = maxSize
        val probe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        while (size > minSize) {
            probe.textSize = size
            if (probe.measureText(text) <= maxWidth) return size
            size -= 0.6f
        }
        return minSize
    }

    private fun truncateText(text: String, maxWidth: Float, paint: Paint): String {
        if (maxWidth <= 0f || paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }
}
