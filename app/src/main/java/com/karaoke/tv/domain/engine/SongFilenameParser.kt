package com.karaoke.tv.domain.engine

data class ParsedSongInfo(
    val rawFilename: String,
    val artist: String,
    val title: String,
    val normalizedKey: String,
    val quality: String = "1080p",
    val isDuplicateCandidate: Boolean = false
)

object SongFilenameParser {

    private val QUALITY_SUFFIX_REGEX = Regex(
        """\s*[\(\[\{]?(?:4k|1080p?|720p?|hd|uhd|fhd|караоке|karaoke)[\)\]\}]?\s*$""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val DASH_SEPARATORS_REGEX = Regex("""\s+[-—–_]\s+""")

    /**
     * Parses a filename from Google Drive into artist, title, and normalized deduplication key.
     * Examples:
     *   "#2Маши - Босая.mp4" -> Artist: "2Маши", Title: "Босая"
     *   "50 Cent - In Da Club 4K.mp4" -> Artist: "50 Cent", Title: "In Da Club"
     *   "7Б - Молодые ветра.mp4" -> Artist: "7Б", Title: "Молодые ветра"
     */
    fun parse(filename: String): ParsedSongInfo {
        // 1. Remove file extension
        val withoutExt = filename.substringBeforeLast('.').trim()

        // 2. Remove technical leading prefix like '#' if present
        val cleanName = withoutExt.removePrefix("#").trim()

        // 3. Split by the first dash separator: " - ", " — ", " – ", " _ "
        val match = DASH_SEPARATORS_REGEX.find(cleanName)

        val (rawArtist, rawTitle) = if (match != null) {
            val artistPart = cleanName.substring(0, match.range.first).trim()
            val titlePart = cleanName.substring(match.range.last + 1).trim()
            if (artistPart.isNotBlank() && titlePart.isNotBlank()) {
                artistPart to titlePart
            } else {
                "Неизвестен" to cleanName
            }
        } else {
            // No separator found: do not lose the song! Keep entire clean name as title
            "Неизвестен" to cleanName
        }

        // 4. Clean up quality tags from title (e.g. "4K", "1080p", "караоке")
        var cleanTitle = rawTitle
        var previous: String
        do {
            previous = cleanTitle
            cleanTitle = cleanTitle.replace(QUALITY_SUFFIX_REGEX, "").trim()
        } while (cleanTitle != previous)

        val finalArtist = rawArtist.replace(Regex("""\s+"""), " ").trim()
        val finalTitle = if (cleanTitle.isNotBlank()) {
            cleanTitle.replace(Regex("""\s+"""), " ").trim()
        } else {
            rawTitle
        }

        val normalizedKey = buildNormalizedKey(finalArtist, finalTitle)

        val detectedQuality = when {
            Regex("""(?i)\b(?:4k|2160p?|uhd)\b""").containsMatchIn(withoutExt) -> "4K"
            Regex("""(?i)\b(?:1080p?|fhd)\b""").containsMatchIn(withoutExt) -> "1080p"
            Regex("""(?i)\b(?:720p?|hd)\b""").containsMatchIn(withoutExt) -> "720p"
            Regex("""(?i)\b(?:480p?|sd)\b""").containsMatchIn(withoutExt) -> "480p"
            else -> "1080p"
        }

        return ParsedSongInfo(
            rawFilename = filename,
            artist = finalArtist,
            title = finalTitle,
            normalizedKey = normalizedKey,
            quality = detectedQuality
        )
    }

    /**
     * Builds a normalized key for search and deduplication.
     * Replaces 'ё' with 'е', lowercases, removes non-alphanumeric punctuation.
     */
    fun buildNormalizedKey(artist: String, title: String): String {
        val normArtist = normalizeText(artist)
        val normTitle = normalizeText(title)
        return "$normArtist | $normTitle"
    }

    /**
     * Normalizes text for case-insensitive and 'е'/'ё' equivalent search.
     */
    fun normalize(input: String): String = normalizeText(input)

    fun normalizeText(input: String): String {
        return input.lowercase()
            .replace('ё', 'е')
            .replace(Regex("""[^\p{L}\p{Nd}\s]"""), " ") // keep letters and digits
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
