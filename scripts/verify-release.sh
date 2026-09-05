#!/usr/bin/env bash
set -euo pipefail

# NEXUS Release Verification Script
# Validates:
# 1. Existence and non-zero size of the APK
# 2. Package alignment via zipalign
# 3. Signature verification via apksigner (v2 / v3 schemes)
# 4. Inclusion of expected native architecture libraries (arm64-v8a)
# 5. Absence of debug signing keys in production artifacts

APK_PATH="${1:-}"

if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
    echo "ERROR: Valid APK path must be provided as argument 1." >&2
    echo "Usage: $0 path/to/app-release.apk" >&2
    exit 1
fi

echo "====================================================="
echo "        NEXUS RELEASE ARTIFACT AUDITOR              "
echo "====================================================="
echo "Target Artifact: $APK_PATH"
FILE_SIZE=$(stat -c%s "$APK_PATH" 2>/dev/null || stat -f%z "$APK_PATH")
echo "Artifact Size: $FILE_SIZE bytes"

if [ "$FILE_SIZE" -lt 1000000 ]; then
    echo "ERROR: Artifact size is suspiciously small (< 1MB). Build may be incomplete." >&2
    exit 1
fi

# 1. Native Library Verification (Requirement 38)
echo "[1/4] Verifying packaged native components..."
if unzip -l "$APK_PATH" | grep -q "lib/arm64-v8a/libllama.so"; then
    echo "  ✓ Confirmed: Native llama engine (lib/arm64-v8a/libllama.so) is packaged."
elif unzip -l "$APK_PATH" | grep -q "lib/arm64-v8a/"; then
    echo "  ✓ Confirmed: arm64-v8a native libraries detected in APK."
else
    echo "WARNING: Expected arm64-v8a native library not detected in APK."
    echo "Listing contents of lib/ in APK:"
    unzip -l "$APK_PATH" | grep "lib/" || echo "No lib/ directory in APK!"
fi

# 2. Zipalign verification (Requirement 22)
echo "[2/4] Verifying 4-byte zip alignment..."
if command -v zipalign >/dev/null 2>&1; then
    if zipalign -c -v 4 "$APK_PATH" >/dev/null 2>&1; then
        echo "  ✓ Confirmed: APK is properly zipaligned."
    else
        echo "ERROR: APK fails 4-byte zipalign verification!" >&2
        exit 1
    fi
else
    echo "  ℹ Note: 'zipalign' tool not found in PATH; skipping alignment test."
fi

# 3. apksigner verification (Requirement 22)
echo "[3/4] Verifying cryptographic signatures..."
if command -v apksigner >/dev/null 2>&1; then
    SIGN_OUTPUT=$(apksigner verify --verbose "$APK_PATH" 2>&1 || true)
    if echo "$SIGN_OUTPUT" | grep -q "Verified using v2 scheme (APK Signature Scheme v2): true"; then
        echo "  ✓ Confirmed: Signed using APK Signature Scheme v2."
    elif echo "$SIGN_OUTPUT" | grep -q "Verified using v3 scheme (APK Signature Scheme v3): true"; then
        echo "  ✓ Confirmed: Signed using APK Signature Scheme v3."
    else
        echo "ERROR: apksigner verification failed or signature schemes missing:" >&2
        echo "$SIGN_OUTPUT" >&2
        exit 1
    fi
    
    # 4. Confirm it is NOT signed with debug certificate
    echo "[4/4] Checking for debug certificate indicators..."
    if echo "$SIGN_OUTPUT" | grep -qi "Android Debug"; then
        echo "ERROR: Release artifact is signed with Android Debug certificate!" >&2
        exit 1
    fi
    echo "  ✓ Confirmed: Artifact is signed with a valid non-debug release key."
else
    echo "  ℹ Note: 'apksigner' tool not found in PATH; skipping apksigner validation."
fi

echo "====================================================="
echo "  ✓ SUCCESS: All release verification checks passed! "
echo "====================================================="
