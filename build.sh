#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Resolve SDK from $ANDROID_HOME or $ANDROID_SDK_ROOT
SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-"$HOME/Android/Sdk"}}"
BUILD_TOOLS="$SDK_DIR/build-tools/36.0.0"
PLATFORM="$SDK_DIR/platforms/android-35/android.jar"

AAPT2="$BUILD_TOOLS/aapt2"
D8="$BUILD_TOOLS/d8"
ZIPALIGN="$BUILD_TOOLS/zipalign"
APKSIGNER="$BUILD_TOOLS/apksigner"

echo "=== 1. Cleaning & preparing build directories ==="
rm -rf "$PROJECT_DIR/build"
mkdir -p "$PROJECT_DIR/build/compiled"
mkdir -p "$PROJECT_DIR/build/gen"
mkdir -p "$PROJECT_DIR/build/obj"
mkdir -p "$PROJECT_DIR/build/dex"
mkdir -p "$PROJECT_DIR/build/out"

echo "=== 2. Compiling resources with aapt2 ==="
$AAPT2 compile --dir "$PROJECT_DIR/src/main/res" -o "$PROJECT_DIR/build/compiled/res.zip"

echo "=== 3. Linking resources & generating R.java ==="
$AAPT2 link -I "$PLATFORM" \
    --manifest "$PROJECT_DIR/src/main/AndroidManifest.xml" \
    -o "$PROJECT_DIR/build/app-unaligned.apk" \
    --java "$PROJECT_DIR/build/gen" \
    "$PROJECT_DIR/build/compiled/res.zip" \
    --auto-add-overlay

echo "=== 4. Compiling Java sources with javac ==="
javac --release 8 -cp "$PLATFORM" \
    -d "$PROJECT_DIR/build/obj" \
    "$PROJECT_DIR/build/gen/com/killindodo/printserver/R.java" \
    "$PROJECT_DIR/src/main/java/com/killindodo/printserver/MainActivity.java"

echo "=== 5. Converting bytecode to DEX with d8 ==="
$D8 --output "$PROJECT_DIR/build/dex" "$PROJECT_DIR/build/obj/com/killindodo/printserver/"*.class

echo "=== 6. Packaging DEX into APK ==="
cd "$PROJECT_DIR/build/dex"
zip -u "$PROJECT_DIR/build/app-unaligned.apk" classes.dex
cd "$PROJECT_DIR"

echo "=== 7. Aligning APK with zipalign ==="
$ZIPALIGN -v -p 4 "$PROJECT_DIR/build/app-unaligned.apk" "$PROJECT_DIR/build/out/PrintServer-Root.apk"

echo "=== 8. Checking / Generating signing key ==="
KEYSTORE="$PROJECT_DIR/keystore/debug.keystore"
KS_PASS="${KEYSTORE_PASS:-android}"
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -v \
        -keystore "$KEYSTORE" \
        -storepass "$KS_PASS" -keypass "$KS_PASS" \
        -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Android Debug, O=Android, C=US"
fi

echo "=== 9. Signing APK with apksigner ==="
$APKSIGNER sign --ks "$KEYSTORE" \
    --ks-pass "pass:$KS_PASS" \
    --key-pass "pass:$KS_PASS" \
    --ks-key-alias androiddebugkey \
    "$PROJECT_DIR/build/out/PrintServer-Root.apk"

echo "=== 10. Verifying APK signature ==="
$APKSIGNER verify "$PROJECT_DIR/build/out/PrintServer-Root.apk"

echo "SUCCESS: $PROJECT_DIR/build/out/PrintServer-Root.apk is ready!"
ls -lh "$PROJECT_DIR/build/out/PrintServer-Root.apk"
