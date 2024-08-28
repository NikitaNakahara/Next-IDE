package com.nakaharadev.nextide.ui.editorfile

import android.graphics.Canvas
import java.io.File

abstract class AbstractFile {
    lateinit var name: String
    open lateinit var file: File

    companion object {
        fun getInstance(params: FileParams, onCreatedCallback: (AbstractFile) -> Unit): AbstractFile {
            val name = params.file.name

            var fileType = ""
            val nameSepArr = name.split('.')
            if (nameSepArr.size > 1) fileType = nameSepArr[nameSepArr.size - 1]
            val isImage = fileType == "png" || fileType == "jpg"

            if (isImage) {
                return ImageFile(
                    params.context,
                    params.file,
                    fileType,
                    params.windowWidth,
                    params.windowHeight,
                    onCreatedCallback
                )
            } else {
                return CodeFile(
                    params.context,
                    params.file,
                    fileType,
                    params.windowWidth,
                    params.windowHeight,
                    onCreatedCallback
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is AbstractFile) return false

        return "${file.path}:$name" == "${other.file.path}:${other.name}"
    }

    abstract fun move(deltaX: Float, deltaY: Float)
    abstract fun save()
    abstract fun print(canvas: Canvas)

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + file.hashCode()

        return result
    }
}