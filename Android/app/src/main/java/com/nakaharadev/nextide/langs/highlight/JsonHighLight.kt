package com.nakaharadev.nextide.langs.highlight

import android.graphics.Color

class JsonHighLight(private val lines: ArrayList<String>) : HighLight() {
    private val tokens = ArrayList<HighLightToken>()

    override fun initHighLight() {
        var lexeme = ""
        var isStr = false

        for (i in lines.indices) {
            for (j in lines[i].indices) {
                if (lines[i][j] == '"') {
                    isStr = !isStr
                }

                if (!isStr) {
                    if (lines[i][j] == '{' || lines[i][j] == '}' || lines[i][j] == '[' || lines[i][j] == ']') {
                        tokens.add(
                            HighLightToken(
                                j + 1,
                                i + 1,
                                Color.WHITE,
                                lines[i][j].toString()
                            )
                        )
                    } else if (lines[i][j] == ':') {
                        tokens.add(
                            HighLightToken(
                                j - lexeme.length + 1,
                                i + 1,
                                Color.parseColor("#6688ee"),
                                lexeme
                            )
                        )
                        tokens.add(
                            HighLightToken(
                                j + 1,
                                i + 1,
                                Color.WHITE,
                                lines[i][j].toString()
                            )
                        )
                        lexeme = ""
                    } else if (lines[i][j] == ',') {
                        tokens.add(
                            HighLightToken(
                                j - lexeme.length + 1,
                                i + 1,
                                Color.parseColor("#eebb77"),
                                lexeme
                            )
                        )
                        tokens.add(
                            HighLightToken(
                                j + 1,
                                i + 1,
                                Color.WHITE,
                                lines[i][j].toString()
                            )
                        )
                        lexeme = ""
                    } else if (j == lines[i].length - 1) {
                        tokens.add(
                            HighLightToken(
                                j - lexeme.length + 1,
                                i + 1,
                                Color.parseColor("#eebb77"),
                                "$lexeme\""
                            )
                        )
                        lexeme = ""
                    } else {
                        lexeme += lines[i][j]
                    }
                } else {
                    lexeme += lines[i][j]
                }
            }
        }
    }

    override fun dynamicHighLight() {

    }

    override fun getTokens(): List<HighLightToken> {
        return tokens
    }
}