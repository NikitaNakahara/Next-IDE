package com.nakaharadev.nextide.ui.editorfile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import com.nakaharadev.nextide.R
import com.nakaharadev.nextide.langs.highlight.HighLight
import com.nakaharadev.nextide.ui.NDevCodeEditor
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.InvocationTargetException

class CodeFile(
    val context: Context,
    override var file: File,
    val fileType: String,
    val windowWidth: Int,
    val windowHeight: Int,
    onCreatedCallback: (CodeFile) -> Unit
) : AbstractFile() {
    private val paint = Paint()

    private val textSize = 40f
    private val textPadding = 7f
    private val linesBarWidth = 70f

    private var textOffsetX = 0f
    private var maxWidth = 0f

    private lateinit var lines: ArrayList<String>
    private lateinit var highLight: HighLight

    private var filesListHeight = 0f

    init {
        Thread {
            super.name = file.name

            filesListHeight = _dpToPx(NDevCodeEditor.FILES_LIST_HEIGHT_DP)

            var data = ""
            try {
                val input = DataInputStream(FileInputStream(file))
                data = input.readUTF()
                input.close()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }

            lines = ArrayList()

            var line = ""
            for (c in data) {
                if (c == '\n') {
                    lines.add(line)
                    if (line.length > maxWidth) maxWidth = line.length.toFloat()

                    line = ""
                } else {
                    line += c
                }
            }
            lines.add(line)

            highLight = HighLight.getInstance(fileType, lines)
            highLight.initHighLight()

            onCreatedCallback(this)
        }.start()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false

        if (other is File) {
            return file.path == other.path
        } else if (other is AbstractFile) {
            return file.path == other.file.path
        }

        return false
    }

    override fun move(deltaX: Float, deltaY: Float) {
        if (maxWidth * paint.measureText(" ") > windowWidth) {
            textOffsetX += deltaX

            if (textOffsetX > 0) textOffsetX = 0f

            if (textOffsetX < windowWidth - 200f - (maxWidth * paint.measureText(" "))) {
                textOffsetX = windowWidth - 200f - (maxWidth * paint.measureText(" "))
            }
        }
    }

    fun click(xPos: Float, yPos: Float) {

    }

    override fun save() {

    }

    override fun print(canvas: Canvas) {
        paint.isAntiAlias = true
        paint.setTypeface(Typeface.MONOSPACE)
        paint.textSize = textSize
        paint.style = Paint.Style.FILL

        for (token in highLight.getTokens()) {
            paint.color = token.color
            canvas.drawText(
                token.lexeme,
                token.startXPos * paint.measureText(" ") + linesBarWidth + textOffsetX,
                token.yPos * (textSize + textPadding) + _dpToPx(NDevCodeEditor.FILES_LIST_HEIGHT_DP),
                paint
            )
        }

        paint.setColor(context.getColor(R.color.window_bg))
        canvas.drawRect(
            0f,
            filesListHeight,
            linesBarWidth,
            (textSize + textPadding) * (lines.size + 1) + _dpToPx(NDevCodeEditor.FILES_LIST_HEIGHT_DP),
            paint
        )

        paint.setColor(context.getColor(R.color.main_text))
        canvas.drawLine(
            linesBarWidth,
            filesListHeight,
            linesBarWidth,
            (textSize + textPadding) * (lines.size + 1) + _dpToPx(NDevCodeEditor.FILES_LIST_HEIGHT_DP),
            paint
        )

        paint.color = context.getColor(R.color.gray_text)
        for (i in lines.indices) {
            canvas.drawText(
                (i + 1).toString(),
                5f,
                (i + 1) * (textSize + textPadding) + _dpToPx(NDevCodeEditor.FILES_LIST_HEIGHT_DP),
                paint
            )
        }
    }

    private fun _getCursorPos(xPos: Int, yPos: Int): Pair<Float, Float> {
        return Pair(
            _getCursorX(xPos),
            _getCursorY(yPos)
        )
    }

    private fun _getCursorPos(xPos: Float, yPos: Float): Pair<Int, Int> {
        return Pair(
            _getCursorX(xPos),
            _getCursorY(yPos)
        )
    }

    private fun _getCursorX(xPos: Int): Float {
        return 0f
    }

    private fun _getCursorY(yPos: Int): Float {
        return 0f
    }

    private fun _getCursorX(xPos: Float): Int {
        return 0
    }

    private fun _getCursorY(yPos: Float): Int {
        return 0
    }

    private fun _dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + file.hashCode()
        result = 31 * result + fileType.hashCode()
        result = 31 * result + windowWidth
        result = 31 * result + windowHeight
        result = 31 * result + paint.hashCode()
        result = 31 * result + textSize.hashCode()
        result = 31 * result + textPadding.hashCode()
        result = 31 * result + linesBarWidth.hashCode()
        result = 31 * result + textOffsetX.hashCode()
        result = 31 * result + maxWidth.hashCode()
        result = 31 * result + lines.hashCode()
        result = 31 * result + highLight.hashCode()
        return result
    }

    override fun toString(): String {
        return """
            {
                "name": CodeFile,
                "fileName": ${file.name},
                "fileType": $fileType,
                "textSize": $textSize,
                "textPadding": $textPadding,
                "linesBarWidth": $linesBarWidth,
                "textOffsetX": $textOffsetX,
                "maxWidth": $maxWidth
            }
        """.trimIndent()
    }
}