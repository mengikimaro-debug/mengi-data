package tz.mengidata.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tz.mengidata.app.data.accounting.AutoAccountingEngine
import tz.mengidata.app.data.accounting.AutoBucket
import tz.mengidata.app.data.accounting.CategoryAnalyticsEngine
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.BusinessSettingsEntity
import tz.mengidata.app.data.db.DeletedRecordEntity
import tz.mengidata.app.data.db.InvoiceEntity
import tz.mengidata.app.data.db.InvoiceItemEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.MengiDatabase
import tz.mengidata.app.data.db.PendingTransactionEntity
import tz.mengidata.app.data.db.PhoneStorageStatus
import tz.mengidata.app.data.db.PhoneStorageVault
import tz.mengidata.app.data.db.SavedPublicPdf
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.SavingsLogEntity
import tz.mengidata.app.data.db.TransactionCategoryEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.data.export.ColorReportExporter
import tz.mengidata.app.data.prefs.AppPreferences
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.max

class FinanceRepository(
    private val appContext: Context,
    private val db: MengiDatabase,
    private val prefs: AppPreferences
) {
    private val settingsDao = db.settingsDao()
    private val txnDao = db.transactionDao()
    private val invoiceDao = db.invoiceDao()
    private val billDao = db.billDao()
    private val savingsDao = db.savingsDao()
    private val categoryDao = db.categoryDao()
    private val deletedRecordDao = db.deletedRecordDao()
    private val pendingTxnDao = db.pendingTransactionDao()
    private val storageVault = PhoneStorageVault(appContext).apply {
        setCustomFolderUri(prefs.customBackupFolderUri.value)
    }
    private val backupMutex = Mutex()
    private val autoBackupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var scheduledBackupJob: Job? = null

    val phoneStorageStatusFlow: StateFlow<PhoneStorageStatus> = storageVault.statusFlow

    val settingsFlow: Flow<BusinessSettingsEntity> =
        settingsDao.observeSettings().map { it ?: BusinessSettingsEntity() }

    val transactionsFlow: Flow<List<TransactionEntity>> = txnDao.observeAll()
    val invoicesFlow: Flow<List<InvoiceWithItems>> = invoiceDao.observeAllWithItems()
    val billsFlow: Flow<List<BillEntity>> = billDao.observeAll()
    val savingsGoalsFlow: Flow<List<SavingsGoalEntity>> = savingsDao.observeGoals()
    val savingsLogsFlow: Flow<List<SavingsLogEntity>> = savingsDao.observeAllLogs()
    val categoriesFlow: Flow<List<TransactionCategoryEntity>> = categoryDao.observeAll()
    val deletedRecordsFlow: Flow<List<DeletedRecordEntity>> = deletedRecordDao.observeAll()
    val pendingTransactionsFlow: Flow<List<PendingTransactionEntity>> = pendingTxnDao.observeAll()

    fun getPhoneStorageStatus(): PhoneStorageStatus = storageVault.getStatus()

    fun setCustomBackupFolderUri(uriString: String) {
        prefs.setCustomBackupFolderUri(uriString)
        storageVault.setCustomFolderUri(uriString)
    }

    fun savePdfToPublicDocuments(filename: String, pdfBytes: ByteArray): Uri? {
        return storageVault.savePdfToPublicDocuments(filename, pdfBytes)
    }

    suspend fun listSavedPublicInvoicePdfs(): List<SavedPublicPdf> = withContext(Dispatchers.IO) {
        storageVault.listSavedPublicInvoicePdfs()
    }

    suspend fun deleteSavedPublicPdf(pdf: SavedPublicPdf): Boolean = withContext(Dispatchers.IO) {
        storageVault.deleteSavedPublicPdf(pdf)
    }

    suspend fun ensureInitialized(): Boolean = withContext(Dispatchers.IO) {
        ensureDefaultCategoriesSeeded()
        var autoRestored = false
        if (!prefs.isSeeded()) {
            val existing = settingsDao.getSettings()
            val existingTxns = txnDao.getAll()
            if (existing == null && existingTxns.isEmpty()) {
                // Auto-recover REAL user data from phone storage if readable immediately!
                val recovered = runCatching {
                    val savedJson = storageVault.readBestAvailableSnapshot(requireRealUserData = true)
                    if (!savedJson.isNullOrBlank()) {
                        importBackupJson(savedJson)
                        true
                    } else {
                        false
                    }
                }.getOrDefault(false)
                if (recovered) {
                    autoRestored = true
                    prefs.setPendingReinstallRestore(false)
                } else {
                    // On Android 11–16 reinstall, MANAGE_EXTERNAL_STORAGE is reset by OS so Documents/Mengidata
                    // cannot be read until All Files Access is granted. Flag pending restore and NEVER overwrite public backups!
                    if (!storageVault.hasAllFilesAccess()) {
                        prefs.setPendingReinstallRestore(true)
                    }
                    loadDemoData()
                }
            }
            prefs.markSeeded()
        } else if (prefs.pendingReinstallRestore.value && storageVault.hasAllFilesAccess()) {
            autoRestored = tryAutoRestoreAfterStorageAccessGranted()
        }
        reconcileLedgerIntegrity()
        storageVault.refreshStatus()
        // Only schedule public auto-backup if database actually contains real user data (not untouched demo!)
        if (!isDatabaseOnlyDemoOrEmpty()) {
            scheduleAutoBackup()
        }
        autoRestored
    }

    /**
     * Returns true if the local Room DB is empty or still contains strictly unmodified demo records.
     */
    suspend fun isDatabaseOnlyDemoOrEmpty(): Boolean = withContext(Dispatchers.IO) {
        val s = settingsDao.getSettings()
        val txns = txnDao.getAll()
        val invs = invoiceDao.getAllWithItems()
        val bills = billDao.getAll()
        val goals = savingsDao.getGoals()
        val pending = pendingTxnDao.getAll()

        if (s == null && txns.isEmpty() && invs.isEmpty() && bills.isEmpty() && goals.isEmpty() && pending.isEmpty()) {
            return@withContext true
        }

        val hasRealTxn = txns.any { !it.id.startsWith("demo_") }
        val hasRealInv = invs.any { !it.invoice.id.startsWith("demo_") }
        val hasRealBill = bills.any { !it.id.startsWith("demo_") }
        val hasRealGoal = goals.any { !it.id.startsWith("demo_") }
        val hasRealPending = pending.any { !it.id.startsWith("demo_") }
        if (hasRealTxn || hasRealInv || hasRealBill || hasRealGoal || hasRealPending) {
            return@withContext false
        }

        val owner = s?.ownerName?.trim().orEmpty()
        val biz = s?.businessName?.trim().orEmpty()
        if (owner.isNotEmpty()) return@withContext false
        if (biz.isNotEmpty() && !biz.equals("VaultLedger", ignoreCase = true) && !biz.equals("Mengidata", ignoreCase = true)) {
            return@withContext false
        }
        s?.demo != false
    }

    /**
     * Called automatically the instant the user grants "All Files Access" (Storage Manager)
     * after reinstalling the app. Scans Documents/Mengidata & Download/VaultLedger and
     * restores all real user data automatically without touching demo data over real backups.
     */
    suspend fun tryAutoRestoreAfterStorageAccessGranted(): Boolean = withContext(Dispatchers.IO) {
        if (!storageVault.hasAllFilesAccess()) return@withContext false
        if (!prefs.pendingReinstallRestore.value && !isDatabaseOnlyDemoOrEmpty()) {
            return@withContext false
        }
        if (!isDatabaseOnlyDemoOrEmpty()) {
            // User already created new real records; don't clobber them automatically
            prefs.setPendingReinstallRestore(false)
            return@withContext false
        }
        val savedJson = storageVault.readBestAvailableSnapshot(requireRealUserData = true)
        if (!savedJson.isNullOrBlank()) {
            val imported = runCatching {
                importBackupJson(savedJson)
                reconcileLedgerIntegrity()
                true
            }.getOrDefault(false)
            if (imported) {
                prefs.setPendingReinstallRestore(false)
                prefs.setRestorePromptDismissed(true)
                storageVault.saveAutoSnapshot(savedJson, allowPublicOverwrite = false)
                storageVault.refreshStatus()
                return@withContext true
            }
        }
        prefs.setPendingReinstallRestore(false)
        storageVault.refreshStatus()
        false
    }

    suspend fun ensureDefaultCategoriesSeeded() {
        val existingIds = categoryDao.getAll().map { it.id }.toSet()
        val missingDefaults = CategoryAnalyticsEngine.defaultCategories().filter { it.id !in existingIds }
        if (missingDefaults.isNotEmpty()) {
            categoryDao.upsertAll(missingDefaults)
        }
    }

    // --- Transaction Categories (Custom & Default Business Grouping) ---

    suspend fun saveCategory(
        category: TransactionCategoryEntity,
        previousName: String? = null
    ): TransactionCategoryEntity {
        val saved = db.withTransaction {
            val cleanName = category.name.trim()
            val cleanType = if (category.type == "expense") "expense" else "income"
            val id = category.id.ifBlank { "cat_${UUID.randomUUID()}" }
            val entity = category.copy(
                id = id,
                name = cleanName,
                type = cleanType,
                iconKey = category.iconKey.ifBlank { "tag" },
                colorHex = category.colorHex.ifBlank { "#2563EB" },
                monthlyBudget = max(category.monthlyBudget, 0.0),
                createdAt = category.createdAt.ifBlank { FormatUtils.todayIso() }
            )
            categoryDao.upsert(entity)
            if (!previousName.isNullOrBlank() && previousName != cleanName) {
                txnDao.renameCategory(previousName, cleanName, cleanType)
            }
            entity
        }
        scheduleAutoBackup()
        return saved
    }

    suspend fun deleteCategory(categoryId: String, reassignToCategory: String? = null) {
        db.withTransaction {
            val existing = categoryDao.getById(categoryId) ?: return@withTransaction
            val fallback = reassignToCategory?.takeIf { it.isNotBlank() }
                ?: if (existing.isIncome) "Mapato mengine" else "Matumizi mengine"
            if (existing.name != fallback) {
                txnDao.renameCategory(existing.name, fallback, existing.type)
            }
            categoryDao.deleteById(categoryId)
        }
        scheduleAutoBackup()
    }

    /**
     * Zero-loss ledger guard: verifies every paid bill and every paid invoice has its
     * matching transaction entry so no shilling entering or leaving is ever lost.
     */
    suspend fun reconcileLedgerIntegrity(): Int = db.withTransaction {
        var repaired = 0
        val existingTxnIds = txnDao.getAll().map { it.id }.toSet()

        billDao.getAll().forEach { bill ->
            if (bill.paid && (bill.linkedTxnId.isNullOrBlank() || bill.linkedTxnId !in existingTxnIds)) {
                val newTxnId = syncBillTransactionLocked(bill.copy(linkedTxnId = null))
                billDao.upsert(bill.copy(linkedTxnId = newTxnId))
                repaired++
            }
        }

        invoiceDao.getAllWithItems().forEach { invWithItems ->
            val inv = invWithItems.invoice
            if (inv.state == "issued" && inv.amountPaid > 0.005 &&
                (inv.linkedTxnId.isNullOrBlank() || inv.linkedTxnId !in existingTxnIds)
            ) {
                val newTxnId = syncInvoiceTransactionLocked(inv.copy(linkedTxnId = null))
                invoiceDao.upsertInvoice(inv.copy(linkedTxnId = newTxnId))
                repaired++
            }
        }
        repaired
    }

    suspend fun saveSettings(settings: BusinessSettingsEntity) {
        settingsDao.upsert(settings.copy(id = 1))
        scheduleAutoBackup()
    }

    suspend fun convertAllMoneyToCurrency(targetCurrencyCode: String, factor: Double) {
        val target = FormatUtils.normalizeCurrencyCode(targetCurrencyCode)
        val safeFactor = if (factor.isFinite() && factor > 0.0) factor else 1.0
        db.withTransaction {
            val current = settingsDao.getSettings() ?: BusinessSettingsEntity()
            if (safeFactor != 1.0) {
                txnDao.scaleAllAmounts(safeFactor)
                invoiceDao.scaleAllInvoiceAmounts(safeFactor)
                invoiceDao.scaleAllItemPrices(safeFactor)
                billDao.scaleAllAmounts(safeFactor)
                savingsDao.scaleAllGoalAmounts(safeFactor)
                savingsDao.scaleAllLogAmounts(safeFactor)
                categoryDao.scaleAllBudgets(safeFactor)
                deletedRecordDao.scaleAllAmounts(safeFactor)
                pendingTxnDao.scaleAllAmounts(safeFactor)
            }
            settingsDao.upsert(current.copy(id = 1, currency = target))
        }
        if (safeFactor != 1.0) {
            prefs.setLargeBillThreshold(prefs.largeBillThreshold.value * safeFactor)
        }
        scheduleAutoBackup()
    }

    // --- Smart Auto-Entry (auto-classifies Akiba, Bili, Matumizi, Pesa Kuingia) ---

    suspend fun saveSmartAutoEntry(
        description: String,
        amount: Double,
        party: String,
        method: String,
        date: String,
        overrideBucket: AutoBucket? = null,
        markBillPaid: Boolean = true
    ): AutoBucket {
        val rawDesc = description.trim()
        val rawParty = party.trim()
        val cleanDate = date.ifBlank { FormatUtils.todayIso() }
        val learnedHouseholdNames = buildSet {
            txnDao.getAll()
                .filter { AutoAccountingEngine.isHouseholdTransaction(it) && it.party.isNotBlank() }
                .forEach { add(it.party.trim()) }
            billDao.getAll()
                .filter { AutoAccountingEngine.isHouseholdBill(it) && it.payee.isNotBlank() }
                .forEach { add(it.payee.trim()) }
        }
        val classification = AutoAccountingEngine.classifyText(
            description = rawDesc,
            party = rawParty,
            learnedHouseholdNames = learnedHouseholdNames
        )
        val bucket = overrideBucket ?: classification.bucket

        // Clean any slash-tag suffix (e.g. "Juma/household", "Asha/rentsaving", "Neema my/mybilll")
        val parsedParty = AutoAccountingEngine.parseSlashTag(rawParty)
        val parsedDesc = AutoAccountingEngine.parseSlashTag(rawDesc)
        val cleanParty = when {
            parsedParty.hasSlashTag -> parsedParty.cleanName
            rawParty.isNotBlank() -> rawParty
            parsedDesc.hasSlashTag && parsedDesc.cleanName.isNotBlank() -> parsedDesc.cleanName
            else -> ""
        }
        val cleanDesc = when {
            parsedDesc.hasSlashTag -> parsedDesc.cleanName
            else -> rawDesc
        }

        when (bucket) {
            AutoBucket.SAVINGS -> {
                db.withTransaction {
                    val goals = savingsDao.getGoals()
                    val requestedGoalTitle = classification.suggestedSavingsGoalTitle
                    val targetGoal = when {
                        !requestedGoalTitle.isNullOrBlank() -> {
                            goals.firstOrNull { g ->
                                AutoAccountingEngine.matchesSavingsGoal(g.title, requestedGoalTitle)
                            } ?: SavingsGoalEntity(
                                id = "svg_${UUID.randomUUID()}",
                                title = requestedGoalTitle,
                                targetAmount = max(amount * 5, 1_000_000.0),
                                currentAmount = 0.0,
                                targetDate = FormatUtils.addMonthsIso(cleanDate, 6),
                                notes = "Imeundwa kiotomatiki kupitia /${classification.slashTag.rawTag}.",
                                createdAt = FormatUtils.nowIsoTimestamp(),
                                updatedAt = FormatUtils.nowIsoTimestamp()
                            ).also { savingsDao.upsertGoal(it) }
                        }
                        else -> {
                            goals.firstOrNull { g ->
                                cleanDesc.isNotBlank() && cleanDesc.lowercase().contains(g.title.lowercase().take(6))
                            } ?: goals.firstOrNull() ?: SavingsGoalEntity(
                                id = "svg_${UUID.randomUUID()}",
                                title = "Akiba Kuu",
                                targetAmount = max(amount * 5, 1_000_000.0),
                                currentAmount = 0.0,
                                targetDate = FormatUtils.addMonthsIso(cleanDate, 6),
                                notes = "Imeundwa kiotomatiki kwa Akiba.",
                                createdAt = FormatUtils.nowIsoTimestamp(),
                                updatedAt = FormatUtils.nowIsoTimestamp()
                            ).also { savingsDao.upsertGoal(it) }
                        }
                    }

                    recordSavingsTransactionLocked(
                        goal = targetGoal,
                        type = "deposit",
                        amount = amount,
                        note = cleanDesc.ifBlank { requestedGoalTitle ?: "Akiba" },
                        date = cleanDate,
                        method = method,
                        partyName = cleanParty
                    )
                }
            }

            AutoBucket.HOUSEHOLD_BILL -> {
                saveTransaction(
                    TransactionEntity(
                        id = "",
                        type = "expense",
                        date = cleanDate,
                        description = cleanDesc.ifBlank { "Bili ya Nyumbani" },
                        party = cleanParty,
                        category = AutoAccountingEngine.HOUSEHOLD_CATEGORY,
                        amount = amount,
                        method = method.ifBlank { "Cash" },
                        reference = "",
                        notes = "Bili ya Nyumbani — imetambuliwa na kuhifadhiwa kwenye mfumo.",
                        source = AutoAccountingEngine.HOUSEHOLD_SOURCE,
                        createdAt = FormatUtils.nowIsoTimestamp()
                    )
                )
            }

            AutoBucket.BILL, AutoBucket.HIGH_BILL_TODAY -> {
                saveBill(
                    BillEntity(
                        id = "",
                        name = cleanDesc.ifBlank { if (cleanParty.isNotBlank()) "Bili: $cleanParty" else "Bili" },
                        category = classification.suggestedCategory,
                        payee = cleanParty,
                        amount = amount,
                        dueDate = cleanDate,
                        paid = markBillPaid,
                        paidDate = if (markBillPaid) cleanDate else "",
                        method = method.ifBlank { "M-Pesa" },
                        recurring = false,
                        notes = "Imetambuliwa kiotomatiki kama Bili Zangu.",
                        createdAt = FormatUtils.nowIsoTimestamp()
                    )
                )
            }

            AutoBucket.MONEY_IN -> {
                if (classification.isInvoiceTagged && overrideBucket == null) {
                    val invCustomer = cleanParty.ifBlank { cleanDesc.ifBlank { "Mteja" } }
                    val itemDesc = cleanDesc.ifBlank { "Huduma / Malipo ya Ankara" }
                    saveInvoice(
                        invoice = InvoiceEntity(
                            id = "",
                            number = "",
                            customerName = invCustomer,
                            issueDate = cleanDate,
                            dueDate = FormatUtils.addDaysIso(cleanDate, 14),
                            amountPaid = amount,
                            state = "issued",
                            paidDate = cleanDate,
                            notes = "Imeundwa kiotomatiki kupitia /${classification.slashTag.rawTag}."
                        ),
                        items = listOf(
                            InvoiceItemEntity(
                                invoiceId = "",
                                description = itemDesc,
                                qty = 1.0,
                                price = amount,
                                position = 0
                            )
                        )
                    )
                } else {
                    saveTransaction(
                        TransactionEntity(
                            id = "",
                            type = "income",
                            date = cleanDate,
                            description = cleanDesc.ifBlank { "Mapato" },
                            party = cleanParty,
                            category = classification.suggestedCategory,
                            amount = amount,
                            method = method.ifBlank { "M-Pesa" },
                            reference = "",
                            notes = "Imetambuliwa kiotomatiki: Pesa Kuingia.",
                            source = "manual",
                            createdAt = FormatUtils.nowIsoTimestamp()
                        )
                    )
                }
            }

            AutoBucket.EXPENSE -> {
                saveTransaction(
                    TransactionEntity(
                        id = "",
                        type = "expense",
                        date = cleanDate,
                        description = cleanDesc.ifBlank { "Matumizi binafsi" },
                        party = cleanParty,
                        category = classification.suggestedCategory,
                        amount = amount,
                        method = method.ifBlank { "Cash" },
                        reference = "",
                        notes = "Imetambuliwa kiotomatiki: Matumizi Yangu.",
                        source = "manual",
                        createdAt = FormatUtils.nowIsoTimestamp()
                    )
                )
            }
        }
        scheduleAutoBackup()
        return bucket
    }

    // --- Transactions ---

    suspend fun saveTransaction(txn: TransactionEntity) {
        val withId = if (txn.id.isBlank()) {
            txn.copy(
                id = "txn_${UUID.randomUUID()}",
                createdAt = FormatUtils.nowIsoTimestamp()
            )
        } else {
            txn
        }
        val clean = when {
            !withId.isIncome && AutoAccountingEngine.isHouseholdTransaction(withId) -> {
                withId.copy(
                    source = AutoAccountingEngine.HOUSEHOLD_SOURCE,
                    category = AutoAccountingEngine.HOUSEHOLD_CATEGORY,
                    party = withId.party.trim()
                )
            }
            !withId.isIncome && AutoAccountingEngine.isSavingsTransaction(withId) -> {
                withId.copy(
                    source = "savings",
                    category = "Akiba"
                )
            }
            withId.isIncome && withId.source == AutoAccountingEngine.HOUSEHOLD_SOURCE -> {
                withId.copy(source = "manual")
            }
            else -> withId
        }
        txnDao.upsert(clean)
        scheduleAutoBackup()
    }

    suspend fun deleteTransaction(id: String): TransactionEntity? {
        val existing = db.withTransaction {
            val found = txnDao.getById(id) ?: return@withTransaction null
            archiveTransactionLocked(found)
            txnDao.deleteById(id)
            found
        } ?: return null
        scheduleAutoBackup()
        return existing
    }

    // --- Invoices (with atomic linked-transaction sync) ---

    suspend fun nextInvoiceNumber(): String {
        val settings = settingsDao.getSettings() ?: BusinessSettingsEntity()
        val rawPrefix = settings.invoicePrefix.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "")
        val prefix = rawPrefix.ifEmpty { "MD" }
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val start = "$prefix-$year-"
        val existing = invoiceDao.getNumbersWithPrefix("$start%")
        val maxNum = existing.mapNotNull { number ->
            number.removePrefix(start).toIntOrNull()
        }.maxOrNull() ?: 0
        return "$start${String.format(Locale.US, "%03d", maxNum + 1)}"
    }

    suspend fun saveInvoice(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): InvoiceEntity {
        val result = db.withTransaction {
            val id = invoice.id.ifBlank { "inv_${UUID.randomUUID()}" }
            val number = invoice.number.ifBlank { nextInvoiceNumber() }
            val nowTs = FormatUtils.nowIsoTimestamp()
            val base = invoice.copy(
                id = id,
                number = number,
                createdAt = if (invoice.id.isBlank()) nowTs else invoice.createdAt.ifBlank { nowTs },
                updatedAt = nowTs
            )

            val linkedTxnId = syncInvoiceTransactionLocked(base)
            val finalInvoice = base.copy(linkedTxnId = linkedTxnId)

            invoiceDao.upsertInvoice(finalInvoice)
            invoiceDao.deleteItemsForInvoice(id)
            val normalizedItems = items.mapIndexed { index, item ->
                item.copy(id = 0, invoiceId = id, position = index)
            }
            if (normalizedItems.isNotEmpty()) {
                invoiceDao.insertItems(normalizedItems)
            }
            finalInvoice
        }
        scheduleAutoBackup()
        return result
    }

    suspend fun markInvoicePaid(invoiceId: String): InvoiceWithItems? {
        val result = db.withTransaction {
            val current = invoiceDao.getByIdWithItems(invoiceId) ?: return@withTransaction null
            val updated = current.invoice.copy(
                state = "issued",
                amountPaid = current.total,
                paidDate = FormatUtils.todayIso(),
                updatedAt = FormatUtils.todayIso()
            )
            val linkedId = syncInvoiceTransactionLocked(updated)
            invoiceDao.upsertInvoice(updated.copy(linkedTxnId = linkedId))
            current
        }
        scheduleAutoBackup()
        return result
    }

    suspend fun markInvoiceUnpaid(invoiceId: String) {
        db.withTransaction {
            val current = invoiceDao.getByIdWithItems(invoiceId) ?: return@withTransaction
            val updated = current.invoice.copy(
                state = "issued",
                amountPaid = 0.0,
                paidDate = "",
                updatedAt = FormatUtils.todayIso()
            )
            val linkedId = syncInvoiceTransactionLocked(updated)
            invoiceDao.upsertInvoice(updated.copy(linkedTxnId = linkedId))
        }
        scheduleAutoBackup()
    }

    suspend fun recordInvoicePayment(invoiceId: String, addedAmount: Double): InvoiceWithItems? {
        if (addedAmount <= 0.0) return null
        val result = db.withTransaction {
            val current = invoiceDao.getByIdWithItems(invoiceId) ?: return@withTransaction null
            val newPaid = (current.invoice.amountPaid + addedAmount).coerceAtMost(current.total)
            val updated = current.invoice.copy(
                state = "issued",
                amountPaid = newPaid,
                paidDate = FormatUtils.todayIso(),
                updatedAt = FormatUtils.todayIso()
            )
            val linkedId = syncInvoiceTransactionLocked(updated)
            invoiceDao.upsertInvoice(updated.copy(linkedTxnId = linkedId))
            current
        }
        scheduleAutoBackup()
        return result
    }

    suspend fun duplicateInvoice(invoiceId: String): InvoiceEntity? {
        val result = db.withTransaction {
            val current = invoiceDao.getByIdWithItems(invoiceId) ?: return@withTransaction null
            val today = FormatUtils.todayIso()
            val newId = "inv_${UUID.randomUUID()}"
            val nextNum = nextInvoiceNumber()
            val copyInvoice = current.invoice.copy(
                id = newId,
                number = nextNum,
                issueDate = today,
                dueDate = FormatUtils.addDaysIso(today, 14),
                amountPaid = 0.0,
                state = "draft",
                paidDate = "",
                linkedTxnId = null,
                createdAt = FormatUtils.nowIsoTimestamp(),
                updatedAt = FormatUtils.nowIsoTimestamp()
            )
            invoiceDao.upsertInvoice(copyInvoice)
            val copiedItems = current.orderedItems.mapIndexed { idx, item ->
                item.copy(id = 0, invoiceId = newId, position = idx)
            }
            if (copiedItems.isNotEmpty()) {
                invoiceDao.insertItems(copiedItems)
            }
            copyInvoice
        }
        scheduleAutoBackup()
        return result
    }

    suspend fun deleteInvoice(invoiceId: String) {
        db.withTransaction {
            val current = invoiceDao.getByIdWithItems(invoiceId) ?: return@withTransaction
            archiveInvoiceLocked(current)
            current.invoice.linkedTxnId?.let { txnDao.deleteById(it) }
            invoiceDao.deleteItemsForInvoice(invoiceId)
            invoiceDao.deleteInvoiceById(invoiceId)
        }
        scheduleAutoBackup()
    }

    private suspend fun syncInvoiceTransactionLocked(invoice: InvoiceEntity): String? {
        val shouldRecord = invoice.state == "issued" && invoice.amountPaid > 0.005
        if (!shouldRecord) {
            invoice.linkedTxnId?.let { txnDao.deleteById(it) }
            return null
        }
        val txnId = invoice.linkedTxnId?.takeIf { it.isNotBlank() } ?: "txn_${UUID.randomUUID()}"
        val txnDate = invoice.paidDate.ifBlank { invoice.issueDate.ifBlank { FormatUtils.todayIso() } }
        txnDao.upsert(
            TransactionEntity(
                id = txnId,
                type = "income",
                date = txnDate,
                description = "Malipo ya ankara ${invoice.number}",
                party = invoice.customerName,
                category = "Malipo ya ankara",
                amount = invoice.amountPaid,
                method = "Benki",
                reference = invoice.number,
                notes = "Imeundwa kiotomatiki kutoka ankara.",
                source = "invoice",
                linkedId = invoice.id,
                createdAt = FormatUtils.nowIsoTimestamp()
            )
        )
        return txnId
    }

    // --- Bills (with atomic linked-transaction sync) ---

    suspend fun saveBill(bill: BillEntity): BillEntity {
        val result = db.withTransaction {
            val isNew = bill.id.isBlank()
            val id = if (isNew) "bill_${UUID.randomUUID()}" else bill.id
            val base = bill.copy(
                id = id,
                createdAt = if (isNew) FormatUtils.nowIsoTimestamp() else bill.createdAt.ifBlank { FormatUtils.nowIsoTimestamp() }
            )
            val linkedId = syncBillTransactionLocked(base)
            val finalBill = base.copy(linkedTxnId = linkedId)
            billDao.upsert(finalBill)
            finalBill
        }
        scheduleAutoBackup()
        return result
    }

    suspend fun toggleBillPaid(billId: String): BillEntity? {
        val result = db.withTransaction {
            val existing = billDao.getById(billId) ?: return@withTransaction null
            val newPaid = !existing.paid
            val updated = existing.copy(
                paid = newPaid,
                paidDate = if (newPaid) FormatUtils.todayIso() else ""
            )
            val linkedId = syncBillTransactionLocked(updated)
            billDao.upsert(updated.copy(linkedTxnId = linkedId))
            existing
        }
        scheduleAutoBackup()
        return result
    }

    suspend fun scheduleNextRecurringBill(billId: String): BillEntity? {
        val result = db.withTransaction {
            val existing = billDao.getById(billId) ?: return@withTransaction null
            val nextDue = FormatUtils.addMonthsIso(existing.dueDate.ifBlank { FormatUtils.todayIso() }, 1)
            val nextBill = existing.copy(
                id = "bill_${UUID.randomUUID()}",
                dueDate = nextDue,
                paid = false,
                paidDate = "",
                recurring = true,
                linkedTxnId = null,
                createdAt = FormatUtils.nowIsoTimestamp()
            )
            billDao.upsert(nextBill)
            nextBill
        }
        scheduleAutoBackup()
        return result
    }

    suspend fun deleteBill(billId: String) {
        db.withTransaction {
            val existing = billDao.getById(billId) ?: return@withTransaction
            archiveBillLocked(existing)
            existing.linkedTxnId?.let { txnDao.deleteById(it) }
            billDao.deleteById(billId)
        }
        scheduleAutoBackup()
    }

    private suspend fun syncBillTransactionLocked(bill: BillEntity): String? {
        if (!bill.paid) {
            bill.linkedTxnId?.let { txnDao.deleteById(it) }
            return null
        }
        val txnId = bill.linkedTxnId?.takeIf { it.isNotBlank() } ?: "txn_${UUID.randomUUID()}"
        val txnDate = bill.paidDate.ifBlank { bill.dueDate.ifBlank { FormatUtils.todayIso() } }
        val isHousehold = AutoAccountingEngine.isHouseholdBill(bill)
        txnDao.upsert(
            TransactionEntity(
                id = txnId,
                type = "expense",
                date = txnDate,
                description = if (isHousehold) bill.name else "Malipo: ${bill.name}",
                party = bill.payee.trim(),
                category = if (isHousehold) AutoAccountingEngine.HOUSEHOLD_CATEGORY else bill.category,
                amount = bill.amount,
                method = bill.method.ifBlank { "Benki" },
                reference = "",
                notes = if (isHousehold) {
                    "Bili ya Nyumbani — imetenganishwa na matumizi binafsi."
                } else {
                    "Imeundwa kiotomatiki kutoka bili."
                },
                source = if (isHousehold) AutoAccountingEngine.HOUSEHOLD_SOURCE else "bill",
                linkedId = bill.id,
                createdAt = FormatUtils.nowIsoTimestamp()
            )
        )
        return txnId
    }

    // --- Savings Goals (Akiba) ---

    suspend fun saveSavingsGoal(goal: SavingsGoalEntity) {
        val today = FormatUtils.todayIso()
        val nowTs = FormatUtils.nowIsoTimestamp()
        val isNewGoal = goal.id.isBlank()
        val initialDeposit = if (isNewGoal && goal.currentAmount > 0.0) goal.currentAmount else 0.0
        val clean = goal.copy(
            id = goal.id.ifBlank { "svg_${UUID.randomUUID()}" },
            currentAmount = if (initialDeposit > 0.0) 0.0 else goal.currentAmount,
            createdAt = if (isNewGoal) nowTs else goal.createdAt.ifBlank { nowTs },
            updatedAt = nowTs
        )
        db.withTransaction {
            savingsDao.upsertGoal(clean)
            if (initialDeposit > 0.0) {
                recordSavingsTransactionLocked(
                    goal = clean,
                    type = "deposit",
                    amount = initialDeposit,
                    note = "Akiba ya awali — imekatwa kwenye mfumo",
                    date = today,
                    method = "M-Pesa",
                    partyName = clean.title
                )
            }
        }
        scheduleAutoBackup()
    }

    suspend fun deleteSavingsGoal(id: String) {
        db.withTransaction {
            val goal = savingsDao.getGoalById(id) ?: return@withTransaction
            val logs = savingsDao.getLogsForGoal(id)
            archiveSavingsGoalLocked(goal, logs)
            savingsDao.deleteGoalById(id)
        }
        scheduleAutoBackup()
    }

    suspend fun recordSavingsTransaction(
        goalId: String,
        type: String,
        amount: Double,
        note: String,
        partyName: String = ""
    ) {
        db.withTransaction {
            if (amount <= 0.0) return@withTransaction
            val goal = savingsDao.getGoalById(goalId) ?: return@withTransaction
            recordSavingsTransactionLocked(
                goal = goal,
                type = type,
                amount = amount,
                note = note.trim(),
                date = FormatUtils.todayIso(),
                method = "M-Pesa",
                partyName = partyName.trim()
            )
        }
        scheduleAutoBackup()
    }

    private suspend fun recordSavingsTransactionLocked(
        goal: SavingsGoalEntity,
        type: String,
        amount: Double,
        note: String,
        date: String,
        method: String,
        partyName: String = ""
    ) {
        val updatedAmount = if (type == "deposit") {
            goal.currentAmount + amount
        } else {
            max(0.0, goal.currentAmount - amount)
        }
        val cleanParty = partyName.trim()
        val combinedNote = when {
            cleanParty.isNotBlank() && note.isNotBlank() -> "$cleanParty · $note"
            cleanParty.isNotBlank() -> cleanParty
            else -> note
        }
        val logId = "slog_${UUID.randomUUID()}"
        val nowTs = FormatUtils.nowIsoTimestamp()
        savingsDao.upsertGoal(goal.copy(currentAmount = updatedAmount, updatedAt = nowTs))
        savingsDao.upsertLog(
            SavingsLogEntity(
                id = logId,
                savingsId = goal.id,
                type = type,
                amount = amount,
                date = date,
                note = combinedNote,
                createdAt = nowTs
            )
        )
        // Zero-loss ledger mirror tagged with source = "savings" so cashflow never loses a shilling
        txnDao.upsert(
            TransactionEntity(
                id = "txn_$logId",
                type = if (type == "deposit") "expense" else "income",
                date = date,
                description = if (type == "deposit") "Akiba: ${goal.title}" else "Kutoa Akiba: ${goal.title}",
                party = cleanParty.ifBlank { goal.title },
                category = "Akiba",
                amount = amount,
                method = method,
                reference = logId.takeLast(6).uppercase(Locale.US),
                notes = combinedNote.ifBlank { "Muamala wa Akiba." },
                source = "savings",
                linkedId = logId,
                createdAt = nowTs
            )
        )
    }

    // --- Recovery Vault (Soft-Delete Protection & 1-Tap Restore) ---

    private suspend fun archiveTransactionLocked(t: TransactionEntity) {
        val payload = JSONObject().apply {
            put("id", t.id)
            put("type", t.type)
            put("date", t.date)
            put("description", t.description)
            put("party", t.party)
            put("category", t.category)
            put("amount", t.amount)
            put("method", t.method)
            put("reference", t.reference)
            put("notes", t.notes)
            put("source", t.source)
            put("linkedId", t.linkedId)
            put("createdAt", t.createdAt)
        }.toString()
        deletedRecordDao.upsert(
            DeletedRecordEntity(
                id = "trash_${UUID.randomUUID()}",
                entityType = "transaction",
                originalId = t.id,
                title = t.description.ifBlank { t.category },
                subtitle = listOf(t.party, t.date).filter { it.isNotBlank() }.joinToString(" · "),
                amount = t.amount,
                payloadJson = payload
            )
        )
    }

    private suspend fun archiveBillLocked(b: BillEntity) {
        val payload = JSONObject().apply {
            put("id", b.id)
            put("name", b.name)
            put("category", b.category)
            put("payee", b.payee)
            put("amount", b.amount)
            put("dueDate", b.dueDate)
            put("paid", b.paid)
            put("paidDate", b.paidDate)
            put("method", b.method)
            put("recurring", b.recurring)
            put("notes", b.notes)
            put("linkedTxnId", b.linkedTxnId)
            put("createdAt", b.createdAt)
        }.toString()
        deletedRecordDao.upsert(
            DeletedRecordEntity(
                id = "trash_${UUID.randomUUID()}",
                entityType = "bill",
                originalId = b.id,
                title = b.name.ifBlank { b.category },
                subtitle = listOf(b.payee, b.dueDate).filter { it.isNotBlank() }.joinToString(" · "),
                amount = b.amount,
                payloadJson = payload
            )
        )
    }

    private suspend fun archiveInvoiceLocked(invWithItems: InvoiceWithItems) {
        val inv = invWithItems.invoice
        val itemsJson = JSONArray()
        invWithItems.orderedItems.forEach { item ->
            itemsJson.put(JSONObject().apply {
                put("description", item.description)
                put("qty", item.qty)
                put("price", item.price)
            })
        }
        val payload = JSONObject().apply {
            put("id", inv.id)
            put("number", inv.number)
            put("customerName", inv.customerName)
            put("customerPhone", inv.customerPhone)
            put("customerEmail", inv.customerEmail)
            put("customerAddress", inv.customerAddress)
            put("issueDate", inv.issueDate)
            put("dueDate", inv.dueDate)
            put("discount", inv.discount)
            put("vatRate", inv.vatRate)
            put("amountPaid", inv.amountPaid)
            put("state", inv.state)
            put("paidDate", inv.paidDate)
            put("notes", inv.notes)
            put("linkedTxnId", inv.linkedTxnId)
            put("createdAt", inv.createdAt)
            put("updatedAt", inv.updatedAt)
            put("items", itemsJson)
        }.toString()
        deletedRecordDao.upsert(
            DeletedRecordEntity(
                id = "trash_${UUID.randomUUID()}",
                entityType = "invoice",
                originalId = inv.id,
                title = "${inv.number} · ${inv.customerName}",
                subtitle = inv.issueDate,
                amount = invWithItems.total,
                payloadJson = payload
            )
        )
    }

    private suspend fun archiveSavingsGoalLocked(goal: SavingsGoalEntity, logs: List<SavingsLogEntity>) {
        val logsJson = JSONArray()
        logs.forEach { l ->
            logsJson.put(JSONObject().apply {
                put("id", l.id)
                put("savingsId", l.savingsId)
                put("type", l.type)
                put("amount", l.amount)
                put("date", l.date)
                put("note", l.note)
                put("createdAt", l.createdAt)
            })
        }
        val payload = JSONObject().apply {
            put("id", goal.id)
            put("title", goal.title)
            put("targetAmount", goal.targetAmount)
            put("currentAmount", goal.currentAmount)
            put("targetDate", goal.targetDate)
            put("notes", goal.notes)
            put("createdAt", goal.createdAt)
            put("updatedAt", goal.updatedAt)
            put("logs", logsJson)
        }.toString()
        deletedRecordDao.upsert(
            DeletedRecordEntity(
                id = "trash_${UUID.randomUUID()}",
                entityType = "savings_goal",
                originalId = goal.id,
                title = goal.title,
                subtitle = goal.targetDate,
                amount = goal.currentAmount,
                payloadJson = payload
            )
        )
    }

    suspend fun restoreDeletedRecord(trashId: String): Boolean {
        val restored = db.withTransaction {
            val record = deletedRecordDao.getById(trashId) ?: return@withTransaction false
            val o = JSONObject(record.payloadJson)
            when (record.entityType) {
                "transaction" -> {
                    txnDao.upsert(
                        TransactionEntity(
                            id = o.optString("id").ifBlank { "txn_${UUID.randomUUID()}" },
                            type = if (o.optString("type") == "expense") "expense" else "income",
                            date = o.optString("date", FormatUtils.todayIso()),
                            description = o.optString("description", ""),
                            party = o.optString("party", ""),
                            category = o.optString("category", "Mauzo ya bidhaa"),
                            amount = o.optDouble("amount", 0.0),
                            method = o.optString("method", "Cash"),
                            reference = o.optString("reference", ""),
                            notes = o.optString("notes", ""),
                            source = o.optString("source", "manual"),
                            linkedId = o.optString("linkedId").takeIf { it.isNotBlank() && it != "null" },
                            createdAt = o.optString("createdAt", FormatUtils.todayIso())
                        )
                    )
                }
                "bill" -> {
                    val bill = BillEntity(
                        id = o.optString("id").ifBlank { "bill_${UUID.randomUUID()}" },
                        name = o.optString("name", ""),
                        category = o.optString("category", "Kodi ya pango"),
                        payee = o.optString("payee", ""),
                        amount = o.optDouble("amount", 0.0),
                        dueDate = o.optString("dueDate", FormatUtils.todayIso()),
                        paid = o.optBoolean("paid", false),
                        paidDate = o.optString("paidDate", ""),
                        method = o.optString("method", "Benki"),
                        recurring = o.optBoolean("recurring", false),
                        notes = o.optString("notes", ""),
                        linkedTxnId = null,
                        createdAt = o.optString("createdAt", FormatUtils.todayIso())
                    )
                    val linkedId = syncBillTransactionLocked(bill)
                    billDao.upsert(bill.copy(linkedTxnId = linkedId))
                }
                "invoice" -> {
                    val invId = o.optString("id").ifBlank { "inv_${UUID.randomUUID()}" }
                    val inv = InvoiceEntity(
                        id = invId,
                        number = o.optString("number", ""),
                        customerName = o.optString("customerName", ""),
                        customerPhone = o.optString("customerPhone", ""),
                        customerEmail = o.optString("customerEmail", ""),
                        customerAddress = o.optString("customerAddress", ""),
                        issueDate = o.optString("issueDate", FormatUtils.todayIso()),
                        dueDate = o.optString("dueDate", FormatUtils.todayIso()),
                        discount = o.optDouble("discount", 0.0),
                        vatRate = o.optDouble("vatRate", 0.0),
                        amountPaid = o.optDouble("amountPaid", 0.0),
                        state = o.optString("state", "draft"),
                        paidDate = o.optString("paidDate", ""),
                        notes = o.optString("notes", ""),
                        linkedTxnId = null,
                        createdAt = o.optString("createdAt", FormatUtils.todayIso()),
                        updatedAt = FormatUtils.todayIso()
                    )
                    val linkedId = syncInvoiceTransactionLocked(inv)
                    invoiceDao.upsertInvoice(inv.copy(linkedTxnId = linkedId))
                    invoiceDao.deleteItemsForInvoice(invId)
                    val itemsJson = o.optJSONArray("items")
                    if (itemsJson != null) {
                        val items = mutableListOf<InvoiceItemEntity>()
                        for (j in 0 until itemsJson.length()) {
                            val io = itemsJson.getJSONObject(j)
                            items += InvoiceItemEntity(
                                invoiceId = invId,
                                description = io.optString("description", ""),
                                qty = io.optDouble("qty", 1.0),
                                price = io.optDouble("price", 0.0),
                                position = j
                            )
                        }
                        if (items.isNotEmpty()) invoiceDao.insertItems(items)
                    }
                }
                "savings_goal" -> {
                    val gId = o.optString("id").ifBlank { "svg_${UUID.randomUUID()}" }
                    savingsDao.upsertGoal(
                        SavingsGoalEntity(
                            id = gId,
                            title = o.optString("title", "Akiba"),
                            targetAmount = o.optDouble("targetAmount", 0.0),
                            currentAmount = o.optDouble("currentAmount", 0.0),
                            targetDate = o.optString("targetDate", ""),
                            notes = o.optString("notes", ""),
                            createdAt = o.optString("createdAt", FormatUtils.todayIso()),
                            updatedAt = FormatUtils.todayIso()
                        )
                    )
                    val logsJson = o.optJSONArray("logs")
                    if (logsJson != null) {
                        val logs = mutableListOf<SavingsLogEntity>()
                        for (j in 0 until logsJson.length()) {
                            val lo = logsJson.getJSONObject(j)
                            logs += SavingsLogEntity(
                                id = lo.optString("id").ifBlank { "slog_${UUID.randomUUID()}" },
                                savingsId = gId,
                                type = if (lo.optString("type") == "withdraw") "withdraw" else "deposit",
                                amount = lo.optDouble("amount", 0.0),
                                date = lo.optString("date", FormatUtils.todayIso()),
                                note = lo.optString("note", ""),
                                createdAt = lo.optString("createdAt", FormatUtils.todayIso())
                            )
                        }
                        if (logs.isNotEmpty()) savingsDao.upsertLogs(logs)
                    }
                }
                else -> return@withTransaction false
            }
            deletedRecordDao.deleteById(trashId)
            true
        }
        if (restored) {
            scheduleAutoBackup()
        }
        return restored
    }

    suspend fun permanentlyDeleteTrashRecord(trashId: String) {
        deletedRecordDao.deleteById(trashId)
    }

    suspend fun emptyTrash() {
        deletedRecordDao.deleteAll()
    }

    // --- Data Management, Demo Seed & Backup ---

    suspend fun clearFinancialData() {
        // Always write an emergency pre-clear backup to phone storage before wiping live tables
        runCatching { storageVault.savePreClearSnapshot(exportBackupJson()) }
        db.withTransaction {
            txnDao.deleteAll()
            pendingTxnDao.deleteAll()
            invoiceDao.deleteAll()
            billDao.deleteAll()
            savingsDao.deleteAllLogs()
            savingsDao.deleteAllGoals()
            val currentSettings = settingsDao.getSettings() ?: BusinessSettingsEntity()
            settingsDao.upsert(currentSettings.copy(demo = false))
        }
    }

    suspend fun loadDemoData() = db.withTransaction {
        txnDao.deleteAll()
        pendingTxnDao.deleteAll()
        invoiceDao.deleteAll()
        billDao.deleteAll()
        savingsDao.deleteAllLogs()
        savingsDao.deleteAllGoals()

        val today = FormatUtils.todayIso()
        val m1 = FormatUtils.addMonthsIso(today, -1)
        val m2 = FormatUtils.addMonthsIso(today, -2)
        val m3 = FormatUtils.addMonthsIso(today, -3)
        val year = Calendar.getInstance().get(Calendar.YEAR)

        settingsDao.upsert(
            BusinessSettingsEntity(
                id = 1,
                businessName = "VaultLedger",
                ownerName = "",
                tagline = "Ushauri, mauzo & huduma za kidijitali",
                address = "Kimara Mwisho, Ubungo, Dar es Salaam",
                phone = "+255 754 000 000",
                email = "hello@mengidata.co.tz",
                tin = "123-456-789",
                website = "mengidata.co.tz",
                invoicePrefix = "MD",
                vatRate = 0.0,
                paymentDetails = "M-Pesa Lipa Namba: 5521400 (Mengidata)\nBenki: CRDB Bank — A/C 0150 2200 4400",
                invoiceNote = "Asante kwa kufanya biashara nasi. Malipo yafanyike kabla ya tarehe ya mwisho.",
                currency = "TZS",
                demo = true
            )
        )

        val seedTxns = listOf(
            TransactionEntity("demo_t1", "income", today, "Mauzo ya vifaa vya ofisi & kompyuta", "Tumaini Stores", "Mauzo ya bidhaa", 1_850_000.0, "M-Pesa", "MP-884210", "Malipo ya taslimu", "manual", null, today),
            TransactionEntity("demo_t2", "income", FormatUtils.addDaysIso(today, -3), "Huduma ya ufungaji wa mtandao", "Green Valley Hotel", "Huduma", 950_000.0, "Benki", "CRDB-221045", "Awamu ya pili", "manual", null, today),
            TransactionEntity("demo_t3", "expense", FormatUtils.addDaysIso(today, -4), "Manunuzi ya stock — vifaa vya ofisi", "Kariakoo Electronics", "Manunuzi / stock", 620_000.0, "Cash", "RCP-9941", "", "manual", null, today),
            TransactionEntity("demo_t4", "expense", FormatUtils.addDaysIso(today, -6), "Mafuta na usafiri wa kikazi", "Puma Energy", "Usafiri", 180_000.0, "Cash", "", "", "manual", null, today),
            TransactionEntity("demo_t5", "income", FormatUtils.addDaysIso(m1, -8), "Ushauri wa mfumo wa mauzo na TEHAMA", "Sahara Agro Ltd", "Ushauri", 2_100_000.0, "Benki", "NMB-77812", "", "manual", null, m1),
            TransactionEntity("demo_t6", "expense", FormatUtils.addDaysIso(m1, -5), "Usafiri wa kikazi — Mwanza", "Air Tanzania", "Usafiri", 350_000.0, "M-Pesa", "ATCL-114", "", "manual", null, m1),
            TransactionEntity("demo_t7", "income", FormatUtils.addDaysIso(m2, -10), "Mauzo ya printer na toner za ofisi", "Mlimani Fabrics", "Mauzo ya bidhaa", 1_450_000.0, "Benki", "", "", "manual", null, m2),
            TransactionEntity("demo_t8", "expense", FormatUtils.addDaysIso(m2, -7), "Matengenezo ya gari la kazi", "Garage ya Mjini", "Matengenezo", 280_000.0, "Cash", "", "", "manual", null, m2),
            TransactionEntity("demo_t9", "income", FormatUtils.addDaysIso(m3, -12), "Mradi wa mfumo wa stoo na data", "Bahari Logistics", "Huduma", 3_200_000.0, "Benki", "CRDB-8812", "", "manual", null, m3),
            TransactionEntity("demo_t10", "expense", FormatUtils.addDaysIso(m3, -9), "Umeme na maji ya ofisi", "TANESCO & DAWASA", "Umeme & maji", 190_000.0, "M-Pesa", "", "", "manual", null, m3)
        )
        txnDao.upsertAll(seedTxns)

        saveInvoice(
            InvoiceEntity(
                id = "demo_inv_1",
                number = "MD-$year-003",
                customerName = "Bahari Logistics Ltd",
                customerPhone = "+255 713 220 110",
                customerEmail = "accounts@bahari.co.tz",
                customerAddress = "Kurasini Bandari, Dar es Salaam",
                issueDate = FormatUtils.addDaysIso(today, -2),
                dueDate = FormatUtils.addDaysIso(today, 12),
                state = "draft",
                notes = "Rasimu inayosubiri saini ya mteja."
            ),
            listOf(
                InvoiceItemEntity(invoiceId = "demo_inv_1", description = "Mfumo wa kufuatilia mizigo na maghala", qty = 1.0, price = 2_400_000.0, position = 0),
                InvoiceItemEntity(invoiceId = "demo_inv_1", description = "Mafunzo ya timu ya stoo na ripoti", qty = 1.0, price = 600_000.0, position = 1)
            )
        )

        saveInvoice(
            InvoiceEntity(
                id = "demo_inv_2",
                number = "MD-$year-002",
                customerName = "Kilimanjaro Coffee Co.",
                customerPhone = "+255 754 889 200",
                customerEmail = "orders@kilimanjarocoffee.co.tz",
                customerAddress = "Moshi Mjini, Kilimanjaro",
                issueDate = FormatUtils.addDaysIso(today, -7),
                dueDate = FormatUtils.addDaysIso(today, 7),
                state = "issued",
                amountPaid = 600_000.0,
                paidDate = FormatUtils.addDaysIso(today, -3),
                notes = "Awamu ya kwanza imepokelewa."
            ),
            listOf(
                InvoiceItemEntity(invoiceId = "demo_inv_2", description = "Ubunifu na uchapishaji wa vifungashio", qty = 1.0, price = 1_250_000.0, position = 0),
                InvoiceItemEntity(invoiceId = "demo_inv_2", description = "Ushauri wa chapa na masoko", qty = 1.0, price = 700_000.0, position = 1)
            )
        )

        saveInvoice(
            InvoiceEntity(
                id = "demo_inv_3",
                number = "MD-$year-001",
                customerName = "Tumaini Stores",
                customerPhone = "+255 765 330 440",
                customerAddress = "Kariakoo Msimbazi, Dar es Salaam",
                issueDate = FormatUtils.addDaysIso(today, -16),
                dueDate = FormatUtils.addDaysIso(today, -2),
                state = "issued",
                amountPaid = 2_300_000.0,
                paidDate = FormatUtils.addDaysIso(today, -2),
                notes = "Imelipwa yote kupitia M-Pesa."
            ),
            listOf(
                InvoiceItemEntity(invoiceId = "demo_inv_3", description = "Kuweka mfumo wa mauzo (POS)", qty = 1.0, price = 1_800_000.0, position = 0),
                InvoiceItemEntity(invoiceId = "demo_inv_3", description = "Mafunzo ya wafanyakazi wa kaunta", qty = 1.0, price = 500_000.0, position = 1)
            )
        )

        saveBill(BillEntity("demo_bill_1", "Kodi ya pango ya ofisi", "Kodi ya pango", "Ubungo Plaza Ltd", 700_000.0, FormatUtils.addDaysIso(today, -5), true, FormatUtils.addDaysIso(today, -6), "Benki", true, "Pango la ofisi ya Ubungo"))
        saveBill(BillEntity("demo_bill_2", "Mishahara ya timu", "Mishahara", "Timu ya Ofisi", 1_200_000.0, FormatUtils.addDaysIso(today, 6), false, "", "Benki", true, "Malipo ya mwisho wa mwezi"))
        saveBill(BillEntity("demo_bill_3", "Bili ya Umeme — LUKU", "Umeme & maji", "TANESCO", 85_000.0, FormatUtils.addDaysIso(today, 3), false, "", "M-Pesa", true, ""))
        saveBill(BillEntity("demo_bill_4", "Internet ya kasi (Fiber 50Mbps)", "Internet & simu", "Vodacom Business", 120_000.0, FormatUtils.addDaysIso(today, -1), false, "", "M-Pesa", true, "Muda umepita — lipa leo"))
        saveBill(BillEntity("demo_bill_5", "Kodi ya Mapato (TRA)", "Kodi & vibali", "TRA", 450_000.0, FormatUtils.addDaysIso(today, 10), false, "", "Benki", false, "Makadirio ya robo mwaka"))

        val g1 = SavingsGoalEntity("demo_svg_1", "Mfuko wa Dharura ya Biashara", 5_000_000.0, 2_800_000.0, FormatUtils.addDaysIso(today, 180), "Akiba ya kulinda biashara.", today, today)
        val g2 = SavingsGoalEntity("demo_svg_2", "Kodi ya Pango ya Mwaka Ujao", 3_500_000.0, 1_400_000.0, FormatUtils.addDaysIso(today, 120), "Akiba ya kodi ya pango.", today, today)
        val g3 = SavingsGoalEntity("demo_svg_3", "Kupanua Akiba ya Vifaa", 2_000_000.0, 650_000.0, FormatUtils.addDaysIso(today, 90), "Kununua vifaa vipya vya kazi.", today, today)
        savingsDao.upsertGoals(listOf(g1, g2, g3))
        savingsDao.upsertLogs(
            listOf(
                SavingsLogEntity("demo_slog_1", g1.id, "deposit", 2_800_000.0, FormatUtils.addDaysIso(today, -20), "Akiba ya kuanzia", today),
                SavingsLogEntity("demo_slog_2", g2.id, "deposit", 1_400_000.0, FormatUtils.addDaysIso(today, -12), "Mchango wa robo mwaka", today)
            )
        )
    }

    // --- Backward-compatible JSON Backup Export & Import ---

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 4)
        root.put("exportedAt", FormatUtils.todayIso())
        root.put("exportedAtMillis", System.currentTimeMillis())

        val s = settingsDao.getSettings() ?: BusinessSettingsEntity()
        root.put("settings", JSONObject().apply {
            put("businessName", s.businessName)
            put("ownerName", s.ownerName)
            put("tagline", s.tagline)
            put("address", s.address)
            put("phone", s.phone)
            put("email", s.email)
            put("tin", s.tin)
            put("website", s.website)
            put("invoicePrefix", s.invoicePrefix)
            put("vatRate", s.vatRate)
            put("paymentDetails", s.paymentDetails)
            put("invoiceNote", s.invoiceNote)
            put("currency", s.currency)
            put("demo", s.demo)
        })

        val txnsArray = JSONArray()
        txnDao.getAll().forEach { t ->
            txnsArray.put(JSONObject().apply {
                put("id", t.id)
                put("type", t.type)
                put("date", t.date)
                put("description", t.description)
                put("party", t.party)
                put("category", t.category)
                put("amount", t.amount)
                put("method", t.method)
                put("reference", t.reference)
                put("notes", t.notes)
                put("source", t.source)
                put("linkedId", t.linkedId)
                put("createdAt", t.createdAt)
            })
        }
        root.put("transactions", txnsArray)

        val invArray = JSONArray()
        invoiceDao.getAllWithItems().forEach { invWithItems ->
            val inv = invWithItems.invoice
            val itemsJson = JSONArray()
            invWithItems.orderedItems.forEach { item ->
                itemsJson.put(JSONObject().apply {
                    put("description", item.description)
                    put("qty", item.qty)
                    put("price", item.price)
                })
            }
            invArray.put(JSONObject().apply {
                put("id", inv.id)
                put("number", inv.number)
                put("customerName", inv.customerName)
                put("customerPhone", inv.customerPhone)
                put("customerEmail", inv.customerEmail)
                put("customerAddress", inv.customerAddress)
                put("issueDate", inv.issueDate)
                put("dueDate", inv.dueDate)
                put("discount", inv.discount)
                put("vatRate", inv.vatRate)
                put("amountPaid", inv.amountPaid)
                put("state", inv.state)
                put("paidDate", inv.paidDate)
                put("notes", inv.notes)
                put("linkedTxnId", inv.linkedTxnId)
                put("createdAt", inv.createdAt)
                put("updatedAt", inv.updatedAt)
                put("items", itemsJson)
            })
        }
        root.put("invoices", invArray)

        val billsArray = JSONArray()
        billDao.getAll().forEach { b ->
            billsArray.put(JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("category", b.category)
                put("payee", b.payee)
                put("amount", b.amount)
                put("dueDate", b.dueDate)
                put("paid", b.paid)
                put("paidDate", b.paidDate)
                put("method", b.method)
                put("recurring", b.recurring)
                put("notes", b.notes)
                put("linkedTxnId", b.linkedTxnId)
                put("createdAt", b.createdAt)
            })
        }
        root.put("bills", billsArray)

        val savingsArray = JSONArray()
        savingsDao.getGoals().forEach { g ->
            savingsArray.put(JSONObject().apply {
                put("id", g.id)
                put("title", g.title)
                put("targetAmount", g.targetAmount)
                put("currentAmount", g.currentAmount)
                put("targetDate", g.targetDate)
                put("notes", g.notes)
                put("createdAt", g.createdAt)
                put("updatedAt", g.updatedAt)
            })
        }
        root.put("savings", savingsArray)

        val logsArray = JSONArray()
        savingsDao.getAllLogs().forEach { l ->
            logsArray.put(JSONObject().apply {
                put("id", l.id)
                put("savingsId", l.savingsId)
                put("type", l.type)
                put("amount", l.amount)
                put("date", l.date)
                put("note", l.note)
                put("createdAt", l.createdAt)
            })
        }
        root.put("savingsLogs", logsArray)

        val categoriesArray = JSONArray()
        categoryDao.getAll().forEach { c ->
            categoriesArray.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("type", c.type)
                put("iconKey", c.iconKey)
                put("colorHex", c.colorHex)
                put("monthlyBudget", c.monthlyBudget)
                put("isDefault", c.isDefault)
                put("createdAt", c.createdAt)
            })
        }
        root.put("categories", categoriesArray)

        val pendingArray = JSONArray()
        pendingTxnDao.getAll().forEach { p ->
            pendingArray.put(JSONObject().apply {
                put("id", p.id)
                put("type", p.type)
                put("description", p.description)
                put("details", p.details)
                put("party", p.party)
                put("phoneNumber", p.phoneNumber)
                put("location", p.location)
                put("category", p.category)
                put("amount", p.amount)
                put("paidAmount", p.paidAmount)
                put("method", p.method)
                put("expectedDate", p.expectedDate)
                put("createdAt", p.createdAt)
            })
        }
        root.put("pendingTransactions", pendingArray)

        root.toString(2)
    }

    suspend fun importBackupJson(jsonText: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonText)
        val txnsJson = root.optJSONArray("transactions")
            ?: throw IllegalArgumentException("Backup missing transactions array")
        val invoicesJson = root.optJSONArray("invoices")
            ?: throw IllegalArgumentException("Backup missing invoices array")

        db.withTransaction {
            txnDao.deleteAll()
            pendingTxnDao.deleteAll()
            invoiceDao.deleteAll()
            billDao.deleteAll()
            savingsDao.deleteAllLogs()
            savingsDao.deleteAllGoals()

            root.optJSONObject("settings")?.let { js ->
                val defaultSettings = BusinessSettingsEntity()
                settingsDao.upsert(
                    BusinessSettingsEntity(
                        id = 1,
                        businessName = js.optString("businessName", defaultSettings.businessName),
                        ownerName = js.optString("ownerName", ""),
                        tagline = js.optString("tagline", defaultSettings.tagline),
                        address = js.optString("address", ""),
                        phone = js.optString("phone", ""),
                        email = js.optString("email", ""),
                        tin = js.optString("tin", ""),
                        website = js.optString("website", ""),
                        invoicePrefix = js.optString("invoicePrefix", "MD"),
                        vatRate = js.optDouble("vatRate", 0.0),
                        paymentDetails = js.optString("paymentDetails", ""),
                        invoiceNote = js.optString("invoiceNote", defaultSettings.invoiceNote),
                        currency = js.optString("currency", "TZS"),
                        demo = js.optBoolean("demo", false)
                    )
                )
            }

            val txns = mutableListOf<TransactionEntity>()
            for (i in 0 until txnsJson.length()) {
                val o = txnsJson.getJSONObject(i)
                txns += TransactionEntity(
                    id = o.optString("id").ifBlank { "txn_${UUID.randomUUID()}" },
                    type = if (o.optString("type") == "expense") "expense" else "income",
                    date = o.optString("date", FormatUtils.todayIso()),
                    description = o.optString("description", ""),
                    party = o.optString("party", ""),
                    category = o.optString("category", "Mauzo ya bidhaa"),
                    amount = o.optDouble("amount", 0.0),
                    method = o.optString("method", "Cash"),
                    reference = o.optString("reference", ""),
                    notes = o.optString("notes", ""),
                    source = o.optString("source", "manual"),
                    linkedId = o.optString("linkedId").takeIf { it.isNotBlank() && it != "null" },
                    createdAt = o.optString("createdAt", FormatUtils.todayIso())
                )
            }
            txnDao.upsertAll(txns)

            for (i in 0 until invoicesJson.length()) {
                val o = invoicesJson.getJSONObject(i)
                val invId = o.optString("id").ifBlank { "inv_${UUID.randomUUID()}" }
                invoiceDao.upsertInvoice(
                    InvoiceEntity(
                        id = invId,
                        number = o.optString("number", ""),
                        customerName = o.optString("customerName", ""),
                        customerPhone = o.optString("customerPhone", ""),
                        customerEmail = o.optString("customerEmail", ""),
                        customerAddress = o.optString("customerAddress", ""),
                        issueDate = o.optString("issueDate", FormatUtils.todayIso()),
                        dueDate = o.optString("dueDate", FormatUtils.todayIso()),
                        discount = o.optDouble("discount", 0.0),
                        vatRate = o.optDouble("vatRate", 0.0),
                        amountPaid = o.optDouble("amountPaid", 0.0),
                        state = o.optString("state", "draft"),
                        paidDate = o.optString("paidDate", ""),
                        notes = o.optString("notes", ""),
                        linkedTxnId = o.optString("linkedTxnId").takeIf { it.isNotBlank() && it != "null" },
                        createdAt = o.optString("createdAt", FormatUtils.todayIso()),
                        updatedAt = o.optString("updatedAt", FormatUtils.todayIso())
                    )
                )
                val itemsJson = o.optJSONArray("items")
                if (itemsJson != null) {
                    val items = mutableListOf<InvoiceItemEntity>()
                    for (j in 0 until itemsJson.length()) {
                        val io = itemsJson.getJSONObject(j)
                        items += InvoiceItemEntity(
                            invoiceId = invId,
                            description = io.optString("description", ""),
                            qty = io.optDouble("qty", 1.0),
                            price = io.optDouble("price", 0.0),
                            position = j
                        )
                    }
                    invoiceDao.insertItems(items)
                }
            }

            root.optJSONArray("bills")?.let { arr ->
                val bills = mutableListOf<BillEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    bills += BillEntity(
                        id = o.optString("id").ifBlank { "bill_${UUID.randomUUID()}" },
                        name = o.optString("name", ""),
                        category = o.optString("category", "Kodi ya pango"),
                        payee = o.optString("payee", ""),
                        amount = o.optDouble("amount", 0.0),
                        dueDate = o.optString("dueDate", FormatUtils.todayIso()),
                        paid = o.optBoolean("paid", false),
                        paidDate = o.optString("paidDate", ""),
                        method = o.optString("method", "Benki"),
                        recurring = o.optBoolean("recurring", false),
                        notes = o.optString("notes", ""),
                        linkedTxnId = o.optString("linkedTxnId").takeIf { it.isNotBlank() && it != "null" },
                        createdAt = o.optString("createdAt", FormatUtils.todayIso())
                    )
                }
                billDao.upsertAll(bills)
            }

            val importedGoalIds = mutableSetOf<String>()
            root.optJSONArray("savings")?.let { arr ->
                val goals = mutableListOf<SavingsGoalEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val gId = o.optString("id").ifBlank { "svg_${UUID.randomUUID()}" }
                    importedGoalIds += gId
                    goals += SavingsGoalEntity(
                        id = gId,
                        title = o.optString("title", ""),
                        targetAmount = o.optDouble("targetAmount", 0.0),
                        currentAmount = o.optDouble("currentAmount", 0.0),
                        targetDate = o.optString("targetDate", ""),
                        notes = o.optString("notes", ""),
                        createdAt = o.optString("createdAt", FormatUtils.todayIso()),
                        updatedAt = o.optString("updatedAt", FormatUtils.todayIso())
                    )
                }
                savingsDao.upsertGoals(goals)
            }

            root.optJSONArray("savingsLogs")?.let { arr ->
                val logs = mutableListOf<SavingsLogEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val sId = o.optString("savingsId", "")
                    if (sId in importedGoalIds) {
                        logs += SavingsLogEntity(
                            id = o.optString("id").ifBlank { "slog_${UUID.randomUUID()}" },
                            savingsId = sId,
                            type = if (o.optString("type") == "withdraw") "withdraw" else "deposit",
                            amount = o.optDouble("amount", 0.0),
                            date = o.optString("date", FormatUtils.todayIso()),
                            note = o.optString("note", ""),
                            createdAt = o.optString("createdAt", FormatUtils.todayIso())
                        )
                    }
                }
                savingsDao.upsertLogs(logs)
            }

            root.optJSONArray("categories")?.let { arr ->
                val cats = mutableListOf<TransactionCategoryEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val cName = o.optString("name", "").trim()
                    if (cName.isNotEmpty()) {
                        cats += TransactionCategoryEntity(
                            id = o.optString("id").ifBlank { "cat_${UUID.randomUUID()}" },
                            name = cName,
                            type = if (o.optString("type") == "expense") "expense" else "income",
                            iconKey = o.optString("iconKey", "tag"),
                            colorHex = o.optString("colorHex", "#2563EB"),
                            monthlyBudget = o.optDouble("monthlyBudget", 0.0),
                            isDefault = o.optBoolean("isDefault", false),
                            createdAt = o.optString("createdAt", FormatUtils.todayIso())
                        )
                    }
                }
                if (cats.isNotEmpty()) {
                    categoryDao.deleteAll()
                    categoryDao.upsertAll(cats)
                }
            }
            root.optJSONArray("pendingTransactions")?.let { arr ->
                val pendingList = mutableListOf<PendingTransactionEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val desc = o.optString("description", "").trim()
                    if (desc.isNotEmpty()) {
                        pendingList += PendingTransactionEntity(
                            id = o.optString("id").ifBlank { "ptx_${UUID.randomUUID().toString().take(12)}" },
                            type = if (o.optString("type") == "income") "income" else "expense",
                            description = desc,
                            details = o.optString("details", ""),
                            party = o.optString("party", ""),
                            phoneNumber = o.optString("phoneNumber", ""),
                            location = o.optString("location", ""),
                            category = o.optString("category", "Mengineyo"),
                            amount = o.optDouble("amount", 0.0),
                            paidAmount = o.optDouble("paidAmount", 0.0),
                            method = o.optString("method", "M-Pesa"),
                            expectedDate = o.optString("expectedDate", FormatUtils.todayIso()),
                            createdAt = o.optString("createdAt", FormatUtils.nowIsoTimestamp())
                        )
                    }
                }
                if (pendingList.isNotEmpty()) {
                    pendingTxnDao.upsertAll(pendingList)
                }
            }
            ensureDefaultCategoriesSeeded()
        }
    }

    // --- Automatic Local & Phone Storage Safety Vault (Zero-Loss Recovery) ---

    fun hasSafetySnapshot(): Boolean = storageVault.statusFlow.value.hasAnyBackup

    suspend fun refreshStorageStatusAsync(): PhoneStorageStatus = withContext(Dispatchers.IO) {
        storageVault.getStatus()
    }

    /**
     * Non-blocking debounced background auto-backup to phone storage.
     * Lets every UI save/edit/delete return in milliseconds while safely mirroring to
     * Documents/Mengidata, daily history, and custom SAF folder off the main thread.
     */
    fun scheduleAutoBackup(delayMs: Long = 600L) {
        if (!prefs.autoPhoneBackupEnabled.value) return
        scheduledBackupJob?.cancel()
        scheduledBackupJob = autoBackupScope.launch {
            if (delayMs > 0L) delay(delayMs)
            persistSafetySnapshot(force = false)
        }
    }

    suspend fun persistSafetySnapshot(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!force && !prefs.autoPhoneBackupEnabled.value) {
            return@withContext false
        }
        scheduledBackupJob?.cancel()
        backupMutex.withLock {
            runCatching {
                val demoOnly = isDatabaseOnlyDemoOrEmpty()
                if (!demoOnly) {
                    // Clear legacy demo flag so real data is permanently recognized as owner data
                    val currentSettings = settingsDao.getSettings()
                    if (currentSettings != null && currentSettings.demo) {
                        settingsDao.upsert(currentSettings.copy(demo = false))
                    }
                    if (prefs.pendingReinstallRestore.value) {
                        prefs.setPendingReinstallRestore(false)
                    }
                }
                val json = exportBackupJson()
                // CRITICAL: Never let untouched demo data overwrite a real user backup in Documents/Mengidata!
                storageVault.saveAutoSnapshot(json, allowPublicOverwrite = !demoOnly)
            }.getOrDefault(false)
        }
    }

    suspend fun syncToPhoneStorageNow(): Boolean = persistSafetySnapshot(force = true)

    suspend fun restoreLatestSafetySnapshot(): Boolean = withContext(Dispatchers.IO) {
        val json = storageVault.readBestAvailableSnapshot(requireRealUserData = true)
            ?: storageVault.readBestAvailableSnapshot(requireRealUserData = false)
            ?: return@withContext false
        runCatching {
            importBackupJson(json)
            reconcileLedgerIntegrity()
            prefs.setPendingReinstallRestore(false)
            prefs.setRestorePromptDismissed(true)
            storageVault.refreshStatus()
            true
        }.getOrDefault(false)
    }

    // --- Color-Coded & Separated Reports (Excel .xls + Separated CSVs) ---

    suspend fun exportColorExcelReport(language: AppLanguage): String = withContext(Dispatchers.IO) {
        ColorReportExporter.buildColorExcelXml(
            settings = settingsDao.getSettings() ?: BusinessSettingsEntity(),
            transactions = txnDao.getAll(),
            bills = billDao.getAll(),
            invoices = invoiceDao.getAllWithItems(),
            savingsGoals = savingsDao.getGoals(),
            savingsLogs = savingsDao.getAllLogs(),
            language = language
        )
    }

    suspend fun exportTransactionsCsv(language: AppLanguage = AppLanguage.SW): String = withContext(Dispatchers.IO) {
        ColorReportExporter.buildTransactionsOnlyCsv(
            transactions = txnDao.getAll(),
            language = language
        )
    }

    suspend fun exportBillsCsv(language: AppLanguage = AppLanguage.SW): String = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettings() ?: BusinessSettingsEntity()
        ColorReportExporter.buildBillsOnlyCsv(
            bills = billDao.getAll(),
            transactions = txnDao.getAll(),
            invoices = invoiceDao.getAllWithItems(),
            savingsGoals = savingsDao.getGoals(),
            savingsLogs = savingsDao.getAllLogs(),
            currency = settings.currency.ifBlank { "TZS" },
            language = language
        )
    }

    suspend fun exportSavingsCsv(language: AppLanguage = AppLanguage.SW): String = withContext(Dispatchers.IO) {
        ColorReportExporter.buildSavingsOnlyCsv(
            savingsGoals = savingsDao.getGoals(),
            savingsLogs = savingsDao.getAllLogs(),
            language = language
        )
    }

    suspend fun exportMasterSeparatedCsv(language: AppLanguage = AppLanguage.SW): String = withContext(Dispatchers.IO) {
        ColorReportExporter.buildMasterSeparatedCsv(
            settings = settingsDao.getSettings() ?: BusinessSettingsEntity(),
            transactions = txnDao.getAll(),
            bills = billDao.getAll(),
            invoices = invoiceDao.getAllWithItems(),
            savingsGoals = savingsDao.getGoals(),
            savingsLogs = savingsDao.getAllLogs(),
            language = language
        )
    }

    suspend fun exportCategoryAnalyticsCsv(
        period: PeriodFilter,
        language: AppLanguage = AppLanguage.SW
    ): String = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettings() ?: BusinessSettingsEntity()
        ColorReportExporter.buildCategoryAnalyticsCsv(
            transactions = txnDao.getAll(),
            categories = categoryDao.getAll(),
            period = period,
            currency = settings.currency.ifBlank { "TZS" },
            language = language
        )
    }

    suspend fun savePendingTransaction(item: PendingTransactionEntity): PendingTransactionEntity =
        withContext(Dispatchers.IO) {
            val cleanAmount = max(item.amount, 0.0)
            val cleanPaid = item.paidAmount.coerceIn(0.0, cleanAmount)
            val resolved = item.copy(
                id = item.id.ifBlank { "ptx_${UUID.randomUUID().toString().take(12)}" },
                party = item.party.trim(),
                phoneNumber = item.phoneNumber.trim(),
                location = item.location.trim(),
                details = item.details.trim(),
                amount = cleanAmount,
                paidAmount = cleanPaid,
                expectedDate = item.expectedDate.ifBlank { FormatUtils.todayIso() },
                createdAt = item.createdAt.ifBlank { FormatUtils.nowIsoTimestamp() }
            )
            pendingTxnDao.upsert(resolved)
            scheduleAutoBackup()
            resolved
        }

    suspend fun recordPendingPayment(
        id: String,
        addedAmount: Double,
        markFull: Boolean = false
    ): PendingTransactionEntity? = withContext(Dispatchers.IO) {
        val updated = db.withTransaction {
            val existing = pendingTxnDao.getById(id) ?: return@withTransaction null
            val nextPaid = if (markFull) {
                max(existing.amount, 0.0)
            } else {
                (existing.effectivePaidAmount + max(addedAmount, 0.0)).coerceIn(0.0, max(existing.amount, 0.0))
            }
            val next = existing.copy(paidAmount = nextPaid)
            pendingTxnDao.upsert(next)
            next
        }
        if (updated != null) {
            scheduleAutoBackup()
        }
        updated
    }

    suspend fun completePendingTransaction(id: String): TransactionEntity? = withContext(Dispatchers.IO) {
        val posted = db.withTransaction {
            val pending = pendingTxnDao.getById(id) ?: return@withTransaction null
            val combinedNotes = buildList {
                if (pending.phoneNumber.isNotBlank()) add("Simu: ${pending.phoneNumber.trim()}")
                if (pending.location.isNotBlank()) add("Eneo: ${pending.location.trim()}")
                if (pending.details.isNotBlank()) add(pending.details.trim())
            }.joinToString(" • ")
            val ledgerTxn = TransactionEntity(
                id = "tx_${UUID.randomUUID().toString().take(12)}",
                type = pending.type,
                date = FormatUtils.todayIso(),
                description = pending.description,
                party = pending.party,
                category = pending.category,
                amount = pending.amount,
                method = pending.method,
                notes = combinedNotes,
                source = "manual",
                createdAt = FormatUtils.nowIsoTimestamp()
            )
            txnDao.upsert(ledgerTxn)
            pendingTxnDao.deleteById(id)
            ledgerTxn
        }
        if (posted != null) {
            scheduleAutoBackup()
        }
        posted
    }

    suspend fun deletePendingTransaction(id: String): Boolean = withContext(Dispatchers.IO) {
        val existing = pendingTxnDao.getById(id) ?: return@withContext false
        pendingTxnDao.deleteById(existing.id)
        scheduleAutoBackup()
        true
    }

    companion object {
        private const val AUTO_SNAPSHOT_FILE = "mengidata_auto_snapshot.json"
    }
}
