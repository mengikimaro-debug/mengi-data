package tz.mengidata.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.data.accounting.AutoAccountingEngine
import tz.mengidata.app.data.accounting.PartySummary
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.SavingsLogEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.AnimatedMoneyText
import tz.mengidata.app.ui.components.ConfirmActionDialog
import tz.mengidata.app.ui.components.EmptyStateCard
import tz.mengidata.app.ui.components.MengiCard
import tz.mengidata.app.ui.components.OneUiDialogShape
import tz.mengidata.app.ui.components.OneUiFieldShape
import tz.mengidata.app.ui.components.OneUiFloatingFab
import tz.mengidata.app.ui.components.OneUiScreenHeader
import tz.mengidata.app.ui.components.OneUiSheetShape
import tz.mengidata.app.ui.components.RecognizedNameSuggestionsBar
import tz.mengidata.app.ui.components.StatusPill
import tz.mengidata.app.ui.components.oneUiTextFieldColors
import tz.mengidata.app.ui.theme.LocalMengiColors

@Composable
fun HubScreen(
    savingsGoals: List<SavingsGoalEntity>,
    savingsLogs: List<SavingsLogEntity>,
    knownParties: List<PartySummary> = emptyList(),
    currency: String,
    language: AppLanguage,
    onSaveGoal: (SavingsGoalEntity) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onRecordSavingsTxn: (goalId: String, type: String, amount: Double, note: String, partyName: String) -> Unit,
    onExportSavingsCsv: () -> Unit
) {
    var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var showGoalEditor by remember { mutableStateOf(false) }
    var txnTarget by remember { mutableStateOf<Pair<SavingsGoalEntity, String>?>(null) }
    var logsGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var deleteGoalId by remember { mutableStateOf<String?>(null) }
    var goalFilter by remember { mutableStateOf("all") }

    val colors = LocalMengiColors.current
    val activeGoalsCount = remember(savingsGoals) {
        savingsGoals.count { it.targetAmount <= 0.0 || it.currentAmount < it.targetAmount }
    }
    val completedGoalsCount = remember(savingsGoals) {
        savingsGoals.count { it.targetAmount > 0.0 && it.currentAmount >= it.targetAmount }
    }
    val filteredGoals = remember(savingsGoals, goalFilter) {
        when (goalFilter) {
            "active" -> savingsGoals.filter { it.targetAmount <= 0.0 || it.currentAmount < it.targetAmount }
            "done" -> savingsGoals.filter { it.targetAmount > 0.0 && it.currentAmount >= it.targetAmount }
            else -> savingsGoals
        }
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
                    eyebrow = tr(language, "Mfuko wa Malengo", "Savings Vault"),
                    title = tr(language, "Akiba", "Savings"),
                    trailing = {
                        FilledTonalButton(
                            onClick = onExportSavingsCsv,
                            shape = CircleShape,
                            modifier = Modifier.testTag("savings_export_csv_btn")
                        ) {
                            Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(tr(language, "Akiba CSV", "Savings CSV"), maxLines = 1)
                        }
                    }
                )
            }

            item(key = "savings_summary") {
                OneUiSavingsVaultSummaryCard(
                    goals = savingsGoals,
                    currency = currency,
                    language = language
                )
            }

            if (savingsGoals.isNotEmpty()) {
                item(key = "savings_filters") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "all" to "${tr(language, "Zote", "All")} (${savingsGoals.size})",
                            "active" to "${tr(language, "Zinaendelea", "In Progress")} ($activeGoalsCount)",
                            "done" to "${tr(language, "Zimekamilika", "Completed")} ($completedGoalsCount)"
                        ).forEach { (key, label) ->
                            val selected = goalFilter == key
                            Surface(
                                onClick = { goalFilter = key },
                                shape = CircleShape,
                                color = when {
                                    selected && colors.isDark -> Color(0xFF2B425B)
                                    selected -> Color(0xFF253447)
                                    colors.isDark -> Color(0xFF171C24)
                                    else -> Color(0xFFFAF8F4)
                                },
                                border = when {
                                    selected && colors.isDark -> BorderStroke(1.dp, Color(0xFF496A8C))
                                    colors.isDark -> BorderStroke(1.dp, Color(0xFF293240))
                                    selected -> BorderStroke(1.dp, Color(0xFF253447))
                                    else -> BorderStroke(1.dp, Color(0xFFC2C4CC))
                                },
                                tonalElevation = if (selected) 1.dp else 0.dp,
                                modifier = Modifier.testTag("savings_filter_$key")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = when {
                                        selected && colors.isDark -> Color(0xFFEBF0F7)
                                        selected -> Color(0xFFF6F5F2)
                                        colors.isDark -> Color(0xFFB5C2D2)
                                        else -> Color(0xFF253447)
                                    },
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (filteredGoals.isEmpty()) {
                item(key = "empty_savings") {
                    EmptyStateCard(
                        icon = Icons.Rounded.Savings,
                        title = tr(language, "Hakuna malengo ya akiba", "No savings goals yet"),
                        subtitle = tr(language, "Tenga akiba ya dharura, kodi ya pango, au upanuzi wa biashara.", "Ring-fence funds for emergencies, rent, or expansion."),
                        actionLabel = tr(language, "Ongeza Lengo", "Add Goal"),
                        onAction = {
                            editingGoal = null
                            showGoalEditor = true
                        }
                    )
                }
            } else {
                items(
                    items = filteredGoals,
                    key = { it.id },
                    contentType = { "savings_goal_card" }
                ) { goal ->
                    OneUiSavingsGoalCard(
                        goal = goal,
                        currency = currency,
                        language = language,
                        onDeposit = { txnTarget = goal to "deposit" },
                        onWithdraw = { txnTarget = goal to "withdraw" },
                        onHistory = { logsGoal = goal },
                        onEdit = {
                            editingGoal = goal
                            showGoalEditor = true
                        },
                        onDelete = { deleteGoalId = goal.id }
                    )
                }
            }
        }

        OneUiFloatingFab(
            icon = Icons.Rounded.Add,
            contentDescription = tr(language, "Lengo jipya la akiba", "New savings goal"),
            onClick = {
                editingGoal = null
                showGoalEditor = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 96.dp)
                .testTag("hub_add_fab")
        )
    }

    if (showGoalEditor) {
        SavingsGoalEditorSheet(
            initial = editingGoal,
            knownParties = knownParties,
            currency = currency,
            language = language,
            onDismiss = { showGoalEditor = false; editingGoal = null },
            onSave = { goal ->
                onSaveGoal(goal)
                showGoalEditor = false
                editingGoal = null
            }
        )
    }

    txnTarget?.let { (goal, type) ->
        SavingsTransactionDialog(
            goal = goal,
            type = type,
            knownParties = knownParties,
            currency = currency,
            language = language,
            onDismiss = { txnTarget = null },
            onSubmit = { amount, note, partyName ->
                onRecordSavingsTxn(goal.id, type, amount, note, partyName)
                txnTarget = null
            }
        )
    }

    logsGoal?.let { goal ->
        val logs = remember(savingsLogs, goal.id) {
            savingsLogs.filter { it.savingsId == goal.id }
        }
        SavingsHistorySheet(
            goal = goal,
            logs = logs,
            currency = currency,
            language = language,
            onDismiss = { logsGoal = null }
        )
    }

    deleteGoalId?.let { id ->
        ConfirmActionDialog(
            title = tr(language, "Futa lengo la akiba?", "Delete savings goal?"),
            message = tr(language, "Lengo hili na historia yake ya miamala vitaondolewa.", "This savings goal and its transaction history will be deleted."),
            confirmLabel = tr(language, "Futa", "Delete"),
            cancelLabel = tr(language, "Ghairi", "Cancel"),
            isDanger = true,
            onConfirm = { onDeleteGoal(id); deleteGoalId = null },
            onDismiss = { deleteGoalId = null }
        )
    }
}

