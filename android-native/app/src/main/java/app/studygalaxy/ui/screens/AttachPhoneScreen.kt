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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Lets a guest add a phone and password to the account they already have.
 * Guests keep their galaxy; this only makes it reachable from another device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachPhoneScreen(
    cosmicName: String,
    onAttach: suspend (phone: String, password: String) -> String?,
    onBack: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }
    LaunchedEffect(err) {
        if (err != null) {
            shake.snapTo(0f)
            shake.animateTo(1f, keyframes {
                durationMillis = 420
                0f at 0; -1f at 80; 1f at 160; -0.7f at 240; 0.7f at 320; 0f at 420
            })
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp).padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("← Back", color = White70, fontSize = 13.sp)
        }
        Text("Save your galaxy", style = MaterialTheme.typography.displaySmall.copy(brush = gradientText()))
        Text(
            "You're signed in as $cosmicName on this phone only. Add a number " +
                "and password so you can reach this same galaxy from another device. " +
                "Nothing you've done is lost.",
            color = White70, fontSize = 13.sp
        )

        Column {
            Text("PHONE NUMBER", color = White50, fontSize = 10.sp, letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' || c == '-' }.take(20) },
                placeholder = { Text("98765 43210", color = White30) },
                leadingIcon = { Text("+91", color = White70, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = attachFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column {
            Text("PASSWORD", color = White50, fontSize = 10.sp, letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it.take(72) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                placeholder = { Text("At least 8 characters", color = White30) },
                colors = attachFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column {
            Text("CONFIRM PASSWORD", color = White50, fontSize = 10.sp, letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it.take(72) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = attachFieldColors(),
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
                err = null
                val clean = phone.replace("\\s|-".toRegex(), "")
                if (!clean.matches(Regex("^\\+?[0-9]{10,15}$"))) { err = "Enter a valid phone number"; return@Button }
                if (pass.length < 8) { err = "Password must be at least 8 characters"; return@Button }
                if (pass != confirm) { err = "Passwords do not match"; return@Button }
                loading = true
                scope.launch {
                    val e = onAttach(clean, pass)
                    loading = false
                    if (e != null) err = e
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
            ) { Text(if (loading) "Saving…" else "Save my account", color = White90) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun attachFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = White90,
    unfocusedTextColor = White90,
    focusedBorderColor = Violet500,
    unfocusedBorderColor = White10,
    cursorColor = Violet500,
    focusedContainerColor = Color(0x0DFFFFFF),
    unfocusedContainerColor = Color(0x0DFFFFFF)
)
