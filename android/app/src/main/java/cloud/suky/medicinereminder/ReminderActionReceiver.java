package cloud.suky.medicinereminder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderActionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Reminder reminder = ReminderStore.find(context, intent.getStringExtra("reminder_id"));
        if (reminder == null) return;
        String scheduledTime = intent.getStringExtra("scheduled_time");
        if (scheduledTime == null) scheduledTime = reminder.times.get(0);
        if ("TAKEN".equals(intent.getAction())) ReminderStore.addHistory(context, reminder, "已服用", scheduledTime);
        if ("SNOOZE".equals(intent.getAction())) {
            ReminderStore.addHistory(context, reminder, "已延后", scheduledTime);
            AlarmScheduler.snooze(context, reminder, scheduledTime, 5);
        }
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(AlarmReceiver.notificationId(reminder.id, scheduledTime));
    }
}
