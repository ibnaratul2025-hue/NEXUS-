# NEXUS

<p align="center">
  <strong>The Sovereign, Local-First Autonomous AI Assistant and Personal Operating Intelligence for Android</strong>
</p>

<p align="center">
  <a href="https://github.com/nexus-ai/nexus/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/nexus-ai/nexus/ci.yml?branch=main&style=flat-square&label=CI%20Build" alt="CI Status" /></a>
  <a href="https://github.com/nexus-ai/nexus/releases"><img src="https://img.shields.io/badge/Release-v0.4.0-blue.svg?style=flat-square" alt="Current Release v0.4.0" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square" alt="License" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat-square&logo=kotlin" alt="Kotlin Version" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-API%2026%2B-green.svg?style=flat-square&logo=android" alt="Android API" /></a>
  <img src="https://img.shields.io/badge/ABIs-arm64--v8a%20%7C%20x86__64-orange.svg?style=flat-square" alt="Supported ABIs" />
  <img src="https://img.shields.io/badge/Inference-GGUF%20%2F%20llama.cpp-cyan.svg?style=flat-square" alt="GGUF llama.cpp" />
  <img src="https://img.shields.io/badge/Cloud%20Dependency-0%25%20Offline-success.svg?style=flat-square" alt="Zero Cloud Dependency" />
</p>

---

## ⚡ What is NEXUS?

**NEXUS** is an open-source, local-first personal AI operating intelligence designed natively for Android. 

Unlike traditional cloud voice assistants that transmit private audio, clipboard contents, app metadata, and personal data to remote corporate clusters, NEXUS runs quantized **GGUF** large language models directly on your device's CPU and NPU hardware via an ultra-low-latency C++ JNI bridge (`llama.cpp`).

NEXUS bridges the gap between conversational LLM reasoning and real-world Android execution through a **Local Digital World Model**, a deterministic **Universal Action Fabric**, an **Anti-Hallucination Verification Layer**, and an ambient **JARVIS Presence Engine**.

> **"Jarvis, handle this."**  
> NEXUS perceives permitted context, builds a world model, negotiates available capabilities, generates verified plans, asks for confirmation only when high risk demands it, executes operations through sandboxed adapters, verifies real-world outcomes, and preserves state—all completely on-device.

---

## 🏛️ System Architecture Topology

