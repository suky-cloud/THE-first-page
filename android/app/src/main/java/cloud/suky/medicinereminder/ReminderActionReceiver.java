package cloud.suky.medicinereminder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderActionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Reminder reminder = ReminderStore.find(context, intent.getStringExtra("reminder_id"));
        if (reminder == null) return;
        if ("TAKEN".equals(intent.getAction())) ReminderStore.addHistory(context, reminder);
        if ("SNOOZE".equals(intent.getAction())) AlarmScheduler.snooze(context, reminder, 5);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(reminder.id.hashCode());
    }
}
