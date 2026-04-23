package com.CO1102.Chatty.domain.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val online: Boolean = false,
    val lastSeen: Timestamp? = null
)