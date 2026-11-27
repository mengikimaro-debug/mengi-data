package tz.mengidata.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tz.mengidata.app.data.db.BusinessSettingsEntity
import tz.mengidata.app.data.db.MengiDatabase
import tz.mengidata.app.data.prefs.AppPreferences

/**
 * Fires every evening at 19:00 (and on device reboot) to:
 * 1. Keep the daily evening schedule armed.
 * 2. Remind the user about any pending ledger transactions (with their details/maelezo).
 * 3. Remind the user about all unpaid bills (bili ambazo hazijalipwa).
 */
class EveningReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appCtx = context.applicationContext

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            EveningReminderScheduler.scheduleDailyEveningReminder(appCtx)
            return
        }

        if (action != EveningReminderScheduler.ACTION_DAILY_EVENING_REMINDER) return

        // Always arm tomorrow evening's reminder immediately
        EveningReminderScheduler.scheduleDailyEveningReminder(appCtx)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = AppPreferences(appCtx)
                val db = MengiDatabase.getInstance(appCtx)
                val notifier = MengiNotificationManager(appCtx)
                notifier.ensureNotificationChannels()

                val settings = runCatching { db.settingsDao().getSettings() }.getOrNull()
                    ?: BusinessSettingsEntity()
                val currency = settings.currency.ifBlank { "TZS" }
                val language = prefs.language.value

                val pendingTxns = runCatching { db.pendingTransactionDao().getAll() }
                    .getOrDefault(emptyList())
                if (pendingTxns.isNotEmpty()) {
                    notifier.notifyPendingTransactionsReminder(
                        pendingItems = pendingTxns,
                        currency = currency,
                        language = language
                    )
                }

                val bills = runCatching { db.billDao().getAll() }
                    .getOrDefault(emptyList())
                if (bills.any { !it.paid }) {
                    notifier.notifyUnpaidBillsEveningReminder(
                        bills = bills,
                        currency = currency,
                        language = language
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
