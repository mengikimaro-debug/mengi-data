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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.PeriodFilter
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.theme.LocalMengiColors

// Samsung One UI 8.5 Shared Shape Tokens
val OneUiSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
val OneUiDialogShape = RoundedCornerShape(28.dp)
val OneUiFieldShape = RoundedCornerShape(18.dp)

@Composable
fun oneUiTextFieldColors(): TextFieldColors {
    val colors = LocalMengiColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = colors.cardBorder
    )
}

/**
 * Samsung One UI 8.5 Signature 26dp Squircle Grouped Surface Card.
 * Relies on crisp surface contrast over Frost Light / AMOLED Black canvas with a whisper-thin specular rim.
 */
@Composable
fun MengiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalMengiColors.current
    val resolvedBorder = borderColor ?: colors.cardBorder
    val border = BorderStroke(1.dp, resolvedBorder)
    val shape = MaterialTheme.shapes.large // 26.dp One UI squircle

    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.976f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "oneui_card_press"
        )
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = modifier
                .fillMaxWidth()
                .scale(scale),
            shape = shape,
            color = containerColor,
            border = border,
            tonalElevation = 0.dp,
            shadowElevation = if (colors.isDark) 0.dp else 2.5.dp
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            border = border,
            tonalElevation = 0.dp,
            shadowElevation = if (colors.isDark) 0.dp else 1.5.dp
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

/**
 * Samsung One UI 8.5 Spacious Viewing Area Header.
 * Clean geometric title with optional eyebrow pill, subtitle, and trailing pill actions.
 */
@Composable
fun OneUiScreenHeader(
    title: String,
    eyebrow: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (!eyebrow.isNullOrBlank()) {
            SectionEyebrow(text = eyebrow)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailing
                )
            }
        }
    }
}

/**
 * Samsung One UI 8.5 Pill-in-Pill Segmented Tab Switcher.
 * Replaces boxy Material outlined segmented buttons with a soft rounded capsule track & elevated active pill.
 */
