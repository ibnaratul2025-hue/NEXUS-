package com.example.nexus.core.kernel

import com.example.nexus.core.cognitive.capability.LimitationRegistry
import com.example.nexus.core.cognitive.explain.DecisionExplanation
import com.example.nexus.core.cognitive.explain.ExplainabilityEngine
import com.example.nexus.core.cognitive.intent.IntentClassifier
import com.example.nexus.core.cognitive.intent.IntentResult
import com.example.nexus.core.cognitive.learning.LearningEngine
import com.example.nexus.core.cognitive.plan.ExecutionPlan
import com.example.nexus.core.cognitive.plan.PlanningEngine
import com.example.nexus.core.error.ErrorSeverity
import com.example.nexus.core.error.RetryPolicy
import com.example.nexus.core.error.ToolError
import com.example.nexus.core.model.ChatMessage
import com.example.nexus.core.model.ChatRole
import com.example.nexus.core.model.GenerationEvent
import com.example.nexus.core.model.InferenceController
import com.example.nexus.core.policy.PolicyDecision
import com.example.nexus.core.policy.PolicyEngine
import com.example.nexus.core.receipt.ConfirmationRequest
import com.example.nexus.core.receipt.ToolReceipt
import com.example.nexus.core.receipt.ToolStatus
import com.example.nexus.core.tool.AgentTool
import com.example.nexus.core.tool.ToolContext
import com.example.nexus.core.tool.ToolRegistry
import com.example.nexus.core.tool.ToolResult
import com.example.nexus.data.repository.AuditLogRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.UUID

sealed interface StepStatus {
    object Pending : StepStatus
    object InProgress : StepStatus
    object WaitingConfirmation : StepStatus
    data class Completed(val result: String) : StepStatus
    data class Failed(val error: String) : StepStatus
}

data class PlanStep(
    val id: String = UUID.randomUUID().toString(),
    val toolId: String,
    val description: String,
    val arguments: JSONObject = JSONObject(),
    val riskLevel: String = "LOW",
    val status: StepStatus = StepStatus.Pending
)

data class ActiveTaskState(
    val taskId: String = "",
    val goal: String = "",
    val agentState: AgentState = AgentState.IDLE,
    val steps: List<PlanStep> = emptyList(),
    val isExecuting: Boolean = false,
    val pendingConfirmationStep: PlanStep? = null,
    val pendingConfirmationRequest: ConfirmationRequest? = null,
    val currentStreamingText: String = "",
    val executionLog: List<String> = emptyList(),
    val finalAnswer: String? = null,
    val receipts: List<ToolReceipt> = emptyList(),
    val hallucinationReports: List<HallucinationReport> = emptyList(),
    val intentResult: IntentResult? = null,
    val executionPlan: ExecutionPlan? = null,
    val latestExplanation: DecisionExplanation? = null
)

