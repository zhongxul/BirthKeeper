package com.zhongxul.birthkeeper.core.domain.repository

enum class BackupImportMode {
    OVERWRITE,
    MERGE
}

data class BackupImportResult(
    val importedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int
)

interface BackupRepository {
    suspend fun exportEncryptedBackup(): String
    suspend fun importEncryptedBackup(
        payload: String,
        mode: BackupImportMode
    ): BackupImportResult
}
