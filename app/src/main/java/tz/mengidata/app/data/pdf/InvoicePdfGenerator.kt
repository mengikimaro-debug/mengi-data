package tz.mengidata.app.data.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import tz.mengidata.app.data.db.BusinessSettingsEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.Locale

object InvoicePdfGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2f)

    // Calm Matte Executive Palette (zero glare / zero neon / 100% solid)
    private const val HEX_HEADER_NAVY = "#242E38"
    private const val HEX_TABLE_HEADER = "#2E3A46"
    private const val HEX_ACCENT_SAGE = "#346352"
    private const val HEX_SOFT_CARD = "#F4F6F8"
    private const val HEX_ZEBRA_ROW = "#F8F9FB"
    private const val HEX_BORDER = "#D4DBE2"
    private const val HEX_INK_PRIMARY = "#1A222B"
    private const val HEX_INK_SECONDARY = "#4F5D6B"
    private const val HEX_INK_MUTED = "#687684"
    private const val HEX_HEADER_SUB = "#C8D3DC"

    // Matte Status Badge Palettes
    private const val HEX_PAID_BG = "#E1EDE7"
    private const val HEX_PAID_INK = "#234B3D"
    private const val HEX_PAID_BORDER = "#BBD4C7"

    private const val HEX_PARTIAL_BG = "#F2ECE3"
    private const val HEX_PARTIAL_INK = "#6E5030"
    private const val HEX_PARTIAL_BORDER = "#D9CCB9"

    private const val HEX_OVERDUE_BG = "#EFE7E6"
    private const val HEX_OVERDUE_INK = "#7A3E3A"
    private const val HEX_OVERDUE_BORDER = "#D6BFBD"

    private const val HEX_DUE_BG = "#E6ECF2"
    private const val HEX_DUE_INK = "#2F4860"
    private const val HEX_DUE_BORDER = "#C4D2DF"

    private const val HEX_DRAFT_BG = "#ECEEF0"
    private const val HEX_DRAFT_INK = "#4B5563"
    private const val HEX_DRAFT_BORDER = "#CFD5DC"

    private data class BadgePalette(
        val label: String,
        val bgColor: Int,
        val inkColor: Int,
        val borderColor: Int
    )

    fun generatePdfBytes(
        invoiceWithItems: InvoiceWithItems,
        settings: BusinessSettingsEntity,
        language: AppLanguage = AppLanguage.SW
    ): ByteArray {
        val doc = PdfDocument()
        val inv = invoiceWithItems.invoice
        val currency = settings.currency.ifBlank { "TZS" }
        val businessName = settings.businessName.ifBlank { "Mengidata" }

        // Solid matte paints
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_HEADER_NAVY) }
        val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_TABLE_HEADER) }
        val accentSagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_ACCENT_SAGE) }
        val softCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_SOFT_CARD) }
        val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_ZEBRA_ROW) }
        val whiteFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        val borderStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_BORDER)
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_BORDER)
            strokeWidth = 0.8f
        }

        // Typography paints
        val brandTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 19f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_HEADER_SUB)
            textSize = 9.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val docTypePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val invNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_HEADER_SUB)
            textSize = 10.5f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val sectionEyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_ACCENT_SAGE)
            textSize = 8.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val inkBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_INK_PRIMARY)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val inkRegular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_INK_SECONDARY)
            textSize = 9.4f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_INK_MUTED)
            textSize = 8.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val tableHeaderText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val tableHeaderRight = Paint(tableHeaderText).apply { textAlign = Paint.Align.RIGHT }
        val rightBold = Paint(inkBold).apply { textAlign = Paint.Align.RIGHT }
        val rightRegular = Paint(inkRegular).apply { textAlign = Paint.Align.RIGHT }
        val rightMuted = Paint(mutedPaint).apply { textAlign = Paint.Align.RIGHT }

        val badge = resolveStatusBadge(invoiceWithItems, language)

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        fun drawFooter(c: android.graphics.Canvas, pNum: Int) {
            val footerTop = PAGE_HEIGHT - 38f
            c.drawLine(MARGIN, footerTop, PAGE_WIDTH - MARGIN, footerTop, dividerPaint)
            val leftFooter = listOf(businessName, settings.phone, settings.website)
                .filter { it.isNotBlank() }
                .joinToString("  ·  ")
            c.drawText(leftFooter.take(76), MARGIN, PAGE_HEIGHT - 20f, mutedPaint)
            c.drawText(
                "${tr(language, "Ukurasa", "Page")} $pNum",
                PAGE_WIDTH - MARGIN,
                PAGE_HEIGHT - 20f,
                rightMuted
            )
        }

        // ==================== 1. EXECUTIVE HEADER BAND ====================
        val headerHeight = 122f
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, headerBgPaint)
        canvas.drawRect(0f, headerHeight - 4f, PAGE_WIDTH.toFloat(), headerHeight, accentSagePaint)

        // Left: Business identity
        canvas.drawText(businessName.take(36), MARGIN, 40f, brandTitlePaint)
        var leftY = 56f
        if (settings.tagline.isNotBlank()) {
            canvas.drawText(settings.tagline.take(58), MARGIN, leftY, headerSubPaint)
            leftY += 13f
        }
        if (settings.address.isNotBlank()) {
            canvas.drawText(settings.address.take(58), MARGIN, leftY, headerSubPaint)
            leftY += 13f
        }
        val contactDetails = listOf(
            settings.phone,
            settings.email,
            settings.tin.takeIf { it.isNotBlank() }?.let { "TIN: $it" }
        ).filterNotNull().filter { it.isNotBlank() }.joinToString("  ·  ")
        if (contactDetails.isNotBlank()) {
            canvas.drawText(contactDetails.take(66), MARGIN, leftY, headerSubPaint)
        }

        // Right: Document title, invoice number & matte status badge
        canvas.drawText(tr(language, "ANKARA", "INVOICE"), PAGE_WIDTH - MARGIN, 40f, docTypePaint)
        canvas.drawText(inv.number, PAGE_WIDTH - MARGIN, 58f, invNumberPaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badge.inkColor
            textSize = 8.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val badgeTextWidth = badgeTextPaint.measureText(badge.label)
        val badgeWidth = (badgeTextWidth + 24f).coerceAtLeast(78f)
        val badgeRight = PAGE_WIDTH - MARGIN
        val badgeLeft = badgeRight - badgeWidth
        val badgeRect = RectF(badgeLeft, 68f, badgeRight, 90f)
        canvas.drawRoundRect(
            badgeRect,
            11f,
            11f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badge.bgColor }
        )
        canvas.drawRoundRect(
            badgeRect,
            11f,
            11f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = badge.borderColor
                style = Paint.Style.STROKE
                strokeWidth = 0.9f
            }
        )
        canvas.drawText(badge.label, badgeRect.centerX(), badgeRect.centerY() + 3.2f, badgeTextPaint)

        // ==================== 2. BILL TO & METADATA CARDS ====================
        var y = headerHeight + 20f
        val gap = 14f
        val leftCardWidth = CONTENT_WIDTH * 0.54f
        val rightCardLeft = MARGIN + leftCardWidth + gap
        val rightCardWidth = CONTENT_WIDTH - leftCardWidth - gap

        val customerLines = buildList {
            if (inv.customerPhone.isNotBlank()) add(inv.customerPhone)
            if (inv.customerEmail.isNotBlank()) add(inv.customerEmail)
            if (inv.customerAddress.isNotBlank()) {
                wrapText(inv.customerAddress, inkRegular, leftCardWidth - 28f, maxLines = 2).forEach { add(it) }
            }
        }
        val metaRows = buildList {
            add(tr(language, "Tarehe ya Kutolewa", "Issue Date") to inv.issueDate.ifBlank { "—" })
            add(tr(language, "Tarehe ya Mwisho", "Due Date") to inv.dueDate.ifBlank { "—" })
            if (inv.paidDate.isNotBlank() && inv.amountPaid > 0.005) {
                add(tr(language, "Tarehe ya Malipo", "Paid Date") to inv.paidDate)
            }
            add(tr(language, "Sarafu", "Currency") to currency)
        }

        val leftContentH = 44f + (customerLines.size * 13.5f)
        val rightContentH = 26f + (metaRows.size * 16.5f)
        val infoCardHeight = maxOf(leftContentH, rightContentH, 86f)

        // Left Card: Bill To
        val billToRect = RectF(MARGIN, y, MARGIN + leftCardWidth, y + infoCardHeight)
        canvas.drawRoundRect(billToRect, 8f, 8f, softCardPaint)
        canvas.drawRoundRect(billToRect, 8f, 8f, borderStrokePaint)
        canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + 3.5f, y + infoCardHeight), 2f, 2f, accentSagePaint)

        var cardY = y + 18f
        canvas.drawText(tr(language, "MTEJA", "BILL TO"), MARGIN + 14f, cardY, sectionEyebrowPaint)
        cardY += 16f
        canvas.drawText(inv.customerName.ifBlank { "—" }.take(38), MARGIN + 14f, cardY, inkBold)
        cardY += 14f
        customerLines.forEach { line ->
            canvas.drawText(line, MARGIN + 14f, cardY, inkRegular)
            cardY += 13.5f
        }

        // Right Card: Invoice Metadata
        val metaRect = RectF(rightCardLeft, y, rightCardLeft + rightCardWidth, y + infoCardHeight)
        canvas.drawRoundRect(metaRect, 8f, 8f, softCardPaint)
        canvas.drawRoundRect(metaRect, 8f, 8f, borderStrokePaint)

        var metaY = y + 18f
        canvas.drawText(tr(language, "TAARIFA ZA ANKARA", "INVOICE DETAILS"), rightCardLeft + 14f, metaY, sectionEyebrowPaint)
        metaY += 16f
        metaRows.forEach { (label, value) ->
            canvas.drawText(label, rightCardLeft + 14f, metaY, mutedPaint)
            canvas.drawText(value, rightCardLeft + rightCardWidth - 14f, metaY, rightBold)
            metaY += 16.5f
        }

        y += infoCardHeight + 20f

        // ==================== 3. LINE ITEMS TABLE ====================
        val colNumX = MARGIN + 10f
        val colDescX = MARGIN + 30f
        val descMaxWidth = 278f
        val colQtyRight = 382f
        val colPriceRight = 466f
        val colTotalRight = PAGE_WIDTH - MARGIN - 10f

        fun drawTableHeader(c: android.graphics.Canvas, topY: Float): Float {
            val headerRect = RectF(MARGIN, topY, PAGE_WIDTH - MARGIN, topY + 26f)
            c.drawRoundRect(headerRect, 6f, 6f, tableHeaderBgPaint)
            val textY = topY + 16.5f
            c.drawText("#", colNumX, textY, tableHeaderText)
            c.drawText(tr(language, "MAELEZO YA BIDHAA / HUDUMA", "ITEM DESCRIPTION"), colDescX, textY, tableHeaderText)
            c.drawText(tr(language, "IDADI", "QTY"), colQtyRight, textY, tableHeaderRight)
            c.drawText(tr(language, "BEI", "UNIT PRICE"), colPriceRight, textY, tableHeaderRight)
            c.drawText(tr(language, "JUMLA", "AMOUNT"), colTotalRight, textY, tableHeaderRight)
            return topY + 30f
        }

        fun drawContinuationHeader(c: android.graphics.Canvas): Float {
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 44f, headerBgPaint)
            c.drawRect(0f, 41f, PAGE_WIDTH.toFloat(), 44f, accentSagePaint)
            val contTitle = Paint(brandTitlePaint).apply { textSize = 12f }
            val contRight = Paint(invNumberPaint).apply { textSize = 10f }
            c.drawText(businessName.take(40), MARGIN, 27f, contTitle)
            c.drawText("${tr(language, "ANKARA", "INVOICE")} ${inv.number}", PAGE_WIDTH - MARGIN, 27f, contRight)
            return drawTableHeader(c, 58f)
        }

        y = drawTableHeader(canvas, y)

        invoiceWithItems.orderedItems.forEachIndexed { idx, item ->
            val descLines = wrapText(item.description.ifBlank { "—" }, inkRegular, descMaxWidth, maxLines = 3)
            val rowHeight = maxOf(26f, 12f + (descLines.size * 13.5f))

            if (y + rowHeight > PAGE_HEIGHT - 130f) {
                drawFooter(canvas, pageNumber)
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = drawContinuationHeader(canvas)
            }

            // Zebra striping
            val rowRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight)
            canvas.drawRect(rowRect, if (idx % 2 == 1) zebraPaint else whiteFillPaint)

            val firstLineY = y + 16f
            canvas.drawText("${idx + 1}", colNumX, firstLineY, mutedPaint)
            descLines.forEachIndexed { lineIdx, line ->
                canvas.drawText(line, colDescX, firstLineY + (lineIdx * 13f), inkRegular)
            }
            canvas.drawText(formatQty(item.qty), colQtyRight, firstLineY, rightRegular)
            canvas.drawText(FormatUtils.formatMoney(item.price, ""), colPriceRight, firstLineY, rightRegular)
            canvas.drawText(FormatUtils.formatMoney(item.lineTotal, ""), colTotalRight, firstLineY, rightBold)

            canvas.drawLine(MARGIN, y + rowHeight, PAGE_WIDTH - MARGIN, y + rowHeight, dividerPaint)
            y += rowHeight
        }

        // ==================== 4. BOTTOM SUMMARY & PAYMENT DETAILS ====================
        y += 18f
        val neededBottomHeight = 195f
        if (y + neededBottomHeight > PAGE_HEIGHT - 44f) {
            drawFooter(canvas, pageNumber)
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = 58f
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 44f, headerBgPaint)
            canvas.drawRect(0f, 41f, PAGE_WIDTH.toFloat(), 44f, accentSagePaint)
            canvas.drawText(businessName.take(40), MARGIN, 27f, Paint(brandTitlePaint).apply { textSize = 12f })
            canvas.drawText("${tr(language, "ANKARA", "INVOICE")} ${inv.number}", PAGE_WIDTH - MARGIN, 27f, Paint(invNumberPaint).apply { textSize = 10f })
        }

        val summaryTopY = y
        val leftBoxWidth = CONTENT_WIDTH * 0.48f
        val totalsLeft = MARGIN + leftBoxWidth + 16f
        val totalsWidth = CONTENT_WIDTH - leftBoxWidth - 16f

        // Right Column: Financial Summary Card
        var sumY = summaryTopY + 18f
        val summaryLinesCount = 2 +
            (if (invoiceWithItems.effectiveDiscount > 0.0) 1 else 0) +
            (if (inv.vatRate > 0.0) 1 else 0)
        val summaryCardH = 22f + (summaryLinesCount * 18f) + 36f + 42f
        val summaryRect = RectF(totalsLeft, summaryTopY, totalsLeft + totalsWidth, summaryTopY + summaryCardH)
        canvas.drawRoundRect(summaryRect, 8f, 8f, softCardPaint)
        canvas.drawRoundRect(summaryRect, 8f, 8f, borderStrokePaint)

        fun drawSummaryRow(label: String, value: String, bold: Boolean = false) {
            canvas.drawText(label, totalsLeft + 14f, sumY, if (bold) inkBold else mutedPaint)
            canvas.drawText(value, totalsLeft + totalsWidth - 14f, sumY, if (bold) rightBold else rightRegular)
            sumY += 18f
        }

        drawSummaryRow(tr(language, "Jumla Ndogo", "Subtotal"), FormatUtils.formatMoney(invoiceWithItems.subtotal, currency))
        if (invoiceWithItems.effectiveDiscount > 0.0) {
            drawSummaryRow(
                tr(language, "Punguzo", "Discount"),
                "-${FormatUtils.formatMoney(invoiceWithItems.effectiveDiscount, currency)}"
            )
        }
        if (inv.vatRate > 0.0) {
            val vatLabel = "${tr(language, "Kodi (VAT)", "VAT")} (${formatQty(inv.vatRate)}%)"
            drawSummaryRow(vatLabel, FormatUtils.formatMoney(invoiceWithItems.vatAmount, currency))
        }

        // Grand Total Matte Slate Bar
        sumY += 2f
        val grandRect = RectF(totalsLeft + 10f, sumY - 12f, totalsLeft + totalsWidth - 10f, sumY + 14f)
        canvas.drawRoundRect(grandRect, 6f, 6f, headerBgPaint)
        val totalLabelWhite = Paint(inkBold).apply { color = Color.WHITE; textSize = 9.8f }
        val totalValueWhite = Paint(rightBold).apply { color = Color.WHITE; textSize = 10.6f }
        canvas.drawText(tr(language, "JUMLA KUU", "GRAND TOTAL"), grandRect.left + 10f, sumY + 4.5f, totalLabelWhite)
        canvas.drawText(FormatUtils.formatMoney(invoiceWithItems.total, currency), grandRect.right - 10f, sumY + 4.5f, totalValueWhite)
        sumY += 28f

        drawSummaryRow(tr(language, "Imelipwa", "Amount Paid"), FormatUtils.formatMoney(inv.amountPaid, currency))

        // Balance Due Matte Highlight Bar
        val balBgColor = if (invoiceWithItems.balance <= 0.005) Color.parseColor(HEX_PAID_BG) else Color.parseColor(HEX_PARTIAL_BG)
        val balInkColor = if (invoiceWithItems.balance <= 0.005) Color.parseColor(HEX_PAID_INK) else Color.parseColor(HEX_PARTIAL_INK)
        val balRect = RectF(totalsLeft + 10f, sumY - 11f, totalsLeft + totalsWidth - 10f, sumY + 13f)
        canvas.drawRoundRect(balRect, 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = balBgColor })
        val balLabelPaint = Paint(inkBold).apply { color = balInkColor; textSize = 9.6f }
        val balValPaint = Paint(rightBold).apply { color = balInkColor; textSize = 10.2f }
        canvas.drawText(
            if (invoiceWithItems.balance <= 0.005) {
                tr(language, "SALIO (IMELIPWA)", "BALANCE (SETTLED)")
            } else {
                tr(language, "SALIO LINALODAIWA", "BALANCE DUE")
            },
            balRect.left + 10f,
            sumY + 4f,
            balLabelPaint
        )
        canvas.drawText(
            FormatUtils.formatMoney(invoiceWithItems.balance, currency),
            balRect.right - 10f,
            sumY + 4f,
            balValPaint
        )

        // Left Column: Payment Instructions & Notes Card
        val paymentLines = settings.paymentDetails
            .lines()
            .filter { it.isNotBlank() }
            .flatMap { wrapText(it.trim(), inkRegular, leftBoxWidth - 28f, maxLines = 2) }
            .take(5)
        val footerNote = inv.notes.ifBlank { settings.invoiceNote }.trim()
        val noteLines = if (footerNote.isNotBlank()) {
            wrapText(footerNote, mutedPaint, leftBoxWidth - 28f, maxLines = 3)
        } else {
            emptyList()
        }

        if (paymentLines.isNotEmpty() || noteLines.isNotEmpty()) {
            val leftRect = RectF(MARGIN, summaryTopY, MARGIN + leftBoxWidth, summaryTopY + summaryCardH)
            canvas.drawRoundRect(leftRect, 8f, 8f, softCardPaint)
            canvas.drawRoundRect(leftRect, 8f, 8f, borderStrokePaint)
            canvas.drawRoundRect(RectF(MARGIN, summaryTopY, MARGIN + 3.5f, summaryTopY + summaryCardH), 2f, 2f, accentSagePaint)

            var payY = summaryTopY + 18f
            if (paymentLines.isNotEmpty()) {
                canvas.drawText(tr(language, "MAELEZO YA MALIPO", "PAYMENT DETAILS"), MARGIN + 14f, payY, sectionEyebrowPaint)
                payY += 15f
                paymentLines.forEach { line ->
                    canvas.drawText(line, MARGIN + 14f, payY, inkRegular)
                    payY += 13f
                }
                payY += 6f
            }
            if (noteLines.isNotEmpty()) {
                canvas.drawText(tr(language, "MAELEZO", "NOTES"), MARGIN + 14f, payY, sectionEyebrowPaint)
                payY += 14f
                noteLines.forEach { line ->
                    canvas.drawText(line, MARGIN + 14f, payY, mutedPaint)
                    payY += 12.5f
                }
            }
        }

        drawFooter(canvas, pageNumber)
        doc.finishPage(page)

        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun resolveStatusBadge(item: InvoiceWithItems, language: AppLanguage): BadgePalette = when {
        item.isPaid -> BadgePalette(
            label = tr(language, "IMELIPWA", "PAID"),
            bgColor = Color.parseColor(HEX_PAID_BG),
            inkColor = Color.parseColor(HEX_PAID_INK),
            borderColor = Color.parseColor(HEX_PAID_BORDER)
        )
        item.isDraft -> BadgePalette(
            label = tr(language, "RASIMU", "DRAFT"),
            bgColor = Color.parseColor(HEX_DRAFT_BG),
            inkColor = Color.parseColor(HEX_DRAFT_INK),
            borderColor = Color.parseColor(HEX_DRAFT_BORDER)
        )
        item.isOverdue -> BadgePalette(
            label = tr(language, "IMECHELEWA", "OVERDUE"),
            bgColor = Color.parseColor(HEX_OVERDUE_BG),
            inkColor = Color.parseColor(HEX_OVERDUE_INK),
            borderColor = Color.parseColor(HEX_OVERDUE_BORDER)
        )
        item.isPartial -> BadgePalette(
            label = tr(language, "AWAMU", "PARTIAL"),
            bgColor = Color.parseColor(HEX_PARTIAL_BG),
            inkColor = Color.parseColor(HEX_PARTIAL_INK),
            borderColor = Color.parseColor(HEX_PARTIAL_BORDER)
        )
        else -> BadgePalette(
            label = tr(language, "INADAIWA", "DUE"),
            bgColor = Color.parseColor(HEX_DUE_BG),
            inkColor = Color.parseColor(HEX_DUE_INK),
            borderColor = Color.parseColor(HEX_DUE_BORDER)
        )
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float, maxLines: Int = 3): List<String> {
        val clean = text.trim()
        if (clean.isEmpty()) return emptyList()
        val words = clean.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "${current} $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) {
                    lines += current.toString()
                    if (lines.size >= maxLines) break
                    current = StringBuilder(word)
                } else {
                    lines += trimToWidth(word, paint, maxWidth)
                    if (lines.size >= maxLines) break
                    current = StringBuilder()
                }
            }
        }
        if (current.isNotEmpty() && lines.size < maxLines) {
            lines += trimToWidth(current.toString(), paint, maxWidth)
        }
        return lines
    }

    private fun trimToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }

    fun printInvoicePdf(context: Context, title: String, pdfBytes: ByteArray): Boolean {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false
        printManager.print(
            title,
            object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder("$title.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback
                ) {
                    try {
                        FileOutputStream(destination.fileDescriptor).use { it.write(pdfBytes) }
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback.onWriteFailed(e.message)
                    }
                }
            },
            null
        )
        return true
    }

    private fun formatQty(qty: Double): String =
        if (qty % 1.0 == 0.0) qty.toInt().toString() else String.format(Locale.US, "%.2f", qty)
}
