package app.studygalaxy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onSignup: suspend (phone: String, password: String) -> String?, // returns error or null
    onDone: () -> Unit,
    onBack: () -> Unit,
    onLoginInstead: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val shake = remember { Animatable(0f) }
    LaunchedEffect(err) {
        if (err != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            shake.snapTo(0f)
            shake.animateTo(1f, keyframes { durationMillis = 420
                0f at 0; -1f at 80; 1f at 160; -0.7f at 240; 0.7f at 320; 0f at 420 })
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp).padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("← Back", color = White70, fontSize = 13.sp)
        }
        Text("Create account", style = MaterialTheme.typography.displaySmall.copy(brush = gradientText()))
        Text(
            "Your phone is your key. We never send SMS — we never spend your money.",
            color = White70, fontSize = 13.sp
        )

        Field("Phone number") {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' || c == '-' }.take(20) },
                placeholder = { Text("+91 98765 43210", color = White30) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Field("Password") {
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it.take(72) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                placeholder = { Text("At least 6 characters", color = White30) },
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Field("Confirm password") {
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it.take(72) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        err?.let {
            Box(
                Modifier.fillMaxWidth()
                    .graphicsLayer { translationX = shake.value * 12f }
                    .background(Color(0x33FF4D5A), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x55FF4D5A), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) { Text(it, color = Color(0xFFFCA5A5), fontSize = 13.sp) }
        }

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                err = null
                val clean = phone.replace("\\s|-".toRegex(), "")
                if (!clean.matches(Regex("^\\+?[0-9]{10,15}$"))) { err = "Enter a valid phone number"; return@Button }
                if (pass.length < 6) { err = "Password must be at least 6 characters"; return@Button }
                if (pass != confirm) { err = "Passwords do not match"; return@Button }
                loading = true
                scope.launch {
                    val e = onSignup(clean, pass)
                    loading = false
                    if (e != null) err = e else onDone()
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = !loading,
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
            ) { Text(if (loading) "Charting your galaxy…" else "Create account", color = White90) }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Already have an account? ", color = White50, fontSize = 12.sp)
            Text(
                "Log in",
                color = Violet300, fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            TextButton(onClick = onLoginInstead, modifier = Modifier.height(20.dp), contentPadding = PaddingValues(0.dp)) {}
        }
    }
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label.uppercase(), color = White50, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = White90,
    unfocusedTextColor = White90,
    focusedBorderColor = Violet500,
    unfocusedBorderColor = White10,
    cursorColor = Violet500,
    focusedContainerColor = Color(0x0DFFFFFF),
    unfocusedContainerColor = Color(0x0DFFFFFF)
)
