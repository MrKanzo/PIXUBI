package com.pixubi.game

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Optional network-only update flow. Normal care, games and sound remain offline.
 * Stable APK releases must keep package id and use the same private signing key.
 */
data class PixubiRelease(
    val version: String,
    val url: String,
    val bytes: Long,
    val sha256: String?
)

sealed class PixubiUpdateResult {
    data class NewRelease(val release: PixubiRelease) : PixubiUpdateResult()
    data class UpToDate(val version: String) : PixubiUpdateResult()
    data class Unavailable(val message: String) : PixubiUpdateResult()
}

class PixubiUpdater(private val activity: Activity) {
    private val repo = "MrKanzo/PIXUBI"
    private val stablePackage = "com.pixubi.game.stable"
    private val assetName = "PIXUBI-release.apk"
    private val endpoint = "https://api.github.com/repos/$repo/releases/latest"
    private val prefs by lazy {
        activity.getSharedPreferences("pixubi_updates", Context.MODE_PRIVATE)
    }

    val isStableInstall: Boolean get() = activity.packageName == stablePackage

    private fun versions(version: String): List<Int> =
        version.removePrefix("v").substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }

    internal fun newer(remote: String, installed: String): Boolean {
        val a = versions(remote)
        val b = versions(installed)
        for (i in 0 until maxOf(a.size, b.size)) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av > bv
        }
        return false
    }

    private fun currentVersion(): String =
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "0"

    /** Run only once per 24h when installed as a stable release. No background service. */
    fun checkDaily(onFound: (PixubiRelease) -> Unit) {
        if (!isStableInstall) return
        val now = System.currentTimeMillis()
        val last = prefs.getLong("last_check", 0L)
        val day = 24L * 60L * 60L * 1000L
        if (last > 0 && now >= last && now - last < day) return
        prefs.edit().putLong("last_check", now).apply()
        check { result ->
            if (result is PixubiUpdateResult.NewRelease) onFound(result.release)
        }
    }

    fun check(onComplete: (PixubiUpdateResult) -> Unit) {
        Thread {
            val result: PixubiUpdateResult = try {
                val connection = (URL(endpoint).openConnection() as HttpURLConnection)
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "PIXUBI-Android")
                try {
                    if (connection.responseCode != 200) {
                        throw IllegalStateException("GitHub Releases недоступний (HTTP ${connection.responseCode})")
                    }
                    val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                        it.readText()
                    }
                    val json = JSONObject(text)
                    val version = json.optString("tag_name")
                    if (!version.matches(Regex("v?[0-9]+\\.[0-9]+\\.[0-9]+"))) {
                        throw IllegalStateException("Невідома версія релізу")
                    }
                    val assets = json.getJSONArray("assets")
                    var release: PixubiRelease? = null
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.optString("name") !in setOf(assetName, "app-release.apk")) continue
                        val uri = Uri.parse(asset.getString("browser_download_url"))
                        if (uri.scheme != "https" || uri.host != "github.com" ||
                            !uri.path.orEmpty().startsWith("/$repo/releases/download/")) {
                            throw IllegalStateException("Посилання релізу не пройшло перевірку")
                        }
                        val bytes = asset.optLong("size", 0L)
                        if (bytes !in 1024L..150_000_000L) {
                            throw IllegalStateException("Неприпустимий розмір APK")
                        }
                        val digest = asset.optString("digest")
                            .removePrefix("sha256:").lowercase()
                            .takeIf { it.matches(Regex("[0-9a-f]{64}")) }
                        release = PixubiRelease(version, uri.toString(), bytes, digest)
                        break
                    }
                    when {
                        release == null -> PixubiUpdateResult.Unavailable(
                            "Останній реліз поки не має підписаного APK."
                        )
                        newer(version, currentVersion()) -> PixubiUpdateResult.NewRelease(release)
                        else -> PixubiUpdateResult.UpToDate(currentVersion())
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                PixubiUpdateResult.Unavailable(
                    "Не вдалося перевірити оновлення. Перевір інтернет або наявність релізу."
                )
            }
            activity.runOnUiThread {
                if (!activity.isFinishing && !activity.isDestroyed) onComplete(result)
            }
        }.start()
    }

    fun downloadAndInstall(release: PixubiRelease, onMessage: (String) -> Unit) {
        if (!isStableInstall) {
            onMessage("Це тестова версія. Оновлення поверх працюватимуть у стабільній PIXUBI.")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()) {
            activity.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}"))
            )
            onMessage("Дозволь встановлення з PIXUBI, повернись у гру та натисни «Оновити» ще раз.")
            return
        }

        val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val directory = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (directory == null) {
            onMessage("Пам'ять для завантаження APK недоступна.")
            return
        }
        val name = "PIXUBI-${release.version.removePrefix("v")}.apk"
        val target = File(directory, name)
        // Avoid an already present file causing DownloadManager to fail.
        if (target.exists() && !target.delete()) {
            onMessage("Не вдалося підготувати файл оновлення.")
            return
        }
        try {
            val request = DownloadManager.Request(Uri.parse(release.url))
                .setTitle("PIXUBI ${release.version}")
                .setDescription("Безпечне оновлення кіберзвірят")
                .setAllowedOverMetered(true)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setMimeType("application/vnd.android.package-archive")
                .setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, name)
            val downloadId = manager.enqueue(request)
            onMessage("Завантажую оновлення PIXUBI…")
            Thread {
                var finished = false
                try {
                    while (!finished) {
                        Thread.sleep(650L)
                        val cursor = manager.query(
                            DownloadManager.Query().setFilterById(downloadId)
                        ) ?: throw IllegalStateException("Завантаження недоступне")
                        cursor.use {
                            if (!it.moveToFirst()) {
                                throw IllegalStateException("Завантаження загубилося")
                            }
                            when (it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    finished = true
                                    checkAndOpenInstaller(target, release, onMessage)
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    throw IllegalStateException("Не вдалося завантажити APK")
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    activity.runOnUiThread {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            onMessage("Помилка завантаження або перевірки APK.")
                        }
                    }
                }
            }.start()
        } catch (_: Exception) {
            onMessage("Не вдалося запустити завантаження.")
        }
    }

    private fun checkAndOpenInstaller(
        file: File,
        release: PixubiRelease,
        onMessage: (String) -> Unit
    ) {
        if (!file.exists() || file.length() != release.bytes) {
            throw IllegalStateException("Розмір завантаженого файлу не збігається")
        }
        val expected = release.sha256
        if (expected != null && sha256(file) != expected) {
            throw IllegalStateException("SHA-256 APK не збігається")
        }
        val pm = activity.packageManager
        @Suppress("DEPRECATION")
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        @Suppress("DEPRECATION")
        val candidate = pm.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IllegalStateException("APK неможливо прочитати")
        @Suppress("DEPRECATION")
        val installed = pm.getPackageInfo(activity.packageName, flags)
        if (candidate.packageName != activity.packageName) {
            throw IllegalStateException("Ідентифікатор застосунку відрізняється")
        }
        val sameSignature = signatures(candidate).isNotEmpty() &&
            signatures(candidate) == signatures(installed)
        if (!sameSignature) {
            throw IllegalStateException("Підпис APK відрізняється від встановленого")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (candidate.longVersionCode <= installed.longVersionCode) {
                throw IllegalStateException("Версія APK не новіша")
            }
        } else {
            @Suppress("DEPRECATION")
            if (candidate.versionCode <= installed.versionCode) {
                throw IllegalStateException("Версія APK не новіша")
            }
        }
        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
            val uri = FileProvider.getUriForFile(
                activity, "${activity.packageName}.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                activity.startActivity(intent)
            } catch (_: Exception) {
                onMessage("Інсталятор недоступний. Знайди APK у завантаженнях Android.")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun signatures(info: PackageInfo): Set<String> {
        val certs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
        } else {
            info.signatures?.map { it.toByteArray() }.orEmpty()
        }
        return certs.map { bytes ->
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val chunk = ByteArray(8192)
            while (true) {
                val n = input.read(chunk)
                if (n < 0) break
                md.update(chunk, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
