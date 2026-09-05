package com.example.nexus.data.repository

import com.example.nexus.data.database.dao.KnowledgeGraphDao
import com.example.nexus.data.database.entity.KnowledgeEdgeEntity
import com.example.nexus.data.database.entity.KnowledgeNodeEntity
import kotlinx.coroutines.flow.Flow

class KnowledgeGraphRepository(private val dao: KnowledgeGraphDao) {
    val allNodes: Flow<List<KnowledgeNodeEntity>> = dao.getAllNodes()
    val allEdges: Flow<List<KnowledgeEdgeEntity>> = dao.getAllEdges()
    val nodeCount: Flow<Int> = dao.getNodeCount()

    fun getNodesByType(type: String): Flow<List<KnowledgeNodeEntity>> =
        dao.getNodesByType(type)

    suspend fun searchNodes(query: String): List<KnowledgeNodeEntity> =
        dao.searchNodesSync(query)

    suspend fun getNodeById(id: String): KnowledgeNodeEntity? =
        dao.getNodeById(id)

    suspend fun getNodeByNameAndType(name: String, type: String): KnowledgeNodeEntity? =
        dao.getNodeByNameAndType(name, type)

    suspend fun saveNode(node: KnowledgeNodeEntity) =
        dao.insertNode(node)

    suspend fun saveEdge(edge: KnowledgeEdgeEntity) =
        dao.insertEdge(edge)

    suspend fun getEdgesForNode(nodeId: String): List<KnowledgeEdgeEntity> =
        dao.getEdgesForNode(nodeId)

    suspend fun deleteNode(node: KnowledgeNodeEntity) =
        dao.deleteNode(node)

    suspend fun clearAll() {
        dao.clearAllEdges()
        dao.clearAllNodes()
    }
}
