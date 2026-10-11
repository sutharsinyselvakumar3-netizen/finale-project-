# Network Configuration & Topology

## 1. Network Topologies Supported

### Mode A: Robot SoftAP (Default Standalone Field Mode)
No external router required in the onion garden.
* **ESP32 #1** creates an encrypted Access Point:
  * **SSID:** `AI_COMPANION_ROBOT`
  * **Password:** `OnionProtect2026`
  * **Gateway / Main Controller IP:** `192.168.4.1`
* **ESP32-CAM** connects as a Wi-Fi client to `AI_COMPANION_ROBOT`:
  * Assigned static or DHCP IP (typically `192.168.4.2`).
* **Android Tablet / Smartphone** connects to Wi-Fi `AI_COMPANION_ROBOT`:
  * Assigned IP `192.168.4.x`.

### Mode B: Infrastructure Farm Wi-Fi (Router Mode)
When operating within farm Wi-Fi coverage:
1. Configure both ESP32 #1 and ESP32-CAM with your farm router's SSID and WPA2 key.
2. Reserve static DHCP IP leases on your router:
   * Main ESP32: `192.168.1.150`
   * ESP32-CAM: `192.168.1.151`
3. Enter these IPs directly into the app's **SETTINGS** tab and tap **Save Network Settings**.

---

## 2. Port Allocations
| Device | Service | Protocol | Port | Path |
|---|---|---|---|---|
| ESP32 #1 | REST API Server | HTTP | 80 | `/api/*` |
| ESP32-CAM | Capture Server | HTTP | 80 | `/capture` |
| ESP32-CAM | MJPEG Stream Server | HTTP | 81 | `/stream` |
