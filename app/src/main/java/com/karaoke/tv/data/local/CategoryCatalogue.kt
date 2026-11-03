package com.karaoke.tv.data.local

import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus
import com.karaoke.tv.presentation.screens.MockPopularItem

object CategoryCatalogue {

    private fun createSong(
        id: String,
        artist: String,
        title: String,
        category: String,
        durationSeconds: Int = 210,
        preferredSource: SourceType = SourceType.GOOGLE_DRIVE
    ): Song {
        val version = KaraokeVersion(
            id = "${id}_active",
            songId = id,
            source = preferredSource,
            sourceVideoId = id,
            title = "$artist — $title",
            durationSeconds = durationSeconds,
            quality = "MP4 / HD",
            karaokeScore = 1.0f,
            thumbnailUrl = "",
            embedUrl = "",
            videoUrl = "",
            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
            availabilityStatus = PlaybackAvailability.AVAILABLE
        )

        return Song(
            id = id,
            title = title,
            artist = artist,
            category = category,
            coverUrl = "",
            versions = listOf(version),
            bestVersionId = version.id
        )
    }

    /**
     * Популярные караоке-песни для Главного экрана с чистым концертным оформлением карточек.
     */
    private val allPopularPool = listOf(
        MockPopularItem("Полина Гагарина", "Спектакль окончен", "4:01", "gagarina_spektakl", "", SourceType.GOOGLE_DRIVE, "gagarina_spektakl"),
        MockPopularItem("Руки Вверх!", "18 мне уже", "3:59", "ruki_vverh_18", "", SourceType.GOOGLE_DRIVE, "ruki_vverh_18"),
        MockPopularItem("Король и Шут", "Лесник", "3:12", "kish_lesnik", "", SourceType.GOOGLE_DRIVE, "kish_lesnik"),
        MockPopularItem("Макс Корж", "Жить в кайф", "2:54", "korzh_zhit_v_kaif", "", SourceType.GOOGLE_DRIVE, "korzh_zhit_v_kaif"),
        MockPopularItem("Кино", "Группа крови", "4:23", "kino_gruppa_krovi", "", SourceType.GOOGLE_DRIVE, "kino_gruppa_krovi"),
        MockPopularItem("Кино", "Пачка сигарет", "4:23", "kino_pachka", "", SourceType.GOOGLE_DRIVE, "kino_pachka"),
        MockPopularItem("Баста", "Сансара", "5:35", "basta_sansara", "", SourceType.GOOGLE_DRIVE, "basta_sansara"),
        MockPopularItem("Баста", "Выпускной (Медлячок)", "5:35", "basta_medlyachok", "", SourceType.GOOGLE_DRIVE, "basta_medlyachok"),
        MockPopularItem("Юрий Шатунов", "Седая ночь", "6:35", "shatunov_sedaya", "", SourceType.GOOGLE_DRIVE, "shatunov_sedaya"),
        MockPopularItem("Ласковый май", "Белые розы", "5:42", "shatunov_belye_rozy", "", SourceType.GOOGLE_DRIVE, "shatunov_belye_rozy"),
        MockPopularItem("Би-2", "Полковнику никто не пишет", "4:53", "bi2_polkovnik", "", SourceType.GOOGLE_DRIVE, "bi2_polkovnik"),
        MockPopularItem("ANNA ASTI", "По барам", "3:58", "asti_po_baram", "", SourceType.GOOGLE_DRIVE, "asti_po_baram"),
        MockPopularItem("ANNA ASTI", "Царица", "3:35", "asti_tsaritsa", "", SourceType.GOOGLE_DRIVE, "asti_tsaritsa"),
        MockPopularItem("Звери", "Районы-кварталы", "3:21", "zveri_rayony", "", SourceType.GOOGLE_DRIVE, "zveri_rayony"),
        MockPopularItem("JONY", "Комета", "3:06", "jony_kometa", "", SourceType.GOOGLE_DRIVE, "jony_kometa"),
        MockPopularItem("Мираж", "Музыка нас связала", "4:25", "mirazh_muzyka", "", SourceType.GOOGLE_DRIVE, "mirazh_muzyka"),
        MockPopularItem("МакSим", "Знаешь ли ты", "4:09", "maksim_znaesh", "", SourceType.GOOGLE_DRIVE, "maksim_znaesh"),
        MockPopularItem("Любэ", "Конь (Выйду ночью в поле)", "3:47", "lyube_kon", "", SourceType.GOOGLE_DRIVE, "lyube_kon"),
        MockPopularItem("Иванушки Int.", "Тополиный пух", "3:50", "ivanushki_topol", "", SourceType.GOOGLE_DRIVE, "ivanushki_topol"),
        MockPopularItem("Земфира", "Хочешь", "3:22", "zemfira_hochesh", "", SourceType.GOOGLE_DRIVE, "zemfira_hochesh")
    )

