package com.karaoke.tv.domain.model

data class Participant(
    val id: Long = 0,
    val name: String,
    val songsSung: Int = 0,
    val isHost: Boolean = false,
    val lastActive: Long = System.currentTimeMillis()
)
