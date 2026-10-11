package app.studygalaxy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.studygalaxy.data.api.ApiModule
import app.studygalaxy.data.local.TokenStore
import app.studygalaxy.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

enum class Screen { Loading, Onboarding, AuthLanding, Signup, Login, NamePicker, Home, SubjectPicker, SessionSetup, Timer, Galaxy, Circles, Notes, NoteCanvas, Stats, Privacy }

data class AppState(
    val screen: Screen = Screen.Loading,
    val user: User? = null,
    val subjects: List<Planet> = emptyList(),
    val activeSubjectId: String? = null,
    val activeSessionId: String? = null,
    val activeSessionEndsAtMs: Long? = null,
    val activeTodos: List<app.studygalaxy.ui.screens.TimerTodo> = emptyList(),
    val editingNoteId: String? = null,
    val editingNoteTitle: String = "",
    val editingNoteStrokes: List<app.studygalaxy.data.model.StrokeDto> = emptyList(),
    val error: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state

    private val api get() = ApiModule.get(getApplication())
    private val ctx get() = getApplication<Application>()

    init { bootstrap() }

    private fun bootstrap() = viewModelScope.launch {
        val jwt = runCatching { TokenStore.jwtFlow(ctx).firstOrNull() }.getOrNull()
        if (jwt != null) {
            try {
                val me = api.me()
                _state.value = _state.value.copy(
                    user = me,
                    screen = if (nameClaimed()) Screen.Home else Screen.NamePicker
                )
                reloadSubjects()
                scheduleReminderFromProfile()
                return@launch
            } catch (_: Throwable) {
                TokenStore.setJwt(ctx, null)
            }
        }
        val onboarded = TokenStore.isOnboarded(ctx)
        _state.value = _state.value.copy(screen = if (onboarded) Screen.AuthLanding else Screen.Onboarding)
    }

    private fun reloadSubjects() = viewModelScope.launch {
        try {
            val subs = api.subjects()
            val currentActive = _state.value.activeSubjectId
            val keep = if (currentActive != null && subs.any { it.id == currentActive }) currentActive else subs.firstOrNull()?.id
            _state.value = _state.value.copy(subjects = subs, activeSubjectId = keep)
        } catch (_: Throwable) {}
    }

    private fun scheduleReminderFromProfile() = viewModelScope.launch {
        val json = TokenStore.getOnboardingJson(ctx) ?: return@launch
        val time = Regex("\"studyTime\"\\s*:\\s*\"(\\w+)\"").find(json)?.groupValues?.get(1)
        app.studygalaxy.notifications.StudyReminder.scheduleDaily(ctx, time)
    }

    fun pickSubject(id: String) {
        _state.value = _state.value.copy(activeSubjectId = id)
    }

    suspend fun createSubject(name: String, color: String): Planet? {
        return try {
            val created = api.createSubject(CreateSubjectReq(name, color))
            reloadSubjects().join()
            created
        } catch (_: Throwable) { null }
    }

    fun refreshSubjects() = reloadSubjects()

    fun saveOnboarding(answers: app.studygalaxy.ui.screens.OnboardingAnswers) = viewModelScope.launch {
        TokenStore.saveOnboarding(ctx, answers.toJson())
        _state.value = _state.value.copy(screen = Screen.AuthLanding)
    }

    /** Called after signup/login so profile can be stored on the user record */
    private fun syncPendingProfile() = viewModelScope.launch {
        val json = TokenStore.getOnboardingJson(ctx) ?: return@launch
        try {
            val req = parseProfileJson(json) ?: return@launch
            api.saveProfile(req)
        } catch (_: Throwable) { /* non-fatal */ }
    }

    private fun parseProfileJson(json: String): ProfileReq? = try {
        kotlinx.serialization.json.Json { ignoreUnknownKeys = true; coerceInputValues = true }
            .decodeFromString(ProfileReq.serializer(), json)
    } catch (_: Throwable) { null }

    private suspend fun nameClaimed(): Boolean =
        TokenStore.nameClaimedFlow(ctx).firstOrNull() ?: false

    fun go(screen: Screen) { _state.value = _state.value.copy(screen = screen) }

    fun signup(phone: String, password: String, onResult: (String?) -> Unit) = viewModelScope.launch {
        val device = TokenStore.getOrCreateDeviceId(ctx)
        try {
            val reply = api.signup(SignupReq(phone, password, device))
            TokenStore.setJwt(ctx, reply.jwt)
            _state.value = _state.value.copy(user = reply.user, screen = Screen.NamePicker)
            syncPendingProfile()
            reloadSubjects()
            scheduleReminderFromProfile()
            onResult(null)
        } catch (e: Throwable) {
            onResult(mapAuthError(e))
        }
    }

    fun login(phone: String, password: String, onResult: (String?) -> Unit) = viewModelScope.launch {
        val device = TokenStore.getOrCreateDeviceId(ctx)
        try {
            val reply = api.login(LoginReq(phone, password, device))
            TokenStore.setJwt(ctx, reply.jwt)
            TokenStore.markNameClaimed(ctx)
            _state.value = _state.value.copy(user = reply.user, screen = Screen.Home)
            syncPendingProfile()
            reloadSubjects()
            scheduleReminderFromProfile()
            onResult(null)
        } catch (_: Throwable) {
            onResult("Wrong phone or password")
        }
    }


    fun claimName(name: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        try {
            api.claimName(ClaimNameReq(name))
            TokenStore.markNameClaimed(ctx)
            val me = api.me()
            _state.value = _state.value.copy(user = me)
            onResult(true)
        } catch (_: Throwable) { onResult(false) }
    }

    fun startFocusWithTodos(plannedMin: Int, intention: String, todos: List<String>) = viewModelScope.launch {
        try {
            val subjectId = _state.value.activeSubjectId
            val reply = api.startSession(StartSessionWithTodosReq(
                subjectId = subjectId,
                plannedMin = plannedMin,
                intention = intention.ifBlank { null },
                todos = if (todos.isEmpty()) null else todos.map { TodoInput(it) }
            ))
            val timerTodos = reply.todoIds.zip(todos).map { (id, text) ->
                app.studygalaxy.ui.screens.TimerTodo(id = id, text = text, done = false)
            }
            _state.value = _state.value.copy(
                activeSessionId = reply.sessionId,
                activeSessionEndsAtMs = reply.serverStartTs + plannedMin * 60_000L,
                activeTodos = timerTodos,
                screen = Screen.Timer
            )
        } catch (_: Throwable) { /* ignore for MVP */ }
    }

    fun toggleTodo(id: String) {
        _state.value = _state.value.copy(
            activeTodos = _state.value.activeTodos.map {
                if (it.id == id) it.copy(done = !it.done) else it
            }
        )
    }

    suspend fun completeSession() {
        val id = _state.value.activeSessionId ?: return
        val done = _state.value.activeTodos.filter { it.done }.map { it.id }
        try {
            api.completeSession(CompleteSessionWithTodosReq(id, selfRating = null, doneTodoIds = done))
            val me = api.me()
            _state.value = _state.value.copy(
                user = me, activeSessionId = null, activeSessionEndsAtMs = null, activeTodos = emptyList()
            )
            reloadSubjects()
        } catch (_: Throwable) {}
    }

    suspend fun breakSession() {
        val id = _state.value.activeSessionId ?: return
        try { api.breakSession(BreakSessionReq(id)) } catch (_: Throwable) {}
        _state.value = _state.value.copy(
            activeSessionId = null, activeSessionEndsAtMs = null, activeTodos = emptyList()
        )
    }

    suspend fun loadNotes() = runCatching { api.notes() }.getOrDefault(emptyList())

    fun openNewNote() {
        _state.value = _state.value.copy(
            editingNoteId = null, editingNoteTitle = "", editingNoteStrokes = emptyList(),
            screen = Screen.NoteCanvas
        )
    }

    fun openExistingNote(note: app.studygalaxy.data.model.Note) {
        _state.value = _state.value.copy(
            editingNoteId = note.id, editingNoteTitle = note.title, editingNoteStrokes = note.strokes,
            screen = Screen.NoteCanvas
        )
    }

    suspend fun saveNote(title: String, strokes: List<app.studygalaxy.data.model.StrokeDto>): Boolean {
        val id = _state.value.editingNoteId
        val subjectId = _state.value.activeSubjectId
        return try {
            if (id == null) {
                api.createNote(CreateNoteReq(subjectId = subjectId, title = title, strokes = strokes))
            } else {
                api.updateNote(id, UpdateNoteReq(title = title, strokes = strokes))
            }
            true
        } catch (_: Throwable) { false }
    }

    suspend fun loadGalaxy(): Galaxy? = runCatching { api.galaxy() }.getOrNull()
    suspend fun loadCircles(): List<Circle> = runCatching { api.circles() }.getOrDefault(emptyList())
    suspend fun createCircle(name: String): Circle? = runCatching { api.createCircle(CreateCircleReq(name)) }.getOrNull()
    suspend fun joinCircle(code: String): String? = runCatching {
        api.joinCircle(JoinCircleReq(code)); null as String?
    }.getOrElse { "Invalid code" }
    suspend fun loadStats(): WeeklyStats? = runCatching { api.statsWeekly() }.getOrNull()

    private fun mapAuthError(t: Throwable): String {
        val m = t.message.orEmpty()
        return when {
            m.contains("409", true) || m.contains("phone_taken", true) -> "This phone number already has an account"
            m.contains("bad_phone", true) -> "Enter a valid phone number"
            else -> "Sign up failed. Try again."
        }
    }
}

