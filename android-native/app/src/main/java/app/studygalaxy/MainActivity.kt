package app.studygalaxy

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import app.studygalaxy.notifications.Nudges
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import app.studygalaxy.ui.components.GalaxyBackground
import app.studygalaxy.ui.screens.*
import app.studygalaxy.ui.theme.StudyGalaxyTheme
import app.studygalaxy.ui.theme.gradientText
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    /**
     * Android 13+ will silently drop every notification until the user grants
     * this. The permission was declared in the manifest but never requested,
     * so no reminder could ever appear on a modern phone.
     */
    private val notifPermission = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* granted or not, the app works either way */ }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onResume() {
        super.onResume()
        // Opening the app resets the 2.5-hour idle clock.
        Nudges.markActive(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleInviteIntent(intent)
        ensureNotificationPermission()
        Nudges.scheduleAll(this)
        setContent {
            StudyGalaxyTheme {
                Box(Modifier.fillMaxSize()) {
                    GalaxyBackground()
                    val state by vm.state.collectAsState()
                    AnimatedContent(
                        modifier = Modifier.safeDrawingPadding(),
                        targetState = state.screen,
                        // Kept short: a 350ms transition on every navigation is
                        // a third of a second of perceived lag before the
                        // screen is even usable.
                        transitionSpec = {
                            (fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 10 }) togetherWith
                                (fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 10 })
                        },
                        label = "screen"
                    ) { screen ->
                        when (screen) {
                            Screen.Loading -> LoadingView()
                            Screen.Onboarding -> OnboardingScreen(onDone = { vm.saveOnboarding(it) })
                            Screen.AuthLanding -> AuthLandingScreen(
                                onGuest = {
                                    suspendCancellableCoroutine<String?> { cont ->
                                        vm.continueAsGuest { err -> cont.resume(err) }
                                    }
                                }
                            )
                            Screen.NamePicker -> {
                                val u = state.user
                                if (u == null) LoadingView() else NamePickerScreen(
                                    initialName = u.cosmicName,
                                    avatarSeed = u.avatarSeed,
                                    onSave = { name ->
                                        suspendCancellableCoroutine<Boolean> { cont ->
                                            vm.claimName(name) { ok -> cont.resume(ok) }
                                        }
                                    },
                                    onDone = { vm.go(Screen.Home) }
                                )
                            }
                            Screen.Home -> {
                                val u = state.user
                                if (u == null) LoadingView() else HomeScreen(
                                    user = u,
                                    subjects = state.subjects,
                                    activeSubjectId = state.activeSubjectId,
                                    onPickSubject = { vm.pickSubject(it) },
                                    onCreateSubject = { vm.go(Screen.SubjectPicker) },
                                    onStartFocus = { vm.go(Screen.SessionSetup) },
                                    onOpenGalaxy = { vm.go(Screen.Galaxy) },
                                    onOpenCircles = { vm.go(Screen.Circles) },
                                    onOpenStats = { vm.go(Screen.Stats) },
                                    onOpenNotes = { vm.go(Screen.Notes) }
                                )
                            }
                            Screen.SubjectPicker -> SubjectPickerScreen(
                                subjects = state.subjects,
                                onPick = { id -> vm.pickSubject(id); vm.go(Screen.Home) },
                                onCreate = { name, color -> vm.createSubject(name, color) },
                                onBack = { vm.go(Screen.Home) }
                            )
                            Screen.SessionSetup -> {
                                val activeSubj = state.subjects.firstOrNull { it.id == state.activeSubjectId }
                                SessionSetupScreen(
                                    subject = activeSubj,
                                    onPickSubject = { vm.go(Screen.SubjectPicker) },
                                    onBack = { vm.go(Screen.Home) },
                                    onStart = { min, intention, todos ->
                                        vm.startFocusWithTodos(min, intention, todos)
                                    }
                                )
                            }
                            Screen.Timer -> {
                                val endsAt = state.activeSessionEndsAtMs
                                if (endsAt == null) {
                                    LoadingView()
                                    LaunchedEffect(Unit) { vm.go(Screen.Home) }
                                } else TimerScreen(
                                    endsAtMs = endsAt,
                                    initialTodos = state.activeTodos,
                                    onToggleTodo = { vm.toggleTodo(it) },
                                    onComplete = { vm.completeSession() },
                                    onBreak = { _ -> vm.breakSession() },
                                    onDone = { vm.go(if (vm.state.value.user != null) Screen.Galaxy else Screen.Home) }
                                )
                            }
                            Screen.Notes -> NotesListScreen(
                                load = { vm.loadNotes() },
                                onOpen = { vm.openExistingNote(it) },
                                onCreate = { vm.openNewNote() },
                                onBack = { vm.go(Screen.Home) }
                            )
                            Screen.NoteCanvas -> NoteCanvasScreen(
                                initialStrokes = state.editingNoteStrokes,
                                initialTitle = state.editingNoteTitle,
                                onSave = { title, strokes -> vm.saveNote(title, strokes) },
                                onBack = { vm.go(Screen.Notes) }
                            )
                            Screen.Galaxy -> GalaxyScreen(
                                loadGalaxy = { vm.loadGalaxy() },
                                onBack = { vm.go(Screen.Home) }
                            )
                            Screen.Circles -> CirclesScreen(
                                load = { vm.loadCircles() },
                                create = { vm.createCircle(it) },
                                join = { vm.joinCircle(it) },
                                openSocket = { id, cb -> vm.openCircleSocket(id, cb) },
                                onBack = { vm.go(Screen.Home) }
                            )
                            Screen.Stats -> StatsScreen(
                                load = { vm.loadStats() },
                                onBack = { vm.go(Screen.Home) }
                            )
                            Screen.Privacy -> PrivacyScreen(onBack = { vm.go(Screen.Home) })
                            else -> LoadingView()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleInviteIntent(intent)
    }

    /** Pulls the invite code out of /join/<CODE> or studygalaxy://join/<CODE>. */
    private fun handleInviteIntent(intent: android.content.Intent?) {
        val data = intent?.data ?: return
        val code = data.pathSegments?.lastOrNull()?.takeIf { it.isNotBlank() } ?: return
        if (data.path?.contains("join") == true || data.host == "join") {
            vm.handleInviteCode(code)
        }
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "StudyGalaxy",
            style = androidx.compose.material3.MaterialTheme.typography.displayMedium.copy(brush = gradientText()),
            color = Color.Unspecified,
            fontSize = 44.sp
        )
    }
}
