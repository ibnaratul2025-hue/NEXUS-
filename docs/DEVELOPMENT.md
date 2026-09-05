# NEXUS Developer Guide

This guide describes the core development workflows for engineers and researchers extending the NEXUS codebase.

---

## 1. Code Architecture & Conventions

NEXUS uses a strict separation between UI, cognitive planning, tool execution, and local persistence:

- **`core/kernel`**: Agent lifecycle, `ContextBuilder`, `ErrorRecoveryEngine`, and `AntiHallucinationValidator`.
- **`core/model`**: Native inference contracts (`LocalModelEngine`), JNI bridges (`LlamaCppNativeAdapter`), and token streaming flows.
- **`core/policy`**: `PolicyEngine` enforcing risk classifications and user confirmations.
- **`core/tool`**: Concrete Android tool implementations.
- **`data/`**: Room SQLite database, entities, and DAOs.
- **`ui/`**: Jetpack Compose screens, ViewModels, and Material 3 design elements.

---

## 2. Adding a New Tool

To add a new tool to NEXUS:

1. Create a class implementing `AgentTool` in `com.example.nexus.core.tool.tools`:
   ```kotlin
   class FlashlightTool(
       private val context: Context
   ) : AgentTool {
       override val id: String = "flashlight_toggle"
       override val name: String = "Toggle Flashlight"
       override val description: String = "Turns the device camera flashlight on or off."
       override val riskLevel: RiskLevel = RiskLevel.LOW
       override val requiredPermissions: List<String> = listOf(Manifest.permission.CAMERA)
       override val argumentSchema: ToolSchema = ToolSchema(
           properties = mapOf("enabled" to SchemaProperty(type = "boolean", description = "True to turn on, false to turn off")),
           required = listOf("enabled")
       )

       override suspend fun execute(args: Map<String, Any?>): ToolReceipt {
           val enabled = args["enabled"] as? Boolean ?: false
           val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
           val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return ToolReceipt.failure(id, "No camera found")

           return try {
               cameraManager.setTorchMode(cameraId, enabled)
               ToolReceipt.success(id, "Flashlight set to $enabled")
           } catch (e: Exception) {
               ToolReceipt.failure(id, "Failed to toggle flashlight: ${e.message}")
           }
       }
   }
   ```
2. Register your tool in `ToolRegistry.kt`:
   ```kotlin
   registerTool(FlashlightTool(context))
   ```
3. Add a unit test in `app/src/test/java/com/example/nexus/core/tool/FlashlightToolTest.kt`.

---

## 3. Writing Tests

NEXUS utilizes **Robolectric** for fast, local JVM testing without requiring physical emulators.

### Running Tests
```bash
./gradlew test
```

### Writing a Kernel Test
```kotlin
@RunWith(RobolectricTestRunner::class)
class MyToolTest {
    @Test
    fun execute_whenPermissionGranted_returnsSuccessReceipt() = runTest {
        val tool = MyTool(ApplicationProvider.getApplicationContext())
        val receipt = tool.execute(mapOf("key" to "value"))
        assertTrue(receipt.isSuccess)
    }
}
```

---

## 4. Testing GGUF Models & JNI Locally

When testing model parsing and quantization detection:
- Use `GgufMetadataParserTest` to verify header validation without needing multi-gigabyte files.
- To test live generation on a connected physical device:
  ```bash
  ./gradlew installDebug -PincludeNative=true
  adb logcat -s NEXUS_LlamaCpp NEXUS_NativeLlama
  ```
