package com.example.nexus.core.presence

import com.example.nexus.data.database.entity.PersonalModelEntity
import com.example.nexus.data.repository.PersonalModelRepository
import kotlinx.coroutines.flow.Flow

/**
 * Local Personal Model ("Digital Twin") Manager.
 * Maintains an editable local productivity model of the user's goals, projects,
 * routines, preferences, and workflows.
 * Complete transparency: user can VIEW, EDIT, DELETE, EXPORT, and RESET everything.
 */
class PersonalModel(
    private val repository: PersonalModelRepository
) {

    fun getAllEntries(): Flow<List<PersonalModelEntity>> = repository.getAllEntries()

    suspend fun getActiveEntries(): List<PersonalModelEntity> = repository.getActiveEntriesSync()

    suspend fun getEntriesByCategory(category: String): List<PersonalModelEntity> =
        repository.getEntriesByCategorySync(category)

    suspend fun addOrUpdateEntry(
        category: String,
        title: String,
        detailsJson: String,
        priority: Int = 1
    ): PersonalModelEntity {
        val entry = PersonalModelEntity(
            category = category.uppercase(),
            title = title,
            detailsJson = detailsJson,
            priority = priority,
            isActive = true
        )
        repository.saveEntry(entry)
        return entry
    }

    suspend fun deleteEntry(id: String) {
        repository.deleteById(id)
    }

    suspend fun exportJson(): String {
        val entries = repository.getActiveEntriesSync()
        val items = entries.joinToString(",\n  ") {
            "{\"id\": \"${it.id}\", \"category\": \"${it.category}\", \"title\": \"${it.title}\", \"priority\": ${it.priority}}"
        }
        return "[\n  $items\n]"
    }

    suspend fun resetAll() {
        repository.purgeAll()
    }
}
