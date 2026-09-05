# Contributing to NEXUS

Thank you for your interest in contributing to **NEXUS**! We welcome contributions from developers, researchers, and designers committed to advancing private, local-first on-device AI.

---

## Code of Conduct

All contributors and maintainers are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md).

---

## Development Environment & Prerequisites

To develop and build NEXUS locally, ensure you have:
- **Java Development Kit (JDK):** JDK 17 or JDK 21 (Temurin or OpenJDK recommended).
- **Android SDK:** API Level 36 (Android 15 / 16 preview), with Platform Tools installed.
- **Android NDK (Optional for native JNI builds):** NDK r26b (`26.1.10909125`) or CMake 3.22.1+.
- **Android Studio:** Ladybug or newer.

---

## Project Structure

```
NEXUS/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/nexus/
│   │   │   ├── core/         # Kernel, PolicyEngine, ToolRegistry, AntiHallucinationValidator
│   │   │   ├── data/         # Room Database, DAOs, Repositories
│   │   │   └── ui/           # Jetpack Compose screens, components, viewmodels
│   │   ├── cpp/              # JNI bridge (CMakeLists.txt, llama-jni.cpp)
│   │   └── res/              # Android resources, strings, icons
│   └── src/test/             # Robolectric, unit, and reliability test suites
├── docs/                     # Architectural, security, privacy, and release specs
└── .github/                  # CI, release, CodeQL workflows & issue templates
```

---

## Building and Testing Locally

From the project root:

```bash
# Run all unit and Robolectric tests
./gradlew test

# Run Android Lint checks
./gradlew lint

# Assemble debug APK
./gradlew assembleDebug

# Clean build artifacts
./gradlew clean
```

> **Note for Local Release Builds:**
> Release builds (`./gradlew assembleRelease`) require signing configuration. If environment secrets are not configured, release builds will deliberately fail with clear instructions to prevent publishing unsigned or debug-signed packages.

---

## Coding Standards

1. **Kotlin Style:** Follow official Kotlin coding conventions and Material 3 design guidelines.
2. **Deterministic Architecture:** Always keep business and tool logic within `core/` and state persistence within `data/`. Never perform I/O or model calls directly inside Composable functions.
3. **Receipt Authority:** When creating new tools in `ToolRegistry`, always return structured, immutable `ToolReceipt` results. Never allow the LLM prompt to dictate tool success states.
4. **Error Handling:** Use typed `ToolError` classes with explicit retry classifications.
5. **No Hardcoded Secrets:** Never commit API keys, personal tokens, keystores, or credentials.

---

## Commit Message Conventions

We follow Conventional Commits:

- `feat(kernel)`: Add new capability or agent kernel feature
- `fix(policy)`: Correct risk classification or confirmation logic
- `test(reliability)`: Add unit test coverage for anti-hallucination guard
- `docs(arch)`: Update architectural or security documentation
- `refactor(ui)`: Improve Jetpack Compose layout or styling
- `ci`: Update GitHub Actions workflows or release automation

---

## Pull Request Process

1. Fork the repository and create a feature branch (`git checkout -b feat/my-improvement`).
2. Implement your changes following coding standards.
3. Verify all tests pass locally:
   ```bash
   ./gradlew test
   ./gradlew lint
   ./gradlew assembleDebug
   ```
4. Commit your changes with a descriptive message.
5. Push to your fork and submit a Pull Request referencing the PR template.
6. Ensure all CI checks (lint, tests, build, CodeQL) pass green.
