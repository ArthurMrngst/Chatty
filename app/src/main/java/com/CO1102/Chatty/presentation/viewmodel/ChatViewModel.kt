package com.CO1102.Chatty.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.CO1102.Chatty.domain.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    // ================================
    // 🔥 STATE
    // ================================
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
    val typingUsers: StateFlow<List<String>> = _typingUsers

    // ================================
    // 📩 LOAD MESSAGES
    // ================================
    fun loadMessages(groupId: String) {
        db.collection("groups")
            .document(groupId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->

                if (error != null) return@addSnapshotListener

                _messages.value =
                    snapshot?.documents?.mapNotNull {
                        it.toObject<Message>()?.copy(id = it.id)
                    } ?: emptyList()
            }
    }

    // ================================
    // ✉️ SEND TEXT
    // ================================
    fun sendText(groupId: String, message: Message) {
        if (currentUserId.isEmpty()) return

        db.collection("groups")
            .document(groupId)
            .collection("messages")
            .add(message.copy(senderId = currentUserId))
            .addOnSuccessListener {
                it.update("timestamp", FieldValue.serverTimestamp())
            }
    }

    // ================================
    // 🖼 IMAGE
    // ================================
    fun sendImage(groupId: String, imageUrl: String) {
        sendText(groupId, Message(imageUrl = imageUrl))
    }

    // ================================
    // 🎤 AUDIO
    // ================================
    fun sendAudio(groupId: String, audioUrl: String) {
        sendText(groupId, Message(audioUrl = audioUrl))
    }

    // ================================
    // 📊 POLL
    // ================================
    fun sendPoll(groupId: String, message: Message) {
        sendText(groupId, message)
    }

    fun votePoll(
        groupId: String,
        messageId: String,
        newVotes: Map<String, String>,
        newOptions: Map<String, Long>
    ) {
        db.collection("groups")
            .document(groupId)
            .collection("messages")
            .document(messageId)
            .update(
                mapOf(
                    "pollVotes" to newVotes,
                    "pollOptions" to newOptions
                )
            )
    }

    // ================================
    // 👀 SEEN
    // ================================
    fun markSeen(groupId: String, messageId: String) {
        if (currentUserId.isEmpty()) return

        db.collection("groups")
            .document(groupId)
            .collection("messages")
            .document(messageId)
            .update("seenBy", FieldValue.arrayUnion(currentUserId))
    }

    // ================================
    // ⌨️ TYPING
    // ================================
    fun setTyping(groupId: String, isTyping: Boolean) {
        if (currentUserId.isEmpty()) return

        val ref = db.collection("groups")
            .document(groupId)
            .collection("typing")
            .document(currentUserId)

        if (isTyping) {
            ref.set(mapOf("typing" to true))
        } else {
            ref.delete()
        }
    }

    fun listenTyping(groupId: String) {
        db.collection("groups")
            .document(groupId)
            .collection("typing")
            .addSnapshotListener { snapshot, _ ->
                _typingUsers.value =
                    snapshot?.documents?.map { it.id } ?: emptyList()
            }
    }

    // ================================
    // ✏️ EDIT
    // ================================
    fun editMessage(groupId: String, messageId: String, newText: String) {
        db.collection("groups")
            .document(groupId)
            .collection("messages")
            .document(messageId)
            .update("text", newText)
    }

    // ================================
    // 🗑 DELETE
    // ================================
    fun deleteMessage(groupId: String, messageId: String) {
        db.collection("groups")
            .document(groupId)
            .collection("messages")
            .document(messageId)
            .delete()
    }

    // ================================
    // 🔇 MUTE USER
    // ================================
    fun toggleMuteUser(groupId: String, targetUserId: String) {

        val groupRef = db.collection("groups").document(groupId)

        db.runTransaction { transaction ->

            val snapshot = transaction.get(groupRef)

            val muted =
                (snapshot.get("mutedUsers") as? List<*>)
                    ?.map { it.toString() }
                    ?.toMutableList()
                    ?: mutableListOf()

            if (muted.contains(targetUserId)) {
                muted.remove(targetUserId)
            } else {
                muted.add(targetUserId)
            }

            transaction.update(groupRef, "mutedUsers", muted)
        }
    }

    // ================================
    // 👑 ADMIN SYSTEM
    // ================================
    fun addAdmin(groupId: String, userId: String) {
        val groupRef = db.collection("groups").document(groupId)

        groupRef.update(
            "admins",
            FieldValue.arrayUnion(userId)
        )
    }

    fun removeAdmin(groupId: String, userId: String) {
        val groupRef = db.collection("groups").document(groupId)

        groupRef.update(
            "admins",
            FieldValue.arrayRemove(userId)
        )
    }

    // ================================
    // 🟢 ONLINE STATUS
    // ================================
    fun setOnlineStatus(isOnline: Boolean) {
        if (currentUserId.isEmpty()) return

        db.collection("users")
            .document(currentUserId)
            .update(
                mapOf(
                    "online" to isOnline,
                    "lastSeen" to FieldValue.serverTimestamp()
                )
            )
    }
}