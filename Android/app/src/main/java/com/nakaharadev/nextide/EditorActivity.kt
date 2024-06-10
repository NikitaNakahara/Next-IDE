package com.nakaharadev.nextide

import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.nakaharadev.nextide.ui.CodeEditor
import com.nakaharadev.nextide.ui.FileManagerView
import java.io.File

class EditorActivity : Activity() {
    var project: Project? = null
    var menuIsOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        project = intent.getSerializableExtra("project") as Project

        setContentView(R.layout.editor)

        findViewById<TextView>(R.id.editor_project_name).text = project!!.name

        findViewById<ImageView>(R.id.toggle_menu_state).setOnClickListener {
            toggleMenuState(it as ImageView)
        }

        initFileManager(project?.files!!)
    }

    private fun toggleMenuState(menuBtn: ImageView) {
        val animator: ObjectAnimator

        if (menuIsOpened) {
            menuBtn.setImageResource(R.drawable.opened_menu_icon)
            animator = ObjectAnimator.ofFloat(findViewById(R.id.editor_menu_layout), "translationX", 0f, dpToPx(-300f))
        } else {
            menuBtn.setImageResource(R.drawable.menu_burger)
            animator = ObjectAnimator.ofFloat(findViewById(R.id.editor_menu_layout), "translationX", dpToPx(-300f), 0f)
        }

        animator.duration = 300
        animator.start()

        if (menuBtn.drawable is Animatable) {
            (menuBtn.drawable as AnimatedVectorDrawable).start()
        }

        menuIsOpened = !menuIsOpened
    }

    private fun initFileManager(root: File) {
        val fileManager = findViewById<FileManagerView>(R.id.editor_file_manager)
        fileManager.setFilesRoot(root)
        fileManager.setOnCreateCallback { fileName, fileType ->
            Toast.makeText(this@EditorActivity, "$fileName | $fileType", Toast.LENGTH_SHORT).show()
            val newFile = File(fileName)
            if (fileType == FileManagerView.ELEMENT_TYPE_FILE) newFile.createNewFile()
            else newFile.mkdir()

            return@setOnCreateCallback newFile
        }
        fileManager.setOnOpenFileCallback {
            findViewById<CodeEditor>(R.id.editor).addFile(it)
            toggleMenuState(findViewById(R.id.toggle_menu_state))
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}