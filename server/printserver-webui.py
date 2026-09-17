#!/usr/bin/env python3
"""
Dodo PrintServer Web Dashboard
Universal Wireless Print Server Web UI by killindodo
Runs on port 8080, styled with Dodo-RF themes (Matrix, Cyberpunk, Monochrome, Light, Slate)
Supports real-time printer status, queue management, and browser drag-and-drop printing.
"""

import http.server
import socketserver
import json
import subprocess
import os
import sys
import time
import socket
from email.parser import BytesFeedParser
from email.policy import default

PORT = 8080

HTML_PAGE = r"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<title>Pinion Dashboard</title>
<style>
  :root {
    --bg: #000000;
    --panel-bg: #0a0a0a;
    --border: #113322;
    --green: #00FF66;
    --cyan: #00E5FF;
    --red: #FF2A6D;
    --amber: #FFB800;
    --text: #00FF66;
    --text-muted: #008833;
    --card-shadow: 0 0 12px rgba(0, 255, 102, 0.08);
  }

  body.theme-cyberpunk {
    --bg: #05020a;
    --panel-bg: #12091f;
    --border: #44125e;
    --green: #00f0ff;
    --cyan: #ff007f;
    --red: #ff0055;
    --amber: #ffaa00;
    --text: #00f0ff;
    --text-muted: #bd00ff;
    --card-shadow: 0 0 12px rgba(255, 0, 127, 0.12);
  }

  body.theme-monochrome {
    --bg: #0d0d0d;
    --panel-bg: #1a1a1a;
    --border: #333333;
    --green: #ffffff;
    --cyan: #cccccc;
    --red: #ff4444;
    --amber: #ffbb33;
    --text: #ffffff;
    --text-muted: #888888;
    --card-shadow: 0 0 12px rgba(255, 255, 255, 0.05);
  }

  body.theme-light {
    --bg: #f4f6f8;
    --panel-bg: #ffffff;
    --border: #d0d7de;
    --green: #0969da;
    --cyan: #1f2328;
    --red: #cf222e;
    --amber: #9a6700;
    --text: #1f2328;
    --text-muted: #656d76;
    --card-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  }

  body.theme-slate {
    --bg: #0F172A;
    --panel-bg: #1E293B;
    --border: #334155;
    --green: #22C55E;
    --cyan: #38BDF8;
    --red: #EF4444;
    --amber: #F59E0B;
    --text: #F8FAFC;
    --text-muted: #94A3B8;
    --card-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
  }

  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: var(--bg);
    color: var(--text);
    font-family: 'Courier New', Courier, monospace, system-ui, -apple-system;
    padding: 16px;
    max-width: 960px;
    margin: 0 auto;
    transition: background 0.3s, color 0.3s;
    line-height: 1.5;
  }

  /* Header */
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 2px solid var(--border);
    padding-bottom: 12px;
    margin-bottom: 18px;
    flex-wrap: wrap;
    gap: 12px;
  }
  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .dodo-logo {
    font-size: 32px;
    filter: drop-shadow(0 0 6px var(--cyan));
    cursor: pointer;
    transition: transform 0.2s;
  }
  .dodo-logo:hover { transform: scale(1.15) rotate(-5deg); }
  .header-title h1 {
    font-size: 20px;
    color: var(--cyan);
    text-shadow: 0 0 6px var(--cyan);
    letter-spacing: 1px;
    text-transform: uppercase;
  }
  .header-title p {
    font-size: 11px;
    color: var(--text-muted);
  }
  .header-right {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .badge {
    padding: 5px 12px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: bold;
    border: 1px solid currentColor;
    text-shadow: 0 0 4px currentColor;
    letter-spacing: 0.5px;
  }
  .badge.running { color: var(--green); border-color: var(--green); }
  .badge.printing { color: var(--amber); border-color: var(--amber); animation: pulse 0.7s infinite alternate; }
  .badge.stopped { color: var(--red); border-color: var(--red); }
  @keyframes pulse { from { opacity: 1; } to { opacity: 0.4; } }

  /* Grid Layout for Tablet/Desktop */
  .grid-layout {
    display: grid;
    grid-template-columns: 1fr;
    gap: 16px;
  }
  @media (min-width: 720px) {
    .grid-layout {
      grid-template-columns: 1fr 1fr;
    }
    .full-width {
      grid-column: span 2;
    }
  }

  /* Panels / Cards */
  .panel {
    background: var(--panel-bg);
    border: 1px solid var(--border);
    border-radius: 8px;
    padding: 16px;
    box-shadow: var(--card-shadow);
    transition: background 0.3s, border-color 0.3s;
  }
  .panel h2 {
    font-size: 13px;
    color: var(--cyan);
    margin-bottom: 12px;
    letter-spacing: 1px;
    text-transform: uppercase;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  /* Info Rows */
  .info-row {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    border-bottom: 1px dashed var(--border);
    font-size: 13px;
  }
  .info-row:last-child { border-bottom: none; }
  .info-label { color: var(--text-muted); }
  .info-val { font-weight: bold; color: var(--text); word-break: break-all; }

  /* Theme Selector */
  select {
    background: var(--bg);
    border: 1px solid var(--border);
    color: var(--cyan);
    padding: 7px 12px;
    font-family: inherit;
    font-size: 12px;
    border-radius: 4px;
    outline: none;
    cursor: pointer;
    transition: border-color 0.2s;
  }
  select:focus { border-color: var(--cyan); }

  /* Buttons */
  .btn-group {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
    gap: 8px;
    margin-top: 12px;
  }
  button {
    font-family: inherit;
    font-size: 12px;
    padding: 10px 14px;
    border-radius: 4px;
    border: 1px solid currentColor;
    background: transparent;
    cursor: pointer;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    font-weight: bold;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
  }
  button:hover {
    box-shadow: 0 0 10px currentColor;
    transform: translateY(-1px);
  }
  button:active { transform: translateY(0); }
  button.primary { color: var(--green); border-color: var(--green); }
  button.cyan { color: var(--cyan); border-color: var(--cyan); }
  button.danger { color: var(--red); border-color: var(--red); }
  button.amber { color: var(--amber); border-color: var(--amber); }

  /* Drag and Drop Zone */
  .drop-zone {
    border: 2px dashed var(--cyan);
    border-radius: 8px;
    padding: 24px 16px;
    text-align: center;
    background: rgba(0, 229, 255, 0.03);
    cursor: pointer;
    transition: all 0.2s;
    margin-bottom: 12px;
  }
  .drop-zone:hover, .drop-zone.dragover {
    background: rgba(0, 229, 255, 0.08);
    border-color: var(--green);
    transform: scale(1.01);
  }
  .drop-zone-icon { font-size: 36px; margin-bottom: 8px; display: block; }
  .drop-zone-text { font-size: 13px; color: var(--cyan); font-weight: bold; }
  .drop-zone-sub { font-size: 11px; color: var(--text-muted); margin-top: 4px; }
  input[type="file"] { display: none; }

  .file-preview {
    display: none;
    background: var(--bg);
    border: 1px solid var(--border);
    padding: 10px;
    border-radius: 4px;
    margin-bottom: 12px;
    font-size: 12px;
    justify-content: space-between;
    align-items: center;
  }

  /* Table */
  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 12px;
    margin-top: 8px;
  }
  th, td {
    padding: 8px 6px;
    text-align: left;
    border-bottom: 1px solid var(--border);
  }
  th {
    color: var(--cyan);
    font-size: 10px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  td.empty-msg {
    text-align: center;
    color: var(--text-muted);
    padding: 16px;
  }

  /* Toast Notification */
  #toast {
    visibility: hidden;
    min-width: 250px;
    background: var(--panel-bg);
    color: var(--cyan);
    border: 1px solid var(--cyan);
    box-shadow: 0 0 14px var(--cyan);
    text-align: center;
    border-radius: 4px;
    padding: 12px 18px;
    position: fixed;
    z-index: 999;
    bottom: 24px;
    left: 50%;
    transform: translateX(-50%);
    font-size: 13px;
    font-weight: bold;
  }
  #toast.show {
    visibility: visible;
    animation: fadein 0.3s, fadeout 0.5s 2.5s;
  }
  @keyframes fadein { from { bottom: 0; opacity: 0; } to { bottom: 24px; opacity: 1; } }
  @keyframes fadeout { from { bottom: 24px; opacity: 1; } to { bottom: 0; opacity: 0; } }

  /* Footer */
  .footer {
    text-align: center;
    margin-top: 24px;
    padding-top: 14px;
    border-top: 1px solid var(--border);
    font-size: 12px;
    color: var(--text-muted);
  }
  .footer a {
    color: var(--cyan);
    text-decoration: none;
    font-weight: bold;
  }
  .footer a:hover { text-decoration: underline; }
