package com.CO1102.Chatty

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupChatScreen(
    group: Group,
    onBackClick: () -> Unit
) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: return

    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf(listOf<Message>()) }
    var messageText by remember { mutableStateOf("") }
    var userMap by remember { mutableStateOf(mapOf<String, String>()) }
    var typingUsers by remember { mutableStateOf(listOf<String>()) }

    // 🔥 Load messages (REAL-TIME)
    LaunchedEffect(group.id) {
        db.collection("groups")
            .document(group.id)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->

                if (snapshot != null) {
                    messages = snapshot.documents
                        .mapNotNull { it.toObject(Message::class.java) }
                }
            }
    }

    // 🔥 Load users (for email display)
    LaunchedEffect(true) {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                userMap = result.documents.associate {
                    val uid = it.getString("uid") ?: ""
                    val email = it.getString("email") ?: ""
                    uid to email
                }
            }
    }

    // 🔥 Auto scroll to newest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(group.id) {

        db.collection("groups")
            .document(group.id)
            .collection("typing")
            .addSnapshotListener { snapshot, _ ->

                if (snapshot != null) {

                    val typingList = snapshot.documents
                        .filter { it.getBoolean("typing") == true }
                        .mapNotNull { it.id }
                        .filter { it != currentUserId } // ❗ exclude yourself

                    typingUsers = typingList
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {

        // 🔙 HEADER
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(end = 8.dp),
                style = MaterialTheme.typography.headlineMedium
            )

            Column {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.headlineSmall
                )

                val memberNames = group.members.map {
                    userMap[it] ?: "Unknown"
                }.joinToString(", ")

                Text(
                    text = memberNames,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (typingUsers.isNotEmpty()) {

            val typingText = typingUsers.joinToString(", ")

            Text(
                text = "$typingText is typing...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 💬 EMPTY STATE
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No messages yet...")
            }
        } else {

            // 💬 MESSAGE LIST
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f)
            ) {

                items(messages) { message ->

                    val isMe = message.senderId == currentUserId

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment =
                            if (isMe) Alignment.End else Alignment.Start
                    ) {

                        // 👤 Sender name
                        if (!isMe) {
                            Text(
                                text = userMap[message.senderId] ?: "Unknown",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Surface(
                            color =
                                if (isMe)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .padding(4.dp)
                                .widthIn(max = 250.dp)
                        ) {

                            Column(modifier = Modifier.padding(8.dp)) {

                                Text(
                                    text = message.text,
                                    color =
                                        if (isMe)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        .format(Date(message.timestamp)),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // ✍️ INPUT AREA
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = messageText,
                onValueChange = {
                    messageText = it

                    db.collection("groups")
                        .document(group.id)
                        .collection("typing")
                        .document(currentUserId)
                        .set(mapOf("typing" to it.isNotEmpty()))
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type message") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (messageText.isNotBlank()) {

                        val message = Message(
                            senderId = currentUserId,
                            text = messageText,
                            timestamp = System.currentTimeMillis()
                        )

                        // 🔥 SEND MESSAGE
                        db.collection("groups")
                            .document(group.id)
                            .collection("messages")
                            .add(message)

                        // 🔥 STOP TYPING
                        db.collection("groups")
                            .document(group.id)
                            .collection("typing")
                            .document(currentUserId)
                            .set(mapOf("typing" to false))

                        messageText = ""
                    }
                }
            )    {
                Text("Send")
            }
        }
    }
}