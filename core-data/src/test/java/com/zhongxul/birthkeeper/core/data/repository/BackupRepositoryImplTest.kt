package com.zhongxul.birthkeeper.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.zhongxul.birthkeeper.core.common.util.BackupCipher
import com.zhongxul.birthkeeper.core.data.db.BirthKeeperDatabase
import com.zhongxul.birthkeeper.core.data.db.entity.PersonEntity
import com.zhongxul.birthkeeper.core.data.db.entity.ReminderConfigEntity
import com.zhongxul.birthkeeper.core.domain.repository.BackupImportMode
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRepositoryImplTest {

    private lateinit var database: BirthKeeperDatabase
    private lateinit var repository: BackupRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BirthKeeperDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BackupRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportThenImportOverwrite_shouldRestorePersonAndReminderConfig() = runTest {
        database.personDao().upsertPerson(
            personEntity(
                id = 101L,
                name = "张三",
                relation = "父亲",
                updatedAt = 1_000L
            )
        )
        database.reminderConfigDao().upsert(
            reminderConfigEntity(
                id = 201L,
                personId = 101L,
                offsetsJson = "[7,3,1,0]",
                remindTime = "09:00"
            )
        )

        val payload = repository.exportEncryptedBackup()

        database.reminderConfigDao().deleteAll()
        database.personDao().deleteAll()

        val result = repository.importEncryptedBackup(payload, BackupImportMode.OVERWRITE)
        assertEquals(1, result.importedCount)
        assertEquals(0, result.updatedCount)
        assertEquals(0, result.skippedCount)

        val restored = database.personDao().findById(101L)
        assertNotNull(restored)
        assertEquals("张三", restored?.person?.name)
        assertEquals("父亲", restored?.person?.relation)
        assertEquals("[7,3,1,0]", restored?.reminderConfig?.offsetsJson)
        assertEquals("09:00", restored?.reminderConfig?.remindTime)
    }

    @Test
    fun importMerge_shouldSkipOlderAndUpdateNewerRecord() = runTest {
        database.personDao().upsertPerson(
            personEntity(
                id = 1L,
                name = "本地记录",
                relation = "母亲",
                updatedAt = 200L
            )
        )
        database.reminderConfigDao().upsert(
            reminderConfigEntity(
                id = 301L,
                personId = 1L,
                offsetsJson = "[7]",
                remindTime = "08:00"
            )
        )

        val olderPayload = buildBackupPayload(
            personJson(
                id = 1L,
                name = "备份旧记录",
                relation = "母亲",
                updatedAt = 100L,
                offsetsJson = "[7,3]",
                remindTime = "09:00"
            )
        )
        val olderResult = repository.importEncryptedBackup(olderPayload, BackupImportMode.MERGE)
        assertEquals(0, olderResult.importedCount)
        assertEquals(0, olderResult.updatedCount)
        assertEquals(1, olderResult.skippedCount)

        val unchanged = database.personDao().findById(1L)
        assertEquals("本地记录", unchanged?.person?.name)
        assertEquals("[7]", unchanged?.reminderConfig?.offsetsJson)

        val newerPayload = buildBackupPayload(
            personJson(
                id = 1L,
                name = "备份新记录",
                relation = "母亲",
                updatedAt = 300L,
                offsetsJson = "[7,3,1]",
                remindTime = "10:00"
            )
        )
        val newerResult = repository.importEncryptedBackup(newerPayload, BackupImportMode.MERGE)
        assertEquals(0, newerResult.importedCount)
        assertEquals(1, newerResult.updatedCount)
        assertEquals(0, newerResult.skippedCount)

        val updated = database.personDao().findById(1L)
        assertEquals("备份新记录", updated?.person?.name)
        assertEquals(300L, updated?.person?.updatedAt)
        assertEquals("[7,3,1]", updated?.reminderConfig?.offsetsJson)
        assertEquals("10:00", updated?.reminderConfig?.remindTime)
    }

    @Test
    fun importOverwrite_whenPayloadInvalid_shouldRollbackTransaction() = runTest {
        database.personDao().upsertPerson(
            personEntity(
                id = 99L,
                name = "原始数据",
                relation = "朋友",
                updatedAt = 1_000L
            )
        )
        database.reminderConfigDao().upsert(
            reminderConfigEntity(
                id = 401L,
                personId = 99L,
                offsetsJson = "[1,0]",
                remindTime = "07:30"
            )
        )

        val invalidPayload = buildBackupPayload(
            personJson(
                id = 1L,
                name = "有效记录",
                relation = "父亲",
                updatedAt = 2_000L,
                offsetsJson = "[7]",
                remindTime = "08:30"
            ),
            JSONObject()
                .put("id", 2L)
                .put("birthdaySolar", "1988-01-01")
                .put("createdAt", 1L)
                .put("updatedAt", 1L)
        )

        try {
            repository.importEncryptedBackup(invalidPayload, BackupImportMode.OVERWRITE)
            fail("导入非法 payload 应抛出异常")
        } catch (error: Throwable) {
            assertTrue(error is org.json.JSONException)
        }

        val people = database.personDao().listAllActive()
        assertEquals(1, people.size)
        assertEquals(99L, people.first().person.id)
        assertEquals("原始数据", people.first().person.name)
        assertEquals("[1,0]", people.first().reminderConfig?.offsetsJson)
    }

    private fun personEntity(
        id: Long,
        name: String,
        relation: String,
        updatedAt: Long
    ): PersonEntity {
        return PersonEntity(
            id = id,
            name = name,
            idNumberEncrypted = "encrypted_$id",
            birthdaySolar = "1990-01-01",
            birthdayLunar = null,
            gender = 1,
            relation = relation,
            note = null,
            avatarUri = null,
            isDeleted = 0,
            createdAt = 1L,
            updatedAt = updatedAt
        )
    }

    private fun reminderConfigEntity(
        id: Long,
        personId: Long,
        offsetsJson: String,
        remindTime: String
    ): ReminderConfigEntity {
        return ReminderConfigEntity(
            id = id,
            personId = personId,
            offsetsJson = offsetsJson,
            remindTime = remindTime,
            enabled = 1
        )
    }

    private fun buildBackupPayload(vararg people: JSONObject): String {
        val root = JSONObject()
            .put("version", 1)
            .put("exportedAt", 0L)
            .put(
                "people",
                JSONArray().apply {
                    people.forEach { put(it) }
                }
            )
        return BackupCipher.encrypt(root.toString())
    }

    private fun personJson(
        id: Long,
        name: String,
        relation: String,
        updatedAt: Long,
        offsetsJson: String,
        remindTime: String
    ): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("idNumberEncrypted", JSONObject.NULL)
            .put("birthdaySolar", "1990-01-01")
            .put("birthdayLunar", JSONObject.NULL)
            .put("gender", 1)
            .put("relation", relation)
            .put("note", JSONObject.NULL)
            .put("avatarUri", JSONObject.NULL)
            .put("isDeleted", 0)
            .put("createdAt", 1L)
            .put("updatedAt", updatedAt)
            .put(
                "reminderConfig",
                JSONObject()
                    .put("id", id + 1000L)
                    .put("personId", id)
                    .put("offsetsJson", offsetsJson)
                    .put("remindTime", remindTime)
                    .put("enabled", 1)
            )
    }
}
