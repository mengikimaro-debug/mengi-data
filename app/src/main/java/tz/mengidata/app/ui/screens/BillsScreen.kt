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
import tz.mengidata.app.data.accounting.PartyDirectoryEngine
import tz.mengidata.app.data.accounting.PartyRole
import tz.mengidata.app.data.accounting.PartySummary
import tz.mengidata.app.data.accounting.ZeroLossLedgerSummary
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
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
    val colors = LocalMengiColors.current
    val householdColors = bucketVisualColors(AutoBucket.HOUSEHOLD_BILL)
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var editingBill by remember { mutableStateOf<BillEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val unpaidCount = remember(bills) { bills.count { !it.paid } }
    val paidCount = remember(bills) { bills.count { it.paid } }
    val overdueCount = remember(bills) { bills.count { it.isOverdue } }
    val dueNowCount = remember(bills) {
        val today = FormatUtils.todayIso()
        bills.count { !it.paid && (it.isOverdue || it.dueDate <= today) }
    }
    val householdCount = remember(bills) { bills.count { AutoAccountingEngine.isHouseholdBill(it) } }
    val unpaidTotal = remember(bills) { bills.filter { !it.paid }.sumOf { it.amount } }
    val householdTotal = remember(bills) {
        bills.filter { AutoAccountingEngine.isHouseholdBill(it) }.sumOf { it.amount }
    }
    val paidFraction = remember(bills) {
        if (bills.isEmpty()) 0f else (paidCount.toFloat() / bills.size.toFloat()).coerceIn(0f, 1f)
    }

    val filtered = remember(bills, filter, search) {
        val q = search.trim().lowercase(Locale.ROOT)
        bills.filter { b ->
            val filterOk = when (filter) {
                "unpaid" -> !b.paid
                "overdue" -> b.isOverdue
                "paid" -> b.paid
                "household" -> AutoAccountingEngine.isHouseholdBill(b)
                else -> true
            }
            val searchOk = q.isEmpty() ||
                b.name.lowercase(Locale.ROOT).contains(q) ||
                b.payee.lowercase(Locale.ROOT).contains(q) ||
                b.category.lowercase(Locale.ROOT).contains(q) ||
                FormatUtils.localizedCategory(b.category, language).lowercase(Locale.ROOT).contains(q)
            filterOk && searchOk
        }.sortedWith(
            compareByDescending<BillEntity> { it.dueDate }
                .thenByDescending { it.createdAt }
        )
    }

    val unpaidSection = remember(filtered, filter) {
        if (filter == "all") filtered.filter { !it.paid } else emptyList()
    }
    val paidSection = remember(filtered, filter) {
        if (filter == "all") filtered.filter { it.paid } else emptyList()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                OneUiScreenHeader(
                    eyebrow = tr(language, "Malipo Yanayokuja", "Scheduled Payables"),
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

            // 2026 Solid Expressive Bills Overview Bento
            item(key = "summary") {
                MengiCard(contentPadding = 18.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(language, "Bili Zinazodaiwa", "Unpaid Bills Total"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = FormatUtils.formatMoney(unpaidTotal, currency),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = colors.incomeSoft
                        ) {
                            Text(
                                text = tr(
                                    language,
                                    "$paidCount/${bills.size} zimelipwa",
                                    "$paidCount/${bills.size} paid"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.income,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    // Solid 6dp completion progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(colors.subtleSurface)
                    ) {
                        if (paidFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(paidFraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(colors.income)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = householdColors.softBg,
                            border = BorderStroke(1.dp, householdColors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(householdColors.dot),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Home,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr(language, "Nyumbani", "Household"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = householdColors.ink,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = FormatUtils.formatCompactMoney(householdTotal, currency),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = householdColors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (overdueCount > 0) colors.warningSoft else colors.subtleSurface,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(
                                            if (overdueCount > 0) colors.warning else MaterialTheme.colorScheme.primary
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr(language, "Zilizochelewa", "Overdue"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (overdueCount > 0) colors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = tr(language, "$overdueCount bili", "$overdueCount bills"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (overdueCount > 0) colors.warning else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (dueNowCount > 0) {
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = onPayAllDueBills,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("bills_pay_all_due_btn")
                        ) {
                            Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = tr(
                                    language,
                                    "Lipa Bili Zote Zinazodaiwa Sasa ($dueNowCount)",
                                    "Pay All Due Bills Now ($dueNowCount)"
                                ),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            item(key = "search_and_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text(tr(language, "Tafuta bili au jina...", "Search bill or payee...")) },
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
                            "all" to tr(language, "Zote (${bills.size})", "All (${bills.size})"),
                            "unpaid" to tr(language, "Hazijalipwa ($unpaidCount)", "Unpaid ($unpaidCount)"),
                            "household" to tr(language, "Bili ya Nyumbani ($householdCount)", "Household ($householdCount)"),
                            "overdue" to tr(language, "Zimechelewa ($overdueCount)", "Overdue ($overdueCount)"),
                            "paid" to tr(language, "Zimelipwa ($paidCount)", "Paid ($paidCount)")
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

            if (filtered.isEmpty()) {
                item(key = "empty") {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = tr(language, "Hakuna bili", "No bills here"),
                        subtitle = tr(
                            language,
                            "Fuatilia kodi, umeme, bili ya nyumbani na gharama za kila mwezi.",
                            "Track rent, utilities, household bills, and recurring expenses."
                        ),
                        actionLabel = tr(language, "Ongeza Bili", "Add Bill"),
                        onAction = {
                            editingBill = null
                            showEditor = true
                        }
                    )
                }
            } else if (filter == "all" && unpaidSection.isNotEmpty() && paidSection.isNotEmpty()) {
                item(key = "section_unpaid") {
                    BillsSectionLabel(
                        title = tr(language, "Bili Zinazodaiwa", "Pending & Due"),
                        count = unpaidSection.size,
                        accentColor = colors.warning
                    )
                }
                items(
                    items = unpaidSection,
                    key = { it.id },
                    contentType = { "bill_card" }
                ) { bill ->
                    val isHighToday = bill.id in zeroLossLedger.highBillIds
                    BillCard(
                        bill = bill,
                        isHighToday = isHighToday,
                        currency = currency,
                        language = language,
                        onTogglePaid = { onTogglePaid(bill.id) },
                        onScheduleNextMonth = { onScheduleNextMonth(bill.id) },
                        onEdit = {
                            editingBill = bill
                            showEditor = true
                        },
                        onDelete = { pendingDeleteId = bill.id }
                    )
                }
                item(key = "section_paid") {
                    Spacer(Modifier.height(4.dp))
                    BillsSectionLabel(
                        title = tr(language, "Zilizolipwa", "Completed & Paid"),
                        count = paidSection.size,
                        accentColor = colors.income
                    )
                }
                items(
                    items = paidSection,
                    key = { it.id },
                    contentType = { "bill_card" }
                ) { bill ->
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
            } else {
                items(
                    items = filtered,
                    key = { it.id },
                    contentType = { "bill_card" }
                ) { bill ->
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
            knownParties = knownParties,
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
    val colors = LocalMengiColors.current
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg,
        border = if (selected) null else BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = 0.dp,
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
private fun BillsSectionLabel(
    title: String,
    count: Int,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
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
    knownParties: List<PartySummary>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (BillEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val learnedHouseholdNames = remember(knownParties) {
        PartyDirectoryEngine.householdNames(knownParties)
    }
    var isHouseholdBill by remember(initial) {
        mutableStateOf(initial?.let { AutoAccountingEngine.isHouseholdBill(it) } ?: false)
    }
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var payee by remember(initial) { mutableStateOf(initial?.payee ?: "") }
    var amountText by remember(initial) {
        mutableStateOf(if (initial != null && initial.amount > 0) initial.amount.toLong().toString() else "")
    }
    var dueDate by remember(initial) { mutableStateOf(initial?.dueDate ?: FormatUtils.todayIso()) }
    var category by remember(initial) {
        mutableStateOf(
            initial?.category ?: FormatUtils.billCategories.first()
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
                    tr(language, "Bili Zangu", "My Bill"),
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
                    label = { Text(tr(language, "Kiasi (TZS) *", "Amount *")) },
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
