package com.nakaharadev.nextide.ui

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.nakaharadev.nextide.R
import com.nakaharadev.nextide.langs.highlight.HighLight
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.InvocationTargetException


class CodeEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var currentOpenedFile: EditableFile? = null
    private val files = ArrayList<EditableFile>()
    private var paint = Paint()

    private var textSize = 40f
    private var textPadding = 7f

    private var linesBarWidth = 70f

    private var scrollX = 0f
    private var scrollY = 0f

    private var textOffsetX = 0f
    private var textOffsetY = 0f
    private var downX = 0f
    private var downY = 0f

    private var cursorX = 0f
    private var cursorY = 0f

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)

        if (currentOpenedFile != null) {
            currentOpenedFile?.print(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                scrollX = event.x
                scrollY = event.y
                downX = event.x
                downY = event.y

                return true
            }

            MotionEvent.ACTION_UP -> {
                if (downX == event.x && downY == event.y) {
                    click(event.x, event.y)
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (currentOpenedFile?.maxWidth == null) return false

                if (currentOpenedFile?.maxWidth!! * paint.measureText(" ") > width) {
                    val deltaX = event.x - scrollX
                    scrollX = event.x

                    textOffsetX += deltaX

                    if (textOffsetX > 0) textOffsetX = 0f
                    if (currentOpenedFile?.maxWidth!! * paint.measureText(" ") > width) {
                        if (textOffsetX < width - 200f - (currentOpenedFile?.maxWidth!! * paint.measureText(" "))) {
                            textOffsetX = width - 200f - (currentOpenedFile?.maxWidth!! * paint.measureText(" "))
                        }
                    } else {
                        textOffsetX = 0f
                    }

                    invalidate()
                }

                return true
            }
        }

        return false
    }

    fun click(xPos: Float, yPos: Float) {
        requestFocus()

        val inputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)

        Toast.makeText(context, "Click: $xPos | $yPos", Toast.LENGTH_SHORT).show()
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

    fun removeFile(file: File) {
        for (f in files) {
            if (f.equals(file)) {
                files.remove(f)
            }
        }

        if (currentOpenedFile != null) {
            if (currentOpenedFile!!.equals(file)) {
                currentOpenedFile = if (files.size >= 1) files[0] else null
            }
        }

        invalidate()
    }

    private inner class EditableFile(val file: File) {
        var name: String
        var lines = ArrayList<String>()
        var highLight: HighLight? = null
        var fileType = ""

        var maxWidth = 0f

        init {
            name = file.name

            val nameSepArr = name.split('.')
            if (nameSepArr.size > 1) fileType = nameSepArr[nameSepArr.size - 1]

            var data = ""
            try {
                val input = DataInputStream(FileInputStream(file))
                data = input.readUTF()
                input.close()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }

            var line = ""
            for (c in data) {
                if (c == '\n') {
                    lines.add(line)
                    if (line.length > maxWidth) maxWidth = line.length.toFloat()
                    Log.i("max width", "$maxWidth")

                    line = ""
                } else {
                    line += c
                }
            }
            lines.add(line)

            highLight = HighLight.getInstance(fileType, lines)
            highLight?.initHighLight()
        }

        fun equals(f: File): Boolean {
            return file.path == f.path
        }

        fun save() {

        }

        fun print(canvas: Canvas) {
            paint.isAntiAlias = true
            paint.setTypeface(Typeface.MONOSPACE)
            paint.textSize = textSize
            paint.style = Paint.Style.FILL

            for (token in highLight?.getTokens()!!) {
                paint.color = token.color
                canvas.drawText(token.lexeme, token.startXPos * paint.measureText(" ") + linesBarWidth + textOffsetX, token.yPos * (textSize + textPadding), paint)
            }

            paint.setColor(context.resources.getColor(R.color.window_bg))
            canvas.drawRect(0f, 0f, linesBarWidth, (textSize + textPadding) * (lines.size + 1), paint)

            paint.setColor(context.resources.getColor(R.color.main_text))
            canvas.drawLine(linesBarWidth, 0f, linesBarWidth, (textSize + textPadding) * (lines.size + 1), paint)

            paint.color = resources.getColor(R.color.gray_text)
            for (i in lines.indices) {
                canvas.drawText((i + 1).toString(), 5f, (i + 1) * (textSize + textPadding), paint)
            }
        }
    }
}