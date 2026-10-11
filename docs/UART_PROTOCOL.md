# AI COMPANION — UART Communication Protocol Specification

## 1. Overview
The AI Companion Onion Garden Robot uses a bidirectional hardware UART link between:
* **ESP32 #1 (Main Controller)**: Master coordinator, Wi-Fi gateway, AI vision target processor.
* **ESP32 #2 (Hardware Controller)**: Actuator executor, sensor digitizer, motor driver.

Physical Link: **115200 baud, 8 data bits, no parity, 1 stop bit (8N1)**.
Cross-connect:
* ESP32 #1 TX (GPIO 17) -> ESP32 #2 RX (GPIO 16)
* ESP32 #1 RX (GPIO 16) -> ESP32 #2 TX (GPIO 17)
* Common GND connected between both boards.

---

## 2. Packet Framing Structure

All messages are strictly framed with start/end delimiters and a byte-wise XOR checksum.

| Byte Offset | Field | Length | Description |
|---|---|---|---|
| 0 | `START_BYTE` | 1 Byte | Constant `0xAA` |
| 1 | `PROTOCOL_VERSION` | 1 Byte | `0x01` |
| 2 | `MSG_TYPE` | 1 Byte | Command / Response Type ID |
| 3 | `SEQ_NUM` | 1 Byte | Incremental rolling sequence number (0..255) |
| 4 | `PAYLOAD_LEN` | 1 Byte | Payload byte length ($N \le 64$) |
| 5 .. $(4+N)$ | `PAYLOAD` | $N$ Bytes | Packed struct binary data |
| $5+N$ | `CHECKSUM` | 1 Byte | XOR checksum over bytes 1 through $(4+N)$ |
| $6+N$ | `END_BYTE` | 1 Byte | Constant `0x55` |

---

## 3. Message Type Catalog

### Commands (ESP32 #1 -> ESP32 #2)
| Type ID | Enum Name | Description | Payload |
|---|---|---|---|
| `0x01` | `MSG_PING` | Link heartbeat request | None |
| `0x02` | `MSG_ESTOP_TRIGGER` | Emergency stop — kill all outputs | 1 Byte |
| `0x03` | `MSG_ESTOP_RESET` | Clear emergency stop | 1 Byte |
| `0x10` | `MSG_MOTOR_DRIVE` | Drive motors (direction, speed, watchdog) | `MotorDrivePayload` (4 Bytes) |
| `0x20` | `MSG_ARM_MOVE` | 3-DOF Arm joint angles (Base, Shoulder, Elbow) | `ArmMovePayload` (3 Bytes) |
| `0x30` | `MSG_CUTTER_TIMED` | Engage weed cutter with max duration (max 7s) | `TimedActuatorPayload` (3 Bytes) |
| `0x31` | `MSG_CHEM_PUMP_TIMED` | Spray chemical pump with max duration (max 10s) | `TimedActuatorPayload` (3 Bytes) |
| `0x32` | `MSG_WATER_PUMP_TIMED` | Root hydration pump with max duration (max 5s) | `TimedActuatorPayload` (3 Bytes) |
| `0x33` | `MSG_RELAY_CONTROL` | Auxiliary Relay 1 on/off | `TimedActuatorPayload` (3 Bytes) |
| `0x40` | `MSG_READ_SENSORS` | Poll all sensors | 1 Byte |

### Responses & Telemetry (ESP32 #2 -> ESP32 #1)
| Type ID | Enum Name | Description | Payload |
|---|---|---|---|
| `0x80` | `MSG_ACK` | Command acknowledged | 1 Byte |
| `0x81` | `MSG_NACK` | Command rejected / parameter error | 1 Byte |
| `0x90` | `MSG_TELEMETRY_PACKET` | Complete sensor & actuator state | `TelemetryPayload` (28 Bytes) |

---

## 4. Safety Fail-Safe & Watchdog Logic
1. **Heartbeat Timeout:** If ESP32 #2 receives no valid packets for $> 2000\text{ ms}$, it immediately stops DC motors, disengages the cutter, and turns off all pumps.
2. **Motor Watchdog:** Every `MSG_MOTOR_DRIVE` specifies a timeout limit (default $600\text{ ms}$). Motors stop automatically unless refreshed.
3. **Hardware-Enforced Actuator Caps:**
   - Cutter runtime is capped at $\le 7$ seconds.
   - Sprayer runtime is capped at $\le 10$ seconds.
   - Water pump runtime is capped at $\le 5$ seconds.
4. **Ultrasonic Safety Override:** If front distance $< 250\text{ mm}$ ($25\text{ cm}$), ESP32 #2 automatically overrides any forward drive command and forces an immediate stop.
