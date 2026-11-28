package tz.mengidata.app.data.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
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
import tz.mengidata.app.security.CopyrightVault
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.roundToLong

object InvoicePdfGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2f)

    // 2026 Bespoke Luxury Corporate Palette (Midnight Obsidian + Matte Indigo-Navy + Champagne Gold)
    private const val HEX_NAVY_DEEP = "#0E1821"
    private const val HEX_SLATE_MID = "#192B38"
    private const val HEX_EMERALD_DEEP = "#1D2B3A"
    private const val HEX_EMERALD = "#25374A"
    private const val HEX_EMERALD_SOFT = "#F0F4F8"
    private const val HEX_EMERALD_BORDER = "#C6D3E1"
    private const val HEX_GOLD = "#C8A66B"
    private const val HEX_GOLD_SOFT = "#FAF7F0"
    private const val HEX_GOLD_BORDER = "#E7DAC1"
    private const val HEX_GOLD_INK = "#7E6232"
    private const val HEX_SURFACE = "#F6F8FA"
    private const val HEX_ZEBRA = "#F9FBFD"
    private const val HEX_ITEMS_ROW_EVEN = "#EEF2F7"
    private const val HEX_ITEMS_ROW_ODD = "#E4EBF3"
    private const val HEX_ITEMS_DIVIDER = "#C8D5E3"
    private const val HEX_ITEMS_BORDER = "#B3C3D4"
    private const val HEX_BORDER = "#D8E1E8"
    private const val HEX_HAIRLINE = "#E8EEF3"
    private const val HEX_INK_PRIMARY = "#101820"
    private const val HEX_INK_BODY = "#374655"
    private const val HEX_INK_MUTED = "#677786"
    private const val HEX_HEADER_SUB = "#C7D4DF"

    // Status Badge Palettes (Zero-Green Matte Royal Slate-Blue for Paid)
    private const val HEX_PAID_BG = "#DCE5F0"
    private const val HEX_PAID_INK = "#1D3550"
    private const val HEX_PAID_DOT = "#2C4C70"

    private const val HEX_PARTIAL_BG = "#FDF0DF"
    private const val HEX_PARTIAL_INK = "#7E4E18"
    private const val HEX_PARTIAL_DOT = "#C07A28"

    private const val HEX_OVERDUE_BG = "#FCE8E7"
    private const val HEX_OVERDUE_INK = "#8E2B26"
    private const val HEX_OVERDUE_DOT = "#C9423B"

    private const val HEX_DUE_BG = "#E6F0FA"
    private const val HEX_DUE_INK = "#1E4B75"
    private const val HEX_DUE_DOT = "#2D71AF"

    private const val HEX_DRAFT_BG = "#EDF1F5"
    private const val HEX_DRAFT_INK = "#485664"
    private const val HEX_DRAFT_DOT = "#738394"

    private data class BadgePalette(
        val label: String,
        val bgColor: Int,
        val inkColor: Int,
        val dotColor: Int
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
        val badge = resolveStatusBadge(invoiceWithItems, language)
        val isSettled = invoiceWithItems.isPaid || invoiceWithItems.balance <= 0.005

        // Core Paints
        val navyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_NAVY_DEEP) }
        val slatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_SLATE_MID) }
        val emeraldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_EMERALD) }
        val emeraldSoftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_EMERALD_SOFT) }
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_GOLD) }
        val goldSoftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_GOLD_SOFT) }
        val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_SURFACE) }
        val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_ZEBRA) }
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_BORDER)
            style = Paint.Style.STROKE
            strokeWidth = 0.85f
        }
        val emeraldBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_EMERALD_BORDER)
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }
        val goldBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_GOLD_BORDER)
            style = Paint.Style.STROKE
            strokeWidth = 0.85f
        }
        val hairlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_HAIRLINE)
            strokeWidth = 0.8f
        }
        val strongDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_BORDER)
            strokeWidth = 1.1f
        }

        // Typography Paints
        val brandWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val taglineGoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E3CFA8")
            textSize = 8.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val headerMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_HEADER_SUB)
            textSize = 8.4f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val docEyebrowRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E3CFA8")
            textSize = 7.4f
            letterSpacing = 0.16f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val docHeroRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 25f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val invNumRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8E6EE")
            textSize = 10.6f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_EMERALD)
            textSize = 7.5f
            letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val inkBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_INK_PRIMARY)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val inkBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_INK_BODY)
            textSize = 9.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val inkMuted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_INK_MUTED)
            textSize = 8.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.2f
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val tableHeaderRight = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightBold = Paint(inkBold).apply { textAlign = Paint.Align.RIGHT }
        val rightBody = Paint(inkBody).apply { textAlign = Paint.Align.RIGHT }
        val rightMuted = Paint(inkMuted).apply { textAlign = Paint.Align.RIGHT }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        fun drawSoftCardShadow(c: Canvas, rect: RectF, radius: Float) {
            val s1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(12, 14, 24, 33) }
            val s2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(7, 14, 24, 33) }
            c.drawRoundRect(RectF(rect.left + 1f, rect.top + 3f, rect.right + 1f, rect.bottom + 4.5f), radius + 1f, radius + 1f, s2)
            c.drawRoundRect(RectF(rect.left + 0.5f, rect.top + 1.5f, rect.right + 0.5f, rect.bottom + 2.5f), radius, radius, s1)
        }

        fun drawWatermark(c: Canvas) {
            val watermarkText = when {
                isSettled -> tr(language, "IMELIPWA", "PAID")
                invoiceWithItems.isDraft -> tr(language, "RASIMU", "DRAFT")
                else -> null
            } ?: return
            c.save()
            c.rotate(-26f, PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.54f)
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isSettled) Color.parseColor(HEX_EMERALD) else Color.parseColor(HEX_INK_MUTED)
                alpha = 14
                textSize = 86f
                letterSpacing = 0.16f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            c.drawText(watermarkText, PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.56f, wmPaint)
            c.restore()
        }

        fun drawFooter(c: Canvas, pNum: Int) {
            val footerTop = PAGE_HEIGHT - 44f
            // Slim emerald + gold footer rule
            c.drawLine(MARGIN, footerTop, PAGE_WIDTH - MARGIN, footerTop, hairlinePaint)
            c.drawRect(MARGIN, footerTop - 0.6f, MARGIN + 56f, footerTop + 1.2f, goldPaint)

            val thankYou = settings.invoiceNote.trim().ifBlank {
                tr(language, "Asante kwa kutuamini na kufanya biashara nasi.", "Thank you for your trust and business.")
            }
            val contactLine = listOf(
                businessName,
                settings.phone,
                settings.email,
                settings.website,
                CopyrightVault.pdfFooterBrand(language)
            ).filter { it.isNotBlank() }.joinToString("  •  ")

            val thankPaint = Paint(inkBold).apply {
                textSize = 8.4f
                color = Color.parseColor(HEX_EMERALD)
            }
            c.drawText(trimToWidth(thankYou, thankPaint, CONTENT_WIDTH - 86f), MARGIN, footerTop + 15f, thankPaint)
            c.drawText(trimToWidth(contactLine, inkMuted, CONTENT_WIDTH - 86f), MARGIN, footerTop + 28f, inkMuted)

            // Crisp Page Badge Pill
            val pageText = "${tr(language, "Ukurasa", "Page")} $pNum"
            val pPillW = inkMuted.measureText(pageText) + 18f
            val pPillRect = RectF(PAGE_WIDTH - MARGIN - pPillW, footerTop + 10f, PAGE_WIDTH - MARGIN, footerTop + 28f)
            c.drawRoundRect(pPillRect, 9f, 9f, surfacePaint)
            c.drawRoundRect(pPillRect, 9f, 9f, borderPaint)
            val pTextPaint = Paint(inkMuted).apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            c.drawText(pageText, pPillRect.centerX(), pPillRect.centerY() + 2.8f, pTextPaint)
        }

        // ==================== 1. LUXURY GRADIENT EXECUTIVE HEADER ====================
        val headerHeight = 148f
        val headerGradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, PAGE_WIDTH.toFloat(), headerHeight,
                intArrayOf(
                    Color.parseColor(HEX_NAVY_DEEP),
                    Color.parseColor(HEX_SLATE_MID),
                    Color.parseColor(HEX_EMERALD_DEEP)
                ),
                floatArrayOf(0f, 0.56f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, headerGradPaint)

        // Architectural geometric graphic accents clipped cleanly inside header
        canvas.save()
        canvas.clipRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight)
        val geoRing1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 13
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val geoFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_GOLD)
            alpha = 18
        }
        canvas.drawCircle(PAGE_WIDTH - 52f, 22f, 96f, geoRing1)
        canvas.drawCircle(PAGE_WIDTH - 52f, 22f, 64f, geoRing1)
        canvas.drawCircle(PAGE_WIDTH - 12f, headerHeight + 18f, 78f, geoFill)
        // Subtle diagonal sheen band
        val sheenPath = Path().apply {
            moveTo(PAGE_WIDTH * 0.56f, 0f)
            lineTo(PAGE_WIDTH * 0.72f, 0f)
            lineTo(PAGE_WIDTH * 0.58f, headerHeight)
            lineTo(PAGE_WIDTH * 0.42f, headerHeight)
            close()
        }
        canvas.drawPath(sheenPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 8 })
        canvas.restore()

        // Champagne Gold Bottom Trim Line
        canvas.drawRect(0f, headerHeight - 3f, PAGE_WIDTH.toFloat(), headerHeight, goldPaint)

        // Left: Luxury Monogram Crest + Business Identity
        val crestTop = 24f
        val crestSize = 48f
        val crestRect = RectF(MARGIN, crestTop, MARGIN + crestSize, crestTop + crestSize)
        canvas.drawRoundRect(crestRect, 12f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1D3342") })
        canvas.drawRoundRect(
            crestRect,
            12f,
            12f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(HEX_GOLD)
                style = Paint.Style.STROKE
                strokeWidth = 1.3f
            }
        )
        // Inner decorative corner bar inside crest
        canvas.drawRoundRect(
            RectF(crestRect.left + 11f, crestRect.bottom - 7.5f, crestRect.right - 11f, crestRect.bottom - 5f),
            1.2f,
            1.2f,
            goldPaint
        )
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(extractMonogram(businessName), crestRect.centerX(), crestRect.centerY() + 4.5f, monoPaint)

        val brandX = MARGIN + crestSize + 14f
        val brandMaxW = CONTENT_WIDTH * 0.54f
        canvas.drawText(trimToWidth(businessName, brandWhitePaint, brandMaxW), brandX, crestTop + 18f, brandWhitePaint)
        var hy = crestTop + 32f
        if (settings.tagline.isNotBlank()) {
            canvas.drawText(trimToWidth(settings.tagline, taglineGoldPaint, brandMaxW), brandX, hy, taglineGoldPaint)
            hy += 14f
        }

        // Contact lines with custom mini vector icons
        val contactRowItems = buildList {
            if (settings.phone.isNotBlank()) add("phone" to settings.phone.trim())
            if (settings.email.isNotBlank()) add("mail" to settings.email.trim())
        }
        if (contactRowItems.isNotEmpty()) {
            var cx = brandX
            contactRowItems.forEachIndexed { idx, (kind, text) ->
                if (idx > 0) cx += 10f
                drawMiniIcon(canvas, kind, cx, hy - 6.5f, Color.parseColor(HEX_GOLD))
                cx += 11f
                val trimmed = trimToWidth(text, headerMetaPaint, (brandX + brandMaxW - cx).coerceAtLeast(60f))
                canvas.drawText(trimmed, cx, hy, headerMetaPaint)
                cx += headerMetaPaint.measureText(trimmed)
            }
            hy += 13.5f
        }
        if (settings.address.isNotBlank()) {
            drawMiniIcon(canvas, "pin", brandX, hy - 6.5f, Color.parseColor(HEX_GOLD))
            canvas.drawText(trimToWidth(settings.address.trim(), headerMetaPaint, brandMaxW - 14f), brandX + 11f, hy, headerMetaPaint)
            hy += 13.5f
        }
        if (settings.tin.isNotBlank()) {
            val tinText = "TIN: ${settings.tin.trim()}"
            val tinTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E3CFA8")
                textSize = 7.6f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val tinW = tinTextPaint.measureText(tinText) + 14f
            val tinRect = RectF(brandX, hy - 8.5f, brandX + tinW, hy + 5f)
            canvas.drawRoundRect(tinRect, 4f, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(40, 255, 255, 255) })
            canvas.drawText(tinText, tinRect.left + 7f, tinRect.centerY() + 2.6f, tinTextPaint)
        }

        // Right: Official Document Identity + Status Pill
        canvas.drawText(
            tr(language, "ANKARA RASMI YA BIASHARA", "OFFICIAL BUSINESS INVOICE"),
            PAGE_WIDTH - MARGIN,
            crestTop + 10f,
            docEyebrowRight
        )
        canvas.drawText(
            tr(language, "ANKARA", "INVOICE"),
            PAGE_WIDTH - MARGIN,
            crestTop + 37f,
            docHeroRight
        )
        canvas.drawText(
            "#${inv.number}",
            PAGE_WIDTH - MARGIN,
            crestTop + 54f,
            invNumRight
        )

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badge.inkColor
            textSize = 8.4f
            letterSpacing = 0.06f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val pillW = (badgeTextPaint.measureText(badge.label) + 30f).coerceAtLeast(80f)
        val pillRect = RectF(PAGE_WIDTH - MARGIN - pillW, crestTop + 63f, PAGE_WIDTH - MARGIN, crestTop + 84f)
        canvas.drawRoundRect(pillRect, 10.5f, 10.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badge.bgColor })
        canvas.drawCircle(pillRect.left + 11.5f, pillRect.centerY(), 3.2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badge.dotColor })
        canvas.drawText(badge.label, pillRect.left + 19.5f, pillRect.centerY() + 3f, badgeTextPaint)

        // ==================== 2. CLEAN 2-COLUMN CLIENT & INVOICE DETAILS CARD (NO MONEY AT TOP) ====================
        val heroCardTop = headerHeight - 18f
        val leftColCardW = CONTENT_WIDTH * 0.58f
        val rightColCardW = CONTENT_WIDTH - leftColCardW

        val clientContactLines = buildList {
            if (inv.customerPhone.isNotBlank()) add("phone" to inv.customerPhone.trim())
            if (inv.customerEmail.isNotBlank()) add("mail" to inv.customerEmail.trim())
            if (inv.customerAddress.isNotBlank()) {
                wrapText(inv.customerAddress.trim(), inkBody, leftColCardW - 42f, maxLines = 2).forEachIndexed { i, line ->
                    add((if (i == 0) "pin" else "none") to line)
                }
            }
        }
        val heroCardHeight = maxOf(102f, 52f + (clientContactLines.size * 14f))
        val heroCardRect = RectF(MARGIN, heroCardTop, PAGE_WIDTH - MARGIN, heroCardTop + heroCardHeight)

        drawSoftCardShadow(canvas, heroCardRect, 12f)
        canvas.drawRoundRect(heroCardRect, 12f, 12f, whitePaint)

        // Subtle tinted right panel for Invoice Details (clean, airy, zero money at top)
        val detailsPanelLeft = heroCardRect.left + leftColCardW
        val detailsPanelRect = RectF(detailsPanelLeft, heroCardRect.top, heroCardRect.right, heroCardRect.bottom)
        val detailsPanelPath = Path().apply {
            addRoundRect(detailsPanelRect, floatArrayOf(0f, 0f, 12f, 12f, 12f, 12f, 0f, 0f), Path.Direction.CW)
        }
        canvas.drawPath(detailsPanelPath, surfacePaint)
        canvas.drawLine(detailsPanelLeft, heroCardRect.top + 12f, detailsPanelLeft, heroCardRect.bottom - 12f, hairlinePaint)
        canvas.drawRoundRect(heroCardRect, 12f, 12f, borderPaint)

        // Column 1: BILLED TO (Customer)
        canvas.drawRoundRect(
            RectF(heroCardRect.left, heroCardRect.top + 14f, heroCardRect.left + 3.8f, heroCardRect.bottom - 14f),
            2f,
            2f,
            emeraldPaint
        )
        var cY = heroCardRect.top + 20f
        canvas.drawText(tr(language, "IMETAYARISHWA KWA (MTEJA)", "BILLED TO"), heroCardRect.left + 16f, cY, eyebrowPaint)
        cY += 18f
        val custTitlePaint = Paint(inkBold).apply { textSize = 13.4f }
        canvas.drawText(
            trimToWidth(inv.customerName.ifBlank { "—" }, custTitlePaint, leftColCardW - 28f),
            heroCardRect.left + 16f,
            cY,
            custTitlePaint
        )
        cY += 15.5f
        clientContactLines.forEach { (iconKind, line) ->
            val textX = if (iconKind != "none") {
                drawMiniIcon(canvas, iconKind, heroCardRect.left + 16f, cY - 6.5f, Color.parseColor(HEX_EMERALD))
                heroCardRect.left + 27f
            } else {
                heroCardRect.left + 27f
            }
            canvas.drawText(trimToWidth(line, inkBody, leftColCardW - 38f), textX, cY, inkBody)
            cY += 14f
        }

        // Column 2: INVOICE DETAILS (Number, Dates, Currency, Status — NO MONEY)
        var dY = heroCardRect.top + 20f
        val detailX = detailsPanelLeft + 16f
        val detailRightX = heroCardRect.right - 16f
        canvas.drawText(tr(language, "TAARIFA ZA ANKARA", "INVOICE DETAILS"), detailX, dY, eyebrowPaint)
        dY += 17f

        val metaLbl = Paint(inkMuted).apply { textSize = 8.4f }
        val metaValRight = Paint(inkBold).apply {
            textSize = 9.0f
            textAlign = Paint.Align.RIGHT
        }

        fun drawDetailRow(label: String, value: String, valuePaint: Paint = metaValRight) {
            canvas.drawText(label, detailX, dY, metaLbl)
            canvas.drawText(trimToWidth(value, valuePaint, rightColCardW * 0.54f), detailRightX, dY, valuePaint)
            dY += 15.5f
        }

        drawDetailRow(tr(language, "Namba ya Ankara", "Invoice No."), "#${inv.number}")
        drawDetailRow(tr(language, "Tarehe", "Issue Date"), inv.issueDate.ifBlank { "—" })
        val dueValPaint = Paint(metaValRight).apply {
            if (invoiceWithItems.isOverdue) color = Color.parseColor(HEX_OVERDUE_INK)
        }
        drawDetailRow(tr(language, "Mwisho wa Malipo", "Due Date"), inv.dueDate.ifBlank { "—" }, dueValPaint)
        val statusValPaint = Paint(metaValRight).apply { color = badge.inkColor }
        drawDetailRow(tr(language, "Hali / Sarafu", "Status / Currency"), "${badge.label} · $currency", statusValPaint)

        // Draw subtle background watermark on Page 1
        drawWatermark(canvas)

        // ==================== 3. DESIGNER ROUNDED LINE-ITEMS TABLE ====================
        var y = heroCardRect.bottom + 18f

        val colNumCenterX = MARGIN + 22f
        val colDescX = MARGIN + 44f
        val descMaxWidth = 258f
        val colQtyRight = 376f
        val colPriceRight = 466f
        val colTotalRight = PAGE_WIDTH - MARGIN - 14f

        fun drawTableHeader(c: Canvas, topY: Float): Float {
            val hRect = RectF(MARGIN, topY, PAGE_WIDTH - MARGIN, topY + 30f)
            val headerPath = Path().apply {
                addRoundRect(hRect, floatArrayOf(10f, 10f, 10f, 10f, 0f, 0f, 0f, 0f), Path.Direction.CW)
            }
            c.drawPath(headerPath, slatePaint)
            // Subtle gold underline below header
            c.drawRect(MARGIN, topY + 28.5f, PAGE_WIDTH - MARGIN, topY + 30f, goldPaint)

            val ty = topY + 18.5f
            val centerHeader = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.CENTER }
            c.drawText("#", colNumCenterX, ty, centerHeader)
            c.drawText(tr(language, "MAELEZO YA HUDUMA / BIDHAA", "ITEM & DESCRIPTION"), colDescX, ty, tableHeaderPaint)
            c.drawText(tr(language, "IDADI", "QTY"), colQtyRight, ty, tableHeaderRight)
            c.drawText(tr(language, "BEI ($currency)", "UNIT PRICE"), colPriceRight, ty, tableHeaderRight)
            c.drawText(tr(language, "JUMLA ($currency)", "AMOUNT"), colTotalRight, ty, tableHeaderRight)
            return topY + 30f
        }

        fun drawContinuationHeader(c: Canvas): Float {
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 44f, navyPaint)
            c.drawRect(0f, 41.5f, PAGE_WIDTH.toFloat(), 44f, goldPaint)
            val cb = Paint(brandWhitePaint).apply { textSize = 12f }
            val cn = Paint(invNumRight).apply { textSize = 10f }
            c.drawText(businessName.take(40), MARGIN, 27f, cb)
            c.drawText("${tr(language, "ANKARA", "INVOICE")} #${inv.number}", PAGE_WIDTH - MARGIN, 27f, cn)
            drawWatermark(c)
            return drawTableHeader(c, 56f)
        }

        var tableBlockTop = y
        y = drawTableHeader(canvas, y)

        val items = invoiceWithItems.orderedItems
        val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_INK_PRIMARY)
            textSize = 9.6f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val itemsEvenBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_ITEMS_ROW_EVEN) }
        val itemsOddBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_ITEMS_ROW_ODD) }
        val itemsDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_ITEMS_DIVIDER)
            strokeWidth = 0.75f
        }
        val itemsBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_ITEMS_BORDER)
            style = Paint.Style.STROKE
            strokeWidth = 1.0f
        }
        val numBadgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEX_SLATE_MID) }
        val numBadgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.0f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        items.forEachIndexed { idx, item ->
            val descLines = wrapText(item.description.ifBlank { "—" }, descPaint, descMaxWidth, maxLines = 3)
            val rowHeight = maxOf(34f, 18f + (descLines.size * 13.5f))

            if (y + rowHeight > PAGE_HEIGHT - 120f) {
                // Close outer table card border on current page
                canvas.drawRoundRect(RectF(MARGIN, tableBlockTop, PAGE_WIDTH - MARGIN, y), 10f, 10f, itemsBorderPaint)
                drawFooter(canvas, pageNumber)
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                tableBlockTop = 56f
                y = drawContinuationHeader(canvas)
            }

            val rowRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight)
            val rowBgPaint = if (idx % 2 == 1) itemsOddBgPaint else itemsEvenBgPaint
            val isLastOnTable = idx == items.lastIndex
            if (isLastOnTable) {
                val bottomRoundedPath = Path().apply {
                    addRoundRect(rowRect, floatArrayOf(0f, 0f, 0f, 0f, 10f, 10f, 10f, 10f), Path.Direction.CW)
                }
                canvas.drawPath(bottomRoundedPath, rowBgPaint)
            } else {
                canvas.drawRect(rowRect, rowBgPaint)
            }

            val firstLineY = y + 20.5f
            // Executive dark slate circular item number badge
            canvas.drawCircle(colNumCenterX, y + 17.5f, 9f, numBadgeBg)
            canvas.drawText("${idx + 1}", colNumCenterX, y + 20.3f, numBadgeText)

            descLines.forEachIndexed { lineIdx, line ->
                canvas.drawText(line, colDescX, firstLineY + (lineIdx * 13.5f), descPaint)
            }
            canvas.drawText(formatQty(item.qty), colQtyRight, firstLineY, rightBody)
            canvas.drawText(FormatUtils.formatMoney(item.price, ""), colPriceRight, firstLineY, rightBody)
            canvas.drawText(FormatUtils.formatMoney(item.lineTotal, ""), colTotalRight, firstLineY, rightBold)

            if (!isLastOnTable) {
                canvas.drawLine(MARGIN + 10f, y + rowHeight, PAGE_WIDTH - MARGIN - 10f, y + rowHeight, itemsDividerPaint)
            }
            y += rowHeight
        }

        // Complete crisp outer frame around the line-items table
        canvas.drawRoundRect(RectF(MARGIN, tableBlockTop, PAGE_WIDTH - MARGIN, y), 10f, 10f, itemsBorderPaint)

        // ==================== 4. AMOUNT IN WORDS OFFICIAL STRIP ====================
        y += 12f
        val wordsLabel = tr(language, "KIASI KWA MANENO:", "AMOUNT IN WORDS:")
        val wordsValue = amountToWords(invoiceWithItems.total, currency, language)
        val wordsValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_INK_PRIMARY)
            textSize = 8.6f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        val wordsLblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(HEX_GOLD_INK)
            textSize = 7.4f
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val wordsLines = wrapText(wordsValue, wordsValPaint, CONTENT_WIDTH - 28f, maxLines = 2)
        val wordsBoxH = 22f + (wordsLines.size * 12f)
        if (y + wordsBoxH + 220f > PAGE_HEIGHT - 50f) {
            drawFooter(canvas, pageNumber)
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 44f, navyPaint)
            canvas.drawRect(0f, 41.5f, PAGE_WIDTH.toFloat(), 44f, goldPaint)
            canvas.drawText(businessName.take(40), MARGIN, 27f, Paint(brandWhitePaint).apply { textSize = 12f })
            canvas.drawText("${tr(language, "ANKARA", "INVOICE")} #${inv.number}", PAGE_WIDTH - MARGIN, 27f, Paint(invNumRight).apply { textSize = 10f })
            drawWatermark(canvas)
            y = 58f
        }

        val wordsRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + wordsBoxH)
        canvas.drawRoundRect(wordsRect, 8f, 8f, goldSoftPaint)
        canvas.drawRoundRect(wordsRect, 8f, 8f, goldBorderPaint)
        canvas.drawText(wordsLabel, wordsRect.left + 12f, wordsRect.top + 13.5f, wordsLblPaint)
        var wy = wordsRect.top + 25.5f
        wordsLines.forEach { line ->
            canvas.drawText(line, wordsRect.left + 12f, wy, wordsValPaint)
            wy += 12f
        }
        y = wordsRect.bottom + 14f

        // ==================== 5. PAYMENT ACTION PANEL & EXECUTIVE TOTALS ====================
        val bottomStartY = y
        val leftColW = CONTENT_WIDTH * 0.50f
        val rightColLeft = MARGIN + leftColW + 16f
        val rightColW = CONTENT_WIDTH - leftColW - 16f

        // --- Right Column: Executive Totals Card ---
        val extraSummaryRows = (if (invoiceWithItems.effectiveDiscount > 0.0) 1 else 0) +
            (if (inv.vatRate > 0.0) 1 else 0) +
            (if (inv.amountPaid > 0.005) 1 else 0)
        val totalsCardH = 108f + (extraSummaryRows * 19f)
        val totalsRect = RectF(rightColLeft, bottomStartY, PAGE_WIDTH - MARGIN, bottomStartY + totalsCardH)
        drawSoftCardShadow(canvas, totalsRect, 10f)
        canvas.drawRoundRect(totalsRect, 10f, 10f, whitePaint)
        canvas.drawRoundRect(totalsRect, 10f, 10f, borderPaint)

        var sy = totalsRect.top + 20f
        val sumLblPaint = Paint(inkBody).apply { textSize = 9.3f }
        val sumValPaint = Paint(rightBody).apply { textSize = 9.6f }
        val sumEmeraldPaint = Paint(rightBold).apply {
            textSize = 9.6f
            color = Color.parseColor(HEX_EMERALD)
        }

        fun drawTotalLine(label: String, value: String, vPaint: Paint = sumValPaint) {
            canvas.drawText(label, totalsRect.left + 14f, sy, sumLblPaint)
            canvas.drawText(value, totalsRect.right - 14f, sy, vPaint)
            sy += 19f
        }

        drawTotalLine(tr(language, "Jumla Ndogo", "Subtotal"), FormatUtils.formatMoney(invoiceWithItems.subtotal, currency))
        if (invoiceWithItems.effectiveDiscount > 0.0) {
            drawTotalLine(
                tr(language, "Punguzo", "Discount"),
                "-${FormatUtils.formatMoney(invoiceWithItems.effectiveDiscount, currency)}",
                sumEmeraldPaint
            )
        }
        if (inv.vatRate > 0.0) {
            drawTotalLine(
                "${tr(language, "Kodi (VAT)", "VAT")} (${formatQty(inv.vatRate)}%)",
                FormatUtils.formatMoney(invoiceWithItems.vatAmount, currency)
            )
        }

        canvas.drawLine(totalsRect.left + 14f, sy - 7f, totalsRect.right - 14f, sy - 7f, strongDividerPaint)
        sy += 8f

        val grandLblPaint = Paint(inkBold).apply { textSize = 10.6f }
        val grandValPaint = Paint(rightBold).apply { textSize = 11.6f }
        canvas.drawText(tr(language, "Jumla Kuu", "Grand Total"), totalsRect.left + 14f, sy, grandLblPaint)
        canvas.drawText(FormatUtils.formatMoney(invoiceWithItems.total, currency), totalsRect.right - 14f, sy, grandValPaint)
        sy += 19f

        if (inv.amountPaid > 0.005) {
            drawTotalLine(
                tr(language, "Imelipwa Hadi Sasa", "Amount Paid"),
                "-${FormatUtils.formatMoney(inv.amountPaid, currency)}",
                sumEmeraldPaint
            )
        }

        // Bold High-Contrast Final Balance Footer inside the Totals Card
        val finalBarRect = RectF(totalsRect.left, totalsRect.bottom - 38f, totalsRect.right, totalsRect.bottom)
        val finalBarPath = Path().apply {
            addRoundRect(finalBarRect, floatArrayOf(0f, 0f, 0f, 0f, 10f, 10f, 10f, 10f), Path.Direction.CW)
        }
        canvas.drawPath(finalBarPath, if (isSettled) emeraldPaint else navyPaint)
        canvas.drawRect(finalBarRect.left, finalBarRect.top, finalBarRect.right, finalBarRect.top + 1.5f, goldPaint)

        val finalLblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E3CFA8")
            textSize = 8.2f
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val finalBalText = FormatUtils.formatMoney(invoiceWithItems.balance, currency)
        val finalValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = fitTextSize(finalBalText, rightColW * 0.54f, 12.6f, 9.8f)
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(
            if (isSettled) tr(language, "SALIO (IMELIPWA)", "BALANCE SETTLED")
            else tr(language, "SALIO LA KULIPA", "BALANCE DUE"),
            finalBarRect.left + 14f,
            finalBarRect.centerY() + 3.4f,
            finalLblPaint
        )
        canvas.drawText(finalBalText, finalBarRect.right - 14f, finalBarRect.centerY() + 4.2f, finalValPaint)

        // --- Left Column: High-Conversion Payment Card ---
        val paymentLines = settings.paymentDetails
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .flatMap { wrapText(it, inkBody, leftColW - 36f, maxLines = 2) }
            .take(5)
        val customNote = inv.notes.trim()
        val noteLines = if (customNote.isNotBlank()) {
            wrapText(customNote, inkMuted, leftColW - 28f, maxLines = 3)
        } else {
            emptyList()
        }

        val payCardHeight = maxOf(
            totalsCardH,
            88f + (paymentLines.size * 13.5f) + (if (noteLines.isNotEmpty()) 26f + (noteLines.size * 12f) else 0f)
        )
        val payRect = RectF(MARGIN, bottomStartY, MARGIN + leftColW, bottomStartY + payCardHeight)
        canvas.drawRoundRect(payRect, 10f, 10f, emeraldSoftPaint)
        canvas.drawRoundRect(payRect, 10f, 10f, emeraldBorderPaint)
        // Left emerald + gold accent ribbon
        canvas.drawRoundRect(RectF(payRect.left, payRect.top + 10f, payRect.left + 4f, payRect.bottom - 10f), 2f, 2f, emeraldPaint)

        var py = payRect.top + 20f
        drawMiniIcon(canvas, "wallet", payRect.left + 14f, py - 7f, Color.parseColor(HEX_EMERALD))
        canvas.drawText(
            tr(language, "JINSI YA KULIPIA ANKARA HII", "HOW TO SETTLE THIS INVOICE"),
            payRect.left + 26f,
            py,
            eyebrowPaint
        )
        py += 16f

        if (paymentLines.isNotEmpty()) {
            paymentLines.forEach { line ->
                canvas.drawCircle(payRect.left + 17f, py - 3f, 2f, emeraldPaint)
                canvas.drawText(line, payRect.left + 24f, py, inkBody)
                py += 13.5f
            }
        } else {
            val fallbackPay = tr(
                language,
                "Malipo yanakubaliwa kwa Benki, M-Pesa, Tigo Pesa, Airtel Money au Taslimu.",
                "Payment accepted via Bank Transfer, Mobile Money, or Cash."
            )
            wrapText(fallbackPay, inkBody, leftColW - 28f, maxLines = 2).forEach { line ->
                canvas.drawText(line, payRect.left + 14f, py, inkBody)
                py += 13.5f
            }
        }

        // Highlighted White Payment Reference Chip
        py += 4f
        val refChipText = "${tr(language, "Kumbukumbu / Ref:", "Payment Ref:")}  #${inv.number}"
        val refChipPaint = Paint(inkBold).apply {
            textSize = 8.5f
            color = Color.parseColor(HEX_EMERALD)
        }
        val chipW = (refChipPaint.measureText(refChipText) + 20f).coerceAtMost(leftColW - 28f)
        val chipRect = RectF(payRect.left + 14f, py - 10f, payRect.left + 14f + chipW, py + 9f)
        canvas.drawRoundRect(chipRect, 6f, 6f, whitePaint)
        canvas.drawRoundRect(chipRect, 6f, 6f, emeraldBorderPaint)
        canvas.drawText(trimToWidth(refChipText, refChipPaint, chipW - 14f), chipRect.left + 10f, chipRect.centerY() + 3f, refChipPaint)
        py = chipRect.bottom + 12f

        if (noteLines.isNotEmpty()) {
            canvas.drawText(tr(language, "MAELEZO YA ZIADA", "CLIENT NOTES"), payRect.left + 14f, py, eyebrowPaint)
            py += 12.5f
            noteLines.forEach { line ->
                canvas.drawText(line, payRect.left + 14f, py, inkMuted)
                py += 12f
            }
        }

        // ==================== 6. OFFICIAL AUTHORIZATION & CORPORATE SEAL ====================
        val signTopY = maxOf(totalsRect.bottom, payRect.bottom) + 22f
        if (signTopY + 66f < PAGE_HEIGHT - 46f) {
            // Left: Authorized Signatory Box
            val sigLineY = signTopY + 34f
            val dashedSigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(HEX_BORDER)
                style = Paint.Style.STROKE
                strokeWidth = 1f
                pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
            }
            canvas.drawLine(MARGIN, sigLineY, MARGIN + 205f, sigLineY, dashedSigPaint)

            val signerName = settings.ownerName.ifBlank { businessName }
            val sigNamePaint = Paint(inkBold).apply { textSize = 9.4f }
            canvas.drawText(trimToWidth(signerName, sigNamePaint, 205f), MARGIN, sigLineY + 13f, sigNamePaint)
            canvas.drawText(
                tr(language, "Sahihi Rasmi & Muhuri  ·  ${inv.issueDate}", "Authorized Signatory  ·  ${inv.issueDate}"),
                MARGIN,
                sigLineY + 25f,
                inkMuted
            )

            // Right: Authentic Dual-Ring Circular Corporate Seal Stamp
            val sealCenterX = PAGE_WIDTH - MARGIN - 52f
            val sealCenterY = signTopY + 32f
            val sealR = 30f
            val sealColor = if (isSettled) Color.parseColor(HEX_PAID_INK) else Color.parseColor(HEX_EMERALD)

            val ringOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sealColor
                alpha = 115
                style = Paint.Style.STROKE
                strokeWidth = 1.6f
            }
            val ringInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sealColor
                alpha = 85
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
                pathEffect = DashPathEffect(floatArrayOf(2.5f, 2f), 0f)
            }
            canvas.drawCircle(sealCenterX, sealCenterY, sealR, ringOuter)
            canvas.drawCircle(sealCenterX, sealCenterY, sealR - 4.2f, ringInner)

            // Horizontal ribbon band across seal center
            val bandRect = RectF(sealCenterX - sealR - 3f, sealCenterY - 8.5f, sealCenterX + sealR + 3f, sealCenterY + 8.5f)
            canvas.drawRoundRect(bandRect, 4f, 4f, whitePaint)
            canvas.drawRoundRect(bandRect, 4f, 4f, ringOuter)

            val sealTopTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sealColor
                alpha = 185
                textSize = 6.2f
                letterSpacing = 0.10f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val sealCenterTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sealColor
                alpha = 215
                textSize = 7.6f
                letterSpacing = 0.12f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val sealBottomTxt = Paint(sealTopTxt).apply { textSize = 5.6f }

            canvas.drawText(extractMonogram(businessName) + " ★", sealCenterX, sealCenterY - 13.5f, sealTopTxt)
            canvas.drawText(
                if (isSettled) tr(language, "IMELIPWA", "PAID") else tr(language, "RASMI", "VERIFIED"),
                sealCenterX,
                sealCenterY + 2.8f,
                sealCenterTxt
            )
            canvas.drawText(inv.number.takeLast(10), sealCenterX, sealCenterY + 19.5f, sealBottomTxt)
        }

        drawFooter(canvas, pageNumber)
        doc.finishPage(page)

        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    /** Draws ultra-crisp 8pt vector icons directly onto the PDF Canvas (phone, mail, pin, wallet). */
    private fun drawMiniIcon(canvas: Canvas, kind: String, x: Float, y: Float, color: Int) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 0.95f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        when (kind) {
            "phone" -> {
                // Crisp mini handset icon
                canvas.drawRoundRect(RectF(x + 1.5f, y, x + 6.5f, y + 7.5f), 1.4f, 1.4f, stroke)
                canvas.drawCircle(x + 4f, y + 6.0f, 0.65f, fill)
            }
            "mail" -> {
                canvas.drawRoundRect(RectF(x, y + 1f, x + 8f, y + 6.8f), 1.2f, 1.2f, stroke)
                val flap = Path().apply {
                    moveTo(x + 0.6f, y + 1.6f)
                    lineTo(x + 4f, y + 4.4f)
                    lineTo(x + 7.4f, y + 1.6f)
                }
                canvas.drawPath(flap, stroke)
            }
            "pin" -> {
                canvas.drawCircle(x + 4f, y + 2.8f, 2.5f, stroke)
                val tip = Path().apply {
                    moveTo(x + 2.1f, y + 4.4f)
                    lineTo(x + 4f, y + 7.6f)
                    lineTo(x + 5.9f, y + 4.4f)
                }
                canvas.drawPath(tip, stroke)
                canvas.drawCircle(x + 4f, y + 2.8f, 0.8f, fill)
            }
            "wallet" -> {
                canvas.drawRoundRect(RectF(x, y + 1f, x + 8.5f, y + 7.2f), 1.6f, 1.6f, stroke)
                canvas.drawLine(x, y + 3.2f, x + 8.5f, y + 3.2f, stroke)
                canvas.drawCircle(x + 6.4f, y + 5.2f, 0.85f, fill)
            }
        }
    }

    private fun fitTextSize(text: String, maxWidth: Float, maxSize: Float, minSize: Float): Float {
        var size = maxSize
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = size
        }
        while (size > minSize && p.measureText(text) > maxWidth) {
            size -= 0.5f
            p.textSize = size
        }
        return size
    }

    private fun extractMonogram(name: String): String {
        val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
            words.size == 1 && words[0].length >= 2 -> words[0].substring(0, 2).uppercase(Locale.US)
            words.size == 1 -> words[0].first().uppercaseChar().toString()
            else -> "MD"
        }
    }

    private fun amountToWords(amount: Double, currency: String, language: AppLanguage): String {
        val whole = amount.roundToLong().coerceAtLeast(0L)
        return if (language == AppLanguage.EN) {
            val words = numberToEnglishWords(whole)
            "$words $currency Only"
        } else {
            val words = numberToSwahiliWords(whole)
            val currLabel = if (currency.equals("TZS", ignoreCase = true)) "Shilingi za Kitanzania" else currency
            "$currLabel $words Pekee"
        }
    }

    private fun numberToEnglishWords(n: Long): String {
        if (n == 0L) return "Zero"
        val ones = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

        fun underThousand(num: Int): String {
            val parts = mutableListOf<String>()
            val h = num / 100
            val rem = num % 100
            if (h > 0) parts += "${ones[h]} Hundred"
            if (rem >= 20) {
                val t = rem / 10
                val o = rem % 10
                parts += if (o > 0) "${tens[t]}-${ones[o]}" else tens[t]
            } else if (rem > 0) {
                parts += ones[rem]
            }
            return parts.joinToString(" ")
        }

        val scales = arrayOf("", "Thousand", "Million", "Billion")
        var remaining = n
        var scaleIdx = 0
        val chunks = mutableListOf<String>()
        while (remaining > 0 && scaleIdx < scales.size) {
            val chunk = (remaining % 1000L).toInt()
            if (chunk > 0) {
                val label = underThousand(chunk)
                chunks += if (scales[scaleIdx].isNotEmpty()) "$label ${scales[scaleIdx]}" else label
            }
            remaining /= 1000L
            scaleIdx++
        }
        return chunks.reversed().joinToString(" ")
    }

    private fun numberToSwahiliWords(n: Long): String {
        if (n == 0L) return "Sifuri"
        val ones = arrayOf("", "Moja", "Mbili", "Tatu", "Nne", "Tano", "Sita", "Saba", "Nane", "Tisa")
        val tens = arrayOf("", "Kumi", "Ishirini", "Thelathini", "Arobaini", "Hamsini", "Sitini", "Sabini", "Themanini", "Tisini")

        fun underThousand(num: Int): String {
            val parts = mutableListOf<String>()
            val h = num / 100
            val rem = num % 100
            if (h > 0) parts += "Mia ${ones[h]}"
            if (rem > 0) {
                val t = rem / 10
                val o = rem % 10
                when {
                    t > 0 && o > 0 -> parts += "${tens[t]} na ${ones[o]}"
                    t > 0 -> parts += tens[t]
                    else -> parts += ones[o]
                }
            }
            return parts.joinToString(" na ")
        }

        val parts = mutableListOf<String>()
        val billions = (n / 1_000_000_000L).toInt()
        val millions = ((n % 1_000_000_000L) / 1_000_000L).toInt()
        val thousands = ((n % 1_000_000L) / 1_000L).toInt()
        val rest = (n % 1_000L).toInt()

        if (billions > 0) parts += "Bilioni ${underThousand(billions)}"
        if (millions > 0) parts += "Milioni ${underThousand(millions)}"
        if (thousands > 0) parts += "Elfu ${underThousand(thousands)}"
        if (rest > 0) parts += underThousand(rest)

        return if (parts.size <= 1) {
            parts.firstOrNull() ?: "Sifuri"
        } else {
            parts.dropLast(1).joinToString(", ") + " na " + parts.last()
        }
    }

    private fun resolveStatusBadge(item: InvoiceWithItems, language: AppLanguage): BadgePalette = when {
        item.isPaid -> BadgePalette(
            label = tr(language, "IMELIPWA", "PAID"),
            bgColor = Color.parseColor(HEX_PAID_BG),
            inkColor = Color.parseColor(HEX_PAID_INK),
            dotColor = Color.parseColor(HEX_PAID_DOT)
        )
        item.isDraft -> BadgePalette(
            label = tr(language, "RASIMU", "DRAFT"),
            bgColor = Color.parseColor(HEX_DRAFT_BG),
            inkColor = Color.parseColor(HEX_DRAFT_INK),
            dotColor = Color.parseColor(HEX_DRAFT_DOT)
        )
        item.isOverdue -> BadgePalette(
            label = tr(language, "IMECHELEWA", "OVERDUE"),
            bgColor = Color.parseColor(HEX_OVERDUE_BG),
            inkColor = Color.parseColor(HEX_OVERDUE_INK),
            dotColor = Color.parseColor(HEX_OVERDUE_DOT)
        )
        item.isPartial -> BadgePalette(
            label = tr(language, "AWAMU", "PARTIAL"),
            bgColor = Color.parseColor(HEX_PARTIAL_BG),
            inkColor = Color.parseColor(HEX_PARTIAL_INK),
            dotColor = Color.parseColor(HEX_PARTIAL_DOT)
        )
        else -> BadgePalette(
            label = tr(language, "INADAIWA", "DUE"),
            bgColor = Color.parseColor(HEX_DUE_BG),
            inkColor = Color.parseColor(HEX_DUE_INK),
            dotColor = Color.parseColor(HEX_DUE_DOT)
        )
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float, maxLines: Int = 3): List<String> {
        val clean = text.trim()
        if (clean.isEmpty()) return emptyList()
        val words = clean.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
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
