package app.studygalaxy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.studygalaxy.data.api.ApiModule
import app.studygalaxy.data.api.CircleSocket
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
                prefetchScreens()
                consumePendingInvite()
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

    /**
     * Zero-friction entry: no phone, no email, no password. The device gets an
     * anonymous account it can attach a phone to later if it ever wants one.
     */
    fun continueAsGuest(onResult: (String?) -> Unit) = viewModelScope.launch {
        val device = TokenStore.getOrCreateDeviceId(ctx)
        try {
            val reply = api.anonymous(AnonymousReq(device))
            TokenStore.setJwt(ctx, reply.jwt)
            _state.value = _state.value.copy(user = reply.user, screen = Screen.NamePicker)
            syncPendingProfile()
            reloadSubjects()
            scheduleReminderFromProfile()
            prefetchScreens()
            consumePendingInvite()
            onResult(null)
        } catch (e: Throwable) {
            onResult(mapAuthError(e, signingUp = true))
        }
    }

    /**
     * An invite link was tapped. Remember the code; once the user has an
     * account (guest counts) we join the circle and drop them on Circles.
     */
    fun handleInviteCode(code: String) = viewModelScope.launch {
        val clean = code.uppercase().filter { it.isLetterOrDigit() }.take(10)
        if (clean.isEmpty()) return@launch
        pendingInvite = clean
        if (_state.value.user != null) consumePendingInvite()
    }

    private var pendingInvite: String? = null

    private fun consumePendingInvite() = viewModelScope.launch {
        val code = pendingInvite ?: return@launch
        pendingInvite = null
        val err = joinCircle(code)
        _state.value = _state.value.copy(
            screen = Screen.Circles,
            error = err?.let { "Couldn't join with code $code" }
        )
    }

    /** Opens a live presence socket for a circle. The caller closes it. */
    fun openCircleSocket(circleId: String, onReady: (CircleSocket?) -> Unit) = viewModelScope.launch {
        val token = TokenStore.getJwt(ctx)
        if (token == null) { onReady(null); return@launch }
        onReady(CircleSocket(viewModelScope, circleId, token).also { it.connect() })
    }

    suspend fun loadPresence(circleId: String): List<FocusingMember> =
        runCatching { api.circlePresence(circleId).focusing }.getOrDefault(emptyList())

    fun signup(phone: String, password: String, onResult: (String?) -> Unit) = viewModelScope.launch {
        val device = TokenStore.getOrCreateDeviceId(ctx)
        try {
            val reply = api.signup(SignupReq(normalizePhone(phone), password, device))
            TokenStore.setJwt(ctx, reply.jwt)
            _state.value = _state.value.copy(user = reply.user, screen = Screen.NamePicker)
            syncPendingProfile()
            reloadSubjects()
            scheduleReminderFromProfile()
            prefetchScreens()
            consumePendingInvite()
            onResult(null)
        } catch (e: Throwable) {
            onResult(mapAuthError(e, signingUp = true))
        }
    }

    fun login(phone: String, password: String, onResult: (String?) -> Unit) = viewModelScope.launch {
        val device = TokenStore.getOrCreateDeviceId(ctx)
        try {
            val reply = api.login(LoginReq(normalizePhone(phone), password, device))
            TokenStore.setJwt(ctx, reply.jwt)
            TokenStore.markNameClaimed(ctx)
            _state.value = _state.value.copy(user = reply.user, screen = Screen.Home)
            syncPendingProfile()
            reloadSubjects()
            scheduleReminderFromProfile()
            prefetchScreens()
            consumePendingInvite()
            onResult(null)
        } catch (e: Throwable) {
            onResult(mapAuthError(e, signingUp = false))
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
            invalidateCaches()
            // Finishing a session resets the idle clock — no nagging after work done.
            app.studygalaxy.notifications.Nudges.markActive(ctx)
        } catch (_: Throwable) {}
    }

    suspend fun breakSession() {
        val id = _state.value.activeSessionId ?: return
        try { api.breakSession(BreakSessionReq(id)) } catch (_: Throwable) {}
        invalidateCaches() // a black hole is new galaxy state
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

    // ---- Screen data caches ----
    // Every screen used to open on a cold network call. Against a free-tier
    // host roughly a second away, that is a second of "Loading…" every single
    // time you open Galaxy. Cached data is returned instantly and refreshed in
    // the background; prefetchScreens() fills the cache right after sign-in so
    // even the first visit is immediate.
    private var galaxyCache: Galaxy? = null
    private var circlesCache: List<Circle>? = null
    private var statsCache: WeeklyStats? = null

    /** Warms the caches so the first open of each screen paints instantly. */
    private fun prefetchScreens() = viewModelScope.launch {
        runCatching { api.galaxy() }.getOrNull()?.let { galaxyCache = it }
        runCatching { api.circles() }.getOrNull()?.let { circlesCache = it }
        runCatching { api.statsWeekly() }.getOrNull()?.let { statsCache = it }
    }

    suspend fun loadGalaxy(): Galaxy? {
        galaxyCache?.let { cached ->
            viewModelScope.launch { runCatching { api.galaxy() }.getOrNull()?.let { galaxyCache = it } }
            return cached
        }
        return runCatching { api.galaxy() }.getOrNull()?.also { galaxyCache = it }
    }

    suspend fun loadCircles(): List<Circle> {
        circlesCache?.let { cached ->
            viewModelScope.launch { runCatching { api.circles() }.getOrNull()?.let { circlesCache = it } }
            return cached
        }
        return runCatching { api.circles() }.getOrDefault(emptyList()).also { circlesCache = it }
    }
    suspend fun createCircle(name: String): Circle? = runCatching { api.createCircle(CreateCircleReq(name)) }.getOrNull()
    suspend fun joinCircle(code: String): String? = runCatching {
        api.joinCircle(JoinCircleReq(code)); null as String?
    }.getOrElse { "Invalid code" }
    suspend fun loadStats(): WeeklyStats? {
        statsCache?.let { cached ->
            viewModelScope.launch { runCatching { api.statsWeekly() }.getOrNull()?.let { statsCache = it } }
            return cached
        }
        return runCatching { api.statsWeekly() }.getOrNull()?.also { statsCache = it }
    }

    /** A finished session changes the galaxy and the stats, so drop the stale copies. */
    private fun invalidateCaches() {
        galaxyCache = null
        statsCache = null
    }

    /**
     * The API stores phones as E.164 (+<dial><10 digits>) and rejects anything else.
     * A bare 10-digit number is assumed to be Indian; a number the user typed with
     * its own "+<code>" is passed through untouched.
     */
    private fun normalizePhone(raw: String): String {
        val s = raw.replace(Regex("[\\s\\-()]"), "")
        return if (s.startsWith("+")) s else "+$DEFAULT_DIAL$s"
    }

    /**
     * Retrofit's HttpException message is only "HTTP 400 Bad Request" — the server's
     * error code lives in the response body, so read that instead of the message.
     */
    private fun mapAuthError(t: Throwable, signingUp: Boolean): String {
        val http = t as? retrofit2.HttpException
        val code = http?.code()
        val body = runCatching { http?.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        return when {
            t is java.io.IOException ->
                "Can't reach StudyGalaxy. Check your internet and try again."
            code == 409 || body.contains("phone_taken", true) ->
                "This number already has an account. Log in instead."
            body.contains("bad_phone", true) ->
                "Enter a valid 10-digit phone number."
            body.contains("bad_body", true) ->
                "Check your number and use a password of at least 8 characters."
            code == 401 -> "Wrong phone number or password."
            code == 429 -> "Too many attempts. Wait a few minutes and try again."
            code != null && code >= 500 -> "StudyGalaxy's server hiccuped. Try again in a moment."
            else -> if (signingUp) "Sign up failed. Try again." else "Login failed. Try again."
        }
    }

    private companion object { const val DEFAULT_DIAL = "91" }
}

