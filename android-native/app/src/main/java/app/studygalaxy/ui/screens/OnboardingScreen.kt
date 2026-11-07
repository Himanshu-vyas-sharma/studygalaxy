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
import app.studygalaxy.ui.components.rememberSound
import kotlinx.coroutines.CoroutineScope
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Voice-led setup. Galaxy — the guide — introduces itself, asks six questions,
 * then shows the student their own numbers before the app is ever used.
 *
 * The persona name and opening line are deliberately not the category-standard
 * "I'll help you stay focused on your studies": that sentence appears in half
 * the focus apps on the store. Galaxy opens by refusing to motivate, which sets
 * the same tone as the rest of the product.
 */

/** The guide introduces itself as the app. */
const val GUIDE_NAME = "Galaxy"

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
    val screenHours: Float? = null,
    val examWhen: String? = null,
    val focusTolerance: Int? = null,
    val studyingWith: String? = null
) {
    fun toJson(): String {
        val subs = topSubjects.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
        return """{"examGoal":${q(examGoal)},"dailyHours":${dailyHours ?: "null"},""" +
            """"topSubjects":[$subs],"studyTime":${q(studyTime)},""" +
            """"biggestDistraction":${q(biggestDistraction)},"studyFrequency":${q(studyFrequency)},""" +
            """"phoneProblem":${q(phoneProblem)},"screenHours":${screenHours ?: "null"},""" +
            """"examWhen":${q(examWhen)},"focusTolerance":${focusTolerance ?: "null"},""" +
            """"studyingWith":${q(studyingWith)}}"""
    }
    private fun q(s: String?) = if (s == null) "null" else "\"$s\""
}

private data class Opt(val id: String, val label: String, val emoji: String = "", val payload: Float = 0f)

private const val TOTAL_QUESTIONS = 10

