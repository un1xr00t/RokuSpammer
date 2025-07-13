# 📺 RokuSpammer

A simple Android app that sends keypress commands to Roku devices on your local network.

Built in **Kotlin + Android Studio**, the app is designed for educational testing of Roku devices’ remote interfaces.

---

## 🚀 Features

- 🔁 **Chaos Mode**: Automatically spams all common remote commands (Volume, Home, Arrows, etc.)
- 🌐 **Manual IP entry** or dropdown selection from scanned devices
- 📡 **Local network Roku scanner** — finds all Roku devices on your subnet
- 🎛️ **Attack Method dropdown** — select a specific command or activate "Chaos"
- ⏱️ **Delay input** — throttle how fast commands are sent (in milliseconds)
- 📜 **Real-time log output** with scroll and auto-scroll to bottom
- 🎨 Styled UI with transparent background image, neon green accents, and themed spinners

---

## 🛠 Setup

1. **Clone the repo**  
   ```bash
   git clone https://github.com/un1xr00t/RokuSpammer.git
   cd RokuSpammer
   ```

2. **Open in Android Studio**  
   - Connect a real Android device with USB debugging enabled

3. **Build APK**  
   `Build > Build APK(s)`  
   Output APK is located at:  
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 📦 Download APK

👉 [Download Latest APK](https://github.com/un1xr00t/RokuSpammer/releases)
*(Will be live after the first GitHub release)*

---

## 📸 UI Walkthrough

- 🖥️ **Enter IP** or tap **Scan** to find Rokus on your local network
- 🧩 **Choose an attack method** from the dropdown (e.g., `VolumeUp`, `Home`, or `Chaos`)
- ⏱️ Set a custom **delay between attacks** (ms)
- ▶️ Tap **Start Pwning** to begin
- ⏹️ Tap **Stop** to halt

### 🧾 Logs
All actions are logged in real-time below the controls.  
The log area supports auto-scrolling and selectable/copyable text.

---

## 🎨 UI Customizations

- ✅ Transparent hacker-style background image
- ✅ Glow-effect green buttons
- ✅ Styled spinners with dark backgrounds and green text
- ✅ Clean vertical layout with padding and margin tweaks

---

## 🔐 Disclaimer

This app is intended **only for testing and educational purposes**.  
Do **not** use it on Roku devices you don’t own or without permission.  
The developer is **not responsible** for misuse.

---

## ❤️ Credits

- Created by [@un1xr00t](https://github.com/un1xr00t)
- Powered by:
  - `kotlinx.coroutines` — async task execution
  - Android SDK + XML layouts
  - `HttpURLConnection` for native HTTP requests

---

## 🧪 Coming Soon

- 📊 Command usage stats or log export
- 🌈 Themed color mode switch (dark mode, matrix, etc.)
- 🛠 Chaos intensity dial / command set customization

---
