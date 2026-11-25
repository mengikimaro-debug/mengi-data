package tz.mengidata.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import tz.mengidata.app.MainActivity
import tz.mengidata.app.R
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr

data class PhonePermissionsSnapshot(
    val postNotificationsGranted: Boolean = false,
    val systemNotificationsEnabled: Boolean = false,
    val installPackagesGranted: Boolean = false,
    val storageVaultReady: Boolean = true,
    val channelsConfigured: Int = 4
) {
    val allRequiredGranted: Boolean
        get() = postNotificationsGranted && systemNotificationsEnabled && installPackagesGranted
}

/**
 * Centralized Android 13+ (API 33+) Notification & Phone Permission Manager for Mengidata.
 *
 * Manages 4 dedicated notification channels:
 * 1. `mengidata_bills_invoices` — Overdue/upcoming bills & unpaid invoice alerts
 * 2. `mengidata_transactions` — Income/Expense ledger & household bill activity alerts
 * 3. `mengidata_savings` — Savings deposits, withdrawals & goal milestones
 * 4. `mengidata_system_updates` — Phone storage backup syncs & GitHub release updates
 */
class MengiNotificationManager(private val context: Context) {

    private val appContext = context.applicationContext

    init {
        ensureNotificationChannels()
    }

    fun ensureNotificationChannels() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_LARGE_BILL_WARNING,
                "Onyo la Bili Kubwa (Large Bill Warning)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tahadhari ya haraka (Pop-up Warning) wakati bili kubwa au matumizi makubwa ya nyumbani yanapoingizwa."
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_BILLS_INVOICES,
                "Bili & Ankara (Bills & Invoices)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Vikumbusho vya bili za nyumbani, bili zilizochelewa, na malipo ya ankara."
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_TRANSACTIONS,
                "Miamala & Leja (Ledger Activity)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Taarifa za mapato, matumizi, na miamala mipya iliyohifadhiwa."
            },
            NotificationChannel(
                CHANNEL_SAVINGS,
                "Akiba & Malengo (Savings Vault)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Taarifa za kuweka akiba, kutoa akiba, na kufikia malengo ya akiba."
            },
            NotificationChannel(
                CHANNEL_SYSTEM_UPDATES,
                "Mfumo, Hifadhi & Masasisho (System & Updates)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Taarifa za usawazishaji wa hifadhidata kwenye simu na masasisho ya GitHub."
            }
        )

        manager.createNotificationChannels(channels)
    }

    fun isPostNotificationsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun areSystemNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun getPermissionsSnapshot(storageVaultReady: Boolean = true): PhonePermissionsSnapshot {
        ensureNotificationChannels()
        return PhonePermissionsSnapshot(
            postNotificationsGranted = isPostNotificationsGranted(),
            systemNotificationsEnabled = areSystemNotificationsEnabled(),
            installPackagesGranted = canRequestPackageInstalls(),
            storageVaultReady = storageVaultReady,
            channelsConfigured = 4
        )
    }

    fun buildAppNotificationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildAppDetailsSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${appContext.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun buildInstallPermissionIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${appContext.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun sendTestNotification(language: AppLanguage, businessName: String): Boolean {
        val title = tr(
            language,
            "Taarifa za $businessName Zimewashwa",
            "$businessName Notifications Active"
        )
        val body = tr(
            language,
            "Mfumo wa taarifa za Bili, Ankara, Miamala, Akiba na Masasisho unafanya kazi vizuri kwenye Android 13+.",
            "Bills, Invoices, Ledger, Savings, and Update alerts are active and working on Android 13+."
        )
        return postNotification(
            channelId = CHANNEL_SYSTEM_UPDATES,
            notificationId = NOTIF_ID_TEST,
            title = title,
            message = body,
            highPriority = true
        )
    }

    fun notifyTransactionSaved(
        txn: TransactionEntity,
        currency: String,
        language: AppLanguage
    ) {
        val formatted = FormatUtils.formatMoney(txn.amount, currency)
        val kindLabel = if (txn.isIncome) {
            tr(language, "Mapato Mapya", "New Income")
        } else {
            tr(language, "Matumizi Mapya", "New Expense")
        }
        val title = "$kindLabel: $formatted"
        val body = "${txn.description} • ${txn.category}" +
            if (txn.party.isNotBlank()) " (${txn.party})" else ""
        postNotification(
            channelId = CHANNEL_TRANSACTIONS,
            notificationId = (NOTIF_ID_TXN_BASE + (txn.id.hashCode() and 0x7FF)),
            title = title,
            message = body
        )
    }

    fun notifyBillEvent(
        bill: BillEntity,
        currency: String,
        language: AppLanguage,
        justPaid: Boolean
    ) {
        val formatted = FormatUtils.formatMoney(bill.amount, currency)
        val title = if (justPaid) {
            tr(language, "Bili Imelipwa: ${bill.name}", "Bill Paid: ${bill.name}")
        } else {
            tr(language, "Bili Imehifadhiwa: ${bill.name}", "Bill Saved: ${bill.name}")
        }
        val body = if (justPaid) {
            tr(
                language,
                "Malipo ya $formatted yamekamilika na kuingizwa kwenye leja.",
                "Payment of $formatted recorded and synced to ledger."
            )
        } else {
            tr(
                language,
                "Kiasi: $formatted • Tarehe ya kulipa: ${bill.dueDate}",
                "Amount: $formatted • Due date: ${bill.dueDate}"
            )
        }
        postNotification(
            channelId = CHANNEL_BILLS_INVOICES,
            notificationId = (NOTIF_ID_BILL_BASE + (bill.id.hashCode() and 0x7FF)),
            title = title,
            message = body,
            highPriority = !justPaid
        )
    }

    fun notifyInvoiceEvent(
        invoiceNumber: String,
        clientName: String,
        amount: Double,
        currency: String,
        language: AppLanguage,
        isPayment: Boolean
    ) {
        val formatted = FormatUtils.formatMoney(amount, currency)
        val title = if (isPayment) {
            tr(language, "Malipo ya Ankara $invoiceNumber", "Invoice $invoiceNumber Payment")
        } else {
            tr(language, "Ankara $invoiceNumber Imehifadhiwa", "Invoice $invoiceNumber Saved")
        }
        val body = "$clientName • $formatted"
        postNotification(
            channelId = CHANNEL_BILLS_INVOICES,
            notificationId = (NOTIF_ID_INVOICE_BASE + (invoiceNumber.hashCode() and 0x7FF)),
            title = title,
            message = body
        )
    }

    fun notifySavingsActivity(
        goalName: String,
        amount: Double,
        newTotal: Double,
        targetAmount: Double,
        isDeposit: Boolean,
        currency: String,
        language: AppLanguage
    ) {
        val formattedAmount = FormatUtils.formatMoney(amount, currency)
        val formattedTotal = FormatUtils.formatMoney(newTotal, currency)
        val reachedGoal = targetAmount > 0.0 && newTotal >= targetAmount
        val title = when {
            reachedGoal -> tr(language, "Hongera! Lengo la Akiba Limefikiwa", "Savings Goal Reached!")
            isDeposit -> tr(language, "Akiba Imeongezwa: $goalName", "Savings Deposit: $goalName")
            else -> tr(language, "Akiba Imetolewa: $goalName", "Savings Withdrawal: $goalName")
        }
        val body = tr(
            language,
            "$goalName: $formattedAmount • Salio la sasa ni $formattedTotal",
            "$goalName: $formattedAmount • Current balance is $formattedTotal"
        )
        postNotification(
            channelId = CHANNEL_SAVINGS,
            notificationId = (NOTIF_ID_SAVINGS_BASE + (goalName.hashCode() and 0x7FF)),
            title = title,
            message = body
        )
    }

    fun notifySystemBackupSynced(pathLabel: String, language: AppLanguage) {
        val title = tr(
            language,
            "Hifadhidata Imelindwa Kwenye Simu",
            "Database Backed Up to Phone Storage"
        )
        val body = tr(
            language,
            "Nakala salama imehifadhiwa kwenye $pathLabel.",
            "Encrypted snapshot synced to $pathLabel."
        )
        postNotification(
            channelId = CHANNEL_SYSTEM_UPDATES,
            notificationId = NOTIF_ID_STORAGE_SYNC,
            title = title,
            message = body
        )
    }

    fun notifyUpdateAvailable(version: String, language: AppLanguage) {
        val title = tr(
            language,
            "Toleo Jipya la Mengidata v$version Linapatikana",
            "Mengidata v$version Update Available"
        )
        val body = tr(
            language,
            "Fungua Mipangilio kupakua na kusakinisha toleo jipya kutoka GitHub.",
            "Open Settings to download and install the latest release from GitHub."
        )
        postNotification(
            channelId = CHANNEL_SYSTEM_UPDATES,
            notificationId = NOTIF_ID_UPDATE_AVAILABLE,
            title = title,
            message = body,
            highPriority = true
        )
    }

    fun runSmartDueReminders(
        bills: List<BillEntity>,
        invoices: List<InvoiceWithItems>,
        currency: String,
        language: AppLanguage
    ): Int {
        val today = FormatUtils.todayIso()
        val overdueOrTodayBills = bills.filter { !it.paid && it.dueDate <= today }
        val overdueInvoices = invoices.filter { !it.isDraft && it.balance > 0.0 && it.invoice.dueDate < today }

        if (overdueOrTodayBills.isEmpty() && overdueInvoices.isEmpty()) return 0

        val totalBillDue = overdueOrTodayBills.sumOf { it.amount }
        val totalInvDue = overdueInvoices.sumOf { it.balance }

        val title = tr(
            language,
            "Ukumbusho wa Malipo (${overdueOrTodayBills.size} Bili, ${overdueInvoices.size} Ankara)",
            "Payment Reminder (${overdueOrTodayBills.size} Bills, ${overdueInvoices.size} Invoices)"
        )
        val bodyParts = mutableListOf<String>()
        if (overdueOrTodayBills.isNotEmpty()) {
            bodyParts += tr(
                language,
                "Bili zinazodaiwa: ${FormatUtils.formatMoney(totalBillDue, currency)}",
                "Bills due: ${FormatUtils.formatMoney(totalBillDue, currency)}"
            )
        }
        if (overdueInvoices.isNotEmpty()) {
            bodyParts += tr(
                language,
                "Madeni ya wateja: ${FormatUtils.formatMoney(totalInvDue, currency)}",
                "Unpaid invoices: ${FormatUtils.formatMoney(totalInvDue, currency)}"
            )
        }

        val posted = postNotification(
            channelId = CHANNEL_BILLS_INVOICES,
            notificationId = NOTIF_ID_DUE_SUMMARY,
            title = title,
            message = bodyParts.joinToString(" • "),
            highPriority = true
        )
        return if (posted) (overdueOrTodayBills.size + overdueInvoices.size) else 0
    }

    fun notifyLargeBillWarning(
        billName: String,
        amount: Double,
        threshold: Double,
        category: String,
        currency: String,
        language: AppLanguage
    ) {
        val formattedAmount = FormatUtils.formatMoney(amount, currency)
        val formattedThreshold = FormatUtils.formatMoney(threshold, currency)
        val title = tr(
            language,
            "ONYO: Bili Kubwa ($formattedAmount)",
            "WARNING: Large Bill ($formattedAmount)"
        )
        val body = tr(
            language,
            "$billName ($category) imezidi kiwango cha tahadhari cha $formattedThreshold.",
            "$billName ($category) exceeds your warning limit of $formattedThreshold."
        )
        postNotification(
            channelId = CHANNEL_LARGE_BILL_WARNING,
            notificationId = (NOTIF_ID_LARGE_BILL_BASE + (billName.hashCode() and 0x7FF)),
            title = title,
            message = body,
            highPriority = true,
            timeoutAfterMs = 3_000L
        )
    }

    private fun postNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        highPriority: Boolean = false,
        timeoutAfterMs: Long = 0L
    ): Boolean {
        if (!isPostNotificationsGranted() || !areSystemNotificationsEnabled()) {
            return false
        }
        ensureNotificationChannels()

        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priority = if (highPriority) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }

        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setCategory(if (highPriority) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (timeoutAfterMs > 0L) {
            builder.setTimeoutAfter(timeoutAfterMs)
        }

        val notification = builder.build()

        return runCatching {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
            true
        }.getOrDefault(false)
    }

    companion object {
        const val CHANNEL_LARGE_BILL_WARNING = "mengidata_large_bill_warning"
        const val CHANNEL_BILLS_INVOICES = "mengidata_bills_invoices"
        const val CHANNEL_TRANSACTIONS = "mengidata_transactions"
        const val CHANNEL_SAVINGS = "mengidata_savings"
        const val CHANNEL_SYSTEM_UPDATES = "mengidata_system_updates"

        private const val NOTIF_ID_TEST = 9001
        private const val NOTIF_ID_DUE_SUMMARY = 9002
        private const val NOTIF_ID_STORAGE_SYNC = 9003
        private const val NOTIF_ID_UPDATE_AVAILABLE = 9004
        private const val NOTIF_ID_TXN_BASE = 10000
        private const val NOTIF_ID_BILL_BASE = 12000
        private const val NOTIF_ID_INVOICE_BASE = 14000
        private const val NOTIF_ID_SAVINGS_BASE = 16000
        private const val NOTIF_ID_LARGE_BILL_BASE = 18000
    }
}

data class LargeBillWarningPopup(
    val id: Long = System.currentTimeMillis(),
    val billName: String,
    val amountFormatted: String,
    val thresholdFormatted: String,
    val category: String,
    val durationMillis: Long = 3_000L
)
