package com.nakaharadev.nextide.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.graphics.ColorUtils
import com.nakaharadev.nextide.R
import com.nakaharadev.nextide.ui.editorfile.AbstractFile
import com.nakaharadev.nextide.ui.editorfile.CodeFile
import com.nakaharadev.nextide.ui.editorfile.FileParams
import com.nakaharadev.nextide.ui.editorfile.ImageFile
import java.io.File
import kotlin.math.max


class NDevCodeEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var currentOpenedFileIndex = -1
    private val files = ArrayList<AbstractFile>()
    private var paint = Paint()

    private var headerTextSize = 45f

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

    private var loadBarX = 0
    private var loadBarWidth = 200
    private var loadBarHeight = 0
    private var loadBarAnimator: ValueAnimator? = null

    val loadRect = Rect()
    val headerRect = RectF()

    companion object {
        internal const val FILES_LIST_HEIGHT_DP = 40f
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)

        if (files.size > 0) {
            paint.color = context.getColor(R.color.window_bg)
            paint.style = Paint.Style.FILL

            headerRect.left = 0f
            headerRect.top = 0f
            headerRect.right = width.toFloat()
            headerRect.bottom = _dpToPx(FILES_LIST_HEIGHT_DP)

            canvas.drawRect(headerRect, paint)

            paint.isAntiAlias = true
            paint.setTypeface(Typeface.MONOSPACE)
            paint.textSize = headerTextSize
            paint.style = Paint.Style.FILL
            paint.color = context.getColor(R.color.light_gray_text)

            var barWidth = 30f
            for (i in files.indices) {
                if (i == currentOpenedFileIndex) {
                    paint.color = context.getColor(R.color.main_ui_1)
                    headerRect.left = barWidth + 25f + headerTextOffset
                    headerRect.top = _dpToPx(FILES_LIST_HEIGHT_DP) - 5f
                    headerRect.right = barWidth + 25f + headerTextOffset + paint.measureText(files[i].name)
                    headerRect.bottom = _dpToPx(FILES_LIST_HEIGHT_DP)

                    canvas.drawRect(headerRect, paint)

                    paint.color = context.getColor(R.color.light_gray_text)
                }

                canvas.drawText(files[i].name, barWidth + 25f + headerTextOffset, _dpToPx(FILES_LIST_HEIGHT_DP) / 2 + headerTextSize / 2, paint)
                barWidth += paint.measureText(files[i].name) + 25f
            }
        }

        if (currentOpenedFileIndex > -1) {
            files[currentOpenedFileIndex].print(canvas)
        }
        loadRect.top = if (currentOpenedFileIndex > -1) _dpToPx(FILES_LIST_HEIGHT_DP).toInt() else 0
        loadRect.left = loadBarX
        loadRect.right = loadBarX + loadBarWidth + loadBarX + (loadBarWidth / 100)
        loadRect.bottom = loadRect.top + loadBarHeight
        paint.color = ColorUtils.blendARGB(context.getColor(R.color.main_ui_1), context.getColor(R.color.main_ui_3), max(loadBarX / width.toFloat(), 0f))
        canvas.drawRect(loadRect, paint)
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
                    _click(event.x, event.y)
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - scrollX
                scrollX = event.x

                if (isCodeScroll) {
                    if (currentOpenedFileIndex == -1) return false

                    files[currentOpenedFileIndex].move(deltaX, 0f)
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

    fun addFile(file: File) {
        _showLoadBar()

        if (files.size == 10) {
            files.removeAt(0)
        }

        for (i in files.indices) {
            if (files[i] == file) {
                currentOpenedFileIndex = i

                _hideLoadBar()

                invalidate()
                return
            }
        }

        AbstractFile.getInstance(FileParams(
            context,
            file,
            width, height
        )) {
            files.add(it)
            currentOpenedFileIndex = files.size - 1

            paint.setTypeface(Typeface.MONOSPACE)
            paint.textSize = headerTextSize
            fullHeaderLength += paint.measureText(it.name) + 25f

            _hideLoadBar()

            postInvalidate()
        }
    }

    fun removeFile(file: File) {
        var delFile: AbstractFile? = null
        for (f in files) {
            if (f == file) {
                delFile = f
            }
        }

        if (currentOpenedFileIndex > -1) {
            if (files[currentOpenedFileIndex] == file) {
                currentOpenedFileIndex = if (files.size >= 1) 0 else -1
            } else {
                if (currentOpenedFileIndex > 0) {
                    currentOpenedFileIndex--
                }
            }
        }


        files.remove(delFile)

        invalidate()
    }

    private fun _click(xPos: Float, yPos: Float) {
        if (currentOpenedFileIndex == -1) return

        if (_clickPosIsHeader(yPos)) {
            _changeOpenedFile(_getFileIndex(xPos))
        } else {
            if (files[currentOpenedFileIndex] is CodeFile) {
                requestFocus()

                val inputMethodManager =
                    context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)

                (files[currentOpenedFileIndex] as CodeFile).click(xPos, yPos)
            } else {
                (files[currentOpenedFileIndex] as ImageFile).toggleBinaryDrawMode(xPos, yPos)
            }
        }

        invalidate()
    }

    private fun _changeOpenedFile(index: Int) {
        if (index != -1) {
            currentOpenedFileIndex = index
        }

        textOffsetX = 0f
        textOffsetY = 0f

        val headerOffsetAnimator = ValueAnimator.ofFloat(headerTextOffset, _calcHeaderTextOffsetAfterChanging(index))
        headerOffsetAnimator.duration = 150
        headerOffsetAnimator.addUpdateListener {
            headerTextOffset = it.animatedValue as Float

            postInvalidate()
        }

        headerOffsetAnimator.start()


    }

    private fun _calcHeaderTextOffsetAfterChanging(newIndex: Int): Float {
        if (newIndex == -1) return headerTextOffset
        if (newIndex == 0) return 0f
        if (newIndex == files.size - 1) return (width - fullHeaderLength) - 60f

        val globalTextPos = _calcHeaderTextPos(newIndex)
        val shiftedTextPos = globalTextPos + headerTextOffset

        paint.setTypeface(Typeface.MONOSPACE)
        paint.textSize = headerTextSize
        val textSize = paint.measureText(files[newIndex].name)

        if (shiftedTextPos > 0 && shiftedTextPos + textSize < width) return headerTextOffset

        if (shiftedTextPos < 0) return -globalTextPos + 30f

        if (shiftedTextPos + textSize + 60f > width) return headerTextOffset - textSize

        return headerTextOffset
    }

    private fun _calcHeaderTextPos(index: Int): Float {
        var result = 30f

        paint.setTypeface(Typeface.MONOSPACE)
        paint.textSize = headerTextSize

        for (i in 0 until index) {
            result += paint.measureText(files[i].name) + 25f
        }

        return result
    }

    private fun _showLoadBar() {
        loadBarHeight = 2

        loadBarAnimator = ValueAnimator.ofInt(-loadBarWidth, width)
        loadBarAnimator?.duration = 1500
        loadBarAnimator?.repeatCount = ValueAnimator.INFINITE

        loadBarAnimator?.addUpdateListener {
            loadBarX = it.animatedValue as Int
            invalidate()
        }

        loadBarAnimator?.start()
    }

    private fun _hideLoadBar() {
        if (loadBarAnimator != null) {
            post {
                loadBarHeight = 0
                loadBarAnimator?.end()
                loadBarAnimator = null
            }
        }
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
}