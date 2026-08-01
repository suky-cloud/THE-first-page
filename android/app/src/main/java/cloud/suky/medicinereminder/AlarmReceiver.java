package cloud.suky.medicinereminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "medicine_reminders";

    @Override public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra("reminder_id");
        String scheduledTime = intent.getStringExtra("scheduled_time");
        Reminder reminder = ReminderStore.find(context, id);
        if (reminder == null || !reminder.enabled) return;
        if (scheduledTime == null) scheduledTime = reminder.times.get(0);
        createChannel(context);
        boolean privateMode = ReminderStore.privateNotifications(context);

        PendingIntent taken = action(context, reminder, scheduledTime, "TAKEN", 1);
        PendingIntent snooze = action(context, reminder, scheduledTime, "SNOOZE", 2);
        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(context, reminder.id.hashCode(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, CHANNEL_ID) : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_pill)
            .setContentTitle(privateMode ? "到服药时间了" : "该服用 " + reminder.name + " 了")
            .setContentText(privateMode ? "请打开应用查看详情" : (reminder.note.isEmpty() ? "请按计划服药" : reminder.note))
            .setContentIntent(open).setAutoCancel(true).setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(privateMode ? Notification.VISIBILITY_SECRET : Notification.VISIBILITY_PRIVATE)
            .addAction(new Notification.Action.Builder(R.drawable.ic_pill, "我已服用", taken).build())
            .addAction(new Notification.Action.Builder(R.drawable.ic_pill, "5 分钟后", snooze).build());
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(notificationId(reminder.id, scheduledTime), builder.build());
        AlarmScheduler.scheduleNext(context, reminder, scheduledTime);
    }

    private PendingIntent action(Context context, Reminder reminder, String scheduledTime, String action, int suffix) {
        Intent intent = new Intent(context, ReminderActionReceiver.class).setAction(action)
            .putExtra("reminder_id", reminder.id).putExtra("scheduled_time", scheduledTime);
        return PendingIntent.getBroadcast(context, (reminder.id + scheduledTime).hashCode() + suffix, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static int notificationId(String id, String time) { return (id + "-" + time).hashCode(); }

    public static void showTestNotification(Context context) {
        createChannel(context);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, CHANNEL_ID) : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_pill).setContentTitle("测试通知成功")
            .setContentText("安卓系统可以显示吃药提醒。").setAutoCancel(true)
            .setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PRIVATE);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(504, builder.build());
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "服药提醒", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("按设定时间显示服药通知"); channel.enableVibration(true); channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
    }
}
