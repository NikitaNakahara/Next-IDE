package com.nakaharadev.nextide

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.ViewFlipper
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.abs


class AppActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.menu)

        loadProjects()
        initNewProject()

        findViewById<ImageView>(R.id.new_project).setOnClickListener {
            findViewById<ViewFlipper>(R.id.menu_flipper).displayedChild = 1
            findViewById<ViewFlipper>(R.id.header_btn_flipper).displayedChild = 1
        }
    }

    private fun initNewProject() {
        findViewById<ViewFlipper>(R.id.header_btn_flipper).setOnClickListener {
            it as ViewFlipper

            findViewById<EditText>(R.id.project_name_edit).setText("")
            findViewById<Spinner>(R.id.target_os_spinner).setSelection(0)
            findViewById<RelativeLayout>(convertProjectTypeToResId(ProjectConstructor.type)).setPadding(0, 0, 0, 0)
            findViewById<RelativeLayout>(R.id.project_type_empty).setPadding(0, 0, 0, dpToPx(3f).toInt())

            findViewById<ViewFlipper>(R.id.menu_flipper).displayedChild = 0
            it.displayedChild = 0
        }

        ProjectConstructor.type = convertProjectTypeToString(R.id.project_type_empty)

        findViewById<RelativeLayout>(convertProjectTypeToResId(ProjectConstructor.type)).setPadding(0, 0, 0, dpToPx(3f).toInt())

        arrayOf(
            R.id.project_type_empty,
            R.id.project_type_terminal,
            R.id.project_type_ui
        ).forEach {
            findViewById<RelativeLayout>(it).setOnClickListener { view ->
                changeProjectType(view)
            }
        }

        findViewById<TextView>(R.id.create_project).setOnClickListener {
            val projectNameEdit = findViewById<EditText>(R.id.project_name_edit)
            if (projectNameEdit.text.isEmpty()) {
                projectNameEdit.background = resources.getDrawable(R.drawable.empty_text_error)
                projectNameEdit.hint = "это поле обязательное"

                projectNameEdit.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {}
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {}

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        projectNameEdit.setBackgroundColor(Color.TRANSPARENT)
                        projectNameEdit.hint = "имя проекта"
                    }
                })
                return@setOnClickListener
            }

            ProjectConstructor.name = projectNameEdit.text.toString()
            ProjectConstructor.targetOS = findViewById<Spinner>(R.id.target_os_spinner).selectedItem as String

            projectNameEdit.setText("")
            findViewById<Spinner>(R.id.target_os_spinner).setSelection(0)
            findViewById<RelativeLayout>(convertProjectTypeToResId(ProjectConstructor.type)).setPadding(0, 0, 0, 0)
            findViewById<RelativeLayout>(R.id.project_type_empty).setPadding(0, 0, 0, dpToPx(3f).toInt())

            val loadBar = findViewById<RelativeLayout>(R.id.create_project_loading)
            loadBar.visibility = View.VISIBLE
            val animator = ObjectAnimator.ofFloat(
                loadBar,
                "alpha",
                0.0f, 1.0f
            )
            animator.duration = 200
            animator.start()

            createProject {
                loadBar.alpha = 0.0f
                loadBar.visibility = View.GONE

                addProjectToUI(it)

                findViewById<ViewFlipper>(R.id.header_btn_flipper).displayedChild = 0
                findViewById<ViewFlipper>(R.id.menu_flipper).displayedChild = 0
            }
        }
    }

    private fun createProject(finishCallback: (success: Project) -> Unit) {
        Thread {
            val projectsDir = File("${filesDir.path}/projects")
            if (!projectsDir.exists()) {
                projectsDir.mkdir()
            }

            val projectDir = File("${projectsDir.path}/${ProjectConstructor.name}")
            projectDir.mkdir()

            val project = ProjectConstructor.create(projectDir)

            val config = """
            {
                "name":"${ProjectConstructor.name}",
                "targetOs":"${ProjectConstructor.targetOS}",
                "version":"${project.compilerVersion}",
                "libs": [
                    "io",
                    "_COMPILER_DIR_/libs/io.next"
                ]
            }
            """.trimIndent()

            val configFile = File("${projectDir.path}/config.json")
            configFile.createNewFile()
            val dos = DataOutputStream(FileOutputStream(configFile))
            dos.writeUTF(config)
            dos.close()

            var projectFileDir = File("${projectDir.path}/src")
            projectFileDir.mkdir()
            projectFileDir = File("${projectDir.path}/res")
            projectFileDir.mkdir()
            projectFileDir = File("${projectDir.path}/build")
            projectFileDir.mkdir()

            runOnUiThread {
                finishCallback(project)
            }
        }.start()
    }

    private fun changeProjectType(view: View) {
        if (convertProjectTypeToResId(ProjectConstructor.type) == view.id) return

        val animator = ValueAnimator.ofFloat(0f, 3f)
        animator.duration = 200
        animator.addUpdateListener {
            findViewById<RelativeLayout>(convertProjectTypeToResId(ProjectConstructor.type)).setPadding(0, 0, 0, dpToPx(abs((it.animatedValue as Float) - 3f)).toInt())
            view.setPadding(0, 0, 0, dpToPx(it.animatedValue as Float).toInt())
        }
        animator.addListener(object: AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                ProjectConstructor.type = convertProjectTypeToString(view.id)
            }

            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        animator.start()
    }

    private fun loadProjects() {
        Thread {
            val projectsDir = File("${filesDir.path}/projects")
            if (!projectsDir.exists()) return@Thread

            for (dir in projectsDir.listFiles()!!) {
                for (file in dir.listFiles()!!) {
                    if (file.name == "config.json") {
                        val input = DataInputStream(FileInputStream(file))
                        val config = JSONObject(input.readUTF())
                        input.close()

                        ProjectConstructor.name = config.getString("name")
                        ProjectConstructor.targetOS = config.getString("targetOs")

                        addProjectToUI(ProjectConstructor.create(dir))
                    }
                }
            }
        }.start()
    }

    private fun addProjectToUI(project: Project) {
        val field = LayoutInflater.from(this).inflate(R.layout.project_field, null)
        field.findViewById<TextView>(R.id.project_field_name).text = project.name

        val list = findViewById<LinearLayout>(R.id.projects_list)
        list.removeView(findViewById(R.id.empty_projects_list))
        list.addView(field)

        field.setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            intent.putExtra("project", project)
            startActivity(intent)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }

    private fun convertProjectTypeToString(value: Int): String {
        return when (value) {
            R.id.project_type_empty -> "empty"
            R.id.project_type_terminal -> "terminal"
            R.id.project_type_ui -> "ui"
            else -> "none"
        }
    }

    private fun convertProjectTypeToResId(value: String): Int {
        return when (value) {
            "empty" -> R.id.project_type_empty
            "terminal" -> R.id.project_type_terminal
             "ui" -> R.id.project_type_ui
            else -> 0
        }
    }
}