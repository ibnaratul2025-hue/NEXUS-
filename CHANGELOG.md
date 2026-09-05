# Changelog

All notable changes to the **NEXUS** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Additional on-device tool integrations (Calendar, Media player control, Notifications).
- Multi-threaded token generation optimizations for heterogeneous CPU architectures.

---

## [0.1.0] - 2026-09-05

### Added
- **Core Agent Kernel**: Deterministic execution loop separating LLM planning from tool execution.
- **On-Device GGUF Inference Engine**: Native JNI bridge (`llama-jni.cpp`) with RAII memory management and callback-based token streaming.
- **Anti-Hallucination Validator**: Independent verification layer ensuring model claims strictly align with immutable `ToolReceipt` outcomes.
- **Error Recovery Engine**: Automatic self-correction for missing arguments, sandboxing violations, and invalid tool names.
- **Android Permission & Capability Matrix**: Dynamic runtime permission requests, graceful degradation for restricted device capabilities, and safe sandboxing (`FileSandboxHelper`).
- **PolicyEngine**: Deterministic 4-tier risk classification (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) requiring explicit cryptographic user confirmation before high-risk actions.
- **Auditable Room Database**: Local SQLite persistence for long-term memory, session state, and execution history.
- **Material 3 Agent Monitor UI**: Real-time token streaming display, tool audit trail inspector, model switcher, and device settings dashboard.
- **GitHub Release Engineering**: Automated CI/CD workflows for linting, unit tests, automated releases, SHA-256 checksum generation, and CodeQL security scanning.

### Security
- Prohibited prompt injection vectors by enforcing deterministic schema validation on all tool arguments.
- Isolated file operations to app-specific internal storage directories (`FileSandboxHelper`).
- Implemented tamper-resistant SHA-256 confirmation tokens for high-risk device modifications.
- Established strict policy forbidding unverified model claims from updating agent state.

### Native
- Integrated CMake build system with Android NDK targeting `arm64-v8a` and `x86_64`.
- Added JNI exception boundaries, null checks, and thread cancellation support in native C++ layer.

### Android
- Configured edge-to-edge layout, Material Design 3 theming, and responsive typography.
- Declared mandatory Android permissions with runtime status checks (`READ_CONTACTS`, `CAMERA`, `POST_NOTIFICATIONS`, `READ_EXTERNAL_STORAGE`).
