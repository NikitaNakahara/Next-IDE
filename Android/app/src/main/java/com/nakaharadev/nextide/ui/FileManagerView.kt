package com.nakaharadev.nextide.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.nakaharadev.nextide.R
import java.io.File

class FileManagerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var root: File? = null

    init {
        orientation = VERTICAL
    }

    fun setFilesRoot(root: File) {
        this.root = root

        for (elem in root.listFiles()!!) {
            if (elem.isFile) {
                addFileToList(elem, this, 0)
            } else {
                addDirToList(elem, this, 0)
            }
        }
    }

    private fun addDirToList(dir: File, root: LinearLayout, index: Int) {
        val rootDir = LinearLayout(context)
        rootDir.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        rootDir.orientation = VERTICAL

        val dirField = LayoutInflater.from(context).inflate(R.layout.dir_elem, null)
        dirField.findViewById<TextView>(R.id.dir_name).text = dir.name
        root.addView(dirField)

        val contentLayout = LinearLayout(context)
        contentLayout.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        contentLayout.orientation = VERTICAL
        contentLayout.setPadding(contentLayout.paddingStart + dpToPx(5f).toInt() * index, 0, 0, 0)

        for (elem in dir.listFiles()!!) {
            if (elem.isFile) {
                addFileToList(elem, contentLayout, index + 1)
            } else {
                addDirToList(elem, contentLayout, index + 1)
            }
        }
    }

    private fun addFileToList(file: File, root: LinearLayout, index: Int) {
        val fileField = LayoutInflater.from(context).inflate(R.layout.file_elem, null)
        fileField.findViewById<TextView>(R.id.file_name).text = file.name
        root.addView(fileField)

        val fileNameSplit = file.name.split('.')
        if (fileNameSplit.size == 1) {
            fileField.findViewById<ImageView>(R.id.file_icon).setImageResource(getIconIdForFile(null))
        } else {
            val iconId = getIconIdForFile(fileNameSplit[fileNameSplit.size - 1])
            if (iconId != 0) {
                fileField.findViewById<ImageView>(R.id.file_icon).setImageResource(iconId)
            }
        }

        fileField.setPadding(fileField.paddingStart + dpToPx(5f).toInt() * index, 0, 0, 0)
    }

    private fun getIconIdForFile(end: String?): Int {
        if (end == "json") {
            return R.drawable.json_file_icon
        }

        return 0
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}