package tz.mengidata.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.data.accounting.AutoBucket
import tz.mengidata.app.data.accounting.BusinessHealthReport
import tz.mengidata.app.data.accounting.CategoryAnalyticsReport
import tz.mengidata.app.data.accounting.ZeroLossLedgerSummary
import tz.mengidata.app.data.db.BusinessSettingsEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.AnimatedMoneyText
import tz.mengidata.app.ui.components.BusinessHealthScoreCard
import tz.mengidata.app.ui.components.CategoryAnalyticsCard
import tz.mengidata.app.ui.components.EmptyStateCard
import tz.mengidata.app.ui.components.HighBillTodayAlertBanner
import tz.mengidata.app.ui.components.IncomeExpenseBarChart
import tz.mengidata.app.ui.components.MengiCard
import tz.mengidata.app.ui.components.OneUiScreenHeader
import tz.mengidata.app.ui.components.PeriodSelectorRow
import tz.mengidata.app.ui.components.PremiumReveal
import tz.mengidata.app.ui.components.SmartAutoEntrySheet
import tz.mengidata.app.ui.components.StatusPill
import tz.mengidata.app.ui.components.ZeroLossAutoBreakdownCard
import tz.mengidata.app.ui.theme.LocalMengiColors
import tz.mengidata.app.updater.UpdateUiState
import tz.mengidata.app.viewmodel.DashboardMetrics

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    zeroLossLedger: ZeroLossLedgerSummary,
    categoryAnalytics: CategoryAnalyticsReport,
    businessHealth: BusinessHealthReport,
    partyCount: Int,
    privacyMaskEnabled: Boolean,
    settings: BusinessSettingsEntity,
    selectedPeriod: PeriodFilter,
    language: AppLanguage,
    updateState: UpdateUiState,
    onSelectPeriod: (PeriodFilter) -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenInvoices: () -> Unit,
    onOpenBills: () -> Unit,
    onOpenHub: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPartyDirectory: () -> Unit,
    onQuickAddTransaction: () -> Unit,
    onSaveSmartAutoEntry: (String, Double, String, String, String, AutoBucket?, Boolean) -> Unit,
    onReconcileLedger: () -> Unit,
    onExportColorExcel: () -> Unit,
    onExportCategoryCsv: () -> Unit
) {
    val colors = LocalMengiColors.current
    val currency = settings.currency.ifBlank { "TZS" }
    var showSmartEntry by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Update Available Pill Banner
            if (updateState is UpdateUiState.Available) {
                item(key = "update_banner") {
                    Surface(
                        onClick = onOpenSettings,
                        shape = MaterialTheme.shapes.large,
                        color = colors.limeSoft,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_update_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = tr(language, "Update inapatikana", "Update available"),
                                tint = colors.limeInk
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        language,
                                        "Toleo jipya ${updateState.info.version} linapatikana",
                                        "Version ${updateState.info.version} available"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.limeInk
                                )
                                Text(
                                    text = tr(
                                        language,
                                        "Gusa kufungua Mipangilio na kusakinisha kwa usalama.",
                                        "Tap to open Settings and install securely."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.limeInk.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // High Bill Today Soft Alert Banner
            if (zeroLossLedger.isTodayBillHigh) {
                item(key = "high_bill_alert") {
                    HighBillTodayAlertBanner(
                        ledger = zeroLossLedger,
                        currency = currency,
                        language = language,
                        onOpenBills = onOpenBills,
                        onExportColorReport = onExportColorExcel
                    )
                }
            }

            // Samsung One UI 8.5 Viewing Area Header + Pill Period Selector
            item(key = "header") {
                OneUiScreenHeader(
                    eyebrow = settings.businessName.ifBlank { "Mengidata" },
                    title = tr(language, "Dashibodi ya Fedha", "Finance Overview")
                )
                Spacer(Modifier.height(10.dp))
                PeriodSelectorRow(
                    selected = selectedPeriod,
                    language = language,
                    onSelect = onSelectPeriod
                )
            }

            // Samsung One UI 8.5 Galaxy Frost Hero Balance Card
            item(key = "hero_balance") {
                val heroMotion = rememberInfiniteTransition(label = "hero_aura")
                val glowShift by heroMotion.animateFloat(
                    initialValue = 0.78f,
                    targetValue = 0.94f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow_x"
                )
                val goldShift by heroMotion.animateFloat(
                    initialValue = 0.14f,
                    targetValue = 0.28f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 6400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "gold_x"
                )

                PremiumReveal(delayMillis = 0) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = colors.heroStart,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        shadowElevation = if (colors.isDark) 0.dp else 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_hero_card")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(colors.heroStart, colors.heroMid, colors.heroEnd)
                                    )
                                )
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                colors.heroAccentGlow.copy(alpha = 0.28f),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width * glowShift, size.height * 0.16f),
                                            radius = size.width * 0.62f
                                        )
                                    )
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                colors.heroGoldGlow.copy(alpha = 0.18f),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width * goldShift, size.height * 0.92f),
                                            radius = size.width * 0.48f
                                        )
                                    )
                                }
                                .padding(22.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(language, "FAIDA HALISI", "NET BALANCE"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.lime
                                    )
                                    StatusPill(
                                        text = if (language == AppLanguage.EN) selectedPeriod.labelEn else selectedPeriod.labelSw,
                                        containerColor = Color.White.copy(alpha = 0.15f),
                                        contentColor = Color.White
                                    )
                                }

                                if (privacyMaskEnabled) {
                                    Text(
                                        text = "••••••",
                                        style = MaterialTheme.typography.displayLarge,
                                        color = Color.White
                                    )
                                } else {
                                    AnimatedMoneyText(
                                        amount = metrics.netProfit,
                                        currency = currency,
                                        style = MaterialTheme.typography.displayLarge,
                                        color = Color.White
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HeroStatChip(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Rounded.ArrowDownward,
                                        iconTint = Color(0xFF34D399),
                                        label = tr(language, "Mapato", "Income"),
                                        amount = metrics.periodIncome,
                                        currency = currency,
                                        masked = privacyMaskEnabled
                                    )
                                    HeroStatChip(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Rounded.ArrowUpward,
                                        iconTint = Color(0xFFC4B5FD),
                                        label = tr(language, "Matumizi", "Expenses"),
                                        amount = metrics.periodExpense,
                                        currency = currency,
                                        masked = privacyMaskEnabled
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onQuickAddTransaction,
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF0A1733)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("dashboard_add_txn_btn")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(6.dp))
                                        Text(tr(language, "Rekodi Muamala", "New Entry"), maxLines = 1, fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalButton(
                                        onClick = onOpenInvoices,
                                        shape = CircleShape,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color.White.copy(alpha = 0.16f),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("dashboard_invoices_btn")
                                    ) {
                                        Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(6.dp))
                                        Text(tr(language, "Ankara", "Invoices"), maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Business Intelligence & Health Score Card
            item(key = "business_health_card") {
                PremiumReveal(delayMillis = 50) {
                    BusinessHealthScoreCard(
                        report = businessHealth,
                        partyCount = partyCount,
                        language = language,
                        onOpenPartyDirectory = onOpenPartyDirectory
                    )
                }
            }

            // Zero-Loss Auto-Accounting Breakdown Card
            item(key = "auto_breakdown_card") {
                PremiumReveal(delayMillis = 85) {
                    ZeroLossAutoBreakdownCard(
                        ledger = zeroLossLedger,
                        currency = currency,
                        language = language,
                        onOpenSmartEntry = { showSmartEntry = true },
                        onVerifyLedger = onReconcileLedger,
                        onExportColorExcel = onExportColorExcel
                    )
                }
            }

            // Secondary KPI Grid (2x2 One UI Squircles)
            item(key = "kpi_grid") {
                PremiumReveal(delayMillis = 120) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardKpiTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Description,
                                iconBg = colors.warningSoft,
                                iconTint = colors.warning,
                                label = tr(language, "Madai ya Ankara", "Receivables"),
                                value = FormatUtils.formatCompactMoney(metrics.receivablesBalance, currency),
                                caption = tr(language, "Salio linalodaiwa", "Unpaid invoices"),
                                onClick = onOpenInvoices
                            )
                            DashboardKpiTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                                iconBg = colors.expenseSoft,
                                iconTint = colors.expense,
                                label = tr(language, "Bili Zinazodaiwa", "Unpaid Bills"),
                                value = FormatUtils.formatCompactMoney(metrics.unpaidBillsTotal, currency),
                                caption = if (metrics.overdueBillsCount > 0) {
                                    tr(language, "${metrics.overdueBillsCount} zimechelewa", "${metrics.overdueBillsCount} overdue")
                                } else {
                                    tr(language, "Hakuna zilizochelewa", "All on schedule")
                                },
                                onClick = onOpenBills
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardKpiTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Savings,
                                iconBg = colors.incomeSoft,
                                iconTint = colors.income,
                                label = tr(language, "Jumla ya Akiba", "Total Savings"),
                                value = FormatUtils.formatCompactMoney(metrics.totalSavings, currency),
                                caption = if (metrics.totalSavingsTarget > 0) {
                                    val pct = ((metrics.totalSavings / metrics.totalSavingsTarget) * 100).toInt().coerceIn(0, 100)
                                    tr(language, "$pct% ya lengo", "$pct% of target")
                                } else {
                                    tr(language, "Malengo ya Akiba", "Savings goals")
                                },
                                onClick = onOpenHub
                            )
                            DashboardKpiTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.AccountBalanceWallet,
                                iconBg = colors.limeSoft,
                                iconTint = colors.limeInk,
                                label = tr(language, "Pesa Kuingia Leo", "Today's Money In"),
                                value = FormatUtils.formatCompactMoney(zeroLossLedger.todayMoneyIn, currency),
                                caption = tr(language, "Miamala yote", "View ledger"),
                                onClick = onOpenTransactions
                            )
                        }
                    }
                }
            }

            // 6-Month Cashflow Chart
            item(key = "chart_card") {
                PremiumReveal(delayMillis = 170) {
                    MengiCard {
                        Text(
                            text = tr(language, "Mtiririko wa Miezi 6", "6-Month Cashflow"),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tr(language, "Mapato dhidi ya matumizi ya biashara", "Business income vs. expenses"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        IncomeExpenseBarChart(
                            points = metrics.chartPoints,
                            language = language
                        )
                    }
                }
            }

            // Category Analytical Breakdown Card
            item(key = "category_analytics_card") {
                PremiumReveal(delayMillis = 200) {
                    CategoryAnalyticsCard(
                        report = categoryAnalytics,
                        currency = currency,
                        language = language,
                        maxVisibleItems = 5,
                        onCategoryClick = { _, _ -> onOpenTransactions() },
                        onExportCategoryCsv = onExportCategoryCsv
                    )
                }
            }

            // Recent Activity Section Header
            item(key = "recent_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(language, "Miamala ya Hivi Karibuni", "Recent Transactions"),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onOpenTransactions, shape = CircleShape) {
                        Text(tr(language, "Ona yote", "View all"))
                    }
                }
            }

            // One UI 8.5 Signature Grouped List Card for Recent Transactions
            if (metrics.recentTransactions.isEmpty()) {
                item(key = "empty_recent") {
                    EmptyStateCard(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = tr(language, "Hakuna miamala bado", "No transactions yet"),
                        subtitle = tr(
                            language,
                            "Anza kurekodi mapato au matumizi ya biashara yako.",
                            "Start logging your business income or expenses."
                        ),
                        actionLabel = tr(language, "Ongeza Muamala", "Add Transaction"),
                        onAction = onQuickAddTransaction
                    )
                }
            } else {
                item(key = "recent_grouped_card") {
                    MengiCard(contentPadding = 0.dp) {
                        metrics.recentTransactions.forEachIndexed { idx, txn ->
                            RecentTransactionRow(
                                txn = txn,
                                currency = currency,
                                onClick = onOpenTransactions
                            )
                            if (idx < metrics.recentTransactions.lastIndex) {
                                HorizontalDivider(
                                    color = colors.cardBorder.copy(alpha = 0.45f),
                                    modifier = Modifier.padding(start = 70.dp, end = 18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSmartEntry) {
        SmartAutoEntrySheet(
            language = language,
            onDismiss = { showSmartEntry = false },
            onSave = { desc, amt, party, method, date, overrideBucket, markBillPaid ->
                onSaveSmartAutoEntry(desc, amt, party, method, date, overrideBucket, markBillPaid)
                showSmartEntry = false
            }
        )
    }
}

@Composable
private fun HeroStatChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    amount: Double,
    currency: String,
    masked: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.11f),
        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
                if (masked) {
                    Text(
                        text = "••••",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                } else {
                    AnimatedMoneyText(
                        amount = amount,
                        currency = currency,
                        compact = true,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardKpiTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    caption: String,
    onClick: () -> Unit
) {
    MengiCard(modifier = modifier, onClick = onClick, contentPadding = 16.dp) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecentTransactionRow(
    txn: TransactionEntity,
    currency: String,
    onClick: () -> Unit
) {
    val colors = LocalMengiColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (txn.isIncome) colors.incomeSoft else colors.expenseSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (txn.isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                contentDescription = txn.type,
                tint = if (txn.isIncome) colors.income else colors.expense,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.description.ifBlank { txn.category },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(txn.party, txn.category, txn.date).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = (if (txn.isIncome) "+" else "-") + FormatUtils.formatMoney(txn.amount, currency),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (txn.isIncome) colors.income else colors.expense
        )
    }
}
