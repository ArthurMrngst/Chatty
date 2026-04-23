package com.CO1102.Chatty.presentation.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun AdminActionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isMuted: Boolean,
    isAdmin: Boolean,
    onMuteToggle: () -> Unit,
    onMakeAdmin: () -> Unit,
    onRemoveAdmin: () -> Unit
) {

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {

        DropdownMenuItem(
            text = {
                Text(if (isMuted) "Unmute User 🔊" else "Mute User 🔇")
            },
            onClick = {
                onMuteToggle()
                onDismiss()
            }
        )

        if (!isAdmin) {
            DropdownMenuItem(
                text = { Text("Make Admin 👑") },
                onClick = {
                    onMakeAdmin()
                    onDismiss()
                }
            )
        } else {
            DropdownMenuItem(
                text = { Text("Remove Admin ❌") },
                onClick = {
                    onRemoveAdmin()
                    onDismiss()
                }
            )
        }
    }
}