package tz.mengidata.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.data.accounting.AutoAccountingEngine
import tz.mengidata.app.data.accounting.AutoBucket
import tz.mengidata.app.data.accounting.BillCategoryTab
import tz.mengidata.app.data.accounting.BillLedgerEntry
import tz.mengidata.app.data.accounting.BillsComparisonEngine
import tz.mengidata.app.data.accounting.PartyDirectoryEngine
import tz.mengidata.app.data.accounting.PartyRole
import tz.mengidata.app.data.accounting.PartySummary
import tz.mengidata.app.data.accounting.ZeroLossLedgerSummary
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.AutoBillExpenseRowCard
import tz.mengidata.app.ui.components.BillsComparisonOverviewCard
import tz.mengidata.app.ui.components.BillsDailyGroupHeader
import tz.mengidata.app.ui.components.ConfirmActionDialog
import tz.mengidata.app.ui.components.EmptyStateCard
import tz.mengidata.app.ui.components.MengiCard
import tz.mengidata.app.ui.components.OneUiSegmentedTabs
import tz.mengidata.app.ui.components.OneUiFieldShape
import tz.mengidata.app.ui.components.OneUiFloatingFab
import tz.mengidata.app.ui.components.OneUiScreenHeader
import tz.mengidata.app.ui.components.OneUiSheetShape
import tz.mengidata.app.ui.components.RecognizedNameSuggestionsBar
import tz.mengidata.app.ui.components.StatusPill
import tz.mengidata.app.ui.components.bucketVisualColors
import tz.mengidata.app.ui.components.categoryIconForName
import tz.mengidata.app.ui.components.oneUiTextFieldColors
import tz.mengidata.app.ui.theme.LocalMengiColors
import java.util.Locale

