package cloud.suky.medicinereminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public final class AlarmScheduler {
    private AlarmScheduler() { }

    public static void schedule(Context context, Reminder reminder) {
        cancel(context, reminder);
        if (!reminder.enabled) return;
        Calendar trigger = nextTrigger(reminder);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent operation = alarmIntent(context, reminder.id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.getTimeInMillis(), operation);
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.getTimeInMillis(), operation);
        }
    }

    public static void scheduleAll(Context context) {
        for (Reminder reminder : ReminderStore.getAll(context)) schedule(context, reminder);
    }

    public static void snooze(Context context, Reminder reminder, int minutes) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutes * 60_000L, alarmIntent(context, reminder.id));
    }

    public static void cancel(Context context, Reminder reminder) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(alarmIntent(context, reminder.id));
    }

    private static PendingIntent alarmIntent(Context context, String id) {
        Intent intent = new Intent(context, AlarmReceiver.class).putExtra("reminder_id", id);
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static Calendar nextTrigger(Reminder reminder) {
        Calendar now = Calendar.getInstance();
        Calendar candidate = Calendar.getInstance();
        candidate.set(Calendar.SECOND, 0); candidate.set(Calendar.MILLISECOND, 0);
        candidate.set(Calendar.HOUR_OF_DAY, reminder.hour); candidate.set(Calendar.MINUTE, reminder.minute);
        for (int add = 0; add <= 7; add++) {
            Calendar test = (Calendar) candidate.clone();
            test.add(Calendar.DAY_OF_YEAR, add);
            int javaDay = test.get(Calendar.DAY_OF_WEEK);
            if (reminder.days.contains(javaDay) && test.after(now)) return test;
        }
        candidate.add(Calendar.DAY_OF_YEAR, 1);
        return candidate;
    }
}
