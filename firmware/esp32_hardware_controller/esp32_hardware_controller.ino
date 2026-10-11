/**
 * ============================================================================
 * AI COMPANION — Smart Onion Garden Maintainer Robot
 * ESP32 #2 — Hardware Controller Firmware
 * ============================================================================
 * Responsibilities:
 * - Dual H-Bridge Motor Driver (L298N / TB6612) with PWM speed control
 * - 3-DOF Robotic Arm Servos (Base, Shoulder, Elbow)
 * - Actuator drivers: Cutter, Chemical Pump, Water Pump, Relay 1
 * - Sensor acquisition:
 *     * HC-SR04 Front Ultrasonic Sensor (Hardware safety stop)
 *     * TCS230 / TCS3200 Color Sensor
 *     * Capacitive Soil Moisture Sensor (Analog)
 *     * DHT11 Temperature & Relative Humidity Sensor
 *     * HC-SR04 Water Tank & Chemical Tank Level Sensors
 *     * Voltage Divider Battery Telemetry (ADC)
 * - Autonomous Fail-Safe: If UART communication is lost > 2000ms or E-STOP
 *   is triggered, all motors, cutters, and pumps are immediately de-energized.
 * ============================================================================
 */

#include <Arduino.h>
#include <ESP32Servo.h>
#include <DHT.h>
#include "../shared/uart_protocol.h"

// ----------------------------------------------------------------------------
// PIN ASSIGNMENTS (Configurable for 38-Pin ESP32 NodeMCU / DevKit)
// ----------------------------------------------------------------------------
// UART2 to ESP32 #1
#define UART_RX_PIN          16
#define UART_TX_PIN          17

// DC Motor Driver (L298N / TB6612FNG)
#define MOTOR_ENA_PIN        14  // Left Motors PWM
#define MOTOR_IN1_PIN        27  // Left Motor Forward
#define MOTOR_IN2_PIN        26  // Left Motor Backward
#define MOTOR_IN3_PIN        25  // Right Motor Forward
#define MOTOR_IN4_PIN        33  // Right Motor Backward
#define MOTOR_ENB_PIN        32  // Right Motors PWM

// 3-DOF Robotic Arm Servos
#define ARM_BASE_PIN         18  // Joint 1: Base azimuth
#define ARM_SHOULDER_PIN     19  // Joint 2: Shoulder elevation
#define ARM_ELBOW_PIN        21  // Joint 3: Elbow flexion

// Actuators (Relays / MOSFET drivers - Active HIGH)
#define CUTTER_PIN           22  // Weed Cutter Motor
#define CHEM_PUMP_PIN        23  // Chemical Herbicide/Fungicide Sprayer
#define WATER_PUMP_PIN       5   // Root Hydration Pump
#define RELAY_1_PIN          13  // Auxiliary Relay 1

// Sensors
#define FRONT_TRIG_PIN       12  // Front HC-SR04 Ultrasonic Trigger
#define FRONT_ECHO_PIN       35  // Front HC-SR04 Ultrasonic Echo (Input only)

#define WATER_TRIG_PIN       15  // Water Tank HC-SR04 Trigger
#define WATER_ECHO_PIN       34  // Water Tank HC-SR04 Echo (Input only)

#define CHEM_TRIG_PIN        2   // Chemical Tank HC-SR04 Trigger
#define CHEM_ECHO_PIN        39  // Chemical Tank HC-SR04 Echo (Input only)

#define DHT_PIN              4   // DHT11 Data Pin
#define DHTTYPE              DHT11

#define SOIL_ANALOG_PIN      36  // Capacitive Soil Moisture Analog (ADC1_CH0)
#define BATTERY_ADC_PIN      39  // Battery Voltage Divider (ADC1_CH3)

// TCS230 / TCS3200 Color Sensor
#define TCS_S0_PIN           0
#define TCS_S1_PIN           2   // Note: Shared or multiplexed
#define TCS_S2_PIN           25  // Alternative filter pin
#define TCS_S3_PIN           26
#define TCS_OUT_PIN          27

// ----------------------------------------------------------------------------
// ACTUATOR OBJECTS & TIMEOUT CONSTANTS
// ----------------------------------------------------------------------------
Servo armBaseServo;
Servo armShoulderServo;
Servo armElbowServo;
DHT dht(DHT_PIN, DHTTYPE);

HardwareSerial MainSerial(2);

// Hardware Enforced Timeout Trackers
unsigned long cutterStartTime = 0;
unsigned long chemPumpStartTime = 0;
unsigned long waterPumpStartTime = 0;
unsigned long relay1StartTime = 0;
unsigned long motorCommandTime = 0;
unsigned long motorWatchdogLimitMs = 600;

