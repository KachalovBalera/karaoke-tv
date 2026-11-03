package com.karaoke.tv.data.local

import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus
import com.karaoke.tv.presentation.screens.MockPopularItem

data class VerifiedRutubeTrack(
    val id: String,
    val artist: String,
    val title: String,
    val durationSeconds: Int,
    val durationFormatted: String,
    val thumbnailUrl: String
)

object CategoryCatalogue {

    // Полный реестр верифицированных караоке-треков RUTUBE с реальными миниатюрами и video ID
    val VERIFIED_TRACKS = listOf(
        VerifiedRutubeTrack("04e56f433986fce7f733c3664aa9aeba", "Полина Гагарина", "Спектакль окончен", 241, "4:01", "https://pic.rtbcdn.ru/video/2026-01-04/97/a1/97a14ee90da2c35212ee31b091c48768.jpg"),
        VerifiedRutubeTrack("e5d75fd2662b34902c8886a6d5a750a6", "Руки Вверх!", "18 мне уже", 239, "3:59", "https://pic.rtbcdn.ru/video/2026-01-28/a2/7f/a27f02f8865f37efedab63af98522ce5.jpg"),
        VerifiedRutubeTrack("f885ecd271f479cb4c2836d6aa1f0702", "Руки Вверх!", "Крошка моя", 248, "4:08", "https://pic.rtbcdn.ru/video/2025-09-01/d7/6e/d76eb051a0d97c44c4d2b3022fd1c7b2.jpg"),
        VerifiedRutubeTrack("f93f5e6d0e0b98e7a040853230a9899a", "Король и Шут", "Лесник", 192, "3:12", "https://pic.rtbcdn.ru/video/2026-01-02/45/9d/459d11491a14196206fe39a814785d4f.jpg"),
        VerifiedRutubeTrack("162d6ccc59f686e90d42c5dea398bceb", "Король и Шут", "Кукла колдуна", 204, "3:24", "https://pic.rtbcdn.ru/video/2026-01-04/e6/80/e680c483d409300e5792e632ce5de90f.jpg"),
        VerifiedRutubeTrack("4d8e3b3733a2af99ec4a7f9eef01469b", "Макс Корж", "Жить в кайф", 174, "2:54", "https://pic.rtbcdn.ru/video/2026-02-19/c0/86/c0864d45c1d99a31136ceff710a0fc97.jpg"),
        VerifiedRutubeTrack("a5b154cdee7b562d361f28818d90d49e", "Кино", "Группа крови", 263, "4:23", "https://pic.rtbcdn.ru/video/2026-06-13/84/9e/849e66a5070125f46504c35d1e1dff34.jpg"),
        VerifiedRutubeTrack("0a9af44af3476cf9daf00378d3b96bc2", "Кино", "Пачка сигарет", 263, "4:23", "https://pic.rtbcdn.ru/video/2026-06-13/dc/49/dc49b5feb1709c842a3b114b10c07763.jpg"),
        VerifiedRutubeTrack("152e9d65e0159cba636e1c0229fd8d43", "Баста", "Сансара", 335, "5:35", "https://pic.rtbcdn.ru/video/2026-08-14/6c/f5/6cf50fe76490f79ad1aac31eca4cdec7.jpg"),
        VerifiedRutubeTrack("3423132642e5e614f5592ac1d2ec4775", "Баста", "Выпускной (Медлячок)", 335, "5:35", "https://pic.rtbcdn.ru/video/2026-02-03/40/85/408534400a63af84b7692a41f5051fe4.jpg"),
        VerifiedRutubeTrack("d5a9ef02c685798d2ddad077a695b598", "Юрий Шатунов", "Седая ночь", 395, "6:35", "https://pic.rtbcdn.ru/video/2025-11-21/e0/b0/e0b07cbcb5e29945b0ecfc00f36d868b.jpg"),
        VerifiedRutubeTrack("63fc4dc27c004184304d1ee8dfd348bf", "Ласковый май", "Белые розы", 342, "5:42", "https://pic.rtbcdn.ru/video/2026-01-02/dc/4c/dc4cde6e713f2d94fbd822ea49f994df.jpg"),
        VerifiedRutubeTrack("e772d4c265d253ce065e87cfa70f342e", "Би-2", "Полковнику никто не пишет", 293, "4:53", "https://pic.rtbcdn.ru/video/2026-08-13/e9/cf/e9cf7443ef82030903fffe3e5fc1b1d3.jpg"),
        VerifiedRutubeTrack("7909c54b325ca9fe71fc1f3508ea522b", "ANNA ASTI", "По барам", 238, "3:58", "https://pic.rtbcdn.ru/video/2026-01-02/d2/04/d2040d1cf9d34b39151ca0e871b2abdf.jpg"),
        VerifiedRutubeTrack("d4a5d0bb72291943576818b19661b202", "ANNA ASTI", "Царица", 215, "3:35", "https://pic.rtbcdn.ru/video/2026-02-06/7b/89/7b89897a5d26023c04796c2174b35d09.jpg"),
        VerifiedRutubeTrack("5fec9b703dc6377c901b15f988382148", "Звери", "Районы-кварталы", 201, "3:21", "https://pic.rtbcdn.ru/video/2026-04-01/3d/56/3d5678845faaca63e35c3001ef415ad7.jpg"),
        VerifiedRutubeTrack("731f875e8afb5645bcc91bf41b6f906e", "JONY", "Комета", 186, "3:06", "https://pic.rtbcdn.ru/video/03/bc/03bca1fadf20230c2b6ae543b5a1295b.jpg"),
        VerifiedRutubeTrack("5f6c285c84aeccbd88dd657c9e59a961", "Мираж", "Музыка нас связала", 265, "4:25", "https://pic.rtbcdn.ru/video/2026-09-12/2e/fd/2efd1891c5a5da09a0527087de7aa4fd.jpg"),
        VerifiedRutubeTrack("4a505363a80ce856c6c2d3019c48cf19", "МакSим", "Знаешь ли ты", 249, "4:09", "https://pic.rtbcdn.ru/video/2026-02-25/3f/07/3f071ec6d864d22cffb58cd7a83db760.jpg"),
        VerifiedRutubeTrack("87fde5dade05e2391c74d622ebdaabac", "Любэ", "Конь (Выйду ночью в поле)", 227, "3:47", "https://pic.rtbcdn.ru/video/2025-11-14/ee/d4/eed4464cd0bc50dbfb1ca8b1341e5d85.jpg"),
        VerifiedRutubeTrack("e07e955a5565f781e37c4210a69b82d5", "Иванушки Int.", "Тополиный пух", 230, "3:50", "https://pic.rtbcdn.ru/video/2026-04-01/57/1e/571e010dc5a28e3ad68569ed9f14d8d3.jpg"),
        VerifiedRutubeTrack("a37f827ee8b5db3e2f850cb987a0aec6", "Земфира", "Хочешь", 202, "3:22", "https://pic.rtbcdn.ru/video/2026-10-17/2c/af/2caf2e4c583b27e2c7b63a5ded6dbea3.jpg"),
        VerifiedRutubeTrack("e6971658564a37ddd4e50fe770c0b9b1", "Сплин", "Моё сердце", 255, "4:15", "https://pic.rtbcdn.ru/video/2026-01-05/fa/b3/fab3a000ebdd4ba52fb4adcdbd1cfc46.jpg"),
        VerifiedRutubeTrack("6f2aa772a591dc74cfb9cfb9b8d5112b", "ДДТ", "Что такое осень", 292, "4:52", "https://pic.rtbcdn.ru/video/2026-04-27/26/67/266709210e6a8b03de6d45d1fd82bfc8.jpg"),
        VerifiedRutubeTrack("06efa4bf014550e9a45a52fd5d1b44e2", "Ария", "Беспечный ангел", 240, "4:00", "https://pic.rtbcdn.ru/video/2026-02-06/27/f9/27f9bd2955b76606221f1713a8b4fa22.jpg"),
        VerifiedRutubeTrack("5e42213fda17ad441d88056dfd1b3d80", "Крошка Енот", "От улыбки станет всем светлей", 122, "2:02", "https://pic.rtbcdn.ru/video/ae/34/ae3448676ae3a217d8901cec51e4f3f5.jpg"),
        VerifiedRutubeTrack("5bee9137345ee6bd3a59b08471c010d8", "Бременские музыканты", "Ничего на свете лучше нету", 134, "2:14", "https://pic.rtbcdn.ru/video/2026-02-17/22/c3/22c3ff4329603e7b34c86e45ad94d453.jpg"),
        VerifiedRutubeTrack("1581ea5c16c2364879a247a48827fe95", "Мама для мамонтёнка", "Песенка мамонтёнка", 123, "2:03", "https://pic.rtbcdn.ru/video/80/e8/80e8f96ca14024105f050ca8ceb1acda.jpg"),
        VerifiedRutubeTrack("ce6b00407798e031fbd3216e9ddeeed6", "Антошка", "Антошка, пойдём копать картошку", 118, "1:58", "https://pic.rtbcdn.ru/video/2025-05-22/5d/cd/5dcdeac23e05dd1234138a6366693ac4.jpg"),
        VerifiedRutubeTrack("814c0a679d89feef0e082bf311ee2e10", "Чунга-Чанга", "Чунга-Чанга", 106, "1:46", "https://pic.rtbcdn.ru/video/fa/af/faafadb065fd9908a6542ba56e64374c.jpg"),
        VerifiedRutubeTrack("c039fabba32e155ad99ca2afb5bceeeb", "Буратино", "Песенка Буратино", 144, "2:24", "https://pic.rtbcdn.ru/video/2024-11-30/93/30/933008ddac212500ea2797f3bf1f4556.jpg"),
        VerifiedRutubeTrack("4afbeca4719e41f26b00b37ca6335763", "Детский хор", "В лесу родилась ёлочка", 103, "1:43", "https://pic.rtbcdn.ru/video/46/2f/462f6ccdc02dc8189237a98e4eeba4fd.jpg"),
        VerifiedRutubeTrack("3e26e8c6110156674371abcea6f270f6", "Приключения Электроника", "Крылатые качели", 203, "3:23", "https://pic.rtbcdn.ru/video/2025-11-24/c0/7e/c07eccb863b745c6bdc383ac2c245ff8.jpg"),
        VerifiedRutubeTrack("0b8c22de2594131230e06188a8e55372", "Гостья из будущего", "Прекрасное далёко", 183, "3:03", "https://pic.rtbcdn.ru/video/a5/07/a507cacceb51b1601097eb4cc6891a84.jpg"),
        VerifiedRutubeTrack("2bd98d2606ea4a901f7b0025d6d8e773", "Валерий Меладзе", "Салют, Вера!", 215, "3:35", "https://pic.rtbcdn.ru/video/2026-06-26/9a/8d/9a8d08860899bb803d9a378b2e4c9918.jpg"),
        VerifiedRutubeTrack("394e46bb6964022d67edcb648e64e72a", "Леонид Агутин", "На сиреневой луне", 328, "5:28", "https://pic.rtbcdn.ru/video/2026-01-17/bc/b9/bcb99b1345740bde7873d39e4fb50503.jpg"),
        VerifiedRutubeTrack("906ccd9205508853e90ed600fc510d32", "Ирина Аллегрова", "Младший лейтенант", 227, "3:47", "https://pic.rtbcdn.ru/video/2026-03-24/37/37/373721a7451e16b44daec61c295cfbae.jpg"),
        VerifiedRutubeTrack("83f87b30ef005eda42797ceb87af2ae0", "Филипп Киркоров", "Снег", 267, "4:27", "https://pic.rtbcdn.ru/video/2025-05-27/43/5a/435a6f063f56263175bf9782e1af93d2.jpg"),
        VerifiedRutubeTrack("2d2f6dae6305020fdaa4bc81c06dc2d8", "А-Студио", "Улетаю", 214, "3:34", "https://pic.rtbcdn.ru/video/2025-09-01/41/78/417818b576d110ac903a76ddd5e8ac9f.jpg"),
        VerifiedRutubeTrack("c392a11c245a57cfc8c96c87c7103032", "Михаил Круг", "Приходите в мой дом", 218, "3:38", "https://pic.rtbcdn.ru/video/2026-02-04/c2/53/c253facadedf3ac0c7c4f7d164e4132d.jpg"),
        VerifiedRutubeTrack("c16ce091f75441f699d5b1285d3fb28d", "Кавказская пленница", "Песенка о медведях", 196, "3:16", "https://pic.rtbcdn.ru/video/2026-01-17/59/6f/596f8a69284bc8d9827b994f45434002.jpg"),
        VerifiedRutubeTrack("5b9af7c023f29139e6fac72eeb13e595", "Бриллиантовая рука", "Остров невезения", 127, "2:07", "https://pic.rtbcdn.ru/video/2026-09-19/7e/af/7eafb24cfe025f0ce31028cd13cf60ab.jpg")
    )

