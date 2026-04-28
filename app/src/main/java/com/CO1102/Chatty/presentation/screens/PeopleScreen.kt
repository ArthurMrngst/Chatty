package com.CO1102.Chatty.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.CO1102.Chatty.domain.model.Group
import com.CO1102.Chatty.domain.model.User
import com.CO1102.Chatty.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*
import com.CO1102.Chatty.presentation.screens.chat.MessengerAvatar
import com.CO1102.Chatty.presentation.screens.chat.MessengerBottomNav

@Composable
fun PeopleScreen(
    onOpenChat: (Group) -> Unit,
    onBackToChats: () -> Unit,
    onMeClick: () -> Unit = {}
) {
    val db            = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var allUsers    by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Live listen to all users
    LaunchedEffect(true) {
        db.collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allUsers = snapshot.documents
                        .mapNotNull { it.toObject<User>() }
                        .filter { it.uid != currentUserId }
                        .sortedWith(
                            compareByDescending<User> { it.online }.thenBy { it.email }
                        )
                }
                isLoading = false
            }
    }

    val filtered = if (searchQuery.isBlank()) allUsers
    else allUsers.filter { it.email.contains(searchQuery, ignoreCase = true) }

    val onlineUsers  = filtered.filter { it.online }
    val offlineUsers = filtered.filter { !it.online }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "People",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MessengerTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MessengerOnline.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${onlineUsers.size} online",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MessengerOnline
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MessengerBackground)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔍", fontSize = 15.sp)
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = MessengerTextPrimary),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("Search people", color = MessengerTextSecondary, fontSize = 15.sp)
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        HorizontalDivider(color = MessengerDivider, thickness = 0.5.dp)

        if (isLoading) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MessengerBlue)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {

                // ── Online ─────────────────────────────────────────────────
                if (onlineUsers.isNotEmpty()) {
                    item {
                        Text(
                            text = "ACTIVE NOW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MessengerTextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
                        )
                    }
                    items(onlineUsers) { user ->
                        PeopleRow(
                            user          = user,
                            currentUserId = currentUserId,
                            db            = db,
                            onOpenChat    = onOpenChat
                        )
                    }
                }

                // ── Offline ────────────────────────────────────────────────
                if (offlineUsers.isNotEmpty()) {
                    item {
                        Text(
                            text = "OFFLINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MessengerTextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
                        )
                    }
                    items(offlineUsers) { user ->
                        PeopleRow(
                            user          = user,
                            currentUserId = currentUserId,
                            db            = db,
                            onOpenChat    = onOpenChat
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No people found", color = MessengerTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Bottom nav with back to chats wired
        MessengerBottomNav(
            activeIndex = 1,
            onTabClick  = { index ->
                when (index) {
                    0 -> onBackToChats()
                    3 -> onMeClick()
                }
            }
        )
    }
}

// ── Single user row ────────────────────────────────────────────────────────
@Composable
private fun PeopleRow(
    user: User,
    currentUserId: String,
    db: FirebaseFirestore,
    onOpenChat: (Group) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) {
                isLoading = true
                findOrCreateDM(
                    db            = db,
                    currentUserId = currentUserId,
                    otherUserId   = user.uid,
                    otherEmail    = user.email
                ) { group ->
                    isLoading = false
                    onOpenChat(group)
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MessengerAvatar(name = user.email, size = 52, online = user.online)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.email.substringBefore("@"),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MessengerTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = user.email,
                fontSize = 12.sp,
                color = MessengerTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Status pill
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MessengerBlue,
                strokeWidth = 2.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (user.online) MessengerOnline.copy(alpha = 0.12f)
                        else MessengerBackground
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                if (user.online) {
                    Text("Active", fontSize = 12.sp, color = MessengerOnline, fontWeight = FontWeight.SemiBold)
                } else {
                    val lastSeen = user.lastSeen?.toDate()?.let { date ->
                        val diff = System.currentTimeMillis() - date.time
                        val mins = diff / 60000
                        when {
                            mins < 1    -> "Just now"
                            mins < 60   -> "${mins}m ago"
                            mins < 1440 -> "${mins / 60}h ago"
                            else        -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
                        }
                    } ?: "Offline"
                    Text(lastSeen, fontSize = 12.sp, color = MessengerTextSecondary)
                }
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 80.dp),
        color = MessengerDivider,
        thickness = 0.5.dp
    )
}

// ── Find or create DM ──────────────────────────────────────────────────────
private fun findOrCreateDM(
    db: FirebaseFirestore,
    currentUserId: String,
    otherUserId: String,
    otherEmail: String,
    onResult: (Group) -> Unit
) {
    db.collection("groups")
        .whereEqualTo("isDirectChat", true)
        .whereArrayContains("members", currentUserId)
        .get()
        .addOnSuccessListener { snapshot ->
            val existing = snapshot.documents.firstOrNull { doc ->
                val members = doc.get("members") as? List<*> ?: emptyList<String>()
                members.contains(otherUserId) && members.size == 2
            }
            if (existing != null) {
                val group = existing.toObject<Group>()?.copy(id = existing.id)
                if (group != null) onResult(group)
            } else {
                val newGroup = hashMapOf(
                    "name"         to otherEmail.substringBefore("@"),
                    "members"      to listOf(currentUserId, otherUserId),
                    "admins"       to listOf(currentUserId),
                    "mutedUsers"   to emptyList<String>(),
                    "isDirectChat" to true,
                    "createdAt"    to FieldValue.serverTimestamp()
                )
                db.collection("groups").add(newGroup)
                    .addOnSuccessListener { ref ->
                        onResult(
                            Group(
                                id           = ref.id,
                                name         = otherEmail.substringBefore("@"),
                                members      = listOf(currentUserId, otherUserId),
                                admins       = listOf(currentUserId),
                                isDirectChat = true
                            )
                        )
                    }
            }
        }
}