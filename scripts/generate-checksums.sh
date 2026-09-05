#!/usr/bin/env bash
set -euo pipefail

# NEXUS Checksum Generator
# Generates SHA-256 and SHA-512 checksums for build artifacts

TARGET_DIR="${1:-.}"

if [ ! -d "$TARGET_DIR" ] && [ ! -f "$TARGET_DIR" ]; then
    echo "Error: Target '$TARGET_DIR' does not exist." >&2
    exit 1
fi

echo "Generating cryptographic checksums for artifacts in: $TARGET_DIR"

if [ -f "$TARGET_DIR" ]; then
    FILES=("$TARGET_DIR")
else
    mapfile -t FILES < <(find "$TARGET_DIR" -maxdepth 2 -type f \( -name "*.apk" -o -name "*.aab" \) | sort)
fi

if [ ${#FILES[@]} -eq 0 ]; then
    echo "Warning: No APK or AAB artifacts found to hash." >&2
    exit 0
fi

# Generate SHA-256
> checksums.sha256
for file in "${FILES[@]}"; do
    filename=$(basename "$file")
    (cd "$(dirname "$file")" && sha256sum "$filename") >> checksums.sha256
done

# Generate SHA-512
> checksums.sha512
for file in "${FILES[@]}"; do
    filename=$(basename "$file")
    (cd "$(dirname "$file")" && sha512sum "$filename") >> checksums.sha512
done

echo "Successfully generated checksums:"
echo "--- checksums.sha256 ---"
cat checksums.sha256
echo "------------------------"
