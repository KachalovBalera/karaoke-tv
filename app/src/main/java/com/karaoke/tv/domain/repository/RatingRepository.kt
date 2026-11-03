package com.karaoke.tv.domain.repository

import com.karaoke.tv.domain.model.Rating
import com.karaoke.tv.domain.model.RatingReason

interface RatingRepository {
    suspend fun addRating(versionId: String, stars: Int, reasons: List<RatingReason>, comment: String = ""): Rating
    suspend fun getRatingsForVersion(versionId: String): List<Rating>
    suspend fun deleteRating(ratingId: Long, versionId: String)
}
