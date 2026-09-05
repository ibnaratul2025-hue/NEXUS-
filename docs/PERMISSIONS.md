# Android Permission Architecture

NEXUS adheres to the principle of least privilege. Permissions are requested dynamically at runtime only when a user explicitly initiates an action requiring system access.

---

## 1. Declared Permissions & Functional Rationale

| Permission | Android Protection Level | Justification & Usage | Tool Component |
| :--- | :--- | :--- | :--- |
| `android.permission.INTERNET` | Normal | Optional network access for external API fallback (Firebase) or remote health checks. Not required for core local GGUF execution. | Cloud fallback adapter |
| `android.permission.CAMERA` | Dangerous | Required for taking diagnostic photos when instructed by the user. | `CameraCaptureTool` |
| `android.permission.READ_CONTACTS` | Dangerous | Queries contact entries upon user request. | `ContactQueryTool` |
| `android.permission.POST_NOTIFICATIONS` | Dangerous (API 33+) | Alerts user to completion of background tasks or required action confirmations. | Agent notification manager |
| `android.permission.READ_EXTERNAL_STORAGE` | Dangerous (API 24–32) | Sideloading GGUF model files from public storage directories on legacy Android versions. Zero-permission Photo Picker is preferred on modern devices. | `ModelImporter` |

---

## 2. Runtime Dynamic Permission Flow

NEXUS does **not** batch-request permissions upon app startup. Permissions are handled through modern Compose dynamic contracts:

1. **Pre-Flight Check**: When a tool is selected, `PermissionHelper.hasPermission(context, permission)` verifies current OS grant state.
2. **User Education**: If ungranted, the UI explains exactly why the permission is needed for the requested tool.
3. **OS Prompt**: The app triggers `ActivityResultContracts.RequestPermission()`.
4. **Grant or Graceful Degradation**:
   - If granted, tool proceeds immediately.
   - If denied, the tool aborts with a structured `ToolReceipt.denied()` error. The model is informed that the capability is unavailable, and suggests alternatives without crashing.

---

## 3. Sandboxed Storage vs. Broad Storage Permissions

- **Zero-Storage for Files**: All file reading, writing, and scratchpad logs operate within `context.filesDir/sandbox`. This requires **no Android storage permissions whatsoever**, isolating NEXUS files from other apps.
- **Model Import**: Models can be copied directly into the app's private files directory, eliminating the need for broad storage access.
