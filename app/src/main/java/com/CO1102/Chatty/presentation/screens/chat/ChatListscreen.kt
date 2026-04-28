package com.CO1102.Chatty.presentation.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

// ─────────────────────────────────────────────────────────────────────────────
// MESSENGER AVATAR  (shared across screens)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MessengerAvatar(name: String, size: Int = 56, online: Boolean = false) {
    val palettes = listOf(
        listOf(Color(0xFF0084FF), Color(0xFF0099FF)),
        listOf(Color(0xFFE91E8C), Color(0xFFFF6B35)),
        listOf(Color(0xFF7B61FF), Color(0xFF0084FF)),
        listOf(Color(0xFF00C853), Color(0xFF0099FF)),
        listOf(Color(0xFFFF6B35), Color(0xFFFFB300)),
    )
    val (a, b) = palettes[(name.firstOrNull()?.code ?: 0) % palettes.size]
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2).joinToString("").ifEmpty { "?" }

    Box(modifier = Modifier.size(size.dp), contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(a, b))),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = Color.White, fontSize = (size * 0.36f).sp, fontWeight = FontWeight.Bold)
        }
        if (online) {
            Box(
                modifier = Modifier
                    .size((size * 0.28f).dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
            ) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(MessengerOnline))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM NAV  (shared)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MessengerBottomNav(
    activeIndex: Int = 0,
    onTabClick: (Int) -> Unit = {}   // ← ADD THIS
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("💬" to "Chats", "👥" to "People", "🔔" to "Notifs", "👤" to "Me")
            .forEachIndexed { i, (icon, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTabClick(i) }  // ← WIRED
                ) {
                    Text(icon, fontSize = 22.sp, color = if (i == activeIndex) MessengerBlue else MessengerTextSecondary)
                    Text(
                        label, fontSize = 10.sp,
                        fontWeight = if (i == activeIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (i == activeIndex) MessengerBlue else MessengerTextSecondary
                    )
                }
            }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHAT LIST SCREEN  — fetches its own data from Firestore
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ChatListScreen(
    // These params are kept for MainActivity compatibility but ignored —
    // the screen fetches from Firestore directly.
    currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "",
    onGroupClick: (Group) -> Unit,
    onNewChatClick: () -> Unit = {},
    onPeopleClick: () -> Unit = {},
    onMeClick: () -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()

    var fetchedGroups  by remember { mutableStateOf<List<Group>>(emptyList()) }
    var fetchedUserMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var search         by remember { mutableStateOf("") }
    var isLoading      by remember { mutableStateOf(true) }

    // ── Fetch groups where current user is a member ────────────────────
    LaunchedEffect(currentUserId) {
        if (currentUserId.isEmpty()) return@LaunchedEffect
        db.collection("groups")
            .whereArrayContains("members", currentUserId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    fetchedGroups = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Group ::class.java)?.copy(id = doc.id)
                    }
                }
                isLoading = false
            }
    }

    // ── Fetch all users for display names + online status ─────────────
    LaunchedEffect(true) {
        db.collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    fetchedUserMap = snapshot.documents
                        .mapNotNull { it.toObject(User::class.java) }
                        .associateBy { it.uid }
                }
            }
    }

    val displayGroups = fetchedGroups.filter {
        it.name.contains(search, ignoreCase = true)
    }
    val activeGroups = fetchedGroups.filter { g ->
        g.members.any { uid -> uid != currentUserId && fetchedUserMap[uid]?.online == true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chats", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MessengerTextPrimary)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MessengerBackground)
                        .clickable { onNewChatClick() },
                    contentAlignment = Alignment.Center
                ) { Text("✏️", fontSize = 16.sp) }
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔍", fontSize = 15.sp)
                    BasicTextField(
                        value = search,
                        onValueChange = { search = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = MessengerTextPrimary),
                        decorationBox = { inner ->
                            if (search.isEmpty()) Text("Search Chatty", color = MessengerTextSecondary, fontSize = 15.sp)
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (isLoading) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MessengerBlue)
            }
        } else {
            // ── Active now stories ─────────────────────────────────────────
            if (activeGroups.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(activeGroups.take(8)) { group ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onGroupClick(group) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(MessengerBlue, MessengerBlueLight)))
                                    .padding(2.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MessengerAvatar(name = group.name, size = 52, online = true)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = group.name.split(" ").first(),
                                fontSize = 11.sp, color = MessengerTextPrimary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 60.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MessengerDivider, thickness = 0.5.dp)
            }

            // ── Group rows ─────────────────────────────────────────────────
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (displayGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (search.isNotEmpty()) "No chats found for \"$search\""
                                else "No chats yet. Start a new conversation!",
                                color = MessengerTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(displayGroups) { group ->
                        val isOnline = group.members
                            .filter { it != currentUserId }
                            .any { fetchedUserMap[it]?.online == true }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGroupClick(group) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MessengerAvatar(name = group.name, size = 56, online = isOnline)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.name,
                                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                        color = MessengerTextPrimary,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Active",
                                        fontSize = 12.sp,
                                        color = if (isOnline) MessengerOnline else MessengerTextSecondary
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                val memberPreview = group.members
                                    .mapNotNull { uid ->
                                        if (uid == currentUserId) null
                                        else fetchedUserMap[uid]?.email?.substringBefore("@")
                                    }
                                    .take(3).joinToString(", ")
                                Text(
                                    text = memberPreview.ifEmpty { "No members" },
                                    fontSize = 13.sp, color = MessengerTextSecondary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 84.dp),
                            color = MessengerDivider,
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }

        // ── Bottom nav ─────────────────────────────────────────────────────
        MessengerBottomNav(
            activeIndex = 0,
            onTabClick  = { index ->
                when (index) {

                    1 -> onPeopleClick() // People tab
                    3 -> onMeClick()
                }
            }
        )
    }
}