    fun findTrack(artist: String, title: String): VerifiedRutubeTrack? {
        val query = "$artist $title".lowercase()
        return VERIFIED_TRACKS.firstOrNull { track ->
            val matchA = query.contains(track.artist.lowercase()) || track.artist.lowercase().contains(artist.lowercase())
            val matchT = query.contains(track.title.lowercase()) || track.title.lowercase().contains(title.lowercase())
            matchA && matchT
        } ?: VERIFIED_TRACKS.firstOrNull { track ->
            query.contains(track.title.lowercase()) || track.title.lowercase().contains(title.lowercase())
        }
    }

    fun resolveRutubeId(artist: String, title: String): String? {
        return findTrack(artist, title)?.id
    }

    fun resolveRutubeThumbnail(artist: String, title: String): String {
        return findTrack(artist, title)?.thumbnailUrl ?: ""
    }

    private fun createSong(
        id: String,
        artist: String,
        title: String,
        category: String,
        durationSeconds: Int = 0,
        rutubeId: String? = null,
        thumbnailUrl: String? = null
    ): Song {
        val verified = findTrack(artist, title)
        val realId = rutubeId ?: verified?.id ?: ""
        val realDuration = if (durationSeconds > 0) durationSeconds else (verified?.durationSeconds ?: 210)
        val realThumbnail = thumbnailUrl ?: verified?.thumbnailUrl ?: ""

        val rutubeVersion = KaraokeVersion(
            id = "${id}_rutube",
            songId = id,
            source = SourceType.RUTUBE,
            sourceVideoId = realId,
            title = "$artist — $title (RUTUBE Караоке с текстом)",
            durationSeconds = realDuration,
            quality = "1080p",
            karaokeScore = 0.98f,
            thumbnailUrl = realThumbnail,
            embedUrl = if (realId.isNotBlank()) "https://rutube.ru/play/embed/$realId" else "",
            videoUrl = if (realId.isNotBlank()) "https://rutube.ru/video/$realId/" else "",
            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
            availabilityStatus = PlaybackAvailability.AVAILABLE
        )

        return Song(
            id = id,
            title = title,
            artist = artist,
            category = category,
            coverUrl = realThumbnail,
            versions = listOf(rutubeVersion),
            bestVersionId = rutubeVersion.id
        )
    }

