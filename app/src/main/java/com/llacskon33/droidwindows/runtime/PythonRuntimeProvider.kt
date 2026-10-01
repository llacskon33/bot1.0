package com.llacskon33.droidwindows.runtime

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File

object PythonRuntimeProvider : RuntimeProvider {
    override val id: String = "chaquopy"

    override fun install(context: Context): Result<Unit> = runCatching {
        synchronized(this) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context.applicationContext))
            }
        }
    }

    override fun launch(context: Context, program: File): Result<Unit> =
        executeFile(context, program, program.parentFile ?: context.filesDir).map { }

    fun executeCode(context: Context, source: String, workingDirectory: File, filename: String = "<console>"): Result<String> =
        install(context).mapCatching {
            Python.getInstance()
                .getModule("droidwindows_runner")
                .callAttr("run_code", source, workingDirectory.absolutePath, filename)
                .toString()
        }

    fun executeFile(context: Context, program: File, workingDirectory: File): Result<String> =
        install(context).mapCatching {
            Python.getInstance()
                .getModule("droidwindows_runner")
                .callAttr("run_file", program.absolutePath, workingDirectory.absolutePath)
                .toString()
        }
}
