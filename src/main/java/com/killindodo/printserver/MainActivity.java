package com.killindodo.printserver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

    public static class DodoTheme {
        public final String id;
        public final String name;
        public final int bg;
        public final int cardBg;
        public final int cardBorder;
        public final int primary;
        public final int accent;
        public final int textPrimary;
        public final int textSecondary;
        public final int textMuted;
        public final int statusGreen;
        public final boolean isLight;

        public DodoTheme(String id, String name, int bg, int cardBg, int cardBorder,
                         int primary, int accent, int textPrimary, int textSecondary,
                         int textMuted, int statusGreen, boolean isLight) {
            this.id = id;
            this.name = name;
            this.bg = bg;
            this.cardBg = cardBg;
            this.cardBorder = cardBorder;
            this.primary = primary;
            this.accent = accent;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
            this.textMuted = textMuted;
            this.statusGreen = statusGreen;
            this.isLight = isLight;
        }
    }

    private static final DodoTheme[] THEMES = new DodoTheme[]{
            new DodoTheme("matrix", "Matrix Green (Dark)",
                    0xFF000000, 0xFF0A120D, 0xFF113322, 0xFF00FF66, 0xFF00E5FF, 0xFF00FF66, 0xFF00CC55, 0xFF008833, 0xFF00FF66, false),
            new DodoTheme("cyberpunk", "Cyberpunk Neon",
                    0xFF05020A, 0xFF12091F, 0xFF44125E, 0xFF00F0FF, 0xFFFF007F, 0xFF00F0FF, 0xFFCC77FF, 0xFFBD00FF, 0xFF00F0FF, false),
            new DodoTheme("monochrome", "Monochrome White",
                    0xFF0D0D0D, 0xFF1A1A1A, 0xFF333333, 0xFFFFFFFF, 0xFFCCCCCC, 0xFFFFFFFF, 0xFFCCCCCC, 0xFF888888, 0xFFFFFFFF, false),
            new DodoTheme("light", "Light Mode",
                    0xFFF4F6F8, 0xFFFFFFFF, 0xFFD0D7DE, 0xFF0969DA, 0xFF1F2328, 0xFF1F2328, 0xFF424A53, 0xFF656D76, 0xFF1A7F37, true),
            new DodoTheme("slate", "Deep Slate (Default)",
                    0xFF0F172A, 0xFF1E293B, 0xFF334155, 0xFF38BDF8, 0xFF818CF8, 0xFFF8FAFC, 0xFF94A3B8, 0xFF64748B, 0xFF22C55E, false)
    };

    private DodoTheme currentTheme = THEMES[0]; // Default to Matrix Green from Dodo-RF!

    // Views
    private View rootScroll;
    private ImageView ivDodoLogo;
    private View btnThemeSelector;
    private TextView tvThemeLabel;
    private TextView tvAppTitle;
    private TextView tvAppSubtitle;
    private TextView tvStatusBadge;
    private TextView tvIpAddress;
    private TextView tvWifiSsid;
    private TextView tvPrinterModel;
    private TextView tvRootStatus;
    private TextView tvGuideWindows;
    private TextView tvGuideLinux;
    private TextView tvGithubLink;
    private TextView tvFooterBranding;
    private Button btnCopyUrl;
    private Button btnOpenWebUi;
    private Button btnStart;
    private Button btnStop;
    private Button btnRestart;
    private Button btnTestPage;

    // Card Views for dynamic theming
    private View cardStatus;
    private View cardControls;
    private View cardWebUiInfo;
    private View cardGuide;
    private TextView labelStatus;
    private TextView labelControls;
    private TextView labelWebui;
    private TextView labelGuide;
    private TextView tvTitleWin;
    private TextView tvTitleAndroid;
    private TextView tvTitleApple;
    private TextView tvTitleLinux;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private String currentIp = "127.0.0.1";
    private String currentPrinterQueue = "<PRINTER_NAME>";
    private boolean isServerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load saved theme preference
        SharedPreferences prefs = getSharedPreferences("dodo_prefs", MODE_PRIVATE);
        String savedThemeId = prefs.getString("theme_id", "matrix");
        for (DodoTheme t : THEMES) {
            if (t.id.equals(savedThemeId)) {
                currentTheme = t;
                break;
            }
        }

        // Initialize Views
        rootScroll = findViewById(R.id.root_scroll);
        ivDodoLogo = findViewById(R.id.iv_dodo_logo);
        btnThemeSelector = findViewById(R.id.btn_theme_selector);
        tvThemeLabel = findViewById(R.id.tv_theme_label);
        tvAppTitle = findViewById(R.id.tv_app_title);
        tvAppSubtitle = findViewById(R.id.tv_app_subtitle);
        tvStatusBadge = findViewById(R.id.tv_status_badge);
        tvIpAddress = findViewById(R.id.tv_ip_address);
        tvWifiSsid = findViewById(R.id.tv_wifi_ssid);
        tvPrinterModel = findViewById(R.id.tv_printer_model);
        tvRootStatus = findViewById(R.id.tv_root_status);
        tvGuideWindows = findViewById(R.id.tv_guide_windows);
        tvGuideLinux = findViewById(R.id.tv_guide_linux);
        tvGithubLink = findViewById(R.id.tv_github_link);
        tvFooterBranding = findViewById(R.id.tv_footer_branding);

        btnCopyUrl = findViewById(R.id.btn_copy_url);
        btnOpenWebUi = findViewById(R.id.btn_open_webui);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnRestart = findViewById(R.id.btn_restart);
        btnTestPage = findViewById(R.id.btn_test_page);

        cardStatus = findViewById(R.id.card_status);
        cardControls = findViewById(R.id.card_controls);
        cardWebUiInfo = findViewById(R.id.card_webui_info);
        cardGuide = findViewById(R.id.card_guide);

        labelStatus = findViewById(R.id.label_status);
        labelControls = findViewById(R.id.label_controls);
        labelWebui = findViewById(R.id.label_webui);
        labelGuide = findViewById(R.id.label_guide);

        tvTitleWin = findViewById(R.id.tv_title_win);
        tvTitleAndroid = findViewById(R.id.tv_title_android);
        tvTitleApple = findViewById(R.id.tv_title_apple);
        tvTitleLinux = findViewById(R.id.tv_title_linux);

        // Theme Switcher Trigger (Clicking logo or theme chip)
        View.OnClickListener themeClickListener = v -> showThemeDialog();
        if (ivDodoLogo != null) ivDodoLogo.setOnClickListener(themeClickListener);
        if (btnThemeSelector != null) btnThemeSelector.setOnClickListener(themeClickListener);

        // Window Setup: Edge-to-Edge & Display Cutout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight = resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : dpToPx(28);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.view.Window window = getWindow();
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            window.setNavigationBarColor(currentTheme.bg);
        }

        // Apply snug top padding (exact status bar height + 4dp) to eliminate all wasted top void
        View rootContainer = findViewById(R.id.root_container);
        if (rootContainer != null) {
            rootContainer.setPadding(
                    dpToPx(14),
                    statusBarHeight + dpToPx(4),
                    dpToPx(14),
                    dpToPx(8)
            );
        }

        // Apply current theme
        applyTheme(currentTheme);

        // GitHub Link Click
        tvGithubLink.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.github.com/killindodo"));
            startActivity(browserIntent);
        });

        // Open Pinion Web Dashboard Click (:8080)
        btnOpenWebUi.setOnClickListener(v -> {
            String networkUrl = "http://" + currentIp + ":8080";
            String localUrl = "http://127.0.0.1:8080";
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Pinion Web Dashboard URL", networkUrl);
            if (clipboard != null) clipboard.setPrimaryClip(clip);

            if (!isServerRunning) {
                Toast.makeText(MainActivity.this, "Starting server first...", Toast.LENGTH_SHORT).show();
                executor.execute(() -> {
                    ensureStartupScriptExists();
                    runRootCommand("/data/local/bin/start-printserver.sh");
                    try {
                        for (int i = 0; i < 6; i++) {
                            Thread.sleep(500);
                            if (checkCupsRunning()) break;
                        }
                    } catch (InterruptedException ignored) {}

                    mainHandler.post(() -> {
                        refreshStatus();
                        Toast.makeText(MainActivity.this, "Opening Web UI & Copied: " + networkUrl, Toast.LENGTH_SHORT).show();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(localUrl));
                        startActivity(browserIntent);
                    });
                });
            } else {
                Toast.makeText(MainActivity.this, "Opening Web UI & Copied: " + networkUrl, Toast.LENGTH_SHORT).show();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(localUrl));
                startActivity(browserIntent);
            }
        });

        // Copy Printer IPP URL to Clipboard
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

    private void showThemeDialog() {
        String[] names = new String[THEMES.length];
        int selectedIdx = 0;
        for (int i = 0; i < THEMES.length; i++) {
            names[i] = THEMES[i].name;
            if (THEMES[i].id.equals(currentTheme.id)) {
                selectedIdx = i;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🪶 Select Pinion Theme");
        builder.setSingleChoiceItems(names, selectedIdx, (dialog, which) -> {
            currentTheme = THEMES[which];
            getSharedPreferences("dodo_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("theme_id", currentTheme.id)
                    .apply();
            applyTheme(currentTheme);
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "Theme: " + currentTheme.name, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void applyTheme(DodoTheme t) {
        // 1. Root & Window Background
        if (rootScroll != null) {
            rootScroll.setBackgroundColor(t.bg);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(t.bg);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            if (t.isLight) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decor.setSystemUiVisibility(flags);
        }

        // 2. Dodo Logo & Chip
        if (ivDodoLogo != null) {
            ivDodoLogo.setColorFilter(t.primary);
        }
        View frameLogo = findViewById(R.id.frame_dodo_logo);
        if (frameLogo != null) {
            GradientDrawable logoBg = new GradientDrawable();
            logoBg.setShape(GradientDrawable.OVAL);
            logoBg.setColor(t.cardBg);
            logoBg.setStroke(dpToPx(2), t.primary);
            frameLogo.setBackground(logoBg);
        }
        if (btnThemeSelector != null) {
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(dpToPx(20));
            chipBg.setColor(t.cardBg);
            chipBg.setStroke(dpToPx(1.5f), t.primary);
            btnThemeSelector.setBackground(chipBg);
        }
        if (tvThemeLabel != null) {
            tvThemeLabel.setText(t.name.split(" ")[0]);
            tvThemeLabel.setTextColor(t.primary);
        }

        // 3. Card Backgrounds
        applyCardStyle(cardStatus, t);
        applyCardStyle(cardControls, t);
        applyCardStyle(cardWebUiInfo, t);
        applyCardStyle(cardGuide, t);

        // 4. Action Buttons
        applyActionButtonStyle(btnCopyUrl, t);
        applyActionButtonStyle(btnOpenWebUi, t);
        applyActionButtonStyle(btnRestart, t);

        // 5. Text Colors
        if (tvAppTitle != null) tvAppTitle.setTextColor(t.textPrimary);
        if (tvAppSubtitle != null) tvAppSubtitle.setTextColor(t.textMuted);
        if (tvIpAddress != null) tvIpAddress.setTextColor(t.primary);
        if (tvPrinterModel != null) tvPrinterModel.setTextColor(t.textSecondary);
        if (tvWifiSsid != null) tvWifiSsid.setTextColor(t.textMuted);
        if (tvRootStatus != null) tvRootStatus.setTextColor(t.textMuted);

        if (labelStatus != null) labelStatus.setTextColor(t.textMuted);
        if (labelControls != null) labelControls.setTextColor(t.textMuted);
        if (labelWebui != null) labelWebui.setTextColor(t.textMuted);
        if (labelGuide != null) labelGuide.setTextColor(t.textMuted);

        if (tvTitleWin != null) tvTitleWin.setTextColor(t.primary);
        if (tvTitleAndroid != null) tvTitleAndroid.setTextColor(t.primary);
        if (tvTitleApple != null) tvTitleApple.setTextColor(t.primary);
        if (tvTitleLinux != null) tvTitleLinux.setTextColor(t.primary);

        if (tvGuideWindows != null) tvGuideWindows.setTextColor(t.textSecondary);
        TextView tvGuideAndroid = findViewById(R.id.tv_guide_android);
        if (tvGuideAndroid != null) tvGuideAndroid.setTextColor(t.textSecondary);
        TextView tvGuideApple = findViewById(R.id.tv_guide_apple);
        if (tvGuideApple != null) tvGuideApple.setTextColor(t.textSecondary);
        if (tvGuideLinux != null) tvGuideLinux.setTextColor(t.textSecondary);
        TextView tvWebuiDesc = findViewById(R.id.tv_webui_desc);
        if (tvWebuiDesc != null) tvWebuiDesc.setTextColor(t.textSecondary);

        if (tvFooterBranding != null) tvFooterBranding.setTextColor(t.accent);
        if (tvGithubLink != null) tvGithubLink.setTextColor(t.primary);

        // Refresh badge style
        updateStatusBadge(isServerRunning);
    }

    private void applyCardStyle(View card, DodoTheme t) {
        if (card == null) return;
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dpToPx(12));
        gd.setColor(t.cardBg);
        gd.setStroke(dpToPx(1), t.cardBorder);
        card.setBackground(gd);
    }

    private void applyActionButtonStyle(Button btn, DodoTheme t) {
        if (btn == null) return;
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dpToPx(8));
        gd.setColor(t.cardBg);
        gd.setStroke(dpToPx(1.2f), t.primary);
        btn.setBackground(gd);
        btn.setTextColor(t.primary);
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void setupNetworkObserver() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    mainHandler.post(() -> refreshStatus());
                }

                @Override
                public void onLost(Network network) {
                    mainHandler.post(() -> refreshStatus());
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    mainHandler.post(() -> refreshStatus());
                }
            };

            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
        }
        executor.shutdown();
    }

    private void refreshStatus() {
        executor.execute(() -> {
            final boolean hasRoot = checkRootAccess();
            final String lanIp = getLocalIpAddress();
            final String wifiSsid = getConnectedWifiName();
            final boolean cupsRunning = checkCupsRunning();
            final String usbPrinter = checkUsbPrinter();

            mainHandler.post(() -> {
                currentIp = lanIp;
                isServerRunning = cupsRunning;

                tvRootStatus.setText(hasRoot ? "Root Access: Granted (Active)" : "Root Access: Denied (Required)");
                tvRootStatus.setTextColor(hasRoot ? currentTheme.statusGreen : 0xFFEF4444);

                tvIpAddress.setText("IP: " + lanIp + ":631  |  Web: :8080");
                tvWifiSsid.setText("Network: " + wifiSsid);
                tvPrinterModel.setText("Printer: " + usbPrinter);

                if (tvGuideLinux != null) {
                    tvGuideLinux.setText("Command: lp -h " + lanIp + ":631 -d <PRINTER> document.pdf");
                }

                updateStatusBadge(cupsRunning);
            });
        });
    }

    private void updateStatusBadge(boolean running) {
        if (running) {
            tvStatusBadge.setText("RUNNING");
            tvStatusBadge.setTextColor(currentTheme.statusGreen);
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(dpToPx(12));
            badgeBg.setColor(Color.argb(40, Color.red(currentTheme.statusGreen), Color.green(currentTheme.statusGreen), Color.blue(currentTheme.statusGreen)));
            badgeBg.setStroke(dpToPx(1), currentTheme.statusGreen);
            tvStatusBadge.setBackground(badgeBg);
        } else {
            tvStatusBadge.setText("STOPPED");
            tvStatusBadge.setTextColor(0xFFEF4444);
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(dpToPx(12));
            badgeBg.setColor(0x30EF4444);
            badgeBg.setStroke(dpToPx(1), 0xFFEF4444);
            tvStatusBadge.setBackground(badgeBg);
        }
    }

    private void executeRootAction(String action) {
        Toast.makeText(this, "Executing: " + action + "...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            switch (action) {
                case "START":
                    ensureStartupScriptExists();
                    runRootCommand("/data/local/bin/start-printserver.sh");
                    break;
                case "STOP":
                    runRootCommand("killall cupsd avahi-daemon python3 2>/dev/null || true");
                    break;
                case "RESTART":
                    ensureStartupScriptExists();
                    runRootCommand("killall cupsd avahi-daemon python3 2>/dev/null || true; sleep 1; /data/local/bin/start-printserver.sh");
                    break;
                case "TEST_PAGE":
                    String lpTarget = (currentPrinterQueue == null || currentPrinterQueue.startsWith("<")) ? "" : (" -d " + currentPrinterQueue);
                    runRootCommand("chroot /data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs /bin/bash -c "
                            + "\"export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; "
                            + "echo 'Pinion Test Page - Universal Wireless Print Engine - killindodo' | lp" + lpTarget + "\"");
                    break;
            }

            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}

            mainHandler.post(() -> refreshStatus());
        });
    }

    private void ensureStartupScriptExists() {
        runRootCommand("mkdir -p /data/local/bin; "
                + "cat << 'EOF' > /data/local/bin/start-printserver.sh\n"
                + "#!/system/bin/sh\n"
                + "ROOTFS=\"/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs\"\n"
                + "echo \"printserver\" > /sys/power/wake_lock 2>/dev/null\n"
                + "chmod -R 666 /dev/bus/usb 2>/dev/null\n"
                + "chmod -R 666 /dev/usb 2>/dev/null\n"
                + "mount -o remount,exec /data 2>/dev/null\n"
                + "mountpoint -q $ROOTFS/proc || mount -t proc proc $ROOTFS/proc\n"
                + "mountpoint -q $ROOTFS/sys  || mount -t sysfs sysfs $ROOTFS/sys\n"
                + "mountpoint -q $ROOTFS/dev  || mount -o bind /dev $ROOTFS/dev\n"
                + "mountpoint -q $ROOTFS/dev/pts || mount -t devpts devpts $ROOTFS/dev/pts\n"
                + "echo \"nameserver 1.1.1.1\" > $ROOTFS/etc/resolv.conf\n"
                + "echo \"nameserver 8.8.8.8\" >> $ROOTFS/etc/resolv.conf\n"
                + "mkdir -p $ROOTFS/run/dbus $ROOTFS/run/cups $ROOTFS/tmp $ROOTFS/data/local/tmp\n"
                + "chmod 1777 $ROOTFS/tmp $ROOTFS/data/local/tmp\n"
                + "killall cupsd avahi-daemon dbus-daemon 2>/dev/null\n"
                + "pkill -f printserver-webui.py 2>/dev/null\n"
                + "rm -f $ROOTFS/run/dbus/pid $ROOTFS/run/cups/cups.sock $ROOTFS/run/dbus/system_bus_socket $ROOTFS/run/avahi-daemon/pid\n"
                + "chroot $ROOTFS /bin/bash -c \"\n"
                + "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\n"
                + "export TMPDIR=/tmp\n"
                + "export HOME=/root\n"
                + "/usr/bin/dbus-daemon --system >/dev/null 2>&1\n"
                + "avahi-daemon -D >/dev/null 2>&1\n"
                + "cupsd >/dev/null 2>&1\n"
                + "\" </dev/null >/dev/null 2>&1\n"
                + "nohup chroot $ROOTFS /bin/bash -c \"\n"
                + "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\n"
                + "python3 /usr/local/bin/printserver-webui.py\n"
                + "\" </dev/null >/data/local/tmp/webui.log 2>&1 &\n"
                + "EOF\n"
                + "chmod +x /data/local/bin/start-printserver.sh");
    }

    private boolean checkRootAccess() {
        String result = runRootCommand("id");
        return result != null && result.contains("uid=0");
    }

    private boolean checkCupsRunning() {
        String result = runRootCommand("pgrep cupsd || pidof cupsd || true");
        return result != null && !result.trim().isEmpty();
    }

    private String checkUsbPrinter() {
        // Query CUPS default printer or configured queue dynamically
        String queueOut = runRootCommand("chroot /data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs /usr/bin/lpstat -d 2>/dev/null || true");
        if (queueOut != null && queueOut.contains("system default destination:")) {
            String[] parts = queueOut.split("system default destination:");
            if (parts.length > 1) {
                String detected = parts[1].trim();
                if (!detected.isEmpty()) currentPrinterQueue = detected;
            }
        } else {
            String pOut = runRootCommand("chroot /data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs /usr/bin/lpstat -p 2>/dev/null || true");
            if (pOut != null && pOut.contains("printer ")) {
                try {
                    String firstLine = pOut.split("\n")[0];
                    String[] pParts = firstLine.split(" ");
                    if (pParts.length > 1 && !pParts[1].trim().isEmpty()) {
                        currentPrinterQueue = pParts[1].trim();
                    }
                } catch (Exception ignored) {}
            }
        }

        String result = runRootCommand("lsusb 2>/dev/null || dumpsys usb 2>/dev/null || true");
        if (result != null && !result.trim().isEmpty()) {
            String lower = result.toLowerCase();
            if (lower.contains("printer") || lower.contains("print") || (result.contains("Bus ") && result.split("\n").length > 1)) {
                return "USB Printer Connected";
            }
        }
        return "No USB Printer Detected (Connect OTG Cable)";
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                String name = iface.getName().toLowerCase();
                if (name.contains("wlan") || name.contains("eth") || name.contains("ap")) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
            Enumeration<NetworkInterface> allInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allInterfaces.hasMoreElements()) {
                NetworkInterface iface = allInterfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private String getConnectedWifiName() {
        // 1. Try reading real SSID via root (bypasses Android location permission requirement)
        String wifiStatus = runRootCommand("cmd wifi status 2>/dev/null || dumpsys wifi 2>/dev/null || true");
        if (wifiStatus != null) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("connected to \"([^\"]+)\"");
            java.util.regex.Matcher m = p.matcher(wifiStatus);
            if (m.find()) {
                return "Wi-Fi: " + m.group(1);
            }
            java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("SSID:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher m2 = p2.matcher(wifiStatus);
            if (m2.find()) {
                return "Wi-Fi: " + m2.group(1);
            }
            java.util.regex.Pattern p3 = java.util.regex.Pattern.compile("SSID:\\s*([a-zA-Z0-9_-]+)");
            java.util.regex.Matcher m3 = p3.matcher(wifiStatus);
            if (m3.find() && !m3.group(1).equals("<unknown") && !m3.group(1).equals("NONE")) {
                return "Wi-Fi: " + m3.group(1);
            }
        }

        // 2. Fallback to Android WifiManager
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info != null && info.getSSID() != null) {
                    String ssid = info.getSSID().replace("\"", "");
                    if (!ssid.equals("<unknown ssid>") && !ssid.isEmpty()) {
                        return "Wi-Fi: " + ssid;
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Wi-Fi Connected";
    }

    private String runRootCommand(String command) {
        StringBuilder output = new StringBuilder();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            try (DataOutputStream os = new DataOutputStream(process.getOutputStream())) {
                os.writeBytes(command + "\n");
                os.writeBytes("exit\n");
                os.flush();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