@Composable
fun OneUiSegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTags: List<String> = emptyList()
) {
    val colors = LocalMengiColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.subtleSurface,
        border = BorderStroke(
            width = 1.dp,
            color = colors.cardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { idx, label ->
                val selected = idx == selectedIndex
                val pillBg by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        colors.subtleSurface
                    },
                    animationSpec = tween(200),
                    label = "seg_pill_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(200),
                    label = "seg_pill_fg"
                )
                val tag = testTags.getOrNull(idx)
                Surface(
                    onClick = { onSelect(idx) },
                    shape = RoundedCornerShape(18.dp),
                    color = pillBg,
                    shadowElevation = if (selected && !colors.isDark) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .then(if (tag != null) Modifier.testTag(tag) else Modifier)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                lineHeight = 16.sp
                            ),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Samsung One UI 8.5 Floating Frosted-Glass Capsule Bottom Navigation Dock.
 * Hovers cleanly above the gesture/navigation bar with proper insets, specular glass rim,
 * and spring-animated active pill capsule indicators.
 */
data class OneUiNavTab(
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
fun OneUiFloatingNavBar(
    tabs: List<OneUiNavTab>,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = colors.dockSurface,
            border = BorderStroke(1.2.dp, colors.dockBorder),
            shadowElevation = if (colors.isDark) 6.dp else 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val pillColor by animateColorAsState(
                        targetValue = if (tab.selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            colors.dockSurface
                        },
                        animationSpec = tween(220),
                        label = "dock_pill"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (tab.selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(200),
                        label = "dock_fg"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (tab.selected) 1.1f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "dock_icon_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .clip(CircleShape)
                            .background(pillColor)
                            .clickable(onClick = tab.onClick)
                            .padding(vertical = 7.dp, horizontal = 2.dp)
                            .testTag(tab.testTag),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = contentColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(iconScale)
                            )
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 0.sp
                                ),
                                fontWeight = if (tab.selected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Samsung One UI 8.5 Signature Floating Squircle Action Button.
 */
@Composable
fun OneUiFloatingFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "oneui_fab_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
        shadowElevation = 8.dp,
        modifier = modifier
            .size(58.dp)
            .scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun SectionEyebrow(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.5.dp)
        )
    }
}

/**
 * Samsung One UI 8.5 Borderless Full-Pill Period Filter Strip.
 */
@Composable
fun PeriodSelectorRow(
    selected: PeriodFilter,
    language: AppLanguage,
    onSelect: (PeriodFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PeriodFilter.entries.forEach { period ->
            val isSelected = period == selected
            val bg by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                animationSpec = tween(180),
                label = "period_bg"
            )
            val fg by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(180),
                label = "period_fg"
            )
            Surface(
                onClick = { onSelect(period) },
                shape = CircleShape,
                color = bg,
                border = if (isSelected) null else BorderStroke(
                    1.dp,
                    colors.cardBorder
                ),
                shadowElevation = if (isSelected && !colors.isDark) 3.dp else 0.dp,
                modifier = Modifier
                    .heightIn(min = 38.dp)
                    .testTag("period_chip_${period.key}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.EN) period.labelEn else period.labelSw,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = fg,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    MengiCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    shape = CircleShape
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

data class MonthlyChartPoint(
    val label: String,
    val income: Double,
    val expense: Double
)

@Composable
fun IncomeExpenseBarChart(
    points: List<MonthlyChartPoint>,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current
    val gridColor = colors.cardBorder
    val incomeColor = colors.income
    val incomeTopColor = colors.lime
    val expenseColor = colors.expense
    val expenseSoftTop = colors.warning

    var animateChart by remember(points) { mutableStateOf(false) }
    LaunchedEffect(points) { animateChart = true }
    val growth by animateFloatAsState(
        targetValue = if (animateChart) 1f else 0.08f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chart_bar_growth"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = incomeColor, label = tr(language, "Mapato", "Income"))
            LegendDot(color = expenseColor, label = tr(language, "Matumizi", "Expenses"))
        }
        Spacer(Modifier.height(14.dp))

        val maxValue = points.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(166.dp)
        ) {
            val w = size.width
            val h = size.height
            val chartBottom = h - 6.dp.toPx()
            val chartTop = 8.dp.toPx()
            val usableHeight = (chartBottom - chartTop).coerceAtLeast(1f)

            for (i in 0..2) {
                val y = chartTop + usableHeight * (i / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 0.9.dp.toPx()
                )
            }

            if (points.isEmpty()) return@Canvas
            val groupWidth = w / points.size
            val barWidth = (groupWidth * 0.28f).coerceAtMost(18.dp.toPx())
            val gap = 5.dp.toPx()
            val corner = CornerRadius(10.dp.toPx(), 10.dp.toPx())

            points.forEachIndexed { idx, pt ->
                val centerX = groupWidth * idx + groupWidth / 2f

                val incHeight = (((pt.income / maxValue).toFloat() * usableHeight) * growth).coerceAtLeast(
                    if (pt.income > 0.0) 6.dp.toPx() else 0f
                )
                val expHeight = (((pt.expense / maxValue).toFloat() * usableHeight) * growth).coerceAtLeast(
                    if (pt.expense > 0.0) 6.dp.toPx() else 0f
                )

                if (incHeight > 0f) {
                    val topY = chartBottom - incHeight
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(incomeTopColor, incomeColor),
                            startY = topY,
                            endY = chartBottom
                        ),
                        topLeft = Offset(centerX - barWidth - gap / 2f, topY),
                        size = Size(barWidth, incHeight),
                        cornerRadius = corner
                    )
                }
                if (expHeight > 0f) {
                    val topY = chartBottom - expHeight
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(expenseSoftTop, expenseColor),
                            startY = topY,
                            endY = chartBottom
                        ),
                        topLeft = Offset(centerX + gap / 2f, topY),
                        size = Size(barWidth, expHeight),
                        cornerRadius = corner
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    isDanger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = OneUiDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = CircleShape,
                colors = if (isDanger) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.testTag("confirm_dialog_ok")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape,
                modifier = Modifier.testTag("confirm_dialog_cancel")
            ) {
                Text(cancelLabel)
            }
        }
    )
}
