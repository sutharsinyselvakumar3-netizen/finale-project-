/**
 * ============================================================================
 * AI COMPANION — Smart Onion Garden Maintainer Robot
 * ESP32 #1 — Main Controller Firmware
 * ============================================================================
 * Responsibilities:
 * - Wi-Fi Station / SoftAP connectivity & HTTP REST API on Port 80
 * - UART Master link (UART2 on GPIO 17 TX / GPIO 16 RX) to ESP32 #2
 * - Mode Enforcement: MANUAL vs AUTO vs EMERGENCY_STOP
 * - Safety overrides (Ultrasonic obstacle detection stops motors)
 * - Camera Pan / Tilt Servo Control (GPIO 18 / GPIO 19)
 * - Heartbeat supervisor & structured JSON telemetry reporting
 * ============================================================================
 */

#include <WiFi.h>
#include <WebServer.h>
#include <ArduinoJson.h> // ArduinoJson v6 or v7 compatible
#include <ESP32Servo.h>
#include "../shared/uart_protocol.h"

// Wi-Fi Configuration (Default SoftAP; optionally joins garden router)
const char* AP_SSID = "AI_COMPANION_ROBOT";
const char* AP_PASS = "OnionProtect2026"; // Minimum 8 chars
IPAddress local_IP(192, 168, 4, 1);
IPAddress gateway(192, 168, 4, 1);
IPAddress subnet(255, 255, 255, 0);

WebServer server(80);

// Hardware UART2 for ESP32 #2 Communication
#define UART2_RX_PIN 16
#define UART2_TX_PIN 17
HardwareSerial HWSerial(2);

// Camera Pan/Tilt Servos on ESP32 #1
#define PAN_SERVO_PIN  18
#define TILT_SERVO_PIN 19
Servo panServo;
Servo tiltServo;

// Operating State
enum SystemMode {
    SYS_MANUAL,
    SYS_AUTO,
    SYS_ESTOP
};

volatile SystemMode currentMode = SYS_MANUAL;
volatile bool emergencyStopActive = false;
int currentSpeed = 1; // 1: SLOW, 2: MED, 3: FAST
String currentMovement = "STOP";
int camPanAngle = 90;
int camTiltAngle = 90;
bool relay1State = false;

// Cached Telemetry from ESP32 #2
TelemetryPayload lastTelemetry;
bool secondaryEspOnline = false;
bool uartLinkHealthy = false;
unsigned long lastTelemetryRecvTime = 0;
uint8_t txSequence = 0;

// Function Prototypes
void setupHardware();
void setupWiFi();
void setupRestApi();
void sendUartPacket(uint8_t msgType, const uint8_t *payload, uint8_t len);
void processUartIncoming();
void handleCORS();

void setup() {
    Serial.begin(115200);
    Serial.println("\n[BOOT] ESP32 #1 Main Controller Initializing...");

    setupHardware();
    setupWiFi();
    setupRestApi();

    Serial.println("[BOOT] Main Controller Ready. Listening on http://192.168.4.1:80");
}

void loop() {
    server.handleClient();
    processUartIncoming();

    // Check UART heartbeat timeout
    if (millis() - lastTelemetryRecvTime > UART_HEARTBEAT_TIMEOUT_MS) {
        secondaryEspOnline = false;
        uartLinkHealthy = false;
    }

    // Periodic telemetry poll to ESP32 #2 (every 250ms)
    static unsigned long lastPollTime = 0;
    if (millis() - lastPollTime >= 250) {
        lastPollTime = millis();
        uint8_t dummy = 0;
        sendUartPacket(MSG_READ_SENSORS, &dummy, 1);
    }
}

void setupHardware() {
    // Initialize UART2 to ESP32 #2
    HWSerial.begin(115200, SERIAL_8N1, UART2_RX_PIN, UART2_TX_PIN);

    // Initialize Camera Servos
    ESP32PWM::allocateTimer(0);
    ESP32PWM::allocateTimer(1);
    panServo.setPeriodHertz(50);
    tiltServo.setPeriodHertz(50);
    panServo.attach(PAN_SERVO_PIN, 500, 2400);
    tiltServo.attach(TILT_SERVO_PIN, 500, 2400);

    panServo.write(camPanAngle);
    tiltServo.write(camTiltAngle);

    memset(&lastTelemetry, 0, sizeof(TelemetryPayload));
}

