package tz.mengidata.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AssistChip
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
import tz.mengidata.app.data.accounting.CategoryAnalyticsEngine
import tz.mengidata.app.data.accounting.PartyDirectoryEngine
import tz.mengidata.app.data.accounting.PartyRole
import tz.mengidata.app.data.accounting.PartySummary
import tz.mengidata.app.data.db.TransactionCategoryEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.CategoryAnalyticsCard
import tz.mengidata.app.ui.components.CategoryManagerSheet
import tz.mengidata.app.ui.components.ConfirmActionDialog
import tz.mengidata.app.ui.components.EmptyStateCard
import tz.mengidata.app.ui.components.MengiCard
import tz.mengidata.app.ui.components.OneUiFieldShape
import tz.mengidata.app.ui.components.OneUiFloatingFab
import tz.mengidata.app.ui.components.OneUiScreenHeader
import tz.mengidata.app.ui.components.OneUiSegmentedTabs
import tz.mengidata.app.ui.components.OneUiSheetShape
import tz.mengidata.app.ui.components.PeriodSelectorRow
import tz.mengidata.app.ui.components.RecognizedNameSuggestionsBar
import tz.mengidata.app.ui.components.accessibleCategoryAccent
import tz.mengidata.app.ui.components.bucketVisualColors
import tz.mengidata.app.ui.components.categoryIconForKey
import tz.mengidata.app.ui.components.oneUiTextFieldColors
import tz.mengidata.app.ui.theme.LocalMengiColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    categories: List<TransactionCategoryEntity>,
    knownParties: List<PartySummary> = emptyList(),
    selectedPeriod: PeriodFilter,
    currency: String,
    language: AppLanguage,
    openEditorTrigger: Int,
    onSelectPeriod: (PeriodFilter) -> Unit,
    onSaveTransaction: (TransactionEntity) -> Unit,
    onDuplicateTransaction: (TransactionEntity) -> Unit = {},
    onShareTransactionReceipt: (TransactionEntity) -> Unit = {},
    onDeleteTransaction: (String) -> Unit,
    onSaveCategory: (TransactionCategoryEntity, previousName: String?) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onExportStatementPdf: (PeriodFilter) -> Unit = {},
    onExportTransactionsCsv: () -> Unit,
    onExportCategoryAnalyticsCsv: (PeriodFilter) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("all") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var showCategoryStrip by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("list") } // "list" | "analytics"
    var editingTxn by remember { mutableStateOf<TransactionEntity?>(null) }
    var showEditor by remember(openEditorTrigger) { mutableStateOf(openEditorTrigger > 0) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var categoryManagerInitialType by remember { mutableStateOf("expense") }
    var pendingDeleteTxnId by remember { mutableStateOf<String?>(null) }

    val categoryLookup = remember(categories) {
        categories.associateBy { "${it.type}:${it.name.trim().lowercase()}" }
    }

    val usedCategoryNames = remember(transactions, categories) {
        val fromTxns = transactions.map { it.category.trim() }.filter { it.isNotBlank() }
        val fromDb = categories.map { it.name.trim() }.filter { it.isNotBlank() }
        (fromTxns + fromDb).distinct()
    }

    val analyticsReport = remember(transactions, categories, selectedPeriod, activeTab) {
        if (activeTab == "analytics") {
            CategoryAnalyticsEngine.analyze(transactions, categories, selectedPeriod)
        } else {
            null
        }
    }

    val filtered = remember(transactions, search, typeFilter, categoryFilter, selectedPeriod) {
        val q = search.trim().lowercase()
        transactions.filter { txn ->
            val periodOk = FormatUtils.isDateInPeriod(txn.date, selectedPeriod)
            val bucket = AutoAccountingEngine.classifyTransaction(txn)
            val matchesType = when (typeFilter) {
                "all" -> true
                "direct" -> txn.source != "bill" && !AutoAccountingEngine.isSavingsTransaction(txn) && !AutoAccountingEngine.isHouseholdTransaction(txn)
                "income" -> txn.isIncome
                "expense" -> !txn.isIncome && bucket == AutoBucket.EXPENSE
                "household" -> bucket == AutoBucket.HOUSEHOLD_BILL || AutoAccountingEngine.isHouseholdTransaction(txn)
                "bill" -> bucket == AutoBucket.BILL || (txn.source == "bill" && !AutoAccountingEngine.isHouseholdTransaction(txn))
                "savings" -> bucket == AutoBucket.SAVINGS || AutoAccountingEngine.isSavingsTransaction(txn)
                else -> true
            }
            val matchesCategory = categoryFilter == null ||
                txn.category.trim().equals(categoryFilter, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                txn.description.lowercase().contains(q) ||
                txn.party.lowercase().contains(q) ||
                txn.category.lowercase().contains(q) ||
                FormatUtils.localizedCategory(txn.category, language).lowercase().contains(q) ||
                txn.reference.lowercase().contains(q)
            periodOk && matchesType && matchesCategory && matchesQuery
        }
    }

    val groupedByDate = remember(filtered) {
        filtered
            .groupBy { it.date.ifBlank { FormatUtils.todayIso() } }
            .entries
            .sortedByDescending { it.key }
            .map { it.key to it.value }
    }

    val totalIn = remember(filtered) { filtered.filter { it.isIncome }.sumOf { it.amount } }
    val totalOut = remember(filtered) { filtered.filter { !it.isIncome }.sumOf { it.amount } }
    val netFiltered = totalIn - totalOut
    val colors = LocalMengiColors.current

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
                    eyebrow = tr(language, "Kitabu cha Fedha", "Cash Ledger"),
                    title = tr(language, "Miamala", "Transactions"),
                    trailing = {
                        Surface(
                            onClick = {
                                categoryManagerInitialType = "expense"
                                showCategoryManager = true
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("txns_manage_categories_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Category,
                                    contentDescription = tr(language, "Simamia Makundi", "Manage Categories"),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = { onExportStatementPdf(selectedPeriod) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("txns_export_statement_pdf_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PictureAsPdf,
                                    contentDescription = tr(language, "Pakua Taarifa PDF", "Export Statement PDF"),
                                    tint = colors.income,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                if (activeTab == "analytics") {
                                    onExportCategoryAnalyticsCsv(selectedPeriod)
                                } else {
                                    onExportTransactionsCsv()
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier.testTag("txns_screen_export_csv_btn")
                        ) {
                            Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(
                                text = if (activeTab == "analytics") {
                                    tr(language, "Makundi CSV", "Report CSV")
                                } else {
                                    tr(language, "CSV", "CSV")
                                },
                                maxLines = 1
                            )
                        }
                    }
                )
            }

            item(key = "view_mode_switcher") {
                OneUiSegmentedTabs(
                    options = listOf(
                        tr(language, "Orodha (${filtered.size})", "Ledger (${filtered.size})"),
                        tr(language, "Uchambuzi wa Makundi", "Category Analytics")
                    ),
                    selectedIndex = if (activeTab == "list") 0 else 1,
                    onSelect = { activeTab = if (it == 0) "list" else "analytics" },
                    testTags = listOf("txns_tab_list", "txns_tab_analytics")
                )
            }

            item(key = "ledger_period_selector") {
                PeriodSelectorRow(
                    selected = selectedPeriod,
                    language = language,
                    onSelect = onSelectPeriod
                )
            }

            if (activeTab == "analytics" && analyticsReport != null) {

                item(key = "analytics_breakdown") {
                    CategoryAnalyticsCard(
                        report = analyticsReport,
                        currency = currency,
                        language = language,
                        maxVisibleItems = 25,
                        onCategoryClick = { catName, catType ->
                            categoryFilter = catName
                            typeFilter = if (catType == "income") "income" else "all"
                            activeTab = "list"
                        },
                        onManageCategories = {
                            categoryManagerInitialType = "expense"
                            showCategoryManager = true
                        },
                        onExportCategoryCsv = { onExportCategoryAnalyticsCsv(selectedPeriod) }
                    )
                }
            } else {
                // 2026 3-Column Solid Ledger Summary Bento Card
                item(key = "summary_strip") {
                    MengiCard(contentPadding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LedgerMetricTile(
                                label = tr(language, "Mapato", "Income"),
                                value = FormatUtils.formatCompactMoney(totalIn, currency),
                                containerColor = colors.incomeSoft,
                                accentColor = colors.income,
                                modifier = Modifier.weight(1f)
                            )
                            LedgerMetricTile(
                                label = tr(language, "Matumizi", "Outflow"),
                                value = FormatUtils.formatCompactMoney(totalOut, currency),
                                containerColor = colors.expenseSoft,
                                accentColor = colors.expense,
                                modifier = Modifier.weight(1f)
                            )
                            LedgerMetricTile(
                                label = tr(language, "Salio", "Net"),
                                value = FormatUtils.formatCompactMoney(netFiltered, currency),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                accentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        val flowTotal = (totalIn + totalOut).coerceAtLeast(1.0)
                        val inShare = (totalIn / flowTotal).toFloat().coerceIn(0f, 1f)
                        val outShare = (totalOut / flowTotal).toFloat().coerceIn(0f, 1f)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(colors.subtleSurface),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (inShare > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .weight(inShare)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(colors.income)
                                )
                            }
                            if (outShare > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .weight(outShare)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(colors.expense)
                                )
                            }
                        }
                    }
                }

                item(key = "search_filters") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = {
                                Text(tr(language, "Tafuta maelezo, jina, kundi...", "Search description, party, category..."))
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
                                .testTag("txn_search_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                "all" to tr(language, "Yote (${transactions.size})", "All (${transactions.size})"),
                                "income" to tr(language, "Mapato", "Income"),
                                "expense" to tr(language, "Matumizi Yangu", "My Expenses"),
                                "household" to tr(language, "Bili ya Nyumbani", "Household Bill"),
                                "savings" to tr(language, "Akiba", "Savings"),
                                "bill" to tr(language, "Bili Zangu", "My Bills"),
                                "direct" to tr(language, "Miamala Pekee", "Direct Only")
                            ).forEach { (key, label) ->
                                LedgerSolidFilterPill(
                                    label = label,
                                    selected = typeFilter == key,
                                    onClick = { typeFilter = key },
                                    modifier = Modifier.testTag("txn_filter_$key")
                                )
                            }

                            // Compact toggle pill for Category filtering
                            Surface(
                                onClick = { showCategoryStrip = !showCategoryStrip },
                                shape = CircleShape,
                                color = if (showCategoryStrip || categoryFilter != null) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(1.dp, colors.cardBorder),
                                modifier = Modifier.heightIn(min = 38.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FilterList,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (categoryFilter != null) {
                                            FormatUtils.localizedCategory(categoryFilter!!, language)
                                        } else {
                                            tr(language, "Kundi", "Category")
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = showCategoryStrip || categoryFilter != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (categoryFilter != null) {
                                    Surface(
                                        onClick = { categoryFilter = null },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .heightIn(min = 34.dp)
                                            .testTag("clear_category_filter_chip")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = FormatUtils.localizedCategory(categoryFilter!!, language),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                maxLines = 1
                                            )
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = tr(language, "Ondoa kichujio", "Clear filter"),
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                usedCategoryNames.forEach { catName ->
                                    val selected = categoryFilter.equals(catName, ignoreCase = true)
                                    if (!selected) {
                                        LedgerSolidFilterPill(
                                            label = FormatUtils.localizedCategory(catName, language),
                                            selected = false,
                                            onClick = { categoryFilter = catName }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (groupedByDate.isEmpty()) {
                    item(key = "empty") {
                        EmptyStateCard(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            title = tr(language, "Hakuna miamala", "No transactions found"),
                            subtitle = tr(
                                language,
                                "Ongeza muamala mpya wa mapato, matumizi, bili ya nyumbani au akiba.",
                                "Add a new income, expense, household bill, or savings entry."
                            ),
                            actionLabel = tr(language, "Ongeza Muamala", "Add Transaction"),
                            onAction = {
                                editingTxn = null
                                showEditor = true
                            }
                        )
                    }
                } else {
                    groupedByDate.forEach { (dateIso, dayTxns) ->
                        item(
                            key = "date_hdr_$dateIso",
                            contentType = "date_header"
                        ) {
                            val dayNet = remember(dayTxns) {
                                dayTxns.sumOf { if (it.isIncome) it.amount else -it.amount }
                            }
                            val friendlyDate = remember(dateIso, language) {
                                FormatUtils.friendlyDateLabel(dateIso, language).uppercase()
                            }
                            val formattedDayNet = remember(dayNet, currency) {
                                (if (dayNet >= 0.0) "+" else "") + FormatUtils.formatCompactMoney(dayNet, currency)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = friendlyDate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${dayTxns.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = formattedDayNet,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dayNet >= 0.0) colors.income else colors.expense
                                )
                            }
                        }

                        itemsIndexed(
                            items = dayTxns,
                            key = { _, txn -> txn.id },
                            contentType = { _, _ -> "txn_row" }
                        ) { _, txn ->
                            val catMeta = remember(categoryLookup, txn.type, txn.category) {
                                categoryLookup["${txn.type}:${txn.category.trim().lowercase()}"]
                            }
                            MengiCard(contentPadding = 0.dp) {
                                LedgerTransactionRow(
                                    txn = txn,
                                    categoryMeta = catMeta,
                                    currency = currency,
                                    language = language,
                                    onEdit = {
                                        editingTxn = txn
                                        showEditor = true
                                    },
                                    onDuplicate = { onDuplicateTransaction(txn) },
                                    onShareReceipt = { onShareTransactionReceipt(txn) },
                                    onDelete = { pendingDeleteTxnId = txn.id }
                                )
                            }
                        }
                    }
                }
            }
        }

        OneUiFloatingFab(
            icon = Icons.Rounded.Add,
            contentDescription = tr(language, "Ongeza muamala", "Add transaction"),
            onClick = {
                editingTxn = null
                showEditor = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 96.dp)
                .testTag("add_transaction_fab")
        )
    }

    if (showEditor) {
        TransactionEditorSheet(
            initial = editingTxn,
            categories = categories,
            knownParties = knownParties,
            language = language,
            onDismiss = {
                showEditor = false
                editingTxn = null
            },
            onOpenCategoryManager = { activeType ->
                categoryManagerInitialType = activeType
                showCategoryManager = true
            },
            onSave = { txn ->
                onSaveTransaction(txn)
                showEditor = false
                editingTxn = null
            }
        )
    }

    if (showCategoryManager) {
        CategoryManagerSheet(
            categories = categories,
            initialType = categoryManagerInitialType,
            language = language,
            onDismiss = { showCategoryManager = false },
            onSaveCategory = onSaveCategory,
            onDeleteCategory = onDeleteCategory
        )
    }

    pendingDeleteTxnId?.let { id ->
        ConfirmActionDialog(
            title = tr(language, "Futa muamala?", "Delete transaction?"),
            message = tr(
                language,
                "Muamala huu utaondolewa kwenye kitabu cha fedha na salio litasasishwa.",
                "This transaction will be removed from your cash ledger and balances will update."
            ),
            confirmLabel = tr(language, "Futa", "Delete"),
            cancelLabel = tr(language, "Ghairi", "Cancel"),
            isDanger = true,
            onConfirm = {
                onDeleteTransaction(id)
                pendingDeleteTxnId = null
            },
            onDismiss = { pendingDeleteTxnId = null }
        )
    }
}

@Composable
private fun LedgerMetricTile(
    label: String,
    value: String,
    containerColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LedgerSolidFilterPill(
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
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
private fun LedgerTransactionRow(
    txn: TransactionEntity,
    categoryMeta: TransactionCategoryEntity?,
    currency: String,
    language: AppLanguage,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onShareReceipt: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMengiColors.current
    val bucket = remember(txn) { AutoAccountingEngine.classifyTransaction(txn) }
    val bColors = bucketVisualColors(bucket)
    val isHousehold = bucket == AutoBucket.HOUSEHOLD_BILL
    val isSavings = bucket == AutoBucket.SAVINGS
    var showMenu by remember { mutableStateOf(false) }

    val iconBg = when {
        isHousehold -> bColors.softBg
        isSavings -> colors.savingsSoft
        txn.isIncome -> colors.incomeSoft
        else -> colors.expenseSoft
    }
    val iconTint = when {
        isHousehold -> bColors.dot
        isSavings -> colors.savings
        txn.isIncome -> colors.income
        else -> categoryMeta?.let { accessibleCategoryAccent(it.colorHex) } ?: colors.expense
    }
    val amountColor = when {
        txn.isIncome -> colors.income
        isHousehold -> bColors.ink
        isSavings -> colors.savings
        else -> colors.expense
    }

    val localizedCat = remember(txn.category, language) {
        FormatUtils.localizedCategory(txn.category, language)
    }
    val localizedMethod = remember(txn.method, language) {
        FormatUtils.localizedMethod(txn.method, language)
    }
    val partyAndCat = remember(txn.party, localizedCat) {
        listOf(txn.party.trim(), localizedCat)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" · ")
    }
    val formattedAmount = remember(txn.amount, txn.isIncome, currency) {
        (if (txn.isIncome) "+" else "-") + FormatUtils.formatMoney(txn.amount, currency)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left: Crisp 44dp Squircle Icon Badge with subtle border
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = iconBg,
            border = BorderStroke(0.8.dp, bColors.border),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        isHousehold -> Icons.Rounded.Home
                        isSavings -> Icons.Rounded.Savings
                        categoryMeta != null -> categoryIconForKey(categoryMeta.iconKey)
                        txn.isIncome -> Icons.Rounded.ArrowDownward
                        else -> Icons.Rounded.ArrowUpward
                    },
                    contentDescription = localizedCat.ifBlank { txn.type },
                    tint = iconTint,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        // Center: Structured 3-Tier Data Hierarchy (Title -> Party/Category -> Bucket + Method Badges)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = txn.description.ifBlank { localizedCat },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (partyAndCat.isNotBlank()) {
                Text(
                    text = partyAndCat,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bColors.softBg,
                    border = BorderStroke(0.75.dp, bColors.border)
                ) {
                    Text(
                        text = bucket.label(language),
                        style = MaterialTheme.typography.labelSmall,
                        color = bColors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                    )
                }
                if (localizedMethod.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.subtleSurface,
                        border = BorderStroke(0.75.dp, colors.cardBorder)
                    ) {
                        Text(
                            text = localizedMethod,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                        )
                    }
                }
            }
        }

        // Right: Structured Amount + Clean Overflow Menu
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor,
                    maxLines = 1
                )
                if (txn.reference.isNotBlank()) {
                    Text(
                        text = "#${txn.reference}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = tr(language, "Chaguo zaidi", "More options"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DropdownMenuItem(
                        text = { Text(tr(language, "Hariri Muamala", "Edit Transaction")) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(tr(language, "Nakili kwa Leo", "Duplicate for Today")) },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(tr(language, "Tuma Risiti", "Share Receipt")) },
                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onShareReceipt()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                tr(language, "Futa Muamala", "Delete Transaction"),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditorSheet(
    initial: TransactionEntity?,
    categories: List<TransactionCategoryEntity>,
    knownParties: List<PartySummary>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onOpenCategoryManager: (activeType: String) -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val learnedHouseholdNames = remember(knownParties) {
        PartyDirectoryEngine.householdNames(knownParties)
    }
    var type by remember(initial) { mutableStateOf(initial?.type ?: "income") }
    var isHouseholdSelected by remember(initial) {
        mutableStateOf(initial?.let { AutoAccountingEngine.isHouseholdTransaction(it) } ?: false)
    }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var amountText by remember(initial) {
        mutableStateOf(if (initial != null && initial.amount > 0) initial.amount.toLong().toString() else "")
    }
    var party by remember(initial) { mutableStateOf(initial?.party ?: "") }

    val availableCats = remember(categories, type) {
        val matching = categories.filter { it.type == type }
        if (matching.isNotEmpty()) matching else CategoryAnalyticsEngine.defaultCategories().filter { it.type == type }
    }

    var category by remember(initial) {
        mutableStateOf(
            initial?.category?.takeIf { it.isNotBlank() }
                ?: availableCats.firstOrNull()?.name
                ?: FormatUtils.incomeCategories.first()
        )
    }
    var method by remember(initial) { mutableStateOf(initial?.method ?: FormatUtils.paymentMethods.first()) }
    var date by remember(initial) { mutableStateOf(initial?.date ?: FormatUtils.todayIso()) }
    var reference by remember(initial) { mutableStateOf(initial?.reference ?: "") }
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
                text = if (initial == null) {
                    tr(language, "Muamala Mpya", "New Transaction")
                } else {
                    tr(language, "Hariri Muamala", "Edit Transaction")
                },
                style = MaterialTheme.typography.headlineMedium
            )

            OneUiSegmentedTabs(
                options = listOf(
                    tr(language, "Mapato", "Income"),
                    tr(language, "Matumizi", "Expense"),
                    tr(language, "Nyumbani", "Household")
                ),
                selectedIndex = when {
                    isHouseholdSelected -> 2
                    type == "income" -> 0
                    else -> 1
                },
                onSelect = { idx ->
                    when (idx) {
                        0 -> {
                            isHouseholdSelected = false
                            type = "income"
                            val incomeNames = categories.filter { it.isIncome }.map { it.name }.ifEmpty { FormatUtils.incomeCategories }
                            if (category !in incomeNames) category = incomeNames.first()
                        }
                        1 -> {
                            isHouseholdSelected = false
                            type = "expense"
                            val expenseNames = categories.filter { !it.isIncome }.map { it.name }.ifEmpty { FormatUtils.expenseCategories }
                            if (category !in expenseNames || category.equals(AutoAccountingEngine.HOUSEHOLD_CATEGORY, ignoreCase = true)) {
                                category = expenseNames.first()
                            }
                        }
                        else -> {
                            isHouseholdSelected = true
                            type = "expense"
                            category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                            if (description.isBlank()) {
                                description = tr(language, "Bili ya Nyumbani", "Household Bill")
                            }
                        }
                    }
                },
                testTags = listOf("txn_type_income", "txn_type_expense", "txn_type_household")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { newVal ->
                    description = newVal
                    errorText = null
                    val slash = AutoAccountingEngine.parseSlashTag(newVal)
                    when {
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.HOUSEHOLD_BILL -> {
                            isHouseholdSelected = true
                            type = "expense"
                            category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                        }
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.MONEY_IN -> {
                            isHouseholdSelected = false
                            type = "income"
                            category = if (slash.isInvoiceTag) "Malipo ya ankara" else "Mauzo ya bidhaa"
                        }
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.SAVINGS -> {
                            isHouseholdSelected = false
                            type = "expense"
                            category = "Akiba"
                        }
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.BILL -> {
                            isHouseholdSelected = false
                            type = "expense"
                            category = "Bili"
                        }
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.EXPENSE -> {
                            isHouseholdSelected = false
                            type = "expense"
                        }
                        AutoAccountingEngine.isHouseholdText(newVal) -> {
                            isHouseholdSelected = true
                            type = "expense"
                            category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                        }
                    }
                },
                label = { Text(tr(language, "Maelezo *", "Description *")) },
                placeholder = {
                    Text(
                        tr(
                            language,
                            "Mfano: Jina/household, Jina/saving, au maelezo ya kawaida",
                            "e.g. Name/household, Name/saving, or any description"
                        )
                    )
                },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("txn_description_input")
            )

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
                        .testTag("txn_amount_input")
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
                onValueChange = { newVal ->
                    party = newVal
                    val slash = AutoAccountingEngine.parseSlashTag(newVal)
                    when {
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.HOUSEHOLD_BILL -> {
                            isHouseholdSelected = true
                            type = "expense"
                            category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                        }
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.MONEY_IN -> {
                            isHouseholdSelected = false
                            type = "income"
                            category = if (slash.isInvoiceTag) "Malipo ya ankara" else "Mauzo ya bidhaa"
                        }
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.SAVINGS -> {
                            isHouseholdSelected = false
                            type = "expense"
                            category = "Akiba"
                        }
                        slash.hasSlashTag && slash.detectedBucket == AutoBucket.BILL -> {
                            isHouseholdSelected = false
                            type = "expense"
                            category = "Bili"
                        }
                        AutoAccountingEngine.isHouseholdParty(newVal, learnedHouseholdNames) -> {
                            isHouseholdSelected = true
                            type = "expense"
                            category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                        }
                    }
                },
                label = { Text(tr(language, "Jina Lolote la Mhusika (hiari)", "Any Contact or Name (optional)")) },
                placeholder = {
                    Text(
                        tr(
                            language,
                            "Jina lolote au Jina/household, Jina/invoice, Jina/rentsaving",
                            "Any name or Name/household, Name/invoice, Name/rentsaving"
                        )
                    )
                },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("txn_party_input")
            )

            // Universal Name Recognition Bar (real saved names across Transactions, Bills, Savings & Invoices)
            RecognizedNameSuggestionsBar(
                query = party,
                knownParties = knownParties,
                language = language,
                onSelectParty = { selected ->
                    party = selected.name
                    if (selected.role == PartyRole.HOUSEHOLD) {
                        isHouseholdSelected = true
                        type = "expense"
                        category = AutoAccountingEngine.HOUSEHOLD_CATEGORY
                        if (description.isBlank()) {
                            description = tr(language, "Bili ya Nyumbani", "Household Bill")
                        }
                    }
                }
            )

            // Live Auto-Recognition Strip (Household vs Personal Expense vs Savings)
            val detected = remember(description, category, party, type, isHouseholdSelected, learnedHouseholdNames) {
                AutoAccountingEngine.classifyText(
                    description = description,
                    category = if (isHouseholdSelected) AutoAccountingEngine.HOUSEHOLD_CATEGORY else category,
                    party = party,
                    explicitType = type,
                    learnedHouseholdNames = learnedHouseholdNames
                )
            }
            if (isHouseholdSelected || description.isNotBlank() || party.isNotBlank()) {
                val dColors = bucketVisualColors(detected.bucket)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = dColors.softBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${tr(language, "Mfumo umetambua:", "System detected:")} ${detected.bucket.label(language)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = dColors.ink
                        )
                        Text(
                            text = detected.reason(language),
                            style = MaterialTheme.typography.bodySmall,
                            color = dColors.ink
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr(language, "Kundi la Biashara", "Business Category"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AssistChip(
                    onClick = { onOpenCategoryManager(type) },
                    shape = CircleShape,
                    label = { Text(tr(language, "+ Kundi Jipya", "+ New Category")) },
                    leadingIcon = { Icon(Icons.Rounded.Category, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("editor_new_category_chip")
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCats.forEach { cat ->
                    val accent = accessibleCategoryAccent(cat.colorHex)
                    FilterChip(
                        selected = category.equals(cat.name, ignoreCase = true),
                        onClick = { category = cat.name },
                        shape = CircleShape,
                        leadingIcon = {
                            Icon(
                                imageVector = categoryIconForKey(cat.iconKey),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(FormatUtils.localizedCategory(cat.name, language)) }
                    )
                }
            }

            Text(
                text = tr(language, "Njia ya Malipo", "Payment Method"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormatUtils.paymentMethods.forEach { m ->
                    FilterChip(
                        selected = method == m,
                        onClick = { method = m },
                        shape = CircleShape,
                        label = { Text(FormatUtils.localizedMethod(m, language)) }
                    )
                }
            }

            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text(tr(language, "Kumbukumbu / Risiti", "Reference")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(tr(language, "Maelezo ya ziada", "Notes")) },
                minLines = 2,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (description.isBlank() || amount <= 0.0) {
                        errorText = tr(
                            language,
                            "Tafadhali jaza maelezo na kiasi sahihi.",
                            "Please enter a description and valid amount."
                        )
                        return@Button
                    }
                    val descSlash = AutoAccountingEngine.parseSlashTag(description)
                    val partySlash = AutoAccountingEngine.parseSlashTag(party)
                    val cleanParty = AutoAccountingEngine.stripSlashTag(party).ifBlank {
                        if (descSlash.hasSlashTag) descSlash.cleanName else ""
                    }
                    val cleanDesc = AutoAccountingEngine.stripSlashTag(description).ifBlank {
                        descSlash.suggestedSavingsGoalTitle
                            ?: partySlash.suggestedSavingsGoalTitle
                            ?: detected.bucket.label(language)
                    }
                    val isHousehold = isHouseholdSelected || (type == "expense" && (
                        AutoAccountingEngine.isHouseholdParty(party, learnedHouseholdNames) ||
                            AutoAccountingEngine.isHouseholdText("$description $category")
                        ))
                    val isSavings = !isHousehold && type == "expense" && (
                        category.equals("Akiba", ignoreCase = true) ||
                            category.equals("Savings", ignoreCase = true) ||
                            detected.bucket == AutoBucket.SAVINGS
                        )
                    val resolvedSource = when {
                        isHousehold -> AutoAccountingEngine.HOUSEHOLD_SOURCE
                        isSavings -> "savings"
                        initial?.source == AutoAccountingEngine.HOUSEHOLD_SOURCE -> "manual"
                        else -> initial?.source ?: "manual"
                    }
                    val resolvedCategory = when {
                        isHousehold -> AutoAccountingEngine.HOUSEHOLD_CATEGORY
                        isSavings -> "Akiba"
                        else -> category
                    }
                    onSave(
                        (initial ?: TransactionEntity(id = "", type = type, date = date, description = cleanDesc)).copy(
                            type = type,
                            date = date.ifBlank { FormatUtils.todayIso() },
                            description = cleanDesc,
                            party = cleanParty,
                            category = resolvedCategory,
                            amount = amount,
                            method = method,
                            reference = reference.trim(),
                            notes = notes.trim(),
                            source = resolvedSource
                        )
                    )
                },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_btn")
            ) {
                Text(tr(language, "Hifadhi Muamala", "Save Transaction"))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
