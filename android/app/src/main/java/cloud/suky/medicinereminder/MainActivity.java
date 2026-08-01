package cloud.suky.medicinereminder;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(55, 106, 87);
    private static final int INK = Color.rgb(24, 52, 42);
    private static final int MUTED = Color.rgb(104, 128, 118);
    private static final int CREAM = Color.rgb(247, 243, 233);
    private EditText nameInput;
    private EditText noteInput;
    private Button timeButton;
    private final int[] selectedTime = {8, 0};
    private final List<CheckBox> dayBoxes = new ArrayList<>();
    private LinearLayout reminderContainer;
    private LinearLayout historyContainer;
    private TextView permissionStatus;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AlarmReceiver.createChannel(this);
        setContentView(buildScreen());
        renderReminders();
        renderHistory();
    }

    @Override protected void onResume() {
        super.onResume();
        if (permissionStatus != null) updatePermissionStatus();
        renderHistory();
        AlarmScheduler.scheduleAll(this);
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(CREAM);
        LinearLayout root = column();
        root.setPadding(dp(20), dp(32), dp(20), dp(42));
        scroll.addView(root);

        root.addView(label("MEDICINE REMINDER · V0.4", 12, GREEN, true));
        TextView headline = label("好好爱自己，\n痛苦会过去。", 34, INK, true);
        headline.setLetterSpacing(-0.02f); root.addView(headline, marginBottom(8));
        root.addView(label("原生安卓测试版 · 即使关闭应用，系统仍会保留提醒", 14, MUTED, false), marginBottom(24));

        root.addView(buildPermissionCard(), marginBottom(16));
        root.addView(buildAddCard(), marginBottom(24));
        root.addView(sectionTitle("服药计划"));
        reminderContainer = column(); root.addView(reminderContainer, marginBottom(24));
        root.addView(sectionTitle("最近服药记录"));
        historyContainer = column(); root.addView(historyContainer);
        root.addView(label("数据仅保存在当前手机内部 · 不能替代医生建议", 12, MUTED, false), marginTop(28));
        return scroll;
    }

    private View buildPermissionCard() {
        LinearLayout card = card();
        card.addView(label("提醒权限", 19, INK, true), marginBottom(8));
        permissionStatus = label("正在检查…", 13, MUTED, false);
        card.addView(permissionStatus, marginBottom(12));
        Switch privacy = new Switch(this);
        privacy.setText("锁屏隐藏药名和备注");
        privacy.setTextColor(INK);
        privacy.setChecked(ReminderStore.privateNotifications(this));
        privacy.setOnCheckedChangeListener((button, checked) -> ReminderStore.setPrivateNotifications(this, checked));
        card.addView(privacy, marginBottom(12));
        LinearLayout actions = row();
        Button notifications = button("允许通知", false);
        notifications.setOnClickListener(v -> requestNotificationPermission());
        Button exact = button("允许准时提醒", true);
        exact.setOnClickListener(v -> requestExactAlarmPermission());
        actions.addView(notifications, weighted()); actions.addView(exact, weightedWithLeftMargin());
        card.addView(actions);
        return card;
    }

    private View buildAddCard() {
        LinearLayout card = card();
        card.addView(label("添加服药提醒", 20, INK, true), marginBottom(12));
        nameInput = input("药品名称，例如：维生素 C"); card.addView(nameInput, marginBottom(10));
        noteInput = input("备注，例如：饭后 1 粒"); card.addView(noteInput, marginBottom(10));
        timeButton = button("08:00", false);
        timeButton.setOnClickListener(v -> new TimePickerDialog(this, (picker, hour, minute) -> {
            selectedTime[0] = hour; selectedTime[1] = minute; updateTimeButton();
        }, selectedTime[0], selectedTime[1], true).show());
        card.addView(timeButton, marginBottom(12));
        card.addView(label("重复日期", 13, MUTED, true), marginBottom(6));
        LinearLayout days = row();
        String[] names = {"一", "二", "三", "四", "五", "六", "日"};
        int[] values = {2, 3, 4, 5, 6, 7, 1};
        for (int i = 0; i < names.length; i++) {
            CheckBox box = new CheckBox(this); box.setText(names[i]); box.setTag(values[i]); box.setChecked(true);
            box.setTextColor(INK); box.setButtonTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{GREEN, MUTED}));
            dayBoxes.add(box); days.addView(box, new LinearLayout.LayoutParams(0, dp(42), 1));
        }
        card.addView(days, marginBottom(12));
        Button add = button("保存提醒", true); add.setOnClickListener(v -> addReminder()); card.addView(add);
        return card;
    }

    private void addReminder() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) { nameInput.setError("请输入药品名称"); return; }
        Reminder reminder = new Reminder(); reminder.name = name; reminder.note = noteInput.getText().toString().trim();
        reminder.hour = selectedTime[0]; reminder.minute = selectedTime[1];
        for (CheckBox box : dayBoxes) if (box.isChecked()) reminder.days.add((Integer) box.getTag());
        if (reminder.days.isEmpty()) { Toast.makeText(this, "请至少选择一天", Toast.LENGTH_SHORT).show(); return; }
        List<Reminder> all = ReminderStore.getAll(this); all.add(reminder); ReminderStore.save(this, all);
        AlarmScheduler.schedule(this, reminder); nameInput.setText(""); noteInput.setText("");
        renderReminders(); Toast.makeText(this, "提醒已保存", Toast.LENGTH_SHORT).show();
    }

    private void renderReminders() {
        if (reminderContainer == null) return;
        reminderContainer.removeAllViews();
        List<Reminder> reminders = ReminderStore.getAll(this);
        if (reminders.isEmpty()) { reminderContainer.addView(label("还没有提醒，先添加第一条计划吧。", 14, MUTED, false), marginTop(8)); return; }
        for (Reminder reminder : reminders) reminderContainer.addView(reminderCard(reminder), marginTop(10));
    }

    private View reminderCard(Reminder reminder) {
        LinearLayout card = card();
        LinearLayout top = row();
        TextView time = label(String.format(Locale.CHINA, "%02d:%02d", reminder.hour, reminder.minute), 23, GREEN, true);
        TextView name = label(reminder.name, 17, INK, true);
        top.addView(time, new LinearLayout.LayoutParams(dp(88), LinearLayout.LayoutParams.WRAP_CONTENT));
        top.addView(name, weighted());
        Switch enabled = new Switch(this); enabled.setChecked(reminder.enabled); enabled.setContentDescription("启用提醒");
        enabled.setOnCheckedChangeListener((button, checked) -> {
            reminder.enabled = checked; updateReminder(reminder); if (checked) AlarmScheduler.schedule(this, reminder); else AlarmScheduler.cancel(this, reminder);
        });
        top.addView(enabled); card.addView(top);
        if (!reminder.note.isEmpty()) card.addView(label(reminder.note, 13, MUTED, false), marginTop(6));
        card.addView(label(dayText(reminder.days), 12, GREEN, false), marginTop(6));
        Button delete = button("删除", false); delete.setTextColor(Color.rgb(181, 84, 77));
        delete.setOnClickListener(v -> {
            AlarmScheduler.cancel(this, reminder); List<Reminder> all = ReminderStore.getAll(this);
            all.removeIf(item -> item.id.equals(reminder.id)); ReminderStore.save(this, all); renderReminders();
        });
        card.addView(delete, marginTop(8));
        return card;
    }

    private void updateReminder(Reminder changed) {
        List<Reminder> all = ReminderStore.getAll(this);
        for (int i = 0; i < all.size(); i++) if (all.get(i).id.equals(changed.id)) all.set(i, changed);
        ReminderStore.save(this, all);
    }

    private void renderHistory() {
        if (historyContainer == null) return;
        historyContainer.removeAllViews(); List<String> history = ReminderStore.getHistory(this);
        if (history.isEmpty()) { historyContainer.addView(label("还没有服药记录。", 14, MUTED, false)); return; }
        for (String entry : history) historyContainer.addView(label("✓  " + entry, 14, INK, false), marginTop(8));
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        else Toast.makeText(this, "通知权限已经开启", Toast.LENGTH_SHORT).show();
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!manager.canScheduleExactAlarms()) startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
            else Toast.makeText(this, "准时提醒权限已经开启", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePermissionStatus() {
        boolean notification = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        boolean exact = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms();
        permissionStatus.setText("通知：" + (notification ? "已允许" : "待允许") + "  ·  准时提醒：" + (exact ? "已允许" : "待允许"));
    }

    private String dayText(List<Integer> days) {
        if (days.size() == 7) return "每天";
        String[] names = {"", "日", "一", "二", "三", "四", "五", "六"}; StringBuilder result = new StringBuilder();
        for (int day : days) { if (result.length() > 0) result.append("、"); result.append("周").append(names[day]); }
        return result.toString();
    }

    private void updateTimeButton() { timeButton.setText(String.format(Locale.CHINA, "%02d:%02d", selectedTime[0], selectedTime[1])); }
    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private LinearLayout row() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.HORIZONTAL); view.setGravity(Gravity.CENTER_VERTICAL); return view; }
    private LinearLayout card() { LinearLayout view = column(); view.setPadding(dp(18), dp(18), dp(18), dp(18)); GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(255,254,250)); bg.setCornerRadius(dp(20)); bg.setStroke(dp(1), Color.rgb(219,228,222)); view.setBackground(bg); return view; }
    private TextView label(String text, int size, int color, boolean bold) { TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color); if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return view; }
    private TextView sectionTitle(String text) { return label(text, 21, INK, true); }
    private EditText input(String hint) { EditText view = new EditText(this); view.setHint(hint); view.setSingleLine(true); view.setTextColor(INK); view.setHintTextColor(MUTED); view.setBackgroundTintList(ColorStateList.valueOf(GREEN)); return view; }
    private Button button(String text, boolean primary) { Button view = new Button(this); view.setText(text); view.setAllCaps(false); view.setTextColor(primary ? Color.WHITE : GREEN); view.setBackgroundTintList(ColorStateList.valueOf(primary ? GREEN : Color.rgb(220,236,228))); return view; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout.LayoutParams weighted() { return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); }
    private LinearLayout.LayoutParams weightedWithLeftMargin() { LinearLayout.LayoutParams p = weighted(); p.setMargins(dp(8),0,0,0); return p; }
    private LinearLayout.LayoutParams marginTop(int value) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(value),0,0); return p; }
    private LinearLayout.LayoutParams marginBottom(int value) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,dp(value)); return p; }
}
