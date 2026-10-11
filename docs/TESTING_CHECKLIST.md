# AI COMPANION — Hardware Testing & Commissioning Checklist

Perform these progressive validation steps before deploying the robot into an actual onion bed.

---

## Phase 1: Bench Power & Communication Tests
- [ ] **Power Rail Verification:** Measure 5.0V on Servo Buck Converter and 5.0V on Logic Buck Converter before attaching ESP32 boards.
- [ ] **ESP32 #1 Wi-Fi Beacon:** Power on ESP32 #1 and confirm `AI_COMPANION_ROBOT` Wi-Fi SSID appears on your mobile device.
- [ ] **App Connectivity:** Connect phone to `AI_COMPANION_ROBOT`. Open AI Companion app and verify `ESP #1 (MAIN)` shows **ONLINE** (green badge).
- [ ] **UART Link Integrity:** Connect ESP32 #2 via UART2 (GPIO 16/17 cross-connected + common GND). Verify `UART LINK` shows **CONNECTED** (green badge) on app home screen.
- [ ] **Camera Feed:** Power on ESP32-CAM. In **ROBOT** screen, verify live MJPEG stream renders without artifacts.

---

## Phase 2: Manual Control & Safety Override Verification
- [ ] **Emergency Stop (E-STOP):**
  - Press the prominent red **EMERGENCY STOP** button.
  - Verify all motor outputs and relays immediately cut out.
  - Verify red banner activates and requires deliberate **RESET** confirmation.
- [ ] **4-Way Directional Drive:**
  - Test `FORWARD`, `REVERSE`, `LEFT`, `RIGHT` on stands (wheels off ground).
  - Verify motors halt immediately when buttons are released.
- [ ] **Speed Selection:** Test `SLOW` (PWM 130), `MEDIUM` (PWM 185), `FAST` (PWM 240).
- [ ] **Ultrasonic Override Test:**
  - Place obstacle $< 25\text{ cm}$ in front of front HC-SR04.
  - Press `FORWARD`. Confirm movement is blocked with "Obstacle Detected" alert.
- [ ] **Camera Pan/Tilt:** Press `PAN LEFT/RIGHT`, `TILT UP/DOWN`, and `CENTER`. Confirm servo movements match angle readouts.
- [ ] **Manual Relay 1:** Toggle Relay 1. Confirm activation and automatic timeout after 30 seconds.

---

## Phase 3: Autonomous Sequences Validation
- [ ] **Pre-flight Checks Validation:**
  - Disconnect ESP32 #2 and attempt to tap `START AUTO`. Confirm app refuses to start and flags secondary ESP32 offline.
  - Reconnect ESP32 #2. Tap `START AUTO` and accept confirmation modal.
- [ ] **Branch 1 (AI Weed Removal):**
  - Place a test broadleaf weed in camera view.
  - Verify Slow Search stops upon weed confirmation.
  - Verify 3-DOF arm moves to calculated $(X, Y)$ ground coordinates.
  - Verify cutter activates for $\le 7$ seconds, turns OFF, and arm returns HOME.
  - Place an onion plant or green stalk. Confirm system classifies as ONION and DOES NOT activate cutter.
- [ ] **Branch 2 (Disease Monitoring):**
  - Expose TCS230 color sensor to a diseased/discolored leaf specimen.
  - Confirm robot stops and flags "Suspected Disease".
  - If chemical tank level $\ge 15\%$, verify sprayer activates for $\le 10$ seconds.
  - Empty chemical reservoir below $15\%$. Verify sprayer is SKIPPED with "CHEMICAL LOW" warning logged.
- [ ] **Branch 3 (Soil Testing Interval):**
  - Wait for the 5-minute interval trigger.
  - Verify robot stops, soil probe servo lowers to 90°, and temporary soil/temp value bars appear.
  - If soil moisture is below calibrated dry threshold and water tank is adequate, verify water pump activates for $\le 5$ seconds.
  - Verify probe returns HOME and robot resumes slow search.
