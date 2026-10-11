package app.studygalaxy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.ui.theme.*

data class OnboardingAnswers(
    val examGoal: String? = null,
    val dailyHours: Int? = null,
    val topSubjects: List<String> = emptyList(),
    val studyTime: String? = null,
    val biggestDistraction: String? = null
) {
    fun toJson(): String {
        val subs = topSubjects.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
        return """{"examGoal":${quote(examGoal)},"dailyHours":${dailyHours ?: "null"},"topSubjects":[$subs],"studyTime":${quote(studyTime)},"biggestDistraction":${quote(biggestDistraction)}}"""
    }
    private fun quote(s: String?) = if (s == null) "null" else "\"$s\""
}

private data class OptionItem(val id: String, val label: String, val emoji: String = "")

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(onDone: (OnboardingAnswers) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var step by remember { mutableStateOf(0) }
    var answers by remember { mutableStateOf(OnboardingAnswers()) }
    val totalSteps = 5
    val progress by animateFloatAsState(
        targetValue = (step + 1) / totalSteps.toFloat(),
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "progress"
    )

    val tap: () -> Unit = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    val next = {
        tap()
        if (step < totalSteps - 1) step++ else onDone(answers)
    }

    Column(Modifier.fillMaxSize()) {
        // Progress bar
        Box(
            Modifier.fillMaxWidth().height(3.dp).background(White5)
        ) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(progress)
                    .background(Brush.horizontalGradient(listOf(Violet500, Fuchsia500, Amber300)))
            )
        }

        Row(Modifier.fillMaxWidth().padding(20.dp)) {
            if (step > 0) {
                TextButton(onClick = { tap(); step-- }) {
                    Text("← Back", color = White70, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${step + 1} / $totalSteps".uppercase(),
                color = White50, fontSize = 10.sp, letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(450, easing = EaseOutCubic)) { if (targetState > initialState) it else -it } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { if (targetState > initialState) -it else it } + fadeOut(tween(200)))
            },
            label = "step",
            modifier = Modifier.weight(1f)
        ) { current ->
            when (current) {
                0 -> QuestionSingle(
                    headline = "What are you studying for?",
                    sub = "Pick the one closest to your goal. We'll shape your galaxy around it.",
                    options = listOf(
                        OptionItem("boards", "School Boards (CBSE/ICSE/State)", "📚"),
                        OptionItem("jee", "JEE (Engineering)", "⚡"),
                        OptionItem("neet", "NEET (Medical)", "🩺"),
                        OptionItem("gate", "GATE", "🎓"),
                        OptionItem("upsc", "UPSC", "🏛️"),
                        OptionItem("cat", "CAT (MBA)", "📈"),
                        OptionItem("college", "College / Semesters", "🏫"),
                        OptionItem("focus", "I just want to focus", "✨"),
                    ),
                    selected = answers.examGoal,
                    onPick = { answers = answers.copy(examGoal = it); tap(); next() }
                )
                1 -> HoursSlider(
                    initial = answers.dailyHours ?: 2,
                    onNext = { answers = answers.copy(dailyHours = it); next() }
                )
                2 -> QuestionMulti(
                    headline = "Which subjects need the most focus?",
                    sub = "Pick up to 4. These become your first planets.",
                    options = listOf("Math", "Physics", "Chemistry", "Biology", "English", "History",
                        "Computer Science", "Economics", "Hindi", "Geography"),
                    selected = answers.topSubjects,
                    max = 4,
                    onPick = { answers = answers.copy(topSubjects = it); tap() },
                    onNext = { next() }
                )
                3 -> QuestionSingle(
                    headline = "When do you study best?",
                    sub = "We'll remind you at the right time.",
                    options = listOf(
                        OptionItem("morning", "Early morning (5am-10am)", "🌅"),
                        OptionItem("afternoon", "Afternoon (12pm-5pm)", "☀️"),
                        OptionItem("evening", "Evening (5pm-9pm)", "🌆"),
                        OptionItem("night", "Late night (9pm-2am)", "🌙"),
                    ),
                    selected = answers.studyTime,
                    onPick = { answers = answers.copy(studyTime = it); tap(); next() }
                )
                4 -> QuestionSingle(
                    headline = "What's your biggest distraction?",
                    sub = "Being honest here helps your galaxy fight back.",
                    options = listOf(
                        OptionItem("phone", "This phone itself", "📱"),
                        OptionItem("social", "Social media (Instagram/YouTube)", "🎬"),
                        OptionItem("friends", "Friends messaging me", "💬"),
                        OptionItem("noise", "Noise around me", "🔊"),
                        OptionItem("sleep", "Sleep / being tired", "😴"),
                        OptionItem("boredom", "I just lose interest", "🥱"),
                    ),
                    selected = answers.biggestDistraction,
                    onPick = { answers = answers.copy(biggestDistraction = it); tap(); next() }
                )
            }
        }
    }
}

