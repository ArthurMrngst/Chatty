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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable

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
    var replyingMessage by remember { mutableStateOf<Message?>(null) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        uri?.let {

            uploadImage(it, group.id) { url ->

                val message = Message(
                    senderId = currentUserId,
                    imageUrl = url,
                    timestamp = System.currentTimeMillis()
                )

                db.collection("groups")
                    .document(group.id)
                    .collection("messages")
                    .add(message)
            }
        }
    }
    

    // 🔥 Load messages (REAL-TIME)
    LaunchedEffect(group.id) {
        db.collection("groups")
            .document(group.id)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->

                if (snapshot != null) {
                    messages = snapshot.documents.mapNotNull { doc ->
                        val msg = doc.toObject(Message::class.java)
                        msg?.copy(id = doc.id)
                    }
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
                                .combinedClickable(
                                    onClick = {
                                        replyingMessage = message
                                    },
                                    onLongClick = {
                                        if (isMe) {
                                            messageToDelete = message
                                        }
                                    }
                                )
                        ) {

                            Column(modifier = Modifier.padding(8.dp)) {

                                if (message.replyToText.isNotEmpty()) {

                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp)) {

                                            Text(
                                                text = userMap[message.replyToSender] ?: "User",
                                                style = MaterialTheme.typography.labelSmall
                                            )

                                            Text(
                                                text = message.replyToText,
                                                maxLines = 1,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }



                                if (message.imageUrl.isNotEmpty()) {

                                    Image(
                                        painter = rememberAsyncImagePainter(message.imageUrl),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(200.dp)
                                            .padding(4.dp)
                                    )

                                } else {

                                    Text(
                                        text = message.text,
                                        color =
                                            if (isMe)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                    )
                                }

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
        messageToDelete?.let { msg ->

            AlertDialog(
                onDismissRequest = { messageToDelete = null },

                title = {
                    Text("Delete message?")
                },

                text = {
                    Text("This message will be deleted for everyone.")
                },

                confirmButton = {
                    TextButton(
                        onClick = {
                            deleteMessage(group.id, msg.id)
                            messageToDelete = null
                        }
                    ) {
                        Text("Delete")
                    }
                },

                dismissButton = {
                    TextButton(
                        onClick = {
                            messageToDelete = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        replyingMessage?.let { reply ->

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Column {
                        Text(
                            text = "Replying to ${userMap[reply.senderId] ?: "User"}",
                            style = MaterialTheme.typography.labelSmall
                        )

                        Text(
                            text = if (reply.text.isNotEmpty()) reply.text else "📷 Image",
                            maxLines = 1
                        )
                    }

                    Text(
                        text = "❌",
                        modifier = Modifier.clickable {
                            replyingMessage = null
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 📷 IMAGE BUTTON
            Button(
                onClick = { launcher.launch("image/*") }
            ) {
                Text("📷")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ✍️ TEXT FIELD
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

            // 📤 SEND BUTTON
            Button(
                onClick = {
                    if (messageText.isNotBlank()) {

                        val message = Message(
                            senderId = currentUserId,
                            text = messageText,
                            timestamp = System.currentTimeMillis(),

                            replyToText = replyingMessage?.text ?: "",
                            replyToSender = replyingMessage?.senderId ?: ""
                        )

                        db.collection("groups")
                            .document(group.id)
                            .collection("messages")
                            .add(message)

                        // stop typing
                        db.collection("groups")
                            .document(group.id)
                            .collection("typing")
                            .document(currentUserId)
                            .set(mapOf("typing" to false))

                        messageText = ""

                        replyingMessage = null
                    }
                }
            ) {
                Text("Send")
            }
        }


    }
}
fun uploadImage(
    uri: Uri,
    groupId: String,
    onSuccess: (String) -> Unit
) {
    val storage = com.google.firebase.storage.FirebaseStorage.getInstance()

    val ref = storage.reference.child(
        "group_chats/$groupId/${System.currentTimeMillis()}.jpg"
    )

    println("🚀 Upload started")

    ref.putFile(uri)
        .addOnSuccessListener {
            println("✅ Upload success")
        }
        .addOnFailureListener {
            println("❌ Upload failed: ${it.message}")
        }
        .continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: Exception("Upload failed")
            }
            ref.downloadUrl
        }
        .addOnSuccessListener { downloadUrl ->
            println("🔥 Download URL: $downloadUrl")
            onSuccess(downloadUrl.toString())
        }
}
fun deleteMessage(groupId: String, messageId: String) {
    val db = FirebaseFirestore.getInstance()

    db.collection("groups")
        .document(groupId)
        .collection("messages")
        .document(messageId)
        .delete()
}