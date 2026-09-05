package com.example.nexus.core.policy

/**
 * Risk classification for autonomous agent actions.
 * Evaluated by the PolicyEngine to prevent unauthorized operations.
 */
enum class RiskLevel {
    LOW,      // E.g., open launchable app, read permitted file, get system time
    MEDIUM,   // E.g., create file, browser navigation, clipboard read/write
    HIGH,     // E.g., send messages, delete files, modify system settings
    CRITICAL  // E.g., financial transactions, account credentials, irreversible destructive actions
}

enum class PolicyDecision {
    ALLOW,    // Safe to execute immediately without interruption
    CONFIRM,  // Requires explicit UI user confirmation
    DENY      // Blocked outright by local security policy
}

interface PolicyEngine {
    fun evaluate(
        toolId: String,
        riskLevel: RiskLevel,
        isUserConfirmed: Boolean
    ): PolicyDecision
}

class StandardPolicyEngine : PolicyEngine {
    override fun evaluate(
        toolId: String,
        riskLevel: RiskLevel,
        isUserConfirmed: Boolean
    ): PolicyDecision {
        return when (riskLevel) {
            RiskLevel.LOW -> PolicyDecision.ALLOW
            RiskLevel.MEDIUM -> {
                if (isUserConfirmed) PolicyDecision.ALLOW else PolicyDecision.CONFIRM
            }
            RiskLevel.HIGH -> {
                if (isUserConfirmed) PolicyDecision.ALLOW else PolicyDecision.CONFIRM
            }
            RiskLevel.CRITICAL -> {
                // Critical actions ALWAYS require real-time explicit confirmation, never auto-allowed
                if (isUserConfirmed) PolicyDecision.ALLOW else PolicyDecision.DENY
            }
        }
    }
}
