package tz.mengidata.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.data.accounting.AutoAccountingEngine
import tz.mengidata.app.data.accounting.AutoBucket
import tz.mengidata.app.data.accounting.PartyDirectoryEngine
import tz.mengidata.app.data.accounting.PartyRole
import tz.mengidata.app.data.accounting.PartySummary
import tz.mengidata.app.data.accounting.ZeroLossLedgerSummary
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.theme.LocalMengiColors

data class BucketVisualColors(
    val dot: Color,
    val softBg: Color,
    val border: Color,
    val ink: Color
)

@Composable
fun bucketVisualColors(bucket: AutoBucket): BucketVisualColors {
    val isDark = LocalMengiColors.current.isDark
    return if (isDark) {
        when (bucket) {
            AutoBucket.MONEY_IN -> BucketVisualColors(
                dot = Color(0xFF739E8C),
                softBg = Color(0xFF1D2C26),
                border = Color(0xFF334E43),
                ink = Color(0xFFCAE0D6)
            )
            AutoBucket.SAVINGS -> BucketVisualColors(
                dot = Color(0xFF8894B0),
                softBg = Color(0xFF212736),
                border = Color(0xFF39445E),
                ink = Color(0xFFD0D7E8)
            )
            AutoBucket.BILL -> BucketVisualColors(
                dot = Color(0xFFB59A7B),
                softBg = Color(0xFF2B241D),
                border = Color(0xFF4C3F32),
                ink = Color(0xFFE3D7C7)
            )
            AutoBucket.HIGH_BILL_TODAY -> BucketVisualColors(
                dot = Color(0xFF9E92B0),
                softBg = Color(0xFF272231),
                border = Color(0xFF453C57),
                ink = Color(0xFFDDD6E8)
            )
            AutoBucket.HOUSEHOLD_BILL -> BucketVisualColors(
                dot = Color(0xFF769AA6),
                softBg = Color(0xFF1C292F),
                border = Color(0xFF344C57),
                ink = Color(0xFFC8DCE3)
            )
            AutoBucket.EXPENSE -> BucketVisualColors(
                dot = Color(0xFFAD8E7D),
                softBg = Color(0xFF2B221E),
                border = Color(0xFF4C3C34),
                ink = Color(0xFFE2D1C8)
            )
        }
    } else {
        when (bucket) {
            AutoBucket.MONEY_IN -> BucketVisualColors(
                dot = Color(0xFF346352),
                softBg = Color(0xFFE3ECE8),
                border = Color(0xFFC4D6CE),
                ink = Color(0xFF264A3D)
            )
            AutoBucket.SAVINGS -> BucketVisualColors(
                dot = Color(0xFF4E5975),
                softBg = Color(0xFFE5E8F0),
                border = Color(0xFFC8CEDE),
                ink = Color(0xFF353E54)
            )
            AutoBucket.BILL -> BucketVisualColors(
                dot = Color(0xFF7D5D3B),
                softBg = Color(0xFFF0EAE1),
                border = Color(0xFFD9CEBE),
                ink = Color(0xFF5B432A)
            )
            AutoBucket.HIGH_BILL_TODAY -> BucketVisualColors(
                dot = Color(0xFF665B7A),
                softBg = Color(0xFFECE9F2),
                border = Color(0xFFD3CCE0),
                ink = Color(0xFF4A4159)
            )
            AutoBucket.HOUSEHOLD_BILL -> BucketVisualColors(
                dot = Color(0xFF3F6570),
                softBg = Color(0xFFE2ECEF),
                border = Color(0xFFC1D4DA),
                ink = Color(0xFF2C4A52)
            )
            AutoBucket.EXPENSE -> BucketVisualColors(
                dot = Color(0xFF7A5645),
                softBg = Color(0xFFF0E8E4),
                border = Color(0xFFD9C9C1),
                ink = Color(0xFF593E31)
            )
        }
    }
}

@Composable
fun HighBillTodayAlertBanner(
    ledger: ZeroLossLedgerSummary,
    currency: String,
    language: AppLanguage,
    onOpenBills: () -> Unit,
    onExportColorReport: () -> Unit
) {
    if (!ledger.isTodayBillHigh) return
    val accent = bucketVisualColors(AutoBucket.HIGH_BILL_TODAY)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = accent.softBg,
        border = BorderStroke(0.9.dp, accent.border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("high_bill_today_alert")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PulsingStatusDot(color = accent.dot, size = 10.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            language,
                            "TAHADHARI · Bili ya leo imekuwa kubwa!",
                            "ALERT · Today's bills are very high!"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent.ink
                    )
                    Text(
                        text = tr(
                            language,
                            "Bili za leo: ${FormatUtils.formatMoney(ledger.todayBillsTotal, currency)} (${ledger.todayBillsCount} bili).",
                            "Today's bills: ${FormatUtils.formatMoney(ledger.todayBillsTotal, currency)} (${ledger.todayBillsCount} bills)."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = accent.ink
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenBills, shape = CircleShape) {
                    Text(tr(language, "Kagua Bili", "Review Bills"), color = accent.ink, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onExportColorReport, shape = CircleShape) {
                    Text(tr(language, "Pakua Ripoti", "Export Report"), color = accent.ink)
                }
            }
        }
    }
}

