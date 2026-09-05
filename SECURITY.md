# Security Policy

The NEXUS team and contributors take the security and privacy of on-device AI applications seriously. We welcome security researchers and community members reporting security vulnerabilities responsibly.

---

## Supported Versions

Only the latest stable release and the default branch (`main`) receive active security updates and vulnerability patches.

| Version | Supported | Notes |
| :--- | :--- | :--- |
| **0.1.x** (Current) | :white_check_mark: | Active support |
| **main** branch | :white_check_mark: | Development branch |
| **< 0.1.0** | :x: | Deprecated preview builds |

---

## Reporting a Vulnerability

**Please do NOT report security vulnerabilities through public GitHub issues, discussions, or social media.**

To report a vulnerability:
1. Navigate to the **Security** tab of the NEXUS GitHub repository.
2. Select **Advisories** and click **Report a vulnerability** to open a private advisory draft.
3. Provide:
   - Detailed description of the vulnerability and attack vector.
   - Exact steps to reproduce or Proof-of-Concept (PoC) code.
   - Affected components (e.g., JNI bridge, PolicyEngine, Room database, Sandbox traversal).
   - Potential impact assessment.

### Expected Response Timeline
- **Initial Acknowledgment:** Within **48 hours**.
- **Triage & Reproduction:** Within **5 business days**.
- **Fix & Advisory Publication:** Typically within **14 to 30 days**, coordinated under mutual disclosure.

---

## Scope & Out-of-Scope

### In Scope
- Memory corruption or arbitrary code execution in native JNI components (`libllama.so` / `llama-jni.cpp`).
- Sandboxing bypasses, path traversal outside app-specific storage (`FileSandboxHelper`).
- Bypass of the `PolicyEngine`, unconfirmed high-risk execution, or authorization state corruption.
- Cryptographic hash tampering in confirmation requests (`actionHash`).
- Leakage of private long-term semantic memory or audit logs to unprivileged applications.
- Contradiction or hallucination verification bypasses that result in unverified physical actions.

### Out of Scope
- Attacks requiring physical root access or compromised OS/custom ROM on the user's device.
- Standard model hallucinations or inaccuracies that do NOT trigger unauthorized physical device actions or bypass the `AntiHallucinationValidator`.
- Social engineering attacks coercing the user into manually pressing "Confirm" on an explicit confirmation dialogue.
- Denial of Service (DoS) caused by intentionally loading corrupt or excessively large GGUF files exceeding physical device RAM.

---

## Core Security Architecture Principles

1. **The Model is Untrusted:**
   Output from on-device GGUF language models or cloud models is treated as untrusted text. The model cannot execute code, issue system commands, or access system services directly.
2. **PolicyEngine is Authoritative:**
   Every tool invocation must be evaluated against deterministic policies and risk tiers (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
3. **Receipts are Authoritative:**
   The `AntiHallucinationValidator` verifies every model claim against immutable, runtime-generated `ToolReceipt` records.
4. **JNI Boundary Isolation:**
   C++ native contexts operate under strict RAII lifecycle management, bound by pointer handles, and isolated from JVM heap memory.
5. **No Bundled Third-Party Weights:**
   NEXUS does not bundle model weights or secret keys. User-imported GGUF models are validated with strict format inspection prior to loading.

For comprehensive technical threat modeling, please review [docs/SECURITY.md](docs/SECURITY.md).