@Composable
private fun QuestionSingle(
    headline: String,
    sub: String,
    options: List<OptionItem>,
    selected: String?,
    onPick: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
    ) {
        Headline(headline, sub)
        Spacer(Modifier.height(24.dp))
        options.forEach { opt ->
            val isSelected = selected == opt.id
            OptionCard(opt.label, opt.emoji, isSelected) { onPick(opt.id) }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuestionMulti(
    headline: String,
    sub: String,
    options: List<String>,
    selected: List<String>,
    max: Int,
    onPick: (List<String>) -> Unit,
    onNext: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Headline(headline, sub)
        Spacer(Modifier.height(18.dp))
        Column(
            Modifier.weight(1f, fill = true).verticalScroll(rememberScrollState())
        ) {
            options.forEach { label ->
                val isSelected = selected.contains(label)
                OptionCard(label, if (isSelected) "✓" else "", isSelected) {
                    val list = selected.toMutableList()
                    if (isSelected) list.remove(label) else if (list.size < max) list.add(label)
                    onPick(list)
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onNext,
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    if (selected.isEmpty()) Brush.horizontalGradient(listOf(White10, White10))
                    else Brush.horizontalGradient(listOf(Violet500, Fuchsia500)),
                    RoundedCornerShape(16.dp)
                ),
                contentAlignment = Alignment.Center
            ) { Text(if (selected.isEmpty()) "Pick at least one" else "Continue", color = White90) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HoursSlider(initial: Int, onNext: (Int) -> Unit) {
    var hours by remember { mutableStateOf(initial.toFloat()) }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Headline("How many hours do you study every day right now?", "Be honest. We'll help you grow from here.")
        Spacer(Modifier.height(40.dp))
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${hours.toInt()}",
                style = MaterialTheme.typography.displayLarge.copy(brush = gradientText(), fontSize = 100.sp)
            )
            Text(
                if (hours.toInt() == 1) "hour / day" else "hours / day",
                color = White70, fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(32.dp))
        Slider(
            value = hours,
            onValueChange = { hours = it },
            valueRange = 0f..12f,
            steps = 11,
            colors = SliderDefaults.colors(
                thumbColor = Violet300,
                activeTrackColor = Violet500,
                inactiveTrackColor = White10
            )
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onNext(hours.toInt()) },
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
            ) { Text("Continue", color = White90) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Headline(h: String, sub: String) {
    Text(h, style = MaterialTheme.typography.headlineLarge.copy(brush = gradientText()))
    Spacer(Modifier.height(10.dp))
    Text(sub, color = White70, fontSize = 13.sp, lineHeight = 19.sp)
}

@Composable
private fun OptionCard(label: String, emoji: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(targetValue = if (selected) 1.02f else 1f, label = "s")
    val border by animateColorAsState(targetValue = if (selected) Violet500 else White10, label = "b")
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(
                if (selected) Brush.horizontalGradient(listOf(Violet500.copy(alpha = 0.15f), Fuchsia500.copy(alpha = 0.1f)))
                else Brush.linearGradient(listOf(White5, White5)),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji.isNotEmpty()) {
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
        }
        Text(label, color = White90, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (selected) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(Violet500), contentAlignment = Alignment.Center) {
                Text("✓", color = White90, fontSize = 11.sp)
            }
        }
    }
}

