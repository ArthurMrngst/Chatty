package com.CO1102.Chatty.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.CO1102.Chatty.ui.theme.*


@Composable
fun CreatePollDialog(
    onDismiss: () -> Unit,
    // options is Map<String, Long> — option text → initial vote count (always 0)
    onCreatePoll: (question: String, options: Map<String, Long>) -> Unit
) {
    var question by remember { mutableStateOf("") }
    // Store options as a list of strings for editing, convert to Map on submit
    var optionList by remember { mutableStateOf(listOf("", "")) }

    val isValid = question.isNotBlank() && optionList.count { it.isNotBlank() } >= 2

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // ── Title ──────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📊", fontSize = 22.sp)
                    Text(
                        "Create Poll",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MessengerTextPrimary
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MessengerBackground)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 13.sp, color = MessengerTextSecondary)
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ── Question ───────────────────────────────────────────────
                Text(
                    "Question",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MessengerTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = { Text("Ask something...", color = MessengerTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MessengerBlue,
                        unfocusedBorderColor    = MessengerDivider,
                        focusedContainerColor   = MessengerBackground,
                        unfocusedContainerColor = MessengerBackground,
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(Modifier.height(18.dp))

                // ── Options ────────────────────────────────────────────────
                Text(
                    "Options",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MessengerTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(6.dp))

                optionList.forEachIndexed { index, option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        // Letter badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MessengerBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ('A' + index).toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MessengerBlue
                            )
                        }

                        OutlinedTextField(
                            value = option,
                            onValueChange = { newVal ->
                                optionList = optionList.toMutableList().also { it[index] = newVal }
                            },
                            placeholder = { Text("Option ${index + 1}", color = MessengerTextSecondary) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = MessengerBlue,
                                unfocusedBorderColor    = MessengerDivider,
                                focusedContainerColor   = MessengerBackground,
                                unfocusedContainerColor = MessengerBackground,
                            ),
                            singleLine = true
                        )

                        // Remove button (only if more than 2 options)
                        if (optionList.size > 2) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MessengerError.copy(alpha = 0.1f))
                                    .clickable {
                                        optionList = optionList.toMutableList().also { it.removeAt(index) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✕", fontSize = 12.sp, color = MessengerError)
                            }
                        }
                    }
                }

                // Add option button (max 6)
                if (optionList.size < 6) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MessengerBackground)
                            .clickable { optionList = optionList + "" }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "+ Add Option",
                            fontSize = 14.sp,
                            color = MessengerBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Create button ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isValid)
                                Brush.linearGradient(listOf(MessengerBlue, MessengerBlueLight))
                            else
                                Brush.linearGradient(listOf(Color(0xFFB0BEC5), Color(0xFFB0BEC5)))
                        )
                        .clickable(enabled = isValid) {
                            // Convert list → Map<String, Long> with 0 votes each
                            val optionsMap = optionList
                                .filter { it.isNotBlank() }
                                .associate { it.trim() to 0L }
                            onCreatePoll(question.trim(), optionsMap)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Create Poll",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}