unsigned int cutterMaxDurationSec = 7;     // Strict 7-second max limit
unsigned int chemPumpMaxDurationSec = 10;  // Strict 10-second max limit
unsigned int waterPumpMaxDurationSec = 5;  // Strict 5-second max limit

bool cutterRunning = false;
bool chemPumpRunning = false;
bool waterPumpRunning = false;
bool relay1Running = false;
bool estopLatched = false;

unsigned long lastUartPacketReceived = 0;
uint8_t txSequence = 0;

// Telemetry cache
TelemetryPayload currentTelemetry;

// Function Prototypes
void initPins();
void setMotorSpeeds(int leftSpeed, int rightSpeed);
void stopMotors();
void updateActuatorTimeouts();
void readAllSensors();
void sendUartPacket(uint8_t msgType, const uint8_t *payload, uint8_t len);
void processUartCommands();
uint16_t measureUltrasonicMm(uint8_t trigPin, uint8_t echoPin);

void setup() {
    Serial.begin(115200);
    Serial.println("\n[BOOT] ESP32 #2 Hardware Controller Initializing...");

    initPins();
    dht.begin();
    MainSerial.begin(115200, SERIAL_8N1, UART_RX_PIN, UART_TX_PIN);

    // Initial safe state
    stopMotors();
    digitalWrite(CUTTER_PIN, LOW);
    digitalWrite(CHEM_PUMP_PIN, LOW);
    digitalWrite(WATER_PUMP_PIN, LOW);
    digitalWrite(RELAY_1_PIN, LOW);

    // Arm to HOME position
    armBaseServo.write(90);
    armShoulderServo.write(90);
    armElbowServo.write(90);

    lastUartPacketReceived = millis();
    Serial.println("[BOOT] ESP32 #2 Hardware Controller Ready.");
}

void loop() {
    processUartCommands();
    updateActuatorTimeouts();

    // Check UART Watchdog (2000ms lost communication safety shutdown)
    if (millis() - lastUartPacketReceived > UART_HEARTBEAT_TIMEOUT_MS) {
        stopMotors();
        if (cutterRunning) { digitalWrite(CUTTER_PIN, LOW); cutterRunning = false; }
        if (chemPumpRunning) { digitalWrite(CHEM_PUMP_PIN, LOW); chemPumpRunning = false; }
        if (waterPumpRunning) { digitalWrite(WATER_PUMP_PIN, LOW); waterPumpRunning = false; }
    }

    // Periodic sensor read and telemetry send (every 250ms)
    static unsigned long lastSensorTick = 0;
    if (millis() - lastSensorTick >= 250) {
        lastSensorTick = millis();
        readAllSensors();
        sendUartPacket(MSG_TELEMETRY_PACKET, (uint8_t*)&currentTelemetry, sizeof(TelemetryPayload));
    }
}

void initPins() {
    pinMode(MOTOR_ENA_PIN, OUTPUT);
    pinMode(MOTOR_IN1_PIN, OUTPUT);
    pinMode(MOTOR_IN2_PIN, OUTPUT);
    pinMode(MOTOR_IN3_PIN, OUTPUT);
    pinMode(MOTOR_IN4_PIN, OUTPUT);
    pinMode(MOTOR_ENB_PIN, OUTPUT);

    pinMode(CUTTER_PIN, OUTPUT);
    pinMode(CHEM_PUMP_PIN, OUTPUT);
    pinMode(WATER_PUMP_PIN, OUTPUT);
    pinMode(RELAY_1_PIN, OUTPUT);

    pinMode(FRONT_TRIG_PIN, OUTPUT);
    pinMode(FRONT_ECHO_PIN, INPUT);
    pinMode(WATER_TRIG_PIN, OUTPUT);
    pinMode(WATER_ECHO_PIN, INPUT);

    // PWM Setup for DC Motor Driver
    ledcAttach(MOTOR_ENA_PIN, 1000, 8);
    ledcAttach(MOTOR_ENB_PIN, 1000, 8);

    // Servo Setup
    ESP32PWM::allocateTimer(2);
    ESP32PWM::allocateTimer(3);
    armBaseServo.setPeriodHertz(50);
    armShoulderServo.setPeriodHertz(50);
    armElbowServo.setPeriodHertz(50);

    armBaseServo.attach(ARM_BASE_PIN, 500, 2400);
    armShoulderServo.attach(ARM_SHOULDER_PIN, 500, 2400);
    armElbowServo.attach(ARM_ELBOW_PIN, 500, 2400);

    memset(&currentTelemetry, 0, sizeof(TelemetryPayload));
    currentTelemetry.armBaseAngle = 90;
    currentTelemetry.armShoulderAngle = 90;
    currentTelemetry.armElbowAngle = 90;
}

