package app.studygalaxy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudyGalaxyTheme {
                Box(Modifier.fillMaxSize()) {
                    GalaxyBackground()
                    val state by vm.state.collectAsState()
                    AnimatedContent(
                        targetState = state.screen,
                        transitionSpec = {
                            (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 6 }) togetherWith
                                (fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it / 6 })
                        },
                        label = "screen"
                    ) { screen ->
                        when (screen) {
                            Screen.Loading -> LoadingView()
                            Screen.Onboarding -> OnboardingScreen(onDone = { vm.saveOnboarding(it) })
                            Screen.AuthLanding -> AuthLandingScreen(
                                onCreateAccount = { vm.go(Screen.Signup) },
                                onLogin = { vm.go(Screen.Login) },
                                onGuest = { vm.guest() }
                            )
                            Screen.Signup -> SignupScreen(
                                onSignup = { phone, pw ->
                                    suspendCancellableCoroutine<String?> { cont ->
                                        vm.signup(phone, pw) { err -> cont.resume(err) }
                                    }
                                },
                                onDone = { vm.go(Screen.NamePicker) },
                                onBack = { vm.go(Screen.AuthLanding) },
                                onLoginInstead = { vm.go(Screen.Login) }
                            )
                            Screen.Login -> LoginScreen(
                                onLogin = { phone, pw ->
                                    suspendCancellableCoroutine<String?> { cont ->
                                        vm.login(phone, pw) { err -> cont.resume(err) }
                                    }
                                },
                                onDone = { vm.go(Screen.Home) },
                                onBack = { vm.go(Screen.AuthLanding) }
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
                                    onStartFocus = { min, intention -> vm.startFocus(min, intention) },
                                    onOpenGalaxy = { vm.go(Screen.Galaxy) },
                                    onOpenCircles = { vm.go(Screen.Circles) },
                                    onOpenStats = { vm.go(Screen.Stats) },
                                    onOpenPrivacy = { vm.go(Screen.Privacy) }
                                )
                            }
                            Screen.Timer -> {
                                val endsAt = state.activeSessionEndsAtMs
                                if (endsAt == null) {
                                    LoadingView()
                                    LaunchedEffect(Unit) { vm.go(Screen.Home) }
                                } else TimerScreen(
                                    endsAtMs = endsAt,
                                    onComplete = { vm.completeSession() },
                                    onBreak = { _ -> vm.breakSession() },
                                    onDone = { vm.go(if (vm.state.value.user != null) Screen.Galaxy else Screen.Home) }
                                )
                            }
                            Screen.Galaxy -> GalaxyScreen(
                                loadGalaxy = { vm.loadGalaxy() },
                                onBack = { vm.go(Screen.Home) }
                            )
                            Screen.Circles -> CirclesScreen(
                                load = { vm.loadCircles() },
                                create = { vm.createCircle(it) },
                                join = { vm.joinCircle(it) },
                                onBack = { vm.go(Screen.Home) }
                            )
                            Screen.Stats -> StatsScreen(
                                load = { vm.loadStats() },
                                onBack = { vm.go(Screen.Home) }
                            )
                            Screen.Privacy -> PrivacyScreen(onBack = { vm.go(Screen.Home) })
                        }
                    }
                }
            }
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
