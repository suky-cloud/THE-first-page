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
        for (String time : reminder.times) scheduleTime(context, reminder, time, nextTrigger(reminder, time).getTimeInMillis());
    }

    public static void scheduleAll(Context context) {
        for (Reminder reminder : ReminderStore.getAll(context)) schedule(context, reminder);
    }

    public static void scheduleNext(Context context, Reminder reminder, String time) {
        if (reminder.enabled && reminder.times.contains(time)) scheduleTime(context, reminder, time, nextTrigger(reminder, time).getTimeInMillis());
    }

    public static void snooze(Context context, Reminder reminder, String scheduledTime, int minutes) {
        scheduleTime(context, reminder, scheduledTime, System.currentTimeMillis() + minutes * 60_000L);
    }

    public static void cancel(Context context, Reminder reminder) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (String time : reminder.times) manager.cancel(alarmIntent(context, reminder.id, time));
    }

    private static void scheduleTime(Context context, Reminder reminder, String time, long triggerAt) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent operation = alarmIntent(context, reminder.id, time);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation);
        else manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation);
    }

    private static PendingIntent alarmIntent(Context context, String id, String time) {
        Intent intent = new Intent(context, AlarmReceiver.class).putExtra("reminder_id", id).putExtra("scheduled_time", time);
        return PendingIntent.getBroadcast(context, (id + "-" + time).hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static Calendar nextTrigger(Reminder reminder, String time) {
        String[] parts = time.split(":");
        Calendar now = Calendar.getInstance();
        Calendar candidate = Calendar.getInstance();
        candidate.set(Calendar.SECOND, 0); candidate.set(Calendar.MILLISECOND, 0);
        candidate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0])); candidate.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        for (int add = 0; add <= 7; add++) {
            Calendar test = (Calendar) candidate.clone(); test.add(Calendar.DAY_OF_YEAR, add);
            if (reminder.days.contains(test.get(Calendar.DAY_OF_WEEK)) && test.after(now)) return test;
        }
        candidate.add(Calendar.DAY_OF_YEAR, 7);
        return candidate;
    }

    public static Calendar nextOverall(Context context) {
        Calendar earliest = null;
        for (Reminder reminder : ReminderStore.getAll(context)) if (reminder.enabled) {
            for (String time : reminder.times) {
                Calendar candidate = nextTrigger(reminder, time);
                if (earliest == null || candidate.before(earliest)) earliest = candidate;
            }
        }
        return earliest;
    }
}
