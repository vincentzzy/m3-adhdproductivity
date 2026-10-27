package com.nowapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowapp.ServiceLocator
import com.nowapp.data.Backup
import com.nowapp.data.FocusSession
import com.nowapp.data.Prefs
import com.nowapp.data.db.AnchorEntity
import com.nowapp.domain.Breakdown
import com.nowapp.domain.BreakdownEngine
import com.nowapp.domain.Bucket
import com.nowapp.domain.Multiplier
import com.nowapp.domain.StreakState
import com.nowapp.domain.Task
import com.nowapp.domain.TimeMath
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * One ViewModel for a one-user app.
 *
 * It owns no UI state beyond transient messages: everything durable comes from
 * Room/DataStore flows, so the screens cannot drift out of sync with the database.
 */
class NowViewModel : ViewModel() {

    private val tasksRepo = ServiceLocator.tasks
    private val prefsRepo = ServiceLocator.prefs
    private val streakRepo = ServiceLocator.streak
    private val focusRepo = ServiceLocator.focus
    private val anchorRepo = ServiceLocator.anchors

    val tasks: StateFlow<List<Task>> =
        tasksRepo.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val prefs: StateFlow<Prefs> =
        prefsRepo.prefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Prefs())

    val streak: StateFlow<StreakState> =
        streakRepo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreakState())

    val sessions: StateFlow<List<FocusSession>> =
        focusRepo.sessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val anchors: StateFlow<List<AnchorEntity>> =
        anchorRepo.anchors.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Neutral, one-line messages produced by bucket swaps. Never errors. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    /** Set for a moment when a task is completed, to drive the celebration morph. */
    private val _justCompleted = MutableStateFlow<Long?>(null)
    val justCompleted: StateFlow<Long?> = _justCompleted.asStateFlow()

    fun capture(title: String, intoToday: Boolean = false) {
        if (title.isBlank()) return
        viewModelScope.launch {
            tasksRepo.capture(title, intoToday)
            val today = LocalDate.now().toString()
            prefsRepo.setLastActiveDay(today)
        }
    }

    fun complete(id: Long, actualMinutes: Int? = null) {
        viewModelScope.launch {
            tasksRepo.complete(id, actualMinutes)
            _justCompleted.value = id
            prefsRepo.setLastActiveDay(LocalDate.now().toString())
        }
    }

    fun clearJustCompleted() {
        _justCompleted.value = null
    }

    fun uncomplete(id: Long) {
        viewModelScope.launch { tasksRepo.uncomplete(id) }
    }

    fun move(id: Long, bucket: Bucket) {
        viewModelScope.launch {
            val message = tasksRepo.move(id, bucket)
            if (message != null) _messages.emit(message)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { tasksRepo.delete(id) }
    }

    fun rename(id: Long, title: String) {
        viewModelScope.launch { tasksRepo.rename(id, title) }
    }

    fun setEstimate(id: Long, minutes: Int?) {
        viewModelScope.launch { tasksRepo.setEstimate(id, minutes) }
    }

    fun setDue(id: Long, dueAt: Long?, hard: Boolean) {
        viewModelScope.launch { tasksRepo.setDue(id, dueAt, hard) }
    }

    fun breakdownFor(title: String): Breakdown = BreakdownEngine.forTask(title)

    fun applyBreakdown(id: Long) {
        viewModelScope.launch {
            val task = tasks.first().firstOrNull { it.id == id } ?: return@launch
            tasksRepo.applyBreakdown(id, BreakdownEngine.forTask(task.title))
            _messages.emit("First step is now the task. The rest is in its notes.")
        }
    }

    fun snooze(id: Long) {
        viewModelScope.launch {
            prefsRepo.snooze(id, System.currentTimeMillis() + 30 * 60 * 1000L)
        }
    }

    fun clearSnooze() {
        viewModelScope.launch { prefsRepo.clearSnooze() }
    }

    fun toggleQuietForToday() {
        viewModelScope.launch {
            val current = prefsRepo.prefs.first().quietForTodayKey
            val today = LocalDate.now().toString()
            prefsRepo.setQuietForToday(if (current == today) null else today)
        }
    }

    fun setCalmMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setCalmMode(enabled) }
    }

    fun setQuietHours(start: LocalTime, end: LocalTime) {
        viewModelScope.launch { prefsRepo.setQuietHours(start, end) }
    }

    fun setAwakeWindow(start: LocalTime, end: LocalTime) {
        viewModelScope.launch { prefsRepo.setAwakeWindow(start, end) }
    }

    fun setOnboarded() {
        viewModelScope.launch {
            prefsRepo.setOnboarded(true)
            prefsRepo.setNotificationAsked()
            prefsRepo.setLastActiveDay(LocalDate.now().toString())
        }
    }

    fun ackWelcomeBack() {
        viewModelScope.launch { prefsRepo.setWelcomeBackAck(LocalDate.now().toString()) }
    }

    fun markBreak() {
        viewModelScope.launch { prefsRepo.markBreak(System.currentTimeMillis()) }
    }

    fun recordFocus(taskId: Long?, plannedMinutes: Int, actualMinutes: Int, startedAt: Long, endedAt: Long) {
        viewModelScope.launch {
            focusRepo.record(taskId, plannedMinutes, actualMinutes, startedAt, endedAt)
            prefsRepo.markBreak(System.currentTimeMillis())
            prefsRepo.setLastActiveDay(LocalDate.now().toString())
        }
    }

    fun upsertAnchor(anchor: AnchorEntity) {
        viewModelScope.launch { anchorRepo.upsert(anchor) }
    }

    fun deleteAnchor(id: Long) {
        viewModelScope.launch { anchorRepo.delete(id) }
    }

    /** Serialises everything to a JSON string, ready to be written to a user-chosen file. */
    fun exportJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val tasks = ServiceLocator.database.taskDao().allOnce()
            val sessions = ServiceLocator.database.focusDao().allOnce()
            val streak = ServiceLocator.database.streakDao().get()
            val anchors = ServiceLocator.database.anchorDao().allOnce()
            onReady(Backup.encode(tasks, sessions, streak, anchors))
        }
    }

    fun importJson(json: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val snapshot = Backup.decode(json)
                tasksRepo.replaceAllRaw(snapshot.tasks, snapshot.sessions)
                snapshot.streak?.let { streakRepo.replace(com.nowapp.data.toDomain(it)) }
                if (snapshot.anchors.isNotEmpty()) anchorRepo.replaceAll(snapshot.anchors)
                snapshot.tasks.size
            }
            onResult(result)
        }
    }

    /** Personal calibration, derived from completed tasks. */
    fun multiplierFor(tasks: List<Task>): Double? = Multiplier.multiplier(tasksRepo.samples(tasks))

    fun bestMultiplierFor(tasks: List<Task>): Double? = Multiplier.bestMultiplier(tasksRepo.samples(tasks))

    /** Days since the app was last opened, for the welcome-back message. */
    fun daysAway(prefs: Prefs, today: LocalDate = LocalDate.now()): Long {
        val last = prefs.lastActiveDay?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return 0
        return java.time.temporal.ChronoUnit.DAYS.between(last, today)
    }

    val today: LocalDate get() = TimeMath.today()
}
