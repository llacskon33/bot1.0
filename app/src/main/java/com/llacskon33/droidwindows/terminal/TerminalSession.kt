package com.llacskon33.droidwindows.terminal

import com.llacskon33.droidwindows.files.FileWorkspace
import java.io.File

class TerminalSession(
    private val workspace: FileWorkspace,
    private val runPython: (String, File, String) -> Result<String> = { _, _, _ ->
        Result.failure(IllegalStateException("Python todavía no está disponible"))
    },
    private val runPythonFile: (File, File) -> Result<String> = { _, _ ->
        Result.failure(IllegalStateException("Python todavía no está disponible"))
    }
) {
    private var currentDirectory: File = workspace.root

    fun prompt(command: String? = null): String =
        "dw:${workspace.displayPath(currentDirectory)}${if (command == null) " $" else " $command"}"

    fun execute(line: String): String {
        val redirect = ECHO_REDIRECT.matchEntire(line.trim())
        if (redirect != null) {
            return runCatching {
                val destination = workspace.resolve(redirect.groupValues[3], currentDirectory)
                val text = redirect.groupValues[1]
                if (redirect.groupValues[2] == ">>") destination.appendText(text) else workspace.writeText(destination, text)
                ""
            }.getOrElse { "Error: ${it.message}" }
        }
        val args = tokenize(line)
        if (args.isEmpty()) return ""
        val command = args.first().lowercase()
        val values = args.drop(1)
        return runCatching {
            when (command) {
                "help" -> "Comandos: help, pwd, ls, dir, cd, mkdir, touch, cat, type, echo, cp, mv, rm, del, rmdir, python, pip, clear"
                "pwd" -> workspace.displayPath(currentDirectory)
                "ls", "dir" -> {
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
                    val file = workspace.resolve(values[0], currentDirectory)
                    require(file.parentFile?.isDirectory == true) { "La carpeta de destino no existe" }
                    if (!file.exists()) check(file.createNewFile()) { "No se pudo crear el archivo" }
                    file.name
                }
                "cat", "type" -> {
                    require(values.isNotEmpty()) { "Uso: cat <archivo>" }
                    values.joinToString("\n") { workspace.readText(workspace.resolve(it, currentDirectory)) }
                        .ifEmpty { "(archivo vacío)" }
                }
                "echo" -> values.joinToString(" ")
                "cp", "copy" -> {
                    val recursive = values.firstOrNull() == "-r" || values.firstOrNull() == "-R" ||
                        values.firstOrNull() == "/s"
                    val paths = values.filterNot { it in setOf("-r", "-R", "/s") }
                    require(paths.size == 2) { "Uso: cp [-r] <origen> <destino>" }
                    workspace.copy(
                        workspace.resolve(paths[0], currentDirectory),
                        workspace.resolve(paths[1], currentDirectory),
                        recursive
                    ).let(workspace::displayPath)
                }
                "mv", "move" -> {
                    require(values.size == 2) { "Uso: mv <origen> <destino>" }
                    workspace.move(
                        workspace.resolve(values[0], currentDirectory),
                        workspace.resolve(values[1], currentDirectory)
                    ).let(workspace::displayPath)
                }
                "rm", "del" -> {
                    val recursive = values.any { it in setOf("-r", "-R", "-rf", "-fr", "/s") }
                    val paths = values.filterNot { it.startsWith("-") || it == "/s" }
                    require(paths.size == 1) { "Uso: rm [-r] <ruta>" }
                    workspace.delete(workspace.resolve(paths[0], currentDirectory), recursive)
                    ""
                }
                "rmdir" -> {
                    require(values.size == 1) { "Uso: rmdir <carpeta>" }
                    workspace.delete(workspace.resolve(values[0], currentDirectory))
                    ""
                }
                "python", "python3" -> executePython(values)
                "pip" -> "pip interactivo no está disponible en Android. Python se ejecuta de forma real; los paquetes compatibles deben incluirse al compilar la app mediante Chaquopy/Gradle."
                "clear", "cls" -> ""
                else -> "Comando desconocido: $command. Escribe help para ver los comandos."
            }
        }.getOrElse { "Error: ${it.message}" }
    }

    private fun executePython(args: List<String>): String {
        if (args.isEmpty()) return "Python 3 está disponible. Usa python -c \"print('Hola')\" o python <archivo.py>."
        if (args.first() == "--version" || args.first() == "-V") {
            return runPython("import sys; print(sys.version.split()[0])", currentDirectory, "<version>").getOrThrow()
        }
        if (args.first() == "-c") {
            require(args.size >= 2) { "Uso: python -c <código>" }
            return runPython(args.drop(1).joinToString(" "), currentDirectory, "<console>").getOrThrow()
        }
        require(args.size == 1 && args[0].endsWith(".py", ignoreCase = true)) {
            "Uso: python <archivo.py> o python -c <código>"
        }
        val program = workspace.resolve(args[0], currentDirectory)
        require(program.isFile) { "No se encontró el archivo Python" }
        return runPythonFile(program, currentDirectory).getOrThrow().ifEmpty { "(sin salida)" }
    }

    private fun tokenize(input: String): List<String> =
        TOKEN.findAll(input).map { it.groups[1]?.value ?: it.groups[2]?.value ?: it.groups[3]?.value.orEmpty() }.toList()

    companion object {
        private val TOKEN = Regex("\"([^\"]*)\"|'([^']*)'|(\\S+)")
        private val ECHO_REDIRECT = Regex("""echo\s+(.+?)\s*(>>|>)\s*(.+)""", RegexOption.IGNORE_CASE)
    }
}
