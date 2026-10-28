package com.example.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NexBlue
import com.example.ui.theme.NexBorder
import com.example.ui.theme.NexCardBg
import com.example.ui.theme.NexTextMuted
import com.example.ui.theme.NexTextPrimary
import com.example.ui.theme.NexTextSecondary

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexCardBg,
        title = {
            Text(
                text = "📁 Create Folder",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NexTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = {
                        folderName = it
                        errorMessage = null
                    },
                    label = { Text("Folder name") },
                    placeholder = { Text("e.g. Documents, Photos") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage ?: "", color = androidx.compose.ui.graphics.Color(0xFFEF4444))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_folder_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexBlue,
                        unfocusedBorderColor = NexBorder,
                        focusedTextColor = NexTextPrimary,
                        unfocusedTextColor = NexTextPrimary,
                        focusedLabelColor = NexBlue,
                        unfocusedLabelColor = NexTextSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = folderName.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Enter a folder name"
                    } else {
                        onConfirm(trimmed)
                    }
                },
                modifier = Modifier.testTag("confirm_create_folder_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexBlue,
                    contentColor = NexTextPrimary
                )
            ) {
                Text("Create Folder", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_create_folder_button")
            ) {
                Text("Cancel", color = NexTextMuted)
            }
        }
    )
}
