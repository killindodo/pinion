# Pinion Print Engine — Magisk & KernelSU Integration Guide ⚙️

This guide covers building, flashing, configuring, and maintaining the **Pinion Core Print Engine** Magisk/KernelSU/APatch module for Android.

---

## 1. Overview

The Pinion Android app is a native frontend supervisor, but printing requires a full Linux userspace stack containing:
- **CUPS 2.4+** (`cupsd`, `lp`, `lpstat`, `lpinfo`, `cancel`)
- **Avahi Daemon** (`avahi-daemon`, mDNS zero-conf network publisher)
- **Python 3** (lightweight Web UI dashboard on `:8080`)
- **Printer Drivers & Filter Stack** (Foomatic, `foo2zjs`, `foo2xqx`, HPLIP, `ghostscript`)

The **Pinion Core Magisk Module** installs this self-contained rootfs under `/data/adb/pinion/rootfs` and provides system binary aliases without polluting the Android `/system` partition (utilizing Magisk's overlayfs/magic mount).

---

## 2. Module Directory Structure

```text
pinion-core.zip
├── module.prop                # Magisk module metadata & version
├── customize.sh               # Post-flash extraction & permissions setup
├── service.sh                 # Early boot trigger (optional auto-start)
├── start-printserver.sh       # Root supervisor and mount manager
├── printserver-webui.py       # Web UI server with REST API & dropzone
└── rootfs.tar.xz              # (Optional) Compressed Linux chroot rootfs
```

### Installation Targets on Device
| Source Component | Android System Destination | Permissions | Purpose |
|---|---|---|---|
| `rootfs` | `/data/adb/pinion/rootfs/` | `0755` | Complete Debian/Alpine printer rootfs |
| `start-printserver.sh` | `/data/adb/pinion/bin/start-printserver.sh` | `0755` | Chroot mount & daemon supervisor |
| `start-printserver` | `/system/bin/start-printserver` (Overlay) | `0755` | Direct terminal invocation |
| `printserver-webui.py` | `/data/adb/pinion/rootfs/usr/local/bin/` | `0755` | Web dashboard & drag-and-drop handler |

---

## 3. Installation Methods

### Method A: 1-Click In-App Flashing (Recommended)
1. Install **Pinion** (`PrintServer-Root.apk`).
2. Grant Root permission when prompted (Magisk / KernelSU / APatch).
3. If the print engine is not yet installed, Pinion will show the **SETUP NEEDED** badge.
4. Tap **⚡ Install Print Engine (Magisk Module)**.
5. Pinion automatically downloads `pinion-core.zip` from GitHub Releases and executes `magisk --install-module` in the background.
6. Once completed, plug in your printer and tap **▶ START**.

### Method B: Manual Magisk / KernelSU Flashing
1. Download `pinion-core.zip` from [Pinion Releases](https://github.com/killindodo/pinion/releases).
2. Open the **Magisk Manager** or **KernelSU** app.
3. Navigate to **Modules** -> **Install from storage**.
4. Select `pinion-core.zip`.
5. Once installation finishes, reboot (or start immediately via the Pinion app).

### Method C: Standalone Rootfs Extraction (No Reboot Needed)
If you don't use Magisk module mounting, you can extract the rootfs directly into `/data/local/pinion/rootfs`:
```bash
su
mkdir -p /data/local/pinion/rootfs
tar -xJf pinion-core-rootfs.tar.xz -C /data/local/pinion/rootfs/
cp start-printserver.sh /data/local/pinion/
chmod +x /data/local/pinion/start-printserver.sh
```

---

## 4. Building the Module from Source

To package the Magisk module locally:

```bash
# 1. Clone the repository
git clone https://github.com/killindodo/pinion.git
cd pinion

# 2. Build the module package
cd magisk
chmod +x build-module.sh
./build-module.sh
```

The compiled module will be generated at:
```text
build/out/pinion-core.zip
```

### Customizing the Embedded Rootfs
If you want to bundle a pre-configured Debian or Alpine arm64 rootfs directly inside the zip, place the compressed archive in `magisk/rootfs.tar.xz` before running `./build-module.sh`. The builder will automatically detect and pack it.

---

## 5. Chroot Mounts & USB Devnode Permissions

Android applies strict SELinux policies and restrictive permissions on USB character devices under `/dev/bus/usb/*`. The `start-printserver.sh` script automates these steps upon startup:

1. **Wake Lock Acquisition**:
   Writes `pinion_printserver` to `/sys/power/wake_lock` to ensure the device CPU does not enter deep sleep while serving print jobs.
2. **Dynamic USB Devnode Chmod**:
   Scans `/dev/bus/usb/` and grants `0666` read/write access to allow CUPS USB backends (`usb://`) to communicate with the printer.
3. **Chroot Pseudofs Mounting**:
   - `/proc` -> `$ROOTFS/proc`
   - `/sys` -> `$ROOTFS/sys`
   - `/dev` -> `$ROOTFS/dev` (bind mount)
   - `/dev/pts` -> `$ROOTFS/dev/pts`
4. **Daemon Launch Sequence**:
   - D-Bus system bus
   - Avahi mDNS daemon (with readiness verification loop)
   - CUPS print scheduler (`cupsd`)
   - Python 3 Web UI server (`0.0.0.0:8080`)

---

## 6. Troubleshooting

### Module Fails to Install in Magisk
- Check that your Magisk version is v24.0 or newer.
- Verify that your `/data` partition has at least 300MB of free space for the Linux rootfs.

### Printer Shows "USB Disconnected"
- Ensure your Android device supports **USB Host Mode (OTG)**.
- Use a powered USB OTG hub or Y-cable if your printer requires higher power negotiation.
- Verify with `lsusb` in a root shell:
  ```bash
  su -c lsusb
  ```

### CUPS Daemon Does Not Start
Check the CUPS error log inside the rootfs:
```bash
su -c "cat /data/adb/pinion/rootfs/var/log/cups/error_log"
```
Or check the Web UI server log:
```bash
su -c "cat /data/local/tmp/webui.log"
```
