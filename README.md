# PrintServer Root 🖨️

A wireless network print server for Android that turns any rooted Android device into an AirPrint, Mopria, and IPP print server for USB printers (including host-based GDI printers like the HP LaserJet M1005 MFP).

Made with ❤️ by [killindodo](https://www.github.com/killindodo).

---

## ✨ Features

- **Universal Driver Support:** Powered by CUPS, Foomatic `foo2xqx`/`foo2zjs`, and HPLIP.
- **Zero-Config Wireless Printing:** Built-in Avahi mDNS / AirPrint / Mopria daemon.
- **Root Controlled GUI:** One-tap Start, Stop, Restart, and Test Print directly from your phone.
- **Live Device Monitoring:** Shows real-time server status, Wi-Fi IP address, and detected USB printer.
- **Web Administration Dashboard:** Quick access to the full CUPS web manager on port 631.
- **Ultra Lightweight:** Clean native Android app (< 40 KB) with no unnecessary background bloat.

---

## 💻 Client Usage Guide

### 🪟 Windows 10 / 11
1. Open **Settings > Bluetooth & devices > Printers & scanners**.
2. Click **Add printer or scanner**.
3. Windows will automatically find the printer via mDNS (`HP LaserJet M1005 @ linux.local`).
4. Alternatively, click *The printer that I want isn't listed* and select **Select a shared printer by name**:
   ```
   http://<PHONE_IP>:631/printers/HP_LaserJet_M1005
   ```

### 📱 Android
1. Open any photo, document, or PDF.
2. Tap **Share** or **Print**.
3. Select **HP LaserJet M1005** directly from the discovered printer list (via Android Default Print Service or Mopria).

### 🍎 iOS / macOS (AirPrint)
1. Ensure your iPhone, iPad, or Mac is connected to the same Wi-Fi network.
2. Tap **Share > Print**.
3. The printer appears natively under **AirPrint** with zero drivers or configuration needed.

### 🐧 Linux
Print directly from the command line:
```bash
lp -h <PHONE_IP>:631 -d HP_LaserJet_M1005 document.pdf
```
Or add the IPP printer via your desktop's system printer settings.

---

## 🛠️ Building From Source

Prerequisites:
- Android SDK Build-Tools (30+)
- JDK 17+ / OpenJDK

Run the automated build script:
```bash
chmod +x build.sh
./build.sh
```
The compiled, aligned, and signed APK will be output to:
`build/out/PrintServer-Root.apk`

---

## 📜 Credits & License
Created and maintained by [killindodo](https://www.github.com/killindodo).
Licensed under the MIT License.