@Composable
private fun OneUiSavingsVaultSummaryCard(
    goals: List<SavingsGoalEntity>,
    currency: String,
    language: AppLanguage
) {
    val colors = LocalMengiColors.current
    val totalSaved = remember(goals) { goals.sumOf { it.currentAmount } }
    val totalTarget = remember(goals) { goals.sumOf { it.targetAmount } }
    val ratio = remember(totalSaved, totalTarget) {
        if (totalTarget > 0.0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f
    }
    val pct = (ratio * 100f).toInt()
    val formattedTarget = remember(totalTarget, currency) {
        FormatUtils.formatCompactMoney(totalTarget, currency)
    }
    val formattedRemaining = remember(totalTarget, totalSaved, currency) {
        val remaining = (totalTarget - totalSaved).coerceAtLeast(0.0)
        FormatUtils.formatCompactMoney(remaining, currency)
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.heroStart,
        border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334254) else Color(0xFF3A4E66)),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(language, "JUMLA YA AKIBA", "TOTAL VAULT SAVINGS"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (colors.isDark) Color(0xFFBCCAD8) else Color(0xFFD2DCE8)
                    )
                    StatusPill(
                        text = "$pct% · ${goals.size} ${tr(language, "malengo", "goals")}",
                        containerColor = if (colors.isDark) Color(0xFF2B3A4C) else Color(0xFFECEAE3),
                        contentColor = if (colors.isDark) Color(0xFFE6ECF2) else Color(0xFF1F2B3A)
                    )
                }

                AnimatedMoneyText(
                    amount = totalSaved,
                    currency = currency,
                    style = MaterialTheme.typography.displayLarge,
                    color = if (colors.isDark) Color(0xFFECEFF4) else Color(0xFFF6F5F2)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF121821) else Color(0xFF17212D))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio.coerceAtLeast(if (totalSaved > 0) 0.03f else 0f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (colors.isDark) Color(0xFF7FA8D6) else Color(0xFFE5DFD3))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${tr(language, "Lengo", "Target")}: $formattedTarget",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (colors.isDark) Color(0xFFB6C3D2) else Color(0xFFCFD9E6)
                    )
                    Text(
                        text = "${tr(language, "Baki", "Remaining")}: $formattedRemaining",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (colors.isDark) Color(0xFFB6C3D2) else Color(0xFFCFD9E6)
                    )
                }
            }
        }
    }
}

