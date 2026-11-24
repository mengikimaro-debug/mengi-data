package tz.mengidata.app.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.data.accounting.AutoBucket
import tz.mengidata.app.data.accounting.ZeroLossLedgerSummary
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.ConfirmActionDialog
import tz.mengidata.app.ui.components.EmptyStateCard
import tz.mengidata.app.ui.components.MengiCard
import tz.mengidata.app.ui.components.OneUiFieldShape
import tz.mengidata.app.ui.components.OneUiFloatingFab
import tz.mengidata.app.ui.components.OneUiScreenHeader
import tz.mengidata.app.ui.components.OneUiSheetShape
import tz.mengidata.app.ui.components.StatusPill
import tz.mengidata.app.ui.components.bucketVisualColors
import tz.mengidata.app.ui.components.oneUiTextFieldColors
import tz.mengidata.app.ui.theme.LocalMengiColors
import java.util.Locale

@Composable
fun BillsScreen(
    bills: List<BillEntity>,
    zeroLossLedger: ZeroLossLedgerSummary,
    currency: String,
    language: AppLanguage,
    onSaveBill: (BillEntity) -> Unit,
    onTogglePaid: (String) -> Unit,
    onScheduleNextMonth: (String) -> Unit,
    onDeleteBill: (String) -> Unit,
    onExportBillsCsv: () -> Unit
) {
    val colors = LocalMengiColors.current
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var editingBill by remember { mutableStateOf<BillEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(bills, filter, search) {
        val q = search.trim().lowercase(Locale.ROOT)
        bills.filter { b ->
            val filterOk = when (filter) {
                "unpaid" -> !b.paid
                "overdue" -> b.isOverdue
                "paid" -> b.paid
                else -> true
            }
            val searchOk = q.isEmpty() ||
                b.name.lowercase(Locale.ROOT).contains(q) ||
                b.payee.lowercase(Locale.ROOT).contains(q) ||
                b.category.lowercase(Locale.ROOT).contains(q)
            filterOk && searchOk
        }
    }

    val unpaidTotal = remember(bills) { bills.filter { !it.paid }.sumOf { it.amount } }
    val overdueCount = remember(bills) { bills.count { it.isOverdue } }

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
                    eyebrow = tr(language, "Malipo Yanayokuja (Tenganishwa)", "Scheduled Payables (Separated)"),
                    title = tr(language, "Bili", "Bills"),
                    trailing = {
                        FilledTonalButton(
                            onClick = onExportBillsCsv,
                            shape = CircleShape,
                            modifier = Modifier.testTag("bills_screen_export_csv_btn")
                        ) {
                            Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(tr(language, "Bili CSV", "Bills CSV"), maxLines = 1)
                        }
                    }
                )
            }

            item(key = "summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MengiCard(modifier = Modifier.weight(1f), containerColor = colors.warningSoft, contentPadding = 16.dp) {
                        Text(tr(language, "Bili Zinazodaiwa", "Unpaid Total"), style = MaterialTheme.typography.labelMedium, color = colors.warning)
                        Text(FormatUtils.formatCompactMoney(unpaidTotal, currency), style = MaterialTheme.typography.titleLarge, color = colors.warning)
                    }
                    MengiCard(modifier = Modifier.weight(1f), containerColor = colors.expenseSoft, contentPadding = 16.dp) {
                        Text(tr(language, "Zilizochelewa", "Overdue"), style = MaterialTheme.typography.labelMedium, color = colors.expense)
                        Text("$overdueCount", style = MaterialTheme.typography.titleLarge, color = colors.expense)
                    }
                }
            }

            item(key = "search_and_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text(tr(language, "Tafuta bili au mlipwaji...", "Search bill or payee...")) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = CircleShape,
                        colors = oneUiTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("bills_search_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "all" to tr(language, "Zote", "All"),
                            "unpaid" to tr(language, "Hazijalipwa", "Unpaid"),
                            "overdue" to tr(language, "Zimechelewa", "Overdue"),
                            "paid" to tr(language, "Zimelipwa", "Paid")
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = filter == key,
                                onClick = { filter = key },
                                shape = CircleShape,
                                label = { Text(label) },
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
                        subtitle = tr(language, "Fuatilia kodi, umeme, mishahara na gharama za kila mwezi.", "Track rent, utilities, payroll, and recurring expenses."),
                        actionLabel = tr(language, "Ongeza Bili", "Add Bill"),
                        onAction = {
                            editingBill = null
                            showEditor = true
                        }
                    )
                }
            } else {
                items(filtered, key = { it.id }) { bill ->
                    val isHighToday = bill.id in zeroLossLedger.highBillIds
                    BillCard(
                        bill = bill,
                        isHighToday = isHighToday,
                        currency = currency,
                        language = language,
                        modifier = Modifier.animateItem(),
                        onTogglePaid = { onTogglePaid(bill.id) },
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
private fun BillCard(
    bill: BillEntity,
    isHighToday: Boolean,
    currency: String,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onTogglePaid: () -> Unit,
    onScheduleNextMonth: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMengiColors.current
    val highColors = bucketVisualColors(AutoBucket.HIGH_BILL_TODAY)
    val (statusText, bg, fg) = when {
        isHighToday -> Triple(tr(language, "Kubwa Leo!", "High Today!"), highColors.softBg, highColors.ink)
        bill.paid -> Triple(tr(language, "Imelipwa", "Paid"), colors.incomeSoft, colors.income)
        bill.isOverdue -> Triple(tr(language, "Imechelewa", "Overdue"), highColors.softBg, highColors.ink)
        else -> Triple(tr(language, "Inasubiri", "Upcoming"), colors.warningSoft, colors.warning)
    }

    MengiCard(
        modifier = modifier,
        containerColor = if (isHighToday) highColors.softBg else MaterialTheme.colorScheme.surface,
        borderColor = if (isHighToday) highColors.border else null,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onTogglePaid,
                modifier = Modifier.size(48.dp).testTag("toggle_bill_paid_${bill.id}")
            ) {
                Icon(
                    imageVector = if (bill.paid) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = tr(language, "Badili hali ya malipo", "Toggle paid status"),
                    tint = if (bill.paid) colors.income else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(bill.payee, bill.category).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(text = statusText, containerColor = bg, contentColor = fg)
                    Text(bill.dueDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (bill.recurring) {
                        Icon(
                            Icons.Rounded.Autorenew,
                            contentDescription = tr(language, "Inajirudia", "Recurring"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatMoney(bill.amount, currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = tr(language, "Hariri", "Edit"), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.Delete, contentDescription = tr(language, "Futa", "Delete"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        if (bill.paid && bill.recurring) {
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onScheduleNextMonth,
                    shape = CircleShape,
                    modifier = Modifier.testTag("schedule_next_bill_${bill.id}")
                ) {
                    Icon(Icons.Rounded.EventRepeat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(tr(language, "Panga Mwezi Ujao", "Schedule Next Month"))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillEditorSheet(
    initial: BillEntity?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (BillEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var payee by remember(initial) { mutableStateOf(initial?.payee ?: "") }
    var amountText by remember(initial) {
        mutableStateOf(if (initial != null && initial.amount > 0) initial.amount.toLong().toString() else "")
    }
    var dueDate by remember(initial) { mutableStateOf(initial?.dueDate ?: FormatUtils.todayIso()) }
    var category by remember(initial) { mutableStateOf(initial?.category ?: FormatUtils.expenseCategories.first()) }
    var method by remember(initial) { mutableStateOf(initial?.method ?: "Benki") }
    var recurring by remember(initial) { mutableStateOf(initial?.recurring ?: false) }
    var paid by remember(initial) { mutableStateOf(initial?.paid ?: false) }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

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

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorText = null },
                label = { Text(tr(language, "Jina la Bili *", "Bill Name *")) },
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
                onValueChange = { payee = it },
                label = { Text(tr(language, "Mpokeaji / Kampuni", "Payee / Vendor")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Text(tr(language, "Kundi", "Category"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormatUtils.expenseCategories.forEach { cat ->
                    FilterChip(selected = category == cat, onClick = { category = cat }, shape = CircleShape, label = { Text(cat) })
                }
            }

            Text(tr(language, "Njia ya Malipo", "Payment Method"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormatUtils.paymentMethods.forEach { m ->
                    FilterChip(selected = method == m, onClick = { method = m }, shape = CircleShape, label = { Text(m) })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(language, "Bili ya kila mwezi (Recurring)", "Recurring bill"), style = MaterialTheme.typography.bodyMedium)
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
                    val base = initial ?: BillEntity(id = "", name = name)
                    onSave(
                        base.copy(
                            name = name.trim(),
                            payee = payee.trim(),
                            category = category,
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
