package app.studygalaxy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.ui.components.rememberNarrator
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Voice-led setup. Vega — the guide — introduces itself, asks six questions,
 * then shows the student their own numbers before the app is ever used.
 *
 * The persona name and opening line are deliberately not the category-standard
 * "I'll help you stay focused on your studies": that sentence appears in half
 * the focus apps on the store. Vega opens by refusing to motivate, which sets
 * the same tone as the rest of the product.
 */

/** The guide's name. One word, a real star, easy to say in any accent. */
const val GUIDE_NAME = "Vega"

data class OnboardingAnswers(
    val examGoal: String? = null,
    val dailyHours: Int? = null,
    val topSubjects: List<String> = emptyList(),
    val studyTime: String? = null,
    val biggestDistraction: String? = null,
    // Newer answers: kept locally and used for the reveal. The server's
    // profile schema ignores unknown keys, so these cost nothing to send.
    val studyFrequency: String? = null,
    val phoneProblem: String? = null,
    val screenHours: Float? = null
) {
    fun toJson(): String {
        val subs = topSubjects.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
        return """{"examGoal":${q(examGoal)},"dailyHours":${dailyHours ?: "null"},""" +
            """"topSubjects":[$subs],"studyTime":${q(studyTime)},""" +
            """"biggestDistraction":${q(biggestDistraction)},"studyFrequency":${q(studyFrequency)},""" +
            """"phoneProblem":${q(phoneProblem)},"screenHours":${screenHours ?: "null"}}"""
    }
    private fun q(s: String?) = if (s == null) "null" else "\"$s\""
}

private data class Opt(val id: String, val label: String, val emoji: String = "", val payload: Float = 0f)

private const val TOTAL_QUESTIONS = 7

