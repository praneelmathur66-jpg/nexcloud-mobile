package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.NexCloudRepository
import com.example.data.repository.formatBytes
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
fun SettingsScreen(
    repository: NexCloudRepository,
    usedBytes: Long,
    quotaBytes: Long,
    onBack: () -> Unit
) {
    // BackHandler for system back button
    BackHandler {
        onBack()
    }

    val coroutineScope = rememberCoroutineScope()
    val userEmail = repository.session.getUserEmail() ?: ""

    // Change password state
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }
    var passwordChangeMessage by remember { mutableStateOf<String?>(null) }
    var isPasswordSuccess by remember { mutableStateOf(false) }

    // Reset email state
    var isSendingReset by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var isResetSuccess by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NexDarkBg,
        topBar = {
            Surface(
                color = NexCardBg,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NexTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "⚙️ NexCloud Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexTextPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account Overview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_overview_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NexCardBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(NexBorder, NexBorder)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "👤 Account Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Email", color = NexTextSecondary, fontSize = 13.sp)
                        Text(userEmail, color = NexCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Storage Quota", color = NexTextSecondary, fontSize = 13.sp)
                        Text(
                            "${formatBytes(usedBytes)} / ${formatBytes(quotaBytes)}",
                            color = NexTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Change Password Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("change_password_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NexCardBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(NexBorder, NexBorder)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔐", fontSize = 18.sp)
                        Text(
                            text = "Change Password",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexTextPrimary
                        )
                    }

                    Text(
                        text = "Change your NexCloud account password.",
                        color = NexTextSecondary,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            passwordChangeMessage = null
                        },
                        label = { Text("New password") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = NexTextMuted)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = NexTextMuted
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_password_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexBlue,
                            unfocusedBorderColor = NexBorder,
                            focusedTextColor = NexTextPrimary,
                            unfocusedTextColor = NexTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordChangeMessage = null
                        },
                        label = { Text("Confirm new password") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = NexTextMuted)
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_password_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexBlue,
                            unfocusedBorderColor = NexBorder,
                            focusedTextColor = NexTextPrimary,
                            unfocusedTextColor = NexTextPrimary
                        )
                    )

                    AnimatedVisibility(visible = passwordChangeMessage != null) {
                        passwordChangeMessage?.let { msg ->
                            val bg = if (isPasswordSuccess) NexSuccessBg else NexDangerBg
                            val col = if (isPasswordSuccess) NexSuccess else NexDanger
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(text = msg, color = col, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (newPassword.isBlank() || confirmPassword.isBlank()) {
                                passwordChangeMessage = "Fill in both fields."
                                isPasswordSuccess = false
                                return@Button
                            }
                            if (newPassword.length < 6) {
                                passwordChangeMessage = "Password must be at least 6 characters."
                                isPasswordSuccess = false
                                return@Button
                            }
                            if (newPassword != confirmPassword) {
                                passwordChangeMessage = "Passwords do not match."
                                isPasswordSuccess = false
                                return@Button
                            }

                            isChangingPassword = true
                            passwordChangeMessage = null

                            coroutineScope.launch {
                                val result = repository.updatePassword(newPassword)
                                isChangingPassword = false
                                result.onSuccess {
                                    passwordChangeMessage = "Password changed successfully! 🔐"
                                    isPasswordSuccess = true
                                    newPassword = ""
                                    confirmPassword = ""
                                }.onFailure { err ->
                                    passwordChangeMessage = "Password change failed: ${err.message}"
                                    isPasswordSuccess = false
                                }
                            }
                        },
                        enabled = !isChangingPassword,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_password_submit_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexBlue,
                            contentColor = NexTextPrimary
                        )
                    ) {
                        if (isChangingPassword) {
                            CircularProgressIndicator(color = NexTextPrimary, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Change Password", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Password Reset Email Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_password_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NexCardBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(NexBorder, NexBorder)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📧", fontSize = 18.sp)
                        Text(
                            text = "Password Reset",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexTextPrimary
                        )
                    }

                    Text(
                        text = "Send a password reset email to your account ($userEmail).",
                        color = NexTextSecondary,
                        fontSize = 13.sp
                    )

                    AnimatedVisibility(visible = resetMessage != null) {
                        resetMessage?.let { msg ->
                            val bg = if (isResetSuccess) NexSuccessBg else NexDangerBg
                            val col = if (isResetSuccess) NexSuccess else NexDanger
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(text = msg, color = col, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (userEmail.isNotBlank()) {
                                isSendingReset = true
                                resetMessage = null
                                coroutineScope.launch {
                                    val result = repository.sendPasswordReset(userEmail)
                                    isSendingReset = false
                                    result.onSuccess {
                                        resetMessage = "Reset email sent! 📧"
                                        isResetSuccess = true
                                    }.onFailure { err ->
                                        resetMessage = "Could not send email: ${err.message}"
                                        isResetSuccess = false
                                    }
                                }
                            }
                        },
                        enabled = !isSendingReset && userEmail.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_reset_email_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexBlue,
                            contentColor = NexTextPrimary
                        )
                    ) {
                        if (isSendingReset) {
                            CircularProgressIndicator(color = NexTextPrimary, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Send Reset Email", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Back to Files button matching website button
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("back_to_files_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexCyan)
            ) {
                Text("← Back to Files", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
