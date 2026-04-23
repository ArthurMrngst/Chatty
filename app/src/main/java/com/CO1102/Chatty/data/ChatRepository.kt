package com.CO1102.Chatty.data

import com.google.firebase.firestore.ktx.toObject
import com.CO1102.Chatty.domain.model.Message
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()

    // ================================
    // 🔥 LISTEN MESSAGES
    // ================================
    fun listenMessages(
        groupId: String,
        onResult: (List<Message>) -> Unit
    ) {
        db.collection("groups")
            .document(groupId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->

                if (error != null) return@addSnapshotListener

                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject<Message>()?.copy(id = it.id)
                } ?: emptyList()

                onResult(messages)
            }
    }

    // ================================
    // ✉️ GENERIC SEND MESSAGE
    // ================================
    private fun sendMessage(groupId: String, message: Message) {
        db.collection("groups")
            .document(groupId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                it.update("timestamp", FieldValue.serverTimestamp())
            }
    }

    // ================================
    // ✉️ SEND TEXT
    // ================================
    fun sendTextMessage(groupId: String, message: Message) {
        sendMessage(groupId, message)
    }

    // ================================
    // 🖼 SEND IMAGE
    // ================================
    fun sendImageMessage(groupId: String, message: Message) {
        sendMessage(groupId, message)
    }

    // ================================
    // 🎤 SEND AUDIO
    // ================================
    fun sendAudioMessage(groupId: String, message: Message) {
        sendMessage(groupId, message)
    }

    // ================================
    // 📊 SEND POLL
    // ================================
    fun sendPoll(groupId: String, message: Message) {
        sendMessage(groupId, message)
    }

    // ================================
    // 🗳 VOTE POLL
    // ================================
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
    fun markSeen(groupId: String, messageId: String, userId: String) {
        db.collection("groups")
            .document(groupId)
            .collection("messages")
            .document(messageId)
            .update(
                "seenBy",
                FieldValue.arrayUnion(userId)
            )
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
    fun toggleMuteUser(groupId: String, targetUserId: String) {
        val groupRef = db.collection("groups").document(groupId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(groupRef)

            val muted =
                snapshot.get("mutedUsers") as? MutableList<String> ?: mutableListOf()

            if (muted.contains(targetUserId)) {
                muted.remove(targetUserId)
            } else {
                muted.add(targetUserId)
            }

            transaction.update(groupRef, "mutedUsers", muted)
        }
    }

    fun addAdmin(groupId: String, userId: String) {
        db.collection("groups")
            .document(groupId)
            .update("admins", FieldValue.arrayUnion(userId))
    }

    fun removeAdmin(groupId: String, userId: String) {
        db.collection("groups")
            .document(groupId)
            .update("admins", FieldValue.arrayRemove(userId))
    }
}