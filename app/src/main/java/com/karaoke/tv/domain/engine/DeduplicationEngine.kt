package com.karaoke.tv.domain.engine

import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.Song
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

object DeduplicationEngine {

    private val STOP_WORDS = listOf(
        "караоке", "karaoke", "минусовка", "минус", "instrumental", "инструментал",
        "с текстом", "текст", "lyrics", "lyric", "слова", "hd", "hq", "4k", "1080p", "720p",
        "full hd", "official", "video", "audio", "клип", "песня", "трек", "песни",
        "cover", "кавер", "backing track", "sing along", "sing-along", "remastered",
        "версия", "version", "караоке версия", "karaoke version", "караоке-версия",
        "на русском", "на английском", "субтитры", "subtitles", "subs",
        "бегущая строка", "бегущей строкой", "с бегущей строкой",
        "на экране", "с экраном", "без вокала", "no vocals", "no vocal", "instrumental version",
        "фонограмма", "фонограммы", "минусовки", "плюс", "минус один", "минуса"
    ).sortedByDescending { it.length }

    private val DELIMITER_REGEX = Regex("\\s+[-—–:]\\s+")
    private val BRACKETS_REGEX = Regex("(?:\\[[^\\]]*\\]|\\([^\\)]*\\))")
    private val PUNCTUATION_REGEX = Regex("[«»\"'“”_#*|!?,.]")
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val FUZZY_CLEANUP_REGEX = Regex("[^a-zа-я0-9]")

    // Precompiled single-pass Unicode-aware stop words regex
    private val STOP_WORDS_REGEX by lazy {
        val pattern = STOP_WORDS.joinToString("|") { Regex.escape(it) }
        Regex("(?iu)(?<![\\p{L}\\p{Nd}])(?:$pattern)(?![\\p{L}\\p{Nd}])")
    }

    private val REMIX_PATTERNS = listOf(
        "remix", "ремикс", "mix", "микс", "club mix", "extended", "dance mix"
    )

    private val ACOUSTIC_PATTERNS = listOf(
        "acoustic", "акустика", "акустическая", "piano", "пиано", "фортепиано"
    )

    private val TONE_PATTERNS = listOf(
        "+1", "-1", "+2", "-2", "+3", "-3", "тональность", "key change", "female key", "male key", "на тон выше", "на тон ниже"
    )

    data class ParsedMetadata(
        val artist: String,
        val title: String,
        val isRemix: Boolean,
        val isAcoustic: Boolean,
        val toneShift: String?,
        val rawNormalized: String
    )

    /**
     * Parses and normalizes raw video title into artist and song title
     * Example: "Кино - Группа крови (Караоке с текстом 1080p)" -> ParsedMetadata("Кино", "Группа крови", ...)
     */
    fun parseVideoTitle(rawTitle: String, channelAuthor: String = ""): ParsedMetadata {
        // Normalize all Unicode dash variations to standard ASCII '-'
        var cleaned = rawTitle
            .replace('—', '-')
            .replace('–', '-')
            .replace('−', '-')
            .replace('‒', '-')
            .replace('―', '-')
            .trim()

        // Check for tone changes
        val toneShift = TONE_PATTERNS.find { cleaned.contains(it, ignoreCase = true) }

        // Check for remix
        val isRemix = REMIX_PATTERNS.any { cleaned.contains(it, ignoreCase = true) }

        // Check for acoustic
        val isAcoustic = ACOUSTIC_PATTERNS.any { cleaned.contains(it, ignoreCase = true) }

        // Split by standard delimiters: " - ", " — ", " – ", " : "
        val splitParts = cleaned.split(DELIMITER_REGEX)

        var extractedArtist = ""
        var extractedTitle = ""

        if (splitParts.size >= 2) {
            extractedArtist = splitParts[0].trim()
            extractedTitle = splitParts.subList(1, splitParts.size).joinToString(" - ").trim()
        } else {
            // If no delimiter, check if channel name is an artist (or title might contain artist)
            extractedTitle = cleaned
            if (channelAuthor.isNotBlank() && !channelAuthor.contains("караоке", ignoreCase = true) && !channelAuthor.contains("karaoke", ignoreCase = true)) {
                extractedArtist = channelAuthor.trim()
            }
        }

        // Clean extracted artist and title from brackets and stop words
        extractedArtist = cleanStopWordsAndSymbols(extractedArtist)
        extractedTitle = cleanStopWordsAndSymbols(extractedTitle)

        // If artist ended up empty but title has content, provide sensible fallback
        if (extractedArtist.isBlank()) {
            extractedArtist = "Неизвестный исполнитель"
        }

        return ParsedMetadata(
            artist = normalizeCasing(extractedArtist),
            title = normalizeCasing(extractedTitle),
            isRemix = isRemix,
            isAcoustic = isAcoustic,
            toneShift = toneShift,
            rawNormalized = normalizeForFuzzyComparison("$extractedArtist $extractedTitle")
        )
    }

