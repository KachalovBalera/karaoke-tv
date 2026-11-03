package com.karaoke.tv.domain.model

data class Rating(
    val id: Long = 0,
    val versionId: String,
    val stars: Int, // 1 to 5
    val reasons: List<RatingReason> = emptyList(),
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
