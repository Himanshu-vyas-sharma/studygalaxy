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

enum class Screen { Loading, Onboarding, AuthLanding, Signup, Login, NamePicker, Home, Timer, Galaxy, Circles, Stats, Privacy }

data class AppState(
    val screen: Screen = Screen.Loading,
    val user: User? = null,
    val activeSessionId: String? = null,
    val activeSessionEndsAtMs: Long? = null,
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
                _state.value = _state.value.copy(user = me, screen = if (nameClaimed()) Screen.Home else Screen.NamePicker)
                return@launch
            } catch (_: Throwable) {
                TokenStore.setJwt(ctx, null)
            }
        }
        val onboarded = TokenStore.isOnboarded(ctx)
        _state.value = _state.value.copy(screen = if (onboarded) Screen.AuthLanding else Screen.Onboarding)
    }

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
            onResult(null)
        } catch (_: Throwable) {
            onResult("Wrong phone or password")
        }
    }

    fun guest() = viewModelScope.launch {
        val device = TokenStore.getOrCreateDeviceId(ctx)
        try {
            val reply = api.anonymous(AnonymousReq(device))
            TokenStore.setJwt(ctx, reply.jwt)
            _state.value = _state.value.copy(user = reply.user, screen = Screen.NamePicker)
            syncPendingProfile()
        } catch (_: Throwable) {
            _state.value = _state.value.copy(screen = Screen.AuthLanding)
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

    fun startFocus(plannedMin: Int, intention: String) = viewModelScope.launch {
        try {
            val reply = api.startSession(StartSessionReq(null, plannedMin, intention.ifBlank { null }))
            _state.value = _state.value.copy(
                activeSessionId = reply.sessionId,
                activeSessionEndsAtMs = reply.serverStartTs + plannedMin * 60_000L,
                screen = Screen.Timer
            )
        } catch (_: Throwable) { /* ignore for MVP */ }
    }

    suspend fun completeSession() {
        val id = _state.value.activeSessionId ?: return
        try {
            api.completeSession(CompleteSessionReq(id, 4))
            val me = api.me()
            _state.value = _state.value.copy(user = me, activeSessionId = null, activeSessionEndsAtMs = null)
        } catch (_: Throwable) {}
    }

    suspend fun breakSession() {
        val id = _state.value.activeSessionId ?: return
        try { api.breakSession(BreakSessionReq(id)) } catch (_: Throwable) {}
        _state.value = _state.value.copy(activeSessionId = null, activeSessionEndsAtMs = null)
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

