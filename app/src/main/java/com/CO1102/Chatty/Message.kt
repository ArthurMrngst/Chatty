package com.CO1102.Chatty

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val gifUrl: String = "",
    val timestamp: Long = 0,
    val replyToText: String = "",
    val replyToSender: String = ""
)

