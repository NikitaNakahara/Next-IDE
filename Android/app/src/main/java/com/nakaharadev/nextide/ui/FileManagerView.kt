package com.nakaharadev.nextide.ui

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import com.nakaharadev.nextide.R
import java.io.File

class FileManagerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    companion object {
        const val ELEMENT_TYPE_FILE = 0
        const val ELEMENT_TYPE_DIR = 1
    }

    private var root: File? = null
    private var onCreateCallback: ((fileName: String, fileType: Int) -> File)? = null
    private var onOpenCallback: ((file: File) -> Unit)? = null

    init {
        orientation = VERTICAL
    }

    fun setOnCreateCallback(callback: (fileName: String, fileType: Int) -> File) {
        onCreateCallback = callback
    }

    fun setOnOpenFileCallback(callback: (file: File) -> Unit) {
        onOpenCallback = callback
    }

    fun setFilesRoot(root: File) {
        this.root = root

        for (elem in root.listFiles()!!) {
            if (elem.isFile) {
                addFileToList(elem, this)
            } else {
                addDirToList(elem, this)
            }
        }
    }

    private fun addDirToList(dir: File, root: LinearLayout) {
        val dirField = LayoutInflater.from(context).inflate(R.layout.dir_elem, null)
        dirField.findViewById<TextView>(R.id.dir_name).text = dir.name
        dirField.setOnClickListener {
            openDirContent(dirField as LinearLayout)
        }

        val contentLayout = dirField.findViewById<LinearLayout>(R.id.dir_content)

        dirField.findViewById<LinearLayout>(R.id.dir_title).setOnLongClickListener {
            openDirTools(dir.path, dirField as LinearLayout)

            return@setOnLongClickListener true
        }

        var dirIsOpen = false
        dirField.findViewById<LinearLayout>(R.id.dir_title).setOnClickListener {
            if (!dirIsOpen) openDirContent(dirField.findViewById(R.id.dir_content))
            else closeDirContent(dirField.findViewById(R.id.dir_content))

            dirIsOpen = !dirIsOpen
        }

        for (elem in dir.listFiles()!!) {
            if (elem.isFile) {
                addFileToList(elem, contentLayout)
            } else {
                addDirToList(elem, contentLayout)
            }
        }

        root.addView(dirField)
    }

    private fun addFileToList(file: File, root: LinearLayout) {
        val fileField = LayoutInflater.from(context).inflate(R.layout.file_elem, null)
        fileField.findViewById<TextView>(R.id.file_name).text = file.name
        root.addView(fileField)

        fileField.setOnClickListener {
            onOpenCallback!!(file)
        }

        val fileNameSplit = file.name.split('.')
        if (fileNameSplit.size == 1) {
            fileField.findViewById<ImageView>(R.id.file_icon).setImageResource(getIconIdForFile(null))
        } else {
            val iconId = getIconIdForFile(fileNameSplit[fileNameSplit.size - 1])
            if (iconId != 0) {
                fileField.findViewById<ImageView>(R.id.file_icon).setImageResource(iconId)
            }
        }
    }

    private fun openDirTools(path: String, dir: LinearLayout) {
        dir.findViewById<ImageView>(R.id.dir_add_elem).setOnClickListener {
            createNewElement(path, dir)
        }

        dir.findViewById<LinearLayout>(R.id.dir_title).setOnClickListener {
            val animator = ValueAnimator.ofInt(dpToPx(25f).toInt(), 0)
            animator.duration = 200
            animator.addUpdateListener {
                dir.findViewById<LinearLayout>(R.id.dir_tools).layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, it.animatedValue as Int)
            }
            animator.start()

            var dirIsOpen = false
            it.findViewById<LinearLayout>(R.id.dir_title).setOnClickListener {
                if (!dirIsOpen) openDirContent(dir.findViewById(R.id.dir_content))
                else closeDirContent(dir.findViewById(R.id.dir_content))

                dirIsOpen = !dirIsOpen
            }
        }

        val animator = ValueAnimator.ofInt(0, dpToPx(25f).toInt())
        animator.duration = 200
        animator.addUpdateListener {
            dir.findViewById<LinearLayout>(R.id.dir_tools).layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, it.animatedValue as Int)
        }
        animator.start()
    }

    private fun createNewElement(path: String, dir: LinearLayout) {
        dir.findViewById<ViewFlipper>(R.id.dir_tools_flipper).displayedChild = 1

        val edit = dir.findViewById<EditText>(R.id.editor_new_file_name)

        dir.findViewById<ImageView>(R.id.elem_create_done).setOnClickListener {
            if (edit.text.isNotEmpty()) {
                addFileToList(onCreateCallback!!("$path/${edit.text}", ELEMENT_TYPE_FILE), dir.findViewById(R.id.dir_content))
            }

            dir.findViewById<ViewFlipper>(R.id.dir_tools_flipper).displayedChild = 0
        }

        dir.findViewById<RelativeLayout>(R.id.elem_create_dir_done).setOnClickListener {
            if (edit.text.isNotEmpty()) {
                addDirToList(onCreateCallback!!("$path/${edit.text}", ELEMENT_TYPE_DIR), dir.findViewById(R.id.dir_content))
            }

            dir.findViewById<ViewFlipper>(R.id.dir_tools_flipper).displayedChild = 0
        }
    }

    private fun openDirContent(dir: LinearLayout) {
        dir.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private fun closeDirContent(dir: LinearLayout) {
        dir.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0)
    }

    private fun getIconIdForFile(end: String?): Int {
        if (end == "json") {
            return R.drawable.json_file_icon
        }
        if (end == "next") {
            return R.drawable.next_lang_icon
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