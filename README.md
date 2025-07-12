# 📺 RokuSpammer

A simple Android app that sends randomized keypress commands to Roku devices on your local network.

Built with Kotlin + Android Studio, this app is purely for **educational and testing** purposes.

---

## 🚀 Features

- 🔁 Chaos Mode: sends randomized Roku remote commands (Home, Up, Down, Select, etc.)
- 🌐 Manual IP entry
- 🔍 (Coming Soon) Network scanner to find Roku devices automatically
- 📱 Works on real Android devices (emulator won't affect real TVs)

---

## 🛠 Setup

1. **Clone the repo**  
   ```bash
   git clone git@github.com:un1xr00t/RokuSpammer.git
   cd RokuSpammer
   ```

2. **Open in Android Studio**  
   - Run on a physical device with Developer Mode + USB Debugging enabled

3. **Build APK**  
   `Build > Build APK(s)` in Android Studio  
   The `.apk` will be in `app/build/outputs/apk/debug/`

---

## 📦 Download APK

👉 [Download Latest APK](https://github.com/un1xr00t/RokuSpammer/releases/latest/download/app-debug.apk)  
*(Link will work after you publish your first GitHub release)*

---

## 📸 UI

- 🔲 Enter your Roku’s IP address (e.g. `192.168.0.106`)
- ▶️ Tap **Start Chaos** to begin sending random commands
- ⏹️ Tap **Stop** to halt the spam

---

## 🔐 Disclaimer

This tool is meant for **personal educational use only**.  
Do not use it on devices you do not own or without permission.  
I’m not responsible for how it’s used.

---

## ❤️ Credits

- Developed by [@un1xr00t](https://github.com/un1xr00t)
- Uses:
  - `okhttp3` for HTTP requests
  - `kotlinx.coroutines` for async tasking

---

## 🧪 Coming Soon

- 📡 Auto-scan network for Roku devices
- 🧩 Add mode toggle for specific command patterns
- 📊 Command history viewer (log)

---
