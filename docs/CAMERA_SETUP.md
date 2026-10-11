# AI-Thinker ESP32-CAM Setup & Calibration Guide

## 1. Overview
The AI-Thinker ESP32-CAM (with OV2640 sensor) provides:
1. Continuous real-time MJPEG video stream on port 81 (`http://CAMERA_IP:81/stream`).
2. High-resolution single-frame JPEG capture on port 80 (`http://CAMERA_IP/capture`).

The Android app processes captured frames for onion/weed classification and arm kinematics.

---

## 2. Firmware Flashing (Arduino IDE / PlatformIO)

### Prerequisites:
* AI-Thinker ESP32-CAM module
* FTDI USB-to-TTL programmer (3.3V / 5V jumper set to 5V for ESP32-CAM VCC)
* Connect **GPIO 0 to GND** during flashing to enter bootloader mode.

### Pin Wiring for FTDI Flashing:
| FTDI Programmer | ESP32-CAM Pin |
|---|---|
| VCC (5V) | 5V |
| GND | GND |
| TX | U0R (GPIO 3) |
| RX | U0T (GPIO 1) |
| *Jumper* | Connect GPIO 0 to GND |

### Camera WebServer Code Template:
Use the standard ESP32 Camera WebServer sketch (`Examples -> ESP32 -> Camera -> CameraWebServer`), configured with:
```cpp
#define CAMERA_MODEL_AI_THINKER // Has PSRAM
```
Set resolution to `FRAMESIZE_VGA` (640x480) or `FRAMESIZE_SVGA` (800x600) for optimal AI weed boundary precision and smooth frame rate.

After uploading:
1. Disconnect GPIO 0 from GND.
2. Press the reset button on ESP32-CAM.
3. Open Serial Monitor (115200 baud) to view the assigned IP address (e.g., `192.168.4.2` or DHCP lease).

---

## 3. Verified Endpoints
* **Live MJPEG Stream:** `http://192.168.4.2:81/stream`
* **Still Frame Capture:** `http://192.168.4.2/capture`
* **Camera Status:** `http://192.168.4.2/status`

Enter these URLs in the Android App **SETTINGS** screen. Use the **Test Camera** button to confirm image decoding.
