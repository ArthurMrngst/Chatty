package com.CO1102.Chatty

data class Group(
    val id: String = "",
    val name: String = "",
    val members: List<String> = listOf()
)