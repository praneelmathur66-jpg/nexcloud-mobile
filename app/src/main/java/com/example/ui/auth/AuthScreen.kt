package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.NexCloudRepository
import com.example.ui.theme.NexBlue
import com.example.ui.theme.NexBorder
import com.example.ui.theme.NexCardBg
import com.example.ui.theme.NexCyan
import com.example.ui.theme.NexDanger
import com.example.ui.theme.NexDangerBg
import com.example.ui.theme.NexDarkBg
import com.example.ui.theme.NexSuccess
import com.example.ui.theme.NexSuccessBg
import com.example.ui.theme.NexTextMuted
import com.example.ui.theme.NexTextPrimary
import com.example.ui.theme.NexTextSecondary
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: NexCloudRepository,
    onLoginSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Forgot password dialog
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var isForgotLoading by remember { mutableStateOf(false) }

    fun performAuth() {
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()

        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            errorMessage = "Please enter both email and password."
            return
        }

        if (!isLoginMode && trimmedPass.length < 6) {
            errorMessage = "Password must be at least 6 characters."
            return
        }

        isLoading = true
        errorMessage = null
        successMessage = null

        coroutineScope.launch {
            if (isLoginMode) {
                val result = repository.login(trimmedEmail, trimmedPass)
                isLoading = false
                result.onSuccess {
                    onLoginSuccess()
                }.onFailure { err ->
                    errorMessage = "Login failed: ${err.message ?: "Invalid credentials"}"
                }
            } else {
                val result = repository.signUp(trimmedEmail, trimmedPass)
                isLoading = false
                result.onSuccess { res ->
                    if (res.accessToken != null) {
                        onLoginSuccess()
                    } else {
                        successMessage = "Account created! Check your email to confirm it."
                    }
                }.onFailure { err ->
                    errorMessage = "Signup failed: ${err.message ?: "Could not create account"}"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexDarkBg)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Card container matching website .login-card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NexCardBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(NexBorder, NexBorder)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Logo and Brand
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(NexBlue.copy(alpha = 0.3f), NexDarkBg))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "☁️",
                            fontSize = 32.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NexCloud",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your personal cloud storage",
                            fontSize = 14.sp,
                            color = NexTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Email input
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email") },
                        placeholder = { Text("your.email@example.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = NexTextMuted
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexBlue,
                            unfocusedBorderColor = NexBorder,
                            focusedTextColor = NexTextPrimary,
                            unfocusedTextColor = NexTextPrimary,
                            focusedLabelColor = NexBlue,
                            unfocusedLabelColor = NexTextSecondary
                        )
                    )

                    // Password input
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text(if (isLoginMode) "Password" else "Create a password (min 6 chars)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NexTextMuted
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                    tint = NexTextMuted
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { performAuth() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexBlue,
                            unfocusedBorderColor = NexBorder,
                            focusedTextColor = NexTextPrimary,
                            unfocusedTextColor = NexTextPrimary,
                            focusedLabelColor = NexBlue,
                            unfocusedLabelColor = NexTextSecondary
                        )
                    )

                    // Error Message Banner
                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NexDangerBg)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = NexDanger,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Success Message Banner
                    AnimatedVisibility(visible = successMessage != null) {
                        successMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NexSuccessBg)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = NexSuccess,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Submit button
                    Button(
                        onClick = { performAuth() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexBlue,
                            contentColor = NexTextPrimary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = NexTextPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = if (isLoginMode) "Login" else "Create Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Forgot password link (Login mode only)
                    if (isLoginMode) {
                        Text(
                            text = "Forgot your password?",
                            color = NexCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable {
                                    forgotEmail = email
                                    showForgotDialog = true
                                }
                                .testTag("forgot_password_button")
                        )
                    }

                    // Switch between Login and Signup
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                            color = NexTextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isLoginMode) "Create one" else "Login",
                            color = NexBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    isLoginMode = !isLoginMode
                                    errorMessage = null
                                    successMessage = null
                                }
                                .testTag("switch_auth_mode_button")
                        )
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = NexCardBg,
            title = {
                Text(
                    text = "📧 Password Reset",
                    color = NexTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your email address to receive password reset instructions.",
                        color = NexTextSecondary,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexBlue,
                            unfocusedBorderColor = NexBorder,
                            focusedTextColor = NexTextPrimary,
                            unfocusedTextColor = NexTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = forgotEmail.trim()
                        if (trimmed.isNotBlank()) {
                            isForgotLoading = true
                            coroutineScope.launch {
                                val result = repository.sendPasswordReset(trimmed)
                                isForgotLoading = false
                                showForgotDialog = false
                                result.onSuccess {
                                    successMessage = "Password reset email sent! 📧"
                                }.onFailure { err ->
                                    errorMessage = "Reset failed: ${err.message}"
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexBlue)
                ) {
                    if (isForgotLoading) {
                        CircularProgressIndicator(color = NexTextPrimary, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Send Reset Email")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Cancel", color = NexTextMuted)
                }
            }
        )
    }
}
