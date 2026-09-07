package com.example.nexus.core.presence

import com.example.nexus.data.database.entity.MissionEntity
import com.example.nexus.data.repository.PersonalModelRepository
import java.util.UUID

data class ContextualRecommendation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: String, // PROJECT, ROUTINE, SYSTEM, LEARNING, RECOVERY
    val estimatedMinutes: Int,
    val rationale: String,
    val actionCommand: String
)

/**
 * Contextual Action Recommendation Engine ("What Should I Do?").
 * Evaluates current time, active missions, device state, and working memory
 * to produce 3-5 prioritized options.
 */
class WhatShouldIDoEngine(
    private val personalModelRepository: PersonalModelRepository,
    private val timeAwareIntelligence: TimeAwareIntelligence
) {

    suspend fun generateRecommendations(
        context: ContextSnapshot,
        workingMemory: WorkingMemoryState,
        activeMission: MissionEntity?
    ): List<ContextualRecommendation> {
        val list = mutableListOf<ContextualRecommendation>()

        // 1. Resume active paused/running mission if present
        if (activeMission != null && activeMission.status in listOf("RUNNING", "PAUSED")) {
            list.add(
                ContextualRecommendation(
                    title = "Resume Mission: ${activeMission.title}",
                    description = "Continue step ${activeMission.currentStep + 1} of ${activeMission.totalSteps}",
                    category = "PROJECT",
                    estimatedMinutes = 15,
                    rationale = "Mission is currently in ${activeMission.status} state with ${(activeMission.progress * 100).toInt()}% progress",
                    actionCommand = "mission.resume ${activeMission.id}"
                )
            )
        }

        // 2. Project from Context
        if (context.currentProject != null) {
            list.add(
                ContextualRecommendation(
                    title = "Continue ${context.currentProject}",
                    description = "Pick up where you left off on project files",
                    category = "PROJECT",
                    estimatedMinutes = 25,
                    rationale = "Project is actively loaded in working session",
                    actionCommand = "project.open ${context.currentProject}"
                )
            )
        }

        // 3. System maintenance recommendation if storage or battery is low
        if (context.isLowStorage) {
            list.add(
                ContextualRecommendation(
                    title = "Reclaim Storage Space",
                    description = "Clean cached application files and temporary logs",
                    category = "SYSTEM",
                    estimatedMinutes = 3,
                    rationale = "Device storage is over 90% full",
                    actionCommand = "system.storage_clean"
                )
            )
        }

        // 4. Time-of-day routine
        val timeOfDay = timeAwareIntelligence.getTimeOfDay(context.hourOfDay)
        when (timeOfDay) {
            TimeOfDay.MORNING -> {
                list.add(
                    ContextualRecommendation(
                        title = "Morning Objective Review",
                        description = "Review your goals and structure today's priorities",
                        category = "ROUTINE",
                        estimatedMinutes = 10,
                        rationale = "Optimal morning alignment window",
                        actionCommand = "routine.morning_plan"
                    )
                )
            }
            TimeOfDay.EVENING, TimeOfDay.NIGHT -> {
                list.add(
                    ContextualRecommendation(
                        title = "Daily Summary & State Save",
                        description = "Review actions taken today and commit working state",
                        category = "ROUTINE",
                        estimatedMinutes = 5,
                        rationale = "Evening wind-down routine",
                        actionCommand = "routine.evening_summary"
                    )
                )
            }
            TimeOfDay.AFTERNOON -> {
                list.add(
                    ContextualRecommendation(
                        title = "Focus Block: Next Priority Goal",
                        description = "Tackle your primary pending task in a focused sprint",
                        category = "ROUTINE",
                        estimatedMinutes = 30,
                        rationale = "High-energy afternoon window",
                        actionCommand = "routine.focus_sprint"
                    )
                )
            }
        }

        // 5. If failure occurred recently, recommend fixing it
        val lastFailure = workingMemory.recentFailures.lastOrNull()
        if (lastFailure != null) {
            list.add(
                ContextualRecommendation(
                    title = "Resolve Failed Action",
                    description = "Fix error for ${lastFailure.toolId}: ${lastFailure.errorSummary}",
                    category = "RECOVERY",
                    estimatedMinutes = 5,
                    rationale = "Recent execution error logged in working memory",
                    actionCommand = "recovery.retry ${lastFailure.toolId}"
                )
            )
        }

        return list.take(5)
    }
}
