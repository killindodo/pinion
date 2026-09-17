#!/system/bin/sh
# Pinion Late-Start Service
MODDIR=${0%/*}

# Wait until boot is fully completed
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 3
done

# If autostart flag file exists, start print server on boot
if [ -f "/data/adb/pinion/autostart" ]; then
    if [ -x "/data/adb/pinion/bin/start-printserver.sh" ]; then
        /data/adb/pinion/bin/start-printserver.sh >/dev/null 2>&1 &
    fi
fi
