package com.karaoke.tv.data.repository

import com.karaoke.tv.data.local.KaraokeDatabase
import com.karaoke.tv.data.local.entity.RatingEntity
import com.karaoke.tv.domain.model.Rating
import com.karaoke.tv.domain.model.RatingReason
import com.karaoke.tv.domain.model.VerificationStatus
import com.karaoke.tv.domain.repository.RatingRepository

class RatingRepositoryImpl(
    private val database: KaraokeDatabase
) : RatingRepository {

    override suspend fun addRating(
        versionId: String,
        stars: Int,
        reasons: List<RatingReason>,
        comment: String
    ): Rating {
        val reasonsCsv = reasons.joinToString(",") { it.name }
        val entity = RatingEntity(
            versionId = versionId,
            stars = stars.coerceIn(1, 5),
            reasonsCsv = reasonsCsv,
            comment = comment
        )
        val id = database.ratingDao().insertRating(entity)

        // Recompute version average and count
        val avg = database.ratingDao().getAverageRating(versionId) ?: stars.toFloat()
        val count = database.ratingDao().getRatingCount(versionId)

        val version = database.karaokeVersionDao().getVersionById(versionId)
        if (version != null) {
            val hasNotKaraokeReason = reasons.contains(RatingReason.NOT_KARAOKE)
            val updatedVerification = when {
                hasNotKaraokeReason && count >= 1 -> VerificationStatus.NOT_KARAOKE
                count >= 2 && avg >= 4.0f -> VerificationStatus.VERIFIED_KARAOKE
                else -> version.verificationStatus
            }

            database.karaokeVersionDao().updateVersion(
                version.copy(
                    userRatingAverage = avg,
                    ratingCount = count,
                    verificationStatus = updatedVerification
                )
            )
        }

        return Rating(
            id = id,
            versionId = versionId,
            stars = stars,
            reasons = reasons,
            comment = comment,
            timestamp = entity.timestamp
        )
    }

    override suspend fun getRatingsForVersion(versionId: String): List<Rating> {
        val entities = database.ratingDao().getRatingsForVersion(versionId)
        return entities.map { entity ->
            val reasons = if (entity.reasonsCsv.isBlank()) {
                emptyList()
            } else {
                entity.reasonsCsv.split(",").mapNotNull { name ->
                    runCatching { RatingReason.valueOf(name) }.getOrNull()
                }
            }
            Rating(
                id = entity.id,
                versionId = entity.versionId,
                stars = entity.stars,
                reasons = reasons,
                comment = entity.comment,
                timestamp = entity.timestamp
            )
        }
    }

    override suspend fun deleteRating(ratingId: Long, versionId: String) {
        database.ratingDao().deleteRating(ratingId)
        val avg = database.ratingDao().getAverageRating(versionId) ?: 0.0f
        val count = database.ratingDao().getRatingCount(versionId)

        val version = database.karaokeVersionDao().getVersionById(versionId)
        if (version != null) {
            database.karaokeVersionDao().updateVersion(
                version.copy(
                    userRatingAverage = avg,
                    ratingCount = count
                )
            )
        }
    }
}