// ----------------------------------------------------------------------------
// MOTOR DRIVER LOGIC
// ----------------------------------------------------------------------------
void setMotorSpeeds(int leftPwm, int rightPwm) {
    if (estopLatched) {
        stopMotors();
        return;
    }

    // Left Motors
    if (leftPwm > 0) {
        digitalWrite(MOTOR_IN1_PIN, HIGH);
        digitalWrite(MOTOR_IN2_PIN, LOW);
        ledcWrite(MOTOR_ENA_PIN, constrain(leftPwm, 0, 255));
    } else if (leftPwm < 0) {
        digitalWrite(MOTOR_IN1_PIN, LOW);
        digitalWrite(MOTOR_IN2_PIN, HIGH);
        ledcWrite(MOTOR_ENA_PIN, constrain(-leftPwm, 0, 255));
    } else {
        digitalWrite(MOTOR_IN1_PIN, LOW);
        digitalWrite(MOTOR_IN2_PIN, LOW);
        ledcWrite(MOTOR_ENA_PIN, 0);
    }

    // Right Motors
    if (rightPwm > 0) {
        digitalWrite(MOTOR_IN3_PIN, HIGH);
        digitalWrite(MOTOR_IN4_PIN, LOW);
        ledcWrite(MOTOR_ENB_PIN, constrain(rightPwm, 0, 255));
    } else if (rightPwm < 0) {
        digitalWrite(MOTOR_IN3_PIN, LOW);
        digitalWrite(MOTOR_IN4_PIN, HIGH);
        ledcWrite(MOTOR_ENB_PIN, constrain(-rightPwm, 0, 255));
    } else {
        digitalWrite(MOTOR_IN3_PIN, LOW);
        digitalWrite(MOTOR_IN4_PIN, LOW);
        ledcWrite(MOTOR_ENB_PIN, 0);
    }
}

void stopMotors() {
    digitalWrite(MOTOR_IN1_PIN, LOW);
    digitalWrite(MOTOR_IN2_PIN, LOW);
    digitalWrite(MOTOR_IN3_PIN, LOW);
    digitalWrite(MOTOR_IN4_PIN, LOW);
    ledcWrite(MOTOR_ENA_PIN, 0);
    ledcWrite(MOTOR_ENB_PIN, 0);
}

// ----------------------------------------------------------------------------
// ACTUATOR TIMEOUT SUPERVISOR
// ----------------------------------------------------------------------------
void updateActuatorTimeouts() {
    unsigned long now = millis();

    // Motor command timeout
    if (now - motorCommandTime > motorWatchdogLimitMs) {
        stopMotors();
    }

    // Cutter max 7s timeout
    if (cutterRunning && (now - cutterStartTime >= (cutterMaxDurationSec * 1000UL))) {
        digitalWrite(CUTTER_PIN, LOW);
        cutterRunning = false;
        currentTelemetry.cutterActive = 0;
    }

    // Chemical pump max 10s timeout
    if (chemPumpRunning && (now - chemPumpStartTime >= (chemPumpMaxDurationSec * 1000UL))) {
        digitalWrite(CHEM_PUMP_PIN, LOW);
        chemPumpRunning = false;
        currentTelemetry.chemPumpActive = 0;
    }

    // Water pump max 5s timeout
    if (waterPumpRunning && (now - waterPumpStartTime >= (waterPumpMaxDurationSec * 1000UL))) {
        digitalWrite(WATER_PUMP_PIN, LOW);
        waterPumpRunning = false;
        currentTelemetry.waterPumpActive = 0;
    }

    // Relay 1 max 30s timeout
    if (relay1Running && (now - relay1StartTime >= 30000UL)) {
        digitalWrite(RELAY_1_PIN, LOW);
        relay1Running = false;
        currentTelemetry.relay1Active = 0;
    }
}

// ----------------------------------------------------------------------------
// SENSOR ACQUISITION
// ----------------------------------------------------------------------------
uint16_t measureUltrasonicMm(uint8_t trig, uint8_t echo) {
    digitalWrite(trig, LOW);
    delayMicroseconds(2);
    digitalWrite(trig, HIGH);
    delayMicroseconds(10);
    digitalWrite(trig, LOW);

    long durationUs = pulseIn(echo, HIGH, 25000); // 25ms timeout (~4.3m max)
    if (durationUs <= 0) return 65535; // Error/Out of range

    // Distance = (duration * speedOfSound 343m/s) / 2
    return (uint16_t)((durationUs * 343) / 2000); // result in mm
}

