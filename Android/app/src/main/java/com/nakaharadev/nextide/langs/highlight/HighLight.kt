package com.nakaharadev.nextide.langs.highlight

import android.graphics.Canvas
import java.util.Locale

open class HighLight {
    companion object {
        fun getInstance(expansion: String, lines: List<String>): HighLight {
            val clazz: Class<*> = Class.forName(
                "com.nakaharadev.nextide.langs.highlight.${
                    expansion.lowercase().replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()
                        ) else it.toString()
                    }
                }HighLight"
            )

            return clazz.getConstructor(ArrayList::class.java)
                .newInstance(lines) as HighLight
        }
    }

    open fun initHighLight() {}
    open fun dynamicHighLight() {}
    open fun getTokens(): List<HighLightToken> { return ArrayList() }
}