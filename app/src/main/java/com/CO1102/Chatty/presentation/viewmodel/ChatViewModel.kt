package com.CO1102.Chatty.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.CO1102.Chatty.domain.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.CO1102.Chatty.domain.model.User
import android.net.Uri
import java.io.File
import com.google.firebase.storage.FirebaseStorage

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
                        it.toObject(Message::class.java)?.copy(id = it.id)
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
    fun sendImage(groupId: String, uri: Uri) {
        val fileName = "img_${System.currentTimeMillis()}.jpg"

        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child("chat_images/$fileName")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->

                    val message = Message(
                        imageUrl = downloadUrl.toString()
                    )

                    sendText(groupId, message)   // ✅ USE THIS (you don't have sendMessageToFirestore)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("UPLOAD", "Image upload failed", e)
            }
    }

    // ================================
    // 🎤 AUDIO
    // ================================
    fun sendAudio(groupId: String, filePath: String) {
        val file = File(filePath)
        val uri = Uri.fromFile(file)

        val fileName = "audio_${System.currentTimeMillis()}.3gp"

        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child("chat_audio/$fileName")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->

                    val message = Message(
                        audioUrl = downloadUrl.toString()
                    )

                    sendText(groupId, message)   // ✅ FIXED
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("UPLOAD", "Audio upload failed", e)
            }
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
    // ================================
// 👥 USER MAP
// ================================
    private val _userMap = MutableStateFlow<Map<String, User>>(emptyMap())
    val userMap: StateFlow<Map<String, User>> = _userMap

    fun loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                _userMap.value = result.documents.mapNotNull { doc ->
                    doc.toObject(User ::class.java)
                }.associateBy { it.uid }
            }
    }

    fun nominateAdmin(groupId: String, nomineeId: String, nomineeDisplay: String) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val nominatorId = auth.currentUser?.uid ?: return
        val nominatorDisplay = auth.currentUser?.email ?: "Someone"

        // Check: don't allow duplicate open elections for the same person
        db.collection("groups").document(groupId)
            .collection("messages")
            .whereEqualTo("electionNominee", nomineeId)
            .whereEqualTo("electionResolved", false)
            .get()
            .addOnSuccessListener { existing ->
                if (!existing.isEmpty) return@addOnSuccessListener // already an open vote

                val electionMessage = hashMapOf(
                    "senderId"                to nominatorId,
                    "text"                    to "",
                    "electionNominee"         to nomineeId,
                    "electionNomineeDisplay"  to nomineeDisplay,
                    "electionNominator"       to nominatorId,
                    "electionNominatorDisplay" to nominatorDisplay,
                    "electionYesVotes"        to emptyList<String>(),
                    "electionNoVotes"         to emptyList<String>(),
                    "electionResolved"        to false,
                    "timestamp"               to FieldValue.serverTimestamp()
                )

                db.collection("groups").document(groupId)
                    .collection("messages")
                    .add(electionMessage)
            }
    }
    fun voteElection(
        groupId: String,
        messageId: String,
        voterId: String,
        approve: Boolean,
        totalMembers: Int  // pass group.members.size from GroupChatScreen
    ) {
        val db = FirebaseFirestore.getInstance()
        val msgRef = db.collection("groups").document(groupId)
            .collection("messages").document(messageId)
        val groupRef = db.collection("groups").document(groupId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(msgRef)

            // Guard: already resolved or already voted
            if (snapshot.getBoolean("electionResolved") == true) return@runTransaction
            val yesVotes = snapshot.get("electionYesVotes") as? List<*> ?: emptyList<String>()
            val noVotes  = snapshot.get("electionNoVotes")  as? List<*> ?: emptyList<String>()
            if (yesVotes.contains(voterId) || noVotes.contains(voterId)) return@runTransaction

            // Record vote
            val voteField = if (approve) "electionYesVotes" else "electionNoVotes"
            transaction.update(msgRef, voteField, FieldValue.arrayUnion(voterId))

            // Check majority
            val newYesCount = yesVotes.size + (if (approve) 1 else 0)
            val majority = (totalMembers / 2) + 1

            if (newYesCount >= majority) {
                val nomineeId = snapshot.getString("electionNominee") ?: return@runTransaction
                // Resolve the election
                transaction.update(msgRef, "electionResolved", true)
                // Promote to admin in group doc
                transaction.update(groupRef, "admins", FieldValue.arrayUnion(nomineeId))
            }
        }
    }
    fun muteUser(groupId: String, targetUserId: String, minutes: Int) {
        val mutedUntil = com.google.firebase.Timestamp(
            java.util.Date(System.currentTimeMillis() + minutes * 60 * 1000L)
        )
        val groupRef = db.collection("groups").document(groupId)
        val muteRef  = db.collection("groups").document(groupId)
            .collection("mutes").document(targetUserId)

        db.runBatch { batch ->
            batch.set(muteRef, mapOf(
                "userId"     to targetUserId,
                "mutedUntil" to mutedUntil,
                "mutedAt"    to FieldValue.serverTimestamp()
            ))
            batch.update(groupRef, "mutedUsers", FieldValue.arrayUnion(targetUserId))
        }
    }

    fun unmuteUser(groupId: String, targetUserId: String) {
        val groupRef = db.collection("groups").document(groupId)
        val muteRef  = db.collection("groups").document(groupId)
            .collection("mutes").document(targetUserId)

        db.runBatch { batch ->
            batch.delete(muteRef)
            batch.update(groupRef, "mutedUsers", FieldValue.arrayRemove(targetUserId))
        }
    }

    fun checkAndAutoUnmute(groupId: String) {
        val now = com.google.firebase.Timestamp.now()
        db.collection("groups").document(groupId)
            .collection("mutes")
            .whereLessThan("mutedUntil", now)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val userId = doc.getString("userId") ?: return@forEach
                    unmuteUser(groupId, userId)
                }
            }
    }

    fun observeMuteStatus(groupId: String, userId: String, onResult: (Boolean) -> Unit) {
        db.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, _ ->
                val mutedList = snapshot?.get("mutedUsers") as? List<String> ?: emptyList()
                onResult(mutedList.contains(userId))
            }
    }

    fun reactToMessage(groupId: String, messageId: String, userId: String, emoji: String) {
        val ref = db.collection("groups").document(groupId)
            .collection("messages").document(messageId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val reactions = (snapshot.get("reactions") as? MutableMap<String, String>)
                ?: mutableMapOf()
            if (reactions[userId] == emoji) reactions.remove(userId)
            else reactions[userId] = emoji
            transaction.update(ref, "reactions", reactions)
        }
    }
}