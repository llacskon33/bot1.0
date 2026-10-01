package com.llacskon33.droidwindows.files

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import java.io.File

data class WorkspaceDownload(
    val id: Long,
    val file: File,
    val status: Int,
    val statusLabel: String,
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val isActive: Boolean
        get() = status == DownloadManager.STATUS_PENDING ||
            status == DownloadManager.STATUS_RUNNING ||
            status == DownloadManager.STATUS_PAUSED

    val progressPercent: Int?
        get() = if (totalBytes > 0) {
            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
        } else {
            null
        }
}

class WorkspaceDownloads(context: Context, private val workspace: FileWorkspace) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(DownloadManager::class.java)
        ?: error("El gestor de descargas de Android no está disponible")
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun enqueue(address: String, directory: File): Long {
        val uri = Uri.parse(address.trim())
        require(uri.scheme.equals("https", ignoreCase = true)) { "Usa un enlace HTTPS seguro" }
        require(!uri.host.isNullOrBlank()) { "El enlace no contiene un servidor válido" }

        val safeDirectory = workspace.resolve(directory.absolutePath)
        require(safeDirectory.isDirectory) { "La carpeta de destino no existe" }
        val externalWorkspace = appContext.getExternalFilesDir(WORKSPACE_DIRECTORY)?.canonicalFile
        require(externalWorkspace == workspace.root.canonicalFile) {
            "El almacenamiento externo de la aplicación no está disponible para descargar"
        }

        val relativeDirectory = safeDirectory.relativeTo(workspace.root.canonicalFile).path
            .takeUnless { it == "." }
            .orEmpty()
        val fileName = availableFileName(
            safeDirectory,
            URLUtil.guessFileName(uri.toString(), null, null)
        )
        val relativePath = listOf(relativeDirectory, fileName).filter { it.isNotEmpty() }.joinToString("/")
        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription("Guardando en ${workspace.displayPath(safeDirectory)}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(appContext, WORKSPACE_DIRECTORY, relativePath)
        val id = manager.enqueue(request)
        preferences.edit().putString(taskKey(id), "$relativeDirectory\n$fileName").apply()
        return id
    }

    fun list(): List<WorkspaceDownload> =
        preferences.all.keys.mapNotNull { key ->
            val id = key.removePrefix(TASK_PREFIX).toLongOrNull() ?: return@mapNotNull null
            val metadata = preferences.getString(key, null) ?: return@mapNotNull null
            val separator = metadata.indexOf('\n')
            if (separator < 0) return@mapNotNull null
            val relativeDirectory = metadata.substring(0, separator)
            val fileName = metadata.substring(separator + 1)
            val relativePath = listOf(relativeDirectory, fileName).filter { it.isNotEmpty() }.joinToString("/")
            val file = workspace.resolve(File(workspace.root, relativePath).absolutePath)
            query(id, file)
        }.sortedByDescending { it.id }

    fun cancel(id: Long) {
        manager.remove(id)
        forget(id)
    }

    fun forget(id: Long) {
        preferences.edit().remove(taskKey(id)).apply()
    }

    private fun query(id: Long, file: File): WorkspaceDownload? {
        val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            val downloaded = it.getLong(
                it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            )
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val label = when (status) {
                DownloadManager.STATUS_PENDING -> "En espera"
                DownloadManager.STATUS_RUNNING -> "Descargando"
                DownloadManager.STATUS_PAUSED -> "En pausa por Android"
                DownloadManager.STATUS_SUCCESSFUL -> "Completada"
                DownloadManager.STATUS_FAILED -> "Fallida (código $reason)"
                else -> "Estado desconocido"
            }
            return WorkspaceDownload(id, file, status, label, downloaded, total)
        }
    }

    private fun availableFileName(directory: File, suggestedName: String): String {
        val cleaned = suggestedName
            .replace(Regex("""[\p{Cntrl}<>:"/\\|?*]"""), "_")
            .trim(' ', '.')
            .take(MAX_FILE_NAME_LENGTH)
            .ifBlank { "descarga" }
        val activePaths = list().filter { it.isActive }.map { it.file.absolutePath }.toSet()
        var sequence = 0
        while (true) {
            val name = if (sequence == 0) cleaned else withSuffix(cleaned, sequence)
            val candidate = File(directory, name)
            if (!candidate.exists() && candidate.absolutePath !in activePaths) return name
            sequence++
        }
    }

    private fun withSuffix(name: String, suffix: Int): String {
        val extensionIndex = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
        val stem = name.substring(0, extensionIndex)
        val extension = name.substring(extensionIndex)
        val marker = " ($suffix)"
        return stem.take((MAX_FILE_NAME_LENGTH - marker.length - extension.length).coerceAtLeast(1)) +
            marker + extension
    }

    private fun taskKey(id: Long) = "$TASK_PREFIX$id"

    companion object {
        private const val PREFERENCES_NAME = "workspace_downloads"
        private const val TASK_PREFIX = "task_"
        private const val WORKSPACE_DIRECTORY = "Droid Windows"
        private const val MAX_FILE_NAME_LENGTH = 120
    }
}