void readAllSensors() {
    // 1. Front Ultrasonic (HC-SR04)
    uint16_t distMm = measureUltrasonicMm(FRONT_TRIG_PIN, FRONT_ECHO_PIN);
    currentTelemetry.frontUltrasonicMm = distMm;
    // Obstacle detected if distance < 250mm (25 cm)
    currentTelemetry.obstacleDetected = (distMm != 65535 && distMm < 250) ? 1 : 0;

    // 2. Capacitive Soil Moisture Sensor (Analog reading mapped 0..100%)
    int rawSoil = analogRead(SOIL_ANALOG_PIN);
    // Typical calibrated values: 3200 (air/dry) to 1400 (saturated water)
    int moisturePct = map(rawSoil, 3200, 1400, 0, 100);
    currentTelemetry.soilMoisturePct = constrain(moisturePct, 0, 100);

    // 3. DHT11 Climate
    float t = dht.readTemperature();
    float h = dht.readHumidity();
    if (!isnan(t)) currentTelemetry.temperatureC10 = (int16_t)(t * 10);
    if (!isnan(h)) currentTelemetry.humidityPct10 = (uint16_t)(h * 10);

    // 4. Tank Levels (HC-SR04)
    uint16_t waterMm = measureUltrasonicMm(WATER_TRIG_PIN, WATER_ECHO_PIN);
    if (waterMm != 65535) {
        // Tank depth 200mm: 30mm = full (100%), 180mm = empty (0%)
        int waterPct = map(waterMm, 180, 30, 0, 100);
        currentTelemetry.waterTankPct = constrain(waterPct, 0, 100);
    }

    // 5. Battery Voltage Divider (R1 = 30k, R2 = 7.5k -> 1:5 ratio)
    int rawBatt = analogRead(BATTERY_ADC_PIN);
    float pinVolts = (rawBatt / 4095.0) * 3.3;
    float busVolts = pinVolts * 5.0;
    currentTelemetry.batteryMilliVolts = (uint16_t)(busVolts * 1000);

    // Actuator states
    currentTelemetry.cutterActive = cutterRunning ? 1 : 0;
    currentTelemetry.chemPumpActive = chemPumpRunning ? 1 : 0;
    currentTelemetry.waterPumpActive = waterPumpRunning ? 1 : 0;
    currentTelemetry.relay1Active = relay1Running ? 1 : 0;
}

// ----------------------------------------------------------------------------
// UART PROTOCOL HANDLING
// ----------------------------------------------------------------------------
void sendUartPacket(uint8_t msgType, const uint8_t *payload, uint8_t len) {
    if (len > MAX_PAYLOAD_SIZE) return;

    uint8_t packet[MAX_PAYLOAD_SIZE + 7];
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

    MainSerial.write(packet, len + 7);
}

