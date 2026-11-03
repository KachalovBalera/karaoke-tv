package com.karaoke.tv.domain.engine

import com.karaoke.tv.domain.model.VerificationStatus
import java.util.Locale

object KaraokeScorer {

    private val POSITIVE_KEYWORDS = mapOf(
        "караоке" to 0.35f,
        "karaoke" to 0.35f,
        "минусовка с текстом" to 0.30f,
        "минус с текстом" to 0.30f,
        "karaoke lyrics" to 0.30f,
        "karaoke instrumental" to 0.25f,
        "с текстом" to 0.20f,
        "lyrics on screen" to 0.20f,
        "sing along" to 0.20f,
        "backing track" to 0.15f
    )

    private val NEGATIVE_KEYWORDS = mapOf(
        "official video" to -0.60f,
        "официальный клип" to -0.60f,
        "клип" to -0.30f,
        "music video" to -0.40f,
        "концерт" to -0.50f,
        "live" to -0.40f,
        "живой звук" to -0.40f,
        "без текста" to -0.50f,
        "без слов" to -0.30f,
        "урок" to -0.50f,
        "tutorial" to -0.50f,
        "how to play" to -0.50f,
        "разбор на гитаре" to -0.50f,
        "кавер с вокалом" to -0.50f,
        "vocal cover" to -0.50f,
        "reaction" to -0.70f,
        "реакция" to -0.70f,
        "interview" to -0.70f,
        "интервью" to -0.70f
    )

    private val KARAOKE_CHANNELS = listOf(
        "караоке", "karaoke", "минусовки", "x-star", "evolution", "ast", "караоке для всех", "sing king"
    )

    data class KaraokeScoreResult(
        val score: Float, // 0.0 to 1.0
        val isKaraoke: Boolean,
        val verificationStatus: VerificationStatus,
        val reason: String
    )

    fun evaluate(
        title: String,
        description: String = "",
        channelAuthor: String = "",
        userRatingAverage: Float = 0.0f,
        ratingCount: Int = 0
    ): KaraokeScoreResult {
        val combinedText = "${title.lowercase(Locale.getDefault())} ${description.lowercase(Locale.getDefault())}"
        val authorLower = channelAuthor.lowercase(Locale.getDefault())

        var baseScore = 0.10f

        // Check channel author
        if (KARAOKE_CHANNELS.any { authorLower.contains(it) }) {
            baseScore += 0.35f
        }

        // Apply positive weights
        for ((keyword, weight) in POSITIVE_KEYWORDS) {
            if (combinedText.contains(keyword)) {
                baseScore += weight
            }
        }

        // Apply negative weights
        for ((keyword, penalty) in NEGATIVE_KEYWORDS) {
            if (combinedText.contains(keyword)) {
                baseScore += penalty
            }
        }

        // Factor in local user ratings if present
        if (ratingCount > 0) {
            val userFactor = (userRatingAverage - 3.0f) / 5.0f // -0.4 to +0.4
            baseScore += userFactor * 0.3f
        }

        val finalScore = baseScore.coerceIn(0.0f, 1.0f)

        val verification = when {
            ratingCount >= 2 && userRatingAverage >= 4.0f -> VerificationStatus.VERIFIED_KARAOKE
            finalScore >= 0.65f -> VerificationStatus.PROBABLE_KARAOKE
            finalScore < 0.30f -> VerificationStatus.NOT_KARAOKE
            else -> VerificationStatus.UNVERIFIED
        }

        val reason = when (verification) {
            VerificationStatus.VERIFIED_KARAOKE -> "Проверено пользователями: караоке с текстом"
            VerificationStatus.PROBABLE_KARAOKE -> "Высокая вероятность: содержит метки караоке и текст"
            VerificationStatus.UNVERIFIED -> "Требует проверки пользователем"
            VerificationStatus.NOT_KARAOKE -> "Скорее всего не караоке (клип, концерт или оригинал)"
        }

        return KaraokeScoreResult(
            score = finalScore,
            isKaraoke = finalScore >= 0.30f,
            verificationStatus = verification,
            reason = reason
        )
    }
}
