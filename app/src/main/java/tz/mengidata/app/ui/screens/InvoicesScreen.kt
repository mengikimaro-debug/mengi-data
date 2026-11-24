package tz.mengidata.app.ui.screens

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import tz.mengidata.app.data.db.BusinessSettingsEntity
import tz.mengidata.app.data.db.InvoiceEntity
import tz.mengidata.app.data.db.InvoiceItemEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.ConfirmActionDialog
import tz.mengidata.app.ui.components.EmptyStateCard
import tz.mengidata.app.ui.components.MengiCard
import tz.mengidata.app.ui.components.OneUiDialogShape
import tz.mengidata.app.ui.components.OneUiFieldShape
import tz.mengidata.app.ui.components.OneUiFloatingFab
import tz.mengidata.app.ui.components.OneUiScreenHeader
import tz.mengidata.app.ui.components.OneUiSegmentedTabs
import tz.mengidata.app.ui.components.OneUiSheetShape
import tz.mengidata.app.ui.components.StatusPill
import tz.mengidata.app.ui.components.oneUiTextFieldColors
import tz.mengidata.app.ui.theme.LocalMengiColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    invoices: List<InvoiceWithItems>,
    settings: BusinessSettingsEntity,
    language: AppLanguage,
    generateNextNumber: suspend () -> String,
    onSaveInvoice: (InvoiceEntity, List<InvoiceItemEntity>) -> Unit,
    onMarkPaid: (String) -> Unit,
    onRecordPayment: (String, Double) -> Unit,
    onDuplicateInvoice: (String) -> Unit,
    onShareInvoice: (InvoiceWithItems) -> Unit,
    onMarkUnpaid: (String) -> Unit,
    onDeleteInvoice: (String) -> Unit,
    onExportPdf: (InvoiceWithItems) -> Unit,
    onPrintPdf: (InvoiceWithItems) -> Unit
) {
    val currency = settings.currency.ifBlank { "TZS" }
    val colors = LocalMengiColors.current

    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var viewingInvoice by remember { mutableStateOf<InvoiceWithItems?>(null) }
    var editingInvoice by remember { mutableStateOf<InvoiceWithItems?>(null) }
    var partialPayTarget by remember { mutableStateOf<InvoiceWithItems?>(null) }
    var showBuilder by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(invoices, filter, search) {
        val q = search.trim().lowercase(Locale.ROOT)
        invoices.filter { inv ->
            val filterOk = when (filter) {
                "due" -> !inv.isDraft && !inv.isPaid
                "paid" -> inv.isPaid
                "draft" -> inv.isDraft
                else -> true
            }
            val searchOk = q.isEmpty() ||
                inv.invoice.customerName.lowercase(Locale.ROOT).contains(q) ||
                inv.invoice.number.lowercase(Locale.ROOT).contains(q) ||
                inv.items.any { it.description.lowercase(Locale.ROOT).contains(q) }
            filterOk && searchOk
        }
    }

    val totalReceived = remember(invoices) { invoices.filter { !it.isDraft }.sumOf { it.invoice.amountPaid } }
    val totalReceivables = remember(invoices) { invoices.filter { !it.isDraft }.sumOf { it.balance } }

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
                    eyebrow = tr(language, "Ankara za Biashara & PDF", "Business Invoicing & PDF"),
                    title = tr(language, "Ankara", "Invoices")
                )
            }

            item(key = "summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MengiCard(modifier = Modifier.weight(1f), containerColor = colors.incomeSoft, contentPadding = 16.dp) {
                        Text(tr(language, "Zilizopokelewa", "Collected"), style = MaterialTheme.typography.labelMedium, color = colors.income)
                        Text(FormatUtils.formatCompactMoney(totalReceived, currency), style = MaterialTheme.typography.titleLarge, color = colors.income)
                    }
                    MengiCard(modifier = Modifier.weight(1f), containerColor = colors.warningSoft, contentPadding = 16.dp) {
                        Text(tr(language, "Salio Linalodaiwa", "Balance Due"), style = MaterialTheme.typography.labelMedium, color = colors.warning)
                        Text(FormatUtils.formatCompactMoney(totalReceivables, currency), style = MaterialTheme.typography.titleLarge, color = colors.warning)
                    }
                }
            }

            item(key = "search_and_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text(tr(language, "Tafuta mteja au namba ya ankara...", "Search customer or invoice #...")) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = CircleShape,
                        colors = oneUiTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("invoices_search_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "all" to tr(language, "Zote", "All"),
                            "due" to tr(language, "Zinadaiwa", "Due"),
                            "paid" to tr(language, "Zimelipwa", "Paid"),
                            "draft" to tr(language, "Rasimu", "Draft")
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = filter == key,
                                onClick = { filter = key },
                                shape = CircleShape,
                                label = { Text(label) },
                                modifier = Modifier.testTag("inv_filter_$key")
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item(key = "empty") {
                    EmptyStateCard(
                        icon = Icons.Rounded.Description,
                        title = tr(language, "Hakuna ankara", "No invoices yet"),
                        subtitle = tr(language, "Unda ankara ya kitaalamu na pakua PDF kwa mteja wako.", "Create a branded invoice and export a clean PDF."),
                        actionLabel = tr(language, "Unda Ankara", "Create Invoice"),
                        onAction = {
                            editingInvoice = null
                            showBuilder = true
                        }
                    )
                }
            } else {
                items(filtered, key = { it.invoice.id }) { item ->
                    InvoiceRowCard(
                        item = item,
                        currency = currency,
                        language = language,
                        onClick = { viewingInvoice = item },
                        onMarkPaid = { onMarkPaid(item.invoice.id) }
                    )
                }
            }
        }

        OneUiFloatingFab(
            icon = Icons.Rounded.Add,
            contentDescription = tr(language, "Ankara mpya", "New invoice"),
            onClick = {
                editingInvoice = null
                showBuilder = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 96.dp)
                .testTag("add_invoice_fab")
        )
    }

    viewingInvoice?.let { current ->
        val fresh = invoices.firstOrNull { it.invoice.id == current.invoice.id } ?: current
        InvoiceDetailSheet(
            item = fresh,
            currency = currency,
            language = language,
            onDismiss = { viewingInvoice = null },
            onEdit = {
                viewingInvoice = null
                editingInvoice = fresh
                showBuilder = true
            },
            onMarkPaid = { onMarkPaid(fresh.invoice.id) },
            onOpenPartialPayment = { partialPayTarget = fresh },
            onDuplicate = {
                onDuplicateInvoice(fresh.invoice.id)
                viewingInvoice = null
            },
            onShare = { onShareInvoice(fresh) },
            onMarkUnpaid = { onMarkUnpaid(fresh.invoice.id) },
            onExportPdf = { onExportPdf(fresh) },
            onPrintPdf = { onPrintPdf(fresh) },
            onDelete = {
                viewingInvoice = null
                pendingDeleteId = fresh.invoice.id
            }
        )
    }

    partialPayTarget?.let { inv ->
        PartialInvoicePaymentDialog(
            item = inv,
            currency = currency,
            language = language,
            onDismiss = { partialPayTarget = null },
            onConfirm = { amount ->
                onRecordPayment(inv.invoice.id, amount)
                partialPayTarget = null
            }
        )
    }

    if (showBuilder) {
        InvoiceBuilderSheet(
            initial = editingInvoice,
            settings = settings,
            language = language,
            generateNextNumber = generateNextNumber,
            onDismiss = {
                showBuilder = false
                editingInvoice = null
            },
            onSave = { inv, items ->
                onSaveInvoice(inv, items)
                showBuilder = false
                editingInvoice = null
            }
        )
    }

    pendingDeleteId?.let { id ->
        ConfirmActionDialog(
            title = tr(language, "Futa ankara?", "Delete invoice?"),
            message = tr(
                language,
                "Ankara hii na muamala wa malipo uliounganishwa nayo vitaondolewa.",
                "This invoice and any linked payment transaction will be removed."
            ),
            confirmLabel = tr(language, "Futa", "Delete"),
            cancelLabel = tr(language, "Ghairi", "Cancel"),
            isDanger = true,
            onConfirm = {
                onDeleteInvoice(id)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null }
        )
    }
}

