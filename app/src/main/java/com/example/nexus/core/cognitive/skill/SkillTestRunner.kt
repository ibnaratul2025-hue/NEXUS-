package com.example.nexus.core.cognitive.skill

import com.example.nexus.core.tool.ToolRegistry

data class SkillTestResult(
    val isPassed: Boolean,
    val issues: List<String>,
    val testedStepCount: Int,
    val durationMs: Long
)

class SkillTestRunner(
    private val toolRegistry: ToolRegistry
) {

    fun testSkill(skill: SkillModel): SkillTestResult {
        val startTime = System.currentTimeMillis()
        val issues = mutableListOf<String>()

        if (skill.steps.isEmpty()) {
            issues.add("Skill has no defined execution steps.")
        }

        for (step in skill.steps) {
            val tool = toolRegistry.getTool(step.toolId)
            if (tool == null) {
                issues.add("Required tool '${step.toolId}' is not registered in ToolRegistry.")
            }
        }

        if (skill.name.isBlank()) {
            issues.add("Skill name cannot be empty.")
        }

        val passed = issues.isEmpty()
        val duration = System.currentTimeMillis() - startTime

        return SkillTestResult(
            isPassed = passed,
            issues = issues,
            testedStepCount = skill.steps.size,
            durationMs = duration
        )
    }
}
