package com.CO1102.Chatty

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
    val currentUserId = auth.currentUser?.uid ?: ""
    if (currentUserId.isEmpty()) return

    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf(listOf<Message>()) }
    var messageText by remember { mutableStateOf("") }
    var userMap by remember { mutableStateOf(mapOf<String, String>()) }
    var typingUsers by remember { mutableStateOf(listOf<String>()) }
    var replyingMessage by remember { mutableStateOf<Message?>(null) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var messageOptions by remember { mutableStateOf<Message?>(null) }

    // 🔥 GIF SEND FUNCTION
    // Defined inside the scope to access currentUserId, db, and group.id
    fun sendGif(url: String) {
        val message = Message(
            senderId = currentUserId,
            gifUrl = url,
            timestamp = null
        )
        db.collection("groups")
            .document(group.id)
            .collection("messages")
            .add(message)
            .addOnSuccessListener { doc ->
                doc.update("timestamp", FieldValue.serverTimestamp())
            }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uploadImage(it, group.id) { url ->
                val message = Message(
                    senderId = currentUserId,
                    imageUrl = url,
                    timestamp = null
                )
                db.collection("groups")
                    .document(group.id)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener { doc ->
                        doc.update("timestamp", FieldValue.serverTimestamp())
                    }
            }
        }
    }

    // 🔥 Load messages with error handling
    LaunchedEffect(group.id) {
        db.collection("groups")
            .document(group.id)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            val msg = doc.toObject(Message::class.java)
                            msg?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("Firestore", "Error mapping message: ${e.message}")
                            null
                        }
                    }
                }
            }
    }

    // 🔥 Update Seen Status
    LaunchedEffect(messages) {
        messages.forEach { msg ->
            if (!msg.seenBy.contains(currentUserId)) {
                db.collection("groups")
                    .document(group.id)
                    .collection("messages")
                    .document(msg.id)
                    .update("seenBy", FieldValue.arrayUnion(currentUserId))
                    .addOnFailureListener { Log.e("Firestore", "Update seen failed") }
            }
        }
    }

    // 🔥 Load users for display names/emails
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

    // 🔥 Auto scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 🔥 Typing listener logic
    LaunchedEffect(group.id) {
        db.collection("groups")
            .document(group.id)
            .collection("typing")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    typingUsers = snapshot.documents
                        .filter { it.getBoolean("typing") == true }
                        .mapNotNull { it.id }
                        .filter { it != currentUserId }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // HEADER
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "←",
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(end = 8.dp),
                style = MaterialTheme.typography.headlineMedium
            )

            Column {
                Text(text = group.name, style = MaterialTheme.typography.headlineSmall)
                val memberNames = group.members.map { userMap[it] ?: "Unknown" }.joinToString(", ")
                Text(text = memberNames, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (typingUsers.isNotEmpty()) {
            Text(
                text = "${typingUsers.joinToString(", ")} is typing...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // MESSAGE LIST
        if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No messages yet...")
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                items(messages) { message ->
                    val isMe = message.senderId == currentUserId
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        if (!isMe) {
                            Text(
                                text = userMap[message.senderId] ?: "Unknown",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Surface(
                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .padding(4.dp)
                                .widthIn(max = 250.dp)
                                .combinedClickable(
                                    onClick = { replyingMessage = message },
                                    onLongClick = { messageOptions = message },
                                    onDoubleClick = { reactToMessage(group.id, message.id, currentUserId, "❤️") }
                                )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                if (message.replyToText.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp)) {
                                            Text(text = userMap[message.replyToSender] ?: "User", style = MaterialTheme.typography.labelSmall)
                                            Text(text = message.replyToText, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }

                                if (message.edited) {
                                    Text(text = "(edited)", style = MaterialTheme.typography.labelSmall)
                                }

                                // Priority: GIF > Image > Text
                                if (message.gifUrl.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(message.gifUrl),
                                        contentDescription = "GIF Content",
                                        modifier = Modifier.size(200.dp).padding(4.dp)
                                    )
                                } else if (message.imageUrl.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(message.imageUrl),
                                        contentDescription = "Image Content",
                                        modifier = Modifier.size(200.dp).padding(4.dp)
                                    )
                                }

                                if (message.text.isNotEmpty()) {
                                    Text(
                                        text = message.text,
                                        color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = message.timestamp?.toDate()?.let {
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                                    } ?: "...",
                                    style = MaterialTheme.typography.labelSmall
                                )

                                if (isMe && message.seenBy.isNotEmpty()) {
                                    Text(text = "Seen by ${message.seenBy.size}", style = MaterialTheme.typography.labelSmall)
                                }

                                if (message.reactions.isNotEmpty()) {
                                    Row(modifier = Modifier.padding(top = 4.dp)) {
                                        message.reactions.values.groupBy { it }.forEach { (emoji, users) ->
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.padding(end = 4.dp)
                                            ) {
                                                Text(text = "$emoji ${users.size}", modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // DIALOGS
        messageOptions?.let { msg ->
            AlertDialog(
                onDismissRequest = { messageOptions = null },
                title = { Text("Options") },
                text = {
                    Column {
                        Row {
                            listOf("👍","❤️","😂","😮","😢","😡").forEach { emoji ->
                                Text(text = emoji, modifier = Modifier.padding(8.dp).clickable {
                                    reactToMessage(group.id, msg.id, currentUserId, emoji)
                                    messageOptions = null
                                })
                            }
                        }
                        if (msg.senderId == currentUserId) {
                            Text(text = "Edit", modifier = Modifier.fillMaxWidth().clickable {
                                editingMessage = msg
                                messageText = msg.text
                                messageOptions = null
                            }.padding(8.dp))
                            Text(text = "Delete", modifier = Modifier.fillMaxWidth().clickable {
                                messageToDelete = msg
                                messageOptions = null
                            }.padding(8.dp))
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }

        messageToDelete?.let { msg ->
            AlertDialog(
                onDismissRequest = { messageToDelete = null },
                title = { Text("Delete message?") },
                text = { Text("This message will be deleted for everyone.") },
                confirmButton = { TextButton(onClick = { deleteMessage(group.id, msg.id); messageToDelete = null }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { messageToDelete = null }) { Text("Cancel") } }
            )
        }

        replyingMessage?.let { reply ->
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = "Replying to ${userMap[reply.senderId] ?: "User"}", style = MaterialTheme.typography.labelSmall)
                        Text(text = if (reply.text.isNotEmpty()) reply.text else "📷 Media", maxLines = 1)
                    }
                    Text(text = "❌", modifier = Modifier.clickable { replyingMessage = null })
                }
            }
        }

        // BOTTOM INPUT BAR
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { launcher.launch("image/*") }) { Text("📷") }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = {
                    // Sample GIF for testing - replace with your search logic
                    sendGif("https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJueHNoZDV6bmV0am93Ym9vbmR6bm9vbmR6bm9vbmR6bm9vbmR6biZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/3o7TKMGpxVfJLYEO9G/giphy.gif")
                }
            ) { Text("GIF") }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = messageText,
                onValueChange = {
                    messageText = it
                    db.collection("groups").document(group.id).collection("typing")
                        .document(currentUserId).set(mapOf("typing" to it.isNotEmpty()))
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type message") }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Button(onClick = {
                if (messageText.isNotBlank()) {
                    if (editingMessage != null) {
                        db.collection("groups").document(group.id).collection("messages")
                            .document(editingMessage!!.id).update(mapOf("text" to messageText, "edited" to true))
                        editingMessage = null
                    } else {
                        val message = Message(
                            senderId = currentUserId,
                            text = messageText,
                            replyToText = replyingMessage?.text ?: "",
                            replyToSender = replyingMessage?.senderId ?: ""
                        )
                        db.collection("groups").document(group.id).collection("messages").add(message)
                            .addOnSuccessListener { doc -> doc.update("timestamp", FieldValue.serverTimestamp()) }
                    }
                    messageText = ""
                    replyingMessage = null
                }
            }) { Text(if (editingMessage != null) "Update" else "Send") }
        }
    }
}

// HELPER FUNCTIONS
fun uploadImage(uri: Uri, groupId: String, onSuccess: (String) -> Unit) {
    val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
    val ref = storage.reference.child("group_chats/$groupId/${System.currentTimeMillis()}.jpg")
    ref.putFile(uri).continueWithTask { task ->
        if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
        ref.downloadUrl
    }.addOnSuccessListener { onSuccess(it.toString()) }
}

fun deleteMessage(groupId: String, messageId: String) {
    FirebaseFirestore.getInstance().collection("groups").document(groupId)
        .collection("messages").document(messageId).delete()
}

fun reactToMessage(groupId: String, messageId: String, userId: String, emoji: String) {
    val db = FirebaseFirestore.getInstance()
    val ref = db.collection("groups").document(groupId).collection("messages").document(messageId)
    db.runTransaction { transaction ->
        val snapshot = transaction.get(ref)
        val reactions = snapshot.get("reactions") as? MutableMap<String, String> ?: mutableMapOf()
        if (reactions[userId] == emoji) reactions.remove(userId) else reactions[userId] = emoji
        transaction.update(ref, "reactions", reactions)
    }
}