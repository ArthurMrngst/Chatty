package com.CO1102.Chatty.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.CO1102.Chatty.ui.theme.*


@Composable
fun AdminActionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isMuted: Boolean,
    isAdmin: Boolean,
    onMute: (minutes: Int) -> Unit,
    onUnmute: () -> Unit,
    onMakeAdmin: () -> Unit,
    onRemoveAdmin: () -> Unit
) {
    var showMuteDialog by remember { mutableStateOf(false) }
    var muteMinutesText by remember { mutableStateOf("") }

    // ── Dropdown menu ──────────────────────────────────────────────────────
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        // Mute / Unmute
        if (isMuted) {
            DropdownMenuItem(
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔊", fontSize = 16.sp)
                        Text("Unmute", fontSize = 14.sp, color = MessengerTextPrimary)
                    }
                },
                onClick = {
                    onUnmute()
                    onDismiss()
                }
            )
        } else {
            DropdownMenuItem(
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔇", fontSize = 16.sp)
                        Text("Mute...", fontSize = 14.sp, color = MessengerTextPrimary)
                    }
                },
                onClick = {
                    showMuteDialog = true
                    onDismiss()
                }
            )
        }

        HorizontalDivider(color = MessengerDivider, thickness = 0.5.dp)

        // Admin toggle
        if (isAdmin) {
            DropdownMenuItem(
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("👑", fontSize = 16.sp)
                        Text("Remove admin", fontSize = 14.sp, color = MessengerError)
                    }
                },
                onClick = { onRemoveAdmin(); onDismiss() }
            )
        } else {
            DropdownMenuItem(
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("👑", fontSize = 16.sp)
                        Text("Make admin", fontSize = 14.sp, color = MessengerTextPrimary)
                    }
                },
                onClick = { onMakeAdmin(); onDismiss() }
            )
        }
    }

    // ── Mute duration dialog ───────────────────────────────────────────────
    if (showMuteDialog) {
        Dialog(onDismissRequest = { showMuteDialog = false; muteMinutesText = "" }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "🔇 Mute User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MessengerTextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Enter how many minutes to mute this user.",
                        fontSize = 13.sp,
                        color = MessengerTextSecondary
                    )
                    Spacer(Modifier.height(16.dp))

                    // Quick presets
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("5" to "5m", "30" to "30m", "60" to "1hr", "1440" to "24hr").forEach { (value, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (muteMinutesText == value) MessengerBlue
                                        else MessengerBackground
                                    )
                                    .clickable { muteMinutesText = value }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (muteMinutesText == value) Color.White else MessengerTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Custom input
                    OutlinedTextField(
                        value = muteMinutesText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) muteMinutesText = it },
                        placeholder = { Text("Custom minutes e.g. 90", color = MessengerTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = MessengerBlue,
                            unfocusedBorderColor    = MessengerDivider,
                            focusedContainerColor   = MessengerBackground,
                            unfocusedContainerColor = MessengerBackground,
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Show human-readable duration
                    val minutes = muteMinutesText.toIntOrNull() ?: 0
                    if (minutes > 0) {
                        val display = when {
                            minutes < 60   -> "$minutes minute${if (minutes > 1) "s" else ""}"
                            minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m".trimEnd('m').trimEnd(' ')
                            else           -> "${minutes / 1440} day${if (minutes / 1440 > 1) "s" else ""}"
                        }
                        Text(
                            "Will be muted for $display",
                            fontSize = 12.sp,
                            color = MessengerBlue,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showMuteDialog = false; muteMinutesText = "" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("Cancel", color = MessengerTextSecondary) }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (minutes > 0) MessengerBlue else Color(0xFFB0BEC5)
                                )
                                .clickable(enabled = minutes > 0) {
                                    onMute(minutes)
                                    showMuteDialog = false
                                    muteMinutesText = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Mute", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}