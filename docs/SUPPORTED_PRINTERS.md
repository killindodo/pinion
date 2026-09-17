# Pinion Supported Printers & Driver Matrix 🖨️📑

Pinion's embedded Linux print stack includes comprehensive driver engines designed to support both modern network-ready printers and legacy host-based USB desktop printers.

---

## 1. Supported Printer Classes

### 1.1 Standard PostScript & PCL Printers
Any printer supporting standard PostScript (Level 2/3) or Hewlett-Packard PCL (PCL 5e, PCL 6 / PCL-XL) works out of the box with zero proprietary binary blobs:
- **HP**: LaserJet Pro, Enterprise, OfficeJet Pro series.
- **Brother**: HL-L, MFC-L laser printer series.
- **Canon**: imageRUNNER, imageCLASS series (PCL/PS models).
- **Epson**: WorkForce Pro, EcoTank Pro (PCL/PS supported models).
- **Ricoh, Xerox, Lexmark, Kyocera**: All network and office laser printers.

### 1.2 Host-Based / GDI Printers (Engineered Support)
Many affordable USB desktop laser printers rely on host-based proprietary rasterization (GDI) rather than onboard page processing. Pinion includes dedicated open-source driver pipelines:

| Printer Model | Protocol / Driver Engine | Driver Package | Firmware Download Needed? |
|---|---|---|---|
| **HP LaserJet 1000** | ZjStream (`foo2zjs`) | `printer-driver-foo2zjs` | Yes (`sihp1000.dl`) |
| **HP LaserJet 1005** | ZjStream (`foo2zjs`) | `printer-driver-foo2zjs` | Yes (`sihp1005.dl`) |
| **HP LaserJet 1018** | ZjStream (`foo2zjs`) | `printer-driver-foo2zjs` | Yes (`sihp1018.dl`) |
| **HP LaserJet 1020** | ZjStream (`foo2zjs`) | `printer-driver-foo2zjs` | Yes (`sihp1020.dl`) |
| **HP LaserJet M1005 MFP** | XQX Stream (`foo2xqx`) | `printer-driver-foo2zjs` | No |
| **HP LaserJet P1005 / P1006 / P1505** | HPLIP (`hp-plugin`) | `hplip` | Recommended |
| **Canon LBP-2900 / 3000** | CAPT (`capt-driver`) | `capt-src` | Optional |
| **Samsung ML-1210 / 1710** | SPL (`splix`) | `printer-driver-splix` | No |
| **Minolta / QMS magicolor** | ZjStream (`foo2zjs`) | `printer-driver-foo2zjs` | No |

### 1.3 ESC/POS Thermal Receipt Printers
Pinion supports standard 58mm and 80mm USB POS thermal receipt printers via raw AppSocket streaming (:9100) or generic text backends.

---

## 2. Setting Up Firmware for Host-Based Printers (HP LaserJet 1000/1005/1018/1020)

Certain host-based printers require firmware upload over USB each time they power on.

If using an HP LaserJet 1000, 1005, 1018, or 1020:
1. Obtain the firmware file (e.g. `sihp1020.dl`).
2. Transfer the file to your device's rootfs:
   ```bash
   su
   cp sihp1020.dl /data/adb/pinion/rootfs/usr/share/foo2zjs/firmware/
   ```
3. Whenever the printer is connected, the udev/hotplug rule or `arm2hpdl` will load firmware directly to the printer over the USB bus:
   ```bash
   cat /usr/share/foo2zjs/firmware/sihp1020.dl > /dev/usb/lp0
   ```

---

## 3. Detecting Connected Printers

To see how Pinion and CUPS identify your connected hardware:

```bash
# In an adb shell or terminal with root:
su

# Check if the kernel recognizes the USB peripheral
lsusb

# Check CUPS hardware discovery backends
chroot /data/adb/pinion/rootfs lpinfo -v
```

Typical output:
```text
direct usb://HP/LaserJet%20M1005?serial=J00XXXXX
```
