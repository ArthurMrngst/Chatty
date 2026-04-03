package com.CO1102.Chatty

data class User(
    val uid: String = "",
    val email: String = "",
    val online: Boolean = false,
    val lastSeen: com.google.firebase.Timestamp? = null
)