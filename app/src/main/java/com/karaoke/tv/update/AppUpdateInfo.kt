package com.karaoke.tv.update

import java.io.File

/**
 * Информация об обновлении приложения из version.json или GitHub Releases.
 */
data class AppUpdateInfo(
    val version: String,
    val versionCode: Int = 0,
    val apkUrl: String,
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false,
    val publishDate: String = ""
)

/**
 * Результат проверки обновлений на сервере.
 */
data class UpdateCheckResult(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val currentVersionCode: Int,
    val updateInfo: AppUpdateInfo?,
    val errorMessage: String? = null
)

/**
 * Состояние процесса обновления для UI.
 */
sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateStatus()
    data class UpToDate(val message: String) : UpdateStatus()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File, val info: AppUpdateInfo) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

/**
 * Сравнение версий в формате semver (например, "1.0.1" > "1.0.0").
 * Возвращает:
 *  > 0 если v1 > v2
 *  < 0 если v1 < v2
 *    0 если v1 == v2
 */
fun compareVersions(v1: String, v2: String): Int {
    val clean1 = v1.removePrefix("v").trim()
    val clean2 = v2.removePrefix("v").trim()

    val parts1 = clean1.split(".").mapNotNull { it.toIntOrNull() }
    val parts2 = clean2.split(".").mapNotNull { it.toIntOrNull() }

    val maxLength = maxOf(parts1.size, parts2.size)
    for (i in 0 until maxLength) {
        val num1 = parts1.getOrElse(i) { 0 }
        val num2 = parts2.getOrElse(i) { 0 }
        if (num1 != num2) {
            return num1.compareTo(num2)
        }
    }
    return 0
}
