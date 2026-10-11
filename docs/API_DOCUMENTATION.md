# AI COMPANION — ESP32 #1 REST API Documentation

Base URL: `http://192.168.4.1:80` (Default SoftAP) or configured LAN IP.
All request and response payloads use `application/json; charset=utf-8`.

---

## 1. System Telemetry & Sensors

### `GET /api/status`
Returns full robot telemetry, communication health, sensor readings, and actuator feedback.

**Response:**
```json
{
  "mainOnline": true,
  "secondaryOnline": true,
  "uartStatus": "OK",
  "mode": "MANUAL",
  "movement": "STOP",
  "speed": 1,
  "estopActive": false,
  "frontDistCm": 42.5,
  "obstacleDetected": false,
  "soilMoisturePct": 45,
  "temperatureC": 25.4,
  "humidityPct": 62.0,
  "waterTankPct": 80,
  "chemicalTankPct": 65,
  "tcsRed": 120,
  "tcsGreen": 240,
  "tcsBlue": 90,
  "tcsIntensity": 450,
  "diseaseStatus": "HEALTHY",
  "armStatus": "HOME",
  "armBase": 90,
  "armShoulder": 90,
  "armElbow": 90,
  "camPan": 90,
  "camTilt": 90,
  "cutterOn": false,
  "chemicalPumpOn": false,
  "waterPumpOn": false,
  "relay1On": false,
  "batteryVoltage": 12.4,
  "autoActive": false,
  "mainFwVersion": "v1.0.0-MAIN",
  "secondaryFwVersion": "v1.0.0-HW",
  "systemUptimeSec": 1240
}
```

### `GET /api/sensors`
Lightweight endpoint for fast sensor telemetry polling.

---

## 2. Drive & Motion Controls

### `POST /api/move`
Sends robot chassis drive commands.

**Request:**
```json
{
  "direction": "FORWARD", // "STOP", "FORWARD", "BACKWARD", "LEFT", "RIGHT"
  "speed": 1             // 1: SLOW, 2: MEDIUM, 3: FAST
}
```

**Response (Obstacle Conflict):**
```json
{
  "success": false,
  "message": "Obstacle Override Active"
}
```

### `POST /api/speed`
Sets active speed preset (`1`, `2`, or `3`).

---

## 3. Servos & Robotic Arm

### `POST /api/camera/pantilt`
Controls camera pan/tilt servos on ESP32 #1.

**Request:**
```json
{
  "pan": 90,
  "tilt": 85
}
```

### `POST /api/actuators/arm`
Controls the 3-DOF mechanical robotic arm on ESP32 #2.

**Request:**
```json
{
  "base": 90,
  "shoulder": 65,
  "elbow": 115
}
```

---

## 4. Actuators & Safety

### `POST /api/actuators/cutter`
Activates the mechanical weed cutting blade. Hardware enforces a maximum duration of 7 seconds.

**Request:**
```json
{
  "state": true,
  "durationSec": 7
}
```

### `POST /api/actuators/pump/chemical`
Activates the chemical treatment pump (maximum 10 seconds).

### `POST /api/actuators/pump/water`
Activates the root irrigation pump (maximum 5 seconds).

### `POST /api/actuators/relay`
Controls auxiliary Relay 1 with a 30-second safe auto-shutoff timeout.

### `POST /api/estop`
Emergency Stop trigger or reset.

**Request:**
```json
{
  "action": "TRIGGER" // or "RESET"
}
```

---

## 5. Autonomous & AI Targets

### `POST /api/auto`
Starts or stops autonomous sequence loop.
```json
{
  "command": "START" // or "STOP"
}
```

### `POST /api/ai/target`
Sends AI weed classification result from Android camera frame analysis.
```json
{
  "detected": "WEED",
  "confidence": 0.88,
  "xCenter": 320.5,
  "yCenter": 240.0,
  "baseAngle": 84,
  "shoulderAngle": 62,
  "elbowAngle": 112
}
```
