SKIPUNZIP=0

ui_print "****************************************"
ui_print "*        Pinion Print Engine           *"
ui_print "*       Universal Print Server         *"
ui_print "*        by github.com/killindodo      *"
ui_print "****************************************"

ROOTFS_TARGET="/data/adb/pinion/rootfs"
BIN_TARGET="/data/adb/pinion/bin"

ui_print "- Preparing target directories..."
mkdir -p "$ROOTFS_TARGET"
mkdir -p "$BIN_TARGET"
mkdir -p "$MODPATH/system/bin"

if [ -f "$MODPATH/rootfs.tar.xz" ]; then
    ui_print "- Extracting Print Engine rootfs..."
    tar -xJf "$MODPATH/rootfs.tar.xz" -C "$ROOTFS_TARGET"
    rm -f "$MODPATH/rootfs.tar.xz"
elif [ -f "$MODPATH/rootfs.tar.gz" ]; then
    ui_print "- Extracting Print Engine rootfs..."
    tar -xzf "$MODPATH/rootfs.tar.gz" -C "$ROOTFS_TARGET"
    rm -f "$MODPATH/rootfs.tar.gz"
fi

ui_print "- Installing start scripts..."
if [ -f "$MODPATH/start-printserver.sh" ]; then
    cp "$MODPATH/start-printserver.sh" "$BIN_TARGET/start-printserver.sh"
    chmod 755 "$BIN_TARGET/start-printserver.sh"
    
    # System binary overlay
    cp "$MODPATH/start-printserver.sh" "$MODPATH/system/bin/start-printserver"
    chmod 755 "$MODPATH/system/bin/start-printserver"
fi

if [ -f "$MODPATH/printserver-webui.py" ]; then
    mkdir -p "$ROOTFS_TARGET/usr/local/bin"
    cp "$MODPATH/printserver-webui.py" "$ROOTFS_TARGET/usr/local/bin/printserver-webui.py"
    chmod 755 "$ROOTFS_TARGET/usr/local/bin/printserver-webui.py"
fi

# Ensure required runtime mountpoints exist
mkdir -p "$ROOTFS_TARGET/proc" "$ROOTFS_TARGET/sys" "$ROOTFS_TARGET/dev" "$ROOTFS_TARGET/dev/pts"
mkdir -p "$ROOTFS_TARGET/run/dbus" "$ROOTFS_TARGET/run/cups" "$ROOTFS_TARGET/tmp" "$ROOTFS_TARGET/var/spool/cups"
chmod 1777 "$ROOTFS_TARGET/tmp" 2>/dev/null || true

ui_print "- Pinion Engine installation complete!"