</style>
</head>
<body class="theme-matrix">

<div class="header">
  <div class="header-left">
    <div class="dodo-logo" title="Pinion" onclick="cycleTheme()">⚙️🪶</div>
    <div class="header-title">
      <h1>&gt; Pinion</h1>
      <p>Universal Wireless CUPS Print Server Dashboard</p>
    </div>
  </div>
  <div class="header-right">
    <select id="themeSelect" onchange="applyTheme(this.value)">
      <option value="default">Matrix Green (Dark)</option>
      <option value="theme-cyberpunk">Cyberpunk Neon</option>
      <option value="theme-monochrome">Monochrome White</option>
      <option value="theme-light">Light Mode</option>
      <option value="theme-slate">Deep Slate</option>
    </select>
    <span id="serverBadge" class="badge running">DETECTING...</span>
  </div>
</div>

<div class="grid-layout">

  <!-- Left: Live Status -->
  <div class="panel">
    <h2>
      <span>⚡ Live Status</span>
      <span style="font-size:10px; cursor:pointer;" onclick="fetchStatus()">[REFRESH]</span>
    </h2>
    <div class="info-row">
      <span class="info-label">Connected Printer:</span>
      <span class="info-val" id="valPrinter">Checking...</span>
    </div>
    <div class="info-row">
      <span class="info-label">Network IP:</span>
      <span class="info-val" id="valIp">Checking...</span>
    </div>
    <div class="info-row">
      <span class="info-label">Wi-Fi SSID:</span>
      <span class="info-val" id="valWifi">Checking...</span>
    </div>
    <div class="info-row">
      <span class="info-label">IPP URL:</span>
      <span class="info-val" id="valIpp" style="cursor:pointer;" onclick="copyIpp()" title="Click to copy">Click to copy</span>
    </div>
    <div class="info-row">
      <span class="info-label">Queue State:</span>
      <span class="info-val" id="valQueue">Idle</span>
    </div>

    <div class="btn-group">
      <button class="primary" onclick="triggerTestPrint()">🖨️ Test Print</button>
      <button class="amber" onclick="triggerRestart()">🔄 Restart</button>
      <button class="danger" onclick="triggerClearQueue()">🗑️ Clear</button>
    </div>
  </div>

  <!-- Right: Direct Browser Drop & Print -->
  <div class="panel">
    <h2>📄 Direct Browser Print</h2>
    <p style="font-size:11px; color:var(--text-muted); margin-bottom:10px;">
      Print instantly from any phone, tablet, or PC without installing any printer drivers!
    </p>

    <div class="drop-zone" id="dropZone" onclick="document.getElementById('fileInput').click()">
      <span class="drop-zone-icon">📤</span>
      <div class="drop-zone-text">Drop PDF, TXT, or Image here</div>
      <div class="drop-zone-sub">or click to browse from device</div>
    </div>
    <input type="file" id="fileInput" accept=".pdf,.txt,.png,.jpg,.jpeg" onchange="handleFileSelect(this.files)">

    <div class="file-preview" id="filePreview">
      <span id="fileName">No file chosen</span>
      <button class="cyan" style="padding:4px 8px; font-size:10px;" onclick="uploadAndPrint()">🖨️ Print Now</button>
    </div>
  </div>

  <!-- Full Width: Active Jobs -->
  <div class="panel full-width">
    <h2>
      <span>📑 Active Print Jobs</span>
      <span id="jobCount" style="font-size:11px; color:var(--text-muted);">(0 active)</span>
    </h2>
    <table>
      <thead>
        <tr>
          <th>Job ID</th>
          <th>User</th>
          <th>File / Title</th>
          <th>Size</th>
          <th>Status</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody id="jobsTableBody">
        <tr><td colspan="6" class="empty-msg">No active print jobs in queue</td></tr>
      </tbody>
    </table>
  </div>

  <!-- Full Width: Client Guide -->
  <div class="panel full-width">
    <h2>ℹ️ Client Connection Reference (No Password Required)</h2>
    <div style="font-size:12px; display:grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap:12px; margin-top:8px;">
      <div>
        <strong style="color:var(--cyan);">🪟 Windows:</strong>
        <p style="color:var(--text-muted); font-size:11px; margin-top:3px;">
          Settings &gt; Printers &amp; Scanners &gt; Add printer. Or "Add manually" &gt; Shared printer by name &gt; paste the IPP URL.
        </p>
      </div>
      <div>
        <strong style="color:var(--cyan);">📱 Android:</strong>
        <p style="color:var(--text-muted); font-size:11px; margin-top:3px;">
          Open document &gt; Share &gt; Print &gt; Select the printer directly via Mopria / Default Print Service.
        </p>
      </div>
      <div>
        <strong style="color:var(--cyan);">🍎 Apple (AirPrint):</strong>
        <p style="color:var(--text-muted); font-size:11px; margin-top:3px;">
          Zero config! Tap Share &gt; Print on iPhone/iPad/Mac. Discovered automatically via Avahi mDNS.
        </p>
      </div>
      <div>
        <strong style="color:var(--cyan);">🐧 Linux:</strong>
        <p style="color:var(--text-muted); font-size:11px; margin-top:3px;">
          Run: <code style="color:var(--green);">lp -h &lt;IP&gt;:631 -d HP_LaserJet_M1005 file.pdf</code>
        </p>
      </div>
    </div>
  </div>

