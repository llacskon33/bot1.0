package com.llacskon33.droidwindows.files

import android.content.Context
import java.io.File

class FileWorkspace(context: Context) {
    val root: File = (context.getExternalFilesDir("Droid Windows") ?: File(context.filesDir, "Droid Windows"))
        .also { it.mkdirs() }

    fun list(directory: File): List<File> {
        val safeDirectory = resolve(directory.absolutePath)
        require(safeDirectory.isDirectory) { "No es una carpeta" }
        return safeDirectory.listFiles()?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
    }

    fun createDirectory(directory: File, name: String): File {
        val file = child(directory, name)
        check(file.mkdir()) { "No se pudo crear la carpeta (quizá ya existe)" }
        return file
    }

    fun createFile(directory: File, name: String): File {
        val file = child(directory, name)
        check(file.createNewFile()) { "No se pudo crear el archivo (quizá ya existe)" }
        return file
    }

    fun writeText(file: File, text: String) {
        resolve(file.absolutePath).writeText(text)
    }

    fun readText(file: File): String {
        val safeFile = resolve(file.absolutePath)
        require(safeFile.isFile) { "No es un archivo" }
        require(safeFile.length() <= MAX_TEXT_BYTES) { "El archivo supera el límite de vista previa de 256 KB" }
        return safeFile.readText()
    }

    fun displayPath(file: File): String {
        val safeFile = resolve(file.absolutePath)
        val relative = safeFile.relativeTo(root.canonicalFile).path
        return if (relative == ".") "/" else "/$relative"
    }

    fun resolve(path: String, base: File = root): File {
        val rootCanonical = root.canonicalFile
        val candidate = if (path.startsWith("/")) File(rootCanonical, path.removePrefix("/")) else File(base, path)
        val canonical = candidate.canonicalFile
        require(canonical == rootCanonical || canonical.path.startsWith(rootCanonical.path + File.separator)) {
            "La ruta debe permanecer dentro del espacio de trabajo"
        }
        return canonical
    }

    private fun child(directory: File, name: String): File {
        val validName = name.trim()
        require(validName.isNotEmpty() && validName !in setOf(".", "..") &&
            !validName.contains('/') && !validName.contains('\\')) { "Nombre de archivo no válido" }
        val parent = resolve(directory.absolutePath)
        require(parent.isDirectory) { "No es una carpeta" }
        return resolve(validName, parent)
    }

    companion object {
        private const val MAX_TEXT_BYTES = 256L * 1024L
    }
}
