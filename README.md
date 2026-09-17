# Pinion ⚙️🪶
### Universal Wireless Print Engine for Android

Turn any rooted Android device into a standalone network print server with native support for **Apple AirPrint**, **Android Mopria / Default Print Service**, **Windows IPP**, and **Linux CUPS**. Engineered for USB printers across standard PostScript, PCL, and host-based GDI devices.

Developed by [killindodo](https://github.com/killindodo).

---

## Overview

Pinion bridges USB OTG printers directly to your local wireless network using an embedded CUPS and Avahi stack running within a contained Linux userspace on Android. It provides a clean, native Android control app and a responsive web dashboard for direct browser printing.

### Key Capabilities
- **Universal Protocol Support**: Zero-configuration discovery via Avahi mDNS (`_ipp._tcp`, `_printer._tcp`, `_pdl-datastream._tcp`).
- **GDI & Host-Based Engine**: Native driver pipeline with Foomatic `foo2xqx`/`foo2zjs` and HPLIP.
- **Native Android Controller**: Fast, lightweight (<160 KB) native app with live status, interface IP detection, service supervisor, and 5 cyber/terminal themes.
- **Web Dashboard (:8080)**: Direct browser drag-and-drop document & photo printing via `lp` without client-side driver installations.
- **Tablet & Phone Optimized**: Dynamic responsive layout supporting both phone portrait and tablet multi-column views.

---

## Quick Start & Engine Setup

Pinion requires root access and the background print engine (CUPS, Avahi, Python 3, and printer drivers).

### Option 1: In-App 1-Click Install (Recommended)
1. Install and launch **PrintServer-Root.apk** on any rooted Android device.
2. The app detects that the engine is missing and displays **SETUP NEEDED**.
3. Tap **⚡ Install Print Engine (Magisk Module)**.
4. Pinion will download `pinion-core.zip` directly from GitHub releases and flash it via Magisk root automatically.
5. Plug your printer into USB OTG and tap **▶ START**!

### Option 2: Flash Magisk Module Manually
1. Download `pinion-core.zip` from the latest [GitHub Releases](https://github.com/killindodo/PrintServer-App/releases).
2. Open the **Magisk** (or KernelSU / APatch) app.
3. Go to **Modules** > **Install from storage** and select `pinion-core.zip`.
4. Launch Pinion and tap **▶ START**.

### Option 3: Termux Debian Fallback
If you prefer running inside a user-managed Termux container:
```bash
pkg update && pkg install -y proot-distro
proot-distro install debian
proot-distro login debian -- apt update
proot-distro login debian -- apt install -y cups cups-client cups-filters avahi-daemon python3 printer-driver-foo2zjs hplip
```
Pinion automatically detects the Termux container and starts seamlessly.

---

## Client Connection Guide

No passwords or client drivers required. Ensure your client device is connected to the same Wi-Fi network.

### Windows 10 / 11
1. Navigate to **Settings > Bluetooth & devices > Printers & scanners**.
2. Click **Add device**. Windows will discover the printer automatically via mDNS.
3. If connecting manually, select **The printer that I want isn't listed** > **Select a shared printer by name**:
   ```text
   http://<DEVICE_IP>:631/printers/<PRINTER_NAME>
   ```
4. Choose the appropriate driver or generic IPP / MS Publisher Imagesetter if prompted.

### Android
1. Open any document, photo, or web page.
2. Tap **Share > Print**.
3. The printer appears automatically under **Default Print Service** or **Mopria Print Service**.

### iOS / macOS (AirPrint)
1. Tap **Share > Print** on any iPhone, iPad, or Mac.
2. The printer appears immediately via Bonjour / AirPrint.

### Linux
Submit jobs directly through the command line or desktop printer settings:
```bash
lp -h <DEVICE_IP>:631 -d <PRINTER_NAME> document.pdf
```

### Browser Direct Print (:8080)
Open `http://<DEVICE_IP>:8080` in any web browser on the network. Drag and drop any PDF or image to print instantly.

---

## Architecture & Server Daemons

Pinion coordinates the following services under root:
- **CUPS (`cupsd`)**: Core print scheduler and IPP 2.0 implementation.
- **Avahi (`avahi-daemon`)**: Multicast DNS responder announcing AirPrint service records.
- **Web UI (`server/printserver-webui.py`)**: Lightweight Python HTTP server managing browser uploads and CUPS queue status.
- **Supervisor Script (`start-printserver.sh`)**: Automates USB permissions, devnode links, and daemon lifecycles.

---

## Build Instructions

### Requirements
- Android SDK Build-Tools (v30+)
- JDK 17+
- Bash & Zip utilities

### Build APK
```bash
chmod +x build.sh
./build.sh
```
The aligned and signed production APK will be generated at:
```text
build/out/PrintServer-Root.apk
```

---

## License & Author
Developed and maintained by [killindodo](https://github.com/killindodo).  
Released under the MIT License.
