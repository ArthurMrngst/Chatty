package com.CO1102.Chatty

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val gifUrl: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    val replyToText: String = "",
    val replyToSender: String = "",
    val edited: Boolean = false,
    val reactions: Map<String, String> = emptyMap(),
    val seenBy: List<String> = emptyList(),
    val audioUrl: String = "",
    val audioDuration: Long = 0,
    val pollQuestion: String = "",
    val pollOptions: Map<String, Long> = mapOf(),
    val pollVotes: Map<String, String> = mapOf(),
)

