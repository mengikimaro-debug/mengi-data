package tz.mengidata.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import tz.mengidata.app.data.db.TransactionCategoryEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.CategoryAnalyticsCard
import tz.mengidata.app.ui.components.CategoryManagerSheet
import tz.mengidata.app.ui.components.EmptyStateCard
import tz.mengidata.app.ui.components.MengiCard
import tz.mengidata.app.ui.components.OneUiFieldShape
import tz.mengidata.app.ui.components.OneUiFloatingFab
import tz.mengidata.app.ui.components.OneUiScreenHeader
import tz.mengidata.app.ui.components.OneUiSegmentedTabs
import tz.mengidata.app.ui.components.OneUiSheetShape
import tz.mengidata.app.ui.components.PeriodSelectorRow
import tz.mengidata.app.ui.components.StatusPill
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
    selectedPeriod: PeriodFilter,
    currency: String,
    language: AppLanguage,
    openEditorTrigger: Int,
    onSelectPeriod: (PeriodFilter) -> Unit,
    onSaveTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onSaveCategory: (TransactionCategoryEntity, previousName: String?) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onExportTransactionsCsv: () -> Unit,
    onExportCategoryAnalyticsCsv: (PeriodFilter) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("all") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf("list") } // "list" | "analytics"
    var editingTxn by remember { mutableStateOf<TransactionEntity?>(null) }
    var showEditor by remember(openEditorTrigger) { mutableStateOf(openEditorTrigger > 0) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var categoryManagerInitialType by remember { mutableStateOf("expense") }

    val categoryLookup = remember(categories) {
        categories.associateBy { "${it.type}:${it.name.trim().lowercase()}" }
    }

    val usedCategoryNames = remember(transactions, categories) {
        val fromTxns = transactions.map { it.category.trim() }.filter { it.isNotBlank() }
        val fromDb = categories.map { it.name.trim() }.filter { it.isNotBlank() }
        (fromTxns + fromDb).distinct()
    }

    val analyticsReport = remember(transactions, categories, selectedPeriod) {
        CategoryAnalyticsEngine.analyze(transactions, categories, selectedPeriod)
    }

    val filtered = remember(transactions, search, typeFilter, categoryFilter) {
        val q = search.trim().lowercase()
        transactions.filter { txn ->
            val bucket = AutoAccountingEngine.classifyTransaction(txn)
            val matchesType = when (typeFilter) {
                "all" -> true
                "direct" -> txn.source != "bill" && txn.source != "savings"
                "income" -> txn.isIncome
                "expense" -> !txn.isIncome && bucket == AutoBucket.EXPENSE
                "bill" -> bucket == AutoBucket.BILL || txn.source == "bill"
                "savings" -> bucket == AutoBucket.SAVINGS || txn.source == "savings"
                else -> true
            }
            val matchesCategory = categoryFilter == null ||
                txn.category.trim().equals(categoryFilter, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                txn.description.lowercase().contains(q) ||
                txn.party.lowercase().contains(q) ||
                txn.category.lowercase().contains(q) ||
                txn.reference.lowercase().contains(q)
            matchesType && matchesCategory && matchesQuery
        }
    }

    val totalIn = remember(filtered) { filtered.filter { it.isIncome }.sumOf { it.amount } }
    val totalOut = remember(filtered) { filtered.filter { !it.isIncome }.sumOf { it.amount } }
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
                    eyebrow = tr(language, "Kitabu cha Fedha (0% Upotevu)", "Cash Ledger (Zero-Loss)"),
                    title = tr(language, "Miamala", "Transactions"),
                    trailing = {
                        Surface(
                            onClick = {
                                categoryManagerInitialType = "expense"
                                showCategoryManager = true
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
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
                                    tr(language, "Miamala CSV", "Txns CSV")
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

            if (activeTab == "analytics") {
                item(key = "analytics_period") {
                    PeriodSelectorRow(
                        selected = selectedPeriod,
                        language = language,
                        onSelect = onSelectPeriod
                    )
                }

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
                item(key = "summary_strip") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MengiCard(modifier = Modifier.weight(1f), containerColor = colors.incomeSoft, contentPadding = 16.dp) {
                            Text(
                                text = tr(language, "Mapato", "Income"),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.income
                            )
                            Text(
                                text = FormatUtils.formatCompactMoney(totalIn, currency),
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.income
                            )
                        }
                        MengiCard(modifier = Modifier.weight(1f), containerColor = colors.expenseSoft, contentPadding = 16.dp) {
                            Text(
                                text = tr(language, "Matumizi", "Expenses"),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.expense
                            )
                            Text(
                                text = FormatUtils.formatCompactMoney(totalOut, currency),
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.expense
                            )
                        }
                    }
                }

                item(key = "search_filters") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = {
                                Text(tr(language, "Tafuta maelezo, mteja, kundi...", "Search description, party, category..."))
                            },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            singleLine = true,
                            shape = CircleShape,
                            colors = oneUiTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("txn_search_input")
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "all" to tr(language, "Yote (${transactions.size})", "All (${transactions.size})"),
                                "direct" to tr(language, "Miamala Pekee", "Txns Only"),
                                "income" to tr(language, "Pesa Kuingia", "Money In"),
                                "expense" to tr(language, "Matumizi", "Expenses"),
                                "bill" to tr(language, "Bili", "Bills"),
                                "savings" to tr(language, "Akiba", "Savings")
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = typeFilter == key,
                                    onClick = { typeFilter = key },
                                    shape = CircleShape,
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("txn_filter_$key")
                                )
                            }
                        }

                        // Category Filter Strip
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (categoryFilter != null) {
                                AssistChip(
                                    onClick = { categoryFilter = null },
                                    shape = CircleShape,
                                    label = { Text("${tr(language, "Kundi", "Category")}: $categoryFilter") },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = tr(language, "Ondoa kichujio", "Clear filter"),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("clear_category_filter_chip")
                                )
                            }
                            usedCategoryNames.forEach { catName ->
                                val selected = categoryFilter.equals(catName, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        categoryFilter = if (selected) null else catName
                                    },
                                    shape = CircleShape,
                                    label = { Text(catName, maxLines = 1) }
                                )
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        EmptyStateCard(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            title = tr(language, "Hakuna miamala", "No transactions found"),
                            subtitle = tr(
                                language,
                                "Ongeza muamala mpya wa mapato au matumizi.",
                                "Add a new income or expense record."
                            ),
                            actionLabel = tr(language, "Ongeza Muamala", "Add Transaction"),
                            onAction = {
                                editingTxn = null
                                showEditor = true
                            }
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { txn ->
                        val catMeta = categoryLookup["${txn.type}:${txn.category.trim().lowercase()}"]
                        TransactionItemCard(
                            txn = txn,
                            categoryMeta = catMeta,
                            currency = currency,
                            language = language,
                            modifier = Modifier.animateItem(),
                            onEdit = {
                                editingTxn = txn
                                showEditor = true
                            },
                            onDelete = { onDeleteTransaction(txn.id) }
                        )
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
}

@Composable
private fun TransactionItemCard(
    txn: TransactionEntity,
    categoryMeta: TransactionCategoryEntity?,
    currency: String,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMengiColors.current
    val catAccent = categoryMeta?.let { accessibleCategoryAccent(it.colorHex) }
        ?: if (txn.isIncome) colors.income else colors.expense

    MengiCard(modifier = modifier, contentPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (txn.isIncome) colors.incomeSoft else colors.expenseSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryMeta?.let { categoryIconForKey(it.iconKey) }
                        ?: if (txn.isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    contentDescription = txn.category.ifBlank { txn.type },
                    tint = if (txn.isIncome) colors.income else catAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.description.ifBlank { txn.category },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(txn.party, txn.category).filter { it.isNotBlank() }.joinToString(" · "),
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
                    val bucket = AutoAccountingEngine.classifyTransaction(txn)
                    val bColors = bucketVisualColors(bucket)
                    StatusPill(
                        text = bucket.label(language),
                        containerColor = bColors.softBg,
                        contentColor = bColors.ink
                    )
                    Text(
                        text = txn.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (txn.isIncome) "+" else "-") + FormatUtils.formatMoney(txn.amount, currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (txn.isIncome) colors.income else colors.expense
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = tr(language, "Hariri", "Edit"),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = tr(language, "Futa", "Delete"),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
    language: AppLanguage,
    onDismiss: () -> Unit,
    onOpenCategoryManager: (activeType: String) -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember(initial) { mutableStateOf(initial?.type ?: "income") }
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
                    tr(language, "Matumizi", "Expense")
                ),
                selectedIndex = if (type == "income") 0 else 1,
                onSelect = { idx ->
                    if (idx == 0) {
                        type = "income"
                        val incomeNames = categories.filter { it.isIncome }.map { it.name }.ifEmpty { FormatUtils.incomeCategories }
                        if (category !in incomeNames) category = incomeNames.first()
                    } else {
                        type = "expense"
                        val expenseNames = categories.filter { !it.isIncome }.map { it.name }.ifEmpty { FormatUtils.expenseCategories }
                        if (category !in expenseNames) category = expenseNames.first()
                    }
                }
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it; errorText = null },
                label = { Text(tr(language, "Maelezo *", "Description *")) },
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
                onValueChange = { party = it },
                label = { Text(tr(language, "Mteja / Msambazaji", "Customer / Supplier")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

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
                        label = { Text(cat.name) }
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
                        label = { Text(m) }
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
                    onSave(
                        (initial ?: TransactionEntity(id = "", type = type, date = date, description = description)).copy(
                            type = type,
                            date = date.ifBlank { FormatUtils.todayIso() },
                            description = description.trim(),
                            party = party.trim(),
                            category = category,
                            amount = amount,
                            method = method,
                            reference = reference.trim(),
                            notes = notes.trim()
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
