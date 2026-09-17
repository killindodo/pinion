#!/bin/bash
# ===========================================================================
# Pinion PrintServer — Shell Script Tests
# Tests for start-printserver.sh and build.sh issues
#
# Run: bash tests/test_shell_scripts.sh
# ===========================================================================

set -euo pipefail

PASS=0
FAIL=0
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

pass() { echo "  ✅ PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  ❌ FAIL: $1"; FAIL=$((FAIL + 1)); }

echo "=== Pinion PrintServer — Shell Script Tests ==="
echo ""

# ---------------------------------------------------------------------------
# Bug #7: Heredoc script integrity check
# ---------------------------------------------------------------------------
echo "--- Bug #7: Startup script integrity ---"

# Test: start-printserver.sh should be a valid shell script
if bash -n "$PROJECT_DIR/server/start-printserver.sh" 2>/dev/null; then
    pass "start-printserver.sh has valid syntax"
else
    fail "start-printserver.sh has syntax errors"
fi

# Test: The heredoc in MainActivity.java should produce the same script
# (Can't fully test without Java, but we can check for critical commands)
STARTUP="$PROJECT_DIR/server/start-printserver.sh"

if grep -q "ROOTFS=" "$STARTUP"; then
    pass "ROOTFS variable is defined in startup script"
else
    fail "ROOTFS variable missing from startup script"
fi

if grep -q "chroot.*cupsd" "$STARTUP"; then
    pass "cupsd is started via chroot"
else
    fail "cupsd startup not found"
fi

if grep -q "avahi-daemon" "$STARTUP"; then
    pass "avahi-daemon is started"
else
    fail "avahi-daemon startup not found"
fi

if grep -q "printserver-webui.py" "$STARTUP"; then
    pass "Web UI server is started"
else
    fail "Web UI server startup not found"
fi

# Test: Script uses wake_lock (important for keeping server alive)
if grep -q "wake_lock" "$STARTUP"; then
    pass "wake_lock is set to prevent sleep"
else
    fail "wake_lock not set — server may stop when screen turns off"
fi

# ---------------------------------------------------------------------------
# Bug #18: Hardcoded paths in build.sh
# ---------------------------------------------------------------------------
echo ""
echo "--- Bug #18: build.sh hardcoded paths ---"

BUILD="$PROJECT_DIR/build.sh"

# Test: build.sh contains hardcoded user path
if grep -q '/home/killindodo' "$BUILD"; then
    fail "build.sh contains hardcoded /home/killindodo path (not portable)"
else
    pass "build.sh does not contain hardcoded paths"
fi

# Test: build.sh should use ANDROID_HOME or ANDROID_SDK_ROOT
if grep -q '\$ANDROID_HOME\|$ANDROID_SDK_ROOT\|${ANDROID_HOME}\|${ANDROID_SDK_ROOT}' "$BUILD"; then
    pass "build.sh uses ANDROID_HOME/ANDROID_SDK_ROOT env variable"
else
    fail "build.sh does NOT use ANDROID_HOME env variable (hardcodes SDK path)"
fi

# Test: build.sh uses set -e for error handling
if head -5 "$BUILD" | grep -q 'set -e'; then
    pass "build.sh uses 'set -e' for error handling"
else
    fail "build.sh missing 'set -e'"
fi

# ---------------------------------------------------------------------------
# Bug #20: Manifest permission check
# ---------------------------------------------------------------------------
echo ""
echo "--- Bug #20: AndroidManifest.xml permission audit ---"

MANIFEST="$PROJECT_DIR/src/main/AndroidManifest.xml"

# Test: RECEIVE_BOOT_COMPLETED without matching receiver
if grep -q 'RECEIVE_BOOT_COMPLETED' "$MANIFEST"; then
    if grep -q 'android.intent.action.BOOT_COMPLETED' "$MANIFEST"; then
        pass "BOOT_COMPLETED permission has matching receiver"
    else
        fail "RECEIVE_BOOT_COMPLETED permission declared but NO receiver registered (dead permission)"
    fi
else
    pass "BOOT_COMPLETED permission not requested (not needed)"
fi

# Test: All required permissions are present
for perm in INTERNET ACCESS_NETWORK_STATE ACCESS_WIFI_STATE; do
    if grep -q "android.permission.$perm" "$MANIFEST"; then
        pass "Required permission $perm is declared"
    else
        fail "Missing required permission: $perm"
    fi
done

# ---------------------------------------------------------------------------
# Security: Check for exposed secrets in build.sh
# ---------------------------------------------------------------------------
echo ""
echo "--- Security: Secrets check ---"

# Test: Keystore passwords should not be hardcoded
if grep -q 'storepass android' "$BUILD" || grep -q 'keypass android' "$BUILD"; then
    fail "Keystore uses default 'android' password (acceptable for debug only)"
else
    pass "Keystore does not use default password"
fi

# Test: Check for accidental secret leaks
if grep -rq 'API_KEY\|SECRET\|TOKEN\|PASSWORD' "$PROJECT_DIR/src/" 2>/dev/null; then
    fail "Possible secrets found in source code"
else
    pass "No obvious secrets in source code"
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "========================================"
echo "  Results: $PASS passed, $FAIL failed"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
