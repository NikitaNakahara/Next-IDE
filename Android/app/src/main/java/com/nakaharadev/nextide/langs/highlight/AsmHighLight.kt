package com.nakaharadev.nextide.langs.highlight

import android.graphics.Color

class AsmHighLight(private val lines: ArrayList<String>) : HighLight() {
    private val tokens = ArrayList<HighLightToken>()

    override fun initHighLight() {

    }

    override fun dynamicHighLight() {

    }

    override fun getTokens(): List<HighLightToken> {
        return tokens
    }
}