@Composable
private fun InvoiceRowCard(
    item: InvoiceWithItems,
    currency: String,
    language: AppLanguage,
    onClick: () -> Unit,
    onMarkPaid: () -> Unit
) {
    val colors = LocalMengiColors.current
    val (statusLabel, bg, fg) = when {
        item.isDraft -> Triple(tr(language, "Rasimu", "Draft"), colors.subtleSurface, MaterialTheme.colorScheme.onSurfaceVariant)
        item.isPaid -> Triple(tr(language, "Imelipwa", "Paid"), colors.incomeSoft, colors.income)
        item.isOverdue -> Triple(tr(language, "Imechelewa", "Overdue"), colors.expenseSoft, colors.expense)
        item.isPartial -> Triple(tr(language, "Sehemu", "Partial"), colors.warningSoft, colors.warning)
        else -> Triple(tr(language, "Inadaiwa", "Due"), colors.warningSoft, colors.warning)
    }

    MengiCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.invoice.number,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.invoice.customerName.ifBlank { "—" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.invoice.issueDate} → ${item.invoice.dueDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusPill(text = statusLabel, containerColor = bg, contentColor = fg)
                Text(
                    text = FormatUtils.formatMoney(item.total, currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (!item.isPaid && !item.isDraft && item.balance > 0) {
                    Text(
                        text = "${tr(language, "Salio", "Bal")}: ${FormatUtils.formatCompactMoney(item.balance, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.expense
                    )
                }
            }
        }
        if (!item.isPaid) {
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onMarkPaid, shape = CircleShape) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(tr(language, "Weka Imelipwa", "Mark Paid"))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceDetailSheet(
    item: InvoiceWithItems,
    currency: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onMarkPaid: () -> Unit,
    onOpenPartialPayment: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    onMarkUnpaid: () -> Unit,
    onExportPdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.invoice.number, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(item.invoice.customerName, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row {
                    IconButton(onClick = onShare, modifier = Modifier.testTag("share_invoice_btn")) {
                        Icon(Icons.Rounded.Share, contentDescription = tr(language, "Shiriki", "Share"))
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.testTag("duplicate_invoice_btn")) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = tr(language, "Nakili", "Duplicate"))
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = tr(language, "Hariri", "Edit"))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = tr(language, "Futa", "Delete"), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (item.invoice.customerPhone.isNotBlank() || item.invoice.customerAddress.isNotBlank()) {
                Text(
                    text = listOf(item.invoice.customerPhone, item.invoice.customerEmail, item.invoice.customerAddress)
                        .filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = LocalMengiColors.current.cardBorder.copy(alpha = 0.5f))

            item.orderedItems.forEach { line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(line.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${line.qty.toInt()} × ${FormatUtils.formatMoney(line.price, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(FormatUtils.formatMoney(line.lineTotal, currency), style = MaterialTheme.typography.titleSmall)
                }
            }

            HorizontalDivider(color = LocalMengiColors.current.cardBorder.copy(alpha = 0.5f))

            SummaryLine(tr(language, "Jumla ndogo", "Subtotal"), FormatUtils.formatMoney(item.subtotal, currency))
            if (item.effectiveDiscount > 0) {
                SummaryLine(tr(language, "Punguzo", "Discount"), "-${FormatUtils.formatMoney(item.effectiveDiscount, currency)}")
            }
            if (item.invoice.vatRate > 0) {
                SummaryLine("VAT (${item.invoice.vatRate}%)", FormatUtils.formatMoney(item.vatAmount, currency))
            }
            SummaryLine(tr(language, "Jumla Kuu", "Grand Total"), FormatUtils.formatMoney(item.total, currency), bold = true)
            SummaryLine(tr(language, "Imelipwa", "Paid"), FormatUtils.formatMoney(item.invoice.amountPaid, currency))
            SummaryLine(tr(language, "Salio", "Balance Due"), FormatUtils.formatMoney(item.balance, currency), bold = true)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onExportPdf,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("export_invoice_pdf_btn")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(tr(language, "Pakua PDF", "Save PDF"))
                }
                OutlinedButton(
                    onClick = onPrintPdf,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("print_invoice_pdf_btn")
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(tr(language, "Chapisha", "Print"))
                }
            }

            if (!item.isPaid) {
                OutlinedButton(
                    onClick = onOpenPartialPayment,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("partial_invoice_pay_btn")
                ) {
                    Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(tr(language, "Rekodi Malipo ya Awamu", "Record Partial Payment"))
                }
                Button(
                    onClick = onMarkPaid,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("mark_invoice_paid_btn")
                ) {
                    Text(tr(language, "Weka Imelipwa Yote", "Mark Fully Paid"))
                }
            } else {
                OutlinedButton(
                    onClick = onMarkUnpaid,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(tr(language, "Rudisha Inadaiwa", "Mark Unpaid"))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PartialInvoicePaymentDialog(
    item: InvoiceWithItems,
    currency: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = OneUiDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(tr(language, "Malipo ya Awamu", "Record Partial Payment")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${item.invoice.number} · ${tr(language, "Salio", "Balance")}: ${FormatUtils.formatMoney(item.balance, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(tr(language, "Kiasi kilicholipwa ($currency)", "Amount Received ($currency)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("partial_pay_amount_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) onConfirm(amt)
                },
                shape = CircleShape,
                modifier = Modifier.testTag("partial_pay_confirm_btn")
            ) {
                Text(tr(language, "Thibitisha", "Record"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = CircleShape) {
                Text(tr(language, "Ghairi", "Cancel"))
            }
        }
    )
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private data class DraftLineItem(
    val description: String = "",
    val qty: String = "1",
    val price: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceBuilderSheet(
    initial: InvoiceWithItems?,
    settings: BusinessSettingsEntity,
    language: AppLanguage,
    generateNextNumber: suspend () -> String,
    onDismiss: () -> Unit,
    onSave: (InvoiceEntity, List<InvoiceItemEntity>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var number by remember(initial) { mutableStateOf(initial?.invoice?.number ?: "") }
    var customerName by remember(initial) { mutableStateOf(initial?.invoice?.customerName ?: "") }
    var customerPhone by remember(initial) { mutableStateOf(initial?.invoice?.customerPhone ?: "") }
    var customerEmail by remember(initial) { mutableStateOf(initial?.invoice?.customerEmail ?: "") }
    var customerAddress by remember(initial) { mutableStateOf(initial?.invoice?.customerAddress ?: "") }
    var issueDate by remember(initial) { mutableStateOf(initial?.invoice?.issueDate ?: FormatUtils.todayIso()) }
    var dueDate by remember(initial) {
        mutableStateOf(initial?.invoice?.dueDate ?: FormatUtils.addDaysIso(FormatUtils.todayIso(), 14))
    }
    var state by remember(initial) { mutableStateOf(initial?.invoice?.state ?: "issued") }
    var discountText by remember(initial) {
        mutableStateOf(if ((initial?.invoice?.discount ?: 0.0) > 0) initial!!.invoice.discount.toLong().toString() else "")
    }
    var vatText by remember(initial) {
        val v = initial?.invoice?.vatRate ?: settings.vatRate
        mutableStateOf(if (v > 0) v.toString() else "0")
    }
    var paidText by remember(initial) {
        mutableStateOf(if ((initial?.invoice?.amountPaid ?: 0.0) > 0) initial!!.invoice.amountPaid.toLong().toString() else "")
    }
    var notes by remember(initial) { mutableStateOf(initial?.invoice?.notes ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val lineItems = remember(initial) {
        mutableStateListOf<DraftLineItem>().apply {
            if (initial != null && initial.orderedItems.isNotEmpty()) {
                initial.orderedItems.forEach {
                    add(DraftLineItem(it.description, it.qty.toInt().toString(), it.price.toLong().toString()))
                }
            } else {
                add(DraftLineItem())
            }
        }
    }

    LaunchedEffect(initial) {
        if (number.isBlank()) {
            number = generateNextNumber()
        }
    }

    val subtotal = lineItems.sumOf { (it.qty.toDoubleOrNull() ?: 0.0) * (it.price.toDoubleOrNull() ?: 0.0) }
    val discount = (discountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotal)
    val vatRate = (vatText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val total = subtotal - discount + ((subtotal - discount) * vatRate / 100.0)
    val currency = settings.currency.ifBlank { "TZS" }

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (initial == null) tr(language, "Ankara Mpya", "New Invoice") else tr(language, "Hariri Ankara", "Edit Invoice"),
                style = MaterialTheme.typography.headlineMedium
            )

            OneUiSegmentedTabs(
                options = listOf(
                    tr(language, "Rasmi (Imetolewa)", "Issued"),
                    tr(language, "Rasimu", "Draft")
                ),
                selectedIndex = if (state == "issued") 0 else 1,
                onSelect = { state = if (it == 0) "issued" else "draft" }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(tr(language, "Namba", "Number")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text(tr(language, "Mwisho", "Due Date")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it; errorText = null },
                label = { Text(tr(language, "Jina la Mteja *", "Customer Name *")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("invoice_customer_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text(tr(language, "Simu", "Phone")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = customerEmail,
                    onValueChange = { customerEmail = it },
                    label = { Text(tr(language, "Barua pepe", "Email")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = customerAddress,
                onValueChange = { customerAddress = it },
                label = { Text(tr(language, "Anwani ya Mteja", "Address")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = LocalMengiColors.current.cardBorder.copy(alpha = 0.5f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(language, "Bidhaa / Huduma", "Line Items"), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { lineItems.add(DraftLineItem()) }, shape = CircleShape) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(tr(language, "Ongeza mstari", "Add item"))
                }
            }

            lineItems.forEachIndexed { idx, row ->
                MengiCard(containerColor = LocalMengiColors.current.subtleSurface.copy(alpha = 0.45f), contentPadding = 14.dp) {
                    OutlinedTextField(
                        value = row.description,
                        onValueChange = { lineItems[idx] = row.copy(description = it) },
                        label = { Text(tr(language, "Maelezo ya bidhaa/huduma", "Item description")) },
                        singleLine = true,
                        shape = OneUiFieldShape,
                        colors = oneUiTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = row.qty,
                            onValueChange = { lineItems[idx] = row.copy(qty = it.filter { c -> c.isDigit() || c == '.' }) },
                            label = { Text(tr(language, "Idadi", "Qty")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = OneUiFieldShape,
                            colors = oneUiTextFieldColors(),
                            modifier = Modifier.weight(0.7f)
                        )
                        OutlinedTextField(
                            value = row.price,
                            onValueChange = { lineItems[idx] = row.copy(price = it.filter { c -> c.isDigit() || c == '.' }) },
                            label = { Text(tr(language, "Bei ($currency)", "Unit Price")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = OneUiFieldShape,
                            colors = oneUiTextFieldColors(),
                            modifier = Modifier.weight(1.3f)
                        )
                        if (lineItems.size > 1) {
                            IconButton(onClick = { lineItems.removeAt(idx) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(tr(language, "Punguzo", "Discount")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = vatText,
                    onValueChange = { vatText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("VAT (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(0.8f)
                )
                OutlinedTextField(
                    value = paidText,
                    onValueChange = { paidText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(tr(language, "Amelipa", "Paid")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
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

            MengiCard(containerColor = LocalMengiColors.current.subtleSurface, contentPadding = 16.dp) {
                SummaryLine(tr(language, "Jumla Kuu", "Grand Total"), FormatUtils.formatMoney(total, currency), bold = true)
            }

            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val validItems = lineItems.mapIndexedNotNull { idx, item ->
                        val desc = item.description.trim()
                        val qty = item.qty.toDoubleOrNull() ?: 0.0
                        val price = item.price.toDoubleOrNull() ?: 0.0
                        if (desc.isNotEmpty() && qty > 0.0 && price >= 0.0) {
                            InvoiceItemEntity(invoiceId = "", description = desc, qty = qty, price = price, position = idx)
                        } else null
                    }
                    if (customerName.isBlank() || validItems.isEmpty()) {
                        errorText = tr(
                            language,
                            "Weka jina la mteja na angalau bidhaa/huduma moja.",
                            "Enter customer name and at least one valid line item."
                        )
                        return@Button
                    }
                    val paidAmount = (paidText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    val base = initial?.invoice ?: InvoiceEntity(id = "", number = number, customerName = customerName)
                    onSave(
                        base.copy(
                            number = number.trim(),
                            customerName = customerName.trim(),
                            customerPhone = customerPhone.trim(),
                            customerEmail = customerEmail.trim(),
                            customerAddress = customerAddress.trim(),
                            issueDate = issueDate.ifBlank { FormatUtils.todayIso() },
                            dueDate = dueDate.ifBlank { FormatUtils.todayIso() },
                            discount = discount,
                            vatRate = vatRate,
                            amountPaid = paidAmount,
                            state = state,
                            paidDate = if (paidAmount > 0.0 && base.paidDate.isBlank()) FormatUtils.todayIso() else base.paidDate,
                            notes = notes.trim()
                        ),
                        validItems
                    )
                },
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_invoice_btn")
            ) {
                Text(tr(language, "Hifadhi Ankara", "Save Invoice"))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