</div>

<div class="footer">
  Made with 🦤 love by <a href="https://github.com/killindodo" target="_blank">killindodo</a> &bull;
  <a href="http://" id="cupsAdminLink" target="_blank">Legacy CUPS Admin (:631)</a>
</div>

<div id="toast">Message</div>

<script>
  let selectedFile = null;
  let currentIppUrl = "";

  // Theming
  function applyTheme(theme) {
    document.body.className = theme === "default" ? "" : theme;
    localStorage.setItem("dodo_print_theme", theme);
    document.getElementById("themeSelect").value = theme;
  }

  function cycleTheme() {
    const themes = ["default", "theme-cyberpunk", "theme-monochrome", "theme-light", "theme-slate"];
    const current = localStorage.getItem("dodo_print_theme") || "default";
    let nextIdx = (themes.indexOf(current) + 1) % themes.length;
    applyTheme(themes[nextIdx]);
    showToast("Theme switched to: " + themes[nextIdx].replace("theme-", "").toUpperCase());
  }

  const savedTheme = localStorage.getItem("dodo_print_theme") || "default";
  applyTheme(savedTheme);

  function showToast(msg) {
    const t = document.getElementById("toast");
    t.innerText = msg;
    t.className = "show";
    setTimeout(() => { t.className = t.className.replace("show", ""); }, 2800);
  }

  function copyIpp() {
    if (!currentIppUrl) return;
    navigator.clipboard.writeText(currentIppUrl).then(() => {
      showToast("📋 Copied: " + currentIppUrl);
    }).catch(() => {
      showToast("URL: " + currentIppUrl);
    });
  }

  // Fetch status
  function fetchStatus() {
    fetch('/api/status')
      .then(res => res.json())
      .then(data => {
        const badge = document.getElementById("serverBadge");
        if (data.jobs && data.jobs.length > 0) {
          badge.className = "badge printing";
          badge.innerText = "PRINTING (" + data.jobs.length + ")";
        } else if (data.running) {
          badge.className = "badge running";
          badge.innerText = "RUNNING";
        } else {
          badge.className = "badge stopped";
          badge.innerText = "STOPPED";
        }

        document.getElementById("valPrinter").innerText = data.printer || "HP LaserJet M1005 MFP";
        document.getElementById("valIp").innerText = data.ip + ":631";
        document.getElementById("valWifi").innerText = data.wifi || "Wi-Fi Connected";
        document.getElementById("valQueue").innerText = data.queue_state || "Accepting jobs, idle";

        currentIppUrl = "http://" + data.ip + ":631/printers/HP_LaserJet_M1005";
        document.getElementById("valIpp").innerText = currentIppUrl;
        document.getElementById("cupsAdminLink").href = "http://" + data.ip + ":631";

        renderJobs(data.jobs || []);
      })
      .catch(err => {
        const badge = document.getElementById("serverBadge");
        badge.className = "badge stopped";
        badge.innerText = "OFFLINE";
      });
  }

  function renderJobs(jobs) {
    const tbody = document.getElementById("jobsTableBody");
    document.getElementById("jobCount").innerText = "(" + jobs.length + " active)";
    if (jobs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-msg">No active print jobs in queue</td></tr>';
      return;
    }
    let html = "";
    jobs.forEach(j => {
      html += `<tr>
        <td style="color:var(--cyan); font-weight:bold;">${j.id}</td>
        <td>${j.user}</td>
        <td style="color:var(--green);">${j.title}</td>
        <td>${j.size}</td>
        <td>${j.status}</td>
        <td><button class="danger" style="padding:3px 8px; font-size:10px;" onclick="cancelJob('${j.id}')">Cancel</button></td>
      </tr>`;
    });
    tbody.innerHTML = html;
  }

  // File Upload & Print
  const dropZone = document.getElementById("dropZone");
  dropZone.addEventListener("dragover", e => { e.preventDefault(); dropZone.classList.add("dragover"); });
  dropZone.addEventListener("dragleave", () => { dropZone.classList.remove("dragover"); });
  dropZone.addEventListener("drop", e => {
    e.preventDefault();
    dropZone.classList.remove("dragover");
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFileSelect(e.dataTransfer.files);
    }
  });

  function handleFileSelect(files) {
    if (files.length === 0) return;
    selectedFile = files[0];
    document.getElementById("fileName").innerText = "📄 " + selectedFile.name + " (" + Math.round(selectedFile.size / 1024) + " KB)";
    document.getElementById("filePreview").style.display = "flex";
  }

  function uploadAndPrint() {
    if (!selectedFile) {
      showToast("Please choose a file to print");
      return;
    }
    showToast("📤 Uploading & spooling to printer...");
    const formData = new FormData();
    formData.append("file", selectedFile);

    fetch('/api/print', {
      method: 'POST',
      body: formData
    })
    .then(res => res.json())
    .then(data => {
      if (data.success) {
        showToast("🖨️ " + data.message);
        document.getElementById("filePreview").style.display = "none";
        selectedFile = null;
        fetchStatus();
      } else {
        showToast("❌ Error: " + data.error);
      }
    })
    .catch(err => {
      showToast("❌ Failed to send print job: " + err);
    });
  }

  function triggerTestPrint() {
    showToast("Sending test page to printer...");
    fetch('/api/testprint', { method: 'POST' })
      .then(res => res.json())
      .then(data => {
        showToast(data.message || "Test page spooled!");
        fetchStatus();
      });
  }

  function triggerRestart() {
    showToast("Restarting print server daemons...");
    fetch('/api/restart', { method: 'POST' })
      .then(res => res.json())
      .then(data => {
        showToast(data.message || "Services restarted");
        setTimeout(fetchStatus, 3000);
      });
  }

  function triggerClearQueue() {
    if (!confirm("Cancel all active print jobs in queue?")) return;
    fetch('/api/cancel', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ all: true })
    })
    .then(res => res.json())
    .then(data => {
      showToast(data.message || "Queue cleared");
      fetchStatus();
    });
  }

  function cancelJob(jobId) {
    fetch('/api/cancel', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ jobId: jobId })
    })
    .then(res => res.json())
    .then(data => {
      showToast(data.message || "Job cancelled");
      fetchStatus();
    });
  }

  // Poll status every 4 seconds
  fetchStatus();
  setInterval(fetchStatus, 4000);
