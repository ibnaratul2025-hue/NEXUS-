package com.example.nexus.core.cognitive.skill

import com.example.nexus.data.database.entity.SkillEntity
import com.example.nexus.data.repository.SkillRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SkillEngine(
    private val skillRepository: SkillRepository,
    private val testRunner: SkillTestRunner
) {

    /**
     * Converts a database entity into domain SkillModel.
     */
    fun toModel(entity: SkillEntity): SkillModel {
        val stepsList = mutableListOf<SkillStep>()
        try {
            val jsonArr = JSONArray(entity.stepsJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val argsMap = mutableMapOf<String, String>()
                val argsObj = obj.optJSONObject("argumentsTemplate")
                argsObj?.keys()?.forEach { k -> argsMap[k] = argsObj.optString(k) }

                val permsList = mutableListOf<String>()
                val permsArr = obj.optJSONArray("requiredPermissions")
                if (permsArr != null) {
                    for (p in 0 until permsArr.length()) permsList.add(permsArr.getString(p))
                }

                stepsList.add(
                    SkillStep(
                        stepIndex = obj.optInt("stepIndex", i + 1),
                        toolId = obj.optString("toolId"),
                        description = obj.optString("description"),
                        argumentsTemplate = argsMap,
                        requiredPermissions = permsList
                    )
                )
            }
        } catch (e: Exception) {
            // Safe fallback
        }

        val toolsList = mutableListOf<String>()
        try {
            val arr = JSONArray(entity.requiredToolsJson)
            for (i in 0 until arr.length()) toolsList.add(arr.getString(i))
        } catch (e: Exception) { }

        val permsList = mutableListOf<String>()
        try {
            val arr = JSONArray(entity.requiredPermissionsJson)
            for (i in 0 until arr.length()) permsList.add(arr.getString(i))
        } catch (e: Exception) { }

        val stateEnum = try {
            SkillState.valueOf(entity.state)
        } catch (e: Exception) {
            SkillState.DISCOVERED
        }

        return SkillModel(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            state = stateEnum,
            version = entity.version,
            triggerIntent = entity.triggerIntent,
            steps = stepsList,
            requiredTools = toolsList,
            requiredPermissions = permsList,
            riskLevel = entity.riskLevel,
            successCount = entity.successCount,
            failureCount = entity.failureCount,
            author = entity.author
        )
    }

    suspend fun discoverSkill(
        name: String,
        description: String,
        triggerIntent: String,
        steps: List<SkillStep>,
        riskLevel: String = "LOW"
    ): SkillEntity = withContext(Dispatchers.IO) {
        val stepsJson = serializeSteps(steps)
        val toolsJson = JSONArray(steps.map { it.toolId }.distinct()).toString()
        val permsJson = JSONArray(steps.flatMap { it.requiredPermissions }.distinct()).toString()

        val entity = SkillEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            state = SkillState.DISCOVERED.name,
            version = 1,
            triggerIntent = triggerIntent,
            stepsJson = stepsJson,
            requiredToolsJson = toolsJson,
            requiredPermissionsJson = permsJson,
            riskLevel = riskLevel,
            author = "LEARNED_FROM_WORKFLOW"
        )
        skillRepository.saveSkill(entity)
        entity
    }

    suspend fun testSkill(skillId: String): SkillTestResult = withContext(Dispatchers.IO) {
        val entity = skillRepository.getSkillById(skillId) ?: return@withContext SkillTestResult(
            isPassed = false,
            issues = listOf("Skill $skillId not found"),
            testedStepCount = 0,
            durationMs = 0
        )
        val model = toModel(entity)
        skillRepository.updateSkillState(skillId, SkillState.TESTING.name)

        val result = testRunner.testSkill(model)
        val nextState = if (result.isPassed) SkillState.VERIFIED.name else SkillState.DRAFT.name
        skillRepository.updateSkillState(skillId, nextState)
        result
    }

    suspend fun approveAndActivateSkill(skillId: String): Boolean = withContext(Dispatchers.IO) {
        val entity = skillRepository.getSkillById(skillId) ?: return@withContext false
        if (entity.state == SkillState.VERIFIED.name || entity.state == SkillState.DRAFT.name) {
            skillRepository.updateSkillState(skillId, SkillState.USER_APPROVED.name)
            skillRepository.updateSkillState(skillId, SkillState.ACTIVE.name)
            true
        } else {
            false
        }
    }

    suspend fun disableSkill(skillId: String) = withContext(Dispatchers.IO) {
        skillRepository.updateSkillState(skillId, SkillState.DRAFT.name)
    }

    suspend fun deleteSkill(skillId: String) = withContext(Dispatchers.IO) {
        skillRepository.deleteSkillById(skillId)
    }

    suspend fun rollbackSkill(skillId: String): Boolean = withContext(Dispatchers.IO) {
        val entity = skillRepository.getSkillById(skillId) ?: return@withContext false
        if (entity.version > 1) {
            val rolledBack = entity.copy(
                version = entity.version - 1,
                state = SkillState.DRAFT.name,
                updatedAt = System.currentTimeMillis()
            )
            skillRepository.updateSkill(rolledBack)
            true
        } else {
            false
        }
    }

    suspend fun seedInitialSkills() = withContext(Dispatchers.IO) {
        if (skillRepository.getActiveSkillsSync().isEmpty()) {
            discoverSkill(
                name = "Inspect Device Telemetry",
                description = "Reads internal battery, memory, and filesystem metrics safely",
                triggerIntent = "COMMAND",
                steps = listOf(
                    SkillStep(1, "system.info", "Query device system info")
                )
            )
        }
    }

    fun exportSkillToJson(model: SkillModel): String {
        val obj = JSONObject()
        obj.put("id", model.id)
        obj.put("name", model.name)
        obj.put("description", model.description)
        obj.put("version", model.version)
        obj.put("triggerIntent", model.triggerIntent)
        obj.put("riskLevel", model.riskLevel)
        obj.put("steps", JSONArray(serializeSteps(model.steps)))
        return obj.toString(2)
    }

    suspend fun importSkillFromJson(jsonStr: String): SkillEntity = withContext(Dispatchers.IO) {
        val obj = JSONObject(jsonStr)
        val name = obj.getString("name")
        val desc = obj.optString("description", "")
        val trigger = obj.optString("triggerIntent", "")
        val risk = obj.optString("riskLevel", "LOW")
        val stepsArr = obj.optJSONArray("steps") ?: JSONArray()

        val entity = SkillEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = desc,
            state = SkillState.DRAFT.name, // Imported skills start in DRAFT for safe user review
            version = 1,
            triggerIntent = trigger,
            stepsJson = stepsArr.toString(),
            riskLevel = risk,
            author = "IMPORTED"
        )
        skillRepository.saveSkill(entity)
        entity
    }

    private fun serializeSteps(steps: List<SkillStep>): String {
        val arr = JSONArray()
        for (step in steps) {
            val stepObj = JSONObject()
            stepObj.put("stepIndex", step.stepIndex)
            stepObj.put("toolId", step.toolId)
            stepObj.put("description", step.description)

            val argsObj = JSONObject()
            step.argumentsTemplate.forEach { (k, v) -> argsObj.put(k, v) }
            stepObj.put("argumentsTemplate", argsObj)

            val permsArr = JSONArray(step.requiredPermissions)
            stepObj.put("requiredPermissions", permsArr)
            arr.put(stepObj)
        }
        return arr.toString()
    }
}
