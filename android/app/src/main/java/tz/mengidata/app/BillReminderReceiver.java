package tz.mengidata.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;

import tz.mengidata.app.Models.Bill;

final class BillReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "MengiBillReminder";
    private static final String CHANNEL_ID = "bill_reminders";
    private static final String EXTRA_BILL_ID = "bill_id";
    private static final String PREFS = "mengi_data_preferences";
    private static final String ENABLED = "bill_reminders_enabled";
    private static final String ALARM_IDS = "bill_reminder_ids";

    static void setEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(ENABLED, enabled).apply();
        if (enabled) scheduleAll(context);
        else cancelAll(context);
    }

    static boolean isEnabled(Context context) {
        return preferences(context).getBoolean(ENABLED, false);
    }

    static void scheduleAll(Context context) {
        createChannel(context);
        cancelAlarms(context, savedAlarmIds(context));
        if (!isEnabled(context) || !canNotify(context)) return;

        List<Integer> ids = new ArrayList<>();
        for (Bill bill : Db.get(context).bills()) {
            if (bill.paid) continue;
            Integer id = alarmId(bill.id);
            schedule(context, bill.id, dueTime(bill.dueDate));
            if (!ids.contains(id)) ids.add(id);
        }
        Set<String> stored = new HashSet<>();
        for (Integer id : ids) stored.add(String.valueOf(id));
        preferences(context).edit().putStringSet(ALARM_IDS, stored).apply();
    }

    private static long dueTime(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);
        try {
            Date parsed = format.parse(date);
            if (parsed == null) return System.currentTimeMillis() + 60_000L;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long due = calendar.getTimeInMillis();
            return due > System.currentTimeMillis() ? due : System.currentTimeMillis() + 60_000L;
        } catch (ParseException e) {
            Log.w(TAG, "Skipping bill reminder with invalid due date: " + date, e);
            return -1;
        }
    }

    private static void schedule(Context context, String billId, long triggerAt) {
        if (triggerAt < 0) return;
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        PendingIntent alarm = alarmIntent(context, billId);
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, alarm);
    }

    private static void cancelAll(Context context) {
        cancelAlarms(context, savedAlarmIds(context));
        preferences(context).edit().remove(ALARM_IDS).apply();
    }

    private static Set<String> savedAlarmIds(Context context) {
        return new HashSet<>(preferences(context).getStringSet(ALARM_IDS, new HashSet<>()));
    }

    private static void cancelAlarms(Context context, Set<String> ids) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        for (String value : ids) {
            try {
                int id = Integer.parseInt(value);
                PendingIntent alarm = PendingIntent.getBroadcast(context, id,
                        new Intent(context, BillReminderReceiver.class), pendingFlags());
                manager.cancel(alarm);
                alarm.cancel();
            } catch (NumberFormatException e) {
                Log.w(TAG, "Ignoring invalid stored reminder identifier", e);
            }
        }
    }

    private static PendingIntent alarmIntent(Context context, String billId) {
        Intent intent = new Intent(context, BillReminderReceiver.class);
        intent.putExtra(EXTRA_BILL_ID, billId);
        return PendingIntent.getBroadcast(context, alarmId(billId), intent, pendingFlags());
    }

    private static int pendingFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    }

    private static int alarmId(String billId) {
        return billId.hashCode() & 0x7fffffff;
    }

    private static boolean canNotify(Context context) {
        return Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                LocaleText.tr(context, "Vikumbusho vya bili"), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(LocaleText.tr(context, "Arifa za bili zinazofika siku ya malipo."));
        manager.createNotificationChannel(channel);
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            scheduleAll(context);
            return;
        }
        if (!isEnabled(context) || !canNotify(context)) return;
        String billId = intent.getStringExtra(EXTRA_BILL_ID);
        if (billId == null) return;
        Bill bill = Db.get(context).findBill(billId);
        if (bill == null || bill.paid) return;

        createChannel(context);
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap = PendingIntent.getActivity(context, 0, openApp, pendingFlags());
        String title = LocaleText.tr(context, "Kikumbusho cha bili");
        String message = LocaleText.tr(context, "Malipo yanatakiwa: ") + bill.name
                + " · TZS " + Util.money(bill.amount);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID) : new Notification.Builder(context);
        Notification notification = builder.setSmallIcon(R.drawable.ic_receipt)
                .setContentTitle(title).setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(tap).setAutoCancel(true).build();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(alarmId(bill.id), notification);

        schedule(context, bill.id, System.currentTimeMillis() + 24 * 60 * 60 * 1000L);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
