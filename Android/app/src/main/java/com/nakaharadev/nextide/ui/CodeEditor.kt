package com.nakaharadev.nextide.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.nakaharadev.nextide.R
import com.nakaharadev.nextide.langs.highlight.HighLight
import java.io.ByteArrayOutputStream
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
    private var headerTextSize = 45f
    private var textPadding = 7f

    private var linesBarWidth = 70f

    private var scrollX = 0f
    private var scrollY = 0f

    private var textOffsetX = 0f
    private var textOffsetY = 0f
    private var downX = 0f
    private var downY = 0f

    private var headerTextOffset = 0f

    private var cursorX = 0
    private var cursorY = 0

    private var isCodeScroll = false

    private var fullHeaderLength = 30f

    companion object {
        const val FILES_LIST_HEIGHT_DP = 40f
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)

        if (files.size > 0) {
            paint.color = context.getColor(R.color.window_bg)
            paint.style = Paint.Style.FILL

            val rect = RectF()
            rect.left = 0f
            rect.top = 0f
            rect.right = width.toFloat()
            rect.bottom = _dpToPx(FILES_LIST_HEIGHT_DP)

            canvas.drawRect(rect, paint)

            paint.isAntiAlias = true
            paint.setTypeface(Typeface.MONOSPACE)
            paint.textSize = headerTextSize
            paint.style = Paint.Style.FILL
            paint.color = context.getColor(R.color.light_gray_text)

            var barWidth = 30f
            for (i in files.indices) {
                if (files[i] == currentOpenedFile) {
                    paint.color = context.getColor(R.color.main_ui_1)
                    rect.left = barWidth + 25f + headerTextOffset
                    rect.top = _dpToPx(FILES_LIST_HEIGHT_DP) - 5f
                    rect.right = barWidth + 25f + headerTextOffset + paint.measureText(files[i].name)
                    rect.bottom = _dpToPx(FILES_LIST_HEIGHT_DP)

                    paint.color = context.getColor(R.color.main_ui_1)

                    canvas.drawRect(rect, paint)

                    paint.color = context.getColor(R.color.light_gray_text)
                }

                canvas.drawText(files[i].name, barWidth + 25f + headerTextOffset, _dpToPx(FILES_LIST_HEIGHT_DP) / 2 + headerTextSize / 2, paint)
                barWidth += paint.measureText(files[i].name) + 25f
            }
        }

        if (currentOpenedFile != null) {
            currentOpenedFile?.print(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                isCodeScroll = event.y > _dpToPx(FILES_LIST_HEIGHT_DP)

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
                val deltaX = event.x - scrollX
                scrollX = event.x

                if (isCodeScroll) {
                    if (currentOpenedFile?.maxWidth == null) return false

                    if (currentOpenedFile?.maxWidth!! * paint.measureText(" ") > width) {
                        textOffsetX += deltaX

                        if (textOffsetX > 0) textOffsetX = 0f

                        if (textOffsetX < width - 200f - (currentOpenedFile?.maxWidth!! * paint.measureText(" "))) {
                            textOffsetX = width - 200f - (currentOpenedFile?.maxWidth!! * paint.measureText(" "))
                        }
                    }
                } else {
                    if (fullHeaderLength > width) {
                        headerTextOffset += deltaX

                        if (headerTextOffset > 0) headerTextOffset = 0f

                        if (headerTextOffset < width - 60f - fullHeaderLength) {
                            headerTextOffset = width - 60f - fullHeaderLength
                        }

                        Log.i("Scroll", "$headerTextOffset")
                    }
                }

                invalidate()

                return true
            }
        }

        return false
    }

    fun click(xPos: Float, yPos: Float) {
        if (_clickPosIsHeader(yPos)) {
            val index = _getFileIndex(xPos)
            if (index != -1) {
                currentOpenedFile = files[index]
            }

            textOffsetX = 0f
            textOffsetY = 0f
        } else {
            if (!currentOpenedFile?.isImage!!) {
                requestFocus()

                val inputMethodManager =
                    context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)

                val pos = _getCursorPos(xPos, yPos)
            } else {
                currentOpenedFile?.toggleBinaryDrawMode(xPos, yPos)
            }
        }

        invalidate()
    }

    fun addFile(file: File) {
        if (files.size == 10) {
            files.removeAt(0)
        }

        for (f in files) {
            if (f.equals(file)) {
                currentOpenedFile = f

                invalidate()
                return
            }
        }

        val editable = EditableFile(file)
        files.add(editable)
        currentOpenedFile = editable

        paint.setTypeface(Typeface.MONOSPACE)
        paint.textSize = headerTextSize
        fullHeaderLength += paint.measureText(editable.name) + 25f

        invalidate()
    }

    fun removeFile(file: File) {
        var delFile: EditableFile? = null
        for (f in files) {
            if (f.equals(file)) {
                delFile = f
            }
        }
        files.remove(delFile)

        if (currentOpenedFile != null) {
            if (currentOpenedFile!!.equals(file)) {
                currentOpenedFile = if (files.size >= 1) files[0] else null
            }
        }

        invalidate()
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
            resources.displayMetrics
        )
    }

    private fun _clickPosIsHeader(y: Float): Boolean {
        return y < _dpToPx(FILES_LIST_HEIGHT_DP)
    }

    private fun _getFileIndex(xPos: Float): Int {
        var x = 30f

        for (i in files.indices) {
            if (xPos > x + headerTextOffset && xPos < x + paint.measureText(files[i].name) + headerTextOffset)
                return i

            paint.setTypeface(Typeface.MONOSPACE)
            paint.textSize = headerTextSize
            x += paint.measureText(files[i].name) + 25f
        }

        return -1
    }

    private fun _getVectorBitmap(context: Context, drawableId: Int): Bitmap? {
        var bitmap: Bitmap? = null
        when (val drawable = ContextCompat.getDrawable(context, drawableId)) {
            is BitmapDrawable -> {
                bitmap = drawable.bitmap
            }
            is VectorDrawable -> {
                bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }
        return bitmap
    }

    @OptIn(ExperimentalStdlibApi::class)
    private inner class EditableFile(val file: File) {
        var name: String
        var lines = ArrayList<String>()
        var highLight: HighLight? = null
        var fileType = ""
        var isImage = false
        var isBinary = false
        var image: Bitmap? = null
        var bytesArray: List<String>? = null

        var maxWidth = 0f

        val toggleModeDst = Rect()

        init {
            toggleModeDst.top = _dpToPx(FILES_LIST_HEIGHT_DP + 20f).toInt()
            toggleModeDst.bottom = toggleModeDst.top + _dpToPx(30f).toInt()
            toggleModeDst.right = width - _dpToPx(20f).toInt()
            toggleModeDst.left = toggleModeDst.right - toggleModeDst.height()

            name = file.name

            val nameSepArr = name.split('.')
            if (nameSepArr.size > 1) fileType = nameSepArr[nameSepArr.size - 1]
            isImage = fileType == "png" || fileType == "jpg"

            if (!isImage) {
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
            } else {
                image = BitmapFactory.decodeFile(file.path)

                val stream = ByteArrayOutputStream()
                val compressFormat: Bitmap.CompressFormat
                if (fileType == "png") {
                    compressFormat = Bitmap.CompressFormat.PNG
                } else {
                    compressFormat = Bitmap.CompressFormat.JPEG
                }
                image?.compress(compressFormat, 100, stream)
                val arr = stream.toByteArray()
                val hexFormat = HexFormat {
                    bytes {
                        byteSeparator=" "
                        upperCase=true
                    }
                }
                bytesArray = arr.toHexString(hexFormat).split(" ")
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other == null) return false

            if (other is File) {
                return file.path == other.path
            } else if (other is EditableFile) {
                return file.path == other.file.path
            }

            return false
        }

        fun save() {

        }

        fun toggleBinaryDrawMode(xPos: Float, yPos: Float) {
            if (xPos > toggleModeDst.left && xPos < toggleModeDst.right && yPos > toggleModeDst.top && yPos < toggleModeDst.bottom)
                isBinary = !isBinary
        }

        fun print(canvas: Canvas) {
            if (isImage && !isBinary) {
                val dst = Rect()

                if (image?.width!! < image?.height!!) {
                    dst.top = _dpToPx(FILES_LIST_HEIGHT_DP).toInt()
                    dst.bottom = height

                    val ratio = dst.height().toFloat() / image?.height!!
                    val imageWidth = (image?.width!! * ratio).toInt()

                    dst.left = width / 2 - imageWidth / 2
                    dst.right = dst.left + imageWidth
                } else {
                    dst.left = 0
                    dst.right = width

                    val ratio = dst.width().toFloat() / image?.width!!
                    val imageHeight = (image?.height!! * ratio).toInt()

                    dst.top = height / 2 - imageHeight / 2
                    dst.bottom = dst.top + imageHeight
                }

                canvas.drawBitmap(image!!, null, dst, paint)

                paint.color = Color.BLACK
                canvas.drawCircle(toggleModeDst.left + toggleModeDst.width() / 2f, toggleModeDst.top + toggleModeDst.height() / 2f, _dpToPx(22f), paint)

                paint.color = Color.WHITE
                canvas.drawBitmap(_getVectorBitmap(context, R.drawable.binary_icon)!!, null, toggleModeDst, paint)
            } else if (isBinary) {
                paint.color = resources.getColor(R.color.window_bg)
                canvas.drawRect(0f, _dpToPx(FILES_LIST_HEIGHT_DP), width.toFloat(), height.toFloat(), paint)

                paint.color = Color.BLACK
                canvas.drawRoundRect(0f, toggleModeDst.bottom.toFloat() + 15f, toggleModeDst.left.toFloat() - 40f, height.toFloat(), 20f, 20f, paint)

                paint.color = context.resources.getColor(R.color.main_text)
                paint.setTypeface(Typeface.MONOSPACE)
                paint.textSize = 33f

                var x = 20f
                for (i: Int in 0..<4) {
                    for (j: Int in 0..<4) {
                        canvas.drawText(bytesArray?.get(i * j) ?: "", x, toggleModeDst.bottom + 60f, paint)

                        x += paint.measureText(" ") * 2 + 10f
                    }

                    x += paint.measureText(" ")
                }

                if (isImage) {
                    paint.color = Color.WHITE
                    canvas.drawBitmap(_getVectorBitmap(context, R.drawable.close_binary_icon)!!, null, toggleModeDst, paint)
                }
            } else {
                paint.isAntiAlias = true
                paint.setTypeface(Typeface.MONOSPACE)
                paint.textSize = textSize
                paint.style = Paint.Style.FILL

                for (token in highLight?.getTokens()!!) {
                    paint.color = token.color
                    canvas.drawText(
                        token.lexeme,
                        token.startXPos * paint.measureText(" ") + linesBarWidth + textOffsetX,
                        token.yPos * (textSize + textPadding) + _dpToPx(FILES_LIST_HEIGHT_DP),
                        paint
                    )
                }

                paint.setColor(context.resources.getColor(R.color.window_bg))
                canvas.drawRect(
                    0f,
                    _dpToPx(FILES_LIST_HEIGHT_DP),
                    linesBarWidth,
                    (textSize + textPadding) * (lines.size + 1) + _dpToPx(FILES_LIST_HEIGHT_DP),
                    paint
                )

                paint.setColor(context.resources.getColor(R.color.main_text))
                canvas.drawLine(
                    linesBarWidth,
                    _dpToPx(FILES_LIST_HEIGHT_DP),
                    linesBarWidth,
                    (textSize + textPadding) * (lines.size + 1) + _dpToPx(FILES_LIST_HEIGHT_DP),
                    paint
                )

                paint.color = resources.getColor(R.color.gray_text)
                for (i in lines.indices) {
                    canvas.drawText(
                        (i + 1).toString(),
                        5f,
                        (i + 1) * (textSize + textPadding) + _dpToPx(FILES_LIST_HEIGHT_DP),
                        paint
                    )
                }
            }
        }

        override fun hashCode(): Int {
            var result = file.hashCode()

            result = 31 * result + name.hashCode()
            result = 31 * result + lines.hashCode()
            result = 31 * result + (highLight?.hashCode() ?: 0)
            result = 31 * result + fileType.hashCode()
            result = 31 * result + isImage.hashCode()
            result = 31 * result + (image?.hashCode() ?: 0)
            result = 31 * result + maxWidth.hashCode()

            return result
        }
    }
}