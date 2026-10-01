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

    fun rename(file: File, name: String): File {
        val source = resolve(file.absolutePath)
        require(source != root.canonicalFile) { "No se puede renombrar el espacio de trabajo" }
        val destination = child(source.parentFile ?: root, name)
        check(!destination.exists()) { "Ya existe un archivo con ese nombre" }
        check(source.renameTo(destination)) { "No se pudo renombrar" }
        return destination
    }

    fun copy(source: File, destination: File, recursive: Boolean = false): File {
        val safeSource = resolve(source.absolutePath)
        val requestedDestination = resolve(destination.absolutePath)
        require(safeSource != root.canonicalFile) { "No se puede copiar el espacio de trabajo completo" }
        require(!safeSource.isDirectory || recursive) { "Para copiar carpetas usa cp -r" }
        require(requestedDestination != safeSource &&
            !(safeSource.isDirectory && requestedDestination.path.startsWith(safeSource.path + File.separator))) {
            "No se puede copiar una carpeta dentro de sí misma"
        }
        val target = if (requestedDestination.isDirectory) {
            resolve(File(requestedDestination, safeSource.name).absolutePath)
        } else {
            requestedDestination
        }
        copyContained(safeSource, target)
        return target
    }

    fun move(source: File, destination: File): File {
        val safeSource = resolve(source.absolutePath)
        val requestedDestination = resolve(destination.absolutePath)
        require(safeSource != root.canonicalFile) { "No se puede mover el espacio de trabajo" }
        require(requestedDestination != safeSource &&
            !(safeSource.isDirectory && requestedDestination.path.startsWith(safeSource.path + File.separator))) {
            "No se puede mover una carpeta dentro de sí misma"
        }
        val target = if (requestedDestination.isDirectory) {
            resolve(File(requestedDestination, safeSource.name).absolutePath)
        } else {
            requestedDestination
        }
        check(!target.exists()) { "Ya existe un archivo con ese nombre" }
        check(safeSource.renameTo(target)) { "No se pudo mover el archivo" }
        return target
    }

    fun delete(file: File, recursive: Boolean = false) {
        val safeFile = resolve(file.absolutePath)
        require(safeFile != root.canonicalFile) { "No se puede eliminar el espacio de trabajo" }
        if (safeFile.isDirectory) {
            val children = safeFile.listFiles()?.toList().orEmpty()
            require(recursive || children.isEmpty()) { "La carpeta no está vacía; usa rm -r" }
            children.forEach { deleteContained(it) }
        }
        check(safeFile.delete()) { "No se pudo eliminar" }
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

    fun filesWithExtension(extension: String): List<File> {
        val suffix = extension.removePrefix(".").lowercase()
        val matches = mutableListOf<File>()
        fun collect(directory: File) {
            list(directory).forEach { file ->
                if (file.isDirectory) collect(file)
                else if (file.extension.lowercase() == suffix) matches.add(file)
            }
        }
        collect(root)
        return matches
    }

    fun resolve(path: String, base: File = root): File {
        val rootCanonical = root.canonicalFile
        val candidate = when {
            path == rootCanonical.path || path.startsWith(rootCanonical.path + File.separator) -> File(path)
            path.startsWith("/") -> File(rootCanonical, path.removePrefix("/"))
            else -> File(base, path)
        }
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

    private fun copyContained(source: File, destination: File) {
        val safeSource = resolve(source.absolutePath)
        val safeDestination = resolve(destination.absolutePath)
        check(!safeDestination.exists()) { "Ya existe un archivo con ese nombre" }
        if (safeSource.isDirectory) {
            check(safeDestination.mkdir()) { "No se pudo crear la carpeta de destino" }
            safeSource.listFiles()?.forEach { child ->
                copyContained(child, File(safeDestination, child.name))
            }
        } else {
            safeSource.copyTo(safeDestination, overwrite = false)
        }
    }

    private fun deleteContained(file: File) {
        val safeFile = resolve(file.absolutePath)
        if (safeFile.isDirectory) {
            safeFile.listFiles()?.forEach { deleteContained(it) }
        }
        check(safeFile.delete()) { "No se pudo eliminar ${safeFile.name}" }
    }

    companion object {
        private const val MAX_TEXT_BYTES = 256L * 1024L
    }
}
