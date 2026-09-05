# NEXUS Phase 5: Cognitive Evolution Engine

This document details the **Cognitive Evolution Engine** integrated into NEXUS. This architecture upgrades NEXUS into a local-first, continuously self-improving cognitive system that operates without remote telemetry or cloud dependency while adhering strictly to safety, privacy, and truth-first invariants.

---

## 1. High-Level Cognitive Architecture Pipeline

The NEXUS cognitive loop enforces a strict truth-first sequence:

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. CONTEXT INTELLIGENCE (CognitiveContextManager)                      │
│    - Trust Labels: SYSTEM, VERIFIED_TOOL, USER, LOCAL_MEMORY, EXTERNAL │
│    - Dynamic Token Budget Allocation & Decay Filtering (<0.15 pruned)  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. INTENT CLASSIFICATION (IntentClassifier)                           │
│    - Multi-Intent Disambiguation: QUESTION, COMMAND, TASK,            │
│      MULTI_STEP_TASK, LEARNING_REQUEST, MEMORY_REQUEST, SKILL_REQUEST, │
│      WORKFLOW_REQUEST, SYSTEM_REQUEST, UNKNOWN                         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. INVARIANT & LIMITATION CHECK (LimitationRegistry)                   │
│    - Enforces hard boundaries (root, destructive wiping, cloud sync)  │
│    - Fails fast if request demands impossible OS capability            │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 4. PLANNING & FEASIBILITY (PlanningEngine & PlanFeasibilityValidator)  │
│    - Goal, Ordered Steps, Dependencies, Tools, Risk, Rollback Action  │
│    - Pre-execution validation: Missing permissions, unavailable sensor │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 5. RISK GATE & POLICY APPROVAL (PolicyEngine & CancellationController) │
│    - LOW/MEDIUM: Autonomous or safe execution                          │
│    - HIGH/CRITICAL: Cryptographic Action Hash & Explicit User Consent │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 6. DETERMINISTIC TOOL EXECUTION (ToolRegistry & Android OS)            │
│    - Direct invocation of sandbox, package manager, sensors, camera   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 7. IMMUTABLE TOOL RECEIPT (ToolReceipt)                                │
│    - Status: SUCCESS, FAILED, PERMISSION_REQUIRED, CANCELLED           │
│    - Execution timestamp, structured data, verified error codes        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 8. TRUTH-FIRST LEARNING (LearningEngine & FailureClassifier)           │
│    - Learns ONLY from verified Tool Receipts and explicit corrections  │
│    - Categorizes failures: PERMISSION, LIMITATION, ENVIRONMENT, TOOL,  │
│      AMBIGUITY, MODEL                                                  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 9. COGNITIVE MEMORY & KNOWLEDGE GRAPH (CognitiveMemoryEngine)          │
│    - Exponential Decay Calculator (Recency, Frequency, Entity Type)    │
│    - Contradiction Detector (Theme, Editor, Negation -> SUPERSEDE)     │
│    - Privacy Guard: Blocks unauthorized personal sensitive attributes  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 10. EXPLAINABILITY & PROACTIVE SUGGESTIONS (ExplainabilityEngine)      │
│    - Generates user-facing DecisionExplanation with evidence and costs │
│    - ProactiveEngine surfaces actionable suggestions requiring approval│
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Intent Classification Engine

The `IntentClassifier` analyzes user utterances to extract goals, detect structural complexity, and map inputs to one of 10 structured intent types:

| Intent Type | Trigger Pattern | Planning Required | Example Utterance |
| :--- | :--- | :--- | :--- |
| **`QUESTION`** | Interrogative prefixes (`what`, `who`, `why`, `how`, `?`) | No | *"What is the capital of France?"* |
| **`COMMAND`** | Imperative action verbs (`open`, `launch`, `delete`, `list`) | No | *"Open Chrome"* |
| **`TASK`** | Single-goal autonomous instructions | Yes | *"Analyze available sandbox space"* |
| **`MULTI_STEP_TASK`** | Sequential conjunctions (`then`, `and then`, `after that`) | Yes | *"First inspect storage then backup files and notify me"* |
| **`LEARNING_REQUEST`**| User corrections and explicit preferences (`remember that`) | No | *"Remember that my preferred editor is Vim"* |
| **`MEMORY_REQUEST`**  | Explicit memory inspection or purge requests | No | *"What do you remember about my projects?"* |
| **`SKILL_REQUEST`**   | Skill creation, testing, or invocation | Yes | *"Create a new skill for nightly backup"* |
| **`WORKFLOW_REQUEST`**| Routines, schedulers, and periodic actions | Yes | *"Automate my weekly battery audit"* |
| **`SYSTEM_REQUEST`**  | Hardware telemetry, battery, CPU, RAM queries | No | *"What is the current battery level and device info?"* |
| **`UNKNOWN`**         | Empty or unclassifiable inputs | No | *""* |

---

## 3. Planning and Feasibility Verification

Every multi-step or autonomous task is compiled into an `ExecutionPlan`:
- **`goal`**: The high-level user intention.
- **`steps`**: Ordered list of `CognitivePlanStep` objects.
  - `stepNumber`, `toolId`, `description`
  - `arguments`: Concrete key-value arguments
  - `dependencies`: Step IDs that must succeed before this step executes
  - `requiredCapabilities`: Hardware capabilities (e.g., `camera.photo`, `storage.sandbox`)
  - `requiredPermissions`: Android runtime permissions
  - `riskLevel`: LOW, MEDIUM, HIGH, CRITICAL
  - `expectedResult`: Expected post-condition
  - `rollbackStrategy`: Optional compensation action (e.g. file deletion on copy failure)

