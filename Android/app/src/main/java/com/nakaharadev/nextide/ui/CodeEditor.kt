package com.nakaharadev.nextide.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream


class CodeEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var currentOpenedFile: EditableFile? = null
    private val files = ArrayList<EditableFile>()

    override fun onDraw(canvas: Canvas) {
        if (currentOpenedFile != null) {
            val shadowPaint = Paint()
            shadowPaint.isAntiAlias = true
            shadowPaint.setColor(Color.WHITE)
            shadowPaint.textSize = 40.0f
            shadowPaint.style = Paint.Style.FILL

            var y = 40f
            for (line in currentOpenedFile!!.lines) {
                canvas.drawText(line, 20f, y, shadowPaint)
                y += 40f
            }
        }
    }

    fun addFile(file: File) {
        if (files.size == 10) {
            files.removeAt(0)
        }

        val editable = EditableFile(file)
        files.add(editable)
        currentOpenedFile = editable

        invalidate()
    }

    private inner class EditableFile(file: File) {
        var name: String
        var lines = ArrayList<String>()

        init {
            name = file.name

            val input = DataInputStream(FileInputStream(file))
            val data = input.readUTF()
            input.close()

            var line = ""
            for (c in data) {
                if (c == '\n') {
                    lines.add(line)
                    line = ""
                } else {
                    line += c
                }
            }
            lines.add(line)
        }
    }
}