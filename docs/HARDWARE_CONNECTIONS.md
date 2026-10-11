# AI COMPANION — Hardware Connections & Wiring Guide

## 1. ESP32 #1 (Main Controller — 38-Pin DevKit)
| ESP32 Pin | Component / Function | Direction | Notes |
|---|---|---|---|
| **GPIO 16 (RX2)** | ESP32 #2 TX (GPIO 17) | Input | Inter-board UART link |
| **GPIO 17 (TX2)** | ESP32 #2 RX (GPIO 16) | Output | Inter-board UART link |
| **GPIO 18** | Camera Pan Servo Signal | Output | PWM timer 0 |
| **GPIO 19** | Camera Tilt Servo Signal | Output | PWM timer 1 |
| **GND** | Common System Ground | Power | Common to Battery & ESP32 #2 |
| **VIN / 5V** | 5V Regulated (UBEC / Buck Converter) | Power | 5V 2A supply |

---

## 2. ESP32 #2 (Hardware Controller — 38-Pin DevKit)
| ESP32 Pin | Component / Function | Direction | Voltage / Notes |
|---|---|---|---|
| **GPIO 16 (RX2)** | ESP32 #1 TX (GPIO 17) | Input | UART2 Rx |
| **GPIO 17 (TX2)** | ESP32 #1 RX (GPIO 16) | Output | UART2 Tx |
| **GPIO 14** | Motor Driver ENA (PWM) | Output | Left wheels speed PWM |
| **GPIO 27** | Motor Driver IN1 | Output | Left wheels forward |
| **GPIO 26** | Motor Driver IN2 | Output | Left wheels backward |
| **GPIO 25** | Motor Driver IN3 | Output | Right wheels forward |
| **GPIO 33** | Motor Driver IN4 | Output | Right wheels backward |
| **GPIO 32** | Motor Driver ENB (PWM) | Output | Right wheels speed PWM |
| **GPIO 18** | 3-DOF Arm Base Servo (J1) | Output | High-torque metal gear servo |
| **GPIO 19** | 3-DOF Arm Shoulder Servo (J2) | Output | High-torque metal gear servo |
| **GPIO 21** | 3-DOF Arm Elbow Servo (J3) | Output | High-torque metal gear servo |
| **GPIO 22** | Weed Cutter Motor Relay / MOSFET | Output | Active HIGH (Max 7s timeout) |
| **GPIO 23** | Chemical Pump Relay / MOSFET | Output | Active HIGH (Max 10s timeout) |
| **GPIO 5** | Water Irrigation Pump Relay | Output | Active HIGH (Max 5s timeout) |
| **GPIO 13** | Auxiliary Relay 1 | Output | Manual user assignment |
| **GPIO 12** | Front HC-SR04 Trig | Output | 10µs trigger pulse |
| **GPIO 35** | Front HC-SR04 Echo | Input | Voltage divider to 3.3V safe |
| **GPIO 15** | Water Tank HC-SR04 Trig | Output | Level sensor |
| **GPIO 34** | Water Tank HC-SR04 Echo | Input | Voltage divider to 3.3V safe |
| **GPIO 4** | DHT11 Climate Data | In/Out | 4.7kΩ pullup to 3.3V |
| **GPIO 36 (VP)**| Capacitive Soil Moisture Analog | Analog In | 0..3.3V range |
| **GPIO 39 (VN)**| 12V Battery Voltage Divider | Analog In | 30kΩ / 7.5kΩ divider |

---

## 3. Power Architecture & Grounding
* **Main Battery:** 3S LiPo or 12V LiFePO4 battery (11.1V - 12.6V nominal).
* **Buck Converter 1 (5V 5A):** Supplies servos (high current draw during arm motion). Never power arm servos directly from ESP32 3.3V or 5V pins!
* **Buck Converter 2 (5V 3A):** Supplies clean power to ESP32 #1, ESP32 #2, ESP32-CAM, and sensors.
* **Flyback Diodes (1N4007):** Placed across DC motor, cutter motor, and pump terminals to suppress inductive voltage spikes.
