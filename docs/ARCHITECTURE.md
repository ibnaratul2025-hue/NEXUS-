# NEXUS System Architecture

This document provides a detailed technical overview of the **NEXUS** system architecture, including execution flow, component boundaries, data persistence, and security controls.

---

## 1. High-Level System Topology

```
┌────────────────────────────────────────────────────────────────────────┐
│                        USER INTERFACE LAYER                            │
│           (Jetpack Compose • MVVM • Reactive StateFlows)               │
│                                                                        │
│   AgentScreen      ToolsScreen      AuditScreen      SettingsScreen    │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ User Command / UI Events
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        AGENT KERNEL RUNTIME                            │
│                                                                        │
│   ┌─────────────────────┐               ┌──────────────────────────┐   │
│   │   ContextBuilder    │               │ AntiHallucinationValidator│  │
│   └──────────┬──────────┘               └────────────▲─────────────┘   │
│              │ Context                               │ Execution Claim  │
│              ▼                                       │ Verification     │
│   ┌─────────────────────┐               ┌────────────┴─────────────┐   │
│   │   ModelManager      │               │   ErrorRecoveryEngine    │   │
│   │ (Local GGUF Bridge) │               └────────────▲─────────────┘   │
│   └──────────┬──────────┘                            │ Error Catch     │
│              │ Tool Call Proposal                    │                 │
│              ▼                                       │                 │
│   ┌─────────────────────┐               ┌────────────┴─────────────┐   │
│   │    PolicyEngine     │──────────────►│       ToolRegistry       │   │
│   │ (Risk Authorization)│ Authorized    │  (Execution & Receipts)  │   │
│   └─────────────────────┘ Action        └────────────┬─────────────┘   │
└──────────────────────────────────────────────────────┼─────────────────┘
                                                       │
                           ┌───────────────────────────┴──────────┐
                           ▼                                      ▼
┌─────────────────────────────────────────┐ ┌─────────────────────────────┐
│          ANDROID HARDWARE & OS          │ │     DATA PERSISTENCE        │
│  (Sandbox Storage, Camera, Contacts,    │ │ (Room SQLite Database:      │
│   PackageManager, Runtime Permissions)  │ │  Memory, Tools, Audits)     │
└─────────────────────────────────────────┘ └─────────────────────────────┘
```

---

## 2. Core Subsystems

### 2.1 Agent Kernel & Context Builder
The `AgentKernel` coordinates the step-by-step cognitive cycle:
1. **Context Formulation**: `ContextBuilder` aggregates active user queries, recent chat history, relevant long-term memories from the Room database, and the JSON schemas of registered tools.
2. **Inference Trigger**: Dispatches context to the `ModelManager`.
3. **Structured Tool Decoding**: Parses the model's output into a `ToolProposal`. If the format is invalid, the `ErrorRecoveryEngine` formats a structured correction prompt.

### 2.2 Model Manager & Native GGUF Bridge
- **Interface**: `LocalModelEngine` defines the contract for local inference.
- **Native Implementation**: `LlamaCppNativeAdapter` interacts with `libllama.so` through JNI.
- **Memory Safety (RAII)**: Native model handles and contexts are encapsulated in C++ pointers (`NexusNativeLlamaContext`) with explicit deallocation routines triggered when models are unloaded or cancelled.
- **Cancellation**: Supports atomic cancellation (`nativeCancel`) to interrupt long generation loops without leaking memory.

### 2.3 PolicyEngine & Authorization
The `PolicyEngine` enforces a 4-tier risk classification:
- **`LOW`**: Read-only operations without privacy implications (e.g., listing installed apps, checking device battery). Executed automatically.
- **`MEDIUM`**: Read-only operations involving sensitive personal data (e.g., reading contacts, reading internal files). Auto-approved only if permission is granted and context matches.
- **`HIGH`**: Operations that alter persistent state or hardware (e.g., creating files, taking photos, modifying settings). Requires explicit user confirmation via a signed action hash.
- **`CRITICAL`**: Irreversible actions (e.g., recursive file deletion, session wipes). Requires double-factor explicit confirmation dialogue.

### 2.4 ToolRegistry & Real Tool Implementations
Every tool implements the `AgentTool` interface:
```kotlin
interface AgentTool {
    val id: String
    val name: String
    val description: String
    val argumentSchema: ToolSchema
    val riskLevel: RiskLevel
    val requiredPermissions: List<String>
    suspend fun execute(args: Map<String, Any?>): ToolReceipt
}
```
Available tools:
- **`LaunchAppTool`**: Resolves launch intent for packages via `PackageManager`.
- **`FileWriteTool` / `FileReadTool`**: Strictly bounded by `FileSandboxHelper` to internal storage (`context.filesDir/sandbox`).
- **`CameraCaptureTool`**: Accesses hardware camera using Camera2 / CameraX contracts.
- **`ContactQueryTool`**: Queries `ContactsContract` with runtime permission enforcement.
- **`MemorySaveTool`**: Persists semantic keys and values into Room SQLite.

### 2.5 AntiHallucinationValidator
The model is **never** treated as the source of truth.
When the model produces text after a tool step, `AntiHallucinationValidator` inspects the text for contradictory claims:
- If a tool execution returned `SUCCESS = false` (e.g., file not found, permission denied), but the model text claims "I have successfully created/deleted the file", the validator intercepts the response.
- The validator replaces the hallucinated claim with a factual correction derived from the `ToolReceipt`.
- Mismatches are categorized (`FILE_CREATION_CONTRADICTION`, `PERMISSION_GRANT_CONTRADICTION`, etc.) and logged to the audit trail.

---

## 3. Data Persistence Layer (Room Database)

- **Database**: `NexusDatabase` (Room SQLite)
- **Entities**:
  - `MemoryEntity`: Long-term key-value semantic facts.
  - `ToolAuditEntity`: Historical ledger of every tool invocation, arguments, execution duration, and `ToolReceipt`.
  - `ModelMetadataEntity`: Catalog of imported GGUF models, quantization formats, and context configurations.
- **DAOs**:
  - `MemoryDao`, `AuditDao`, `ModelDao` provide clean coroutine-based Flow queries.

---

## 4. UI Layer Architecture

Built with 100% Jetpack Compose following Material Design 3 guidelines:
- **`AgentScreen`**: Interactive terminal displaying live token generation, user input, confirmation prompts, and anti-hallucination alerts.
- **`ToolsScreen`**: Live capability matrix showing installed tools, permission statuses, and sandbox health.
- **`AuditScreen`**: Transparent timeline of executed actions, tool arguments, and cryptographic receipts.
- **`SettingsScreen`**: On-device model controls, generation parameters (temperature, top_p, max_tokens), and About metadata.
