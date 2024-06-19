package com.nakaharadev.nextide.ui.editorfile

import android.content.Context
import java.io.File

data class FileParams(
    val context: Context,
    val file: File,
    val windowWidth: Int,
    val windowHeight: Int
)