void processUartCommands() {
    static uint8_t rxBuffer[128];
    static uint8_t rxIdx = 0;
    static bool inFrame = false;

    while (MainSerial.available()) {
        uint8_t b = MainSerial.read();

        if (!inFrame) {
            if (b == PROTOCOL_START_BYTE) {
                inFrame = true;
                rxIdx = 0;
                rxBuffer[rxIdx++] = b;
            }
        } else {
            rxBuffer[rxIdx++] = b;

            if (rxIdx >= 6) {
                uint8_t expectedLen = rxBuffer[4];
                uint8_t totalExpected = expectedLen + 7;

                if (rxIdx >= totalExpected) {
                    if (rxBuffer[totalExpected - 1] == PROTOCOL_END_BYTE) {
                        uint8_t calcChk = computeChecksum(&rxBuffer[1], expectedLen + 4);
                        uint8_t rxChk = rxBuffer[5 + expectedLen];

                        if (calcChk == rxChk) {
                            lastUartPacketReceived = millis();
                            uint8_t cmdType = rxBuffer[2];
                            const uint8_t* payload = &rxBuffer[5];

                            // Command Handler
                            switch (cmdType) {
                                case MSG_MOTOR_DRIVE: {
                                    if (expectedLen == sizeof(MotorDrivePayload) && !estopLatched) {
                                        MotorDrivePayload p;
                                        memcpy(&p, payload, sizeof(p));
                                        int pwm = (p.speed == 3) ? 240 : (p.speed == 2 ? 185 : 130);
                                        motorCommandTime = millis();
                                        motorWatchdogLimitMs = p.timeoutMs > 0 ? p.timeoutMs : 600;

                                        // Check front obstacle safety before driving forward
                                        if (p.direction == 1 && currentTelemetry.obstacleDetected == 1) {
                                            stopMotors();
                                        } else if (p.direction == 1) { // FWD
                                            setMotorSpeeds(pwm, pwm);
                                        } else if (p.direction == 2) { // REV
                                            setMotorSpeeds(-pwm, -pwm);
                                        } else if (p.direction == 3) { // LEFT
                                            setMotorSpeeds(-pwm, pwm);
                                        } else if (p.direction == 4) { // RIGHT
                                            setMotorSpeeds(pwm, -pwm);
                                        } else {
                                            stopMotors();
                                        }
                                    }
                                    break;
                                }

                                case MSG_ARM_MOVE: {
                                    if (expectedLen == sizeof(ArmMovePayload)) {
                                        ArmMovePayload p;
                                        memcpy(&p, payload, sizeof(p));
                                        armBaseServo.write(p.baseAngle);
                                        armShoulderServo.write(p.shoulderAngle);
                                        armElbowServo.write(p.elbowAngle);
                                        currentTelemetry.armBaseAngle = p.baseAngle;
                                        currentTelemetry.armShoulderAngle = p.shoulderAngle;
                                        currentTelemetry.armElbowAngle = p.elbowAngle;
                                    }
                                    break;
                                }

                                case MSG_CUTTER_TIMED: {
                                    if (expectedLen == sizeof(TimedActuatorPayload) && !estopLatched) {
                                        TimedActuatorPayload p;
                                        memcpy(&p, payload, sizeof(p));
                                        if (p.state == 1) {
                                            cutterMaxDurationSec = min((unsigned int)p.durationSec, 7U);
                                            cutterStartTime = millis();
                                            digitalWrite(CUTTER_PIN, HIGH);
                                            cutterRunning = true;
                                        } else {
                                            digitalWrite(CUTTER_PIN, LOW);
                                            cutterRunning = false;
                                        }
                                    }
                                    break;
                                }

                                case MSG_CHEM_PUMP_TIMED: {
                                    if (expectedLen == sizeof(TimedActuatorPayload) && !estopLatched) {
                                        TimedActuatorPayload p;
                                        memcpy(&p, payload, sizeof(p));
                                        if (p.state == 1) {
                                            chemPumpMaxDurationSec = min((unsigned int)p.durationSec, 10U);
                                            chemPumpStartTime = millis();
                                            digitalWrite(CHEM_PUMP_PIN, HIGH);
                                            chemPumpRunning = true;
                                        } else {
                                            digitalWrite(CHEM_PUMP_PIN, LOW);
                                            chemPumpRunning = false;
                                        }
                                    }
                                    break;
                                }

                                case MSG_WATER_PUMP_TIMED: {
                                    if (expectedLen == sizeof(TimedActuatorPayload) && !estopLatched) {
                                        TimedActuatorPayload p;
                                        memcpy(&p, payload, sizeof(p));
                                        if (p.state == 1) {
                                            waterPumpMaxDurationSec = min((unsigned int)p.durationSec, 5U);
                                            waterPumpStartTime = millis();
                                            digitalWrite(WATER_PUMP_PIN, HIGH);
                                            waterPumpRunning = true;
                                        } else {
                                            digitalWrite(WATER_PUMP_PIN, LOW);
                                            waterPumpRunning = false;
                                        }
                                    }
                                    break;
                                }

                                case MSG_RELAY_CONTROL: {
                                    if (expectedLen == sizeof(TimedActuatorPayload)) {
                                        TimedActuatorPayload p;
                                        memcpy(&p, payload, sizeof(p));
                                        if (p.state == 1) {
                                            digitalWrite(RELAY_1_PIN, HIGH);
                                            relay1Running = true;
                                            relay1StartTime = millis();
                                        } else {
                                            digitalWrite(RELAY_1_PIN, LOW);
                                            relay1Running = false;
                                        }
                                    }
                                    break;
                                }

                                case MSG_ESTOP_TRIGGER: {
                                    estopLatched = true;
                                    stopMotors();
                                    digitalWrite(CUTTER_PIN, LOW); cutterRunning = false;
                                    digitalWrite(CHEM_PUMP_PIN, LOW); chemPumpRunning = false;
                                    digitalWrite(WATER_PUMP_PIN, LOW); waterPumpRunning = false;
                                    break;
                                }

                                case MSG_ESTOP_RESET: {
                                    estopLatched = false;
                                    break;
                                }

                                default:
                                    break;
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
