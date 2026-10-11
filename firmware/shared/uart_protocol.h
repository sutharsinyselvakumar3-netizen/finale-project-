/**
 * ============================================================================
 * AI COMPANION — Smart Onion Garden Maintainer Robot
 * Shared UART Protocol Definition
 * ============================================================================
 * Defines the binary packet structure, message IDs, framing bytes, and
 * CRC checksum computation for high-reliability UART communication between
 * ESP32 #1 (Main Controller) and ESP32 #2 (Hardware Controller).
 *
 * Baud Rate: 115200 bps
 * Framing: START_BYTE (0xAA), PROTOCOL_VERSION (0x01), MSG_TYPE, SEQ_NUM,
 *          PAYLOAD_LENGTH, PAYLOAD [0..64], CHECKSUM (XOR), END_BYTE (0x55)
 * ============================================================================
 */

#ifndef UART_PROTOCOL_H
#define UART_PROTOCOL_H

#include <stdint.h>
#include <stdbool.h>

#define PROTOCOL_START_BYTE     0xAA
#define PROTOCOL_END_BYTE       0x55
#define PROTOCOL_VERSION        0x01
#define MAX_PAYLOAD_SIZE        64
#define UART_HEARTBEAT_TIMEOUT_MS 2000

// Message Types (ESP32 #1 -> ESP32 #2)
typedef enum {
    MSG_PING                = 0x01,
    MSG_ESTOP_TRIGGER       = 0x02,
    MSG_ESTOP_RESET         = 0x03,
    MSG_MOTOR_DRIVE         = 0x10,
    MSG_ARM_MOVE            = 0x20,
    MSG_CUTTER_TIMED        = 0x30,
    MSG_CHEM_PUMP_TIMED     = 0x31,
    MSG_WATER_PUMP_TIMED    = 0x32,
    MSG_RELAY_CONTROL       = 0x33,
    MSG_READ_SENSORS        = 0x40,
    MSG_READ_COLOR_TCS      = 0x41
} UartCommandType;

// Response / Telemetry Types (ESP32 #2 -> ESP32 #1)
typedef enum {
    MSG_ACK                 = 0x80,
    MSG_NACK                = 0x81,
    MSG_PONG                = 0x82,
    MSG_TELEMETRY_PACKET    = 0x90,
    MSG_FAULT_ALERT         = 0x99
} UartResponseType;

// Structured Telemetry Payload returned by ESP32 #2
typedef struct __attribute__((packed)) {
    uint16_t frontUltrasonicMm;  // HC-SR04 front distance in mm (65535 = error)
    uint8_t  obstacleDetected;   // 1 = obstacle closer than safety threshold
    uint8_t  soilMoisturePct;    // Capacitive soil moisture % (0..100)
    int16_t  temperatureC10;     // Temperature * 10 (e.g. 254 = 25.4 deg C)
    uint16_t humidityPct10;      // Humidity * 10 (e.g. 620 = 62.0 % RH)
    uint8_t  waterTankPct;       // Water tank level %
    uint8_t  chemicalTankPct;    // Chemical tank level %
    uint16_t tcsRed;             // TCS230 raw red frequency
    uint16_t tcsGreen;           // TCS230 raw green frequency
    uint16_t tcsBlue;            // TCS230 raw blue frequency
    uint16_t tcsClear;           // TCS230 clear intensity
    uint8_t  armBaseAngle;       // 0..180
    uint8_t  armShoulderAngle;   // 0..180
    uint8_t  armElbowAngle;      // 0..180
    uint8_t  cutterActive;       // 0 or 1
    uint8_t  chemPumpActive;     // 0 or 1
    uint8_t  waterPumpActive;    // 0 or 1
    uint8_t  relay1Active;       // 0 or 1
    uint16_t batteryMilliVolts;  // Voltage in mV (e.g. 12400 = 12.4V)
    uint8_t  hwStatusFlags;      // Bit 0: Estop active, Bit 1: Motor fault, Bit 2: Sensor fault
} TelemetryPayload;

// Motor Drive Payload
typedef struct __attribute__((packed)) {
    uint8_t direction; // 0: STOP, 1: FWD, 2: REV, 3: LEFT, 4: RIGHT
    uint8_t speed;     // 1: SLOW (PWM 130), 2: MED (PWM 185), 3: FAST (PWM 240)
    uint16_t timeoutMs;// Local controller watchdog timeout (e.g. 500ms)
} MotorDrivePayload;

// Arm Position Payload
typedef struct __attribute__((packed)) {
    uint8_t baseAngle;     // 15..165 deg
    uint8_t shoulderAngle; // 20..160 deg
    uint8_t elbowAngle;    // 20..160 deg
} ArmMovePayload;

// Timed Actuator Payload
typedef struct __attribute__((packed)) {
    uint8_t state;         // 1 = ON, 0 = OFF
    uint16_t durationSec;  // Hardware enforced max runtime timeout in seconds
} TimedActuatorPayload;

// Helper XOR Checksum
static inline uint8_t computeChecksum(const uint8_t *data, uint8_t len) {
    uint8_t checksum = 0;
    for (uint8_t i = 0; i < len; i++) {
        checksum ^= data[i];
    }
    return checksum;
}

#endif // UART_PROTOCOL_H
