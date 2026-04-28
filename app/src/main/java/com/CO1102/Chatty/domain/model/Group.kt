package com.CO1102.Chatty.domain.model

data class Group(
    val id: String = "",
    val name: String = "",
    val members: List<String> = listOf(),
    val admins: List<String> = listOf(),
    val mutedUsers: List<String> = listOf(),
    val isDirectChat: Boolean = false
)