</script>
</body>
</html>
"""

def get_lan_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.settimeout(0.5)
        s.connect(('1.1.1.1', 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

def get_wifi_ssid():
    # Check Android wifi status via dumpsys/cmd if accessible
    try:
        out = subprocess.check_output("cmd wifi status 2>/dev/null || dumpsys wifi 2>/dev/null || true", shell=True, text=True)
        import re
        m = re.search(r'connected to "([^"]+)"', out)
        if m:
            return m.group(1)
        m = re.search(r'SSID:\s*"([^"]+)"', out)
        if m:
            return m.group(1)
    except Exception:
        pass
    return "Wi-Fi Connected"

ENV_PATH = {"PATH": "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"}

def get_cups_status():
    running = False
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(0.3)
        running = (s.connect_ex(('127.0.0.1', 631)) == 0)
        s.close()
    except Exception:
        running = False

    printer_name = "HP LaserJet M1005 MFP"
    queue_state = "Idle"
    try:
        p_out = subprocess.check_output("lpstat -p 2>/dev/null || true", shell=True, text=True, env=ENV_PATH)
        if "is idle" in p_out:
            queue_state = "Accepting jobs, idle"
        elif "is printing" in p_out:
            queue_state = "Printing..."
        elif "disabled" in p_out:
            queue_state = "Queue paused"
    except Exception:
        pass

    # Active jobs
    jobs = []
    try:
        j_out = subprocess.check_output("lpstat -o 2>/dev/null || true", shell=True, text=True, env=ENV_PATH).strip()
        if j_out:
            for line in j_out.split("\n"):
                parts = line.split()
                if len(parts) >= 4:
                    jobs.append({
                        "id": parts[0],
                        "user": parts[1],
                        "size": parts[2] if len(parts) > 2 else "Unknown",
                        "title": " ".join(parts[3:]),
                        "status": "In Queue"
                    })
    except Exception:
        pass

    return {
        "running": running,
        "printer": printer_name,
        "queue_state": queue_state,
        "jobs": jobs,
        "ip": get_lan_ip(),
        "wifi": get_wifi_ssid()
    }

class RequestHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        # Silence routine request logging
        pass

    def do_GET(self):
        if self.path == "/" or self.path.startswith("/index"):
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(HTML_PAGE.encode("utf-8"))
        elif self.path == "/api/status":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            data = get_cups_status()
            self.wfile.write(json.dumps(data).encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        if self.path == "/api/status":
            self.do_GET()
            return

        if self.path == "/api/testprint":
            try:
                # Send test print via lp
                cmd = "echo 'Dodo PrintServer Test Page\nUNIVERSAL WIRELESS PRINT SERVER BY KILLINDODO\nDate: $(date)\nCUPS Server: OK\nPrinter: HP LaserJet M1005 MFP\n\nMade with love by killindodo\nhttps://github.com/killindodo' | lp -d HP_LaserJet_M1005"
                subprocess.check_output(cmd, shell=True, env=ENV_PATH)
                resp = {"success": True, "message": "Test print submitted to HP LaserJet M1005!"}
            except Exception as e:
                resp = {"success": False, "error": str(e)}

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(resp).encode("utf-8"))
            return

        if self.path == "/api/restart":
            try:
                subprocess.Popen("killall cupsd avahi-daemon 2>/dev/null; sleep 1; avahi-daemon -D 2>/dev/null; cupsd", shell=True, env=ENV_PATH)
                resp = {"success": True, "message": "Print daemons restarting..."}
            except Exception as e:
                resp = {"success": False, "error": str(e)}

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(resp).encode("utf-8"))
            return

        if self.path == "/api/cancel":
            try:
                content_len = int(self.headers.get("Content-Length", 0))
                body = self.rfile.read(content_len).decode("utf-8")
                payload = json.loads(body) if body else {}
                if payload.get("all"):
                    subprocess.call("cancel -a HP_LaserJet_M1005 2>/dev/null || true", shell=True, env=ENV_PATH)
                    resp = {"success": True, "message": "All print jobs cancelled"}
                elif payload.get("jobId"):
                    subprocess.call(f"cancel {payload['jobId']} 2>/dev/null || true", shell=True, env=ENV_PATH)
                    resp = {"success": True, "message": f"Cancelled job {payload['jobId']}"}
                else:
                    resp = {"success": False, "error": "Missing jobId or all"}
            except Exception as e:
                resp = {"success": False, "error": str(e)}

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(resp).encode("utf-8"))
            return

        if self.path == "/api/print":
            try:
                content_len = int(self.headers.get("Content-Length", 0))
                ctype = self.headers.get("Content-Type", "")

                if "multipart/form-data" in ctype:
                    raw_data = self.rfile.read(content_len)
                    header_bytes = f"Content-Type: {ctype}\r\n\r\n".encode("utf-8")
                    parser = BytesFeedParser(policy=default)
                    parser.feed(header_bytes + raw_data)
                    msg = parser.close()

                    saved_path = None
                    file_name = "document.pdf"
                    for part in msg.iter_parts():
                        fn = part.get_filename()
                        if fn:
                            file_name = fn
                            payload = part.get_payload(decode=True)
                            ext = os.path.splitext(file_name)[1] or ".bin"
                            saved_path = f"/tmp/webprint_{int(time.time())}{ext}"
                            with open(saved_path, "wb") as f:
                                f.write(payload)
                            break

                    if saved_path and os.path.exists(saved_path):
                        cmd = f"lp -d HP_LaserJet_M1005 -t '{file_name}' '{saved_path}'"
                        out = subprocess.check_output(cmd, shell=True, text=True, env=ENV_PATH).strip()
                        resp = {"success": True, "message": f"Printed '{file_name}' ({out})"}
                    else:
                        resp = {"success": False, "error": "No file content detected in upload"}
                else:
                    resp = {"success": False, "error": "Expected multipart/form-data"}
            except Exception as e:
                resp = {"success": False, "error": str(e)}

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(resp).encode("utf-8"))
            return

        self.send_response(404)
        self.end_headers()

class ReusableThreadingServer(socketserver.ThreadingMixIn, http.server.HTTPServer):
    allow_reuse_address = True
    daemon_threads = True

def run_server():
    server_address = ('0.0.0.0', PORT)
    httpd = ReusableThreadingServer(server_address, RequestHandler)
    print(f"[Dodo PrintServer WebUI] Serving on http://0.0.0.0:{PORT}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()

if __name__ == "__main__":
    run_server()
