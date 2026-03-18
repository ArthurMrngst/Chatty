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

    var messages by remember { mutableStateOf(listOf<Message>()) }
    var messageText by remember { mutableStateOf("") }

    // 🔥 Load messages
    LaunchedEffect(true) {
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

            Text(
                text = group.name,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 💬 MESSAGE LIST
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {

            items(messages.reversed()) { message ->

                val isMe = message.senderId == currentUserId

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        if (isMe) Alignment.End else Alignment.Start
                ) {

                    // 👤 Sender name (important for group chat)
                    if (!isMe) {
                        Text(
                            text = message.senderId, // later we can replace with email
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

        // ✍️ INPUT
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
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

                        db.collection("groups")
                            .document(group.id)
                            .collection("messages")
                            .add(message)

                        messageText = ""
                    }
                }
            ) {
                Text("Send")
            }
        }
    }
}