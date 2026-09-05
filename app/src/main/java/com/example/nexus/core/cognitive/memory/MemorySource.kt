package com.example.nexus.core.cognitive.memory

enum class MemorySource {
    USER_EXPLICIT,
    USER_CORRECTION,
    OBSERVED_RESULT,
    SUCCESSFUL_WORKFLOW,
    SYSTEM_FACT,
    INFERRED
}

enum class KnowledgeEntityType {
    PEOPLE,
    PROJECTS,
    DEVICES,
    APPS,
    TASKS,
    PREFERENCES,
    HABITS,
    SKILLS,
    WORKFLOWS,
    DOCUMENTS,
    GENERAL
}