void setupWiFi() {
    WiFi.mode(WIFI_AP);
    WiFi.softAPConfig(local_IP, gateway, subnet);
    WiFi.softAP(AP_SSID, AP_PASS);

    Serial.print("[WIFI] Access Point started: ");
    Serial.println(AP_SSID);
    Serial.print("[WIFI] IP Address: ");
    Serial.println(WiFi.softAPIP());
}

// ============================================================================
// REST API ENDPOINTS
// ============================================================================
void handleCORS() {
    server.sendHeader("Access-Control-Allow-Origin", "*");
    server.sendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
}

void setupRestApi() {
    // OPTIONS for CORS pre-flight
    server.onNotFound([]() {
        if (server.method() == HTTP_OPTIONS) {
            handleCORS();
            server.send(204);
        } else {
            handleCORS();
            server.send(404, "application/json", "{\"error\":\"Not Found\"}");
        }
    });

    // GET /api/status - Complete System Telemetry
    server.on("/api/status", HTTP_GET, []() {
        handleCORS();
        DynamicJsonDocument doc(2048);

        doc["mainOnline"] = true;
        doc["secondaryOnline"] = secondaryEspOnline;
        doc["uartStatus"] = uartLinkHealthy ? "OK" : "ERROR";
        doc["mode"] = emergencyStopActive ? "ESTOP" : (currentMode == SYS_AUTO ? "AUTO" : "MANUAL");
        doc["movement"] = currentMovement;
        doc["speed"] = currentSpeed;
        doc["estopActive"] = emergencyStopActive;

        if (secondaryEspOnline) {
            if (lastTelemetry.frontUltrasonicMm != 65535) {
                doc["frontDistCm"] = lastTelemetry.frontUltrasonicMm / 10.0;
            } else {
                doc["frontDistCm"] = (char*)NULL;
                doc["ultrasonicError"] = true;
            }
            doc["obstacleDetected"] = (lastTelemetry.obstacleDetected == 1);
            doc["soilMoisturePct"] = lastTelemetry.soilMoisturePct;
            doc["temperatureC"] = lastTelemetry.temperatureC10 / 10.0;
            doc["humidityPct"] = lastTelemetry.humidityPct10 / 10.0;
            doc["waterTankPct"] = lastTelemetry.waterTankPct;
            doc["chemicalTankPct"] = lastTelemetry.chemicalTankPct;

            doc["tcsRed"] = lastTelemetry.tcsRed;
            doc["tcsGreen"] = lastTelemetry.tcsGreen;
            doc["tcsBlue"] = lastTelemetry.tcsBlue;
            doc["tcsIntensity"] = lastTelemetry.tcsClear;

            // Health heuristic from TCS values
            if (lastTelemetry.tcsGreen > (lastTelemetry.tcsRed * 1.3)) {
                doc["diseaseStatus"] = "HEALTHY";
            } else if (lastTelemetry.tcsRed > (lastTelemetry.tcsGreen * 0.9)) {
                doc["diseaseStatus"] = "DISEASE_SUSPECTED";
            } else {
                doc["diseaseStatus"] = "UNKNOWN";
            }

            doc["armBase"] = lastTelemetry.armBaseAngle;
            doc["armShoulder"] = lastTelemetry.armShoulderAngle;
            doc["armElbow"] = lastTelemetry.armElbowAngle;
            doc["armStatus"] = (lastTelemetry.armBaseAngle == 90 && lastTelemetry.armShoulderAngle == 90) ? "HOME" : "TARGET";

            doc["cutterOn"] = (lastTelemetry.cutterActive == 1);
            doc["chemicalPumpOn"] = (lastTelemetry.chemPumpActive == 1);
            doc["waterPumpOn"] = (lastTelemetry.waterPumpActive == 1);
            doc["relay1On"] = relay1State;

            if (lastTelemetry.batteryMilliVolts > 0) {
                doc["batteryVoltage"] = lastTelemetry.batteryMilliVolts / 1000.0;
            } else {
                doc["batteryVoltage"] = (char*)NULL;
            }
        } else {
            doc["frontDistCm"] = (char*)NULL;
            doc["soilMoisturePct"] = (char*)NULL;
            doc["temperatureC"] = (char*)NULL;
            doc["humidityPct"] = (char*)NULL;
            doc["waterTankPct"] = (char*)NULL;
            doc["chemicalTankPct"] = (char*)NULL;
            doc["batteryVoltage"] = (char*)NULL;
            doc["armBase"] = 90;
            doc["armShoulder"] = 90;
            doc["armElbow"] = 90;
        }

        doc["camPan"] = camPanAngle;
        doc["camTilt"] = camTiltAngle;
        doc["autoActive"] = (currentMode == SYS_AUTO);
        doc["mainFwVersion"] = "v1.0.0-MAIN";
        doc["secondaryFwVersion"] = "v1.0.0-HW";
        doc["systemUptimeSec"] = millis() / 1000;

        String output;
        serializeJson(doc, output);
        server.send(200, "application/json", output);
    });

    // GET /api/sensors - Lightweight Sensor Query
    server.on("/api/sensors", HTTP_GET, []() {
        handleCORS();
        DynamicJsonDocument doc(1024);
        doc["frontDistCm"] = (lastTelemetry.frontUltrasonicMm != 65535) ? (lastTelemetry.frontUltrasonicMm / 10.0) : 0;
        doc["soilMoisturePct"] = lastTelemetry.soilMoisturePct;
        doc["temperatureC"] = lastTelemetry.temperatureC10 / 10.0;
        doc["humidityPct"] = lastTelemetry.humidityPct10 / 10.0;
        doc["waterTankPct"] = lastTelemetry.waterTankPct;
        doc["chemicalTankPct"] = lastTelemetry.chemicalTankPct;
        doc["tcsRed"] = lastTelemetry.tcsRed;
        doc["tcsGreen"] = lastTelemetry.tcsGreen;
        doc["tcsBlue"] = lastTelemetry.tcsBlue;

        String out;
        serializeJson(doc, out);
        server.send(200, "application/json", out);
    });

    // POST /api/move - 4 Movement Directions with Obstacle Safety
    server.on("/api/move", HTTP_POST, []() {
        handleCORS();
        if (emergencyStopActive) {
            server.send(403, "application/json", "{\"success\":false,\"message\":\"Emergency Stop is active\"}");
            return;
        }

        DynamicJsonDocument doc(512);
        deserializeJson(doc, server.arg("plain"));
        String dir = doc["direction"] | "STOP";
        int spd = doc["speed"] | currentSpeed;

        currentMovement = dir;
        currentSpeed = spd;

        MotorDrivePayload payload;
        payload.speed = spd;
        payload.timeoutMs = 600; // Local watchdog

        if (dir == "FORWARD") {
            // Check ultrasonic safety override
            if (secondaryEspOnline && lastTelemetry.obstacleDetected == 1) {
                payload.direction = 0; // Force stop
                currentMovement = "STOP";
                sendUartPacket(MSG_MOTOR_DRIVE, (uint8_t*)&payload, sizeof(payload));
                server.send(409, "application/json", "{\"success\":false,\"message\":\"Obstacle Override Active\"}");
                return;
            }
            payload.direction = 1;
        } else if (dir == "BACKWARD") payload.direction = 2;
        else if (dir == "LEFT") payload.direction = 3;
        else if (dir == "RIGHT") payload.direction = 4;
        else payload.direction = 0;

        sendUartPacket(MSG_MOTOR_DRIVE, (uint8_t*)&payload, sizeof(payload));
        server.send(200, "application/json", "{\"success\":true}");
    });

    // POST /api/speed
    server.on("/api/speed", HTTP_POST, []() {
        handleCORS();
        DynamicJsonDocument doc(256);
        deserializeJson(doc, server.arg("plain"));
        currentSpeed = doc["speed"] | 1;
        server.send(200, "application/json", "{\"success\":true}");
    });

    // POST /api/camera/pantilt
    server.on("/api/camera/pantilt", HTTP_POST, []() {
        handleCORS();
        DynamicJsonDocument doc(256);
        deserializeJson(doc, server.arg("plain"));
        camPanAngle = constrain((int)doc["pan"], 10, 170);
        camTiltAngle = constrain((int)doc["tilt"], 20, 160);
        panServo.write(camPanAngle);
        tiltServo.write(camTiltAngle);
        server.send(200, "application/json", "{\"success\":true}");
    });

    // POST /api/actuators/relay
    server.on("/api/actuators/relay", HTTP_POST, []() {
        handleCORS();
        DynamicJsonDocument doc(256);
        deserializeJson(doc, server.arg("plain"));
        relay1State = doc["state"] | false;
        TimedActuatorPayload p = { (uint8_t)(relay1State ? 1 : 0), 30 };
        sendUartPacket(MSG_RELAY_CONTROL, (uint8_t*)&p, sizeof(p));
        server.send(200, "application/json", "{\"success\":true}");
    });

    // POST /api/estop - Immediate Emergency Stop
    server.on("/api/estop", HTTP_POST, []() {
        handleCORS();
        DynamicJsonDocument doc(256);
        deserializeJson(doc, server.arg("plain"));
        String action = doc["action"] | "TRIGGER";

        if (action == "TRIGGER") {
            emergencyStopActive = true;
            currentMode = SYS_ESTOP;
            currentMovement = "STOP";
            uint8_t dummy = 1;
            sendUartPacket(MSG_ESTOP_TRIGGER, &dummy, 1);
            server.send(200, "application/json", "{\"success\":true,\"message\":\"Emergency Stop Triggered\"}");
        } else {
            emergencyStopActive = false;
            currentMode = SYS_MANUAL;
            uint8_t dummy = 0;
            sendUartPacket(MSG_ESTOP_RESET, &dummy, 1);
            server.send(200, "application/json", "{\"success\":true,\"message\":\"Emergency Stop Reset\"}");
        }
    });

    // POST /api/auto - Start/Stop Autonomous Sequence
    server.on("/api/auto", HTTP_POST, []() {
        handleCORS();
        if (emergencyStopActive) {
            server.send(403, "application/json", "{\"success\":false,\"message\":\"Estop Active\"}");
            return;
        }
        DynamicJsonDocument doc(256);
        deserializeJson(doc, server.arg("plain"));
        String cmd = doc["command"] | "STOP";

        if (cmd == "START") {
            if (!secondaryEspOnline || !uartLinkHealthy) {
                server.send(412, "application/json", "{\"success\":false,\"message\":\"ESP32 #2 UART Offline\"}");
                return;
            }
            currentMode = SYS_AUTO;
            server.send(200, "application/json", "{\"success\":true,\"message\":\"AUTO mode active\"}");
        } else {
            currentMode = SYS_MANUAL;
            // Stop movement
            MotorDrivePayload stopPayload = {0, 1, 0};
            sendUartPacket(MSG_MOTOR_DRIVE, (uint8_t*)&stopPayload, sizeof(stopPayload));
            server.send(200, "application/json", "{\"success\":true,\"message\":\"AUTO mode stopped\"}");
        }
    });

    // POST /api/ai/target - Receive AI Weed Detection Target
    server.on("/api/ai/target", HTTP_POST, []() {
        handleCORS();
        DynamicJsonDocument doc(512);
        deserializeJson(doc, server.arg("plain"));

        String detected = doc["detected"] | "UNKNOWN";
        if (detected == "WEED") {
            ArmMovePayload armPayload;
            armPayload.baseAngle = doc["baseAngle"] | 90;
            armPayload.shoulderAngle = doc["shoulderAngle"] | 90;
            armPayload.elbowAngle = doc["elbowAngle"] | 90;
            sendUartPacket(MSG_ARM_MOVE, (uint8_t*)&armPayload, sizeof(armPayload));
        }
        server.send(200, "application/json", "{\"success\":true}");
    });

    // POST /api/actuators/arm
    server.on("/api/actuators/arm", HTTP_POST, []() {
        handleCORS();
        DynamicJsonDocument doc(256);
        deserializeJson(doc, server.arg("plain"));
        ArmMovePayload p;
        p.baseAngle = doc["base"] | 90;
        p.shoulderAngle = doc["shoulder"] | 90;
        p.elbowAngle = doc["elbow"] | 90;
        sendUartPacket(MSG_ARM_MOVE, (uint8_t*)&p, sizeof(p));
        server.send(200, "application/json", "{\"success\":true}");
    });

    // POST /api/actuators/cutter - Max 7 seconds timeout enforced
    server.on("/api/actuators/cutter", HTTP_POST, []() {
        handleCORS();
        DynamicJsonDocument doc(256);
        deserializeJson(doc, server.arg("plain"));
        bool state = doc["state"] | false;
        int dur = min((int)(doc["durationSec"] | 7), 7); // Clamped to 7s max
        TimedActuatorPayload p = { (uint8_t)(state ? 1 : 0), (uint16_t)dur };
        sendUartPacket(MSG_CUTTER_TIMED, (uint8_t*)&p, sizeof(p));
        server.send(200, "application/json", "{\"success\":true}");
    });

    // POST /api/actuators/pump/chemical - Max 10s
    server.on("/api/actuators/pump/chemical", HTTP_POST, []() {
        handleCORS();
        DynamicJsonDocument doc(256);
        deserializeJson(doc, server.arg("plain"));
        bool state = doc["state"] | false;
        int dur = min((int)(doc["durationSec"] | 10), 10);
        TimedActuatorPayload p = { (uint8_t)(state ? 1 : 0), (uint16_t)dur };
        sendUartPacket(MSG_CHEM_PUMP_TIMED, (uint8_t*)&p, sizeof(p));
        server.send(200, "application/json", "{\"success\":true}");
    });

    // POST /api/actuators/pump/water - Max 5s
    server.on("/api/actuators/pump/water", HTTP_POST, []() {
        handleCORS();
        DynamicJsonDocument doc(256);
        deserializeJson(doc, server.arg("plain"));
        bool state = doc["state"] | false;
        int dur = min((int)(doc["durationSec"] | 5), 5);
        TimedActuatorPayload p = { (uint8_t)(state ? 1 : 0), (uint16_t)dur };
        sendUartPacket(MSG_WATER_PUMP_TIMED, (uint8_t*)&p, sizeof(p));
        server.send(200, "application/json", "{\"success\":true}");
    });

    server.begin();
}

