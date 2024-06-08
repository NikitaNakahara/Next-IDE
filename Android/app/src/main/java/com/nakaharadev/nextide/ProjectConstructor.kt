package com.nakaharadev.nextide

import java.io.File

object ProjectConstructor {
    var type = ""
    var name = ""
    var targetOS = ""

    fun create(projectFiles: File): Project {
        val project = Project()
        project.name = name
        project.targetOS = targetOS
        project.files = projectFiles

        return project
    }
}