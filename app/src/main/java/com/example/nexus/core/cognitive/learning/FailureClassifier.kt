package com.example.nexus.core.cognitive.learning

import com.example.nexus.core.receipt.ToolReceipt
import com.example.nexus.core.receipt.ToolStatus

class FailureClassifier {

    fun classify(receipt: ToolReceipt): FailureType {
        val error = receipt.error
        if (error != null) {
            val code = error.code.uppercase()
            val msg = (error.userMessage + " " + error.technicalMessage).lowercase()

            return when {
                code.contains("PERMISSION") || msg.contains("permission") || msg.contains("denied") ->
                    FailureType.PERMISSION

                code.contains("SANDBOX") || code.contains("POLICY") || msg.contains("policy") || msg.contains("blocked") ->
                    FailureType.LIMITATION

                code.contains("NOT_FOUND") || msg.contains("does not exist") || msg.contains("not found") ->
                    FailureType.ENVIRONMENT

                code.contains("SCHEMA") || code.contains("INVALID_ARGUMENT") || msg.contains("argument") ->
                    FailureType.TOOL

                code.contains("CANCELLED") ->
                    FailureType.AMBIGUITY

                else -> FailureType.TOOL
            }
        }

        return when (receipt.status) {
            ToolStatus.SUCCESS -> FailureType.TOOL
            ToolStatus.FAILED -> FailureType.TOOL
            ToolStatus.PERMISSION_REQUIRED -> FailureType.PERMISSION
            ToolStatus.CONFIRMATION_REQUIRED -> FailureType.PLANNING
            ToolStatus.CANCELLED -> FailureType.AMBIGUITY
        }
    }

    fun classifyException(throwable: Throwable, contextSummary: String = ""): FailureType {
        val msg = (throwable.message ?: "").lowercase()
        return when {
            throwable is SecurityException || msg.contains("permission") -> FailureType.PERMISSION
            msg.contains("no such file") || msg.contains("space") || msg.contains("storage") -> FailureType.ENVIRONMENT
            msg.contains("syntax") || msg.contains("json") || msg.contains("parse") -> FailureType.MODEL
            msg.contains("plan") || msg.contains("dependency") -> FailureType.PLANNING
            msg.contains("limit") || msg.contains("unsupported") -> FailureType.LIMITATION
            else -> FailureType.TOOL
        }
    }
}
