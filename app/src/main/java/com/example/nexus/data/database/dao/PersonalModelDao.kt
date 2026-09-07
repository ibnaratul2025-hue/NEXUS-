package com.example.nexus.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.database.entity.PersonalModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalModelDao {
    @Query("SELECT * FROM personal_model ORDER BY category ASC, priority DESC")
    fun getAllEntries(): Flow<List<PersonalModelEntity>>

    @Query("SELECT * FROM personal_model WHERE isActive = 1 ORDER BY priority DESC")
    fun getActiveEntries(): Flow<List<PersonalModelEntity>>

    @Query("SELECT * FROM personal_model WHERE isActive = 1")
    suspend fun getActiveEntriesSync(): List<PersonalModelEntity>

    @Query("SELECT * FROM personal_model WHERE category = :category AND isActive = 1 ORDER BY priority DESC")
    fun getEntriesByCategory(category: String): Flow<List<PersonalModelEntity>>

    @Query("SELECT * FROM personal_model WHERE category = :category AND isActive = 1 ORDER BY priority DESC")
    suspend fun getEntriesByCategorySync(category: String): List<PersonalModelEntity>

    @Query("SELECT * FROM personal_model WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: String): PersonalModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PersonalModelEntity)

    @Update
    suspend fun updateEntry(entry: PersonalModelEntity)

    @Delete
    suspend fun deleteEntry(entry: PersonalModelEntity)

    @Query("DELETE FROM personal_model WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM personal_model")
    suspend fun purgeAll()
}
