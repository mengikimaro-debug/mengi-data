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
import tz.mengidata.app.i18n.FormatUtils
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

object InvoicePdfGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun generatePdfBytes(
        invoiceWithItems: InvoiceWithItems,
        settings: BusinessSettingsEntity
    ): ByteArray {
        val doc = PdfDocument()
        val inv = invoiceWithItems.invoice
        val currency = settings.currency.ifBlank { "TZS" }

        val forestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0C2B22") }
        val limePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D8F36B") }
        val softFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F7F5EF") }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2DCCD")
            strokeWidth = 1f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val subWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C7DACF")
            textSize = 10f
        }
        val inkBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1F17")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val inkRegular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#243D31")
            textSize = 10f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5C7265")
            textSize = 9.5f
        }
        val rightBold = Paint(inkBold).apply { textAlign = Paint.Align.RIGHT }
        val rightRegular = Paint(inkRegular).apply { textAlign = Paint.Align.RIGHT }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        // Header banner
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 112f, forestPaint)
        canvas.drawRect(0f, 108f, PAGE_WIDTH.toFloat(), 112f, limePaint)

        canvas.drawText(settings.businessName.ifBlank { "Mengidata" }, MARGIN, 46f, titlePaint)
        var headerY = 64f
        if (settings.tagline.isNotBlank()) {
            canvas.drawText(settings.tagline.take(64), MARGIN, headerY, subWhitePaint)
            headerY += 14f
        }
        val contactLine = listOf(settings.phone, settings.email, settings.tin.takeIf { it.isNotBlank() }?.let { "TIN: $it" })
            .filterNotNull()
            .filter { it.isNotBlank() }
            .joinToString("  •  ")
        if (contactLine.isNotBlank()) {
            canvas.drawText(contactLine.take(78), MARGIN, headerY, subWhitePaint)
        }

        val invBadgeTitle = Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 18f }
        val invBadgeSub = Paint(subWhitePaint).apply { textAlign = Paint.Align.RIGHT; color = Color.parseColor("#D8F36B") }
        canvas.drawText("ANKARA / INVOICE", PAGE_WIDTH - MARGIN, 46f, invBadgeTitle)
        canvas.drawText(inv.number, PAGE_WIDTH - MARGIN, 66f, invBadgeSub)
        canvas.drawText(
            if (invoiceWithItems.isPaid) "IMELIPWA / PAID" else if (invoiceWithItems.isDraft) "RASIMU / DRAFT" else "INADAIWA / DUE",
            PAGE_WIDTH - MARGIN,
            84f,
            invBadgeSub
        )

        var y = 138f
        // Bill To & Dates block
        canvas.drawText("Mteja / Bill To:", MARGIN, y, mutedPaint)
        canvas.drawText("Tarehe / Issue Date:", 360f, y, mutedPaint)
        canvas.drawText(inv.issueDate, PAGE_WIDTH - MARGIN, y, rightBold)
        y += 16f

        canvas.drawText(inv.customerName.ifBlank { "—" }, MARGIN, y, inkBold)
        canvas.drawText("Mwisho / Due Date:", 360f, y, mutedPaint)
        canvas.drawText(inv.dueDate, PAGE_WIDTH - MARGIN, y, rightBold)
        y += 14f

        if (inv.customerPhone.isNotBlank()) {
            canvas.drawText(inv.customerPhone, MARGIN, y, inkRegular)
            y += 13f
        }
        if (inv.customerEmail.isNotBlank()) {
            canvas.drawText(inv.customerEmail, MARGIN, y, inkRegular)
            y += 13f
        }
        if (inv.customerAddress.isNotBlank()) {
            canvas.drawText(inv.customerAddress.take(65), MARGIN, y, inkRegular)
            y += 13f
        }

        y = maxOf(y + 12f, 206f)

        // Table Header
        fun drawTableHeader(topY: Float): Float {
            canvas.drawRoundRect(RectF(MARGIN, topY, PAGE_WIDTH - MARGIN, topY + 24f), 6f, 6f, softFillPaint)
            canvas.drawText("#", MARGIN + 8f, topY + 15f, inkBold)
            canvas.drawText("Maelezo / Description", MARGIN + 30f, topY + 15f, inkBold)
            canvas.drawText("Idadi", 375f, topY + 15f, rightBold)
            canvas.drawText("Bei", 460f, topY + 15f, rightBold)
            canvas.drawText("Jumla", PAGE_WIDTH - MARGIN - 8f, topY + 15f, rightBold)
            return topY + 34f
        }

        y = drawTableHeader(y)

        invoiceWithItems.orderedItems.forEachIndexed { idx, item ->
            if (y > PAGE_HEIGHT - 160f) {
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = drawTableHeader(MARGIN)
            }
            canvas.drawText("${idx + 1}", MARGIN + 8f, y, mutedPaint)
            canvas.drawText(item.description.take(46), MARGIN + 30f, y, inkRegular)
            canvas.drawText(formatQty(item.qty), 375f, y, rightRegular)
            canvas.drawText(FormatUtils.formatMoney(item.price, ""), 460f, y, rightRegular)
            canvas.drawText(FormatUtils.formatMoney(item.lineTotal, ""), PAGE_WIDTH - MARGIN - 8f, y, rightBold)
            canvas.drawLine(MARGIN, y + 8f, PAGE_WIDTH - MARGIN, y + 8f, linePaint)
            y += 24f
        }

        y += 12f
        if (y > PAGE_HEIGHT - 210f) {
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN + 10f
        }

        // Totals Card
        val totalsLeft = 325f
        fun drawTotalRow(label: String, value: String, bold: Boolean = false) {
            canvas.drawText(label, totalsLeft, y, if (bold) inkBold else mutedPaint)
            canvas.drawText(value, PAGE_WIDTH - MARGIN - 6f, y, if (bold) rightBold else rightRegular)
            y += 18f
        }

        drawTotalRow("Jumla ndogo / Subtotal", FormatUtils.formatMoney(invoiceWithItems.subtotal, currency))
        if (invoiceWithItems.effectiveDiscount > 0.0) {
            drawTotalRow("Punguzo / Discount", "-${FormatUtils.formatMoney(invoiceWithItems.effectiveDiscount, currency)}")
        }
        if (inv.vatRate > 0.0) {
            drawTotalRow("VAT (${inv.vatRate}%)", FormatUtils.formatMoney(invoiceWithItems.vatAmount, currency))
        }

        // Grand Total Pill
        canvas.drawRoundRect(RectF(totalsLeft - 10f, y - 13f, PAGE_WIDTH - MARGIN, y + 12f), 6f, 6f, forestPaint)
        val totalWhite = Paint(inkBold).apply { color = Color.WHITE; textSize = 11.5f }
        val totalLime = Paint(rightBold).apply { color = Color.parseColor("#D8F36B"); textSize = 11.5f }
        canvas.drawText("JUMLA KUU / TOTAL", totalsLeft, y + 3f, totalWhite)
        canvas.drawText(FormatUtils.formatMoney(invoiceWithItems.total, currency), PAGE_WIDTH - MARGIN - 8f, y + 3f, totalLime)
        y += 28f

        drawTotalRow("Imelipwa / Paid", FormatUtils.formatMoney(inv.amountPaid, currency))
        drawTotalRow("Salio / Balance Due", FormatUtils.formatMoney(invoiceWithItems.balance, currency), bold = true)

        // Payment instructions & Footer note
        y += 18f
        if (settings.paymentDetails.isNotBlank()) {
            canvas.drawText("Maelezo ya Malipo / Payment Details:", MARGIN, y, inkBold)
            y += 14f
            settings.paymentDetails.lines().take(4).forEach { line ->
                canvas.drawText(line.take(86), MARGIN, y, inkRegular)
                y += 13f
            }
            y += 6f
        }
        val footerNote = inv.notes.ifBlank { settings.invoiceNote }
        if (footerNote.isNotBlank()) {
            canvas.drawText(footerNote.take(92), MARGIN, y, mutedPaint)
        }

        canvas.drawLine(MARGIN, PAGE_HEIGHT - 36f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 36f, linePaint)
        canvas.drawText("Imeandaliwa na Mengidata", MARGIN, PAGE_HEIGHT - 20f, mutedPaint)
        canvas.drawText("Ukurasa $pageNumber", PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 20f, Paint(mutedPaint).apply { textAlign = Paint.Align.RIGHT })

        doc.finishPage(page)

        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
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
        if (qty % 1.0 == 0.0) qty.toInt().toString() else String.format(java.util.Locale.US, "%.2f", qty)
}