@Composable
private fun OneUiSavingsGoalCard(
    goal: SavingsGoalEntity,
    currency: String,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onHistory: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMengiColors.current
    val progress = remember(goal.currentAmount, goal.targetAmount) {
        if (goal.targetAmount > 0.0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    }
    val pct = (progress * 100f).toInt()
    val formattedCurrent = remember(goal.currentAmount, currency) {
        FormatUtils.formatMoney(goal.currentAmount, currency)
    }
    val formattedTarget = remember(goal.targetAmount, currency) {
        "/ ${FormatUtils.formatCompactMoney(goal.targetAmount, currency)}"
    }

    MengiCard(modifier = modifier) {
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
                        .background(colors.savingsSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Savings,
                        contentDescription = goal.title,
                        tint = colors.savings,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (goal.targetDate.isNotBlank()) {
                        Text(
                            text = "${tr(language, "Tarehe ya lengo", "Target date")}: ${goal.targetDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            StatusPill(
                text = "$pct%",
                containerColor = if (pct >= 100) colors.incomeSoft else colors.savingsSoft,
                contentColor = if (pct >= 100) colors.income else colors.savings
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = formattedCurrent,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formattedTarget,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(colors.subtleSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceAtLeast(if (goal.currentAmount > 0) 0.03f else 0f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(if (pct >= 100) colors.income else MaterialTheme.colorScheme.primary)
            )
        }

        val remainingGoal = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        if (remainingGoal > 0.0 && goal.targetDate.isNotBlank()) {
            val daysLeft = remember(goal.targetDate) {
                runCatching {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val targetMs = sdf.parse(goal.targetDate)?.time ?: 0L
                    val nowMs = System.currentTimeMillis()
                    ((targetMs - nowMs) / (1000L * 60L * 60L * 24L)).toInt()
                }.getOrDefault(0)
            }
            if (daysLeft > 0) {
                val monthsLeft = (daysLeft / 30.0).coerceAtLeast(1.0)
                val monthlyPace = remainingGoal / monthsLeft
                Spacer(Modifier.height(8.dp))
                Text(
                    text = tr(
                        language,
                        "Kasi ya kufikia lengo: ${FormatUtils.formatCompactMoney(monthlyPace, currency)}/mwezi · siku $daysLeft zimebaki",
                        "Target pace: ${FormatUtils.formatCompactMoney(monthlyPace, currency)}/mo · $daysLeft days left"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.savings
                )
            }
        }

        if (goal.notes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = goal.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onDeposit,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(tr(language, "Weka", "Deposit"), maxLines = 1)
                }
                OutlinedButton(
                    onClick = onWithdraw,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(tr(language, "Toa", "Withdraw"), maxLines = 1)
                }
            }
            Row {
                IconButton(onClick = onHistory, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.History, contentDescription = tr(language, "Historia", "History"), modifier = Modifier.size(19.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = tr(language, "Hariri", "Edit"), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = tr(language, "Futa", "Delete"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsGoalEditorSheet(
    initial: SavingsGoalEntity?,
    knownParties: List<PartySummary>,
    currency: String = "TZS",
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (SavingsGoalEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var targetText by remember(initial, currency) { mutableStateOf(if ((initial?.targetAmount ?: 0.0) > 0) FormatUtils.formatEditableAmount(initial!!.targetAmount, currency) else "") }
    var currentText by remember(initial, currency) { mutableStateOf(if ((initial?.currentAmount ?: 0.0) > 0) FormatUtils.formatEditableAmount(initial!!.currentAmount, currency) else "0") }
    var targetDate by remember(initial) { mutableStateOf(initial?.targetDate ?: FormatUtils.addMonthsIso(FormatUtils.todayIso(), 6)) }
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
                text = if (initial == null) tr(language, "Lengo Jipya la Akiba", "New Savings Goal")
                else tr(language, "Hariri Lengo la Akiba", "Edit Savings Goal"),
                style = MaterialTheme.typography.headlineMedium
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; errorText = null },
                label = { Text(tr(language, "Jina la Lengo *", "Goal Title *")) },
                placeholder = { Text(tr(language, "Mfano: Jina/rentsaving au Akiba ya Kodi", "e.g. Name/rentsaving or Rent Savings")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("savings_goal_title_input")
            )
            if (knownParties.isNotEmpty()) {
                RecognizedNameSuggestionsBar(
                    query = title,
                    knownParties = knownParties,
                    language = language,
                    onSelectParty = { selected ->
                        title = selected.name
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' }; errorText = null },
                    label = { Text(tr(language, "Lengo ($currency) *", "Target ($currency) *")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(tr(language, "Kiasi cha sasa", "Current")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = targetDate,
                onValueChange = { targetDate = it },
                label = { Text(tr(language, "Tarehe ya lengo", "Target Date")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
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
                    val target = targetText.toDoubleOrNull() ?: 0.0
                    val current = (currentText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    if (title.isBlank() || target <= 0.0) {
                        errorText = tr(language, "Jaza jina na kiasi cha lengo.", "Enter goal title and valid target amount.")
                        return@Button
                    }
                    val parsed = AutoAccountingEngine.parseSlashTag(title)
                    val resolvedTitle = parsed.suggestedSavingsGoalTitle
                        ?: AutoAccountingEngine.stripSlashTag(title).ifBlank { title.trim() }
                    val resolvedNotes = if (
                        parsed.hasSlashTag &&
                        parsed.cleanName.isNotBlank() &&
                        parsed.suggestedSavingsGoalTitle != null &&
                        !notes.contains(parsed.cleanName, ignoreCase = true)
                    ) {
                        listOf(parsed.cleanName, notes.trim()).filter { it.isNotBlank() }.joinToString(" · ")
                    } else {
                        notes.trim()
                    }
                    onSave(
                        (initial ?: SavingsGoalEntity(id = "", title = resolvedTitle)).copy(
                            title = resolvedTitle,
                            targetAmount = target,
                            currentAmount = current,
                            targetDate = targetDate.trim(),
                            notes = resolvedNotes
                        )
                    )
                },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(tr(language, "Hifadhi Lengo", "Save Goal"))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SavingsTransactionDialog(
    goal: SavingsGoalEntity,
    type: String,
    knownParties: List<PartySummary>,
    currency: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var partyName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = OneUiDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                if (type == "deposit") tr(language, "Weka Akiba", "Deposit to Savings")
                else tr(language, "Toa Akiba", "Withdraw from Savings")
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "${goal.title} (${FormatUtils.formatMoney(goal.currentAmount, currency)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(tr(language, "Kiasi ($currency)", "Amount ($currency)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("savings_txn_amount_input")
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(10_000L to "+10K", 25_000L to "+25K", 50_000L to "+50K", 100_000L to "+100K").forEach { (preset, chipLabel) ->
                        Surface(
                            onClick = {
                                val current = amountText.toDoubleOrNull()?.toLong() ?: 0L
                                amountText = (current + preset).toString()
                            },
                            shape = CircleShape,
                            color = LocalMengiColors.current.savingsSoft
                        ) {
                            Text(
                                text = chipLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = LocalMengiColors.current.savings,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text(tr(language, "Jina Lolote la Mchangiaji (Hiari)", "Any Contributor Name (Optional)")) },
                    placeholder = { Text(tr(language, "Jina lolote au Jina/saving", "Any name or Name/saving")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("savings_txn_party_input")
                )
                RecognizedNameSuggestionsBar(
                    query = partyName,
                    knownParties = knownParties,
                    language = language,
                    onSelectParty = { selected -> partyName = selected.name }
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(tr(language, "Maelezo (Hiari)", "Note (Optional)")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    val cleanParty = AutoAccountingEngine.stripSlashTag(partyName)
                    if (amt > 0.0) onSubmit(amt, note.trim(), cleanParty)
                },
                shape = CircleShape,
                modifier = Modifier.testTag("savings_txn_confirm_btn")
            ) {
                Text(tr(language, "Thibitisha", "Confirm"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = CircleShape) { Text(tr(language, "Ghairi", "Cancel")) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsHistorySheet(
    goal: SavingsGoalEntity,
    logs: List<SavingsLogEntity>,
    currency: String,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalMengiColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = OneUiSheetShape,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "history_header") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(goal.title, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        tr(language, "Historia ya Akiba", "Savings History"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = colors.cardBorder)
                }
            }

            if (logs.isEmpty()) {
                item(key = "history_empty") {
                    Text(
                        tr(language, "Hakuna miamala ya akiba bado.", "No savings activity recorded yet."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                itemsIndexed(
                    items = logs,
                    key = { _, log -> log.id },
                    contentType = { _, _ -> "savings_log_row" }
                ) { idx, log ->
                    val formattedLogAmount = remember(log.amount, log.isDeposit, currency) {
                        (if (log.isDeposit) "+" else "-") + FormatUtils.formatMoney(log.amount, currency)
                    }
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.note.ifBlank {
                                        if (log.isDeposit) tr(language, "Kuweka akiba", "Deposit")
                                        else tr(language, "Kutoa akiba", "Withdrawal")
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    log.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formattedLogAmount,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (log.isDeposit) colors.income else colors.expense
                            )
                        }
                        if (idx < logs.lastIndex) {
                            HorizontalDivider(color = colors.cardBorder)
                        }
                    }
                }
            }

            item(key = "history_footer") {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
