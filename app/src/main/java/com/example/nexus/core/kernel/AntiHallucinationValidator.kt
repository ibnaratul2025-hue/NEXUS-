package com.example.nexus.core.kernel

import com.example.nexus.core.receipt.ToolReceipt
import com.example.nexus.core.receipt.ToolStatus

enum class MismatchCategory {
    APP_LAUNCH_CONTRADICTION,
    FILE_DELETION_CONTRADICTION,
    FILE_CREATION_CONTRADICTION,
    MEMORY_SAVE_CONTRADICTION,
    BROWSER_READ_CONTRADICTION,
    HARDWARE_CAPTURE_CONTRADICTION,
    PERMISSION_GRANT_CONTRADICTION,
    UNVERIFIED_ACTION_CLAIM
}

data class HallucinationReport(
    val detected: Boolean,
    val claim: String,
    val verifiedState: String,
    val mismatchCategory: MismatchCategory,
    val correctedResponse: String
)

data class ValidationOutcome(
    val isValid: Boolean,
    val verifiedResponse: String,
    val hallucinationReport: HallucinationReport? = null
)

object AntiHallucinationValidator {

    /**
     * Inspects the model's final response against the authoritative list of tool execution receipts.
     * If the model claims an operation succeeded when the corresponding receipt is FAILED or missing,
     * this method detects the contradiction, logs the mismatch, and returns a corrected safe response.
     */
    fun validateResponse(
        userCommand: String,
        modelText: String,
        receipts: List<ToolReceipt>
    ): ValidationOutcome {
        val lowerModel = modelText.lowercase()

        // 1. Check App Launching Claims
        val appLaunchClaimPatterns = listOf(
            "is now open",
            "has been opened",
            "i have opened",
            "i've opened",
            "opened successfully",
            "launched successfully",
            "i launched",
            "app is open",
            "started the app"
        )
        val claimsAppLaunch = appLaunchClaimPatterns.any { lowerModel.contains(it) }
        if (claimsAppLaunch) {
            val launchReceipt = receipts.lastOrNull { it.toolId in listOf("app.launch", "open_app") }
            if (launchReceipt == null) {
                val report = HallucinationReport(
                    detected = true,
                    claim = modelText,
                    verifiedState = "NO_RECEIPT_RECORDED",
                    mismatchCategory = MismatchCategory.UNVERIFIED_ACTION_CLAIM,
                    correctedResponse = "I cannot verify that the application was opened because no launch request was executed."
                )
                return ValidationOutcome(isValid = false, verifiedResponse = report.correctedResponse, hallucinationReport = report)
            } else if (launchReceipt.status != ToolStatus.SUCCESS) {
                val appQuery = extractTargetName(userCommand, listOf("open", "launch", "start"))
                val errReason = launchReceipt.error?.userMessage ?: "it could not be found on this device"
                val report = HallucinationReport(
                    detected = true,
                    claim = modelText,
                    verifiedState = "FAILED: ${launchReceipt.error?.code}",
                    mismatchCategory = MismatchCategory.APP_LAUNCH_CONTRADICTION,
                    correctedResponse = "I couldn't open ${if (appQuery.isNotBlank()) "\"$appQuery\"" else "the app"} because $errReason."
                )
                return ValidationOutcome(isValid = false, verifiedResponse = report.correctedResponse, hallucinationReport = report)
            }
        }

        // 2. Check File Deletion Claims
        val fileDeleteClaimPatterns = listOf(
            "deleted the file",
            "file has been deleted",
            "i deleted",
            "removed the file",
            "permanently removed",
            "file deleted"
        )
        val claimsFileDelete = fileDeleteClaimPatterns.any { lowerModel.contains(it) }
        if (claimsFileDelete) {
            val deleteReceipt = receipts.lastOrNull { it.toolId == "file.delete" }
            if (deleteReceipt == null) {
                val report = HallucinationReport(
                    detected = true,
                    claim = modelText,
                    verifiedState = "NO_FILE_DELETE_RECEIPT",
                    mismatchCategory = MismatchCategory.UNVERIFIED_ACTION_CLAIM,
                    correctedResponse = "I did not delete any files because no deletion operation was executed."
                )
                return ValidationOutcome(isValid = false, verifiedResponse = report.correctedResponse, hallucinationReport = report)
            } else if (deleteReceipt.status != ToolStatus.SUCCESS) {
                val errReason = deleteReceipt.error?.userMessage ?: "the file does not exist in the sandbox"
                val report = HallucinationReport(
                    detected = true,
                    claim = modelText,
                    verifiedState = "FAILED: ${deleteReceipt.error?.code}",
                    mismatchCategory = MismatchCategory.FILE_DELETION_CONTRADICTION,
                    correctedResponse = "I could not delete the file: $errReason."
                )
                return ValidationOutcome(isValid = false, verifiedResponse = report.correctedResponse, hallucinationReport = report)
            }
        }

