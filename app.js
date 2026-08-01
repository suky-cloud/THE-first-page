const STORAGE_KEY = "medicine-reminders-v0.1";
const form = document.querySelector("#reminder-form");
const list = document.querySelector("#reminder-list");
const emptyState = document.querySelector("#empty-state");
const count = document.querySelector("#reminder-count");
const message = document.querySelector("#form-message");
const dialog = document.querySelector("#reminder-dialog");
const notificationButton = document.querySelector("#notification-button");
const installButton = document.querySelector("#install-button");
let activeReminderId = null;
let reminders = loadReminders();
let deferredInstallPrompt = null;

function loadReminders() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) || [];
  } catch {
    return [];
  }
}

function saveReminders() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(reminders));
}

function todayKey() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function render() {
  list.innerHTML = "";
  reminders.sort((a, b) => a.time.localeCompare(b.time)).forEach((reminder) => {
    const item = document.querySelector("#reminder-template").content.firstElementChild.cloneNode(true);
    const taken = reminder.takenOn === todayKey();
    item.dataset.id = reminder.id;
    item.classList.toggle("taken", taken);
    item.querySelector(".time-badge").textContent = reminder.time;
    item.querySelector("h3").textContent = reminder.name;
    item.querySelector(".reminder-copy p").textContent = reminder.note || "按计划服用";
    item.querySelector(".status-badge").textContent = taken ? "今日已服用" : "待服用";
    item.querySelector(".delete-button").addEventListener("click", () => deleteReminder(reminder.id));
    list.append(item);
  });
  emptyState.hidden = reminders.length > 0;
  count.textContent = `${reminders.length} 项`;
}

function deleteReminder(id) {
  reminders = reminders.filter((item) => item.id !== id);
  saveReminders();
  render();
}

form.addEventListener("submit", (event) => {
  event.preventDefault();
  const data = new FormData(form);
  reminders.push({
    id: crypto.randomUUID ? crypto.randomUUID() : String(Date.now()),
    name: data.get("medicine").trim(),
    time: data.get("time"),
    note: data.get("note").trim(),
    takenOn: null,
    lastAlert: null,
    snoozeUntil: null
  });
  saveReminders();
  render();
  form.reset();
  message.textContent = "提醒已添加，并保存在这个浏览器中。";
  setTimeout(() => { message.textContent = ""; }, 3000);
});

notificationButton.addEventListener("click", async () => {
  if (!("Notification" in window)) {
    message.textContent = "当前浏览器不支持系统通知，但页面弹窗仍可使用。";
    return;
  }
  const permission = await Notification.requestPermission();
  message.textContent = permission === "granted" ? "系统通知已开启。" : "未开启通知，仍会使用页面弹窗提醒。";
  updateNotificationButton();
});

document.querySelector("#test-button").addEventListener("click", () => {
  showReminder({ id: null, name: "测试药品", note: "弹窗功能正常。" });
});

window.addEventListener("beforeinstallprompt", (event) => {
  event.preventDefault();
  deferredInstallPrompt = event;
  installButton.hidden = false;
});

installButton.addEventListener("click", async () => {
  if (!deferredInstallPrompt) {
    message.textContent = "请在浏览器菜单中选择“安装应用”或“添加到主屏幕”。";
    return;
  }
  deferredInstallPrompt.prompt();
  await deferredInstallPrompt.userChoice;
  deferredInstallPrompt = null;
  installButton.hidden = true;
});

window.addEventListener("appinstalled", () => {
  message.textContent = "安装成功，可以从手机桌面打开。";
  installButton.hidden = true;
});

function updateNotificationButton() {
  notificationButton.textContent = "Notification" in window && Notification.permission === "granted"
    ? "系统通知已开启"
    : "开启系统通知";
}

function showReminder(reminder) {
  activeReminderId = reminder.id;
  document.querySelector("#dialog-title").textContent = `该服用 ${reminder.name} 了`;
  document.querySelector("#dialog-note").textContent = reminder.note || "请按医生或药品说明服用。";
  if (!dialog.open) dialog.showModal();
  if ("Notification" in window && Notification.permission === "granted") {
    new Notification(`该服用 ${reminder.name} 了`, { body: reminder.note || "请按计划服药。" });
  }
}

document.querySelector("#taken-button").addEventListener("click", () => {
  const reminder = reminders.find((item) => item.id === activeReminderId);
  if (reminder) {
    reminder.takenOn = todayKey();
    reminder.snoozeUntil = null;
    saveReminders();
    render();
  }
  dialog.close();
});

document.querySelector("#later-button").addEventListener("click", () => {
  const reminder = reminders.find((item) => item.id === activeReminderId);
  if (reminder) {
    reminder.snoozeUntil = Date.now() + 5 * 60 * 1000;
    saveReminders();
  }
  dialog.close();
});

function checkReminders() {
  const now = new Date();
  const currentTime = now.toTimeString().slice(0, 5);
  const minuteKey = `${todayKey()}-${currentTime}`;
  reminders.forEach((reminder) => {
    const dueByTime = reminder.time === currentTime && reminder.lastAlert !== minuteKey;
    const dueBySnooze = reminder.snoozeUntil && Date.now() >= reminder.snoozeUntil;
    if (reminder.takenOn !== todayKey() && (dueByTime || dueBySnooze)) {
      reminder.lastAlert = minuteKey;
      reminder.snoozeUntil = null;
      saveReminders();
      showReminder(reminder);
    }
  });
}

function updateClock() {
  const now = new Date();
  document.querySelector("#current-time").textContent = now.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false });
  document.querySelector("#current-date").textContent = now.toLocaleDateString("zh-CN", { month: "long", day: "numeric", weekday: "long" });
}

render();
updateClock();
updateNotificationButton();
checkReminders();
setInterval(updateClock, 1000);
setInterval(checkReminders, 10000);

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("./service-worker.js").catch(() => {
      message.textContent = "离线功能暂时未能启动，在线使用不受影响。";
    });
  });
}
