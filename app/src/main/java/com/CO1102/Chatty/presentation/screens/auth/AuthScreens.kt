package com.CO1102.Chatty.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.CO1102.Chatty.presentation.viewmodel.AuthViewModel
import com.CO1102.Chatty.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel

val messengerGradient = Brush.linearGradient(listOf(MessengerBlue, MessengerBlueLight))

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE FIELD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MessengerField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPasswordToggle: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MessengerTextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = MessengerBlue,
            unfocusedBorderColor    = MessengerDivider,
            focusedContainerColor   = MessengerBackground,
            unfocusedContainerColor = MessengerBackground,
            cursorColor             = MessengerBlue,
        ),
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        trailingIcon = if (showPasswordToggle && onTogglePassword != null) ({
            Text(
                text = if (passwordVisible) "Hide" else "Show",
                color = MessengerBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(end = 14.dp)
                    .clickable { onTogglePassword() }
            )
        }) else null
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE GRADIENT BUTTON
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GradientButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) messengerGradient
                else Brush.linearGradient(listOf(Color(0xFFB0BEC5), Color(0xFFB0BEC5)))
            )
            .clickable(enabled = enabled && !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOGIN SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var localError  by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()
    val authError by viewModel.error.collectAsState()

    // Navigate on success
    LaunchedEffect(viewModel.loginSuccess.collectAsState().value) {
        if (viewModel.loginSuccess.value) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(72.dp))

        // Title — no icon
        Text(
            text = "Chatty",
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MessengerTextPrimary,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Sign in to continue",
            fontSize = 15.sp,
            color = MessengerTextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 36.dp)
        )

        // Fields
        MessengerField(
            value = email,
            onValueChange = { email = it; localError = "" },
            placeholder = "Email or phone number",
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(12.dp))
        MessengerField(
            value = password,
            onValueChange = { password = it; localError = "" },
            placeholder = "Password",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            showPasswordToggle = true,
            passwordVisible = showPass,
            onTogglePassword = { showPass = !showPass }
        )

        // Error message (local validation or Firebase)
        val displayError = localError.ifEmpty { authError ?: "" }
        if (displayError.isNotEmpty()) {
            Text(
                text = displayError,
                color = MessengerError,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }

        // Forgot password
        Text(
            text = "Forgot password?",
            color = MessengerBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp, bottom = 20.dp)
                .clickable { /* navigate to forgot password */ }
        )

        // Login button
        GradientButton(
            label = "Log In",
            onClick = {
                if (email.isBlank())    { localError = "Please enter your email.";    return@GradientButton }
                if (password.isBlank()) { localError = "Please enter your password."; return@GradientButton }
                viewModel.login(email.trim(), password)
            },
            enabled   = !isLoading,
            isLoading = isLoading
        )

        Spacer(Modifier.height(20.dp))

        // Divider
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MessengerDivider)
            Text("  or  ", color = MessengerTextSecondary, fontSize = 13.sp)
            HorizontalDivider(modifier = Modifier.weight(1f), color = MessengerDivider)
        }

        Spacer(Modifier.height(20.dp))

        // Sign up link
        Row {
            Text("Don't have an account? ", color = MessengerTextSecondary, fontSize = 14.sp)
            Text(
                text = "Sign Up",
                color = MessengerBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onGoRegister() }
            )
        }

        Spacer(Modifier.height(48.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REGISTER SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onGoLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email      by remember { mutableStateOf("") }
    var password   by remember { mutableStateOf("") }
    var confirm    by remember { mutableStateOf("") }
    var showPass   by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf("") }

    val isLoading   by viewModel.isLoading.collectAsState()
    val authError   by viewModel.error.collectAsState()

    val mismatch = confirm.isNotEmpty() && password != confirm
    val ready    = email.isNotBlank() && password.isNotBlank() && confirm.isNotBlank() && !mismatch

    LaunchedEffect(viewModel.registerSuccess.collectAsState().value) {
        if (viewModel.registerSuccess.value) onRegisterSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        // Title — no icon
        Text(
            text = "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MessengerTextPrimary,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "It's quick and easy.",
            fontSize = 14.sp,
            color = MessengerTextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 28.dp)
        )

        // Email
        MessengerField(
            value = email,
            onValueChange = { email = it; localError = "" },
            placeholder = "Email or phone number",
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(12.dp))

        // Password
        MessengerField(
            value = password,
            onValueChange = { password = it; localError = "" },
            placeholder = "Password",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            showPasswordToggle = true,
            passwordVisible = showPass,
            onTogglePassword = { showPass = !showPass }
        )
        Spacer(Modifier.height(12.dp))

        // Confirm password
        MessengerField(
            value = confirm,
            onValueChange = { confirm = it; localError = "" },
            placeholder = "Confirm password",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            passwordVisible = showPass
        )

        // Errors
        val displayError = when {
            mismatch     -> "Passwords don't match"
            localError.isNotEmpty() -> localError
            authError != null       -> authError!!
            else -> ""
        }
        if (displayError.isNotEmpty()) {
            Text(
                text = displayError,
                color = MessengerError,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Register button
        GradientButton(
            label = "Sign Up",
            onClick = {
                when {
                    email.isBlank()    -> { localError = "Please enter your email." }
                    password.isBlank() -> { localError = "Please enter a password." }
                    mismatch           -> { localError = "Passwords don't match." }
                    else -> viewModel.register(email.trim(), password)
                }
            },
            enabled   = ready && !isLoading,
            isLoading = isLoading
        )

        Spacer(Modifier.height(20.dp))

        Row {
            Text("Already have an account? ", color = MessengerTextSecondary, fontSize = 14.sp)
            Text(
                text = "Log In",
                color = MessengerBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onGoLogin() }
            )
        }

        Spacer(Modifier.height(48.dp))
    }
}

