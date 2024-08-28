package com.nakaharadev.nextide.langs.highlight

import android.graphics.Color

class NextHighLight(private val lines: ArrayList<String>) : HighLight() {
    private val tokens = ArrayList<HighLightToken>()

    override fun initHighLight() {
        for (i in lines.indices) {
            tokens.add(HighLightToken(1, i + 1, Color.WHITE, lines[i]))
        }
    }

    override fun dynamicHighLight() {

    }

    override fun getTokens(): List<HighLightToken> {
        return tokens
    }
}