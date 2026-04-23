package com.CO1102.Chatty.presentation.screens.chat

import android.Manifest
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.CO1102.Chatty.domain.model.Group
import com.CO1102.Chatty.domain.model.Message
import com.CO1102.Chatty.presentation.components.AdminActionMenu
import com.CO1102.Chatty.presentation.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.File

@Composable
fun GroupChatScreen(
    group: Group,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {

    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val messages by viewModel.messages.collectAsState()
    val typingUsers by viewModel.typingUsers.collectAsState()

    val isAdmin = group.admins.contains(currentUserId)
    val isMuted = group.mutedUsers.contains(currentUserId)

    var messageText by remember { mutableStateOf("") }
    var showEmoji by remember { mutableStateOf(false) }

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var audioPath by remember { mutableStateOf("") }

    // 🔥 LOAD
    LaunchedEffect(group.id) {
        viewModel.loadMessages(group.id)
        viewModel.listenTyping(group.id)
    }

    // 🟢 ONLINE STATUS
    LaunchedEffect(Unit) {
        viewModel.setOnlineStatus(true)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setOnlineStatus(false)
        }
    }

    // 📷 IMAGE PICKER
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendImage(group.id, it.toString())
        }
    }

    // 🎤 RECORD AUDIO
    fun startRecording() {
        val file = File(context.cacheDir, "${System.currentTimeMillis()}.3gp")
        audioPath = file.absolutePath

        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioPath)
            prepare()
            start()
        }
        isRecording = true
    }

    fun stopRecording() {
        recorder?.stop()
        recorder?.release()
        recorder = null
        isRecording = false

        viewModel.sendAudio(group.id, audioPath)
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {

        // 🔹 HEADER
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("←", modifier = Modifier.clickable { onBackClick() })
            Spacer(Modifier.width(8.dp))
            Text(group.name, style = MaterialTheme.typography.titleLarge)
        }

        // 🔇 MUTED WARNING
        if (isMuted) {
            Text("You are muted 🔇", color = MaterialTheme.colorScheme.error)
        }

        // ⌨️ TYPING
        if (typingUsers.isNotEmpty()) {
            Text("Someone is typing...")
        }

        Spacer(Modifier.height(8.dp))

        // 💬 MESSAGE LIST
        LazyColumn(Modifier.weight(1f)) {

            items(messages) { message ->

                val isMe = message.senderId == currentUserId
                var showMenu by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        if (isMe) Alignment.End else Alignment.Start
                ) {

                    Box {

                        Surface(
                            color = if (isMe)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(4.dp)
                        ) {

                            Column(
                                Modifier
                                    .padding(8.dp)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            if (isAdmin && !isMe) {
                                                showMenu = true
                                            }
                                        }
                                    )
                            ) {

                                // TEXT
                                if (message.text.isNotEmpty()) {
                                    Text(message.text)
                                }

                                // IMAGE
                                if (message.imageUrl.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(message.imageUrl),
                                        contentDescription = null,
                                        modifier = Modifier.size(180.dp)
                                    )
                                }

                                // AUDIO
                                if (message.audioUrl.isNotEmpty()) {
                                    Text("🎧 Voice message")
                                }

                                // SEEN
                                if (isMe) {
                                    Text(
                                        if (message.seenBy.isNotEmpty()) "Seen"
                                        else "Sent",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                // POLL
                                if (message.pollQuestion.isNotEmpty()) {
                                    PollMessageUI(
                                        message = message,
                                        currentUserId = currentUserId,
                                        onVote = { votes, options ->
                                            viewModel.votePoll(
                                                group.id,
                                                message.id,
                                                votes,
                                                options
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // 👑 ADMIN MENU
                        if (!isMe) {
                            AdminActionMenu(
                                expanded = showMenu,
                                onDismiss = { showMenu = false },
                                isMuted = group.mutedUsers.contains(message.senderId),
                                isAdmin = group.admins.contains(message.senderId),
                                onMuteToggle = {
                                    viewModel.toggleMuteUser(group.id, message.senderId)
                                },
                                onMakeAdmin = {
                                    viewModel.addAdmin(group.id, message.senderId)
                                },
                                onRemoveAdmin = {
                                    viewModel.removeAdmin(group.id, message.senderId)
                                }
                            )
                        }
                    }

                    // 👀 MARK SEEN
                    LaunchedEffect(message.id) {
                        viewModel.markSeen(group.id, message.id)
                    }
                }
            }
        }

        // 😀 EMOJI BAR
        if (showEmoji) {
            Row {
                listOf("😀","😂","🔥","❤️","👍").forEach {
                    Text(
                        it,
                        modifier = Modifier
                            .padding(6.dp)
                            .clickable { messageText += it }
                    )
                }
            }
        }

        // 🔹 INPUT BAR
        Row(verticalAlignment = Alignment.CenterVertically) {

            Button(onClick = { imageLauncher.launch("image/*") }) {
                Text("📷")
            }

            OutlinedTextField(
                value = messageText,
                onValueChange = {
                    messageText = it
                    viewModel.setTyping(group.id, it.isNotEmpty())
                },
                modifier = Modifier.weight(1f)
            )

            Button(onClick = { showEmoji = !showEmoji }) {
                Text("😀")
            }

            Button(onClick = {
                if (!isMuted && messageText.isNotBlank()) {
                    viewModel.sendText(
                        group.id,
                        Message(text = messageText)
                    )
                    messageText = ""
                }
            }) {
                Text("Send")
            }

            Button(onClick = {
                if (!isRecording) startRecording()
                else stopRecording()
            }) {
                Text(if (isRecording) "Stop 🎙️" else "🎙️")
            }
        }
    }
}