        // 3. Check File Creation Claims
        val fileCreateClaimPatterns = listOf(
            "created the file",
            "file has been created",
            "saved to file",
            "wrote to file",
            "created a new file"
        )
        val claimsFileCreate = fileCreateClaimPatterns.any { lowerModel.contains(it) }
        if (claimsFileCreate) {
            val createReceipt = receipts.lastOrNull { it.toolId == "file.create" }
            if (createReceipt == null) {
                val report = HallucinationReport(
                    detected = true,
                    claim = modelText,
                    verifiedState = "NO_FILE_CREATE_RECEIPT",
                    mismatchCategory = MismatchCategory.UNVERIFIED_ACTION_CLAIM,
                    correctedResponse = "I have not created any files because no file creation operation was performed."
                )
                return ValidationOutcome(isValid = false, verifiedResponse = report.correctedResponse, hallucinationReport = report)
            } else if (createReceipt.status != ToolStatus.SUCCESS) {
                val errReason = createReceipt.error?.userMessage ?: "the file operation failed"
                val report = HallucinationReport(
                    detected = true,
                    claim = modelText,
                    verifiedState = "FAILED: ${createReceipt.error?.code}",
                    mismatchCategory = MismatchCategory.FILE_CREATION_CONTRADICTION,
                    correctedResponse = "I could not create the file: $errReason."
                )
                return ValidationOutcome(isValid = false, verifiedResponse = report.correctedResponse, hallucinationReport = report)
            }
        }

        // 4. Check Memory Storage Claims
        val memorySaveClaimPatterns = listOf(
            "saved to memory",
            "stored in memory",
            "i've remembered",
            "i have remembered",
            "saved your preference",
            "recorded in my memory"
        )
        val claimsMemorySave = memorySaveClaimPatterns.any { lowerModel.contains(it) }
        if (claimsMemorySave) {
            val memReceipt = receipts.lastOrNull { it.toolId in listOf("memory.save", "memory.store") }
            if (memReceipt == null || memReceipt.status != ToolStatus.SUCCESS) {
                val report = HallucinationReport(
                    detected = true,
                    claim = modelText,
                    verifiedState = "NO_SUCCESSFUL_MEMORY_RECEIPT",
                    mismatchCategory = MismatchCategory.MEMORY_SAVE_CONTRADICTION,
                    correctedResponse = "I have not stored this in persistent memory because no memory save operation was confirmed."
                )
                return ValidationOutcome(isValid = false, verifiedResponse = report.correctedResponse, hallucinationReport = report)
            }
        }

        // 5. Check Camera / Hardware Claims
        val cameraClaimPatterns = listOf(
            "took a picture",
            "captured a photo",
            "taken a photo",
            "picture is taken",
            "captured the image"
        )
        val claimsCamera = cameraClaimPatterns.any { lowerModel.contains(it) }
        if (claimsCamera) {
            val cameraReceipt = receipts.lastOrNull { it.toolId == "camera.capture" }
            if (cameraReceipt == null || cameraReceipt.status != ToolStatus.SUCCESS) {
                val report = HallucinationReport(
                    detected = true,
                    claim = modelText,
                    verifiedState = "NO_CAMERA_RECEIPT",
                    mismatchCategory = MismatchCategory.HARDWARE_CAPTURE_CONTRADICTION,
                    correctedResponse = "I cannot capture photographs because camera hardware capture was not performed."
                )
                return ValidationOutcome(isValid = false, verifiedResponse = report.correctedResponse, hallucinationReport = report)
            }
        }

        // 6. Check Browser Content Reading Claims
        val browserReadPatterns = listOf(
            "i read the webpage",
            "the webpage contains",
            "i read the website",
            "content of the page is"
        )
        val claimsBrowserRead = browserReadPatterns.any { lowerModel.contains(it) }
        if (claimsBrowserRead) {
            // Opening browser does NOT equal reading webpage
            val report = HallucinationReport(
                detected = true,
                claim = modelText,
                verifiedState = "BROWSER_DISPATCH_ONLY",
                mismatchCategory = MismatchCategory.BROWSER_READ_CONTRADICTION,
                correctedResponse = "I opened the system browser for that address, but I cannot read third-party webpage contents without an authorized reading extension."
            )
            return ValidationOutcome(isValid = false, verifiedResponse = report.correctedResponse, hallucinationReport = report)
        }

        // 7. General Failure Guard: If user asked for an action, and the tool explicitly failed, model cannot output generic success
        val failedReceipt = receipts.lastOrNull { it.status == ToolStatus.FAILED }
        if (failedReceipt != null && (lowerModel.contains("all operations completed") || lowerModel.contains("successfully executed") || lowerModel.contains("done!"))) {
            val report = HallucinationReport(
                detected = true,
                claim = modelText,
                verifiedState = "TOOL_FAILED: ${failedReceipt.error?.code}",
                mismatchCategory = MismatchCategory.UNVERIFIED_ACTION_CLAIM,
                correctedResponse = "The requested operation failed: ${failedReceipt.error?.userMessage ?: "Operation could not be completed."}"
            )
            return ValidationOutcome(isValid = false, verifiedResponse = report.correctedResponse, hallucinationReport = report)
        }

        return ValidationOutcome(isValid = true, verifiedResponse = modelText, hallucinationReport = null)
    }

    private fun extractTargetName(command: String, prefixes: List<String>): String {
        var clean = command.trim()
        for (prefix in prefixes) {
            if (clean.startsWith(prefix, ignoreCase = true)) {
                clean = clean.substring(prefix.length).trim()
                break
            }
        }
        return clean.removePrefix("the").removePrefix("an").removePrefix("a").trim('"', '\'', ' ')
    }
}
