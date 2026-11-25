package tz.mengidata.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import tz.mengidata.app.MainActivity
import tz.mengidata.app.R
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr

data class PhonePermissionsSnapshot(
    val postNotificationsGranted: Boolean = false,
    val systemNotificationsEnabled: Boolean = false,
    val installPackagesGranted: Boolean = false,
    val storageManagerGranted: Boolean = false,
    val storageVaultReady: Boolean = true,
    val channelsConfigured: Int = 5
) {
    val allRequiredGranted: Boolean
        get() = postNotificationsGranted &&
            systemNotificationsEnabled &&
            installPackagesGranted &&
            storageManagerGranted
}

/**
 * Centralized Android 13+ (API 33+) System Notification & Phone Permission Manager for Mengidata.
 *
 * Delivers real Android system notifications (status bar icon, large brand icon, system sound,
 * vibration, and heads-up pop-up banners) across 5 dedicated channels:
 * 1. `mengidata_large_bill_warning_v2` — High-importance 3-second system pop-up warning for large bills
 * 2. `mengidata_bills_invoices_v2` — Overdue/upcoming bills & unpaid invoice alerts
 * 3. `mengidata_transactions_v2` — Income/Expense ledger & household bill activity alerts
 * 4. `mengidata_savings_v2` — Savings deposits, withdrawals & goal milestones
 * 5. `mengidata_system_updates_v2` — Phone storage backup syncs & GitHub release updates
 */
class MengiNotificationManager(private val context: Context) {

    private val appContext = context.applicationContext

    private val largeAppIconBitmap: Bitmap? by lazy {
        runCatching {
            val raw = BitmapFactory.decodeResource(
                appContext.resources,
                R.drawable.mengidata_oneui_icon_1795525888393
            ) ?: return@runCatching null
            Bitmap.createScaledBitmap(raw, 128, 128, true).also {
                if (it !== raw) raw.recycle()
            }
        }.getOrNull()
    }

    @Volatile
    private var channelsInitialized: Boolean = false

    fun ensureNotificationChannels() {
        if (channelsInitialized) return
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        synchronized(this) {
            if (channelsInitialized) return

            // Clean up legacy v1 channels once per process
            LEGACY_CHANNELS.forEach { oldId ->
                runCatching { manager.deleteNotificationChannel(oldId) }
            }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channels = listOf(
            NotificationChannel(
                CHANNEL_LARGE_BILL_WARNING,
                "Onyo la Bili Kubwa (Large Bill Warning)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tahadhari ya mfumo (System Heads-Up Warning) wakati bili kubwa au matumizi makubwa ya nyumbani yanapoingizwa."
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 180L, 80L, 180L)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttrs)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(
                CHANNEL_BILLS_INVOICES,
                "Bili & Ankara (Bills & Invoices)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Vikumbusho vya bili za nyumbani, bili zilizochelewa, na malipo ya ankara."
                enableVibration(true)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttrs)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(
                CHANNEL_TRANSACTIONS,
                "Miamala & Leja (Ledger Activity)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Taarifa za mfumo kwa mapato, matumizi, na miamala mipya iliyohifadhiwa."
                enableVibration(true)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttrs)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(
                CHANNEL_SAVINGS,
                "Akiba & Malengo (Savings Vault)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Taarifa za mfumo za kuweka akiba, kutoa akiba, na kufikia malengo ya akiba."
                enableVibration(true)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttrs)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(
                CHANNEL_SYSTEM_UPDATES,
                "Mfumo, Hifadhi & Masasisho (System & Updates)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Taarifa za usawazishaji wa hifadhidata kwenye simu na masasisho ya GitHub."
                enableVibration(true)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttrs)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )

            manager.createNotificationChannels(channels)
            channelsInitialized = true
        }
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

