package com.example.nexus.core.receipt

import com.example.nexus.core.error.ToolError
import com.example.nexus.core.policy.RiskLevel
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

enum class ToolStatus {
    SUCCESS,
    FAILED,
    PERMISSION_REQUIRED,
    CONFIRMATION_REQUIRED,
    CANCELLED
}

data class ConfirmationRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val toolId: String,
    val arguments: JSONObject,
    val explanation: String,
    val riskLevel: RiskLevel,
    val actionHash: String = computeActionHash(toolId, arguments)
) {
    companion object {
        fun computeActionHash(toolId: String, arguments: JSONObject): String {
            val raw = "$toolId:${arguments.toString()}"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
            return digest.fold("") { str, it -> str + "%02x".format(it) }.take(16)
        }
    }
}

/**
 * An immutable cryptographic/structured receipt of an executed or attempted tool action.
 * THE MODEL IS NEVER THE SOURCE OF TRUTH — only receipts verify real outcomes.
 */
data class ToolReceipt(
    val executionId: String = UUID.randomUUID().toString(),
    val toolId: String,
    val status: ToolStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val data: JSONObject? = null,
    val error: ToolError? = null,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val outputSummary: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("executionId", executionId)
            put("tool", toolId)
            put("status", status.name)
            put("timestamp", timestamp)
            put("riskLevel", riskLevel.name)
            if (data != null) put("data", data)
            if (error != null) {
                put("error", JSONObject().apply {
                    put("code", error.code)
                    put("userMessage", error.userMessage)
                    put("technicalMessage", error.technicalMessage)
                    put("retryable", error.retryable)
                })
            }
            put("outputSummary", outputSummary)
        }
    }

    /**
     * Formats receipt as safe, untrusted factual data injection for the LLM.
     */
    fun toToolResultInjection(): String {
        val sb = StringBuilder()
        sb.appendLine("<tool_result tool=\"$toolId\" status=\"${status.name}\" execution_id=\"$executionId\">")
        if (status == ToolStatus.SUCCESS) {
            sb.appendLine("status=SUCCESS")
            if (outputSummary.isNotBlank()) sb.appendLine("output=$outputSummary")
            if (data != null) sb.appendLine("data=${data.toString()}")
        } else {
            sb.appendLine("status=${status.name}")
            if (error != null) {
                sb.appendLine("error_code=${error.code}")
                sb.appendLine("error_message=${error.userMessage}")
            }
        }
        sb.appendLine("</tool_result>")
        return sb.toString().trimEnd()
    }
}

sealed interface ToolExecutionResult {
    val receipt: ToolReceipt

    data class Success(
        val data: JSONObject?,
        val message: String,
        override val receipt: ToolReceipt
    ) : ToolExecutionResult

    data class Failed(
        val error: ToolError,
        override val receipt: ToolReceipt
    ) : ToolExecutionResult

    data class PermissionRequired(
        val permission: String,
        val explanation: String,
        override val receipt: ToolReceipt
    ) : ToolExecutionResult

    data class ConfirmationRequired(
        val request: ConfirmationRequest,
        override val receipt: ToolReceipt
    ) : ToolExecutionResult

    data class Cancelled(
        override val receipt: ToolReceipt
    ) : ToolExecutionResult
}
