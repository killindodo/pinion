#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUT_DIR="$PROJECT_DIR/build/out"
MODULE_STAGE="$PROJECT_DIR/build/magisk-stage"

mkdir -p "$OUT_DIR"
rm -rf "$MODULE_STAGE"
mkdir -p "$MODULE_STAGE"

echo "=== Building Pinion Magisk Module ==="

# Copy module metadata & scripts
cp "$SCRIPT_DIR/module.prop" "$MODULE_STAGE/"
cp "$SCRIPT_DIR/customize.sh" "$MODULE_STAGE/"
cp "$SCRIPT_DIR/service.sh" "$MODULE_STAGE/"

# Copy core server files
cp "$PROJECT_DIR/server/start-printserver.sh" "$MODULE_STAGE/"
cp "$PROJECT_DIR/server/printserver-webui.py" "$MODULE_STAGE/"

# If rootfs archive exists, copy it
if [ -f "$SCRIPT_DIR/rootfs.tar.xz" ]; then
    echo "Found rootfs.tar.xz, bundling into module..."
    cp "$SCRIPT_DIR/rootfs.tar.xz" "$MODULE_STAGE/"
elif [ -f "$SCRIPT_DIR/rootfs.tar.gz" ]; then
    echo "Found rootfs.tar.gz, bundling into module..."
    cp "$SCRIPT_DIR/rootfs.tar.gz" "$MODULE_STAGE/"
else
    echo "Note: No rootfs.tar.xz found in magisk/ directory."
    echo "Creating bootstrap module (rootfs will be bootstrapped or downloaded separately)."
fi

# Package into zip
MODULE_ZIP="$OUT_DIR/pinion-core.zip"
rm -f "$MODULE_ZIP"
(
    cd "$MODULE_STAGE"
    zip -r9 "$MODULE_ZIP" ./*
)

echo "✅ Magisk Module built successfully: $MODULE_ZIP"
ls -lh "$MODULE_ZIP"
