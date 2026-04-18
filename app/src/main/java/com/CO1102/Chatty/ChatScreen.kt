package com.CO1102.Chatty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
@Composable
fun ChatScreen(user: User, onBackClick: () -> Unit) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val currentUserId = auth.currentUser?.uid ?: return

    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }

    val chatRoomId =
        if (currentUserId < user.uid)
            "${currentUserId}_${user.uid}"
        else
            "${user.uid}_${currentUserId}"

    LaunchedEffect(true) {
        db.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->

                if (snapshot != null) {
                    messages = snapshot.documents
                        .mapNotNull { it.toObject(Message::class.java)}
                        .sortedByDescending { it.timestamp }

                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "←",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { onBackClick() }
            )

            Text(
                text = user.email,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true
        ) {

            items(messages) { message ->

                val isCurrentUser = message.senderId == currentUserId

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        if (isCurrentUser) Arrangement.End
                        else Arrangement.Start
                ) {

                    Surface(
                        color =
                            if (isCurrentUser)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,

                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .padding(6.dp)
                            .widthIn(max = 250.dp)
                    ) {

                        Text(
                            text = message.text,
                            modifier = Modifier.padding(12.dp),
                            color =
                                if (isCurrentUser)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row {

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
                            timestamp = null
                        )

                        db.collection("chats")
                            .document(chatRoomId)
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