# NEXUS Privacy Policy & Guarantees

NEXUS was engineered from inception on a single non-negotiable premise: **Your thoughts, prompts, files, and device data belong exclusively to you.**

---

## 1. Core Privacy Guarantees

- **100% Local Inference**: GGUF language models execute entirely on your device's processor using C++ JNI (`llama.cpp`). No prompt text, completion tokens, or context embeddings are ever transmitted to a cloud server during local mode.
- **Zero Telemetry**: NEXUS contains no analytics SDKs (no Firebase Analytics, no Mixpanel, no Google Analytics, no telemetry pings).
- **Zero Third-Party Trackers**: No third-party advertisement networks, behavioral tracking, or fingerprinting scripts are bundled or executed.
- **Isolated SQLite Storage**: All conversation history, long-term memories, and execution logs remain within the application's private SQLite sandbox (`/data/data/com.aistudio.nexus.aiagent/databases/`).

---

## 2. Sensor & Personal Data Handling

- **Camera**: Images captured via `CameraCaptureTool` are stored in app-private cache files and analyzed locally. They are never transmitted off-device.
- **Contacts**: Queried contacts are processed strictly within memory to fulfill the active command and are not harvested into external servers.
- **Files**: All generated notes, summaries, and outputs are saved strictly inside the app-internal sandbox directory.

---

## 3. Cloud Fallback (Optional)

If you explicitly configure an optional cloud provider (e.g., Firebase AI / Gemini API via user-provided API key):
- Cloud inference is invoked **only when explicitly selected** by the user in Settings.
- Requests are dispatched directly from your device to the provider endpoint without intermediary proxies.
- You can revoke API keys at any time through the app settings or environment configuration.
