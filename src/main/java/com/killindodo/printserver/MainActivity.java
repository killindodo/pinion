package com.killindodo.printserver;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private TextView tvStatusBadge;
    private TextView tvIpAddress;
    private TextView tvWifiSsid;
    private TextView tvPrinterModel;
    private TextView tvRootStatus;
    private TextView tvGuideWindows;
    private TextView tvGuideLinux;
    private TextView tvGithubLink;
    private Button btnCopyUrl;
    private Button btnOpenWebUi;
    private Button btnStart;
    private Button btnStop;
    private Button btnRestart;
    private Button btnTestPage;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private String currentIp = "127.0.0.1";
    private String currentPrinterQueue = "HP_LaserJet_M1005";
    private boolean isServerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatusBadge = findViewById(R.id.tv_status_badge);
        tvIpAddress = findViewById(R.id.tv_ip_address);
        tvWifiSsid = findViewById(R.id.tv_wifi_ssid);
        tvPrinterModel = findViewById(R.id.tv_printer_model);
        tvRootStatus = findViewById(R.id.tv_root_status);
        tvGuideWindows = findViewById(R.id.tv_guide_windows);
        tvGuideLinux = findViewById(R.id.tv_guide_linux);
        tvGithubLink = findViewById(R.id.tv_github_link);
        btnCopyUrl = findViewById(R.id.btn_copy_url);
        btnOpenWebUi = findViewById(R.id.btn_open_webui);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnRestart = findViewById(R.id.btn_restart);
        btnTestPage = findViewById(R.id.btn_test_page);

        // GitHub Link Click
        tvGithubLink.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.github.com/killindodo"));
            startActivity(browserIntent);
        });

        // Open CUPS Dashboard Click
        btnOpenWebUi.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + currentIp + ":631"));
            startActivity(browserIntent);
        });

        // Copy Printer URL to Clipboard
        btnCopyUrl.setOnClickListener(v -> {
            String printerUrl = "http://" + currentIp + ":631/printers/" + currentPrinterQueue;
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Printer URL", printerUrl);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Copied: " + printerUrl, Toast.LENGTH_SHORT).show();
            }
        });

        // Server Controls
        btnStart.setOnClickListener(v -> executeRootAction("START"));
        btnStop.setOnClickListener(v -> executeRootAction("STOP"));
        btnRestart.setOnClickListener(v -> executeRootAction("RESTART"));
        btnTestPage.setOnClickListener(v -> executeRootAction("TEST_PAGE"));

        // Register Real-Time Network Observer
        setupNetworkObserver();

        // Initial Refresh
        refreshStatus();
    }

    private void setupNetworkObserver() {
        try {
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();

                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        mainHandler.post(() -> refreshStatus());
                    }

                    @Override
                    public void onLost(Network network) {
                        mainHandler.post(() -> refreshStatus());
                    }
                };
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception ignored) {}
    }

    private void refreshStatus() {
        executor.execute(() -> {
            currentIp = getWifiIpAddress();
            String networkSsid = getConnectedWifiName();
            boolean rootOk = checkRootAccess();
            boolean running = isCupsRunning();
            String printerInfo = detectPrinter();

            mainHandler.post(() -> {
                tvRootStatus.setText(rootOk ? "Root Access: Granted (Active)" : "Root Access: NOT Detected (su required)");
                tvIpAddress.setText("IP: " + currentIp + ":631");
                tvWifiSsid.setText("Network: " + networkSsid);
                tvPrinterModel.setText("Printer: " + printerInfo);

                isServerRunning = running;
                if (running) {
                    tvStatusBadge.setText("RUNNING");
                    tvStatusBadge.setBackgroundResource(R.drawable.badge_running);
                    tvStatusBadge.setTextColor(Color.parseColor("#22C55E"));
                } else {
                    tvStatusBadge.setText("STOPPED");
                    tvStatusBadge.setBackgroundResource(R.drawable.badge_stopped);
                    tvStatusBadge.setTextColor(Color.parseColor("#EF4444"));
                }

                // Update dynamic guides
                tvGuideWindows.setText("Settings > Bluetooth & devices > Printers & scanners > Add printer. Or click 'Add manually' > Select a shared printer by name > Paste URL: http://" + currentIp + ":631/printers/" + currentPrinterQueue);
                tvGuideLinux.setText("Command: lp -h " + currentIp + ":631 -d " + currentPrinterQueue + " document.pdf");
            });
        });
    }

    private void executeRootAction(String action) {
        Toast.makeText(this, "Executing " + action + "...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                if ("START".equals(action)) {
                    ensureStartupScriptExists();
                    runRootCommand("/data/local/bin/start-printserver.sh");
                } else if ("STOP".equals(action)) {
                    runRootCommand("killall cupsd avahi-daemon dbus-daemon 2>/dev/null || true");
                } else if ("RESTART".equals(action)) {
                    runRootCommand("killall cupsd avahi-daemon dbus-daemon 2>/dev/null || true; sleep 1");
                    ensureStartupScriptExists();
                    runRootCommand("/data/local/bin/start-printserver.sh");
                } else if ("TEST_PAGE".equals(action)) {
                    runRootCommand("ROOTFS=/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs; chroot $ROOTFS /bin/bash -c 'export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; export TMPDIR=/tmp; echo -e \"=== KILLINDODO PRINT TEST ===\\nDate: $(date)\\nStatus: Operational\" | lp -d " + currentPrinterQueue + "'");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            refreshStatus();

            mainHandler.post(() -> {
                Toast.makeText(MainActivity.this, action + " completed!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void ensureStartupScriptExists() {
        // Universal self-healing: ensures USB permissions & mountpoints are always present
        String cmd = "chmod -R 666 /dev/bus/usb 2>/dev/null; chmod -R 666 /dev/usb 2>/dev/null; "
                   + "if [ ! -f /data/local/bin/start-printserver.sh ]; then "
                   + "mkdir -p /data/local/bin; "
                   + "echo '#!/system/bin/sh' > /data/local/bin/start-printserver.sh; "
                   + "echo 'ROOTFS=/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs' >> /data/local/bin/start-printserver.sh; "
                   + "echo 'mountpoint -q $ROOTFS/proc || mount -t proc proc $ROOTFS/proc' >> /data/local/bin/start-printserver.sh; "
                   + "echo 'mountpoint -q $ROOTFS/sys || mount -t sysfs sysfs $ROOTFS/sys' >> /data/local/bin/start-printserver.sh; "
                   + "echo 'mountpoint -q $ROOTFS/dev || mount -o bind /dev $ROOTFS/dev' >> /data/local/bin/start-printserver.sh; "
                   + "echo 'chmod -R 666 /dev/bus/usb' >> /data/local/bin/start-printserver.sh; "
                   + "echo 'chroot $ROOTFS /bin/bash -c \"export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; export TMPDIR=/tmp; service dbus restart; avahi-daemon -D 2>/dev/null; cupsd\"' >> /data/local/bin/start-printserver.sh; "
                   + "chmod +x /data/local/bin/start-printserver.sh; "
                   + "fi";
        runRootCommand(cmd);
    }

    private boolean checkRootAccess() {
        String result = runRootCommand("id");
        return result != null && result.contains("uid=0");
    }

    private boolean isCupsRunning() {
        String result = runRootCommand("ps -ef | grep cupsd | grep -v grep || true");
        return result != null && result.contains("cupsd");
    }

    private String detectPrinter() {
        // Run universal CUPS usb backend detection
        String result = runRootCommand("ROOTFS=/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs; chroot $ROOTFS /bin/bash -c 'export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; /usr/lib/cups/backend/usb 2>/dev/null || true'");
        if (result != null && result.contains("usb://")) {
            if (result.contains("M1005")) {
                currentPrinterQueue = "HP_LaserJet_M1005";
                return "HP LaserJet M1005 MFP (USB Online)";
            }
            try {
                int firstQuote = result.indexOf("\"");
                int secondQuote = result.indexOf("\"", firstQuote + 1);
                if (firstQuote != -1 && secondQuote != -1) {
                    return result.substring(firstQuote + 1, secondQuote) + " (USB Online)";
                }
            } catch (Exception ignored) {}
            return "USB Printer Detected (Online)";
        }

        // Fallback check on Linux kernel USB devices
        String lsusb = runRootCommand("lsusb 2>/dev/null || /system/bin/lsusb 2>/dev/null || true");
        if (lsusb != null && (lsusb.contains("03f0:3b17") || lsusb.toLowerCase().contains("m1005"))) {
            currentPrinterQueue = "HP_LaserJet_M1005";
            return "HP LaserJet M1005 MFP (USB Online)";
        } else if (lsusb != null && lsusb.contains("03f0:")) {
            return "HP Printer Connected (USB Online)";
        }

        return "No USB Printer Detected (Connect OTG Cable)";
    }

    private String runRootCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader is = new BufferedReader(new InputStreamReader(process.getInputStream()));

            os.writeBytes(command + "\nexit\n");
            os.flush();

            String line;
            while ((line = is.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            os.close();
            is.close();
            return output.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String getWifiIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan") || intf.getName().contains("ap") || intf.getName().contains("eth")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private String getConnectedWifiName() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info != null && info.getSSID() != null) {
                    String ssid = info.getSSID();
                    if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length() > 2) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    if (!ssid.equals("<unknown ssid>")) {
                        return "Wi-Fi: " + ssid;
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Wi-Fi Connected";
    }
}
