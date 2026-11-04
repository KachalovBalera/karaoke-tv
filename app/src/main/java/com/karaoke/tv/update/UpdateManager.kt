package com.karaoke.tv.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
        const val PREFS_NAME = "karaoke_update_prefs"
        const val KEY_CUSTOM_MANIFEST_URL = "custom_manifest_url"
        const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/KachalovBalera/karaoke-tv/main/version.json"
        const val FALLBACK_GITHUB_API_URL =
            "https://api.github.com/repos/KachalovBalera/karaoke-tv/releases/latest"

        private const val APK_FILE_NAME = "karaoke_tv_update.apk"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    var customManifestUrl: String
        get() = prefs.getString(KEY_CUSTOM_MANIFEST_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_MANIFEST_URL, value.trim()).apply()

    fun getEffectiveManifestUrl(): String {
        val custom = customManifestUrl
        return if (custom.isNotBlank()) custom else DEFAULT_MANIFEST_URL
    }

    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (packageInfo.longVersionCode and 0xFFFFFFFFL).toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (_: Exception) {
            1
        }
    }

    /**
     * Проверяет наличие новой версии на GitHub.
     * Опрашивает официальный GitHub Releases API (мгновенный актуальный результат)
     * и манифест version.json, выбирая наиболее свежую версию.
     */
    suspend fun checkForUpdates(urlOverride: String? = null): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVer = getCurrentVersionName()
        val currentCode = getCurrentVersionCode()

        var bestUpdateInfo: AppUpdateInfo? = null

        // 1. Опрос официального GitHub Releases API (без задержек кэша CDN raw.githubusercontent.com)
        try {
            val request = Request.Builder()
                .url(FALLBACK_GITHUB_API_URL)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "KaraokeTV-Updater")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val json = gson.fromJson(bodyString, JsonObject::class.java)
                        val tagName = json.get("tag_name")?.asString?.removePrefix("v") ?: ""
                        val releaseNotes = json.get("body")?.asString ?: ""
                        val publishDate = json.get("published_at")?.asString?.take(10) ?: ""

                        var apkDownloadUrl = ""
                        val assets = json.getAsJsonArray("assets")
                        if (assets != null) {
                            for (element in assets) {
                                val assetObj = element.asJsonObject
                                val name = assetObj.get("name")?.asString ?: ""
                                if (name.endsWith(".apk", ignoreCase = true)) {
                                    apkDownloadUrl = assetObj.get("browser_download_url")?.asString ?: ""
                                    break
                                }
                            }
                        }

                        if (tagName.isNotBlank() && apkDownloadUrl.isNotBlank()) {
                            bestUpdateInfo = AppUpdateInfo(
                                version = tagName,
                                versionCode = 0,
                                apkUrl = apkDownloadUrl,
                                releaseNotes = releaseNotes,
                                forceUpdate = false,
                                publishDate = publishDate
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Опрос манифеста version.json (пользовательский URL или репозиторий)
        val manifestUrl = urlOverride ?: getEffectiveManifestUrl()
        try {
            val cacheBuster = if (manifestUrl.contains("?")) "&t=${System.currentTimeMillis()}" else "?t=${System.currentTimeMillis()}"
            val request = Request.Builder()
                .url(manifestUrl + cacheBuster)
                .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                .addHeader("Pragma", "no-cache")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val parsed = gson.fromJson(bodyString, AppUpdateInfo::class.java)
                        if (parsed != null && parsed.version.isNotBlank() && parsed.apkUrl.isNotBlank()) {
                            if (bestUpdateInfo == null || compareVersions(parsed.version, bestUpdateInfo!!.version) >= 0) {
                                bestUpdateInfo = parsed
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (bestUpdateInfo != null) {
            val isSemverNewer = compareVersions(bestUpdateInfo!!.version, currentVer) > 0
            val isCodeNewer = bestUpdateInfo!!.versionCode > 0 && bestUpdateInfo!!.versionCode > currentCode
            val hasUpdate = isSemverNewer || isCodeNewer

            if (hasUpdate) {
                return@withContext UpdateCheckResult(
                    hasUpdate = true,
                    currentVersion = currentVer,
                    currentVersionCode = currentCode,
                    updateInfo = bestUpdateInfo
                )
            }
        }

        UpdateCheckResult(
            hasUpdate = false,
            currentVersion = currentVer,
            currentVersionCode = currentCode,
            updateInfo = bestUpdateInfo,
            errorMessage = "Установлена последняя версия Karaoke TV ($currentVer)"
        )
    }

    /**
     * Возвращает директорию для хранения временных файлов обновлений.
     */
    private fun getUpdateDir(): File {
        val dir = File(context.cacheDir, "ota_updates")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Проверяет, скачан ли уже рабочий APK для указанной версии.
     * Если файл устарел или не соответствует expectedVersion, он автоматически удаляется.
     */
    fun getCachedApk(expectedVersion: String? = null): File? {
        val file = File(getUpdateDir(), APK_FILE_NAME)
        if (!file.exists() || !file.isFile || file.length() < 5 * 1024 * 1024) {
            return null
        }
        if (expectedVersion != null) {
            val archiveInfo = runCatching {
                context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            }.getOrNull()

            if (archiveInfo == null || archiveInfo.versionName != expectedVersion) {
                Log.w(
                    TAG,
                    "Cached APK (${archiveInfo?.versionName}) does not match expected version ($expectedVersion), deleting stale file"
                )
                runCatching { file.delete() }
                return null
            }
        }
        return file
    }

    /**
     * Удаляет скачанный файл APK из кэша.
     */
    fun clearCachedApk() {
        runCatching {
            val file = File(getUpdateDir(), APK_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    /**
     * Скачивает APK-файл с отображением прогресса в реальном времени.
     */
    suspend fun downloadApk(
        apkUrl: String,
        onProgress: (progress: Float, downloaded: Long, total: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = File(getUpdateDir(), APK_FILE_NAME)
        val tempFile = File(getUpdateDir(), "${APK_FILE_NAME}.tmp")

        try {
            if (tempFile.exists()) tempFile.delete()

            val request = Request.Builder()
                .url(apkUrl)
                .addHeader("User-Agent", "KaraokeTV-Updater")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Сервер вернул ошибку при скачивании: ${response.code} ${response.message}")
                )
            }

            val body = response.body ?: return@withContext Result.failure(
                Exception("Пустой ответ от сервера при скачивании APK")
            )

            val contentLength = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32 * 1024) // 32KB buffer
                    var read: Int
                    var lastUpdate = System.currentTimeMillis()

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 100 || downloadedBytes == contentLength) {
                            lastUpdate = now
                            val progress = if (contentLength > 0) {
                                (downloadedBytes.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0.5f
                            }
                            onProgress(progress, downloadedBytes, contentLength)
                        }
                    }
                    output.flush()
                }
            }

            // Валидация скачанного файла как валидного APK пакета
            val archiveInfo = runCatching {
                context.packageManager.getPackageArchiveInfo(tempFile.absolutePath, 0)
            }.getOrNull()

            if (archiveInfo == null || archiveInfo.packageName != context.packageName) {
                tempFile.delete()
                return@withContext Result.failure(
                    Exception("Скачанный файл повреждён или не является пакетом приложения Karaoke TV")
                )
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                // Если renameTo не сработал через границы разделов
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            targetFile.setReadable(true, false)

            onProgress(1.0f, downloadedBytes, contentLength)
            Result.success(targetFile)
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Запускает установку APK через системный PackageInstaller Android.
     */
    fun installApk(apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists() || apkFile.length() < 1024 * 1024) {
                return Result.failure(Exception("Файл APK не найден или повреждён"))
            }

            // Обеспечиваем доступность файла для чтения сторонними процессами
            apkFile.setReadable(true, false)

            // Проверка разрешения на установку неизвестных приложений (Android 8.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Log.i(TAG, "REQUEST_INSTALL_PACKAGES not granted, opening settings")
                    try {
                        val settingsIntent = Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(settingsIntent)
                    } catch (_: Exception) {
                        try {
                            val generalSettings = Intent(Settings.ACTION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(generalSettings)
                        } catch (_: Exception) {}
                    }
                    return Result.failure(
                        Exception("Необходимо разрешить установку приложений для Karaoke TV в открывшихся системных настройках, после чего нажать «Установить» ещё раз.")
                    )
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            Log.i(TAG, "Starting PackageInstaller with apkUri: $apkUri, size: ${apkFile.length()} bytes")

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }

            // Явно выдаем права на чтение Uri для PackageInstaller
            val packageInstallerCandidates = listOf(
                "com.android.packageinstaller",
                "com.google.android.packageinstaller"
            )
            for (pkg in packageInstallerCandidates) {
                runCatching {
                    context.grantUriPermission(pkg, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            // Также выдаем права всем резолверам данного интента
            runCatching {
                val resInfoList = context.packageManager.queryIntentActivities(installIntent, 0)
                for (resolveInfo in resInfoList) {
                    val pkg = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(pkg, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            try {
                context.startActivity(installIntent)
            } catch (e: Exception) {
                Log.w(TAG, "ACTION_VIEW failed, attempting ACTION_INSTALL_PACKAGE fallback: ${e.message}")
                val fallbackIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                }
                context.startActivity(fallbackIntent)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            Result.failure(Exception("Не удалось запустить установщик Android: ${e.localizedMessage ?: e.message}"))
        }
    }
}
