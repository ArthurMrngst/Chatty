package com.CO1102.Chatty

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupChatScreen(
    group: Group,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: return

    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf(listOf<Message>()) }
    var messageText by remember { mutableStateOf("") }

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFilePath by remember { mutableStateOf("") }
    var showPollDialog by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOptionsInput by remember { mutableStateOf("") }

    // 🎤 Permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // 📷 Image picker
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
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
                    .addOnSuccessListener {
                        it.update("timestamp", FieldValue.serverTimestamp())
                    }
            }
        }
    }

    // 🔥 Load messages
    LaunchedEffect(group.id) {
        db.collection("groups")
            .document(group.id)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", error.message ?: "")
                    return@addSnapshotListener
                }

                messages = snapshot?.documents?.mapNotNull {
                    it.toObject(Message::class.java)?.copy(id = it.id)
                } ?: listOf()
            }
    }

    // 🎙 START RECORD
    fun startRecording() {
        try {
            val file = File(context.cacheDir, "${System.currentTimeMillis()}.3gp")
            audioFilePath = file.absolutePath

            recorder = MediaRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }

            isRecording = true

        } catch (e: Exception) {
            Log.e("Recorder", "Start failed: ${e.message}")
        }
    }

    // 🛑 STOP RECORD
    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }

            recorder = null
            isRecording = false

            if (audioFilePath.isEmpty()) return

            uploadAudio(Uri.fromFile(File(audioFilePath)), group.id) { url ->
                val message = Message(
                    senderId = currentUserId,
                    audioUrl = url,
                    timestamp = null
                )

                db.collection("groups")
                    .document(group.id)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener {
                        it.update("timestamp", FieldValue.serverTimestamp())
                    }
            }

        } catch (e: Exception) {
            Log.e("Recorder", "Stop failed: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {

        // HEADER
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("←", modifier = Modifier.clickable { onBackClick() })
            Spacer(Modifier.width(8.dp))
            Text(group.name)
        }

        Spacer(Modifier.height(10.dp))

        // MESSAGES
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

                    Surface(
                        color = if (isMe)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(4.dp)
                    ) {

                        Column(Modifier.padding(8.dp)) {

                            // IMAGE
                            if (message.imageUrl.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(message.imageUrl),
                                    contentDescription = null,
                                    modifier = Modifier.size(200.dp)
                                )
                            }

                            // 🎧 AUDIO PLAYER
                            if (message.audioUrl.isNotEmpty()) {
                                AudioPlayer(message.audioUrl)
                            }

                            // TEXT
                            if (message.text.isNotEmpty()) {
                                Text(message.text)
                            }

                            Text(
                                message.timestamp?.toDate()?.let {
                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                                } ?: ""
                            )


                            if (message.pollQuestion.isNotEmpty()) {
                                PollMessageUI(message, group.id, currentUserId)
                            }

                        }
                    }
                }
            }
        }
        if (showPollDialog) {
            AlertDialog(
                onDismissRequest = { showPollDialog = false },
                title = { Text("Create Poll") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = pollQuestion,
                            onValueChange = { pollQuestion = it },
                            label = { Text("Question") }
                        )

                        OutlinedTextField(
                            value = pollOptionsInput,
                            onValueChange = { pollOptionsInput = it },
                            label = { Text("Options (comma separated)") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {

                        val optionsList = pollOptionsInput.split(",").map { it.trim() }
                        val optionsMap = optionsList.associateWith { 0L }

                        val message = Message(
                            senderId = currentUserId,
                            pollQuestion = pollQuestion,
                            pollOptions = optionsMap
                        )

                        db.collection("groups")
                            .document(group.id)
                            .collection("messages")
                            .add(message)
                            .addOnSuccessListener {
                                it.update("timestamp", FieldValue.serverTimestamp())
                            }

                        pollQuestion = ""
                        pollOptionsInput = ""
                        showPollDialog = false

                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPollDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // INPUT BAR
        Row(verticalAlignment = Alignment.CenterVertically) {

            Button(onClick = { imageLauncher.launch("image/*") }) {
                Text("📷")
            }

            Spacer(Modifier.width(6.dp))

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(6.dp))
            Button(onClick = { showPollDialog = true }) {
                Text("📊")
            }
            Button(onClick = {
                if (messageText.isNotBlank()) {

                    val message = Message(
                        senderId = currentUserId,
                        text = messageText,
                        timestamp = null
                    )

                    db.collection("groups")
                        .document(group.id)
                        .collection("messages")
                        .add(message)
                        .addOnSuccessListener {
                            it.update("timestamp", FieldValue.serverTimestamp())
                        }

                    messageText = ""
                }
            })

            {
                Text("Send")
            }

            Spacer(Modifier.width(6.dp))

            Button(
                onClick = {
                    val permission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)

                    if (permission != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        return@Button
                    }

                    if (!isRecording) startRecording()
                    else stopRecording()
                }
            ) {
                Text(if (isRecording) "Stop 🎙️" else "Record 🎙️")
            }
        }
    }
}

// 🎧 AUDIO PLAYER
@Composable
fun AudioPlayer(audioUrl: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {

        Button(onClick = {
            if (!isPlaying) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    prepare()
                    start()
                    setOnCompletionListener {
                        isPlaying = false
                    }
                }
                isPlaying = true
            } else {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                isPlaying = false
            }
        }) {
            Text(if (isPlaying) "⏹ Stop" else "▶ Play")
        }

        Spacer(Modifier.width(8.dp))
        Text("Voice message")
    }
}

