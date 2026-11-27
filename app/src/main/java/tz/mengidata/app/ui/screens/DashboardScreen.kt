package tz.mengidata.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.data.accounting.AutoAccountingEngine
import tz.mengidata.app.data.accounting.AutoBucket
import tz.mengidata.app.data.accounting.BusinessHealthReport
import tz.mengidata.app.data.accounting.CategoryAnalyticsReport
import tz.mengidata.app.data.accounting.PartySummary
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
import tz.mengidata.app.ui.components.MengiCard
import tz.mengidata.app.ui.components.OneUiScreenHeader
import tz.mengidata.app.ui.components.PeriodSelectorRow
import tz.mengidata.app.ui.components.PremiumReveal
import tz.mengidata.app.ui.components.PulsingStatusDot
import tz.mengidata.app.ui.components.SmartAutoEntrySheet
import tz.mengidata.app.ui.components.SpendingTrendsChartCard
import tz.mengidata.app.ui.components.DashboardUpdateCapsule
import tz.mengidata.app.ui.components.StatusPill
import tz.mengidata.app.ui.components.bucketVisualColors
import tz.mengidata.app.ui.theme.LocalMengiColors
import tz.mengidata.app.updater.UpdateInfo
import tz.mengidata.app.updater.UpdateUiState
import tz.mengidata.app.viewmodel.DashboardMetrics

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    zeroLossLedger: ZeroLossLedgerSummary,
    categoryAnalytics: CategoryAnalyticsReport,
    businessHealth: BusinessHealthReport,
    partyCount: Int,
    deletedCount: Int = 0,
    knownParties: List<PartySummary> = emptyList(),
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
    onOpenRecoveryVault: () -> Unit = {},
    onSyncPhoneStorage: () -> Unit = {},
    onExportStatementPdf: () -> Unit = {},
    onQuickAddTransaction: () -> Unit,
    onSaveSmartAutoEntry: (String, Double, String, String, String, AutoBucket?, Boolean) -> Unit,
    onReconcileLedger: () -> Unit,
    onExportColorExcel: () -> Unit,
    onExportCategoryCsv: () -> Unit,
    onDownloadUpdate: (UpdateInfo) -> Unit = {},
    onCancelDownloadUpdate: () -> Unit = {},
    onOpenInstallPermissionSettings: () -> Unit = {}
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
                    eyebrow = settings.businessName.ifBlank { "VaultLedger" },
                    title = tr(language, "Dashibodi ya Fedha", "Finance Overview")
                )
                Spacer(Modifier.height(10.dp))
                PeriodSelectorRow(
                    selected = selectedPeriod,
                    language = language,
                    onSelect = onSelectPeriod
                )
            }

            // Matte Android 17 Bento Hero Balance Card (Zero Glow / Zero Glare)
            item(key = "hero_balance") {
                PremiumReveal(delayMillis = 0) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = colors.heroStart,
                        border = BorderStroke(1.dp, Color(0xFF33453F)),
                        shadowElevation = if (colors.isDark) 0.dp else 4.dp,
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
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(15.dp)
                            ) {
                                // Top Hero Status Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PulsingStatusDot(color = Color(0xFF7BB597), size = 7.dp)
                                        Text(
                                            text = tr(language, "SALIO HALISI LA MFUMO", "SYSTEM NET BALANCE"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.lime
                                        )
                                    }
                                    StatusPill(
                                        text = if (language == AppLanguage.EN) selectedPeriod.labelEn else selectedPeriod.labelSw,
                                        containerColor = Color(0xFF2D3E39),
                                        contentColor = Color(0xFFEBE9E3)
                                    )
                                }

                                // Calm Matte Balance Display
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (privacyMaskEnabled) {
                                        Text(
                                            text = "••••••",
                                            style = MaterialTheme.typography.displayLarge,
                                            color = Color(0xFFEBE9E3)
                                        )
                                    } else {
                                        AnimatedMoneyText(
                                            amount = metrics.netProfit,
                                            currency = currency,
                                            style = MaterialTheme.typography.displayLarge,
                                            color = Color(0xFFEBE9E3)
                                        )
                                    }
                                    Text(
                                        text = tr(
                                            language,
                                            "Mapato baada ya kukata Matumizi, Bili ya Nyumbani na Akiba",
                                            "Income after deducting Expenses, Household Bill & Savings"
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB8C4BF)
                                    )
                                }

                                // Matte 4-Segment Cashflow Proportion Track
                                val flowBase = (metrics.periodIncome.coerceAtLeast(metrics.periodTotalOutflow)).coerceAtLeast(1.0)
                                val personalShare = ((metrics.periodExpense / flowBase).toFloat()).coerceIn(0f, 1f)
                                val householdShare = ((metrics.periodHouseholdBills / flowBase).toFloat()).coerceIn(0f, 1f - personalShare)
                                val savingsShare = ((metrics.periodSavingsOutflow / flowBase).toFloat()).coerceIn(0f, 1f - personalShare - householdShare)
                                val keptShare = (1f - personalShare - householdShare - savingsShare).coerceAtLeast(0f)
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF151E1B),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (keptShare > 0.01f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(keptShare)
                                                    .fillMaxHeight()
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF7BB597))
                                            )
                                        }
                                        if (personalShare > 0.01f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(personalShare)
                                                    .fillMaxHeight()
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFC9959A))
                                            )
                                        }
                                        if (householdShare > 0.01f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(householdShare)
                                                    .fillMaxHeight()
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF83AEB8))
                                            )
                                        }
                                        if (savingsShare > 0.01f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(savingsShare)
                                                    .fillMaxHeight()
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFA9A3C4))
                                            )
                                        }
                                    }
                                }

                                // Matte 2x2 Bento Stat Grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HeroStatChip(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Rounded.ArrowDownward,
                                        iconTint = Color(0xFF9ACAB2),
                                        iconBg = Color(0xFF233A31),
                                        label = tr(language, "Mapato Yote", "Total Income"),
                                        amount = metrics.periodIncome,
                                        currency = currency,
                                        masked = privacyMaskEnabled
                                    )
                                    HeroStatChip(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Rounded.ArrowUpward,
                                        iconTint = Color(0xFFD6B0B4),
                                        iconBg = Color(0xFF392B2E),
                                        label = tr(language, "Matumizi Yangu", "My Expenses"),
                                        amount = metrics.periodExpense,
                                        currency = currency,
                                        masked = privacyMaskEnabled
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HeroStatChip(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Rounded.Home,
                                        iconTint = Color(0xFFA2C4CC),
                                        iconBg = Color(0xFF26383E),
                                        label = tr(language, "Bili ya Nyumbani", "Household Bill"),
                                        amount = metrics.periodHouseholdBills,
                                        currency = currency,
                                        masked = privacyMaskEnabled
                                    )
                                    HeroStatChip(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Rounded.Savings,
                                        iconTint = Color(0xFFC2BDD6),
                                        iconBg = Color(0xFF302E40),
                                        label = tr(language, "Akiba (Imekatwa)", "Savings (Deducted)"),
                                        amount = metrics.periodSavingsOutflow,
                                        currency = currency,
                                        masked = privacyMaskEnabled
                                    )
                                }

                                // Matte Quick-Action Pill Dock inside Hero
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onQuickAddTransaction,
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEAE7DF),
                                            contentColor = Color(0xFF192622)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp),
                                        modifier = Modifier
                                            .weight(1.25f)
                                            .height(48.dp)
                                            .testTag("dashboard_add_txn_btn")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text(tr(language, "Rekodi", "New Entry"), maxLines = 1, fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalButton(
                                        onClick = { showSmartEntry = true },
                                        shape = CircleShape,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFF344C45),
                                            contentColor = Color(0xFFEBE9E3)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("dashboard_smart_auto_btn")
                                    ) {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text(tr(language, "Haraka", "Smart"), maxLines = 1, fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalButton(
                                        onClick = onOpenInvoices,
                                        shape = CircleShape,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFF2B3B36),
                                            contentColor = Color(0xFFEBE9E3)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("dashboard_invoices_btn")
                                    ) {
                                        Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text(tr(language, "Ankara", "Invoices"), maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // All-Services Command Hub (Huduma Zote) — 1-Tap Access to Every Upgraded Service
            item(key = "all_services_hub") {
                PremiumReveal(delayMillis = 30) {
                    MengiCard(contentPadding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = tr(language, "Huduma Zote za Mfumo", "All System Services"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(language, "Fikia ripoti za PDF, Excel, hifadhi ya simu na kikapu kwa mguso mmoja.", "One-tap access to PDF statements, Excel, phone backup & recovery."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ServiceQuickTile(
                                    modifier = Modifier.weight(1f).testTag("service_statement_pdf_tile"),
                                    icon = Icons.Rounded.PictureAsPdf,
                                    iconBg = colors.incomeSoft,
                                    iconTint = colors.income,
                                    label = tr(language, "Taarifa PDF", "Statement PDF"),
                                    onClick = onExportStatementPdf
                                )
                                ServiceQuickTile(
                                    modifier = Modifier.weight(1f).testTag("service_excel_tile"),
                                    icon = Icons.Rounded.TableChart,
                                    iconBg = colors.limeSoft,
                                    iconTint = colors.limeInk,
                                    label = tr(language, "Ripoti Excel", "Excel Report"),
                                    onClick = onExportColorExcel
                                )
                                ServiceQuickTile(
                                    modifier = Modifier.weight(1f).testTag("service_phone_sync_tile"),
                                    icon = Icons.Rounded.PhoneAndroid,
                                    iconBg = colors.savingsSoft,
                                    iconTint = colors.savings,
                                    label = tr(language, "Hifadhi Simu", "Phone Sync"),
                                    onClick = onSyncPhoneStorage
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ServiceQuickTile(
                                    modifier = Modifier.weight(1f).testTag("service_recovery_vault_tile"),
                                    icon = Icons.Rounded.RestoreFromTrash,
                                    iconBg = colors.warningSoft,
                                    iconTint = colors.warning,
                                    label = if (deletedCount > 0) {
                                        tr(language, "Kikapu ($deletedCount)", "Vault ($deletedCount)")
                                    } else {
                                        tr(language, "Kikapu", "Recovery")
                                    },
                                    onClick = onOpenRecoveryVault
                                )
                                ServiceQuickTile(
                                    modifier = Modifier.weight(1f).testTag("service_contacts_tile"),
                                    icon = Icons.Rounded.Groups,
                                    iconBg = colors.subtleSurface,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    label = tr(language, "Wahusika ($partyCount)", "Contacts ($partyCount)"),
                                    onClick = onOpenPartyDirectory
                                )
                                ServiceQuickTile(
                                    modifier = Modifier.weight(1f).testTag("service_reconcile_tile"),
                                    icon = Icons.Rounded.VerifiedUser,
                                    iconBg = colors.incomeSoft,
                                    iconTint = colors.income,
                                    label = tr(language, "Kagua Salio", "Verify Ledger"),
                                    onClick = onReconcileLedger
                                )
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
                        // Dedicated Separated Household Bill Card
                        val householdColors = bucketVisualColors(AutoBucket.HOUSEHOLD_BILL)
                        MengiCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_household_bill_card"),
                            containerColor = householdColors.softBg,
                            borderColor = householdColors.border,
                            onClick = onOpenTransactions,
                            contentPadding = 16.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(householdColors.dot),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Home,
                                            contentDescription = tr(language, "Bili ya Nyumbani", "Household Bill"),
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = tr(language, "Bili ya Nyumbani", "Household Bill"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = householdColors.ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tr(
                                                language,
                                                "Leo: ${if (privacyMaskEnabled) "••••" else FormatUtils.formatCompactMoney(metrics.todayHouseholdBills, currency)} · Imekatwa kwenye mfumo",
                                                "Today: ${if (privacyMaskEnabled) "••••" else FormatUtils.formatCompactMoney(metrics.todayHouseholdBills, currency)} · Deducted in system"
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = householdColors.ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text(
                                    text = if (privacyMaskEnabled) "••••" else FormatUtils.formatCompactMoney(metrics.periodHouseholdBills, currency),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = householdColors.ink
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Monthly Spending & Savings Trends Chart (Household vs Personal vs Savings)
            item(key = "spending_trends_chart_card") {
                PremiumReveal(delayMillis = 150) {
                    SpendingTrendsChartCard(
                        points = metrics.spendingTrendPoints,
                        totalSavings = metrics.totalSavings,
                        totalSavingsTarget = metrics.totalSavingsTarget,
                        currency = currency,
                        language = language,
                        privacyMaskEnabled = privacyMaskEnabled,
                        onOpenSavings = onOpenHub
                    )
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
                itemsIndexed(
                    items = metrics.recentTransactions,
                    key = { _, txn -> "recent_${txn.id}" },
                    contentType = { _, _ -> "recent_txn" }
                ) { _, txn ->
                    MengiCard(contentPadding = 0.dp) {
                        RecentTransactionRow(
                            txn = txn,
                            currency = currency,
                            language = language,
                            onClick = onOpenTransactions
                        )
                    }
                }
            }
        }
    }

    if (showSmartEntry) {
        SmartAutoEntrySheet(
            language = language,
            currency = settings.currency.ifBlank { "TZS" },
            knownParties = knownParties,
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
    masked: Boolean = false,
    compact: Boolean = true,
    containerColor: Color = Color(0xFF283733),
    borderColor: Color = Color(0xFF394D47),
    iconBg: Color = Color(0xFF30433E)
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(0.8.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB8C4BF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (masked) {
                    Text(
                        text = "••••",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFEBE9E3)
                    )
                } else {
                    AnimatedMoneyText(
                        amount = amount,
                        currency = currency,
                        compact = compact,
                        style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        color = Color(0xFFEBE9E3)
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
    language: AppLanguage,
    onClick: () -> Unit
) {
    val colors = LocalMengiColors.current
    val isHousehold = remember(txn) { AutoAccountingEngine.isHouseholdTransaction(txn) }
    val householdColors = bucketVisualColors(AutoBucket.HOUSEHOLD_BILL)
    val iconBg = when {
        isHousehold -> householdColors.softBg
        txn.isIncome -> colors.incomeSoft
        else -> colors.expenseSoft
    }
    val iconTint = when {
        isHousehold -> householdColors.dot
        txn.isIncome -> colors.income
        else -> colors.expense
    }
    val amountColor = when {
        isHousehold -> householdColors.ink
        txn.isIncome -> colors.income
        else -> colors.expense
    }
    val localizedCategory = remember(isHousehold, txn.category, language) {
        FormatUtils.localizedCategory(
            if (isHousehold) AutoAccountingEngine.HOUSEHOLD_CATEGORY else txn.category,
            language
        )
    }
    val subtitleLine = remember(txn.party, localizedCategory, txn.date) {
        listOf(
            txn.party.trim(),
            localizedCategory,
            txn.date
        ).filter { it.isNotBlank() }.joinToString(" · ")
    }
    val formattedAmount = remember(txn.amount, txn.isIncome, currency) {
        (if (txn.isIncome) "+" else "-") + FormatUtils.formatMoney(txn.amount, currency)
    }
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
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isHousehold -> Icons.Rounded.Home
                    txn.isIncome -> Icons.Rounded.ArrowDownward
                    else -> Icons.Rounded.ArrowUpward
                },
                contentDescription = if (isHousehold) tr(language, "Bili ya Nyumbani", "Household Bill") else localizedCategory,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.description.ifBlank { localizedCategory },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitleLine,
                style = MaterialTheme.typography.bodySmall,
                color = if (isHousehold) householdColors.ink else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formattedAmount,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
private fun ServiceQuickTile(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

