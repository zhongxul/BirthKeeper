package com.zhongxul.birthkeeper.core.data.repository

import androidx.room.withTransaction
import com.zhongxul.birthkeeper.core.common.util.BackupCipher
import com.zhongxul.birthkeeper.core.data.db.BirthKeeperDatabase
import com.zhongxul.birthkeeper.core.data.db.entity.PersonEntity
import com.zhongxul.birthkeeper.core.data.db.entity.PersonWithReminder
import com.zhongxul.birthkeeper.core.data.db.entity.ReminderConfigEntity
import com.zhongxul.birthkeeper.core.domain.repository.BackupImportMode
import com.zhongxul.birthkeeper.core.domain.repository.BackupImportResult
import com.zhongxul.birthkeeper.core.domain.repository.BackupRepository
import org.json.JSONArray
import org.json.JSONObject

private const val BACKUP_VERSION = 1

class BackupRepositoryImpl(
    private val database: BirthKeeperDatabase
) : BackupRepository {

    override suspend fun exportEncryptedBackup(): String {
        val people = database.personDao().listAllActive()
        val root = JSONObject()
            .put("version", BACKUP_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("people", JSONArray().apply {
                people.forEach { personWithReminder ->
                    put(personWithReminder.toJson())
                }
            })
        return BackupCipher.encrypt(root.toString())
    }

    override suspend fun importEncryptedBackup(
        payload: String,
        mode: BackupImportMode
    ): BackupImportResult {
        val decrypted = BackupCipher.decrypt(payload)
        val root = JSONObject(decrypted)
        val version = root.optInt("version", -1)
        require(version == BACKUP_VERSION) { "backup version mismatch: $version" }
        val peopleArray = root.optJSONArray("people") ?: JSONArray()

        var imported = 0
        var updated = 0
        var skipped = 0
        database.withTransaction {
            if (mode == BackupImportMode.OVERWRITE) {
                database.reminderLogDao().deleteAll()
                database.reminderConfigDao().deleteAll()
                database.personDao().deleteAll()
            }
            for (index in 0 until peopleArray.length()) {
                val item = peopleArray.getJSONObject(index)
                val incomingPerson = item.toPersonEntity()
                val incomingReminder = item.optJSONObject("reminderConfig")?.toReminderConfigEntity(incomingPerson.id)

                val existing = if (mode == BackupImportMode.MERGE) {
                    database.personDao().findPersonEntityById(incomingPerson.id)
                } else {
                    null
                }

                val shouldWrite = mode == BackupImportMode.OVERWRITE ||
                    existing == null ||
                    incomingPerson.updatedAt >= existing.updatedAt
                if (!shouldWrite) {
                    skipped += 1
                    continue
                }

                database.personDao().upsertPerson(incomingPerson)
                if (incomingReminder != null) {
                    database.reminderConfigDao().upsert(incomingReminder)
                } else {
                    database.reminderConfigDao().deleteByPersonId(incomingPerson.id)
                }

                if (existing == null) {
                    imported += 1
                } else {
                    updated += 1
                }
            }
        }
        return BackupImportResult(
            importedCount = imported,
            updatedCount = updated,
            skippedCount = skipped
        )
    }
}

private fun PersonWithReminder.toJson(): JSONObject {
    val personObject = JSONObject()
        .put("id", person.id)
        .put("name", person.name)
        .put("idNumberEncrypted", person.idNumberEncrypted.toJsonNullable())
        .put("birthdaySolar", person.birthdaySolar)
        .put("birthdayLunar", person.birthdayLunar.toJsonNullable())
        .put("gender", person.gender)
        .put("relation", person.relation)
        .put("note", person.note.toJsonNullable())
        .put("avatarUri", person.avatarUri.toJsonNullable())
        .put("isDeleted", person.isDeleted)
        .put("createdAt", person.createdAt)
        .put("updatedAt", person.updatedAt)
    reminderConfig?.let { config ->
        personObject.put(
            "reminderConfig",
            JSONObject()
                .put("id", config.id)
                .put("personId", config.personId)
                .put("offsetsJson", config.offsetsJson)
                .put("remindTime", config.remindTime)
                .put("enabled", config.enabled)
        )
    }
    return personObject
}

private fun JSONObject.toPersonEntity(): PersonEntity {
    return PersonEntity(
        id = getLong("id"),
        name = getString("name"),
        idNumberEncrypted = optStringOrNull("idNumberEncrypted"),
        birthdaySolar = getString("birthdaySolar"),
        birthdayLunar = optStringOrNull("birthdayLunar"),
        gender = getInt("gender"),
        relation = getString("relation"),
        note = optStringOrNull("note"),
        avatarUri = optStringOrNull("avatarUri"),
        isDeleted = optInt("isDeleted", 0),
        createdAt = getLong("createdAt"),
        updatedAt = getLong("updatedAt")
    )
}

private fun JSONObject.toReminderConfigEntity(personId: Long): ReminderConfigEntity {
    return ReminderConfigEntity(
        id = optLong("id", 0L),
        personId = personId,
        offsetsJson = getString("offsetsJson"),
        remindTime = getString("remindTime"),
        enabled = getInt("enabled")
    )
}

private fun String?.toJsonNullable(): Any {
    return this ?: JSONObject.NULL
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) {
        return null
    }
    return getString(key)
}
