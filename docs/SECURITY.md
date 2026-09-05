# NEXUS Security Architecture & Threat Model

This document outlines the detailed security architecture, threat vectors, mitigations, and operational guarantees implemented across **NEXUS**.

---

## 1. Threat Model & Assumptions

### 1.1 Untrusted Model Assumption
In NEXUS, **language models are strictly assumed to be untrusted components**.
A model can:
- Be influenced by prompt injections embedded in user files, scraped text, or web content.
- Suffer from catastrophic hallucinations or state delusions.
- Propose destructive tool calls or parameter values.

Therefore:
- Model output is **never executed directly as arbitrary code**.
- Model claims cannot alter system state without real `ToolReceipt` verification.
- Tools only accept validated, strongly-typed JSON parameters matching predefined schemas.

### 1.2 Device Security Boundaries
- NEXUS adheres strictly to the Android Application Sandbox.
- NEXUS does not request `root` privileges and cannot bypass OS-level permission gates.
- Operations involving external storage or dangerous permissions require explicit Android OS permission grants from the user.

---

## 2. Core Security Controls

```
┌────────────────────────────────────────────────────────┐
│               UNTRUSTED GGUF INFERENCE                 │
│                 (Proposed Tool Call)                   │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                   POLICY ENGINE                        │
│ 1. Schema Validation (types, bounds, unexpected keys)  │
│ 2. Risk Classification (LOW, MEDIUM, HIGH, CRITICAL)   │
│ 3. Confirmation Evaluation                             │
└───────────────────────────┬────────────────────────────┘
                            │
               ┌────────────┴────────────┐
               ▼                         ▼
         [LOW / MEDIUM]           [HIGH / CRITICAL]
         Direct Execution         Explicit User Confirmation
               │                  with SHA-256 Action Hash
               │                         │ (Approved)
               └────────────┬────────────┘
                            ▼
┌────────────────────────────────────────────────────────┐
│                  SANDBOXED EXECUTION                   │
│ 1. FileSandboxHelper prevents path traversal           │
│ 2. Android OS runtime permission check                 │
│ 3. Returns immutable ToolReceipt                       │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│              ANTI-HALLUCINATION GATEWAY                │
│ Verifies LLM response against actual ToolReceipt state │
└────────────────────────────────────────────────────────┘
```

---

## 3. Sandboxing & Path Traversal Prevention

The `FileSandboxHelper` protects device storage by strictly enforcing path boundaries:

1. **Internal Storage Jail**: All file operations (`FileWriteTool`, `FileReadTool`, `FileDeleteTool`) are restricted to the application's internal sandboxed directory:
   `context.filesDir.resolve("sandbox")`
2. **Canonical Path Verification**: Before accessing any file, the helper resolves the canonical path:
   ```kotlin
   val resolved = sandboxDir.resolve(requestedPath).canonicalFile
   if (!resolved.path.startsWith(sandboxDir.canonicalPath)) {
       throw SecurityException("Path traversal attempt blocked: $requestedPath")
   }
   ```
3. **No Dot-Dot (`..`) Escapes**: Any attempt to escape the sandbox via `../` or symlinks fails immediately with a security violation.

---

## 4. PolicyEngine & Cryptographic Confirmation Tokens

High-risk actions cannot be executed automatically:
1. When a `HIGH` or `CRITICAL` risk tool is proposed, the `PolicyEngine` generates a cryptographically random confirmation request including:
   - Tool identifier
   - Formatted arguments
   - Action hash: `SHA-256(toolId + timestamp + serializedArgs)`
2. The UI renders an explicit, non-bypassable confirmation dialogue.
3. The execution engine only proceeds if the confirmation matches the pending action hash.

---

## 5. Anti-Hallucination Gate

The `AntiHallucinationValidator` prevents the model from misleading users about real-world device state:
- If a tool execution fails, but the LLM claims success, the validator intercepts the text before display.
- The validator injects a clear factual report:
  *"Action could not be completed: [Error Details]. The system did not perform this operation."*
- Contradiction occurrences are permanently recorded in the local SQLite audit ledger.

---

## 6. Native JNI Isolation & Memory Safety

The native C++ bridge (`llama-jni.cpp`) adheres to strict defensive programming:
- **RAII Resource Management**: Context memory (`NexusNativeLlamaContext`) is tied to C++ objects freed deterministically via `delete`.
- **Atomic Cancellation**: Interrupt flags are managed via `std::atomic<bool>`.
- **Null Pointer Checks**: Every JNI pointer argument is checked before dereferencing.
- **Exception Boundaries**: Native operations are wrapped in C++ `try/catch` blocks returning error status codes rather than terminating the JVM process.
