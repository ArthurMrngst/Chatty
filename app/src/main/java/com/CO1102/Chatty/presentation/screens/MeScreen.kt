package com.CO1102.Chatty.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.CO1102.Chatty.domain.model.User
import com.CO1102.Chatty.presentation.screens.chat.MessengerBottomNav
import com.CO1102.Chatty.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject


@Composable
fun MeScreen(
    onLogout: () -> Unit,
    onBackToChats: () -> Unit = {}
) {
    val db            = FirebaseFirestore.getInstance()
    val auth          = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val currentEmail  = auth.currentUser?.email ?: ""

    var currentUser   by remember { mutableStateOf<User?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Fetch current user data
    LaunchedEffect(currentUserId) {
        if (currentUserId.isEmpty()) return@LaunchedEffect
        db.collection("users").document(currentUserId)
            .addSnapshotListener { snapshot, _ ->
                currentUser = snapshot?.toObject<User>()
            }
    }

    val displayName = currentEmail.substringBefore("@")
    val initials = displayName.take(2).uppercase()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .verticalScroll(rememberScrollState())
    ) {
        // ── PROFILE HEADER ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 52.dp, bottom = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(MessengerBlue, MessengerBlueLight))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = displayName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MessengerTextPrimary
                )
                Text(
                    text = currentEmail,
                    fontSize = 14.sp,
                    color = MessengerTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Online status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MessengerOnline.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MessengerOnline)
                        )
                        Text(
                            text = "Active now",
                            fontSize = 13.sp,
                            color = MessengerOnline,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── MENU SECTIONS ──────────────────────────────────────────────────

        // Section: Account
        MenuSection(title = "Account") {
            MenuItem(icon = "👤", label = "Edit Profile", subtitle = "Name, photo, username") {}
            MenuItem(icon = "🔒", label = "Privacy", subtitle = "Control who sees your info") {}
            MenuItem(icon = "🔔", label = "Notifications", subtitle = "Manage alerts and sounds") {}
        }

        Spacer(Modifier.height(12.dp))

        // Section: Preferences
        MenuSection(title = "Preferences") {
            MenuItem(icon = "🌙", label = "Dark Mode", subtitle = "Switch appearance") {}
            MenuItem(icon = "💬", label = "Message Requests", subtitle = "Manage incoming requests") {}
            MenuItem(icon = "🌐", label = "Language", subtitle = "English") {}
        }

        Spacer(Modifier.height(12.dp))

        // Section: Support
        MenuSection(title = "Support") {
            MenuItem(icon = "❓", label = "Help Centre", subtitle = "Get help with Chatty") {}
            MenuItem(icon = "⚠️", label = "Report a Problem", subtitle = "Something not working?") {}
            MenuItem(icon = "ℹ️", label = "About", subtitle = "Version 1.0.0") {}
        }

        Spacer(Modifier.height(12.dp))

        // ── LOGOUT BUTTON ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .clickable { showLogoutDialog = true }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MessengerError.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↩️", fontSize = 18.sp)
                }
                Text(
                    text = "Log Out",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MessengerError
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Bottom nav
        MessengerBottomNav(
            activeIndex = 3,
            onTabClick  = { index ->
                when (index) {
                    0 -> onBackToChats()
                }
            }
        )
    }

    // ── LOGOUT CONFIRMATION DIALOG ─────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Log Out?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    "Are you sure you want to log out of Chatty?",
                    color = MessengerTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MessengerError)
                        .clickable { onLogout() }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = MessengerTextSecondary)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MENU SECTION — white card with title
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MenuSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MessengerTextSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MENU ITEM — single row with icon, label, subtitle, chevron
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MenuItem(
    icon: String,
    label: String,
    subtitle: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MessengerBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }

        // Label + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MessengerTextPrimary
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MessengerTextSecondary
                )
            }
        }

        // Chevron
        Text("›", fontSize = 20.sp, color = MessengerTextSecondary)
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp),
        color = MessengerDivider,
        thickness = 0.5.dp
    )
}
