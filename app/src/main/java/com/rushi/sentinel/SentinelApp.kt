package com.rushi.sentinel

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SentinelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Load SQLCipher native libraries
        System.loadLibrary("sqlcipher")
    }
}
