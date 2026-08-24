cat > README.md << 'EOF'
# ⚠️ Educational Prank App – Use at Your Own Risk

**This application is provided strictly for educational and research purposes only.**

It demonstrates how Android background services, audio playback, and system volume control work.  
The app is **not intended for malicious use**, and should never be installed on any device without the explicit consent of the owner.

---

## 🚨 WARNING

- This app plays a continuous sound in the background and repeatedly sets the media volume to maximum.
- It is designed to be a harmless demonstration of Android system APIs.
- **Do not install this app on someone else's device without their permission.**
- **Do not use this app for any illegal or unethical activities.**

By downloading, building, or using this app, you **acknowledge and agree** that:

1. You are solely responsible for any consequences arising from its use.
2. The developer(s) assume **no liability** for any damage, data loss, annoyance, or legal issues that may result.
3. You will use this app only in controlled environments where all users are aware and have consented.

---

## 📱 How It Works

- When launched, the app starts a foreground service that plays an audio file (included as `res/raw/og.mp3`).
- The service continuously sets the media volume to the maximum level (every 200ms).
- The main activity finishes immediately, leaving only the background service running.

---

## 🔧 Technical Details

- **Language:** Kotlin
- **Minimum SDK:** 21 (Android 5.0)
- **Target SDK:** 35
- **Permissions used:**
  - `MODIFY_AUDIO_SETTINGS` – attempts to change system volume.
  - `WAKE_LOCK` – keeps the CPU awake.
  - `FOREGROUND_SERVICE` – required for background playback.

---

## 🛠️ Build Instructions

```bash
./gradlew clean assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