class AgentKernel(
    private val toolRegistry: ToolRegistry,
    private val policyEngine: PolicyEngine,
    private val auditLogRepository: AuditLogRepository,
    private val contextBuilder: ContextBuilder,
    private val promptEngine: PromptEngine,
    private val inferenceController: InferenceController,
    val cancellationController: CancellationController = CancellationController(),
    val retryPolicy: RetryPolicy = RetryPolicy(maxRetries = 2),
    private val maxSteps: Int = 8,
    private val intentClassifier: IntentClassifier = IntentClassifier(),
    private val planningEngine: PlanningEngine? = null,
    private val limitationRegistry: LimitationRegistry = LimitationRegistry(),
    private val learningEngine: LearningEngine? = null,
    private val explainabilityEngine: ExplainabilityEngine = ExplainabilityEngine()
) {
    private val _taskState = MutableStateFlow(ActiveTaskState())
    val taskState: StateFlow<ActiveTaskState> = _taskState.asStateFlow()

    private var pendingConfirmationDeferred: CompletableDeferred<Boolean>? = null

    init {
        cancellationController.addListener { reason ->
            cancelTask(reason)
        }
    }

    /**
     * Executes the complete autonomous agent loop:
     * User Command -> Context Retrieval -> Local LLM -> Structured Response -> Tool Selection
     * -> Policy Check -> Permission/Confirmation -> Tool Execution -> Tool Result -> Local LLM -> Final Response.
     * Enforces strict Anti-Hallucination result boundary: "THE MODEL IS NEVER THE SOURCE OF TRUTH."
     */
    suspend fun executeAutonomousTask(command: String): String = withContext(Dispatchers.Default) {
        val taskId = UUID.randomUUID().toString()
        cancellationController.reset()

        // 1. Cognitive Intent Classification
        val intentResult = intentClassifier.classify(command)

        // 2. Limitation Boundary Check: Truth-first check before attempting impossible execution
        val limitation = limitationRegistry.findLimitationForRequest(command)
        if (limitation != null) {
            val limitationAnswer = "I cannot fulfill this request: ${limitation.summary}. Reason: ${limitation.detailedReason}. Alternative: ${limitation.recommendedAlternative}"
            updateState {
                copy(
                    taskId = taskId,
                    goal = command,
                    agentState = AgentState.COMPLETED,
                    isExecuting = false,
                    finalAnswer = limitationAnswer,
                    intentResult = intentResult,
                    executionLog = listOf("Limitation encountered: ${limitation.summary}")
                )
            }
            return@withContext limitationAnswer
        }

        // 3. Cognitive Planning
        val executionPlan = planningEngine?.generatePlan(intentResult)

        val taskReceipts = mutableListOf<ToolReceipt>()

        _taskState.value = ActiveTaskState(
            taskId = taskId,
            goal = command,
            agentState = AgentState.PLANNING,
            isExecuting = true,
            executionLog = listOf("Task initialized: '$command' (Intent: ${intentResult.type.name})"),
            intentResult = intentResult,
            executionPlan = executionPlan
        )

        val context = contextBuilder.build(command, AgentState.PLANNING)
        val conversation = promptEngine.buildInitialMessages(context).toMutableList()

        var finalResponse = ""
        var stepCount = 0

        while (stepCount < maxSteps) {
            if (cancellationController.isCancelled.value) {
                return@withContext "Execution cancelled."
            }

            stepCount++
            updateState {
                copy(
                    agentState = AgentState.THINKING,
                    executionLog = executionLog + "Step $stepCount: Reasoning with local model..."
                )
            }

            var generatedText = ""
            var generationFailed = false
            var wasCancelled = false

            // Stream tokens from inference controller
            inferenceController.generate(conversation).collect { event ->
                when (event) {
                    is GenerationEvent.Started -> {
                        updateState { copy(currentStreamingText = "Thinking...") }
                    }
                    is GenerationEvent.Token -> {
                        updateState { copy(currentStreamingText = event.accumulatedText) }
                    }
                    is GenerationEvent.Completed -> {
                        generatedText = event.fullText
                        updateState { copy(currentStreamingText = event.fullText) }
                    }
                    is GenerationEvent.Cancelled -> {
                        wasCancelled = true
                        updateState {
                            copy(
                                agentState = AgentState.CANCELLED,
                                isExecuting = false,
                                executionLog = executionLog + "Inference cancelled by user."
                            )
                        }
                    }
                    is GenerationEvent.Error -> {
                        generationFailed = true
                        updateState {
                            copy(
                                agentState = AgentState.FAILED,
                                isExecuting = false,
                                executionLog = executionLog + "Model generation error: ${event.throwable.localizedMessage}"
                            )
                        }
                    }
                }
            }

            if (wasCancelled || cancellationController.isCancelled.value) {
                return@withContext "Execution cancelled."
            }

            if (generationFailed || generatedText.isBlank()) {
                return@withContext "Inference failed to produce a valid response."
            }

            // Parse response
            val parsedResponse = try {
                AgentResponseParser.parse(generatedText, toolRegistry)
            } catch (e: Exception) {
                // If model output could not be parsed into strict schema, treat as final text
                AgentResponse.FinalMessage(generatedText)
            }

            when (parsedResponse) {
                is AgentResponse.FinalMessage -> {
                    // ANTI-HALLUCINATION BOUNDARY:
                    // Validate model claims against authoritative tool receipts
                    val validationOutcome = AntiHallucinationValidator.validateResponse(
                        userCommand = command,
                        modelText = parsedResponse.message,
                        receipts = taskReceipts
                    )

                    finalResponse = validationOutcome.verifiedResponse

                    val reports = if (validationOutcome.hallucinationReport != null) {
                        val rep = validationOutcome.hallucinationReport
                        auditLogRepository.log(
                            command = command,
                            toolId = "anti_hallucination",
                            riskLevel = "HIGH",
                            permissionChecked = "ValidationBoundary",
                            userConfirmation = "AUTO_CORRECTED",
                            executionStatus = "HALLUCINATION_DETECTED",
                            resultSummary = "Claim: '${rep.claim.take(50)}' replaced with verified statement: '${rep.correctedResponse.take(50)}'"
                        )
                        _taskState.value.hallucinationReports + rep
                    } else {
                        _taskState.value.hallucinationReports
                    }

                    val explanation = explainabilityEngine.buildExplanation(
                        userCommand = command,
                        intentResult = intentResult,
                        planSteps = executionPlan?.steps,
                        receipts = taskReceipts,
                        finalOutcome = finalResponse
                    )

                    updateState {
                        copy(
                            agentState = AgentState.COMPLETED,
                            isExecuting = false,
                            finalAnswer = finalResponse,
                            receipts = taskReceipts.toList(),
                            hallucinationReports = reports,
                            latestExplanation = explanation,
                            executionLog = executionLog + (
                                if (validationOutcome.hallucinationReport != null) {
                                    "[SECURITY] Hallucination detected & corrected: ${validationOutcome.hallucinationReport.mismatchCategory}"
                                } else {
                                    "Task completed successfully."
                                }
                            )
                        )
                    }

                    auditLogRepository.log(
                        command = command,
                        toolId = "agent_kernel",
                        riskLevel = "LOW",
                        permissionChecked = "None",
                        userConfirmation = "N/A",
                        executionStatus = "COMPLETED",
                        resultSummary = finalResponse.take(120)
                    )
                    return@withContext finalResponse
                }

                is AgentResponse.ToolCall -> {
                    val tool = toolRegistry.getTool(parsedResponse.toolId)
                    if (tool == null) {
                        conversation.add(
                            ChatMessage(
                                role = ChatRole.TOOL,
                                content = "<tool_result tool=\"${parsedResponse.toolId}\" status=\"FAILED\"><error_code>TOOL_NOT_FOUND</error_code><error_message>Tool '${parsedResponse.toolId}' does not exist.</error_message></tool_result>",
                                toolCallId = parsedResponse.toolId
                            )
                        )
                        continue
                    }

                    val planStep = PlanStep(
                        toolId = tool.id,
                        description = tool.description,
                        arguments = parsedResponse.arguments,
                        riskLevel = tool.riskLevel.name,
                        status = StepStatus.InProgress
                    )

                    updateState {
                        copy(
                            steps = steps + planStep,
                            executionLog = executionLog + "Planned tool: ${tool.id} (Risk: ${tool.riskLevel.name})"
                        )
                    }

                    // Policy evaluation
                    val decision = policyEngine.evaluate(tool.id, tool.riskLevel, isUserConfirmed = false)
                    when (decision) {
                        PolicyDecision.DENY -> {
                            val failReceipt = ToolReceipt(
                                toolId = tool.id,
                                status = ToolStatus.FAILED,
                                error = ToolError.policyDenied(tool.id, "Blocked by security policy"),
                                riskLevel = tool.riskLevel,
                                outputSummary = "Blocked by security policy"
                            )
                            taskReceipts.add(failReceipt)

                            auditLogRepository.log(
                                command = command,
                                toolId = tool.id,
                                riskLevel = tool.riskLevel.name,
                                permissionChecked = tool.requiredPermissions.joinToString().ifEmpty { "None" },
                                userConfirmation = "POLICY_DENIED",
                                executionStatus = "DENIED",
                                resultSummary = "Action denied by security policy"
                            )
                            conversation.add(
                                ChatMessage(
                                    role = ChatRole.TOOL,
                                    content = failReceipt.toToolResultInjection(),
                                    toolCallId = tool.id
                                )
                            )
                        }

                        PolicyDecision.CONFIRM -> {
                            val confRequest = ConfirmationRequest(
                                toolId = tool.id,
                                arguments = parsedResponse.arguments,
                                explanation = tool.description,
                                riskLevel = tool.riskLevel
                            )

                            updateState {
                                copy(
                                    agentState = AgentState.WAITING_FOR_PERMISSION,
                                    pendingConfirmationStep = planStep,
                                    pendingConfirmationRequest = confRequest,
                                    executionLog = executionLog + "Waiting for user confirmation for ${tool.name} (Hash: ${confRequest.actionHash})..."
                                )
                            }

                            // Await user confirmation
                            val deferred = CompletableDeferred<Boolean>()
                            pendingConfirmationDeferred = deferred
                            val confirmed = deferred.await()
                            pendingConfirmationDeferred = null

                            if (!confirmed) {
                                val cancelReceipt = ToolReceipt(
                                    toolId = tool.id,
                                    status = ToolStatus.CANCELLED,
                                    error = ToolError.confirmationRejected(tool.id),
                                    riskLevel = tool.riskLevel,
                                    outputSummary = "User declined confirmation"
                                )
                                taskReceipts.add(cancelReceipt)

                                updateState {
                                    copy(
                                        agentState = AgentState.CANCELLED,
                                        isExecuting = false,
                                        pendingConfirmationStep = null,
                                        pendingConfirmationRequest = null,
                                        receipts = taskReceipts.toList(),
                                        executionLog = executionLog + "User declined action ${tool.id}."
                                    )
                                }
                                auditLogRepository.log(
                                    command = command,
                                    toolId = tool.id,
                                    riskLevel = tool.riskLevel.name,
                                    permissionChecked = "UserPrompt",
                                    userConfirmation = "DECLINED",
                                    executionStatus = "CANCELLED",
                                    resultSummary = "User declined confirmation"
                                )
                                return@withContext "Action cancelled by user."
                            }

                            // User approved - execute tool with receipt & retry handling
                            val receipt = executeToolWithReceipt(
                                tool = tool,
                                arguments = parsedResponse.arguments,
                                userConfirmed = true,
                                taskId = taskId,
                                command = command
                            )
                            taskReceipts.add(receipt)
                            learningEngine?.processReceipt(receipt)
                            conversation.add(
                                ChatMessage(
                                    role = ChatRole.TOOL,
                                    content = receipt.toToolResultInjection(),
                                    toolCallId = tool.id
                                )
                            )
                        }

                        PolicyDecision.ALLOW -> {
                            val receipt = executeToolWithReceipt(
                                tool = tool,
                                arguments = parsedResponse.arguments,
                                userConfirmed = false,
                                taskId = taskId,
                                command = command
                            )
                            taskReceipts.add(receipt)
                            learningEngine?.processReceipt(receipt)
                            conversation.add(
                                ChatMessage(
                                    role = ChatRole.TOOL,
                                    content = receipt.toToolResultInjection(),
                                    toolCallId = tool.id
                                )
                            )
                        }
                    }
                }
            }
        }

        // Exceeded max steps
        val limitMsg = "Agent stopped: maximum reasoning/action steps reached ($maxSteps steps)."
        updateState {
            copy(
                agentState = AgentState.FAILED,
                isExecuting = false,
                finalAnswer = limitMsg,
                receipts = taskReceipts.toList(),
                executionLog = executionLog + limitMsg
            )
        }
        return@withContext limitMsg
    }

    private suspend fun executeToolWithReceipt(
        tool: AgentTool,
        arguments: JSONObject,
        userConfirmed: Boolean,
        taskId: String,
        command: String
    ): ToolReceipt {
        updateState {
            copy(
                agentState = AgentState.EXECUTING_TOOL,
                pendingConfirmationStep = null,
                pendingConfirmationRequest = null,
                executionLog = executionLog + "Executing tool: ${tool.id}..."
            )
        }

        var attempt = 0
        var finalReceipt: ToolReceipt? = null

        while (attempt <= retryPolicy.maxRetries) {
            attempt++
            val start = System.currentTimeMillis()

            // Enforce 30-second tool execution timeout
            val rawResult: ToolResult? = withTimeoutOrNull(30_000L) {
                try {
                    tool.execute(arguments, ToolContext(taskId = taskId, userConfirmed = userConfirmed))
                } catch (e: Throwable) {
                    ToolResult(
                        success = false,
                        output = "",
                        error = e.localizedMessage ?: "Unexpected tool exception",
                        executionTimeMs = System.currentTimeMillis() - start
                    )
                }
            }

            val result = rawResult ?: ToolResult(
                success = false,
                output = "",
                error = "TOOL_TIMEOUT: Operation exceeded 30s timeout",
                executionTimeMs = 30_000L
            )

            if (result.success) {
                finalReceipt = ToolReceipt(
                    toolId = tool.id,
                    status = ToolStatus.SUCCESS,
                    timestamp = System.currentTimeMillis(),
                    riskLevel = tool.riskLevel,
                    outputSummary = result.output
                )
                break
            } else {
                val err = classifyToolError(tool.id, result.error ?: "Unknown failure")
                finalReceipt = ToolReceipt(
                    toolId = tool.id,
                    status = ToolStatus.FAILED,
                    timestamp = System.currentTimeMillis(),
                    error = err,
                    riskLevel = tool.riskLevel,
                    outputSummary = result.output
                )

                if (!retryPolicy.isRetryable(err, attempt)) {
                    break
                }

                updateState {
                    copy(executionLog = executionLog + "Tool ${tool.id} failed (${err.code}). Retrying attempt $attempt...")
                }
            }
        }

        val receipt = finalReceipt ?: ToolReceipt(
            toolId = tool.id,
            status = ToolStatus.FAILED,
            error = ToolError.toolUnavailable(tool.id, "No result returned"),
            riskLevel = tool.riskLevel
        )

        updateState {
            copy(
                agentState = AgentState.WAITING_FOR_RESULT,
                executionLog = executionLog + "Receipt [${receipt.toolId}]: ${receipt.status} (attempt: $attempt)"
            )
        }

        auditLogRepository.log(
            command = command,
            toolId = tool.id,
            riskLevel = tool.riskLevel.name,
            permissionChecked = tool.requiredPermissions.joinToString().ifEmpty { "None" },
            userConfirmation = if (userConfirmed) "CONFIRMED" else "AUTO_ALLOWED",
            executionStatus = receipt.status.name,
            resultSummary = if (receipt.status == ToolStatus.SUCCESS) receipt.outputSummary.take(120) else (receipt.error?.userMessage ?: "Failed")
        )

        return receipt
    }

    private fun classifyToolError(toolId: String, errorText: String): ToolError {
        return when {
            errorText.contains("TOOL_TIMEOUT", ignoreCase = true) ->
                ToolError.toolTimeout(toolId, 30_000L)
            errorText.contains("APP_NOT_FOUND", ignoreCase = true) ->
                ToolError.appNotFound(errorText)
            errorText.contains("FILE_NOT_FOUND", ignoreCase = true) ->
                ToolError.fileNotFound(errorText)
            errorText.contains("ACCESS_DENIED", ignoreCase = true) || errorText.contains("PATH_TRAVERSAL", ignoreCase = true) ->
                ToolError.filePathInvalid(errorText)
            errorText.contains("PERMISSION_DENIED", ignoreCase = true) ->
                ToolError.permissionDenied(errorText)
            errorText.contains("BROWSER_FAILED", ignoreCase = true) ->
                ToolError.browserFailed("", errorText)
            else -> ToolError(
                code = "TOOL_ERROR",
                userMessage = errorText,
                technicalMessage = errorText,
                retryable = false,
                severity = ErrorSeverity.MEDIUM
            )
        }
    }

    fun confirmPendingStep() {
        pendingConfirmationDeferred?.complete(true)
    }

    fun rejectPendingStep() {
        pendingConfirmationDeferred?.complete(false)
    }

    fun cancelTask(reason: String = "User requested cancellation") {
        inferenceController.cancel()
        pendingConfirmationDeferred?.complete(false)
        updateState {
            copy(
                agentState = AgentState.CANCELLED,
                isExecuting = false,
                pendingConfirmationStep = null,
                pendingConfirmationRequest = null,
                executionLog = executionLog + "Task cancelled: $reason"
            )
        }
    }

    suspend fun executeStructuredToolCall(
        toolId: String,
        arguments: JSONObject,
        userConfirmed: Boolean = false,
        commandText: String = "Direct tool dispatch"
    ): ToolResult {
        val tool = toolRegistry.getTool(toolId)
            ?: return ToolResult(
                success = false,
                output = "",
                error = "TOOL_NOT_AVAILABLE: Tool '$toolId' is not registered in NEXUS registry"
            )

        val decision = policyEngine.evaluate(tool.id, tool.riskLevel, isUserConfirmed = userConfirmed)
        return when (decision) {
            PolicyDecision.DENY -> {
                auditLogRepository.log(
                    command = commandText,
                    toolId = toolId,
                    riskLevel = tool.riskLevel.name,
                    permissionChecked = tool.requiredPermissions.joinToString().ifEmpty { "None" },
                    userConfirmation = "BLOCKED_BY_POLICY",
                    executionStatus = "DENIED",
                    resultSummary = "Blocked by security policy"
                )
                ToolResult(success = false, output = "", error = "ACTION_DENIED: Blocked by Policy Engine")
            }
            PolicyDecision.CONFIRM -> {
                ToolResult(success = false, output = "", error = "ACTION_REQUIRES_CONFIRMATION")
            }
            PolicyDecision.ALLOW -> {
                val start = System.currentTimeMillis()
                val result = try {
                    tool.execute(arguments, ToolContext(taskId = UUID.randomUUID().toString(), userConfirmed = userConfirmed))
                } catch (e: Throwable) {
                    ToolResult(
                        success = false,
                        output = "",
                        error = e.localizedMessage ?: "Tool failure",
                        executionTimeMs = System.currentTimeMillis() - start
                    )
                }

                auditLogRepository.log(
                    command = commandText,
                    toolId = toolId,
                    riskLevel = tool.riskLevel.name,
                    permissionChecked = tool.requiredPermissions.joinToString().ifEmpty { "None" },
                    userConfirmation = if (userConfirmed) "CONFIRMED" else "AUTO_ALLOWED",
                    executionStatus = if (result.success) "SUCCESS" else "FAILED",
                    resultSummary = if (result.success) result.output.take(120) else (result.error ?: "Failed")
                )
                result
            }
        }
    }

    private inline fun updateState(transform: ActiveTaskState.() -> ActiveTaskState) {
        _taskState.value = _taskState.value.transform()
    }
}