// 🔥 UPLOAD IMAGE
fun uploadImage(uri: Uri, groupId: String, onSuccess: (String) -> Unit) {
    val ref = com.google.firebase.storage.FirebaseStorage.getInstance()
        .reference.child("images/$groupId/${System.currentTimeMillis()}.jpg")

    ref.putFile(uri).continueWithTask {
        if (!it.isSuccessful) throw it.exception!!
        ref.downloadUrl
    }.addOnSuccessListener { onSuccess(it.toString()) }
}

// 🔥 UPLOAD AUDIO
fun uploadAudio(uri: Uri, groupId: String, onSuccess: (String) -> Unit) {
    val ref = com.google.firebase.storage.FirebaseStorage.getInstance()
        .reference.child("audio/$groupId/${System.currentTimeMillis()}.3gp")

    ref.putFile(uri).continueWithTask {
        if (!it.isSuccessful) throw it.exception!!
        ref.downloadUrl
    }.addOnSuccessListener { onSuccess(it.toString()) }
}
@Composable
fun PollMessageUI(
    message: Message,
    groupId: String,
    currentUserId: String
) {
    val db = FirebaseFirestore.getInstance()

    Column(modifier = Modifier.padding(8.dp)) {

        Text(
            text = message.pollQuestion,
            style = MaterialTheme.typography.titleMedium
        )
        val totalVotes = message.pollOptions.values.sum()

        message.pollOptions.forEach { (option, countLong) ->

            val count = countLong
            val votedOption = message.pollVotes[currentUserId]

            val percentage =
                if (totalVotes == 0L) 0f
                else count.toFloat() / totalVotes.toFloat()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {

                        val newVotes = message.pollVotes.toMutableMap()
                        val newOptions = message.pollOptions.toMutableMap()

                        if (votedOption != null) {
                            newOptions[votedOption] =
                                (newOptions[votedOption] ?: 1L) - 1L
                        }

                        newVotes[currentUserId] = option
                        newOptions[option] =
                            (newOptions[option] ?: 0L) + 1L

                        db.collection("groups")
                            .document(groupId)
                            .collection("messages")
                            .document(message.id)
                            .update(
                                mapOf(
                                    "pollVotes" to newVotes,
                                    "pollOptions" to newOptions
                                )
                            )
                    }
                    .padding(vertical = 6.dp)
            ) {

                Text("$option ($count)")

                LinearProgressIndicator(
                    progress = percentage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