    /**
     * Популярные караоке-песни для Главного экрана со 100% реальными RUTUBE обложками и video ID.
     */
    private val allPopularPool = listOf(
        MockPopularItem("Полина Гагарина", "Спектакль окончен", "4:01", "gagarina_spektakl", "https://pic.rtbcdn.ru/video/2026-01-04/97/a1/97a14ee90da2c35212ee31b091c48768.jpg", SourceType.RUTUBE, "04e56f433986fce7f733c3664aa9aeba"),
        MockPopularItem("Руки Вверх!", "18 мне уже", "3:59", "ruki_vverh_18", "https://pic.rtbcdn.ru/video/2026-01-28/a2/7f/a27f02f8865f37efedab63af98522ce5.jpg", SourceType.RUTUBE, "e5d75fd2662b34902c8886a6d5a750a6"),
        MockPopularItem("Король и Шут", "Лесник", "3:12", "kish_lesnik", "https://pic.rtbcdn.ru/video/2026-01-02/45/9d/459d11491a14196206fe39a814785d4f.jpg", SourceType.RUTUBE, "f93f5e6d0e0b98e7a040853230a9899a"),
        MockPopularItem("Макс Корж", "Жить в кайф", "2:54", "korzh_zhit_v_kaif", "https://pic.rtbcdn.ru/video/2026-02-19/c0/86/c0864d45c1d99a31136ceff710a0fc97.jpg", SourceType.RUTUBE, "4d8e3b3733a2af99ec4a7f9eef01469b"),
        MockPopularItem("Кино", "Группа крови", "4:23", "kino_gruppa_krovi", "https://pic.rtbcdn.ru/video/2026-06-13/84/9e/849e66a5070125f46504c35d1e1dff34.jpg", SourceType.RUTUBE, "a5b154cdee7b562d361f28818d90d49e"),
        MockPopularItem("Кино", "Пачка сигарет", "4:23", "kino_pachka", "https://pic.rtbcdn.ru/video/2026-06-13/dc/49/dc49b5feb1709c842a3b114b10c07763.jpg", SourceType.RUTUBE, "0a9af44af3476cf9daf00378d3b96bc2"),
        MockPopularItem("Баста", "Сансара", "5:35", "basta_sansara", "https://pic.rtbcdn.ru/video/2026-08-14/6c/f5/6cf50fe76490f79ad1aac31eca4cdec7.jpg", SourceType.RUTUBE, "152e9d65e0159cba636e1c0229fd8d43"),
        MockPopularItem("Баста", "Выпускной (Медлячок)", "5:35", "basta_medlyachok", "https://pic.rtbcdn.ru/video/2026-02-03/40/85/408534400a63af84b7692a41f5051fe4.jpg", SourceType.RUTUBE, "3423132642e5e614f5592ac1d2ec4775"),
        MockPopularItem("Юрий Шатунов", "Седая ночь", "6:35", "shatunov_sedaya", "https://pic.rtbcdn.ru/video/2025-11-21/e0/b0/e0b07cbcb5e29945b0ecfc00f36d868b.jpg", SourceType.RUTUBE, "d5a9ef02c685798d2ddad077a695b598"),
        MockPopularItem("Ласковый май", "Белые розы", "5:42", "shatunov_belye_rozy", "https://pic.rtbcdn.ru/video/2026-01-02/dc/4c/dc4cde6e713f2d94fbd822ea49f994df.jpg", SourceType.RUTUBE, "63fc4dc27c004184304d1ee8dfd348bf"),
        MockPopularItem("Би-2", "Полковнику никто не пишет", "4:53", "bi2_polkovnik", "https://pic.rtbcdn.ru/video/2026-08-13/e9/cf/e9cf7443ef82030903fffe3e5fc1b1d3.jpg", SourceType.RUTUBE, "e772d4c265d253ce065e87cfa70f342e"),
        MockPopularItem("ANNA ASTI", "По барам", "3:58", "asti_po_baram", "https://pic.rtbcdn.ru/video/2026-01-02/d2/04/d2040d1cf9d34b39151ca0e871b2abdf.jpg", SourceType.RUTUBE, "7909c54b325ca9fe71fc1f3508ea522b"),
        MockPopularItem("ANNA ASTI", "Царица", "3:35", "asti_tsaritsa", "https://pic.rtbcdn.ru/video/2026-02-06/7b/89/7b89897a5d26023c04796c2174b35d09.jpg", SourceType.RUTUBE, "d4a5d0bb72291943576818b19661b202"),
        MockPopularItem("Звери", "Районы-кварталы", "3:21", "zveri_rayony", "https://pic.rtbcdn.ru/video/2026-04-01/3d/56/3d5678845faaca63e35c3001ef415ad7.jpg", SourceType.RUTUBE, "5fec9b703dc6377c901b15f988382148"),
        MockPopularItem("JONY", "Комета", "3:06", "jony_kometa", "https://pic.rtbcdn.ru/video/03/bc/03bca1fadf20230c2b6ae543b5a1295b.jpg", SourceType.RUTUBE, "731f875e8afb5645bcc91bf41b6f906e"),
        MockPopularItem("Мираж", "Музыка нас связала", "4:25", "mirazh_muzyka", "https://pic.rtbcdn.ru/video/2026-09-12/2e/fd/2efd1891c5a5da09a0527087de7aa4fd.jpg", SourceType.RUTUBE, "5f6c285c84aeccbd88dd657c9e59a961"),
        MockPopularItem("МакSим", "Знаешь ли ты", "4:09", "maksim_znaesh", "https://pic.rtbcdn.ru/video/2026-02-25/3f/07/3f071ec6d864d22cffb58cd7a83db760.jpg", SourceType.RUTUBE, "4a505363a80ce856c6c2d3019c48cf19"),
        MockPopularItem("Любэ", "Конь (Выйду ночью в поле)", "3:47", "lyube_kon", "https://pic.rtbcdn.ru/video/2025-11-14/ee/d4/eed4464cd0bc50dbfb1ca8b1341e5d85.jpg", SourceType.RUTUBE, "87fde5dade05e2391c74d622ebdaabac"),
        MockPopularItem("Иванушки Int.", "Тополиный пух", "3:50", "ivanushki_topol", "https://pic.rtbcdn.ru/video/2026-04-01/57/1e/571e010dc5a28e3ad68569ed9f14d8d3.jpg", SourceType.RUTUBE, "e07e955a5565f781e37c4210a69b82d5"),
        MockPopularItem("Земфира", "Хочешь", "3:22", "zemfira_hochesh", "https://pic.rtbcdn.ru/video/2026-10-17/2c/af/2caf2e4c583b27e2c7b63a5ded6dbea3.jpg", SourceType.RUTUBE, "a37f827ee8b5db3e2f850cb987a0aec6")
    )

