package com.CO1102.Chatty

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun HomeScreen(
    onUserClick: (User) -> Unit,
    onGroupClick: (Group) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var users by remember { mutableStateOf(listOf<User>()) }
    var groups by remember { mutableStateOf(listOf<Group>()) }
    var lastMessages by remember { mutableStateOf(mapOf<String, String>()) }

    var showDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var selectedUsers by remember { mutableStateOf(setOf<String>()) }
    var groupLastMessages by remember { mutableStateOf(mapOf<String, String>()) }
    var groupUnreadCounts by remember { mutableStateOf(mapOf<String, Int>()) }

    // 🔥 REAL-TIME USERS (UPDATED)
    LaunchedEffect(true) {
        db.collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    users = snapshot.documents.mapNotNull {
                        it.toObject(User::class.java)
                    }.filter { it.uid != currentUser?.uid }
                }
            }
    }

    // 🔹 Load last messages
    LaunchedEffect(users) {
        val uid = currentUser?.uid ?: return@LaunchedEffect

        users.forEach { user ->

            val chatRoomId =
                if (uid < user.uid) "${uid}_${user.uid}"
                else "${user.uid}_${uid}"

            db.collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { snapshot, _ ->

                    val lastMessage = snapshot?.documents
                        ?.firstOrNull()
                        ?.getString("text") ?: ""

                    lastMessages = lastMessages + (user.uid to lastMessage)
                }
        }
    }

    // 🔹 Load groups
    LaunchedEffect(true) {
        val uid = currentUser?.uid ?: return@LaunchedEffect

        db.collection("groups")
            .whereArrayContains("members", uid)
            .addSnapshotListener { snapshot, error ->

                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    groups = snapshot.documents.mapNotNull {
                        val group = it.toObject(Group::class.java)
                        group?.copy(id = it.id)
                    }
                }
            }
    }

    // 🔹 Group last messages
    LaunchedEffect(groups) {

        groups.forEach { group ->

            db.collection("groups")
                .document(group.id)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { snapshot, _ ->

                    val lastMessage = snapshot?.documents
                        ?.firstOrNull()
                        ?.getString("text") ?: ""

                    groupLastMessages =
                        groupLastMessages + (group.id to lastMessage)
                }
        }
    }

    // 🔹 UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("➕ Create Group")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {

            item {
                Text(
                    text = "Users",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // 🔥 USERS LIST WITH ONLINE STATUS
            items(users) { user ->

                val isOnline = user.online

                val lastSeenText = user.lastSeen?.toDate()?.let {
                    val diff = System.currentTimeMillis() - it.time
                    val minutes = diff / 60000

                    when {
                        minutes < 1 -> "Just now"
                        minutes < 60 -> "$minutes min ago"
                        else -> "${minutes / 60} hr ago"
                    }
                } ?: ""

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUserClick(user) }
                        .padding(vertical = 12.dp)
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Text("👤 ${user.email}")

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (isOnline) "🟢 Online" else "⚫ Offline",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Text(
                        text = lastMessages[user.uid] ?: "No messages yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isOnline && lastSeenText.isNotEmpty()) {
                        Text(
                            text = "Last seen $lastSeenText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider()
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Groups",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(groups) { group ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGroupClick(group) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = group.name.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text("👥 ${group.name}")

                            Text(
                                text = groupLastMessages[group.id] ?: "No messages yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val count = groupUnreadCounts[group.id] ?: 0

                    if (count > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = count.toString(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Divider()
            }
        }
    }

    // 🔹 Create Group Dialog (UNCHANGED)
    if (showDialog) {

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Create Group") },
            text = {

                Column {

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        placeholder = { Text("Enter group name") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select Members:")

                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(users) { user ->

                            val isSelected = selectedUsers.contains(user.uid)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedUsers =
                                            if (isSelected)
                                                selectedUsers - user.uid
                                            else
                                                selectedUsers + user.uid
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedUsers =
                                            if (isSelected)
                                                selectedUsers - user.uid
                                            else
                                                selectedUsers + user.uid
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(user.email)
                            }
                        }
                    }
                }
            },

            confirmButton = {
                Button(
                    onClick = {

                        val uid = currentUser?.uid ?: return@Button

                        if (groupName.isNotBlank()) {

                            val members = selectedUsers.toMutableList()
                            members.add(uid)

                            val newGroup = hashMapOf(
                                "name" to groupName,
                                "members" to members
                            )

                            db.collection("groups").add(newGroup)
                        }

                        groupName = ""
                        selectedUsers = emptySet()
                        showDialog = false
                    }
                ) {
                    Text("Create")
                }
            },

            dismissButton = {
                Button(onClick = {
                    showDialog = false
                    selectedUsers = emptySet()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}