@Composable
fun OnboardingScreen(onDone: (OnboardingAnswers) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val narrator = rememberNarrator()
    var step by remember { mutableIntStateOf(0) }
    var a by remember { mutableStateOf(OnboardingAnswers()) }

    val tap: () -> Unit = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    // step 0 = intro, 1..7 = questions, 8 = reveal
    val progress by animateFloatAsState(
        targetValue = (step.coerceIn(0, TOTAL_QUESTIONS + 1)) / (TOTAL_QUESTIONS + 1f),
        animationSpec = tween(450, easing = EaseOutCubic),
        label = "progress"
    )

    fun advance() { tap(); step++ }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(White5)) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(progress)
                    .background(Brush.horizontalGradient(listOf(Violet500, Fuchsia500, Amber300)))
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step in 1..TOTAL_QUESTIONS) {
                TextButton(onClick = { tap(); narrator.stop(); step-- }, contentPadding = PaddingValues(0.dp)) {
                    Text("← Back", color = White70, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            // Muting matters: plenty of students open an app in a classroom.
            TextButton(
                onClick = {
                    narrator.muted = !narrator.muted
                    if (narrator.muted) narrator.stop()
                },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(if (narrator.muted) "🔇 Voice off" else "🔊 Voice on", color = White50, fontSize = 11.sp)
            }
            if (step in 1..TOTAL_QUESTIONS) {
                Text("$step / $TOTAL_QUESTIONS", color = White50, fontSize = 10.sp, letterSpacing = 2.sp)
            }
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 8 }) togetherWith
                    fadeOut(tween(180))
            },
            label = "onboarding",
            modifier = Modifier.weight(1f)
        ) { s ->
            when (s) {
                0 -> IntroStep(
                    speaking = narrator.speaking,
                    onSpeak = { narrator.speak(it) },
                    onBegin = { advance() }
                )

                1 -> QuestionStep(
                    narrator = { narrator.speak(it) },
                    spoken = "First — is something riding on this? Are you preparing for an exam?",
                    title = "Is something riding on this?",
                    subtitle = "Be honest. It changes how hard I push.",
                    options = listOf(
                        Opt("yes", "Yes, I have an exam coming", "🎯"),
                        Opt("someday", "Not yet, but one is coming", "🌱"),
                        Opt("no", "No, I just want to focus better", "✨"),
                    ),
                    onPick = { o ->
                        a = if (o.id == "no") a.copy(examGoal = "focus") else a
                        // Skip the "which exam" question when there is none.
                        step = if (o.id == "no") 3 else 2
                        tap()
                    }
                )

                2 -> QuestionStep(
                    narrator = { narrator.speak(it) },
                    spoken = "Which one are you preparing for?",
                    title = "Which exam?",
                    subtitle = "Your galaxy gets shaped around it.",
                    options = listOf(
                        Opt("jee", "JEE — Engineering", "⚡"),
                        Opt("neet", "NEET — Medical", "🩺"),
                        Opt("boards", "School Boards", "📚"),
                        Opt("upsc", "UPSC", "🏛"),
                        Opt("gate", "GATE", "🎓"),
                        Opt("cat", "CAT — MBA", "📈"),
                        Opt("college", "College / Semesters", "🏫"),
                    ),
                    onPick = { o -> a = a.copy(examGoal = o.id); advance() }
                )

                3 -> QuestionStep(
                    narrator = { narrator.speak(it) },
                    spoken = "How often do you actually sit down and study? Not how often you plan to.",
                    title = "How often do you actually study?",
                    subtitle = "Not how often you plan to. How often you do.",
                    options = listOf(
                        Opt("rarely", "Rarely — I keep putting it off", "🌑", 1f),
                        Opt("sometimes", "Sometimes, when I feel like it", "🌘", 2f),
                        Opt("most_days", "Most days", "🌗", 3f),
                        Opt("daily", "Every single day", "🌕", 4f),
                    ),
                    onPick = { o ->
                        a = a.copy(studyFrequency = o.id, dailyHours = o.payload.toInt())
                        advance()
                    }
                )

                4 -> QuestionStep(
                    narrator = { narrator.speak(it) },
                    spoken = "What pulls you away the most?",
                    title = "What pulls you away?",
                    subtitle = "The one that wins most often.",
                    options = listOf(
                        Opt("social", "Instagram, Snapchat, reels", "📱"),
                        Opt("phone", "YouTube rabbit holes", "▶️"),
                        Opt("friends", "Chats and group messages", "💬"),
                        Opt("boredom", "Games", "🎮"),
                        Opt("noise", "Noise around me", "🔊"),
                        Opt("sleep", "Tiredness and sleep", "😴"),
                    ),
                    onPick = { o -> a = a.copy(biggestDistraction = o.id); advance() }
                )

                5 -> QuestionStep(
                    narrator = { narrator.speak(it) },
                    spoken = "And what does your phone actually cost you?",
                    title = "What does your phone cost you?",
                    subtitle = "Pick the one that stings.",
                    options = listOf(
                        Opt("cant_start", "I can never get started", "🪨"),
                        Opt("cant_stop", "I pick it up and lose an hour", "🕳"),
                        Opt("guilt", "I study with it beside me and absorb nothing", "🌫"),
                        Opt("sleep_late", "It steals my sleep", "🌙"),
                    ),
                    onPick = { o -> a = a.copy(phoneProblem = o.id); advance() }
                )

                6 -> QuestionStep(
                    narrator = { narrator.speak(it) },
                    spoken = "Last real question. Check your screen time, then tell me the truth.",
                    title = "Your daily screen time?",
                    subtitle = "Settings → Digital Wellbeing. I'll wait. Guessing low only cheats you.",
                    options = listOf(
                        Opt("lt2", "Under 2 hours", "🟢", 1.5f),
                        Opt("2_4", "2 to 4 hours", "🟡", 3f),
                        Opt("4_6", "4 to 6 hours", "🟠", 5f),
                        Opt("6_8", "6 to 8 hours", "🔴", 7f),
                        Opt("gt8", "More than 8 hours", "⚫", 9f),
                    ),
                    onPick = { o -> a = a.copy(screenHours = o.payload); advance() }
                )

                7 -> QuestionStep(
                    narrator = { narrator.speak(it) },
                    spoken = "When does your head work best?",
                    title = "When does your head work best?",
                    subtitle = "I'll nudge you then, and leave you alone otherwise.",
                    options = listOf(
                        Opt("morning", "Early morning", "🌅"),
                        Opt("afternoon", "Afternoon", "☀️"),
                        Opt("evening", "Evening", "🌆"),
                        Opt("night", "Late night", "🌃"),
                    ),
                    onPick = { o -> a = a.copy(studyTime = o.id); advance() }
                )

                else -> WhereYouStandScreen(
                    screenHoursPerDay = a.screenHours ?: 4f,
                    examLabel = examPhrase(a.examGoal),
                    distractionLabel = distractionPhrase(a.biggestDistraction),
                    onSpeak = { narrator.speak(it) },
                    onContinue = { tap(); narrator.stop(); onDone(a) }
                )
            }
        }
    }
}