@Composable
fun OnboardingScreen(onDone: (OnboardingAnswers) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val sound = rememberSound()
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { sound.startPad(scope) }
    var a by remember { mutableStateOf(OnboardingAnswers()) }

    val tap: () -> Unit = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    // step 0 = intro, 1..7 = questions, 8 = reveal
    val progress by animateFloatAsState(
        targetValue = (step.coerceIn(0, TOTAL_QUESTIONS + 1)) / (TOTAL_QUESTIONS + 1f),
        animationSpec = tween(450, easing = EaseOutCubic),
        label = "progress"
    )

    fun advance() { tap(); sound.chime(scope, step); step++ }

    Column(Modifier.fillMaxSize()) {
        ConstellationProgress(
            answered = (step - 1).coerceIn(0, TOTAL_QUESTIONS),
            total = TOTAL_QUESTIONS,
            progress = progress
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step in 1..TOTAL_QUESTIONS) {
                TextButton(onClick = { tap(); step-- }, contentPadding = PaddingValues(0.dp)) {
                    Text("← Back", color = White70, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            // Muting matters: plenty of students open an app in a classroom.
            TextButton(
                onClick = {
                    sound.muted = !sound.muted
                    if (!sound.muted) sound.startPad(scope)
                },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(if (sound.muted) "🔇 Music off" else "🔊 Music on", color = White50, fontSize = 11.sp)
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
                    onBeat = { sound.chime(scope, it) },
                    onBegin = { advance() }
                )

                1 -> QuestionStep(
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
                        sound.chime(scope, step)
                        step = if (o.id == "no") 4 else 2
                        tap()
                    }
                )

                2 -> QuestionStep(
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
                    title = "How close is it?",
                    subtitle = "This decides how hard I push you.",
                    options = listOf(
                        Opt("weeks", "Weeks away", "🔥"),
                        Opt("months_3", "Two or three months", "⏳"),
                        Opt("months_6", "About six months", "🗓"),
                        Opt("year_plus", "A year or more", "🌱"),
                    ),
                    onPick = { o -> a = a.copy(examWhen = o.id); advance() }
                )

                4 -> QuestionStep(
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

                5 -> QuestionStep(
                    title = "How long before you reach for your phone?",
                    subtitle = "Answer small. Your first session gets built from this, and I want you to win it.",
                    options = listOf(
                        Opt("t5", "About 5 minutes", "🪫", 5f),
                        Opt("t15", "Around 15 minutes", "🔋", 15f),
                        Opt("t30", "Half an hour", "⚡", 30f),
                        Opt("t60", "An hour or more", "🚀", 60f),
                    ),
                    onPick = { o -> a = a.copy(focusTolerance = o.payload.toInt()); advance() }
                )

                6 -> QuestionStep(
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

                7 -> QuestionStep(
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

                8 -> QuestionStep(
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

                9 -> QuestionStep(
                    title = "Who else is doing this with you?",
                    subtitle = "Studying where someone can see you changes everything.",
                    options = listOf(
                        Opt("alone", "Nobody. I'm on my own", "🌑"),
                        Opt("friends_preparing", "Friends preparing for the same thing", "🤝"),
                        Opt("classmates", "Classmates, but we don't study together", "🏫"),
                        Opt("secret", "Nobody knows I'm trying", "🤫"),
                    ),
                    onPick = { o -> a = a.copy(studyingWith = o.id); advance() }
                )

                10 -> QuestionStep(
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
                    onSwell = { sound.swell(scope) },
                    onContinue = { tap(); sound.stopPad(); onDone(a) }
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

/**
 * The opening, carried by typography and score rather than a voice.
 *
 * Each line lands word by word on a rising bell, so the rhythm of reading it
 * is the rhythm of the music. A synthetic voice would undercut this; silence
 * with good timing reads as confidence.
 */
@Composable
private fun IntroStep(
    onBeat: (Int) -> Unit,
    onBegin: () -> Unit
) {
    val lines = listOf(
        "Hi. I'm Galaxy." to true,
        "I'm not here to motivate you." to false,
        "Motivation runs out by Thursday." to false,
        "I'm here to show you where your time actually goes — and help you take it back." to false,
        "Ten questions. Answer them honestly." to false,
    )

    var shown by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500)
        lines.indices.forEach { i ->
            onBeat(i * 2)
            shown = i + 1
            delay(if (i == 0) 1500L else 1750L)
        }
        ready = true
    }

    val pulse = rememberInfiniteTransition(label = "orb")
    val breathe by pulse.animateFloat(
        1f, 1.05f,
        infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "breathe"
    )
    // The core flares on each new line, so the visual pulse matches the bell.
    val flare by animateFloatAsState(
        targetValue = 1f + (shown % 2) * 0.10f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "flare"
    )

    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(34.dp))

        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(128.dp).scale(breathe * flare)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Violet500, Fuchsia500, Indigo700)))
            )
            Box(
                Modifier.size(168.dp).scale(flare)
                    .clip(CircleShape)
                    .border(1.dp, Violet300.copy(alpha = 0.30f), CircleShape)
            )
        }

        Spacer(Modifier.height(36.dp))

        lines.forEachIndexed { i, (text, big) ->
            WordReveal(visible = shown > i, text = text, big = big)
        }

        Spacer(Modifier.height(30.dp))

        AnimatedVisibility(ready, enter = fadeIn(tween(600))) {
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
                ) { Text("Start", color = White90, fontSize = 16.sp) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Reveals a line one word at a time, each word rising and fading in. Reading
 * it takes about as long as hearing it would, which is what keeps the pacing
 * cinematic rather than making the user wait on a wall of text.
 */
@Composable
private fun WordReveal(visible: Boolean, text: String, big: Boolean) {
    val words = remember(text) { text.split(" ") }
    var revealed by remember(text) { mutableIntStateOf(0) }

    LaunchedEffect(visible) {
        if (!visible) { revealed = 0; return@LaunchedEffect }
        words.indices.forEach { i ->
            revealed = i + 1
            delay(if (big) 120L else 58L)
        }
    }

    if (!visible) return

    FlowRowCentered(modifier = Modifier.fillMaxWidth().padding(bottom = if (big) 18.dp else 12.dp)) {
        words.forEachIndexed { i, w ->
            val on = i < revealed
            val alpha by animateFloatAsState(if (on) 1f else 0f, tween(340), label = "wa")
            val dy by animateFloatAsState(if (on) 0f else 14f, tween(340, easing = EaseOutCubic), label = "wy")
            Text(
                text = w,
                color = if (big) Color.White else White70,
                fontSize = if (big) 30.sp else 16.sp,
                fontWeight = if (big) FontWeight.Black else FontWeight.Medium,
                lineHeight = if (big) 38.sp else 24.sp,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .graphicsLayer { this.alpha = alpha; translationY = dy }
            )
        }
    }
}

/** Minimal centred flow layout, so a revealed line wraps like a paragraph. */
@Composable
private fun FlowRowCentered(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content = content, modifier = modifier) { measurables, constraints ->
        val max = constraints.maxWidth
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        var row = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var w = 0
        placeables.forEach { pl ->
            if (w + pl.width > max && row.isNotEmpty()) { rows.add(row); row = mutableListOf(); w = 0 }
            row.add(pl); w += pl.width
        }
        if (row.isNotEmpty()) rows.add(row)
        val height = rows.sumOf { r -> r.maxOf { it.height } }
        layout(max, height) {
            var y = 0
            rows.forEach { r ->
                var x = (max - r.sumOf { it.width }) / 2
                r.forEach { pl -> pl.placeRelative(x, y); x += pl.width }
                y += r.maxOf { it.height }
            }
        }
    }
}

/* ---------------------------- Question ---------------------------- */

@Composable
private fun QuestionStep(
    title: String,
    subtitle: String,
    options: List<Opt>,
    onPick: (Opt) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(10.dp))

        // The question arrives rather than appearing: it settles out of wide
        // letter-spacing while the options fade up underneath.
        var landed by remember(title) { mutableStateOf(false) }
        LaunchedEffect(title) { landed = false; delay(40); landed = true }
        val spacing by animateFloatAsState(
            if (landed) 0f else 6f, tween(620, easing = EaseOutCubic), label = "qsp"
        )
        val titleAlpha by animateFloatAsState(
            if (landed) 1f else 0f, tween(520), label = "qa"
        )

        Text(
            title,
            style = MaterialTheme.typography.displaySmall.copy(brush = gradientText()),
            lineHeight = 38.sp,
            letterSpacing = spacing.sp,
            modifier = Modifier.graphicsLayer { alpha = titleAlpha }
        )
        Spacer(Modifier.height(10.dp))
        AnimatedVisibility(landed, enter = fadeIn(tween(520)) + slideInVertically(tween(520)) { it / 3 }) {
            Text(subtitle, color = White70, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Spacer(Modifier.height(22.dp))

        options.forEachIndexed { idx, o ->
            var appeared by remember(o.id) { mutableStateOf(false) }
            LaunchedEffect(o.id) { delay(160L + idx * 70L); appeared = true }
            val optAlpha by animateFloatAsState(if (appeared) 1f else 0f, tween(380), label = "oa")
            val optDy by animateFloatAsState(if (appeared) 0f else 22f, tween(380, easing = EaseOutCubic), label = "od")
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
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale
                        alpha = optAlpha; translationY = optDy
                    }
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

/* ------------------------ Constellation progress ------------------------ */

/**
 * Replaces the progress bar. Each answered question lights a star and draws the
 * line back to the previous one, so the student watches a constellation form as
 * they go — the same object the whole app is about, assembling from their answers.
 */
@Composable
private fun ConstellationProgress(answered: Int, total: Int, progress: Float) {
    val lit by animateFloatAsState(
        targetValue = answered.toFloat(),
        animationSpec = tween(520, easing = EaseOutCubic),
        label = "lit"
    )
    val twinkle = rememberInfiniteTransition(label = "tw")
    val t by twinkle.animateFloat(
        0f, 6.2831855f,
        infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "t"
    )

    androidx.compose.foundation.Canvas(
        Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 18.dp)
    ) {
        val n = total
        val stepX = size.width / (n - 1).coerceAtLeast(1)
        // A fixed zig-zag so the shape reads as a constellation, not a ruler.
        val offsets = floatArrayOf(0.62f, 0.3f, 0.72f, 0.38f, 0.58f, 0.26f, 0.68f, 0.44f, 0.34f, 0.6f)
        fun pt(i: Int) = androidx.compose.ui.geometry.Offset(
            i * stepX,
            size.height * offsets[i % offsets.size]
        )

        // Lines between stars already earned
        for (i in 0 until n - 1) {
            val reach = (lit - 1 - i).coerceIn(0f, 1f)
            if (reach <= 0f) continue
            val a = pt(i); val b = pt(i + 1)
            drawLine(
                brush = Brush.horizontalGradient(listOf(Violet500, Fuchsia500)),
                start = a,
                end = androidx.compose.ui.geometry.Offset(
                    a.x + (b.x - a.x) * reach,
                    a.y + (b.y - a.y) * reach
                ),
                strokeWidth = 2f
            )
        }

        repeat(n) { i ->
            val p = pt(i)
            val on = i < lit
            val shimmer = 0.75f + 0.25f * sin(t + i)
            if (on) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Amber300.copy(alpha = 0.55f * shimmer), Color.Transparent),
                        center = p, radius = 11f
                    ),
                    radius = 11f, center = p
                )
            }
            drawCircle(
                color = if (on) Color.White else White10,
                radius = if (on) 3.4f else 2f,
                center = p,
                alpha = if (on) shimmer else 1f
            )
        }
    }
}