@Composable
fun ZeroLossAutoBreakdownCard(
    ledger: ZeroLossLedgerSummary,
    currency: String,
    language: AppLanguage,
    onVerifyLedger: () -> Unit,
    onExportColorExcel: () -> Unit
) {
    val green = bucketVisualColors(AutoBucket.MONEY_IN)
    val blue = bucketVisualColors(AutoBucket.SAVINGS)
    val orange = bucketVisualColors(AutoBucket.EXPENSE)
    val householdTeal = bucketVisualColors(AutoBucket.HOUSEHOLD_BILL)
    val billColor = if (ledger.isTodayBillHigh) {
        bucketVisualColors(AutoBucket.HIGH_BILL_TODAY)
    } else {
        bucketVisualColors(AutoBucket.BILL)
    }

    MengiCard(modifier = Modifier.testTag("zero_loss_auto_card")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr(language, "Uchambuzi wa Kiotomatiki", "Auto-Accounting Engine"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tr(
                        language,
                        "Hakuna hesabu inayopotea · ${ledger.totalRecordsCount} rekodi zimehakikiwa",
                        "Zero-loss ledger · ${ledger.totalRecordsCount} records verified"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(
                text = if (ledger.integrityVerified) {
                    tr(language, "0% Upotevu", "0% Loss")
                } else {
                    tr(language, "Sawazisha", "Sync Needed")
                },
                containerColor = if (ledger.integrityVerified) green.softBg else billColor.softBg,
                contentColor = if (ledger.integrityVerified) green.ink else billColor.ink
            )
        }

        Spacer(Modifier.height(14.dp))

        // 2x2 One UI 8.5 Squircle Auto Buckets + Separated Household Bill Row
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AutoBucketTile(
                    modifier = Modifier.weight(1f),
                    colors = green,
                    badge = tr(language, "Pesa Kuingia", "Money In"),
                    value = FormatUtils.formatCompactMoney(ledger.totalMoneyIn, currency)
                )
                AutoBucketTile(
                    modifier = Modifier.weight(1f),
                    colors = blue,
                    badge = tr(language, "Akiba", "Savings"),
                    value = FormatUtils.formatCompactMoney(ledger.netSavingsBalance, currency)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AutoBucketTile(
                    modifier = Modifier.weight(1f),
                    colors = orange,
                    badge = tr(language, "Matumizi Yangu", "My Expenses"),
                    value = FormatUtils.formatCompactMoney(ledger.totalDirectExpenses, currency)
                )
                AutoBucketTile(
                    modifier = Modifier.weight(1f),
                    colors = billColor,
                    badge = if (ledger.isTodayBillHigh) {
                        tr(language, "Bili Kubwa Leo", "High Bill Today")
                    } else {
                        tr(language, "Bili Zangu", "My Bills")
                    },
                    value = FormatUtils.formatCompactMoney(ledger.totalBillsPaid, currency)
                )
            }
            // Dedicated Household Bill Tile (Strictly Separated from Personal Expenses)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auto_bucket_household_tile"),
                shape = RoundedCornerShape(18.dp),
                color = householdTeal.softBg,
                border = BorderStroke(1.dp, householdTeal.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = null,
                            tint = householdTeal.dot,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = tr(language, "Bili ya Nyumbani", "Household Bill"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = householdTeal.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(
                                    language,
                                    "Leo: ${FormatUtils.formatCompactMoney(ledger.todayHouseholdBills, currency)} · Imekatwa kwenye mfumo",
                                    "Today: ${FormatUtils.formatCompactMoney(ledger.todayHouseholdBills, currency)} · Deducted in system"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = householdTeal.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = FormatUtils.formatCompactMoney(ledger.totalHouseholdBills, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = householdTeal.ink
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = LocalMengiColors.current.subtleSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(language, "Salio Halisi + Akiba (Jumla ya Mali)", "Verified Cash + Savings (Total)"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedMoneyText(
                        amount = ledger.totalNetWorthWithSavings,
                        currency = currency,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PulsingStatusDot(
                        color = if (ledger.integrityVerified) green.dot else billColor.dot,
                        size = 8.dp
                    )
                    Icon(
                        imageVector = if (ledger.integrityVerified) Icons.Rounded.VerifiedUser else Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = if (ledger.integrityVerified) green.dot else billColor.dot,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onExportColorExcel,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("dashboard_export_color_btn")
            ) {
                Text(tr(language, "Ripoti Excel", "Excel Report"), maxLines = 1)
            }
            if (!ledger.integrityVerified) {
                TextButton(
                    onClick = onVerifyLedger,
                    shape = CircleShape,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(tr(language, "Sawazisha", "Sync"), maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoAccountingSheet(
    ledger: ZeroLossLedgerSummary,
    currency: String,
    language: AppLanguage,
    onVerifyLedger: () -> Unit,
    onExportColorExcel: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = OneUiSheetShape,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ZeroLossAutoBreakdownCard(
                ledger = ledger,
                currency = currency,
                language = language,
                onVerifyLedger = onVerifyLedger,
                onExportColorExcel = onExportColorExcel
            )
            MengiCard(contentPadding = 14.dp) {
                ReportColorLegendStrip(language = language)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AutoBucketTile(
    modifier: Modifier = Modifier,
    colors: BucketVisualColors,
    badge: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = colors.softBg,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.dot)
                )
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ReportColorLegendStrip(language: AppLanguage) {
    val items = listOf(
        AutoBucket.MONEY_IN to tr(language, "Kijani: Pesa Kuingia", "Green: Money In"),
        AutoBucket.HOUSEHOLD_BILL to tr(language, "Samawati: Bili ya Nyumbani", "Teal: Household Bill"),
        AutoBucket.HIGH_BILL_TODAY to tr(language, "Zambarau: Bili Kubwa Leo", "Lavender: High Bill Today"),
        AutoBucket.BILL to tr(language, "Manjano: Bili Zangu", "Amber: My Bills"),
        AutoBucket.EXPENSE to tr(language, "Machungwa: Matumizi Yangu", "Orange: My Expenses"),
        AutoBucket.SAVINGS to tr(language, "Bluu: Akiba", "Blue: Savings")
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = tr(language, "Mwongozo wa Rangi kwenye Excel/CSV:", "Excel/CSV Color Guide:"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { (bucket, label) ->
                val c = bucketVisualColors(bucket)
                Surface(
                    shape = CircleShape,
                    color = c.softBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(c.dot)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.ink,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAutoEntrySheet(
    language: AppLanguage,
    knownParties: List<PartySummary> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        description: String,
        amount: Double,
        party: String,
        method: String,
        date: String,
        overrideBucket: AutoBucket?,
        markBillPaid: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var party by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(FormatUtils.paymentMethods.first()) }
    var date by remember { mutableStateOf(FormatUtils.todayIso()) }
    var manualOverride by remember { mutableStateOf<AutoBucket?>(null) }
    var markBillPaid by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val learnedHouseholdNames = remember(knownParties) {
        PartyDirectoryEngine.householdNames(knownParties)
    }
    val autoResult = remember(description, party, learnedHouseholdNames) {
        AutoAccountingEngine.classifyText(
            description = description,
            party = party,
            learnedHouseholdNames = learnedHouseholdNames
        )
    }
    val activeBucket = manualOverride ?: autoResult.bucket
    val activeColors = bucketVisualColors(activeBucket)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = OneUiSheetShape,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = tr(language, "Ingiza Haraka (Kiotomatiki)", "Smart Auto-Entry"),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Text(
                text = tr(
                    language,
                    "Andika jina lolote na alama ya mkwaju (/) kwa utambuzi otomatiki: mf. Juma my/mybill, Asha/household, Neema/invoice, Kaka/saving au Juma/rentsaving.",
                    "Type any name with a slash tag (/) for instant auto-routing: e.g. Alex my/mybill, Sam/household, Maya/invoice, Chris/saving, or Pat/rentsaving."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it; errorText = null },
                label = { Text(tr(language, "Maelezo au Jina/Tag *", "Description or Name/Tag *")) },
                placeholder = { Text(tr(language, "Mf. Juma/household, Asha my/mybill, Neema/rentsaving...", "e.g. Alex/household, Sam my/mybill, Maya/rentsaving...")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_entry_description")
            )

            // Live Auto-Classification Preview Card
            val animBg by animateColorAsState(activeColors.softBg, tween(240), label = "preview_bg")
            val animBorder by animateColorAsState(activeColors.border, tween(240), label = "preview_border")
            val animInk by animateColorAsState(activeColors.ink, tween(240), label = "preview_ink")
            val animDot by animateColorAsState(activeColors.dot, tween(240), label = "preview_dot")

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = animBg,
                border = BorderStroke(1.dp, animBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_entry_preview")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (activeBucket == AutoBucket.HOUSEHOLD_BILL) Icons.Rounded.Home else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = animDot,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "${tr(language, "Imetambuliwa:", "Detected:")} ${activeBucket.label(language)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = animInk
                        )
                        Text(
                            text = if (manualOverride != null) {
                                tr(language, "Umechagua mwenyewe aina hii.", "Manually selected bucket.")
                            } else {
                                autoResult.reason(language)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = animInk
                        )
                        if (manualOverride == null && autoResult.slashTag.hasSlashTag) {
                            val tagMeta = buildList {
                                if (autoResult.slashTag.cleanName.isNotBlank()) {
                                    add("${tr(language, "Jina", "Name")}: ${autoResult.slashTag.cleanName}")
                                }
                                if (!autoResult.suggestedSavingsGoalTitle.isNullOrBlank()) {
                                    add("${tr(language, "Lengo", "Goal")}: ${autoResult.suggestedSavingsGoalTitle}")
                                }
                                if (autoResult.isInvoiceTagged) {
                                    add(tr(language, "Ankara Otomatiki", "Auto-Invoice"))
                                }
                            }.joinToString(" · ")
                            if (tagMeta.isNotBlank()) {
                                Text(
                                    text = tagMeta,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = animInk
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = manualOverride == null,
                    onClick = { manualOverride = null },
                    shape = CircleShape,
                    label = { Text(tr(language, "Kiotomatiki", "Auto")) }
                )
                listOf(
                    AutoBucket.HOUSEHOLD_BILL to tr(language, "Bili ya Nyumbani", "Household Bill"),
                    AutoBucket.EXPENSE to tr(language, "Matumizi Yangu", "My Expense"),
                    AutoBucket.BILL to tr(language, "Bili Zangu", "My Bill"),
                    AutoBucket.MONEY_IN to tr(language, "Pesa Kuingia", "Money In"),
                    AutoBucket.SAVINGS to tr(language, "Akiba", "Savings")
                ).forEach { (bucket, label) ->
                    FilterChip(
                        selected = manualOverride == bucket,
                        onClick = { manualOverride = bucket },
                        shape = CircleShape,
                        label = { Text(label) },
                        modifier = Modifier.testTag("smart_override_${bucket.code.lowercase()}")
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' }; errorText = null },
                    label = { Text(tr(language, "Kiasi (TZS) *", "Amount *")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("smart_entry_amount")
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text(tr(language, "Tarehe", "Date")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = party,
                onValueChange = { party = it; errorText = null },
                label = { Text(tr(language, "Jina Lolote au Jina/Tag (hiari)", "Any Name or Name/Tag (optional)")) },
                placeholder = { Text(tr(language, "Mf. Juma/household, Asha/invoice, Kaka/rentsaving", "e.g. Alex/household, Sam/invoice, Pat/rentsaving")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_entry_party")
            )

            // Universal Name Recognition Bar (real saved names across Transactions, Bills, Savings & Invoices)
            RecognizedNameSuggestionsBar(
                query = party,
                knownParties = knownParties,
                language = language,
                onSelectParty = { selected ->
                    party = selected.name
                    if (selected.role == PartyRole.HOUSEHOLD) {
                        if (description.isBlank()) {
                            description = tr(language, "Bili ya Nyumbani", "Household Bill")
                        }
                        manualOverride = AutoBucket.HOUSEHOLD_BILL
                    } else {
                        manualOverride = null
                    }
                }
            )

            if (activeBucket == AutoBucket.BILL || activeBucket == AutoBucket.HIGH_BILL_TODAY) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(checked = markBillPaid, onCheckedChange = { markBillPaid = it })
                    Text(
                        text = tr(language, "Bili hii imeshalipwa leo (irekodiwe kwenye matumizi)", "Bill already paid today (sync to ledger)"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                text = tr(language, "Mfumo wa Malipo (${FormatUtils.paymentMethods.size})", "Payment Method (${FormatUtils.paymentMethods.size})"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormatUtils.paymentMethods.forEach { m ->
                    FilterChip(
                        selected = FormatUtils.samePaymentMethod(method, m),
                        onClick = { method = m },
                        shape = CircleShape,
                        label = { Text(FormatUtils.localizedMethod(m, language)) }
                    )
                }
            }

            if (errorText != null) {
                Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if ((description.isBlank() && party.isBlank()) || amt <= 0.0) {
                        errorText = tr(language, "Tafadhali jaza maelezo (au jina) na kiasi sahihi.", "Please enter description (or name) and valid amount.")
                        return@Button
                    }
                    onSave(description.trim(), amt, party.trim(), method, date, manualOverride, markBillPaid)
                },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("smart_entry_save_btn")
            ) {
                Text(tr(language, "Hifadhi Kiotomatiki", "Auto-Save Entry"))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
