package com.example.nexus.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.database.entity.ModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM models ORDER BY importedAt DESC")
    fun getAllModels(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE isActive = 1 LIMIT 1")
    fun getActiveModel(): Flow<ModelEntity?>

    @Query("SELECT * FROM models WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveModelSync(): ModelEntity?

    @Query("SELECT * FROM models WHERE id = :id")
    suspend fun getModelById(id: String): ModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelEntity)

    @Update
    suspend fun updateModel(model: ModelEntity)

    @Query("UPDATE models SET isActive = 0")
    suspend fun clearActiveStatus()

    @Query("UPDATE models SET isActive = 1 WHERE id = :id")
    suspend fun setActiveModel(id: String)

    @Delete
    suspend fun deleteModel(model: ModelEntity)

    @Query("DELETE FROM models WHERE id = :id")
    suspend fun deleteModelById(id: String)
}