// ============================================================================
// UART PROTOCOL ENCODER & DECODER
// ============================================================================
void sendUartPacket(uint8_t msgType, const uint8_t *payload, uint8_t len) {
    if (len > MAX_PAYLOAD_SIZE) return;

    uint8_t packet[MAX_PAYLOAD_SIZE + 6];
    packet[0] = PROTOCOL_START_BYTE;
    packet[1] = PROTOCOL_VERSION;
    packet[2] = msgType;
    packet[3] = txSequence++;
    packet[4] = len;

    if (len > 0 && payload != NULL) {
        memcpy(&packet[5], payload, len);
    }

    uint8_t chk = computeChecksum(&packet[1], len + 4);
    packet[5 + len] = chk;
    packet[6 + len] = PROTOCOL_END_BYTE;

    HWSerial.write(packet, len + 7);
}

void processUartIncoming() {
    static uint8_t rxBuffer[128];
    static uint8_t rxIdx = 0;
    static bool inFrame = false;

    while (HWSerial.available()) {
        uint8_t b = HWSerial.read();

        if (!inFrame) {
            if (b == PROTOCOL_START_BYTE) {
                inFrame = true;
                rxIdx = 0;
                rxBuffer[rxIdx++] = b;
            }
        } else {
            rxBuffer[rxIdx++] = b;

            // Header is at least 6 bytes: START, VER, TYPE, SEQ, LEN, CHK, END
            if (rxIdx >= 6) {
                uint8_t expectedLen = rxBuffer[4];
                uint8_t totalExpected = expectedLen + 7;

                if (rxIdx >= totalExpected) {
                    if (rxBuffer[totalExpected - 1] == PROTOCOL_END_BYTE) {
                        uint8_t calcChk = computeChecksum(&rxBuffer[1], expectedLen + 4);
                        uint8_t rxChk = rxBuffer[5 + expectedLen];

                        if (calcChk == rxChk) {
                            // Valid packet!
                            uint8_t msgType = rxBuffer[2];
                            if (msgType == MSG_TELEMETRY_PACKET && expectedLen == sizeof(TelemetryPayload)) {
                                memcpy(&lastTelemetry, &rxBuffer[5], sizeof(TelemetryPayload));
                                secondaryEspOnline = true;
                                uartLinkHealthy = true;
                                lastTelemetryRecvTime = millis();
                            } else if (msgType == MSG_ACK) {
                                uartLinkHealthy = true;
                                lastTelemetryRecvTime = millis();
                            }
                        }
                    }
                    inFrame = false;
                    rxIdx = 0;
                }
            }

            if (rxIdx >= sizeof(rxBuffer)) {
                inFrame = false;
                rxIdx = 0;
            }
        }
    }
}