### Feasibility Checks
Before any execution begins, `PlanFeasibilityValidator` inspects:
1. **Tool Existence (Impossible Assumptions)**: Fails immediately if an unregistered tool is referenced.
2. **Missing Permissions**: Flags missing Android runtime permissions before invoking tools.
3. **Hardware Availability**: Queries `LiveCapabilityRegistry` to confirm physical hardware/sensor availability.
4. **Destructive Actions**: Ensures that high-risk or destructive actions include user confirmation and rollback compensation.

---

## 4. Truth-First Learning & Failure Classification

NEXUS never treats language model outputs as factual. All learning events logged into Room persistence must stem from **verified Tool Receipts** or **explicit user corrections**:

### Failure Classification
When an action fails, `FailureClassifier` assigns one of six deterministic categories:
- **`PERMISSION`**: Android OS denied permission (e.g., Camera or Storage not granted).
- **`LIMITATION`**: Action requested violates invariant boundaries (e.g. root command, cloud sync).
- **`ENVIRONMENT`**: OS environment state failure (e.g., File not found, network offline).
- **`TOOL`**: The tool itself produced an internal execution error or timeout.
- **`AMBIGUITY`**: The user cancelled the operation or parameters were ambiguous.
- **`MODEL`**: The model emitted malformed syntax or hallucinatory parameters.

### Safety Guarantee
NEXUS **never** performs unprompted or silent modifications to executable code or system security policies. Self-improvement proposals generated by `SelfImprovementEngine` follow the mandatory pattern:
```
PROPOSE → USER APPROVES IN UI → APPLY
```

---

## 5. Memory Decay, Contradiction, and Privacy

The `CognitiveMemoryEngine` pairs persistent Room storage with a lightweight Knowledge Graph:

### Decay Model
Memory relevance decays exponentially based on recency, access frequency, and entity type:
$$\text{Score} = \exp\left(-\lambda \times \Delta t_{\text{days}}\right)$$
- **Half-Life by Source**:
  - `USER_EXPLICIT` / `USER_CORRECTION`: 180 days (high persistence)
  - `SYSTEM_FACT`: 365 days
  - `SUCCESSFUL_WORKFLOW`: 90 days
  - `OBSERVED_RESULT`: 14 days
  - `INFERRED`: 7 days (rapid decay)
- **Entity Multiplier**: Preferences and Habits decay slower ($1.5\times$ half-life); ephemeral tasks decay faster ($0.8\times$).
- **Pruning**: Memories with decay scores below $0.15$ are filtered from model context; scores below $0.25$ can be pruned on demand.

### Contradiction Detection
When a new memory is proposed, `ContradictionDetector` evaluates semantic overlaps:
- Conflicting theme preferences (*"User prefers dark mode"* vs *"User prefers light mode"*)
- Opposing tool or editor preferences (*"Vim"* vs *"VS Code"*)
- Direct semantic negations
When a contradiction is verified, the prior memory is automatically marked as `isSuperseded = true` and linked to the new entry.

### Privacy Guard
The system actively blocks the unprompted inference of sensitive personal attributes (including financial credentials, passwords, biometric records, political/religious affiliations, or health data) without explicit user intent.

---

## 6. Skill System & Lifecycle

NEXUS encapsulates reusable capabilities as modular Skills:

```
[DISCOVERED] ──► [DRAFT] ──► [TESTING] ──► [VERIFIED] ──► [USER_APPROVED] ──► [ACTIVE]
                               │
                       Failed Sandbox
                               ▼
                            [DRAFT]
```

1. **Discovery**: Recurring successful workflows are synthesized into parameter-templated skill recipes.
2. **Sandbox Testing**: `SkillTestRunner` executes dry-run validation against tool argument schemas without side effects.
3. **User Approval**: A skill cannot be activated without explicit user approval.
4. **Version Control & Rollback**: Skills maintain integer version numbers and can be rolled back to prior revisions.
5. **Portable JSON Schema**: Skills can be exported and imported as structured JSON files.

---

## 7. Context Intelligence & Trust Hierarchy

To prevent prompt injection and maintain strict token economy, `CognitiveContextManager` segments inputs by source and assigns **Trust Labels**:

1. **`SYSTEM`** (Trust Tier 1 — Authoritative): Core safety rules, capability boundaries, tool contracts.
2. **`VERIFIED_TOOL`** (Trust Tier 2 — Empirical): Cryptographic tool execution receipts.
3. **`USER`** (Trust Tier 3 — Instruction): Direct user commands.
4. **`LOCAL_MEMORY`** (Trust Tier 4 — Informational): Persistent facts stored in on-device Room database.
5. **`EXTERNAL`** (Trust Tier 5 — Untrusted): External files, scanned web data, or third-party inputs.

---

## 8. Model Benchmark Lab

The on-device benchmark suite evaluates local quantized models without network access:
- **First Token Latency (TTFT)**: Time to emit the initial token from cold/warm state.
- **Throughput (Tokens/sec)**: Generation velocity during reasoning.
- **RAM Peak Consumption**: Runtime resident set size (RSS) via `Debug.MemoryInfo`.
- **JSON Tool Reliability**: Deterministic parsing validation on structured JSON function-call syntax.
- **Cancellation Latency**: Speed of thread interruption and context recycling upon user emergency stop.
