package tz.mengidata.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.data.accounting.BusinessHealthReport
import tz.mengidata.app.data.accounting.BusinessInsight
import tz.mengidata.app.data.accounting.HealthGrade
import tz.mengidata.app.data.accounting.InsightSeverity
import tz.mengidata.app.data.accounting.PartyRole
import tz.mengidata.app.data.accounting.PartySummary
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.theme.LocalMengiColors
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BusinessHealthScoreCard(
    report: BusinessHealthReport,
    partyCount: Int,
    language: AppLanguage,
    onOpenPartyDirectory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = (report.score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "health_gauge"
    )
    val (gradeColor, gradeSoft) = when (report.grade) {
        HealthGrade.EXCELLENT -> colors.income to colors.incomeSoft
        HealthGrade.GOOD -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
        HealthGrade.CAUTION -> colors.warning to colors.warningSoft
        HealthGrade.CRITICAL -> colors.expense to colors.expenseSoft
    }

    MengiCard(modifier = modifier.testTag("business_health_card")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(gradeSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoGraph,
                        contentDescription = null,
                        tint = gradeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = tr(language, "Afya ya Biashara", "Business Health"),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tr(language, "Uchambuzi wa papo hapo", "Live financial intelligence"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusPill(
                text = report.grade.label(language),
                containerColor = gradeSoft,
                contentColor = gradeColor
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circular Score Gauge
            Box(
                modifier = Modifier.size(86.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = colors.subtleSurface
                Canvas(modifier = Modifier.size(86.dp)) {
                    val strokeWidth = 9.dp.toPx()
                    val pad = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = gradeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${report.score}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3 Compact Intelligence Metrics
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val marginStr = String.format(Locale.US, "%.0f%%", report.profitMarginPct)
                val runwayStr = String.format(Locale.US, "%.1f %s", report.cashRunwayMonths, tr(language, "miezi", "mo"))
                val momDelta = report.incomeMoMDeltaPct
                val momStr = if (momDelta == null) {
                    "—"
                } else {
                    val sign = if (momDelta >= 0) "+" else ""
                    "$sign${momDelta.roundToInt()}%"
                }

                HealthMetricLine(
                    label = tr(language, "Kiwango cha Faida", "Profit Margin"),
                    value = marginStr,
                    valueColor = if (report.profitMarginPct >= 0) colors.income else colors.expense
                )
                HealthMetricLine(
                    label = tr(language, "Ukuaji wa Mwezi (MoM)", "Monthly Trend (MoM)"),
                    value = momStr,
                    valueColor = when {
                        momDelta == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        momDelta >= 0 -> colors.income
                        else -> colors.expense
                    },
                    icon = when {
                        momDelta == null -> null
                        momDelta >= 0 -> Icons.Rounded.TrendingUp
                        else -> Icons.Rounded.TrendingDown
                    }
                )
                HealthMetricLine(
                    label = tr(language, "Uhimilivu wa Fedha", "Cash Runway"),
                    value = runwayStr,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (report.insights.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                report.insights.forEach { insight ->
                    InsightRowItem(insight = insight, language = language)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        FilledTonalButton(
            onClick = onOpenPartyDirectory,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("dashboard_open_crm_btn")
        ) {
            Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                text = tr(
                    language,
                    "Daftari la Wateja & Wadau ($partyCount)",
                    "Customers & Vendors Directory ($partyCount)"
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HealthMetricLine(
    label: String,
    value: String,
    valueColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = valueColor, modifier = Modifier.size(15.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun InsightRowItem(
    insight: BusinessInsight,
    language: AppLanguage
) {
    val colors = LocalMengiColors.current
    val (icon, tint, bg) = when (insight.severity) {
        InsightSeverity.POSITIVE -> Triple(Icons.Rounded.CheckCircle, colors.income, colors.incomeSoft)
        InsightSeverity.INFO -> Triple(Icons.Rounded.Info, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        InsightSeverity.WARNING -> Triple(Icons.Rounded.WarningAmber, colors.warning, colors.warningSoft)
        InsightSeverity.ACTION -> Triple(Icons.Rounded.NotificationsActive, colors.expense, colors.expenseSoft)
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bg.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title(language),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = insight.detail(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyDirectorySheet(
    parties: List<PartySummary>,
    businessName: String,
    currency: String,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalMengiColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }

    val filtered = remember(parties, query, filter) {
        val q = query.trim().lowercase(Locale.ROOT)
        parties.filter { p ->
            val roleOk = when (filter) {
                "customers" -> p.role == PartyRole.CUSTOMER || p.role == PartyRole.BOTH
                "vendors" -> p.role == PartyRole.VENDOR || p.role == PartyRole.BOTH
                "owing" -> p.unpaidReceivables > 0.005
                else -> true
            }
            val searchOk = q.isEmpty() ||
                p.name.lowercase(Locale.ROOT).contains(q) ||
                p.phone.lowercase(Locale.ROOT).contains(q) ||
                p.email.lowercase(Locale.ROOT).contains(q)
            roleOk && searchOk
        }
    }

    val totalOwed = remember(parties) { parties.sumOf { it.unpaidReceivables } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = OneUiSheetShape,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(language, "Wateja & Wadau (CRM)", "Customers & Vendors"),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = tr(
                            language,
                            "${parties.size} wadau · Madai: ${FormatUtils.formatCompactMoney(totalOwed, currency)}",
                            "${parties.size} contacts · Receivables: ${FormatUtils.formatCompactMoney(totalOwed, currency)}"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(tr(language, "Tafuta jina, simu au barua pepe...", "Search name, phone or email...")) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("crm_search_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "all" to tr(language, "Wote", "All"),
                    "customers" to tr(language, "Wateja", "Customers"),
                    "vendors" to tr(language, "Wadau", "Vendors"),
                    "owing" to tr(language, "Wenye Madai", "Owing")
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        shape = CircleShape,
                        label = { Text(label) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Rounded.Groups,
                    title = tr(language, "Hakuna mteja au mdau", "No contacts found"),
                    subtitle = tr(
                        language,
                        "Rekodi miamala, ankara, au bili na majina ya wateja ili waonekane hapa kiotomatiki.",
                        "Contacts automatically populate from your transactions, invoices, and bills."
                    )
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.name.lowercase(Locale.ROOT) }) { party ->
                        MengiCard(contentPadding = 14.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (party.unpaidReceivables > 0.0) colors.warningSoft
                                            else MaterialTheme.colorScheme.primaryContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = party.name.take(2).uppercase(Locale.ROOT),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (party.unpaidReceivables > 0.0) colors.warning
                                        else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = party.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusPill(
                                            text = party.role.label(language),
                                            containerColor = colors.subtleSurface,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (party.unpaidReceivables > 0.005) {
                                            StatusPill(
                                                text = "${tr(language, "Anadaiwa", "Owes")} ${FormatUtils.formatCompactMoney(party.unpaidReceivables, currency)}",
                                                containerColor = colors.warningSoft,
                                                contentColor = colors.warning
                                            )
                                        }
                                    }
                                }
                                if (party.phone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${party.phone}"))
                                            runCatching { context.startActivity(dial) }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Rounded.Call,
                                            contentDescription = tr(language, "Piga simu", "Call"),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (party.unpaidReceivables > 0.005) {
                                    IconButton(
                                        onClick = {
                                            val msg = tr(
                                                language,
                                                "Habari ${party.name}, tunakukumbusha salio la ankara ${FormatUtils.formatMoney(party.unpaidReceivables, currency)} kutoka ${businessName.ifBlank { "Mengidata" }}. Asante!",
                                                "Hello ${party.name}, friendly reminder of your outstanding invoice balance of ${FormatUtils.formatMoney(party.unpaidReceivables, currency)} with ${businessName.ifBlank { "Mengidata" }}. Thank you!"
                                            )
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, msg)
                                            }
                                            runCatching { context.startActivity(Intent.createChooser(send, null)) }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Rounded.Share,
                                            contentDescription = tr(language, "Tuma ukumbusho", "Send reminder"),
                                            tint = colors.warning
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${tr(language, "Mapato", "In")}: ${FormatUtils.formatCompactMoney(party.totalReceived, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.income,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${tr(language, "Malipo", "Out")}: ${FormatUtils.formatCompactMoney(party.totalPaidOut, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.expense,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${party.transactionCount + party.invoiceCount} ${tr(language, "rekodi", "recs")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
