package tz.mengidata.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.TimeZone

/**
 * Schedules a daily evening (19:00 / 7:00 PM) Android AlarmManager reminder for:
 * 1. Pending ledger transactions (miamala inayosubiri)
 * 2. Unpaid bills (bili ambazo hazijalipwa)
 *
 * Uses Play-Policy-safe `setAndAllowWhileIdle` (no restricted SCHEDULE_EXACT_ALARM permission)
 * and reschedules itself daily inside `EveningReminderReceiver`.
 */
object EveningReminderScheduler {

    const val ACTION_DAILY_EVENING_REMINDER = "tz.mengidata.app.action.DAILY_EVENING_REMINDER"
    const val DEFAULT_EVENING_HOUR = 19 // 7:00 PM (Saa 1:00 Jioni)
    const val DEFAULT_EVENING_MINUTE = 0
    private const val REQUEST_CODE_EVENING_REMINDER = 4109

    fun computeNextEveningTriggerMillis(
        nowMillis: Long = System.currentTimeMillis(),
        hourOfDay: Int = DEFAULT_EVENING_HOUR,
        minute: Int = DEFAULT_EVENING_MINUTE,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If today's 19:00 has already passed (or is within 60s), schedule for tomorrow evening
        if (cal.timeInMillis <= nowMillis + 60_000L) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun scheduleDailyEveningReminder(context: Context) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(appCtx)
        val triggerAt = computeNextEveningTriggerMillis()
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    fun cancelDailyEveningReminder(context: Context) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        runCatching {
            alarmManager.cancel(buildPendingIntent(appCtx))
        }
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, EveningReminderReceiver::class.java).apply {
            action = ACTION_DAILY_EVENING_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EVENING_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
