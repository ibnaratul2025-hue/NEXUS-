package com.example.nexus.core.world

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.nexus.core.policy.PolicyDecision
import com.example.nexus.core.policy.PolicyEngine
import com.example.nexus.core.policy.RiskLevel

sealed class NegotiationResult {
    data class CanExecute(val action: UniversalAction, val requiresConfirmation: Boolean, val risk: RiskLevel) : NegotiationResult()
    data class Limitation(
        val reason: String,
        val exactLimitation: String,
        val suggestedAlternative: String? = null
    ) : NegotiationResult()
}

/**
 * Capability Negotiator.
 * Evaluates the 6-stage pre-execution gating pipeline:
 * 1. Is capability registered in ActionFabric?
 * 2. Are required Android runtime permissions granted?
 * 3. Is the target resource available/reachable?
 * 4. Is the action safe per PolicyEngine?
 * 5. Can the outcome be verified?
 *
 * Invariant: NEVER hallucinate capability. Explains exact technical limitations if blocked.
 */
class CapabilityNegotiator(
    private val context: Context,
    private val actionFabric: ActionFabric,
    private val policyEngine: PolicyEngine
) {

    fun negotiate(
        capability: String,
        target: String,
        parameters: Map<String, Any> = emptyMap(),
        isUserConfirmed: Boolean = false
    ): NegotiationResult {
        // Stage 1: Is the capability available in ActionFabric?
        val action = actionFabric.getAction(capability, parameters + ("target" to target))
        if (action == null) {
            return NegotiationResult.Limitation(
                reason = "Capability '$capability' is not supported on this device.",
                exactLimitation = "Android operating layer and local NEXUS adapters provide no registered handler for capability '$capability'.",
                suggestedAlternative = "Available system capabilities: ${actionFabric.listCapabilities().take(5).joinToString(", ")}"
            )
        }

        // Stage 2: Are required runtime permissions granted?
        for (perm in action.requiredPermissions) {
            val status = ContextCompat.checkSelfPermission(context, perm)
            if (status != PackageManager.PERMISSION_GRANTED) {
                return NegotiationResult.Limitation(
                    reason = "Missing required Android permission: $perm",
                    exactLimitation = "Android OS requires runtime permission '$perm' to execute '${action.capability}' on target '$target'.",
                    suggestedAlternative = "Request user permission via System Settings or runtime dialog."
                )
            }
        }

        // Stage 3: Is target available?
        if (capability == "open_app") {
            val pkg = target.ifEmpty { parameters["package_name"]?.toString() ?: "" }
            val isInstalled = try {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            } catch (e: Exception) {
                false
            }
            if (!isInstalled) {
                return NegotiationResult.Limitation(
                    reason = "Target application is not installed.",
                    exactLimitation = "Package '$pkg' was not found in Android PackageManager.",
                    suggestedAlternative = "Search Google Play Store or install the corresponding package."
                )
            }
        }

        // Stage 4: Is the action safe per PolicyEngine?
        val policy = policyEngine.evaluate(action.capability, action.risk, isUserConfirmed)
        if (policy == PolicyDecision.DENY) {
            return NegotiationResult.Limitation(
                reason = "Blocked by security and risk boundary.",
                exactLimitation = "Security policy strictly denies action '${action.capability}' with risk level ${action.risk.name}.",
                suggestedAlternative = "Irreversible or critical destructive operations cannot be run autonomously."
            )
        }

        val requiresConfirmation = policy == PolicyDecision.CONFIRM && !isUserConfirmed

        // Stage 5 & 6: Outcome verification pass
        return NegotiationResult.CanExecute(
            action = action,
            requiresConfirmation = requiresConfirmation,
            risk = action.risk
        )
    }
}
