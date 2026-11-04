package com.karaoke.tv.data.local

import com.karaoke.tv.data.local.entity.toDomain
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.presentation.screens.MockPopularItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Каталог категорий караоке, работающий исключительно с реальной базой Room (Google Drive).
 * Все фальшивые заглушки и невалидные ID удалены.
 */
object CategoryCatalogue {

    private var database: KaraokeDatabase? = null
    private val categoryCache = ConcurrentHashMap<String, List<Song>>()

    fun initDatabase(db: KaraokeDatabase) {
        database = db
    }

    private val categoryKeywords = mapOf(
        "Рок" to listOf(
            "король и шут", "кино", "цой", "ленинград", "сектор газа", "би 2", "ария", "земфира",
            "звери", "сплин", "ддт", "наутилус", "агата кристи", "чайф", "чиж", "мумий тролль",
            "кукрыниксы", "княzz", "пикник", "linkin park", "rammstein", "nirvana", "scorpions",
            "queen", "metallica", "ac dc"
        ),
        "Русский поп" to listOf(
            "гагарина", "баста", "руки вверх", "артик", "асти", "лобода", "зиверт",
            "билан", "барских", "нюша", "меладзе", "лазарев", "клава кока", "мот",
            "jony", "хаммали", "бузова", "крид", "время и стекло", "темникова", "елка"
        ),
        "Дискотека 90-х" to listOf(
            "руки вверх", "ласковый май", "мираж", "комбинация", "иванушки", "отпетые мошенники",
            "шура", "кар мэн", "аллегрова", "на на", "стрелки", "hi fi", "демо",
            "вирус", "краски", "шатунов", "кай метов", "губин", "божья коровка"
        ),
        "Хиты 2000-х" to listOf(
            "звери", "дискотека авария", "глюкоза", "фабрика", "корни", "тату", "рефлекс",
            "блестящие", "корж", "максим", "градусы", "бумбокс", "винтаж", "потап",
            "серебро", "serebro", "братья грим", "город 312", "виа гра"
        ),
        "Детские песни" to listOf(
            "детск", "песенка", "улыбка", "крокодил гена", "чебурашка", "мамонтенок",
            "бременские", "антошка", "синий трактор", "фиксики", "барбарики",
            "крылатые качели", "прекрасное далеко", "лесной олень", "облака"
        )
    )

    suspend fun getAllCategorySongs(db: KaraokeDatabase): Map<String, List<Song>> = withContext(Dispatchers.IO) {
        database = db
        val result = mutableMapOf<String, List<Song>>()
        for ((catName, _) in categoryKeywords) {
            result[catName] = queryCategorySongs(db, catName)
        }
        result
    }

    /**
     * Возвращает список проверенных реальных караоке-песен для выбранной категории.
     */
    fun getSongsForCategory(category: String, preferredSource: SourceType? = null): List<Song> {
        val cached = categoryCache[category]
        if (!cached.isNullOrEmpty()) return cached

        val db = database ?: return emptyList()
        return runBlocking(Dispatchers.IO) {
            queryCategorySongs(db, category)
        }
    }

    private suspend fun queryCategorySongs(db: KaraokeDatabase, category: String): List<Song> {
        val cached = categoryCache[category]
        if (!cached.isNullOrEmpty()) return cached

        val keywords = categoryKeywords.entries.firstOrNull {
            category.contains(it.key, ignoreCase = true) || it.key.contains(category, ignoreCase = true)
        }?.value ?: listOf(category.lowercase())

        val matchedSongs = mutableListOf<Song>()
        val seenIds = mutableSetOf<String>()

        for (kw in keywords) {
            val entities = db.songDao().searchSongsNormalized(kw)
            for (entity in entities) {
                if (seenIds.add(entity.id)) {
                    val versions = db.karaokeVersionDao().getVersionsForSong(entity.id)
                    val matchingVer = versions.firstOrNull { it.source == SourceType.GOOGLE_DRIVE } ?: versions.firstOrNull()
                    if (matchingVer != null) {
                        matchedSongs.add(
                            Song(
                                id = entity.id,
                                title = entity.title,
                                artist = entity.artist,
                                category = category,
                                coverUrl = matchingVer.thumbnailUrl.ifBlank { entity.coverUrl },
                                versions = versions.map { it.toDomain() },
                                bestVersionId = matchingVer.id
                            )
                        )
                    }
                }
                if (matchedSongs.size >= 30) break
            }
            if (matchedSongs.size >= 30) break
        }

        // Если совпадений мало, дополнить реальными песнями из общего каталога
        if (matchedSongs.size < 8) {
            val all = db.songDao().getAllSongs().shuffled()
            for (entity in all) {
                if (seenIds.add(entity.id)) {
                    val versions = db.karaokeVersionDao().getVersionsForSong(entity.id)
                    val matchingVer = versions.firstOrNull { it.source == SourceType.GOOGLE_DRIVE } ?: versions.firstOrNull()
                    if (matchingVer != null) {
                        matchedSongs.add(
                            Song(
                                id = entity.id,
                                title = entity.title,
                                artist = entity.artist,
                                category = category,
                                coverUrl = matchingVer.thumbnailUrl.ifBlank { entity.coverUrl },
                                versions = versions.map { it.toDomain() },
                                bestVersionId = matchingVer.id
                            )
                        )
                    }
                }
                if (matchedSongs.size >= 16) break
            }
        }

        categoryCache[category] = matchedSongs
        return matchedSongs
    }

    /**
     * Популярные караоке-песни для Главного экрана с реальными Google Drive источниками.
     */
    fun getShuffledPopularKaraoke(count: Int = 6, preferredSource: SourceType? = null): List<MockPopularItem> {
        val cached = categoryCache.values.flatten().distinctBy { it.id }
        if (cached.isNotEmpty()) {
            return cached.shuffled().take(count).map { song ->
                val bestVer = song.versions.firstOrNull()
                MockPopularItem(
                    title = song.title,
                    artist = song.artist,
                    duration = "MP4",
                    songId = song.id,
                    coverUrl = song.coverUrl,
                    source = SourceType.GOOGLE_DRIVE,
                    sourceVideoId = bestVer?.sourceVideoId.orEmpty()
                )
            }
        }
        return emptyList()
    }
}
