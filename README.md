# NEXUS

<p align="center">
  <strong>Local-First Autonomous Android AI Assistant and Operating Layer</strong>
</p>

<p align="center">
  <a href="https://github.com/nexus-ai/nexus/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/nexus-ai/nexus/ci.yml?branch=main&style=flat-square&label=CI" alt="CI Status" /></a>
  <a href="https://github.com/nexus-ai/nexus/releases"><img src="https://img.shields.io/github/v/release/nexus-ai/nexus?style=flat-square&label=Release" alt="Latest Release" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square" alt="License" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat-square&logo=kotlin" alt="Kotlin Version" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-API%2024%2B-green.svg?style=flat-square&logo=android" alt="Android API" /></a>
  <img src="https://img.shields.io/badge/ABI-arm64--v8a%20%7C%20x86__64-orange.svg?style=flat-square" alt="Supported ABIs" />
  <img src="https://img.shields.io/badge/Inference-GGUF%20%2F%20llama.cpp-cyan.svg?style=flat-square" alt="GGUF llama.cpp" />
</p>

---

## What is NEXUS?

**NEXUS** is an open-source, local-first AI assistant and execution kernel designed for Android devices. Unlike cloud-dependent voice assistants that stream private user data, audio, and device telemetry to external servers, NEXUS executes quantized GGUF language models directly on your device's CPU and NPU hardware via an optimized C++ JNI bridge (`llama.cpp`).

NEXUS combines local language model reasoning with a deterministic **Policy Engine**, an **Anti-Hallucination Validator**, and Android system tool integrations.

---

## Core Architectural Principles

```
           ┌──────────────────────────────────────┐
           │             USER COMMAND             │
           └──────────────────┬───────────────────┘
                              ▼
           ┌──────────────────────────────────────┐
           │        LOCAL GGUF LLM REASONING      │
           │  (Proposes intent, plan, & arguments)│
           └──────────────────┬───────────────────┘
                              ▼
           ┌──────────────────────────────────────┐
           │         POLICY & RISK ENGINE         │
           │   (LOW / MEDIUM / HIGH / CRITICAL)   │
           └──────────────────┬───────────────────┘
                              ▼
                     Requires Approval?
                     ├── Yes ──► Explicit User Confirmation
                     └── No
                              ▼
           ┌──────────────────────────────────────┐
           │       ANDROID TOOL EXECUTION         │
           │     (Sandbox, Camera, File, App)     │
           └──────────────────┬───────────────────┘
                              ▼
           ┌──────────────────────────────────────┐
           │     IMMUTABLE TOOL RECEIPT (TR)      │
           │    (Return code, stdout, status)     │
           └──────────────────┬───────────────────┘
                              ▼
           ┌──────────────────────────────────────┐
           │     ANTI-HALLUCINATION VALIDATOR     │
           │(Verifies model claims against Receipt│
           └──────────────────┬───────────────────┘
                              ▼
           ┌──────────────────────────────────────┐
           │     AUDITED STATE & USER RESPONSE    │
           └──────────────────────────────────────┘
```

1. **The Model is Never the Source of Truth**: The local language model can propose intentions, plans, and parameters. However, only real runtime execution receipts determine whether an action succeeded or failed.
2. **Local-First & Offline Autonomy**: Core reasoning and execution operate entirely offline with zero cloud dependency.
3. **Deterministic Safety Policy**: Actions that alter persistent device state, access private contacts, or modify files are categorized by risk level and require explicit user consent.
4. **Anti-Hallucination Gate**: Model outputs claiming file modifications, app launches, or memory saves are cross-referenced with cryptographic execution receipts before being presented to the user.
5. **Auditable System Journal**: Every tool execution, permission grant, and memory mutation is recorded in a local SQLite Room database.

---

## Honest Capability Boundaries

### What NEXUS Can Do :white_check_mark:
- Run quantized GGUF models (`q4_k_m`, `q5_k_m`, `q8_0`) on-device.
- Stream generated tokens to a reactive Jetpack Compose interface.
- Launch installed Android applications via deterministic Package Manager queries.
- Read and manage app-specific sandboxed internal files securely.
- Query device contacts when explicit runtime permission is granted.
- Capture camera photos through standard Android hardware camera providers.
- Maintain persistent, searchable semantic memory across sessions.
- Self-correct and recover from malformed JSON tool calls and missing arguments.

### What NEXUS Cannot Do :x:
- **No Unrestricted Root Access**: NEXUS does not bypass Android OS sandboxing, root phones, or modify protected system partitions.
- **No Silent High-Risk Actions**: NEXUS cannot delete files, wipe data, or invoke high-risk APIs without explicit user authorization.
- **No Telemetry Tracking**: NEXUS contains zero analytics trackers, ad SDKs, or background telemetry services.
- **No Magic Cloud Sync**: Data stays strictly on your device.

---

## Supported Device Architectures

| Architecture | Status | Recommendation |
| :--- | :--- | :--- |
| **arm64-v8a** | **Tier 1 (Full Support)** | Required for real Android devices (Snapdragon, Tensor, Dimensity). |
| **x86_64** | **Tier 2 (Supported)** | Development & Android Studio Emulator testing. |
| **armeabi-v7a** | Not Supported | 32-bit architecture is unsupported due to memory address limitations. |

---

## Quickstart & Local Build

### Prerequisites
- **JDK 17** or **JDK 21**
- **Android SDK Platform 36**
- **Android NDK r26b** (optional, for native CMake builds)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/nexus-ai/nexus.git
cd nexus

# Run tests
./gradlew test

# Run Android Lint
./gradlew lint

# Build Debug APK
./gradlew assembleDebug
```

The resulting debug APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## GGUF Model Guide

NEXUS requires an on-device GGUF model file to perform local inference.

### Recommended Models
- **Qwen 2.5 0.5B / 1.5B Instruct** (`q4_k_m`) — Optimal for lightweight devices (2GB–4GB RAM).
- **Llama 3.2 1B / 3B Instruct** (`q4_k_m`, `q5_k_m`) — Recommended for modern mid-to-flagship devices (6GB–8GB+ RAM).
- **SmolLM2 1.7B Instruct** (`q4_k_m`) — Fast inference with low memory overhead.

### Model Storage & Sideloading
1. Download or convert your chosen model to `.gguf` format.
2. Push the model to your device storage via ADB:
   ```bash
   adb push qwen2.5-1.5b-instruct-q4_k_m.gguf /sdcard/Download/
   ```
3. Open **NEXUS**, navigate to **Models**, and select the imported file to validate and mount the model.

> **Important Note**: Model weights are NOT bundled in the Git repository or APK to keep repository size lean and honor distribution licenses.

---

## Documentation Index

- [Architecture Overview](docs/ARCHITECTURE.md)
- [Building & Compilation](docs/BUILDING.md)
- [Development Setup](docs/DEVELOPMENT.md)
- [Security Policy & Threat Model](docs/SECURITY.md)
- [Permission Architecture](docs/PERMISSIONS.md)
- [Privacy Model](docs/PRIVACY.md)
- [Contributing Guidelines](docs/CONTRIBUTING.md)
- [Release Engineering](docs/RELEASE.md)
- [Third-Party Licenses](LICENSES.md)

---

## License

NEXUS is open-source software licensed under the **[Apache License 2.0](LICENSE)**.
Native inference bridge incorporates components derived from [llama.cpp](https://github.com/ggerganov/llama.cpp) under the **MIT License**.
See [NOTICE](NOTICE) and [LICENSES.md](LICENSES.md) for full attribution notices.