    fun getShuffledPopularKaraoke(count: Int = 6, preferredSource: SourceType? = null): List<MockPopularItem> {
        val src = preferredSource ?: SourceType.GOOGLE_DRIVE
        return allPopularPool.shuffled().take(count).map { item ->
            item.copy(source = src)
        }
    }

    /**
     * Возвращает список проверенных караоке-песен для выбранной категории.
     */
    fun getSongsForCategory(category: String, preferredSource: SourceType? = null): List<Song> {
        val src = preferredSource ?: SourceType.GOOGLE_DRIVE
        val cleanCat = category.trim().replace("\n", " ")
        return when {
            cleanCat.contains("Русский поп", ignoreCase = true) -> listOf(
                createSong("gagarina_spektakl", "Полина Гагарина", "Спектакль окончен", "Русский поп", preferredSource = src),
                createSong("ruki_vverh_18", "Руки Вверх!", "18 мне уже", "Русский поп", preferredSource = src),
                createSong("korzh_zhit_v_kaif", "Макс Корж", "Жить в кайф", "Русский поп", preferredSource = src),
                createSong("asti_tsaritsa", "ANNA ASTI", "Царица", "Русский поп", preferredSource = src),
                createSong("asti_po_baram", "ANNA ASTI", "По барам", "Русский поп", preferredSource = src),
                createSong("jony_kometa", "JONY", "Комета", "Русский поп", preferredSource = src),
                createSong("shatunov_sedaya", "Юрий Шатунов", "Седая ночь", "Русский поп", preferredSource = src),
                createSong("basta_sansara", "Баста", "Сансара", "Русский поп", preferredSource = src)
            )

            cleanCat.contains("Рок", ignoreCase = true) -> listOf(
                createSong("kino_gruppa_krovi", "Кино", "Группа крови", "Рок", preferredSource = src),
                createSong("kish_lesnik", "Король и Шут", "Лесник", "Рок", preferredSource = src),
                createSong("kish_kukla", "Король и Шут", "Кукла колдуна", "Рок", preferredSource = src),
                createSong("kino_pachka", "Кино", "Пачка сигарет", "Рок", preferredSource = src),
                createSong("zemfira_hochesh", "Земфира", "Хочешь", "Рок", preferredSource = src),
                createSong("bi2_polkovnik", "Би-2", "Полковнику никто не пишет", "Рок", preferredSource = src),
                createSong("zveri_rayony", "Звери", "Районы-кварталы", "Рок", preferredSource = src),
                createSong("aria_angel", "Ария", "Беспечный ангел", "Рок", preferredSource = src),
                createSong("splin_serdtse", "Сплин", "Моё сердце", "Рок", preferredSource = src),
                createSong("ddt_osen", "ДДТ", "Что такое осень", "Рок", preferredSource = src)
            )

            cleanCat.contains("Дискотека 90-х", ignoreCase = true) -> listOf(
                createSong("ruki_vverh_18", "Руки Вверх!", "18 мне уже", "Дискотека 90-х", preferredSource = src),
                createSong("ruki_kroshka", "Руки Вверх!", "Крошка моя", "Дискотека 90-х", preferredSource = src),
                createSong("shatunov_belye_rozy", "Ласковый май", "Белые розы", "Дискотека 90-х", preferredSource = src),
                createSong("shatunov_sedaya", "Юрий Шатунов", "Седая ночь", "Дискотека 90-х", preferredSource = src),
                createSong("mirazh_muzyka", "Мираж", "Музыка нас связала", "Дискотека 90-х", preferredSource = src),
                createSong("ivanushki_topol", "Иванушки Int.", "Тополиный пух", "Дискотека 90-х", preferredSource = src),
                createSong("allegrova_leytenant", "Ирина Аллегрова", "Младший лейтенант", "Дискотека 90-х", preferredSource = src),
                createSong("krug_prihodite", "Михаил Круг", "Приходите в мой дом", "Дискотека 90-х", preferredSource = src)
            )

            cleanCat.contains("Хиты 2000-х", ignoreCase = true) -> listOf(
                createSong("maksim_znaesh", "МакSим", "Знаешь ли ты", "Хиты 2000-х", preferredSource = src),
                createSong("zveri_rayony", "Звери", "Районы-кварталы", "Хиты 2000-х", preferredSource = src),
                createSong("bi2_polkovnik", "Би-2", "Полковнику никто не пишет", "Хиты 2000-х", preferredSource = src),
                createSong("splin_serdtse", "Сплин", "Моё сердце", "Хиты 2000-х", preferredSource = src),
                createSong("zemfira_hochesh", "Земфира", "Хочешь", "Хиты 2000-х", preferredSource = src),
                createSong("astudio_uletayu", "А-Студио", "Улетаю", "Хиты 2000-х", preferredSource = src),
                createSong("meladze_salut", "Валерий Меладзе", "Салют, Вера!", "Хиты 2000-х", preferredSource = src),
                createSong("kirkorov_sneg", "Филипп Киркоров", "Снег", "Хиты 2000-х", preferredSource = src)
            )

            cleanCat.contains("Детские", ignoreCase = true) -> listOf(
                createSong("kids_ulybka", "Крошка Енот", "От улыбки станет всем светлей", "Детские песни", preferredSource = src),
                createSong("kids_bremen", "Бременские музыканты", "Ничего на свете лучше нету", "Детские песни", preferredSource = src),
                createSong("kids_chunga", "Чунга-Чанга", "Чунга-Чанга", "Детские песни", preferredSource = src),
                createSong("kids_antoshka", "Антошка", "Антошка, пойдём копать картошку", "Детские песни", preferredSource = src),
                createSong("kids_mamontenok", "Мама для мамонтёнка", "Песенка мамонтёнка", "Детские песни", preferredSource = src),
                createSong("kids_buratino", "Буратино", "Песенка Буратино", "Детские песни", preferredSource = src),
                createSong("kids_kacheli", "Приключения Электроника", "Крылатые качели", "Детские песни", preferredSource = src),
                createSong("kids_elochka", "Детский хор", "В лесу родилась ёлочка", "Детские песни", preferredSource = src),
                createSong("kids_daleko", "Гостья из будущего", "Прекрасное далёко", "Детские песни", preferredSource = src)
            )

            cleanCat.contains("Любовные", ignoreCase = true) -> listOf(
                createSong("basta_medlyachok", "Баста", "Выпускной (Медлячок)", "Любовные хиты", preferredSource = src),
                createSong("maksim_znaesh", "МакSим", "Знаешь ли ты", "Любовные хиты", preferredSource = src),
                createSong("meladze_salut", "Валерий Меладзе", "Салют, Вера!", "Любовные хиты", preferredSource = src),
                createSong("agutin_luna", "Леонид Агутин", "На сиреневой луне", "Любовные хиты", preferredSource = src),
                createSong("kirkorov_sneg", "Филипп Киркоров", "Снег", "Любовные хиты", preferredSource = src),
                createSong("astudio_uletayu", "А-Студио", "Улетаю", "Любовные хиты", preferredSource = src),
                createSong("allegrova_leytenant", "Ирина Аллегрова", "Младший лейтенант", "Любовные хиты", preferredSource = src),
                createSong("krug_prihodite", "Михаил Круг", "Приходите в мой дом", "Любовные хиты", preferredSource = src)
            )

            cleanCat.contains("кино", ignoreCase = true) || cleanCat.contains("мультфильм", ignoreCase = true) -> listOf(
                createSong("lyube_kon", "Любэ", "Конь (Выйду ночью в поле)", "Из кино и мультфильмов", preferredSource = src),
                createSong("kino_ostrov", "Бриллиантовая рука", "Остров невезения", "Из кино и мультфильмов", preferredSource = src),
                createSong("kino_medvedi", "Кавказская пленница", "Песенка о медведях", "Из кино и мультфильмов", preferredSource = src),
                createSong("kids_daleko", "Гостья из будущего", "Прекрасное далёко", "Из кино и мультфильмов", preferredSource = src),
                createSong("kids_kacheli", "Приключения Электроника", "Крылатые качели", "Из кино и мультфильмов", preferredSource = src),
                createSong("kids_bremen", "Бременские музыканты", "Ничего на свете лучше нету", "Из кино и мультфильмов", preferredSource = src)
            )

            else -> listOf(
                createSong("gagarina_spektakl", "Полина Гагарина", "Спектакль окончен", "Хиты", preferredSource = src),
                createSong("ruki_vverh_18", "Руки Вверх!", "18 мне уже", "Хиты", preferredSource = src),
                createSong("kish_lesnik", "Король и Шут", "Лесник", "Хиты", preferredSource = src),
                createSong("korzh_zhit_v_kaif", "Макс Корж", "Жить в кайф", "Хиты", preferredSource = src),
                createSong("kino_gruppa_krovi", "Кино", "Группа крови", "Хиты", preferredSource = src),
                createSong("basta_sansara", "Баста", "Сансара", "Хиты", preferredSource = src)
            )
        }
    }
}
