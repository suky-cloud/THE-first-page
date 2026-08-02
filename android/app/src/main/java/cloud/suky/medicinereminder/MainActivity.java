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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(55, 106, 87);
    private static final int INK = Color.rgb(24, 52, 42);
    private static final int MUTED = Color.rgb(104, 128, 118);
    private static final int CREAM = Color.rgb(247, 243, 233);
    private EditText nameInput;
    private EditText noteInput;
    private LinearLayout timesContainer;
    private final List<String> selectedTimes = new ArrayList<>();
    private final List<CheckBox> dayBoxes = new ArrayList<>();
    private LinearLayout reminderContainer;
    private LinearLayout historyContainer;
    private TextView permissionStatus;
    private TextView nextReminderText;
    private Button saveButton;
    private Button cancelEditButton;
    private String editingId;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AlarmReceiver.createChannel(this);
        selectedTimes.add("08:00");
        setContentView(buildScreen());
        renderTimes(); renderReminders(); renderHistory();
    }

    @Override protected void onResume() {
        super.onResume();
        if (permissionStatus != null) updatePermissionStatus();
        renderHistory(); renderNextReminder(); AlarmScheduler.scheduleAll(this);
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(CREAM);
        LinearLayout root = column(); root.setPadding(dp(20), dp(32), dp(20), dp(42)); scroll.addView(root);
        root.addView(label("MEDICINE REMINDER · V0.5", 12, GREEN, true));
        root.addView(label("好好爱自己，\n痛苦会过去。", 34, INK, true), marginBottom(8));
        root.addView(label("可靠提醒测试版 · 多时间、可编辑、可诊断", 14, MUTED, false), marginBottom(20));
        nextReminderText = label("下一次提醒：尚未设置", 15, GREEN, true);
        LinearLayout nextCard = card(); nextCard.addView(nextReminderText); root.addView(nextCard, marginBottom(16));
        root.addView(buildPermissionCard(), marginBottom(16));
        root.addView(buildAddCard(), marginBottom(24));
        root.addView(buildAboutCard(), marginBottom(24));
        root.addView(sectionTitle("服药计划")); reminderContainer = column(); root.addView(reminderContainer, marginBottom(24));
        root.addView(sectionTitle("最近操作记录")); historyContainer = column(); root.addView(historyContainer);
        root.addView(label("数据仅保存在当前手机内部 · 不能替代医生建议", 12, MUTED, false), marginTop(28));
        return scroll;
    }

    private View buildAboutCard() {
        LinearLayout card = card();
        card.addView(label("关于应用", 19, INK, true), marginBottom(6));
        card.addView(label("当前版本：v" + BuildConfig.VERSION_NAME, 14, MUTED, false), marginBottom(10));
        Button updates = button("检查更新", false);
        updates.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://github.com/suky-cloud/THE-first-page/releases"))));
        card.addView(updates);
        return card;
    }

    private View buildPermissionCard() {
        LinearLayout card = card(); card.addView(label("权限诊断", 19, INK, true), marginBottom(8));
        permissionStatus = label("正在检查…", 13, MUTED, false); card.addView(permissionStatus, marginBottom(10));
        Switch privacy = new Switch(this); privacy.setText("锁屏隐藏药名和备注"); privacy.setTextColor(INK);
        privacy.setChecked(ReminderStore.privateNotifications(this));
        privacy.setOnCheckedChangeListener((button, checked) -> ReminderStore.setPrivateNotifications(this, checked));
        card.addView(privacy, marginBottom(10));
        LinearLayout first = row();
        Button notifications = button("允许通知", false); notifications.setOnClickListener(v -> requestNotificationPermission());
        Button exact = button("允许准时提醒", true); exact.setOnClickListener(v -> requestExactAlarmPermission());
        first.addView(notifications, weighted()); first.addView(exact, weightedWithLeftMargin()); card.addView(first, marginBottom(8));
        Button test = button("发送测试通知", false); test.setOnClickListener(v -> testNotification()); card.addView(test);
        return card;
    }

    private View buildAddCard() {
        LinearLayout card = card(); card.addView(label("添加或编辑提醒", 20, INK, true), marginBottom(12));
        nameInput = input("药品名称，例如：维生素 C"); card.addView(nameInput, marginBottom(10));
        noteInput = input("备注，例如：饭后 1 粒"); card.addView(noteInput, marginBottom(10));
        card.addView(label("提醒时间（可添加多个）", 13, MUTED, true), marginBottom(6));
        timesContainer = column(); card.addView(timesContainer);
        Button addTime = button("＋ 添加时间", false); addTime.setOnClickListener(v -> openTimePicker()); card.addView(addTime, marginBottom(12));
        card.addView(label("重复日期", 13, MUTED, true), marginBottom(6));
        LinearLayout days = row(); String[] names = {"一", "二", "三", "四", "五", "六", "日"}; int[] values = {2,3,4,5,6,7,1};
        for (int i = 0; i < names.length; i++) {
            CheckBox box = new CheckBox(this); box.setText(names[i]); box.setTag(values[i]); box.setChecked(true); box.setTextColor(INK);
            box.setButtonTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{GREEN, MUTED}));
            dayBoxes.add(box); days.addView(box, new LinearLayout.LayoutParams(0, dp(42), 1));
        }
        card.addView(days, marginBottom(12));
        saveButton = button("保存提醒", true); saveButton.setOnClickListener(v -> saveReminder()); card.addView(saveButton);
        cancelEditButton = button("取消编辑", false); cancelEditButton.setVisibility(View.GONE); cancelEditButton.setOnClickListener(v -> resetForm());
        card.addView(cancelEditButton, marginTop(8));
        return card;
    }

    private void openTimePicker() {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this, (picker, hour, minute) -> {
            String time = String.format(Locale.ROOT, "%02d:%02d", hour, minute);
            if (!selectedTimes.contains(time)) selectedTimes.add(time);
            Collections.sort(selectedTimes); renderTimes();
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
    }

    private void renderTimes() {
        if (timesContainer == null) return; timesContainer.removeAllViews();
        for (String time : new ArrayList<>(selectedTimes)) {
            Button chip = button(time + (selectedTimes.size() > 1 ? "  ×" : ""), false);
            chip.setOnClickListener(v -> { if (selectedTimes.size() > 1) { selectedTimes.remove(time); renderTimes(); } });
            timesContainer.addView(chip, marginBottom(6));
        }
    }

    private void saveReminder() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) { nameInput.setError("请输入药品名称"); return; }
        List<Integer> days = new ArrayList<>(); for (CheckBox box : dayBoxes) if (box.isChecked()) days.add((Integer) box.getTag());
        if (days.isEmpty()) { Toast.makeText(this, "请至少选择一天", Toast.LENGTH_SHORT).show(); return; }
        boolean wasEditing = editingId != null;
        List<Reminder> all = ReminderStore.getAll(this); Reminder target = null;
        if (editingId != null) for (Reminder item : all) if (item.id.equals(editingId)) { target = item; AlarmScheduler.cancel(this, item); break; }
        if (target == null) { target = new Reminder(); all.add(target); }
        target.name = name; target.note = noteInput.getText().toString().trim(); target.days = days;
        target.times = new ArrayList<>(selectedTimes); target.enabled = true;
        ReminderStore.save(this, all); AlarmScheduler.schedule(this, target); resetForm(); renderReminders(); renderNextReminder();
        Toast.makeText(this, wasEditing ? "提醒已更新" : "提醒已保存", Toast.LENGTH_SHORT).show();
    }

    private void editReminder(Reminder reminder) {
        editingId = reminder.id; nameInput.setText(reminder.name); noteInput.setText(reminder.note);
        selectedTimes.clear(); selectedTimes.addAll(reminder.times); renderTimes();
        for (CheckBox box : dayBoxes) box.setChecked(reminder.days.contains((Integer) box.getTag()));
        saveButton.setText("更新提醒"); cancelEditButton.setVisibility(View.VISIBLE); nameInput.requestFocus();
    }

    private void resetForm() {
        editingId = null; nameInput.setText(""); noteInput.setText(""); selectedTimes.clear(); selectedTimes.add("08:00"); renderTimes();
        for (CheckBox box : dayBoxes) box.setChecked(true); saveButton.setText("保存提醒"); cancelEditButton.setVisibility(View.GONE);
    }

    private void renderReminders() {
        if (reminderContainer == null) return; reminderContainer.removeAllViews(); List<Reminder> reminders = ReminderStore.getAll(this);
        if (reminders.isEmpty()) { reminderContainer.addView(label("还没有提醒，先添加第一条计划吧。", 14, MUTED, false), marginTop(8)); return; }
        for (Reminder reminder : reminders) reminderContainer.addView(reminderCard(reminder), marginTop(10));
    }

    private View reminderCard(Reminder reminder) {
        LinearLayout card = card(); LinearLayout top = row();
        TextView times = label(String.join(" · ", reminder.times), 18, GREEN, true); TextView name = label(reminder.name, 17, INK, true);
        top.addView(times, new LinearLayout.LayoutParams(dp(142), -2)); top.addView(name, weighted());
        Switch enabled = new Switch(this); enabled.setChecked(reminder.enabled); enabled.setContentDescription("启用提醒");
        enabled.setOnCheckedChangeListener((button, checked) -> { reminder.enabled = checked; updateReminder(reminder); if (checked) AlarmScheduler.schedule(this, reminder); else AlarmScheduler.cancel(this, reminder); renderNextReminder(); });
        top.addView(enabled); card.addView(top);
        if (!reminder.note.isEmpty()) card.addView(label(reminder.note, 13, MUTED, false), marginTop(6));
        card.addView(label(dayText(reminder.days), 12, GREEN, false), marginTop(6));
        LinearLayout actions = row(); Button edit = button("编辑", false); edit.setOnClickListener(v -> editReminder(reminder));
        Button delete = button("删除", false); delete.setTextColor(Color.rgb(181,84,77));
        delete.setOnClickListener(v -> { AlarmScheduler.cancel(this, reminder); List<Reminder> all = ReminderStore.getAll(this); all.removeIf(item -> item.id.equals(reminder.id)); ReminderStore.save(this, all); renderReminders(); renderNextReminder(); });
        actions.addView(edit, weighted()); actions.addView(delete, weightedWithLeftMargin()); card.addView(actions, marginTop(8)); return card;
    }

    private void updateReminder(Reminder changed) {
        List<Reminder> all = ReminderStore.getAll(this); for (int i = 0; i < all.size(); i++) if (all.get(i).id.equals(changed.id)) all.set(i, changed); ReminderStore.save(this, all);
    }

    private void renderNextReminder() {
        if (nextReminderText == null) return; Calendar next = AlarmScheduler.nextOverall(this);
        nextReminderText.setText(next == null ? "下一次提醒：尚未设置" : "下一次提醒：" + new SimpleDateFormat("M月d日 E HH:mm", Locale.CHINA).format(next.getTime()));
    }

    private void renderHistory() {
        if (historyContainer == null) return; historyContainer.removeAllViews(); List<String> history = ReminderStore.getHistory(this);
        if (history.isEmpty()) { historyContainer.addView(label("还没有服药或延后记录。", 14, MUTED, false)); return; }
        for (String entry : history) historyContainer.addView(label(entry, 14, INK, false), marginTop(8));
    }

    private void testNotification() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请先允许通知权限", Toast.LENGTH_SHORT).show(); requestNotificationPermission(); return;
        }
        AlarmReceiver.showTestNotification(this); Toast.makeText(this, "测试通知已发送", Toast.LENGTH_SHORT).show();
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
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE); boolean exact = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms();
        permissionStatus.setText("通知：" + (notification ? "✓ 已允许" : "✕ 待允许") + "\n准时提醒：" + (exact ? "✓ 已允许" : "✕ 待允许") + (notification && exact ? "\n状态正常，可以测试通知。" : "\n请开启待允许的权限。"));
        permissionStatus.setTextColor(notification && exact ? GREEN : Color.rgb(181,84,77));
    }

    private String dayText(List<Integer> days) {
        if (days.size() == 7) return "每天"; String[] names = {"", "日", "一", "二", "三", "四", "五", "六"}; StringBuilder result = new StringBuilder();
        for (int day : days) { if (result.length() > 0) result.append("、"); result.append("周").append(names[day]); } return result.toString();
    }

    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private LinearLayout row() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.HORIZONTAL); view.setGravity(Gravity.CENTER_VERTICAL); return view; }
    private LinearLayout card() { LinearLayout view = column(); view.setPadding(dp(18),dp(18),dp(18),dp(18)); GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(255,254,250)); bg.setCornerRadius(dp(20)); bg.setStroke(dp(1),Color.rgb(219,228,222)); view.setBackground(bg); return view; }
    private TextView label(String text, int size, int color, boolean bold) { TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color); if (bold) view.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return view; }
    private TextView sectionTitle(String text) { return label(text,21,INK,true); }
    private EditText input(String hint) { EditText view = new EditText(this); view.setHint(hint); view.setSingleLine(true); view.setTextColor(INK); view.setHintTextColor(MUTED); view.setBackgroundTintList(ColorStateList.valueOf(GREEN)); return view; }
    private Button button(String text, boolean primary) { Button view = new Button(this); view.setText(text); view.setAllCaps(false); view.setTextColor(primary ? Color.WHITE : GREEN); view.setBackgroundTintList(ColorStateList.valueOf(primary ? GREEN : Color.rgb(220,236,228))); return view; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout.LayoutParams weighted() { return new LinearLayout.LayoutParams(0,-2,1); }
    private LinearLayout.LayoutParams weightedWithLeftMargin() { LinearLayout.LayoutParams p = weighted(); p.setMargins(dp(8),0,0,0); return p; }
    private LinearLayout.LayoutParams marginTop(int value) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(value),0,0); return p; }
    private LinearLayout.LayoutParams marginBottom(int value) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,dp(value)); return p; }
}