    fun isStorageManagerGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getPermissionsSnapshot(storageVaultReady: Boolean = true): PhonePermissionsSnapshot {
        return PhonePermissionsSnapshot(
            postNotificationsGranted = isPostNotificationsGranted(),
            systemNotificationsEnabled = areSystemNotificationsEnabled(),
            installPackagesGranted = canRequestPackageInstalls(),
            storageManagerGranted = isStorageManagerGranted(),
            storageVaultReady = storageVaultReady,
            channelsConfigured = 5
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

    fun buildManageStoragePermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${appContext.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            buildAppDetailsSettingsIntent()
        }
    }

    fun buildManageAllFilesFallbackIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            buildAppDetailsSettingsIntent()
        }
    }

    fun sendTestNotification(language: AppLanguage, businessName: String): Boolean {
        val title = tr(
            language,
            "Taarifa za $businessName Zimewashwa",
            "$businessName Notifications Active"
        )
        val body = tr(
            language,
            "Mfumo wa taarifa za Android (System Notifications) za Bili, Ankara, Miamala na Akiba unafanya kazi vizuri.",
            "Android system notifications for Bills, Invoices, Ledger, Savings, and Updates are active."
        )
        return postNotification(
            channelId = CHANNEL_SYSTEM_UPDATES,
            notificationId = NOTIF_ID_TEST,
            title = title,
            message = body,
            highPriority = true,
            smallIconRes = R.drawable.ic_notification_mengidata,
            accentColor = COLOR_BRAND_EMERALD
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
            message = body,
            highPriority = true,
            smallIconRes = R.drawable.ic_notification_mengidata,
            accentColor = COLOR_BRAND_EMERALD
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
            highPriority = true,
            smallIconRes = R.drawable.ic_notification_mengidata,
            accentColor = COLOR_BRAND_EMERALD
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
            message = body,
            highPriority = true,
            smallIconRes = R.drawable.ic_notification_mengidata,
            accentColor = COLOR_BRAND_EMERALD
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
            message = body,
            highPriority = true,
            smallIconRes = R.drawable.ic_notification_mengidata,
            accentColor = COLOR_BRAND_SLATE
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
            message = body,
            highPriority = true,
            smallIconRes = R.drawable.ic_notification_mengidata,
            accentColor = COLOR_BRAND_EMERALD
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
            highPriority = true,
            smallIconRes = R.drawable.ic_notification_mengidata,
            accentColor = COLOR_BRAND_EMERALD
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
            highPriority = true,
            smallIconRes = R.drawable.ic_notification_warning,
            accentColor = COLOR_WARNING_AMBER
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
    ): Boolean {
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
        return postNotification(
            channelId = CHANNEL_LARGE_BILL_WARNING,
            notificationId = (NOTIF_ID_LARGE_BILL_BASE + ((billName.hashCode() xor System.currentTimeMillis().toInt()) and 0x7FF)),
            title = title,
            message = body,
            highPriority = true,
            timeoutAfterMs = 3_000L,
            smallIconRes = R.drawable.ic_notification_warning,
            accentColor = COLOR_WARNING_AMBER
        )
    }

    private fun postNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        highPriority: Boolean = true,
        timeoutAfterMs: Long = 0L,
        @DrawableRes smallIconRes: Int = R.drawable.ic_notification_mengidata,
        accentColor: Int = COLOR_BRAND_EMERALD
    ): Boolean {
        if (!isPostNotificationsGranted() || !areSystemNotificationsEnabled()) {
            return false
        }
        ensureNotificationChannels()
        val sysManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val channel = sysManager?.getNotificationChannel(channelId)
        if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
            return false
        }

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
            .setSmallIcon(smallIconRes)
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setCategory(if (smallIconRes == R.drawable.ic_notification_warning) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        largeAppIconBitmap?.let { bmp ->
            builder.setLargeIcon(bmp)
        }

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
        const val CHANNEL_LARGE_BILL_WARNING = "mengidata_large_bill_warning_v2"
        const val CHANNEL_BILLS_INVOICES = "mengidata_bills_invoices_v2"
        const val CHANNEL_TRANSACTIONS = "mengidata_transactions_v2"
        const val CHANNEL_SAVINGS = "mengidata_savings_v2"
        const val CHANNEL_SYSTEM_UPDATES = "mengidata_system_updates_v2"

        private val LEGACY_CHANNELS = listOf(
            "mengidata_large_bill_warning",
            "mengidata_bills_invoices",
            "mengidata_transactions",
            "mengidata_savings",
            "mengidata_system_updates"
        )

        private const val COLOR_BRAND_EMERALD = 0xFF346352.toInt()
        private const val COLOR_BRAND_SLATE = 0xFF4E5975.toInt()
        private const val COLOR_WARNING_AMBER = 0xFF9A5B32.toInt()

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
