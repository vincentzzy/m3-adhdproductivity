package com.nowapp.data

import com.nowapp.data.db.AnchorDao
import com.nowapp.data.db.AnchorEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

/** The user's daily anchors. Defaults are seeded once, then fully editable. */
class AnchorRepository(private val dao: AnchorDao) {

    val anchors: Flow<List<AnchorEntity>> = dao.observeAll()

    suspend fun allOnce(): List<AnchorEntity> = dao.allOnce()

    suspend fun upsert(anchor: AnchorEntity): Long = dao.upsert(anchor)

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun seedDefaultsIfEmpty() {
        if (dao.allOnce().isNotEmpty()) return
        dao.upsert(AnchorEntity(label = "Waking", timeOfDay = "08:30"))
        dao.upsert(AnchorEntity(label = "Afternoon", timeOfDay = "14:30"))
        dao.upsert(AnchorEntity(label = "Evening", timeOfDay = "20:00"))
    }

    /** Anchor times in wall-clock order, for the ping scheduler. */
    suspend fun anchorTimes(): List<LocalTime> = dao.allOnce()
        .filter { it.enabled }
        .mapNotNull { runCatching { LocalTime.parse(it.timeOfDay) }.getOrNull() }
        .sorted()

    suspend fun replaceAll(list: List<AnchorEntity>) {
        dao.clear()
        list.forEach { dao.upsert(it.copy(id = 0)) }
    }

    suspend fun clear() = dao.clear()
}
