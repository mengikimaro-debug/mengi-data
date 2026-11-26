package tz.mengidata.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.data.accounting.AutoBucket
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.theme.LocalMengiColors
import kotlin.math.max
import kotlin.math.roundToInt

data class MonthlyTrendPoint(
    val monthKey: String,
    val label: String,
    val householdBills: Double,
    val personalExpenses: Double,
    val savingsDeposited: Double,
    val cumulativeSavings: Double
)

@Composable
fun SpendingTrendsChartCard(
    points: List<MonthlyTrendPoint>,
    totalSavings: Double,
    totalSavingsTarget: Double,
    currency: String,
    language: AppLanguage,
    privacyMaskEnabled: Boolean = false,
    onOpenSavings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current
    val householdColors = bucketVisualColors(AutoBucket.HOUSEHOLD_BILL)

    // Calm matte opaque series colors (Zero glare, WCAG AA+ contrast on Light & Dark surfaces)
    val householdStroke = householdColors.dot
    val personalStroke = colors.expense
    val savingsStroke = colors.income

    var chartMode by remember { mutableStateOf("line") } // "line" | "bar"
    var showHousehold by remember { mutableStateOf(true) }
    var showPersonal by remember { mutableStateOf(true) }
    var showSavings by remember { mutableStateOf(true) }
    var selectedIdx by remember(points.size) {
        mutableIntStateOf((points.size - 1).coerceAtLeast(0))
    }

    var animateChart by remember(points, chartMode) { mutableStateOf(false) }
    LaunchedEffect(points, chartMode) { animateChart = true }
    val growth by animateFloatAsState(
        targetValue = if (animateChart) 1f else 0.08f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "trends_chart_growth"
    )

    val safeIndex = selectedIdx.coerceIn(0, (points.size - 1).coerceAtLeast(0))
    val activePoint = points.getOrNull(safeIndex)

    val maxChartValue = remember(points, showHousehold, showPersonal, showSavings) {
        points.maxOfOrNull { pt ->
            max(
                if (showHousehold) pt.householdBills else 0.0,
                max(
                    if (showPersonal) pt.personalExpenses else 0.0,
                    if (showSavings) pt.savingsDeposited else 0.0
                )
            )
        }?.coerceAtLeast(1.0) ?: 1.0
    }

    MengiCard(
        modifier = modifier.testTag("dashboard_spending_trends_card"),
        contentPadding = 18.dp
    ) {
        // Header + Chart Mode Switcher (Line vs Bar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = tr(language, "Mwenendo wa Matumizi na Akiba", "Monthly Spending & Savings"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tr(
                        language,
                        "Bili ya Nyumbani, Matumizi Yangu na Akiba (Miezi 6)",
                        "Household bills, personal expenses & savings (6 months)"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ChartModeToggleButton(
                        icon = Icons.AutoMirrored.Rounded.ShowChart,
                        contentDescription = tr(language, "Chati ya mistari", "Line chart"),
                        selected = chartMode == "line",
                        onClick = { chartMode = "line" },
                        modifier = Modifier.testTag("trends_mode_line")
                    )
                    ChartModeToggleButton(
                        icon = Icons.Rounded.BarChart,
                        contentDescription = tr(language, "Chati ya nguzo", "Bar chart"),
                        selected = chartMode == "bar",
                        onClick = { chartMode = "bar" },
                        modifier = Modifier.testTag("trends_mode_bar")
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Interactive Tappable Series Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SeriesToggleChip(
                label = tr(language, "Nyumbani", "Household"),
                dotColor = householdStroke,
                activeBg = householdColors.softBg,
                activeFg = householdColors.ink,
                selected = showHousehold,
                onClick = {
                    if (!showHousehold || showPersonal || showSavings) showHousehold = !showHousehold
                },
                modifier = Modifier.weight(1f)
            )
            SeriesToggleChip(
                label = tr(language, "Binafsi", "Personal"),
                dotColor = personalStroke,
                activeBg = colors.expenseSoft,
                activeFg = colors.expense,
                selected = showPersonal,
                onClick = {
                    if (!showPersonal || showHousehold || showSavings) showPersonal = !showPersonal
                },
                modifier = Modifier.weight(1f)
            )
            SeriesToggleChip(
                label = tr(language, "Akiba", "Savings"),
                dotColor = savingsStroke,
                activeBg = colors.incomeSoft,
                activeFg = colors.income,
                selected = showSavings,
                onClick = {
                    if (!showSavings || showHousehold || showPersonal) showSavings = !showSavings
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        // Recharts-Inspired Interactive Multi-Series Chart Canvas with Y-Axis Scale
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Y-Axis Labels
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .height(176.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                val topLabel = if (privacyMaskEnabled) "••" else compactAxisValue(maxChartValue)
                val midLabel = if (privacyMaskEnabled) "••" else compactAxisValue(maxChartValue / 2.0)
                Text(
                    text = topLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = midLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = "0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(8.dp))

            val gridColor = colors.cardBorder
            val cursorGuideColor = MaterialTheme.colorScheme.primary
            val surfaceNodeCenter = MaterialTheme.colorScheme.surface

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(176.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isNotEmpty() && size.width > 0) {
                                val slotWidth = size.width.toFloat() / points.size.toFloat()
                                val tapped = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                                selectedIdx = tapped
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val topPad = 12.dp.toPx()
                val bottomPad = 10.dp.toPx()
                val usableH = (h - topPad - bottomPad).coerceAtLeast(1f)
                val baseY = h - bottomPad

                // Dashed Recharts-style Cartesian Gridlines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                for (i in 0..2) {
                    val y = topPad + usableH * (i / 2f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (i < 2) dashEffect else null
                    )
                }

                if (points.isEmpty()) return@Canvas

                val groupW = w / points.size.toFloat()
                val activeCenterX = groupW * safeIndex + groupW / 2f

                // Active Month Vertical Guide Cursor
                drawLine(
                    color = cursorGuideColor,
                    start = Offset(activeCenterX, topPad),
                    end = Offset(activeCenterX, baseY),
                    strokeWidth = 1.4.dp.toPx(),
                    pathEffect = dashEffect
                )

                fun yFor(value: Double): Float {
                    val ratio = ((value / maxChartValue).toFloat() * growth).coerceIn(0f, 1f)
                    return baseY - ratio * usableH
                }

                if (chartMode == "line") {
                    // Smooth Cubic-Bezier Trend Lines (Recharts Monotone Style)
                    fun drawSeriesLine(
                        color: Color,
                        selector: (MonthlyTrendPoint) -> Double
                    ) {
                        val coords = points.mapIndexed { idx, pt ->
                            Offset(groupW * idx + groupW / 2f, yFor(selector(pt)))
                        }
                        if (coords.size >= 2) {
                            val path = Path().apply {
                                moveTo(coords.first().x, coords.first().y)
                                for (i in 0 until coords.lastIndex) {
                                    val p0 = coords[i]
                                    val p1 = coords[i + 1]
                                    val midX = (p0.x + p1.x) / 2f
                                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                        coords.forEachIndexed { idx, c ->
                            val isSelected = idx == safeIndex
                            drawCircle(
                                color = color,
                                radius = if (isSelected) 6.5.dp.toPx() else 4.2.dp.toPx(),
                                center = c
                            )
                            drawCircle(
                                color = surfaceNodeCenter,
                                radius = if (isSelected) 3.dp.toPx() else 1.8.dp.toPx(),
                                center = c
                            )
                        }
                    }

                    if (showHousehold) drawSeriesLine(householdStroke) { it.householdBills }
                    if (showPersonal) drawSeriesLine(personalStroke) { it.personalExpenses }
                    if (showSavings) drawSeriesLine(savingsStroke) { it.savingsDeposited }
                } else {
                    // Recharts Grouped Rounded Bar Clusters
                    val activeSeries = buildList {
                        if (showHousehold) add(householdStroke to { p: MonthlyTrendPoint -> p.householdBills })
                        if (showPersonal) add(personalStroke to { p: MonthlyTrendPoint -> p.personalExpenses })
                        if (showSavings) add(savingsStroke to { p: MonthlyTrendPoint -> p.savingsDeposited })
                    }
                    if (activeSeries.isNotEmpty()) {
                        val gap = 3.dp.toPx()
                        val maxClusterW = groupW * 0.72f
                        val barW = ((maxClusterW - gap * (activeSeries.size - 1)) / activeSeries.size)
                            .coerceAtMost(13.dp.toPx())
                            .coerceAtLeast(4.dp.toPx())
                        val totalClusterW = barW * activeSeries.size + gap * (activeSeries.size - 1)
                        val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())

                        points.forEachIndexed { idx, pt ->
                            val startX = groupW * idx + (groupW - totalClusterW) / 2f
                            activeSeries.forEachIndexed { sIdx, (barColor, valueOf) ->
                                val rawVal = valueOf(pt)
                                val barH = (((rawVal / maxChartValue).toFloat() * usableH) * growth)
                                    .coerceAtLeast(if (rawVal > 0.0) 5.dp.toPx() else 0f)
                                if (barH > 0f) {
                                    val x = startX + sIdx * (barW + gap)
                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset(x, baseY - barH),
                                        size = Size(barW, barH),
                                        cornerRadius = corner
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Interactive Month X-Axis Pill Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 58.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEachIndexed { idx, pt ->
                val isSelected = idx == safeIndex
                Surface(
                    onClick = { selectedIdx = idx },
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("trends_month_selector_$idx")
                ) {
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Interactive Recharts-Style Selected Month Inspector Tooltip
        if (activePoint != null) {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.subtleSurface,
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(
                                language,
                                "Mchanganuo · ${activePoint.label}",
                                "Breakdown · ${activePoint.label}"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val monthTotalOut = activePoint.householdBills + activePoint.personalExpenses + activePoint.savingsDeposited
                        Text(
                            text = if (privacyMaskEnabled) {
                                "••••"
                            } else {
                                "${tr(language, "Jumla:", "Total:")} ${FormatUtils.formatCompactMoney(monthTotalOut, currency)}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrendTooltipMetric(
                            icon = Icons.Rounded.Home,
                            label = tr(language, "Nyumbani", "Household"),
                            amount = activePoint.householdBills,
                            currency = currency,
                            masked = privacyMaskEnabled,
                            containerColor = householdColors.softBg,
                            contentColor = householdColors.ink,
                            modifier = Modifier.weight(1f)
                        )
                        TrendTooltipMetric(
                            icon = Icons.Rounded.Person,
                            label = tr(language, "Binafsi", "Personal"),
                            amount = activePoint.personalExpenses,
                            currency = currency,
                            masked = privacyMaskEnabled,
                            containerColor = colors.expenseSoft,
                            contentColor = colors.expense,
                            modifier = Modifier.weight(1f)
                        )
                        TrendTooltipMetric(
                            icon = Icons.Rounded.Savings,
                            label = tr(language, "Akiba", "Savings"),
                            amount = activePoint.savingsDeposited,
                            currency = currency,
                            masked = privacyMaskEnabled,
                            containerColor = colors.incomeSoft,
                            contentColor = colors.income,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = colors.cardBorder, thickness = 0.8.dp)
        Spacer(Modifier.height(12.dp))

        // Savings Progress Bar Footer
        val progressFraction = if (totalSavingsTarget > 0.0) {
            (totalSavings / totalSavingsTarget).toFloat().coerceIn(0f, 1f)
        } else if (totalSavings > 0.0) {
            1f
        } else {
            0f
        }
        val progressPct = (progressFraction * 100f).roundToInt()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenSavings)
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.incomeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Savings,
                            contentDescription = null,
                            tint = colors.income,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Column {
                        Text(
                            text = tr(language, "Maendeleo ya Akiba", "Savings Goal Progress"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (privacyMaskEnabled) {
                                "•••• / ••••"
                            } else if (totalSavingsTarget > 0.0) {
                                "${FormatUtils.formatCompactMoney(totalSavings, currency)} / ${FormatUtils.formatCompactMoney(totalSavingsTarget, currency)}"
                            } else {
                                FormatUtils.formatMoney(totalSavings, currency)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = colors.incomeSoft
                ) {
                    Text(
                        text = "$progressPct%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.income,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(colors.subtleSurface)
            ) {
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(colors.income)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartModeToggleButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else LocalMengiColors.current.subtleSurface,
        animationSpec = tween(180),
        label = "chart_mode_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "chart_mode_fg"
    )
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg,
        modifier = modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SeriesToggleChip(
    label: String,
    dotColor: Color,
    activeBg: Color,
    activeFg: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) activeBg else colors.subtleSurface,
        border = BorderStroke(1.dp, if (selected) dotColor else colors.cardBorder),
        modifier = modifier.heightIn(min = 34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) dotColor else MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeFg else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrendTooltipMetric(
    icon: ImageVector,
    label: String,
    amount: Double,
    currency: String,
    masked: Boolean,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (masked) "••••" else FormatUtils.formatCompactMoney(amount, currency),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun compactAxisValue(value: Double): String = when {
    value >= 1_000_000_000.0 -> "${(value / 1_000_000_000.0).roundToInt()}B"
    value >= 1_000_000.0 -> "${String.format(java.util.Locale.US, "%.1f", value / 1_000_000.0)}M"
    value >= 1_000.0 -> "${(value / 1_000.0).roundToInt()}K"
    else -> value.roundToInt().toString()
}
