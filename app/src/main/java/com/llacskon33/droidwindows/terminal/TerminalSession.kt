package com.llacskon33.droidwindows.terminal

import com.llacskon33.droidwindows.files.FileWorkspace
import java.io.File

class TerminalSession(private val workspace: FileWorkspace) {
    private var currentDirectory: File = workspace.root

    fun prompt(command: String? = null): String =
        "dw:${workspace.displayPath(currentDirectory)}${if (command == null) " $" else " $command"}"

    fun execute(line: String): String {
        val redirect = ECHO_REDIRECT.matchEntire(line.trim())
        if (redirect != null) {
            return runCatching {
                val destination = workspace.resolve(redirect.groupValues[2], currentDirectory)
                workspace.writeText(destination, redirect.groupValues[1])
                ""
            }.getOrElse { "Error: ${it.message}" }
        }
        val args = tokenize(line)
        if (args.isEmpty()) return ""
        val command = args.first().lowercase()
        val values = args.drop(1)
        return runCatching {
            when (command) {
                "help" -> "Comandos: help, pwd, ls [ruta], cd [ruta], mkdir <nombre>, touch <archivo>, cat <archivo>, echo <texto>, clear"
                "pwd" -> workspace.displayPath(currentDirectory)
                "ls" -> {
                    val directory = values.firstOrNull()?.let { workspace.resolve(it, currentDirectory) } ?: currentDirectory
                    workspace.list(directory).joinToString("\n") { if (it.isDirectory) "${it.name}/" else it.name }
                        .ifEmpty { "(carpeta vacía)" }
                }
                "cd" -> {
                    val destination = values.firstOrNull()?.let { workspace.resolve(it, currentDirectory) } ?: workspace.root
                    require(destination.isDirectory) { "No es una carpeta" }
                    currentDirectory = destination
                    workspace.displayPath(currentDirectory)
                }
                "mkdir" -> {
                    require(values.size == 1) { "Uso: mkdir <nombre>" }
                    workspace.createDirectory(currentDirectory, values[0]).name
                }
                "touch" -> {
                    require(values.size == 1) { "Uso: touch <archivo>" }
                    workspace.createFile(currentDirectory, values[0]).name
                }
                "cat" -> {
                    require(values.size == 1) { "Uso: cat <archivo>" }
                    workspace.readText(workspace.resolve(values[0], currentDirectory)).ifEmpty { "(archivo vacío)" }
                }
                "echo" -> values.joinToString(" ")
                "clear" -> ""
                else -> "Comando desconocido: $command. Escribe help para ver los comandos."
            }
        }.getOrElse { "Error: ${it.message}" }
    }

    private fun tokenize(input: String): List<String> =
        TOKEN.findAll(input).map { it.groups[1]?.value ?: it.groups[2]?.value ?: it.groups[3]?.value.orEmpty() }.toList()

    companion object {
        private val TOKEN = Regex("\"([^\"]*)\"|'([^']*)'|(\\S+)")
        private val ECHO_REDIRECT = Regex("""echo\s+(.+?)\s*>\s*(.+)""", RegexOption.IGNORE_CASE)
    }
}
