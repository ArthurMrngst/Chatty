package com.CO1102.Chatty

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ChatScreen(user: User) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var messageText by remember { mutableStateOf("") }

    val currentUserId = auth.currentUser?.uid ?: return

    // Generate chatRoomId
    val chatRoomId =
        if (currentUserId < user.uid)
            "${currentUserId}_${user.uid}"
        else
            "${user.uid}_${currentUserId}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Chat with ${user.email}",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Spacer(modifier = Modifier.weight(1f))

        Row {

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") }
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