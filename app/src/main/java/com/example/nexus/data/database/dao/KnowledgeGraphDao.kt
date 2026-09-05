package com.example.nexus.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.database.entity.KnowledgeEdgeEntity
import com.example.nexus.data.database.entity.KnowledgeNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeGraphDao {
    @Query("SELECT * FROM knowledge_nodes ORDER BY updatedAt DESC")
    fun getAllNodes(): Flow<List<KnowledgeNodeEntity>>

    @Query("SELECT * FROM knowledge_nodes WHERE entityType = :type ORDER BY name ASC")
    fun getNodesByType(type: String): Flow<List<KnowledgeNodeEntity>>

    @Query("SELECT * FROM knowledge_nodes WHERE name LIKE '%' || :query || '%' LIMIT 10")
    suspend fun searchNodesSync(query: String): List<KnowledgeNodeEntity>

    @Query("SELECT * FROM knowledge_nodes WHERE id = :id LIMIT 1")
    suspend fun getNodeById(id: String): KnowledgeNodeEntity?

    @Query("SELECT * FROM knowledge_nodes WHERE name = :name AND entityType = :type LIMIT 1")
    suspend fun getNodeByNameAndType(name: String, type: String): KnowledgeNodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: KnowledgeNodeEntity)

    @Update
    suspend fun updateNode(node: KnowledgeNodeEntity)

    @Delete
    suspend fun deleteNode(node: KnowledgeNodeEntity)

    @Query("SELECT * FROM knowledge_edges WHERE sourceNodeId = :nodeId OR targetNodeId = :nodeId")
    suspend fun getEdgesForNode(nodeId: String): List<KnowledgeEdgeEntity>

    @Query("SELECT * FROM knowledge_edges ORDER BY createdAt DESC")
    fun getAllEdges(): Flow<List<KnowledgeEdgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdge(edge: KnowledgeEdgeEntity)

    @Delete
    suspend fun deleteEdge(edge: KnowledgeEdgeEntity)

    @Query("DELETE FROM knowledge_nodes")
    suspend fun clearAllNodes()

    @Query("DELETE FROM knowledge_edges")
    suspend fun clearAllEdges()

    @Query("SELECT COUNT(*) FROM knowledge_nodes")
    fun getNodeCount(): Flow<Int>
}