private fun examPhrase(id: String?): String? = when (id) {
    "jee" -> "JEE"; "neet" -> "NEET"; "boards" -> "your boards"
    "upsc" -> "UPSC"; "gate" -> "GATE"; "cat" -> "CAT"
    "college" -> "this semester"; else -> null
}

private fun distractionPhrase(id: String?): String? = when (id) {
    "social" -> "the reels"; "phone" -> "the next video"
    "friends" -> "the group chats"; "boredom" -> "the games"
    "noise" -> "the noise"; "sleep" -> "the tiredness"; else -> null
}

/* ------------------------------ Intro ------------------------------ */

@Composable
private fun IntroStep(
    speaking: Boolean,
    onSpeak: (String) -> Unit,
    onBegin: () -> Unit
) {
    // Written to sound like nothing else in the category: it opens by refusing
    // the usual promise rather than making it.
    val line1 = "Hi. I'm $GUIDE_NAME."
    val line2 = "I'm not here to motivate you. Motivation runs out by Thursday."
    val line3 = "I'm here to show you exactly where your time goes — and then help you take it back, one hour at a time."
    val line4 = "Six questions. Answer them honestly and I'll show you something about your year."

    var shown by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400)
        onSpeak("$line1 $line2")
        shown = 1; delay(1500)
        shown = 2; delay(2900)
        onSpeak(line3)
        shown = 3; delay(3800)
        onSpeak(line4)
        shown = 4; delay(900)
        ready = true
    }

    val pulse = rememberInfiniteTransition(label = "orb")
    val breathe by pulse.animateFloat(
        1f, 1.06f,
        infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "breathe"
    )
    // The orb swells while the voice is talking, so it reads as the speaker.
    val voiceScale by animateFloatAsState(
        if (speaking) 1.12f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "voice"
    )

    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(150.dp).scale(breathe * voiceScale)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Violet500, Fuchsia500, Indigo700))
                    )
            )
            // Ripple rings while speaking
            if (speaking) {
                Box(
                    Modifier.size(190.dp).scale(voiceScale)
                        .clip(CircleShape)
                        .border(1.dp, Violet300.copy(alpha = 0.35f), CircleShape)
                )
            }
        }

        Spacer(Modifier.height(34.dp))

        // Captions, so the script lands whether or not the voice is audible.
        Caption(shown >= 1, line1, big = true)
        Caption(shown >= 2, line2)
        Caption(shown >= 3, line3)
        Caption(shown >= 4, line4)

        Spacer(Modifier.height(30.dp))

        AnimatedVisibility(ready, enter = fadeIn(tween(500))) {
            Button(
                onClick = onBegin,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(listOf(Violet500, Fuchsia500)),
                        RoundedCornerShape(16.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) { Text("I'm listening", color = White90, fontSize = 16.sp) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Caption(visible: Boolean, text: String, big: Boolean = false) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(tween(550)) + slideInVertically(tween(550)) { it / 5 }
    ) {
        Text(
            text,
            color = if (big) Color.White else White70,
            fontSize = if (big) 28.sp else 15.sp,
            fontWeight = if (big) FontWeight.Black else FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = if (big) 34.sp else 22.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )
    }
}

/* ---------------------------- Question ---------------------------- */

@Composable
private fun QuestionStep(
    narrator: (String) -> Unit,
    spoken: String,
    title: String,
    subtitle: String,
    options: List<Opt>,
    onPick: (Opt) -> Unit
) {
    LaunchedEffect(title) {
        delay(280)
        narrator(spoken)
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.displaySmall.copy(brush = gradientText()),
            lineHeight = 38.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(subtitle, color = White70, fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(22.dp))

        options.forEach { o ->
            var pressed by remember(o.id) { mutableStateOf(false) }
            val scale by animateFloatAsState(
                if (pressed) 0.97f else 1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "press"
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(White5, RoundedCornerShape(16.dp))
                    .border(1.dp, White10, RoundedCornerShape(16.dp))
                    .clickable { pressed = true; onPick(o) }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (o.emoji.isNotEmpty()) {
                    Text(o.emoji, fontSize = 19.sp)
                    Spacer(Modifier.width(14.dp))
                }
                Text(o.label, color = White90, fontSize = 15.sp, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
