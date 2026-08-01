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
    public int hour = 8;
    public int minute = 0;
    public boolean enabled = true;
    public List<Integer> days = new ArrayList<>();

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id).put("name", name).put("note", note)
                .put("hour", hour).put("minute", minute).put("enabled", enabled);
            json.put("days", new JSONArray(days));
        } catch (Exception ignored) { }
        return json;
    }

    public static Reminder fromJson(JSONObject json) {
        Reminder reminder = new Reminder();
        reminder.id = json.optString("id", reminder.id);
        reminder.name = json.optString("name", "");
        reminder.note = json.optString("note", "");
        reminder.hour = json.optInt("hour", 8);
        reminder.minute = json.optInt("minute", 0);
        reminder.enabled = json.optBoolean("enabled", true);
        JSONArray array = json.optJSONArray("days");
        if (array != null) for (int i = 0; i < array.length(); i++) reminder.days.add(array.optInt(i));
        if (reminder.days.isEmpty()) for (int i = 1; i <= 7; i++) reminder.days.add(i);
        return reminder;
    }
}
