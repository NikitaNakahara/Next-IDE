package com.nakaharadev.nextide

import android.app.Activity
import android.os.Bundle

class AppActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.menu)
    }
}