@Composable
fun BillsScreen(
    bills: List<BillEntity>,
    transactions: List<TransactionEntity> = emptyList(),
    zeroLossLedger: ZeroLossLedgerSummary,
    knownParties: List<PartySummary> = emptyList(),
    currency: String,
    language: AppLanguage,
    onSaveBill: (BillEntity) -> Unit,
    onTogglePaid: (String) -> Unit,
    onDuplicateBill: (BillEntity) -> Unit = {},
    onPayAllDueBills: () -> Unit = {},
    onScheduleNextMonth: (String) -> Unit,
    onDeleteBill: (String) -> Unit,
    onExportBillsCsv: () -> Unit
) {
    var selectedCategoryTab by remember { mutableStateOf(BillCategoryTab.HOUSEHOLD) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var editingBill by remember { mutableStateOf<BillEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val comparison = remember(bills, transactions) {
        BillsComparisonEngine.analyze(
            bills = bills,
            transactions = transactions
        )
    }

    val activeDailyGroups = remember(comparison, selectedCategoryTab, filter, search, language) {
        BillsComparisonEngine.filterDailyGroups(
            groups = comparison.dailyGroupsFor(selectedCategoryTab),
            filterKey = filter,
            query = search,
            language = language
        )
    }

    val activeTotalCount = comparison.countFor(selectedCategoryTab)
    val activeUnpaidCount = comparison.unpaidCountFor(selectedCategoryTab)
    val activeOverdueCount = comparison.overdueCountFor(selectedCategoryTab)
    val activePaidCount = comparison.paidCountFor(selectedCategoryTab)
    val activeAutoCount = comparison.autoCountFor(selectedCategoryTab)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header", contentType = "header") {
                OneUiScreenHeader(
                    eyebrow = tr(language, "Uchambuzi wa Bili Kila Siku", "Daily Bills & Spending"),
                    title = tr(language, "Bili", "Bills"),
                    trailing = {
                        FilledTonalButton(
                            onClick = onExportBillsCsv,
                            shape = CircleShape,
                            modifier = Modifier.testTag("bills_screen_export_csv_btn")
                        ) {
                            Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(tr(language, "Hamisha CSV", "Export CSV"), maxLines = 1)
                        }
                    }
                )
            }

            // 2-option Category Switcher inside the Bills tab: Household Bill vs Personal Bill
            item(key = "category_tabs", contentType = "category_tabs") {
                OneUiSegmentedTabs(
                    options = listOf(
                        tr(
                            language,
                            "Bili ya Nyumbani (${comparison.householdCount})",
                            "Household Bill (${comparison.householdCount})"
                        ),
                        tr(
                            language,
                            "Bili Binafsi (${comparison.personalCount})",
                            "Personal Bill (${comparison.personalCount})"
                        )
                    ),
                    selectedIndex = if (selectedCategoryTab == BillCategoryTab.HOUSEHOLD) 0 else 1,
                    onSelect = { idx ->
                        selectedCategoryTab = if (idx == 0) BillCategoryTab.HOUSEHOLD else BillCategoryTab.PERSONAL
                    },
                    testTags = listOf("bills_cat_household", "bills_cat_personal")
                )
            }

            // Automatic Household vs Personal comparison card ("Ipi Iko Juu")
            item(key = "comparison_overview", contentType = "comparison_overview") {
                BillsComparisonOverviewCard(
                    summary = comparison,
                    selectedTab = selectedCategoryTab,
                    currency = currency,
                    language = language,
                    onSelectTab = { selectedCategoryTab = it },
                    onPayAllDueBills = onPayAllDueBills
                )
            }

            item(key = "search_and_filters", contentType = "search_and_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = {
                            Text(
                                if (selectedCategoryTab == BillCategoryTab.HOUSEHOLD) {
                                    tr(language, "Tafuta bili ya nyumbani au tarehe...", "Search household bill or date...")
                                } else {
                                    tr(language, "Tafuta bili binafsi au matumizi...", "Search personal bill or expense...")
                                }
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = if (search.isNotEmpty()) {
                            {
                                IconButton(onClick = { search = "" }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = tr(language, "Futa utafutaji", "Clear search")
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = CircleShape,
                        colors = oneUiTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bills_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "all" to tr(language, "Zote ($activeTotalCount)", "All ($activeTotalCount)"),
                            "unpaid" to tr(language, "Hazijalipwa ($activeUnpaidCount)", "Unpaid ($activeUnpaidCount)"),
                            "overdue" to tr(language, "Zimechelewa ($activeOverdueCount)", "Overdue ($activeOverdueCount)"),
                            "paid" to tr(language, "Zimelipwa ($activePaidCount)", "Paid ($activePaidCount)"),
                            "auto" to tr(language, "Auto Miamala ($activeAutoCount)", "Auto Ledger ($activeAutoCount)")
                        ).forEach { (key, label) ->
                            BillsSolidFilterPill(
                                label = label,
                                selected = filter == key,
                                onClick = { filter = key },
                                modifier = Modifier.testTag("bill_filter_$key")
                            )
                        }
                    }
                }
            }

            if (activeDailyGroups.isEmpty()) {
                item(key = "empty_${selectedCategoryTab.name}", contentType = "empty") {
                    val isHouseholdTab = selectedCategoryTab == BillCategoryTab.HOUSEHOLD
                    EmptyStateCard(
                        icon = if (isHouseholdTab) Icons.Rounded.Home else Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = if (isHouseholdTab) {
                            tr(language, "Hakuna bili ya nyumbani", "No household bills yet")
                        } else {
                            tr(language, "Hakuna bili binafsi", "No personal bills yet")
                        },
                        subtitle = if (isHouseholdTab) {
                            tr(
                                language,
                                "Bili na matumizi ya nyumbani hupangwa kiotomatiki kwa kila siku hapa.",
                                "Household bills and expenses are automatically grouped per day here."
                            )
                        } else {
                            tr(
                                language,
                                "Bili na matumizi yako binafsi hupangwa kiotomatiki kwa kila siku hapa.",
                                "Your personal bills and daily expenses are automatically grouped per day here."
                            )
                        },
                        actionLabel = if (isHouseholdTab) {
                            tr(language, "Ongeza Bili ya Nyumbani", "Add Household Bill")
                        } else {
                            tr(language, "Ongeza Bili Binafsi", "Add Personal Bill")
                        },
                        onAction = {
                            editingBill = null
                            showEditor = true
                        }
                    )
                }
            } else {
                activeDailyGroups.forEach { dayGroup ->
                    item(
                        key = "day_header_${selectedCategoryTab.name}_${dayGroup.dayIso}",
                        contentType = "day_header"
                    ) {
                        BillsDailyGroupHeader(
                            group = dayGroup,
                            tab = selectedCategoryTab,
                            currency = currency,
                            language = language
                        )
                    }

                    items(
                        items = dayGroup.entries,
                        key = { "${selectedCategoryTab.name}_${it.stableKey}" },
                        contentType = { entry ->
                            when (entry) {
                                is BillLedgerEntry.ScheduledBill -> "bill_card"
                                is BillLedgerEntry.AutoExpense -> "auto_expense_card"
                            }
                        }
                    ) { entry ->
                        when (entry) {
                            is BillLedgerEntry.ScheduledBill -> {
                                val bill = entry.bill
                                val isHighToday = bill.id in zeroLossLedger.highBillIds
                                BillCard(
                                    bill = bill,
                                    isHighToday = isHighToday,
                                    currency = currency,
                                    language = language,
                                    onTogglePaid = { onTogglePaid(bill.id) },
                                    onDuplicate = { onDuplicateBill(bill) },
                                    onScheduleNextMonth = { onScheduleNextMonth(bill.id) },
                                    onEdit = {
                                        editingBill = bill
                                        showEditor = true
                                    },
                                    onDelete = { pendingDeleteId = bill.id }
                                )
                            }
                            is BillLedgerEntry.AutoExpense -> {
                                AutoBillExpenseRowCard(
                                    entry = entry,
                                    currency = currency,
                                    language = language
                                )
                            }
                        }
                    }
                }
            }
        }

        OneUiFloatingFab(
            icon = Icons.Rounded.Add,
            contentDescription = tr(language, "Ongeza bili", "Add bill"),
            onClick = {
                editingBill = null
                showEditor = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 96.dp)
                .testTag("add_bill_fab")
        )
    }

    if (showEditor) {
        BillEditorSheet(
            initial = editingBill,
            defaultHousehold = selectedCategoryTab == BillCategoryTab.HOUSEHOLD,
            knownParties = knownParties,
            currency = currency,
            language = language,
            onDismiss = {
                showEditor = false
                editingBill = null
            },
            onSave = { bill ->
                onSaveBill(bill)
                showEditor = false
                editingBill = null
            }
        )
    }

    pendingDeleteId?.let { id ->
        ConfirmActionDialog(
            title = tr(language, "Futa bili?", "Delete bill?"),
            message = tr(
                language,
                "Bili hii na muamala wa matumizi uliounganishwa nayo vitaondolewa.",
                "This bill and any linked expense transaction will be removed."
            ),
            confirmLabel = tr(language, "Futa", "Delete"),
            cancelLabel = tr(language, "Ghairi", "Cancel"),
            isDanger = true,
            onConfirm = {
                onDeleteBill(id)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null }
        )
    }
}

