const STORAGE_KEY = "medicine-reminders-v0.1";
const HISTORY_KEY = "medicine-history-v0.3";
const SETTINGS_KEY = "medicine-settings-v0.3";
const EVERY_DAY = [0, 1, 2, 3, 4, 5, 6];
const DAY_NAMES = ["日", "一", "二", "三", "四", "五", "六"];

const form = document.querySelector("#reminder-form");
const list = document.querySelector("#reminder-list");
const emptyState = document.querySelector("#empty-state");
const count = document.querySelector("#reminder-count");
const message = document.querySelector("#form-message");
const dialog = document.querySelector("#reminder-dialog");
const notificationButton = document.querySelector("#notification-button");
const installButton = document.querySelector("#install-button");
let reminders = loadJson(STORAGE_KEY, []).map(migrateReminder);
let history = loadJson(HISTORY_KEY, []);
let settings = loadJson(SETTINGS_KEY, { privacyMode: "private" });
let activeAlert = null;
let deferredInstallPrompt = null;

function loadJson(key, fallback) {
  try { return JSON.parse(localStorage.getItem(key)) ?? fallback; } catch { return fallback; }
}

function migrateReminder(item) {
  return {
    ...item,
    times: Array.isArray(item.times) ? item.times : [item.time || "08:00"],
    days: Array.isArray(item.days) ? item.days : EVERY_DAY,
    enabled: item.enabled !== false,
    takenSlots: item.takenSlots || {}
  };
}

function saveAll() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(reminders));
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history.slice(0, 100)));
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
}

function todayKey(date = new Date()) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function scheduleLabel(days) {
  if (days.length === 7) return "每天";
  if (days.length === 5 && [1,2,3,4,5].every((day) => days.includes(day))) return "工作日";
  return days.map((day) => `周${DAY_NAMES[day]}`).join("、");
}

function isTaken(reminder, time) {
  return reminder.takenSlots[`${todayKey()}-${time}`] === true;
}

function render() {
  list.innerHTML = "";
  reminders.sort((a, b) => a.times[0].localeCompare(b.times[0])).forEach((reminder) => {
    const item = document.querySelector("#reminder-template").content.firstElementChild.cloneNode(true);
    item.classList.toggle("disabled", !reminder.enabled);
    item.querySelector(".time-badge").textContent = reminder.times.join(" · ");
    item.querySelector("h3").textContent = reminder.name;
    item.querySelector(".reminder-copy > p").textContent = reminder.note || "按计划服用";
    item.querySelector(".schedule-copy").textContent = scheduleLabel(reminder.days);
    const toggle = item.querySelector(".toggle input");
    toggle.checked = reminder.enabled;
    toggle.addEventListener("change", () => { reminder.enabled = toggle.checked; saveAll(); render(); });
    item.querySelector(".delete-button").addEventListener("click", () => {
      reminders = reminders.filter((entry) => entry.id !== reminder.id); saveAll(); render();
    });
    list.append(item);
  });
  emptyState.hidden = reminders.length > 0;
  count.textContent = `${reminders.filter((item) => item.enabled).length} 项启用`;
  renderHistory();
}

