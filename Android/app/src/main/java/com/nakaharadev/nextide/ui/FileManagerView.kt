package com.nakaharadev.nextide.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import java.io.File

class FileManagerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        orientation = VERTICAL
    }

    fun setFilesRoot(file: File) {

    }
}