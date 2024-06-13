package com.nakaharadev.nextide.langs.highlight

data class HighLightToken (
    var startXPos: Int,
    var yPos: Int,
    var color: Int,
    var lexeme: String,
)