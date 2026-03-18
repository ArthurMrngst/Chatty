package com.CO1102.Chatty

import androidx.compose.foundation.background
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

@Composable
fun HomeScreen(
    onUserClick: (User) -> Unit,
    onGroupClick: (Group) -> Unit
)

{
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var users by remember { mutableStateOf(listOf<User>()) }
    var lastMessages by remember { mutableStateOf(mapOf<String, String>()) }
    var groups by remember { mutableStateOf(listOf<Group>()) }

    // 1. Load users
    LaunchedEffect(true) {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                users = result.documents.mapNotNull {
                    it.toObject(User::class.java)
                }.filter { it.uid != currentUser?.uid }
            }
    }

// 2. Listen for last messages AFTER users loaded
    LaunchedEffect(users) {
        users.forEach { user ->

            val chatRoomId =
                if (currentUser!!.uid < user.uid)
                    "${currentUser.uid}_${user.uid}"
                else
                    "${user.uid}_${currentUser.uid}"

            db.collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { snapshot, _ ->

                    val lastMessage = snapshot?.documents
                        ?.firstOrNull()
                        ?.getString("text") ?: ""

                    lastMessages = lastMessages + (user.uid to lastMessage)
                }
        }
    }
    LaunchedEffect(true) {

        val uid = currentUser?.uid ?: return@LaunchedEffect

        db.collection("groups")
            .whereArrayContains("members", uid)   // 🔥 ONLY YOUR GROUPS
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {


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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUserClick(user) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👤 ${user.email}")
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
                        .clickable {
                            onGroupClick(group)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👥 ${group.name}")
                }
                Divider()
            }
        }
    }
}