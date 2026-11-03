package com.karaoke.tv.data.local

import androidx.room.TypeConverter
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.QueueStatus
import com.karaoke.tv.domain.model.RatingReason
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus

class Converters {

    @TypeConverter
    fun fromSourceType(source: SourceType): String = source.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = runCatching { SourceType.valueOf(value) }.getOrDefault(SourceType.YOUTUBE)

    @TypeConverter
    fun fromAvailability(status: PlaybackAvailability): String = status.name

    @TypeConverter
    fun toAvailability(value: String): PlaybackAvailability = runCatching { PlaybackAvailability.valueOf(value) }.getOrDefault(PlaybackAvailability.UNKNOWN)

    @TypeConverter
    fun fromVerification(status: VerificationStatus): String = status.name

    @TypeConverter
    fun toVerification(value: String): VerificationStatus = runCatching { VerificationStatus.valueOf(value) }.getOrDefault(VerificationStatus.UNVERIFIED)

    @TypeConverter
    fun fromQueueStatus(status: QueueStatus): String = status.name

    @TypeConverter
    fun toQueueStatus(value: String): QueueStatus = runCatching { QueueStatus.valueOf(value) }.getOrDefault(QueueStatus.WAITING)
}
