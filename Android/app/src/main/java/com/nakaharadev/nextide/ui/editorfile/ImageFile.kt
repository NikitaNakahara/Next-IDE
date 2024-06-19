package com.nakaharadev.nextide.ui.editorfile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.nakaharadev.nextide.R
import com.nakaharadev.nextide.ui.CodeEditor
import java.io.ByteArrayOutputStream
import java.io.File

@OptIn(ExperimentalStdlibApi::class)
class ImageFile(
    val context: Context,
    override var file: File,
    val fileType: String,
    val windowWidth: Int,
    val windowHeight: Int,
    onCreatedCallback: (ImageFile) -> Unit
) : AbstractFile() {
    private val paint = Paint()
    private val toggleModeDst = Rect()

    private var isBinary = false
    private var image: Bitmap? = null
    private var bytesArray: List<String>? = null

    init {
        Thread {
            toggleModeDst.top = _dpToPx(CodeEditor.FILES_LIST_HEIGHT_DP + 20f).toInt()
            toggleModeDst.bottom = toggleModeDst.top + _dpToPx(30f).toInt()
            toggleModeDst.right = windowWidth - _dpToPx(20f).toInt()
            toggleModeDst.left = toggleModeDst.right - toggleModeDst.height()

            super.name = file.name

            image = BitmapFactory.decodeFile(file.path)

            val stream = ByteArrayOutputStream()
            image?.compress(
                if (fileType == "png")
                    Bitmap.CompressFormat.PNG
                else
                    Bitmap.CompressFormat.JPEG,
                100, stream)
            val arr = stream.toByteArray()
            val hexFormat = HexFormat {
                bytes {
                    byteSeparator=" "
                    upperCase=true
                }
            }
            bytesArray = arr.toHexString(hexFormat).split(" ")

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

    override fun move(deltaX: Float, deltaY: Float) {}

    override fun save() {

    }

    fun toggleBinaryDrawMode(xPos: Float, yPos: Float) {
        if (xPos > toggleModeDst.left && xPos < toggleModeDst.right && yPos > toggleModeDst.top && yPos < toggleModeDst.bottom)
            isBinary = !isBinary
    }

    override fun print(canvas: Canvas) {
        if (!isBinary) {
            val dst = Rect()

            if (image?.width!! < image?.height!!) {
                dst.top = _dpToPx(CodeEditor.FILES_LIST_HEIGHT_DP).toInt()
                dst.bottom = windowHeight

                val ratio = dst.height().toFloat() / image?.height!!
                val imageWidth = (image?.width!! * ratio).toInt()

                dst.left = windowWidth / 2 - imageWidth / 2
                dst.right = dst.left + imageWidth
            } else {
                dst.left = 0
                dst.right = windowWidth

                val ratio = dst.width().toFloat() / image?.width!!
                val imageHeight = (image?.height!! * ratio).toInt()

                dst.top = windowHeight / 2 - imageHeight / 2
                dst.bottom = dst.top + imageHeight
            }

            canvas.drawBitmap(image!!, null, dst, paint)

            paint.color = Color.BLACK
            canvas.drawCircle(toggleModeDst.left + toggleModeDst.width() / 2f, toggleModeDst.top + toggleModeDst.height() / 2f, _dpToPx(22f), paint)

            paint.color = Color.WHITE
            canvas.drawBitmap(_getVectorBitmap(context, R.drawable.binary_icon)!!, null, toggleModeDst, paint)
        } else {
            paint.color = context.resources.getColor(R.color.window_bg)
            canvas.drawRect(0f, _dpToPx(CodeEditor.FILES_LIST_HEIGHT_DP), windowWidth.toFloat(), windowHeight.toFloat(), paint)

            paint.color = Color.BLACK
            canvas.drawRoundRect(-20f, toggleModeDst.bottom.toFloat() + 15f, toggleModeDst.left.toFloat() - 60f, windowHeight.toFloat(), 20f, 20f, paint)

            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(-20f, toggleModeDst.bottom.toFloat() + 15f, toggleModeDst.left.toFloat() - 60f, windowHeight.toFloat(), 20f, 20f, paint)

            paint.style = Paint.Style.FILL

            paint.color = context.resources.getColor(R.color.main_text)
            paint.setTypeface(Typeface.MONOSPACE)
            paint.textSize = 33f

            var x = 10f
            for (i: Int in 0..<4) {
                for (j: Int in 0..<4) {
                    canvas.drawText(bytesArray?.get(i * j) ?: "", x, toggleModeDst.bottom + 60f, paint)

                    x += paint.measureText(" ") * 2 + 10f
                }

                x += paint.measureText(" ")
            }

            paint.color = Color.WHITE
            canvas.drawBitmap(_getVectorBitmap(context, R.drawable.close_binary_icon)!!, null, toggleModeDst, paint)
        }
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

    private fun _dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    override fun toString(): String {
        return """
            {
                "name": CodeFile,
                "fileName": ${file.name},
                "fileType": $fileType
            }
        """.trimIndent()
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + file.hashCode()
        result = 31 * result + fileType.hashCode()
        result = 31 * result + windowWidth
        result = 31 * result + windowHeight
        result = 31 * result + paint.hashCode()
        result = 31 * result + toggleModeDst.hashCode()
        result = 31 * result + isBinary.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + (bytesArray?.hashCode() ?: 0)
        return result
    }
}