```
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                                   USER INTERACTION LAYER                                  │
│                 (Jetpack Compose • Material 3 • Reactive StateFlow Pipelines)             │
│                                                                                           │
│   Agent Terminal    JARVIS Presence HUD    Decision Center    Cognitive Lab    Settings   │
└─────────────────────────────────────────────┬─────────────────────────────────────────────┘
                                              │ Continuous Context / Goals / Voice / Touch
                                              ▼
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                                  NEXUS DIGITAL WORLD MODEL                                │
│        15 Ontological Domains • Bidirectional Semantic Graph • Epistemic Separation       │
│                                                                                           │
│  [User] ──USES──► [App] ──DEPENDS_ON──► [Permission] ◄──BLOCKED_BY── [Security Policy]   │
│    │               │                          │                                           │
│  OWNS           RUNNING                   AVAILABLE                                       │
│    ▼               ▼                          ▼                                           │
│ [Project]      [Device] ──CAN_EXECUTE──► [Skill / Mission] ◄──CREATED_BY── [User]         │
│                                                                                           │
│  Epistemic Truth Guardrails:                                                              │
│  ├── FACT (1.00) ──────── Ground truth verified from Android OS APIs & Hardware           │
│  ├── OBSERVATION (1.00) ── Ephemeral runtime telemetry & system signals                  │
│  ├── PREDICTION (≤ 0.95) ─ Model projected state (bounded confidence)                     │
│  └── ASSUMPTION (≤ 0.75) ─ Unverified heuristics (flagged before execution)               │
└─────────────────────────────────────────────┬─────────────────────────────────────────────┘
                                              │ World Model Entities & Ground Truth Context
                                              ▼
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                           CAPABILITY NEGOTIATOR (6-STAGE GATING)                          │
│                                                                                           │
│  1. Can I do this? ────────► Verify theoretical capability exists                         │
│  2. Is capability available?► Verify tool / adapter / activity registered                 │
│  3. Is permission granted? ─► Query Android runtime permissions & sandbox                 │
│  4. Is target present? ────► Query PackageManager / Storage / Hardware                    │
│  5. Is action safe? ───────► PolicyEngine risk tier evaluation (LOW / MED / HIGH / CRIT)  │
│  6. Can result be verified?► Ensure post-execution verification assertion available      │
└─────────────────────────────────────────────┬─────────────────────────────────────────────┘
                                              │ Validated Objective
                                              ▼
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                                 UNIVERSAL ACTION FABRIC                                   │
│                                                                                           │
│  ┌───────────────────────┐  ┌────────────────────────┐  ┌──────────────────────────────┐ │
│  │ Standard System Tools │  │ Sandboxed App Adapters │  │ Accessibility Controller     │ │
│  │ (Files, Intents, Sys) │  │ (YouTube, GitHub, etc) │  │ (Semantic Nodes, E-Stop)     │ │
│  └───────────┬───────────┘  └───────────┬────────────┘  └──────────────┬───────────────┘ │
│              │                          │                              │                 │
│              └──────────────────────────┼──────────────────────────────┘                 │
│                                         ▼                                                 │
│                        Deterministic Execution Pipeline                                   │
│                        ├── User Confirmation (High/Critical)                              │
│                        ├── Execution & Output Capture                                     │
│                        ├── Post-Execution State Verification                              │
│                        └── Atomic Rollback Registration (Universal Undo)                  │
└─────────────────────────────────────────────┬─────────────────────────────────────────────┘
                                              │ Immutable Tool Receipts
                                              ▼
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                       ANTI-HALLUCINATION & COGNITIVE LEARNING                             │
│                                                                                           │
│   AntiHallucinationValidator ◄── Validates model response against real Tool Receipts      │
│   ContradictionDetector      ◄── Reconciles new observations against long-term memory     │
│   SkillEngine & LearningLoop ◄── Analyzes failures, synthesizes skills in test sandbox    │
└─────────────────────────────────────────────┬─────────────────────────────────────────────┘
                                              │ Audited Persistence
                                              ▼
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                              LOCAL SECURE PERSISTENCE (ROOM)                              │
│         SQLite Encrypted Storage • Zero Cloud Telemetry • Ephemeral Memory TTLs          │
└───────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🌟 The 7 Major Subsystems

### 1. Local GGUF Inference Engine (`llama.cpp` JNI Bridge)
- **High-Performance Native Core**: Native JNI bridge with zero memory copies, compiled with NEON optimizations for ARM64 and AVX2 for x86_64.
- **Strict RAII Lifecycle**: Model weights and contexts are wrapped in native handles, guaranteed to deallocate on task completion or cancellation.
- **Hardware Tier Adaptability**: Dynamically scales context window (512 to 4096 tokens) and thread allocation based on device RAM and CPU cluster topology.

### 2. Digital World Model Engine (`WorldModelEngine`)
- **15 Ontological Domains**: `USER`, `DEVICE`, `APP`, `FILE`, `PROJECT`, `TASK`, `DOCUMENT`, `SKILL`, `MISSION`, `PERMISSION`, `NOTIFICATION`, `SETTING`, `CONNECTED_DEVICE`, `AVAILABLE_SERVICE`, `CURRENT_CONTEXT`.
- **Bidirectional Semantic Relations**: Connects entities with `USES`, `OWNS`, `DEPENDS_ON`, `RUNNING`, `AVAILABLE`, `REQUIRES`, `CREATED_BY`, `RELATED_TO`, `BLOCKED_BY`, `CAN_EXECUTE`.
- **The Epistemic Invariant**: Strictly segregates ground truth from inference:
  - `FACT`: Verified device truth (confidence = 1.0).
  - `OBSERVATION`: Empirically witnessed transient events (confidence = 1.0).
  - `PREDICTION`: Projected model state (confidence bounded ≤ 0.95).
  - `ASSUMPTION`: Inferred intent requiring confirmation (confidence bounded ≤ 0.75).

### 3. Universal Action Fabric (`ActionFabric`)
- **Unified Action Abstraction**: Standardizes actions with explicit schemas, risk levels, parameter sanitization, and verification rules.
- **Reversible Action History & Universal Undo**: Every reversible mutation is stored in `ActionHistoryEntity`, allowing atomic rollbacks if a task fails or the user taps "Undo".
- **Hard Security Boundary**: Confirmation requirements, audit logging, and sandbox limits can **never** be overridden by model prompt injection or learned heuristics.

### 4. Capability Negotiator & Non-Hallucination Guarantee
- **Honest Boundary Enforcement**: If an action is technically impossible (e.g. unsupported device API, lack of root, hardware absence), NEXUS explains the exact architectural reason:
  > *"I cannot execute 'send_satellite_message' because no satellite radio hardware is available on this device and no corresponding system capability exists."*
- **Six-Stage Pre-Execution Gating**: Automatically halts unsafe or missing operations before code execution.

### 5. Sandboxed App Adapter SDK & Dynamic App Discovery
- **Zero-Privilege App Graph**: Scans installed applications for public activities, share targets, and declared capabilities without requesting root.
- **Pluggable App Adapters**: Developer SDK for community integrations (shipped with verified adapters for **YouTube**, **GitHub**, and **System Files**).
- **User Sandboxing Gate**: Third-party adapters cannot run until explicitly approved by the user in the Decision Center.

### 6. Accessibility Action Layer & Hardware-Style Emergency Stop
- **Semantic UI Understanding**: Inspects UI elements by `resourceId`, `text`, `contentDescription`, and `className`—never brittle fixed screen coordinates.
- **Physical Emergency Stop**: Global kill switch halts all active automations immediately.
- **Privacy First**: Screen inspection is strictly ephemeral and never stored or cached.

### 7. JARVIS Presence & Proactive Operating Layer
- **Ambient Awareness Engine**: Detects user focus, battery thresholds, and activity milestones to offer non-intrusive proactive assistance.
- **Continuous Autonomous Missions**: Long-running goal execution with state machines, automatic re-planning, and checkpointing.
- **JARVIS Presence HUD**: Sci-fi inspired animated voice visualizer orb, ambient timeline, and live status telemetry.
- **Decision Center & Self-Awareness Panel**: Fully transparent introspective console answering *"What NEXUS Knows"*, *"What NEXUS Can Do"*, *"What NEXUS Is Preparing"*, and *"What NEXUS Needs"*.

---

## 🔒 Hard Security Boundaries

NEXUS enforces inviolable architectural invariants that can **never** be bypassed:

| Boundary | Enforcement Mechanism | Failure Action |
| :--- | :--- | :--- |
| **Permission Policy** | Android OS `checkSelfPermission` before any API call | Hard block; prompts user via OS dialog |
| **High-Risk Confirmation** | Cryptographic SHA-256 action hash confirmation | Requires explicit button press from user |
| **File Sandbox** | Path canonicalization strictly bounded to `context.filesDir/sandbox` | Throws `SecurityException`; logs attempt |
| **Audit System** | Append-only Room SQLite ledger | Cannot be disabled by model or tool |
| **Emergency Stop** | Atomic Boolean flag checked before every interaction | Instant execution abort |
| **Adapter Sandbox** | Explicit whitelist stored in Room database | Unapproved adapters blocked at call-site |
| **Model Authority** | `AntiHallucinationValidator` rejects unverified claims | Replaces model response with real receipt |

---

## 📊 Hardware Benchmarks & Model Compatibility

NEXUS executes GGUF models completely on-device without cloud compute.

| Device Tier | Sample Chipsets | Recommended Model | Quantization | RAM Required | Typical Speed |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Tier 1 (Flagship)** | Snapdragon 8 Gen 2 / 3, Tensor G3/G4, Dimensity 9300 | Llama 3.2 3B Instruct | `q4_k_m`, `q5_k_m` | 8GB–16GB | 14–22 tokens/sec |
| **Tier 2 (Mid-Range)** | Snapdragon 7+ Gen 2, Dimensity 8200, Tensor G2 | Llama 3.2 1B / Qwen 2.5 1.5B | `q4_k_m` | 6GB–8GB | 18–28 tokens/sec |
| **Tier 3 (Budget)** | Snapdragon 695, Helio G99, Exynos 1280 | Qwen 2.5 0.5B / SmolLM2 1.7B | `q4_k_m` | 4GB–6GB | 12–20 tokens/sec |

---

## 🚀 Getting Started & Local Build

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or newer
- **JDK 17** or **JDK 21**
- **Android SDK Platform 36** (Compile SDK 36, Min SDK 26)
- **Android NDK r26b+** (for native C++ JNI compilation)
- **CMake 3.22.1+**

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/nexus-ai/nexus.git
cd nexus

# Run all unit and Robolectric tests
gradle :app:testDebugUnitTest

# Run code style and linting
gradle :app:lintDebug

# Build Debug APK
gradle :app:assembleDebug
```

The APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Sideloading a Model
1. Download a compatible quantized `.gguf` file (e.g. from Hugging Face).
2. Transfer it to your device's Downloads directory:
   ```bash
   adb push qwen2.5-1.5b-instruct-q4_k_m.gguf /sdcard/Download/
   ```
3. In NEXUS, tap **Models** in the bottom navigation bar, tap **Import Model File**, select the `.gguf` file, and tap **Mount Model**.

---

## 🗺️ Product Roadmap & Release Versions

```
  v0.1.0 ────────► v0.2.0 ────────► v0.3.0 ────────► v0.4.0 ────────► v0.5.0 ────────► v1.0.0
 Foundation      Cognitive        JARVIS          World Model      NPU Vision        Sovereign
 (Shipped)       Evolution        Presence        & Action         & Multimodal       Android OS
                 (Shipped)        (Shipped)       (Current)        (Upcoming)         (Vision)
```

### [v0.1.0] — Foundation (Shipped)
- Core Agent Kernel with deterministic execution loop.
- Local GGUF inference via `llama.cpp` C++ JNI bridge.
- Immutable `ToolReceipt` validation and Anti-Hallucination Gate.
- 4-Tier Risk Policy Engine (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
- Room SQLite audit journal and file sandboxing.

### [v0.2.0] — Cognitive Evolution (Shipped)
- Multi-Step Dynamic Planning (`PlanningEngine`, `PlanFeasibilityValidator`).
- Long-Term Cognitive Memory with decay models, contradiction detection, and knowledge graph.
- Self-Improving Skill Engine with sandboxed skill compilation and automated self-testing.
- Local Model Benchmark Lab with automated speed and memory profiling.

### [v0.3.0] — JARVIS Personal Operating Layer (Shipped)
- Ambient Awareness & Proactive Presence Engine.
- Persistent Working Memory with TTL decay.
- Universal Undo Manager with reversible action history.
- Attention Manager & Interruptibility heuristics (Quiet Hours, Focus Mode).
- Autonomous Mission Engine with state checkpoints and retry logic.
- JARVIS Presence HUD with real-time voice visualization orb.

### [v0.4.0] — Digital World Model + Universal Action Fabric (Current Release)
- **15-Domain Digital World Model** with bidirectional semantic relations.
- **Epistemic Invariant**: Strict segregation of `FACT`, `OBSERVATION`, `PREDICTION`, `ASSUMPTION`.
- **Universal Action Fabric**: Unified dynamic capability registry with parameter schemas.
- **Capability Negotiator**: 6-stage pre-execution gating with non-hallucinating explanations.
- **Sandboxed App Adapter SDK**: Community adapter framework (YouTube, GitHub, Files).
- **Accessibility Controller**: Semantic UI tree selection with physical Emergency Stop switch.
- **Deterministic Workflow Compiler**: Compiles natural language into 5-stage automata.
- **JARVIS Decision Center**: Complete transparency into what NEXUS knows, can do, and needs.

### [v0.5.0] — Heterogeneous NPU Acceleration & Edge Vision (Upcoming)
- Qualcomm QNN / MediaTek NeuroPilot NPU direct offloading for 5x inference speedup.
- Local edge vision (Moondream2 / SmolVLM) for real-time camera and screenshot understanding.
- On-device speech synthesis (VITS / Kokoro) and Whisper speech-to-text with zero cloud calls.

### [v0.6.0] — Mesh P2P Multi-Device Fabric (Planned)
- Encrypted local Wi-Fi Direct and BLE cross-device task dispatching (Phone ↔ Tablet ↔ PC).
- Distributed memory synchronization using Conflict-Free Replicated Data Types (CRDTs).
- Federated local skill sharing across trusted personal devices.

### [v1.0.0] — The Sovereign Personal Operating System (Vision)
- Complete system-level operating layer for Android devices.
- Fully autonomous ambient assistance with end-to-end user privacy.
- Zero tracking, zero telemetry, zero subscription fees—forever free and open source.

---

## 💡 What Makes NEXUS Unique?

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                             WHAT MAKES NEXUS DIFFERENT                           │
├───────────────────────────────┬──────────────────────────────────────────────────┤
│ Traditional AI Assistants     │ NEXUS Autonomous Operating Intelligence          │
├───────────────────────────────┼──────────────────────────────────────────────────┤
│ ☁️ Cloud servers process data  │ 🔒 100% on-device CPU/NPU processing             │
│ 🎭 Hallucinates completed work │ 🛡️ Immutable Tool Receipts verify every action   │
│ 🚫 Blind to system realities   │ 🌐 Live 15-Domain Digital World Model            │
│ ⚠️ Opaque execution           │ 🔍 Full Decision Center & Self-Awareness Panel   │
│ ⛓️ Proprietary & Locked Down   │ 📜 Apache 2.0 Open Source & Sandboxed Adapters   │
│ 📢 Prompts trigger actions    │ 🛑 Hard Security Boundary & Physical E-Stop      │
└───────────────────────────────┴──────────────────────────────────────────────────┘
```

---

## 📚 Documentation Index

- [System Architecture](docs/ARCHITECTURE.md)
- [World Model & Universal Action Fabric](docs/WORLD_MODEL_ACTION_FABRIC.md)
- [JARVIS Operating Layer](docs/JARVIS_OPERATING_LAYER.md)
- [Cognitive Evolution Engine](docs/COGNITIVE_EVOLUTION.md)
- [Security Policy & Threat Model](docs/SECURITY.md)
- [Permission Architecture](docs/PERMISSIONS.md)
- [Privacy Model](docs/PRIVACY.md)
- [Building & Compilation](docs/BUILDING.md)
- [Development Setup](docs/DEVELOPMENT.md)
- [Release Engineering](docs/RELEASE.md)
- [Contributing Guidelines](CONTRIBUTING.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Third-Party Licenses](LICENSES.md)

---

## 🤝 Contributing

We welcome contributions from developers, researchers, and privacy advocates! Please see [CONTRIBUTING.md](CONTRIBUTING.md) and our [Code of Conduct](CODE_OF_CONDUCT.md) before submitting pull requests.

---

## 📄 License

NEXUS is open-source software licensed under the **[Apache License 2.0](LICENSE)**.  
Native inference components derive from [llama.cpp](https://github.com/ggerganov/llama.cpp) under the **MIT License**.  
See [NOTICE](NOTICE) and [LICENSES.md](LICENSES.md) for full attribution notices.
