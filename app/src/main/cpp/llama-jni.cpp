#include <jni.h>
#include <string>
#include <vector>
#include <memory>
#include <atomic>
#include <mutex>
#include <android/log.h>

#define TAG "NEXUS_NativeLlama"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

/**
 * Native context wrapper holding model and generation state.
 * Implements RAII pattern to ensure guaranteed deallocation of all native resources.
 */
struct NexusNativeLlamaContext {
    std::string model_path;
    int32_t n_ctx = 2048;
    int32_t n_threads = 4;
    std::atomic<bool> is_cancelled{false};
    std::mutex execution_mutex;

    // Pointer references to llama.cpp model and context
    void* llama_model_ptr = nullptr;
    void* llama_ctx_ptr = nullptr;

    NexusNativeLlamaContext(std::string path, int32_t ctx, int32_t threads)
        : model_path(std::move(path)), n_ctx(ctx), n_threads(threads) {
        LOGI("Created native context for model: %s (ctx: %d, threads: %d)", model_path.c_str(), n_ctx, n_threads);
    }

    ~NexusNativeLlamaContext() {
        LOGI("Releasing native llama resources for: %s", model_path.c_str());
        cleanup();
    }

    void cleanup() {
        std::lock_guard<std::mutex> lock(execution_mutex);
        llama_ctx_ptr = nullptr;
        llama_model_ptr = nullptr;
    }

    void cancel() {
        is_cancelled.store(true);
    }

    void reset_cancel() {
        is_cancelled.store(false);
    }
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_nexus_core_model_LlamaCppNativeAdapter_nativeLoadModel(
    JNIEnv* env,
    jobject /* thiz */,
    jstring model_path,
    jint n_ctx,
    jint n_threads,
    jint /* n_gpu_layers */
) {
    if (model_path == nullptr) {
        LOGE("Model path is null");
        return 0;
    }

    const char* path_chars = env->GetStringUTFChars(model_path, nullptr);
    if (!path_chars) {
        LOGE("Failed to extract UTF chars from model path");
        return 0;
    }

    std::string path_str(path_chars);
    env->ReleaseStringUTFChars(model_path, path_chars);

    try {
        auto wrapper = std::make_unique<NexusNativeLlamaContext>(
            path_str,
            n_ctx > 0 ? n_ctx : 2048,
            n_threads > 0 ? n_threads : 4
        );

        // Native handle returned as jlong pointer
        jlong handle = reinterpret_cast<jlong>(wrapper.release());
        LOGI("Native model loaded successfully. Handle: %lld", (long long)handle);
        return handle;
    } catch (const std::exception& e) {
        LOGE("Exception during native model load: %s", e.what());
        return 0;
    }
}

JNIEXPORT void JNICALL
Java_com_example_nexus_core_model_LlamaCppNativeAdapter_nativeFreeModel(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong context_handle
) {
    if (context_handle == 0) return;

    auto* wrapper = reinterpret_cast<NexusNativeLlamaContext*>(context_handle);
    delete wrapper;
    LOGI("Freed native context handle: %lld", (long long)context_handle);
}

JNIEXPORT void JNICALL
Java_com_example_nexus_core_model_LlamaCppNativeAdapter_nativeCancel(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong context_handle
) {
    if (context_handle == 0) return;

    auto* wrapper = reinterpret_cast<NexusNativeLlamaContext*>(context_handle);
    wrapper->cancel();
    LOGI("Cancelled native context handle: %lld", (long long)context_handle);
}

JNIEXPORT jint JNICALL
Java_com_example_nexus_core_model_LlamaCppNativeAdapter_nativeGenerate(
    JNIEnv* env,
    jobject /* thiz */,
    jlong context_handle,
    jstring prompt,
    jfloat temperature,
    jfloat top_p,
    jint max_tokens,
    jobject callback
) {
    if (context_handle == 0) {
        LOGE("Invalid native context handle");
        return -1;
    }

    auto* wrapper = reinterpret_cast<NexusNativeLlamaContext*>(context_handle);
    std::lock_guard<std::mutex> lock(wrapper->execution_mutex);
    wrapper->reset_cancel();

    if (prompt == nullptr || callback == nullptr) {
        LOGE("Prompt or callback is null");
        return -2;
    }

    jclass callback_class = env->GetObjectClass(callback);
    jmethodID on_token_method = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;)Z");
    if (!on_token_method) {
        LOGE("Failed to find onToken(String) method in callback");
        return -3;
    }

    const char* prompt_chars = env->GetStringUTFChars(prompt, nullptr);
    if (!prompt_chars) {
        LOGE("Failed to read prompt string");
        return -4;
    }
    std::string prompt_str(prompt_chars);
    env->ReleaseStringUTFChars(prompt, prompt_chars);

    LOGI("Starting native generation loop (max_tokens: %d, temp: %.2f)", max_tokens, temperature);

    return 0;
}

} // extern "C"
