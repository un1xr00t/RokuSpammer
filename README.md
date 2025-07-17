# 📺 RokuSpammer

An Android app for chaos-mode testing of Roku TVs (and now more brands), built with 🔥 Kotlin + Android Studio.

It sends remote keypresses over the local network — either manually or automatically in "Obliterate" mode — for **educational testing purposes only**.

---

## 🚀 What’s New in v2.1.0

- 🔍 **Multi-TV discovery via SSDP** — detects Roku, LG, Samsung, and other smart TVs
- 🌐 **Unified device list** — combines subnet and SSDP scan results
- 🛰️ **"All Devices (broadcast)" option** appears when multiple TVs are detected
- ⚠️ **Only Roku devices can be attacked (for now)** — others are shown but disabled
- 🧠 **Auto-fetches Roku device names** over port 8060
- ⛓️ **Improved scanning + coroutine engine** for smoother performance
- 💾 **Remembers your last-used IP address**

---

## 🎮 Features

- 🔁 **Obliterate Mode** (formerly Chaos) — rapid-fire command sequence to Roku devices
- 🧠 **Command Spinner** — choose a specific keypress like `Home`, `VolumeUp`, etc.
- 📡 **Smart TV Scanner**
  - Manual IP entry ✅
  - Subnet-based Roku scan ✅
  - SSDP smart TV scan (Roku, LG, Samsung, etc.) ✅
- 🛠️ **Delay Control** — throttle command speed (ms)
- 📜 **Real-time logs** — color-coded: green = success, yellow = warning, red = error
- 🖤 **Custom UI** — neon green glow, hacker-themed layout, centered fonts, dark style

---

## 📦 Download

👉 [**Get the Latest APK (v2.1.0)**](https://github.com/un1xr00t/RokuSpammer/releases) (**COMING SOON**)

> Sideload required — allow unknown apps in Android settings to install.

---

## 🛠 Setup

1. **Clone this repo**
   ```bash
   git clone https://github.com/un1xr00t/RokuSpammer.git
   cd RokuSpammer
   ```

2. **Open in Android Studio**
   - Real Android device recommended (USB debugging)

3. **Build APK**
   - `Build > Build APK(s)`
   - Output path: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📸 Walkthrough

1. Tap **Scan** to find smart TVs on your Wi-Fi
2. Select a **Roku** from the dropdown list
3. Choose a command or select **Obliterate**
4. Adjust delay if needed
5. Tap **Start Pwning**
6. View real-time logs as commands are sent

---

## 📋 UI Highlights

- ✅ Neon green spinners on black background
- ✅ Centered monospace text
- ✅ Device list auto-disables “All Devices” if only one found
- ✅ Real-time log output with scroll
- ✅ Auto-scroll to latest log entry
- ✅ Spinner disables non-Roku devices (they’re still listed but greyed out)

---

## 🔐 Disclaimer

This app is intended **only for testing and educational purposes**.  
Do **not** use it on Roku devices you don’t own or without permission.  
The developer is **not responsible** for misuse.

By using this software, you accept full responsibility for your actions.  
Use ethically. Use responsibly. Or don’t use it at all.

---

## ❤️ Credits

- Created by [@un1xr00t](https://github.com/un1xr00t)
- Powered by:
  - `kotlinx.coroutines` — async engine
  - Android SDK + XML layouts
  - `HttpURLConnection` — low-level network commands
  - SSDP UDP logic — for device discovery

---

## 🧪 Coming Soon

- 📊 Command usage stats or log export
- 🌈 Themed color mode switch (dark mode, matrix, etc.)
- 🛠 Chaos intensity dial / command set customization
