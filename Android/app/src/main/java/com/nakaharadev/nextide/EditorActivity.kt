package com.nakaharadev.nextide

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.nakaharadev.nextide.ui.CodeEditor
import com.nakaharadev.nextide.ui.FileManagerView
import java.io.File
import kotlin.math.abs

class EditorActivity : Activity() {
    var project: Project? = null
    var menuIsOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        project = intent.getSerializableExtra("project") as Project

        setContentView(R.layout.editor)

        findViewById<TextView>(R.id.editor_project_name).text = project!!.name

        findViewById<ImageView>(R.id.toggle_menu_state).setOnClickListener {
            _toggleMenuState(it as ImageView)
        }

        _initFileManager(project?.files!!)
    }

    private fun _toggleMenuState(menuBtn: ImageView) {
        val animator: ValueAnimator

        if (menuIsOpened) {
            menuBtn.setImageResource(R.drawable.opened_menu_icon)
            animator = ValueAnimator.ofFloat( 0f, _dpToPx(-300f))
        } else {
            menuBtn.setImageResource(R.drawable.menu_burger)
            animator = ValueAnimator.ofFloat(_dpToPx(-300f), 0f)
        }

        val view = findViewById<LinearLayout>(R.id.editor_menu_layout)
        val darkening = findViewById<View>(R.id.darkening)

        animator.addUpdateListener {
            view.translationX = it.animatedValue as Float
            darkening.alpha = abs((-(it.animatedValue as Float) / _dpToPx(300f)) - 1f) * .5f
        }

        animator.addListener(object: Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                darkening.isClickable = !darkening.isClickable

                if (darkening.isClickable) {
                    darkening.setOnClickListener {
                        _toggleMenuState(menuBtn)
                    }
                }
            }
        })

        animator.duration = 300
        animator.start()

        if (menuBtn.drawable is Animatable) {
            (menuBtn.drawable as AnimatedVectorDrawable).start()
        }

        menuIsOpened = !menuIsOpened
    }

    private fun _initFileManager(root: File) {
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
            _toggleMenuState(findViewById(R.id.toggle_menu_state))
        }
    }

    private fun _dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}