@Composable
private fun BillsSolidFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.heightIn(min = 38.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = fg,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BillCard(
    bill: BillEntity,
    isHighToday: Boolean,
    currency: String,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onTogglePaid: () -> Unit,
    onDuplicate: () -> Unit = {},
    onScheduleNextMonth: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMengiColors.current
    val isHousehold = remember(bill) { AutoAccountingEngine.isHouseholdBill(bill) }
    val isOverdue = remember(bill.paid, bill.dueDate) { bill.isOverdue }
    val subtitleText = remember(bill.payee, bill.category, bill.method, language) {
        listOf(bill.payee, FormatUtils.localizedCategory(bill.category, language))
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .ifBlank { FormatUtils.localizedMethod(bill.method, language) }
    }
    val formattedAmount = remember(bill.amount, currency) {
        FormatUtils.formatMoney(bill.amount, currency)
    }
    val friendlyDueDate = remember(bill.dueDate, language) {
        FormatUtils.friendlyDateLabel(bill.dueDate, language)
    }
    val highColors = bucketVisualColors(AutoBucket.HIGH_BILL_TODAY)
    val householdColors = bucketVisualColors(AutoBucket.HOUSEHOLD_BILL)
    val billColors = bucketVisualColors(AutoBucket.BILL)

    var showMenu by remember { mutableStateOf(false) }

    val (statusText, statusBg, statusFg) = when {
        isHighToday -> Triple(tr(language, "Kubwa Leo", "High Today"), highColors.softBg, highColors.ink)
        bill.paid -> Triple(tr(language, "Imelipwa", "Paid"), colors.incomeSoft, colors.income)
        isOverdue -> Triple(tr(language, "Imechelewa", "Overdue"), highColors.softBg, highColors.ink)
        else -> Triple(tr(language, "Inasubiri", "Upcoming"), colors.warningSoft, colors.warning)
    }

    val badgeBg = when {
        isHousehold -> householdColors.softBg
        bill.paid -> colors.incomeSoft
        isHighToday || isOverdue -> highColors.softBg
        else -> billColors.softBg
    }
    val badgeFg = when {
        isHousehold -> householdColors.dot
        bill.paid -> colors.income
        isHighToday || isOverdue -> highColors.ink
        else -> billColors.dot
    }

    MengiCard(
        modifier = modifier,
        onClick = onEdit,
        containerColor = if (isHighToday) highColors.softBg else MaterialTheme.colorScheme.surface,
        borderColor = when {
            isHighToday -> highColors.border
            isHousehold -> householdColors.border
            else -> null
        },
        contentPadding = 16.dp
    ) {
        // Row 1: Spacious Primary Info (Icon + Name/Payee + Amount/Due Date)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isHousehold -> Icons.Rounded.Home
                        bill.paid -> Icons.Rounded.CheckCircle
                        else -> Icons.AutoMirrored.Rounded.ReceiptLong
                    },
                    contentDescription = bill.name,
                    tint = badgeFg,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        bill.paid -> colors.income
                        isHousehold -> householdColors.ink
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1
                )
                Text(
                    text = friendlyDueDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue) highColors.ink else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = colors.cardBorder, thickness = 0.8.dp)
        Spacer(Modifier.height(10.dp))

        // Row 2: Clean Status Tags on Left + One-Tap Pay Action & Overflow Menu on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(text = statusText, containerColor = statusBg, contentColor = statusFg)
                if (isHousehold) {
                    StatusPill(
                        text = tr(language, "Bili ya Nyumbani", "Household"),
                        containerColor = householdColors.softBg,
                        contentColor = householdColors.ink
                    )
                }
                if (bill.recurring) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Rounded.Autorenew,
                            contentDescription = tr(language, "Inajirudia", "Recurring"),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(5.dp)
                                .size(13.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = onTogglePaid,
                    shape = CircleShape,
                    color = if (bill.paid) colors.incomeSoft else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .testTag("toggle_bill_paid_${bill.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (bill.paid) Icons.Rounded.Check else Icons.Rounded.Payments,
                            contentDescription = tr(language, "Badili hali ya malipo", "Toggle paid status"),
                            tint = if (bill.paid) colors.income else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (bill.paid) {
                                tr(language, "Imelipwa", "Paid")
                            } else {
                                tr(language, "Lipa", "Pay")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (bill.paid) colors.income else MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = tr(language, "Chaguo zaidi", "More options"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text(tr(language, "Hariri Bili", "Edit Bill")) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(tr(language, "Nakili kwa Mwezi Ujao", "Duplicate for Next Month")) },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )
                        if (bill.paid && bill.recurring) {
                            DropdownMenuItem(
                                text = { Text(tr(language, "Panga Mwezi Ujao", "Schedule Next Month")) },
                                leadingIcon = { Icon(Icons.Rounded.EventRepeat, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onScheduleNextMonth()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    tr(language, "Futa Bili", "Delete Bill"),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }

        if (bill.paid && bill.recurring) {
            Spacer(Modifier.height(10.dp))
            Surface(
                onClick = onScheduleNextMonth,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_next_bill_${bill.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.EventRepeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = tr(language, "Panga Bili ya Mwezi Ujao", "Schedule Next Month's Bill"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillEditorSheet(
    initial: BillEntity?,
    defaultHousehold: Boolean = false,
    knownParties: List<PartySummary>,
    currency: String = "TZS",
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (BillEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val learnedHouseholdNames = remember(knownParties) {
        PartyDirectoryEngine.householdNames(knownParties)
    }
    var isHouseholdBill by remember(initial, defaultHousehold) {
        mutableStateOf(initial?.let { AutoAccountingEngine.isHouseholdBill(it) } ?: defaultHousehold)
    }
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var payee by remember(initial) { mutableStateOf(initial?.payee ?: "") }
    var amountText by remember(initial, currency) {
        mutableStateOf(if (initial != null && initial.amount > 0) FormatUtils.formatEditableAmount(initial.amount, currency) else "")
    }
    var dueDate by remember(initial) { mutableStateOf(initial?.dueDate ?: FormatUtils.todayIso()) }
    var category by remember(initial, defaultHousehold) {
        mutableStateOf(
            initial?.category ?: if (defaultHousehold) {
                AutoAccountingEngine.HOUSEHOLD_CATEGORY
            } else {
                FormatUtils.billCategories.first()
            }
        )
    }
    var method by remember(initial) {
        mutableStateOf(FormatUtils.normalizePaymentMethod(initial?.method ?: "M-Pesa"))
    }
    var recurring by remember(initial) { mutableStateOf(initial?.recurring ?: false) }
    var paid by remember(initial) { mutableStateOf(initial?.paid ?: false) }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val quickBillTemplates = remember(language) {
        listOf(
            Triple(tr(language, "LUKU / Umeme", "Electricity (LUKU)"), "TANESCO", "Umeme & maji"),
            Triple(tr(language, "Maji (DAWASA)", "Water (DAWASA)"), "DAWASA", "Umeme & maji"),
            Triple(tr(language, "Kodi ya Pango", "Rent"), "", "Kodi ya pango"),
            Triple(tr(language, "Mtandao / Wi-Fi", "Internet / Wi-Fi"), "", "Mtandao & simu"),
            Triple(tr(language, "Mishahara", "Salaries / Payroll"), "", "Mishahara"),
            Triple(tr(language, "Kodi / TRA", "Tax / TRA"), "TRA", "Kodi & vibali"),
            Triple(tr(language, "Ulinzi & Usafi", "Security & Cleaning"), "", "Ulinzi & usafi"),
            Triple(tr(language, "Afya / NHIF", "Health / Insurance"), "NHIF", "Afya & bima"),
            Triple(tr(language, "Marejesho ya Mkopo", "Loan Installment"), "", "Mkopo & marejesho"),
            Triple(tr(language, "Manunuzi ya Bidhaa", "Inventory / Stock"), "", "Manunuzi ya bidhaa"),
            Triple(tr(language, "Bili ya Nyumbani", "Household Bill"), "", AutoAccountingEngine.HOUSEHOLD_CATEGORY)
        )
    }

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
            Text(
                text = if (initial == null) tr(language, "Bili Mpya", "New Bill") else tr(language, "Hariri Bili", "Edit Bill"),
                style = MaterialTheme.typography.headlineMedium
            )

            OneUiSegmentedTabs(
                options = listOf(
                    tr(language, "Bili Binafsi", "Personal Bill"),
                    tr(language, "Bili ya Nyumbani", "Household Bill")
                ),
                selectedIndex = if (isHouseholdBill) 1 else 0,
                onSelect = { idx ->
                    val homeSelected = idx == 1
                    isHouseholdBill = homeSelected
                    if (homeSelected) {
                        category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                        if (name.isBlank()) name = tr(language, "Bili ya Nyumbani", "Household Bill")
                    } else if (category.equals(AutoAccountingEngine.HOUSEHOLD_CATEGORY, ignoreCase = true)) {
                        category = FormatUtils.billCategories.first()
                    }
                },
                testTags = listOf("bill_type_personal", "bill_type_household")
            )

            Text(
                text = tr(language, "Chagua Bili ya Haraka", "Quick Bill Templates"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickBillTemplates.forEach { (presetName, presetPayee, presetCat) ->
                    val selectedPreset = name.equals(presetName, ignoreCase = true)
                    FilterChip(
                        selected = selectedPreset,
                        onClick = {
                            name = presetName
                            if (presetPayee.isNotBlank() && payee.isBlank()) {
                                payee = presetPayee
                            }
                            if (presetCat.equals(AutoAccountingEngine.HOUSEHOLD_CATEGORY, ignoreCase = true)) {
                                isHouseholdBill = true
                                category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                            } else {
                                isHouseholdBill = false
                                category = presetCat
                            }
                            errorText = null
                        },
                        shape = CircleShape,
                        leadingIcon = {
                            Icon(
                                imageVector = categoryIconForName(presetCat),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(presetName) }
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { newVal ->
                    name = newVal
                    errorText = null
                    val slash = AutoAccountingEngine.parseSlashTag(newVal)
                    when {
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.HOUSEHOLD_BILL -> {
                            isHouseholdBill = true
                            category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                        }
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.BILL -> {
                            isHouseholdBill = false
                            if (category.equals(AutoAccountingEngine.HOUSEHOLD_CATEGORY, ignoreCase = true)) {
                                category = FormatUtils.billCategories.first()
                            }
                        }
                        AutoAccountingEngine.isHouseholdText(newVal) -> {
                            isHouseholdBill = true
                            category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                        }
                    }
                },
                label = { Text(tr(language, "Jina la Bili *", "Bill Name *")) },
                placeholder = {
                    Text(
                        tr(
                            language,
                            "Mfano: Jina my/mybill au Jina/household",
                            "e.g. Name my/mybill or Name/household"
                        )
                    )
                },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("bill_name_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }; errorText = null },
                    label = { Text(tr(language, "Kiasi ($currency) *", "Amount ($currency) *")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f).testTag("bill_amount_input")
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text(tr(language, "Tarehe ya mwisho", "Due Date")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = payee,
                onValueChange = { newVal ->
                    payee = newVal
                    val slash = AutoAccountingEngine.parseSlashTag(newVal)
                    when {
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.HOUSEHOLD_BILL -> {
                            isHouseholdBill = true
                            category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                        }
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.BILL -> {
                            isHouseholdBill = false
                            if (category.equals(AutoAccountingEngine.HOUSEHOLD_CATEGORY, ignoreCase = true)) {
                                category = FormatUtils.billCategories.first()
                            }
                        }
                        AutoAccountingEngine.isHouseholdParty(newVal, learnedHouseholdNames) -> {
                            isHouseholdBill = true
                            category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                        }
                    }
                },
                label = { Text(tr(language, "Jina Lolote la Mpokeaji (hiari)", "Any Payee or Recipient Name (optional)")) },
                placeholder = {
                    Text(
                        tr(
                            language,
                            "Jina lolote au Jina/household, Jina my/mybill",
                            "Any name or Name/household, Name my/mybill"
                        )
                    )
                },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("bill_payee_input")
            )

            // Universal Name Recognition Bar across all modules
            RecognizedNameSuggestionsBar(
                query = payee,
                knownParties = knownParties,
                language = language,
                onSelectParty = { selected ->
                    payee = selected.name
                    if (selected.role == PartyRole.HOUSEHOLD) {
                        isHouseholdBill = true
                        category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                        if (name.isBlank()) name = tr(language, "Bili ya Nyumbani", "Household Bill")
                    }
                }
            )

            if (!isHouseholdBill) {
                Text(tr(language, "Kundi la Bili (${FormatUtils.billCategories.size})", "Bill Category (${FormatUtils.billCategories.size})"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormatUtils.billCategories.forEach { cat ->
                        FilterChip(
                            selected = category.equals(cat, ignoreCase = true),
                            onClick = { category = cat },
                            shape = CircleShape,
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIconForName(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(FormatUtils.localizedCategory(cat, language)) }
                        )
                    }
                }
            }

            Text(tr(language, "Mfumo wa Malipo (${FormatUtils.paymentMethods.size})", "Payment Method (${FormatUtils.paymentMethods.size})"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(language, "Bili inayojirudia kila mwezi", "Recurring monthly bill"), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = recurring, onCheckedChange = { recurring = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(language, "Tayari imelipwa", "Already paid"), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = paid, onCheckedChange = { paid = it })
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(tr(language, "Maelezo", "Notes")) },
                minLines = 2,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (name.isBlank() || amt <= 0.0) {
                        errorText = tr(language, "Jaza jina la bili na kiasi sahihi.", "Enter bill name and a valid amount.")
                        return@Button
                    }
                    val nameSlash = AutoAccountingEngine.parseSlashTag(name)
                    val cleanName = AutoAccountingEngine.stripSlashTag(name).ifBlank {
                        if (isHouseholdBill) tr(language, "Bili ya Nyumbani", "Household Bill")
                        else tr(language, "Bili", "Bill")
                    }
                    val cleanPayee = AutoAccountingEngine.stripSlashTag(payee).ifBlank {
                        if (nameSlash.hasSlashTag) nameSlash.cleanName else ""
                    }
                    val resolvedCategory = if (isHouseholdBill) {
                        AutoAccountingEngine.HOUSEHOLD_CATEGORY
                    } else {
                        category
                    }
                    val base = initial ?: BillEntity(id = "", name = cleanName)
                    onSave(
                        base.copy(
                            name = cleanName,
                            payee = cleanPayee,
                            category = resolvedCategory,
                            amount = amt,
                            dueDate = dueDate.ifBlank { FormatUtils.todayIso() },
                            method = method,
                            recurring = recurring,
                            paid = paid,
                            paidDate = if (paid) base.paidDate.ifBlank { FormatUtils.todayIso() } else "",
                            notes = notes.trim()
                        )
                    )
                },
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_bill_btn")
            ) {
                Text(tr(language, "Hifadhi Bili", "Save Bill"))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
