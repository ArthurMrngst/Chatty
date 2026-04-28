package com.CO1102.Chatty.presentation.screens.chat

import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.CO1102.Chatty.domain.model.Group
import com.CO1102.Chatty.domain.model.Message
import com.CO1102.Chatty.domain.model.User
import com.CO1102.Chatty.presentation.components.AdminActionMenu
import com.CO1102.Chatty.presentation.components.AdminElectionUI
import com.CO1102.Chatty.presentation.components.AudioPlayer
import com.CO1102.Chatty.presentation.viewmodel.ChatViewModel
import com.CO1102.Chatty.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.CO1102.Chatty.presentation.components.CreatePollDialog
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun GroupChatScreen(
    group: Group,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val context       = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val messages     by viewModel.messages.collectAsState()
    val typingUsers  by viewModel.typingUsers.collectAsState()
    val userMap      by viewModel.userMap.collectAsState()

    val listState  = rememberLazyListState()
    val isAdmin    = group.admins.contains(currentUserId)
    val imageMessages = messages.filter { it.imageUrl.isNotEmpty() }


    var messageText       by remember { mutableStateOf("") }
    var showEmoji         by remember { mutableStateOf(false) }
    var replyingMessage   by remember { mutableStateOf<Message?>(null) }
    var messageOptions    by remember { mutableStateOf<Message?>(null) }
    var messageToDelete   by remember { mutableStateOf<Message?>(null) }
    var editingMessage    by remember { mutableStateOf<Message?>(null) }
    var showNominateDialog by remember { mutableStateOf(false) }
    var nominateTargetId   by remember { mutableStateOf("") }
    var nominateTargetName by remember { mutableStateOf("") }
    var recorder    by remember { mutableStateOf<MediaRecorder?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var audioPath   by remember { mutableStateOf("") }
    var isMuted by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var liveMutedUsers by remember { mutableStateOf(group.mutedUsers) }
    var reactionTargetMessage by remember { mutableStateOf<Message?>(null) }
    var showMembers by remember { mutableStateOf(false) }

    // ── Load data ──────────────────────────────────────────────────────────
    LaunchedEffect(group.id) {
        viewModel.loadMessages(group.id)
        viewModel.listenTyping(group.id)
        viewModel.loadUsers()
        viewModel.checkAndAutoUnmute(group.id)

        FirebaseFirestore.getInstance()
            .collection("groups")
            .document(group.id)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    liveMutedUsers = (snapshot.get("mutedUsers") as? List<*>)
                        ?.map { it.toString() } ?: emptyList()
                }
            }
    }
    LaunchedEffect(group.id) {
        viewModel.observeMuteStatus(group.id, currentUserId) { muted ->
            isMuted = muted
        }
    }
    LaunchedEffect(Unit) { viewModel.setOnlineStatus(true) }
    DisposableEffect(Unit) { onDispose { viewModel.setOnlineStatus(false) } }

    // ── Auto scroll ────────────────────────────────────────────────────────
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // ── Image picker ───────────────────────────────────────────────────────
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendImage(group.id, it) }
    }

    // ── Audio recording ────────────────────────────────────────────────────
    fun startRecording() {
        val file = File(context.cacheDir, "${System.currentTimeMillis()}.3gp")
        audioPath = file.absolutePath
        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioPath)
            prepare(); start()
        }
        isRecording = true
    }

    fun stopRecording() {
        recorder?.stop(); recorder?.release(); recorder = null
        isRecording = false
        viewModel.sendAudio(group.id, audioPath)
    }

    // ── UI ─────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // ── HEADER ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 8.dp, end = 12.dp, top = 48.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back arrow
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", fontSize = 22.sp, color = MessengerBlue, fontWeight = FontWeight.Bold)
            }

            MessengerAvatar(name = group.name, size = 40, online = true)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MessengerTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val memberNames = group.members
                    .mapNotNull { userMap[it]?.email?.substringBefore("@") }
                    .take(3)
                    .joinToString(", ")
                Text(
                    text = if (memberNames.isNotEmpty()) memberNames else "Active now",
                    fontSize = 12.sp,
                    color = MessengerOnline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Header action icons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("📞", "📹", "ℹ️").forEach { icon ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MessengerBackground)
                            .clickable {
                                if (icon == "ℹ️") showMembers = true
                            },
                        contentAlignment = Alignment.Center
                    ) { Text(icon, fontSize = 16.sp) }
                }
                // Poll button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { showPollDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📊", fontSize = 20.sp, color = MessengerBlue)
                }
            }
        }

        HorizontalDivider(color = MessengerDivider, thickness = 0.5.dp)

        // ── TYPING INDICATOR ───────────────────────────────────────────────
        if (typingUsers.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MessengerAvatar(name = "?", size = 24)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(MessengerBubbleOther)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("• • •", fontSize = 14.sp, color = MessengerTextSecondary)
                }
            }
        }

        // ── MUTED WARNING ──────────────────────────────────────────────────
        if (isMuted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3E0))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "🔇 You are muted in this group",
                    fontSize = 13.sp,
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── MESSAGE LIST ───────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .background(Color.White),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                val isMe = message.senderId == currentUserId
                var showMenu by remember { mutableStateOf(false) }

                // Mark seen
                LaunchedEffect(message.id) { viewModel.markSeen(group.id, message.id) }

                MessageRow(
                    message     = message,
                    isMe        = isMe,
                    isAdmin     = isAdmin,
                    senderName  = userMap[message.senderId]?.email?.substringBefore("@") ?: "Unknown",
                    userMap     = userMap,
                    groupId     = group.id,
                    totalMembers = group.members.size,
                    onLongClick = {
                        if (!isMe) {
                            nominateTargetId   = message.senderId
                            nominateTargetName = userMap[message.senderId]?.email ?: "this user"
                            showNominateDialog = true
                            if (isAdmin) showMenu = true
                        } else {
                            messageOptions = message
                        }
                    },
                    onDoubleClick = {
                        viewModel.reactToMessage(group.id, message.id, currentUserId, "❤️")
                    },
                    onShowReactionPicker = {
                        reactionTargetMessage = message
                    },
                    onClick = { if (!isMe) replyingMessage = message },
                    onImageClick = { clickedUrl ->
                        val index = imageMessages.indexOfFirst { it.imageUrl == clickedUrl }
                        if (index != -1) {
                            selectedImageIndex = index
                        }
                    },
                    showMenu    = showMenu,
                    onDismissMenu = { showMenu = false },
                    isSenderMuted = liveMutedUsers.contains(message.senderId),
                    isSenderAdmin = group.admins.contains(message.senderId),
                    onMute   = { minutes -> viewModel.muteUser(group.id, message.senderId, minutes) },
                    onUnmute = { viewModel.unmuteUser(group.id, message.senderId) },
                    onMakeAdmin   = { viewModel.addAdmin(group.id, message.senderId) },
                    onRemoveAdmin = { viewModel.removeAdmin(group.id, message.senderId) },
                    onVoteElection = { approve ->
                        viewModel.voteElection(group.id, message.id, currentUserId, approve, group.members.size)
                    },

                    viewModel   = viewModel
                )
            }
        }

        // ── REPLY PREVIEW ──────────────────────────────────────────────────
        replyingMessage?.let { reply ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MessengerBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(8.dp)
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(36.dp)
                            .background(MessengerBlue)
                    )
                    Column {
                        Text(
                            text = userMap[reply.senderId]?.email?.substringBefore("@") ?: "User",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MessengerBlue
                        )
                        Text(
                            text = if (reply.text.isNotEmpty()) reply.text else "📷 Media",
                            fontSize = 13.sp,
                            color = MessengerTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MessengerDivider)
                        .clickable { replyingMessage = null },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 13.sp, color = MessengerTextSecondary)
                }
            }
        }

        // ── EMOJI BAR ──────────────────────────────────────────────────────
        if (showEmoji) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MessengerBackground)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("😀","😂","🔥","❤️","👍","😮","😢","😡").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 26.sp,
                        modifier = Modifier.clickable { messageText += emoji }
                    )
                }
            }
        }

        // ── INPUT BAR ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Left icons
            listOf("➕", "📷", "🎙️").forEach { icon ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable {
                            when (icon) {
                                "📷" -> imageLauncher.launch("image/*")
                                "🎙️" -> if (!isRecording) startRecording() else stopRecording()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (icon == "🎙️" && isRecording) "⏹️" else icon,
                        fontSize = 20.sp,
                        color = MessengerBlue
                    )
                }
            }

            // Text field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MessengerBackground)
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = messageText,
                    onValueChange = {
                        messageText = it
                        viewModel.setTyping(group.id, it.isNotEmpty())
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 15.sp,
                        color = MessengerTextPrimary
                    ),
                    decorationBox = { innerTextField ->
                        if (messageText.isEmpty()) {
                            Text("Aa", color = MessengerTextSecondary, fontSize = 15.sp)
                        }
                        innerTextField()
                    }
                )
                // Emoji toggle inside field
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable { showEmoji = !showEmoji }
                ) {
                    Text("🙂", fontSize = 20.sp)
                }
            }

            // Send / thumb button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (messageText.isNotBlank())
                            Brush.linearGradient(listOf(MessengerBlue, MessengerBlueLight))
                        else
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable(enabled = !isMuted) {
                        if (messageText.isNotBlank()) {
                            if (editingMessage != null) {
                                viewModel.editMessage(group.id, editingMessage!!.id, messageText)
                                editingMessage = null
                            } else {
                                viewModel.sendText(
                                    group.id,
                                    Message(
                                        text          = messageText,
                                        replyToText   = replyingMessage?.text ?: "",
                                        replyToSender = replyingMessage?.senderId ?: ""
                                    )
                                )
                                replyingMessage = null
                            }
                            messageText = ""
                            viewModel.setTyping(group.id, false)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (messageText.isNotBlank()) "➤" else "👍",
                    fontSize = if (messageText.isNotBlank()) 16.sp else 22.sp,
                    color = if (messageText.isNotBlank()) Color.White else MessengerBlue
                )
            }
        }
    }

    // ── DIALOGS ────────────────────────────────────────────────────────────

    // Message options (edit/delete for own messages)
    messageOptions?.let { msg ->
        AlertDialog(
            onDismissRequest = { messageOptions = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Message Options", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Reactions row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MessengerBackground)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("👍","❤️","😂","😮","😢","😡").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 26.sp,
                                modifier = Modifier.clickable {
                                    // reactToMessage handled in VM or repo
                                    messageOptions = null
                                }
                            )
                        }
                    }
                    if (msg.senderId == currentUserId) {
                        Spacer(Modifier.height(4.dp))
                        OptionRow("✏️  Edit message") {
                            editingMessage = msg
                            messageText = msg.text
                            messageOptions = null
                        }
                        OptionRow("🗑️  Delete message", color = MessengerError) {
                            messageToDelete = msg
                            messageOptions = null
                        }
                    }
                    OptionRow("↩️  Reply") {
                        replyingMessage = msg
                        messageOptions = null
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { messageOptions = null }) {
                    Text("Cancel", color = MessengerTextSecondary)
                }
            }
        )
    }

    // Delete confirmation
    messageToDelete?.let { msg ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete message?", fontWeight = FontWeight.Bold) },
            text  = { Text("This message will be deleted for everyone.", color = MessengerTextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteMessage(group.id, msg.id); messageToDelete = null }) {
                    Text("Delete", color = MessengerError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text("Cancel", color = MessengerTextSecondary)
                }
            }
        )
    }

    // Nominate dialog
    if (showNominateDialog) {
        AlertDialog(
            onDismissRequest = { showNominateDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Nominate as Admin", fontWeight = FontWeight.Bold) },
            text  = { Text("Start a group vote to make $nominateTargetName an admin?", color = MessengerTextSecondary) },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(MessengerBlue, MessengerBlueLight)))
                        .clickable {
                            viewModel.nominateAdmin(group.id, nominateTargetId, nominateTargetName)
                            showNominateDialog = false
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Start Vote", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNominateDialog = false }) {
                    Text("Cancel", color = MessengerTextSecondary)
                }
            }
        )
    }
    // Poll creation dialog
    if (showPollDialog) {
        CreatePollDialog(
            onDismiss = { showPollDialog = false },
            onCreatePoll = { question, options ->
                viewModel.sendPoll(
                    group.id,
                    Message(
                        pollQuestion = question,
                        pollOptions  = options,
                        pollVotes    = emptyMap()
                    )
                )
                showPollDialog = false
            }
        )
    }
    reactionTargetMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { reactionTargetMessage = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = null,
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(30.dp))
                        .background(MessengerBackground)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("❤️","👍","😂","😮","😢","😡").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 32.sp,
                            modifier = Modifier.clickable {
                                viewModel.reactToMessage(
                                    group.id, msg.id, currentUserId, emoji
                                )
                                reactionTargetMessage = null
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
    if (showMembers) {
        AlertDialog(
            onDismissRequest = { showMembers = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Members (${group.members.size})",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    group.members.forEach { memberId ->
                        if (memberId == currentUserId) return@forEach
                        val memberUser  = userMap[memberId]
                        val memberEmail = memberUser?.email ?: memberId
                        val isOnline    = memberUser?.online == true
                        val memberIsAdmin = group.admins.contains(memberId)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showMembers = false }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MessengerAvatar(name = memberEmail, size = 40, online = isOnline)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        memberEmail.substringBefore("@"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MessengerTextPrimary
                                    )
                                    if (memberIsAdmin) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MessengerBlue.copy(alpha = 0.1f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text("Admin", fontSize = 10.sp, color = MessengerBlue)
                                        }
                                    }
                                }
                                Text(
                                    if (isOnline) "Active now" else "Offline",
                                    fontSize = 12.sp,
                                    color = if (isOnline) MessengerOnline else MessengerTextSecondary
                                )
                            }
                            Text("💬", fontSize = 18.sp)
                        }
                        HorizontalDivider(color = MessengerDivider, thickness = 0.5.dp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMembers = false }) {
                    Text("Close", color = MessengerTextSecondary)
                }
            }
        )
    }
    // ── FULLSCREEN IMAGE VIEWER ─────────────────────────────
    selectedImageIndex?.let { startIndex ->

        val pagerState = rememberPagerState(
            initialPage = startIndex,
            pageCount = { imageMessages.size }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {

            // Swipe images
            HorizontalPager(state = pagerState) { page ->

                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(
                            imageMessages[page].imageUrl
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
            }

            // ❌ Close
            Text(
                "✕",
                color = Color.White,
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp)
                    .clickable { selectedImageIndex = null }
            )

            // ⬇️ Download
            Text(
                "⬇️",
                color = Color.White,
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .clickable {
                        val url = imageMessages[pagerState.currentPage].imageUrl
                        downloadImage(context, url)
                    }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MESSAGE ROW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MessageRow(
    message: Message,
    isMe: Boolean,
    isAdmin: Boolean,
    senderName: String,
    userMap: Map<String, User>,
    groupId: String,
    totalMembers: Int,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    isSenderMuted: Boolean,
    isSenderAdmin: Boolean,
    onMute: (Int) -> Unit,
    onUnmute: () -> Unit,
    onMakeAdmin: () -> Unit,
    onRemoveAdmin: () -> Unit,
    onVoteElection: (Boolean) -> Unit,
    onImageClick: (String) -> Unit,
    onDoubleClick: () -> Unit,
    onShowReactionPicker: () -> Unit,
    viewModel: ChatViewModel
) {
    val timeStr = message.timestamp?.toDate()?.let {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
    } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar for others (only show on last message in group)
        if (!isMe) {
            MessengerAvatar(name = senderName, size = 28)
            Spacer(Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Sender name for groups
            if (!isMe) {
                Text(
                    text = senderName,
                    fontSize = 11.sp,
                    color = MessengerTextSecondary,
                    modifier = Modifier.padding(start = 10.dp, bottom = 2.dp)
                )
            }

            // Reply preview
            if (message.replyToText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MessengerBackground)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(28.dp)
                                .background(MessengerBlue)
                        )
                        Column {
                            Text(
                                text = userMap[message.replyToSender]?.email?.substringBefore("@") ?: "User",
                                fontSize = 11.sp, color = MessengerBlue, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = message.replyToText,
                                fontSize = 12.sp, color = MessengerTextSecondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }

            Box {
                // Main bubble
                Surface(
                    color = if (isMe) MessengerBubbleMe else MessengerBubbleOther,
                    shape = RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (isMe) 18.dp else 4.dp,
                        bottomEnd   = if (isMe) 4.dp else 18.dp
                    ),
                    modifier = Modifier.combinedClickable(
                        onClick       = { onClick() },
                        onLongClick   = { onLongClick(); onShowReactionPicker() },
                        onDoubleClick = { onDoubleClick() }
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

                        // Edited badge
                        if (message.edited) {
                            Text(
                                "(edited)", fontSize = 10.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.7f) else MessengerTextSecondary
                            )
                        }

                        // Text
                        if (message.text.isNotEmpty()) {
                            Text(
                                text = message.text,
                                fontSize = 15.sp,
                                color = if (isMe) Color.White else MessengerTextPrimary,
                                lineHeight = 20.sp
                            )
                        }

                        // Image
                        if (message.imageUrl.isNotEmpty()) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(message.imageUrl),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onImageClick(message.imageUrl)
                                    }
                            )
                        }

                        // Audio
                        if (message.audioUrl.isNotEmpty()) {
                            AudioPlayer(url = message.audioUrl)
                        }

                        // Poll
                        if (message.pollQuestion.isNotEmpty()) {
                            PollMessageUI(
                                message = message,
                                currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                onVote = { votes, options ->
                                    viewModel.votePoll(groupId, message.id, votes, options)
                                }
                            )
                        }

                        // Election
                        if (message.electionNominee.isNotEmpty()) {
                            AdminElectionUI(
                                message      = message,
                                currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                totalMembers  = totalMembers,
                                onVote        = onVoteElection
                            )
                        }

                        // Timestamp + seen
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = timeStr,
                                fontSize = 11.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.7f) else MessengerTextSecondary
                            )
                            if (isMe) {
                                Text(
                                    text = if (message.seenBy.isNotEmpty()) "✓✓" else "✓",
                                    fontSize = 11.sp,
                                    color = if (message.seenBy.isNotEmpty())
                                        Color.White
                                    else
                                        Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Reactions bubble
                if (message.reactions.isNotEmpty()) {
                    val grouped = message.reactions.values.groupBy { it }
                    Row(
                        modifier = Modifier
                            .align(if (isMe) Alignment.BottomStart else Alignment.BottomEnd)
                            .offset(x = if (isMe) (-8).dp else 8.dp, y = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        grouped.forEach { (emoji, users) ->
                            Text("$emoji ${users.size}", fontSize = 11.sp)
                        }
                    }
                }

                // Admin dropdown
                if (!isMe && isAdmin) {
                    AdminActionMenu(
                        expanded      = showMenu,
                        onDismiss     = onDismissMenu,
                        isMuted       = isSenderMuted,
                        isAdmin       = isSenderAdmin,
                        onMute        = onMute,
                        onUnmute      = onUnmute,
                        onMakeAdmin   = onMakeAdmin,
                        onRemoveAdmin = onRemoveAdmin
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OPTION ROW HELPER  (used inside the message options dialog)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OptionRow(label: String, color: Color = MessengerTextPrimary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 15.sp, color = color, fontWeight = FontWeight.Medium)
    }
}
fun downloadImage(context: Context, url: String) {

    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle("Chatty Image")
        .setDescription("Downloading image...")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(
            Environment.DIRECTORY_PICTURES,
            "Chatty_${System.currentTimeMillis()}.jpg"
        )

    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    dm.enqueue(request)
}


