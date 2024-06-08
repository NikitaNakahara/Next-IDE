package com.nakaharadev.nextide

import java.io.File
import java.io.Serializable

class Project : Serializable {
    var name = ""
    var targetOS = ""
    var compilerVersion = "0.0.1"

    var files: File? = null
}