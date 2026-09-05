# NEXUS Release Engineering Guide

This document describes the automated release pipeline, signing infrastructure, and verification procedures for publishing stable NEXUS releases.

---

## 1. Overview of Release Pipeline

```
Local Maintainer
      │
      │ git tag -a v0.1.0 -m "Release v0.1.0"
      │ git push origin v0.1.0
      ▼
GitHub Actions (release.yml)
      ├── 1. Checkout repository & validate tag format
      ├── 2. Set up JDK 17, Android SDK 36, and NDK r26b
      ├── 3. Execute unit tests (./gradlew test)
      ├── 4. Run Android Lint (./gradlew lint)
      ├── 5. Decode release keystore from GitHub Secrets
      ├── 6. Build signed Release APK (./gradlew assembleRelease)
      ├── 7. Verify Native Library (arm64-v8a libllama.so packaged)
      ├── 8. Generate SHA-256 and SHA-512 checksums
      ├── 9. Verify APK alignment and v2/v3 signatures
      └── 10. Publish GitHub Release with signed APK, checksums, & release notes
```

---

## 2. Setting Up GitHub Secrets

To enable automated release signing in GitHub Actions, configure the following secrets under **Repository Settings > Secrets and variables > Actions**:

| Secret Name | Description | Example / Format |
| :--- | :--- | :--- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded release `.jks` or `.keystore` binary file | `cat release.jks \| base64 -w 0` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore access password | `MySecureStorePassword123` |
| `ANDROID_KEY_ALIAS` | Key alias within the keystore | `nexus-release` |
| `ANDROID_KEY_PASSWORD` | Private key password | `MySecureKeyPassword123` |

> **Security Mandate**:
> Release signing secrets are strictly restricted to tag pushes (`v*.*.*`) and workflow dispatches by maintainers. They are **never** injected into pull-request runs from untrusted forks.

---

## 3. Creating a Release

### Step 1: Update Version & Changelog
1. Ensure `app/build.gradle.kts` has updated `versionCode` and `versionName`.
2. Update `CHANGELOG.md` moving items from `[Unreleased]` into the new release header (e.g., `## [0.1.0] - YYYY-MM-DD`).
3. Commit and merge to `main`.

### Step 2: Tag and Push
```bash
git checkout main
git pull origin main
git tag -a v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0
```

### Step 3: Monitor Workflow
1. Navigate to the **Actions** tab on GitHub.
2. Select the **Release Pipeline** run corresponding to your tag.
3. Once completed, the release will appear under **Releases** with:
   - `nexus-v0.1.0-release.apk`
   - `checksums.sha256`
   - Automatically generated release notes.

---

## 4. Manual Verification of Release APK

Maintainers and users can verify the integrity of the release artifact:

```bash
# Verify SHA-256 checksum
sha256sum -c checksums.sha256

# Verify native library inclusion
unzip -l nexus-v0.1.0-release.apk | grep "lib/arm64-v8a/libllama.so"

# Verify signature
apksigner verify --verbose nexus-v0.1.0-release.apk
```
