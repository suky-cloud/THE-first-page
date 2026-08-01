package cloud.suky.medicinereminder;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ReminderStore {
    private static final String PREFS = "medicine_reminder_private_data";
    private static final String REMINDERS = "reminders";
    private static final String HISTORY = "history";
    private static final String PRIVACY = "private_notifications";

    private ReminderStore() { }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<Reminder> getAll(Context context) {
        List<Reminder> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs(context).getString(REMINDERS, "[]"));
            for (int i = 0; i < array.length(); i++) result.add(Reminder.fromJson(array.getJSONObject(i)));
        } catch (Exception ignored) { }
        return result;
    }

    public static Reminder find(Context context, String id) {
        for (Reminder reminder : getAll(context)) if (reminder.id.equals(id)) return reminder;
        return null;
    }

    public static void save(Context context, List<Reminder> reminders) {
        JSONArray array = new JSONArray();
        for (Reminder reminder : reminders) array.put(reminder.toJson());
        prefs(context).edit().putString(REMINDERS, array.toString()).apply();
    }

    public static void addHistory(Context context, Reminder reminder, String status, String scheduledTime) {
        try {
            JSONArray old = new JSONArray(prefs(context).getString(HISTORY, "[]"));
            JSONArray next = new JSONArray();
            next.put(status + " · " + reminder.name + " · 计划 " + scheduledTime + " · " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("M月d日 HH:mm")));
            for (int i = 0; i < Math.min(old.length(), 19); i++) next.put(old.getString(i));
            prefs(context).edit().putString(HISTORY, next.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static List<String> getHistory(Context context) {
        List<String> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs(context).getString(HISTORY, "[]"));
            for (int i = 0; i < array.length(); i++) result.add(array.getString(i));
        } catch (Exception ignored) { }
        return result;
    }

    public static boolean privateNotifications(Context context) { return prefs(context).getBoolean(PRIVACY, true); }
    public static void setPrivateNotifications(Context context, boolean value) { prefs(context).edit().putBoolean(PRIVACY, value).apply(); }
}
