package com.nakaharadev.nextide

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setPadding
import com.nakaharadev.nextide.ui.NDevCodeEditor
import com.nakaharadev.nextide.ui.FileManagerView
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs


class EditorActivity : Activity() {
    var project: Project? = null
    var menuIsOpened = false

    var activityResult: ((reqCode: Int, data: Any?) -> Unit)? = null

    companion object {
        const val ACTIVITY_RESULT_GET_IMAGE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        project = intent.getSerializableExtra("project") as Project

        setContentView(R.layout.editor)

        findViewById<TextView>(R.id.editor_project_name).text = project!!.name

        findViewById<ImageView>(R.id.toggle_menu_state).setOnClickListener {
            _toggleMenuState(it as ImageView)
        }

        _initFileManager(project?.files!!)
        _initMenuButtons()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ACTIVITY_RESULT_GET_IMAGE -> {
                if (data == null) return
                if (data.data == null) return

                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(data.data!!))

                if (activityResult != null) {
                    activityResult!!(requestCode, bitmap)
                }
            }
        }
    }

    private fun _toggleMenuState(menuBtn: ImageView, endCallback: (() -> Unit)? = null) {
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

                if (endCallback != null) endCallback()
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
            if (fileType == FileManagerView.ELEMENT_TYPE_FILE) {
                newFile.createNewFile()
                DataOutputStream(FileOutputStream(newFile)).writeUTF("")
            }
            else newFile.mkdir()

            return@setOnCreateCallback newFile
        }
        fileManager.setOnOpenFileCallback {
            findViewById<NDevCodeEditor>(R.id.editor).addFile(it)
            _toggleMenuState(findViewById(R.id.toggle_menu_state))
        }
        fileManager.setOnDeleteCallback {
            findViewById<NDevCodeEditor>(R.id.editor).removeFile(it)
        }
    }

    private fun _initMenuButtons() {
        findViewById<ImageView>(R.id.import_resource).setOnClickListener {
            _toggleMenuState(findViewById(R.id.toggle_menu_state)) {
                _initImport()
            }
        }
    }

    private fun _initImport() {
        val darkening = findViewById<View>(R.id.darkening)
        val importLayout = findViewById<LinearLayout>(R.id.import_layout)

        importLayout.pivotY = importLayout.height.toFloat()

        findViewById<ImageView>(R.id.import_image_field).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.setType("image/*")
            startActivityForResult(intent, ACTIVITY_RESULT_GET_IMAGE)

            activityResult = { code: Int, data: Any? ->
                if (code == ACTIVITY_RESULT_GET_IMAGE) {
                    data as Bitmap

                    val field = findViewById<ImageView>(R.id.import_image_field)
                    field.setPadding(0)
                    field.scaleType = ImageView.ScaleType.CENTER_CROP
                    field.setImageBitmap(data)

                    findViewById<ImageView>(R.id.import_done).setOnClickListener {
                        val nameEdit = findViewById<EditText>(R.id.image_name_input)
                        if (nameEdit.text.isEmpty()) {
                            nameEdit.hint = "введите значение"
                            nameEdit.setBackgroundResource(R.drawable.empty_text_error)

                            nameEdit.setOnEditorActionListener { _, _, _ ->
                                nameEdit.hint = "имя файла"
                                nameEdit.setBackgroundResource(R.color.transparent)

                                return@setOnEditorActionListener true
                            }
                        } else {
                            val bmpDir = File("${filesDir.path}/projects/${project?.name}/res/images")
                            if (!bmpDir.exists()) bmpDir.mkdir()
                            val bmpFile = File("${bmpDir.path}/${nameEdit.text}${findViewById<Spinner>(R.id.import_image_extensions).selectedItem as String}")
                            _writeImageToFile(data, bmpFile)

                            findViewById<FileManagerView>(R.id.editor_file_manager).sync()

                            activityResult = null

                            val objAnim = ObjectAnimator.ofFloat(darkening, "alpha", .5f, 0f)
                            objAnim.duration = 200
                            objAnim.start()

                            val animator = ValueAnimator.ofFloat(0f, _dpToPx(280f))
                            animator.duration = 200

                            animator.addUpdateListener {
                                importLayout.translationY = it.animatedValue as Float
                            }

                            animator.addListener(object : Animator.AnimatorListener {
                                override fun onAnimationStart(animation: Animator) {}
                                override fun onAnimationCancel(animation: Animator) {}
                                override fun onAnimationRepeat(animation: Animator) {}

                                override fun onAnimationEnd(animation: Animator) {
                                    darkening.isClickable = false

                                    findViewById<ImageView>(R.id.import_image_field).setImageResource(R.drawable.import_img_icon)
                                    findViewById<EditText>(R.id.image_name_input).setText("")
                                }
                            })

                            animator.start()
                        }
                    }
                }
            }
        }

        var objAnim = ObjectAnimator.ofFloat(darkening, "alpha", 0f, .5f)
        objAnim.duration = 200
        objAnim.start()

        var animator = ValueAnimator.ofFloat(_dpToPx(280f), 0f)
        animator.duration = 200

        animator.addUpdateListener {
            importLayout.translationY = it.animatedValue as Float
        }

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                darkening.isClickable = true

                darkening.setOnClickListener {
                    objAnim = ObjectAnimator.ofFloat(darkening, "alpha", 0f, .5f)
                    objAnim.duration = 200
                    objAnim.start()

                    animator = ValueAnimator.ofFloat(0f, _dpToPx(280f))
                    animator.duration = 200

                    animator.addUpdateListener {
                        importLayout.translationY = it.animatedValue as Float
                    }

                    animator.addListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationCancel(animation: Animator) {}
                        override fun onAnimationRepeat(animation: Animator) {}

                        override fun onAnimationEnd(animation: Animator) {
                            darkening.isClickable = false
                        }
                    })

                    animator.start()
                }
            }
        })

        animator.start()
    }

    private fun _writeImageToFile(image: Bitmap, file: File) {
        if (!file.exists()) {
            file.createNewFile()
        }

        val extension = file.name.split('.')[1]

        val compressFormat: CompressFormat
        if (extension == "png") {
            compressFormat = CompressFormat.PNG
        } else {
            compressFormat = CompressFormat.JPEG
        }

        val bos = ByteArrayOutputStream()
        image.compress(compressFormat, 100, bos)
        val data = bos.toByteArray()

        val output = FileOutputStream(file)
        output.write(data)
        output.flush()
        output.close()
    }

    private fun _dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}