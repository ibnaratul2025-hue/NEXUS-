# Building NEXUS

This guide covers building NEXUS from source, including local development, native C++ compilation, and production release signing.

---

## Prerequisites

| Tool | Minimum Version | Recommended | Notes |
| :--- | :--- | :--- | :--- |
| **JDK** | 17 | 21 (Temurin / OpenJDK) | Java runtime for Gradle and Android build tools |
| **Android SDK** | API 34 | API 36 | Android compile and target platform |
| **Android NDK** | r25b | r26b (`26.1.10909125`) | Required only for building native C++ `libllama.so` |
| **CMake** | 3.22.1 | 3.22.1+ | CMake build system for native libraries |
| **Gradle** | 8.9 | 9.3.1 (via wrapper) | Build system |

---

## 1. Quick Local Development Build (Debug APK)

By default, NEXUS compiles cleanly without requiring NDK installation. If the NDK is absent, the native layer gracefully compiles the JVM adapter while logging helpful diagnostics on how to compile `libllama.so`.

```bash
# Clean previous builds
./gradlew clean

# Run unit tests
./gradlew test

# Compile and package Debug APK
./gradlew assembleDebug
```

The resulting debug APK is located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 2. Compiling with Native C++ (llama.cpp JNI)

To compile the C++ JNI bridge (`libllama.so`) for on-device GGUF inference:

1. Ensure the Android NDK is installed. Set your environment variable:
   ```bash
   export ANDROID_NDK_HOME=/path/to/android/sdk/ndk/26.1.10909125
   ```
2. Build the project with the native flag:
   ```bash
   ./gradlew assembleDebug -PincludeNative=true
   ```
3. Verify that the generated APK includes the native shared object:
   ```bash
   unzip -l app/build/outputs/apk/debug/app-debug.apk | grep libllama.so
   # Expected output: lib/arm64-v8a/libllama.so
   ```

---

## 3. Production Release Builds & Signing

NEXUS enforces strict release engineering: **Release builds will intentionally fail if valid signing secrets are not configured.** This prevents accidentally publishing unsigned or test-signed packages.

### Required Environment Variables for Release
- `ANDROID_KEYSTORE_PATH`: Absolute path to your release `.jks` or `.keystore` file.
- `ANDROID_KEYSTORE_PASSWORD`: Keystore password.
- `ANDROID_KEY_ALIAS`: Key alias.
- `ANDROID_KEY_PASSWORD`: Key password.

### Building the Release APK
```bash
./gradlew assembleRelease
```

The signed release APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

---

## 4. Troubleshooting Common Build Issues

### Issue: `JAVA_HOME is not set`
Set `JAVA_HOME` to your JDK 17 or JDK 21 installation path:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

### Issue: `SDK location not found`
Ensure your `ANDROID_SDK_ROOT` or `ANDROID_HOME` is set:
```bash
export ANDROID_SDK_ROOT=$HOME/Android/Sdk
```

### Issue: Release build fails with `Release signing configuration is missing`
This is expected behavior when release signing secrets are not present in the environment. For testing without signing secrets, use `./gradlew assembleDebug`.
