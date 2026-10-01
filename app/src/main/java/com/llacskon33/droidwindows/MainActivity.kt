package com.llacskon33.droidwindows

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.llacskon33.droidwindows.files.FileWorkspace
import com.llacskon33.droidwindows.terminal.TerminalSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var workspace: FileWorkspace
    private lateinit var terminal: TerminalSession
    private var currentPage = Page.DESKTOP
    private var startMenu: View? = null

    private val documentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) openExternalDocument(uri)
    }

    private enum class Page { DESKTOP, EXPLORER, TERMINAL }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workspace = FileWorkspace(this)
        terminal = TerminalSession(workspace)
        window.statusBarColor = Color.rgb(21, 36, 57)
        window.navigationBarColor = Color.rgb(19, 30, 46)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentPage) {
                    Page.DESKTOP -> {
                        if (startMenu != null) {
                            (startMenu?.parent as? FrameLayout)?.removeView(startMenu)
                            startMenu = null
                        } else finish()
                    }
                    else -> showDesktop()
                }
            }
        })
        showDesktop()
    }

    private fun showDesktop() {
        currentPage = Page.DESKTOP
        startMenu = null
        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(27, 70, 112), Color.rgb(29, 113, 150), Color.rgb(41, 65, 117))
            )
        }
        val desktop = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(10))
        }
        val header = TextView(this).apply {
            text = "Droid Windows"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        desktop.addView(header)
        desktop.addView(TextView(this).apply {
            text = "Tu escritorio, en cualquier lugar"
            textSize = 14f
            setTextColor(Color.rgb(220, 235, 248))
            setPadding(0, dp(3), 0, dp(22))
        })

        val shortcuts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        shortcuts.addView(shortcut("📁", "Explorador") { showExplorer(workspace.root) }, weightParams(0, 1f))
        shortcuts.addView(shortcut("›_", "Terminal") { showTerminal() }, weightParams(0, 1f))
        desktop.addView(shortcuts)

        val welcome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(Color.argb(58, 255, 255, 255), dp(20))
        }
        welcome.addView(TextView(this).apply {
            text = "Bienvenido a tu escritorio"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        welcome.addView(TextView(this).apply {
            text = "Explora tus archivos y ejecuta comandos en la terminal integrada."
            textSize = 14f
            setTextColor(Color.rgb(232, 241, 250))
            setPadding(0, dp(7), 0, 0)
        })
        val homeContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(welcome)
            addView(TextView(this@MainActivity).apply {
                text = "EN DESARROLLO\nPython y la compatibilidad con programas de Windows se añadirán en futuras versiones."
                textSize = 13f
                setTextColor(Color.rgb(232, 241, 250))
                setPadding(dp(4), dp(22), dp(4), 0)
            })
        }
        desktop.addView(homeContent, LinearLayout.LayoutParams(-1, 0, 1f))

        val taskbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(Color.argb(232, 238, 244, 251), dp(20))
            elevation = dp(8).toFloat()
        }
        val startButton = textButton("⊞", 25f).apply {
            setTextColor(Color.rgb(20, 105, 187))
            setOnClickListener { toggleStartMenu(root) }
        }
        taskbar.addView(startButton, LinearLayout.LayoutParams(dp(48), dp(44)))
        taskbar.addView(textButton("📁", 20f).apply {
            setTextColor(Color.rgb(28, 42, 58))
            contentDescription = "Abrir explorador de archivos"
            setOnClickListener { showExplorer(workspace.root) }
        }, LinearLayout.LayoutParams(dp(48), dp(44)))
        taskbar.addView(textButton("›_", 15f).apply {
            setTextColor(Color.rgb(28, 42, 58))
            contentDescription = "Abrir terminal"
            setOnClickListener { showTerminal() }
        }, LinearLayout.LayoutParams(dp(48), dp(44)))
        taskbar.addView(TextView(this).apply {
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            textSize = 13f
            setTextColor(Color.rgb(28, 42, 58))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(0, -1, 1f))
        desktop.addView(taskbar, LinearLayout.LayoutParams(-1, dp(62)))
        root.addView(desktop, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    private fun toggleStartMenu(root: FrameLayout) {
        if (startMenu != null) {
            root.removeView(startMenu)
            startMenu = null
            return
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(18))
            background = rounded(Color.rgb(245, 248, 252), dp(24))
            elevation = dp(18).toFloat()
        }
        panel.addView(TextView(this).apply {
            text = "Inicio"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(31, 43, 59))
        })
        panel.addView(TextView(this).apply {
            text = "Anclado"
            textSize = 13f
            setTextColor(Color.rgb(90, 105, 122))
            setPadding(0, dp(15), 0, dp(8))
        })
        panel.addView(menuItem("📁", "Explorador de archivos") { showExplorer(workspace.root) })
        panel.addView(menuItem("›_", "Terminal") { showTerminal() })
        panel.addView(TextView(this).apply {
            text = "Droid Windows · Android"
            textSize = 12f
            setTextColor(Color.rgb(100, 111, 126))
            setPadding(0, dp(15), 0, 0)
        })
        val params = FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            setMargins(dp(14), 0, dp(14), dp(82))
        }
        root.addView(panel, params)
        startMenu = panel
    }

    private fun showExplorer(directory: File) {
        currentPage = Page.EXPLORER
        startMenu = null
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(243, 246, 250))
        }
        root.addView(appHeader("Explorador", "Tus archivos", "‹") { showDesktop() })

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(12), dp(12), dp(8))
        }
        actions.addView(actionButton("＋ Carpeta") { promptForName("Nueva carpeta") { name ->
            runCatching { workspace.createDirectory(directory, name) }
                .onSuccess { showExplorer(directory) }
                .onFailure { toast(it.message ?: "No se pudo crear la carpeta") }
        } }, weightParams(dp(42), 1f))
        actions.addView(actionButton("＋ Archivo") { promptForName("Nuevo archivo") { name ->
            runCatching { workspace.createFile(directory, name) }
                .onSuccess { showExplorer(directory) }
                .onFailure { toast(it.message ?: "No se pudo crear el archivo") }
        } }, weightParams(dp(42), 1f))
        actions.addView(actionButton("Abrir…") {
            documentPicker.launch(arrayOf("*/*"))
        }, weightParams(dp(42), 1f))
        root.addView(actions)
        root.addView(TextView(this).apply {
            text = workspace.displayPath(directory)
            textSize = 13f
            setTextColor(Color.rgb(77, 95, 116))
            setPadding(dp(18), dp(4), dp(18), dp(10))
        })

        val scroll = ScrollView(this)
        val entries = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(12), dp(16))
        }
        if (directory != workspace.root) {
            entries.addView(fileRow("⬆", "…", "Carpeta superior") {
                showExplorer(directory.parentFile ?: workspace.root)
            })
        }
        val files = runCatching { workspace.list(directory) }.getOrElse {
            toast(it.message ?: "No se pudo abrir la carpeta")
            emptyList()
        }
        if (files.isEmpty() && directory == workspace.root) {
            entries.addView(TextView(this).apply {
                text = "Esta carpeta está vacía. Crea un archivo o una carpeta para empezar."
                textSize = 14f
                setTextColor(Color.rgb(95, 108, 124))
                setPadding(dp(12), dp(22), dp(12), dp(22))
            })
        }
        files.forEach { file ->
            entries.addView(fileRow(
                if (file.isDirectory) "📁" else fileIcon(file),
                file.name,
                if (file.isDirectory) "Carpeta" else fileSize(file.length())
            ) {
                if (file.isDirectory) showExplorer(file) else openWorkspaceFile(file)
            })
        }
        scroll.addView(entries)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun showTerminal() {
        currentPage = Page.TERMINAL
        startMenu = null
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(243, 246, 250))
        }
        root.addView(appHeader("Terminal", "Terminal de Droid Windows", "‹") { showDesktop() })
        val output = TextView(this).apply {
            text = "Droid Windows Terminal\nEscribe «help» para ver los comandos disponibles.\n\n${terminal.prompt()}"
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.rgb(222, 235, 247))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(Color.rgb(21, 31, 45), dp(14))
        }
        val scroll = ScrollView(this).apply {
            addView(output)
            isFillViewport = true
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            setMargins(dp(12), dp(12), dp(12), dp(8))
        })
        val commandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(12))
        }
        val command = EditText(this).apply {
            hint = "Escribe un comando"
            singleLine = true
            textSize = 14f
            setPadding(dp(14), 0, dp(12), 0)
            background = rounded(Color.WHITE, dp(14))
            imeOptions = EditorInfo.IME_ACTION_SEND
        }
        val run = actionButton("Ejecutar") {
            val line = command.text.toString().trim()
            if (line.isNotEmpty()) {
                val result = terminal.execute(line)
                if (line == "clear") output.text = ""
                else output.append("\n${terminal.prompt(line)}\n$result\n")
                command.text.clear()
                scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }
        command.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                run.performClick()
                true
            } else false
        }
        commandRow.addView(command, LinearLayout.LayoutParams(0, dp(50), 1f))
        commandRow.addView(run, LinearLayout.LayoutParams(dp(100), dp(50)).apply {
            setMargins(dp(8), 0, 0, 0)
        })
        root.addView(commandRow)
        setContentView(root)
    }

    private fun appHeader(title: String, subtitle: String, icon: String, onBack: () -> Unit): LinearLayout {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(18), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        header.addView(textButton(icon, 27f).apply {
            setTextColor(Color.rgb(37, 55, 76))
            setOnClickListener { onBack() }
            contentDescription = "Volver al escritorio"
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(TextView(this).apply {
            text = title
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(31, 43, 59))
        })
        titles.addView(TextView(this).apply {
            text = subtitle
            textSize = 12f
            setTextColor(Color.rgb(101, 115, 132))
            setPadding(0, dp(2), 0, 0)
        })
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        return header
    }

    private fun shortcut(icon: String, label: String, action: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), dp(12))
            isClickable = true
            isFocusable = true
            background = rounded(Color.argb(35, 255, 255, 255), dp(14))
            setOnClickListener { action() }
            addView(TextView(this@MainActivity).apply {
                text = icon
                textSize = 27f
                gravity = Gravity.CENTER
            })
            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 12f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, dp(5), 0, 0)
            })
        }.also { it.layoutParams = LinearLayout.LayoutParams(0, dp(94), 1f).apply { setMargins(0, 0, dp(10), 0) } }

    private fun menuItem(icon: String, label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = "$icon   $label"
            textSize = 14f
            isAllCaps = false
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setTextColor(Color.rgb(35, 49, 66))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { action() }
        }

    private fun fileRow(icon: String, name: String, detail: String, action: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(10), dp(13), dp(10))
            background = rounded(Color.WHITE, dp(14))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            val symbol = TextView(this@MainActivity).apply {
                text = icon
                textSize = 21f
                gravity = Gravity.CENTER
            }
            addView(symbol, LinearLayout.LayoutParams(dp(42), dp(42)))
            val labels = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            labels.addView(TextView(this@MainActivity).apply {
                text = name
                textSize = 14f
                setTextColor(Color.rgb(35, 48, 64))
                maxLines = 1
            })
            labels.addView(TextView(this@MainActivity).apply {
                text = detail
                textSize = 12f
                setTextColor(Color.rgb(107, 120, 137))
                setPadding(0, dp(3), 0, 0)
            })
            addView(labels, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(8), 0, 0, 0) })
        }.also { it.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(4)) } }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.rgb(32, 85, 137))
            background = rounded(Color.rgb(226, 238, 250), dp(12))
            setOnClickListener { action() }
        }

    private fun textButton(label: String, size: Float): TextView =
        TextView(this).apply {
            text = label
            textSize = size
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
        }

    private fun promptForName(title: String, onSubmit: (String) -> Unit) {
        val input = EditText(this).apply { hint = "Nombre" }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) onSubmit(name)
            }
            .show()
    }

    private fun openWorkspaceFile(file: File) {
        if (file.extension.lowercase(Locale.ROOT) in setOf("txt", "md", "log", "json", "xml", "py", "kt", "java", "csv")) {
            val content = runCatching { workspace.readText(file) }.getOrElse {
                toast(it.message ?: "No se pudo leer el archivo")
                return
            }
            val text = TextView(this).apply {
                this.text = content.ifEmpty { "(archivo vacío)" }
                textSize = 14f
                typeface = Typeface.MONOSPACE
                setTextColor(Color.rgb(35, 48, 64))
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }
            AlertDialog.Builder(this)
                .setTitle(file.name)
                .setView(ScrollView(this).apply { addView(text) })
                .setPositiveButton("Cerrar", null)
                .show()
        } else {
            try {
                val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                val mime = contentResolver.getType(uri) ?: "*/*"
                startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            } catch (_: ActivityNotFoundException) {
                toast("No hay una aplicación instalada para abrir este archivo")
            } catch (error: IllegalArgumentException) {
                toast(error.message ?: "No se pudo abrir el archivo")
            }
        }
    }

    private fun openExternalDocument(uri: Uri) {
        val mime = contentResolver.getType(uri).orEmpty()
        val name = runCatching {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull() ?: "Archivo seleccionado"
        if (mime.startsWith("text/") || mime in setOf("application/json", "application/xml")) {
            val content = runCatching {
                contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val chars = CharArray(256 * 1024 + 1)
                    val count = reader.read(chars)
                    if (count > 256 * 1024) "Vista previa limitada a 256 KB." else String(chars, 0, count.coerceAtLeast(0))
                }.orEmpty()
            }.getOrElse { "No se pudo leer el archivo: ${it.message}" }
            val text = TextView(this).apply {
                this.text = content.ifEmpty { "(archivo vacío)" }
                textSize = 14f
                typeface = Typeface.MONOSPACE
                setTextColor(Color.rgb(35, 48, 64))
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }
            AlertDialog.Builder(this)
                .setTitle(name)
                .setView(ScrollView(this).apply { addView(text) })
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("Abrir con…") { _, _ -> launchExternalViewer(uri, mime) }
                .show()
        } else {
            launchExternalViewer(uri, mime)
        }
    }

    private fun launchExternalViewer(uri: Uri, mime: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime.ifEmpty { "*/*" })
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (_: ActivityNotFoundException) {
            toast("No hay una aplicación instalada para abrir este archivo")
        }
    }

    private fun fileIcon(file: File) = when (file.extension.lowercase(Locale.ROOT)) {
        "txt", "md", "log" -> "📄"
        "jpg", "jpeg", "png", "gif", "webp" -> "🖼️"
        "pdf" -> "📕"
        "mp3", "wav", "ogg" -> "🎵"
        "mp4", "mkv" -> "🎞️"
        else -> "📃"
    }

    private fun fileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f))
    }

    private fun weightParams(height: Int, weight: Float) =
        LinearLayout.LayoutParams(0, height, weight).apply { setMargins(dp(3), 0, dp(3), 0) }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
