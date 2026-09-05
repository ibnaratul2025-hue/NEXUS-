package com.example.nexus.core.error

enum class ErrorSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class ToolError(
    val code: String,
    val userMessage: String,
    val technicalMessage: String,
    val retryable: Boolean = false,
    val severity: ErrorSeverity = ErrorSeverity.MEDIUM
) {
    companion object {
        // App Errors
        fun appNotFound(query: String) = ToolError(
            code = "APP_NOT_FOUND",
            userMessage = "No installed application matched '$query'.",
            technicalMessage = "PackageManager could not resolve launchable activity for query: $query",
            retryable = false,
            severity = ErrorSeverity.LOW
        )

        fun appNotLaunchable(pkg: String) = ToolError(
            code = "APP_NOT_LAUNCHABLE",
            userMessage = "Application '$pkg' cannot be launched directly.",
            technicalMessage = "No launch intent found for package: $pkg",
            retryable = false,
            severity = ErrorSeverity.MEDIUM
        )

        // File Errors
        fun fileNotFound(path: String) = ToolError(
            code = "FILE_NOT_FOUND",
            userMessage = "File '$path' does not exist in the sandbox.",
            technicalMessage = "Resolved file does not exist on disk: $path",
            retryable = false,
            severity = ErrorSeverity.LOW
        )

        fun fileAccessDenied(path: String, reason: String) = ToolError(
            code = "FILE_ACCESS_DENIED",
            userMessage = "Access denied for file '$path'.",
            technicalMessage = "Security restriction: $reason",
            retryable = false,
            severity = ErrorSeverity.HIGH
        )

        fun filePathInvalid(path: String) = ToolError(
            code = "FILE_PATH_INVALID",
            userMessage = "Path traversal detected or invalid filename: '$path'.",
            technicalMessage = "Canonical path verification failed for relative path: $path",
            retryable = false,
            severity = ErrorSeverity.HIGH
        )

        // Tool execution errors
        fun toolTimeout(toolId: String, timeoutMs: Long) = ToolError(
            code = "TOOL_TIMEOUT",
            userMessage = "Tool '$toolId' timed out after ${timeoutMs / 1000}s.",
            technicalMessage = "Execution cancelled due to exceeding timeout limit of ${timeoutMs}ms",
            retryable = true,
            severity = ErrorSeverity.MEDIUM
        )

        fun toolUnavailable(toolId: String, reason: String) = ToolError(
            code = "TOOL_UNAVAILABLE",
            userMessage = "Tool '$toolId' is currently unavailable: $reason",
            technicalMessage = "Capability requirement not met: $reason",
            retryable = false,
            severity = ErrorSeverity.MEDIUM
        )

        fun toolInvalidArgument(toolId: String, detail: String) = ToolError(
            code = "TOOL_INVALID_ARGUMENT",
            userMessage = "Invalid parameter supplied for '$toolId': $detail",
            technicalMessage = "Schema validation error: $detail",
            retryable = false,
            severity = ErrorSeverity.LOW
        )

        // Permission & Policy Errors
        fun permissionDenied(permission: String) = ToolError(
            code = "PERMISSION_DENIED",
            userMessage = "Permission '$permission' is required but was denied.",
            technicalMessage = "Android runtime permission not granted: $permission",
            retryable = false,
            severity = ErrorSeverity.HIGH
        )

        fun policyDenied(toolId: String, reason: String) = ToolError(
            code = "POLICY_DENIED",
            userMessage = "Action '$toolId' was blocked by NEXUS security policy.",
            technicalMessage = "PolicyEngine DENY rule triggered: $reason",
            retryable = false,
            severity = ErrorSeverity.HIGH
        )

        fun confirmationRejected(toolId: String) = ToolError(
            code = "CONFIRMATION_REJECTED",
            userMessage = "Operation '$toolId' was declined by the user.",
            technicalMessage = "User clicked Decline on confirmation dialog",
            retryable = false,
            severity = ErrorSeverity.LOW
        )

        fun confirmationExpired(toolId: String) = ToolError(
            code = "CONFIRMATION_EXPIRED",
            userMessage = "Confirmation for '$toolId' timed out.",
            technicalMessage = "Confirmation was not received within time limit",
            retryable = false,
            severity = ErrorSeverity.LOW
        )

        // Native & Model Errors
        fun nativeGenerationFailed(detail: String) = ToolError(
            code = "NATIVE_GENERATION_FAILED",
            userMessage = "Local neural model encountered an inference error.",
            technicalMessage = "llama.cpp native failure: $detail",
            retryable = true,
            severity = ErrorSeverity.HIGH
        )

        fun nativeOutOfMemory() = ToolError(
            code = "NATIVE_OUT_OF_MEMORY",
            userMessage = "Device memory insufficient for this model size.",
            technicalMessage = "Native memory allocation failure during llama context initialization",
            retryable = false,
            severity = ErrorSeverity.CRITICAL
        )

        // Browser Errors
        fun browserFailed(url: String, detail: String) = ToolError(
            code = "BROWSER_FAILED",
            userMessage = "Failed to open web browser for URL: $url",
            technicalMessage = "Intent resolution or network error: $detail",
            retryable = false,
            severity = ErrorSeverity.LOW
        )

        // General
        fun userCancelled() = ToolError(
            code = "USER_CANCELLED",
            userMessage = "Task was cancelled by user.",
            technicalMessage = "CancellationController triggered",
            retryable = false,
            severity = ErrorSeverity.LOW
        )
    }
}

/**
 * Robust retry policy classifying retryable vs non-retryable errors.
 */
class RetryPolicy(val maxRetries: Int = 2) {
    fun isRetryable(error: ToolError, currentAttempt: Int): Boolean {
        if (currentAttempt >= maxRetries) return false
        return error.retryable
    }
}
