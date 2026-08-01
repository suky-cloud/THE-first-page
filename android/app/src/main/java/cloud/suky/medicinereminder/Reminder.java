package cloud.suky.medicinereminder;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Reminder {
    public String id = UUID.randomUUID().toString();
    public String name = "";
    public String note = "";
    public List<String> times = new ArrayList<>();
    public boolean enabled = true;
    public List<Integer> days = new ArrayList<>();

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id).put("name", name).put("note", note).put("enabled", enabled);
            json.put("days", new JSONArray(days));
            json.put("times", new JSONArray(times));
        } catch (Exception ignored) { }
        return json;
    }

    public static Reminder fromJson(JSONObject json) {
        Reminder reminder = new Reminder();
        reminder.id = json.optString("id", reminder.id);
        reminder.name = json.optString("name", "");
        reminder.note = json.optString("note", "");
        reminder.enabled = json.optBoolean("enabled", true);
        JSONArray times = json.optJSONArray("times");
        if (times != null) for (int i = 0; i < times.length(); i++) reminder.times.add(times.optString(i));
        if (reminder.times.isEmpty()) reminder.times.add(String.format(java.util.Locale.ROOT, "%02d:%02d", json.optInt("hour", 8), json.optInt("minute", 0)));
        JSONArray array = json.optJSONArray("days");
        if (array != null) for (int i = 0; i < array.length(); i++) reminder.days.add(array.optInt(i));
        if (reminder.days.isEmpty()) for (int i = 1; i <= 7; i++) reminder.days.add(i);
        return reminder;
    }
}
