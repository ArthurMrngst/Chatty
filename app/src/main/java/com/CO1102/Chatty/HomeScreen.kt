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

    // 🔹 Load users
    LaunchedEffect(true) {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                users = result.documents.mapNotNull {
                    it.toObject(User::class.java)
                }.filter { it.uid != currentUser?.uid }
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

    // 🔹 Load groups (ONLY groups you belong to)
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

    // 🔹 UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ✅ Create Group Button
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("➕ Create Group")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {

            // 👤 USERS TITLE
            item {
                Text(
                    text = "Users",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // 👤 USERS LIST
            items(users) { user ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUserClick(user) }
                        .padding(vertical = 12.dp)
                ) {
                    Text("👤 ${user.email}")

                    Text(
                        text = lastMessages[user.uid] ?: "No messages yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Divider()
            }

            // 👥 GROUP TITLE
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Groups",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // 👥 GROUP LIST
            items(groups) { group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGroupClick(group) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👥 ${group.name}")
                }
                Divider()
            }
        }
    }

    // 🔹 Create Group Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Create Group") },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Enter group name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = currentUser?.uid ?: return@Button

                        if (groupName.isNotBlank()) {
                            val newGroup = hashMapOf(
                                "name" to groupName,
                                "members" to listOf(uid)
                            )

                            db.collection("groups").add(newGroup)
                        }

                        groupName = ""
                        showDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}