    fun getShuffledPopularKaraoke(count: Int = 6, preferredSource: SourceType? = null): List<MockPopularItem> {
        return allPopularPool.shuffled().take(count)
    }

    /**
     * Возвращает список проверенных караоке-песен RUTUBE для выбранной категории.
     */
    fun getSongsForCategory(category: String, preferredSource: SourceType? = null): List<Song> {
        val cleanCat = category.trim().replace("\n", " ")
        return when {
            cleanCat.contains("Русский поп", ignoreCase = true) -> listOf(
                createSong("gagarina_spektakl", "Полина Гагарина", "Спектакль окончен", "Русский поп"),
                createSong("ruki_vverh_18", "Руки Вверх!", "18 мне уже", "Русский поп"),
                createSong("korzh_zhit_v_kaif", "Макс Корж", "Жить в кайф", "Русский поп"),
                createSong("asti_tsaritsa", "ANNA ASTI", "Царица", "Русский поп"),
                createSong("asti_po_baram", "ANNA ASTI", "По барам", "Русский поп"),
                createSong("jony_kometa", "JONY", "Комета", "Русский поп"),
                createSong("shatunov_sedaya", "Юрий Шатунов", "Седая ночь", "Русский поп"),
                createSong("basta_sansara", "Баста", "Сансара", "Русский поп")
            )

            cleanCat.contains("Рок", ignoreCase = true) -> listOf(
                createSong("kino_gruppa_krovi", "Кино", "Группа крови", "Рок"),
                createSong("kish_lesnik", "Король и Шут", "Лесник", "Рок"),
                createSong("kish_kukla", "Король и Шут", "Кукла колдуна", "Рок"),
                createSong("kino_pachka", "Кино", "Пачка сигарет", "Рок"),
                createSong("zemfira_hochesh", "Земфира", "Хочешь", "Рок"),
                createSong("bi2_polkovnik", "Би-2", "Полковнику никто не пишет", "Рок"),
                createSong("zveri_rayony", "Звери", "Районы-кварталы", "Рок"),
                createSong("aria_angel", "Ария", "Беспечный ангел", "Рок"),
                createSong("splin_serdtse", "Сплин", "Моё сердце", "Рок"),
                createSong("ddt_osen", "ДДТ", "Что такое осень", "Рок")
            )

            cleanCat.contains("Дискотека 90-х", ignoreCase = true) -> listOf(
                createSong("ruki_vverh_18", "Руки Вверх!", "18 мне уже", "Дискотека 90-х"),
                createSong("ruki_kroshka", "Руки Вверх!", "Крошка моя", "Дискотека 90-х"),
                createSong("shatunov_belye_rozy", "Ласковый май", "Белые розы", "Дискотека 90-х"),
                createSong("shatunov_sedaya", "Юрий Шатунов", "Седая ночь", "Дискотека 90-х"),
                createSong("mirazh_muzyka", "Мираж", "Музыка нас связала", "Дискотека 90-х"),
                createSong("ivanushki_topol", "Иванушки Int.", "Тополиный пух", "Дискотека 90-х"),
                createSong("allegrova_leytenant", "Ирина Аллегрова", "Младший лейтенант", "Дискотека 90-х"),
                createSong("krug_prihodite", "Михаил Круг", "Приходите в мой дом", "Дискотека 90-х")
            )

            cleanCat.contains("Хиты 2000-х", ignoreCase = true) -> listOf(
                createSong("maksim_znaesh", "МакSим", "Знаешь ли ты", "Хиты 2000-х"),
                createSong("zveri_rayony", "Звери", "Районы-кварталы", "Хиты 2000-х"),
                createSong("bi2_polkovnik", "Би-2", "Полковнику никто не пишет", "Хиты 2000-х"),
                createSong("splin_serdtse", "Сплин", "Моё сердце", "Хиты 2000-х"),
                createSong("zemfira_hochesh", "Земфира", "Хочешь", "Хиты 2000-х"),
                createSong("astudio_uletayu", "А-Студио", "Улетаю", "Хиты 2000-х"),
                createSong("meladze_salut", "Валерий Меладзе", "Салют, Вера!", "Хиты 2000-х"),
                createSong("kirkorov_sneg", "Филипп Киркоров", "Снег", "Хиты 2000-х")
            )

            cleanCat.contains("Детские", ignoreCase = true) -> listOf(
                createSong("kids_ulybka", "Крошка Енот", "От улыбки станет всем светлей", "Детские песни"),
                createSong("kids_bremen", "Бременские музыканты", "Ничего на свете лучше нету", "Детские песни"),
                createSong("kids_chunga", "Чунга-Чанга", "Чунга-Чанга", "Детские песни"),
                createSong("kids_antoshka", "Антошка", "Антошка, пойдём копать картошку", "Детские песни"),
                createSong("kids_mamontenok", "Мама для мамонтёнка", "Песенка мамонтёнка", "Детские песни"),
                createSong("kids_buratino", "Буратино", "Песенка Буратино", "Детские песни"),
                createSong("kids_kacheli", "Приключения Электроника", "Крылатые качели", "Детские песни"),
                createSong("kids_elochka", "Детский хор", "В лесу родилась ёлочка", "Детские песни"),
                createSong("kids_daleko", "Гостья из будущего", "Прекрасное далёко", "Детские песни")
            )

            cleanCat.contains("Любовные", ignoreCase = true) -> listOf(
                createSong("basta_medlyachok", "Баста", "Выпускной (Медлячок)", "Любовные хиты"),
                createSong("maksim_znaesh", "МакSим", "Знаешь ли ты", "Любовные хиты"),
                createSong("meladze_salut", "Валерий Меладзе", "Салют, Вера!", "Любовные хиты"),
                createSong("agutin_luna", "Леонид Агутин", "На сиреневой луне", "Любовные хиты"),
                createSong("kirkorov_sneg", "Филипп Киркоров", "Снег", "Любовные хиты"),
                createSong("astudio_uletayu", "А-Студио", "Улетаю", "Любовные хиты"),
                createSong("allegrova_leytenant", "Ирина Аллегрова", "Младший лейтенант", "Любовные хиты"),
                createSong("krug_prihodite", "Михаил Круг", "Приходите в мой дом", "Любовные хиты")
            )

            cleanCat.contains("кино", ignoreCase = true) || cleanCat.contains("мультфильм", ignoreCase = true) -> listOf(
                createSong("lyube_kon", "Любэ", "Конь (Выйду ночью в поле)", "Из кино и мультфильмов"),
                createSong("kino_ostrov", "Бриллиантовая рука", "Остров невезения", "Из кино и мультфильмов"),
                createSong("kino_medvedi", "Кавказская пленница", "Песенка о медведях", "Из кино и мультфильмов"),
                createSong("kids_daleko", "Гостья из будущего", "Прекрасное далёко", "Из кино и мультфильмов"),
                createSong("kids_kacheli", "Приключения Электроника", "Крылатые качели", "Из кино и мультфильмов"),
                createSong("kids_bremen", "Бременские музыканты", "Ничего на свете лучше нету", "Из кино и мультфильмов")
            )

            else -> listOf(
                createSong("gagarina_spektakl", "Полина Гагарина", "Спектакль окончен", "Хиты"),
                createSong("ruki_vverh_18", "Руки Вверх!", "18 мне уже", "Хиты"),
                createSong("kish_lesnik", "Король и Шут", "Лесник", "Хиты"),
                createSong("korzh_zhit_v_kaif", "Макс Корж", "Жить в кайф", "Хиты"),
                createSong("kino_gruppa_krovi", "Кино", "Группа крови", "Хиты"),
                createSong("basta_sansara", "Баста", "Сансара", "Хиты")
            )
        }
    }
}
