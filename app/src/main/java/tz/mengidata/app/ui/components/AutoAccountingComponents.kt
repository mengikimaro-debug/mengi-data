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
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
                dot = Color(0xFF34D399),
                softBg = Color(0xFF0A241E),
                border = Color(0xFF145341),
                ink = Color(0xFFA7F3D0)
            )
            AutoBucket.SAVINGS -> BucketVisualColors(
                dot = Color(0xFF818CF8),
                softBg = Color(0xFF171A38),
                border = Color(0xFF3730A3),
                ink = Color(0xFFC7D2FE)
            )
            AutoBucket.BILL -> BucketVisualColors(
                dot = Color(0xFFFBBF24),
                softBg = Color(0xFF281F0B),
                border = Color(0xFF78350F),
                ink = Color(0xFFFDE68A)
            )
            AutoBucket.HIGH_BILL_TODAY -> BucketVisualColors(
                dot = Color(0xFFB8A9D9),
                softBg = Color(0xFF1D1A2B),
                border = Color(0xFF3E3659),
                ink = Color(0xFFDCD3F2)
            )
            AutoBucket.EXPENSE -> BucketVisualColors(
                dot = Color(0xFFFB923C),
                softBg = Color(0xFF2C180D),
                border = Color(0xFF9A3412),
                ink = Color(0xFFFED7AA)
            )
        }
    } else {
        when (bucket) {
            AutoBucket.MONEY_IN -> BucketVisualColors(
                dot = Color(0xFF059669),
                softBg = Color(0xFFE9FBF3),
                border = Color(0xFFA7F3D0),
                ink = Color(0xFF065F46)
            )
            AutoBucket.SAVINGS -> BucketVisualColors(
                dot = Color(0xFF3E4CC6),
                softBg = Color(0xFFEEF1FF),
                border = Color(0xFFC7D2FE),
                ink = Color(0xFF262F82)
            )
            AutoBucket.BILL -> BucketVisualColors(
                dot = Color(0xFFD97706),
                softBg = Color(0xFFFFF8E6),
                border = Color(0xFFFDE68A),
                ink = Color(0xFF92400E)
            )
            AutoBucket.HIGH_BILL_TODAY -> BucketVisualColors(
                dot = Color(0xFF7B6B9E),
                softBg = Color(0xFFF3EFFA),
                border = Color(0xFFDDD5EE),
                ink = Color(0xFF4C3E6B)
            )
            AutoBucket.EXPENSE -> BucketVisualColors(
                dot = Color(0xFFEA580C),
                softBg = Color(0xFFFFF6EB),
                border = Color(0xFFFED7AA),
                ink = Color(0xFF9A3412)
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
                        color = accent.ink.copy(alpha = 0.9f)
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
    onOpenSmartEntry: () -> Unit,
    onVerifyLedger: () -> Unit,
    onExportColorExcel: () -> Unit
) {
    val green = bucketVisualColors(AutoBucket.MONEY_IN)
    val blue = bucketVisualColors(AutoBucket.SAVINGS)
    val orange = bucketVisualColors(AutoBucket.EXPENSE)
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

        // 2x2 One UI 8.5 Squircle Auto Buckets
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
                    badge = tr(language, "Matumizi", "Expenses"),
                    value = FormatUtils.formatCompactMoney(ledger.totalDirectExpenses, currency)
                )
                AutoBucketTile(
                    modifier = Modifier.weight(1f),
                    colors = billColor,
                    badge = if (ledger.isTodayBillHigh) {
                        tr(language, "Bili Kubwa Leo", "High Bill Today")
                    } else {
                        tr(language, "Bili Zilizolipwa", "Paid Bills")
                    },
                    value = FormatUtils.formatCompactMoney(ledger.totalBillsPaid, currency)
                )
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
            Button(
                onClick = onOpenSmartEntry,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("open_smart_auto_entry_btn")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(tr(language, "Ingiza Auto", "Smart Entry"), maxLines = 1)
            }
            TextButton(
                onClick = onExportColorExcel,
                shape = CircleShape,
                modifier = Modifier.testTag("dashboard_export_color_btn")
            ) {
                Text(tr(language, "Ripoti Excel", "Excel Report"), maxLines = 1)
            }
            if (!ledger.integrityVerified) {
                TextButton(onClick = onVerifyLedger, shape = CircleShape) {
                    Text(tr(language, "Sawazisha", "Sync"), maxLines = 1)
                }
            }
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
        border = BorderStroke(0.75.dp, colors.border.copy(alpha = 0.7f))
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
        AutoBucket.HIGH_BILL_TODAY to tr(language, "Zambarau Hafifu: Bili Kubwa Leo", "Soft Lavender: High Bill Today"),
        AutoBucket.BILL to tr(language, "Manjano: Bili ya Kawaida", "Amber: Regular Bill"),
        AutoBucket.EXPENSE to tr(language, "Machungwa: Matumizi", "Orange: Expenses"),
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

    val autoResult = remember(description) {
        AutoAccountingEngine.classifyText(description)
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
                    text = tr(language, "Ingiza Haraka (Uchambuzi Auto)", "Smart Auto-Entry"),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Text(
                text = tr(
                    language,
                    "Andika maelezo (mfano: \"LUKU ofisi\", \"Akiba kibubu\", \"Mauzo leo\", au \"Chakula\"). App itachambua kiotomatiki bila kupoteza hesabu yoyote.",
                    "Type what happened (e.g. \"LUKU power\", \"Savings deposit\", \"Shop sales\", or \"Fuel\"). The app auto-classifies with zero-loss accounting."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it; errorText = null },
                label = { Text(tr(language, "Maelezo *", "Description *")) },
                placeholder = { Text(tr(language, "Mf. LUKU ofisi, Akiba kibubu, Mauzo...", "e.g. LUKU bill, Savings, Sales...")) },
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
                border = BorderStroke(0.9.dp, animBorder),
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
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = animDot,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
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
                            color = animInk.copy(alpha = 0.88f)
                        )
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
                    label = { Text(tr(language, "Auto", "Auto")) }
                )
                listOf(
                    AutoBucket.MONEY_IN to tr(language, "Pesa Kuingia", "Money In"),
                    AutoBucket.SAVINGS to tr(language, "Akiba", "Savings"),
                    AutoBucket.BILL to tr(language, "Bili", "Bill"),
                    AutoBucket.EXPENSE to tr(language, "Matumizi", "Expense")
                ).forEach { (bucket, label) ->
                    FilterChip(
                        selected = manualOverride == bucket,
                        onClick = { manualOverride = bucket },
                        shape = CircleShape,
                        label = { Text(label) }
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
                onValueChange = { party = it },
                label = { Text(tr(language, "Mteja / Mlipwaji (Hiari)", "Customer / Payee (Optional)")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
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

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormatUtils.paymentMethods.forEach { m ->
                    FilterChip(
                        selected = method == m,
                        onClick = { method = m },
                        shape = CircleShape,
                        label = { Text(m) }
                    )
                }
            }

            if (errorText != null) {
                Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (description.isBlank() || amt <= 0.0) {
                        errorText = tr(language, "Tafadhali jaza maelezo na kiasi sahihi.", "Please enter description and valid amount.")
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
