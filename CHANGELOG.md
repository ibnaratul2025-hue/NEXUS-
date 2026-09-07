# Changelog

All notable changes to the **NEXUS** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned - v0.5.0 (Heterogeneous NPU Acceleration & Edge Vision)
- Qualcomm QNN and MediaTek NeuroPilot NPU direct offloading for quantized model execution.
- On-device edge vision (Moondream2 / SmolVLM) for real-time visual scene understanding and screenshot parsing.
- On-device TTS (VITS / Kokoro) and STT (Whisper) for completely offline voice interaction loops.

### Planned - v0.6.0 (Mesh P2P Multi-Device Fabric)
- End-to-end encrypted Wi-Fi Direct and BLE mesh protocol for phone ↔ tablet ↔ PC task delegation.
- Distributed CRDT-backed memory synchronizer across personal devices.
- Federated skill transfer across trusted device clusters.

---

## [0.4.0] - 2026-09-07

### Added
- **Digital World Model Engine (`WorldModelEngine`)**:
  - Live 15-domain entity graph representing `USER`, `DEVICE`, `APP`, `FILE`, `PROJECT`, `TASK`, `DOCUMENT`, `SKILL`, `MISSION`, `PERMISSION`, `NOTIFICATION`, `SETTING`, `CONNECTED_DEVICE`, `AVAILABLE_SERVICE`, and `CURRENT_CONTEXT`.
  - Bidirectional relationships (`USES`, `OWNS`, `DEPENDS_ON`, `RUNNING`, `AVAILABLE`, `REQUIRES`, `CREATED_BY`, `RELATED_TO`, `BLOCKED_BY`, `CAN_EXECUTE`).
  - **Epistemic Invariant**: Strict runtime isolation between `FACT` (1.0), `OBSERVATION` (1.0), `PREDICTION` (≤ 0.95), and `ASSUMPTION` (≤ 0.75).
- **Universal Action Fabric (`ActionFabric`)**:
  - Dynamic capability registry with parameter schemas, risk classifications, and post-execution verification rules.
  - Standardized public Android system actions (`open_app`, `open_url`, `read_file`, `create_file`, `move_file`, `launch_intent`, `change_setting`, `send_share_intent`, `create_reminder`, `run_skill`, `start_mission`).
- **Capability Negotiator (`CapabilityNegotiator`)**:
  - 6-stage pre-execution gating pipeline: `Can I do this?` → `Is capability available?` → `Is permission granted?` → `Is target present?` → `Is action safe?` → `Can result be verified?` → `Execute`.
  - Non-hallucination guarantees with exact technical limitation reporting.
- **Sandboxed App Adapter SDK (`AppAdapterSDK`, `AppAdapterManager`)**:
  - Community adapter framework requiring explicit user approval before execution.
  - Built-in safe adapters for **YouTube**, **GitHub**, and **System Files**.
  - Dynamic Android app discovery via `AppCapabilityRegistry`.
- **Accessibility Controller & Hardware Emergency Stop (`AccessibilityController`)**:
  - Semantic node inspection (`resourceId`, `text`, `contentDescription`, `className`) with strict app allowlisting.
  - Dedicated hardware-style Emergency Stop switch that halts automations immediately.
- **Deterministic Workflow Compiler (`WorkflowCompiler`)**:
  - Compiles natural language into structured 5-stage automata (`TRIGGER` → `CONDITIONS` → `ACTIONS` → `VERIFICATION` → `FAILURE HANDLING`).
- **JARVIS Decision Center Screen (`DecisionCenterScreen`)**:
  - Sci-fi inspired multi-tab command console visualizing what NEXUS knows, what it can do, active preparations, and introspective self-awareness.

### Security
- Inviolable Hard Security Boundary: Permission policy, security policy, confirmation requirements, audit system, sandbox, accessibility restrictions, Android OS boundaries, user privacy settings, and kill switches can never be overridden by AI learning or prompt injection.

---

## [0.3.0] - 2026-09-06

### Added
- **JARVIS Proactive Presence Engine (`PresenceEngine`)**:
  - Ambient awareness monitoring device state, battery thresholds, focus blocks, and time-of-day cues.
- **Persistent Working Memory (`WorkingMemory`)**:
  - High-velocity in-memory scratchpad with TTL-based expiration for temporary observations, current focus, and pending approvals.
- **Universal Undo Manager (`UniversalUndoManager`)**:
  - Reversible action tracking and atomic rollback execution for safe recovery.
- **Attention & Interruption Manager (`AttentionManager`)**:
  - Conservative heuristics for user availability (`AVAILABLE`, `BUSY`, `FOCUSED`, `SLEEPING`) and quiet hour enforcement.
- **Autonomous Mission Engine (`MissionEngine`)**:
  - Background goal execution with multi-step plans, pause/resume, checkpointing, and automatic retry.
- **JARVIS Presence HUD Screen (`JarvisPresenceScreen`)**:
  - Animated pulsing voice visualizer orb, ambient timeline, and live mission control panel.

---

## [0.2.0] - 2026-09-05

### Added
- **Cognitive Evolution Architecture**:
  - `PlanningEngine`: Dynamic multi-step intent decomposition and execution plan synthesis.
  - `PlanFeasibilityValidator`: Pre-flight dependency, capability, and permission validation.
  - `CognitiveMemoryEngine`: Decaying memory weights, source tagging, and semantic knowledge graph.
  - `ContradictionDetector`: Semantic consistency checks to prevent belief collisions.
  - `SkillEngine`: Dynamic code/action synthesis with automated sandbox unit test validation.
  - `LearningEngine`: Automated failure classification (`TOOL_EXECUTION_FAILURE`, `PERMISSION_DENIED`, etc.) and learning loops.
  - `ModelBenchmarkLab`: Automated on-device benchmark harness measuring generation speed, time-to-first-token, and RAM usage.
  - `CognitiveDashboardScreen`: Interactive visualization of active skills, plans, and knowledge nodes.

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
