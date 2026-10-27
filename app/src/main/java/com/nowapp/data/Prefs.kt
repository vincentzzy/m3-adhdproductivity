package com.nowapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nowapp.domain.QuietHours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "now_prefs")

/**
 * Every user-facing knob, in one typed snapshot.
 *
 * DataStore rather than Room because these are single values that must be read
 * synchronously-ish at startup (calm mode, quiet hours) without a query.
 */
data class Prefs(
    val onboarded: Boolean = false,
    val calmMode: Boolean = false,
    val quietStart: LocalTime = LocalTime.of(21, 30),
    val quietEnd: LocalTime = LocalTime.of(8, 0),
    val quietForTodayKey: String? = null,
    val lastBreakAt: Long = 0L,
    val lastActiveDay: String? = null,
    val awakeStart: LocalTime = LocalTime.of(7, 0),
    val awakeEnd: LocalTime = LocalTime.of(23, 0),
    val snoozeTaskId: Long? = null,
    val snoozeUntil: Long = 0L,
    val welcomeBackAckDay: String? = null,
    val pingDayKey: String = "",
    val timedPings: Int = 0,
    val escalations: Int = 0,
    val escalationLog: String = "",
    val notificationPermissionAsked: Boolean = false,
) {
    val quietHours: QuietHours get() = QuietHours(quietStart, quietEnd)
}

class PrefsRepository(private val context: Context) {

    private object Keys {
        val onboarded = booleanPreferencesKey("onboarded")
        val calmMode = booleanPreferencesKey("calm_mode")
        val quietStart = stringPreferencesKey("quiet_start")
        val quietEnd = stringPreferencesKey("quiet_end")
        val quietForTodayKey = stringPreferencesKey("quiet_for_today_key")
        val lastBreakAt = longPreferencesKey("last_break_at")
        val lastActiveDay = stringPreferencesKey("last_active_day")
        val awakeStart = stringPreferencesKey("awake_start")
        val awakeEnd = stringPreferencesKey("awake_end")
        val snoozeTaskId = longPreferencesKey("snooze_task_id")
        val snoozeUntil = longPreferencesKey("snooze_until")
        val welcomeBackAckDay = stringPreferencesKey("welcome_back_ack_day")
        val pingDayKey = stringPreferencesKey("ping_day_key")
        val timedPings = intPreferencesKey("timed_pings")
        val escalations = intPreferencesKey("escalations")
        val escalationLog = stringPreferencesKey("escalation_log")
        val notifAsked = booleanPreferencesKey("notif_asked")
    }

    val prefs: Flow<Prefs> = context.dataStore.data.map { p ->
        Prefs(
            onboarded = p[Keys.onboarded] ?: false,
            calmMode = p[Keys.calmMode] ?: false,
            quietStart = parseTime(p[Keys.quietStart], LocalTime.of(21, 30)),
            quietEnd = parseTime(p[Keys.quietEnd], LocalTime.of(8, 0)),
            quietForTodayKey = p[Keys.quietForTodayKey],
            lastBreakAt = p[Keys.lastBreakAt] ?: 0L,
            lastActiveDay = p[Keys.lastActiveDay],
            awakeStart = parseTime(p[Keys.awakeStart], LocalTime.of(7, 0)),
            awakeEnd = parseTime(p[Keys.awakeEnd], LocalTime.of(23, 0)),
            snoozeTaskId = p[Keys.snoozeTaskId],
            snoozeUntil = p[Keys.snoozeUntil] ?: 0L,
            welcomeBackAckDay = p[Keys.welcomeBackAckDay],
            pingDayKey = p[Keys.pingDayKey] ?: "",
            timedPings = p[Keys.timedPings] ?: 0,
            escalations = p[Keys.escalations] ?: 0,
            escalationLog = p[Keys.escalationLog] ?: "",
            notificationPermissionAsked = p[Keys.notifAsked] ?: false,
        )
    }

    suspend fun setOnboarded(value: Boolean) = edit { it[Keys.onboarded] = value }
    suspend fun setCalmMode(value: Boolean) = edit { it[Keys.calmMode] = value }
    suspend fun setQuietHours(start: LocalTime, end: LocalTime) = edit {
        it[Keys.quietStart] = formatTime(start)
        it[Keys.quietEnd] = formatTime(end)
    }
    suspend fun setQuietForToday(dayKey: String?) = edit {
        if (dayKey == null) it.remove(Keys.quietForTodayKey) else it[Keys.quietForTodayKey] = dayKey
    }
    suspend fun setAwakeWindow(start: LocalTime, end: LocalTime) = edit {
        it[Keys.awakeStart] = formatTime(start)
        it[Keys.awakeEnd] = formatTime(end)
    }
    suspend fun markBreak(epochMillis: Long) = edit { it[Keys.lastBreakAt] = epochMillis }
    suspend fun setLastActiveDay(dayKey: String) = edit { it[Keys.lastActiveDay] = dayKey }
    suspend fun setWelcomeBackAck(dayKey: String) = edit { it[Keys.welcomeBackAckDay] = dayKey }
    suspend fun snooze(taskId: Long, until: Long) = edit {
        it[Keys.snoozeTaskId] = taskId
        it[Keys.snoozeUntil] = until
    }
    suspend fun clearSnooze() = edit {
        it.remove(Keys.snoozeTaskId)
        it[Keys.snoozeUntil] = 0L
    }
    suspend fun setPingCounters(dayKey: String, timed: Int, escalations: Int, log: String) = edit {
        it[Keys.pingDayKey] = dayKey
        it[Keys.timedPings] = timed
        it[Keys.escalations] = escalations
        it[Keys.escalationLog] = log
    }
    suspend fun setNotificationAsked() = edit { it[Keys.notifAsked] = true }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    private fun parseTime(value: String?, fallback: LocalTime): LocalTime =
        value?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: fallback

    private fun formatTime(time: LocalTime): String = time.toString()
}
