package com.karaoke.tv.domain.model

enum class PlaybackAvailability {
    AVAILABLE,
    UNAVAILABLE,
    NEEDS_EXTERNAL_APP,
    SOURCE_UNCONFIGURED,
    UNKNOWN
}

enum class VerificationStatus {
    VERIFIED_KARAOKE,
    PROBABLE_KARAOKE,
    UNVERIFIED,
    NOT_KARAOKE
}
