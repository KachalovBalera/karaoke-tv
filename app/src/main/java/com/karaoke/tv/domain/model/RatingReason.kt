package com.karaoke.tv.domain.model

enum class RatingReason(val displayName: String, val isPositive: Boolean) {
    GREAT_TRACK("Отличная минусовка", true),
    READABLE_LYRICS("Хорошо читаемый текст", true),
    GOOD_SOUND("Хороший звук", true),
    LYRICS_DESYNC("Текст не совпадает с музыкой", false),
    VOCAL_PRESENT("Присутствует вокал", false),
    BAD_SOUND("Плохой звук", false),
    BAD_VIDEO("Плохое изображение", false),
    NO_LYRICS("Отсутствует текст", false),
    NOT_PLAYING("Видео не воспроизводится", false),
    NOT_KARAOKE("Это не караоке", false)
}