    private fun cleanStopWordsAndSymbols(input: String): String {
        var text = BRACKETS_REGEX.replace(input, " ")
        text = STOP_WORDS_REGEX.replace(text, " ")
        text = PUNCTUATION_REGEX.replace(text, " ")
        return WHITESPACE_REGEX.replace(text, " ").trim()
    }

    fun normalizeCasing(input: String): String {
        return input.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
    }

    fun normalizeForFuzzyComparison(input: String): String {
        return FUZZY_CLEANUP_REGEX.replace(
            input.lowercase(Locale.getDefault()).replace('ё', 'е'),
            ""
        )
    }

    /**
     * Levenshtein similarity metric from 0.0 (completely different) to 1.0 (exact match)
     */
    fun calculateSimilarity(s1: String, s2: String): Double {
        val str1 = normalizeForFuzzyComparison(s1)
        val str2 = normalizeForFuzzyComparison(s2)

        if (str1 == str2) return 1.0
        if (str1.isEmpty() || str2.isEmpty()) return 0.0

        val maxLen = max(str1.length, str2.length)
        val distance = computeLevenshteinDistance(str1, str2)
        return 1.0 - (distance.toDouble() / maxLen.toDouble())
    }

    private fun computeLevenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        var cost = IntArray(lhs.length + 1) { it }
        var newCost = IntArray(lhs.length + 1)

        for (i in 1..rhs.length) {
            newCost[0] = i
            for (j in 1..lhs.length) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhs.length]
    }

    /**
     * Determines whether two versions belong to the same song entity
     */
    fun isSameSong(meta1: ParsedMetadata, meta2: ParsedMetadata): Boolean {
        // Remixes should NOT be merged with original tracks
        if (meta1.isRemix != meta2.isRemix) return false
        // Acoustics should NOT be merged with originals
        if (meta1.isAcoustic != meta2.isAcoustic) return false

        val artistSim = calculateSimilarity(meta1.artist, meta2.artist)
        val titleSim = calculateSimilarity(meta1.title, meta2.title)

        // Standard order: High similarity on both artist and title
        if (artistSim >= 0.80 && titleSim >= 0.70) return true
        if (artistSim >= 0.65 && titleSim >= 0.85) return true

        val normT1 = normalizeForFuzzyComparison(meta1.title)
        val normT2 = normalizeForFuzzyComparison(meta2.title)
        if (artistSim >= 0.75 && normT1.isNotEmpty() && normT2.isNotEmpty()) {
            if (normT1.contains(normT2) || normT2.contains(normT1)) {
                if (min(normT1.length, normT2.length) >= 4) return true
            }
        }

        // Reversed order check: e.g. "Группа крови - Кино" vs "Кино - Группа крови"
        val crossSim1 = calculateSimilarity(meta1.artist, meta2.title)
        val crossSim2 = calculateSimilarity(meta1.title, meta2.artist)
        if (crossSim1 >= 0.80 && crossSim2 >= 0.70) return true
        if (crossSim1 >= 0.70 && crossSim2 >= 0.80) return true

        val normA1 = normalizeForFuzzyComparison(meta1.artist)
        val normA2 = normalizeForFuzzyComparison(meta2.artist)
        if ((normA1.contains(normT2) || normT2.contains(normA1)) &&
            (normT1.contains(normA2) || normA2.contains(normT1))) {
            if (min(normA1.length, normT2.length) >= 3 && min(normT1.length, normA2.length) >= 3) {
                return true
            }
        }

        return false
    }

    /**
     * Aggregates a flat list of karaoke versions into deduplicated Song entities
     */
    fun groupVersionsIntoSongs(versions: List<KaraokeVersion>): List<Song> {
        val songList = mutableListOf<MutableList<Pair<KaraokeVersion, ParsedMetadata>>>()

        for (version in versions) {
            val meta = parseVideoTitle(version.title, version.channelOrAuthor)
            var matchedGroup: MutableList<Pair<KaraokeVersion, ParsedMetadata>>? = null

            for (group in songList) {
                val sampleMeta = group.first().second
                if (isSameSong(sampleMeta, meta)) {
                    matchedGroup = group
                    break
                }
            }

            if (matchedGroup != null) {
                matchedGroup.add(version to meta)
            } else {
                songList.add(mutableListOf(version to meta))
            }
        }

        return songList.map { group ->
            val firstPair = group.first()
            val parsed = firstPair.second
            val groupedVersions = group.map { it.first }

            // Determine best cover and best title from group
            val bestCover = groupedVersions.firstOrNull { it.thumbnailUrl.isNotBlank() }?.thumbnailUrl ?: ""
            val songId = UUID.nameUUIDFromBytes("${parsed.artist}-${parsed.title}".toByteArray()).toString()

            val updatedVersions = groupedVersions.map { it.copy(songId = songId) }

            Song(
                id = songId,
                title = parsed.title,
                artist = parsed.artist,
                coverUrl = bestCover,
                versions = updatedVersions,
                bestVersionId = updatedVersions.firstOrNull()?.id
            )
        }
    }
}
