# Open Source Licenses & Third-Party Attribution

NEXUS is licensed under the **Apache License 2.0**.
This document outlines the third-party dependencies, native libraries, and components utilized by NEXUS, their corresponding versions, licenses, repositories, and usage within the project.

---

## Direct Dependencies

| Dependency | Version | License | Repository | Usage |
| :--- | :--- | :--- | :--- | :--- |
| **AndroidX Jetpack Compose BOM** | 2025.02.00 | Apache-2.0 | [androidx/androidx](https://github.com/androidx/androidx) | Modern declarative UI framework |
| **AndroidX Core KTX** | 1.15.0 | Apache-2.0 | [androidx/androidx](https://github.com/androidx/androidx) | Android core Kotlin extensions |
| **AndroidX Lifecycle (Runtime, Compose, ViewModel)** | 2.8.7 | Apache-2.0 | [androidx/androidx](https://github.com/androidx/androidx) | MVVM architecture & state flow management |
| **AndroidX Room (Runtime, KTX, Compiler)** | 2.7.0-alpha13 | Apache-2.0 | [androidx/androidx](https://github.com/androidx/androidx) | Local SQLite persistence for memory, tools, & audit logs |
| **AndroidX Navigation Compose** | 2.8.8 | Apache-2.0 | [androidx/androidx](https://github.com/androidx/androidx) | Safe in-app route navigation |
| **AndroidX Activity Compose** | 1.10.1 | Apache-2.0 | [androidx/androidx](https://github.com/androidx/androidx) | Compose Activity bindings & contracts |
| **KotlinX Coroutines (Core, Android)** | 1.10.1 | Apache-2.0 | [Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Asynchronous execution and token streaming flows |
| **Square OkHttp** | 4.12.0 | Apache-2.0 | [square/okhttp](https://github.com/square/okhttp) | HTTP client engine for optional network capabilities |
| **Square Retrofit** | 2.11.0 | Apache-2.0 | [square/retrofit](https://github.com/square/retrofit) | REST API client interface |
| **Square Moshi (Kotlin, Codegen, Converter)** | 1.15.2 | Apache-2.0 | [square/moshi](https://github.com/square/moshi) | JSON serialization and deserialization |
| **Google Firebase AI & BOM** | 33.10.0 | Apache-2.0 | [firebase/firebase-android-sdk](https://github.com/firebase/firebase-android-sdk) | Optional cloud fallback provider integration |
| **Robolectric** | 4.14.1 | Apache-2.0 | [robolectric/robolectric](https://github.com/robolectric/robolectric) | Local JVM unit test execution |
| **Roborazzi** | 1.40.1 | Apache-2.0 | [takahirom/roborazzi](https://github.com/takahirom/roborazzi) | Visual screenshot verification |
| **JUnit 4** | 4.13.2 | EPL-1.0 | [junit-team/junit4](https://github.com/junit-team/junit4) | Unit testing runner |

---

## Native Components (JNI / C++)

| Component | Target ABI | License | Repository | Usage |
| :--- | :--- | :--- | :--- | :--- |
| **llama.cpp / Native GGUF Bridge** | arm64-v8a | MIT License | [ggerganov/llama.cpp](https://github.com/ggerganov/llama.cpp) | Native JNI C++ bridge (`llama-jni.cpp`) for on-device GGUF inference |

### MIT License (llama.cpp)
```
MIT License

Copyright (c) 2023-2026 Georgi Gerganov and llama.cpp contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## License Compatibility Notice

All direct and transitive dependencies have been audited for compatibility:
- **Apache-2.0** and **MIT** licenses are fully compatible with the Apache-2.0 distribution of NEXUS.
- No copyleft licenses (such as GPLv3 or AGPL) are statically linked into the application binaries.
- EPL-1.0 is utilized exclusively within JVM test scopes (`testImplementation`).
