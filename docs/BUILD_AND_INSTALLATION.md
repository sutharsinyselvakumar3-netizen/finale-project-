# Build and Installation Instructions

## 1. Android Application Build
The application is built using modern Jetpack Compose, Kotlin DSL Gradle, Material 3, Room, OkHttp, Retrofit, and Moshi.

### Building in AI Studio / Android Studio:
1. Open the project root in Android Studio or build in this AI Studio environment.
2. Verify `compileSdk = 36`, `minSdk = 24`, and Kotlin 2.2.
3. Build the debug or release APK:
   ```bash
   gradle :app:assembleDebug
   ```
4. The generated APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`
5. Install onto your Android phone or agricultural tablet:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 2. ESP32 Firmware Compilation

### Required Libraries in Arduino IDE Library Manager:
* **ArduinoJson** (by Benoît Blanchon) — version 6.21.x or 7.x
* **ESP32Servo** (by Kevin Harrington) — version 3.0.x
* **DHT sensor library** (by Adafruit)
* **Adafruit Unified Sensor**

### Flashing Procedure:
1. **ESP32 #1 (Main Controller):**
   * Open `firmware/esp32_main_controller/esp32_main_controller.ino`.
   * Select Board: `ESP32 Dev Module`.
   * Partition Scheme: `Default 4MB with spiffs (1.2MB APP / 1.5MB SPIFFS)`.
   * Connect USB cable to ESP32 #1 and click **Upload**.
2. **ESP32 #2 (Hardware Controller):**
   * Open `firmware/esp32_hardware_controller/esp32_hardware_controller.ino`.
   * Select Board: `ESP32 Dev Module`.
   * Connect USB cable to ESP32 #2 and click **Upload**.
3. **ESP32-CAM:**
   * Open CameraWebServer sketch as documented in `docs/CAMERA_SETUP.md`.
   * Upload using FTDI programmer with GPIO 0 grounded.
