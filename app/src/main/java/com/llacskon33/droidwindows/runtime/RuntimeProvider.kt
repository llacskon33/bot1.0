package com.llacskon33.droidwindows.runtime

import android.content.Context
import java.io.File

interface RuntimeProvider {
    val id: String
    fun install(context: Context): Result<Unit>
    fun launch(context: Context, program: File): Result<Unit>
}
