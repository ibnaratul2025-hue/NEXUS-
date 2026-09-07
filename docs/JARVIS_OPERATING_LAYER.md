# JARVIS Proactive Personal Operating Layer

This document details the architecture and operational mechanics of the **JARVIS Personal Operating Layer**, introduced in Phase 6.

---

## 1. Ambient Presence Engine (`PresenceEngine`)

Unlike passive chat assistants that only respond when spoken to, JARVIS maintains a lightweight, battery-efficient ambient awareness loop.

### 1.1 Environmental Cues Monitored
- **Battery & Power State**: Identifies heavy battery drain, suggests battery preservation modes, and triggers reminders at optimal charge levels (e.g. 80%).
- **Time-of-Day & Schedule**: Understands morning briefings, evening wind-down, and scheduled commitments.
- **Focus Blocks**: Detects continuous working periods (e.g. 90+ minutes of sustained app usage) and proactively recommends micro-breaks.
- **Pending Mission Continuations**: Prompts the user when a paused background mission is ready to resume.

### 1.2 Noise Suppression & Interruption Thresholds
To prevent annoying or intrusive notifications:
- Ambient checks run on a conservative tick cycle.
- Suggestions are debounced: identical notifications will not repeat within configurable cooldown periods (default: 4 hours).
- High-noise hours (sleep window) strictly suppress non-emergency interruptions.

---

## 2. Persistent Working Memory (`WorkingMemory`)

NEXUS separates long-term persistent knowledge (Room SQLite) from high-velocity transient state (`WorkingMemory`):

```kotlin
data class WorkingMemoryState(
    val currentConversationId: String? = null,
    val currentTask: String? = null,
    val currentGoal: String? = null,
    val currentAppContext: String? = null,
    val recentActions: List<RecentActionRecord> = emptyList(),
    val pendingApprovals: List<PendingApprovalRecord> = emptyList(),
    val recentFailures: List<RecentFailureRecord> = emptyList(),
    val temporaryObservations: List<TemporaryObservation> = emptyList()
)
```

### Key Properties:
- **Time-To-Live (TTL)**: Temporary observations (e.g. *"user opened PDF reader"*) automatically expire after 30 minutes unless reinforced.
- **Immediate Context Scratchpad**: Allows the model to reference the last 5 executed tools and recent error codes without incurring database read overhead.

---

## 3. Attention & Interruption Manager (`AttentionManager`)

The Attention Manager computes user availability based on context snapshots:

| Availability State | Condition | Permitted Notifications |
| :--- | :--- | :--- |
| `AVAILABLE` | Device active, ambient mode normal | All verified suggestions and briefings |
| `FOCUSED` | Deep work app active, do-not-disturb on | High-priority and critical mission alerts only |
| `BUSY` | Phone call active, calendar event marked busy | Critical alerts only |
| `SLEEPING` | Within user's designated sleep window (e.g. 23:00–07:00) | Emergency alerts only |

---

## 4. Universal Undo Manager (`UniversalUndoManager`)

Safety requires that any action altering persistent state can be reversed:
- Maintains an auditable stack of reversible operations in SQLite (`action_history`).
- Every reversible action records:
  - Invoked capability (e.g. `create_file`, `change_setting`).
  - Target resource and original parameter snapshot.
  - Cryptographic action hash.
  - Inverse execution lambda.
- Single-tap **Undo** available via the JARVIS HUD, Decision Center, or chat prompt: *"Undo last action"*.

---

## 5. Autonomous Mission Engine (`MissionEngine`)

Missions represent complex, long-running goals that require multiple sequential operations (e.g. *"Audit my downloads folder, organize images by date, and summarize unread documents"*).

### Mission Lifecycle State Machine:
```
 [START] ──► PLANNING ──► EXECUTING ──► VERIFYING ──► COMPLETED
                             │             │
                             ▼             ▼
                      PAUSED (Pending)   FAILED (With auto-retry)
                             │
                             ▼
                          RESUMED
```

- **Checkpoints**: Mission state is saved to Room after every completed step. If the app is killed by the OS or the device reboots, missions resume from their last verified checkpoint.
- **User Intervention Point**: If a step encounters a `HIGH` risk action or requires an ungranted permission, the mission transitions to `PAUSED` and surfaces a clear approval card to the user.

---

## 6. JARVIS Presence HUD (`JarvisPresenceScreen`)

The primary interaction interface for JARVIS features:
- **Pulsing Voice Visualizer Orb**: Organic multi-ring animation reflecting model cognitive load, ambient listening, and token streaming.
- **Ambient Timeline**: Stream of proactive observations, context triggers, and suggested quick-actions.
- **Mission Deck**: Real-time progress indicators for active and background missions.
- **Quick Action Triggers**: Instant access to Daily Briefing, Deep Work Focus, System Telemetry, and Emergency Stop.
