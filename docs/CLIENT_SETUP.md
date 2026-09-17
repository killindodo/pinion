# Pinion Client Connection Guide 🖨️💻📱

Pinion turns your Android phone into an enterprise-grade wireless print server. All clients connected to the same local Wi-Fi network can discover and print to your USB printer without installing custom drivers.

---

## 1. Quick Reference: Connection Parameters

| Parameter | Default Value | Notes |
|---|---|---|
| **CUPS IPP Port** | `631` | Universal print protocol port |
| **Web UI Port** | `8080` | Browser drag-and-drop dashboard |
| **Direct IPP URI** | `http://<DEVICE_IP>:631/printers/<PRINTER_NAME>` | Standard IPP queue URL |
| **Raw AppSocket Port** | `9100` | JetDirect / RAW print stream |
| **Discovery** | mDNS / Bonjour / Avahi | `_ipp._tcp`, `_printer._tcp` |

> [!TIP]
> Find your `<DEVICE_IP>` directly on the main screen of the Pinion Android app or Web UI header (e.g., `192.168.1.77`).

---

## 2. Platform Setup Guides

### 2.1 Windows 10 & 11

#### Automatic Discovery (mDNS)
1. Open **Settings** -> **Bluetooth & devices** -> **Printers & scanners**.
2. Click **Add device**.
3. Windows will scan your local network using mDNS and display your printer name.
4. Click **Add device** and wait for Windows to configure the IPP driver.

#### Manual Configuration (Recommended if auto-discovery is delayed)
1. In **Printers & scanners**, click **The printer that I want isn't listed**.
2. Select **Select a shared printer by name**.
3. Enter the IPP address:
   ```text
   http://<DEVICE_IP>:631/printers/<PRINTER_NAME>
   ```
   *(Example: `http://192.168.1.77:631/printers/HP_LaserJet_M1005`)*
4. When prompted for driver:
   - Choose **Generic** -> **MS Publisher Imagesetter** or **Generic / Text Only** for PostScript printers.
   - Or choose your printer manufacturer's standard Windows driver.

---

### 2.2 Apple iOS & iPadOS (AirPrint)

Zero setup required:
1. Ensure your iPhone or iPad is connected to the same Wi-Fi network as your Pinion device.
2. Open any document, photo, email, or Safari page.
3. Tap the **Share** button (or action menu) and select **Print**.
4. Tap **Select Printer**. Your printer will be automatically listed under **AirPrint Printers**.
5. Adjust page range/copies and tap **Print**.

---

### 2.3 Apple macOS

1. Open **System Settings** (or System Preferences) -> **Printers & Scanners**.
2. Click **Add Printer, Scanner, or Fax...** (+ button).
3. Under the **Default** tab, your printer will be detected as a **Bonjour** or **AirPrint** device.
4. Select the printer. macOS will automatically set the **Use** field to **AirPrint** or **Generic PostScript Printer**.
5. Click **Add**.

---

### 2.4 Android Phones & Tablets

Pinion integrates natively with Android's built-in print framework:
1. Open **Settings** -> **Connected devices** -> **Connection preferences** -> **Printing**.
2. Ensure **Default Print Service** is enabled (or install **Mopria Print Service** from the Play Store if your ROM disables mDNS).
3. The printer will appear automatically in the list.
4. Print from Chrome, Gallery, Google Drive, or any app using **Share > Print**.

---

### 2.5 Linux Workstations (Ubuntu, Debian, Fedora, Arch)

#### Desktop GUI
1. Open your desktop's printer configuration utility (e.g. GNOME Settings -> Printers).
2. Click **Add Printer**. Network printers are discovered automatically via Avahi/mDNS.
3. Select the printer and choose **IPP Everywhere** or standard PPD.

#### Command Line Printing (`lp` / `lpr`)
You can print directly to Pinion without configuring a local queue:
```bash
# Print a PDF or PostScript document
lp -h <DEVICE_IP>:631 -d <PRINTER_NAME> document.pdf

# Check queue status remotely
lpstat -h <DEVICE_IP>:631 -o
```

---

### 2.6 Browser Direct Printing (Any Device)

No drivers or printer setup needed on the client at all:
1. Open any web browser on your phone, tablet, or computer.
2. Navigate to:
   ```text
   http://<DEVICE_IP>:8080
   ```
3. Drag and drop any `.pdf`, `.ps`, `.txt`, `.png`, or `.jpg` file into the dropzone.
4. Set copies and click **Print Document**.
5. Monitor live print queue telemetry and cancel jobs in real-time.

---

## 3. Network Troubleshooting

### Printer Not Discovered via mDNS / AirPrint
- **Wi-Fi AP Isolation**: Check your Wi-Fi router settings to ensure "Client Isolation" or "AP Isolation" is disabled. This setting prevents Wi-Fi devices from discovering one another.
- **Multicast / IGMP Snooping**: Ensure your router allows Multicast traffic (IGMP snooping should be enabled).
- **Same Subnet**: Verify both devices are connected to the same subnet (e.g. both are on `192.168.1.x`).