function renderHistory() {
  const historyList = document.querySelector("#history-list");
  const empty = document.querySelector("#history-empty");
  historyList.innerHTML = "";
  history.slice(0, 6).forEach((entry) => {
    const row = document.createElement("div");
    row.className = "history-row";
    row.innerHTML = `<span><strong></strong><small></small></span><em>已服用</em>`;
    row.querySelector("strong").textContent = entry.name;
    row.querySelector("small").textContent = new Date(entry.takenAt).toLocaleString("zh-CN", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" });
    historyList.append(row);
  });
  empty.hidden = history.length > 0;
}

document.querySelector("#add-time-button").addEventListener("click", () => {
  const inputs = document.querySelectorAll('input[name="times"]');
  if (inputs.length >= 4) { message.textContent = "每种药最多添加 4 个时间。"; return; }
  const input = document.createElement("input");
  input.type = "time"; input.name = "times"; input.required = true;
  document.querySelector("#time-fields").append(input);
});

form.addEventListener("submit", (event) => {
  event.preventDefault();
  const data = new FormData(form);
  const days = data.getAll("days").map(Number);
  if (!days.length) { message.textContent = "请至少选择一个重复日期。"; return; }
  const times = [...new Set(data.getAll("times"))].sort();
  reminders.push({
    id: crypto.randomUUID ? crypto.randomUUID() : String(Date.now()),
    name: data.get("medicine").trim(), note: data.get("note").trim(),
    times, days, enabled: true, takenSlots: {}, lastAlert: null, snoozeUntil: null
  });
  saveAll(); render(); form.reset();
  document.querySelector("#time-fields").innerHTML = '<input name="times" type="time" required>';
  document.querySelectorAll('input[name="days"]').forEach((input) => { input.checked = true; });
  message.textContent = "提醒已添加，并保存在当前设备中。";
});

notificationButton.addEventListener("click", async () => {
  if (!("Notification" in window)) { message.textContent = "当前浏览器不支持系统通知。"; return; }
  await Notification.requestPermission(); updatePermissionStatus();
});

document.querySelector("#privacy-mode").value = settings.privacyMode;
document.querySelector("#privacy-mode").addEventListener("change", (event) => {
  settings.privacyMode = event.target.value; saveAll(); message.textContent = "通知隐私设置已保存。";
});

document.querySelector("#clear-data-button").addEventListener("click", () => {
  if (!confirm("确定清除全部提醒、设置和服药历史吗？此操作无法撤销。")) return;
  reminders = []; history = []; settings = { privacyMode: "private" };
  localStorage.removeItem(STORAGE_KEY); localStorage.removeItem(HISTORY_KEY); localStorage.removeItem(SETTINGS_KEY);
  document.querySelector("#privacy-mode").value = "private"; render(); message.textContent = "本地数据已全部清除。";
});

document.querySelector("#test-button").addEventListener("click", () => showReminder({ id: null, name: "测试药品", note: "弹窗功能正常。" }, "现在"));

function updatePermissionStatus() {
  const status = !("Notification" in window) ? "不支持" : ({ granted: "已允许", denied: "已拒绝", default: "未选择" }[Notification.permission]);
  document.querySelector("#permission-status").textContent = status;
  notificationButton.textContent = status === "已允许" ? "系统通知已开启" : "开启系统通知";
}

function showReminder(reminder, time) {
  activeAlert = { reminder, time };
  document.querySelector("#dialog-title").textContent = `该服用 ${reminder.name} 了`;
  document.querySelector("#dialog-note").textContent = reminder.note || "请按医生或药品说明服用。";
  if (!dialog.open) dialog.showModal();
  if ("Notification" in window && Notification.permission === "granted") {
    const privateMode = settings.privacyMode === "private";
    new Notification(privateMode ? "到服药时间了" : `该服用 ${reminder.name} 了`, {
      body: privateMode ? "请打开应用查看详情。" : (reminder.note || "请按计划服药。")
    });
  }
}

document.querySelector("#taken-button").addEventListener("click", () => {
  if (activeAlert?.reminder?.id) {
    const slot = `${todayKey()}-${activeAlert.time}`;
    activeAlert.reminder.takenSlots[slot] = true;
    history.unshift({ name: activeAlert.reminder.name, time: activeAlert.time, takenAt: new Date().toISOString() });
    saveAll(); render();
  }
  dialog.close();
});

document.querySelector("#later-button").addEventListener("click", () => {
  if (activeAlert?.reminder?.id) activeAlert.reminder.snoozeUntil = Date.now() + 5 * 60 * 1000;
  saveAll(); dialog.close();
});

function checkReminders() {
  const now = new Date();
  const currentTime = now.toTimeString().slice(0, 5);
  const minuteKey = `${todayKey()}-${currentTime}`;
  reminders.forEach((reminder) => {
    if (!reminder.enabled || !reminder.days.includes(now.getDay())) return;
    const dueTime = reminder.times.find((time) => time === currentTime && !isTaken(reminder, time));
    const dueByTime = dueTime && reminder.lastAlert !== minuteKey;
    const dueBySnooze = reminder.snoozeUntil && Date.now() >= reminder.snoozeUntil;
    if (dueByTime || dueBySnooze) {
      reminder.lastAlert = minuteKey; reminder.snoozeUntil = null; saveAll();
      showReminder(reminder, dueTime || currentTime);
    }
  });
}

function updateClock() {
  const now = new Date();
  document.querySelector("#current-time").textContent = now.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false });
  document.querySelector("#current-date").textContent = now.toLocaleDateString("zh-CN", { month: "long", day: "numeric", weekday: "long" });
}

window.addEventListener("beforeinstallprompt", (event) => { event.preventDefault(); deferredInstallPrompt = event; installButton.hidden = false; });
installButton.addEventListener("click", async () => {
  if (!deferredInstallPrompt) { message.textContent = "请从浏览器菜单选择“安装应用”。"; return; }
  deferredInstallPrompt.prompt(); await deferredInstallPrompt.userChoice; deferredInstallPrompt = null; installButton.hidden = true;
});
window.addEventListener("appinstalled", () => { installButton.hidden = true; message.textContent = "安装成功。"; });

render(); updateClock(); updatePermissionStatus(); checkReminders();
setInterval(updateClock, 1000); setInterval(checkReminders, 10000);
if ("serviceWorker" in navigator) window.addEventListener("load", () => navigator.serviceWorker.register("./service-worker.js").catch(() => { message.textContent = "离线功能暂时无法启动。"; }));
