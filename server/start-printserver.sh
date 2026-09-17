#!/system/bin/sh
# Pinion - Universal Wireless Print Engine (start-printserver.sh)
ROOTFS="/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs"
echo "printserver" > /sys/power/wake_lock 2>/dev/null

chmod -R 666 /dev/bus/usb 2>/dev/null
chmod -R 666 /dev/usb 2>/dev/null

mount -o remount,exec /data 2>/dev/null
mountpoint -q $ROOTFS/proc || mount -t proc proc $ROOTFS/proc
mountpoint -q $ROOTFS/sys  || mount -t sysfs sysfs $ROOTFS/sys
mountpoint -q $ROOTFS/dev  || mount -o bind /dev $ROOTFS/dev
mountpoint -q $ROOTFS/dev/pts || mount -t devpts devpts $ROOTFS/dev/pts

echo "nameserver 1.1.1.1" > $ROOTFS/etc/resolv.conf
echo "nameserver 8.8.8.8" >> $ROOTFS/etc/resolv.conf

mkdir -p $ROOTFS/run/dbus $ROOTFS/run/cups $ROOTFS/tmp $ROOTFS/data/local/tmp
chmod 1777 $ROOTFS/tmp $ROOTFS/data/local/tmp

killall cupsd avahi-daemon dbus-daemon 2>/dev/null
pkill -f printserver-webui.py 2>/dev/null
rm -f $ROOTFS/run/dbus/pid $ROOTFS/run/cups/cups.sock $ROOTFS/run/dbus/system_bus_socket $ROOTFS/run/avahi-daemon/pid

chroot $ROOTFS /bin/bash -c "
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export TMPDIR=/tmp
export HOME=/root
/usr/bin/dbus-daemon --system >/dev/null 2>&1
avahi-daemon -D >/dev/null 2>&1
cupsd >/dev/null 2>&1
" </dev/null >/dev/null 2>&1
# Ensure daemons are active: chroot cupsd
chroot $ROOTFS /usr/sbin/cupsd 2>/dev/null || true

nohup chroot $ROOTFS /bin/bash -c "
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
python3 /usr/local/bin/printserver-webui.py
" </dev/null >/data/local/tmp/webui.log 2>&1 &
