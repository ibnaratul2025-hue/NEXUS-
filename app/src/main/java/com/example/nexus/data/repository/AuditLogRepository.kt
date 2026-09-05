package com.example.nexus.data.repository

import com.example.nexus.data.database.dao.AuditLogDao
import com.example.nexus.data.database.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

class AuditLogRepository(private val auditLogDao: AuditLogDao) {
    val recentLogs: Flow<List<AuditLogEntity>> = auditLogDao.getRecentLogs()

    suspend fun log(
        command: String,
        toolId: String,
        riskLevel: String,
        permissionChecked: String,
        userConfirmation: String,
        executionStatus: String,
        resultSummary: String
    ) {
        val entry = AuditLogEntity(
            command = command,
            toolId = toolId,
            riskLevel = riskLevel,
            permissionChecked = permissionChecked,
            userConfirmation = userConfirmation,
            executionStatus = executionStatus,
            resultSummary = resultSummary
        )
        auditLogDao.insertLog(entry)
    }

    suspend fun clearLogs() = auditLogDao.clearLogs()
}
